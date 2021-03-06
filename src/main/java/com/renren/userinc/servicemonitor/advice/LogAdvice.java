package com.renren.userinc.servicemonitor.advice;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.renren.userinc.servicemonitor.bean.MonitorInfoBean;

public class LogAdvice implements Advice{

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	@Override
	public void doBegin(MonitorInfoBean monitorInfo) {
		System.out.println(sdf.format(Calendar.getInstance().getTime()) + "--" + monitorInfo.toString() + "begin");
	}

	@Override
	public void doEnd(MonitorInfoBean monitorInfo) {
		System.out.println(sdf.format(Calendar.getInstance().getTime()) + "--" + monitorInfo.toString() + "end");
	}

	@Override
	public void doException(MonitorInfoBean monitorInfo, Throwable t) {
		// 抛Exception了要先打印end信息
		doEnd(monitorInfo);
		System.out.println(sdf.format(Calendar.getInstance().getTime()) + "--" + monitorInfo.toString() + "Exception: " + t.toString());
	}

}