package com.ir.util;

public class Constants {

	public static final String otherGraphPath = "wt2g_inlinks.txt";
//	public static final String otherGraphPath = "Crawled/in_links_crawled.txt";
	public static final String query = "cold war";
	public static final String cluster_name = "homework3";
	public static final String index_name = "homework3";
	public static final String document_type = "VerticalSearch";
	
	// When the inLinks count is > 50, we take a d number of inLinks
	public static final int d = 50;
	
	public static final String hub = "hub.txt";
	public static final String authority = "authority.txt";
	public static final String pageRank = "pageRank.txt";
	public static final String newline_ = System.getProperty("line.separator");

	public static final String link_inLink_ = "Crawled/in_links_crawled.txt";
	public static final String link_outLink_ = "Crawled/out_links_crawled.txt";	
	public static final String top1000Link_ = "Crawled/toplinks.txt";	
//	public static final String link_inLink_ = "New_Crawled/in_links_crawled.txt";
//	public static final String link_outLink_ = "New_Crawled/out_links_crawled.txt";	
//	public static final String top1000Link_ = "New_Crawled/toplinks.txt";	
}
