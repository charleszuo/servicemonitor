package com.renren.seo.monitor.outservice.test;

public class Test2 {

	public static void main(String[] args) {
		ClassC c = new ClassC(1);
		CglibDynamicProxy cglib = new CglibDynamicProxy();
		ClassC proxy = (ClassC)cglib.bind(c);
//		proxy.method();
	}

}
