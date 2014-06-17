package com.renren.userinc.servicemonitor.core;

import java.util.ArrayList;
import java.util.List;

import com.renren.userinc.servicemonitor.advice.Advice;
import com.renren.userinc.servicemonitor.advice.LogAdvice;

public class ServiceMonitorFactory {
	private static ServiceMonitor serviceMonitor = new ServiceMonitor();
	private static List<Advice> advisors = new ArrayList<Advice>();
	private static Advice logAdvisor = new LogAdvice();
	static {
		advisors.add(logAdvisor);
		serviceMonitor.setAdvisors(advisors);
	}
	
	public static void addAdvisor(Advice advisor){
		advisors.add(advisor);
	}
	
	public static ServiceMonitor getServiceMonitor(){
		return serviceMonitor;
	}
	
}
