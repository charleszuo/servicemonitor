package com.renren.userinc.servicemonitor.bean;

public class ClassTypeBean {
	private String className;
	
	private int classType;
	
	private boolean finalClass;
	
	private boolean hasDefaultConstructor;
	
	public boolean isFinalClass() {
		return finalClass;
	}

	public void setFinalClass(boolean finalClass) {
		this.finalClass = finalClass;
	}

	public boolean hasDefaultConstructor() {
		return hasDefaultConstructor;
	}

	public void setHasDefaultConstructor(boolean hasDefaultConstructor) {
		this.hasDefaultConstructor = hasDefaultConstructor;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public int getClassType() {
		return classType;
	}

	public void setClassType(int classType) {
		this.classType = classType;
	}
	
	
}
