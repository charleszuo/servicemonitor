package com.renren.userinc.servicemonitor.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.renren.userinc.servicemonitor.bean.DependentDescriptor;

/**
 * DependencyVisitor
 * 
 */
public class FieldAndExceptionVisitor extends ClassVisitor {
	private String className;

	private boolean isInterface;

	private Map<String, DependentDescriptor> methods;

	private Map<String, String> methodDependentMap = new HashMap<String, String>();

	private Map<String, Set<String>> classStaticFiledDependentMap;
	
	private String annotation;

	public Map<String, String> getMethodDependentMap() {
		return methodDependentMap;
	}

	public FieldAndExceptionVisitor(String className,
			Map<String, DependentDescriptor> methods,
			Map<String, Set<String>> classStaticFiledDependentMap) {
		super(Opcodes.ASM4);
		this.className = className;
		this.methods = methods;
		this.classStaticFiledDependentMap = classStaticFiledDependentMap;
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

	@Override
	public FieldVisitor visitField(final int access, final String name,
			final String desc, final String signature, final Object value) {
		if(classStaticFiledDependentMap == null){
			return null;
		}
		boolean isLowerCase = false;
		char[] nameChars = name.toCharArray();
		for(int i = 0; i < nameChars.length; i++){
			if(Character.isLowerCase(nameChars[i])){
				isLowerCase = true;
				break;
			}
		}
		// 处理依赖的外部类的静态字段引用
		boolean isStatic = (access & Opcodes.ACC_STATIC) != 0? true: false;
		boolean isPublic = (access & Opcodes.ACC_PUBLIC) != 0? true: false;
		if(isStatic && isPublic && !isLowerCase){
			String fieldDescription = name + " " + desc;
			if(!classStaticFiledDependentMap.containsKey(this.className)){
				Set<String> fields = new HashSet<String>();
				classStaticFiledDependentMap.put(this.className, fields);
			}
			Set<String> fields = classStaticFiledDependentMap.get(this.className);
			fields.add(fieldDescription);
		}
		return null;
	}

	// 解析Class自身的Method
	@Override
	public MethodVisitor visitMethod(final int access, final String name,
			final String desc, final String signature, final String[] exceptions) {
		String methodSignature = name + " " + desc;
		if ((access & Opcodes.ACC_STATIC) != 0) {
			methodSignature += " " + Constants.ACC_STATIC;
		} else {
			methodSignature += " " + Constants.ACC_NON_STATIC;
		}
		
		boolean isVarArgs = (access & Opcodes.ACC_VARARGS) != 0 ? true: false;
		String classMethodSignature = this.className + " " + methodSignature;

		if (!name.equals(Constants.INIT_METHOD)
				&& !name.equals(Constants.CINIT_METHOD)
				&& methods.containsKey(classMethodSignature)) {
			DependentDescriptor dependentDescription = methods
					.get(classMethodSignature);
			dependentDescription.setExceptions(exceptions);
			dependentDescription.setAnnotation(this.annotation);
			dependentDescription.setIsInterface(isInterface);
			dependentDescription.setIsVarArgs(isVarArgs);
		}

		return null;
	}

}
