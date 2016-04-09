package com.ir.tokenizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.tartarus.snowball.ext.PorterStemmer;

import com.ir.uti.Constants;
import com.ir.uti.Parser;
import com.ir.uti.TermId_DocId_Posn;

public class Tokenizer {

	int termId_ = -1;
	int docId_ = -1;

	// Keep track a set of unique terms
	Set<String> terms_ = new HashSet<String>();

	// Map of termId and term
	Map<Integer, String> termId_term_ = new HashMap<Integer, String>();
	Map<String, Integer> term_termId_ = new HashMap<String, Integer>();

	// Map of docId and docno
	Map<Integer, String> docId_docno_ = new HashMap<Integer, String>();
	
	Set<String> stopWords_ = readStoplist(Constants.stoplistPath);

	public static void main(String[] args) {

		Tokenizer t = new Tokenizer();

		t.read_data(Constants.ap89_collectionPath, indexes_model.no_stopwords);

	}

	public void read_data(String folderPath, indexes_model model){

		List<File> files = Parser.getFiles(folderPath, Constants.readmePath);

		// The folder contains only 365 files, so break at counter == 365
		int counter = 1;
		//int docId = 1;
		//docId_++;
		// We are reading 15 files from the ap89 collection a time, which contains 365 files
		int numOfFilesToReadOneTime = Constants.numOfFilesToReadOneTime;


		// Map of docId (0 ~ 84677), docno (e.g. "AP891220-0001")
		Map<Integer, String> docId_docno = new HashMap<Integer, String>();

		// Map of termId (0 ~ 170000 ish), term (e.g. "alleg")
		//Map<Integer, String> termId_term = new HashMap<Integer, String>();

		for (int j = 0 ; j < (files.size() / numOfFilesToReadOneTime) + 1; j++){
			System.out.println("j: " + j);
			
			Map<Integer, Map<Integer, List<Integer>>> TermId_DocId_Posn_Lst_FifteenFiles = new HashMap<Integer, Map<Integer, List<Integer>>>();
			for (int i = 0 + j * numOfFilesToReadOneTime; i < numOfFilesToReadOneTime + j * numOfFilesToReadOneTime; i++){

				System.out.println("File (starts 0, ends 364, total 365): " + i);

				Map<String, String> docno_text = read_one_file(files.get(counter), indexes_model.original);
				//				read_oneFile(files.get(counter), indexes_model.no_stopwords);
				//				read_oneFile(files.get(counter), indexes_model.stemmed);
				//				read_oneFile(files.get(counter), indexes_model.no_stopwords_stemmed);

				//Map<Integer, Map<Integer, List<Integer>>> termId_DocId_LstOfPosn = new HashMap<Integer, Map<Integer, List<Integer>>>();
				
				for (String docno : docno_text.keySet()) {
					//					docId_docno.put(docId, docno);
					docId_docno.put(docId_, docno);
					//Map<Integer, Map<Integer, List<Integer>>> TermId_DocId_Posn_Lst = tokenize_one_doc(docno, docno_text.get(docno), indexes_model.original);

					//TermId_DocId_Posn_Lst_FifteenFiles.put(key, value)
					TermId_DocId_Posn_Lst_FifteenFiles = tokenize_one_doc(docno, docno_text.get(docno), indexes_model.original, TermId_DocId_Posn_Lst_FifteenFiles);
					System.out.println("tokenized: " + docno);

					//docId++;
					docId_++;
				}

				counter++;
				if (counter == 366)
					break;
			}

			System.out.println("Print out a index and a catalog");
			
			// Start write an inverted index
			write_one_index_one_catalog(TermId_DocId_Posn_Lst_FifteenFiles, j);
		}
	}

