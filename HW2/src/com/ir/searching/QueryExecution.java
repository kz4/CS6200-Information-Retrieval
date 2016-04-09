package com.ir.searching;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tartarus.snowball.ext.PorterStemmer;

import com.ir.retrievalmodels.BM25;
import com.ir.retrievalmodels.CalcMinWindow;
import com.ir.retrievalmodels.LaplaceSmoothing;
import com.ir.retrievalmodels.OkapiTF;
import com.ir.retrievalmodels.Proximity;
import com.ir.tokenizer.Indexer.indexes_model;
import com.ir.uti.Constants;

public class QueryExecution {

	private RunQuery runQuery_ = null;
	// average document length for the entire corpus, aka avg(len(d)) - 164.7511632301188
	private double avgLengthOfDoc_ = 0; 

	// total number of documents in the corpus - 84678
	private long D_ = 0;

	// Vocabulary size - the total number of unique terms - 178050
	private long V_ = 0;
	private Map<Integer, Integer> docId_lenOfDoc_ = null;
	//	private Map<String, TfTotalTfTotalLenOfDoc> docno_TfTotalTfTotalLenOfDoc_;

	private Map<Integer, String> docId_docno_ = new HashMap<Integer, String>();

	PrintWriter writerOkapi_ = null;
	//	PrintWriter writerTFIDF_ = null;
	PrintWriter writerBM25_ = null;
	PrintWriter writerLaplace_ = null;
	//	PrintWriter writerJMercer_ = null;
	//	PrintWriter writerMetasearch_ = null;
	PrintWriter writerProximity_ = null;

	private static List<String> stopWordLst_ = null;

	public static void main(String[] args) {
		String queryPath = Constants.queryPath;

		QueryExecution qp = new QueryExecution();

		stopWordLst_ = qp.readStopWordFile(Constants.stoplistPath);

		qp.read_docId_docno(Constants.docId_docno_Path);

		qp.readQueryFile(queryPath);		
	}

