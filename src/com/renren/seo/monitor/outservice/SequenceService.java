package com.renren.seo.monitor.outservice;

import java.util.concurrent.atomic.AtomicLong;

public class SequenceService {
	private static AtomicLong sequence = new AtomicLong();
	
	public static long getSequence(){
		long seq = sequence.incrementAndGet();
		if(seq == Long.MAX_VALUE){
			sequence.set(0);
		}
		return seq;
	}
}
