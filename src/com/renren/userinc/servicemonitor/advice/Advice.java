package com.renren.userinc.servicemonitor.advice;

import com.renren.userinc.servicemonitor.bean.MonitorInfoBean;

public interface Advice {
	
	public void doBegin(MonitorInfoBean monitorInfo);
	
	public void doEnd(MonitorInfoBean monitorInfo);
	
	public void doException(MonitorInfoBean monitorInfo, Throwable t);
}
