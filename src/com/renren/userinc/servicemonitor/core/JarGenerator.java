package com.renren.userinc.servicemonitor.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JarGenerator {
	private static JarGenerator jarGenerator = new JarGenerator();
	
	private String classPath;

	private JarGenerator() {
		StringBuilder sb = new StringBuilder();
		try{
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(Constants.TARGET_CLASSPATH_FILENAME)));
			String jarPath = null;
			// String JAR_FILTER = ".*(/com/.*(xiaonei|renren|xce/).*\\.jar).*";
			Pattern p = Pattern.compile(Constants.JAR_SCOPE);
			while ((jarPath = reader.readLine()) != null) {
				Matcher m = p.matcher(jarPath);
				// 抓取相关的jar名字
				if(m.find() && m.groupCount() > 0){
					sb.append(Constants.M2_REPO + m.group(1)).append(":");
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		// 增加asm.jar和cglib.jar, 在ServiceMonitor工具的lib下面
		sb.append(Constants.SERVICE_MONITOR_TOOL_DIR + "/lib/cglib.jar").append(":");
		sb.append(Constants.SERVICE_MONITOR_TOOL_DIR + "/lib/asm.jar");
		
		classPath = sb.toString();
	}

	public static JarGenerator getInstance() {
		return jarGenerator;
	}

	public void compileAndPackage() {
		try {
			File generatedFileDir = new File(Constants.GENERATED_FILE_DIR);
			if(!generatedFileDir.exists()){
				generatedFileDir.mkdir();
			}
			
			StringBuilder sb = new StringBuilder();
			String shellFile = Constants.COMPILE_AND_PACKAGE_GENERATED_FILES_SCRIPT;
			String systemGeneratedJavaFiles = "com/renren/userinc/servicemonitor/system/generated/*.java";
			String generatedJavaFiles = "com/renren/userinc/servicemonitor/generated/*.java";
			sb.append(systemGeneratedJavaFiles).append(" ").append(generatedJavaFiles);
			
			String duplicateGeneratedJavaPackage = "com/renren/userinc/servicemonitor/duplicate/generated";
			String duplicateGeneratedJavaDir = Constants.GENERATED_FILE_DIR + "/" + duplicateGeneratedJavaPackage;
			File f = new File(duplicateGeneratedJavaDir);
			if(f.exists()){
				if(f.list().length > 0){
					sb.append(" ").append(duplicateGeneratedJavaPackage + "/*.java");
				}
			}
			
			String manualGeneratedJavaPackage = "com/renren/userinc/servicemonitor/manual/generated";
			String manualGeneratedJavaDir = Constants.GENERATED_FILE_DIR + "/" + manualGeneratedJavaPackage;
			f = new File(manualGeneratedJavaDir);
			if(f.exists()){
				if(f.list().length > 0){
					sb.append(" ").append(manualGeneratedJavaPackage + "/*.java");
				}
			}
			ProcessBuilder pb = new ProcessBuilder("sh", shellFile, Constants.GENERATED_FILE_DIR, classPath, 
					sb.toString(), Constants.GENERATED_JAR_NAME, Constants.TARGET_WORK_SPACE);
			pb.directory(generatedFileDir);
			pb.redirectErrorStream(true);

			Process ps = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
			String str = null;
			while((str = br.readLine()) != null){
				System.out.println(str);
			}
			System.out.println("在目录" + Constants.GENERATED_FILE_DIR + "下生成" + Constants.GENERATED_JAR_NAME);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		JarGenerator.getInstance().compileAndPackage();
	}
}
