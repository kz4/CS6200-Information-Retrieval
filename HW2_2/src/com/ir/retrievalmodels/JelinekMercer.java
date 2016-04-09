package com.ir.retrievalmodels;

public class JelinekMercer {

	public double Cal_JelinekMercer(long tf_wd, double lenthOfDoc, double sumOfRestOf_tf_wd, double sumOfRestOf_lengthOfDoc, double lambda){		
		return Math.log10(lambda * tf_wd / lenthOfDoc + (1 - lambda) * sumOfRestOf_tf_wd / sumOfRestOf_lengthOfDoc);
	}
}
