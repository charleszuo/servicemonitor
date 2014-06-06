package com.renren.seo.monitor.outservice;

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
public class DependencyVisitor2 extends ClassVisitor {
	private String className;

	private Map<String, String> methodFilter;

	private Map<String, String> allTargetClasses;

	private Map<String, String> globalClassesMethodDependentGraph;

	Set<String> dependentClasses = new HashSet<String>();

	Stack<String> ownMethod = new Stack<String>();

	Map<String, String> methodDependentMap = new HashMap<String, String>();

	public Map<String, String> getMethodDependentMap() {
		return methodDependentMap;
	}

	public DependencyVisitor2(String className,
			Map<String, String> allTargetClasses) {
		super(Opcodes.ASM5);
		this.className = className;
		this.allTargetClasses = allTargetClasses;
	}

	public void printMethodDependent() {
		for (String ownMethod : methodDependentMap.keySet()) {
			System.out.println(ownMethod + " ---- "
					+ methodDependentMap.get(ownMethod));
		}
	}

	// 解析Class自身的Method
	@Override
	public MethodVisitor visitMethod(final int access, final String name,
			final String desc, final String signature, final String[] exceptions) {
		String methodSignature = name + " " + desc;
		if((access & Opcodes.ACC_STATIC) != 0){
			methodSignature += " static"; 
		}else{
			methodSignature += " non-static";
		}
		String classMethodSignature = this.className + " " + methodSignature;
		if (!name.equals(ConstantName.INIT_METHOD)
				&& !name.equals(ConstantName.CINIT_METHOD)) {
			ownMethod.push(classMethodSignature);
			return new MethodDependencyVisitor2(this);
		}
	

		return null;
	}

	class MethodDependencyVisitor2 extends MethodVisitor {

		private DependencyVisitor2 classVisitor;

		public MethodDependencyVisitor2(DependencyVisitor2 classVisitor) {
			super(Opcodes.ASM5);
			this.classVisitor = classVisitor;
		}
		// 解析Class里面所有的invokeXXXX 指令, 得到方法依赖的外部类的方法
		@Override
		public void visitMethodInsn(final int opcode, final String owner,
				final String name, final String desc, final boolean itf) {
			// 过滤目标工程下的类, 过滤类自己的其他方法, 过滤非人人的类, 过滤构造函数
			if (!allTargetClasses.containsKey(owner)
					&& !owner.equals(classVisitor.className)
//					&& !name.equals(ConstantName.INIT_METHOD)
//					&& !name.equals(ConstantName.CINIT_METHOD)
					) {
				String ownMethod = classVisitor.ownMethod.peek();
				String methodSignature = owner + " " + name + " " + desc;
				if((opcode & Opcodes.ACC_STATIC) != 0){
					methodSignature += " static"; 
				}else{
					methodSignature += " non-static";
				}
				if (classVisitor.methodDependentMap.containsKey(ownMethod)) {
					classVisitor.methodDependentMap.put(ownMethod,
							classVisitor.methodDependentMap.get(ownMethod)
									+ ConstantName.SEPARATOR + methodSignature);
				} else {
					classVisitor.methodDependentMap.put(ownMethod, methodSignature);
				}
			}
			
		}
	}

}
