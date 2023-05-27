package analyze;

import soot.Modifier;
import soot.SootMethod;
import soot.Unit;
import util.Commit;
import util.Hunk;
import util.Patch;

import java.util.*;
import java.util.stream.Collectors;


public class PatchSummary {
    final private BinaryAnalyzer pre;
    final private BinaryAnalyzer post;
    final private Configuration config;
    //TODO how about add a inner class?

//    public Map<ClassAttr, Set<MethodAttr>> modifiedClassesMapMethods = new HashMap<>();

    // modify a method in existing class
//    public Map<MethodAttr, Pair<List<Integer>, List<Integer>>> modifiedMethods = new HashMap<>();

    // add a method, if a new class was added, add all method from it
//    public Set<MethodAttr> addedMethods = new HashSet<>();
    /* remove a method in existing class, sometimes it can be ignored,
     * even if modify a method's modifier, we only need to focus on modified an added
     * class
     */
//    public Set<MethodAttr> removedMethods = new HashSet<>();

    public Map<ClassAttr, Set<MarkedMethod>> patchRelatedMethods = new HashMap<>();

    public int postMethodCnt = 0;
    public int preMethodCnt = 0;

    public int modifiedMethodCnt = 0;


    public PatchSummary(Configuration config, BinaryAnalyzer pre, BinaryAnalyzer post) {
        this.post = post;
        this.pre = pre;
        this.config = config;
        Commit commit = new ParsePatchFiles(config.getPatchFiles()).commit;
        resolvePostWithDiff(commit);
        computeMethodCnt();
//        selectPathPair(commit);

    }


    public void resolvePostWithDiff(Commit commit) {
        for (Patch patch : commit.patches) {
            String file = patch.postFile;
            boolean isJava = file.endsWith(".java");
            // ignore non-java or non-kotlin file
            if (!(isJava || file.endsWith(".kt")))
                continue;
                // ignore test-related file
            else if (file.contains("src/test") ||
                    file.contains("src/main/test") ||
                    file.contains("Test"))
                continue;

            // pick the corresponding clazz from post jar
            List<String> clazzList = new LinkedList<>();
            // patch.postFile like retrofit/src/main/java/retrofit2/RequestBuilder.java
            String postPatchFile = patch.postFile.replace('/', '.');
            String prePatchFile = patch.postFile.replace('/', '.');
            if(isJava){ // .java
                postPatchFile = postPatchFile.substring(0, patch.postFile.length() - 5);
                prePatchFile = prePatchFile.substring(0, patch.postFile.length() - 5);
            }else { //.kotlin
                postPatchFile = postPatchFile.substring(0, patch.postFile.length() - 3);
                prePatchFile = prePatchFile.substring(0, patch.postFile.length() - 3);
            }

            for (String name : post.allClasses.keySet()) {
                String prefixName = name.contains("$") ?
                        name.substring(0, name.indexOf('$')) : name;
                if (postPatchFile.endsWith(prefixName) && patch.isAddNewFile) {
                    Set<MarkedMethod> addedMethods = new HashSet<>();
                    patchRelatedMethods.putIfAbsent(post.allClasses.get(name), addedMethods);
                    for (MethodAttr method : post.allClasses.get(name).methods) {
                        if (method.body != null)
                            addedMethods.add(new MarkedMethod(false, method, PatchState.Added));
                    }
                } else if (prePatchFile.endsWith(prefixName) && patch.isDeletedFile) {
                    Set<MarkedMethod> deletedMethods = new HashSet<>();
                    patchRelatedMethods.putIfAbsent(pre.allClasses.get(name), deletedMethods);
                    for (MethodAttr method : pre.allClasses.get(name).methods) {
                        if (method.body != null)
                            deletedMethods.add(new MarkedMethod(true, method, PatchState.Deleted));
                    }
                } else if (prePatchFile.equals(postPatchFile) && postPatchFile.endsWith(prefixName))
                    clazzList.add(name);
            }

            if (clazzList.size() == 0)
                continue;
            List<MarkedMethod> preModifiedMethods = new ArrayList<>();
            List<MarkedMethod> postModifiedMethods = new ArrayList<>();
            for (Hunk hunk : patch.hunks) {
                resolveModifiedMethod(clazzList, hunk, preModifiedMethods, true);
                resolveModifiedMethod(clazzList, hunk, postModifiedMethods, false);
            }
            resolveAllModified(preModifiedMethods, postModifiedMethods);
            postModifiedMethods.addAll(preModifiedMethods);
            resolveAddDeleteMethod(clazzList, postModifiedMethods);
        }

    }

