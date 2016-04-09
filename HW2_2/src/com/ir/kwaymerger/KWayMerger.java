package com.ir.kwaymerger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.commons.lang3.StringUtils;

import com.ir.uti.Catalog;
import com.ir.uti.Constants;
import com.ir.uti.Term_Offset_Len;


public class KWayMerger
{	
	public static void merge(List<List<Term_Offset_Len>> sortedCatalogs) throws IOException {

		int sortedListsize = sortedCatalogs.size();
		
		// fileindex, index, term
		Queue<Catalog> queue = new PriorityQueue<Catalog>(sortedListsize);
		
		// Initialization, fill 25 nodes with the first term block from
		// the 25 partial catalogs
		// counter is the name of the list, that goes from 0 to 24
		// index is the position of the term in the list of blocks
		// term is current term that is polled
		int catNo = 0;
		int index = 0;
		for (List<Term_Offset_Len> catalog : sortedCatalogs) {
			queue.add(new Catalog(catNo, index, catalog.get(index).term_));
			catNo++;
		}
		
		BufferedWriter out_index = null;
		BufferedWriter out_cat = null;
		
		FileWriter ostream = new FileWriter(Constants.finalIndexPath_);
		out_index = new BufferedWriter(ostream);
		
		FileWriter ostream_cat = new FileWriter(Constants.finalCatalogPath_);
		out_cat = new BufferedWriter(ostream_cat);
		
		int finalCatCurrentPosition = 0;
		
		String currentTermFromCatalog = "";
		List<String> docId_tf_lstOfPosn = new ArrayList<String>();
		// process until queue becomes empty
		while (!queue.isEmpty()) {
			
			// Retrieve the min term from the min heap (Priority queue)
			Catalog entry = queue.poll();
			catNo = entry.catName_;
			index = entry.index_;
			String term = sortedCatalogs.get(catNo).get(index).term_;
			int offset = sortedCatalogs.get(catNo).get(index).offset_;
			int len = sortedCatalogs.get(catNo).get(index).len_;
			
			System.out.println(term);
			
			// catNo is the same as invertedIndexNo
			String partialInvertedIndexPath = Constants.index_base + catNo + ".txt";
			
			// Retrieve from partial index using the offset and len
			// and then add it to the arrayList
			String termBlock = readFileByBytes(partialInvertedIndexPath, offset, len);
			
			// For the first time, initialize the temp to the first
			// termblock of the catalog
			if(currentTermFromCatalog.equals("")){
				currentTermFromCatalog = entry.term_;								
				docId_tf_lstOfPosn.add(termBlock);
				
				//output.add(currentTermFromCatalog);
			} else if (currentTermFromCatalog.equals(entry.term_)){
				// Combine duplicated term's docID info
				// For example, the first termBlock is: 
				// "apple"
				// 2 1 5
				// 3 5 2 4 5 6 7
				// and the second term block is:
				// "apple"
				// 5 1 2
				// 7 2 3 6
				// We will have to cut the "apple" from the
				// second term block and combine the docId, tf, lstOfPosn 
				
//				String[] termBlockSplit = termBlock.split(Constants.newline_, 2);		
//				String termBlockWithoutTerm = termBlockSplit[1];
//				docId_tf_lstOfPosn.add(termBlockWithoutTerm);
				
				docId_tf_lstOfPosn.add(termBlock);
				
				//output.add(currentTermFromCatalog);
			} else {
				// This is the case when the current term isn't the same as previous term (temp)
				// Write temp (temp contains all the info for the previous term) to disk
				// and put the current entry into temp 
				
				String finalIndexTermBlock = StringUtils.join(docId_tf_lstOfPosn, "");
				int finalIndexTermBlockLength = finalIndexTermBlock.getBytes().length;
				out_index.write(finalIndexTermBlock);
				
				out_cat.write(currentTermFromCatalog + " " + finalCatCurrentPosition + " " + finalIndexTermBlockLength);
				out_cat.write(Constants.newline_);
				finalCatCurrentPosition += finalIndexTermBlockLength;
				
				docId_tf_lstOfPosn.clear();
				
				currentTermFromCatalog = entry.term_;
				docId_tf_lstOfPosn.add(termBlock);
			}
			
			if (index < sortedCatalogs.get(catNo).size() - 1){
				index++;
				// Add the next term from that catalog to the queue
				// Instead of instantiate a new try, a more efficient
				// way would be just set the current entry to the new index and term
				entry.index_ = index;
				entry.term_ = sortedCatalogs.get(catNo).get(index).term_;
				queue.add(entry);
			} 
		}
		
		// After the queue is all empty, but temp is not dumped into the disk yet
		if (docId_tf_lstOfPosn.size() > 0){
			// write currentTerm to the index file
			String finalIndexTermBlock = StringUtils.join(docId_tf_lstOfPosn, "");
			int finalIndexTermBlockLength = finalIndexTermBlock.getBytes().length;
			out_index.write(finalIndexTermBlock);

			
			out_cat.write(currentTermFromCatalog + " " + finalCatCurrentPosition + " " + finalIndexTermBlockLength);
			out_cat.write(Constants.newline_);
			out_cat.flush();
			out_index.flush();
			finalCatCurrentPosition += finalIndexTermBlockLength;
		}

		out_cat.close();
		out_index.close();
	}

	public static void main(String[] args) {
		
		KWayMerger k = new KWayMerger();
		
		List<List<Term_Offset_Len>> lists = k.read_all_partial_catalogs();

		try {
			merge(lists);
			System.out.println("Done with Merging");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private List<List<Term_Offset_Len>> read_all_partial_catalogs(){
		List<List<Term_Offset_Len>> res = new ArrayList<List<Term_Offset_Len>>();
		int j = 0;
		//List<File> files = Parser.getFiles("catalog/", null);
		for (int i = 0; i < Constants.numOfPartialIndexes; i++) {
			String fn =  Constants.catalog_base + j + ".txt";
			List<Term_Offset_Len> lst_term_offset_len = read_one_catalog_file(fn);
			res.add(lst_term_offset_len);
			j++;
		}
		
		return res;
	}
	
	private static List<Term_Offset_Len> read_one_catalog_file(String file) {
		
		List<Term_Offset_Len> lst_term_offset_len = new ArrayList<Term_Offset_Len>();
		String line = null;
		StringBuilder sb;
		BufferedReader br = null;
		try {

			br = new BufferedReader(new FileReader(file));
			sb = new StringBuilder();
			line = br.readLine();

			while (line != null) {
				String[] s = line.split("\\s+");
				Term_Offset_Len t = new Term_Offset_Len(s[0], Integer.parseInt(s[1]), Integer.parseInt(s[2]));
				lst_term_offset_len.add(t);
	            line = br.readLine();
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
		return lst_term_offset_len;
	}

	private static String readFileByBytes(String path, int start, int len) {
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
//			System.out.println("In catalog file, trying to read: " + file);
			
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
