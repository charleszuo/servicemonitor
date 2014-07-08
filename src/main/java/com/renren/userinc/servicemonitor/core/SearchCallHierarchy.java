package com.renren.userinc.servicemonitor.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.renren.userinc.servicemonitor.util.FileService;

public class SearchCallHierarchy {
	private static SearchCallHierarchy searchCallHierarchy = new SearchCallHierarchy();

	// 被调用方法 - 一组调用它的方法
	private Map<String, Set<String>> callHierarchyGraph = new HashMap<String, Set<String>>();

	private SearchCallHierarchy() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(
					FileService.checkDir(Constants.LOG_DIR)
							+ Constants.LOG_CLASS_METHOD_DEPENDENT_GRAPH));
			String line = null;
			// 生成被调用方法--> 调用方法的映射
			while ((line = reader.readLine()) != null) {
				String[] classMethodDependent = line.split(Constants.SEPARATOR_CURVE);
				String[] dependentClassMethodArray = classMethodDependent[1]
						.split(Constants.SEPARATOR_COMMA);
				for (String dependentClassMethod : dependentClassMethodArray) {
					if (callHierarchyGraph.containsKey(dependentClassMethod)) {
						callHierarchyGraph.get(dependentClassMethod).add(
								classMethodDependent[0]);
					} else {
						Set<String> methods = new HashSet<String>();
						methods.add(classMethodDependent[0]);
						callHierarchyGraph.put(dependentClassMethod, methods);
					}
				}
			}
			if(callHierarchyGraph.size() == 0){
				throw new RuntimeException("Please run the Search Monitor first");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static SearchCallHierarchy getInstance() {
		return searchCallHierarchy;
	}

	// 按照被调用方法搜索
	public void searchCaller(String method) {
		StringBuilder sb = new StringBuilder();
		search(method, sb);
		if(sb.length() == 0){
			System.out.println("Not found");
		}else {
			System.out.println(sb.toString());
		}
	}

	// 按照被调用方法搜索
	private void search(String method, StringBuilder sb) {
		if (callHierarchyGraph.containsKey(method)) {
			Set<String> callerMethods = callHierarchyGraph.get(method);
			for (Iterator<String> it = callerMethods.iterator(); it.hasNext();) {
				String callerMethod = it.next();
				if (sb.length() > 0) {
					sb.append(" <--- " + callerMethod);
				} else {
					sb.append(method + " <--- " + callerMethod);
				}

				if (!callHierarchyGraph.containsKey(callerMethod)) {
					System.out.println(sb.toString());
					sb.delete((sb.length() - callerMethod.length() - 6),
							sb.length());
					continue;
				}
				search(callerMethod, sb);
				sb.delete((sb.length() - callerMethod.length() - 6),
						sb.length());
			}
		}
	}
	
	public static void main(String[] args){
		if(args.length < 1){
			System.out.println("Wrong parameter");
			return;
		}
		String method = args[0];
		SearchCallHierarchy.getInstance().searchCaller(method);
	}
}
