package com.renren.seo.monitor.outservice.advisor;

import java.util.List;

import com.renren.seo.monitor.outservice.obj.MonitorBasicInfo;

public class ServiceMonitor {
	private List<Advisor> advisors;
	
	public void setAdvisors(List<Advisor> advisors){
		this.advisors = advisors;
	}
	
	public void begin(MonitorBasicInfo basicInfo){
		for(Advisor advisor: advisors){
			advisor.doBegin(basicInfo);
		}
	}
	
	public void end(MonitorBasicInfo basicInfo){
		for(Advisor advisor: advisors){
			advisor.doEnd(basicInfo);
		}
	}
	
	public void handleException(MonitorBasicInfo basicInfo, Throwable t){
		for(Advisor advisor: advisors){
			advisor.doException(basicInfo, t);
		}
	}
}
