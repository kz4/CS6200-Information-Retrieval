package com.ir.feature;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.SpanNearQueryBuilder;
import org.elasticsearch.index.query.SpanTermQueryBuilder;
import org.elasticsearch.search.SearchHit;

import com.ir.util.Constants;
import com.ir.util.FileWriter_Helper;

public class ObtainTermFeatureScore {

	public Client client_;

	public ObtainTermFeatureScore(){
		String clusterName = Constants.clusterName;
		System.out.println("Cluster name: " + clusterName);

		ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();        
		TransportClient transportClient = new TransportClient(settings);
		transportClient = transportClient.addTransportAddress(new InetSocketTransportAddress("localhost", 9300));

		if(transportClient.connectedNodes().size() == 0)
		{
			System.out.println("There are no active nodes available for the transport, it will be automatically added once nodes are live!");
		}
		client_ = transportClient;
	}

	public List<Map<String, Double>> getLstNGramMailIdScoreMap(){
		List<Map<String, Double>> lst_mailId_score_map = new ArrayList<Map<String,Double>>();
		String[] ngrams = Constants.ngrams;
		for (int i = 0; i < ngrams.length; i++) {
			// Unigram
			if (ngrams[i].split(" ").length == 1){
				Map<String, Double> mailId_score_map = getUnigramScore(ngrams[i]);
				lst_mailId_score_map.add(mailId_score_map);

			}
			// Bigram
			if (ngrams[i].split(" ").length == 2){
				Map<String, Double> mailId_score_map = getBigramScore(ngrams[i].split(" ")[0], ngrams[i].split(" ")[1]);
				lst_mailId_score_map.add(mailId_score_map);
			}
		}

		return lst_mailId_score_map;
	}

	public Map<String, String> getMailIdSpamMap() {
		QueryBuilder qb = QueryBuilders.matchAllQuery();
		SearchResponse scrollResp = client_.prepareSearch(Constants.indexName)
				.setTypes(Constants.typeName)
				.setScroll(new TimeValue(6000))
				.setQuery(qb)
				.setExplain(true)
				.setSize(1000).execute().actionGet();

		/* No match */
		if (scrollResp.getHits().getTotalHits() == 0)
		{
			return new HashMap<String, String>();
		}

		Map<String, String> mailId_spam_map = new HashMap<String, String>();
		while (true)
		{
			for (SearchHit hit : scrollResp.getHits().getHits())
			{
				String mailId = (String) hit.getId();
				String spam; 
				if (hit.getSource().get("split").equals("test"))
					spam = "?";
				else{
					if (hit.getSource().get("label").equals("spam"))
						spam = "1";
					else
						spam = "0";
				}
				mailId_spam_map.put(mailId, spam);
			}
			scrollResp = client_.prepareSearchScroll(scrollResp.getScrollId()).setScroll(
					new TimeValue(6000)).execute().actionGet();
			if (scrollResp.getHits().getHits().length == 0)
			{
				break;
			}
		}

		return mailId_spam_map;
	}

	public Map<String, String> getActualMailIdSpamMap() {
		QueryBuilder qb = QueryBuilders.matchAllQuery();
		SearchResponse scrollResp = client_.prepareSearch(Constants.indexName)
				.setTypes(Constants.typeName)
				.setScroll(new TimeValue(6000))
				.setQuery(qb)
				.setExplain(true)
				.setSize(1000).execute().actionGet();

		/* No match */
		if (scrollResp.getHits().getTotalHits() == 0)
		{
			return new HashMap<String, String>();
		}

		Map<String, String> mailId_spam_map = new HashMap<String, String>();
		while (true)
		{
			for (SearchHit hit : scrollResp.getHits().getHits())
			{
				String mailId = (String) hit.getId();
				String spam; 
				if (hit.getSource().get("label").equals("spam"))
					spam = "1";
				else
					spam = "0";
				mailId_spam_map.put(mailId, spam);
			}
			scrollResp = client_.prepareSearchScroll(scrollResp.getScrollId()).setScroll(
					new TimeValue(6000)).execute().actionGet();
			if (scrollResp.getHits().getHits().length == 0)
			{
				break;
			}
		}

		return mailId_spam_map;
	}


	private Map<String, Double> getUnigramScore(String term){
		QueryBuilder qb = QueryBuilders.matchQuery(Constants.esFieldBody, term);
		SearchResponse scrollResp = client_.prepareSearch(Constants.indexName)
				.setTypes(Constants.typeName)
				.setScroll(new TimeValue(6000))
				.setQuery(qb)
				.setExplain(true)
				.setSize(1000).execute().actionGet();
		/* No match */
		if (scrollResp.getHits().getTotalHits() == 0)
		{
			return new HashMap<String, Double>();
		}

		Map<String, Double> results = new HashMap<String, Double>();
		while (true)
		{
			for (SearchHit hit : scrollResp.getHits().getHits())
			{
				String mailId = (String) hit.getId();
				//				int tf = (int) hit.getExplanation().getDetails()[0].getDetails()[0].getDetails()[0].getValue();
				double tf = hit.getScore();
				results.put(mailId, tf);
			}
			scrollResp = client_.prepareSearchScroll(scrollResp.getScrollId()).setScroll(
					new TimeValue(6000)).execute().actionGet();
			if (scrollResp.getHits().getHits().length == 0)
			{
				break;
			}
		}
		return results;
	}

	private Map<String, Double> getBigramScore(String firstTerm, String secTerm) {
		QueryBuilder qb = QueryBuilders.spanNearQuery()
				.clause(new SpanTermQueryBuilder(Constants.esFieldBody, firstTerm))    
				.clause(new SpanTermQueryBuilder(Constants.esFieldBody, secTerm))    
				.slop(0)
				.inOrder(true)                             
				.collectPayloads(false);
		//		System.out.println(qb.toString());

		SearchResponse scrollResp = client_.prepareSearch(Constants.indexName)
				.setTypes(Constants.typeName)
				.setScroll(new TimeValue(6000))
				.setQuery(qb)
				.setExplain(true)
				.setSize(1000).execute().actionGet();

		/* No match */
		if (scrollResp.getHits().getTotalHits() == 0)
		{
			return new HashMap<String, Double>();
		}

		Map<String, Double> results = new HashMap<String, Double>();
		while (true)
		{
			for (SearchHit hit : scrollResp.getHits().getHits())
			{
				String file = (String) hit.getId();
				//				int tf = (int) hit.getExplanation().getDetails()[0].getDetails()[0].getDetails()[0].getValue();
				double tf = hit.getScore();
				results.put(file, tf);
			}
			scrollResp = client_.prepareSearchScroll(scrollResp.getScrollId()).setScroll(
					new TimeValue(6000)).execute().actionGet();
			if (scrollResp.getHits().getHits().length == 0)
			{
				break;
			}
		}
		return results;
	}

	public static void main(String[] args) {
		ObtainTermFeatureScore featureScore = new ObtainTermFeatureScore();
		featureScore.getLstNGramMailIdScoreMap();
		Map<String, String> mailId_spam_map = featureScore.getMailIdSpamMap();
	}
}
