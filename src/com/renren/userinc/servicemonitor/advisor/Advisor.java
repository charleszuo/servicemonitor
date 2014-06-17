package com.renren.userinc.servicemonitor.advisor;

import com.renren.userinc.servicemonitor.bean.MonitorInfoBean;

public interface Advisor {
	
	public void doBegin(MonitorInfoBean monitorInfo);
	
	public void doEnd(MonitorInfoBean monitorInfo);
	
	public void doException(MonitorInfoBean monitorInfo, Throwable t);
}
