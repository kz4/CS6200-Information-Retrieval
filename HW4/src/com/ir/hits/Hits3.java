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

public class Hits3 {

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

	/**
	 * Read in the root set written out by ReadIndex.java
	 * @param fileName - Constants.top1000Link_
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void retrieveRootSet(String fileName) throws FileNotFoundException, IOException {
		System.out.println("Creating the root set");

		try(BufferedReader br = new BufferedReader(new FileReader(fileName))) {
			String line = br.readLine();

			while (line != null) {
				root_set_.add(line);
				line = br.readLine();
			}
		}
	}

	/**
	Create a base set : expand root set by incoming and outgoing links. Some capping by size might be necessary (50).
 	 Strategy:
     1) For each page in 1000 web pages (root set), obtain a set of pages that the page is pointing to (outLinks)
	 add the obtained set to the base set
	 2) For each page in 1000 web pages (root set), obtain a set of pages that pointing to the page (inLinks)
	 if the size of the set is less than or equal to d, add all pages in the set to the root set
	 if the size of the set is greater than d, add an arbitrary set of d pages from the set to the root set
	 Note: The constant d is set be 50 in our case. The idea is just trying to include more possibly strong hubs
	 into the base set while constraining the size of the base size.

	 */
	private void createBaseSet(){
		System.out.println("Creating the base set");

		for (String page : root_set_) {
			Set<String> tempInLinks = link_inLinks_.get(page);
			Set<String> tempOutLinks = link_outLinks_.get(page);
			if (tempOutLinks != null){
				base_set_.addAll(tempOutLinks);
			}

			if (tempInLinks != null){
				if (tempInLinks.size() <= Constants.d){
					base_set_.addAll(tempInLinks);
				} else {
					Set<String> pages = get50pages(tempInLinks);
					link_inLinks_.put(page, pages);
					base_set_.addAll(pages);
				}
			}
			base_set_.add(page);
		}
		System.out.println("base_set_ size: " + base_set_.size());
		System.out.println("root_set_ size: " + root_set_.size());

	}

	/**
	 * After getting the base set, now we need to fill the
	 * base_link_inLinks and base_link_outLinks Map
	 */
	private void populate_base_link_inLinksAndOutLinks() {
		for (String base_link : base_set_) {
			base_link_inLinks_.put(base_link, link_inLinks_.get(base_link));
			base_link_outLinks_.put(base_link, link_outLinks_.get(base_link));
		}
	}

	/**
	 * Calculate the authority and hub score according to the algorithm on the Wiki page
	 1 G := set of pages
	 2 for each page p in G do
	 3   p.auth = 1 // p.auth is the authority score of the page p
	 4   p.hub = 1 // p.hub is the hub score of the page p
	 5 function HubsAndAuthorities(G)
	 6   for step from 1 to k do // run the algorithm for k steps
	 7     norm = 0
	 8     for each page p in G do  // update all authority values first
	 9       p.auth = 0
	10       for each page q in p.incomingNeighbors do // p.incomingNeighbors is the set of pages that link to p
	11          p.auth += q.hub
	12       norm += square(p.auth) // calculate the sum of the squared auth values to normalise
	13     norm = sqrt(norm)
	14     for each page p in G do  // update the auth scores 
	15       p.auth = p.auth / norm  // normalise the auth values
	16     norm = 0
	17     for each page p in G do  // then update all hub values
	18       p.hub = 0
	19       for each page r in p.outgoingNeighbors do // p.outgoingNeighbors is the set of pages that p links to
	20         p.hub += r.auth
	21       norm += square(p.hub) // calculate the sum of the squared hub values to normalise
	22     norm = sqrt(norm)
	23     for each page p in G do  // then update all hub values
	24       p.hub = p.hub / norm   // normalise the hub values
	 */
	private void calcAuthorityHubScore() {

		System.out.println("Calculating authority and hub score");
		filter();

		for (String base_link : root_set_) {
			auth_score_.put(base_link, (double)1);
		}

		for (String root_link : base_set_) {
			hub_score_.put(root_link, (double)1);
		}

		auth_score_prev_.clear();
		auth_score_prev_.putAll(auth_score_);

		hub_score_prev_.clear();
		hub_score_prev_.putAll(hub_score_);

		int counter = 1;
		while (true){

			System.out.println("Iteration : " + counter);

			double normSquare = 0;
			for (String root_link : root_set_) {
				double sum = 0;

				if (base_link_inLinks_.containsKey(root_link) && base_link_inLinks_.get(root_link) != null){
					for (String inLink : base_link_inLinks_.get(root_link)) {
						sum += hub_score_.get(inLink);
					}
					normSquare += sum * sum;
					auth_score_.put(root_link, sum);
				} 
			}
			for (String root_link2 : root_set_) {
				auth_score_.put(root_link2, auth_score_.get(root_link2) / Math.sqrt(normSquare));						
			}

			normSquare = 0;
			for (String base_link : base_set_) {
				double sum = 0;
				hub_score_.put(base_link, (double) 0);

				if ( base_link_outLinks_.get(base_link) != null){
					for (String outLink : base_link_outLinks_.get(base_link)) {
						if (auth_score_.containsKey(outLink))
							sum += auth_score_.get(outLink);
					}
					normSquare += sum * sum;
					hub_score_.put(base_link, sum);
				} 
			}
			for (String base_link2 : base_set_) {
				hub_score_.put(base_link2, hub_score_.get(base_link2) / Math.sqrt(normSquare));						
			}

			if (converged())
				break;

			auth_score_prev_.clear();
			auth_score_prev_.putAll(auth_score_);

			hub_score_prev_.clear();
			hub_score_prev_.putAll(hub_score_);

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

	/**
	 * Nees to check converge for both auth_score and hub_score
	 * @return
	 */
	private boolean converged() {

		double sum = 0;
		for (String link : auth_score_.keySet()) {
			sum += Math.pow((auth_score_.get(link) - auth_score_prev_.get(link)), 2);
		}
		sum = sum / auth_score_.size();

		double sum2 = 0;
		for (String link : hub_score_.keySet()) {
			sum2 += Math.pow((hub_score_.get(link) - hub_score_prev_.get(link)), 2);
		}
		sum2 = sum2 / auth_score_.size();

		if (Math.sqrt(sum) <= Math.pow(10, -5) && Math.sqrt(sum2) <= Math.pow(10, -5))
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

	/**
	 * Read the link_inLinks_ and link_outLinks_ Map<String, Set<String>>
	 * written out by generate_matrix.py 
	 * @param fileName - out_links_crawled.txt or in_links_crawled.txt
	 * @param map_to_read - link_outLinks_ or link_inLinks_
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
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
				try {
					line = br.readLine();
				} catch (Exception e) {
					System.out.println(line);
					System.out.println(e);
				}
			}
		}

		System.out.println("Finish reading: " + fileName);

		return map_to_read;
	}

	public static void main(String[] args) {
		Hits3 h = new Hits3();
		try {
			link_inLinks_= h.read_inLinks_outLinks(Constants.link_inLink_, link_inLinks_);
			link_outLinks_ = h.read_inLinks_outLinks(Constants.link_outLink_, link_outLinks_);
			h.retrieveRootSet(Constants.top1000Link_);
			h.createBaseSet();
			h.populate_base_link_inLinksAndOutLinks();
			h.calcAuthorityHubScore();
			h.findTop500HubAuth();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
