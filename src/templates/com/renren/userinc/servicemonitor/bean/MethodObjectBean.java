package com.renren.userinc.servicemonitor.bean;

public class MethodObjectBean {
	private String methodName;
	
	private String returnType;
	
	private String[] parameters;
	
	private String accStatic;
	
	private String exceptions;
	
	private String className;

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public String getReturnType() {
		return returnType;
	}

	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}

	public String[] getParameters() {
		return parameters;
	}

	public void setParameters(String[] parameters) {
		this.parameters = parameters;
	}

	public String getAccStatic() {
		return accStatic;
	}

	public void setAccStatic(String accStatic) {
		this.accStatic = accStatic;
	}

	public String getExceptions() {
		return exceptions;
	}

	public void setExceptions(String exceptions) {
		this.exceptions = exceptions;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}
	
}
