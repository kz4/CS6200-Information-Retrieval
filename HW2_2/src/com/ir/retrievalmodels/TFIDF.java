package com.ir.retrievalmodels;

public class TFIDF {

	/**
	 * 
	 * @param tf - term frequency of term w in document d
	 * @param lengOfDoc - length of document d
	 * @param avgLengthOfDoc - average document length for the entire corpus
	 * @param D - total number of documents in the corpus
	 * @param dfw - number of documents which contain term w
	 * @return
	 */
	public double Cal_TFIDF(long tf, double lengOfDoc, double avgLengthOfDoc, long D, int dfw){		
		return (tf/(tf + 0.5 + 1.5 * (lengOfDoc / avgLengthOfDoc))) * Math.log10(D / dfw);
	}

}
