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
	
	public static void appendToFileWithNewLine(String filename, String string){
		try {
			FileWriter fw = new FileWriter(filename, true);
			BufferedWriter bw = new BufferedWriter(fw);
			
			bw.write(string);
			bw.write(System.getProperty("line.separator"));
			bw.flush();
			
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
