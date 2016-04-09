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

public class Crawler {

	private int time_;
	ReadRobots r_ = new ReadRobots();
	PageParser3 p_ = new PageParser3();

	public void crawl(String[] seeds){

		// Keep a unique frontier of urls
		Set<String> uniqueUrlSet = new HashSet<String>();
		PriorityQueue<Link> pq = new PriorityQueue<Link>();

		// Since we are doing DFS, we crawl the links through waves/depth
		// The seeds will be given waveDepth 0
		int waveDepth = 0;
		int initInLinkCounts = 0;

		for (int i = 0; i < seeds.length; i++) {
			time_ = i;
			Link l = new Link(waveDepth, initInLinkCounts, i);
			l.url_ = seeds[i];
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
		while(true){
			Link l = pq.poll();

			System.out.println("Link count: " + uniqueUrlSet.size());
			if (uniqueUrlSet.size() < 20000){
				String url = l.url_;
				PageParser3 p = new PageParser3(url);
				Link link = p.getHttpResponse(l.url_);
				List<String> newOutLinks = link.outLinks_;

				// Check if each newOutLinks is allowed in robots				
				// debugging purposes
//				Boolean b1 = r_.fetchable("http://news.google.com/search");
//				Boolean b2 = r_.fetchable("http://news.google.com/search/about");				
//				try {
//					newOutLinks = newOutLinks.stream().filter(newOutLink -> r_.fetchable(newOutLink)).collect(Collectors.toSet());
//				} catch (Exception e) {
//				}
//
//				// Check if each newOutLinks exist in either polled links or pq
//				try {
//					newOutLinks = newOutLinks.stream().filter(newOutLink -> !uniqueUrlSet.contains(newOutLink)).collect(Collectors.toSet());
//				} catch (Exception e) {
//				}

				// Check if each newOutLinks's content-type is text/html
//				newOutLinks = newOutLinks.stream().filter(newOutLink -> p_.getHttpResponse(newOutLink).contentType_.equals("text/html")).collect(Collectors.toSet());
				// Add all the links to the full set
//				uniqueUrlSet.addAll(newOutLinks);
//				Set<Link> links = newOutLinks.stream().map(newOutLink -> p_.getHttpResponse(newOutLink)).collect(Collectors.toSet());
//				pq.addAll(links);

				
				try {
					for (String newOutLink : newOutLinks) {
						Link res = p_.getHttpResponse(newOutLink);
						if (res.contentType_.equals("text/html")){
							uniqueUrlSet.add(res.url_);
							pq.add(res);
						}
					}
				} catch (Exception e) {
				}

			}
			//			store(linkInfo);

			//			counter++;
			System.out.println("Counter = " +counter);
		}

	}



	public static void main(String[] args) {
		String seeds_string = "http://en.wikipedia.org/wiki/Cold_War,http://www.historylearningsite.co.uk/coldwar.htm,http://en.wikipedia.org/wiki/Cuban_Missile_Crisis,";
		//		String seeds_string = "http://news.google.com/search,http://news.google.com/search/about,https://news.google.com/news/directory";
//				String seeds_string = "http://news.google.com/";
		String[] seeds = seeds_string.split(",");
		Crawler c = new Crawler();
		c.crawl(seeds);
	}
}
