package com.renren.userinc.servicemonitor.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;


public class MetaDataService {
	private static MetaDataService metaDataService = new MetaDataService();
	
	private Map<String, String> metaDataMap = new HashMap<String, String>();
	
	private MetaDataService(){
		InputStream is = MetaDataService.class.getResourceAsStream("/metaData.txt");
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String str = null;
		try {
			while((str = br.readLine()) != null){
				String[] metaDataArray = str.split(" ");
				//put className.methodName : id
				metaDataMap.put(metaDataArray[1] + "." + metaDataArray[2], metaDataArray[0]);
				//put methodName : id
				metaDataMap.put(metaDataArray[2], metaDataArray[0]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static MetaDataService getInstance(){
		return metaDataService;
	}
	
	
	public Map<String, String> getMetaDataMap(){
		return metaDataMap;
	}
}
