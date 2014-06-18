package com.renren.userinc.servicemonitor.core;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import com.renren.userinc.servicemonitor.bean.ClassTypeBean;
import com.renren.userinc.servicemonitor.bean.DependentDescriptor;
import com.renren.userinc.servicemonitor.util.Writer;

public class Main {

	public static void main(final String[] args) throws IOException {
		try {
			ServiceMonitorGenerator callHierarchyGenerator = new ServiceMonitorGenerator();
			callHierarchyGenerator.generateClassMethodDependentGraph();

			System.out.println("依赖树中所有节点的个数: " + callHierarchyGenerator.countDependentTree());
			
			Map<String, DependentDescriptor> leafNodes = callHierarchyGenerator.collectLeafNodes();
			Writer.writeToFile(leafNodes, Constants.LOG_DIR, Constants.LOG_LEAF);
			
			Map<String, DependentDescriptor> whiteListNodes = callHierarchyGenerator.generateWhiteList();
			System.out.println("白名单共有节点个数: " + whiteListNodes.size());
			Writer.writeToFile(whiteListNodes, Constants.LOG_DIR, Constants.LOG_WHITE_LIST);
			
			Map<String, DependentDescriptor> topNodes = callHierarchyGenerator.getTopNodes(whiteListNodes);
			System.out.println("白名单关联的顶层节点个数: " + topNodes.size());
			
			// 给找到的top nodes添加Exception信息,用于构造代理类的方法
			topNodes = callHierarchyGenerator.addExceptionInfoForDependency(topNodes);
			//按照Class组织的topNode
			Map<String, Set<DependentDescriptor>> classMethods = callHierarchyGenerator.getDependentClassMethodsByNodes(topNodes);
			Writer.writeClassMethodsLog(classMethods, Constants.LOG_DIR, Constants.LOG_TOP_NODES);
			
			Map<String, ClassTypeBean> classTypeMap = callHierarchyGenerator.getDependentClassType(classMethods);
			
			callHierarchyGenerator.generateStaticProxyFile(classMethods, classTypeMap);
			System.out.println("生成代理类");
			
			callHierarchyGenerator.createJar();
			
			// 更新目标原文件,替换import为生成的代理类
			callHierarchyGenerator.updateLocalFileImportReference(classMethods);

		} catch (Exception e) {
			e.printStackTrace();
		}

		
	}
}
