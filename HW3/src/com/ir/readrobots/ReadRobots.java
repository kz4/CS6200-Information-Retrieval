package com.ir.readrobots;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.ws.spi.http.HttpContext;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;

import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRules;
import crawlercommons.robots.SimpleRobotRules.RobotRulesMode;
import crawlercommons.robots.SimpleRobotRulesParser;

public class ReadRobots {
	
	 public Map<String, BaseRobotRules> robotsTxtRules_ = new HashMap<String, BaseRobotRules>();


	/**
	 * Before you crawl the first page from a given domain,
	 * fetch its robots.txt file and make sure your crawler
	 * strictly obeys the file. You should use a third
	 * party library to parse the file and tell you
	 * which URLs are OK to crawl.
	 * @param url
	 * @return
	 * @throws MalformedURLException 
	 */
	public void readRobots(String url) throws MalformedURLException{

		List<String> crawlableLinks = new ArrayList<String>();

		String USER_AGENT = "WhateverBot";
		//		String url = "http://www.....com/";
		URL urlObj = new URL(url);
		String hostId2 = "";
		String hostId = urlObj.getProtocol() + "://" + urlObj.getHost()
				+ (urlObj.getPort() > -1 ? ":" + urlObj.getPort() : "");
		
		if (urlObj.getProtocol().equals("http")){
			hostId2 = urlObj.getProtocol() + "s://" + urlObj.getHost()
					+ (urlObj.getPort() > -1 ? ":" + urlObj.getPort() : "");
		} else if (urlObj.getProtocol().equals("https")) {
			hostId2 = urlObj.getProtocol().substring(0, urlObj.getProtocol().length()-1) + "://" + urlObj.getHost()
					+ (urlObj.getPort() > -1 ? ":" + urlObj.getPort() : "");
		}
		BaseRobotRules rules = robotsTxtRules_.get(hostId);
		BaseRobotRules rules2 = robotsTxtRules_.get(hostId2);
		if (rules == null) {
			HttpGet httpget = new HttpGet(hostId + "/robots.txt");
			//		    HttpContext context = new BasicHttpContext();
			//		    HttpResponse response = httpclient.execute(httpget, context);
			CloseableHttpResponse response = null;
			CloseableHttpClient httpclient = HttpClients.createDefault();
//			HttpGet httpGet = new HttpGet(url);
			HttpClientContext context = HttpClientContext.create();
			try {
				response = httpclient.execute(httpget, context);
			} catch (IOException e) {
//				e.printStackTrace();
			};
			if (response.getStatusLine() != null && response.getStatusLine().getStatusCode() == 404) {
				rules = new SimpleRobotRules(RobotRulesMode.ALLOW_ALL);
				rules2 = new SimpleRobotRules(RobotRulesMode.ALLOW_ALL);
				// consume entity to deallocate connection
				EntityUtils.consumeQuietly(response.getEntity());
			} else {
				BufferedHttpEntity entity = null;
				try {
					entity = new BufferedHttpEntity(response.getEntity());
				} catch (IOException e) {
//					e.printStackTrace();
				}
				SimpleRobotRulesParser robotParser = new SimpleRobotRulesParser();
				try {
					rules = robotParser.parseContent(hostId, IOUtils.toByteArray(entity.getContent()),
							"text/plain", USER_AGENT);
					rules2 = robotParser.parseContent(hostId2, IOUtils.toByteArray(entity.getContent()),
							"text/plain", USER_AGENT);
				} catch (IOException e) {
//					e.printStackTrace();
				}
			}
			robotsTxtRules_.put(hostId, rules);
			robotsTxtRules_.put(hostId2, rules);
		}
	}
	
	public Boolean fetchable(String url){
		URL urlObj = null;
		try {
			urlObj = new URL(url);
		} catch (MalformedURLException e) {
//			e.printStackTrace();
			return false;
		}
		String hostId = urlObj.getProtocol() + "://" + urlObj.getHost()
				+ (urlObj.getPort() > -1 ? ":" + urlObj.getPort() : "");
		BaseRobotRules rules = robotsTxtRules_.get(hostId);
		boolean urlAllowed = rules.isAllowed(url);
//		System.out.println(urlAllowed);
		return urlAllowed;		
	}
	
	public static void main(String[] args) {
		ReadRobots r = new ReadRobots();
		
		// readRobots reads in all the seeds and
		// put them in the map
		
//		String url = "http://www.google.com";
		
		// disallow
//		String url = "http://news.google.com/search";
		
		// allow
//		String url = "http://news.google.com/search/about";
		
		// allow
		String url = "https://news.google.com/news/directory";		
//		String url = "http://www.nytimes.com/college/";
//		String url = "http://www.nytimes.com/ads/public";
		try {
			r.readRobots(url);
			Boolean b = r.fetchable(url);
			System.out.println(b);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

}
