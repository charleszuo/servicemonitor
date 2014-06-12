package com.renren.seo.monitor.outservice;

public interface ConstantName {
	String CLASS_FILTER = ".*(xiaonei|renren|xce/|mop/).*";
	
	// 捕获分组
	String JAR_FILTER = ".*(/com/.*(xiaonei|renren|xce/).*\\.jar).*";
	
	String INIT_METHOD = "<init>";
	
	String CINIT_METHOD = "<cinit>";
	
	String CLASS_POSTFIX = ".class";
	
	String JAVA_FILE_POSTFIX = ".java";
	
	String SEPARATOR = ",";
	
	String INNER_CLASS_FLAG = "$";
	
	String TEMPLATE_STATIC_CLASS = "templates/staticClass.vm";
	
	String TEMPLATE_SINGLETON_CLASS = "templates/singletonClass.vm";
	
	String TEMPLATE_JAVA_DYNAMIC_PROXY = "templates/JavaDynamicProxy.vm";
	
	String TEMPLATE_CGLIB_DYNAMIC_PROXY = "templates/CglibDynamicProxy.vm";
	
	String M2_REPO = "/home/charles/.m2/repository";
	
	String TARGET_WORK_SPACE = "/home/charles/workspace/schoolname";

	String TARGET_SOURCE_PATH = TARGET_WORK_SPACE + "/src/main/java/";
	
	String TARGET_CLASSES_PATH = TARGET_WORK_SPACE + "/target/classes/";
	
	String TARGET_CLASSPATH_FILENAME = TARGET_WORK_SPACE + "/.classpath";
	
	int MAX_SEARCH_ROUND = 16;
	
	String ACC_STATIC = "static";
	
	String ACC_NON_STATIC = "non-static";
	
	String COMMA = ",";
	
	String SLASH = "/";
	
	String POINT = ".";
	
	String TARGET = "target";
	
	String PROXY_TARGET = "proxyTarget";
	
	String GET_INSTANCE = "getInstance";
	
	String XOA_SERVICE_ANNONATION = "Lcom/renren/xoa/lite/annotation/XoaService;";
	
	int CLASS_TYPE_ALL_STATIC_METHOD_CLASS = 1;
	
	int CLASS_TYPE_BOTH_STATIC_INSTANCE_METHOD_CLASS = 2;
	
	int CLASS_TYPE_ALL_INSTANCE_METHOD_CLASS = 3;
	
	int CLASS_TYPE_IS_INTERFACE = 4;
	
	int CLASS_TYPE_IS_SINGLETON = 5;
	
	int CLASS_TYPE_IS_XOA_INTERFACE = 6;
	
	int CLASS_TYPE_RETURN_CLASS_CONTAIN_FINAL_CLASS_FACTORY_METHOD = 7;
	
	int CLASS_TYPE_RETURN_CLASS_DONOT_HAVE_DEFAULT_CONSTRUCTOR = 8;
	
	int CLASS_TYPE_IS_INNER_CLASS = 9;
	
	int METHOD_TYPE_JAVA_PROXY_METHOD = 1;
	
	int METHOD_TYPE_CGLIB_PROXY_METHOD = 2;
	
	int METHOD_TYPE_STATIC_PROXY_METHOD = 3;
	
}
