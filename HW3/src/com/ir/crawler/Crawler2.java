package com.ir.crawler;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Sets;

import com.ir.readrobots.ReadRobots;

public class Crawler2 {

	private int time_;
	ReadRobots r_ = new ReadRobots();
	PageParser4 p_ = new PageParser4();

	public void crawl(String[] seeds){

		// Keep a unique frontier of urls
		Set<String> uniqueUrlSet = new HashSet<String>();
		Set<String> polledUrlSet = new HashSet<String>();
		PriorityQueue<Link> pq = new PriorityQueue<Link>();

		// Since we are doing DFS, we crawl the links through waves/depth
		// The seeds will be given waveDepth 0
		// Since the seeds will be the first ones to be crawled, their inLinkCounts = 0
		int waveDepth = 0;
		int initInLinkCountsSeeds = 0;
		int initInLinkCountsNonSeeds = 1;

		for (int i = 0; i < seeds.length; i++) {
			time_ = i;
			Link l = new Link(waveDepth, initInLinkCountsSeeds, time_, seeds[i]);
			pq.add(l);
			uniqueUrlSet.add(l.url_);

			// Read all the seeds robots information to a HashMap
			try {
				r_.readRobots(seeds[i]);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}

		// We want to crawl 20000 links in total
		int counter = 0;
		Boolean crawlingSeeds = true;
		while(true){
			Link l = pq.poll();
			if (l == null)
				break;

			String polledUrl = l.url_;
			int curWaveDepth = l.waveDepth_;
			
			if (curWaveDepth != 0){
				// If link is not fetchable, move on			
				if (!r_.fetchable(polledUrl))
					continue;

				// If link has been polled before, move on
				if (polledUrlSet.contains(polledUrl))
					continue;
				
				// If link appears in the priority queue, move on
				if (pq.contains(l))
					continue;

				// If link is not a text/html, move on
//				if (!l.contentType_.startsWith("text/html"))
//					continue;
			}

 			counter++;
//			System.out.println("Link count: " + uniqueUrlSet.size());
			//			if (uniqueUrlSet.size() < 20000){
			if (counter <= 20000){
				PageParser4 p = new PageParser4(polledUrl);
				Link link = p.getHttpResponse(polledUrl);
				
				if (link == null)
					continue;
				if (link.waveDepth_ >= 1 && !link.contentType_.startsWith("text/html"))
					continue;

				List<String> newOutLinks = link.outLinks_;
				
				// Filter out the newOutLinks that has been polled before
//				newOutLinks = newOutLinks.stream().filter(newOutLink -> !polledUrlSet.contains(newOutLink)).collect(Collectors.toSet());

				try {
					uniqueUrlSet.addAll(newOutLinks);
//					Set<Link> links = newOutLinks.stream().map(newOutLink -> p_.getHttpResponse(newOutLink)).collect(Collectors.toSet());
//					pq.addAll(links);
					
					int newWaveDepth = ++curWaveDepth;
					for (String newLink : newOutLinks) {
						time_++;
//						Link nLink = p_.getHttpResponse(newLink);
//						nLink.waveDepth_++;
//						time_++;
//						nLink.arriveTime_ = time_;
////						nLink.inLinkCounts_++;
//						pq.add(nLink);
						
						Link toBeAddedToPriorityQueue = new Link(newWaveDepth, initInLinkCountsNonSeeds, time_, newLink);
						pq.add(toBeAddedToPriorityQueue);
					}
				} catch (Exception e) {
				}

			}
			//			store(linkInfo);

			//			counter++;
			System.out.println("Counter = " +counter);
			polledUrlSet.add(polledUrl);
		}

		System.out.println("Finished crawling");
	}



	public static void main(String[] args) {
		String seeds_string = "http://en.wikipedia.org/wiki/Cold_War,http://www.historylearningsite.co.uk/coldwar.htm,http://en.wikipedia.org/wiki/Cuban_Missile_Crisis,https://www.google.com/search?client=safari&rls=en&q=cuban+missile+crisis&ie=UTF-8&oe=UTF-8";
		//		String seeds_string = "http://news.google.com/search,http://news.google.com/search/about,https://news.google.com/news/directory";
//						String seeds_string = "http://news.google.com/";
		String[] seeds = seeds_string.split(",");
		Crawler2 c = new Crawler2();
		c.crawl(seeds);
	}
}
