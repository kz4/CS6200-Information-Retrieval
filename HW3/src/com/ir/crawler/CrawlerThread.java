package com.ir.crawler;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.Document;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Sets;

import com.ir.readrobots.ReadRobots;
import com.ir.scheduler.Frontier;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

public class CrawlerThread implements Runnable{

	private int time_;
	ReadRobots r_ = new ReadRobots();
	PageParser4 p_ = new PageParser4();
	Frontier f_ = new Frontier();
	private MongoClient mongoClient;
	private MongoDatabase mongoDatabase;
	private List<String> seeds;
	
	public CrawlerThread(List<String> seeds) {
		this.mongoClient = new MongoClient();
		this.mongoDatabase = mongoClient.getDatabase("homework3");
//		this.mongoDatabase.getCollection("unmerged").drop();
		
		this.seeds = seeds;
	}

	public void run(){

		// Keep a unique frontier of urls
		// Since we are doing DFS, we crawl the links through waves/depth
		// The seeds will be given waveDepth 0
		// Since the seeds will be the first ones to be crawled, their inLinkCounts = 0

		for (int i = 0; i < seeds.size(); i++) {
			this.f_.addToFrontier(seeds.get(i));

			// Read all the seeds robots information to a HashMap
			try {
				r_.readRobots(seeds.get(i));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}

		// We want to crawl 20000 links in total
		long start;
		long end;
		long counter = 0;
		while(true){
 			
			String url = f_.frontierPop(); 
			
			if (!r_.fetchable(url)){
				continue;
			}
			
			PageParser4 p = new PageParser4();
			start = System.currentTimeMillis();
			Link link = p.getHttpResponse(url);
			
			if (link == null)
				continue;
			
			if (!link.contentType_.startsWith("text/html"))
				continue;

			link.outLinks_ = this.f_.updateFrontier(link.outLinks_);

			// Store the link to MongoDB
			store(link);
			end = System.currentTimeMillis();
			long diff = end - start;
			if (diff < 1000) {
				System.out.println(counter + " sleeping: " + diff);
				try {
					Thread.sleep(1000 - diff);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			counter ++;
		}
	}
	
	public void store(Link link) {
		Document doc = new Document()
			.append("title", link.title_)
			.append("url", link.url_)
			.append("text", link.body_)
			.append("source", link.rawHtml)
			.append("header", link.header_)
			.append("outlinks", link.outLinks_);
		
		this.mongoDatabase.getCollection("unmerged").insertOne(doc);
	}



	public static void main(String[] args) {
		String seeds_string = "http://en.wikipedia.org/wiki/Cold_War,http://www.historylearningsite.co.uk/coldwar.htm,http://en.wikipedia.org/wiki/Cuban_Missile_Crisis,https://www.google.com/search?client=safari&rls=en&q=cuban+missile+crisis&ie=UTF-8&oe=UTF-8";
		//		String seeds_string = "http://news.google.com/search,http://news.google.com/search/about,https://news.google.com/news/directory";
//						String seeds_string = "http://news.google.com/";
		String[] seeds = seeds_string.split(",");
	}


}
