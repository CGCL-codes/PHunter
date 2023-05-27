package util;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class FileToLines {

	public static List<String> fileToLines(String filename) {
		File file = new File(filename);
		List<String> lines = new LinkedList<String>();
		if (!file.exists()) return lines;
		String line = "";
		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			while ((line = in.readLine()) != null) {
				lines.add(line);
			}
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lines;
	}
	/**
	 * retrieve lines from a file, starting from a given line
	 * @param filename
	 * @param startLine
	 * @return
	 */
	public static List<String> fileToLines(String filename, int startLine) {
		List<String> lines = fileToLines(filename);
		return lines.subList(startLine, lines.size());
	}
	
	public static String fileToString(String filename) {
		File file = new File(filename);
		String content = "";
		String line = "";
		if (!file.exists()) return content;
		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			while ((line = in.readLine()) != null) {
				content += line + "\n";
			}
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
	}

	public static String readFile(String path)
			throws IOException
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, StandardCharsets.UTF_8);
	}

	public static String fileToStringNewLineBreak(String filename) {
		File file = new File(filename);
		String content = "";
		String line = "";
		if (!file.exists()) return content;
		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			while ((line = in.readLine()) != null) {
				content += line + "\r\n";
			}
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
	}
}
