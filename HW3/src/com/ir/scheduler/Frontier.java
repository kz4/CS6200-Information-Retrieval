package com.ir.scheduler;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import com.ir.crawler.Link;
import com.ir.urlcanonicalizer.URLCanonicalizer;

public class Frontier {

	public Set<String> visited_;

	// Cell is a simplified version of Link
	public PriorityQueue<Cell> frontier_;
	
	// The reason to keep a Map<Url, Cell> is when
	// a new link is sent to the Frontier to be added
	// we need this Map to get its cell out and update it
	public Map<String, Cell> cellFinder_;
	public int timer_;
	public int waveDepth_;
	public String curUrl_;
	
	public Frontier() {
		this.timer_ = 0;
		this.waveDepth_ = 0;
		this.curUrl_ = "";
		this.cellFinder_ = new HashMap<String, Cell>();
		this.frontier_ = new  PriorityQueue<Cell>();
		this.visited_ = new HashSet<String>();
	}
	
	/**
	 * Pop a Url from Frontier 
	 * @return
	 */
	public String frontierPop(){
		
		Cell l = frontier_.poll();
		curUrl_ = l.url_;
		this.waveDepth_ = l.waveDepth_+1;
		
		return l.url_;
	}
	
	/**
	 * Add a new link to the Frontier
	 * @param url
	 */
	public void addToFrontier(String url){
		
		int inLinkCount = 0;
		int timer = 0;
		int waveDepth = this.waveDepth_;
	
		// Check if it exists in cellFinder_
		if (cellFinder_.containsKey(url)){
			Cell l = cellFinder_.get(url);
			inLinkCount = l.inLinkCounts_+1;
			waveDepth = l.waveDepth_;
			timer = l.arriveTime_; 
			removeFromCellFinder(url);
		}
		else {
			timer_++;
		}		
		Cell cell = new Cell();
		cell.arriveTime_ = timer;
		cell.inLinkCounts_ = inLinkCount;
		cell.url_ = url;
		cell.waveDepth_ = waveDepth;
		
		frontier_.add(cell);		
		cellFinder_.put(url, cell);		
	}
	
	/**
	 * After getting all the out links from a link using JSoup,
	 * call this method to add the eligible out links to Frontier
	 * @param allOutLinks
	 * @return
	 */
	public List<String> updateFrontier(List<String> allOutLinks){
	
		List<String> outLinks = new ArrayList<String>();
		for (String url : allOutLinks) {
			// Canonicalize all the out links obtained from the JSoup
			URL canonlizedUrl = URLCanonicalizer.getCanonicalURL(url, curUrl_);
			
			if (canonlizedUrl != null){
				String canonicalizedUrl = canonlizedUrl.toString();
				
				// Filter the ones that appeared before
				if (visited_.contains(canonicalizedUrl))
					continue;
				
				visited_.add(canonicalizedUrl);
				
				// Add all the out links to the priority queue frontier
				addToFrontier(canonlizedUrl.toString());
				outLinks.add(canonicalizedUrl);
			}
		}
		
		return outLinks;		
	}
	
	/**
	 * Remove url from Map
	 * @param url
	 */
	public void removeFromCellFinder(String url){
		
		Cell l = cellFinder_.get(url);		
		l.url_ = "removed";
		
		cellFinder_.remove(url);
	}
}
