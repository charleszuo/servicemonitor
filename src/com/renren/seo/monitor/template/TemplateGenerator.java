package com.renren.seo.monitor.template;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import com.renren.seo.monitor.outservice.ConstantName;
import com.renren.seo.monitor.outservice.obj.DependentDescription;

public class TemplateGenerator {
	static{
		Properties p = new Properties();
		try {
			p.load(TemplateGenerator.class.getResourceAsStream("/properties/velocity.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		Velocity.init(p);
	}
	
	private static final String generatedFileDir = "/home/charles/workspace_renren/xiaonei-guide/src/test/java/com/renren/seo/serviceproxy/generated/";
	private static final String generatedDuplicateFileDir = "/home/charles/workspace_renren/xiaonei-guide/src/test/java/com/renren/seo/serviceproxy/duplicate/generated/";
	private static final String generatedSystemFileDir = "/home/charles/workspace_renren/xiaonei-guide/src/test/java/com/renren/seo/serviceproxy/system/generated/";
	private static final String packageName = "com.renren.seo.serviceproxy.generated";
	private static final String duplicatePackageName = "com.renren.seo.serviceproxy.duplicate.generated";
	
	public static void generateClassFile(String inputClassName, Set<DependentDescription> methodDescriptionSet, String templateFile, Map<String, String> generatedFileMap) throws Exception{
		VelocityContext context = new VelocityContext();
		String targetClassName = inputClassName.replace(ConstantName.SLASH, ConstantName.POINT);
		String className = targetClassName.substring(targetClassName.lastIndexOf(".") + 1);
		context.put("ClassName", className);
		context.put("TargetClassName", targetClassName);
		String dir = null;
		// 类名有重复,目前只处理最多重复一次
		if(!generatedFileMap.containsKey(className)){
			context.put("PackageName", packageName);
			dir = generatedFileDir;
			generatedFileMap.put(className, null);
		}else{
			dir = generatedDuplicateFileDir;
			context.put("PackageName", duplicatePackageName);
		}
		List<String> methods = new ArrayList<String>();
		for(Iterator<DependentDescription> it = methodDescriptionSet.iterator(); it.hasNext(); ){
			DependentDescription dependent = it.next();
			String classMethodDescription = dependent.toStringWithException();
			if(classMethodDescription.contains(ConstantName.GET_INSTANCE)){
				continue;
			}
			// 需要用模式来重构
			String method = MethodParser.parse(classMethodDescription, dependent.getMethodType());
			StringBuilder methodContentBuilder = new StringBuilder();
			methodContentBuilder.append(method).append(" \n");
			methods.add(method);
		}
		
		context.put("Methods", methods);
		FileWriter writer = new FileWriter(dir + className + ".java");
		Template template = Velocity.getTemplate(templateFile);
		template.merge(context, writer);
		writer.flush();
		writer.close();
	}
	
	public static void generateSystemFile(String fileName, String templateFile) throws Exception{
		VelocityContext context = new VelocityContext();
		FileWriter writer = new FileWriter(generatedSystemFileDir + fileName + ".java");
		Template template = Velocity.getTemplate(templateFile);
		template.merge(context, writer);
		writer.flush();
		writer.close();
	}
	
	public static void main(String[] args){
		String inputClassName = "com/renren/newbie/service/NewbieHelperService";
		Set<DependentDescription> methods = new HashSet<DependentDescription>();
		DependentDescription d1 = new DependentDescription();
		d1.setClassName("com/renren/newbie/service/NewbieHelperService");
		d1.setMethod("getInstance ()Lcom/renren/newbie/service/NewbieHelperService; static");
		methods.add(d1);
		
		DependentDescription d2 = new DependentDescription();
		d2.setClassName("com/renren/newbie/service/NewbieHelperService");
		d2.setMethod("setStep (IJ)Z non-static");
		methods.add(d2);
//		try {
//			TemplateGenerator.generateClassFile(inputClassName, methods,"templates/singletonClass.vm");
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		String inputClassName2 = "com/renren/ugc/util/MiniGroupUtil";
		Set<DependentDescription> methods2 = new HashSet<DependentDescription>();
		DependentDescription d3 = new DependentDescription();
		d3.setClassName("com/renren/ugc/util/MiniGroupUtil");
		d3.setMethod("getHomeMenu (Lcom/xiaonei/xce/usercache/UserCache;)[Lcom/renren/ugc/model/minigroup/MiniGroup4HomeMenu; static");
		methods2.add(d3);
		try {
			TemplateGenerator.generateClassFile(inputClassName2, methods2,"templates/staticClass.vm", new HashMap());
		} catch (Exception e) {
			e.printStackTrace();
		}

	} 
}
