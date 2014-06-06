package com.renren.seo.monitor.outservice.test;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;

import com.renren.seo.monitor.outservice.DependencyVisitor3;

public class Test {
	public static void main(String[] args){
        try {
        	DependencyVisitor3 v = new DependencyVisitor3();
			ClassReader classReader = new ClassReader("com.renren.seo.monitor.outservice.TestObject");
			classReader.accept(v, 0);
			
//			for(Type t: ServiceFactory.class.getGenericInterfaces()){
//				System.out.println(t);
//			}
			List<String> l = new ArrayList();
			for(String s : l){
				s.charAt(0);
			}
			for(Method m: ServiceFactory.class.getMethods()){
				for(Type t: m.getGenericParameterTypes()){
					System.out.println(t.toString());
				}
					System.out.println(m.getGenericReturnType());
			}
////			Set<String> dependentClasses = v.getDependentClasses();
////			for(String s : dependentClasses){
////				System.out.println(s);
////			}
	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void test(Integer[] a){
		
	}
}
