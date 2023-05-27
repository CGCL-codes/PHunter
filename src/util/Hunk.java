package util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Hunk {
	public String sourceFile;
	public String preChangeSet;
	public String postChangeSet;
	public String methodName;
	public List<String> codes;
	public int addLine;
	public int deleteLine;
/* 
 *  The following four variable means the 
 *  start point and length of the hunk in the source code before modification and
 *  start point and length of the hunk in the source code after the modification
 * */
	public int bs;
	public int bl;
	public int as;
	public int al;
	
	/*
	 *  -1 means delete
	 *  0  means do not modifying
	 *  1  means add
	 */
	public List<Integer> mark; 
	
	public Hunk(int bs, int bl, int as, int al, String sourceFile, String preChangeSet,
				String postChangeSet, String methodName) {
		this.bs = bs;
		this.bl = bl;
		this.as = as;
		this.al = al;
		this.methodName = methodName;
		sourceFile = sourceFile.replace("/",".");
		if (preChangeSet.contains(","))
			preChangeSet = preChangeSet.split(",")[0];
        this.sourceFile = sourceFile;
		this.preChangeSet = preChangeSet;
		this.postChangeSet = postChangeSet;
		codes = new ArrayList<>();
		mark = new ArrayList<>();
	}
	
	public void addCode(String code) {
		// Remember to remove the code mark and store the code into mark

		if (code.startsWith("-")) {
			code = code.substring(1);
			mark.add(-1);
		}
		else if (code.startsWith("+")) {
			code = code.substring(1);
			mark.add(1);
		}
		else mark.add(0);
		codes.add(code);
	}
	
	public void getAddDeleteLine() {
		addLine = 0;
		deleteLine = 0;
		for (int i : mark) {
			if (i == 1) addLine++;
			if (i == -1) deleteLine++;
		}
	}
	
	public void setCodes(List<String> codes) {
		this.codes = codes;
	}
	
	public boolean isSemantic() {
		boolean flag = false;
		for (int i = 0; i < codes.size(); i++) {
			if (mark.get(i) == 1 || mark.get(i) == -1) {
				String code = codes.get(i).trim();
				if (!code.equals("") && !code.startsWith("*") && !code.startsWith("//") && !code.startsWith("/*")) {
					flag = true;
					break;
				}
			}
		}
		return flag;
	}
	
	public String getPostLine(int index) {
//		System.out.println(mark.toString());
		int rIndex = 0;
		int count = 0;
		while (count < index) {
			if (mark.get(rIndex) != 1) count++;
			rIndex++;
		}
		if (mark.get(rIndex - 1) != 1) { 
			if (codes.get(rIndex - 1).trim().length() <= 5)
				return "";
			else return codes.get(rIndex - 1).trim();
		}
		else return "";
	}
	
	public String getPreLines(int begin, int end) {
		String contents = "";
		int count = bs;
		for (int i = 0; i < codes.size(); i++) {
			if (mark.get(i) == 1) continue;
			if (begin <= count && count <= end) {
				contents += codes.get(i).trim() + "\n";
			}
			count++;
		}
		return contents;
	}
	
	public String getPostLines(int begin, int end) {
		StringBuilder contents = new StringBuilder();
		int count = bs;
		for (int i = 0; i < codes.size(); i++) {
			if (mark.get(i) == -1) continue;
			if (begin <= count && count <= end) {
				contents.append(codes.get(i).trim()).append("\n");
			}
			count++;
		}
		return contents.toString();
	}
	
	public String getPostLines() {
		String contents = "";
		for (int i = 0; i < codes.size(); i++) {
			if (mark.get(i) == -1) continue;
			contents += codes.get(i).trim() + "\n";
		}
		return contents;
	}
	
	public boolean isValid(String line) {
		line = line.trim();
		return !line.equals("") && !line.startsWith("/*") && !line.startsWith("//") && !line.startsWith("*");
	}
	
	public HashSet<Integer> getChangedLines() {
		HashSet<Integer> lines = new HashSet<>();
		List<Integer> preMark = new ArrayList<>();
		List<String> preLine = new ArrayList<>();
		for (int i = 0; i < mark.size(); i++)
			if (mark.get(i) != 1) {
				preLine.add(codes.get(i));
				preMark.add(mark.get(i));
			}
		for (int i = this.bs; i < this.bl + this.bs; i++) {
			if (preMark.get(i - this.bs) == -1 && isValid(preLine.get(i - this.bs))) lines.add(i);
		}
		return lines;
	}
	
	public HashSet<Integer> getAllLines() {
		HashSet<Integer> lines = new HashSet<>();
		List<Integer> preMark = new ArrayList<>();
		List<String> preLine = new ArrayList<>();
		for (int i = 0; i < mark.size(); i++)
			if (mark.get(i) != 1) {
				preLine.add(codes.get(i));
				preMark.add(mark.get(i));
			}
		for (int i = this.bs; i < this.bl + this.bs; i++) {
			if (isValid(preLine.get(i - this.bs))) lines.add(i);
		}
		return lines;	
	}

	// added lines in post-patch file
	public List<Integer> getPostAffectedLines() {
		List<Integer> lines = new ArrayList<>();
		int index = as;
		for (int i = 0; i < codes.size(); i++) {
			if (mark.get(i) == -1) continue;
			if (mark.get(i) == 1) lines.add(index);
			index++;
		}
		
		return lines;
	}

	// delated lines in pre-patch file
	public List<Integer> getPreAffectedLines() {
		List<Integer> lines = new ArrayList<>();
		int index = bs;
		for (int i = 0; i < codes.size(); i++) {
			if (mark.get(i) == 1) {
//				if (!lines.contains(index))
//					lines.add(index);
				continue;
			}
			if (mark.get(i) == -1) lines.add(index);
			index++;
		}
		return lines;
	} 
	
	
	public String toString() {
		StringBuilder format = new StringBuilder(preChangeSet + "\t" + postChangeSet + "\t" + sourceFile + "\n");
		format.append("-").append(bs).append(",").append(bl).append("\t+").append(as).append(",").append(al);
		for (String code : codes) {
			format.append("\n").append(code);
		}
		return format.toString();
	}
	
	public String getIndex() {
		return preChangeSet + "@" + postChangeSet + "@" + sourceFile + "@" + bs + "@" + bl + "@" + as + "@" + al + ".txt";
	}
}

