package com.ir.tokenizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
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
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.index.MergePolicy.MergeAbortedException;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.tartarus.snowball.ext.PorterStemmer;

import com.ir.uti.Constants;
import com.ir.uti.Parser;
import com.ir.uti.TermId_DocId_Posn;


// Not writing any catalog files until after merging
// Also fixed term_indexId_lstOfTermStartLength_
public class Tokenizer6 {

	int termId_ = -1;
	int docId_ = 0;

	// Keep track a set of unique terms
	Set<String> terms_ = new HashSet<String>();

	// Map of docId and docno
	Map<Integer, String> docId_docno_ = new HashMap<Integer, String>();

	Set<String> stopWords_ = readStoplist(Constants.stoplistPath);

	// Map of docId and length of document
	Map<Integer, Integer> docId_LenOfDoc_ = new HashMap<Integer, Integer>();

	public static void main(String[] args) {

		Tokenizer6 t = new Tokenizer6();

		t.read_data(Constants.ap89_collectionPath, indexes_model.no_stopwords);

		// Average document length for the entire corpus
		double avgLenDoc = t.calcAvgLenDoc();

		try {
			t.store_numOfDoc_vocabSize_avgDocLen(avgLenDoc);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Map<Integer, Integer> docId_LenOfDoc = t.getDocId_LenOfDoc();
		
		try {
			t.store_docId_lenOfDoc(docId_LenOfDoc);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void read_data(String folderPath, indexes_model model){

		List<File> files = Parser.getFiles(folderPath, Constants.readmePath);

		// In fact the folder contains 366 files, the first one is ds.store
		// After calling Parser.getFiles, we remove the readme
		// The 1st file is .ds_store, so the counter starts at 1
		int counter = 1;
		//int docId = 1;
		//docId_++;
		// We are reading 15 files from the ap89 collection a time, which contains 365 files
		int numOfFilesToReadOneTime = Constants.numOfFilesToReadOneTime;

		// Map of docId (0 ~ 84677), docno (e.g. "AP891220-0001")
		Map<Integer, String> docId_docno = new HashMap<Integer, String>();

		// Map of termId (0 ~ 170000 ish), term (e.g. "alleg")
		//Map<Integer, String> termId_term = new HashMap<Integer, String>();

		int totalFiles = files.size();
		for (int j = 0 ; j < (totalFiles / numOfFilesToReadOneTime) + 1; j++){
			System.out.println("j: " + j);

			Map<String, Map<Integer, List<Integer>>> term_DocId_LstOfPosn_FifteenFiles = new HashMap<String, Map<Integer, List<Integer>>>();
			for (int i = 0 + j * numOfFilesToReadOneTime; i < numOfFilesToReadOneTime + j * numOfFilesToReadOneTime; i++){

				// 0 is .ds_store
				System.out.println("File (starts 1, ends 364, total 364): " + i);
				
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
					term_DocId_LstOfPosn_FifteenFiles = tokenize_one_doc(docno, docno_text.get(docno), indexes_model.original, term_DocId_LstOfPosn_FifteenFiles);
					System.out.println("tokenized: " + docno);

					//docId++;
					docId_++;
				}





				counter++;
				if (counter == 365)
					break;
			}

			System.out.println("Print out a index and a catalog");

			// When you create a treemap from a hashmap, it's automatically sorted
			Map<String, Map<Integer, List<Integer>>> term_DocId_LstOfPosn_treeMap = new TreeMap<String, Map<Integer, List<Integer>>>(term_DocId_LstOfPosn_FifteenFiles);

			// Start write an inverted index
			createPartialInvertedIndexCatalog(term_DocId_LstOfPosn_treeMap, j);
		}

		System.out.println("Finished reading all documents");

	}

	private void createPartialInvertedIndexCatalog(Map<String, Map<Integer, List<Integer>>> term_DocId_LstOfPosn, int indexNo) {

		// First create a folder invertedindex on desktop
		// e.g. /Users/kaichenzhang/Desktop/invertedindex

		String fn = getIndexFnFromIndexId(indexNo);
		String fn_cat = getCatalogFnFromIndexId(indexNo);
		BufferedWriter out_index = null;
		BufferedWriter out_cat = null;

		int start = 0;
		int end = 0;

		try
		{   
			//Open the output index file.
			FileWriter ostream = new FileWriter(fn);
			out_index = new BufferedWriter(ostream);
			System.out.println("Writing out index: " + fn);
			StringBuilder block = new StringBuilder();

			//Open the output catalog file.
			FileWriter ostream_cat = new FileWriter(fn_cat);
			out_cat = new BufferedWriter(ostream_cat);
			StringBuilder block_cat = new StringBuilder();

			// Create this one term block to calculate the length of 
			// one term block in bytes, e.g:
			// car
			// 0 6 2 24 44 242 524 604 
			// 3 6 2 3 4 24 52 60 
			// 9 6 0 2 4 24 52 64 
			StringBuilder one_term_block = null;

			for (String term : term_DocId_LstOfPosn.keySet()) {
				// Write to the catalog file
				// starting position				
				start = end;
				block_cat.append(term + " " + start + " ");		//mark
				//Map<String, Map<Integer, List<Integer>>> term_indexId_lstOfTermStartLength = new HashMap<String, Map<Integer,List<Integer>>>();
				Map<Integer, List<Integer>> indexId_lstOfTermStartLength = new HashMap<Integer, List<Integer>>();
				List<Integer> lstOfPosn = new ArrayList<Integer>();
				lstOfPosn.add(start);

				block.append(term);
				block.append(Constants.newline_);
				one_term_block = new StringBuilder();
				one_term_block.append(term);
				one_term_block.append(Constants.newline_);


				for (int docId : term_DocId_LstOfPosn.get(term).keySet()) {
					block.append(docId + " ");
					block.append(term_DocId_LstOfPosn.get(term).get(docId).size() + " ");
					one_term_block.append(docId + " ");
					one_term_block.append(term_DocId_LstOfPosn.get(term).get(docId).size() + " ");
					int lst_length = term_DocId_LstOfPosn.get(term).get(docId).size();
					int counter = 0;
					for (int posn : term_DocId_LstOfPosn.get(term).get(docId)) {						
						block.append(posn);												
						one_term_block.append(posn);

						if (counter < lst_length - 1){
							block.append(" ");
							one_term_block.append(" ");
						}
						counter++;
					}
					block.append(Constants.newline_);
					one_term_block.append(Constants.newline_);

					//indexId_lstOfTermStartLength.put(indexNo, lstOfPosn);
					//					if (term_indexId_lstOfTermStartLength_.containsKey(term))
					//						term_indexId_lstOfTermStartLength_.get(term).put(indexNo, lstOfPosn);
					//					else{
					//						indexId_lstOfTermStartLength.put(indexNo, lstOfPosn);
					//						term_indexId_lstOfTermStartLength_.put(term, indexId_lstOfTermStartLength);
					//					}
				}

				// The length of the one term block
				int byteLength_diff = one_term_block.toString().getBytes().length;

				end = start + byteLength_diff;

				block_cat.append(byteLength_diff);	//mark
				block_cat.append(Constants.newline_);
				lstOfPosn.add(byteLength_diff);

				//				if (term_indexId_lstOfTermStartLength_.containsKey(term))
				//					//term_indexId_lstOfTermStartLength_.get(term).put(key, value);
				//				term_indexId_lstOfTermStartLength_.put(term, indexId_lstOfTermStartLength);

				// Before writing to the next term block, set the
				// start bytes of new term to the end bytes of the 
				// old term
				start = end; 

			}
			String one_map = block.toString();
			out_index.write(one_map);

			// Write to the catalog file
			String catStr = block_cat.toString();
			out_cat.write(catStr);
		}
		catch(Exception e)
		{
			System.out.println(e);
		}
		finally {
			try {
				out_index.flush(); 
				out_cat.flush(); 
				System.out.println("Done with creating partial inverted indexes and catalogs");
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				out_index.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * @param path - Path to the file that will be read
	 * @param start - starting position in byte of the term that needs to be read
	 * @param len - = (end - start) in byte
	 * @return The term, docId, term frequency and positions in the documents
	 * For example:
	 * car
			0 6 2 24 44 242 524 604 
			3 6 2 3 4 24 52 60 
			9 6 0 2 4 24 52 64 

	 */
	private String readFileByBytes(String path, int start, int len) {

		//create file object
		File file = new File(path);

		// For example:
		//			car
		//			0 6 2 24 44 242 524 604 
		//			3 6 2 3 4 24 52 60 
		//			9 6 0 2 4 24 52 64 
		String termBlock = "";

		try
		{
			//create FileInputStream object
			FileInputStream fin = new FileInputStream(file);
			System.out.println("In catalog file, trying to read: " + file);

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


	/**
	 * Read one line at a time to parse the corpus
	 * @param file - every file in the ap89_collection
	 * @return
	 */
	private static Map<String, String> read_one_file(File file, indexes_model model) {
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

	/**
	 * Given a document, tokenize the document into a Map<term, Map<docId, List<posn>>>
	 * @param docno - E.g. "AP890115-0078"
	 * @param text - Actual text inside that document
	 * @param model - One of the four index models, stem/stoplist
	 * @param term_DocId_LstOfPosn - Map<term, Map<docId, List<posn>>>
	 * @return
	 */
	public Map<String, Map<Integer, List<Integer>>> tokenize_one_doc(String docno, String text, indexes_model model, Map<String, Map<Integer, List<Integer>>> term_DocId_LstOfPosn){

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
		//int term = -100;
		String newTerm = "";
		//		int docId = -100;
		while (m.find()) {

			// Find a next matching term by the group method and make it lower case
			newTerm = m.group().toLowerCase();

			//docId_++;
			posn++;
			if (term_DocId_LstOfPosn.containsKey(newTerm)){

				// If we have seen the term before
				// 1) Get its termId using the term_termId_
				// 2) Use its docId, as a key of docId_LstOfPosn to find its lstOfPosn, and add a new position (posn)
				//termId = term_termId_.get(newTerm);

				// When you are reading the same document, you can just get its docId_
				// and append the posn to the position list
				if (term_DocId_LstOfPosn.get(newTerm).containsKey(docId_)){
					term_DocId_LstOfPosn.get(newTerm).get(docId_).add(posn);
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
					term_DocId_LstOfPosn.get(newTerm).put(docId_, lstOfPosn);
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
				//				term = termId_;
				//
				//				termId_term_.put(term, newTerm);
				//				term_termId_.put(newTerm, term);

				docId_LstOfPosn = new HashMap<Integer, List<Integer>>();
				lstOfPosn = new ArrayList<Integer>();
				lstOfPosn.add(posn);

				docId_LstOfPosn.put(docId_, lstOfPosn);

				term_DocId_LstOfPosn.put(newTerm, docId_LstOfPosn);								
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

		int lenOfDoc = posn + 1;
		docId_LenOfDoc_.put(docId_, lenOfDoc);
		return term_DocId_LstOfPosn;
	}

	/**
	 * 
	 * @return - Average length of 84678 documents
	 */
	private double calcAvgLenDoc(){
		double totalLenOfDoc = 0;
		for (int docId : docId_LenOfDoc_.keySet()) {
			totalLenOfDoc += docId_LenOfDoc_.get(docId);
		}
		
		int D = docId_;

		return totalLenOfDoc / D;
	}
	
	/**
	 * 
	 * @return - Map<documentId (0~84677), length of document>
	 */
	private Map<Integer, Integer> getDocId_LenOfDoc() {
		return docId_LenOfDoc_;
	}

	private String getIndexFnFromIndexId(int indexNo) {
		return Constants.index_base + indexNo + ".txt";
	}

	private String getCatalogFnFromIndexId(int indexNo){
		return Constants.catalog_base + indexNo + ".txt";
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

	/**
	 * Given one term and returns the porter stemmed term
	 * @param term - A string that needs to be stemmed
	 * @return
	 */
	public String porterStemForTerm(String term) {
		PorterStemmer stem = new PorterStemmer();
		stem.setCurrent(term);
		stem.stem();
		String stemmedTerm = stem.getCurrent();

		return stemmedTerm;
	}

	/**
	 * Read the stop list file to a set of stop words
	 * @param stoplistPath
	 * @return
	 */
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


	private void store_numOfDoc_vocabSize_avgDocLen(double avgLenDoc) throws IOException {

		BufferedWriter out = null;

		FileWriter ostream = new FileWriter(Constants.numOfDoc_vocabSize_avgDocLen_Path);
		out = new BufferedWriter(ostream);
		

		// Total number of documents in the corpus
		int D = docId_;

		// V is the vocabulary size – the total number of unique terms in the collection
		int V = terms_.size();
		
		// avg(len(d)) is the average document length for the entire corpus
		avgLenDoc = Math.round(avgLenDoc * 100);
		avgLenDoc = avgLenDoc/100;

		out.write(D + "");
		out.write(Constants.newline_);
		out.write(V + "");
		out.write(Constants.newline_);
		out.write(avgLenDoc + "");
		out.write(Constants.newline_);

		out.flush();
		out.close();
	}
	

	private void store_docId_lenOfDoc(Map<Integer, Integer> docId_lenOfDoc) throws IOException {
		BufferedWriter out = null;

		FileWriter ostream = new FileWriter(Constants.docId_lenOfDoc_Path);
		out = new BufferedWriter(ostream);
		
		Map<Integer, Integer> docId_LenOfDoc_treemap = new TreeMap<Integer, Integer>(docId_lenOfDoc);
		
		for (int docId : docId_LenOfDoc_treemap.keySet()) {
			out.write(docId + " " + docId_LenOfDoc_treemap.get(docId));
			out.write(Constants.newline_);
		}
		
		out.flush();
		out.close();
	}

	public enum indexes_model {
		original, no_stopwords, stemmed, no_stopwords_stemmed
	}

}
