package com.renren.userinc.servicemonitor.filter;

public interface ClassFilter {
	public boolean accept(String className);
	
	public boolean reject(String className);
}
