package com.ir.hits;

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

public class Hits {

	private Map<String, Set<String>> link_inLinks_ = new HashMap<String, Set<String>>();
	private Map<String, Set<String>> link_outLinks_ = new HashMap<String, Set<String>>();
		private Map<String, Set<String>> base_link_inLinks_ = new HashMap<String, Set<String>>();
		private Map<String, Set<String>> base_link_outLinks_ = new HashMap<String, Set<String>>();
	Set<String> root_set_ = new HashSet<String>();	// Hub
	Set<String> base_set_ = new HashSet<String>();	// Authority

	private Map<String, Double> page_auth_ = new HashMap<String, Double>();
	private Map<String, Double> page_hub_ = new HashMap<String, Double>();
	private Map<String, Double> page_auth_prev_ = new HashMap<String, Double>();
	private Map<String, Double> page_hub_prev_ = new HashMap<String, Double>();


	public Hits(){

	}

	private void createRootSet(String index, String type){
		// Obtain a basic set of documents by ranking all pages using an IR function (e.g. BM25, ES Search) and add the basic set to the root set
		// Query: cold war







		// For each page in 1000 web pages, add all pages that the page points to
		int counter = 0;
//		for (String page : root_set_) {
//			//			root_link_outLinks_.put(page, link_outLinks_.get(page));
//			base_set_.add(page);
//			base_set_.addAll(link_outLinks_.get(page));
//
//			if(counter == 999)
//				break;
//			counter++;
//		}
		//		counter = 0;

		// For each page in 1000 web pages, obtain a set of pages that pointing to the page
		// if the size of the set is less than or equal to d, add all pages in the set to the root set
		// if the size of the set is greater than d, add an arbitrary set of d pages from the set to the root set
		// Note: The constant d can be 50. The idea of it is trying to include more possibly strong hubs
		// into the root set while constraining the size of the root size.

		// Create a base set : expand root set by incoming and outgoing links. Some capping by size might be necessary. 
		for (String page : root_set_) {
			Set<String> tempInLinks = link_inLinks_.get(page);
			Set<String> tempOutLinks = link_outLinks_.get(page);
			if (tempInLinks.size() <= Constants.d){
				base_set_.addAll(tempInLinks);
				base_link_inLinks_.put(page, tempInLinks);
			} else {
				Set<String> pages = get50pages(tempInLinks);
				base_set_.addAll(pages);
				base_link_inLinks_.put(page, pages);
			}
			base_link_outLinks_.put(page, tempOutLinks);
			base_set_.add(page);

//			if(counter == 999)
//				break;
//			counter++;
		}
	}

	private void calcAuthorityHubScore() {
		int rootSize = root_set_.size();
		int baseSize = base_set_.size();

		int counter = 1;
		while (!converged(counter)){
			
			System.out.println("Iteration : " + counter);
			page_auth_prev_.clear();
			page_auth_prev_.putAll(page_auth_);
			
			page_hub_prev_.clear();
			page_hub_prev_.putAll(page_hub_);

			for (String base_link : base_set_) {
				page_auth_.put(base_link, (double)1);
			}

			for (String root_link : root_set_) {
				page_hub_.put(root_link, (double)1);
			}

			for (String root_link : root_set_) {
				double sum = 0;
				for (String inLink : base_link_inLinks_.get(root_link)) {
					sum += page_hub_.get(inLink);
				}
				double magnitude = calculateMagnitude(page_auth_);
				double normalizedScore = sum / magnitude;
				page_auth_.put(root_link, normalizedScore);
			}

			for (String base_link : base_set_) {
				double sum = 0;
				for (String outLink : base_link_outLinks_.get(base_link)) {
					sum += page_auth_.get(outLink);
				}
				double magnitude = calculateMagnitude(page_hub_);
				double normalizedScore = sum / magnitude;
				page_hub_.put(base_link, normalizedScore);
			}
		}
	}

	private double calculateMagnitude(Map<String, Double> authOrHub_score) {
		
		double sum = 0;
		for (String authOrHub : authOrHub_score.keySet()) {
			sum += (authOrHub_score.get(authOrHub) * authOrHub_score.get(authOrHub));
		}
		
		return Math.sqrt(sum);
	}

	private boolean converged(int counter) {

		if (counter == 1)
			return false;
		
		double sum = 0;
		for (String link : page_auth_.keySet()) {
			sum += Math.pow((page_auth_.get(link) - page_auth_prev_.get(link)), 2);
		}
		
		double sum2 = 0;
		for (String link : page_hub_.keySet()) {
			sum2 += Math.pow((page_hub_.get(link) - page_hub_prev_.get(link)), 2);
		}
		
		if (Math.sqrt(sum) <= Math.pow(10, -5) && Math.sqrt(sum) <= Math.pow(10, -5))
			return true;
		else
			return false;
	}

	private void findTop500HubAuth() throws IOException{
		Map<String, Double> top500_auth = sortByComparator(page_auth_, false);
		Map<String, Double> top500_hub = sortByComparator(page_hub_, false);

		BufferedWriter out_auth = null;
		BufferedWriter out_hub = null;

		FileWriter ostream = new FileWriter(Constants.hub);
		out_hub = new BufferedWriter(ostream);

		FileWriter ostream_cat = new FileWriter(Constants.authority);
		out_auth = new BufferedWriter(ostream_cat);

		//		StringBuilder auth = new StringBuilder();
		for (String link_auth : top500_auth.keySet()) {
			out_auth.write(link_auth + "\t" + top500_auth.get(link_auth));
			out_auth.write(Constants.newline_);			
		}

		for (String link_hub : top500_hub.keySet()) {
			out_hub.write(link_hub + "\t" + top500_auth.get(link_hub));
			out_hub.write(Constants.newline_);			
		}

		out_auth.close();
		out_hub.close();
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

	private Set<String> get50pages(Set<String> temp) {

		Set<String> res = new HashSet<String>();
		int i = 0;
		for (String page : temp) {
			res.add(page);

			if (i == 49)
				return res;
			i++;
		}
		return res;
	}

	private void read_inLinks_outLinks(String fileName, Map<String, Set<String>> map_to_read) throws FileNotFoundException, IOException{
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
						//						if (!link_outLinks_size_.containsKey(link)){
						//							link_outLinks_size_.put(link, 1);
						//						} else {
						//							link_outLinks_size_.put(link, link_outLinks_size_.get(link) + 1);							
						//						}
					}
				}
				link_inLinks_.put(linkKey, inLinks);
				line = br.readLine();
			}
		}
	}

	public static void main(String[] args) {
	}
}
