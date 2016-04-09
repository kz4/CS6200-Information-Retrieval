package com.ir.uti;

import java.io.File;

import com.ir.tokenizer.Indexer.indexes_model;
import com.ir.tokenizer.Indexer;

public class Constants {

	public static final indexes_model indexModel = indexes_model.no_stopwords;

	public static final String configPath = getAbsPath() + "config/elasticsearch.yml";
	public static final String ap89_collectionPath = getAbsPath() + "AP_DATA/ap89_collection";
	
	// For testing purpose
//	public static final String ap89_collectionPath = "/Users/kaichenzhang/Desktop/" + "ap89_collection";
	public static final String stoplistPath = getAbsPath() + "AP_DATA/stoplist.txt";
	public static final String queryPath = getAbsPath() + "AP_DATA/query_desc.51-100.short.txt";
	public static final String readmePath = getAbsPath() + "AP_DATA/ap89_collection/readme";

	public static final String okapiResultOutputPath = "/Users/kaichenzhang/Desktop/Okapi_Result.txt";
	public static final String TFIDFResultOutputPath = "/Users/kaichenzhang/Desktop/TFIDF_Result.txt";
	public static final String BM25ResultOutputPath = "/Users/kaichenzhang/Desktop/BM25_Result.txt";
	public static final String LaplaceResultOutputPath = "/Users/kaichenzhang/Desktop/Laplace_Result.txt";
	public static final String MercerResultOutputPath = "/Users/kaichenzhang/Desktop/Mercer_Result.txt";
	public static final String MetaResultOutputPath = "/Users/kaichenzhang/Desktop/Metasearch_Result.txt";
	public static final String ProximityResultOutputPath = "/Users/kaichenzhang/Desktop/Proximity_Result.txt";

	
	public static final double k1 = 1.2;
	public static final double k2 = 1.5;
	public static final double b = 0.75;
	public static final double lambda = 0.99;
	public static final double proximityConstant = 1500;

	public static final int numOfFilesToReadOneTime = 15;
	
	public static final String index_base = "/Users/kaichenzhang/Desktop/invertedindex/index";
	public static final String catalog_base = "/Users/kaichenzhang/Desktop/catalog/cat";
	public static final String newline_ = System.getProperty("line.separator");
	
	public static final String finalIndexPath_ = "/Users/kaichenzhang/Desktop/finalIndex.txt";
	public static final String finalCatalogPath_ = "/Users/kaichenzhang/Desktop/finalCatalog.txt";
	
	public static final String numOfDoc_vocabSize_avgDocLen_Path = "/Users/kaichenzhang/Desktop/OtherInfo/DVAvg.txt";
	public static final String docId_lenOfDoc_Path = "/Users/kaichenzhang/Desktop/OtherInfo/docId_lenOfDoc.txt";
	public static final String docId_docno_Path = "/Users/kaichenzhang/Desktop/OtherInfo/docId_docno.txt";
	
	// 365 / 15 = 24.3
	public static final int numOfPartialIndexes = 25;	
	
	public static String getAbsPath(){
		// Shows you the path of your Project Folder
		String absolutePath = new File(".").getAbsolutePath();    	
    	int last = absolutePath.length() - 1;
    	// Remove the dot at the end of path
    	absolutePath = absolutePath.substring(0, last);
		return absolutePath;
	}
	
	public static void main(String[] args) {
		
		System.out.println(numOfPartialIndexes);
	}
}
