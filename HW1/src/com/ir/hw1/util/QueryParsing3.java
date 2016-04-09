package com.ir.hw1.util;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.facet.statistical.StatisticalFacet;
import org.tartarus.snowball.ext.PorterStemmer;

import com.ir.hw1.Config;
import com.ir.hw1.RunQuery;
import com.ir.hw1.retrievalmodels.OkapiTF;
import com.ir.hw1.retrievalmodels.TFIDF;

public class QueryParsing3 {

	private RunQuery runQuery_ = null;
	private double avgLengthOfDoc_ = 0; //average document length for the entire corpus, aka avg(len(d)) (164.7511632301188)
	private long D_ = 0; 	// total number of documents in the corpus (84678)
	private long V_ = 0; // Vocabulary size - the total number of unique terms in the collection (178050)
	private Client client_ = null;
	PrintWriter writerOkapi_ = null;
	PrintWriter writerTFIDF_ = null;


	public static void main(String[] args) {
		String queryPath = Constants.queryPath;
		String stopListPath = Constants.stoplistPath;

		QueryParsing3 qp = new QueryParsing3();

		ArrayList<String> stopWords = qp.readStopWordFile(stopListPath);
		qp.readQueryFile(queryPath, stopWords);				
	}

	public QueryParsing3(){
		Config config = new Config(Constants.configPath);    	
		String clusterName = config.getString("cluster.name");

//		Node node = nodeBuilder().client(true).clusterName(clusterName).node();
//		Client client = node.client();
//		client_ = client;
		
		ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();        
		TransportClient transportClient = new TransportClient(settings);
        transportClient = transportClient.addTransportAddress(new InetSocketTransportAddress("localhost", 9300));
        
        if(transportClient.connectedNodes().size() == 0)
        {
            System.out.println("There are no active nodes available for the transport, it will be automatically added once nodes are live!");
        }
        Client client = transportClient;
        client_ = client;

		RunQuery runQuery = new RunQuery();
		runQuery_ = runQuery;

		long V = runQuery.getVocabularySize(client, "ap_dataset", "document", "text");
		V_ = V;

		StatisticalFacet stats = null;
		double avgLengthOfDoc = 0;
		long D = 0;

		// Get avg(len(d))
		try {
			stats = runQuery.getStatsOnTextTerms(client, "ap_dataset", "document", null, null);
			avgLengthOfDoc = stats.getMean();	// avg(len(d))
			D = stats.getCount();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		avgLengthOfDoc_ = avgLengthOfDoc;
		D_ = D;

		try {
			writerOkapi_ = new PrintWriter(Constants.okapiResultOutputPath, "UTF-8");
			writerTFIDF_ = new PrintWriter(Constants.TFIDFResultOutputPath, "UTF-8");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public void readQueryFile(String path, ArrayList<String> stopWords){

		BufferedReader br = null;

		try {
			System.out.println("Started running queries against elasticsearch ");
			System.out.println(new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));

			br = new BufferedReader(new FileReader(path));
			String line = br.readLine();

			// There are 25 queries in the queryPath, every line is a query
			// So this loop will loop 25 times to run each query
			while (line != null) {
				manipulateTerms(line, stopWords);
				line = br.readLine();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
				writerOkapi_.close();
				writerTFIDF_.close();
				System.out.println("Finished running queries against elasticsearch ");
				System.out.println(new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * Read stop words from the input path
	 */
	public ArrayList<String> readStopWordFile(String path){

		ArrayList<String> list = new ArrayList<String>();
		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(path));
			String line = br.readLine();

			while (line != null) {
				list.add(line);
				line = br.readLine();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return list;
	}


	public void manipulateTerms(String line, ArrayList<String> stopWords){
		if (line.equals(""))
			return;
		//String[] queryNoAndQuery = line.split("\\.\\s+");
		String[] queryNoAndQuery = line.split("\\.   ");
		String queryNo = queryNoAndQuery[0];
		String queryLine = queryNoAndQuery[1];

		System.out.println("Running queries " + queryNo + " against elasticsearch ");

//		String[] terms = queryLine.replaceAll("[^a-zA-Z. ]", "").split("\\s+");
		String[] terms = queryLine.replaceAll("[^a-zA-Z ]", "").split("\\s+");

		// Chop off the first three words: "Document" "will "discuss" or "Document" "will" "report" or
		// "Document" "will" "include" or "Document" "will" "identify"
		String[] chopFirstThreeWords = Arrays.copyOfRange(terms, 3, terms.length);

		// Remove stop words, for example: or, being, of, against, any will be removed
		List<String> termsWithoutStopWords = removeStopWords(chopFirstThreeWords, stopWords);

		// Get the words after applying the porter stemming algorithm, for example: allegations will become alleg
		List<String> stemmedTerms = porterStem(termsWithoutStopWords);

		// Get a score for every term in every document from the elasticsearch index
		executeQuery(queryNo, stemmedTerms);
	}

	/**
	 * 
	 * @param queryNo: is the number preceding the query in the query list
	 * @param stemmedTerms: All the terms inside query
	 */
	private void executeQuery(String queryNo, List<String> stemmedTerms) {

//		for (Models model : Models.values()) {
//			HashMap<String,Map<String, Double>> term_map = calcScoreForEveryTermEveryDocno(queryNo, stemmedTerms, model);
//
//			Map<String, Double> sortedMapDesc = calcAndSortScoreForAllDocno(term_map);
//
//			printToFile(sortedMapDesc, queryNo, model);
//		}	
		
//		HashMap<String,Map<String, Double>> term_map = calcScoreForEveryTermEveryDocno(queryNo, stemmedTerms, Models.OkapiTF);
		
		// For term w
		// for test only
//		List<String> test = new ArrayList<String>();
//		test.add("alleg");
//		test.add("measur");
//		test.add("taken");
		List<HashMap<String,Map<String, Double>>> term_docno_score = calcScoreForEveryTermEveryDocno(queryNo, stemmedTerms);
		
		List<Map<String, Double>> docno_score_lst = new ArrayList<Map<String,Double>>();
		for (HashMap<String, Map<String, Double>> term_map : term_docno_score) {
			docno_score_lst.add(calcAndSortScoreForAllDocno(term_map));
		}
		
		//for (Map<String, Double> sortedMap : docno_score_lst) {
		printToFile(docno_score_lst.get(0), queryNo, writerOkapi_);
		printToFile(docno_score_lst.get(1), queryNo, writerTFIDF_);
		//}
	}

	/**
	 * Given a queryNo and all its stemmed terms, calculate its models score
	 * @param queryNo
	 * @param stemmedTerms - All the stemmed terms in the specific queryNo
	 * @return
	 */
	private List<HashMap<String,Map<String, Double>>> calcScoreForEveryTermEveryDocno(String queryNo, List<String> stemmedTerms ) {
		
		List<HashMap<String,Map<String, Double>>> allModels = new ArrayList<HashMap<String,Map<String,Double>>>();
		// For every term w in document d		
		HashMap<String,Map<String, Double>> term_map_okapi = new HashMap<String, Map<String, Double>>();
		HashMap<String,Map<String, Double>> term_map_tfIdf = new HashMap<String, Map<String, Double>>();

		for (String term : stemmedTerms) {
//			QueryBuilder qb = QueryBuilders.matchQuery("text", term);
			QueryBuilder qb = QueryBuilders.termQuery("text", term);

			// This method has bugs
			// In order to fix it, you have to parse the JSon and get the right value
//			Map<String, Integer> docno_tf_one_term = runQuery_.queryTF(client_, qb, "ap_dataset", "document");
			Map<String, Integer> docno_tf_one_term = null;
			try {
				docno_tf_one_term = runQuery_.getTFGroovy(client_, "ap_dataset", "document", "text", term).docno_tf_map_;
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			// number of documents which contain term w
			int dfw = docno_tf_one_term.size();

			// For every docno that
			HashMap<String, Double> docno_score_okapi= new HashMap<String, Double>();
			HashMap<String, Double> docno_score_tfIdf= new HashMap<String, Double>();

			for (Map.Entry<String,Integer> entry : docno_tf_one_term.entrySet()) {
				String docno = entry.getKey();		// docno
				int tf = entry.getValue();			// tf for one term
//				double score = 0;	
				double okapi_score_one_term_one_doc = 0;
				double tfIdf_score_one_term_one_doc = 0;

				if (tf == 0){	// this is optional
					continue;
				}
				else{
					StatisticalFacet stats2 = null;
					try {
						stats2 = runQuery_.getStatsOnTextTerms(client_, "ap_dataset", "document", "docno", docno);
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					double lengOfDoc = stats2.getTotal();
					
					OkapiTF okapiTF = new OkapiTF();
					okapi_score_one_term_one_doc = okapiTF.Cal_OkapiTF(tf, lengOfDoc, avgLengthOfDoc_);
					
					TFIDF tfIdf = new TFIDF();
					tfIdf_score_one_term_one_doc = tfIdf.Cal_TFIDF(tf, lengOfDoc, avgLengthOfDoc_, D_, dfw);					
				}

				docno_score_okapi.put(docno, okapi_score_one_term_one_doc);
				docno_score_tfIdf.put(docno, tfIdf_score_one_term_one_doc);
			}

			term_map_okapi.put(term, docno_score_okapi);
			term_map_tfIdf.put(term, docno_score_tfIdf);
		}
		
		allModels.add(term_map_okapi);
		allModels.add(term_map_tfIdf);
		
		return allModels;
	}
	
	private Map<String, Double> calcAndSortScoreForAllDocno(HashMap<String,Map<String, Double>> term_map) {
		HashSet<String> docno_Set = new HashSet<String>();

		for(String term : term_map.keySet()){			
			Map<String, Double> map = term_map.get(term);			
			Set<String> keySet = map.keySet();			
			docno_Set.addAll(keySet);
		}		

		HashMap<String,Double> hashMap_doc_score = new HashMap<String, Double>();

		for(String docno : docno_Set){

			// add up all the score(tf) of this doc			
			double score = 0;

			for(String term: term_map.keySet()){
				Map<String, Double> map = term_map.get(term);
				if (map.get(docno) != null){		// Have to check null for the map that doesn't exist
					double score_for_term_docno = map.get(docno);
					score += score_for_term_docno;
				} else {
					score += 0;
				}
			}

			hashMap_doc_score.put(docno, score);			
		}

		Map<String, Double> sortedMapDesc = sortByComparator(hashMap_doc_score, false);

		//System.out.println(sortedMapDesc);
		return sortedMapDesc;
	}
	
	private void printToFile(Map<String, Double> sortedMapDesc, String queryNo, PrintWriter pw) {
//		int i = 1;
//		for (Map.Entry<String, Double> entry : sortedMapDesc.entrySet()) {
//			if (i == 1001)
//				break;
//
//				writerOkapi_.println(queryNo + " " + "Q0" + " " + entry.getKey() + " " + i + " " + entry.getValue() + " " + "Exp");	
//				writerTFIDF_.println(queryNo + " " + "Q0" + " " + entry.getKey() + " " + i + " " + entry.getValue() + " " + "Exp");
//				
//			i++;
//		}	
		
		int i = 1;
		for (Map.Entry<String, Double> entry : sortedMapDesc.entrySet()) {
			if (i == 1001)
				break;

				pw.println(queryNo + " " + "Q0" + " " + entry.getKey() + " " + i + " " + entry.getValue() + " " + "Exp");	
				
			i++;
		}	
	}

	private static Map<String, Double> sortByComparator(Map<String, Double> unsortMap, final boolean order)
	{
		List<Entry<String, Double>> list = new LinkedList<Entry<String, Double>>(unsortMap.entrySet());

		// Sorting the list based on values
		Collections.sort(list, new Comparator<Entry<String, Double>>()
				{
			public int compare(Entry<String, Double> o1,
					Entry<String, Double> o2)
			{
				if (order)
				{
					return o1.getValue().compareTo(o2.getValue());
				}
				else
				{
					return o2.getValue().compareTo(o1.getValue());

				}
			}
				});

		// Maintaining insertion order with the help of LinkedList
		Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
		for (Entry<String, Double> entry : list)
		{
			sortedMap.put(entry.getKey(), entry.getValue());
		}

		return sortedMap;
	}

	/*
	 * Returns the terms after porter stemming algorithm
	 */
	public List<String> porterStem(List<String> terms){
		List<String> stemmedTerms = new ArrayList<String>();
		for (String term : terms) {
			PorterStemmer stem = new PorterStemmer();
			stem.setCurrent(term);
			stem.stem();
			String stemmedTerm = stem.getCurrent();

			stemmedTerms.add(stemmedTerm);
		}
		return stemmedTerms;
	}

	public List<String> removeStopWords(String[] terms, ArrayList<String> stopWordLst){

		List<String> termsWithoutStopWords = new ArrayList<String>();

		for (String term : terms) {
			if (stopWordLst.contains (term))
				continue;
			termsWithoutStopWords.add(term);
		}

		return termsWithoutStopWords;		
	}
	
	public enum Models {
		OkapiTF, TFIDF//, BM25, Laplace, JelinekMercer
	}
}