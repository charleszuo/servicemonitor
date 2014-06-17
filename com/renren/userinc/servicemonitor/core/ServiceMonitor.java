package com.renren.userinc.servicemonitor.core;

import java.util.List;

import com.renren.userinc.servicemonitor.advice.Advice;
import com.renren.userinc.servicemonitor.bean.MonitorInfoBean;

public class ServiceMonitor {
	private List<Advice> advisors;
	
	public void setAdvisors(List<Advice> advisors){
		this.advisors = advisors;
	}
	
	public void begin(MonitorInfoBean basicInfo){
		for(Advice advisor: advisors){
			advisor.doBegin(basicInfo);
		}
	}
	
	public void end(MonitorInfoBean basicInfo){
		for(Advice advisor: advisors){
			advisor.doEnd(basicInfo);
		}
	}
	
	public void handleException(MonitorInfoBean basicInfo, Throwable t){
		for(Advice advisor: advisors){
			advisor.doException(basicInfo, t);
		}
	}
}