	/**
	 * Initialize all the instance variables
	 */
	public QueryExecution() {

		RunQuery runQuery = new RunQuery();
		runQuery_ = runQuery;

		V_ = runQuery_.getVocabularySize();
		avgLengthOfDoc_ = runQuery_.getAvgLengthOfDoc();
		D_ = runQuery_.getTotalNumOfDoc();

		try {
			writerOkapi_ = new PrintWriter(Constants.okapiResultOutputPath,
					"UTF-8");
			//			writerTFIDF_ = new PrintWriter(Constants.TFIDFResultOutputPath,
			//					"UTF-8");
			writerBM25_ = new PrintWriter(Constants.BM25ResultOutputPath,
					"UTF-8");
			writerLaplace_ = new PrintWriter(Constants.LaplaceResultOutputPath,
					"UTF-8");
			writerProximity_ = new PrintWriter(Constants.ProximityResultOutputPath,
					"UTF-8");
			//			writerJMercer_ = new PrintWriter(Constants.MercerResultOutputPath,
			//					"UTF-8");
			//			writerMetasearch_ = new PrintWriter(Constants.MetaResultOutputPath,
			//					"UTF-8");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		docId_lenOfDoc_ = runQuery_.getDocId_lenOfDoc();
		//		docno_TfTotalTfTotalLenOfDoc_ = new HashMap<String, TfTotalTfTotalLenOfDoc>();
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
				manipulateQueryTerms(line);
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
				//				writerTFIDF_.close();
				writerBM25_.close();
				writerLaplace_.close();
				//				writerJMercer_.close();
				//				writerMetasearch_.close();
				writerProximity_.close();
				System.out.println("Finished running queries against self made indexer ");
				System.out.println(new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(Calendar.getInstance().getTime()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void manipulateQueryTerms(String line) throws IOException {
		if (line.equals(""))
			return;

		String[] queryNoAndQuery = line.split("\\.   ");
		String queryNo = queryNoAndQuery[0];
		String queryLine = queryNoAndQuery[1];

		System.out.println("Running queries " + queryNo + " against self made indexer.");

		indexes_model model = Constants.indexModel;
		List<String> finalQueryTerms = null;

		switch (model) {
		case original:
			finalQueryTerms = noStem_withStopwords(queryLine);
			break;
		case no_stopwords:
			finalQueryTerms = noStem_withoutStopwords(queryLine);
			break;
		case stemmed:
			finalQueryTerms = porterstem_withStopwords(queryLine);
			break;
		case no_stopwords_stemmed:
			finalQueryTerms = porterStem_removeStopwords(queryLine);
			break;				
		default:
			break;
		}

		// Get a score for every term in every document from the index
		executeQuery(queryNo, finalQueryTerms);
	}

	/**
	 * 
	 * @param queryNo - is the number preceding the query in the query list
	 * @param stemmedTerms - all the terms inside one query
	 * @throws IOException 
	 */
	private void executeQuery(String queryNo, List<String> stemmedTerms) throws IOException {

		// 5 models list
		List<HashMap<String, Map<Integer, Double>>> term_docId_score = calcScoreForEveryTermEveryDocId(queryNo, stemmedTerms);	// need to uncomment
		//		List<HashMap<String, Map<Integer, Double>>> term_docId_score = null;
		Map<String, Double> docId_score = calcProximitySearchScoreForEveryDocId(queryNo, stemmedTerms);
		// to do, convert docId_score to docno_score
		// sort according to score
		List<Map<String, Double>> docId_score_lst = new ArrayList<Map<String, Double>>();

		Map<String, Double> sortedProximityMapDesc = sortByComparator(docId_score, false);
		int count = 0;
		for (HashMap<String, Map<Integer, Double>> term_map : term_docId_score) {
			docId_score_lst.add(calcAndSortScoreForAllDocId(term_map, count));
			count++;
		}

		//		printToFile(docId_score_lst.get(0), queryNo, writerOkapi_);
		//		printToFile(docno_score_lst.get(1), queryNo, writerTFIDF_);
		//		printToFile(docId_score_lst.get(2), queryNo, writerBM25_);
		//		printToFile(docId_score_lst.get(3), queryNo, writerLaplace_);
		//		printToFile(docno_score_lst.get(4), queryNo, writerJMercer_);
		//		printToFile(metasearch_map, queryNo, writerMetasearch_);

		printToFile(docId_score_lst.get(0), queryNo, writerOkapi_);
		//		printToFile(docno_score_lst.get(1), queryNo, writerTFIDF_);
		printToFile(docId_score_lst.get(1), queryNo, writerBM25_);
		printToFile(docId_score_lst.get(2), queryNo, writerLaplace_);
		//		printToFile(docno_score_lst.get(4), queryNo, writerJMercer_);

		//Map<String, Double> metasearch_map = borda_fuse(docno_score_lst);

		//		printToFile(metasearch_map, queryNo, writerMetasearch_);
		printToFile(sortedProximityMapDesc, queryNo, writerProximity_);
	}

	private Map<String, Double> calcProximitySearchScoreForEveryDocId(String queryNo, List<String> stemmedTerms) throws IOException {

		Map<String, Map<Integer, List<Integer>>> term_docId_lstOfPosn = new HashMap<String, Map<Integer,List<Integer>>>();

		for (String term : stemmedTerms) {
			term_docId_lstOfPosn = runQuery_.getDocId_lstOfPosn_one_term(term, term_docId_lstOfPosn);
		}
		Set<Integer> docId_set = new HashSet<Integer>();
		for (String term : term_docId_lstOfPosn.keySet()) {
			for (int docId : term_docId_lstOfPosn.get(term).keySet()) {
				docId_set.add(docId);
			}
		}

		// Convert Map<term, Map<docId, List<Posn>>> to Map<docId, Map<term, List<Posn>>>
		Map<Integer, Map<String, List<Integer>>> docId_term_lstOfPosn = new HashMap<Integer, Map<String,List<Integer>>>();
		for (int docId : docId_set) {
			Map<String, List<Integer>> term_lstOfPosn = new HashMap<String, List<Integer>>();
			for (String term : term_docId_lstOfPosn.keySet()) {
				List<Integer> lstOfPosn = term_docId_lstOfPosn.get(term).get(docId);
				if (lstOfPosn != null && lstOfPosn.size() > 0)
					term_lstOfPosn.put(term, lstOfPosn);
			}
			docId_term_lstOfPosn.put(docId, term_lstOfPosn);
		}

		Map<Integer, Double> docId_proximityscore = new HashMap<Integer, Double>();
		Map<String, Double> docno_proximityscore = new HashMap<String, Double>();
		for (int docId : docId_term_lstOfPosn.keySet()) {
			// If there is only 1 query term appears in a document
			// then the score would be 0 because proximity search
			// requires at least 2 query terms
			// If there is 0 query term, that means the code has bugs
			if (docId_term_lstOfPosn.get(docId).size() == 0){
				System.out.println("There are no query term in this document, code has bugs");				
			}
			if (docId_term_lstOfPosn.get(docId).size() == 1){
				// System.out.println("There are 1 query term in this document, docId: " + docId + " we ignore this document");
				continue;				
			}

			int numOfContainTerms = docId_term_lstOfPosn.get(docId).size();
			CalcMinWindow c = new CalcMinWindow();
			int minRangeOfWindow = -1;
			if (docId_term_lstOfPosn.containsKey(docId))
				minRangeOfWindow = c.merge(docId_term_lstOfPosn.get(docId));

			Proximity p = new Proximity();
			double score = p.Cal_Proxmity(Constants.proximityConstant, minRangeOfWindow, numOfContainTerms, docId_lenOfDoc_.get(docId), V_);
			docId_proximityscore.put(docId, score);
			docno_proximityscore.put(docId_docno_.get(docId), score);
		}

		return docno_proximityscore;
	}

	/**
	 * Given a queryNo and all its stemmed terms, calculate its models score
	 * @param queryNo - the number before period 
	 * @param stemmedTerms - all the stemmed terms in the specific queryNo
	 * @return - a list of all models' results
	 */
	private List<HashMap<String, Map<Integer, Double>>> calcScoreForEveryTermEveryDocId(String queryNo, List<String> stemmedTerms) throws IOException {

		List<HashMap<String, Map<Integer, Double>>> allModels = new ArrayList<HashMap<String, Map<Integer, Double>>>();
		// For every term w in document d
		HashMap<String, Map<Integer, Double>> term_map_okapi = new HashMap<String, Map<Integer, Double>>();
		//		HashMap<String, Map<Integer, Double>> term_map_tfIdf = new HashMap<String, Map<Integer, Double>>();
		HashMap<String, Map<Integer, Double>> term_map_bm25 = new HashMap<String, Map<Integer, Double>>();
		HashMap<String, Map<Integer, Double>> term_map_laplace = new HashMap<String, Map<Integer, Double>>();
		//		HashMap<String, Map<Integer, Double>> term_map_jmercer = new HashMap<String, Map<Integer, Double>>();

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

			Map<Integer, Integer> docId_tf_one_term = null;

			// For every docId that
			Map<Integer, Double> docId_score_okapi = new HashMap<Integer, Double>();
			//			Map<Integer, Double> docId_score_tfIdf = new HashMap<Integer, Double>();
			Map<Integer, Double> docId_score_bm25 = new HashMap<Integer, Double>();
			Map<Integer, Double> docId_score_laplace = new HashMap<Integer, Double>();
			//			Map<Integer, Double> docId_score_jmercer = new HashMap<Integer, Double>();

			// If we have calculated the repeated words before, then we get just get the result
			if (seenTerms.contains(term)) {
				docId_score_okapi = term_map_okapi.get(term);
				//				docId_score_tfIdf = term_map_tfIdf.get(term);
				docId_score_bm25 = term_map_bm25.get(term);
				docId_score_laplace = term_map_laplace.get(term);
				//				docId_score_jmercer = term_map_jmercer.get(term);
			} else {

				docId_tf_one_term = null;

				// Jelinek-Mercer Smoothing
				double totalTf = 0;
				double totalLenOfDoc = 0;

				docId_tf_one_term = runQuery_.getDocId_tf_one_term(term);
				//					totalTf = t.totalTf_;
				//					totalLenOfDoc = t.totalLenOfDoc_;
				//					docno_TfTotalTfTotalLenOfDoc_.put(term, t);

				// number of documents which contain term w
				int dfw = 0;
				try {
					dfw = docId_tf_one_term.size();
				} catch (Exception e1) {
					System.out.println("Term: " + term);
					e1.printStackTrace();
				}

				for (Entry<Integer, Integer> entry : docId_tf_one_term.entrySet()) {
					int docId = entry.getKey(); // docno
					int tf = entry.getValue(); // tf for one term
					// double score = 0;
					double okapi_score_one_term_one_doc = 0;
					//					double tfIdf_score_one_term_one_doc = 0;
					double bm25_score_one_term_one_doc = 0;
					double laplace_score_one_term_one_doc = 0;
					//					double jmercer_score_one_term_one_doc = 0;

					if (tf == 0) {
						// In theory, this case shouldn't happen because
						// when you query against the Elasticsearch, for example
						// for term "alleg", you will get 6252 results back,
						// all of term should have at least tf_wd >= 1
					} else {
						double lengOfDoc = 0;
						if (docId_lenOfDoc_.containsKey(docId))
							lengOfDoc = docId_lenOfDoc_.get(docId);

						OkapiTF okapiTF = new OkapiTF();
						okapi_score_one_term_one_doc = okapiTF.Cal_OkapiTF(tf, lengOfDoc, avgLengthOfDoc_);

						//						TFIDF tfIdf = new TFIDF();
						//						tfIdf_score_one_term_one_doc = tfIdf.Cal_TFIDF(tf, lengOfDoc, avgLengthOfDoc_, D_, dfw);

						BM25 bm25 = new BM25();
						bm25_score_one_term_one_doc = bm25.Cal_BM25(tf, lengOfDoc, avgLengthOfDoc_, D_, dfw, tf_wq_map.get(term), Constants.k1, Constants.k2, Constants.b);

						LaplaceSmoothing laplace = new LaplaceSmoothing();
						laplace_score_one_term_one_doc = laplace.Cal_Laplace(tf, lengOfDoc, V_);

						//						JelinekMercer jk = new JelinekMercer();
						//						jmercer_score_one_term_one_doc = jk.Cal_JelinekMercer(tf, lengOfDoc, (totalTf - tf), (totalLenOfDoc - lengOfDoc), Constants.lambda);
					}

					docId_score_okapi.put(docId, okapi_score_one_term_one_doc);
					//					docno_score_tfIdf.put(docId, tfIdf_score_one_term_one_doc);
					docId_score_bm25.put(docId, bm25_score_one_term_one_doc);
					docId_score_laplace.put(docId, laplace_score_one_term_one_doc);
					//					docno_score_jmercer.put(docId, jmercer_score_one_term_one_doc);
				}
			}

			term_map_okapi.put(term, docId_score_okapi);
			//			term_map_tfIdf.put(term, docId_score_tfIdf);
			term_map_bm25.put(term, docId_score_bm25);
			term_map_laplace.put(term, docId_score_laplace);
			//			term_map_jmercer.put(term, docId_score_jmercer);

			seenTerms.add(term);
		}

		// A list of models
		allModels.add(term_map_okapi);
		//		allModels.add(term_map_tfIdf);
		allModels.add(term_map_bm25);
		allModels.add(term_map_laplace);
		//		allModels.add(term_map_jmercer);

		return allModels;
	}

	/**
	 * First create a set to get a unique collection of docId, then iterate through
	 * every docId and every term to sum up the scores to get the matching score
	 * @param term_map - Map<term, Map<docno, score>>
	 * @param model - all the vector space and language models
	 * @return - a map of docId and a matching score
	 */
	private Map<String, Double> calcAndSortScoreForAllDocId(HashMap<String, Map<Integer, Double>> term_map, int model) {

		// Create a set so that, we can get a unique collection of docId
		HashSet<Integer> docno_Set = new HashSet<Integer>();

		for (String term : term_map.keySet()) {
			Map<Integer, Double> map = term_map.get(term);
			Set<Integer> keySet = map.keySet();
			docno_Set.addAll(keySet);
		}

		Map<Integer, Double> hashMap_docId_score = new HashMap<Integer, Double>();
		Map<String, Double> hashMap_docno_score = new HashMap<String, Double>();

		for (Integer docId : docno_Set) {

			// add up all the score of this doc
			double score = 0;

			for (String term : term_map.keySet()) {

				//				// base is the case when tf_wd = 0, we only need to worry
				//				// for model = 3 (Laplace Smoothing) and model = 4 (Mercer Smoothing)
				//				// because for other cases score = 0 for a word in a query when tf_wd = 0
				//				double base = 0;
				//				if (model == 3){
				//					base = Math.log10(1.0 / (docId_lenOfDoc_.get(docId) + V_)); 
				//				}else if(model == 4){
				////					base = Math.log10((1 - Constants.lambda) * (docno_TfTotalTfTotalLenOfDoc_.get(term).totalTf_)
				////							/ (docno_TfTotalTfTotalLenOfDoc_.get(term).totalLenOfDoc_ - docno_lenOfDoc_.get(docno)));  
				//				}

				// base is the case when tf_wd = 0, we only need to worry
				// for model = 2 (Laplace Smoothing)
				// because for other cases score = 0 for a word in a query when tf_wd = 0
				double base = 0;
				if (model == 2){
					base = Math.log10(1.0 / (docId_lenOfDoc_.get(docId) + V_)); 
				}

				Map<Integer, Double> docId_score = term_map.get(term);

				// For a word, when it's docno matches a tf_wd, we will get the tf_wd
				// and add it to the score
				// But make sure the check null first to avoid null pointer exception
				if (docId_score.get(docId) != null) {
					double score_for_term_docno = docId_score.get(docId);					
					score += score_for_term_docno;
				} else {
					// For model = 0, 1, 2 base = 0
					score += base;
				}
			}

			hashMap_docId_score.put(docId, score);
			hashMap_docno_score.put(docId_docno_.get(docId), score);

		}

		Map<String, Double> sortedMapDesc = sortByComparator(hashMap_docno_score, false);

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

	private Set<String> getIrrelevantLst(){
		Set<String> irrelevantLst = new HashSet<String>();
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
		irrelevantLst.add("taken");
		irrelevantLst.add("caus");
		irrelevantLst.add("determin");
		irrelevantLst.add("system");
		
		// For original
		irrelevantLst.add("or");
		irrelevantLst.add("be");
		irrelevantLst.add("against");
		irrelevantLst.add("of");
		irrelevantLst.add("ani");
		irrelevantLst.add("a");
		irrelevantLst.add("which");
		irrelevantLst.add("ha");
		irrelevantLst.add("at");
		irrelevantLst.add("ha");
		irrelevantLst.add("on");
		irrelevantLst.add("in");
		irrelevantLst.add("some");
		irrelevantLst.add("about");
		irrelevantLst.add("the");
		irrelevantLst.add("will");
		irrelevantLst.add("an");
		irrelevantLst.add("by");
		irrelevantLst.add("into");
		irrelevantLst.add("an");
		irrelevantLst.add("it");
		irrelevantLst.add("method");
		irrelevantLst.add("certain");
		irrelevantLst.add("to");
		irrelevantLst.add("with");
		irrelevantLst.add("about");
		irrelevantLst.add("other");
		irrelevantLst.add("how");
		irrelevantLst.add("do");
		irrelevantLst.add("over");
		irrelevantLst.add("someth");
		irrelevantLst.add("been");
		irrelevantLst.add("and");
		irrelevantLst.add("sinc");
		irrelevantLst.add("action");


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

	public List<String> removeStopwords(String[] terms, List<String> stopWordLst) {

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

	private void read_docId_docno(String docidDocnoPath) {

		String line = null;
		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(docidDocnoPath));
			line = br.readLine();

			String[] docId_lenOfDoc_array = line.split("\\s+");
			docId_docno_.put(Integer.parseInt(docId_lenOfDoc_array[0]), docId_lenOfDoc_array[1]);

			while (line != null) {
				line = br.readLine();
				if (line == null)
					break;
				String[] docId_lenOfDoc_array2 = line.split("\\s+");
				docId_docno_.put(Integer.parseInt(docId_lenOfDoc_array2[0]), docId_lenOfDoc_array2[1]);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private List<String> removeIrrelevant(List<String> porterStemmedTerms) {

		List<String> finalTerms = new ArrayList<String>();
		Set<String> irrelevantLst = getIrrelevantLst();

		for (String term : porterStemmedTerms) {
			if (irrelevantLst.contains(term))
				continue;
			finalTerms.add(term);
		}

		return finalTerms;
	}

	private List<String> noStem_withStopwords(String queryLine) {

		String pattern = "\\w+(\\.?\\w+)*";
		Matcher m = Pattern.compile(pattern).matcher(queryLine);

		List<String> termsLst = new ArrayList<String>();
		while (m.find()) {
			// Find a next matching term by the group method and make it lower case
			String newTerm = m.group().toLowerCase();
			termsLst.add(newTerm);
		}
		
		String[] terms = termsLst.toArray(new String[termsLst.size()]);

		// Chop off the first three words: "Document" "will "discuss" or "Document" "will" "report" or				
		// "Document" "will" "include" or "Document" "will" "identify"
		String[] chopFirstThreeWords = Arrays.copyOfRange(terms, 3, terms.length);

		// Get the words after applying the porter stemming algorithm, for example: allegations will become alleg
		List<String> porterStemmedTerms = porterStem(Arrays.asList(chopFirstThreeWords));

		// Manually remove the irrelevant words
		List<String> finalQueryTerms = removeIrrelevant(porterStemmedTerms);

		return finalQueryTerms;
	}

	private List<String> noStem_withoutStopwords(String queryLine) {

		String pattern = "\\w+(\\.?\\w+)*";
		Matcher m = Pattern.compile(pattern).matcher(queryLine);

		List<String> termsLst = new ArrayList<String>();
		while (m.find()) {
			// Find a next matching term by the group method and make it lower case
			String newTerm = m.group().toLowerCase();
			termsLst.add(newTerm);
		}
		
		String[] terms = termsLst.toArray(new String[termsLst.size()]);

		// Chop off the first three words: "Document" "will "discuss" or "Document" "will" "report" or				
		// "Document" "will" "include" or "Document" "will" "identify"
		String[] chopFirstThreeWords = Arrays.copyOfRange(terms, 3, terms.length);

		// Remove stop words, for example: or, being, of, against, any will be removed
		List<String> termsWithoutStopwords = removeStopwords(chopFirstThreeWords, stopWordLst_);

		// Manually remove the irrelevant words
		List<String> finalQueryTerms = removeIrrelevant(termsWithoutStopwords);

		return finalQueryTerms;
	}

	private List<String> porterstem_withStopwords(String queryLine) {
		String pattern = "\\w+(\\.?\\w+)*";
		Matcher m = Pattern.compile(pattern).matcher(queryLine);

		List<String> termsLst = new ArrayList<String>();
		while (m.find()) {
			// Find a next matching term by the group method and make it lower case
			String newTerm = m.group().toLowerCase();
			termsLst.add(newTerm);
		}
		
		String[] terms = termsLst.toArray(new String[termsLst.size()]);

		// Chop off the first three words: "Document" "will "discuss" or "Document" "will" "report" or				
		// "Document" "will" "include" or "Document" "will" "identify"
		String[] chopFirstThreeWords = Arrays.copyOfRange(terms, 3, terms.length);

		// Get the words after applying the porter stemming algorithm, for example: allegations will become alleg
		List<String> porterStemmedTerms = porterStem(Arrays.asList(chopFirstThreeWords));

		// Manually remove the irrelevant words
		List<String> stemmedTerms = removeIrrelevant(porterStemmedTerms);

		return stemmedTerms;
	}

	private List<String> porterStem_removeStopwords(String queryLine){
		String pattern = "\\w+(\\.?\\w+)*";
		Matcher m = Pattern.compile(pattern).matcher(queryLine);

		List<String> termsLst = new ArrayList<String>();
		while (m.find()) {
			// Find a next matching term by the group method and make it lower case
			String newTerm = m.group().toLowerCase();
			termsLst.add(newTerm);
		}
		
		String[] terms = termsLst.toArray(new String[termsLst.size()]);

		// Chop off the first three words: "Document" "will "discuss" or "Document" "will" "report" or				
		// "Document" "will" "include" or "Document" "will" "identify"
		String[] chopFirstThreeWords = Arrays.copyOfRange(terms, 3, terms.length);

		// Remove stop words, for example: or, being, of, against, any will be removed
		List<String> termsWithoutStopWords = removeStopwords(chopFirstThreeWords, stopWordLst_);

		// Get the words after applying the porter stemming algorithm, for example: allegations will become alleg
		List<String> porterStemmedTerms = porterStem(termsWithoutStopWords);

		// Manually remove the irrelevant words
		List<String> stemmedTerms = removeIrrelevant(porterStemmedTerms);

		return stemmedTerms;
	}
}
