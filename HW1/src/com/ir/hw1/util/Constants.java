package com.ir.hw1.util;

import java.io.File;

public class Constants {

	public static final String configPath = getAbsPath() + "config/elasticsearch.yml";
	public static final String ap89_collectionPath = getAbsPath() + "AP_DATA/ap89_collection";
	public static final String stoplistPath = getAbsPath() + "AP_DATA/stoplist.txt";
	public static final String queryPath = getAbsPath() + "AP_DATA/query_desc.51-100.short.txt";
	public static final String readmePath = getAbsPath() + "AP_DATA/ap89_collection/readme";

	public static final String okapiResultOutputPath = "Okapi_Result.txt";
	public static final String TFIDFResultOutputPath = "TFIDF_Result.txt";
	public static final String BM25ResultOutputPath = "BM25_Result.txt";
	public static final String LaplaceResultOutputPath = "Laplace_Result.txt";
	public static final String MercerResultOutputPath = "Mercer_Result.txt";
	public static final String MetaResultOutputPath = "Metasearch_Result.txt";
	
	public static final double k1 = 1.2;
	public static final double k2 = 1.5;
	public static final double b = 0.75;
	public static final double lambda = 0.99;
	
	public static String getAbsPath(){
		// Shows you the path of your Project Folder
		String absolutePath = new File(".").getAbsolutePath();    	
    	int last = absolutePath.length() - 1;
    	// Remove the dot at the end of path
    	absolutePath = absolutePath.substring(0, last);
		return absolutePath;
	}
}
