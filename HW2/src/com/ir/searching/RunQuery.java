package com.ir.searching;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ir.uti.Constants;
import com.ir.uti.Term_Offset_Len;

public class RunQuery {

	// D is the total number of documents in the corpus
	public long D_ = 0;

	// V is the vocabulary size – the total number of unique terms in the collection
	public long V_ = 0;

	// avg(len(d)) is the average document length for the entire corpus
	public double avgLengthOfDoc_ = 0;

	// Map<term, List<starting byte in final index, ending byte in final index>>
	public Map<String, List<Integer>> term_start_end_ = new HashMap<String, List<Integer>>();
	
	public Map<Integer, Integer> docId_lenOfDoc_ = new HashMap<Integer, Integer>();

	public RunQuery(){
		
		System.out.println("Started retrieving total number of documents in the corpus, vacabulary size and average length of document");
		retrieve_D_V_avgLenD();
		System.out.println("Finished retrieving total number of documents in the corpus, vacabulary size and average length of document");
		
		System.out.println("Started reading final catalog file into local cashe");
		read_final_catalog();
		System.out.println("Finished reading final catalog file into local cashe");
		
		System.out.println("Started reading docId, length of document map into local cashe");
		read_docId_lenOfDoc();
		System.out.println("Finished reading docId, length of document map into into local cashe");
	}

