package com.renren.seo.monitor.outservice.advisor;

import java.util.ArrayList;
import java.util.List;

public class ServiceMonitorFactory {
	private static ServiceMonitor serviceMonitor = new ServiceMonitor();
	private static List<Advisor> advisors = new ArrayList<Advisor>();
	private static Advisor logAdvisor = new LogAdvisor();
	static {
		advisors.add(logAdvisor);
		serviceMonitor.setAdvisors(advisors);
	}
	public static ServiceMonitor getServiceMonitor(){
		return serviceMonitor;
	}
	
	public static void addAdvisor(Advisor advisor){
		advisors.add(advisor);
	}
}
