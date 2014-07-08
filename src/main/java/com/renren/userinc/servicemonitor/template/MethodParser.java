package com.renren.userinc.servicemonitor.template;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.renren.userinc.servicemonitor.bean.MethodObjectBean;
import com.renren.userinc.servicemonitor.core.Constants;
import com.renren.userinc.servicemonitor.util.DBService;

public class MethodParser {
	private static final String TYPE_B = "byte";
	private static final String TYPE_C = "char";
	private static final String TYPE_D = "double";
	private static final String TYPE_F = "float";
	private static final String TYPE_I = "int";
	private static final String TYPE_J = "long";
	private static final String TYPE_S = "short";
	private static final String TYPE_Z = "boolean";
	private static final String TYPE_VOID = "void";
	
	private static final Character CHAR_B = 'B';
	private static final Character CHAR_C = 'C';
	private static final Character CHAR_D = 'D';
	private static final Character CHAR_F = 'F';
	private static final Character CHAR_I = 'I';
	private static final Character CHAR_J = 'J';
	private static final Character CHAR_S = 'S';
	private static final Character CHAR_Z = 'Z';
	
	private static final Character CHAR_L = 'L';
	private static final Character CHAR_ARRAY = '[';
	private static final Character CHAR_VOID = 'V';
	private static final Character CHAR_COLON = ';';
	
	private static final String PARAM = "param";
	private static final String SPACE = " ";
	private static final String COMMA = ",";
	private static final String ARRAY = "[]";
	private static final String VARARGS = "...";
	private static final String BRACKET = "()";
	private static final String LEFT_BRACKET = "(";
	private static final String RIGHT_BRACKET = ")";
	private static final String PUBLIC = "public ";
	private static final String THROWS = "throws ";

	private static Map<Character, String> typeMap;
	
	private static final String reg = "^(\\S*)\\s(\\S*)\\s\\((.*)\\)(\\S*)\\s(\\S*)\\s(\\S*)";
	private static final Pattern p = Pattern.compile(reg);
	
	static{
		typeMap = new HashMap<Character, String>();
		typeMap.put(CHAR_B, TYPE_B);
		typeMap.put(CHAR_C, TYPE_C);
		typeMap.put(CHAR_D, TYPE_D);
		typeMap.put(CHAR_F, TYPE_F);
		typeMap.put(CHAR_I, TYPE_I);
		typeMap.put(CHAR_J, TYPE_J);
		typeMap.put(CHAR_S, TYPE_S);
		typeMap.put(CHAR_Z, TYPE_Z);
		typeMap.put(CHAR_VOID, TYPE_VOID);
	}
	
	public static MethodObjectBean parseMethodDescription(String methodWithStaticAndException, boolean isVarArgs){
		MethodObjectBean method = null;
		Matcher m = p.matcher(methodWithStaticAndException);
		if(m.matches() && m.groupCount() >= 6){
			String ownerClass = m.group(1).replace(Constants.SEPARATOR_SLASH, Constants.SEPARATOR_POINT);
			String methodNameStr = m.group(2);
			String parameterStr = m.group(3);
			String returnStr = m.group(4);
			String accStaticStr = m.group(5);
			String exceptionsStr = m.group(6);
			
			method = new MethodObjectBean();
			method.setMethodName(methodNameStr);
			method.setParameters(parseParameter(parameterStr, isVarArgs));
			method.setAccStatic(parseAccStatic(accStaticStr));
			method.setReturnType(parseReturn(returnStr));
			method.setExceptions(parseException(exceptionsStr));
			method.setClassName(ownerClass);
		}
		return method;
	}
	
