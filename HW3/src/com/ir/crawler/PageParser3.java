package com.ir.crawler;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
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

public class PageParser3 {

	public String link_;

	public PageParser3(){
	}

	public PageParser3(String link){
		link_ = link;
	}

	public Link getHttpResponse(String url){
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(url);
		CloseableHttpResponse response = null;
		Link l = null;

		try {
			HttpClientContext context = HttpClientContext.create();
			response = httpclient.execute(httpGet, context);

		} catch (IOException e1) {
			e1.printStackTrace();
		}
		// The underlying HTTP connection is still held by the response object
		// to allow the response content to be streamed directly from the network socket.
		// In order to ensure correct deallocation of system resources
		// the user MUST call CloseableHttpResponse.close() from a finally clause.
		// Please note that if response content is not fully consumed the underlying
		// connection cannot be safely re-used and will be shut down and discarded
		// by the connection manager. 
		try {
//			System.out.println("Status line: " + response.getStatusLine());
			Header[] headers = response.getAllHeaders();

			// Header
			String header = StringUtils.join(Arrays.asList(headers).stream().map(h -> h.toString()).toArray(), "\n");
//			System.out.println(header);

			if (response.getStatusLine().getStatusCode() == 200){
				// http://stackoverflow.com/a/9197823/1472828
				// An HTTP entity is the majority of an HTTP request or response,
				// consisting of some of the headers and the body, if present.
				// It seems to be the entire request or response without the request
				// or status line
				HttpEntity entity = response.getEntity();
				String contentType = entity.getContentType().getValue().toString();

				// Response
				String entityString = EntityUtils.toString(entity);


				Document doc = null;
				doc = Jsoup.parse(entityString);

				URL currentUrl = URLCanonicalizer.getCanonicalURL(url, url);
				String curUrl = currentUrl.toString();
				Elements links = doc.select("a");

				// Text in Elasaticsearch
				String bodyContent = doc.body().text().toString();
				
				List<String> outLinks = new ArrayList<String>();	
				for (Element link : links) {
//					String absHref = link.attr("abs:href"); // "http://jsoup.org/"
					String absHref = link.attr("href"); // "http://jsoup.org/"

					// Docno in Elasticsearch
					URL canonicalizedUrl = URLCanonicalizer.getCanonicalURL(absHref, url);
					if (canonicalizedUrl == null)
						continue;
//					if (outLinks.contains(canonicalizedUrl.toString()))
//						continue;
					
					outLinks.add(canonicalizedUrl.toString());
				}

				System.out.println("Outlinks for " + curUrl);
				
				// Print every link obtained
//				outLinks.forEach(outlink -> System.out.println(outlink));
				
				l = new Link();
				l.body_ = bodyContent;
				l.header_ = header;
				l.outLinks_ = outLinks;
				l.contentType_ = contentType;
				l.url_ = curUrl;
				
				// do something useful with the response body
				// and ensure it is fully consumed
				//				System.out.println(entityString);
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
		return l;
	}

//	private String getContentType(HttpEntity entity) {
//		ContentType contentType = ContentType.getOrDefault(entity);
//		String mimeType = contentType.getMimeType();
//		return mimeType;
//	}

	public static void main(String[] args) {

		PageParser3 p = new PageParser3();
		Link l = p.getHttpResponse("http://hc.apache.org/httpcomponents-core-ga/httpcore/apidocs/org/apache/http/entity/StringEntity.html#getContent()");
//		Link l = p.getHttpResponse("http://www.charles-yuan.com/index.html");
		//		p.parseUrl("http://www.charles-yuan.com");
//		p.parseUrl("http://hc.apache.org/httpcomponents-core-ga/httpcore/apidocs/org/apache/http/entity/StringEntity.html#getContent()");
	}
}
