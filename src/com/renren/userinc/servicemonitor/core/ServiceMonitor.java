package com.renren.userinc.servicemonitor.core;

import java.util.List;

import com.renren.userinc.servicemonitor.advisor.Advisor;
import com.renren.userinc.servicemonitor.bean.MonitorInfoBean;

public class ServiceMonitor {
	private List<Advisor> advisors;
	
	public void setAdvisors(List<Advisor> advisors){
		this.advisors = advisors;
	}
	
	public void begin(MonitorInfoBean basicInfo){
		for(Advisor advisor: advisors){
			advisor.doBegin(basicInfo);
		}
	}
	
	public void end(MonitorInfoBean basicInfo){
		for(Advisor advisor: advisors){
			advisor.doEnd(basicInfo);
		}
	}
	
	public void handleException(MonitorInfoBean basicInfo, Throwable t){
		for(Advisor advisor: advisors){
			advisor.doException(basicInfo, t);
		}
	}
}
