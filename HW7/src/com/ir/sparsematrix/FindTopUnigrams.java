package com.ir.sparsematrix;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.ir.util.Constants;

public class FindTopUnigrams {

	public Map<Integer, String> termId_term_map_ = new TreeMap<Integer, String>();
	
	public static boolean ASC = true;
    public static boolean DESC = false;

	private void printTopNUnigrams() throws FileNotFoundException, IOException {
		
		Map<Integer, Double> termId_score_map = get_linearModel_termId_score_map();
		Map<Integer, Double> sorted_termId_score_map = sortByComparator(termId_score_map, DESC);
		Map<Integer, Double> top_n_sorted_termId_score_map = getTopNMap(sorted_termId_score_map, Constants.topNUnigrams);

		try (BufferedReader br = new BufferedReader(new FileReader(Constants.term_termId))) {
			String line = "";
			while ((line = br.readLine()) != null) {
				String[] term_termId = line.split(" ");
				termId_term_map_.put(Integer.parseInt(term_termId[1]), term_termId[0]);
			}
		}
		
		for (int termId : top_n_sorted_termId_score_map.keySet()) {
			System.out.println(termId_term_map_.get(termId) + " " + top_n_sorted_termId_score_map.get(termId));
		}
	}

	private Map<Integer, Double> get_linearModel_termId_score_map() throws FileNotFoundException, IOException{
		Map<Integer, Double> termId_score_map = new HashMap<Integer, Double>();

		int counter = 6;
		try (BufferedReader br = new BufferedReader(new FileReader(Constants.linearModel))) {
			String line = "";
			while (counter > 0 && (line = br.readLine()) != null) {
				counter--;
				System.out.println("Not read in because it's not a score: " + line);
			}
			
			int termId = 1;
			line = "";
			while ((line = br.readLine()) != null) {
				termId_score_map.put(termId, Double.parseDouble(line));
				termId++;
			}
		}

		return termId_score_map;
	}
	
	private static Map<Integer, Double> sortByComparator(Map<Integer, Double> unsortMap, final boolean order)
    {

        List<Entry<Integer, Double>> list = new LinkedList<Entry<Integer, Double>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Entry<Integer, Double>>()
        {
            public int compare(Entry<Integer, Double> o1,
                    Entry<Integer, Double> o2)
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
        Map<Integer, Double> sortedMap = new LinkedHashMap<Integer, Double>();
        for (Entry<Integer, Double> entry : list)
        {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
    
    private static Map<Integer, Double> getTopNMap(Map<Integer, Double> map, int n){
    	Map<Integer, Double> res = new LinkedHashMap<Integer, Double>();

    	int counter = 0;
    	for (int key : map.keySet()) {
    		res.put(key, map.get(key));
    		counter++;
    		if (counter == n)
    			return res;
		}
    	
		return map;
    }

    public static void printMap(Map<String, Integer> map)
    {
        for (Entry<String, Integer> entry : map.entrySet())
        {
            System.out.println("Key : " + entry.getKey() + " Value : "+ entry.getValue());
        }
    }

	public static void main(String[] args) throws FileNotFoundException, IOException {
		FindTopUnigrams f = new FindTopUnigrams();
		f.printTopNUnigrams();
//		Map<Integer, Double> unsortMap = new HashMap<Integer, Double>();
//		unsortMap.put(0, 0.2);
//		unsortMap.put(1, 3.2);
//		unsortMap.put(2, 5.2);
//		unsortMap.put(3, 0.7);
//		unsortMap.put(4, 0.2);
//		unsortMap.put(5, 0.12);
//		unsortMap.put(6, 9.12);
//		unsortMap.put(7, 0.29);
//		Map<Integer, Double> sorted = sortByComparator(unsortMap, DESC);
//		for (int num : sorted.keySet()) {
//			System.out.println(num + " " + sorted.get(num));
//		}
	}

}
