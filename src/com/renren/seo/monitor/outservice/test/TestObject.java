package com.renren.seo.monitor.outservice.test;

import java.util.List;


public class TestObject {
	public void method1(){
		ClassA t = new ClassA();
		t.test(null);
		if("it is a test".equals(ClassA.CONSTANT_NAME)){
			int a = 1;
		}
		int b = t.a;
	}
}
