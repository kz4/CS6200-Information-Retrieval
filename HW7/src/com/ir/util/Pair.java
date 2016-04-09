package com.ir.util;

import java.util.Collections;
import java.util.TreeSet;

public class Pair implements Comparable<Pair>{
	private String docid;
	private double result;

	public Pair() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Pair(String docid, double result) {
		super();
		this.docid = docid;
		this.result = result;
	}

	public String getDocid() {
		return docid;
	}

	public void setDocid(String docid) {
		this.docid = docid;
	}

	public double getResult() {
		return result;
	}

	public void setResult(double result) {
		this.result = result;
	}

	@Override
	public String toString() {
		return "Pair [docid=" + docid + ", result=" + result + "]";
	}

	@Override
	public int compareTo(Pair pair) {
		String this_pair = String.format("%.17f", this.result) + this.docid;
		String other_pair = String.format("%.17f", pair.getResult()) + pair.getDocid();
		return this_pair.compareTo(other_pair);
	}
	
	public static void main(String[] args) {
		Pair pair = new Pair("a", 0.222);
		Pair pair2 = new Pair("b", 0.2322);
		Pair pair3 = new Pair("c", 0.9999);
		Pair pair4 = new Pair("d", 0.9999);
		Pair pair5 = new Pair("e", 0.9999);
		TreeSet<Pair> treeSet = new TreeSet<Pair>(Collections.reverseOrder());
		treeSet.add(pair);
		treeSet.add(pair2);
		treeSet.add(pair3);
		treeSet.add(pair4);
		treeSet.add(pair5);
		System.out.println(treeSet);
	}

}
