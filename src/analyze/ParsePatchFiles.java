package analyze;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.Commit;
import util.FileToLines;
import util.Patch;

import java.util.ArrayList;
import java.util.List;

public class ParsePatchFiles {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    public Commit commit = new Commit();


    public ParsePatchFiles(String patchPath) {
        for (String path : patchPath.split(";")) {
            logger.info(String.format("Analyzing the patch %s", path));

            readOneCommitWithHunkGit(path);
        }
        commit.extractHunks();
    }


    public void readOneCommitWithHunkGit(String file) {
        readOneCommitWithHunkGitContent(FileToLines.fileToLines(file));
    }

    public void readOneCommitWithHunkGitContent(List<String> rawCommit) {

        //Variables for commit
        String line;
        // Variables for patch
        String preFile = null;
        String postFile = null;
        String command = null;
        Patch patch = null;
        List<String> content = new ArrayList<>();
        int size = rawCommit.size();
        if (size == 0) return;
        int index = 0;
        try {
            boolean isContent = false;
            line = rawCommit.get(index++);

            while (line != null) {
                if (line.startsWith("diff")) {
                    if (patch != null) {
                        patch.addContent(content);
                        commit.addPatch(patch);
                        content.clear();
                        isContent = false;
                    }
                    command = line;
                } else if (line.startsWith("---")) {
                    try {
                        if (line.contains("/dev/null"))
                            preFile = line;
                        else
                            preFile = line.substring(6);
                    } catch (IndexOutOfBoundsException e) {
                        System.out.println("Exception\t" + line);
                    }
                } else if (line.startsWith("+++")) {
                    try {
                        postFile = line.substring(6);
                    } catch (IndexOutOfBoundsException e) {
                        System.out.println("Exception\t" + line);
                    }
                    patch = new Patch(command, preFile, postFile);
//					System.out.println(patch);
                    isContent = true;
                } else if (line.startsWith("index")) {
                    String[] tmp = line.split(" ");
                    String[] tmp2 = tmp[1].split("\\.\\.");
                    String preIndex = tmp2[0];
                    String postIndex = tmp2[1];
                    tmp = command.split(" ");
                    command = "diff -r " + preIndex + " -r " + postIndex + " " + tmp[2];
                } else {
                    if (isContent) content.add(line);
                }
                if (index >= rawCommit.size()) break;
                line = rawCommit.get(index++);
//				System.out.println(size + "\t" + index);
            }
            if (command != null) {
                patch = new Patch(command, preFile, postFile);
                patch.addContent(content);
                commit.addPatch(patch);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String patchPath = "samples/CVE-2018-1000850_b9a7f6ad72073ddd40254c0058710e87a073047d.diff";
        new ParsePatchFiles(patchPath);
    }
}