	public static String parse(String classMethodDescription, int methodType, boolean isVarArgs){
		StringBuilder sb = new StringBuilder();
		MethodObjectBean methodOject = parseMethodDescription(classMethodDescription, isVarArgs);
		if(methodOject != null){
			sb.append(PUBLIC);
			sb.append(methodOject.getAccStatic()).append(SPACE);
			sb.append(methodOject.getReturnType()).append(SPACE);
			sb.append(methodOject.getMethodName());
			String[] parseParameterResult = methodOject.getParameters();
			sb.append(parseParameterResult[0]).append(SPACE);
			sb.append(methodOject.getExceptions());
			
			//生成方法体
			sb.append(" {\n");
			String caller = null;
			if(Constants.ACC_STATIC.equals(methodOject.getAccStatic())){
				caller = methodOject.getClassName();
			}else{
				caller = Constants.TEMPLATE_CONSTANT_TARGET;
			}
			if(methodType == 0){
				sb.append("\t\tjava.util.Map<String, String> metaDataMap = com.renren.userinc.servicemonitor.system.generated.MetaDataService.getInstance().getMetaDataMap();\n");
				sb.append("\t\tString classMethod = \"").append(methodOject.getClassName()).append(".").append(methodOject.getMethodName()).append("\";\n");
				sb.append("\t\tString methodId = metaDataMap.get(classMethod);\n");
				sb.append("\t\tif(methodId == null){\n");
				sb.append("\t\t\ttry{\n");
				if(!(TYPE_VOID.equals(methodOject.getReturnType()))){
					sb.append("\t\t\t\t").append(methodOject.getReturnType()).append(" result = ");
					sb.append(caller).append(".").append(methodOject.getMethodName()).append("(").append(parseParameterResult[1]).append(");\n");
					sb.append("\t\t\t\treturn result;\n");
				}else{
					sb.append("\t\t\t\t");
					sb.append(caller).append(".").append(methodOject.getMethodName()).append("(").append(parseParameterResult[1]).append(");\n");
				}
				sb.append("\t\t\t}");
				String exceptions = methodOject.getExceptions();
				if(!"".equals(exceptions)){
					String[] exceptionArray = exceptions.substring(6).split(COMMA);
					for(String exception: exceptionArray){
						sb.append("catch(").append(exception).append(" e){\n");
						sb.append("\t\t\t\tthrow e;\n");
						sb.append("\t\t\t}");
					}
				}
				// 对其他可能出现的运行时异常,先catch住记录一下,再抛出去RuntimeException
				sb.append("catch(java.lang.Throwable t){\n");
				sb.append("\t\t\t\tif(t instanceof RuntimeException){\n");
				sb.append("\t\t\t\t\tthrow new RuntimeException(t);\n");
				sb.append("\t\t\t\t}else if(t instanceof Error){\n");
				sb.append("\t\t\t\t\tthrow new Error(t);\n");
				sb.append("\t\t\t\t}else{\n");
				sb.append("\t\t\t\t\tthrow new IllegalStateException(t);\n");
				sb.append("\t\t\t\t}\n");
				sb.append("\t\t\t}\n");
				sb.append("\t\t}else {\n");
				sb.append("\t\t\tcom.renren.userinc.servicemonitor.system.generated.ServiceMonitor serviceMonitor =  com.renren.userinc.servicemonitor.system.generated.ServiceMonitorFactory.getServiceMonitor();\n");
				sb.append("\t\t\tcom.renren.userinc.servicemonitor.system.generated.MonitorInfoBean monitorBasicInfo = new com.renren.userinc.servicemonitor.system.generated.MonitorInfoBean();\n");
				sb.append("\t\t\tmonitorBasicInfo.setAppId(\"").append(DBService.getInstance().getAppId()).append("\");\n");
				sb.append("\t\t\tmonitorBasicInfo.setClassName(\"").append(methodOject.getClassName()).append("\");\n");
				sb.append("\t\t\tmonitorBasicInfo.setMethodName(\"").append(methodOject.getMethodName()).append("\");\n");
				sb.append("\t\t\tmonitorBasicInfo.setIp(com.renren.userinc.servicemonitor.system.generated.IPService.getLocalIp());\n");
				sb.append("\t\t\tmonitorBasicInfo.setTime(System.currentTimeMillis());\n");
				sb.append("\t\t\tmonitorBasicInfo.setMethodId(methodId);\n");
				sb.append("\t\t\ttry{\n");
				if(!(TYPE_VOID.equals(methodOject.getReturnType()))){
					sb.append("\t\t\t\tserviceMonitor.begin(monitorBasicInfo);\n");
					sb.append("\t\t\t\t").append(methodOject.getReturnType()).append(" result = ");
					sb.append(caller).append(".").append(methodOject.getMethodName()).append("(").append(parseParameterResult[1]).append(");\n");
					sb.append("\t\t\t\tserviceMonitor.end(monitorBasicInfo);\n");
					sb.append("\t\t\t\treturn result;\n");
				}else{
					sb.append("\t\t\t\tserviceMonitor.begin(monitorBasicInfo);\n");
					sb.append("\t\t\t\t");
					sb.append(caller).append(".").append(methodOject.getMethodName()).append("(").append(parseParameterResult[1]).append(");\n");
					sb.append("\t\t\t\tserviceMonitor.end(monitorBasicInfo);\n");
				}
				sb.append("\t\t\t}");
				if(!"".equals(exceptions)){
					String[] exceptionArray = exceptions.substring(6).split(COMMA);
					for(String exception: exceptionArray){
						sb.append("catch(").append(exception).append(" e){\n");
						sb.append("\t\t\t\tserviceMonitor.handleException(monitorBasicInfo, e);\n");
						sb.append("\t\t\t\tthrow e;\n");
						sb.append("\t\t\t}");
					}
				}
				// 对其他可能出现的运行时异常,先catch住记录一下,再抛出去RuntimeException
				sb.append("catch(java.lang.Throwable t){\n");
				sb.append("\t\t\t\tserviceMonitor.handleException(monitorBasicInfo, t);\n");
				sb.append("\t\t\t\tif(t instanceof RuntimeException){\n");
				sb.append("\t\t\t\t\tthrow new RuntimeException(t);\n");
				sb.append("\t\t\t\t}else if(t instanceof Error){\n");
				sb.append("\t\t\t\t\tthrow new Error(t);\n");
				sb.append("\t\t\t\t}else{\n");
				sb.append("\t\t\t\t\tthrow new IllegalStateException(t);\n");
				sb.append("\t\t\t\t}\n");
				sb.append("\t\t\t}\n");
				sb.append("\t\t}\n");
				sb.append("\n");
				
			}else if(methodType == Constants.METHOD_TYPE_JAVA_PROXY_METHOD){
				//Text: mop.hi.oce.adapter.BuddyCoreAdapter proxyTarget = mop.hi.oce.adapter.AdapterFactory.getBuddyCoreAdapter();
				//Text: DynamicProxy proxy = new DynamicProxy();
				//Text: return (mop.hi.oce.adapter.BuddyCoreAdapter)proxy.bind(proxyTarget);
				String returnType = methodOject.getReturnType();
				sb.append("\t\t").append(returnType).append(" ").append(Constants.TEMPLATE_CONSTANT_PROXY_TARGET).append(" = ")
					.append(caller).append(".").append(methodOject.getMethodName()).append("(").append(parseParameterResult[1]).append(");\n");
				sb.append("\t\tcom.renren.userinc.servicemonitor.system.generated.JavaDynamicProxy proxy = new com.renren.userinc.servicemonitor.system.generated.JavaDynamicProxy();\n");
				sb.append("\t\t").append("return (").append(returnType).append(")").append("proxy.bind(").append(Constants.TEMPLATE_CONSTANT_PROXY_TARGET).append(");\n");
			}else if(methodType == Constants.METHOD_TYPE_CGLIB_PROXY_METHOD){
				//Text: xce.tripod.client.TripodCacheClient proxyTarget = xce.tripod.client.TripodCacheClientFactory.getClient(param24);
				//Text: CglibDynamicProxy proxy = new CglibDynamicProxy();
				//Text: return (xce.tripod.client.TripodCacheClient)proxy.bind(proxyTarget);
				String returnType = methodOject.getReturnType();
				sb.append("\t\t").append(returnType).append(" ").append(Constants.TEMPLATE_CONSTANT_PROXY_TARGET).append(" = ")
					.append(caller).append(".").append(methodOject.getMethodName()).append("(").append(parseParameterResult[1]).append(");\n");
				sb.append("\t\tcom.renren.userinc.servicemonitor.system.generated.CglibDynamicProxy proxy = new com.renren.userinc.servicemonitor.system.generated.CglibDynamicProxy();\n");
				sb.append("\t\t").append("return (").append(returnType).append(")").append("proxy.bind(").append(Constants.TEMPLATE_CONSTANT_PROXY_TARGET).append(");\n");
			}
			sb.append("\t}\n");
		}
		return sb.toString();
	}
	
	
	public static String parseAccStatic(String accStaticStr){
		StringBuilder accStaticBuilder = new StringBuilder();
		accStaticBuilder.append(Constants.ACC_STATIC.equals(accStaticStr) ? Constants.ACC_STATIC : "");
		return accStaticBuilder.toString();
	}
	
