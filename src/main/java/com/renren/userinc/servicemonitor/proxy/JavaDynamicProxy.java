package com.renren.userinc.servicemonitor.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import com.renren.userinc.servicemonitor.bean.MonitorInfoBean;
import com.renren.userinc.servicemonitor.core.Constants;
import com.renren.userinc.servicemonitor.core.ServiceMonitor;
import com.renren.userinc.servicemonitor.core.ServiceMonitorFactory;
import com.renren.userinc.servicemonitor.util.IPService;
import com.renren.userinc.servicemonitor.util.MetaDataService;

public class JavaDynamicProxy implements InvocationHandler {
	private Object target;

	public Object bind(Object target) {
		this.target = target;
		return Proxy.newProxyInstance(target.getClass().getClassLoader(),
				target.getClass().getInterfaces(), this);
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
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
		String classMethod = className + "." + method.getName();
		String methodId = metaDataMap.get(classMethod);

		// if method not in the metaDataMap, don't monitor
		if (methodId == null) {
			try {
				method.setAccessible(true);
				Object result = method.invoke(target, args);
				return result;
			} catch (Throwable t) {
				throw t;
			}
		}

		ServiceMonitor serviceMonitor = ServiceMonitorFactory
				.getServiceMonitor();
		MonitorInfoBean monitorBasicInfo = new MonitorInfoBean();
		monitorBasicInfo.setAppId("$AppId");
		monitorBasicInfo.setClassName(method.getDeclaringClass().getName());
		monitorBasicInfo.setMethodName(method.getName());
		monitorBasicInfo.setIp(IPService.getLocalIp());
		monitorBasicInfo.setTime(System.currentTimeMillis());
		monitorBasicInfo.setMethodId(methodId);
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