	/**
	 * Retrieve D, V and avg(len(d))
	 */
	private void retrieve_D_V_avgLenD(){
		String line = null;
		BufferedReader br = null;
		List<String> lines = new ArrayList<String>();

		try {
			br = new BufferedReader(new FileReader(Constants.numOfDoc_vocabSize_avgDocLen_Path));
			line = br.readLine();
			lines.add(line);

			while (line != null) {
				line = br.readLine();
				lines.add(line);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		D_ = Long.parseLong(lines.get(0));
		V_ = Long.parseLong(lines.get(1));
		avgLengthOfDoc_ = Double.parseDouble(lines.get(2));
	}

	/**
	 * Read the final catalog file into a HashMap term_start_end_
	 */
	private void read_final_catalog(){
		String line = null;
		BufferedReader br = null;
		List<Integer> bytesPosn = new ArrayList<Integer>();

		try {
			br = new BufferedReader(new FileReader(Constants.finalCatalogPath_));
			line = br.readLine();
			String[] term_start_end = line.split("\\s+");
			bytesPosn.add(Integer.parseInt(term_start_end[1]));
			bytesPosn.add(Integer.parseInt(term_start_end[2]));
			term_start_end_.put(term_start_end[0], bytesPosn);		

			while (line != null) {
				List<Integer> bytesPosn2 = new ArrayList<Integer>();
				line = br.readLine();
				if (line == null)
					break;
				String[] term_start_end2 = line.split("\\s+");
				bytesPosn2.add(Integer.parseInt(term_start_end2[1]));
				bytesPosn2.add(Integer.parseInt(term_start_end2[2]));
				term_start_end_.put(term_start_end2[0], bytesPosn2);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns the map of docId and length of corresponding document length
	 */
	private Map<Integer, Integer> read_docId_lenOfDoc() {

		String line = null;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(Constants.docId_lenOfDoc_Path));
			line = br.readLine();

			String[] docId_lenOfDoc_array = line.split("\\s+");
			docId_lenOfDoc_.put(Integer.parseInt(docId_lenOfDoc_array[0]), Integer.parseInt(docId_lenOfDoc_array[1]));

			while (line != null) {
				line = br.readLine();
				if (line == null)
					break;
				String[] docId_lenOfDoc_array2 = line.split("\\s+");
				docId_lenOfDoc_.put(Integer.parseInt(docId_lenOfDoc_array2[0]), Integer.parseInt(docId_lenOfDoc_array2[1]));
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return docId_lenOfDoc_;
	}
	
	/**
	 * Given a term and an empty map, return its corresponding populated term map
	 * @param term - e.g. "alleg"
	 * @param term_docId_lstOfPosn - Map<term, Map<docId, List<posn>>>
	 * @return
	 */
	public Map<String, Map<Integer, List<Integer>>> getDocId_lstOfPosn_one_term(String term,
			Map<String, Map<Integer, List<Integer>>> term_docId_lstOfPosn) {

		Map<Integer, List<Integer>> docId_tf_one_term = new HashMap<Integer, List<Integer>>();
		String termBlock = "";

		if (term_start_end_.get(term) != null){
			int offset = term_start_end_.get(term).get(0);
			int len = term_start_end_.get(term).get(1); 
			termBlock = readFileByBytes(Constants.finalIndexPath_, offset, len);
			docId_tf_one_term = parseTermBlockTo_docId_lstOfPosn(termBlock);
			term_docId_lstOfPosn.put(term, docId_tf_one_term);
		}
		
		return term_docId_lstOfPosn;
	}

	/**
	 * Returns docId, list of positions map given a termblock
	 * @param termBlock
	 * @return
	 */
	private Map<Integer, List<Integer>> parseTermBlockTo_docId_lstOfPosn(String termBlock) {
		
		Map<Integer, List<Integer>> docId_lstOfPosn = new HashMap<Integer, List<Integer>>();
		
		// Since the first line is term, we want to the block from 2nd line
		String[] lines = termBlock.split(Constants.newline_, 2);

		// We want to read each line now
		String[] linesWithoutTerm = lines[1].split(Constants.newline_);

		for (String line : linesWithoutTerm) {
			String[] docId_tf_posns = line.split("\\s+");
			int totalTokens = docId_tf_posns.length;
			// docId_tf_posns[0] is the docId
			// docId_tf_posns[1] is the term frequency
			// docId_tf_posns[i] i > 1 is the list of positions	
			if (line.equals(""))
				break;
			List<Integer> lstOfPosn = new ArrayList<Integer>();
			int i = 2;
			while(!docId_tf_posns[i].equals("")){
				lstOfPosn.add(Integer.parseInt(docId_tf_posns[i]));
				i++;
				if (i == totalTokens)
					break;
			}
			docId_lstOfPosn.put(Integer.parseInt(docId_tf_posns[0]), lstOfPosn);
		}

		return docId_lstOfPosn;
	}

	public long getVocabularySize(){
		return V_;
	}

	public double getAvgLengthOfDoc() {
		return avgLengthOfDoc_;
	}

	public long getTotalNumOfDoc() {
		return D_;
	}
	
	public Map<Integer, Integer> getDocId_lenOfDoc(){
		return docId_lenOfDoc_;
	}

	/**
	 * For testing purpose
	 * @param args
	 */
	public static void main(String[] args) {

		RunQuery rq = new RunQuery();
		//		System.out.println(rq.getTotalNumOfDoc());
		//		System.out.println(rq.getVocabularySize());
		//		System.out.println(rq.avgLengthOfDoc_);

		//		Map<Integer, Integer> docId_tf = rq.getDocId_tf_one_term("zyl");

		// For debugging purpose
		Map<String, Map<Integer, List<Integer>>> term_docId_lstOfPosn = new HashMap<String, Map<Integer,List<Integer>>>();
		term_docId_lstOfPosn = rq.getDocId_lstOfPosn_one_term("alleg", term_docId_lstOfPosn);
		System.out.println(term_docId_lstOfPosn.size());
		Map<Integer, List<Integer>> res = term_docId_lstOfPosn.get("alleg");
		System.out.println(term_docId_lstOfPosn.get("alleg").size());
		System.out.println(term_docId_lstOfPosn);
//		Map<Integer, Integer> m = rq.read_docId_lenOfDoc();
	}

	public Map<Integer, Integer> getDocId_tf_one_term(String term) {

		Map<Integer, Integer> docId_tf_one_term = new HashMap<Integer, Integer>();
		String termBlock = "";

		if (term_start_end_.get(term) != null){
			int offset = term_start_end_.get(term).get(0);
			int len = term_start_end_.get(term).get(1); 
			termBlock = readFileByBytes(Constants.finalIndexPath_, offset, len);
			docId_tf_one_term = parseTermBlock(termBlock);
		}
		return docId_tf_one_term;
	}

	/**
	 * Given:
	 * zyl
		54181 3 194 240 504
		22985 3 30 135 177
		Returns:
		{Map<54181, 3>, Map<22985, 3>}
	 * @param termBlock
	 * @return
	 */
	private Map<Integer, Integer> parseTermBlock(String termBlock) {
		Map<Integer, Integer> docId_tf = new HashMap<Integer, Integer>();

		// Since the first line is term, we want to the block from 2nd line
		String[] lines = termBlock.split(Constants.newline_, 2);

		// We want to read each line now
		String[] linesWithoutTerm = lines[1].split(Constants.newline_);

		for (String line : linesWithoutTerm) {
			String[] docId_tf_posns = line.split("\\s+");
			docId_tf.put(Integer.parseInt(docId_tf_posns[0]), Integer.parseInt(docId_tf_posns[1]));
		}

		return docId_tf;
	}

	private String readFileByBytes(String path, int start, int len) {
		File file = new File(path);

		// For example:
		//		car
		//		0 6 2 24 44 242 524 604 
		//		3 6 2 3 4 24 52 60 
		//		9 6 0 2 4 24 52 64 
		String termBlock = "";

		FileInputStream fin = null;
		try
		{
			//create FileInputStream object
			fin = new FileInputStream(file);

			/*
			 * To skip n bytes while reading the file, use
			 * int skip(int nBytes) method of Java FileInputStream class.
			 *
			 * This method skip over n bytes of data from stream. This method returns
			 * actual number of bytes that have been skipped.
			 */
			//skip first start bytes
			fin.skip(start);
			byte[] res = new byte[len];

			// second argument is the offset for res, so in our case always 0
			fin.read(res, 0, len);
			termBlock = new String(res, "UTF-8");
			fin.close();
		}
		catch(FileNotFoundException e)
		{
			System.out.println("File not found" + e);
		}
		catch(IOException ioe)
		{
			System.out.println("Exception while reading the file " + ioe);
		}
		return termBlock;
	}
}