    private void computeMethodCnt() {
        int preModifiedCnt = 0, postModifiedCnt = 0;
        for (Set<MarkedMethod> set : patchRelatedMethods.values()) {
            Set<MarkedMethod> toBeRemoved = new HashSet<>();
            for (MarkedMethod markedMethod : set) {
                // ignore access$ method
                if (markedMethod.m.subSignature.contains("access$")
                        || isGenerateInit(markedMethod.m)
                        || isEnumClinit(markedMethod.m)
                        || isGenerateClinit(markedMethod.m)) {
                    toBeRemoved.add(markedMethod);
                    continue;
                }

                if (markedMethod.isPre) {
                    preMethodCnt++;
                    if (markedMethod.state == PatchState.Modified)
                        preModifiedCnt++;
                } else {
                    postMethodCnt++;
                    if (markedMethod.state == PatchState.Modified)
                        postModifiedCnt++;
                }
            }
            set.removeAll(toBeRemoved);
        }
        if (preModifiedCnt != postModifiedCnt) {
            System.err.println("re's and post's Modified method count should be equal, that is wired");
            System.out.println("patch files is " + config.getPatchFiles());
        }

        modifiedMethodCnt = preModifiedCnt;
    }

    private boolean isGenerateInit(MethodAttr m) {
        if (m.subSignature.contains("<init>")) {
            String clazz = m.declaredClass.name;
            SootMethod method = m.body.getMethod();
            if (method.getParameterTypes().isEmpty())
                return false;
            String lastType = method.getParameterType(method.getParameterCount() - 1).toString();
            char lastChar = lastType.charAt(lastType.length() - 1);
            return lastType.startsWith(clazz + "$") && lastChar >= '0' && lastChar <= '9';
        }
        return false;
    }


    private boolean isGenerateClinit(MethodAttr m) {
        if (m.subSignature.contains("<clinit>")) {
            String clazz = m.declaredClass.name;
            char lastChar = clazz.charAt(clazz.length() - 1);
            return clazz.contains("$") && lastChar >= '0' && lastChar <= '9';
        }
        return false;
    }

    // ignore Enum class's <clinit> because it is too large and useless
    private boolean isEnumClinit(MethodAttr m) {
        ClassAttr clazz = m.declaredClass;
        return Modifier.isEnum(clazz.modifier) && m.subSignature.equals("void <clinit>()");
    }


    private void resolveAllModified(List<MarkedMethod> preModifiedMethods, List<MarkedMethod> postModifiedMethods) {
        if (!preModifiedMethods.isEmpty()) {
            for (MarkedMethod preModifiedMethod : preModifiedMethods) {
                if (!postModifiedMethods.contains(preModifiedMethod)) {
                    addMethod(preModifiedMethod.m, false);
                }
            }
        }
        if (!postModifiedMethods.isEmpty()) {
            for (MarkedMethod postModifiedMethod : postModifiedMethods) {
                if (!preModifiedMethods.contains(postModifiedMethod)) {
                    addMethod(postModifiedMethod.m, true);
                }
            }
        }

    }

    private void addMethod(MethodAttr m, boolean isPre) {
        ClassAttr clazz = isPre ? pre.allClasses.get(m.declaredClass.name)
                : post.allClasses.get(m.declaredClass.name);
        if (clazz == null)
            return;
        for (MethodAttr target : clazz.methods) {
            if (target.signature.equals(m.signature) && target.body != null) {
                MarkedMethod markedMethod = new MarkedMethod(isPre, target,
                        PatchState.Modified, null);
                patchRelatedMethods.putIfAbsent(clazz, new HashSet<>());
                patchRelatedMethods.get(clazz).add(markedMethod);
            }
        }
    }

    public void resolveModifiedMethod(List<String> clazzList, Hunk hunk,
                                      List<MarkedMethod> modifiedMethods, boolean isPre) {
        List<Integer> patchRelatedLines;
        if (isPre)
            patchRelatedLines = hunk.getPreAffectedLines();
        else patchRelatedLines = hunk.getPostAffectedLines();

        // for pre
        if (!patchRelatedLines.isEmpty()) {
            List<MethodAttr> inList = new ArrayList<>();
            for (String name : clazzList) {
                ClassAttr ca;
                if (isPre)
                    ca = pre.allClasses.get(name);
                else ca = post.allClasses.get(name);
                if (!pre.allClasses.containsKey(name) || !post.allClasses.containsKey(name))
                    continue;
                for (MethodAttr ma : ca.methods) {
                    // not sure whether it is sound
                    for (Integer l : patchRelatedLines) {
                        if (ma.startLinenumber <= l && l <= ma.endLinenumber) {
                            if (inList.contains(ma))
                                continue;
                            inList.add(ma);
                            List<Integer> affectedLines = patchRelatedLines
                                    .stream()
                                    .filter(x -> ma.startLinenumber <= x && x <= ma.endLinenumber)
                                    .collect(Collectors.toList());
                            MarkedMethod postMarkedMethod = new MarkedMethod(isPre, ma, PatchState.Modified, affectedLines);
                            patchRelatedMethods.putIfAbsent(ca, new HashSet<>());
                            patchRelatedMethods.get(ca).add(postMarkedMethod);
                            modifiedMethods.add(postMarkedMethod);
                        }
                    }
                }
            }
        }
    }

