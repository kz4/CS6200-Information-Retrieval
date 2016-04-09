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

import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse.AnalyzeToken;
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
import com.ir.hw1.retrievalmodels.BM25;
import com.ir.hw1.retrievalmodels.JelinekMercer;
import com.ir.hw1.retrievalmodels.LaplaceSmoothing;
import com.ir.hw1.retrievalmodels.OkapiTF;
import com.ir.hw1.retrievalmodels.TFIDF;

public class QueryParsing4 {

	private RunQuery runQuery_ = null;
	// average document length for the entire corpus, aka avg(len(d)) - 164.7511632301188
	private double avgLengthOfDoc_ = 0; 

	// total number of documents in the corpus - 84678
	private long D_ = 0;
	
	// Vocabulary size - the total number of unique terms - 178050
	private long V_ = 0;
	private Map<String, Double> docno_lenOfDoc_ = null;
	private Map<String, TfTotalTfTotalLenOfDoc> docno_TfTotalTfTotalLenOfDoc_;

	private Client client_ = null;
	PrintWriter writerOkapi_ = null;
	PrintWriter writerTFIDF_ = null;
	PrintWriter writerBM25_ = null;
	PrintWriter writerLaplace_ = null;
	PrintWriter writerJMercer_ = null;
	PrintWriter writerMetasearch_ = null;
	
	private static List<String> stopWordLst_ = null;

	public static void main(String[] args) {
		String queryPath = Constants.queryPath;

		QueryParsing4 qp = new QueryParsing4();

		stopWordLst_ = qp.readStopWordFile(Constants.stoplistPath);
		
		qp.readQueryFile(queryPath);		
	}

