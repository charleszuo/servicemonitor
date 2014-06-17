package com.renren.userinc.servicemonitor.core;

import java.util.ArrayList;
import java.util.List;

import com.renren.userinc.servicemonitor.advisor.Advisor;
import com.renren.userinc.servicemonitor.advisor.LogAdvisor;

public class ServiceMonitorFactory {
	private static ServiceMonitor serviceMonitor = new ServiceMonitor();
	private static List<Advisor> advisors = new ArrayList<Advisor>();
	private static Advisor logAdvisor = new LogAdvisor();
	static {
		advisors.add(logAdvisor);
		serviceMonitor.setAdvisors(advisors);
	}
	
	public static void addAdvisor(Advisor advisor){
		advisors.add(advisor);
	}
	
	public static ServiceMonitor getServiceMonitor(){
		return serviceMonitor;
	}
	
}
