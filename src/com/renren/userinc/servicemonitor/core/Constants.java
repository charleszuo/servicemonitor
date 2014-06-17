package com.renren.userinc.servicemonitor.core;

import com.renren.userinc.servicemonitor.util.PropertiesKey;
import com.renren.userinc.servicemonitor.util.PropertiesService;

public class Constants {
	public static final String CLASS_SCOPE = PropertiesService.getInstance().getProperty(PropertiesKey.CLASS_SCOPE);
	
	// 捕获分组
	public static final String JAR_SCOPE = PropertiesService.getInstance().getProperty(PropertiesKey.JAR_SCOPE);

	public static final String INIT_METHOD = "<init>";

	public static final String CINIT_METHOD = "<cinit>";

	public static final String CLASS_FILE_POSTFIX = ".class";

	public static final String JAVA_FILE_POSTFIX = ".java";

	public static final String INNER_CLASS_FLAG = "$";

	public static final String TEMPLATE_STATIC_CLASS = "templates/staticClass.vm";

	public static final String TEMPLATE_SINGLETON_CLASS = "templates/singletonClass.vm";

	public static final String TEMPLATE_JAVA_DYNAMIC_PROXY = "templates/system/JavaDynamicProxy.vm";

	public static final String TEMPLATE_CGLIB_DYNAMIC_PROXY = "templates/system/CglibDynamicProxy.vm";

	public static final String TEMPLATE_ADVISOR = "templates/system/Advisor.vm";
	
	public static final String TEMPLATE_LOG_ADVISOR = "templates/system/LogAdvisor.vm";
	
	public static final String TEMPLATE_SERVICE_MONITOR = "templates/system/ServiceMointor.vm";
	
	public static final String TEMPLATE_SERVICE_MONITOR_FACTORY = "templates/system/ServiceMonitorFactory.vm";
	
	public static final String TEMPLATE_MONITOR_INFO_BEAN = "templates/system/MonitorInfoBean.vm";
	
	public static final String TEMPLATE_IP_SERVICE = "templates/system/IPService.vm";
	
	public static final String TEMPLATE_SEQ_NUM = "templates/system/SequenceService.vm";
	
	public static final String M2_REPO = PropertiesService.getInstance().getProperty(PropertiesKey.MAVEN_REPO);
	
	public static final String TARGET_PROJECT_NAME = PropertiesService.getInstance().getProperty(PropertiesKey.MONITOR_PROJECT_NAME);

	public static final String TARGET_WORK_SPACE = PropertiesService.getInstance().getProperty(PropertiesKey.MONITOR_PROJECT_DIR);

	public static final String TARGET_SOURCE_PATH = TARGET_WORK_SPACE + "/src/main/java/";

	public static final String TARGET_CLASSES_PATH = TARGET_WORK_SPACE + "/target/classes/";

	public static final String TARGET_CLASSPATH_FILENAME = TARGET_WORK_SPACE + "/.classpath";

	public static final String ACC_STATIC = "static";

	public static final String ACC_NON_STATIC = "non-static";

	public static final String SEPARATOR_COMMA = ",";

	public static final String SEPARATOR_SLASH = "/";

	public static final String SEPARATOR_POINT = ".";

	public static final String TEMPLATE_CONSTANT_TARGET = "target";

	public static final String TEMPLATE_CONSTANT_PROXY_TARGET = "proxyTarget";

	public static final String TEMPLATE_CONSTANT_GET_INSTANCE = "getInstance";

	public static final String XOA_SERVICE_ANNONATION = "Lcom/renren/xoa/lite/annotation/XoaService;";

	public static final int MAX_SEARCH_ROUND = 16;
	
	public static final int CLASS_TYPE_ALL_STATIC_METHOD_CLASS = 1;

	public static final int CLASS_TYPE_BOTH_STATIC_INSTANCE_METHOD_CLASS = 2;

	public static final int CLASS_TYPE_ALL_INSTANCE_METHOD_CLASS = 3;

	public static final int CLASS_TYPE_IS_INTERFACE = 4;

	public static final int CLASS_TYPE_IS_SINGLETON = 5;

	public static final int CLASS_TYPE_IS_XOA_INTERFACE = 6;

	public static final int CLASS_TYPE_RETURN_CLASS_CONTAIN_FINAL_CLASS_FACTORY_METHOD = 7;

	public static final int CLASS_TYPE_RETURN_CLASS_DONOT_HAVE_DEFAULT_CONSTRUCTOR = 8;

	public static final int CLASS_TYPE_IS_INNER_CLASS = 9;

	public static final int METHOD_TYPE_JAVA_PROXY_METHOD = 1;

	public static final int METHOD_TYPE_CGLIB_PROXY_METHOD = 2;

	public static final int METHOD_TYPE_STATIC_PROXY_METHOD = 3;
}
