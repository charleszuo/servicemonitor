package com.renren.userinc.servicemonitor.util;

import java.io.IOException;
import java.util.Properties;

public class PropertiesService {
	private static PropertiesService propertiesManager = new PropertiesService();
	
	private Properties p;
	
	private final String propertiesFileName = "/properties/servicemonitor.properties";
	
	private PropertiesService(){
		p = new Properties();
		try {
			p.load(this.getClass().getResourceAsStream(propertiesFileName));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static PropertiesService getInstance(){
		return propertiesManager;
	}
	
	public String getProperty(String key){
		return p.getProperty(key);
	}
	
	public boolean isWindowsOS(){
		String projectDir = getProperty(PropertiesKey.MONITOR_PROJECT_DIR);
		if(projectDir.contains("\\")){
			return true;
		}
		return false;
	}
	
	public static void main(String[] args){
		System.out.println(PropertiesService.getInstance().isWindowsOS());
		System.out.println(".*(/com/.*(xiaonei|renren|xce/).*\\.jar).*");
		System.out.println(PropertiesService.getInstance().getProperty(PropertiesKey.JAR_SCOPE));
	}
	
}
