package com.renren.seo.monitor.outservice.advisor;

import com.renren.seo.monitor.outservice.obj.MonitorBasicInfo;

public interface Advisor {
	
	public void doBegin(MonitorBasicInfo basicInfo);
	
	public void doEnd(MonitorBasicInfo basicInfo);
	
	public void doException(MonitorBasicInfo basicInfo, Throwable t);
}
