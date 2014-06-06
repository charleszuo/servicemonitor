package com.renren.seo.monitor.template;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.renren.seo.monitor.outservice.ConstantName;
import com.renren.seo.monitor.outservice.obj.MethodObject;

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
	
	public static MethodObject parseMethodDescription(String methodWithStaticAndException){
		MethodObject method = null;
		Matcher m = p.matcher(methodWithStaticAndException);
		if(m.matches() && m.groupCount() >= 6){
			String ownerClass = m.group(1).replace(ConstantName.SLASH, ConstantName.POINT);
			String methodNameStr = m.group(2);
			String parameterStr = m.group(3);
			String returnStr = m.group(4);
			String accStaticStr = m.group(5);
			String exceptionsStr = m.group(6);
			
			method = new MethodObject();
			method.setMethodName(methodNameStr);
			method.setParameters(parseParameter(parameterStr));
			method.setAccStatic(parseAccStatic(accStaticStr));
			method.setReturnType(parseReturn(returnStr));
			method.setExceptions(parseException(exceptionsStr));
			method.setOwner(ownerClass);
		}
		return method;
	}
	
	public static String parse(String classMethodDescription, int methodType){
		StringBuilder sb = new StringBuilder();
		MethodObject methodOject = parseMethodDescription(classMethodDescription);
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
			if(ConstantName.ACC_STATIC.equals(methodOject.getAccStatic())){
				caller = methodOject.getOwner();
			}else{
				caller = ConstantName.TARGET;
			}
			if(methodType == 0){
				if(!(TYPE_VOID.equals(methodOject.getReturnType()))){
					sb.append("\t\t return ");
				}else{
					sb.append("\t\t ");
				}
				sb.append(caller).append(".").append(methodOject.getMethodName()).append("(").append(parseParameterResult[1]).append(");\n");
			}else if(methodType == ConstantName.METHOD_TYPE_INTERFACE_FACTORY_METHOD){
				//Text: mop.hi.oce.adapter.BuddyCoreAdapter proxyTarget = mop.hi.oce.adapter.AdapterFactory.getBuddyCoreAdapter();
				//Text: DynamicProxy proxy = new DynamicProxy();
				//Text: return (mop.hi.oce.adapter.BuddyCoreAdapter)proxy.bind(proxyTarget);
				String returnType = methodOject.getReturnType();
				sb.append("\t\t").append(returnType).append(" ").append(ConstantName.PROXY_TARGET).append(" = ")
					.append(caller).append(".").append(methodOject.getMethodName()).append("(").append(parseParameterResult[1]).append(");\n");
				sb.append("\t\tcom.renren.seo.serviceproxy.system.generated.JavaDynamicProxy proxy = new com.renren.seo.serviceproxy.system.generated.JavaDynamicProxy();\n");
				sb.append("\t\t").append("return (").append(returnType).append(")").append("proxy.bind(").append(ConstantName.PROXY_TARGET).append(");\n");
			}else if(methodType == ConstantName.METHOD_TYPE_ALL_INSTANCE_METHOD_CLASS_FACTORY_METHOD){
				//Text: xce.tripod.client.TripodCacheClient proxyTarget = xce.tripod.client.TripodCacheClientFactory.getClient(param24);
				//Text: CglibDynamicProxy proxy = new CglibDynamicProxy();
				//Text: return (xce.tripod.client.TripodCacheClient)proxy.bind(proxyTarget);
				String returnType = methodOject.getReturnType();
				sb.append("\t\t").append(returnType).append(" ").append(ConstantName.PROXY_TARGET).append(" = ")
					.append(caller).append(".").append(methodOject.getMethodName()).append("(").append(parseParameterResult[1]).append(");\n");
				sb.append("\t\tcom.renren.seo.serviceproxy.system.generated.CglibDynamicProxy proxy = new com.renren.seo.serviceproxy.system.generated.CglibDynamicProxy();\n");
				sb.append("\t\t").append("return (").append(returnType).append(")").append("proxy.bind(").append(ConstantName.PROXY_TARGET).append(");\n");
			}
			sb.append("\t}\n");
		}
		return sb.toString();
	}
	
	
	public static String parseAccStatic(String accStaticStr){
		StringBuilder accStaticBuilder = new StringBuilder();
		accStaticBuilder.append(ConstantName.ACC_STATIC.equals(accStaticStr) ? ConstantName.ACC_STATIC : "");
		return accStaticBuilder.toString();
	}
	
	public static String[] parseParameter(String parameterStr){
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
					for(int j = 0; j < arrayCount; j++){
						parameterBuilder.append(ARRAY);
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
			result[0] = parameterBuilder.toString().replace(ConstantName.SLASH, ConstantName.POINT);
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
		
		return returnBuilder.toString().replace(ConstantName.SLASH, ConstantName.POINT);
	}
	
	public static String parseException(String exceptionsStr){
		if(exceptionsStr == null || exceptionsStr.length() == 0){
			return "";
		}
		
		StringBuilder exceptionBuilder = new StringBuilder();
		String[] exceptions = exceptionsStr.split(ConstantName.COMMA);
		exceptionBuilder.append(THROWS);
		for(String exception: exceptions){
			exceptionBuilder.append(exception).append(COMMA);
		}
		exceptionBuilder.deleteCharAt(exceptionBuilder.length() -1);
		
		return exceptionBuilder.toString().replace(ConstantName.SLASH, ConstantName.POINT);
	}
	
	
	public static void main(String[] args){
		String targetMethod1 = "com/xiaonei/platform/core/usercount/UserCountMgr send (ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z static com.xiaonei.platform.component.application.notification.exception.AppNotificationException";
		String targetMethod2 = "com/xiaonei/platform/core/usercount/UserCountMgr queryUnique (Lcom/xiaonei/platform/core/opt/OpUniq;)B; non-static java/sql/SQLException";
		String targetMethod3 = "com/xiaonei/platform/core/usercount/UserCountMgr findHotShare (IIIIB)Lcom/renren/xoa/lite/ServiceFuture; static java/sql/SQLException";
		String targetMethod4 = "com/xiaonei/platform/core/usercount/UserCountMgr findHotShare (S[[Lcom/renren/xoa/lite/ServiceFuture;IIB)[[I static java/sql/SQLException";
		String targetMethod5 = "com/xiaonei/platform/core/usercount/UserCountMgr findHotShare ()Lcom/renren/xoa/lite/ServiceFuture; static ";
		System.out.println(parse(targetMethod1, 0));
		System.out.println(parse(targetMethod2, 0));
	}
}
