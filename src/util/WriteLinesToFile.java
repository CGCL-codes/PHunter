package util;


import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class WriteLinesToFile {
	public static void writeLinesToFile(List<String> lines, String file){
		try {
			FileWriter writer = new FileWriter(new File(file));
			for(String line:lines) {
				writer.write(line+ "\n");
			}
			writer.close();
		} catch(Exception e) {
			System.err.println("error happens when writing lines to file "+ file);
			e.printStackTrace();
		}
	}
	
	public static void appendLinesToFile(List<String> lines, String file){
		try {
			FileWriter writer = new FileWriter(new File(file), true);
			for(String line:lines) {
				writer.write(line+ "\n");
			}
			writer.close();
		} catch(Exception e) {
			System.err.println("error happens when writing lines to file "+ file);
			e.printStackTrace();
		}
	}
	
	public static void writeToFiles(String content, String file){
		try {
			FileWriter writer = new FileWriter(new File(file));
			writer.write(content);
			writer.close();
		} catch(Exception e) {
			System.err.println("error happens when writing lines to file "+ file);
			e.printStackTrace();
		}
	}
	
}
