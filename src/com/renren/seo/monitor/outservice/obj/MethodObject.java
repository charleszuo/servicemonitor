package com.renren.seo.monitor.outservice.obj;

public class MethodObject {
	private String methodName;
	
	private String returnType;
	
	private String[] parameters;
	
	private String accStatic;
	
	private String exceptions;
	
	private String owner;

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

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}
	
}
