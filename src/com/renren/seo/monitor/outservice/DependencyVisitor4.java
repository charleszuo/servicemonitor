package com.renren.seo.monitor.outservice;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.renren.seo.monitor.outservice.obj.DependentDescription;

/**
 * DependencyVisitor
 * 
 */
public class DependencyVisitor4 extends ClassVisitor {
	private String className;

	private boolean isInterface;
	
	private Map<String, DependentDescription> methods;

	private Map<String, String> methodDependentMap = new HashMap<String, String>();
	
	private String annotation;
	
	public Map<String, String> getMethodDependentMap() {
		return methodDependentMap;
	}

	public DependencyVisitor4(String className,
			Map<String, DependentDescription> methods) {
		super(Opcodes.ASM5);
		this.className = className;
		this.methods = methods;
	}
	
   @Override
    public void visit(final int version, final int access, final String name,
            final String signature, final String superName,
            final String[] interfaces) {
	    isInterface = (access & Opcodes.ACC_INTERFACE) != 0 ? true : false;
    }
	

	@Override
	public AnnotationVisitor visitAnnotation(final String desc,
	            final boolean visible) {
	    this.annotation = desc;
		return null;
	}
	
	// 解析Class自身的Method
	@Override
	public MethodVisitor visitMethod(final int access, final String name,
			final String desc, final String signature, final String[] exceptions) {
		String methodSignature = name + " " + desc;
		if((access & Opcodes.ACC_STATIC) != 0){
			methodSignature += " " + ConstantName.ACC_STATIC; 
		}else{
			methodSignature += " " + ConstantName.ACC_NON_STATIC; 
		}
		String classMethodSignature = this.className + " " + methodSignature;
		
		if (!name.equals(ConstantName.INIT_METHOD)
				&& !name.equals(ConstantName.CINIT_METHOD) && methods.containsKey(classMethodSignature)) {
			DependentDescription dependentDescription = methods.get(classMethodSignature);
			dependentDescription.setExceptions(exceptions);
			dependentDescription.setAnnotation(this.annotation);
			dependentDescription.setIsInterface(isInterface);
		}
		
		return null;
	}

}
