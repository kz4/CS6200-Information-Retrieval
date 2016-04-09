package com.ir.crawler;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.ir.urlcanonicalizer.URLCanonicalizer;

public class PageParser2 {

	public String link_;

	public PageParser2(){
	}

	public PageParser2(String link){
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
		
		// Text in Elasaticsearch
		String bodyContent = doc.body().text().toString();
		List<String> outLinks = new ArrayList<String>();	
		for (Element link : links) {
			String absHref = link.attr("abs:href"); // "http://jsoup.org/"

			// Docno in Elasticsearch
			URL canonicalizedUrl = URLCanonicalizer.getCanonicalURL(absHref, url);
			outLinks.add(canonicalizedUrl.toString());
			System.out.println(canonicalizedUrl);

		}


		String response = doc.body().toString();

		return "";
	}

	private CloseableHttpResponse getHttpResponse(String url){
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(url);
		CloseableHttpResponse response = null;
		List<URI> redirectLocations = null;
		
		try {
			HttpClientContext context = HttpClientContext.create();
			response = httpclient.execute(httpGet, context);
			
			// get all redirection locations
            redirectLocations = context.getRedirectLocations();
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
				System.out.println(entityString);
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

		PageParser2 p = new PageParser2();
//		p.parseUrl("http://www.charles-yuan.com");
		p.parseUrl("http://hc.apache.org/httpcomponents-core-ga/httpcore/apidocs/org/apache/http/entity/StringEntity.html#getContent()");
	}
}
