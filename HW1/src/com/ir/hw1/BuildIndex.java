package com.ir.hw1;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
//import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
//import org.elasticsearch.common.settings.Settings;
//import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.node.Node;

import com.ir.hw1.util.Constants;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;


/**
 * Created by Rainicy on 1/13/15.
 */

/** 
 * Edited by Vanessa Murdock on 2/5/15:  
 * removed code for parsing the documents, 
 * added a few comments
 */

/* 
 * Depending on your setup, you may need to add two jar files to your BuildPath:
 * elasticsearch-1.4.2.jar
 * lucene-core-4.10.2.jar
 * They can be found in the lib directory of your elasticsearch installation.
 */

public class BuildIndex {
	
	//private static String readmePath_ = "";
	
    public static void main(String[] args) throws Exception {
//	if (args.length != 1) {
//            throw new IllegalArgumentException("Only Need config file.");
//        }
	/* An elegant solution is to put these specifications in a config file,
	* so that you don't have to change their values in the code every time you
	* want to run a different query set.
	* The format for the config file is one key/value pair per line where
	* the key and the value are separated by = 
	* For example,
	* input.folder=/Users/myname/cs6200/ap89_collection 
	*/

//        Config config = new Config(args[0]);
		         //Config config = new Config("./configs/hw1Index");
    	
    	String ap89_collectionPath = Constants.ap89_collectionPath;
    	System.out.println("ap89: " + ap89_collectionPath);// Shows you the path of your Project Folder

    	String stoplistPath = Constants.stoplistPath;
    	System.out.println("stoplist: " + stoplistPath);// Shows you the path of your Project Folder
    	
    	String readmePath = Constants.readmePath;
    	System.out.println("readmePath: " + readmePath);// Shows you the path of your Project Folder
    	
    	Config config = new Config(Constants.configPath);
    	//String folder = config.getString("input.folder");
    		
    	String folder = ap89_collectionPath;
        String clusterName = config.getString("cluster.name");

        //System.out.println(folder);
        System.out.println(clusterName);
    	

        // on startup
	//        Settings settings = ImmutableSettings.settingsBuilder()
	//                .put("cluster.name", "elasticsearch").build();
	//        Client client = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress("localhost", 9300));

        
        // It is very slow to start a client through node, so we start it from
        // TransportClient
        //        Node node = nodeBuilder().client(true).clusterName(clusterName).node();
////    	Node node = nodeBuilder().client(true).node();
//        Client client = node.client();
        
        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();        
		TransportClient transportClient = new TransportClient(settings);
		transportClient = transportClient.addTransportAddress(new InetSocketTransportAddress("localhost", 9300));

		if(transportClient.connectedNodes().size() == 0)
		{
			System.out.println("There are no active nodes available for the transport, it will be automatically added once nodes are live!");
		}
		Client client = transportClient;

        // setting
        // 1) set settings and analysis
        XContentBuilder settingsBuilder = getSettingsBuilder();
        client.admin().indices().prepareCreate("ap_dataset")
	    .setSettings(ImmutableSettings.settingsBuilder().loadFromSource(settingsBuilder.string()))
	    .execute()
	    .actionGet();
        // 2) set mapping
        XContentBuilder mappingBuilder = getMappingBuilder();
        client.admin().indices().preparePutMapping("ap_dataset")
	    .setType("document")
	    .setSource(mappingBuilder)
	    .execute()
	    .actionGet();

        // 3) index files to documents
	// IMPLEMENT YOUR OWN METHOD TO ITERATE THROUGH A DIRECTORY AND GET ALL THE FILES
        List<File> files = Parser.getFiles(folder, Constants.readmePath);
        // index, starting from 0
        int id = 0;
        for (File file : files) {
	    
	    // IMPLEMENT YOUR OWN METHOD THAT PARSES THE FILE AND RETURNS A LIST OF JSON DOCUMENTS
            List<XContentBuilder> builders = getBuilders(file);
	    // ITERATE THROUGH THE LIST OF DOCUMENTS AND INDEX EACH ONE
            for (XContentBuilder builder : builders) {
                System.out.println("ID: " + id);
                IndexResponse response = client.prepareIndex("ap_dataset", "document", ""+id)
		    .setSource(builder)
		    .execute()
		    .actionGet();
                ++id;
            }
        }
        //node.close();
        transportClient.close();
        client.close();
    }

    /**
     * Read one line at a time to parse the corpus
     * @param file - every file in the ap89_collection
     * @return
     */
    private static List<XContentBuilder> getBuilders(File file) {
		
    	List<XContentBuilder> lstBuilder = new ArrayList<XContentBuilder>();
    	String line = null;
		StringBuilder sb;
		try {
			BufferedReader br = null;
			br = new BufferedReader(new FileReader(file));
			sb = new StringBuilder();
			line = br.readLine();
			Boolean readingText = false;
			String docNo = null;
			String text;

			while (line != null) {
				if (line.startsWith("<DOC>"))
					sb = new StringBuilder();
				else if (line.startsWith("<DOCNO>"))
					docNo = line.split(" ")[1];
				else if (line.startsWith("<TEXT>"))
					readingText = true;
				else if (line.startsWith("</TEXT>"))
					readingText = false;
				else if (line.startsWith("</DOC>")){
					text = sb.toString();
			    	XContentBuilder builder = createBuilder(docNo, text);
			    	lstBuilder.add(builder);
				}
				else{
					if (readingText){
						//String result = line.replaceAll("[-+.^:,!?'~@#$%&*_=<>\"]","");
						//sb.append(result);
						
						sb.append(line);
						sb.append(System.lineSeparator());
					}
				}
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}	
		return lstBuilder;
	}
    
    private static XContentBuilder createBuilder(String docNo, String text){
    	XContentBuilder builder = null;
		try {
			builder = XContentFactory.jsonBuilder()
					.startObject()
						.field("docno", docNo)
						.field("text", text)
					.endObject();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	return builder;
    }

	private static XContentBuilder getMappingBuilder() throws IOException {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject()
	    .startObject("document")
	    .startObject("properties")
	    .startObject("docno")
	    .field("type", "string")
	    .field("store", true)
	    .field("index", "not_analyzed")
	    .endObject()
	    .startObject("text")
	    .field("type", "string")
	    .field("store", true)
	    .field("index", "analyzed")
	    .field("term_vector", "with_positions_offsets_payloads")
	    .field("analyzer", "my_english")
	    .endObject()
	    .endObject()
	    .endObject()
	    .endObject();
        return builder;
    }

    private static XContentBuilder getSettingsBuilder() throws IOException {  	
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
}
