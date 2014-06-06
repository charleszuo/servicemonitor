package com.renren.seo.monitor.outservice.test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;

import com.renren.seo.monitor.outservice.ConstantName;
import com.renren.seo.monitor.outservice.DependencyVisitor2;
import com.renren.seo.monitor.outservice.FileService;
import com.renren.seo.monitor.outservice.obj.DependentDescription;

public class ClassMethodDependencyGenerator2 implements ConstantName{
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
	private DependentDescription dependentTreeRoot = new DependentDescription();

	
	private int methodCount;
	private int round = 0;
	private boolean displayedFlag = false;
	
	public ClassMethodDependencyGenerator2(){
		// 记录所有的目标workspace自己的class文件名
		allLocalClasses = FileService.getInstance().getAllLocalClasses(TARGET_CLASSES_PATH);
		// 根据.classpath文件里记录的项目依赖的相关jar包,记录这些jar包里面所有的class名和所在的jar的映射关系,
		// 方便后面根据class名去文件系统找jar文件来读取相应的class文件
		generateClassJarMapping();
	}
	
	public Map<String, String> getClassMethodDependentGraph(){
		return this.classMethodDependentGraph;
	}
	
	private void generateClassJarMapping() {
		try{
			// 读取.classpath文件
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(TARGET_CLASSPATH_FILENAME)));
			List<String> allJarName = new ArrayList<String>();
			String jarPath = null;
			// String JAR_FILTER = ".*(/com/.*(xiaonei|renren|xce/).*\\.jar).*";
			Pattern p = Pattern.compile(JAR_FILTER);
			while ((jarPath = reader.readLine()) != null) {
				Matcher m = p.matcher(jarPath);
				// 抓取相关的jar名字
				if(m.find() && m.groupCount() > 0){
					allJarName.add(m.group(1));
				}
			}
			// 遍历所有的jar,获取jar里面的class文件名
			for(String jarName: allJarName){
				ZipFile zipFile = new ZipFile(M2_REPO + jarName);
				Enumeration<? extends ZipEntry> en = zipFile.entries();
				while (en.hasMoreElements()) {
					ZipEntry e = en.nextElement();
					String className = e.getName();
					// 只处理class文件
					if (className.endsWith(CLASS_POSTFIX)) {
						allClassJarMapping.put(className, jarName);
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void generateClassMethodDependentGraph(String className) throws Exception {
		String classFile = className + CLASS_POSTFIX;
		// 根据类名从全局的jar文件路径和jar里面的class文件的映射关系查找类所在的Jar
		String jarName = allClassJarMapping.get(classFile);
		// 加载jar文件,然后加载jar文件里要处理的class文件
		ZipFile zipFile = new ZipFile(M2_REPO + jarName);
		Enumeration<? extends ZipEntry> en = zipFile.entries();
		while (en.hasMoreElements()) {
			ZipEntry e = en.nextElement();
			String name = e.getName();
			// 只处理依赖的class文件
			if (name.equals(classFile)) {
				DependencyVisitor2 vistor = new DependencyVisitor2(
						name.substring(0, name.length() - 6),
						allLocalClasses);
				new ClassReader(zipFile.getInputStream(e)).accept(vistor, 0);
				Map<String, String> methodDependentMap = vistor
						.getMethodDependentMap();
				if(methodDependentMap.size() == 0){
					System.out.println(className);
				}else{
					boolean onlyJavaRefFlag = true;
					for(String dependentMethods : methodDependentMap.values()){
						String[] dependentMethod = dependentMethods.split(SEPARATOR);
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
						System.out.println(className);
					}
				}
			}
		}
	}


	
	public static void main(final String[] args) throws IOException {
		try {
			ClassMethodDependencyGenerator2 callHierarchyGenerator = new ClassMethodDependencyGenerator2();
			callHierarchyGenerator.generateClassMethodDependentGraph("com/xiaonei/platform/component/feed/publish/util/PublishAddIN");
			
			int a = 0;
		} catch (Exception e) {
			e.printStackTrace();
		}

		
	}

}
