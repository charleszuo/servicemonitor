package com.renren.userinc.servicemonitor.core;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;

import com.renren.userinc.servicemonitor.bean.ClassTypeBean;
import com.renren.userinc.servicemonitor.bean.DependentDescriptor;
import com.renren.userinc.servicemonitor.bean.MethodObjectBean;
import com.renren.userinc.servicemonitor.filter.ClassFilter;
import com.renren.userinc.servicemonitor.filter.DefaultClassFilter;
import com.renren.userinc.servicemonitor.template.MethodParser;
import com.renren.userinc.servicemonitor.template.TemplateGenerator;
import com.renren.userinc.servicemonitor.util.FileService;
import com.renren.userinc.servicemonitor.util.Writer;

public class ClassMethodDependencyGenerator{
	// 工程所有依赖的相关类与所在jar包的对应关系
	private Map<String, String> allClassJarMapping = new HashMap<String, String>();
	// 工程自身所有的类
	private Map<String, String> allLocalClasses;
	// 方法和依赖的方法的对应关系,格式是: key="className methodName methodSignature" 
	// value="className methodName methodSignature,className methodName methodSignature"
	private Map<String, String> classMethodDependentGraph = new HashMap<String, String>();
	// 方便搜索某个方法被哪些方法调用的关系. key是被调用方法,格式是"className methodName methodSignature". 
	// value是调用它的方法的Set,Set里面的值是"className methodName methodSignature"
	private Map<String, Set<String>> callHierarchyGraph = new HashMap<String, Set<String>>();
	// 方便按树形结构展示依赖的类和方法的关系. Key是层次,比如0,1,2 Value是每层里面的依赖关系对象的Map
	private Map<Integer, Map<String, DependentDescriptor>> dependentDescriptionMap = new HashMap<Integer, Map<String, DependentDescriptor>>();
	// 方便按树形结构展示依赖的类和方法的关系. 依赖树的根节点,可以先序遍历的方式展示所有的依赖树
	DependentDescriptor dependentTreeRoot = new DependentDescriptor();
	private Map<String, DependentDescriptor> leafNodes = new HashMap<String, DependentDescriptor>();
	private Map<String, DependentDescriptor> allNodes = new HashMap<String, DependentDescriptor>();
	
	private Map<String, Set<String>> classStaticFiledDependentMap = new HashMap<String, Set<String>>();
	
	// 存放目标工程引用的所有外部依赖类和类的方法,在第一轮遍历之后可以获得
	private Map<String, Map<String, String>> allTopNodesDependentClassOwnMethods;
	
	private Map<String, String> needManualGeneratedClassesMap = new HashMap<String, String>();
	
	private Map<String, ClassTypeBean> classTypeMap = new HashMap<String, ClassTypeBean>();
	
	private int methodCount;
	private int round = 0;
	private ClassFilter classFilter = DefaultClassFilter.getInstance();
	
	public ClassMethodDependencyGenerator(){
		// 记录所有的目标workspace自己的class文件名
		allLocalClasses = FileService.getInstance().getAllLocalClasses(Constants.TARGET_CLASSES_PATH);
		// 根据.classpath文件里记录的项目依赖的相关jar包,记录这些jar包里面所有的class名和所在的jar的映射关系,
		// 方便后面根据class名去文件系统找jar文件来读取相应的class文件
		generateClassJarMapping();
		needManualGeneratedClassesMap.put("com/xiaonei/xce/buddybyaddtimecache/BuddyByAddTimeCacheAdapter", "templates/manual/BuddyByAddTimeCacheAdapter.vm");
		needManualGeneratedClassesMap.put("com/xiaonei/xce/offerfriends/OfferFriendsAdapter", "templates/manual/OfferFriendsAdapter.vm");
		needManualGeneratedClassesMap.put("com/xiaonei/xce/scorecache/ScoreCacheAdapter", "templates/manual/ScoreCacheAdapter.vm");
		needManualGeneratedClassesMap.put("com/renren/xoa/lite/ServiceFactories", "templates/manual/ServiceFactories.vm");
		needManualGeneratedClassesMap.put("com/renren/xoa/lite/ServiceFactory", "templates/manual/ServiceFactory.vm");
		needManualGeneratedClassesMap.put("com/renren/xoa/lite/ServiceFutureHelper", "templates/manual/ServiceFutureHelper.vm");
	}
	
	public Map<String, String> getClassMethodDependentGraph(){
		return this.classMethodDependentGraph;
	}
	
