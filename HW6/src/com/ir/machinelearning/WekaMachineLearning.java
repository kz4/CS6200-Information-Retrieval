package com.ir.machinelearning;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import weka.classifiers.functions.LinearRegression;
import weka.core.Instance;
import weka.core.Instances;

import com.ir.datapreparation.DataPreparation;
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
	
	private static HashMap<String, TreeSet<Pair>> train_qid_scoredocid_pair = new HashMap<String, TreeSet<Pair>>();
	private static HashMap<String, TreeSet<Pair>> test_qid_scoredocid_pair = new HashMap<String, TreeSet<Pair>>();
	final private static String SPACE = " ";
	final private static String ENTER = "\r\n";

	public WekaMachineLearning(){
		queryNo_docno_relevance_ = new HashMap<String, Map<String,String>>();
		allQueries_ = new HashSet<String>();
		trainingQueries_ = new HashSet<String>();
		testingQueries_ = new HashSet<String>();
	}

	public void read_data_matrix_with_id(){
		try {
			FileReader fr = new FileReader(Constants.data_matrix_with_queryNo_docno);
			BufferedReader br = new BufferedReader(fr);

			String line = null;
			while ((line = br.readLine()) != null) {
				System.out.println(line);

				String[] components = line.split(" ");
				String q_id = components[0];
				String doc_id = components[1];
				String result = components[7];

				System.out.println("q_id : " + q_id + " doc_id : " + doc_id + " result: "+result);

				doc_ids.add(doc_id);
				q_ids.add(q_id);
				true_results.add(result);
			}

			br.close();

			return;

		} catch (IOException e) {
			e.printStackTrace();
		}

		return;
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
	
	public Set<String> read_training_testing_queryNo(String filename){
		Set<String> set = new HashSet<String>();;
		FileReader fr = null;
		try {
			fr = new FileReader(filename);
			BufferedReader br = new BufferedReader(fr);
			
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] queryNos = line.split(" ");
				set.addAll(Arrays.asList(queryNos));
			}
			return set;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void linear_regression(){
		try {
			Instances data = new Instances(new BufferedReader(new FileReader(Constants.data_matrix)));
			data.setClassIndex(data.numAttributes() - 1);
			//			J48 decision_tree = new J48();
			LinearRegression decision_tree = new LinearRegression();
			decision_tree.buildClassifier(data);
			Enumeration enumerateInstances = data.enumerateInstances();

			int index = 0;

			int re_correct = 0;
			int irre_correct = 0;
			int re_wrong = 0;
			int irre_wrong = 0;

			while (enumerateInstances.hasMoreElements()) {
				System.out.println("index: " + index);
				String q_id = q_ids.get(index);
				String doc_id = doc_ids.get(index);
				//				boolean _label = Label.get_label(q_id, doc_id);
				//				boolean _label = Label.get_label(q_id, doc_id);
				//				
				//
				//				double true_result = 0;
				//				
				//				if(_label){
				//					true_result = 1;
				//				}

				double true_result = Double.parseDouble(queryNo_docno_relevance_.get(q_id).get(doc_id));

				Instance data_element = (Instance)enumerateInstances.nextElement();
				double result = decision_tree.classifyInstance(data_element);
				double[] distributionForInstance = decision_tree.distributionForInstance(data_element);

				System.out.println("q_id: " + q_id
						+" doc_id: " + doc_id
						+ " true_result: " + true_result
						+ " predict_result: " + result);

//				boolean belong_testing_set = MachineLearning.belong_testing_set(q_id);
				boolean belong_testing_set = testingQueries_.contains(q_id);

				if(belong_testing_set){
					update_map(test_qid_scoredocid_pair, q_id, doc_id, result);
				}else{
					update_map(train_qid_scoredocid_pair, q_id, doc_id, result);
				}

				if(true_result == result){
					if(true_result == 1)
						re_correct++;
					else
						irre_correct++;
				} else{
					if(true_result == 1)
						re_wrong++;
					else
						irre_wrong++;
				}

				index++;
			}

			System.out.println(decision_tree);
			System.out.println("re_correct: "+re_correct+" re_wrong: "+re_wrong);
			System.out.println("irre_correct: "+irre_correct+" irre_wrong: "+irre_wrong);
			System.out.println("correct: "+ (irre_correct+re_correct) +" wrong: " + (irre_wrong+re_wrong) );

			System.out.println("*******final_result******");
			System.out.println(test_qid_scoredocid_pair.size());
			System.out.println(train_qid_scoredocid_pair.size());
			for(String q_id : test_qid_scoredocid_pair.keySet()){
				TreeSet<Pair> pairs = test_qid_scoredocid_pair.get(q_id);
				int rank = 1;
				for(Pair pair : pairs){
					String record = q_id+SPACE+"Q0"+SPACE+pair.getDocid()+SPACE+rank+SPACE+pair.getResult()+SPACE+"Exp"+ENTER;
					FileWriter_Helper.appendToFile("ML_test", record);

					System.out.print(record);
					rank++;
				}
			}

			for(String q_id : train_qid_scoredocid_pair.keySet()){
				TreeSet<Pair> pairs = train_qid_scoredocid_pair.get(q_id);
				int rank = 1;
				for(Pair pair : pairs){
					String record = q_id+SPACE+"Q0"+SPACE+pair.getDocid()+SPACE+rank+SPACE+pair.getResult()+SPACE+"Exp" +ENTER;
					FileWriter_Helper.appendToFile("ML_train", record);

					System.out.print(record);
					rank++;
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void update_map(
			HashMap<String, TreeSet<Pair>> qid_scoredocid_pair,
			String q_id, String doc_id, double score) {
		TreeSet<Pair> pairs;

		Pair pair = new Pair(doc_id, score);
		
		if( qid_scoredocid_pair.containsKey(q_id) ){
			pairs = qid_scoredocid_pair.get(q_id);
		} else {
			pairs = new TreeSet<Pair>(Collections.reverseOrder());
			qid_scoredocid_pair.put(q_id, pairs);
		}
		
		pairs.add(pair);

		if(pairs.size() > 1000){
			Pair smallest = pairs.last();
			pairs.remove(smallest);
		}
		
	}


	public static void main(String[] args) {
		WekaMachineLearning weka = new WekaMachineLearning();
		weka.read_data_matrix_with_id();
		weka.read_25queries(Constants.allQueris);
		System.out.println("Total number of queries: " + weka.allQueries_.size());

		weka.read_qrels(Constants.qrels);
		int index = 0;
		for (String queryNo : weka.queryNo_docno_relevance_.keySet()) {
			index += weka.queryNo_docno_relevance_.get(queryNo).size();
		}
		System.out.println("Total number of documents: " + index);
		
		trainingQueries_ = weka.read_training_testing_queryNo(Constants.trainingQueries);
		testingQueries_ = weka.read_training_testing_queryNo(Constants.testingQueries);
		
		weka.linear_regression();
	}

}
