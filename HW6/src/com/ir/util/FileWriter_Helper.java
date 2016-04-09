package com.ir.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class FileWriter_Helper {

	public static void writeToFile(String filename, String string){
		try {
			 FileWriter fw = new FileWriter(filename);
			 BufferedWriter bw = new BufferedWriter(fw);
			        
			 bw.write(string);
			 bw.flush();
			    
			 bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void appendToFile(String filename, String string){
		try {
			FileWriter fw = new FileWriter(filename, true);
			BufferedWriter bw = new BufferedWriter(fw);
			
			bw.write(string);
			bw.flush();
			
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		FileWriter_Helper fw = new FileWriter_Helper();
		fw.appendToFile(Constants.data_matrix_with_queryNo_docno, "@RELATION ML");
		fw.appendToFile(Constants.data_matrix_with_queryNo_docno, Constants.newline);			
		fw.appendToFile(Constants.data_matrix_with_queryNo_docno, "@ATTRIBUTE OKAPI_TF NUMERIC");
		fw.appendToFile(Constants.data_matrix_with_queryNo_docno, Constants.newline);			
		fw.appendToFile(Constants.data_matrix_with_queryNo_docno, "@ATTRIBUTE TF_IDF NUMERIC");
		fw.appendToFile(Constants.data_matrix_with_queryNo_docno, Constants.newline);			
		fw.appendToFile(Constants.data_matrix_with_queryNo_docno, "@ATTRIBUTE OKAPI_BM25 NUMERIC");
		fw.appendToFile(Constants.data_matrix_with_queryNo_docno, Constants.newline);			
		fw.appendToFile(Constants.data_matrix_with_queryNo_docno, "@ATTRIBUTE LM_LAPLACE NUMERIC");
		fw.appendToFile(Constants.data_matrix_with_queryNo_docno, Constants.newline);			
		fw.appendToFile(Constants.data_matrix_with_queryNo_docno, "@ATTRIBUTE LM_JM NUMERIC");
		fw.appendToFile(Constants.data_matrix_with_queryNo_docno, Constants.newline);			
		fw.appendToFile(Constants.data_matrix_with_queryNo_docno, "@ATTRIBUTE label NUMERIC");
		fw.appendToFile(Constants.data_matrix_with_queryNo_docno, Constants.newline);			
		fw.appendToFile(Constants.data_matrix_with_queryNo_docno, "@DATA");
		fw.appendToFile(Constants.data_matrix_with_queryNo_docno, Constants.newline);			
	}
}