	private void generateClassJarMapping() {
		try{
			// 读取.classpath文件
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(Constants.TARGET_CLASSPATH_FILENAME)));
			List<String> allJarName = new ArrayList<String>();
			String jarPath = null;
			// String JAR_FILTER = ".*(/com/.*(xiaonei|renren|xce/).*\\.jar).*";
			Pattern p = Pattern.compile(Constants.JAR_SCOPE);
			while ((jarPath = reader.readLine()) != null) {
				Matcher m = p.matcher(jarPath);
				// 抓取相关的jar名字
				if(m.find() && m.groupCount() > 0){
					allJarName.add(m.group(1));
				}
			}
			// 遍历所有的jar,获取jar里面的class文件名
			for(String jarName: allJarName){
				ZipFile zipFile = new ZipFile(Constants.M2_REPO + jarName);
				Enumeration<? extends ZipEntry> en = zipFile.entries();
				while (en.hasMoreElements()) {
					ZipEntry e = en.nextElement();
					String className = e.getName();
					// 只处理class文件
					if (className.endsWith(Constants.CLASS_FILE_POSTFIX)) {
						allClassJarMapping.put(className, jarName);
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void generateClassMethodDependentGraph() throws Exception {
		// 一: 生成目标工程的类的方法所依赖的所有外部类的方法
		
		// 记录依赖的类的所有方法, 比如依赖了ClassA的a, b, c方法, 这样可以有目标的去加载ClassA的.class文件,并且只分析a, b, c方法
		Map<String, Map<String, String>> dependentClassOwnMethods = new HashMap<String, Map<String, String>>();
		int localClassCount = 0;
		int methodDependentCount = 0;
		for (String className : allLocalClasses.keySet()) {
			String classFilePath = Constants.TARGET_CLASSES_PATH + className
					+ Constants.CLASS_FILE_POSTFIX;

			DependencyVisitor vistor = new DependencyVisitor(className, allLocalClasses);
			ClassReader classReader = new ClassReader(new FileInputStream(classFilePath));
			classReader.accept(vistor, 0);
			
			Map<String, String> methodDependentMap = vistor
					.getMethodDependentMap();
			if(methodDependentMap.size() == 0){
				continue;
			}
			
			
			localClassCount++;
			methodDependentCount += methodDependentMap.size();
			//step1: 把单个类的方法的依赖关系放到全局的类的方法的依赖集合中,这时的methodDependentMap里面的Key是本地Java类的方法, value是本地所依赖的外部类
			classMethodDependentGraph.putAll(methodDependentMap);
			//step2: 统计依赖了哪些类的哪些方法
			for (String localMethod: methodDependentMap.keySet()) {
				DependentDescriptor localCodeDescription = new DependentDescriptor();
				String[] localMethodDetail = localMethod.split(" ");
				String localMethodSignature = localMethodDetail[1]
						+ " " + localMethodDetail[2] + " " + localMethodDetail[3];
				localCodeDescription.setClassName(localMethodDetail[0]);
				localCodeDescription.setMethodName(localMethodSignature);
				localCodeDescription.setIsLocalCode(true);
				dependentTreeRoot.addChildDependentObject(localCodeDescription);
				localCodeDescription.addParenetDependentObjec(dependentTreeRoot);
				
				// for example: methodDependentStr = com/xiaonei/tribe/model/TribeUser setSelected (I)V,com/xiaonei/tribe/model/User methodName (I)V
				// 逗号,是分隔符, methodDependentStr表示一个方法依赖的所有的外部的方法
				String methodDependentStr = methodDependentMap.get(localMethod);
				
				
				String[] methods = methodDependentStr.split(Constants.SEPARATOR_COMMA);
				for (int i = 0; i < methods.length; i++) {
					String method = methods[i];
					// for example: method = com/xiaonei/tribe/model/TribeUser setSelected (I)V
					String[] methodDetail = method.split(" ");
					String methodSignature = methodDetail[1] + " "
							+ methodDetail[2] + " " + methodDetail[3];
					// methodDetail[0] = com/xiaonei/tribe/model/TribeUser 表示类名
					// methodDetail[1] = setSelected 表示方法名
					// methodDetail[2] = (I)V 表示方法签名
					if (dependentClassOwnMethods.containsKey(methodDetail[0])) {
						Map<String, String> ownMethods = dependentClassOwnMethods
								.get(methodDetail[0]);
						
						if (!ownMethods.containsKey(methodSignature)) {
							ownMethods.put(methodSignature, null);
							methodCount++;
						}

					} else {
						Map<String, String> ownMethods = new HashMap<String, String>();
						ownMethods.put(methodSignature, null);
						dependentClassOwnMethods.put(methodDetail[0],
								ownMethods);
						methodCount++;
					}
					
					if(!dependentDescriptionMap.containsKey(round)){
						dependentDescriptionMap.put(round, new HashMap<String, DependentDescriptor>());
					}
					Map<String, DependentDescriptor> thisRoundNodes = dependentDescriptionMap.get(round);
					// 支持多个上层节点对应一个下层节点
					if(!thisRoundNodes.containsKey(method)){
						DependentDescriptor description = new DependentDescriptor();
						description.setClassName(methodDetail[0]);
						description.setMethodName(methodSignature);
						description.setJarName(allClassJarMapping.get(methodDetail[0] + Constants.CLASS_FILE_POSTFIX));
						thisRoundNodes.put(method, description);
						// 加到所有的node里面
						allNodes.put(description.toString(), description);
						// 设置节点父子关系
						localCodeDescription.addChildDependentObject(description);
					}
					DependentDescriptor child = thisRoundNodes.get(method);
					child.addParenetDependentObjec(localCodeDescription);
//					DependentDescriptor child = topLevelNodes.get(method);
					
					//child.addParenetDependentObjec(dependentTreeRoot);
				}
			}
		}
		
		allTopNodesDependentClassOwnMethods = dependentClassOwnMethods;
		
		System.out.println("Round " + round + ": " + localClassCount + "个本地类的" 
				+ methodDependentCount + "个方法依赖外部" + dependentClassOwnMethods.size() + "个类的" + methodCount + "个方法");
		round++;
		// 二: 逐层次查找所依赖外部类的方法所依赖的所有外部类的方法. 
		// 最坏的情况就是所有classpath里面的xiaonei相关的jar的类都被遍历一次, 程序不会死循环
		// 最多运行12次,假设12次之内肯定能出结果
		
		while (methodCount != 0 && round <= Constants.MAX_SEARCH_ROUND) {
			methodCount = 0;
			methodDependentCount = 0;
			Map<String, Map<String, String>> thisRoundDependentClassesOwnMethod = new HashMap<String, Map<String, String>>();
			
			for (String className : dependentClassOwnMethods.keySet()) {
				String classFile = className + Constants.CLASS_FILE_POSTFIX;
				// 根据类名从全局的jar文件路径和jar里面的class文件的映射关系查找类所在的Jar
				String jarName = allClassJarMapping.get(classFile);
				if (jarName == null) {
					continue;
				}
				// 加载jar文件,然后加载jar文件里要处理的class文件
				ZipFile zipFile = new ZipFile(Constants.M2_REPO + jarName);
				Enumeration<? extends ZipEntry> en = zipFile.entries();
				while (en.hasMoreElements()) {
					ZipEntry e = en.nextElement();
					String name = e.getName();
					// 只处理依赖的class文件
					if (name.equals(classFile)) {
						DependencyVisitor visitor = new DependencyVisitor(
								name.substring(0, name.length() - 6),
								// dependentClassOwnMethods.get(className) 返回目标class里要查找的方法, 不需要查找类的所有方法
								dependentClassOwnMethods.get(className),
								// 全局的类方法的依赖,如果已经类方法已经在全局Map里存在了,就不需要再次处理,可以避免死循环
								classMethodDependentGraph, 
								// 不需要处理目标工程里的Class文件
								allLocalClasses);
						ClassReader classReader = new ClassReader(zipFile.getInputStream(e));
						classReader.accept(visitor, 0);
						if(!classTypeMap.containsKey(className)){
							ClassTypeBean classType = new ClassTypeBean();
							classType.setClassName(className);
							classType.setFinalClass(visitor.isFinalClass());
							classType.setHasDefaultConstructor(visitor.isHasDefaultConstructor());
							classTypeMap.put(className, classType);
						}
						
						
						Map<String, String> methodDependentMap = visitor
								.getMethodDependentMap();
						if(methodDependentMap.size() == 0){
							// 这个类的被依赖方法没有再依赖新的外部方法,所以直接返回.这个类的这些方法被认为是叶子节点
							continue;
						}
						methodDependentCount += methodDependentMap.size();
						// 把单个类的方法的依赖关系放到全局的类的方法的依赖集合中
						classMethodDependentGraph.putAll(methodDependentMap);
						for (String sourceMethod: methodDependentMap.keySet()){
							String methodDependentStr = methodDependentMap.get(sourceMethod);
							String[] methods = methodDependentStr.split(Constants.SEPARATOR_COMMA);
							for (int i = 0; i < methods.length; i++) {
								String targetMethod = methods[i];
								// for example: method =
								// com/xiaonei/tribe/model/TribeUser setSelected
								// (I)V
								String[] methodDetail = targetMethod.split(" ");
								String methodSignature = methodDetail[1]
										+ " " + methodDetail[2] + " " + methodDetail[3];
								
								// 如果下层中的节点依赖上层中的某个节点,直接跳过,不需要给上层节点重复计算
								if(allNodes.containsKey(targetMethod)){
									continue;
								}
								
								if (thisRoundDependentClassesOwnMethod
										.containsKey(methodDetail[0])) {
									Map<String, String> ownMethods = thisRoundDependentClassesOwnMethod
											.get(methodDetail[0]);
									
									if (!ownMethods
											.containsKey(methodSignature)) {
										ownMethods.put(methodSignature, null);
										methodCount++;
									}

								} else {
									Map<String, String> ownMethods = new HashMap<String, String>();
									ownMethods.put(methodSignature, null);
									thisRoundDependentClassesOwnMethod.put(methodDetail[0],
											ownMethods);
									methodCount++;
								}
								// 为打印结果树提供数据,与逻辑无关
								if(!dependentDescriptionMap.containsKey(round)){
									dependentDescriptionMap.put(round, new HashMap<String, DependentDescriptor>());
								}
								Map<String, DependentDescriptor> thisRoundNodes = dependentDescriptionMap.get(round);
								if(!thisRoundNodes.containsKey(targetMethod)){
									DependentDescriptor description = new DependentDescriptor();
									description.setClassName(methodDetail[0]);
									description.setMethodName(methodSignature);
									description.setJarName(allClassJarMapping.get(methodDetail[0] + Constants.CLASS_FILE_POSTFIX));
									thisRoundNodes.put(targetMethod, description);
									allNodes.put(description.toString(), description);
									
									DependentDescriptor parent = allNodes.get(sourceMethod);
									parent.addChildDependentObject(description);
								}
								
								DependentDescriptor child = thisRoundNodes.get(targetMethod);
								DependentDescriptor parent = allNodes.get(sourceMethod);
								child.addParenetDependentObjec(parent);
//								allNodes.put(child.toString(), child);
							}
						}
					}
				}
			}
			
//			System.out.println("Round " + round + ": " + dependentClassOwnMethods.size() + "个外部类的" 
//					+ methodDependentCount + "个方法依赖外部" + thisRoundDependentClassesOwnMethod.size() + "个类的" + methodCount + "个方法");
			System.out.println("Round " + round + ": " + dependentClassOwnMethods.size() + "个外部类的" 
					+ methodDependentCount + "个方法依赖外部" + thisRoundDependentClassesOwnMethod.size() + "个类的" + methodCount + "个方法");
			round++;
			dependentClassOwnMethods = thisRoundDependentClassesOwnMethod;
		}

	}
	
	// 生成被调用方法--> 调用方法的映射
	public void generateCallHierarchMap(){
		for(String method: classMethodDependentGraph.keySet()){
			String dependentClassMethods = classMethodDependentGraph.get(method);
			String[] dependentClassMethodArray = dependentClassMethods.split(Constants.SEPARATOR_COMMA);
			for(String dependentClassMethod: dependentClassMethodArray){
				if(callHierarchyGraph.containsKey(dependentClassMethod)){
					callHierarchyGraph.get(dependentClassMethod).add(method);
				}else{
					Set<String> methods = new HashSet<String>();
					methods.add(method);
					callHierarchyGraph.put(dependentClassMethod, methods);
				}
			}
		}
	}
	
	// 按照被调用方法搜索
	public void searchCallHierarchy(String method, StringBuilder sb){
		if(callHierarchyGraph.containsKey(method)){
			Set<String> callerMethods = callHierarchyGraph.get(method);
			for(Iterator<String> it = callerMethods.iterator(); it.hasNext();){
				String callerMethod = it.next();
				if(sb.length() > 0){
					sb.append(" <--- " + callerMethod);
				}else{
					sb.append(method + " <--- " + callerMethod);
				}
				
				if(!callHierarchyGraph.containsKey(callerMethod)){
					System.out.println(sb.toString());
					sb.delete((sb.length() - callerMethod.length() + 6) , sb.length());
					continue;
				}
				searchCallHierarchy(callerMethod, sb);
				sb.delete((sb.length() - callerMethod.length() + 6) , sb.length());
			}
		}
	}

//	// 生成依赖树
//	private void generateDependentDescriptorTree(){
//		// 第round -1 轮时依赖0个类的0个方法,所有从round-2开始有数据
//		for(int i = round - 2; i > 0; i--){
//			Map<String, DependentDescriptor> childRoundDependentMap = dependentDescriptionMap.get(i);
//			Map<String, DependentDescriptor> parentRoundDependentMap = dependentDescriptionMap.get(i-1);
//			for(String method: childRoundDependentMap.keySet()){
//				DependentDescriptor child = childRoundDependentMap.get(method);
//				DependentDescriptor parnet = parentRoundDependentMap.get(child.getParent());
//				parnet.addDependentDescriptor(child);
//			}
//		}
//	}
	
	// 先序遍历依赖树
	private void traverseDependencyTree(DependentDescriptor node, int index, StringBuilder stringBuilder){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i< index; i++){
			sb.append("--");
		}
		stringBuilder.append("|").append(sb.toString()).append(node.getClassName() != null ? node.getClassName() : "").append(" ").append(node.getMethodName() != null ? node.getMethodName() : "").append("\n");
		if(node.getChildDependentObjectList().size() > 0 && index < round -1){
			for(DependentDescriptor child : node.getChildDependentObjectList()){
				traverseDependencyTree(child, index + 1, stringBuilder);
			}
		}
	}
	
	public String displayDependencyTree(){
		StringBuilder stringBuilder = new StringBuilder();
		traverseDependencyTree(dependentTreeRoot, 0, stringBuilder);
		return stringBuilder.toString();
	}
	
	// 先序遍历树,收集叶子节点
	public void recursiveCollectNodes(DependentDescriptor node){
		if(node.getChildDependentObjectList().size() == 0){
			leafNodes.put(node.toString(), node);
			return;
		}
		for(DependentDescriptor child : node.getChildDependentObjectList()){
			recursiveCollectNodes(child);
		}
	}
	
	public Map<String, DependentDescriptor> collectLeafNodes(){
		recursiveCollectNodes(dependentTreeRoot);
		return leafNodes;
	}

	
	// 给找到的顶层顶点添加Exception信息
	public Map<String, DependentDescriptor> addExceptionInfoForDependency(Map<String, DependentDescriptor> nodes) throws Exception{
		// map 结构:  jar1 -- class1 -- m1-d1
		//                          -- m2-d2
		//                   class2 -- m3-d3
		//           jar2 -- class3 -- m4-d4
		Map<String, Map<String,Map<String, DependentDescriptor>>> dependentClassOwnMethods = new HashMap<String, Map<String, Map<String, DependentDescriptor>>>();
		for(DependentDescriptor dependentDescription: nodes.values()){
			String jarName = dependentDescription.getJarName();
			if(jarName == null){
				continue;
			}
			String className = dependentDescription.getClassName();
			if(dependentClassOwnMethods.containsKey(jarName)){
				Map<String,Map<String, DependentDescriptor>> classesMethods = dependentClassOwnMethods.get(jarName);
				Map<String, DependentDescriptor> methods = classesMethods.get(className);
				if(methods == null){
					methods = new HashMap<String, DependentDescriptor>();
				}
				methods.put(dependentDescription.toString(), dependentDescription);
				if(!classesMethods.containsKey(className)){
					classesMethods.put(className, methods);
				}
			}else{
				Map<String,Map<String, DependentDescriptor>> classesMethods = new HashMap<String,Map<String, DependentDescriptor>>();
				Map<String, DependentDescriptor> methods = new HashMap<String, DependentDescriptor>();
				methods.put(dependentDescription.toString(), dependentDescription);
				classesMethods.put(className, methods);
				dependentClassOwnMethods.put(jarName, classesMethods);
			}
		}
		
		for(String jarName: dependentClassOwnMethods.keySet()){
			// 加载jar文件,然后加载jar文件里要处理的class文件
			ZipFile zipFile = new ZipFile(Constants.M2_REPO + jarName);
			Enumeration<? extends ZipEntry> en = zipFile.entries();
			Map<String,Map<String, DependentDescriptor>> classesMethods = dependentClassOwnMethods.get(jarName);
			Map<String, String> classNameMap = new HashMap<String, String>();
			for(String clazz: classesMethods.keySet()){
				String className = clazz + Constants.CLASS_FILE_POSTFIX;
				classNameMap.put(className, null);
			}
			while (en.hasMoreElements()) {
				ZipEntry e = en.nextElement();
				String name = e.getName();
				
				// 只处理依赖的class文件
				if (classNameMap.containsKey(name)) {
					String className = name.substring(0, name.length() - 6);
					Map<String, DependentDescriptor> classMethod = classesMethods.get(className);
					FieldAndExceptionVisitor vistor = new FieldAndExceptionVisitor(
							className,
							classMethod, classStaticFiledDependentMap);
					ClassReader classReader = new ClassReader(zipFile.getInputStream(e));
					classReader.accept(vistor, 0);
				}
			}
		}
		
		// 将添加了Exception信息后的节点组合到一个Map里返回
		Map<String, DependentDescriptor> result = new HashMap<String, DependentDescriptor>();
		for(Map<String,Map<String, DependentDescriptor>> classesMethods : dependentClassOwnMethods.values()){
			for(Map<String, DependentDescriptor> methods: classesMethods.values()){
				result.putAll(methods);
			}
		}
		
		return result;
	}
	
	// 计算当前依赖树中节点的个数
	public int countDependentTree(){
		AtomicInteger count = new AtomicInteger();
		count.set(0);
		recursiveCountNode(dependentTreeRoot, count);
		return count.get();
	}
	
	// 递归计算树中节点的个数
	private void recursiveCountNode(DependentDescriptor node, AtomicInteger count){
		count.incrementAndGet();
		for(DependentDescriptor child : node.getChildDependentObjectList()){
			recursiveCountNode(child, count);
		}
	}
	
	//收集黑名单的节点
	public Map<String, DependentDescriptor> generateBlackList(){
		// 收集叶子节点
		recursiveCollectNodes(dependentTreeRoot);
		
		Map<String, DependentDescriptor> blackList = new HashMap<String, DependentDescriptor>();
		
		// 从叶子节点开始往上检查需要加到黑名单的节点
		for(Iterator<Map.Entry<String, DependentDescriptor>> it = leafNodes.entrySet().iterator();it.hasNext();){
			Map.Entry<String, DependentDescriptor> entry = it.next();
			DependentDescriptor node = entry.getValue();
			recursiveCheckNode(node, blackList);
		}

		return blackList;
	}

	// 递归检查节点和它之上的所有节点
	private void recursiveCheckNode(DependentDescriptor node, Map<String, DependentDescriptor> blackList){
		// 不检查本地代码
		if(node.isLocalCode()){
			return;
		}
		
		if(accept(node)){
			blackList.put(node.toString(), node);
		}else{
			for(DependentDescriptor parent: node.getParenetDependentObjectList()){
				recursiveCheckNode(parent, blackList);
			}
		}
	}
	
	// 根据节点找对应的顶层节点
	public Map<String, DependentDescriptor> getTopNodes(Map<String, DependentDescriptor> nodeList){
		Map<String, DependentDescriptor> allTopNodes = new HashMap<String, DependentDescriptor>();
		// 根据黑名单的节点找到它对应的所有顶层节点
		for(DependentDescriptor node: nodeList.values()){
			recursiveGetTopNode(node, allTopNodes);
		}
//		// 从依赖树上删除该顶层节点
//		for(DependentDescriptor node: allTopNodeList.values()){
//			dependentTreeRoot.getChildDependentObjectList().remove(node);
//		}
		
		// 过滤黑名单中的类
		for(Iterator<Entry<String, DependentDescriptor>> it = allTopNodes.entrySet().iterator(); it.hasNext();){
			Entry<String, DependentDescriptor> entry = it.next();
			DependentDescriptor value = entry.getValue();
			if(reject(value)){
				it.remove();
			}
		}
		
		// 在过滤顶层节点时,是根据类或方法的名称来的,第一次过滤得到了所有有外部调用的节点
		// 需要再一次把跟顶层节点的类被目标工程应用的其他方法也加入,因为替换源文件的import,要保证生成的静态代理类包含了所有的被引用的方法
		Map<String, DependentDescriptor> otherRelatedTopNodes = new HashMap<String, DependentDescriptor>();
		// 所有顶层节点
		Map<String, DependentDescriptor> firstRoundNodes = dependentDescriptionMap.get(0);
		Map<String, String> handledClasses = new HashMap<String, String>();
		for(String dependentStr: allTopNodes.keySet()){
			DependentDescriptor dependent = allTopNodes.get(dependentStr);
			String className = dependent.getClassName();
			if(!handledClasses.containsKey(className)){
				Map<String, String> allClassMethods = allTopNodesDependentClassOwnMethods.get(className);
				for(String method: allClassMethods.keySet()){
					String classMethodDescription = className + " " + method;
					if(!allTopNodes.containsKey(classMethodDescription)){
						DependentDescriptor otherDependentNode = firstRoundNodes.get(classMethodDescription);
						otherRelatedTopNodes.put(classMethodDescription, otherDependentNode);
					}
				}
				handledClasses.put(className, null);
			}
			
		}
		allTopNodes.putAll(otherRelatedTopNodes);
		return allTopNodes;
	}
	
	// 递归获得该节点对应的顶层节点
	private void recursiveGetTopNode(DependentDescriptor node, Map<String, DependentDescriptor> allTopNodeList){
		if(node.isLocalCode()) {
			return;
		}
		
		if(node.getParenetDependentObjectList() != null && node.getParenetDependentObjectList().get(0).isLocalCode() == true){
			allTopNodeList.put(node.toString(), node);
		}else{
			for(DependentDescriptor parent: node.getParenetDependentObjectList()){
				recursiveGetTopNode(parent, allTopNodeList);
			}
		}

	}
	
	// 标记白名单
	public boolean accept(DependentDescriptor node){
		String className = node.getClassName();
		if(classFilter.accept(className)){
			return true;
		}else{
			return false;
		}
	}
	
	// 标记黑名单
	public boolean reject(DependentDescriptor node){
		String className = node.getClassName();
		if(classFilter.reject(className)){
			return true;
		}else{
			return false;
		}
	}
	
	// 把DependentDescriptor按照Class类组织
	public Map<String, Set<DependentDescriptor>> getDependentClassMethodsByNodes(Map<String, DependentDescriptor> nodes){
		Map<String, Set<DependentDescriptor>> classMethods = new HashMap<String, Set<DependentDescriptor>>();
		for(DependentDescriptor dependent: nodes.values()){
			String className = dependent.getClassName();
			if(classMethods.containsKey(className)){
				Set<DependentDescriptor> methods = classMethods.get(className);
				methods.add(dependent);
			}else{
				Set<DependentDescriptor> methods = new HashSet<DependentDescriptor>();
				methods.add(dependent);
				classMethods.put(className, methods);
			}
		}
		return classMethods;
	}
	
	public Map<String, ClassTypeBean> getDependentClassType(Map<String, Set<DependentDescriptor>> classMethods){
		for(String className: classMethods.keySet()){
			ClassTypeBean classType = classTypeMap.get(className);
			if(classType == null){
				// TODO
			}
			Set<DependentDescriptor> methods = classMethods.get(className);
			boolean allStatic = true;
			boolean allInstance = true;
			boolean isInterface = false;
			boolean isSingleton = false;
			boolean isXoaInterface = false;
			boolean isInnerClass = className.contains(Constants.INNER_CLASS_FLAG);
			if(isInnerClass){
				classType.setClassType(Constants.CLASS_TYPE_IS_INNER_CLASS);
				continue;
			}
			for(Iterator<DependentDescriptor> it = methods.iterator(); it.hasNext();){
				DependentDescriptor dependent = it.next();
				isInterface = dependent.isInterface();
				// 处理xoa service
				if(Constants.XOA_SERVICE_ANNONATION.equals(dependent.getAnnotation())){
					isXoaInterface = true;
				}
				if(dependent.getMethodName().contains(Constants.ACC_NON_STATIC)){
					allStatic = false;
				}else{
					allInstance = false;
				}
				
				MethodObjectBean methodObject = MethodParser.parseMethodDescription(dependent.toStringWithException(), dependent.isVarArgs());
				// 约定大于配置,有getInstance,并且是静态的,返回值等于和所在类相同的,认为是单实例模式
				if(Constants.TEMPLATE_CONSTANT_GET_INSTANCE.equals(methodObject.getMethodName()) && (Constants.ACC_STATIC.equals(methodObject.getAccStatic()) && methodObject.getReturnType().equals(methodObject.getClassName()))){
					isSingleton = true;
				}
			}
			
			if(isXoaInterface){
				classType.setClassType(Constants.CLASS_TYPE_IS_XOA_INTERFACE);
			}else if(isInterface){
				classType.setClassType(Constants.CLASS_TYPE_IS_INTERFACE);
			}else if(allStatic){
				classType.setClassType(Constants.CLASS_TYPE_ALL_STATIC_METHOD_CLASS);
			}else if(allInstance){
				classType.setClassType(Constants.CLASS_TYPE_ALL_INSTANCE_METHOD_CLASS);
			}else if(isSingleton){
				classType.setClassType(Constants.CLASS_TYPE_IS_SINGLETON);
			}else {
				classType.setClassType(Constants.CLASS_TYPE_BOTH_STATIC_INSTANCE_METHOD_CLASS);
			}
			classTypeMap.put(className, classType);
		}
		
		return classTypeMap;
	}
	
	public void generateStaticProxyFile(Map<String, Set<DependentDescriptor>> classMethodsMap, Map<String, ClassTypeBean> classTypeMap){
		// 待处理的接口和类, 可以用于结果的统计
		Map<String, Set<DependentDescriptor>> interfaceClassMethodsMap = new HashMap<String, Set<DependentDescriptor>>();
		Map<String, Set<DependentDescriptor>> xoaInterfaceClassMethodsMap = new HashMap<String, Set<DependentDescriptor>>();
		Map<String, Set<DependentDescriptor>> allInstanceMethodClassMethodsMap = new HashMap<String, Set<DependentDescriptor>>();
		Map<String, Set<DependentDescriptor>> singletonMethodClassMethodsMap = new HashMap<String, Set<DependentDescriptor>>();
		Map<String, Set<DependentDescriptor>> allStaticMethodClassMethodsMap = new HashMap<String, Set<DependentDescriptor>>();
		Map<String, Set<DependentDescriptor>> bothStaticInstanceMethodClassMethodsMap = new HashMap<String, Set<DependentDescriptor>>();
		Map<String, Set<DependentDescriptor>> innerClassMethodsMap = new HashMap<String, Set<DependentDescriptor>>();
		
		// 处理的接口和类, 可以用于结果的统计
		Map<String, Set<DependentDescriptor>> handledXoaInterfaceMap = new HashMap<String, Set<DependentDescriptor>>();
		Map<String, Set<DependentDescriptor>> handledInterfaceMap = new HashMap<String, Set<DependentDescriptor>>();
		Map<String, Set<DependentDescriptor>> handledInstanceClassMap = new HashMap<String, Set<DependentDescriptor>>();
		Map<String, Set<DependentDescriptor>> handledSingletonClassMap = new HashMap<String, Set<DependentDescriptor>>();
		Map<String, Set<DependentDescriptor>> handledAllStaticMap = new HashMap<String, Set<DependentDescriptor>>();
		// 未处理的接口和类, 可以用于结果的统计
		Map<String, Set<DependentDescriptor>> unHandledInterfaceWithoutFactoryMethodMap = new HashMap<String, Set<DependentDescriptor>>();
		Map<String, Set<DependentDescriptor>> unHandledInterfaceReturnByInterfaceMap = new HashMap<String, Set<DependentDescriptor>>();
		Map<String, Set<DependentDescriptor>> unHandledInterfaceReturnByInnerClassMap = new HashMap<String, Set<DependentDescriptor>>();
		
		Map<String, Set<DependentDescriptor>> unHandledClassWithoutFactoryMethodMap = new HashMap<String, Set<DependentDescriptor>>();
		Map<String, Set<DependentDescriptor>> unHandledClassReturnByInterfaceMap = new HashMap<String, Set<DependentDescriptor>>();
		Map<String, Set<DependentDescriptor>> unHandledClassReturnByInnerClassMap = new HashMap<String, Set<DependentDescriptor>>();
		Map<String, Set<DependentDescriptor>> unHandledClassWithoutDefaultConstructorMap = new HashMap<String, Set<DependentDescriptor>>();
		Map<String, Set<DependentDescriptor>> unHandledClassIsFinalClassMap = new HashMap<String, Set<DependentDescriptor>>();
		
		Map<String, Set<DependentDescriptor>> unHandledClassReturnFinalClassMap = new HashMap<String, Set<DependentDescriptor>>();
		Map<String, Set<DependentDescriptor>> unHandledClassReturnClassWithoutDefaultConstructorMap = new HashMap<String, Set<DependentDescriptor>>();
		
		// 获得数据结构: 返回值 - Set<DependentDescriptor>的结构
		Map<String, Set<DependentDescriptor>> returnTypeMethodMap = new HashMap<String, Set<DependentDescriptor>>();
		
		for(String className: classMethodsMap.keySet()){
			Set<DependentDescriptor> methods = classMethodsMap.get(className);

			ClassTypeBean classType = classTypeMap.get(className);
			switch(classType.getClassType()){
			case Constants.CLASS_TYPE_IS_XOA_INTERFACE:
				interfaceClassMethodsMap.put(className, methods);
				xoaInterfaceClassMethodsMap.put(className, methods);
				break;
			case Constants.CLASS_TYPE_IS_INTERFACE:
				interfaceClassMethodsMap.put(className, methods);
				break;
			case Constants.CLASS_TYPE_ALL_INSTANCE_METHOD_CLASS:
				allInstanceMethodClassMethodsMap.put(className, methods);
				break;
			case Constants.CLASS_TYPE_ALL_STATIC_METHOD_CLASS:
				allStaticMethodClassMethodsMap.put(className, methods);
				break;
			case Constants.CLASS_TYPE_IS_SINGLETON:
				singletonMethodClassMethodsMap.put(className, methods);
				break;
			case Constants.CLASS_TYPE_BOTH_STATIC_INSTANCE_METHOD_CLASS:
				bothStaticInstanceMethodClassMethodsMap.put(className, methods);
				break;
			case Constants.CLASS_TYPE_IS_INNER_CLASS:
				innerClassMethodsMap.put(className, methods);
				break;
			}
			// 获得数据结构: 返回值 - Set<DependentDescriptor>的结构
			for(Iterator<DependentDescriptor> it= methods.iterator(); it.hasNext();){
				DependentDescriptor dependent = it.next();
				MethodObjectBean methodObject = MethodParser.parseMethodDescription(dependent.toStringWithException(), dependent.isVarArgs());
				String returnType = methodObject.getReturnType();
				// 除去基本类型,数组和java的类
				if(returnType.contains(".") && !returnType.contains("[") && !returnType.startsWith("java")){
					if(!returnTypeMethodMap.containsKey(returnType)){
						Set<DependentDescriptor> dependentSet = new HashSet<DependentDescriptor>();
						returnTypeMethodMap.put(returnType, dependentSet);
					}
					Set<DependentDescriptor> dependentSet = returnTypeMethodMap.get(returnType);
					dependentSet.add(dependent);
				}
			}
		}
		
		for(String interfaceNameStr: interfaceClassMethodsMap.keySet()){
			String interfaceName = interfaceNameStr.replace(Constants.SEPARATOR_SLASH, Constants.SEPARATOR_POINT);
			Set<DependentDescriptor> dependentSet = returnTypeMethodMap.get(interfaceName);
			if(xoaInterfaceClassMethodsMap.containsKey(interfaceNameStr)){
//				System.out.println("该接口是XOA服务接口,已经被专门处理: " + interfaceName);
				handledXoaInterfaceMap.put(interfaceNameStr, null);
				continue;
			}else if(dependentSet == null){
//				System.out.println("该接口没有工厂方法返回: " + interfaceName);
				unHandledInterfaceWithoutFactoryMethodMap.put(interfaceNameStr, dependentSet);
				continue;
			}
			for(Iterator<DependentDescriptor> it = dependentSet.iterator(); it.hasNext();){
				DependentDescriptor dependent = it.next();
				ClassTypeBean classType = classTypeMap.get(dependent.getClassName());
				if(classType.getClassType() == Constants.CLASS_TYPE_IS_INTERFACE || classType.getClassType() == Constants.CLASS_TYPE_IS_XOA_INTERFACE){
					unHandledInterfaceReturnByInterfaceMap.put(interfaceNameStr, dependentSet);
//					System.out.println("该接口由接口方法返回, 暂不处理: " + interfaceName + " " + dependent.toStringWithException());
				}else if(classType.getClassType() == Constants.CLASS_TYPE_IS_INNER_CLASS){
					unHandledInterfaceReturnByInnerClassMap.put(interfaceNameStr, dependentSet);
//					System.out.println("该接口由inner class返回, 暂不处理: " + interfaceName + " " + dependent.toStringWithException());
				}else{
					dependent.setMethodType(Constants.METHOD_TYPE_JAVA_PROXY_METHOD);
					handledInterfaceMap.put(interfaceNameStr, null);
				}
			}
		}
		System.out.println("-----------------");
		for(String instanceClassName: allInstanceMethodClassMethodsMap.keySet()){
			ClassTypeBean instanceClassType = classTypeMap.get(instanceClassName);
			boolean isFinalClass = instanceClassType.isFinalClass();
			boolean hasDefaultConstructor = instanceClassType.hasDefaultConstructor();
			// 返回值是该类的方法的集合
			Set<DependentDescriptor> dependentSet = returnTypeMethodMap.get(instanceClassName.replace(Constants.SEPARATOR_SLASH, Constants.SEPARATOR_POINT));
			if(dependentSet == null){
//				System.out.println("该类没有工厂方法返回: " + instanceClassName);
				unHandledClassWithoutFactoryMethodMap.put(instanceClassName, dependentSet);
				continue;
			}
			for(Iterator<DependentDescriptor> it = dependentSet.iterator(); it.hasNext();){
				DependentDescriptor dependent = it.next();
				ClassTypeBean classType = classTypeMap.get(dependent.getClassName());
				if(classType.getClassType() == Constants.CLASS_TYPE_IS_INTERFACE){
					unHandledClassReturnByInterfaceMap.put(instanceClassName, dependentSet);
//					System.out.println("该类由接口方法返回, 暂不处理: " + instanceClassName + " " + dependent.toStringWithException());
				}else if(classType.getClassType() == Constants.CLASS_TYPE_IS_INNER_CLASS){
					unHandledClassReturnByInnerClassMap.put(instanceClassName, dependentSet);
//					System.out.println("该类由内部类返回, 暂不处理: " + instanceClassName + " " + dependent.toStringWithException());
				}else{
					// 如果这个类可以被Cglib代理,就用Cglib代理,否则用静态代理
					if(!isFinalClass && hasDefaultConstructor){
						// 设置方法类型, 返回类的工厂方法. 由纯静态类或单实例工厂类返回
						dependent.setMethodType(Constants.METHOD_TYPE_CGLIB_PROXY_METHOD);
						handledInstanceClassMap.put(instanceClassName, null);
					}else if(!isFinalClass && !hasDefaultConstructor){
						// 如果类的方法返回了不能被代理的类,那么这个类也不处理,防止出现在一个本地文件里面引用了这个类,既有代理的,又有没代理的,统一不处理
						classType.setClassType(Constants.CLASS_TYPE_RETURN_CLASS_DONOT_HAVE_DEFAULT_CONSTRUCTOR);
						// 不处理不能被代理的类
						unHandledClassWithoutDefaultConstructorMap.put(instanceClassName, dependentSet);
//						System.out.println("该实例类是没有无参的构造函数,不能被代理: " + instanceClassName + " " + dependent.toStringWithException());
					}else{
						// 如果类的方法返回了不能被代理的类,那么这个类也不处理,防止出现在一个本地文件里面引用了这个类,既有代理的,又有没代理的,统一不处理
						classType.setClassType(Constants.CLASS_TYPE_RETURN_CLASS_CONTAIN_FINAL_CLASS_FACTORY_METHOD);
						// 不处理不能被代理的类
						unHandledClassIsFinalClassMap.put(instanceClassName, dependentSet);
//						System.out.println("该实例类是final类,不能被代理: " + instanceClassName + " " + dependent.toStringWithException());
					}
				}
			}
		}
		System.out.println("-----------------");
		TemplateGenerator.generateManalFile("JavaDynamicProxy", null, Constants.TEMPLATE_JAVA_DYNAMIC_PROXY, false);
		TemplateGenerator.generateManalFile("CglibDynamicProxy", null, Constants.TEMPLATE_CGLIB_DYNAMIC_PROXY, false);
		TemplateGenerator.generateManalFile("Advisor", null, Constants.TEMPLATE_ADVISOR, false);
		TemplateGenerator.generateManalFile("LogAdvisor", null, Constants.TEMPLATE_LOG_ADVISOR, false);
		TemplateGenerator.generateManalFile("ServiceMonitor", null, Constants.TEMPLATE_SERVICE_MONITOR, false);
		TemplateGenerator.generateManalFile("ServiceMonitorFactory", null, Constants.TEMPLATE_SERVICE_MONITOR_FACTORY, false);
		TemplateGenerator.generateManalFile("MonitorInfoBean", null, Constants.TEMPLATE_MONITOR_INFO_BEAN, false);
		TemplateGenerator.generateManalFile("IPService", null, Constants.TEMPLATE_IP_SERVICE, false);
		TemplateGenerator.generateManalFile("SequenceService", null, Constants.TEMPLATE_SEQ_NUM, false);
		
		int handledManualClassCount = 0;
		Map<String, String> generatedFileMap = new HashMap<String, String>();
		for(String className: classMethodsMap.keySet()){
			String templateFile = "";
			Set<DependentDescriptor> methods = classMethodsMap.get(className);
			Set<String> fields = classStaticFiledDependentMap.get(className);
			ClassTypeBean classType = classTypeMap.get(className);
			// 先处理manual template的类, 在处理自动生成的类
			if("com/renren/xoa2/client/ServiceFactory".equals(className)){
				TemplateGenerator.generateManalFile("ServiceFactory", methods, "templates/system/ServiceFactoryClass.vm", false);
				handledManualClassCount ++;
			}else if(needManualGeneratedClassesMap.containsKey(className)){
				String targetClassName = className.substring(className
						.lastIndexOf("/") + 1);
				TemplateGenerator.generateManalFile(targetClassName, methods, needManualGeneratedClassesMap.get(className), true);
				handledManualClassCount ++;
			}else{
				if(classType.getClassType() == Constants.CLASS_TYPE_ALL_STATIC_METHOD_CLASS){
					templateFile = Constants.TEMPLATE_STATIC_CLASS;
					TemplateGenerator.generateClassFile(className, methods, fields, templateFile, generatedFileMap);
					handledAllStaticMap.put(className, null);
				}else if(classType.getClassType() == Constants.CLASS_TYPE_IS_SINGLETON){
					templateFile = Constants.TEMPLATE_SINGLETON_CLASS;
					TemplateGenerator.generateClassFile(className, methods, fields, templateFile, generatedFileMap);
					handledSingletonClassMap.put(className, null);
				}else if(classType.getClassType() == Constants.CLASS_TYPE_RETURN_CLASS_DONOT_HAVE_DEFAULT_CONSTRUCTOR){
					unHandledClassReturnClassWithoutDefaultConstructorMap.put(className, null);
				}else if(classType.getClassType() == Constants.CLASS_TYPE_RETURN_CLASS_CONTAIN_FINAL_CLASS_FACTORY_METHOD){
					unHandledClassReturnFinalClassMap.put(className, null);
				}
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append("待处理的依赖数量: \n" );
		sb.append("待处理单实例数量: ").append(singletonMethodClassMethodsMap.size()).append("\n");
		printClassNameInMap(singletonMethodClassMethodsMap, sb);
		sb.append("待处理纯静态类数量: ").append(allStaticMethodClassMethodsMap.size()).append("\n");
		printClassNameInMap(allStaticMethodClassMethodsMap, sb);
		sb.append("待处理的接口数量: ").append(interfaceClassMethodsMap.size()).append("\n");
		printClassNameInMap(interfaceClassMethodsMap, sb);
		sb.append("待处理的纯实例方法类数量: " + allInstanceMethodClassMethodsMap.size()).append("\n");
		printClassNameInMap(allInstanceMethodClassMethodsMap, sb);
		sb.append("待处理的既有静态方法又有实例方法类数量: " + bothStaticInstanceMethodClassMethodsMap.size()).append("\n");;
		printClassNameInMap(bothStaticInstanceMethodClassMethodsMap, sb);
		sb.append("待处理的内部类数量: " + innerClassMethodsMap.size()).append("\n");;
		printClassNameInMap(innerClassMethodsMap, sb);
		sb.append("---------------------\n");
		sb.append("已经处理的依赖数量: " ).append("\n");;
		sb.append("已处理单实例数量: " + handledSingletonClassMap.size()).append("\n");;
		printClassNameInMap(handledSingletonClassMap, sb);
		sb.append("已处理纯静态类数量: " + handledAllStaticMap.size()).append("\n");;
		printClassNameInMap(handledAllStaticMap, sb);
		sb.append("已处理的XOA接口数量: " + handledXoaInterfaceMap.size()).append("\n");;
		printClassNameInMap(handledXoaInterfaceMap, sb);
		sb.append("已处理的普通接口数量: " + handledInterfaceMap.size()).append("\n");;
		printClassNameInMap(handledInterfaceMap, sb);
		sb.append("已处理的纯实例方法类数量: " + handledInstanceClassMap.size()).append("\n");;
		printClassNameInMap(handledInstanceClassMap, sb);
		sb.append("已处理的手动模板类数量: " + handledManualClassCount).append("\n");;
		sb.append("---------------------\n");
		sb.append("未处理的依赖数量: ").append("\n");;
		sb.append("未处理的没有工厂方法返回的接口数量: " + unHandledInterfaceWithoutFactoryMethodMap.size()).append("\n");;
		printClassNameInMap(unHandledInterfaceWithoutFactoryMethodMap, sb);
		sb.append("未处理的由接口返回的接口数量: " + unHandledInterfaceReturnByInterfaceMap.size()).append("\n");;
		printClassNameInMap(unHandledInterfaceReturnByInterfaceMap, sb);
		sb.append("未处理的由内部类返回的接口数量: " + unHandledInterfaceReturnByInnerClassMap.size()).append("\n");;
		printClassNameInMap(unHandledInterfaceReturnByInnerClassMap, sb);
		sb.append("未处理的没有工厂方法返回的纯实例方法类数量: " + unHandledClassWithoutFactoryMethodMap.size()).append("\n");;
		printClassNameInMap(unHandledClassWithoutFactoryMethodMap, sb);
		sb.append("未处理的由接口返回的纯实例方法类数量(有些类会出现在多种情况下,可能某些情况已经处理,有些情况未处理,请在已处理类里面确认下): " + unHandledClassReturnByInterfaceMap.size()).append("\n");;
		printClassNameInMap(unHandledClassReturnByInterfaceMap, sb);
		sb.append("未处理的由内部类返回的纯实例方法类数量: " + unHandledClassReturnByInnerClassMap.size()).append("\n");;
		printClassNameInMap(unHandledClassReturnByInnerClassMap, sb);
		sb.append("未处理的没有无参构造函数的纯实例方法类数量: " + unHandledClassWithoutDefaultConstructorMap.size()).append("\n");;
		printClassNameInMap(unHandledClassWithoutDefaultConstructorMap, sb);
		sb.append("未处理的是final类的纯实例方法类数量: " + unHandledClassIsFinalClassMap.size()).append("\n");;
		printClassNameInMap(unHandledClassIsFinalClassMap, sb);
		sb.append("未处理的方法返回final类的类数量: " + unHandledClassReturnFinalClassMap.size()).append("\n");;
		printClassNameInMap(unHandledClassReturnFinalClassMap, sb);
		sb.append("未处理的方法返回无参构造函数类的类数量: " + unHandledClassReturnClassWithoutDefaultConstructorMap.size()).append("\n");;
		printClassNameInMap(unHandledClassReturnClassWithoutDefaultConstructorMap, sb);
		sb.append("未处理的既有静态方法又有实例方法类数量: " + bothStaticInstanceMethodClassMethodsMap.size()).append("\n");;
		printClassNameInMap(bothStaticInstanceMethodClassMethodsMap, sb);
		sb.append("未处理的内部类数量: " + innerClassMethodsMap.size()).append("\n");;
		printClassNameInMap(innerClassMethodsMap, sb);
		
		try {
			Writer.writeToFile(sb.toString(), "/home/charles/Desktop/result.log");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// 把本地文件的import替换成import生成的静态代理类
	public void updateLocalFileImportReference(Map<String, Set<DependentDescriptor>> outerClassMethods){
		Map<String, Map<String, DependentDescriptor>> localClassOuterDependentMap = new HashMap<String, Map<String, DependentDescriptor>>();
		for(String outerClass : outerClassMethods.keySet()){
			Set<DependentDescriptor> outerMethods = outerClassMethods.get(outerClass);
			for(Iterator<DependentDescriptor> it = outerMethods.iterator(); it.hasNext();){
				DependentDescriptor outerMethod = it.next();
				// 生成过代理类的外部依赖
				if(outerMethod.getGeneratedProxyClassName() != null){
					List<DependentDescriptor> localMethods = outerMethod.getParenetDependentObjectList();
					for(DependentDescriptor localMethod: localMethods){
						String localClassName = localMethod.getClassName();
						if(!localClassOuterDependentMap.containsKey(localClassName)){
							Map<String, DependentDescriptor> outerDependentSet = new HashMap<String, DependentDescriptor>();
							localClassOuterDependentMap.put(localClassName, outerDependentSet);
						}
						Map<String, DependentDescriptor> outerDependentMap = localClassOuterDependentMap.get(localClassName);
						String outerClassName = outerMethod.getClassName();
						// 如果一个本地类依赖同一个外部类的多个方法,只需要替换一次import
						if(!outerDependentMap.containsKey(outerClassName)){
							outerDependentMap.put(outerClassName, outerMethod);
						}
						
					}
				}
			}
		}
		
		StringBuilder sb = new StringBuilder();
		
		for(String localClassName : localClassOuterDependentMap.keySet()){
			// 直接修改本地文件的imort
			String localFilePath = Constants.TARGET_SOURCE_PATH + localClassName + Constants.JAVA_FILE_POSTFIX;
			// 本地类的所有外部依赖对象Map
			Map<String, DependentDescriptor> outerDependentMap = localClassOuterDependentMap.get(localClassName);
			boolean result = FileService.updateFile(localFilePath, outerDependentMap);
			if(result){
				System.out.println("替换了import的文件: " + localFilePath);
			}
		}
		try {
			Writer.writeToFile(sb.toString(), "/home/charles/Desktop/replaceImport.log");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void printClassNameInMap(Map<String, Set<DependentDescriptor>> map, StringBuilder sb){
		for(String className: map.keySet() ){
			sb.append(className).append("\n");
		}
	}
	
	public static void main(final String[] args) throws IOException {
		try {
			ClassMethodDependencyGenerator callHierarchyGenerator = new ClassMethodDependencyGenerator();
			callHierarchyGenerator.generateClassMethodDependentGraph();

			// 生成被调用方法--> 调用方法的映射
			// 生成依赖树
			callHierarchyGenerator.generateCallHierarchMap();
			
			// 查找方法的调用链
			callHierarchyGenerator
					.searchCallHierarchy(
							"com/renren/sns/minisite/xoa/client/TagService sendTagFeedToUser (ILjava/lang/String;I)Lcom/renren/xoa/lite/ServiceFuture; static",
					new StringBuilder());
//			/*
			System.out.println("依赖树中所有节点的个数: " + callHierarchyGenerator.countDependentTree());
			
			Map<String, DependentDescriptor> leafNodes = callHierarchyGenerator.collectLeafNodes();
			Writer.writeToFile(leafNodes, "/home/charles/Desktop/leaf.log");
			
			Map<String, DependentDescriptor> blackListNodes = callHierarchyGenerator.generateBlackList();
			System.out.println("黑名单共有节点个数: " + blackListNodes.size());
			Writer.writeToFile(blackListNodes, "/home/charles/Desktop/blackList.log");
			
			Map<String, DependentDescriptor> topNodes = callHierarchyGenerator.getTopNodes(blackListNodes);
			System.out.println("黑名单关联的顶层节点个数: " + topNodes.size());
			Writer.writeToFile(topNodes, "/home/charles/Desktop/topNodesByBlackList.log");
			
			// 给找到的top nodes添加Exception信息,用于构造代理类的方法
			topNodes = callHierarchyGenerator.addExceptionInfoForDependency(topNodes);
			//按照Class组织的topNode
			Map<String, Set<DependentDescriptor>> classMethods = callHierarchyGenerator.getDependentClassMethodsByNodes(topNodes);
			Writer.writeClassMethodsLog(classMethods, "/home/charles/Desktop/classMethods.log");
			
			Map<String, ClassTypeBean> classTypeMap = callHierarchyGenerator.getDependentClassType(classMethods);
			
			callHierarchyGenerator.generateStaticProxyFile(classMethods, classTypeMap);
			System.out.println("生成代理类");
			
			callHierarchyGenerator.updateLocalFileImportReference(classMethods);

//			String dependencyTree = callHierarchyGenerator.displayDependencyTree();
//			callHierarchyGenerator.writeToFile(dependencyTree, "/home/charles/Desktop/dependentTree.log");
			
			
			int a = 0;
//			*/
		} catch (Exception e) {
			e.printStackTrace();
		}

		
	}

}
