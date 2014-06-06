package com.renren.seo.monitor.outservice;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;

public class FileService {
	
	private static FileService instance = new FileService();
	
	private FileService(){};
	
	public static FileService getInstance(){
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
			}else {
				allLocalClasses.put(fs[i].getAbsolutePath().substring(ConstantName.TARGET_CLASSES_PATH.length(), fs[i].getAbsolutePath().length() - 6), null);
			}
		}
	}
	
	public Map<String, String> getAllLocalClasses(String directory) {
		if(allLocalClasses.size() == 0){
			findAllLocalClasses(directory);
		}
		
		return allLocalClasses;
	}

	public static void main(String[] args) {
		FileService fileService = new FileService();
		Map<String, String> allLocalClasses = fileService.getAllLocalClasses(ConstantName.TARGET_CLASSES_PATH);
		for(String className: allLocalClasses.keySet()){
			System.out.println(className);
		}
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
