package com.renren.seo.monitor.outservice.advisor;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.renren.seo.monitor.outservice.obj.MonitorBasicInfo;

public class LogAdvisor implements Advisor{

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	@Override
	public void doBegin(MonitorBasicInfo basicInfo) {
		System.out.println(sdf.format(Calendar.getInstance().getTime()) + "--" + basicInfo.toString() + "begin");
	}

	@Override
	public void doEnd(MonitorBasicInfo basicInfo) {
		System.out.println(sdf.format(Calendar.getInstance().getTime()) + "--" + basicInfo.toString() + "end");
	}

	@Override
	public void doException(MonitorBasicInfo basicInfo, Throwable t) {
		// 抛Exception了要先打印end信息
		doEnd(basicInfo);
		System.out.println(sdf.format(Calendar.getInstance().getTime()) + "--" + basicInfo.toString() + "Exception: " + t.getMessage());
	}

}