package com.ir.retrievalmodels;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.commons.lang3.StringUtils;

import com.ir.searching.QueryExecution;
import com.ir.tokenizer.Posn_Entry;
import com.ir.uti.Catalog;
import com.ir.uti.Constants;
import com.ir.uti.Term_Offset_Len;

public class CalcMinWindow {

	public int merge(Map<String, List<Integer>> term_lstOfPosn) throws IOException {

		int sortedListsize = term_lstOfPosn.size();
		
		// fileindex, index, term
		Queue<Posn_Entry> queue = new PriorityQueue<Posn_Entry>(sortedListsize);
		
		// Suppose the total query terms is k
		// Initialization, fill k nodes with the 1st positions of every map
		// counter is the name of the list, that goes from 0 to k - 1
		// index is the position of the term in the list of blocks
		// term is current term that is polled
		//int term = 0;
		int index = 0;
		List<Integer> lst = new ArrayList<Integer>();
		for (String term : term_lstOfPosn.keySet()) {
			int posn = term_lstOfPosn.get(term).get(index);
			queue.add(new Posn_Entry(term, index, posn, false));
			//term++;
			lst.add(posn);
		}
		
		int minRangeOfWindow = Collections.max(lst) - Collections.min(lst);
		
		// If a listOfPosn is exhausted, add the last number in it
		List<Integer> ultimateLst = new ArrayList<Integer>();
		// process until queue becomes empty
		while (!queue.isEmpty()) {
			
			// Retrieve the min term from the min heap (Priority queue)
			Posn_Entry newMinNum = queue.poll();
			String tempTerm = newMinNum.term_;
			index = newMinNum.index_;
			int curPosn = newMinNum.posn_;

			int lstSize = term_lstOfPosn.get(tempTerm).size();
			if (lstSize == 1){
				ultimateLst.add(curPosn);
			}
			
			if (index < lstSize - 1){
				index++;
				// Add the next term from that catalog to the queue
				// Instead of instantiate a new try, a more efficient
				// way would be just set the current entry to the new index and term
				int nextPosn = term_lstOfPosn.get(tempTerm).get(index);
				queue.add(new Posn_Entry(tempTerm, index, nextPosn, false));
				if (index == lstSize - 1)
					ultimateLst.add(nextPosn);
				
				List<Posn_Entry> lst2 = new ArrayList<Posn_Entry>(queue);
				List<Integer> lst2Int = new ArrayList<Integer>();
				
				for (Posn_Entry posn_Entry : lst2) {
					lst2Int.add(posn_Entry.posn_);
				}
				
				if (ultimateLst.size() == 0){
					int tempMax = Collections.max(lst2Int);
					int tempMin = Collections.min(lst2Int);
					int tempDiff = (tempMax - tempMin);
					
					if (tempDiff < minRangeOfWindow)
						minRangeOfWindow = (tempMax - tempMin);
				} else {
					lst2Int.addAll(ultimateLst);
					int tempMax = Collections.max(lst2Int);
					int tempMin = Collections.min(lst2Int);
					int tempDiff = (tempMax - tempMin);
					
					if (tempDiff < minRangeOfWindow)
						minRangeOfWindow = (tempMax - tempMin);
				}
			}
		}
		return minRangeOfWindow;
	}
	
	public static void main(String[] args) {
		
		// for debugging purpose
		Map<String, List<Integer>> term_lstOfPosn = new HashMap<String, List<Integer>>();
		List<Integer> l1 = new ArrayList<Integer>();
//		l1.add(0);
//		l1.add(5);
//		l1.add(10);
//		l1.add(15);
//		l1.add(31);
		l1.add(50);
		term_lstOfPosn.put("cheap", l1);
		
		List<Integer> l2 = new ArrayList<Integer>();
//		l2.add(1);
//		l2.add(3);
		l2.add(6);
//		l2.add(9);
//		l2.add(30);
		l2.add(80);
//		l2.add(200);
//		l2.add(300);
//		l2.add(31);
		term_lstOfPosn.put("pudding", l2);
		
		List<Integer> l3 = new ArrayList<Integer>();
		l3.add(4);
		l3.add(8);
		l3.add(21);
//		l3.add(30);
		term_lstOfPosn.put("pops", l3);
		
		CalcMinWindow c = new CalcMinWindow();
		int minWindow = 0;
		try {
			minWindow = c.merge(term_lstOfPosn);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(minWindow);
	}
}
