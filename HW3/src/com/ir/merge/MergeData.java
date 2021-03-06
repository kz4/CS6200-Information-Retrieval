package com.ir.merge;

import com.ir.util.Constants;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Created by kingkz on 6/22/15.
 */
public class MergeData {

    MongoClient mongoClient;

    MongoDatabase mongoDatabase;

    private Client elasticsearchClient;

    public MergeData() {
        this.mongoClient = new MongoClient();
        this.mongoDatabase = mongoClient.getDatabase(Constants.mongoDB_database);

        this.elasticsearchClient = getClient();
    }
    
    public Client getClient() {
//    	ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();
    	Settings setting = ImmutableSettings.settingsBuilder()
    			.put("cluster.name", Constants.cluster_name)
    			.build();
		TransportClient transportClient = new TransportClient(setting);
        transportClient = transportClient.addTransportAddress(new InetSocketTransportAddress(Constants.ip_address, 9300));
        
//        if(transportClient.connectedNodes().size() == 0)
//        {
//            System.out.println("There are no active nodes available for the transport, it will be automatically added once nodes are live!");
//        }
        Client client = transportClient;
        return client;
    }

    public void simpleMerge() {
        FindIterable<Document> cursor = this.mongoDatabase.getCollection(Constants.mongoDB_collection).find();

        Iterator<Document> iterator = cursor.iterator();

        while (iterator.hasNext()) {
            Document o = iterator.next();
            String title = (String) o.get("title");
            String url = (String) o.get("url");
            String sourceText = (String) o.get("source");
            List<String> outLinks = (List<String>) o.get("outlinks");
            String cleanText = (String) o.get("text");
            String header = (String) o.get("header");

            XContentBuilder builder;
            try {
                builder = jsonBuilder()
                        .startObject()
                            .field("title", title)
                            .field("text",cleanText)
                            .field("header", header)
                            .field("html_Source", sourceText)
//                            .startArray(HttpConstant.Mongo.OUT_LINKS)
                            .array("out_links", outLinks.toArray())
//                            .endArray()
//                            .field(HttpConstant.Mongo.OUT_LINKS, response.getOutLinks())
                            .field("author", "XXXXX")
                        .endObject();
//                System.out.println(builder.prettyPrint().string());
//                System.out.println(builder.prettyPrint().string());

                GetResponse getResponse = this.elasticsearchClient.prepareGet(Constants.index_name, Constants.document_type, url)
                        .execute()
                        .actionGet();

                if (!getResponse.isExists()) {
                    IndexResponse resp = this.elasticsearchClient.prepareIndex(
                    		Constants.index_name,
                    		Constants.document_type,
                            url)
                            .setSource(builder)
                            .execute()
                            .actionGet();

                    System.out.println(url + " " + resp.isCreated());
                } else {
                	// If the response was written by me, I want to overwrite it
//                	if (getResponse.getField("author").equals("XXXXX")){
//                		IndexResponse resp = this.elasticsearchClient.prepareIndex(
//                				Constants.index_name,
//                        		Constants.document_type,
//                                url)
//                                .setSource(builder)
//                                .execute()
//                                .actionGet();
//                	}
//                	else{                		
                		System.out.println("already exists");
//                	}
                }

            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        MergeData m = new MergeData();

        m.simpleMerge();
    }
}
