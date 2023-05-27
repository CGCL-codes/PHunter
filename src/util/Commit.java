package util;

import java.util.ArrayList;
import java.util.List;

public class Commit {

	public String description;
	public boolean isFiltered;
	public List<String> files;
	public List<Patch> patches;
	public List<Hunk> hunks;

	public Commit() {
		patches = new ArrayList<Patch>();
		files = new ArrayList<String>();
		isFiltered = false;
	}
	
	public void addFile(String file) {
		files.add(file);
//		System.out.println(files.size());
	}
	


	private boolean isValid(String type) {
//		for (int i = 0; i < validType.length; i++) 
//			if (type.equals(validType[i])) return true;
//		System.out.println(type); 
//		System.out.println(type.equals(".java"));
		return type.equals(".java");
	}

	/**
	 * 
	 * @return return related source files
	 */
	
	public List<String> getSourceFiles() {
		if (!isFiltered) {
			filterSourceFile();
			isFiltered = true;
		}
		return files;
	}
	
	
	/**
	 * This function is used to filter out source code related to testing and those invalid file types  
	 * 
	 */
	public void filterSourceFile() {
//		System.out.println(files.size());
		List<String> filteredFiles = new ArrayList<String>();
		for (String file : files) {
//			if (!files.get(i).contains("test")) {
			if (file != null && file.contains(".") && file.lastIndexOf(".") > 0) {
				String type = file.substring(file.lastIndexOf("."));
//					System.out.println(type);
				if (isValid(type))
					filteredFiles.add(file);
			}
//			}
		}
		files.clear();
		files = filteredFiles;
	}
	
	public void addPatch(Patch patch) {
		patches.add(patch);
		files.add(patch.preFile);
	}
	
	public void extractHunks() {
		for (Patch patch : patches) {
			patch.extractHunks();
		}
		
	}
	
	public List<Hunk> getAllHunks() {
		if (hunks == null) {
			extractHunks();
			hunks = new ArrayList<Hunk>();
			for (Patch patch : patches) {
				hunks.addAll(patch.hunks);
			}
		}
		return hunks;
	}
	
	public List<String> getFileTypes() {
		List<String> types = new ArrayList<String>();
		for (String file : files)
			if (file.lastIndexOf(".") > 0) {
				String type = file.substring(file.lastIndexOf("."));
				if (isValid(type))
					types.add(type);
			}
		return types;
	}
	


}
