package com.ir.tokenizer;

public class Posn_Entry implements Comparable<Posn_Entry>
{	
	public Posn_Entry (String term, int index, int posn, Boolean lastItem) {
		this.term_ = term;
		this.index_ = index;
		this.posn_ = posn;
		this.lastItem_ = lastItem;
	}
	
	public String term_;
	public int index_;
	public int posn_;
	
	// If it's the last posn in the list, don't poll it
	public Boolean lastItem_;

	public int compareTo(Posn_Entry entry) {
		return this.posn_ - entry.posn_;
	}
} 