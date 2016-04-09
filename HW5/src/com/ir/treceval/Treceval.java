package com.ir.treceval;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.jfree.ui.RefineryUtilities;

import com.ir.plot.XYSeriesDemo;
import com.ir.util.Constants;

public class Treceval {

	public static final String plot_stats = "/Users/kaichenzhang/Desktop/plot_stats.txt";
	
	public static boolean ASC = true;
	public static boolean DESC = false;

	int num_topics_ = 0;
	Boolean print_all_queries_ = false;
	String qrel_file_ = ""; 
	String trec_file_ = "";
	Map<String, Map<String, Integer>> qrel_queryNo_docno_relevance_ = new HashMap<String, Map<String, Integer>>();	// relevance is 0, 1, 2 in homework 5
	Map<String, Map<String, Double>> trec_queryNo_docno_score_ = new HashMap<String, Map<String, Double>>();	// score is from the scoring function
	// num_rel in trec_eval
	Map<String, Integer> queryNo_numOfRelevantDocs_ = new HashMap<String, Integer>(); // values are (expected) number of docs relevant for each query
	double[] recalls_ = {0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
	int[] cutoffs_ = {5, 10, 15, 20, 30, 100, 200, 500, 1000};	// a.k.a. k values
	int total_num_retrieved_ = 0;
	int total_num_relevant_ = 0;
	int total_num_relevant_retrieved_ = 0;
	double[] sum_prec_at_cutoffs_ = new double[cutoffs_.length];
	double[] sum_prec_at_recalls_ = new double[recalls_.length];
	double[] sum_f1_measure_at_cutoffs_ = new double[cutoffs_.length];
	double sum_avg_prec_ = 0;
	double sum_r_prec = 0;
	double sum_nDCG_ = 0;

	public Treceval(String qrel_file, String trec_file, Boolean print_all_queries){
		qrel_file_ = qrel_file;
		trec_file_ = trec_file;
		print_all_queries_ = print_all_queries;
		System.out.println(qrel_file_ + " " + trec_file_ + " " + print_all_queries_);
	}

	private void readFiles() throws FileNotFoundException, IOException {
		readQrelFile(qrel_file_);
		readTrecFile(trec_file_);
	}

	private void readQrelFile(String fileName) throws FileNotFoundException, IOException {
		System.out.println("Reading the qrel file");

		try(BufferedReader br = new BufferedReader(new FileReader(fileName))) {
			String line = br.readLine();

			while (line != null) {
				String[] queryNo_assessorNo_docno_relevance = line.split(" ");
				String queryNo = queryNo_assessorNo_docno_relevance[0];
				String assessorNo = queryNo_assessorNo_docno_relevance[1];
				String docno = queryNo_assessorNo_docno_relevance[2];
				int relevance = Integer.parseInt(queryNo_assessorNo_docno_relevance[3]);
				Map<String, Integer> docno_relevance = new HashMap<String, Integer>();
				// Since there are multiple assessors, assessing the same document
				// we take the max score
				if (docno_relevance.containsKey(docno)){
					// Only replaces the current score when this assessor's score is greater
					// so we can get the max score
					if (relevance > docno_relevance.get(docno)){
						docno_relevance.put(docno, relevance);
					}
				} else {
					docno_relevance.put(docno, relevance);
				}

				if (!qrel_queryNo_docno_relevance_.containsKey(queryNo))
					qrel_queryNo_docno_relevance_.put(queryNo, docno_relevance);
				else
					qrel_queryNo_docno_relevance_.get(queryNo).put(docno, relevance);
				if (!queryNo_numOfRelevantDocs_.containsKey(queryNo) && relevance > 0){
					if (relevance > 1)	// relevance can be 0, 1 or 2, if relevance = 2, we count it as 1 relevant document
						relevance = 1;
					queryNo_numOfRelevantDocs_.put(queryNo, relevance);
				}
				else if (relevance > 0){
					if (relevance > 1) // relevance can be 0, 1 or 2, if relevance = 2, we count it as 1 relevant document
						relevance = 1;
					queryNo_numOfRelevantDocs_.put(queryNo, queryNo_numOfRelevantDocs_.get(queryNo) + relevance);
				}
				line = br.readLine();
			}
		}
	}

	private void readTrecFile(String fileName) throws FileNotFoundException, IOException {
		System.out.println("Reading the result file, for example a BM25 result");

		try(BufferedReader br = new BufferedReader(new FileReader(fileName))) {
			String line = br.readLine();

			while (line != null) {
				String[] queryNo_qo_docno_rank_score_exp = line.split(" ");
				String queryNo = queryNo_qo_docno_rank_score_exp[0];
				String docno = queryNo_qo_docno_rank_score_exp[2];
				double score = Double.parseDouble(queryNo_qo_docno_rank_score_exp[4]);
				Map<String, Double> docno_score = new HashMap<String, Double>();
				docno_score.put(docno, score);
				if (!trec_queryNo_docno_score_.containsKey(queryNo))
					trec_queryNo_docno_score_.put(queryNo, docno_score);
				else
					trec_queryNo_docno_score_.get(queryNo).put(docno, score);

				line = br.readLine();
			}
		}
	}

	private void processTrecData() throws IOException {
		// Process queryNo in order
		Map<String, Map<String, Double>> trec_queryNo_docno_score = new TreeMap<String, Map<String, Double>>(trec_queryNo_docno_score_);

		for (String queryNo : trec_queryNo_docno_score.keySet()) {
			// If no relevant docs, skip topic
			if (!queryNo_numOfRelevantDocs_.containsKey(queryNo))
				continue;

			// Processing another topic
			num_topics_++;

			// New list of precisions
			double[] prec_list = new double[1001];
			// Recall list
			double[] recall_list = new double[1001];


			// $num_ret in trec_eval
			int num_retrieved = 0;
			// $num_rel_ret in trec_eval
			// Use a double here, so 
			// prec_list[num_retrieved] = num_relevant_retrieved / num_retrieved;
			// recall_list[num_retrieved] = num_relevant_retrieved / queryNo_numOfRelevantDocs_.get(queryNo);
			// would return an array of doubles
			double num_relevant_retrieved = 0;
			// $num_prec in trec_eval
			double sum_prec = 0;

			// docno_score is the href in trec_eval
			Map<String, Double> docno_score = trec_queryNo_docno_score.get(queryNo);

			// Now sort doc IDs based on scores and calculate stats.
			// Note:  Break score ties lexicographically based on doc IDs.
			// Note2: Explicitly quit after 1000 docs to conform to TREC while still
			// handling trec_files with possibly more docs.
			Map<String, Double> docno_score_sortedByScore = sortByComparator(docno_score, DESC);
			
			// For the Precision-Recall Curve
			List<Integer> index_num_retrieved_lst_ = new ArrayList<Integer>();
			// x axis
			List<Double> recall_plot = new ArrayList<Double>();
			// y axis original
			List<Double> prec_plot = new ArrayList<Double>();
			// y axis interpolated
			List<Double> interpolated_prec_plot = new ArrayList<Double>();
			
			// List of relevance, e.g. {0, 0, 2, 1, 2, ...}
			List<Integer> relevance_lst = new ArrayList<Integer>();

			for (String docno : docno_score_sortedByScore.keySet()) {

				// New retrieved doc
				num_retrieved++;

				// Doc's relevance
				int relevance = 0;
				if (qrel_queryNo_docno_relevance_.get(queryNo).containsKey(docno)){
					relevance = qrel_queryNo_docno_relevance_.get(queryNo).get(docno);

					if (relevance != 0){
						relevance = 1;
						sum_prec += relevance * (1 + num_relevant_retrieved) / num_retrieved;
						num_relevant_retrieved++;
						// index of number retrieved
						index_num_retrieved_lst_.add(num_retrieved);
					}
				}
				
				prec_list[num_retrieved] = num_relevant_retrieved / num_retrieved;
				recall_list[num_retrieved] = num_relevant_retrieved / queryNo_numOfRelevantDocs_.get(queryNo);

				relevance_lst.add(relevance);

				if (num_retrieved >= 1000)
					break;
			}
			
			for (int index : index_num_retrieved_lst_) {
				prec_plot.add(prec_list[index]);
			}
			for (int index : index_num_retrieved_lst_) {
				recall_plot.add(recall_list[index]);
			}

			double avg_prec = sum_prec / queryNo_numOfRelevantDocs_.get(queryNo);

			// Fill out the remainder of the precision/recall lists, if necessary
			// In homework 5, this case will never happen
			double final_recall = num_relevant_retrieved / queryNo_numOfRelevantDocs_.get(queryNo);
			for (int i = num_retrieved + 1; i <= 1000; i++) {
				prec_list[i] = num_relevant_retrieved / i;
				recall_list[i] = final_recall;
			}

			// Now calculate precision at document cutoff levels and R-precision.
			// Note that arrays are indexed starting at 0...

			// CUTOFFS.LENGTH + 1??
			double[] prec_at_cutoffs = new double[cutoffs_.length];
			double[] recall_at_cutoffs = new double[cutoffs_.length];
			double[] f1_measure_at_cutoffs = new double[cutoffs_.length];
			int i = 0;
			for (int cutOff : cutoffs_) {
				prec_at_cutoffs[i] = prec_list[cutOff];
				
				// Now calculating F1-Measure
				recall_at_cutoffs[i] = recall_list[cutOff];
				f1_measure_at_cutoffs[i] = compute_f1_measure(prec_at_cutoffs[i], recall_at_cutoffs[i]);
				i++;
			}

			// Now calculate R-precision.  We'll be a bit anal here and
			// actually interpolate if the number of relevant docs is not
			// an integer...
			// This case won't happen in homework 5
			double r_prec = 0;
			if (queryNo_numOfRelevantDocs_.get(queryNo) > num_retrieved){
				r_prec = num_relevant_retrieved / queryNo_numOfRelevantDocs_.get(queryNo); 
			} else {
				int int_num_relevance = (int)(queryNo_numOfRelevantDocs_.get(queryNo)); // Integer part
				double frac_num_relevance = queryNo_numOfRelevantDocs_.get(queryNo) - int_num_relevance; // Fractional part
				r_prec = (frac_num_relevance > 0) ? (1 - frac_num_relevance) * prec_list[int_num_relevance] +
						frac_num_relevance * prec_list[int_num_relevance] :
							prec_list[int_num_relevance];
			}

			// Now calculate interpolated precisions...
			double max_prec = 0;
			for (int j = 1000; j >= 1; j--) {
				if (prec_list[j] > max_prec)
					max_prec = prec_list[j];
				else 
					prec_list[j] = max_prec;
			}

			// Initialize interpolated res
			for (int j = 0; j < index_num_retrieved_lst_.size(); j++) {
				interpolated_prec_plot.add(prec_plot.get(j));				
			}
			
			double max_prec_plot = 0;
			for (int j = index_num_retrieved_lst_.size() - 1; j >= 0 ; j--) {
				if (interpolated_prec_plot.get(j) > max_prec_plot)
					max_prec_plot = interpolated_prec_plot.get(j);
				else 
					interpolated_prec_plot.set(j, max_prec_plot);				
			}

			// Calculate precision at recall levels
			double[] prec_at_recalls = new double[recalls_.length];
			
			int k = 1;
			i = 0;
			for (double recall : recalls_) {
				while (k <= 1000 && recall_list[k] < recall)
					k++;
				if (k <= 1000)
					prec_at_recalls[i++] = prec_list[k];
				else
					prec_at_recalls[i++] = 0;
			}
			
			// Finally, compute nDCG
			double dCG = compute_dcg(relevance_lst);
//			List<Integer> sorted_relevance_lst = new ArrayList<Integer>();
			Collections.sort(relevance_lst);
			Collections.reverse(relevance_lst);
			double nDCG = dCG / compute_dcg(relevance_lst);

			// Print stats on a per query basis if requested
			if (print_all_queries_){
				print_to_console(queryNo, num_retrieved, queryNo_numOfRelevantDocs_.get(queryNo), num_relevant_retrieved,
						prec_at_recalls, avg_prec, prec_at_cutoffs, r_prec,
						f1_measure_at_cutoffs, nDCG);
			}

			// Now update running sums for overall stats
			total_num_retrieved_ += num_retrieved;
			total_num_relevant_ += queryNo_numOfRelevantDocs_.get(queryNo);
			total_num_relevant_retrieved_ += num_relevant_retrieved;

			for (int j = 0; j < cutoffs_.length; j++) {
				sum_prec_at_cutoffs_[j] += prec_at_cutoffs[j];
				sum_f1_measure_at_cutoffs_[j] += f1_measure_at_cutoffs[j];
			}

			for (int j = 0; j < recalls_.length; j++) {
				sum_prec_at_recalls_[j] += prec_at_recalls[j];
			}

			sum_avg_prec_ += avg_prec;
			sum_r_prec += r_prec;
			
			sum_nDCG_ += nDCG;

			write_precision_recall_curve_data(recall_plot, prec_plot, interpolated_prec_plot);
		}

		// Now calculate summary stats
		double[] avg_prec_at_cutoffs = new double[cutoffs_.length];
		double[] avg_prec_at_recalls = new double[recalls_.length];
		double[] avg_f1_measure = new double[recalls_.length];
		for (int j = 0; j < cutoffs_.length; j++) {
			avg_prec_at_cutoffs[j] = sum_prec_at_cutoffs_[j] / num_topics_;
			avg_f1_measure[j] = sum_f1_measure_at_cutoffs_[j] / num_topics_;
		}

		for (int j = 0; j < avg_prec_at_recalls.length; j++) {
			avg_prec_at_recalls[j] = sum_prec_at_recalls_[j] / num_topics_;
		}

		double mean_avg_prec = sum_avg_prec_ / num_topics_;
		double avg_r_prec = sum_r_prec / num_topics_;
		
		double avg_nDCG = sum_nDCG_ / num_topics_;

		print_to_console(num_topics_ + "", total_num_retrieved_, total_num_relevant_, total_num_relevant_retrieved_,
				avg_prec_at_recalls, mean_avg_prec, avg_prec_at_cutoffs, avg_r_prec,
				avg_f1_measure, avg_nDCG);		
	}

	private double compute_f1_measure(double prec, double cutoff) {
		double res = 0;
		if (prec + cutoff == 0)
			return res;
		res = 2 * prec * cutoff / (prec + cutoff);
		return res;
	}
	
	/**
	 * Compute the Discounted Cumulative Gain for the relevance vector
	 * @param relevance_lst
	 * @return
	 */
	private double compute_dcg(List<Integer> relevance_lst){
		double res = 0;
		double temp = 0;
		for (int i = 1; i < relevance_lst.size(); i++) {
			temp += (double)relevance_lst.get(i) / Math.log10(i + 1);
		}
		res = temp + relevance_lst.get(0);
		return res;
	}

	private void write_precision_recall_curve_data(List<Double> recall_list, List<Double> prec_list, List<Double> interpolated_prec_plot) throws IOException {
		BufferedWriter out_prec_recall_curves = null;

		FileWriter ostream = new FileWriter(Constants.prec_recall_curves);
		out_prec_recall_curves = new BufferedWriter(ostream);

		for (int i = 0; i < recall_list.size(); i++) {
			out_prec_recall_curves.write(recall_list.get(i) + " ");
		}
		out_prec_recall_curves.write(Constants.newline_);
		
		for (int i = 0; i < prec_list.size(); i++) {
			out_prec_recall_curves.write(prec_list.get(i) + " ");
		}
		out_prec_recall_curves.write(Constants.newline_);
		
		for (int i = 0; i < interpolated_prec_plot.size(); i++) {
			out_prec_recall_curves.write(interpolated_prec_plot.get(i) + " ");
		}
		out_prec_recall_curves.write(Constants.newline_);

		out_prec_recall_curves.close();
	}

	private void print_to_console(String queryNo, int num_retrieved, int relevant, double num_relevant_retrieved,
			double[] interpolated_recall_prec, double avg_prec, double[] prec_all_relevant, double r_prec,
			double [] f1_measure, double nDCG) {

		System.out.print("\n");
		System.out.print("Queryid (Num):    " + queryNo + "\n");
		System.out.print("Total number of documents over all queries\n");
		System.out.print("    Retrieved:    " + num_retrieved + "\n");
		System.out.print("    Relevant:     " + relevant + "\n");
		System.out.print("    Rel_ret:      " + num_relevant_retrieved + "\n");
		System.out.print("Interpolated Recall - Precision Averages:\n");
		System.out.print("    at 0.00       " + new DecimalFormat("#0.0000").format(interpolated_recall_prec[0]) + "\n");
		System.out.print("    at 0.10       " + new DecimalFormat("#0.0000").format(interpolated_recall_prec[1]) + "\n");
		System.out.print("    at 0.20       " + new DecimalFormat("#0.0000").format(interpolated_recall_prec[2]) + "\n");
		System.out.print("    at 0.30       " + new DecimalFormat("#0.0000").format(interpolated_recall_prec[3]) + "\n");
		System.out.print("    at 0.40       " + new DecimalFormat("#0.0000").format(interpolated_recall_prec[4]) + "\n");
		System.out.print("    at 0.50       " + new DecimalFormat("#0.0000").format(interpolated_recall_prec[5]) + "\n");
		System.out.print("    at 0.60       " + new DecimalFormat("#0.0000").format(interpolated_recall_prec[6]) + "\n");
		System.out.print("    at 0.70       " + new DecimalFormat("#0.0000").format(interpolated_recall_prec[7]) + "\n");
		System.out.print("    at 0.80       " + new DecimalFormat("#0.0000").format(interpolated_recall_prec[8]) + "\n");
		System.out.print("    at 0.90       " + new DecimalFormat("#0.0000").format(interpolated_recall_prec[9]) + "\n");
		System.out.print("    at 1.00       " + new DecimalFormat("#0.0000").format(interpolated_recall_prec[10]) + "\n");
		System.out.print("Average precision (non-interpolated) for all rel docs (averaged over queries)\n");
		System.out.print("                  " + new DecimalFormat("#0.0000").format(avg_prec) + "\n");
		System.out.print("Precision:\n");
		System.out.print("  At    5 docs:   " + new DecimalFormat("#0.0000").format(prec_all_relevant[0]) + "\n");
		System.out.print("  At   10 docs:   " + new DecimalFormat("#0.0000").format(prec_all_relevant[1]) + "\n");
		System.out.print("  At   15 docs:   " + new DecimalFormat("#0.0000").format(prec_all_relevant[2]) + "\n");
		System.out.print("  At   20 docs:   " + new DecimalFormat("#0.0000").format(prec_all_relevant[3]) + "\n");
		System.out.print("  At   30 docs:   " + new DecimalFormat("#0.0000").format(prec_all_relevant[4]) + "\n");
		System.out.print("  At  100 docs:   " + new DecimalFormat("#0.0000").format(prec_all_relevant[5]) + "\n");
		System.out.print("  At  200 docs:   " + new DecimalFormat("#0.0000").format(prec_all_relevant[6]) + "\n");
		System.out.print("  At  500 docs:   " + new DecimalFormat("#0.0000").format(prec_all_relevant[7]) + "\n");
		System.out.print("  At 1000 docs:   " + new DecimalFormat("#0.0000").format(prec_all_relevant[8]) + "\n");
		System.out.print("F1-Measure:\n");
		System.out.print("  At    5 docs:   " + new DecimalFormat("#0.0000").format(f1_measure[0]) + "\n");
		System.out.print("  At   10 docs:   " + new DecimalFormat("#0.0000").format(f1_measure[1]) + "\n");
		System.out.print("  At   15 docs:   " + new DecimalFormat("#0.0000").format(f1_measure[2]) + "\n");
		System.out.print("  At   20 docs:   " + new DecimalFormat("#0.0000").format(f1_measure[3]) + "\n");
		System.out.print("  At   30 docs:   " + new DecimalFormat("#0.0000").format(f1_measure[4]) + "\n");
		System.out.print("  At  100 docs:   " + new DecimalFormat("#0.0000").format(f1_measure[5]) + "\n");
		System.out.print("  At  200 docs:   " + new DecimalFormat("#0.0000").format(f1_measure[6]) + "\n");
		System.out.print("  At  500 docs:   " + new DecimalFormat("#0.0000").format(f1_measure[7]) + "\n");
		System.out.print("  At 1000 docs:   " + new DecimalFormat("#0.0000").format(f1_measure[8]) + "\n");
		System.out.print("R-Precision (precision after R (= num_rel for a query) docs retrieved:\n");
		System.out.print("    Exact:        " + new DecimalFormat("#0.0000").format(r_prec) + "\n");
		System.out.print("nDCG:\n");
		System.out.print("    Exact:        " + new DecimalFormat("#0.0000").format(nDCG) + "\n");
	}

	private Map<String, Double> sortByComparator(Map<String, Double> unsortMap, final boolean order){

		List<Entry<String, Double>> list = new LinkedList<Entry<String, Double>>(unsortMap.entrySet());

		// Sorting the list based on values
		Collections.sort(list, new Comparator<Entry<String, Double>>()
				{
			public int compare(Entry<String, Double> o1,
					Entry<String, Double> o2)
			{
				if (order)
				{
					return o1.getValue().compareTo(o2.getValue());
				}
				else
				{
					return o2.getValue().compareTo(o1.getValue());

				}
			}
				});

		// Maintaining insertion order with the help of LinkedList
		Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
		for (Entry<String, Double> entry : list)
		{
			sortedMap.put(entry.getKey(), entry.getValue());
		}

		return sortedMap;
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		if (args.length < 3 || args.length > 4){
			System.out.println("Usage:  trec_eval [-q] <qrel_file> <trec_file>\n\n");
			System.exit(-1);
		} else {
			if (args.length == 4){
				Boolean print_all_queries = true;
				String qrel_file = args[2];
				String trec_file = args[3];
				Treceval eval = new Treceval(qrel_file, trec_file, print_all_queries);
				eval.readFiles();
				eval.processTrecData();
			} else {
				// args.length = 3
				Boolean print_all_queries = false;
				String qrel_file = args[1];
				String trec_file = args[2];
				Treceval eval = new Treceval(qrel_file, trec_file, print_all_queries);				
				eval.readFiles();
				eval.processTrecData();
			}
		}
	}

}
