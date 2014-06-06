package com.renren.seo.monitor.outservice.obj;

import java.util.ArrayList;
import java.util.List;

import com.renren.seo.monitor.outservice.ConstantName;

public class DependentDescription implements Comparable<DependentDescription>{
	private String className;
	private String method;
	private String jarName;
	private String[] exceptions;
	private boolean isInterface;
	private int methodType;
	private String annotation;
	
	private List<DependentDescription> parenetDependentObjectList = new ArrayList<DependentDescription>();
	
	private List<DependentDescription> childDependentObjectList = new ArrayList<DependentDescription>();

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

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}
	
	public List<DependentDescription> getParenetDependentObjectList() {
		return parenetDependentObjectList;
	}

	public List<DependentDescription> getChildDependentObjectList() {
		return childDependentObjectList;
	}
	
	public void addParenetDependentObjec(DependentDescription parent){
		this.parenetDependentObjectList.add(parent);
	}
	
	public void addChildDependentObject(DependentDescription child){
		this.childDependentObjectList.add(child);
	}

	public String getExceptions() {
		if(this.exceptions == null){
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for(String exception: this.exceptions){
			sb.append(exception).append(ConstantName.COMMA);
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
		return this.getClassName() + " " + this.getMethod();
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

	@Override
	public int compareTo(DependentDescription o) {
		if(this.className.equals(o.className)){
			return this.method.compareTo(o.getMethod());
		}else{
			return this.className.compareTo(o.getClassName());
		}
	}

}
