package com.ir.machinelearning;

/**
 * Part I, Manual Spam Features
 * @author kaichenzhang
 *
 */
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import weka.classifiers.Evaluation;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;

import com.ir.feature.ObtainTermFeatureScore;
import com.ir.util.Constants;
import com.ir.util.FileWriter_Helper;
import com.ir.util.Pair;

public class WekaMachineLearning {

	public Map<String, Map<String, String>> queryNo_docno_relevance_;
	public Set<String> allQueries_;

	public List<String> doc_ids = new ArrayList<String>();
	public List<String> q_ids = new ArrayList<String>();
	public List<String> true_results = new ArrayList<String>();

	// 20 random training queries
	public static Set<String> trainingQueries_;
	// 5 random training queries
	public static Set<String> testingQueries_;
	
	public Map<String, String> mailId_spam_map_;

	private static HashMap<String, TreeSet<Pair>> train_qid_scoredocid_pair = new HashMap<String, TreeSet<Pair>>();
	private static HashMap<String, TreeSet<Pair>> test_qid_scoredocid_pair = new HashMap<String, TreeSet<Pair>>();
	final private static String SPACE = " ";
	final private static String ENTER = "\r\n";

	public WekaMachineLearning(){
		queryNo_docno_relevance_ = new HashMap<String, Map<String,String>>();
		allQueries_ = new HashSet<String>();
		trainingQueries_ = new HashSet<String>();
		testingQueries_ = new HashSet<String>();
//		mailId_spam_map_ = new HashMap<String, String>();
	}

