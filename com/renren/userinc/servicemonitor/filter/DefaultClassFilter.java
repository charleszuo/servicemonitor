package com.renren.userinc.servicemonitor.filter;

import com.renren.userinc.servicemonitor.core.Constants;
import com.renren.userinc.servicemonitor.util.PropertiesKey;
import com.renren.userinc.servicemonitor.util.PropertiesService;

public class DefaultClassFilter implements ClassFilter{
	
	private static DefaultClassFilter classFilter = new DefaultClassFilter();
	
	private String[] whiteList;
	
	private String[] blackList;
	
	private DefaultClassFilter(){
		String whiteListStr = PropertiesService.getInstance().getProperty(PropertiesKey.CLASSFILTER_WHITE_LIST);
		String blackListStr = PropertiesService.getInstance().getProperty(PropertiesKey.CLASSFILTER_BLACK_LIST);
		whiteList = whiteListStr.split(Constants.SEPARATOR_COMMA);
		blackList = blackListStr.split(Constants.SEPARATOR_COMMA);
	}
	
	public static DefaultClassFilter getInstance(){
		return classFilter;
	}

	@Override
	public boolean accept(String className) {
		for(String item : whiteList){
			if(className.matches(item)){
				return true;
			}
		}
		
		return false;
	}

	@Override
	public boolean reject(String className) {
		for(String item : blackList){
			if(className.matches(item)){
				return true;
			}
		}
		
		return false;
	}
	
	public static void main(String[] args){
		DefaultClassFilter classFilter = DefaultClassFilter.getInstance();
		
		System.out.println(classFilter.accept("MenuHome"));
		System.out.println(classFilter.reject("com.xiaonei.platform.core.opt.base.CookieManager"));
	}
}
