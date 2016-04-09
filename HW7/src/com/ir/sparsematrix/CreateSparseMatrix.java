package com.ir.sparsematrix;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.termvector.TermVectorRequest;
import org.elasticsearch.action.termvector.TermVectorResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.lang3.StringUtils;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.ir.util.Constants;

public class CreateSparseMatrix {

	public Client client_;

	Map<String, Integer> term_termId_map_ = new HashMap<String, Integer>();
	//	int termId = 0;

	public CreateSparseMatrix(){
		ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();        
		TransportClient transportClient = new TransportClient(settings);
		transportClient = transportClient.addTransportAddress(new InetSocketTransportAddress("localhost", 9300));

		if(transportClient.connectedNodes().size() == 0)
		{
			System.out.println("There are no active nodes available for the transport, it will be automatically added once nodes are live!");
		}
		client_ = transportClient;
	}

	private List<String> getTermVector(String mailId) throws InterruptedException, ExecutionException, IOException{

		TermVectorRequest req = new TermVectorRequest(Constants.indexName, Constants.typeName, mailId);
		req = req
				.termStatistics(true)
				.offsets(false)
				.payloads(true)
				.fieldStatistics(true)
				.positions(false)
				.selectedFields(new String[] {"body"});

		TermVectorResponse response = client_.termVector(req).get();

		Fields fields = response.getFields();
		Map<Integer, Integer> totalTermsOneDoc = new TreeMap<Integer, Integer>(); 

		for(String field : fields) {
			Terms terms = fields.terms(field);
			TermsEnum termsEnum = terms.iterator(null);
			BytesRef text;
			while((text = termsEnum.next()) != null) {
				DocsEnum docsEnum = termsEnum.docs(null, null); // enumerate through documents, in this case only one
				int docIdEnum;
				while ((docIdEnum = docsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
					if (term_termId_map_.containsKey(text.utf8ToString())){
						// totalTermsOneDoc.add(term_termId_map_.get(text.utf8ToString()) + ":" + docsEnum.freq());
						totalTermsOneDoc.put(term_termId_map_.get(text.utf8ToString()), docsEnum.freq());
					} else {
						// +1 here because lib linear only allow termIndex that starts from 1
						term_termId_map_.put(text.utf8ToString(), term_termId_map_.size()+1);
						// totalTermsOneDoc.add(term_termId_map_.get(text.utf8ToString()) + ":" + docsEnum.freq());
						totalTermsOneDoc.put(term_termId_map_.get(text.utf8ToString()), docsEnum.freq());
					}
				}
			}
		}
		
		
		List<String> sb = new ArrayList<String>();
		for (Integer termId : totalTermsOneDoc.keySet()) {
			sb.add(termId + ":" + totalTermsOneDoc.get(termId));
		}
		
		return sb;
	}
	
	private void write_term_termId_map_to_disk() throws IOException {
		FileWriter fw = new FileWriter(Constants.term_termId);
		BufferedWriter bw = new BufferedWriter(fw);
		
		for (String term : term_termId_map_.keySet()) {
			bw.write(term + " " + term_termId_map_.get(term));
			bw.write(Constants.newline);
		}
	}

	public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {

		CreateSparseMatrix matrix = new CreateSparseMatrix();
		matrix.createSpareMatrixFile();
		System.out.println("Total number of unigrams: " + matrix.term_termId_map_.size());
		matrix.write_term_termId_map_to_disk();
		
		// matrix.getTermVector("inmail.63267");
	}

	private void createSpareMatrixFile() throws InterruptedException, ExecutionException, IOException {

		QueryBuilder qb = QueryBuilders.matchAllQuery();
		SearchResponse scrollResp = client_.prepareSearch(Constants.indexName)
				.setTypes(Constants.typeName)
				.setScroll(new TimeValue(6000))
				.setQuery(qb)
				.setFetchSource(new String[]{"label", "split"}, new String[]{"body"})
				.setExplain(true)
				.setSize(1000).execute().actionGet();

		try {
			FileWriter fw = new FileWriter(Constants.trainSparseMatrix);
			FileWriter fw2 = new FileWriter(Constants.testSparseMatrix);
			FileWriter fw3 = new FileWriter(Constants.trainSparseMatrixId);
			FileWriter fw4 = new FileWriter(Constants.testSparseMatrixId);
			BufferedWriter bw = new BufferedWriter(fw);
			BufferedWriter bw2 = new BufferedWriter(fw2);
			BufferedWriter bw3 = new BufferedWriter(fw3);
			BufferedWriter bw4 = new BufferedWriter(fw4);

//			bw.write(string);
			while (true)
			{
				if (scrollResp.getHits().getHits().length == 0)
				{
					break;
				}
				for (SearchHit hit : scrollResp.getHits().getHits())
				{
					List<String> LsttermIdColonTermFreq = getTermVector(hit.getId());
					String mailId = (String) hit.getId();
					String spam; 
					if (hit.getSource().get("label").equals("spam"))
						spam = "1";
					else
						spam = "0";
					//				mailId_spam_map.put(mailId, spam);
					String oneEmail = spam + " " + StringUtils.join(LsttermIdColonTermFreq, " ");

					if (hit.getSource().get("split").equals("train")){
						bw.write(oneEmail);
						bw.write(Constants.newline);
						bw3.write(mailId);
						bw3.write(Constants.newline);
					}
					else{
						bw2.write(oneEmail);
						bw2.write(Constants.newline);
						bw4.write(mailId);
						bw4.write(Constants.newline);
					}
					System.out.println(hit.getId());
				}
				scrollResp = client_.prepareSearchScroll(scrollResp.getScrollId()).setScroll(
						new TimeValue(6000)).execute().actionGet();
			}
			
			bw.flush();
			bw.close();
			bw2.flush();
			bw2.close();
			bw3.flush();
			bw3.close();
			bw4.flush();
			bw4.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
