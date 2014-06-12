package com.renren.seo.monitor.template;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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
	static {
		Properties p = new Properties();
		try {
			p.load(TemplateGenerator.class
					.getResourceAsStream("/properties/velocity.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		Velocity.init(p);
	}

	private static final String generatedFileDir = ConstantName.TARGET_WORK_SPACE
			+ "/src/main/java/com/renren/seo/serviceproxy/generated/";
	private static final String generatedDuplicateFileDir = ConstantName.TARGET_WORK_SPACE
			+ "/src/main/java/com/renren/seo/serviceproxy/duplicate/generated/";
	private static final String generatedMannualFileDir = ConstantName.TARGET_WORK_SPACE
			+ "/src/main/java/com/renren/seo/serviceproxy/manual/generated/";
	private static final String defaultPackageName = "com.renren.seo.serviceproxy.generated";
	private static final String manualPackageName = "com.renren.seo.serviceproxy.manual.generated";
	private static final String duplicatePackageName = "com.renren.seo.serviceproxy.duplicate.generated";

	private static void checkDirectories() {
		File dir = new File(generatedFileDir);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		dir = new File(generatedDuplicateFileDir);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		dir = new File(generatedMannualFileDir);
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}

	public static void generateClassFile(String inputClassName,
			Set<DependentDescription> methodDescriptionSet,
			Set<String> fieldDescriptionSet, String templateFile,
			Map<String, String> generatedFileMap) {
		try {
			checkDirectories();

			VelocityContext context = new VelocityContext();
			String targetClassName = inputClassName.replace(ConstantName.SLASH,
					ConstantName.POINT);
			String className = targetClassName.substring(targetClassName
					.lastIndexOf(".") + 1);
			context.put("ClassName", className);
			context.put("TargetClassName", targetClassName);
			String dir = null;
			String packageName = null;
			// 类名有重复,目前只处理最多重复一次
			if (!generatedFileMap.containsKey(className)) {
				dir = generatedFileDir;
				packageName = defaultPackageName;
				generatedFileMap.put(className, null);
			} else {
				dir = generatedDuplicateFileDir;
				packageName = duplicatePackageName;
			}
			context.put("PackageName", packageName);
			List<String> methods = new ArrayList<String>();
			if (methodDescriptionSet != null) {
				for (Iterator<DependentDescription> it = methodDescriptionSet
						.iterator(); it.hasNext();) {
					DependentDescription dependent = it.next();
					String classMethodDescription = dependent
							.toStringWithException();
					// singleton不处理getInstance方法
					if (classMethodDescription
							.contains(ConstantName.GET_INSTANCE) && ConstantName.TEMPLATE_SINGLETON_CLASS.equals(templateFile)) {
						continue;
					}
					dependent.setGeneratedProxyClassName(packageName + "."
							+ className);
					// 需要用模式来重构
					String method = MethodParser.parse(classMethodDescription,
							dependent.getMethodType(), dependent.isVarArgs());
					methods.add(method);
				}
			}
			context.put("Methods", methods);

			List<String> fields = new ArrayList<String>();
			if (fieldDescriptionSet != null) {
				for (Iterator<String> it = fieldDescriptionSet.iterator(); it
						.hasNext();) {
					String fieldDependent = it.next();

					String field = FieldParser.parse(targetClassName,
							fieldDependent);
					fields.add(field);
				}
			}

			context.put("Fields", fields);

			FileWriter writer = new FileWriter(dir + className + ".java");
			Template template = Velocity.getTemplate(templateFile);
			template.merge(context, writer);
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void generateManalFile(String className,
			Set<DependentDescription> methodDescriptionSet,
			String templateFile, boolean isManualPackage) {
		try {
			checkDirectories();
			
			VelocityContext context = new VelocityContext();
			String dir = isManualPackage ? generatedMannualFileDir
					: generatedFileDir;
			String packageName = isManualPackage ? manualPackageName
					: defaultPackageName;
			FileWriter writer = new FileWriter(dir + className + ".java");
			if (methodDescriptionSet != null) {
				for (Iterator<DependentDescription> it = methodDescriptionSet
						.iterator(); it.hasNext();) {
					DependentDescription dependent = it.next();
					dependent.setGeneratedProxyClassName(packageName + "."
							+ className);
				}
			}
			Template template = Velocity.getTemplate(templateFile);
			template.merge(context, writer);
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
