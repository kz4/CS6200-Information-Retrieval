package com.ir.hits;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

public class Hits2 {

	private static Map<String, Set<String>> link_inLinks_ = new HashMap<String, Set<String>>();
	private static Map<String, Set<String>> link_outLinks_ = new HashMap<String, Set<String>>();
	private Map<String, Set<String>> base_link_inLinks_ = new HashMap<String, Set<String>>();
	private Map<String, Set<String>> base_link_outLinks_ = new HashMap<String, Set<String>>();
	Set<String> root_set_ = new HashSet<String>();	// Authority
	Set<String> base_set_ = new HashSet<String>();	// Hub

	private Map<String, Double> auth_score_ = new HashMap<String, Double>();
	private Map<String, Double> hub_score_ = new HashMap<String, Double>();
	private Map<String, Double> auth_score_prev_ = new HashMap<String, Double>();
	private Map<String, Double> hub_score_prev_ = new HashMap<String, Double>();

	private void retrieveRootSet(String fileName) throws FileNotFoundException, IOException {

		System.out.println("Creating the base set");
		// Obtain a basic set of documents by ranking all pages using an IR function (e.g. BM25, ES Search) and add the basic set to the root set
		// Query: cold war
		try(BufferedReader br = new BufferedReader(new FileReader(fileName))) {
			String line = br.readLine();

			while (line != null) {
				String[] rank_link = line.split(" : ");
				root_set_.add(rank_link[1]);
				line = br.readLine();
			}
		}
	}

	private void createBaseSet(){
		System.out.println("Creating the base set");
		// For each page in 1000 web pages, obtain a set of pages that pointing to the page
		// if the size of the set is less than or equal to d, add all pages in the set to the root set
		// if the size of the set is greater than d, add an arbitrary set of d pages from the set to the root set
		// Note: The constant d can be 50. The idea of it is trying to include more possibly strong hubs
		// into the root set while constraining the size of the root size.

		// Create a base set : expand root set by incoming and outgoing links. Some capping by size might be necessary (50). 
		for (String page : root_set_) {
			Set<String> tempInLinks = link_inLinks_.get(page);
			Set<String> tempOutLinks = link_outLinks_.get(page);
			if (tempOutLinks != null){
				base_set_.addAll(tempOutLinks);
			}

			if (tempInLinks != null){
				if (tempInLinks.size() <= Constants.d){
					base_set_.addAll(tempInLinks);
					//				base_link_inLinks_.put(page, tempInLinks);
				} else {
					Set<String> pages = get50pages(tempInLinks);
					base_set_.addAll(pages);
					//				base_link_inLinks_.put(page, pages);
				}
				//			base_link_outLinks_.put(page, tempOutLinks);
			}
			base_set_.add(page);
		}
	}

	private void populate_Base_link_inLinksAndOutLinks() {
		for (String base_link : base_set_) {
			base_link_inLinks_.put(base_link, link_inLinks_.get(base_link));
			base_link_outLinks_.put(base_link, link_outLinks_.get(base_link));
		}
	}

	private void calcAuthorityHubScore() {

		System.out.println("Calculating authority and hub score");
		filter();
		
		for (String base_link : base_set_) {
			auth_score_.put(base_link, (double)1);
		}

		for (String root_link : root_set_) {
			hub_score_.put(root_link, (double)1);
		}

		int counter = 1;
		while (!converged(counter)){

			System.out.println("Iteration : " + counter);
			auth_score_prev_.clear();
			auth_score_prev_.putAll(auth_score_);

			hub_score_prev_.clear();
			hub_score_prev_.putAll(hub_score_);

			for (String root_link : root_set_) {
//				if (!base_link_inLinks_.containsKey(root_link)){
//					auth_score_.put(root_link, (double) 0);
//				}

				double sum = 0;
				if (base_link_inLinks_.containsKey(root_link) && base_link_inLinks_.get(root_link) != null){
					
//					System.out.println(base_link_inLinks_.get(root_link));
					for (String inLink : base_link_inLinks_.get(root_link)) {
						if (hub_score_.containsKey(inLink))
							sum += hub_score_.get(inLink);
					}
//					double magnitude = calculateMagnitude(sum);
					double magnitude = Math.sqrt(sum);
//					double normalizedScore = sum / magnitude;
					for (String root_link2 : root_set_) {
						auth_score_.put(root_link2, auth_score_.get(root_link2) / magnitude);						
					}
//					auth_score_.put(root_link, normalizedScore);
				} 
			}

			for (String base_link : base_set_) {
				if (!base_link_outLinks_.containsKey(base_link)){
					hub_score_.put(base_link, (double) 1);
				}

				double sum = 0;
				if (base_link_outLinks_.containsKey(base_link) && base_link_outLinks_.get(base_link) != null){
					for (String outLink : base_link_outLinks_.get(base_link)) {
						if (auth_score_.containsKey(outLink))
							sum += auth_score_.get(outLink);
					}
					double magnitude = Math.sqrt(sum);
//					double normalizedScore = sum / magnitude;
					for (String base_link2 : base_set_) {
						if (hub_score_.containsKey(base_link2))
							hub_score_.put(base_link2, hub_score_.get(base_link2) / magnitude);						
					}
				} 
			}
			counter++;
		}
	}
	
