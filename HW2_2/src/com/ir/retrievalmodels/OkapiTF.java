package com.ir.retrievalmodels;

public class OkapiTF {

	/**
	 * 
	 * @param tf - term frequency of term w in document d
	 * @param lengOfDoc - length of document d
	 * @param avgLengthOfDoc - average document length for the entire corpus
	 * @return
	 */
	public double Cal_OkapiTF(long tf, double lengOfDoc, double avgLengthOfDoc){		
		return (tf/(tf + 0.5 + 1.5 * (lengOfDoc / avgLengthOfDoc)));
	}

}
