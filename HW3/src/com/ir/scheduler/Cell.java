package com.ir.scheduler;

import com.ir.crawler.Link;

public class Cell implements Comparable <Cell>  {
	
	public int waveDepth_;
	public int inLinkCounts_;
	public int arriveTime_;
	public String url_;
	
	@Override
	public int compareTo(Cell f) {

		if (waveDepth_ < f.waveDepth_)
			return -1;
		else if (waveDepth_ > f.waveDepth_)
			return 1;
		else{
			if (inLinkCounts_ > f.inLinkCounts_)
				return -1;
			else if (inLinkCounts_ < f.inLinkCounts_)
				return 1;
			else{
				if (arriveTime_ < f.arriveTime_)
					return -1;
				else if (arriveTime_ > f.arriveTime_)
					return 1;
			}
		}
		return 0;
	}

}
