package com.ir.indexer;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import com.ir.readtrec07p.DocumentInfo;
import com.ir.readtrec07p.ReadTrec07P;
import com.ir.util.Constants;
import com.ir.util.FileWriter_Helper;

public class IndexBuilder {



	public static void main(String[] args) throws IOException {

		String clusterName = Constants.clusterName;
		System.out.println("Cluster name: " + clusterName);

		ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();        
		TransportClient transportClient = new TransportClient(settings);
		transportClient = transportClient.addTransportAddress(new InetSocketTransportAddress("localhost", 9300));

		if(transportClient.connectedNodes().size() == 0)
		{
			System.out.println("There are no active nodes available for the transport, it will be automatically added once nodes are live!");
		}
		Client client = transportClient;

		if (!Constants.ngram){
			// 1) set settings and analysis
			XContentBuilder settingsBuilder = getUnigramSettingsBuilder();
			client.admin().indices().prepareCreate(Constants.indexName)
			.setSettings(ImmutableSettings.settingsBuilder().loadFromSource(settingsBuilder.string()))
			.execute()
			.actionGet();
	
			// 2) set mapping
			XContentBuilder mappingBuilder = getUnigramMappingBuilder();
			client.admin().indices().preparePutMapping(Constants.indexName)
			.setType(Constants.typeName)
			.setSource(mappingBuilder)
			.execute()
			.actionGet();
	
			// 3) index files to documents
			System.out.println("Started reading mailId label map");
			ReadTrec07P trec = new ReadTrec07P();
			trec.read_spam_ham(Constants.spamHamInfo);
			System.out.println("Finished reading mailId label map");
	
			System.out.println("Started populating mailId body label split list");
			List<DocumentInfo> lstDocInfo = trec.read_trec_mails(Constants.dataFol);
			System.out.println("Finished populating mailId body label split list");

			System.out.println("Started indexing");

			try{
				//Delete if filename exists because we are using appending for new lines
				File fileTemp = new File(Constants.lstDocInfo);
				if (fileTemp.exists()){
					fileTemp.delete();
				}   
			}catch(Exception e){
				// if any error occurs
				e.printStackTrace();
			}
			for (DocumentInfo documentInfo : lstDocInfo) {
				String info = documentInfo.file_name_ + " " + documentInfo.label_ + " " + documentInfo.split_+ " " + documentInfo.body_;
				FileWriter_Helper.appendToFile(Constants.lstDocInfo, info);
				XContentBuilder builder;
				try {
					builder = jsonBuilder()
							.startObject()
							.field("label", documentInfo.label_)
							.field("body", documentInfo.body_)
							.field("split", documentInfo.split_)
							.endObject();

					GetResponse getResponse = client.prepareGet(Constants.indexName, Constants.typeName, documentInfo.file_name_)
							.execute()
							.actionGet();

					if (!getResponse.isExists()) {
						IndexResponse resp = client.prepareIndex(
								Constants.indexName,
								Constants.typeName,
								documentInfo.file_name_)
								.setSource(builder)
								.execute()
								.actionGet();

						System.out.println(documentInfo.file_name_ + " " + resp.isCreated());
					} else {
						System.out.println(documentInfo.file_name_ + " already exists");
					}

				} catch ( IOException e ) {
					e.printStackTrace();
				}
			}
			System.out.println("Finished indexing");
		} else {
			// 1) set settings and analysis
			XContentBuilder settingsBuilder = getNgramSettingsBuilder();
			client.admin().indices().prepareCreate(Constants.nGramIndexName)
			.setSettings(ImmutableSettings.settingsBuilder().loadFromSource(settingsBuilder.string()))
			.execute()
			.actionGet();

			// 2) set mapping
			XContentBuilder mappingBuilder = getNGramSettingsBuilder();
			client.admin().indices().preparePutMapping(Constants.nGramIndexName)
			.setType(Constants.typeName)
			.setSource(mappingBuilder)
			.execute()
			.actionGet();

			// 3) index files to documents
			System.out.println("Started reading mailId label map");
			ReadTrec07P trec = new ReadTrec07P();
			trec.read_spam_ham(Constants.spamHamInfo);
			System.out.println("Finished reading mailId label map");

			System.out.println("Started populating mailId body label split list");
			List<DocumentInfo> lstDocInfo = trec.read_trec_mails(Constants.dataFol);
			System.out.println("Finished populating mailId body label split list");
			
			System.out.println("Started indexing");

			try{
				//Delete if filename exists because we are using appending for new lines
				File fileTemp = new File(Constants.nGramLstDocInfo);
				if (fileTemp.exists()){
					fileTemp.delete();
				}   
			}catch(Exception e){
				// if any error occurs
				e.printStackTrace();
			}
			for (DocumentInfo documentInfo : lstDocInfo) {
				String info = documentInfo.file_name_ + " " + documentInfo.label_ + " " + documentInfo.split_+ " " + documentInfo.body_;
				FileWriter_Helper.appendToFile(Constants.nGramLstDocInfo, info);
				XContentBuilder builder;
				try {
					builder = jsonBuilder()
							.startObject()
							.field("label", documentInfo.label_)
							.field("body", documentInfo.body_)
							.field("split", documentInfo.split_)
							.endObject();

					GetResponse getResponse = client.prepareGet(Constants.nGramIndexName, Constants.nGramTypeName, documentInfo.file_name_)
							.execute()
							.actionGet();

					if (!getResponse.isExists()) {
						IndexResponse resp = client.prepareIndex(
								Constants.indexName,
								Constants.typeName,
								documentInfo.file_name_)
								.setSource(builder)
								.execute()
								.actionGet();

						System.out.println(documentInfo.file_name_ + " " + resp.isCreated());
					} else {
						System.out.println(documentInfo.file_name_ + " already exists");
					}

				} catch ( IOException e ) {
					e.printStackTrace();
				}
			}
			System.out.println("Finished indexing");
		}


		transportClient.close();
		client.close();
	}

