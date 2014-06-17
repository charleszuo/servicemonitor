package com.renren.userinc.servicemonitor.bean;

import java.util.ArrayList;
import java.util.List;

import com.renren.userinc.servicemonitor.core.Constants;

public class DependentDescriptor implements Comparable<DependentDescriptor>{
	private String className;
	private String methodName;
	private String jarName;
	private String[] exceptions;
	private boolean isInterface;
	private int methodType;
	private String annotation;
	private boolean isLocalCode;
	// 是不是有变长参数
	private boolean isVarArgs;
	private String generatedProxyClassName;
	
	private List<DependentDescriptor> parenetDependentObjectList = new ArrayList<DependentDescriptor>();
	
	private List<DependentDescriptor> childDependentObjectList = new ArrayList<DependentDescriptor>();

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getJarName() {
		return jarName;
	}

	public void setJarName(String jarName) {
		this.jarName = jarName;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	
	public List<DependentDescriptor> getParenetDependentObjectList() {
		return parenetDependentObjectList;
	}

	public List<DependentDescriptor> getChildDependentObjectList() {
		return childDependentObjectList;
	}
	
	public void addParenetDependentObjec(DependentDescriptor parent){
		this.parenetDependentObjectList.add(parent);
	}
	
	public void addChildDependentObject(DependentDescriptor child){
		this.childDependentObjectList.add(child);
	}

	public String getExceptions() {
		if(this.exceptions == null){
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for(String exception: this.exceptions){
			sb.append(exception).append(Constants.SEPARATOR_COMMA);
		}
		return sb.substring(0, sb.length() -1);
	}

	public void setExceptions(String[] exceptions) {
		this.exceptions = exceptions;
	}

	public boolean isInterface() {
		return isInterface;
	}

	public void setIsInterface(boolean isInterface) {
		this.isInterface = isInterface;
	}

	public String toString(){
		return this.getClassName() + " " + this.getMethodName();
	}
	
	public String toStringWithException(){
		return toString() + " " + (getExceptions() == null? "" : getExceptions());
	}
	
	public int getMethodType() {
		return methodType;
	}

	public void setMethodType(int methodType) {
		this.methodType = methodType;
	}
	
	public String getAnnotation() {
		return annotation;
	}

	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}

	public boolean isLocalCode() {
		return isLocalCode;
	}

	public void setIsLocalCode(boolean isLocalCode) {
		this.isLocalCode = isLocalCode;
	}

	public boolean isVarArgs() {
		return isVarArgs;
	}

	public void setIsVarArgs(boolean isVarArgs) {
		this.isVarArgs = isVarArgs;
	}

	public String getGeneratedProxyClassName() {
		return generatedProxyClassName;
	}

	public void setGeneratedProxyClassName(String generatedProxyClassName) {
		this.generatedProxyClassName = generatedProxyClassName;
	}

	@Override
	public int compareTo(DependentDescriptor o) {
		if(this.className.equals(o.className)){
			return this.methodName.compareTo(o.getMethodName());
		}else{
			return this.className.compareTo(o.getClassName());
		}
	}

}
