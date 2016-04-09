package com.ir.crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

public class StartCrawl {
	
	private MongoClient mongoClient;
	private MongoDatabase mongoDatabase;


	public StartCrawl() {
		this.mongoClient = new MongoClient();
		this.mongoDatabase = mongoClient.getDatabase("homework3");
		this.mongoDatabase.getCollection("unmerged").drop();
	}
	
	
	public void start() throws InterruptedException {
		
		
//		String seeds_string = "http://en.wikipedia.org/wiki/Cold_War,http://www.historylearningsite.co.uk/coldwar.htm,http://en.wikipedia.org/wiki/Cuban_Missile_Crisis,https://www.google.com/search?client=safari&rls=en&q=cuban+missile+crisis&ie=UTF-8&oe=UTF-8";
		String seeds_string = "http://en.wikipedia.org/wiki/Cold_War,http://www.historylearningsite.co.uk/coldwar.htm,http://en.wikipedia.org/wiki/Cuban_Missile_Crisis,";
		String[] seeds = seeds_string.split(",");
		
		Map<String, List<String>> seedmap = new HashMap<String, List<String>>();
		
		for (String seed: seeds) {
			try {
				URL url = new URL(seed);
				if (!seedmap.containsKey(url.getHost())) {
					List<String> l = new ArrayList<String>();
					l.add(seed);
					seedmap.put(url.getHost(), l);
				} else {
					seedmap.get(url.getHost()).add(seed);
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		
		List<Thread> tlist = new ArrayList<Thread>();
		for (String url : seedmap.keySet()) {
			CrawlerThread ch = new CrawlerThread(seedmap.get(url));
			
			Thread t = new Thread(ch);
			
			t.start();
			tlist.add(t);
		}
		
		for(Thread t : tlist) {
			t.join();
		}	
	}
	
	public static void main(String[] args) throws InterruptedException {
		StartCrawl s = new StartCrawl();
		s.start();
	}
	
	
}
