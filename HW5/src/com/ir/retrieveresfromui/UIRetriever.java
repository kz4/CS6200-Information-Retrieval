package com.ir.retrieveresfromui;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;

import com.ir.util.Constants;

/**
 * Retrieves the manual evaluation on calaca (0, 1, 2) for the 4 queries
 *
 */
public class UIRetriever {

	/**
	 * Write out to disk the first 1000 results in rank of Constants.query
	 * against ElasticSearch
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		Settings setting = ImmutableSettings.settingsBuilder()
				.put("cluster.name", Constants.cluster_name)
				.build();
		TransportClient transportClient = new TransportClient(setting);
		transportClient = transportClient.addTransportAddress(new InetSocketTransportAddress("localhost", 9300));

		if(transportClient.connectedNodes().size() == 0)
		{
			System.out.println("There are no active nodes available for the transport, it will be automatically added once nodes are live!");
		}
		Client client = transportClient;
		QueryBuilder qb = new QueryStringQueryBuilder(Constants.ManualEvalQuery);
		String index_name = Constants.index_name;
		String document_type = Constants.document_type;

		SearchResponse scrollResp = client.prepareSearch(index_name).setTypes(document_type)
				.setScroll(new TimeValue(6000))
				.setQuery(qb)
				.addField("my_score1")
				//			.setExplain(true)
				.setSize(1000).execute().actionGet();

		// no query matched
		Map<String, Integer> results = new HashMap<>();

		FileWriter ostream_cat = new FileWriter(Constants.ManualEvalScoreFromES);
		BufferedWriter out_auth = new BufferedWriter(ostream_cat);

		System.out.println(scrollResp.getHits().getTotalHits());
		int i = 1;
		while (true) {
			for (SearchHit hit : scrollResp.getHits().getHits()) {
				out_auth.write(Constants.queryNo + " XXXXX " + hit.getId() + " " + hit.field("my_score1").getValue());
				out_auth.write(Constants.newline_);
				System.out.println(hit.getId());
				System.out.println(i);
				if (i == 200)
					break;
				i++;
			}
			System.out.println("Finished writing to: " + Constants.ManualEvalScoreFromES);
			out_auth.close();
			System.exit(-1);

			scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(
							new TimeValue(6000)).execute().actionGet();

			if (scrollResp.getHits().getHits().length == 0) {
				break;
			}
		}
	}
}