	private void filter() {
		for (String key : base_link_outLinks_.keySet()) {
			if (!base_set_.contains(key)) {
				base_link_outLinks_.remove(key);
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
		for (String link : auth_score_.keySet()) {
//			if (auth_score_.get(link) == null)
//				auth_score_.put(link, (double) 1);
			if (auth_score_prev_.get(link) == null)
				auth_score_prev_.put(link, (double) 1);
			sum += Math.pow((auth_score_.get(link) - auth_score_prev_.get(link)), 2);
		}
		sum = sum / auth_score_.size();

		double sum2 = 0;
		for (String link : hub_score_.keySet()) {
//			if (hub_score_.get(link) == null)
//				hub_score_.put(link, (double) 1);
			if (hub_score_prev_.get(link) == null)
				hub_score_prev_.put(link, (double) 1);
			sum2 += Math.pow((hub_score_.get(link) - hub_score_prev_.get(link)), 2);
		}
		sum2 = sum2 / auth_score_.size();

		if (Math.sqrt(sum) <= Math.pow(10, -3) && Math.sqrt(sum2) <= Math.pow(10, -3))
			return true;
		else
			return false;
	}

	private void findTop500HubAuth() throws IOException{

		System.out.println("Finding top 500 authority and hub score and writing it to disk");

		Map<String, Double> top_auth = sortByComparator(auth_score_, false);
		Map<String, Double> top500_auth_score = getTopNMap(top_auth, 500);

		Map<String, Double> top_hub = sortByComparator(hub_score_, false);
		Map<String, Double> top500_hub_score = getTopNMap(top_hub, 500);

		BufferedWriter out_auth = null;
		BufferedWriter out_hub = null;

		FileWriter ostream = new FileWriter(Constants.hub);
		out_hub = new BufferedWriter(ostream);

		FileWriter ostream_cat = new FileWriter(Constants.authority);
		out_auth = new BufferedWriter(ostream_cat);

		for (String link_auth : top500_auth_score.keySet()) {
			out_auth.write(link_auth + "\t" + top500_auth_score.get(link_auth));
			out_auth.write(Constants.newline_);			
		}

		for (String link_hub : top500_hub_score.keySet()) {
			out_hub.write(link_hub + "\t" + top500_hub_score.get(link_hub));
			out_hub.write(Constants.newline_);			
		}

		out_auth.close();
		out_hub.close();
		
		System.out.println("All done");
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

	private Map<String, Set<String>> read_inLinks_outLinks(String fileName, Map<String, Set<String>> map_to_read) throws FileNotFoundException, IOException{

		System.out.println("Reading: " + fileName);

		try(BufferedReader br = new BufferedReader(new FileReader(fileName))) {
			String line = br.readLine();

			while (line != null) {
				List<String> inlinks = new ArrayList<String>();				
				inlinks.addAll(Arrays.asList(line.split(" ")));
				String key = inlinks.get(0);
				inlinks.remove(key);
				map_to_read.put(key, new HashSet<String>(inlinks));
				line = br.readLine();
			}
		}

		System.out.println("Finish reading: " + fileName);

		return map_to_read;
	}

	public static void main(String[] args) {
		Hits2 h = new Hits2();
		try {
			link_inLinks_= h.read_inLinks_outLinks(Constants.link_inLink_, link_inLinks_);
			link_outLinks_ = h.read_inLinks_outLinks(Constants.link_outLink_, link_outLinks_);
			h.retrieveRootSet(Constants.top1000Link_);
			h.createBaseSet();
			h.populate_Base_link_inLinksAndOutLinks();
			h.calcAuthorityHubScore();
			h.findTop500HubAuth();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
