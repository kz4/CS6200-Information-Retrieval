package com.ir.uti;

public class Catalog implements Comparable<Catalog> {

	public Catalog (int lstName, int index, String term) {
		this.catName_ = lstName;
		this.index_ = index;
		this.term_ = term;
	}
	
	public int catName_;
	public int index_;
	public String term_;

	public int compareTo(Catalog entry) {
		return this.term_.compareToIgnoreCase(entry.term_);
	}
}
