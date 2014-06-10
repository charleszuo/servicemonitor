package com.renren.seo.monitor.template;

import java.util.HashMap;
import java.util.Map;

import com.renren.seo.monitor.outservice.ConstantName;

public class FieldParser {
	private static final String TYPE_B = "byte";
	private static final String TYPE_C = "char";
	private static final String TYPE_D = "double";
	private static final String TYPE_F = "float";
	private static final String TYPE_I = "int";
	private static final String TYPE_J = "long";
	private static final String TYPE_S = "short";
	private static final String TYPE_Z = "boolean";
	
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
	private static final String ARRAY = "[]";

	private static Map<Character, String> typeMap;

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
	}
	
	public static String parse(String targetClassName, String fieldDescription){
		StringBuilder sb = new StringBuilder();
		String[] fieldDetail = fieldDescription.split(" ");
		String filedName = fieldDetail[0];
		String type = fieldDetail[1];
		String parsedType = parseType(type);
		sb.append("public static final ").append(parsedType).append(" ").append(filedName).append(" = ").append(targetClassName).append(".").append(filedName).append(";\n");
		return sb.toString();
	}
	
	public static String parseType(String typeStr){
		StringBuilder returnBuilder = new StringBuilder();
		char[] chars = typeStr.toCharArray();
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
		
		return returnBuilder.toString().replace(ConstantName.SLASH, ConstantName.POINT);
	}
	
	
	
	public static void main(String[] args){
		String className = "com.Test";
		String fieldDescription = "TEST I";
		System.out.println(parse(className, fieldDescription));
	}
}