    private void selectPathPair(MethodAttr postMethod, List<Integer> preAffectedLines, List<Integer> postAffectedLines) {
        ClassAttr preClazz = pre.allClasses.get(postMethod.declaredClass.name);
        for (MethodAttr preMethod : preClazz.methods) {
            if (preMethod.signature.equals(postMethod.signature)) {
                List<Unit> prePatchRelatedUnits = preMethod.body.getUnits()
                        .stream()
                        .filter(unit -> preAffectedLines.contains(unit.getJavaSourceStartLineNumber()))
                        .collect(Collectors.toList());
//                MethodDigest_new preMd = new MethodDigest_new(preMethod.body, prePatchRelatedUnits); // a corresponding method in pre

                List<Unit> postPatchRelatedUnits = postMethod.body.getUnits()
                        .stream()
                        .filter(unit -> postAffectedLines.contains(unit.getJavaSourceStartLineNumber()))
                        .collect(Collectors.toList());
//                MethodDigest_new postMd = new MethodDigest_new(postMethod.body, postPatchRelatedUnits); // a modified method in post

            }
        }
    }

    public void resolveAddDeleteMethod(List<String> clazzList, List<MarkedMethod> modifiedMethods) {
        for (String name : clazzList) {
            ClassAttr postClazz = post.allClasses.get(name);
            if (postClazz == null)
                continue;
            ClassAttr preClazz = pre.allClasses.get(postClazz.name);
            if (preClazz == null)
                continue;
            Set<String> preMethods = new HashSet<>();
            Set<String> postMethods = new HashSet<>();
            preClazz.methods.forEach(x -> preMethods.add(x.signature));
            for (MethodAttr postM : postClazz.methods) {
                if (!preMethods.contains(postM.getSignature())) {
                    boolean flag = false;
                    for (MarkedMethod modifiedMethod : modifiedMethods) {
                        if (modifiedMethod.m.equals(postM)) {
                            modifiedMethod.state = PatchState.Added;
                            flag = true;
                        }
                    }
                    if (flag)
                        continue;
                    if (postM.body != null) {
                        patchRelatedMethods.putIfAbsent(postClazz, new HashSet<>());
                        patchRelatedMethods.get(postClazz).add(new MarkedMethod(false, postM, PatchState.Added));
                    }
                }
                postMethods.add(postM.getSignature());
            }
            for (MethodAttr preM : preClazz.methods) {
                if (!postMethods.contains(preM.getSignature())) {
                    boolean flag = false;
                    for (MarkedMethod modifiedMethod : modifiedMethods) {
                        if (modifiedMethod.m.equals(preM)) {
                            modifiedMethod.state = PatchState.Deleted;
                            flag = true;
                        }
                    }
                    if (flag)
                        continue;
                    if (preM.body != null) {
                        patchRelatedMethods.putIfAbsent(preClazz, new HashSet<>());
                        patchRelatedMethods.get(preClazz).add(new MarkedMethod(true, preM, PatchState.Deleted));
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<ClassAttr, Set<MarkedMethod>> entry : patchRelatedMethods.entrySet()) {
            sb.append(entry.getKey().name);
            sb.append(":\n");
            for (MarkedMethod markedMethod : entry.getValue()) {
                switch (markedMethod.state) {
                    case Added:
                        sb.append("added_method#");
                        sb.append(markedMethod.m.declaredClass.name);
                        sb.append("->");
                        sb.append(markedMethod.m.getSignature());
                        sb.append("\n");
                        break;
                    case Modified:
                        if (markedMethod.isPre)
                            continue;
                        sb.append("modified_method#");
                        sb.append(markedMethod.m.declaredClass.name);
                        sb.append("->");
                        sb.append(markedMethod.m.getSignature());
                        sb.append("\n");
                        break;
                    case Deleted:
                        sb.append("remove_method#");
                        sb.append(markedMethod.m.declaredClass.name);
                        sb.append("->");
                        sb.append(markedMethod.m.getSignature());
                        sb.append("\n");
                        break;
                }
            }
        }
        return sb.toString();
    }

    public String getCVENumber() {
        return config.getPatchFiles().split(";")[0].split(";")[0];
    }
}