	/*
	 *  PUT /ngram_spam_trec07p/
		{
            "settings": {
                "index": {
                    "number_of_shards": 5,
                    "number_of_replicas": 0
                },
                "analysis": {
                    "filter": {
                        "index_filter": {
                            "type": "common_grams",
                            "common_words": "_english_"
                        },
                        "search_filter": {
                            "type": "common_grams",
                            "common_words": "_english_",
                            "query_mode": True
                        }
                    },
                    "analyzer": {
                        "fulltext_analyzer": {
                            "type": "english",
                            "stopwords_path": STOPWORDS_FILE
                        },
                        "index_grams": {
                            "tokenizer": "standard",
                            "filter": ["lowercase", "index_filter"]
                        },
                        "search_grams": {
                            "tokenizer": "standard",
                            "filter": ["lowercase", "search_filter"]
                        }
                    }
                },
            }
		}
	 */
	private static XContentBuilder getNGramSettingsBuilder() throws IOException {  	
		Object[] filterValue = {"lowercase", "index_filter"};
		XContentBuilder builder = XContentFactory.jsonBuilder();
		builder.startObject()
		.startObject("settings")
		.startObject("index")
		.field("number_of_shards", 1)
		.field("number_of_replicas", 1)
		.endObject()
		.startObject("analysis")
		.startObject("filter")
		.startObject("index_filter")
		.field("type", "common_grams")
		.field("common_words", "_english_")
		.endObject()
		.startObject("search_filter")
		.field("type", "common_grams")
		.field("common_words", "_english_")
		.field("query_mode", true)
		.endObject()
		.endObject()
		.startObject("analyzer")
		.startObject("fulltext_analyzer")
		.field("type", "english")
		.field("stopwords_path", Constants.stoplistPath)
		.endObject()
		.startObject("index_grams")
		.field("tokenizer", "standard")
		.array("filter", filterValue)
		.endObject()
		.startObject("search_grams")
		.field("tokenizer", "standard")
		.array("filter", filterValue)
		.endObject()
		.endObject()
		.endObject()
		.endObject();
		System.out.println(builder.string());
		return builder;
	}
	