	private void write_one_index_one_catalog(Map<Integer, Map<Integer, List<Integer>>> termId_DocId_Posn_Lst_FifteenFiles, int indexNo) {
		
		for (int termId : termId_DocId_Posn_Lst_FifteenFiles.keySet()) {
			
		
			String index_file = Constants.index_base + indexNo;
			String catalog_file = Constants.catalog_base + indexNo;
			
	    	BufferedWriter index_out = null;
	    	BufferedWriter catalog_out = null;
	    	try
	        {   
	            //Open the output file.
	            FileWriter index_ostream = new FileWriter(index_file);
	            index_out = new BufferedWriter(index_ostream);
	            FileWriter catalog_ostream = new FileWriter(catalog_file);
	            catalog_out = new BufferedWriter(catalog_ostream);
	            
	            
	        }
	        catch(Exception e)
	        {
	            System.out.println(e);
	        }
	    	finally {
	    		try {
					index_out.flush();
					catalog_out.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
	    		try {
					index_out.close();
					catalog_out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Read one line at a time to parse the corpus
	 * @param file - every file in the ap89_collection
	 * @return
	 */
	private static Map<String, String> read_one_file(File file, indexes_model model) {

		//List<XContentBuilder> lstBuilder = new ArrayList<XContentBuilder>();

		Map<String, String> docno_text = new HashMap<String, String>();
		String line = null;
		StringBuilder sb;
		try {
			BufferedReader br = null;
			br = new BufferedReader(new FileReader(file));
			sb = new StringBuilder();
			line = br.readLine();
			Boolean readingText = false;
			String docno = null;
			String text = "";

			while (line != null) {
				if (line.startsWith("<DOC>"))
					sb = new StringBuilder();
				else if (line.startsWith("<DOCNO>"))
					docno = line.split(" ")[1];
				else if (line.startsWith("<TEXT>"))
					readingText = true;
				else if (line.startsWith("</TEXT>"))
					readingText = false;
				else if (line.startsWith("</DOC>")){
					text = sb.toString();
					docno_text.put(docno, text);
				}
				else{
					if (readingText){
						//String result = line.replaceAll("[-+.^:,!?'~@#$%&*_=<>\"]","");
						//sb.append(result);

						sb.append(line);
						sb.append(System.lineSeparator());
					}
				}
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}	
		return docno_text;
	}

	public Map<Integer, Map<Integer, List<Integer>>> tokenize_one_doc(String docno, String text, indexes_model model, Map<Integer, Map<Integer, List<Integer>>> termId_DocId_LstOfPosn){
		
		String pattern = "\\w+(\\.?\\w+)*";
		Matcher m = Pattern.compile(pattern).matcher(text);

		// 1) Create a class TermId_DocId_Posn, every time you read a document, you create an instance of List<TermId_DocId_Posn>,
		// and then after you read 1000 documents, you will end up with List<List<TermId_DocId_Posn>>. And then you have to merge
		// it into Map<TermId, Map<DocId, List<Posn>>>. Then you can print it out as your inverted index for 1000 documents
		// BAD APPROACH
		// List<TermId_DocId_Posn> termId_DocId_Posn_Lst = new ArrayList<TermId_DocId_Posn>();

		// 2) Every time you read a document, you create an instance of Map<TermId, Map<DocId, List<Posn>>>,
		// then when you read a new document, if you have seen the term before, put it to the same TermId map;
		// otherwise create a new TermId map.  And then you can directly print it out as your inverted index for 1000 documents
		//Map<Integer, Map<Integer, List<Integer>>> termId_DocId_LstOfPosn = new HashMap<Integer, Map<Integer, List<Integer>>>();
		Map<Integer, List<Integer>> docId_LstOfPosn; // = new HashMap<Integer, List<Integer>>();
		List<Integer> lstOfPosn; // = new ArrayList<Integer>();

		// For every document

		int posn = -1;

		// The reason we have to keep this termId is because we might have seen this term
		// before. For example, "The car was in the car wash". "the" appears twice in
		// the sentence, so the second time we see it, we just want to use the existing
		// termId
		int termId = -100;
		String newTerm = "";
//		int docId = -100;
		while (m.find()) {

			// Find a next matching term by the group method and make it lower case
			newTerm = m.group().toLowerCase();

			//docId_++;
			posn++;
		if (termId_DocId_LstOfPosn.containsKey(newTerm)){
				
				// If we have seen the term before
				// 1) Get its termId using the term_termId_
				// 2) Use its docId, as a key of docId_LstOfPosn to find its lstOfPosn, and add a new position (posn)
				termId = term_termId_.get(newTerm);
				
				// When you are reading the same document, you can just get its docId_
				// and append the posn to the position list
				if (termId_DocId_LstOfPosn.get(termId).containsKey(docId_)){
					termId_DocId_LstOfPosn.get(termId).get(docId_).add(posn);
				} else {
					// This happens when you are reading a new document and see a
					// term exists in the previous documents, in this case a new
					// Map<docId_, lstOfPosn> has to be created and be put into
					// the termId map - termId_DocId_LstOfPosn
					docId_LstOfPosn = new HashMap<Integer, List<Integer>>();
					lstOfPosn = new ArrayList<Integer>();
					lstOfPosn.add(posn);
					
					docId_LstOfPosn.put(docId_, lstOfPosn);
					
					//termId_DocId_LstOfPosn.put(termId, docId_LstOfPosn);	// this line is wrong
					termId_DocId_LstOfPosn.get(termId).put(docId_, lstOfPosn);
				}
			} else {
				// If we haven't seen this term before
				// 1) Add it to the terms set
				// 2) Increment the termId_
				// 3) Put it to the termId_term_, term_termId_ maps
				// 4) Instantiate a lstOfPosn list to record the list of positions
				// 5) Add position posn to lstOfPosn
				// 6) docId_LstOfPosn put the docId_ and newly instantiated lstOfPosn
				// 7) termId_DocId_LstOfPosn put the new termId and docId_LstOfPosn
				terms_.add(newTerm);		

				termId_++;
				termId = termId_;

				termId_term_.put(termId, newTerm);
				term_termId_.put(newTerm, termId);
				
				docId_LstOfPosn = new HashMap<Integer, List<Integer>>();
				lstOfPosn = new ArrayList<Integer>();
				lstOfPosn.add(posn);
				
				docId_LstOfPosn.put(docId_, lstOfPosn);
				
				termId_DocId_LstOfPosn.put(termId, docId_LstOfPosn);								
			}
    
//			switch (model) {
//			case original:
//				TermId_DocId_Posn t = new TermId_DocId_Posn();
//				t.termId_ = termId;
//				t.docId_ = docId_; 
//				t.posn_ = posn;
//				termId_DocId_Posn_Lst.add(t);
//				break;
//			case no_stopwords:
//				if (stopWords.contains(newTerm))
//					continue;
//				TermId_DocId_Posn t2 = new TermId_DocId_Posn();
//				t2.termId_ = termId;
//				t2.docId_ = docId_; 
//				t2.posn_ = posn;
//				termId_DocId_Posn_Lst.add(t2);
//				break;
//			case stemmed:
//				//List<String> stemmedTokens = porterStem(tokens);
//				TermId_DocId_Posn t3 = new TermId_DocId_Posn();
//				t3.termId_ = termId;
//				t3.docId_ = docId_; 
//				t3.posn_ = posn;
//				termId_DocId_Posn_Lst.add(t3);
//				newTerm = porterStemForTerm(newTerm);
//				break;
//			case no_stopwords_stemmed:
//				if (stopWords.contains(newTerm))
//					continue;
//				TermId_DocId_Posn t4 = new TermId_DocId_Posn();
//				t4.termId_ = termId;
//				t4.docId_ = docId_; 
//				t4.posn_ = posn;
//				termId_DocId_Posn_Lst.add(t4);
//
//				newTerm = porterStemForTerm(newTerm);
//				break;				
//			default:
//				break;
//			}
		}
		return termId_DocId_LstOfPosn;
	}

	/*
	 * Returns the terms after porter stemming algorithm
	 * Didn't use it because PorterStem stems incursions to incurs while ElasticSearch stems it to incur
	 */
	public List<String> porterStemForLst(List<String> terms) {
		List<String> stemmedTerms = new ArrayList<String>();
		for (String term : terms) {
			PorterStemmer stem = new PorterStemmer();
			stem.setCurrent(term);
			stem.stem();
			String stemmedTerm = stem.getCurrent();

			stemmedTerms.add(stemmedTerm);
		}
		return stemmedTerms;
	}

	public String porterStemForTerm(String term) {
		PorterStemmer stem = new PorterStemmer();
		stem.setCurrent(term);
		stem.stem();
		String stemmedTerm = stem.getCurrent();

		return stemmedTerm;
	}

	public Set<String> readStoplist(String stoplistPath){

		Set<String> stoplist = new HashSet<String>();
		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(stoplistPath));
			String line = br.readLine();

			while (line != null) {
				stoplist.add(line);
				line = br.readLine();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return stoplist;		
	}

	public enum indexes_model {
		original, no_stopwords, stemmed, no_stopwords_stemmed
	}

}
