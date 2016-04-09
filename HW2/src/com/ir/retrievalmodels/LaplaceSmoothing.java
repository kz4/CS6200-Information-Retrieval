package com.ir.retrievalmodels;

public class LaplaceSmoothing {

	/**
	 * 
	 * @param tf_wd - term frequency of term w in document d
	 * @param lengOfDoc - length of document d
	 * @param V - vocabulary size – the total number of unique terms in the collection.
	 * @return
	 */
	public double Cal_Laplace(long tf_wd, double lengOfDoc, long V){		
		return Math.log10((tf_wd + 1) / (lengOfDoc + V));
	}
}