	/*
	 *  PUT /spam_trec07p/
		{
		  "settings": {
		    "index": {
		      "store": {
		        "type": "default"
		      },
		      "number_of_shards": 1,
		      "number_of_replicas": 1
		    },
		    "analysis": {
		      "analyzer": {
		        "my_english": { 
		          "type": "english",
		          "stopwords_path": "stoplist.txt" 
		        }
		      }
		    }
		  }
		}
	 */
	private static XContentBuilder getUnigramSettingsBuilder() throws IOException {  	
		XContentBuilder builder = XContentFactory.jsonBuilder();
		builder.startObject()
		.startObject("settings")
		.startObject("index")
		.startObject("score")
		.field("type", "default")
		.endObject()
		.field("number_of_shards", 1)
		.field("number_of_replicas", 1)
		.endObject()
		.endObject()
		.startObject("analysis")
		.startObject("analyzer")
		.startObject("my_english")
		.field("type", "english")
		.field("stopwords_path", Constants.stoplistPath)
		.endObject()
		.endObject()
		.endObject()
		.endObject();
		return builder;
	}
	
	/*
	 * PUT /ngram_spam_trec07p/document/_mapping
		{
		  "document": {
		    "properties": {
		      "label": {
		        "type": "string",
		        "store": true,
		        "index": "not_analyzed"
		      },
		      "body": {
		        "type": "string",
		        "store": true,
		        "index_analyzer": "index_grams",
                "search_analyzer": "standard",
		        "term_vector": "with_positions_offsets_payloads",
		        "analyzer": "my_english"
		      },
		      "split": {
		        "type": "string",
		        "store": true,
		        "index": "not_analyzed"
		      }
		    }
		  }
		}
	 */
	private static XContentBuilder getNgramSettingsBuilder() throws IOException {  	
		XContentBuilder builder = XContentFactory.jsonBuilder();
		builder.startObject()
		.startObject(Constants.typeName)
		.startObject("properties")
		.startObject("label")
		.field("type", "string")
		.field("store", true)
		.field("index", "not_analyzed")
		.endObject()
		.startObject("body")
		.field("type", "string")
		.field("store", true)
		.field("index_analyzer", "index_grams")
		.field("search_analyzer", "standardx")
		.field("term_vector", "with_positions_offsets_payloads")
		.field("analyzer", "my_english")
		.endObject()
		.startObject("split")
		.field("type", "string")
		.field("store", true)
		.field("index", "not_analyzed")
		.endObject()
		.endObject()
		.endObject()
		.endObject();
		return builder;
	}

	/*
	 *  PUT /spam_trec07p/document/_mapping
		{
		  "document": {
		    "properties": {
		      "label": {
		        "type": "string",
		        "store": true,
		        "index": "not_analyzed"
		      },
		      "body": {
		        "type": "string",
		        "store": true,
		        "index": "analyzed",
		        "term_vector": "with_positions_offsets_payloads",
		        "analyzer": "my_english"
		      },
		      "split": {
		        "type": "string",
		        "store": true,
		        "index": "not_analyzed"
		      }
		    }
		  }
		}
	 */
	private static XContentBuilder getUnigramMappingBuilder() throws IOException {
		XContentBuilder builder = XContentFactory.jsonBuilder();
		builder.startObject()
		.startObject(Constants.typeName)
		.startObject("properties")
		.startObject("label")
		.field("type", "string")
		.field("store", true)
		.field("index", "not_analyzed")
		.endObject()
		.startObject("body")
		.field("type", "string")
		.field("store", true)
		.field("index", "analyzed")
		.field("term_vector", "with_positions_offsets_payloads")
		.field("analyzer", "my_english")
		.endObject()
		.startObject("split")
		.field("type", "string")
		.field("store", true)
		.field("index", "not_analyzed")
		.endObject()
		.endObject()
		.endObject()
		.endObject();
		return builder;
	}
}
