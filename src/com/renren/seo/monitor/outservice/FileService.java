package com.renren.seo.monitor.outservice;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.renren.seo.monitor.outservice.obj.DependentDescription;

public class FileService {

	private static FileService instance = new FileService();

	private FileService() {
	};

	public static FileService getInstance() {
		return instance;
	}

	private Map<String, String> allLocalClasses = new HashMap<String, String>();

	private ClassFileFilter classFileFilter = new ClassFileFilter();

	private void findAllLocalClasses(String directory) {
		File dir = new File(directory);
		File[] fs = dir.listFiles(classFileFilter);
		for (int i = 0; i < fs.length; i++) {
			if (fs[i].isDirectory()) {
				findAllLocalClasses(fs[i].getAbsolutePath());
			} else {
				allLocalClasses.put(
						fs[i].getAbsolutePath().substring(
								ConstantName.TARGET_CLASSES_PATH.length(),
								fs[i].getAbsolutePath().length() - 6), null);
			}
		}
	}

	public Map<String, String> getAllLocalClasses(String directory) {
		if (allLocalClasses.size() == 0) {
			findAllLocalClasses(directory);
		}

		return allLocalClasses;
	}

	private static String readAndUpdateContent(String filePath, Map<String, DependentDescription> outerDependentMap) {
		BufferedReader reader = null;
		String line = null;
		StringBuilder buf = new StringBuilder();
		List<DependentDescription> outerDependentList = new ArrayList<DependentDescription>();
		outerDependentList.addAll(outerDependentMap.values());
		
		List<Pattern> patterns = new ArrayList<Pattern>();
		for(int i=0; i<outerDependentList.size(); i++){
			String importedClassName = outerDependentList.get(i).getClassName().replace(ConstantName.SLASH, ConstantName.POINT);
			// 捕获分组,用来在后面替换文件中依赖的类名, 有两种情况,一种是在import里面的类,一种是在正文里面通过完整类名引用的代码
			String pattern = ".*(" + importedClassName + ").*";
			patterns.add(Pattern.compile(pattern));
		}

		try {
			reader = new BufferedReader(new FileReader(filePath));
			while ((line = reader.readLine()) != null) {
				boolean isMatched = false;
				for(int i=0; i<patterns.size(); i++){
					Matcher m = patterns.get(i).matcher(line);
					if (m.matches() && m.groupCount() > 0) {
						String newLine = line.replace(m.group(1), outerDependentList.get(i).getGeneratedProxyClassName());
						buf.append(newLine);
						isMatched =true;
						break;
					}
				}
				
				// 如果不用修改, 则按原来的内容回写
				if(! isMatched) {
					buf.append(line);
				}
				buf.append(System.getProperty("line.separator"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					reader = null;
				}
			}
		}
		return buf.toString();
	}

	private static void writeFile(String filePath, String content) {  
		BufferedWriter writer = null;  
          
        try {  
        	writer = new BufferedWriter(new FileWriter(filePath));  
        	writer.write(content);  
        } catch (Exception e) {  
            e.printStackTrace();  
        }finally{
        	if (writer != null) {  
                try {  
                	writer.close();  
                } catch (IOException e1) {  
                	writer = null;  
                }  
            } 
        }
    }  
	
	private static boolean isValidFilePath(String filePath){
		File f = new File(filePath);
		if(!f.exists()){
			return false;
		}
		return true;
	}

	public static boolean updateFile(String filePath, Map<String, DependentDescription> outerMethods) {
		if(!isValidFilePath(filePath)){
			return false;
		}
		String fileContent = readAndUpdateContent(filePath, outerMethods);
		writeFile(filePath, fileContent);
		return true;
	}

	public static void main(String[] args) {
//		FileService fileService = new FileService();
//		Map<String, String> allLocalClasses = fileService
//				.getAllLocalClasses(ConstantName.TARGET_CLASSES_PATH);
//		for (String className : allLocalClasses.keySet()) {
//			System.out.println(className);
//		}
		System.out.println(isValidFilePath(null));
//		String importedClassName = "com.xiaonei.sns.platform.core.opt.ice.impl.SnsAdapterFactory";
//		String generatedClassName = "com.renren.seo.serviceproxy.generated.SnsAdapterFactory";
//		String pattern = ".*(" + importedClassName + ").*";
//		Pattern p = Pattern.compile(pattern);
//		String line = "import com.xiaonei.sns.platform.core.opt.ice.impl.SnsAdapterFactory;";
//		Matcher m = p.matcher(line);
//		if(m.matches()){
//			System.out.println(m.groupCount());
//			System.out.println(m.group(1));
//			System.out.println(line.replace(m.group(1), generatedClassName));
//		}
//		
//		String filePath = "/home/charles/workspace_renren/xiaonei-guide/src/main/java/com/xiaonei/reg/guide/util/GuideUtil.java";
//		Map<String, DependentDescription> outerDependentMap = new HashMap<String, DependentDescription>();
//		DependentDescription d1 = new DependentDescription();
//		d1.setClassName("com/xiaonei/sns/platform/core/opt/ice/impl/SnsAdapterFactory");
//		d1.setGeneratedProxyClassName("com.renren.seo.serviceproxy.generated.SnsAdapterFactory");
//		outerDependentMap.put("com/xiaonei/sns/platform/core/opt/ice/impl/SnsAdapterFactory", d1);
//		DependentDescription d2 = new DependentDescription();
//		d2.setClassName("com.xiaonei.platform.component.friends.home.FriendsHome");
//		d2.setGeneratedProxyClassName("com.renren.seo.serviceproxy.generated.FriendsHome");
//		outerDependentMap.put("com.xiaonei.platform.component.friends.home.FriendsHome", d2);
//		
//		System.out.println(readAndUpdateContent(filePath, outerDependentMap));
	}

	static class ClassFileFilter implements FileFilter {

		@Override
		public boolean accept(File file) {
			if (file.isDirectory())
				return true;
			else {
				String name = file.getName();
				if (name.matches(".*\\.class$")) {
					return true;
				} else {
					return false;
				}
			}

		}

	}
}
