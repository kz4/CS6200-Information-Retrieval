package com.ir.retrievalmodels;

public class BM25 {

	/**
	 * 
	 * @param tf_wd - term frequency of term w in document d
	 * @param lengOfDoc - length of document d
	 * @param avgLengthOfDoc - average document length for the entire corpus
	 * @param D - total number of documents in the corpus
	 * @param dfw - number of documents which contain term w
	 * @param tf_wq - term frequency of term w in query q
	 * @param k1 - constants
	 * @param k2 - constants
	 * @param b - constants
	 * @return
	 */
	public double Cal_BM25(long tf_wd, double lengOfDoc, double avgLengthOfDoc, long D, int dfw, int tf_wq, double k1, double k2, double b){		
		return Math.log10((D + 0.5) / (dfw + 0.5)) * ((tf_wd + k1 * tf_wd)/(tf_wd + k1 * ((1 - b) + b * lengOfDoc / avgLengthOfDoc))) * ((tf_wq + k2 * tf_wq) / (tf_wq + k2));
	}
}
