package com.renren.userinc.servicemonitor.bean;

import com.renren.userinc.servicemonitor.util.SequenceService;

public class MonitorInfoBean {
	private String appId;
	
	private String className;
	
	private String methodName;
	
	private String ip;
	
	private String infoString;

	private long sequenceNumber;
	
	public MonitorInfoBean(){
		sequenceNumber = SequenceService.getSequence();
	}
	
	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}
	
	public String toString(){
		if(infoString == null){
			StringBuilder sb = new StringBuilder();
			sb.append("Service Monitor Log--").append("AppId: ").append(getAppId()).append("--").append("Class: ").append(getClassName())
			.append("--").append("Method: ").append(getMethodName())
			.append("--").append("IP: ").append(getIp())
			.append("--").append("Sequence: ").append(String.valueOf(sequenceNumber)).append("--");
			infoString = sb.toString();
		}
		return infoString;
	}
	
}

