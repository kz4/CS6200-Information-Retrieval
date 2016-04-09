package com.ir.crawler;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

public class Link implements Comparable <Link> {

	public int waveDepth_;
	public int inLinkCounts_;
	public int arriveTime_;

	public List<String> outLinks_;
	public String body_; // Text field in Elasticsearch
	public String url_; // Docno field in Elasticsearch

	// Header, e.g.
	//	Date: Fri, 26 Jun 2015 01:48:55 GMT
	//	Server: Apache/2.4.7 (Ubuntu)
	//	Last-Modified: Thu, 19 Mar 2015 13:04:05 GMT
	//	ETag: "922a-511a3d5ad8c36-gzip"
	//	Accept-Ranges: bytes
	//	Vary: Accept-Encoding
	//	Keep-Alive: timeout=30, max=100
	//	Connection: Keep-Alive
	//	Content-Type: text/html
	public String header_;	// 
	public String contentType_; // Needs to be text/html
	public String title_;
	public String rawHtml;

	public Link(){
		waveDepth_ = -1;
		inLinkCounts_ = -1;
		arriveTime_ = -1;
	}

	public Link(int waveDepth, int inLinkCounts, int arriveTime){
		waveDepth_ = waveDepth;
		inLinkCounts_ = inLinkCounts;
		arriveTime_ = arriveTime;
	}
	
	public Link(int waveDepth, int inLinkCounts, int arriveTime, String url){
		waveDepth_ = waveDepth;
		inLinkCounts_ = inLinkCounts;
		arriveTime_ = arriveTime;
		url_ = url;
	}

	@Override
	public int compareTo(Link f) {

		if (waveDepth_ < f.waveDepth_)
			return -1;
		else if (waveDepth_ > f.waveDepth_)
			return 1;
		else{
			if (inLinkCounts_ > f.inLinkCounts_)
				return -1;
			else if (inLinkCounts_ < f.inLinkCounts_)
				return 1;
			else{
				if (arriveTime_ < f.arriveTime_)
					return -1;
				else if (arriveTime_ > f.arriveTime_)
					return 1;
			}
		}
		return 0;
	}

	public static void main(String[] args) {

		// For debugging purposes
		Link f1 = new Link(0, 0, 0);
		Link f2 = new Link(1, 0, 0);
		Link f3 = new Link(0, 1, 0);
		Link f4 = new Link(0, 0, 1);
		Link f5 = new Link(0, 0, 0);

		PriorityQueue<Link> pq = new PriorityQueue<Link>();
		pq.add(f1);
		pq.add(f2);
		pq.add(f3);
		pq.add(f4);
		pq.add(f5);

		Link poll1 = pq.poll();
		Link poll2 = pq.poll();
		Link poll3 = pq.poll();
		Link poll4 = pq.poll();
		Link poll5 = pq.poll();
	}

}
