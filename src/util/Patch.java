package util;


import java.util.ArrayList;
import java.util.List;

public class Patch {
    public boolean isAddNewFile = false;
    public boolean isDeletedFile = false;
    public int id;
    public int bid;
    public String diff;
    public String preFile;
    public String postFile;
    public String preChangeSet;
    public String postChangeSet;
    public List<String> content;
    public List<Hunk> hunks;


    public Patch(int id, int bid, String diff, String preFile, String postFile) {
        this.id = id;
        this.bid = bid;
        this.diff = diff;
        this.preFile = preFile;
        this.postFile = postFile;
        content = new ArrayList<>();
        hunks = new ArrayList<>();
        parseDiff();
    }

    // This constructor is for patches extracted from commid log, thus without a patch id
    public Patch(String diff, String preFile, String postFile) {
        this.id = 0;
        this.bid = 0;
        this.diff = diff;
        // idea style
        if (preFile.contains("revision"))
            preFile = preFile.substring(0, preFile.indexOf("\t"));
        if (postFile.contains("revision"))
            postFile = postFile.substring(0, postFile.indexOf("\t"));
        this.preFile = preFile;
        this.postFile = postFile;
        if (preFile.contains("/dev/null"))
            isAddNewFile = true;
        else if (!preFile.equals(postFile)) {
            System.out.printf("preFile not equal with postFile, is this renaming?\n" +
                    "pre=%s\t post=%s%n", preFile, postFile);
            isAddNewFile = isDeletedFile = true;
        }
        content = new ArrayList<>();
        hunks = new ArrayList<>();
        parseDiff();
    }

    private void parseDiff() {
        String[] tmp = diff.split(" ");
        if (tmp.length == 6) {
            preChangeSet = tmp[2];
            postChangeSet = tmp[4];
//            System.out.println("Parse Error !!! " + diff);
//            return;
        } else if (tmp.length == 4) {
            preChangeSet = tmp[2];
            postChangeSet = tmp[3];
        }
    }


    public void addContent(List<String> content) {
        this.content.addAll(content);
    }

    public void extractHunks() {
        if (hunks.size() == 0) {
            Hunk hunk = null;
            for (String s : content) {
                //System.out.println(content.get(i));
                if (s.startsWith("@@")) {
                    if (hunk != null) hunks.add(hunk);
                    String[] tmp = s.split(" ");
                    if (!tmp[1].contains(",") || !tmp[2].contains(",")) continue;
//					System.out.println(content.get(i));
                    int bs = Integer.parseInt(tmp[1].substring(1, tmp[1].indexOf(",")));
                    int bl = Integer.parseInt(tmp[1].substring(tmp[1].indexOf(",") + 1));
                    int as = Integer.parseInt(tmp[2].substring(1, tmp[2].indexOf(",")));
                    int al = Integer.parseInt(tmp[2].substring(tmp[2].indexOf(",") + 1));
                    String methodName = null;
                    if (ExistMethodDef(s))
                        methodName = resolveMethodName(s);
                    hunk = new Hunk(bs, bl, as, al, postFile.trim(), preChangeSet, postChangeSet, methodName);
                } else {
                    if (hunk != null)
                        hunk.addCode(s);
                }
            }
            if (hunk != null)
                hunks.add(hunk);
        }
    }

    public boolean ExistMethodDef(String line) {
        if (line.contains("(")) {
            return line.contains("public") || line.contains("private")
                    || line.contains("void") || line.contains("protected");
        }
        return false;
    }

    public String resolveMethodName(String line) {
        int leftParenthesis = line.indexOf("(");
        int blank = line.substring(0, leftParenthesis).lastIndexOf(" ") + 1;
        return line.substring(blank, leftParenthesis);
    }


    public String getContent() {
        StringBuilder ans = new StringBuilder();
        for (String s : content) ans.append(s).append("\n");
        return ans.toString();
    }
}
