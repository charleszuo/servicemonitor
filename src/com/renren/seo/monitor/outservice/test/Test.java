package com.renren.seo.monitor.outservice.test;

import java.io.FileInputStream;

import org.objectweb.asm.ClassReader;

import com.renren.seo.monitor.outservice.DependencyVisitor3;

public class Test {
	public static void main(String[] args){
        try {
        	DependencyVisitor3 v = new DependencyVisitor3();
//        	ClassReader classReader = new ClassReader(new FileInputStream(fileName));
			ClassReader classReader = new ClassReader("com.renren.seo.monitor.outservice.test.ClassC");
			classReader.accept(v, 0);
			System.out.println(v.isFinalClass());
			System.out.println(v.isHasDefaultConstructor());
////			for(Type t: ServiceFactory.class.getGenericInterfaces()){
////				System.out.println(t);
////			}
//			List<String> l = new ArrayList();
//			for(String s : l){
//				s.charAt(0);
//			}
//			for(Method m: ServiceFactory.class.getMethods()){
//				for(Type t: m.getGenericParameterTypes()){
//					System.out.println(t.toString());
//				}
//					System.out.println(m.getGenericReturnType());
//			}
//////			Set<String> dependentClasses = v.getDependentClasses();
//////			for(String s : dependentClasses){
//////				System.out.println(s);
//////			}
//	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void test(Integer[] a){
		
	}
}
