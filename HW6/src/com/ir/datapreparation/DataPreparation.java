package com.ir.datapreparation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.ir.util.Constants;
import com.ir.util.FileWriter_Helper;

public class DataPreparation {

	public Map<String, Map<String, String>> queryNo_docno_relevance_;
	public Set<String> allQueries_;
	// 20 random training queries
	public Set<String> trainingQueries_;
	// 5 random training queries
	public Set<String> testingQueries_;
	
	public static Map<String, Map<String, String>> okapi_queryNo_docno_score_;
	public static Map<String, Map<String, String>> tfidf_queryNo_docno_score_;
	public static Map<String, Map<String, String>> bm25_queryNo_docno_score_;
	public static Map<String, Map<String, String>> laplace_queryNo_docno_score_;
	public static Map<String, Map<String, String>> mercer_queryNo_docno_score_;
	
	public DataPreparation(){
		queryNo_docno_relevance_ = new HashMap<String, Map<String,String>>();
		allQueries_ = new HashSet<String>();
		trainingQueries_ = new HashSet<String>();
		testingQueries_ = new HashSet<String>();
	}
	
	/** 
	 * Read qrels from qrels.adhoc.51-100.AP89.txt
	 */
	public void read_qrels(String filename){
		Map<String, Map<String, String>> queryNo_docno_relevance = new HashMap<String, Map<String,String>>();
		try {
			FileReader fr = new FileReader(filename);
			BufferedReader br = new BufferedReader(fr);

			String line = null;
			while ((line = br.readLine()) != null) {
				String[] queryNo_assessorId_docno_relevance = line.split(" ");
				String queryNo = queryNo_assessorId_docno_relevance[0];
				
				// Since there are 50 queries in the qrels file,
				// we only want the 25 queries that in the query file
				if (!allQueries_.contains(queryNo))
					continue;
				String docno = queryNo_assessorId_docno_relevance[2];
				String relevance = queryNo_assessorId_docno_relevance[3];
				
				Map<String, String> docno_relevance = new HashMap<String, String>();
				docno_relevance.put(docno, relevance);
				if (!queryNo_docno_relevance.containsKey(queryNo)){
					queryNo_docno_relevance.put(queryNo, docno_relevance);
				} else {
					queryNo_docno_relevance.get(queryNo).put(docno, relevance);
				}
			}

			br.close();
			queryNo_docno_relevance_ = queryNo_docno_relevance;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Read 25 queries from query_desc.51-100.short.txt
	 * @param filename
	 */
	public void read_25queries(String filename){
		Map<String, Map<String, String>> queryNo_docno_relevance = new HashMap<String, Map<String,String>>();
		try {
			FileReader fr = new FileReader(filename);
			BufferedReader br = new BufferedReader(fr);

			String line = null;
			while ((line = br.readLine()) != null) {
				String[] queryNo_queryString = line.split(".   ");
				String queryNo = queryNo_queryString[0];
				
				if (!queryNo.equals(""))
					allQueries_.add(queryNo);
			}

			br.close();
			queryNo_docno_relevance_ = queryNo_docno_relevance;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Randomly pick 5 queries for training and 20 queries for testing
	 */
	public void pick_training_testing_queries(){
		List<String> asList = new ArrayList<String>(allQueries_);
		Collections.shuffle(asList);
		trainingQueries_ = new HashSet<String>(asList.subList(0, 20));
		testingQueries_ = new HashSet<String>(asList.subList(20, 25));
		FileWriter_Helper.writeToFile(Constants.trainingQueries, StringUtils.join(trainingQueries_, " "));
		FileWriter_Helper.writeToFile(Constants.testingQueries, StringUtils.join(testingQueries_, " "));
	}
	
	public Map<String, Map<String, String>> read_retrievalModel_results(String filename){
		Map<String, Map<String, String>> queryNo_docno_score = new HashMap<String, Map<String,String>>();
		try {
			FileReader fr = new FileReader(filename);
			BufferedReader br = new BufferedReader(fr);

			String line = null;
			while ((line = br.readLine()) != null) {
				String[] queryNo_Q0_docno_rank_score_exp = line.split(" ");
				String queryNo = queryNo_Q0_docno_rank_score_exp[0];
				String docno = queryNo_Q0_docno_rank_score_exp[2];
				String score = queryNo_Q0_docno_rank_score_exp[4];
				
				Map<String, String> docno_score = new HashMap<String, String>();
				docno_score.put(docno, score);
				if (!queryNo_docno_score.containsKey(queryNo)){
					queryNo_docno_score.put(queryNo, docno_score);
				} else {
					queryNo_docno_score.get(queryNo).put(docno, score);
				}
			}

			br.close();
			return queryNo_docno_score;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void write_feature_matrix(Boolean withId){
		if (withId){
//			fw.appendToFile(Constants.data_matrix_with_queryNo_docno, "@RELATION ML");
//			fw.appendToFile(Constants.data_matrix_with_queryNo_docno, Constants.newline);			
//			fw.appendToFile(Constants.data_matrix_with_queryNo_docno, "@ATTRIBUTE OKAPI_TF NUMERIC");
//			fw.appendToFile(Constants.data_matrix_with_queryNo_docno, Constants.newline);			
//			fw.appendToFile(Constants.data_matrix_with_queryNo_docno, "@ATTRIBUTE TF_IDF NUMERIC");
//			fw.appendToFile(Constants.data_matrix_with_queryNo_docno, Constants.newline);			
//			fw.appendToFile(Constants.data_matrix_with_queryNo_docno, "@ATTRIBUTE OKAPI_BM25 NUMERIC");
//			fw.appendToFile(Constants.data_matrix_with_queryNo_docno, Constants.newline);			
//			fw.appendToFile(Constants.data_matrix_with_queryNo_docno, "@ATTRIBUTE LM_LAPLACE NUMERIC");
//			fw.appendToFile(Constants.data_matrix_with_queryNo_docno, Constants.newline);			
//			fw.appendToFile(Constants.data_matrix_with_queryNo_docno, "@ATTRIBUTE LM_JM NUMERIC");
//			fw.appendToFile(Constants.data_matrix_with_queryNo_docno, Constants.newline);			
//			fw.appendToFile(Constants.data_matrix_with_queryNo_docno, "@ATTRIBUTE label NUMERIC");
//			fw.appendToFile(Constants.data_matrix_with_queryNo_docno, Constants.newline);			
//			fw.appendToFile(Constants.data_matrix_with_queryNo_docno, "@DATA");
//			fw.appendToFile(Constants.data_matrix_with_queryNo_docno, Constants.newline);	
			
			StringBuilder sb = new StringBuilder();
//			sb.append("@RELATION ML");
//			sb.append(Constants.newline);
//			sb.append("@ATTRIBUTE OKAPI_TF NUMERIC");
//			sb.append(Constants.newline);
//			sb.append("@ATTRIBUTE TF_IDF NUMERIC");
//			sb.append(Constants.newline);
//			sb.append("@ATTRIBUTE OKAPI_BM25 NUMERIC");
//			sb.append(Constants.newline);
//			sb.append("@ATTRIBUTE LM_LAPLACE NUMERIC");
//			sb.append(Constants.newline);
//			sb.append("@ATTRIBUTE LM_JM NUMERIC");
//			sb.append(Constants.newline);
//			sb.append("@ATTRIBUTE label NUMERIC");
//			sb.append(Constants.newline);
//			sb.append("@DATA");
//			sb.append(Constants.newline);
			for (String queryNo : queryNo_docno_relevance_.keySet()) {
				for (String docno : queryNo_docno_relevance_.get(queryNo).keySet()) {
					String relevance;
					if (testingQueries_.contains(queryNo))
						relevance = "?";
					else
						relevance = queryNo_docno_relevance_.get(queryNo).get(docno);
					double okapi_score = 0;
					double tfidf_score = 0;
					double bm25_score = 0;
					double laplace_score = 0;
					double mercer_score = 0;
					if (okapi_queryNo_docno_score_.containsKey(queryNo) && okapi_queryNo_docno_score_.get(queryNo).containsKey(docno))
						okapi_score = Double.parseDouble(okapi_queryNo_docno_score_.get(queryNo).get(docno));
					if (tfidf_queryNo_docno_score_.containsKey(queryNo) && tfidf_queryNo_docno_score_.get(queryNo).containsKey(docno))
						tfidf_score = Double.parseDouble(tfidf_queryNo_docno_score_.get(queryNo).get(docno));
					if (bm25_queryNo_docno_score_.containsKey(queryNo) && bm25_queryNo_docno_score_.get(queryNo).containsKey(docno))
						bm25_score = Double.parseDouble(bm25_queryNo_docno_score_.get(queryNo).get(docno));
					if (laplace_queryNo_docno_score_.containsKey(queryNo) && laplace_queryNo_docno_score_.get(queryNo).containsKey(docno))
						laplace_score = Double.parseDouble(laplace_queryNo_docno_score_.get(queryNo).get(docno));
					if (mercer_queryNo_docno_score_.containsKey(queryNo) && mercer_queryNo_docno_score_.get(queryNo).containsKey(docno))
						mercer_score = Double.parseDouble(mercer_queryNo_docno_score_.get(queryNo).get(docno));
					sb.append(queryNo + " " + docno + " " + okapi_score + " " + tfidf_score + " " + bm25_score + " "
							+ laplace_score + " " + mercer_score + " " + relevance);
					sb.append(Constants.newline);
					
				}
			}
			
			String output = sb.toString();
			FileWriter_Helper.writeToFile(Constants.data_matrix_with_queryNo_docno, output);
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("@RELATION ML");
			sb.append(Constants.newline);
			sb.append("@ATTRIBUTE OKAPI_TF NUMERIC");
			sb.append(Constants.newline);
			sb.append("@ATTRIBUTE TF_IDF NUMERIC");
			sb.append(Constants.newline);
			sb.append("@ATTRIBUTE OKAPI_BM25 NUMERIC");
			sb.append(Constants.newline);
			sb.append("@ATTRIBUTE LM_LAPLACE NUMERIC");
			sb.append(Constants.newline);
			sb.append("@ATTRIBUTE LM_JM NUMERIC");
			sb.append(Constants.newline);
			sb.append("@ATTRIBUTE label NUMERIC");
			sb.append(Constants.newline);
			sb.append("@DATA");
			sb.append(Constants.newline);
			for (String queryNo : queryNo_docno_relevance_.keySet()) {
				for (String docno : queryNo_docno_relevance_.get(queryNo).keySet()) {
					String relevance;
					if (testingQueries_.contains(queryNo))
						relevance = "?";
					else
						relevance = queryNo_docno_relevance_.get(queryNo).get(docno);
					double okapi_score = 0;
					double tfidf_score = 0;
					double bm25_score = 0;
					double laplace_score = 0;
					double mercer_score = 0;
					if (okapi_queryNo_docno_score_.containsKey(queryNo) && okapi_queryNo_docno_score_.get(queryNo).containsKey(docno))
						okapi_score = Double.parseDouble(okapi_queryNo_docno_score_.get(queryNo).get(docno));
					if (tfidf_queryNo_docno_score_.containsKey(queryNo) && tfidf_queryNo_docno_score_.get(queryNo).containsKey(docno))
						tfidf_score = Double.parseDouble(tfidf_queryNo_docno_score_.get(queryNo).get(docno));
					if (bm25_queryNo_docno_score_.containsKey(queryNo) && bm25_queryNo_docno_score_.get(queryNo).containsKey(docno))
						bm25_score = Double.parseDouble(bm25_queryNo_docno_score_.get(queryNo).get(docno));
					if (laplace_queryNo_docno_score_.containsKey(queryNo) && laplace_queryNo_docno_score_.get(queryNo).containsKey(docno))
						laplace_score = Double.parseDouble(laplace_queryNo_docno_score_.get(queryNo).get(docno));
					if (mercer_queryNo_docno_score_.containsKey(queryNo) && mercer_queryNo_docno_score_.get(queryNo).containsKey(docno))
						mercer_score = Double.parseDouble(mercer_queryNo_docno_score_.get(queryNo).get(docno));
					sb.append(okapi_score + " " + tfidf_score + " " + bm25_score + " "
							+ laplace_score + " " + mercer_score + " " + relevance);
					sb.append(Constants.newline);
					
				}
			}
			
			String output = sb.toString();
			FileWriter_Helper.writeToFile(Constants.data_matrix, output);
		}
	}
	
	public static void main(String[] args) {
		DataPreparation dp = new DataPreparation();
		dp.read_25queries(Constants.allQueris);
		System.out.println("Total number of queries: " + dp.allQueries_.size());

		dp.read_qrels(Constants.qrels);
		int index = 0;
		for (String queryNo : dp.queryNo_docno_relevance_.keySet()) {
			index += dp.queryNo_docno_relevance_.get(queryNo).size();
		}
		System.out.println("Total number of documents: " + index);
		
		
		dp.pick_training_testing_queries();
		System.out.println("Training queries: " + dp.trainingQueries_);
		System.out.println("Testing queries: " + dp.testingQueries_);
		
		okapi_queryNo_docno_score_ = dp.read_retrievalModel_results(Constants.okapi);
		tfidf_queryNo_docno_score_ = dp.read_retrievalModel_results(Constants.tfidf);
		bm25_queryNo_docno_score_ = dp.read_retrievalModel_results(Constants.bm25);
		laplace_queryNo_docno_score_ = dp.read_retrievalModel_results(Constants.laplace);
		mercer_queryNo_docno_score_ = dp.read_retrievalModel_results(Constants.mercer);
		System.out.println(okapi_queryNo_docno_score_.size());
		System.out.println(tfidf_queryNo_docno_score_.size());
		System.out.println(bm25_queryNo_docno_score_.size());
		System.out.println(laplace_queryNo_docno_score_.size());
		System.out.println(mercer_queryNo_docno_score_.size());
		
		dp.write_feature_matrix(true);
		dp.write_feature_matrix(false);
	}

}
