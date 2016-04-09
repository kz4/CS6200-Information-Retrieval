package com.ir.pagerank;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.ir.util.Constants;

public class ComputePageRank {

	Set<String> all_links_ = new HashSet<String>();
	Set<String> sink_links_ = new HashSet<String>();	// Pages that have no out links
	Map<String, Set<String>> link_inLinks_ = new HashMap<String, Set<String>>();
//	Map<String, Set<String>> link_outLinks_ = new HashMap<String, Set<String>>();
	Map<String, Integer> link_outLinks_size_ = new HashMap<String, Integer>();
	Map<String, Double> link_score_ = new HashMap<String, Double>();
	Map<String, Double> top500_link_score_ = new HashMap<String, Double>();
	Map<String, Double> link_score_previous_ = new HashMap<String, Double>();
	double link_damping_ = 0.85;
	int link_size_ = 0;

	private void readProvidedGraphToLinkInlinks(String fileName) throws FileNotFoundException, IOException {

		try(BufferedReader br = new BufferedReader(new FileReader(fileName))) {
			String line = br.readLine();

			while (line != null) {
				Set<String> inLinks = new HashSet<String>();
				String[] links = line.split(" ");

				int i = 0;
				String linkKey = null;
				for (String link : links) {
					if (i == 0){
						linkKey = link;
						i++;
//						all_links_.add(link);
					}
					else{
						inLinks.add(link);
						i++;
//						all_links_.add(link);
						if (!link_outLinks_size_.containsKey(link)){
							link_outLinks_size_.put(link, 1);
						} else {
							link_outLinks_size_.put(link, link_outLinks_size_.get(link) + 1);							
						}
					}
				}
				link_inLinks_.put(linkKey, inLinks);
				line = br.readLine();
			}
		}
		
		// all_links_ are all the keys of inLinks 
		all_links_ = link_inLinks_.keySet();
	}
	
	private void retrieveSinkLinks() {
		Set<String> outLinks = link_outLinks_size_.keySet();
		for (String link : link_inLinks_.keySet()) {			
			if (!outLinks.contains(link))
				sink_links_.add(link);
		}
	}

//	private void retrieveLinkOutlinks() {
//
//		// Iterate through all the link_inLink map
//		for (String link_in_link_inLinks : link_inLinks_.keySet()) {
//
//			// Iterate through the inlink list
//			for (String inLink : link_inLinks_.get(link_in_link_inLinks)) {
//				Set<String> outLinks = null;
//				
//				// Check if a set has been created before
//				if (link_outLinks_.get(inLink) == null){
//					outLinks = new HashSet<String>();
//					outLinks.add(link_in_link_inLinks);
//					link_outLinks_.put(inLink, outLinks);
//				} else {
//					Set<String> outLinks_existed =  link_outLinks_.get(inLink);
//					outLinks_existed.add(link_in_link_inLinks);
//					link_outLinks_.put(inLink, outLinks_existed);
//				}
//			}
//		}
//		
//		Set<String> all_links_copy = new HashSet(all_links_);
//		if (all_links_copy.removeAll(link_outLinks_.keySet()))
//			sink_links_ = all_links_copy;
//		
//		for (String link : link_outLinks_.keySet()) {
//			if (link_outLinks_.get(link).size() == 0)
//				sink_links_.add(link);
//			String valueOfFirst_link_outLinks = link_outLinks_.get(link).iterator().next();
//		}
//	}
	
	private void calcPageRank() {
		
		link_size_ = all_links_.size();
		for (String link : all_links_) {
			link_score_.put(link, (double) 1 / link_size_);
		}
		
		int counter = 1;
		while (!converged(counter)){
			
			System.out.println("Iteration : " + counter);
			link_score_previous_.clear();
			link_score_previous_.putAll(link_score_);
			
			double sinkPR = 0;
			for (String sink_link : sink_links_) {
				sinkPR += link_score_.get(sink_link);
			}
			
			Map<String, Double> newPR = new HashMap<String, Double>();
			for (String link : all_links_) {
				newPR.put(link, (1 - link_damping_) / link_size_);
				newPR.put(link, newPR.get(link) + link_damping_ * sinkPR / link_size_);
				for (String inLink : link_inLinks_.get(link)) {
//					if (link_outLinks_size_.containsKey(inLink))	// Only uses it if outLinks are calculated instead of outLinks' size
					newPR.put(link, newPR.get(link) + link_damping_ * link_score_.get(inLink) / link_outLinks_size_.get(inLink));
				}
			}
			
			double sum_score = 0;
			for (String link : newPR.keySet()) {
				sum_score += newPR.get(link);
			}
			
			for (String link : newPR.keySet()) {
				// Divide by sum_score to get normal distribution
				link_score_.put(link, newPR.get(link) / sum_score);
			}
			counter++;
		}
	}

	private boolean converged(int counter) {

		if (counter == 1)
			return false;
		
		double sum = 0;
		for (String link : link_score_.keySet()) {
			sum += Math.pow((link_score_.get(link) - link_score_previous_.get(link)), 2);
		}
		
		if (Math.sqrt(sum) <= Math.pow(10, -5))
			return true;
		else
			return false;
	}
	
	private Map<String, Double> sortByComparator(Map<String, Double> unsortMap, final boolean order) {
		List<Entry<String, Double>> list = new LinkedList<Entry<String, Double>>(
				unsortMap.entrySet());

		// Sorting the list based on values
		Collections.sort(list, new Comparator<Entry<String, Double>>() {
			public int compare(Entry<String, Double> o1,
					Entry<String, Double> o2) {
				if (order) {
					return o1.getValue().compareTo(o2.getValue());
				} else {
					return o2.getValue().compareTo(o1.getValue());

				}
			}
		});

		// Maintaining insertion order with the help of LinkedList
		Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
		for (Entry<String, Double> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}

		return sortedMap;
	}
	
	private void rankTop500() {
		Map<String, Double> top500_link_score_ = sortByComparator(link_score_, false);
		System.out.println();
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		ComputePageRank p = new ComputePageRank();
		p.readProvidedGraphToLinkInlinks(Constants.otherGraphPath);
//		p.retrieveLinkOutlinks();
		p.retrieveSinkLinks();
		p.calcPageRank();
		p.rankTop500();
	}
}