	public void write_feature_matrix(Boolean withId){
		if (withId){

			//			StringBuilder sb = new StringBuilder();
			//			for (String queryNo : queryNo_docno_relevance_.keySet()) {
			//				for (String docno : queryNo_docno_relevance_.get(queryNo).keySet()) {
			//					String relevance;
			//					if (testingQueries_.contains(queryNo))
			//						relevance = "?";
			//					else
			//						relevance = queryNo_docno_relevance_.get(queryNo).get(docno);
			//					double okapi_score = 0;
			//					double tfidf_score = 0;
			//					double bm25_score = 0;
			//					double laplace_score = 0;
			//					double mercer_score = 0;
			//					if (okapi_queryNo_docno_score_.containsKey(queryNo) && okapi_queryNo_docno_score_.get(queryNo).containsKey(docno))
			//						okapi_score = Double.parseDouble(okapi_queryNo_docno_score_.get(queryNo).get(docno));
			//					if (tfidf_queryNo_docno_score_.containsKey(queryNo) && tfidf_queryNo_docno_score_.get(queryNo).containsKey(docno))
			//						tfidf_score = Double.parseDouble(tfidf_queryNo_docno_score_.get(queryNo).get(docno));
			//					if (bm25_queryNo_docno_score_.containsKey(queryNo) && bm25_queryNo_docno_score_.get(queryNo).containsKey(docno))
			//						bm25_score = Double.parseDouble(bm25_queryNo_docno_score_.get(queryNo).get(docno));
			//					if (laplace_queryNo_docno_score_.containsKey(queryNo) && laplace_queryNo_docno_score_.get(queryNo).containsKey(docno))
			//						laplace_score = Double.parseDouble(laplace_queryNo_docno_score_.get(queryNo).get(docno));
			//					if (mercer_queryNo_docno_score_.containsKey(queryNo) && mercer_queryNo_docno_score_.get(queryNo).containsKey(docno))
			//						mercer_score = Double.parseDouble(mercer_queryNo_docno_score_.get(queryNo).get(docno));
			//					sb.append(queryNo + " " + docno + " " + okapi_score + " " + tfidf_score + " " + bm25_score + " "
			//							+ laplace_score + " " + mercer_score + " " + relevance);
			//					sb.append(Constants.newline);
			//					
			//				}
			//			}
			//			
			//			String output = sb.toString();
			//			FileWriter_Helper.writeToFile(Constants.data_matrix_with_queryNo_docno, output);
			ObtainTermFeatureScore featureScore = new ObtainTermFeatureScore();
			List<Map<String, Double>> lst_mailId_score_map = featureScore.getLstNGramMailIdScoreMap();
			Map<String, String> mailId_spam_map = featureScore.getMailIdSpamMap();
			StringBuilder sb = new StringBuilder();

			for (String mailId : mailId_spam_map.keySet()) {
				double ngrams0 = 0;
				double ngrams1 = 0;
				double ngrams2 = 0;
				double ngrams3 = 0;
				double ngrams4 = 0;
				double ngrams5 = 0;

				for (int i = 0; i < lst_mailId_score_map.size(); i++){
					if (i == 0 && lst_mailId_score_map.get(i).containsKey(mailId))
						ngrams0 = lst_mailId_score_map.get(i).get(mailId);
					if (i == 1 && lst_mailId_score_map.get(i).containsKey(mailId))
						ngrams1 = lst_mailId_score_map.get(i).get(mailId);
					if (i == 2 && lst_mailId_score_map.get(i).containsKey(mailId))
						ngrams2 = lst_mailId_score_map.get(i).get(mailId);
					if (i == 3 && lst_mailId_score_map.get(i).containsKey(mailId))
						ngrams3 = lst_mailId_score_map.get(i).get(mailId);
					if (i == 4 && lst_mailId_score_map.get(i).containsKey(mailId))
						ngrams4 = lst_mailId_score_map.get(i).get(mailId);
					if (i == 5 && lst_mailId_score_map.get(i).containsKey(mailId))
						ngrams5 = lst_mailId_score_map.get(i).get(mailId);
				}

				sb.append(mailId + " " + ngrams0 + " " + ngrams1 + " " + ngrams2 + " "
						+ ngrams3 + " " + ngrams4 + " " + ngrams5 + " " + mailId_spam_map.get(mailId));
				sb.append(Constants.newline);
			}

			String output = sb.toString();
			FileWriter_Helper.writeToFile(Constants.data_matrix_with_mailId, output);
		} else {
			ObtainTermFeatureScore featureScore = new ObtainTermFeatureScore();
			List<Map<String, Double>> lst_mailId_score_map = featureScore.getLstNGramMailIdScoreMap();
			Map<String, String> mailId_spam_map = featureScore.getMailIdSpamMap();
			Map<String, String> mailId_spam_map2 = featureScore.getActualMailIdSpamMap();
			mailId_spam_map_ = mailId_spam_map2;
			StringBuilder data_matrix = new StringBuilder();
			StringBuilder data_matrix_with_id = new StringBuilder();
			StringBuilder data_matrix_train = new StringBuilder();
			StringBuilder data_matrix_test = new StringBuilder();
			//			StringBuilder data_matrix_with_id = new StringBuilder();
			//			StringBuilder data_matrix_with_id = new StringBuilder();

			// data_matrix
			data_matrix.append("@RELATION ML");
			data_matrix.append(Constants.newline);
			for (String ngram : Constants.ngrams) {
				ngram = ngram.replace(" ", "");
				data_matrix.append("@ATTRIBUTE " + ngram + " NUMERIC");
				data_matrix.append(Constants.newline);
			}
			data_matrix.append("@ATTRIBUTE label {0, 1}");
			data_matrix.append(Constants.newline);
			data_matrix.append("@DATA");
			data_matrix.append(Constants.newline);

			// data_matrix_train
			data_matrix_train.append("@RELATION ML");
			data_matrix_train.append(Constants.newline);
			for (String ngram : Constants.ngrams) {
				ngram = ngram.replace(" ", "");
				data_matrix_train.append("@ATTRIBUTE " + ngram + " NUMERIC");
				data_matrix_train.append(Constants.newline);
			}
			data_matrix_train.append("@ATTRIBUTE label {0, 1}");
			data_matrix_train.append(Constants.newline);
			data_matrix_train.append("@DATA");
			data_matrix_train.append(Constants.newline);

			// data_matrix_test
			data_matrix_test.append("@RELATION ML");
			data_matrix_test.append(Constants.newline);
			for (String ngram : Constants.ngrams) {
				ngram = ngram.replace(" ", "");
				data_matrix_test.append("@ATTRIBUTE " + ngram + " NUMERIC");
				data_matrix_test.append(Constants.newline);
			}
			data_matrix_test.append("@ATTRIBUTE label {0, 1}");
			data_matrix_test.append(Constants.newline);
			data_matrix_test.append("@DATA");
			data_matrix_test.append(Constants.newline);

			for (String mailId : mailId_spam_map.keySet()) {
				double ngrams0 = 0;
				double ngrams1 = 0;
				double ngrams2 = 0;
				double ngrams3 = 0;
				double ngrams4 = 0;
				double ngrams5 = 0;

				for (int i = 0; i < lst_mailId_score_map.size(); i++){
					if (i == 0 && lst_mailId_score_map.get(i).containsKey(mailId))
						ngrams0 = lst_mailId_score_map.get(i).get(mailId);
					if (i == 1 && lst_mailId_score_map.get(i).containsKey(mailId))
						ngrams1 = lst_mailId_score_map.get(i).get(mailId);
					if (i == 2 && lst_mailId_score_map.get(i).containsKey(mailId))
						ngrams2 = lst_mailId_score_map.get(i).get(mailId);
					if (i == 3 && lst_mailId_score_map.get(i).containsKey(mailId))
						ngrams3 = lst_mailId_score_map.get(i).get(mailId);
					if (i == 4 && lst_mailId_score_map.get(i).containsKey(mailId))
						ngrams4 = lst_mailId_score_map.get(i).get(mailId);
					if (i == 5 && lst_mailId_score_map.get(i).containsKey(mailId))
						ngrams5 = lst_mailId_score_map.get(i).get(mailId);
				}

				data_matrix.append(ngrams0 + "," + ngrams1 + "," + ngrams2 + ","
						+ ngrams3 + "," + ngrams4 + "," + ngrams5 + "," + mailId_spam_map.get(mailId));
				data_matrix.append(Constants.newline);
				data_matrix_with_id.append(mailId + "," + ngrams0 + "," + ngrams1 + "," + ngrams2 + ","
						+ ngrams3 + "," + ngrams4 + "," + ngrams5 + "," + mailId_spam_map.get(mailId));
				data_matrix_with_id.append(Constants.newline);
			}
			String data_matrix_whole = data_matrix.toString();
			String data_matri_with_id = data_matrix_with_id.toString();
			FileWriter_Helper.writeToFile(Constants.data_matrix, data_matrix_whole);
			FileWriter_Helper.writeToFile(Constants.data_matrix_with_mailId, data_matri_with_id);

			for (String mailId : mailId_spam_map.keySet()) {
				if (mailId_spam_map.get(mailId).equals("?")){
					double ngrams0 = 0;
					double ngrams1 = 0;
					double ngrams2 = 0;
					double ngrams3 = 0;
					double ngrams4 = 0;
					double ngrams5 = 0;

					for (int i = 0; i < lst_mailId_score_map.size(); i++){
						if (i == 0 && lst_mailId_score_map.get(i).containsKey(mailId))
							ngrams0 = lst_mailId_score_map.get(i).get(mailId);
						if (i == 1 && lst_mailId_score_map.get(i).containsKey(mailId))
							ngrams1 = lst_mailId_score_map.get(i).get(mailId);
						if (i == 2 && lst_mailId_score_map.get(i).containsKey(mailId))
							ngrams2 = lst_mailId_score_map.get(i).get(mailId);
						if (i == 3 && lst_mailId_score_map.get(i).containsKey(mailId))
							ngrams3 = lst_mailId_score_map.get(i).get(mailId);
						if (i == 4 && lst_mailId_score_map.get(i).containsKey(mailId))
							ngrams4 = lst_mailId_score_map.get(i).get(mailId);
						if (i == 5 && lst_mailId_score_map.get(i).containsKey(mailId))
							ngrams5 = lst_mailId_score_map.get(i).get(mailId);
					}

					data_matrix_test.append(ngrams0 + "," + ngrams1 + "," + ngrams2 + ","
							+ ngrams3 + "," + ngrams4 + "," + ngrams5 + "," + mailId_spam_map.get(mailId));
					data_matrix_test.append(Constants.newline);
					//					data_matrix_with_id.append(mailId + " " + ngrams0 + " " + ngrams1 + " " + ngrams2 + " "
					//							+ ngrams3 + " " + ngrams4 + " " + ngrams5 + " " + mailId_spam_map.get(mailId));
					//					data_matrix_with_id.append(Constants.newline);
				} else {
					double ngrams0 = 0;
					double ngrams1 = 0;
					double ngrams2 = 0;
					double ngrams3 = 0;
					double ngrams4 = 0;
					double ngrams5 = 0;

					for (int i = 0; i < lst_mailId_score_map.size(); i++){
						if (i == 0 && lst_mailId_score_map.get(i).containsKey(mailId))
							ngrams0 = lst_mailId_score_map.get(i).get(mailId);
						if (i == 1 && lst_mailId_score_map.get(i).containsKey(mailId))
							ngrams1 = lst_mailId_score_map.get(i).get(mailId);
						if (i == 2 && lst_mailId_score_map.get(i).containsKey(mailId))
							ngrams2 = lst_mailId_score_map.get(i).get(mailId);
						if (i == 3 && lst_mailId_score_map.get(i).containsKey(mailId))
							ngrams3 = lst_mailId_score_map.get(i).get(mailId);
						if (i == 4 && lst_mailId_score_map.get(i).containsKey(mailId))
							ngrams4 = lst_mailId_score_map.get(i).get(mailId);
						if (i == 5 && lst_mailId_score_map.get(i).containsKey(mailId))
							ngrams5 = lst_mailId_score_map.get(i).get(mailId);
					}

					data_matrix_train.append(ngrams0 + "," + ngrams1 + "," + ngrams2 + ","
							+ ngrams3 + "," + ngrams4 + "," + ngrams5 + "," + mailId_spam_map.get(mailId));
					data_matrix_train.append(Constants.newline);
					//					data_matrix_with_id.append(mailId + " " + ngrams0 + " " + ngrams1 + " " + ngrams2 + " "
					//							+ ngrams3 + " " + ngrams4 + " " + ngrams5 + " " + mailId_spam_map.get(mailId));
					//					data_matrix_with_id.append(Constants.newline);
				}

			}
			String data_matrix_test_str = data_matrix_test.toString();
			FileWriter_Helper.writeToFile(Constants.data_matrix_test, data_matrix_test_str);

			String data_matrix_train_str = data_matrix_train.toString();
			FileWriter_Helper.writeToFile(Constants.data_matrix_train, data_matrix_train_str);
		}
	}