	public static String[] parseParameter(String parameterStr, boolean isVarArgs){
		String[] result = new String[2];
		if(parameterStr == null || parameterStr.length() == 0){
			result[0] = BRACKET;
			result[1] = "";
			return result;
		}else{
			StringBuilder parameterBuilder = new StringBuilder();
			StringBuilder callerParameterBuilder = new StringBuilder();
			parameterBuilder.append(LEFT_BRACKET);
			char[] chars = parameterStr.toCharArray();
			for(int i=0; i<chars.length; i++){
				if(chars[i] != CHAR_L && chars[i] != CHAR_ARRAY){
					parameterBuilder.append(typeMap.get(chars[i])).append(SPACE).append(PARAM).append(i).append(COMMA).append(SPACE);
					callerParameterBuilder.append(PARAM).append(i).append(COMMA).append(SPACE);
				}else if(CHAR_ARRAY == chars[i]){
					int arrayCount = 1;
					i++;
					char currentChar = chars[i];
					while(CHAR_ARRAY == currentChar){
						arrayCount++;
						currentChar = chars[++i];
					}
					if(currentChar != CHAR_L){
						parameterBuilder.append(typeMap.get(currentChar));
					}else{
						i++;
						while(CHAR_COLON != chars[i]){
							parameterBuilder.append(chars[i]);
							i++;
						}
					}
					
					// 处理变长数组
					if(isVarArgs){
						parameterBuilder.append(VARARGS);
					}else{
						for(int j = 0; j < arrayCount; j++){
							parameterBuilder.append(ARRAY);
						}
					}
					parameterBuilder.append(SPACE).append(PARAM).append(i).append(COMMA).append(SPACE);
					callerParameterBuilder.append(PARAM).append(i).append(COMMA).append(SPACE);
				}else if(CHAR_L == chars[i]){
					i++;
					while(CHAR_COLON != chars[i]){
						parameterBuilder.append(chars[i]);
						i++;
					}
					parameterBuilder.append(SPACE).append(PARAM).append(i).append(COMMA).append(SPACE);
					callerParameterBuilder.append(PARAM).append(i).append(COMMA).append(SPACE);
				}
			}
			parameterBuilder.delete(parameterBuilder.length() - 2, parameterBuilder.length());
			parameterBuilder.append(RIGHT_BRACKET);
			callerParameterBuilder.delete(callerParameterBuilder.length() - 2, callerParameterBuilder.length());
			result[0] = parameterBuilder.toString().replace(Constants.SEPARATOR_SLASH, Constants.SEPARATOR_POINT);
			result[1] = callerParameterBuilder.toString();
			return result;
		}
		
	}
	
