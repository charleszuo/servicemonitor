package com.renren.userinc.servicemonitor.util;

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

import com.renren.userinc.servicemonitor.bean.DependentDescriptor;
import com.renren.userinc.servicemonitor.core.Constants;

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
								Constants.TARGET_CLASSES_PATH.length(),
								fs[i].getAbsolutePath().length() - 6), null);
			}
		}
	}

	private static String readAndUpdateContent(String filePath, Map<String, DependentDescriptor> outerDependentMap) {
		BufferedReader reader = null;
		String line = null;
		StringBuilder buf = new StringBuilder();
		List<DependentDescriptor> outerDependentList = new ArrayList<DependentDescriptor>();
		outerDependentList.addAll(outerDependentMap.values());
		
		List<Pattern> patterns = new ArrayList<Pattern>();
		for(int i=0; i<outerDependentList.size(); i++){
			String importedClassName = outerDependentList.get(i).getClassName().replace(Constants.SEPARATOR_SLASH, Constants.SEPARATOR_POINT);
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

	public Map<String, String> getAllLocalClasses(String directory) {
		if (allLocalClasses.size() == 0) {
			findAllLocalClasses(directory);
		}

		return allLocalClasses;
	}
	
	public static boolean updateFile(String filePath, Map<String, DependentDescriptor> outerMethods) {
		if(!isValidFilePath(filePath)){
			return false;
		}
		String fileContent = readAndUpdateContent(filePath, outerMethods);
		writeFile(filePath, fileContent);
		return true;
	}
	
	public static String checkDir(String dir){
		File f = new File(dir);
		if(!f.exists()){
			f.mkdirs();
		}
		if(!dir.endsWith("/")){
			return dir + "/";
		}else{
			return dir;
		}
	}

	private static class ClassFileFilter implements FileFilter {

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
