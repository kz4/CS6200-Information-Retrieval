package com.ir.hw1;

import org.apache.lucene.index.Fields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.termvector.TermVectorRequest;
import org.elasticsearch.action.termvector.TermVectorResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.*;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilder.*;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.MetricsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.cardinality.Cardinality;
import org.elasticsearch.search.facet.statistical.StatisticalFacet;

import com.ir.hw1.util.Constants;
import com.ir.hw1.util.TfTotalTfTotalLenOfDoc;

import java.io.*;
import java.util.*;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.elasticsearch.index.query.FilterBuilders.*;
import static org.elasticsearch.index.query.QueryBuilders.*;

public class RunQuery {
	
	private Client client_ = null;
	
	public RunQuery(){
		ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();        
		TransportClient transportClient = new TransportClient(settings);
        transportClient = transportClient.addTransportAddress(new InetSocketTransportAddress("localhost", 9300));
        
        if(transportClient.connectedNodes().size() == 0)
        {
            System.out.println("There are no active nodes available for the transport, it will be automatically added once nodes are live!");
        }
        Client client = transportClient;
        
        client_ = client;
	}
	
	public static void main(String[] args) throws IOException {
		//	        if (args.length != 1) {
		//	            throw new IllegalArgumentException("Only Need config file.");
		//	        }
		//
		//	        Config config = new Config(args[0]);
		// starts client
		//	        String clusterName = "elasticsearch";
		Config config = new Config(Constants.configPath);    	
		String clusterName = config.getString("cluster.name");

		// Very slow to start a client from a node
		//Node node = nodeBuilder().client(true).clusterName(clusterName).node();
//		Client client = node.client();

		ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();        
		TransportClient transportClient = new TransportClient(settings);
        transportClient = transportClient.addTransportAddress(new InetSocketTransportAddress("localhost", 9300));
        
        if(transportClient.connectedNodes().size() == 0)
        {
            System.out.println("There are no active nodes available for the transport, it will be automatically added once nodes are live!");
        }
        Client client = transportClient;
        
        RunQuery rq = new RunQuery();
        try {
			rq.termVectors(client);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * V is the vocabulary size - the total number of unique terms in the collection.
	 * @param client - "ES Client"
	 * @param index - "ap_dataset"
	 * @param type - "document"
	 * @param field - "text"
	 * @return the Vocabulary size, for ap89, it's 178050
	 */
	public long getVocabularySize(Client client, String index, String type, String field) {
		MetricsAggregationBuilder aggregation =
				AggregationBuilders
				.cardinality("agg")
				.field(field);
		SearchResponse sr = client.prepareSearch(index).setTypes(type)
				.addAggregation(aggregation)
				.execute().actionGet();

		Cardinality agg = sr.getAggregations().get("agg");
		long value = agg.getValue();
		return value;

	}

	/**
	 * NOT USED in the code, use getTFGroovy() below instead
	 * This method has problem b/c not all values are at getDetails()[0].getDetails()[0].getDetails()[0].getValue();
	 * If you want to use this method, however, look for the description that starts with "phraseFreq=" or "termFreq="
	 * return Pairs of <"decno", tf value> by given term query.
	 * @param client
	 * @param qb
	 * @param index
	 * @param type
	 * @return
	 */
	public Map<String, Integer> queryTF(Client client, QueryBuilder qb, String index, String type) {
		SearchResponse scrollResp = client.prepareSearch(index).setTypes(type)
				.setScroll(new TimeValue(6000))
				.setQuery(qb)
				.setExplain(true)
				.setSize(1000).execute().actionGet();

		// no query matched
		if (scrollResp.getHits().getTotalHits() == 0) {
			return new HashMap<String, Integer>();
		}
		Map<String, Integer> results = new HashMap<>();

		SearchHit[] hits = scrollResp.getHits().getHits();
		while (true) {
			for (SearchHit hit : scrollResp.getHits().getHits()) {
				String docno = (String) hit.getSource().get("docno");
				if (hit.getExplanation().getDetails()[0].getDetails()[0].getDetails()[0].getValue() != (int) hit.getExplanation().getDetails()[0].getDetails()[0].getDetails()[0].getValue())
					System.out.println(hit.getExplanation().getDetails()[0].getDetails()[0].getDetails()[0].getDescription());
				int tf =  (int) hit.getExplanation().getDetails()[0].getDetails()[0].getDetails()[0].getValue();

				results.put(docno, tf);
			}
			scrollResp =
					client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(
							new TimeValue(6000)).execute().actionGet();
			if (scrollResp.getHits().getHits().length == 0) {
				break;
			}
		}
		System.out.println(results);
		return results;
	}

	/**
	 * get statistical facet by given docno or whole documents
	 * INFO including following:
     "facets": {
        "text": {
             "_type": "statistical",
             "count": 84678,
             "total": 18682561,
             "min": 0,
             "max": 802,
             "mean": 220.63063605659084,
             "sum_of_squares": 4940491417,
             "variance": 9666.573376838636,
             "std_deviation": 98.31873360066552
        }
     }
	 * @param client - in this case, an instance of TransportClient
	 * @param index - in this case, "ap_dataset"
	 * @param type - in this case, "document"
	 * @param matchedField - if passing null for both matchedField and matchedValue, will yield average document length
	 * @param matchedValue - if passing null for both matchedField and matchedValue, will yield average document length
	 * if passing "docno" for matchedField and an actual value e.g. "AP891216-0142" will yield that document's length
	 * @return
	 * @throws IOException
	 */
	public StatisticalFacet getStatsOnTextTerms(Client client, String index, String type, String matchedField, String matchedValue) throws IOException {
		XContentBuilder facetsBuilder;
		if (matchedField == null && matchedValue == null) {    // match_all docs
			facetsBuilder = getStatsTermsBuilder();
		}
		else {
			facetsBuilder = getStatsTermsByMatchFieldBuilder(matchedField, matchedValue);
		}
		SearchResponse response = client.prepareSearch(index).setTypes(type)
				.setSource(facetsBuilder)
				.execute()
				.actionGet();
		StatisticalFacet f = (StatisticalFacet) response.getFacets().facetsAsMap().get("text");
		return f;
	}


	/**
	 * builder for facets statistical terms length by given matched field, like docno.
	 * In Sense:
	 *
	 * statistical is depreciated, use aggregation instead
     POST ap_dataset/document/_search
     {
        "query": {
             "match": {
                 "docno": "AP891216-0142"
            }
        },
        "facets": {
            "text": {
                "statistical": {
                    "script": "doc['text'].values.size()"
                }
            }
        }
     }
     
     # avg(len(d)) : 164.7511632301188
	POST /ap_dataset/document/_search
	{
	  "aggs": {
	    "avg_docs": {
	      "stats": {
	        "script": "doc['text'].values.size()"
	      }
	    }
	  }
	}
	
	# Document length 113 for "AP891216-0142", USE THIS
	POST /ap_dataset/document/_search
	{
	  "query": {
	    "match": {
	      "docno": "AP891216-0142"
	    }
	  }, 
	  "aggs": {
	    "avg_docs": {
	      "stats": {
	        "script": "doc['text'].values.size()"
	      }
	    }
	  }
	}
	 * @param matchField - in this case, "docno"
	 * @param matchValue - in this case, "AP891216-0142"
	 * @return The length of document with "docno" matchValue, which is 113
	 * @throws IOException
	 */
	private static XContentBuilder getStatsTermsByMatchFieldBuilder(String matchField, String matchValue) throws IOException {
		XContentBuilder builder = XContentFactory.jsonBuilder();
		builder.startObject()
		.startObject("query")
		.startObject("match")
		.field(matchField, matchValue)
		.endObject()
		.endObject()
		.startObject("facets")
		.startObject("text")
		.startObject("statistical")
		.field("script", "doc['text'].values.size()")
		.endObject()
		.endObject()
		.endObject()
		.endObject();
		return builder;
	}

	/**
	 * For example
	 * GET ap_dataset/document/_search
		{
		  "fields": "docno", 
		  "query": {
		    "function_score": {
		      "query": {
		        "match": {
		          "text": "alleg"
		        }
		      },
		      "functions": [
		        {
		          "script_score": {
		            "lang": "groovy",
		            "script": "_index[field][term].tf()",
		            "params": {
		              "term": "alleg",
		              "field": "text"
		            }
		          }
		        }
		      ],
		      "boost_mode": "replace"
		    }
		  }
		}
		
		If you want to use script file instead:
		GET ap_dataset/document/_search
		{
		  "fields": "docno", 
		  "query": {
		    "function_score": {
		      "query": {
		        "match": {
		          "text": "government"
		        }
		      },
		      "functions": [
		        {
		          "script_score": {
		            "lang": "groovy",
		            "script_file": "tf_score",
		            "params": {
		              "term": "alleg",
		              "field": "government"
		            }
		          }
		        }
		      ],
		      "boost_mode": "replace"
		    }
		  }
		}
		Find the folder where the elasticsearch is saved.
		1. Create a folder scripts inside config folder 2. Create file tf_score.groovy
		3. Open tf_score.groovy and put _index[field][term].tf() inside
	 *	In the code below it comes
	 *	with one less "query" because in getTFGroovy there is a setQuery
	 * @param matchField - e.g. "text"
	 * @param matchValue - e.g. "alleg"
	 * @return
	 * @throws IOException
	 */
	private static XContentBuilder tFGroovy(String matchField, String matchValue) throws IOException {
		XContentBuilder builder = XContentFactory.jsonBuilder();
		builder.prettyPrint()
		.startObject()
		//.startObject("query")
		.startObject("function_score")
		.startObject("query")
		
		// If I use "match" instead of "term" here,
		// when I search for government, I will find 29151 total "hits"
		// with term I will find 423, which is much more correct
		.startObject("term")
		.field(matchField, matchValue)
		.endObject()
		.endObject()
		.startArray("functions")
		.startObject()
		.startObject("script_score")
		.field("lang", "groovy")
		.field("script", "_index[field][term].tf()")
		.startObject("params")
		.field("term", matchValue)
		.field("field", matchField)
		.endObject()
		.endObject()
		.endObject()
		.endArray()
		.field("boost_mode", "replace")
		.endObject();
		//.endObject();
		
		// Debug purpose
		//System.out.println(builder.string());
		return builder;
	}

	/**
	 * 
	 * @param client - in this case, an instance of TransportClient
	 * @param index - in this case, "ap_dataset"
	 * @param type - in this case, "document"
	 * @param matchedField - in this case, "text"
	 * @param matchedValue - a word, for example "alleg"
	 * @return - an instance of TfTotalTfTotalLenOfDoc, which includes Maps of docno and tf for a word in a query in a doc
	 * total tf for a word in all documents
	 * total length of the documents for that has the word in it 
	 * @throws IOException
	 */
	public TfTotalTfTotalLenOfDoc getTFGroovy(Client client, String index, String type, String matchedField, String matchedValue) throws IOException {

		XContentBuilder tfBuilder = tFGroovy(matchedField, matchedValue);
		
		SearchResponse response = client.prepareSearch(index).setTypes(type)
				.setScroll(new TimeValue(300000))
				.setQuery(tfBuilder)
				.addField("docno")
				.setSize(1000)
				.execute()
				.actionGet();
		

		Map<String, Integer> results = new HashMap<String, Integer>();
		
		// no query matched
		if (response.getHits().getTotalHits() == 0) {
			TfTotalTfTotalLenOfDoc t = new TfTotalTfTotalLenOfDoc();
			t.docno_tf_map_ = results;
			t.totalTf_ = 0;
			t.totalLenOfDoc_ = 0;
			return t;
		}

		SearchHit[] hits = response.getHits().getHits();
		
//		if (hits.length == 0){
//			TfTotalTfTotalLenOfDoc t = new TfTotalTfTotalLenOfDoc();
//			t.docno_tf_map_ = results;
//			t.totalTf_ = 0;
//			t.totalLenOfDoc_ = 0;
//			return t;
//		}
		
		int totalTF = 0;
		double totalLenOfDoc = 0;
		while (true) {
			try {
				for (SearchHit hit : response.getHits().getHits()) {
					//                String docno = (String) hit.getFields().get("docno");
					String docno = (String) hit.field("docno").getValue();
					int score = (int) hit.score();

					totalTF += score;
					
					StatisticalFacet stats2 = null;
					try {
						stats2 = getStatsOnTextTerms(client_, "ap_dataset", "document", "docno", docno);
					} catch (IOException e) {
						e.printStackTrace();
					}

					double lenOfDoc = stats2.getTotal();
					totalLenOfDoc += lenOfDoc;
					
					results.put(docno, score);
				}
			} catch (Exception e1) {
				System.out.println("Term: " + matchedValue);
				e1.printStackTrace();
			}

			try {
				response = client.prepareSearchScroll(response.getScrollId()).setScroll(
								new TimeValue(300000)).execute().actionGet();
			} catch (ElasticsearchException e) {
				e.printStackTrace();
				System.out.println("docno: " + results);
			}
			//System.out.println(response);
			
			try {
				if (response.getHits().getHits().length == 0) {
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("docno: " + results);
			}
		}

		// Debug purpose
		// System.out.println(results);
		TfTotalTfTotalLenOfDoc t = new TfTotalTfTotalLenOfDoc();
		t.docno_tf_map_ = results;
		t.totalTf_ = totalTF;
		t.totalLenOfDoc_ = totalLenOfDoc;
		
		return t;
	}


	/**
	 * builder for the facets statistical terms length by whole documents.
	 * In Sense:
	 * POST /ap_dataset/document/_search
        {
         "query": {"match_all": {}},
            "facets": {
                "text": {
                    "statistical": {
                         "script": "doc['text'].values.size()"
                    }
                 }
             }
         }
	 * @return
	 * @throws IOException
	 */
	private static XContentBuilder getStatsTermsBuilder() throws IOException {
		XContentBuilder builder = XContentFactory.jsonBuilder();
		builder.startObject()
		.startObject("query")
		.startObject("match_all")
		.endObject()
		.endObject()
		.startObject("facets")
		.startObject("text")
		.startObject("statistical")
		.field("script", "doc['text'].values.size()")
		.endObject()
		.endObject()
		.endObject()
		.endObject();
		return builder;
	}

	public void termVectors(Client client) throws Exception {
		//Client client = new TransportClient().addTransportAddress(new InetSocketTransportAddress("localhost",9300));
		TermVectorRequest req = new TermVectorRequest("ap_dataset", "document", "84678");
		req = req.termStatistics(true)
				.offsets(true)
				.payloads(true)
				.fieldStatistics(true)
				.positions(true)
				.fieldStatistics(true)
				.selectedFields(new String[] {"text"});

		TermVectorResponse response = client.termVector(req).get();
		//response.getFields()
		// tf = result['term_vectors']['text']['terms'][term]['term_freq']

		//tf = response.['term_vectors']['text']['terms'][term]['term_freq']


		Fields fields = response.getFields();
//		System.out.println(fields.size());
//		Terms terms = fields.terms("charter");

		for(String field : fields) {
		    Terms terms = fields.terms(field);
		    TermsEnum termsEnum = terms.iterator(null);
		    BytesRef text;
		    while((text = termsEnum.next()) != null) {
		      System.out.println("field=" + field + "; text=" + text.utf8ToString());
		  }
		}
	}

}