package com.ir.util;

import java.io.File;

public class Constants {
	
	public static final String newline = System.getProperty("line.separator");

	public static final String sampleWekaData = getAbsPath() + "SampleWekaData";
	public static final String iris = getAbsPath() + "SampleWekaData/iris.arff";
	public static final String breastCancer = getAbsPath() + "SampleWekaData/breast-cancer.arff";
	public static final String diabetes = getAbsPath() + "SampleWekaData/diabetes.arff";
	
	public static final String data_matrix = getAbsPath() + "data_matrix.arff";
	public static final String data_matrix_with_queryNo_docno = getAbsPath() + "data_matrix_with_id.arff";
	public static String qrels = getAbsPath() + "AP_DATA/qrels.adhoc.51-100.AP89.txt";
	public static String allQueris = getAbsPath() + "AP_DATA/query_desc.51-100.short.txt";

	public static String okapi = getAbsPath() + "HW1_Result/Okapi_Result.txt";
	public static String tfidf = getAbsPath() + "HW1_Result/TFIDF_Result.txt";
	public static String bm25 = getAbsPath() + "HW1_Result/BM25_Result.txt";
	public static String mercer = getAbsPath() + "HW1_Result/Mercer_Result.txt";
	public static String laplace = getAbsPath() + "HW1_Result/Laplace_Result.txt";

	public static String trainingQueries = getAbsPath() + "trainingQueries.txt";
	public static String testingQueries = getAbsPath() + "testingQueries.txt";
	
	public static String getAbsPath(){
		// Shows you the path of your Project Folder
		String absolutePath = new File(".").getAbsolutePath();    	
    	int last = absolutePath.length() - 1;
    	// Remove the dot at the end of path
    	absolutePath = absolutePath.substring(0, last);
		return absolutePath;
	}
	
	public static void main(String[] args) {
		System.out.println(data_matrix);
	}
}