	/**
	 * Initialize all the instance variables
	 */
	public QueryParsing4() {
		Config config = new Config(Constants.configPath);
		String clusterName = config.getString("cluster.name");

		// Node node =
		// nodeBuilder().client(true).clusterName(clusterName).node();
		// Client client = node.client();
		// client_ = client;

		ImmutableSettings.Builder settings = ImmutableSettings
				.settingsBuilder();
		TransportClient transportClient = new TransportClient(settings);
		transportClient = transportClient
				.addTransportAddress(new InetSocketTransportAddress("localhost", 9300));

		if (transportClient.connectedNodes().size() == 0) {
			System.out.println("There are no active nodes available for the transport, it will be automatically added once nodes are live!");
		}
		Client client = transportClient;
		client_ = client;

		RunQuery runQuery = new RunQuery();
		runQuery_ = runQuery;

		long V = runQuery.getVocabularySize(client, "ap_dataset", "document",
				"text");
		V_ = V;

		StatisticalFacet stats = null;
		double avgLengthOfDoc = 0;
		long D = 0;

		try {
			stats = runQuery.getStatsOnTextTerms(client, "ap_dataset",
					"document", null, null);
			// avg(len(d))
			avgLengthOfDoc = stats.getMean();
			// D is the total number of documents in the corpus - 84678
			D = stats.getCount();
		} catch (IOException e) {
			e.printStackTrace();
		}
		avgLengthOfDoc_ = avgLengthOfDoc;
		D_ = D;

		try {
			writerOkapi_ = new PrintWriter(Constants.okapiResultOutputPath,
					"UTF-8");
			writerTFIDF_ = new PrintWriter(Constants.TFIDFResultOutputPath,
					"UTF-8");
			writerBM25_ = new PrintWriter(Constants.BM25ResultOutputPath,
					"UTF-8");
			writerLaplace_ = new PrintWriter(Constants.LaplaceResultOutputPath,
					"UTF-8");
			writerJMercer_ = new PrintWriter(Constants.MercerResultOutputPath,
					"UTF-8");
			writerMetasearch_ = new PrintWriter(Constants.MetaResultOutputPath,
					"UTF-8");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		docno_lenOfDoc_ = new HashMap<String, Double>();
		docno_TfTotalTfTotalLenOfDoc_ = new HashMap<String, TfTotalTfTotalLenOfDoc>();
	}

	/**
	 * Read the query file, one line at a time
	 * @param path - path to the query file
	 * @param stopWords - list of stop words
	 */
	public void readQueryFile(String path) {

		BufferedReader br = null;

		try {
			System.out.println("Started running queries against elasticsearch ");
			System.out.println(new SimpleDateFormat("yyyyMMdd_HHmmss")
					.format(Calendar.getInstance().getTime()));

			br = new BufferedReader(new FileReader(path));
			String line = br.readLine();

			// There are 25 queries in the queryPath, every line is a query
			// So this loop will loop 25 times to run each query
			while (line != null) {
				manipulateTerms(line);
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
				writerBM25_.close();
				writerLaplace_.close();
				writerJMercer_.close();
				writerMetasearch_.close();
				System.out.println("Finished running queries against elasticsearch ");
				System.out.println(new SimpleDateFormat("yyyyMMdd_HHmmss")
						.format(Calendar.getInstance().getTime()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void manipulateTerms(String line) {
		if (line.equals(""))
			return;

		String[] queryNoAndQuery = line.split("\\.   ");
		String queryNo = queryNoAndQuery[0];
		String queryLine = queryNoAndQuery[1];

		System.out.println("Running queries " + queryNo
				+ " against elasticsearch ");

		// METHOD II: Much better performance!
		List<String> stemmedTerms = analyzeResponseStem(queryLine);

		// METHOD I: Worse performance
		// Example: Elasticsearch stems incursions to incur while PorterStem stems it to incurs
		//List<String> stemmedTerms = porterStemAlgorithm(queryLine);		
				
		// Get a score for every term in every document from the elasticsearch
		// index
		executeQuery(queryNo, stemmedTerms);
	}

	/**
	 * 
	 * @param queryNo - is the number preceding the query in the query list
	 * @param stemmedTerms - all the terms inside query
	 */
	private void executeQuery(String queryNo, List<String> stemmedTerms) {

		// 5 models list
		List<HashMap<String, Map<String, Double>>> term_docno_score = calcScoreForEveryTermEveryDocno(queryNo, stemmedTerms);

		List<Map<String, Double>> docno_score_lst = new ArrayList<Map<String, Double>>();
		int count = 0;
		for (HashMap<String, Map<String, Double>> term_map : term_docno_score) {
			docno_score_lst.add(calcAndSortScoreForAllDocno(term_map, count));
			count++;
		}

		printToFile(docno_score_lst.get(0), queryNo, writerOkapi_);
		printToFile(docno_score_lst.get(1), queryNo, writerTFIDF_);
		printToFile(docno_score_lst.get(2), queryNo, writerBM25_);
		printToFile(docno_score_lst.get(3), queryNo, writerLaplace_);
		printToFile(docno_score_lst.get(4), queryNo, writerJMercer_);
		
		Map<String, Double> metasearch_map = borda_fuse(docno_score_lst);
		
		printToFile(metasearch_map, queryNo, writerMetasearch_);
	}

	/**
	 * Given a queryNo and all its stemmed terms, calculate its models score
	 * @param queryNo - the number before period 
	 * @param stemmedTerms - all the stemmed terms in the specific queryNo
	 * @return - a list of all models' results
	 */
	private List<HashMap<String, Map<String, Double>>> calcScoreForEveryTermEveryDocno(String queryNo, List<String> stemmedTerms) {

		List<HashMap<String, Map<String, Double>>> allModels = new ArrayList<HashMap<String, Map<String, Double>>>();
		// For every term w in document d
		HashMap<String, Map<String, Double>> term_map_okapi = new HashMap<String, Map<String, Double>>();
		HashMap<String, Map<String, Double>> term_map_tfIdf = new HashMap<String, Map<String, Double>>();
		HashMap<String, Map<String, Double>> term_map_bm25 = new HashMap<String, Map<String, Double>>();
		HashMap<String, Map<String, Double>> term_map_laplace = new HashMap<String, Map<String, Double>>();
		HashMap<String, Map<String, Double>> term_map_jmercer = new HashMap<String, Map<String, Double>>();

		// Since there are repeated terms in the 25 queries, so if we calculate one term
		// for its score, we don't want to calculate it again. This is why we want to
		// keep a set of terms so we know if they appear again or not
		HashSet<String> seenTerms = new HashSet<String>();

		// For BM25 tf_wq - is the term frequency of term w in query q
		Map<String, Integer> tf_wq_map = new HashMap<String, Integer>();
		for (String term : stemmedTerms) {
			int count = 0;
			for (String termAgain : stemmedTerms) {
				if (termAgain.equals(term))
					count++;
			}
			tf_wq_map.put(term, count);
		}

		for (String term : stemmedTerms) {
			
			System.out.println(term);

			QueryBuilder qb = QueryBuilders.termQuery("text", term);

			Map<String, Integer> docno_tf_one_term = null;

			// For every docno that
			Map<String, Double> docno_score_okapi = new HashMap<String, Double>();
			Map<String, Double> docno_score_tfIdf = new HashMap<String, Double>();
			Map<String, Double> docno_score_bm25 = new HashMap<String, Double>();
			Map<String, Double> docno_score_laplace = new HashMap<String, Double>();
			Map<String, Double> docno_score_jmercer = new HashMap<String, Double>();

			// If we have calculated the repeated words before, then we get just get the result
			if (seenTerms.contains(term)) {
				docno_score_okapi = term_map_okapi.get(term);
				docno_score_tfIdf = term_map_tfIdf.get(term);
				docno_score_bm25 = term_map_bm25.get(term);
				docno_score_laplace = term_map_laplace.get(term);
				docno_score_jmercer = term_map_jmercer.get(term);
			} else {

				docno_tf_one_term = null;

				// Jelinek-Mercer Smoothing
				double totalTf = 0;
				double totalLenOfDoc = 0;

				try {
					// tf_wd
					TfTotalTfTotalLenOfDoc t = runQuery_.getTFGroovy(client_,
							"ap_dataset", "document", "text", term);
					docno_tf_one_term = t.docno_tf_map_;
					totalTf = t.totalTf_;
					totalLenOfDoc = t.totalLenOfDoc_;
					docno_TfTotalTfTotalLenOfDoc_.put(term, t);
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				// number of documents which contain term w
				int dfw = 0;
				try {
					dfw = docno_tf_one_term.size();
				} catch (Exception e1) {
					System.out.println("Term: " + term);
					e1.printStackTrace();
				}

				for (Map.Entry<String, Integer> entry : docno_tf_one_term
						.entrySet()) {
					String docno = entry.getKey(); // docno
					int tf = entry.getValue(); // tf for one term
					// double score = 0;
					double okapi_score_one_term_one_doc = 0;
					double tfIdf_score_one_term_one_doc = 0;
					double bm25_score_one_term_one_doc = 0;
					double laplace_score_one_term_one_doc = 0;
					double jmercer_score_one_term_one_doc = 0;

					if (tf == 0) {
						// In theory, this case shouldn't happen because
						// when you query against the Elasticsearch, for example
						// for term "alleg", you will get 6252 results back,
						// all of term should have at least tf_wd >= 1
					} else {
						double lengOfDoc = 0;
						if (docno_lenOfDoc_.containsKey(docno))
							lengOfDoc = docno_lenOfDoc_.get(docno);
						else {

							StatisticalFacet stats2 = null;
							try {
								stats2 = runQuery_.getStatsOnTextTerms(client_,
										"ap_dataset", "document", "docno",
										docno);
							} catch (IOException e) {
								e.printStackTrace();
							}
							lengOfDoc = stats2.getTotal();
							docno_lenOfDoc_.put(docno, lengOfDoc);
						}

						OkapiTF okapiTF = new OkapiTF();
						okapi_score_one_term_one_doc = okapiTF.Cal_OkapiTF(tf, lengOfDoc, avgLengthOfDoc_);

						TFIDF tfIdf = new TFIDF();
						tfIdf_score_one_term_one_doc = tfIdf.Cal_TFIDF(tf, lengOfDoc, avgLengthOfDoc_, D_, dfw);

						BM25 bm25 = new BM25();
						bm25_score_one_term_one_doc = bm25.Cal_BM25(tf, lengOfDoc, avgLengthOfDoc_, D_, dfw, tf_wq_map.get(term), Constants.k1, Constants.k2, Constants.b);

						LaplaceSmoothing laplace = new LaplaceSmoothing();
						laplace_score_one_term_one_doc = laplace.Cal_Laplace(tf, lengOfDoc, V_);

						JelinekMercer jk = new JelinekMercer();
						jmercer_score_one_term_one_doc = jk.Cal_JelinekMercer(tf, lengOfDoc, (totalTf - tf), (totalLenOfDoc - lengOfDoc), Constants.lambda);
					}

					docno_score_okapi.put(docno, okapi_score_one_term_one_doc);
					docno_score_tfIdf.put(docno, tfIdf_score_one_term_one_doc);
					docno_score_bm25.put(docno, bm25_score_one_term_one_doc);
					docno_score_laplace.put(docno, laplace_score_one_term_one_doc);
					docno_score_jmercer.put(docno, jmercer_score_one_term_one_doc);
				}
			}

			term_map_okapi.put(term, docno_score_okapi);
			term_map_tfIdf.put(term, docno_score_tfIdf);
			term_map_bm25.put(term, docno_score_bm25);
			term_map_laplace.put(term, docno_score_laplace);
			term_map_jmercer.put(term, docno_score_jmercer);

			seenTerms.add(term);
		}

		// A list of models
		allModels.add(term_map_okapi);
		allModels.add(term_map_tfIdf);
		allModels.add(term_map_bm25);
		allModels.add(term_map_laplace);
		allModels.add(term_map_jmercer);

		return allModels;
	}

	/**
	 * First create a set to get a unique collection of docno, then iterate through
	 * every docno and every term to sum up the scores to get the matching score
	 * @param term_map - Map<term, Map<docno, score>>
	 * @param model - all the vector space and language models
	 * @return - a map of docno and a matching score
	 */
	private Map<String, Double> calcAndSortScoreForAllDocno(HashMap<String, Map<String, Double>> term_map, int model) {
		
		// Create a set so that, we can get a unique collection of docno
		HashSet<String> docno_Set = new HashSet<String>();

		for (String term : term_map.keySet()) {
			Map<String, Double> map = term_map.get(term);
			Set<String> keySet = map.keySet();
			docno_Set.addAll(keySet);
		}

		HashMap<String, Double> hashMap_doc_score = new HashMap<String, Double>();

		for (String docno : docno_Set) {

			// add up all the score of this doc
			double score = 0;

			for (String term : term_map.keySet()) {
				
				// base is the case when tf_wd = 0, we only need to worry
				// for model = 3 (Laplace Smoothing) and model = 4 (Mercer Smoothing)
				// because for other cases score = 0 for a word in a query when tf_wd = 0
				double base = 0;
				if (model == 3){
					base = Math.log10(1.0 / (docno_lenOfDoc_.get(docno) + V_)); 
				}else if(model == 4){
					base = Math.log10((1 - Constants.lambda) * (docno_TfTotalTfTotalLenOfDoc_.get(term).totalTf_)
							/ (docno_TfTotalTfTotalLenOfDoc_.get(term).totalLenOfDoc_ - docno_lenOfDoc_.get(docno)));  
				}
				
				Map<String, Double> docno_score = term_map.get(term);
				
				// For a word, when it's docno matches a tf_wd, we will get the tf_wd
				// and add it to the score
				// But make sure the check null first to avoid null pointer exception
				if (docno_score.get(docno) != null) {
					double score_for_term_docno = docno_score.get(docno);					
					score += score_for_term_docno;
				} else {
					// For model = 0, 1, 2 base = 0
					score += base;
				}
			}

			hashMap_doc_score.put(docno, score);
		}

		Map<String, Double> sortedMapDesc = sortByComparator(hashMap_doc_score, false);

		// System.out.println(sortedMapDesc);
		return sortedMapDesc;
	}

	private void printToFile(Map<String, Double> sortedMapDesc, String queryNo, PrintWriter pw) {
		int i = 1;
		for (Map.Entry<String, Double> entry : sortedMapDesc.entrySet()) {
			if (i == 1001)
				break;

			pw.println(queryNo + " " + "Q0" + " " + entry.getKey() + " " + i
					+ " " + entry.getValue() + " " + "Exp");

			i++;
		}
	}

	private static Map<String, Double> sortByComparator(
			Map<String, Double> unsortMap, final boolean order) {
		List<Entry<String, Double>> list = new LinkedList<Entry<String, Double>>(
				unsortMap.entrySet());

		// Sorting the list based on values
		Collections.sort(list, new Comparator<Entry<String, Double>>() {
			public int compare(Entry<String, Double> o1,
					Entry<String, Double> o2) {
				if (order) {
					return o1.getValue().compareTo(o2.getValue());
				} else {
					return o2.getValue().compareTo(o1.getValue());

				}
			}
		});

		// Maintaining insertion order with the help of LinkedList
		Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
		for (Entry<String, Double> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}

		return sortedMap;
	}
	
	/**
	 * Combine all 5 models using a metasearch algorithm for ranking fusion.
	 * Borda Count is algorithm is used to here to give a weight from 1000~1 for
	 * the doc that ranks from 1~1000 
	 * @param docno_score_lst - a list of 5 sorted docno_matching score map
	 * @return - a map of docno and matching score with
	 */
	private Map<String, Double> borda_fuse(List<Map<String, Double>> docno_score_lst) {
		
		List<Map<String, Integer>> lst_map_docno_weight = new ArrayList<Map<String,Integer>>();

		for (Map<String, Double> map_docno_score : docno_score_lst) {
			int i = 1000;
			Map<String, Integer> weighted_map_docno_score = new HashMap<String, Integer>();
			for (String docno : map_docno_score.keySet()) {
				weighted_map_docno_score.put(docno, i);
				i--;
				if (i == 0)
					break;
			}
			lst_map_docno_weight.add(weighted_map_docno_score);
		}
		
		Set<String> all_docno = new HashSet<String>();
		
		// Get a unique set of docno
		for (Map<String, Double> map_docno_matchingscore : docno_score_lst) {
			all_docno.addAll(map_docno_matchingscore.keySet());
		}
		
		Map<String, Double> new_docno_matching_score = new HashMap<String, Double>();
		
		for (String docno : all_docno) {
			double score = 0;
			for (Map<String, Integer> map_docno_matchingscore : lst_map_docno_weight) {
				if (map_docno_matchingscore.get(docno) != null){
					double score_for_term_docno = map_docno_matchingscore.get(docno);					
					score += score_for_term_docno;
				} else {
					score += 0;
				}					
			}
			new_docno_matching_score.put(docno, score);
		}
		
		Map<String, Double> sortedMapDesc = sortByComparator(new_docno_matching_score, false);
		
		return sortedMapDesc;
	}

	/**
	 * Function that uses AnalyzeResponse (Elasticsearch API to stem the words)
	 * @param line - in our case, a query
	 * @return - list of stemmed words
	 */
	public List<String> analyzeResponseStem(String line) {

		List<String> stemmedLst = new ArrayList<String>();
		List<String> irrelevantLst = getIrrelevantLst();

		AnalyzeResponse analyzeResponse = client_.admin().indices()
				.prepareAnalyze("ap_dataset", line).setAnalyzer("my_english")
				.execute().actionGet();

		List<AnalyzeToken> tokens = analyzeResponse.getTokens();

		for (AnalyzeToken token : tokens) {

			String term = token.getTerm();
			if (!irrelevantLst.contains(term))
				stemmedLst.add(term);
		}
		return stemmedLst;
	}
	
	private List<String> getIrrelevantLst(){
		List<String> irrelevantLst = new ArrayList<String>();
		irrelevantLst.add("document");
		irrelevantLst.add("report");
		irrelevantLst.add("include");
		irrelevantLst.add("describe");
		irrelevantLst.add("describ");
		irrelevantLst.add("identifi");
		irrelevantLst.add("predict");
		irrelevantLst.add("cite");
		irrelevantLst.add("discuss");
		irrelevantLst.add("directli");
		irrelevantLst.add("take");
		irrelevantLst.add("describ");
		irrelevantLst.add("sign");
		irrelevantLst.add("current");
		irrelevantLst.add("actual");
		irrelevantLst.add("public");
		irrelevantLst.add("type");
		irrelevantLst.add("event");
		irrelevantLst.add("result");
		irrelevantLst.add("make");
		irrelevantLst.add("effort");
		
		return irrelevantLst;
	}

	/*
	 * Returns the terms after porter stemming algorithm
	 * Didn't use it because PorterStem stems incursions to incurs while ElasticSearch stems it to incur
	 */
	public List<String> porterStem(List<String> terms) {
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

	public List<String> removeStopWords(String[] terms, List<String> stopWordLst) {

		List<String> termsWithoutStopWords = new ArrayList<String>();

		for (String term : terms) {
			if (stopWordLst.contains(term))
				continue;
			termsWithoutStopWords.add(term);
		}

		return termsWithoutStopWords;
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
	
	private List<String> removeIrrelevant(List<String> porterStemmedTerms) {
		
		List<String> finalTerms = new ArrayList<String>();
		List<String> irrelevantLst = getIrrelevantLst();
		
		for (String term : porterStemmedTerms) {
			if (irrelevantLst.contains(term))
				continue;
			finalTerms.add(term);
		}
		
		return finalTerms;
	}
	
	private List<String> porterStemAlgorithm(String queryLine){
		// Split the line into words by letters
		String[] terms = queryLine.replaceAll("[^a-zA-Z ]", "").split("\\s+");
				
		// Chop off the first three words: "Document" "will "discuss" or "Document" "will" "report" or				
		// "Document" "will" "include" or "Document" "will" "identify"
		String[] chopFirstThreeWords = Arrays.copyOfRange(terms, 3, terms.length);

		// Remove stop words, for example: or, being, of, against, any will be removed
		List<String> termsWithoutStopWords = removeStopWords(chopFirstThreeWords, stopWordLst_);

		// Get the words after applying the porter stemming algorithm, for example: allegations will become alleg
		List<String> porterStemmedTerms = porterStem(termsWithoutStopWords);
		
		// Manually remove the irrelevant words
		List<String> stemmedTerms = removeIrrelevant(porterStemmedTerms);
		
		return stemmedTerms;
	}

	// public enum Models {
	// OkapiTF, TFIDF//, BM25, Laplace, JelinekMercer
	// }
}