	public void linear_regression(){
		try {
			//			Instances data = new Instances(new BufferedReader(new FileReader(Constants.data_matrix)));
			Instances test_data = new Instances(new BufferedReader(new FileReader(Constants.data_matrix_test)));
			Instances train_data = new Instances(new BufferedReader(new FileReader(Constants.data_matrix_train)));
			//			data.setClassIndex(data.numAttributes() - 1);
			test_data.setClassIndex(test_data.numAttributes() - 1);
			train_data.setClassIndex(train_data.numAttributes() - 1);
			J48 decision_tree = new J48();
			//			LinearRegression decision_tree = new LinearRegression();
			//			decision_tree.buildClassifier(data);
			decision_tree.buildClassifier(train_data);
			//			Enumeration enumerateInstances = data.enumerateInstances();
			Enumeration enumerateInstances = train_data.enumerateInstances();

			int index = 0;

			String fileName = Constants.data_matrix_with_mailId;
			List<String> lines = null;
			try
			{
				lines = Files.readAllLines(Paths.get(fileName), Charset.defaultCharset());
			}catch(IOException io)
			{
				io.printStackTrace();
			}

			Map<String, Double> mailId_estimation_map = new HashMap<String, Double>();
			while (enumerateInstances.hasMoreElements()) {

				Instance data_element = (Instance)enumerateInstances.nextElement();
				double result = decision_tree.classifyInstance(data_element);

				String mailId = lines.get(index).split(" ", 2)[0];
				mailId_estimation_map.put(mailId, result);


				index++;
			}

			Evaluation eval = new Evaluation(train_data);
			eval.evaluateModel(decision_tree, test_data);
			System.out.println(eval.toSummaryString("\nResults\n=====\n", false));

			System.out.println(decision_tree);
			Map<String, Double> sortedMapDesc = sortByComparator(mailId_estimation_map, DESC);
			Map<String, Double> nMapDesc = getTopNMap(sortedMapDesc, 10);
			System.out.println("Top 10 spams");
			printMap(nMapDesc);
			Map<String, Double> sortedMapAsc = sortByComparator(mailId_estimation_map, ASC);
			Map<String, Double> nMapAsc = getTopNMap(sortedMapAsc, 10);
			System.out.println("Top 10 hams");
			printMap(nMapAsc);
			
			double precision = calculatePrecision(mailId_estimation_map, mailId_spam_map_);
			System.out.println("The total precision is: " + precision);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private double calculatePrecision(Map<String, Double> obtainedRes, Map<String, String> expectedRes) {
		
		if (obtainedRes == null){
			System.out.println("Obtained result is null");
		}

		if (expectedRes == null){
			System.out.println("Expected result is null");
		}
		
		int obtainedCount = obtainedRes.size();
		int expectedCount = expectedRes.size();
		
		if (obtainedCount != expectedCount){
			System.out.println("Count not the same!");
		}
		
		double correct = 0;
		for (String key : obtainedRes.keySet()) {
			String mailId = key.split(",")[0];
//			String wholeRes = obtainedRes.get(key);
			double obtained = obtainedRes.get(key);
			double expected = Double.parseDouble(expectedRes.get(mailId));
			if (obtained == expected){
				correct++;
			}
		}
		
		double prec = correct / obtainedCount;
		
		return prec;
	}

	public static boolean ASC = true;
	public static boolean DESC = false;

	private Map<String, Double> sortByComparator(Map<String, Double> unsortMap, final boolean order)
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

	private Map<String, Double> getTopNMap(Map<String, Double> map, int n){
		Map<String, Double> res = new LinkedHashMap<String, Double>();

		int counter = 0;
		for (String key : map.keySet()) {
			res.put(key, map.get(key));
			counter++;
			if (counter == n)
				return res;
		}

		return map;
	}

	public void printMap(Map<String, Double> map)
	{
		for (Entry<String, Double> entry : map.entrySet())
		{
			System.out.println("Key : " + entry.getKey() + " Value : "+ entry.getValue());
		}
	}

	public static void main(String[] args) {
		WekaMachineLearning weka = new WekaMachineLearning();
		//		weka.write_feature_matrix(true);
		weka.write_feature_matrix(false);

		weka.linear_regression();
	}

}

