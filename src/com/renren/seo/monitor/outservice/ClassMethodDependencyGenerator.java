package com.renren.seo.monitor.outservice;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import com.renren.seo.monitor.outservice.obj.ClassType;
import com.renren.seo.monitor.outservice.obj.DependentDescription;
import com.renren.seo.monitor.outservice.obj.MethodObject;
import com.renren.seo.monitor.template.MethodParser;
import com.renren.seo.monitor.template.TemplateGenerator;

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
	private Map<Integer, Map<String, DependentDescription>> dependentDescriptionMap = new HashMap<Integer, Map<String, DependentDescription>>();
	// 方便按树形结构展示依赖的类和方法的关系. 依赖树的根节点,可以先序遍历的方式展示所有的依赖树
	DependentDescription dependentTreeRoot = new DependentDescription();
	private Map<String, DependentDescription> leafNodes = new HashMap<String, DependentDescription>();
	private Map<String, DependentDescription> allNodes = new HashMap<String, DependentDescription>();
	
	private Map<String, Set<String>> classStaticFiledDependentMap = new HashMap<String, Set<String>>();
	
	// 存放目标工程引用的所有外部依赖类和类的方法,在第一轮遍历之后可以获得
	private Map<String, Map<String, String>> allTopNodesDependentClassOwnMethods;
	
	private Map<String, String> needManualGeneratedClassesMap = new HashMap<String, String>();
	
	private int methodCount;
	private int round = 0;
	
	public ClassMethodDependencyGenerator(){
		// 记录所有的目标workspace自己的class文件名
		allLocalClasses = FileService.getInstance().getAllLocalClasses(ConstantName.TARGET_CLASSES_PATH);
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
					new FileInputStream(ConstantName.TARGET_CLASSPATH_FILENAME)));
			List<String> allJarName = new ArrayList<String>();
			String jarPath = null;
			// String JAR_FILTER = ".*(/com/.*(xiaonei|renren|xce/).*\\.jar).*";
			Pattern p = Pattern.compile(ConstantName.JAR_FILTER);
			while ((jarPath = reader.readLine()) != null) {
				Matcher m = p.matcher(jarPath);
				// 抓取相关的jar名字
				if(m.find() && m.groupCount() > 0){
					allJarName.add(m.group(1));
				}
			}
			// 遍历所有的jar,获取jar里面的class文件名
			for(String jarName: allJarName){
				ZipFile zipFile = new ZipFile(ConstantName.M2_REPO + jarName);
				Enumeration<? extends ZipEntry> en = zipFile.entries();
				while (en.hasMoreElements()) {
					ZipEntry e = en.nextElement();
					String className = e.getName();
					// 只处理class文件
					if (className.endsWith(ConstantName.CLASS_POSTFIX)) {
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
			String classFilePath = ConstantName.TARGET_CLASSES_PATH + className
					+ ConstantName.CLASS_POSTFIX;

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
				DependentDescription localCodeDescription = new DependentDescription();
				String[] localMethodDetail = localMethod.split(" ");
				String localMethodSignature = localMethodDetail[1]
						+ " " + localMethodDetail[2] + " " + localMethodDetail[3];
				localCodeDescription.setClassName(localMethodDetail[0]);
				localCodeDescription.setMethod(localMethodSignature);
				localCodeDescription.setIsLocalCode(true);
				dependentTreeRoot.addChildDependentObject(localCodeDescription);
				localCodeDescription.addParenetDependentObjec(dependentTreeRoot);
				
				// for example: methodDependentStr = com/xiaonei/tribe/model/TribeUser setSelected (I)V,com/xiaonei/tribe/model/User methodName (I)V
				// 逗号,是分隔符, methodDependentStr表示一个方法依赖的所有的外部的方法
				String methodDependentStr = methodDependentMap.get(localMethod);
				
				
				String[] methods = methodDependentStr.split(ConstantName.SEPARATOR);
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
					
					// 收集依赖的数据来将结果展示成依赖树,与逻辑无关
					if(!dependentDescriptionMap.containsKey(round)){
						dependentDescriptionMap.put(round, new HashMap<String, DependentDescription>());
					}
					Map<String, DependentDescription> thisRoundNodes = dependentDescriptionMap.get(round);
					// 支持多个上层节点对应一个下层节点
					if(!thisRoundNodes.containsKey(method)){
						DependentDescription description = new DependentDescription();
						description.setClassName(methodDetail[0]);
						description.setMethod(methodSignature);
						description.setJarName(allClassJarMapping.get(methodDetail[0] + ConstantName.CLASS_POSTFIX));
						thisRoundNodes.put(method, description);
						// 加到所有的node里面
						allNodes.put(description.toString(), description);
						// 设置节点父子关系
						localCodeDescription.addChildDependentObject(description);
					}
					DependentDescription child = thisRoundNodes.get(method);
					child.addParenetDependentObjec(localCodeDescription);
//					DependentDescription child = topLevelNodes.get(method);
					
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
		
		while (methodCount != 0 && round <= ConstantName.MAX_SEARCH_ROUND) {
			methodCount = 0;
			methodDependentCount = 0;
			Map<String, Map<String, String>> thisRoundDependentClassesOwnMethod = new HashMap<String, Map<String, String>>();
			
			for (String className : dependentClassOwnMethods.keySet()) {
				String classFile = className + ConstantName.CLASS_POSTFIX;
				// 根据类名从全局的jar文件路径和jar里面的class文件的映射关系查找类所在的Jar
				String jarName = allClassJarMapping.get(classFile);
				if (jarName == null) {
					continue;
				}
				// 加载jar文件,然后加载jar文件里要处理的class文件
				ZipFile zipFile = new ZipFile(ConstantName.M2_REPO + jarName);
				Enumeration<? extends ZipEntry> en = zipFile.entries();
				while (en.hasMoreElements()) {
					ZipEntry e = en.nextElement();
					String name = e.getName();
					// 只处理依赖的class文件
					if (name.equals(classFile)) {
						DependencyVisitor vistor = new DependencyVisitor(
								name.substring(0, name.length() - 6),
								// dependentClassOwnMethods.get(className) 返回目标class里要查找的方法, 不需要查找类的所有方法
								dependentClassOwnMethods.get(className),
								// 全局的类方法的依赖,如果已经类方法已经在全局Map里存在了,就不需要再次处理,可以避免死循环
								classMethodDependentGraph, 
								// 不需要处理目标工程里的Class文件
								allLocalClasses);
						ClassReader classReader = new ClassReader(zipFile.getInputStream(e));
						classReader.accept(vistor, 0);
						
						Map<String, String> methodDependentMap = vistor
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
							String[] methods = methodDependentStr.split(ConstantName.SEPARATOR);
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
									dependentDescriptionMap.put(round, new HashMap<String, DependentDescription>());
								}
								Map<String, DependentDescription> thisRoundNodes = dependentDescriptionMap.get(round);
								if(!thisRoundNodes.containsKey(targetMethod)){
									DependentDescription description = new DependentDescription();
									description.setClassName(methodDetail[0]);
									description.setMethod(methodSignature);
									description.setJarName(allClassJarMapping.get(methodDetail[0] + ConstantName.CLASS_POSTFIX));
									thisRoundNodes.put(targetMethod, description);
									allNodes.put(description.toString(), description);
									
									DependentDescription parent = allNodes.get(sourceMethod);
									parent.addChildDependentObject(description);
								}
								
								DependentDescription child = thisRoundNodes.get(targetMethod);
								DependentDescription parent = allNodes.get(sourceMethod);
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
			String[] dependentClassMethodArray = dependentClassMethods.split(ConstantName.SEPARATOR);
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
//	private void generateDependentDescriptionTree(){
//		// 第round -1 轮时依赖0个类的0个方法,所有从round-2开始有数据
//		for(int i = round - 2; i > 0; i--){
//			Map<String, DependentDescription> childRoundDependentMap = dependentDescriptionMap.get(i);
//			Map<String, DependentDescription> parentRoundDependentMap = dependentDescriptionMap.get(i-1);
//			for(String method: childRoundDependentMap.keySet()){
//				DependentDescription child = childRoundDependentMap.get(method);
//				DependentDescription parnet = parentRoundDependentMap.get(child.getParent());
//				parnet.addDependentDescription(child);
//			}
//		}
//	}
	
	// 先序遍历依赖树
	private void traverseDependencyTree(DependentDescription node, int index, StringBuilder stringBuilder){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i< index; i++){
			sb.append("--");
		}
		stringBuilder.append("|").append(sb.toString()).append(node.getClassName() != null ? node.getClassName() : "").append(" ").append(node.getMethod() != null ? node.getMethod() : "").append("\n");
		if(node.getChildDependentObjectList().size() > 0 && index < round -1){
			for(DependentDescription child : node.getChildDependentObjectList()){
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
	public void recursiveCollectNodes(DependentDescription node){
		if(node.getChildDependentObjectList().size() == 0){
			leafNodes.put(node.toString(), node);
			return;
		}
		for(DependentDescription child : node.getChildDependentObjectList()){
			recursiveCollectNodes(child);
		}
	}
	
	public Map<String, DependentDescription> collectLeafNodes(){
// 		按层收集,结果和递归收集一样
//		for(int i = 0; i < round -1; i++){
//			int roundLeafCount = 0;
//			Map<String, DependentDescription> roundDependentMap = dependentDescriptionMap.get(i);
//			for(String key: roundDependentMap.keySet()){
//				DependentDescription dependentDescription = roundDependentMap.get(key);
//				if(!leafNodes.containsKey(key) && dependentDescription.getChildDependentObjectList().size() == 0){
//					leafNodes.put(key, dependentDescription);
//					roundLeafCount++;
//				}
//			}
//		}
//		
		recursiveCollectNodes(dependentTreeRoot);
		return leafNodes;
	}

	public Map<String, DependentDescription> filterVOLeaf() throws Exception{
		Map<String, Set<DependentDescription>> dependentClassOwnMethods = new HashMap<String, Set<DependentDescription>>();
		for(DependentDescription dependentDescription: leafNodes.values()){
			String jarName = dependentDescription.getJarName();
			if(jarName == null){
				continue;
			}
			if(dependentClassOwnMethods.containsKey(jarName)){
				Set<DependentDescription> dependentSet = dependentClassOwnMethods.get(jarName);
				dependentSet.add(dependentDescription);
			}else{
				Set<DependentDescription> dependentSet = new HashSet<DependentDescription>();
				dependentSet.add(dependentDescription);
				dependentClassOwnMethods.put(jarName, dependentSet);
			}
		}
		
		Map<String, String> VOClasses = new HashMap<String, String>();
		for(String jarName: dependentClassOwnMethods.keySet()){
			// 加载jar文件,然后加载jar文件里要处理的class文件
			ZipFile zipFile = new ZipFile(ConstantName.M2_REPO + jarName);
			Enumeration<? extends ZipEntry> en = zipFile.entries();
			Set<DependentDescription> dependentSet = dependentClassOwnMethods.get(jarName);
			Map<String, String> classNameMap = new HashMap<String, String>();
			for(DependentDescription dependent: dependentSet){
				String className = dependent.getClassName() + ConstantName.CLASS_POSTFIX;
				classNameMap.put(className, null);
			}
			while (en.hasMoreElements()) {
				ZipEntry e = en.nextElement();
				String name = e.getName();
				
				// 只处理依赖的class文件
				if (classNameMap.containsKey(name)) {
					DependencyVisitor2 vistor = new DependencyVisitor2(
							name.substring(0, name.length() - 6),
							allLocalClasses);
					ClassReader classReader = new ClassReader(zipFile.getInputStream(e));
					classReader.accept(vistor, 0);
					// 去除interface和继承了其他类之后,又只依赖java相关类的叶子节点就是普通的VO类
					if((classReader.getAccess() & Opcodes.ACC_INTERFACE) != 0 || !"java/lang/Object".equals(classReader.getSuperName())){
						continue;
					}
					
					Map<String, String> methodDependentMap = vistor
							.getMethodDependentMap();
					
					if(methodDependentMap.size() == 0){
						VOClasses.put(name.substring(0, name.length() - 6), null);
					}else{
						boolean onlyJavaRefFlag = true;
						for(String dependentMethods : methodDependentMap.values()){
							String[] dependentMethod = dependentMethods.split(ConstantName.SEPARATOR);
							for(String method: dependentMethod){
								if(!method.matches("^java.*")){
									onlyJavaRefFlag = false;
									break;
								}
							}
							if(!onlyJavaRefFlag){
								break;
							}
							
						}
						if(onlyJavaRefFlag){
							VOClasses.put(name.substring(0, name.length() - 6), null);
						}
					}
				}
			}
		}
		
		// 从树中删除vo 类
		for(Iterator<Map.Entry<String, DependentDescription>> it = leafNodes.entrySet().iterator();it.hasNext();){
			Map.Entry<String, DependentDescription> entry = it.next();
			String[] methodDetail = entry.getKey().split(" ");
			String className = methodDetail[0];
			if(VOClasses.containsKey(className)){
				// 从树中删除节点
				DependentDescription node = entry.getValue();
				for(DependentDescription parent: node.getParenetDependentObjectList()){
					parent.getChildDependentObjectList().remove(node);
				}
				it.remove();
			}
		}
		
		return leafNodes;
	}
	
	
	
	// 给找到的顶层顶点添加Exception信息
	public Map<String, DependentDescription> addExceptionInfoForDependency(Map<String, DependentDescription> nodes) throws Exception{
		// map 结构:  jar1 -- class1 -- m1-d1
		//                          -- m2-d2
		//                   class2 -- m3-d3
		//           jar2 -- class3 -- m4-d4
		Map<String, Map<String,Map<String, DependentDescription>>> dependentClassOwnMethods = new HashMap<String, Map<String, Map<String, DependentDescription>>>();
		for(DependentDescription dependentDescription: nodes.values()){
			String jarName = dependentDescription.getJarName();
			if(jarName == null){
				continue;
			}
			String className = dependentDescription.getClassName();
			if(dependentClassOwnMethods.containsKey(jarName)){
				Map<String,Map<String, DependentDescription>> classesMethods = dependentClassOwnMethods.get(jarName);
				Map<String, DependentDescription> methods = classesMethods.get(className);
				if(methods == null){
					methods = new HashMap<String, DependentDescription>();
				}
				methods.put(dependentDescription.toString(), dependentDescription);
				if(!classesMethods.containsKey(className)){
					classesMethods.put(className, methods);
				}
			}else{
				Map<String,Map<String, DependentDescription>> classesMethods = new HashMap<String,Map<String, DependentDescription>>();
				Map<String, DependentDescription> methods = new HashMap<String, DependentDescription>();
				methods.put(dependentDescription.toString(), dependentDescription);
				classesMethods.put(className, methods);
				dependentClassOwnMethods.put(jarName, classesMethods);
			}
		}
		
		for(String jarName: dependentClassOwnMethods.keySet()){
			// 加载jar文件,然后加载jar文件里要处理的class文件
			ZipFile zipFile = new ZipFile(ConstantName.M2_REPO + jarName);
			Enumeration<? extends ZipEntry> en = zipFile.entries();
			Map<String,Map<String, DependentDescription>> classesMethods = dependentClassOwnMethods.get(jarName);
			Map<String, String> classNameMap = new HashMap<String, String>();
			for(String clazz: classesMethods.keySet()){
				String className = clazz + ConstantName.CLASS_POSTFIX;
				classNameMap.put(className, null);
			}
			while (en.hasMoreElements()) {
				ZipEntry e = en.nextElement();
				String name = e.getName();
				
				// 只处理依赖的class文件
				if (classNameMap.containsKey(name)) {
					String className = name.substring(0, name.length() - 6);
					Map<String, DependentDescription> classMethod = classesMethods.get(className);
					DependencyVisitor4 vistor = new DependencyVisitor4(
							className,
							classMethod, classStaticFiledDependentMap);
					ClassReader classReader = new ClassReader(zipFile.getInputStream(e));
					classReader.accept(vistor, 0);
				}
			}
		}
		
		// 将添加了Exception信息后的节点组合到一个Map里返回
		Map<String, DependentDescription> result = new HashMap<String, DependentDescription>();
		for(Map<String,Map<String, DependentDescription>> classesMethods : dependentClassOwnMethods.values()){
			for(Map<String, DependentDescription> methods: classesMethods.values()){
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
	private void recursiveCountNode(DependentDescription node, AtomicInteger count){
		count.incrementAndGet();
		for(DependentDescription child : node.getChildDependentObjectList()){
			recursiveCountNode(child, count);
		}
	}
	
	//收集黑名单的节点
	public Map<String, DependentDescription> generateBlackList(){
		// 收集叶子节点
		recursiveCollectNodes(dependentTreeRoot);
		
		Map<String, DependentDescription> blackList = new HashMap<String, DependentDescription>();
		
		// 从叶子节点开始往上检查需要加到黑名单的节点
		for(Iterator<Map.Entry<String, DependentDescription>> it = leafNodes.entrySet().iterator();it.hasNext();){
			Map.Entry<String, DependentDescription> entry = it.next();
			DependentDescription node = entry.getValue();
			recursiveCheckNode(node, blackList);
		}

		return blackList;
	}

	// 递归检查节点和它之上的所有节点
	private void recursiveCheckNode(DependentDescription node, Map<String, DependentDescription> blackList){
		// 不检查本地代码
		if(node.isLocalCode()){
			return;
		}
		
		if(accept(node)){
			blackList.put(node.toString(), node);
		}else{
			for(DependentDescription parent: node.getParenetDependentObjectList()){
				recursiveCheckNode(parent, blackList);
			}
		}
	}
	
	// 根据节点找对应的顶层节点
	public Map<String, DependentDescription> getTopNodes(Map<String, DependentDescription> nodeList){
		Map<String, DependentDescription> allTopNodes = new HashMap<String, DependentDescription>();
		// 根据黑名单的节点找到它对应的所有顶层节点
		for(DependentDescription node: nodeList.values()){
			recursiveGetTopNode(node, allTopNodes);
		}
//		// 从依赖树上删除该顶层节点
//		for(DependentDescription node: allTopNodeList.values()){
//			dependentTreeRoot.getChildDependentObjectList().remove(node);
//		}
		
		// 在过滤顶层节点时,是根据类或方法的名称来的,第一次过滤得到了所有有外部调用的节点
		// 需要再一次把跟顶层节点的类被目标工程应用的其他方法也加入,因为替换源文件的import,要保证生成的静态代理类包含了所有的被引用的方法
		Map<String, DependentDescription> otherRelatedTopNodes = new HashMap<String, DependentDescription>();
		// 所有顶层节点
		Map<String, DependentDescription> firstRoundNodes = dependentDescriptionMap.get(0);
		Map<String, String> handledClasses = new HashMap<String, String>();
		for(String dependentStr: allTopNodes.keySet()){
			DependentDescription dependent = allTopNodes.get(dependentStr);
			String className = dependent.getClassName();
			if(!handledClasses.containsKey(className)){
				Map<String, String> allClassMethods = allTopNodesDependentClassOwnMethods.get(className);
				for(String method: allClassMethods.keySet()){
					String classMethodDescription = className + " " + method;
					if(!allTopNodes.containsKey(classMethodDescription)){
						DependentDescription otherDependentNode = firstRoundNodes.get(classMethodDescription);
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
	private void recursiveGetTopNode(DependentDescription node, Map<String, DependentDescription> allTopNodeList){
		if(node.isLocalCode()) {
			return;
		}
		
		if(node.getParenetDependentObjectList() != null && node.getParenetDependentObjectList().get(0).isLocalCode() == true){
			allTopNodeList.put(node.toString(), node);
		}else{
			for(DependentDescription parent: node.getParenetDependentObjectList()){
				recursiveGetTopNode(parent, allTopNodeList);
			}
		}

	}
	
	// 标记黑名单的规则
	public boolean accept(DependentDescription node){
		
		if(node.getClassName().matches(".*DAO.*")){
			return true;
		}
		if(node.getClassName().matches(".*Service.*")){
			return true;
		}
		if(node.getClassName().matches(".*Adapter.*")){
			return true;
		}
		if(node.getClassName().matches(".*Manager.*")){
			return true;
		}
		if(node.getClassName().matches(".*Factory.*")){
			return true;
		}
		if(node.getClassName().matches(".*Logic.*")){
			return true;
		}
		if(node.getClassName().matches(".*MemCache.*")){
			return true;
		}
		//MenuNewGameHome class
		if(node.getClassName().matches(".*Home")){
			return true;
		}
//		if(node.getClassName().matches(".*xce/.*")){
//			return true;
//		}
//		if(node.getClassName().matches(".*xoa/.*")){
//			return true;
//		}
//		if(node.getClassName().matches(".*tripod/.*")){
//			return true;
//		}
		
		return false;
	}
	
	
	// 把DependentDescription按照Class类组织
	public Map<String, Set<DependentDescription>> getDependentClassMethodsByNodes(Map<String, DependentDescription> nodes){
		Map<String, Set<DependentDescription>> classMethods = new HashMap<String, Set<DependentDescription>>();
		for(DependentDescription dependent: nodes.values()){
			String className = dependent.getClassName();
			if(classMethods.containsKey(className)){
				Set<DependentDescription> methods = classMethods.get(className);
				methods.add(dependent);
			}else{
				Set<DependentDescription> methods = new HashSet<DependentDescription>();
				methods.add(dependent);
				classMethods.put(className, methods);
			}
		}
		return classMethods;
	}
	
	public Map<String, ClassType> getDependentClassType(Map<String, Set<DependentDescription>> classMethods){
		Map<String, ClassType> classTypeMap = new HashMap<String, ClassType>();
		for(String className: classMethods.keySet()){
			Set<DependentDescription> methods = classMethods.get(className);
			boolean allStatic = true;
			boolean allInstance = true;
			boolean isInterface = false;
			boolean isSingleton = false;
			boolean isXoaInterface = false;
			for(Iterator<DependentDescription> it = methods.iterator(); it.hasNext();){
				DependentDescription dependent = it.next();
				isInterface = dependent.isInterface();
				// 处理xoa service
				if(ConstantName.XOA_SERVICE_ANNONATION.equals(dependent.getAnnotation())){
					isXoaInterface = true;
				}
				if(dependent.getMethod().contains(ConstantName.ACC_NON_STATIC)){
					allStatic = false;
				}else{
					allInstance = false;
				}
				
				MethodObject methodObject = MethodParser.parseMethodDescription(dependent.toStringWithException(), dependent.isVarArgs());
				// 约定大于配置,有getInstance,并且是静态的,返回值等于和所在类相同的,认为是单实例模式
				if(ConstantName.GET_INSTANCE.equals(methodObject.getMethodName()) && (ConstantName.ACC_STATIC.equals(methodObject.getAccStatic()) && methodObject.getReturnType().equals(methodObject.getOwner()))){
					isSingleton = true;
				}
			}
			ClassType classType = new ClassType();
			classType.setClassName(className);
			if(isXoaInterface){
				classType.setClassType(ConstantName.CLASS_TYPE_IS_XOA_INTERFACE);
			}else if(isInterface){
				classType.setClassType(ConstantName.CLASS_TYPE_IS_INTERFACE);
			}else if(allStatic){
				classType.setClassType(ConstantName.CLASS_TYPE_ALL_STATIC_METHOD_CLASS);
			}else if(allInstance){
				classType.setClassType(ConstantName.CLASS_TYPE_ALL_INSTANCE_METHOD_CLASS);
			}else if(isSingleton){
				classType.setClassType(ConstantName.CLASS_TYPE_IS_SINGLETON);
			}else {
				classType.setClassType(ConstantName.CLASS_TYPE_BOTH_STATIC_INSTANCE_METHOD_CLASS);
			}
			classTypeMap.put(className, classType);
		}
		
		return classTypeMap;
	}
	
	public void generateStaticProxyFile(Map<String, Set<DependentDescription>> classMethodsMap, Map<String, ClassType> classTypeMap){
		Map<String, Set<DependentDescription>> interfaceClassMethodsMap = new HashMap<String, Set<DependentDescription>>();
		Map<String, Set<DependentDescription>> xoaInterfaceClassMethodsMap = new HashMap<String, Set<DependentDescription>>();
		Map<String, Set<DependentDescription>> allInstanceMethodClassMethodsMap = new HashMap<String, Set<DependentDescription>>();
		Map<String, Set<DependentDescription>> singletonMethodClassMethodsMap = new HashMap<String, Set<DependentDescription>>();
		Map<String, Set<DependentDescription>> allStaticMethodClassMethodsMap = new HashMap<String, Set<DependentDescription>>();
		Map<String, Set<DependentDescription>> bothStaticInstanceMethodClassMethodsMap = new HashMap<String, Set<DependentDescription>>();
		Map<String, Set<DependentDescription>> instanceClassNoFactoryMethodReturn = new HashMap<String, Set<DependentDescription>>();
		
		Map<String, Set<DependentDescription>> unHandleInterfaceClassMethodsMap = new HashMap<String, Set<DependentDescription>>();
		Map<String, Set<DependentDescription>> unHandleInstanceClassClassMethodsMap = new HashMap<String, Set<DependentDescription>>();
		// 获得数据结构: 返回值 - Set<DependentDescription>的结构
		Map<String, Set<DependentDescription>> returnTypeMethodMap = new HashMap<String, Set<DependentDescription>>();
		
		for(String className: classMethodsMap.keySet()){
			Set<DependentDescription> methods = classMethodsMap.get(className);

			
			ClassType classType = classTypeMap.get(className);
			switch(classType.getClassType()){
			case ConstantName.CLASS_TYPE_IS_XOA_INTERFACE:
				interfaceClassMethodsMap.put(className, methods);
				xoaInterfaceClassMethodsMap.put(className, methods);
				break;
			case ConstantName.CLASS_TYPE_IS_INTERFACE:
				interfaceClassMethodsMap.put(className, methods);
				break;
			case ConstantName.CLASS_TYPE_ALL_INSTANCE_METHOD_CLASS:
				allInstanceMethodClassMethodsMap.put(className, methods);
				break;
			case ConstantName.CLASS_TYPE_ALL_STATIC_METHOD_CLASS:
				allStaticMethodClassMethodsMap.put(className, methods);
				break;
			case ConstantName.CLASS_TYPE_IS_SINGLETON:
				singletonMethodClassMethodsMap.put(className, methods);
				break;
			case ConstantName.CLASS_TYPE_BOTH_STATIC_INSTANCE_METHOD_CLASS:
				bothStaticInstanceMethodClassMethodsMap.put(className, methods);
				break;
			}
			// 获得数据结构: 返回值 - Set<DependentDescription>的结构
			for(Iterator<DependentDescription> it= methods.iterator(); it.hasNext();){
				DependentDescription dependent = it.next();
				
				MethodObject methodObject = MethodParser.parseMethodDescription(dependent.toStringWithException(), dependent.isVarArgs());
				String returnType = methodObject.getReturnType();
				// 除去基本类型,数组和java的类
				if(returnType.contains(".") && !returnType.contains("[") && !returnType.startsWith("java")){
					if(!returnTypeMethodMap.containsKey(returnType)){
						Set<DependentDescription> dependentSet = new HashSet<DependentDescription>();
						returnTypeMethodMap.put(returnType, dependentSet);
					}
					Set<DependentDescription> dependentSet = returnTypeMethodMap.get(returnType);
					dependentSet.add(dependent);
				}
			}
		}
		
		int unhandleInterfaceCount = 0;
		int handledInterfaceCount = 0;
		for(String interfaceNameStr: interfaceClassMethodsMap.keySet()){
			String interfaceName = interfaceNameStr.replace(ConstantName.SLASH, ConstantName.POINT);
			Set<DependentDescription> dependentSet = returnTypeMethodMap.get(interfaceName);
			if(xoaInterfaceClassMethodsMap.containsKey(interfaceNameStr)){
				System.out.println("该接口是XOA服务接口,已经被专门处理: " + interfaceName);
				continue;
			}else if(dependentSet == null){
				System.out.println("该接口没有工厂方法返回: " + interfaceName);
				unHandleInterfaceClassMethodsMap.put(interfaceName, dependentSet);
				unhandleInterfaceCount++;
				continue;
			}
			boolean isHandled = false;
			for(Iterator<DependentDescription> it = dependentSet.iterator(); it.hasNext();){
				DependentDescription dependent = it.next();
				ClassType classType = classTypeMap.get(dependent.getClassName());
				if(classType.getClassType() != ConstantName.CLASS_TYPE_IS_INTERFACE){
//					if(dependent.getMethod().contains(ConstantName.ACC_NON_STATIC)){
//						System.out.println("由实例方法返回接口的工厂方法" + dependent.toStringWithException());
//					}else{
						// 设置方法类型, 返回接口的工厂方法
						dependent.setMethodType(ConstantName.METHOD_TYPE_INTERFACE_FACTORY_METHOD);
						isHandled = true;
//					}
				}else{
					unHandleInterfaceClassMethodsMap.put(interfaceName, dependentSet);
					System.out.println("该接口由接口方法返回: " + interfaceName + " " + dependent.toStringWithException());
				}
			}
			if(isHandled){
				handledInterfaceCount ++;
			}else{
				unhandleInterfaceCount ++;
			}
		}
		System.out.println("-----------------");
		int unhandleInstanceClassCount = 0;
		int handledInstanceClassCount = 0;
		for(String instanceClassName: allInstanceMethodClassMethodsMap.keySet()){
			// 返回值是该接口的方法的集合
			Set<DependentDescription> dependentSet = returnTypeMethodMap.get(instanceClassName.replace(ConstantName.SLASH, ConstantName.POINT));
			if(dependentSet == null){
				System.out.println("该类没有工厂方法返回: " + instanceClassName);
				unHandleInstanceClassClassMethodsMap.put(instanceClassName, dependentSet);
				instanceClassNoFactoryMethodReturn.put(instanceClassName, dependentSet);
				unhandleInstanceClassCount ++;
				continue;
			}
			boolean isHandled = false;
			for(Iterator<DependentDescription> it = dependentSet.iterator(); it.hasNext();){
				DependentDescription dependent = it.next();
				ClassType classType = classTypeMap.get(dependent.getClassName());
				if(classType.getClassType() != ConstantName.CLASS_TYPE_IS_INTERFACE){
					if(!instanceClassNoFactoryMethodReturn.containsKey(dependent.getClassName())){
						// 设置方法类型, 返回类的工厂方法. 由纯静态类或单实例工厂类返回
						dependent.setMethodType(ConstantName.METHOD_TYPE_ALL_INSTANCE_METHOD_CLASS_FACTORY_METHOD);
						isHandled = true;
					}else{
						unHandleInstanceClassClassMethodsMap.put(instanceClassName, dependentSet);
						System.out.println("该实例类由实例方法new之后返回: " + instanceClassName + " " + dependent.toStringWithException());
					}
					
				}else{
					unHandleInstanceClassClassMethodsMap.put(instanceClassName, dependentSet);
					System.out.println("该类由接口方法返回: " + instanceClassName + " " + dependent.toStringWithException());
				}
			}
			if(isHandled){
				handledInstanceClassCount ++;
			}else{
				unhandleInstanceClassCount ++;
			}
		}
		System.out.println("-----------------");
		for(String bothStaticInstanceMethodClassName: bothStaticInstanceMethodClassMethodsMap.keySet()){
			System.out.println("该类既有静态方法,又有实例方法,又不是单实例类的,暂时不处理: " + bothStaticInstanceMethodClassName);
		}
		System.out.println("-----------------");
		TemplateGenerator.generateManalFile("JavaDynamicProxy", null, "templates/JavaDynamicProxy.vm", false);
		TemplateGenerator.generateManalFile("CglibDynamicProxy", null, "templates/CglibDynamicProxy.vm", false);
		
		Map<String, String> generatedFileMap = new HashMap<String, String>();
		for(String className: classMethodsMap.keySet()){
			String templateFile = "";
			Set<DependentDescription> methods = classMethodsMap.get(className);
			Set<String> fields = classStaticFiledDependentMap.get(className);
			ClassType classType = classTypeMap.get(className);
			if("com/renren/xoa2/client/ServiceFactory".equals(className)){
				TemplateGenerator.generateManalFile("ServiceFactory", methods, "templates/ServiceFactoryClass.vm", false);
			}else if(needManualGeneratedClassesMap.containsKey(className)){
				String targetClassName = className.substring(className
						.lastIndexOf("/") + 1);
				TemplateGenerator.generateManalFile(targetClassName, methods, needManualGeneratedClassesMap.get(className), true);
			}else{
				if(classType.getClassType() == ConstantName.CLASS_TYPE_ALL_STATIC_METHOD_CLASS){
					templateFile = "templates/staticClass.vm";
					TemplateGenerator.generateClassFile(className, methods, fields, templateFile, generatedFileMap);
				}else if(classType.getClassType() == ConstantName.CLASS_TYPE_IS_SINGLETON){
					templateFile = "templates/singletonClass.vm";
					TemplateGenerator.generateClassFile(className, methods, fields, templateFile, generatedFileMap);
				}else if(classType.getClassType() == ConstantName.CLASS_TYPE_BOTH_STATIC_INSTANCE_METHOD_CLASS){
					bothStaticInstanceMethodClassMethodsMap.put(className, methods);
					continue;
				}
			}
		}
		System.out.println("总共依赖的类数量: " + classTypeMap.size());
		System.out.println("待处理单实例数量: " + singletonMethodClassMethodsMap.size());
		System.out.println("待处理纯静态类数量: " + allStaticMethodClassMethodsMap.size());
		System.out.println("待处理的接口数量: " + interfaceClassMethodsMap.size());
		System.out.println("待处理的纯实例方法类数量: " + allInstanceMethodClassMethodsMap.size());
		System.out.println("待处理的既有静态方法又有实例方法类数量: " + bothStaticInstanceMethodClassMethodsMap.size());
		System.out.println("---------------------");
		System.out.println("已经处理的依赖数量: " );
		System.out.println("\t已处理单实例数量: " + singletonMethodClassMethodsMap.size());
		System.out.println("\t已处理纯静态类数量: " + allStaticMethodClassMethodsMap.size());
		System.out.println("\t已处理的接口数量: " + handledInterfaceCount);
		System.out.println("\t已处理的纯实例方法类数量: " + handledInstanceClassCount);
		System.out.println("未处理的依赖数量: ");
		System.out.println("\t未处理的接口数量: " + unhandleInterfaceCount);
		System.out.println("\t未处理的纯实例方法类数量: " + unhandleInstanceClassCount);
		System.out.println("\t未处理的既有静态方法又有实例方法类数量: " + bothStaticInstanceMethodClassMethodsMap.size());
		
	}
	
	// 把本地文件的import替换成import生成的静态代理类
	public void updateLocalFileImportReference(Map<String, Set<DependentDescription>> outerClassMethods){
		Map<String, Map<String, DependentDescription>> localClassOuterDependentMap = new HashMap<String, Map<String, DependentDescription>>();
		for(String outerClass : outerClassMethods.keySet()){
			Set<DependentDescription> outerMethods = outerClassMethods.get(outerClass);
			for(Iterator<DependentDescription> it = outerMethods.iterator(); it.hasNext();){
				DependentDescription outerMethod = it.next();
				// 生成过代理类的外部依赖
				if(outerMethod.getGeneratedProxyClassName() != null){
					List<DependentDescription> localMethods = outerMethod.getParenetDependentObjectList();
					for(DependentDescription localMethod: localMethods){
						String localClassName = localMethod.getClassName();
						if(!localClassOuterDependentMap.containsKey(localClassName)){
							Map<String, DependentDescription> outerDependentSet = new HashMap<String, DependentDescription>();
							localClassOuterDependentMap.put(localClassName, outerDependentSet);
						}
						Map<String, DependentDescription> outerDependentMap = localClassOuterDependentMap.get(localClassName);
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
			String localFilePath = ConstantName.TARGET_SOURCE_PATH + localClassName + ConstantName.JAVA_FILE_POSTFIX;
			// 本地类的所有外部依赖对象Map
			Map<String, DependentDescription> outerDependentMap = localClassOuterDependentMap.get(localClassName);
			boolean result = FileService.updateFile(localFilePath, outerDependentMap);
			if(result){
				System.out.println("替换了import的文件: " + localFilePath);
			}
			
//			Map<String, DependentDescription> outerMethods = localClassOuterDependentMap.get(localClassName);
//			for(Iterator<DependentDescription> it = outerMethods.values().iterator(); it.hasNext();){
//				DependentDescription outerMethod = it.next();
//				String originalRef = outerMethod.getClassName().replace(ConstantName.SLASH, ConstantName.POINT);
//				String newRef = outerMethod.getGeneratedProxyClassName();
//				sb.append("本地类").append(localClassName).append("要替换import:").append(originalRef).append(" --> ").append(newRef).append("\n");
//				if("com/renren/xoa/lite/ServiceFactories".equals(outerMethod.getClassName())){
//					String originalServiceFactoryRef = "com.renren.xoa.lite.ServiceFactory";
//					String newServiceFactoryRef = "com.renren.seo.serviceproxy.system.generated.ServiceFactory";
//					sb.append("本地类").append(localClassName).append("要替换import:").append(originalServiceFactoryRef).append(" --> ").append(newServiceFactoryRef).append("\n");
//				}
//			}
		}
		try {
			writeToFile(sb.toString(), "/home/charles/Desktop/replaceImport.log");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void writeClassMethodsLog(Map<String, Set<DependentDescription>> classMethods, String logFile) throws Exception{
		StringBuilder sb = new StringBuilder();
		for(String className: classMethods.keySet()){
			Set<DependentDescription> methodSet = classMethods.get(className);
			DependentDescription[] methods = new DependentDescription[methodSet.size()];
			methodSet.toArray(methods);
			sb.append(className).append("--").append(methods[0].getMethod()).append(" ").append(methods[0].getExceptions() == null? "" : methods[0].getExceptions()).append("\n");
			StringBuilder spaceBuilder = new StringBuilder();
			for(int i = 0; i<className.length(); i++){
				spaceBuilder.append(" ");
			}
			String space = spaceBuilder.toString();
			for(int i = 1; i<methods.length; i++){
				sb.append(space).append("--").append(methods[i].getMethod()).append(" ").append(methods[i].getExceptions() == null? "" : methods[i].getExceptions()).append("\n");
			}
		}
		writeToFile(sb.toString(), logFile);
	}
	
	
	// 将节点写到文件
	public void writeToFile(Map<String, DependentDescription> nodes, String logFile) throws Exception{
		BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));  
		StringBuilder filtedLeafBuilder = new StringBuilder();
		for(DependentDescription dependentDescription: nodes.values()){
			filtedLeafBuilder.append(dependentDescription.getClassName()).append(" ").append(dependentDescription.getMethod()).append(" ").append(dependentDescription.getJarName()).append("\n");
		}
		writer.write(filtedLeafBuilder.toString());
		writer.close();
		System.out.println("生成文件: " + logFile);
	}
	// 将节点写到文件
	public void writeToFile(String tree, String logFile) throws Exception{
		BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));  
		writer.write(tree);
		writer.close();
		System.out.println("生成文件: " + logFile);
	}
	
	// 将节点写到控制台
	public void writeToConsole(Map<String, DependentDescription> nodes) throws Exception{
		StringBuilder filtedLeafBuilder = new StringBuilder();
		for(DependentDescription dependentDescription: nodes.values()){
			filtedLeafBuilder.append(dependentDescription.toString()).append(" ").append(dependentDescription.getJarName()).append("\n");
		}
		System.out.println(filtedLeafBuilder.toString());
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
//			System.out.println(dependencyTree);
			System.out.println("依赖树中所有节点的个数: " + callHierarchyGenerator.countDependentTree());
			
			Map<String, DependentDescription> leafNodes = callHierarchyGenerator.collectLeafNodes();
			callHierarchyGenerator.writeToFile(leafNodes, "/home/charles/Desktop/leaf.log");
			
//			Map<String, DependentDescription> filtedLeafNodes = callHierarchyGenerator.filterVOLeaf();
//			System.out.println("过滤掉VO Classes后总共有" + filtedLeafNodes.size() + "个叶子节点:");
//			callHierarchyGenerator.writeToFile(filtedLeafNodes, "/home/charles/Desktop/filted_leaf.log");
//			System.out.println("过滤VO Classes后依赖树中所有节点的个数: " + callHierarchyGenerator.countDependentTree());
			
			Map<String, DependentDescription> blackListNodes = callHierarchyGenerator.generateBlackList();
			System.out.println("黑名单共有节点个数: " + blackListNodes.size());
			callHierarchyGenerator.writeToFile(blackListNodes, "/home/charles/Desktop/blackList.log");
			
			Map<String, DependentDescription> topNodes = callHierarchyGenerator.getTopNodes(blackListNodes);
			System.out.println("黑名单关联的顶层节点个数: " + topNodes.size());
			callHierarchyGenerator.writeToFile(topNodes, "/home/charles/Desktop/topNodesByBlackList.log");
			
			// 给找到的top nodes添加Exception信息,用于构造代理类的方法
			topNodes = callHierarchyGenerator.addExceptionInfoForDependency(topNodes);
			//按照Class组织的topNode
			Map<String, Set<DependentDescription>> classMethods = callHierarchyGenerator.getDependentClassMethodsByNodes(topNodes);
			callHierarchyGenerator.writeClassMethodsLog(classMethods, "/home/charles/Desktop/classMethods.log");
			
			System.out.println("过滤黑名单后依赖树中所有节点的个数: " + callHierarchyGenerator.countDependentTree());
			
			
			Map<String, ClassType> classTypeMap = callHierarchyGenerator.getDependentClassType(classMethods);
			
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
