package com.renren.userinc.servicemonitor.proxy;

import java.lang.reflect.Method;
import java.util.Map;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import com.renren.userinc.servicemonitor.bean.MonitorInfoBean;
import com.renren.userinc.servicemonitor.core.ServiceMonitor;
import com.renren.userinc.servicemonitor.core.ServiceMonitorFactory;
import com.renren.userinc.servicemonitor.util.IPService;
import com.renren.userinc.servicemonitor.util.MetaDataService;

public class CglibDynamicProxy implements MethodInterceptor {
	private Object target;

	public Object bind(Object target) {
		this.target = target;
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(this.target.getClass());
		enhancer.setCallback(this);
		return enhancer.create();
	}

	@Override
	public Object intercept(Object obj, Method method, Object[] args,
			MethodProxy proxy) throws Throwable {
		String className = method.getDeclaringClass().getName();
		// don't handle the method from java
		if (className.startsWith("java")) {
			try {
				method.setAccessible(true);
				Object result = method.invoke(target, args);
				return result;
			} catch (Throwable t) {
				throw t;
			}
		}
		Map<String, String> metaDataMap = MetaDataService.getInstance()
				.getMetaDataMap();

		// for cglib proxy, the method may be inherit from parent. If can't find
		// the className.methodName in the metaData map, try methodName
		String classMethod = className + "."
				+ method.getName();
		String methodId = metaDataMap.get(classMethod);
		if (methodId == null) {
			methodId = metaDataMap.get(method.getName());
		}
		// if method not in the metaDataMap, don't monitor
		if(methodId == null){
			try {
				method.setAccessible(true);
				Object result = method.invoke(target, args);
				return result;
			} catch (Throwable t) {
				throw t;
			}
		}
		
		MonitorInfoBean monitorBasicInfo = new MonitorInfoBean();
		monitorBasicInfo.setAppId("$AppId");
		monitorBasicInfo.setClassName(className);
		monitorBasicInfo.setMethodName(method.getName());
		monitorBasicInfo.setIp(IPService.getLocalIp());
		monitorBasicInfo.setMethodId(methodId);
		ServiceMonitor serviceMonitor = ServiceMonitorFactory
				.getServiceMonitor();
		try {
			// do something before invoke
			serviceMonitor.begin(monitorBasicInfo);
			method.setAccessible(true);
			Object result = method.invoke(target, args);

			// do something after invoke
			serviceMonitor.end(monitorBasicInfo);
			return result;
		} catch (Throwable t) {
			serviceMonitor.handleException(monitorBasicInfo, t);
			throw t;
		}

	}
}