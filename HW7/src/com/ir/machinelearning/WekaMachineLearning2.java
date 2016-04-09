package com.ir.machinelearning;

/**
 * Part I, Manual Spam Features
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import weka.classifiers.Evaluation;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;

import com.ir.feature.ObtainTermFeatureScore;
import com.ir.util.Constants;
import com.ir.util.FileWriter_Helper;

public class WekaMachineLearning2 {

	public List<String> true_results = new ArrayList<String>();

	public Map<String, String> mailId_spam_map_;

	public void write_feature_matrix(){
		ObtainTermFeatureScore featureScore = new ObtainTermFeatureScore();
		List<Map<String, Double>> lst_mailId_score_map = featureScore.getLstNGramMailIdScoreMap();
		Map<String, String> mailId_spam_map = featureScore.getMailIdSpamMap();
		Map<String, String> mailId_spam_map2 = featureScore.getActualMailIdSpamMap();
		mailId_spam_map_ = mailId_spam_map2;
		StringBuilder data_matrix = new StringBuilder();
		StringBuilder data_matrix_with_id = new StringBuilder();
		StringBuilder data_matrix_train = new StringBuilder();
		StringBuilder data_matrix_test = new StringBuilder();

		// data_matrix
		data_matrix.append("@RELATION ML");
		data_matrix.append(Constants.newline);
		for (String ngram : Constants.ngrams) {
			ngram = ngram.replace(" ", "");
			data_matrix.append("@ATTRIBUTE " + ngram + " NUMERIC");
			data_matrix.append(Constants.newline);
		}
		
		// Originally used NUMERIC, but it didn't work
		// tried REAL, also didn't work
		// Only <nominal-specification> works, which is {0, 1}
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
		
		// Originally used NUMERIC, but it didn't work
		// tried REAL, also didn't work
		// Only <nominal-specification> works, which is {0, 1}
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
		
		// Originally used NUMERIC, but it didn't work
		// tried REAL, also didn't work
		// Only <nominal-specification> works, which is {0, 1}
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

				// Write to the test feature matrix
				data_matrix_test.append(ngrams0 + "," + ngrams1 + "," + ngrams2 + ","
						+ ngrams3 + "," + ngrams4 + "," + ngrams5 + "," + mailId_spam_map.get(mailId));
				data_matrix_test.append(Constants.newline);
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

				// Write to the train feature matrix
				data_matrix_train.append(ngrams0 + "," + ngrams1 + "," + ngrams2 + ","
						+ ngrams3 + "," + ngrams4 + "," + ngrams5 + "," + mailId_spam_map.get(mailId));
				data_matrix_train.append(Constants.newline);
			}

		}
		String data_matrix_test_str = data_matrix_test.toString();
		FileWriter_Helper.writeToFile(Constants.data_matrix_test, data_matrix_test_str);

		String data_matrix_train_str = data_matrix_train.toString();
		FileWriter_Helper.writeToFile(Constants.data_matrix_train, data_matrix_train_str);
	}

	public void linear_regression(){
		// Train a learning algorithm
		try {
			Instances test_data = new Instances(new BufferedReader(new FileReader(Constants.data_matrix_test)));
			Instances train_data = new Instances(new BufferedReader(new FileReader(Constants.data_matrix_train)));
			test_data.setClassIndex(test_data.numAttributes() - 1);
			train_data.setClassIndex(train_data.numAttributes() - 1);
			// Use decision tree because when predicting, 
			J48 decision_tree = new J48();
			decision_tree.buildClassifier(train_data);
			Enumeration enumerateInstances = train_data.enumerateInstances();
			Enumeration enumerateInstances2 = test_data.enumerateInstances();

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

			Map<String, Double> mailId_prediction_map = new HashMap<String, Double>();
			while (enumerateInstances.hasMoreElements()) {

				Instance data_element = (Instance)enumerateInstances.nextElement();
				double result = decision_tree.classifyInstance(data_element);

				String mailId = lines.get(index).split(" ", 2)[0];
				mailId_prediction_map.put(mailId, result);

				index++;
			}
			
			double trainingPrecision = calculatePrecision(mailId_prediction_map, mailId_spam_map_);
			System.out.println("The training precision is: " + trainingPrecision);
			
			Map<String, Double> mailId_testing_estimation_map = new HashMap<String, Double>();
			while (enumerateInstances2.hasMoreElements()) {

				Instance data_element = (Instance)enumerateInstances2.nextElement();
				double result = decision_tree.classifyInstance(data_element);

				String mailId = lines.get(index).split(" ", 2)[0];
				mailId_prediction_map.put(mailId, result);
				mailId_testing_estimation_map.put(mailId, result);

				index++;
			}
			double testingPrecision = calculatePrecision(mailId_testing_estimation_map, mailId_spam_map_);
			System.out.println("The testing precision is: " + testingPrecision);
			
			double precision = calculatePrecision(mailId_prediction_map, mailId_spam_map_);
			System.out.println("The total precision is: " + precision);

			Evaluation eval = new Evaluation(train_data);
			eval.evaluateModel(decision_tree, test_data);
			System.out.println(eval.toSummaryString("\nResults\n=====\n", false));

			System.out.println(decision_tree);
			
			// Test the spam model
			Map<String, Double> sortedMapDesc = sortByComparator(mailId_prediction_map, DESC);
			Map<String, Double> nMapDesc = getTopNMap(sortedMapDesc, 10);
			System.out.println("Top 10 spams");
			printMap(nMapDesc);
			Map<String, Double> sortedMapAsc = sortByComparator(mailId_prediction_map, ASC);
			Map<String, Double> nMapAsc = getTopNMap(sortedMapAsc, 10);
			System.out.println("Top 10 hams");
			printMap(nMapAsc);
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
			// This line is printed out twice because in both training and testing
			// the number of emails are about 4/5 and 1/5 of the total emails
			System.out.println("Count not the same!");
		}

		double correct = 0;
		for (String key : obtainedRes.keySet()) {
			String mailId = key.split(",")[0];
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
		WekaMachineLearning2 weka = new WekaMachineLearning2();
		weka.write_feature_matrix();

		weka.linear_regression();
	}

}

