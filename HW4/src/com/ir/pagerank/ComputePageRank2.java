package com.ir.pagerank;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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

import com.ir.util.Constants;

public class ComputePageRank2 {

	Set<String> all_links_ = new HashSet<String>();
	Set<String> sink_links_ = new HashSet<String>();	// Pages that have no out links
	Map<String, Set<String>> link_inLinks_ = new HashMap<String, Set<String>>();
	Map<String, Integer> link_outLinks_size_ = new HashMap<String, Integer>();
	Map<String, Double> link_score_ = new HashMap<String, Double>();
	Map<String, Double> link_score_previous_ = new HashMap<String, Double>();
	double link_damping_ = 0.85;
	int link_size_ = 0;

	/**
	 * Read in_links_crawled.txt for the link_inLinks map
	 * and calculate the out_links_size while reading the file
	 * (in_links_crawled.txt was written by the Python file generate_matrix.py that gets the response from ElasticSearch)
	 * all_links are just the key set of in_links
	 * @param fileName
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
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
					}
					else{
						inLinks.add(link);
						i++;
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
		
		all_links_ = link_inLinks_.keySet();
	}
	
	/**
	 * They key set of link_outLinks_size_ are all the outLinks
	 * This is because when populating the link_outLinks_size_, if a link, say "Url" has a set of inLinks "InLinks"
	 * every one of those "InLinks" would at least have 1 outLink "Url"
	 * So, by checking if all the links inside the key set of link_inLinks is one of outLinks,
	 * we know if that link is a sink link or not 
	 */
	private void retrieveSinkLinks() {
		Set<String> outLinks = link_outLinks_size_.keySet();
		for (String link : link_inLinks_.keySet()) {			
			if (!outLinks.contains(link))
				sink_links_.add(link);
		}
	}
	
	/**
	 * The iterative algorithm
	    P is the set of all pages; |P| = N
		S is the set of sink nodes, i.e., pages that have no out links
		M(p) is the set of pages that link to page p
		L(q) is the number of out-links from page q
		d is the PageRank damping/teleportation factor; use d = 0.85 as is typical

		foreach page p in P
		  PR(p) = 1/N                          // initial value
		
		while PageRank has not converged do
		  sinkPR = 0
		  foreach page p in S                  // calculate total sink PR
		    sinkPR += PR(p)
		  foreach page p in P
		    newPR(p) = (1-d)/N                 // teleportation
		    newPR(p) += d*sinkPR/N             // spread remaining sink PR evenly
		    foreach page q in M(p)             // pages pointing to p
		      newPR(p) += d*PR(q)/L(q)         // add share of PageRank from in-links
		  foreach page p
		    PR(p) = newPR(p)
		
		return PR
	 */
	private void calcPageRank() {
		
		link_size_ = all_links_.size();

		// Initialize all the score to 1/n
		for (String link : all_links_) {
			link_score_.put(link, (double) 1 / link_size_);
		}
		for (String link : link_inLinks_.keySet()) {
			link_inLinks_.get(link).forEach(inLink -> link_score_.put(inLink, (double) 1 / link_size_));
		}
		
		int counter = 1;
		// Pass in 1 because the first iteration shouldn't converge
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
					newPR.put(link, newPR.get(link) + link_damping_ * link_score_.get(inLink) / link_outLinks_size_.get(inLink));
				}
			}
			
//			double sum_score = newPR.entrySet().stream().map(entry -> entry.getValue()).reduce((double) 0, Double::sum);
			double sum_score = newPR.values().stream().reduce((double) 0, Double::sum);
//			double sum_score = 0;
//			for (String link : newPR.keySet()) {
//				sum_score += newPR.get(link);
//			}
			
			for (String link : newPR.keySet()) {
				// Divide by sum_score to get normal distribution
				link_score_.put(link, newPR.get(link) / sum_score);
			}
			counter++;
			
			// Break if it hasn't converged for 100 iterations
			if (counter == 100)
				break;
				
		}
	}

	/**
	 * Check convergence by comparing the difference of standard deviation
	 * of every score to its previous score.
	 * The tolerance is set to 10^-5 
	 * @param counter
	 * @return
	 */
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
	
	/**
	 * Sorting a Map<String, Double> by its values
	 * @param unsortMap
	 * @param order
	 * @return
	 */
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
	
	/**
	 * Write to disk the top 500 pages
	 * @throws IOException
	 */
	private void rankTop500() throws IOException {
		Map<String, Double> top_link_score_ = sortByComparator(link_score_, false);
		Map<String, Double> top500_link_score = getTopNMap(top_link_score_, 500);
		System.out.println("Top 500 pages");
		System.out.println(top500_link_score);
		
		BufferedWriter out_page_rank = null;

		FileWriter ostream_cat = new FileWriter(Constants.pageRank);
		out_page_rank = new BufferedWriter(ostream_cat);

		for (String link : top500_link_score.keySet()) {
			out_page_rank.write(link + " " +top500_link_score.get(link));
			out_page_rank.write(Constants.newline_);			
		}

		out_page_rank.close();

		System.out.println("All done");
	}

	/**
	 * Get the top n maps
	 * @param map - LinkedHashMap
	 * @param n
	 * @return
	 */
    private Map<String, Double> getTopNMap(Map<String, Double> map, int n){
    	Map<String, Double> res = new LinkedHashMap<String, Double>();

    	int counter = 0;
    	for (String key : map.keySet()) {
    		res.put(key, map.get(key));
    		counter++;
    		if (counter == n)
    			return res;
		}
    	
		return map;
    }

	public static void main(String[] args) throws FileNotFoundException, IOException {
		ComputePageRank2 p = new ComputePageRank2();
		p.readProvidedGraphToLinkInlinks(Constants.otherGraphPath);
		p.retrieveSinkLinks();
		p.calcPageRank();
		p.rankTop500();
	}
}
