package com.ir.retrievalmodels;

public class Proximity {

	/**
	 * 
	 * @param proximityConstant
	 * @param rangeOfWindow
	 * @param numOfContainTerms - the number of querying terms this document contains. For example, if the query is "Information Retrieval", and document_1 only contains "information", then the numOfContainTerms=1. If document_2 contains "Information" and "Retrieval" then numOfContainTerms=2
	 * @param lenOfDoc
	 * @param V
	 * @return
	 */
	public double Cal_Proxmity(double proximityConstant, int rangeOfWindow, int numOfContainTerms, int lenOfDoc, long V){		
		return (proximityConstant - rangeOfWindow) * numOfContainTerms / (lenOfDoc + V);
	}
}
