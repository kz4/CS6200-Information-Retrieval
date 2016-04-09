package com.ir.uti;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Parser {
	
	/**
	 * Returns List<File> given folder path exluding the readMePath
	 * @param folder
	 * @param readMePath
	 * @return
	 */
	public static List<File> getFiles(String folder, String readMePath) {
		File file = new File(folder);
		File[] fileArr = file.listFiles();		
		List<File> fileLst = removeElements(fileArr, new File(readMePath));
		return fileLst;
	}
	
	/**
	 * This method removes the File deleteMe from File[] input
	 * @param input - In our case, ap89_collection
	 * @param deleteMe - readme file
	 * @return
	 */
	public static List<File> removeElements(File[] input, File deleteMe) {
	    List<File> result = new LinkedList<File>();
	    
	    if (input == null)
	    	return result;

	    for(File item : input)
	        if(!deleteMe.equals(item))
	            result.add(item);

	    return result;
	}

	public static final String DOC = "<DOC>";
	public static final String DOCNO = "<DOCNO>";
	public static final String TEXT = "<TEXT>";
	public static final String TEXTEND = "</TEXT>";
	public static final String DOCEND = "</DOC>";

	public enum LineType {
		DOC, DOCNO, TEXT, TEXTEND, DOCEND
	}

}