	public static String parseReturn(String returnStr){
		StringBuilder returnBuilder = new StringBuilder();
		char[] chars = returnStr.toCharArray();
		char currentChar = chars[0];
		int arrayCount = 0;
		
		while(CHAR_ARRAY == currentChar){
			currentChar = chars[++arrayCount];
		}
		
		if(typeMap.containsKey(currentChar)){
			returnBuilder.append(typeMap.get(currentChar));
		}else if(CHAR_L == currentChar){
			returnBuilder.append(chars, arrayCount + 1, chars.length - arrayCount -2);
		}
		if(arrayCount != 0){
			for(int i = 0; i < arrayCount; i++){
				returnBuilder.append(ARRAY);
			}
		}
//		returnBuilder.append(SPACE);
		
		return returnBuilder.toString().replace(Constants.SEPARATOR_SLASH, Constants.SEPARATOR_POINT);
	}
	
	public static String parseException(String exceptionsStr){
		if(exceptionsStr == null || exceptionsStr.length() == 0){
			return "";
		}
		
		StringBuilder exceptionBuilder = new StringBuilder();
		String[] exceptions = exceptionsStr.split(Constants.SEPARATOR_COMMA);
		exceptionBuilder.append(THROWS);
		for(String exception: exceptions){
			exceptionBuilder.append(exception).append(COMMA);
		}
		exceptionBuilder.deleteCharAt(exceptionBuilder.length() -1);
		
		return exceptionBuilder.toString().replace(Constants.SEPARATOR_SLASH, Constants.SEPARATOR_POINT);
	}
	
	
	public static void main(String[] args){
		String targetMethod1 = "com/xiaonei/platform/core/usercount/UserCountMgr send (ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)Z static com.xiaonei.platform.component.application.notification.exception.AppNotificationException";
		String targetMethod2 = "com/xiaonei/platform/core/usercount/UserCountMgr queryUnique ([Lcom/xiaonei/platform/core/opt/OpUniq;)B; non-static java/sql/SQLException";
		String targetMethod6 = "com/xiaonei/platform/core/usercount/UserCountMgr queryUnique ([Lcom/xiaonei/platform/core/opt/OpUniq;)V; static java/sql/SQLException,java/io/IOException";
		String targetMethod7 = "com/xiaonei/platform/core/usercount/UserCountMgr queryUnique ([Lcom/xiaonei/platform/core/opt/OpUniq;)V; static ";
		String targetMethod8 = "com/xiaonei/platform/core/usercount/UserCountMgr queryUnique ([Lcom/xiaonei/platform/core/opt/OpUniq;)V; static java/sql/SQLException,java/io/IOException,java/lang/Exception";
		
		System.out.println(parse(targetMethod1, 0, false));
		System.out.println(parse(targetMethod2, 0, true));
		System.out.println(parse(targetMethod6, 0, false));
		System.out.println(parse(targetMethod7, 0, false));
		System.out.println(parse(targetMethod8, 0, false));
	}
}
