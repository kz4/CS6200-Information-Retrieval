package com.ir.readindex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;

import com.ir.util.Constants;

/**
 * Basic set is all the pages in the index
 * Root set is the top 1000 pages ranked by an IR function (e.g. BM25, ES Search)
 * Obtain the root set of documents by ranking all pages using an ES search
 * Used ES search for simplicity and better results
 * and add it to the root set (Using ES Search in this case)
 * Query: cold war
 *
 */
public class ReadIndex {

	public static void main(String[] args) throws IOException {

		Settings setting = ImmutableSettings.settingsBuilder()
    			.put("cluster.name", Constants.cluster_name)
    			.build();
		TransportClient transportClient = new TransportClient(setting);
        transportClient = transportClient.addTransportAddress(new InetSocketTransportAddress("localhost", 9300));
		
		if(transportClient.connectedNodes().size() == 0)
		{
			System.out.println("There are no active nodes available for the transport, it will be automatically added once nodes are live!");
		}
		Client client = transportClient;
//		QueryBuilder qb = QueryBuilders.termQuery("text", Constants.query);
		QueryStringQueryBuilder qb = new QueryStringQueryBuilder("cold war").field("text");
//		QueryBuilder qb = new MatchAllQueryBuilder();
		String index_name = Constants.index_name;
		String document_type = Constants.document_type;

		SearchResponse scrollResp = client.prepareSearch(index_name).setTypes(document_type)
				.setScroll(new TimeValue(6000))
				.setQuery(qb)
//				.setExplain(true)
				.setSize(1000).execute().actionGet();

		// no query matched
		Map<String, Integer> results = new HashMap<>();
		
		FileWriter ostream_cat = new FileWriter(Constants.top1000Link_);
		BufferedWriter out_auth = new BufferedWriter(ostream_cat);

		System.out.println(scrollResp.getHits().getTotalHits());
		while (true) {
			for (SearchHit hit : scrollResp.getHits().getHits()) {
				out_auth.write(hit.getId() + "\n");
				System.out.println(hit.getId());
			}
			System.exit(-1);

			scrollResp =
					client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(
							new TimeValue(6000)).execute().actionGet();
			
			if (scrollResp.getHits().getHits().length == 0) {
				break;
			}
		}
	}
}
