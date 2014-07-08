package com.renren.userinc.servicemonitor.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * DependencyVisitor
 * 
 */
public class DependencyVisitor extends ClassVisitor {
	private String className;

	private Map<String, String> methodFilter;

	private Map<String, String> allTargetClasses;

	private Map<String, String> globalClassesMethodDependentGraph;
	
	private boolean isFinalClass;
	
	private boolean hasDefaultConstructor;
	
	Set<String> dependentClasses = new HashSet<String>();

	Stack<String> ownMethod = new Stack<String>();

	Map<String, String> methodDependentMap = new HashMap<String, String>();

	public Map<String, String> getMethodDependentMap() {
		return methodDependentMap;
	}

	public DependencyVisitor(String className,
			Map<String, String> allTargetClasses) {
		super(Opcodes.ASM4);
		this.className = className;
		this.allTargetClasses = allTargetClasses;
	}

	public DependencyVisitor(String className,
			Map<String, String> methodFilter,
			Map<String, String> globalClassesMethodDependentGraph,
			Map<String, String> allTargetClasses) {
		super(Opcodes.ASM4);
		this.className = className;
		this.methodFilter = methodFilter;
		this.globalClassesMethodDependentGraph = globalClassesMethodDependentGraph;
		this.allTargetClasses = allTargetClasses;
	}
	
	public void visit(final int version, final int access, final String name,
            final String signature, final String superName,
            final String[] interfaces) {
        isFinalClass = (access & Opcodes.ACC_FINAL) != 0 ? true: false;
    }
	 
	// 解析Class自身的Method
	@Override
	public MethodVisitor visitMethod(final int access, final String name,
			final String desc, final String signature, final String[] exceptions) {
		if(name.equals(Constants.INIT_METHOD) && desc.equals("()V")){
			hasDefaultConstructor = true;
		}
		
		String methodSignature = name + " " + desc;
		if((access & Opcodes.ACC_STATIC) != 0){
			methodSignature += " " + Constants.ACC_STATIC; 
		}else{
			methodSignature += " " + Constants.ACC_NON_STATIC; 
		}
		
		String classMethodSignature = this.className + " " + methodSignature;
		
		if (methodFilter == null) {
			// 过滤构造函数
			if (!name.equals(Constants.INIT_METHOD)
					&& !name.equals(Constants.CINIT_METHOD)) {
				ownMethod.push(classMethodSignature);
				return new MethodDependencyVisitor(this);
			}
		} else {
			if (!globalClassesMethodDependentGraph
					.containsKey(classMethodSignature)) {
				if (methodFilter.containsKey(methodSignature)) {
					ownMethod.push(classMethodSignature);
					return new MethodDependencyVisitor(this);
				}
			}
		}

		return null;
	}
	
	public boolean isFinalClass() {
		return isFinalClass;
	}

	public boolean isHasDefaultConstructor() {
		return hasDefaultConstructor;
	}



	class MethodDependencyVisitor extends MethodVisitor {

		private DependencyVisitor classVisitor;

		public MethodDependencyVisitor(DependencyVisitor classVisitor) {
			super(Opcodes.ASM4);
			this.classVisitor = classVisitor;
		}

		// 解析Class里面所有的invokeXXXX 指令, 得到方法依赖的外部类的方法
		public void visitMethodInsn(final int opcode, final String owner,
				final String name, final String desc) {
			String methodSignature = owner + " " + name + " " + desc;
			if ((globalClassesMethodDependentGraph != null
					&& !globalClassesMethodDependentGraph
							.containsKey(methodSignature)) || globalClassesMethodDependentGraph == null) {
				// 过滤目标工程下的类, 过滤非人人的类, 过滤已经查找过的类方法, 过滤构造函数
				if (!allTargetClasses.containsKey(owner)
						// && !owner.equals(classVisitor.className)
						&& owner.matches(Constants.CLASS_SCOPE)
//						&& !name.equals(ConstantName.INIT_METHOD)
//						&& !name.equals(ConstantName.CINIT_METHOD)
						) {

					if(opcode == Opcodes.INVOKESTATIC){
						methodSignature += " " + Constants.ACC_STATIC; 
					}else {
						methodSignature += " " + Constants.ACC_NON_STATIC; 
					}
					
					String ownMethod = classVisitor.ownMethod.peek();
					if (classVisitor.methodDependentMap.containsKey(ownMethod)) {
						classVisitor.methodDependentMap.put(ownMethod,
								classVisitor.methodDependentMap.get(ownMethod)
										+ Constants.SEPARATOR_COMMA
										+ methodSignature);
					} else {
						classVisitor.methodDependentMap.put(ownMethod, methodSignature);
					}
				}
			}
		}

	}

}
