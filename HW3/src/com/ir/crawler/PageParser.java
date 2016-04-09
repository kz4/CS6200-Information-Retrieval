package com.ir.crawler;

import java.io.IOException;
import java.net.URL;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.ir.urlcanonicalizer.URLCanonicalizer;

public class PageParser {

	public String link_;

	public PageParser(){
	}

	public PageParser(String link){
		link_ = link;
	}

	public String parseUrl(String url){

		CloseableHttpResponse resp = getHttpResponse(url);

		Document doc = null;
		try {
			doc = Jsoup.connect(url).get();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Elements links = doc.select("a");
//		Elements contents = doc.select("p");
//		String title = doc.title();
		//		String relHref = link.attr("href"); // == "/"
		//		String absHref = link.attr("abs:href"); // "http://jsoup.org/"
		//		
		for (Element link : links) {
			//			String relHref = link.attr("href"); // == "/"
			String absHref = link.attr("abs:href"); // "http://jsoup.org/"

			// DocNO in Elasticsearch
			URL canonicalizedUrl = URLCanonicalizer.getCanonicalURL(absHref, url);
			//			System.out.println(relHref);
			System.out.println(canonicalizedUrl);

		}

//		StringBuilder sb = new StringBuilder();
//		for (Element content : contents) {
//			sb.append(content);
//		}
//		String allContent = sb.toString();

		// text in ElasticSearch

		String body = doc.body().text().toString();
		String response = doc.body().toString();

		return "";
	}

	private CloseableHttpResponse getHttpResponse(String url){
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(url);
		CloseableHttpResponse response = null;
		try {
			response = httpclient.execute(httpGet);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		// The underlying HTTP connection is still held by the response object
		// to allow the response content to be streamed directly from the network socket.
		// In order to ensure correct deallocation of system resources
		// the user MUST call CloseableHttpResponse#close() from a finally clause.
		// Please note that if response content is not fully consumed the underlying
		// connection cannot be safely re-used and will be shut down and discarded
		// by the connection manager. 
		try {
			System.out.println("Status line: " + response.getStatusLine());
			//		    Header[] headers = response.getAllHeaders();
			//		    for (Header header : headers) {
			//				if (header.getName().toLowerCase().equals("content-type"))
			//						System.out.println(header.getValue());
			//			}

			if (response.getStatusLine().getStatusCode() == 200){
				HttpEntity entity = response.getEntity();
				String contentType = getContentType(entity);
				String entityString = EntityUtils.toString(entity);
				
				// do something useful with the response body
				// and ensure it is fully consumed
				EntityUtils.consume(entity);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				response.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return response;
	}

	private String getContentType(HttpEntity entity) {
		ContentType contentType = ContentType.getOrDefault(entity);
		String mimeType = contentType.getMimeType();
		return mimeType;
	}

	public static void main(String[] args) {

		PageParser p = new PageParser();
		p.parseUrl("http://www.charles-yuan.com");
	}
}
