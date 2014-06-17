package com.renren.userinc.servicemonitor.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Map;
import java.util.Set;

import com.renren.userinc.servicemonitor.bean.DependentDescriptor;
import com.renren.userinc.servicemonitor.core.Constants;

public class Writer {
	
	public static void writeClassMethodsLog(Map<String, Set<DependentDescriptor>> classMethods, String logDir, String logFile) throws Exception{
		StringBuilder sb = new StringBuilder();
		for(String className: classMethods.keySet()){
			Set<DependentDescriptor> methodSet = classMethods.get(className);
			DependentDescriptor[] methods = new DependentDescriptor[methodSet.size()];
			methodSet.toArray(methods);
			sb.append(className).append("--").append(methods[0].getMethodName()).append(" ").append(methods[0].getExceptions() == null? "" : methods[0].getExceptions()).append("\n");
			StringBuilder spaceBuilder = new StringBuilder();
			for(int i = 0; i<className.length(); i++){
				spaceBuilder.append(" ");
			}
			String space = spaceBuilder.toString();
			for(int i = 1; i<methods.length; i++){
				sb.append(space).append("--").append(methods[i].getMethodName()).append(" ").append(methods[i].getExceptions() == null? "" : methods[i].getExceptions()).append("\n");
			}
		}
		writeToFile(sb.toString(), logDir, logFile);
	}
	
	
	// 将节点写到文件
	public static void writeToFile(Map<String, DependentDescriptor> nodes, String logDir, String logFile) throws Exception{
		StringBuilder sb = new StringBuilder();
		for(DependentDescriptor dependentDescription: nodes.values()){
			sb.append(dependentDescription.getClassName()).append(" ").append(dependentDescription.getMethodName()).append(" ").append(dependentDescription.getJarName()).append("\n");
		}
		writeToFile(sb.toString(), logDir, logFile);
	}
	
	// 将节点写到文件
	public static void writeClassMethodDependentToFile(Map<String, String> classMethodDependentGraph, String logDir, String logFile) throws Exception{
		StringBuilder sb = new StringBuilder();
		for (String method : classMethodDependentGraph.keySet()) {
			String dependentClassMethods = classMethodDependentGraph
					.get(method);
			sb.append(method).append(Constants.SEPARATOR_CURVE).append(dependentClassMethods).append("\n");
		}
		writeToFile(sb.toString(), logDir, logFile);
	}
		
	// 将节点写到文件
	public static void writeToFile(String content, String logDir, String logFile) throws Exception{
		logDir = FileService.checkDir(logDir);
		BufferedWriter writer = new BufferedWriter(new FileWriter(logDir + logFile));  
		writer.write(content);
		writer.close();
		System.out.println("生成文件: " + logFile);
	}
	
	// 将节点写到控制台
	public static void writeToConsole(Map<String, DependentDescriptor> nodes) throws Exception{
		StringBuilder filtedLeafBuilder = new StringBuilder();
		for(DependentDescriptor dependentDescription: nodes.values()){
			filtedLeafBuilder.append(dependentDescription.toString()).append(" ").append(dependentDescription.getJarName()).append("\n");
		}
		System.out.println(filtedLeafBuilder.toString());
	}
	
}
