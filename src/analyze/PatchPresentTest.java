package analyze;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import similarity.CombinationSelect;
import similarity.SimilarityUtil;
import soot.ArrayType;
import soot.RefType;
import soot.SootMethod;
import soot.Type;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.ThrowStmt;
import symbolicExec.MethodDigest_new2;
import treeEditDistance.costmodel.PredicateCostModel_back;
import treeEditDistance.distance.APTED;
import treeEditDistance.node.Node;
import treeEditDistance.node.PredicateNodeData;

import java.util.*;

import static similarity.HungarianAlgorithm.hgAlgorithm;
import static similarity.SimilarityUtil.*;

public class PatchPresentTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    final Configuration config;
    Map<MarkedMethod, Set<MarkedMethod>> candidateMatchedMethods = new HashMap<>();
    Map<MethodAttr, MarkedMethod> candidateInitMap = new HashMap<>(); // for type recover

    Map<MarkedMethod, MarkedMethod> preOptimalMatchingMap = new HashMap<>();
    Map<MarkedMethod, MarkedMethod> postOptimalMatchingMap = new HashMap<>();


    float preSimilarity = 0;
    float postSimilarity = 0;

    public PatchPresentTest(Configuration config, PatchSummary summary, APKAnalyzer apk) {
        this.config = config;
        if (startMatchMethod(summary, apk)) {
            doSimilarityCompute(summary);
            computeResult(summary, apk);
        }
    }


    public boolean startMatchMethod(PatchSummary summary, APKAnalyzer apk) {
        for (ClassAttr appClass : apk.allClasses.values()) {
            for (Map.Entry<ClassAttr, Set<MarkedMethod>> classMethodEntry : summary.patchRelatedMethods.entrySet()) {
                Set<MarkedMethod> anchorMethods = classMethodEntry.getValue();
                matchedMethods(anchorMethods, appClass);
            }
        }
        if (candidateMatchedMethods.isEmpty()) {
            logger.info(String.format("no matched patch-related method in %s, " +
                    "the target method may be deleted by obfuscator", apk.getAPKName()));
            return false;
        }
        return true;
    }

    private void matchedMethods(
            Set<MarkedMethod> anchorMethods,
            ClassAttr appClass) {
        boolean containClinit = false; // exist <clinit>
        boolean containInit = false; // exist <init>
        Map<MarkedMethod, List<MarkedMethod>> tmpMap = new HashMap<>();

        // TODO 如果某个类只改了cinit，有匹配很多很多的类
        for (MarkedMethod anchorMethod : anchorMethods) {
//            if (anchorMethod.isPre && anchorMethod.state == PatchState.Modified)
//                continue;
            for (MethodAttr appMethod : appClass.methods) {
                boolean isAppMethodClinit = appMethod.subSignature.equals("void <clinit>()");
                boolean isAppMethodInit = appMethod.subSignature.contains("<init>");

                if (anchorMethod.m.subSignature.equals("void <clinit>()")
                        != isAppMethodClinit
                        || anchorMethod.m.subSignature.contains("<init>")
                        != isAppMethodInit)
                    continue;
                double sim = SimilarityUtil.matchedMethod(anchorMethod.m, appMethod);
                if (sim > methodSimilarityThreshold) {
                    //TODO check a match <init> is a Sufficient conditions
                    if (!anchorMethod.m.subSignature.contains("<init>")) {
                        Map<MethodAttr, MarkedMethod> pairs = checkConstructMatcher(anchorMethod, appClass);
                        if (!pairs.isEmpty())
                            candidateInitMap.putAll(pairs);
                    }
                    tmpMap.putIfAbsent(anchorMethod, new LinkedList<>());
                    tmpMap.get(anchorMethod).add(new MarkedMethod(sim, appMethod));
                    if (isAppMethodClinit)
                        containClinit = true;
                    if (isAppMethodInit)
                        containInit = true;
                }
            }
        }
        if (((containClinit || containInit) && anchorMethods.size() != 1 && tmpMap.keySet().size() == 1)
                || (containClinit && containInit && anchorMethods.size() != 2 && tmpMap.keySet().size() == 2)) {
            tmpMap = null; // no method in an app class match the target
            return;
        }
//        candidateMatchedMethods.putAll(tmpMap);
        for (Map.Entry<MarkedMethod, List<MarkedMethod>> entry : tmpMap.entrySet()) {
            candidateMatchedMethods.putIfAbsent(entry.getKey(), new HashSet<>());
            candidateMatchedMethods.get(entry.getKey()).addAll(entry.getValue());
        }
    }

    private static Map<MethodAttr, MarkedMethod> checkConstructMatcher(MarkedMethod tpl, ClassAttr apk) {
        Map<MethodAttr, MarkedMethod> pairs = new HashMap<>();
        for (MethodAttr tplInit : tpl.m.declaredClass.methods) {
            if (!tplInit.subSignature.contains("<init>"))
                continue;
            for (MethodAttr apkInit : apk.methods) {
                if (apkInit.subSignature.contains("<init>")) {
                    double sim = matchedInitMethod(tplInit, apkInit);
                    if (sim > initMethodSimilarityThreshold) {
                        if (pairs.containsKey(tplInit)) {
                            MarkedMethod matchedApkInit = pairs.get(tplInit);
                            if (matchedApkInit.sim < sim)
                                pairs.put(tplInit, new MarkedMethod(sim, apkInit));
                        } else
                            pairs.put(tplInit, new MarkedMethod(sim, apkInit));
                        break;
                    }
                }
            }
        }
        return pairs;
    }

    /*
    compute method similarity
     */
    private void doSimilarityCompute(PatchSummary summary) {
        for (Set<MarkedMethod> patchRelatedMethods : summary.patchRelatedMethods.values()) {
            for (MarkedMethod patchRelatedMethod : patchRelatedMethods) {
                System.out.println("1 "+patchRelatedMethod.m.signature);
                MethodDigest_new2 tpl = new MethodDigest_new2(patchRelatedMethod.m.body,
                        patchRelatedMethod.patchRelatedLines);
                Set<MarkedMethod> appMethods = candidateMatchedMethods.get(patchRelatedMethod);
                if (appMethods == null)
                    continue;
                float max = -1;
                MarkedMethod optimalPair = null;
                for (MarkedMethod appMethod : appMethods) {
                    System.out.println("2 "+appMethod.m.signature);
                    MethodDigest_new2 app = new MethodDigest_new2(appMethod.m.body, null);
                    if (app.realUnitsPaths.isEmpty()) {
                        System.err.println(appMethod.m.signature + " has no paths, that is wired");
                        continue;
                    }

                    Map<String, String> typeRecoveryMap = typeRecovery(patchRelatedMethod, appMethod);
                    // compute the similarity with fine-grain value
                    float sim = MethodSimilarityCompute(tpl, app, typeRecoveryMap);
                    if (patchRelatedMethod.isPre) {
                        appMethod.preFineGrainSimilarity = sim;
                    } else appMethod.postFineGrainSimilarity = sim;
                    if (sim > max) {
                        max = sim;
                        optimalPair = appMethod;
                    }
                }
                if (patchRelatedMethod.isPre)
                    preOptimalMatchingMap.put(patchRelatedMethod, optimalPair);
                else postOptimalMatchingMap.put(patchRelatedMethod, optimalPair);
            }
        }
    }


    private void computeResult(PatchSummary summary,
                               APKAnalyzer apk) {
        // now only consider Modified method
        // for pre
        for (Map.Entry<MarkedMethod, MarkedMethod> entry : preOptimalMatchingMap.entrySet()) {
            MarkedMethod anchorMethod = entry.getKey();
            MarkedMethod appMethod = entry.getValue();
            if (anchorMethod.state == PatchState.Modified) {
                preSimilarity += appMethod.preFineGrainSimilarity;
            }
            if (config.enableDebugLevel)
                logger.info(String.format("the pre optimal pair is \n%s \t %s", anchorMethod, appMethod.toString()));
        }
        if (summary.preMethodCnt != 0)
            preSimilarity /= summary.modifiedMethodCnt;

        // for post
        for (Map.Entry<MarkedMethod, MarkedMethod> entry : postOptimalMatchingMap.entrySet()) {
            MarkedMethod anchorMethod = entry.getKey();
            MarkedMethod appMethod = entry.getValue();
            if (anchorMethod.state == PatchState.Modified) {
                postSimilarity += appMethod.postFineGrainSimilarity;
            }
            if (config.enableDebugLevel)
                logger.info(String.format("the post optimal pair is \n%s \t %s", anchorMethod, appMethod.toString()));
        }
        if (summary.postMethodCnt != 0)
            postSimilarity /= summary.modifiedMethodCnt;

        if (preSimilarity < finalMethodSimilarityThreshold && postSimilarity < finalMethodSimilarityThreshold) {
            logger.info(String.format("all candidate patch-related method similarity are below the threshold ," +
                            "the patch is not present, may be the target method is deleted by the obfuscator \n" +
                            "pre similarity=%f\tpost similarity=%f",
                    preSimilarity, postSimilarity));
            return;
        }
        if (postSimilarity > preSimilarity) {
            logger.info(String.format("the patch IS PRESENT, " +
                            "pre similarity=%f\tpost similarity=%f",
                    preSimilarity, postSimilarity));
        } else if (postSimilarity < preSimilarity) {
            logger.info(String.format("the patch IS NOT PRESENT, " +
                            "pre similarity=%f\tpost similarity=%f",
                    preSimilarity, postSimilarity));
        } else if (Math.abs(postSimilarity - preSimilarity) < 0.0001) {
            logger.info(String.format("the similarity for pre and post is same, that is ward ," +
                            "pre similarity=%f\tpost similarity=%f",
                    preSimilarity, postSimilarity));
        }
        if (config.isEnableDebugLevel())
            logger.info(String.format("apk:%s\tcve:%s", apk.getAPKName(), summary.getCVENumber()));
    }

    /*
    argue that matched method between tpl and app should have at least one common <init>
    tpl: A <init>(B b, C c, D d)
    app: A' <init>(B' b, C' c, D' d)

    tpl: X method(Y y, Z z)
    app: X' method(Y' y, Z' z)

    things to be recovered:
    class name, it will influence method, field
    method name
    field name

    when computing similarity, if class or method is from java library or android library, it should be matched 100%
    for those class or method that can not be recovered, we mark it with UNKNOW
     */
    private Map<String, String> typeRecovery(MarkedMethod tplMethod, MarkedMethod appMethod) {
        Map<String, String> tplMapAppClass = new HashMap<>();

        SootMethod tpl = tplMethod.m.body.getMethod();
        SootMethod apk = appMethod.m.body.getMethod();

        // 1. map current class
        tplMapAppClass.put(apk.getDeclaringClass().getName(), tpl.getDeclaringClass().getName());

        // 2. map method sig
        // 2.1 return type
        mapType(tpl.getReturnType(), apk.getReturnType(), tplMapAppClass);

        // 2.2 parameter
        for (int i = 0; i < tpl.getParameterTypes().size(); i++) {
            mapType(tpl.getParameterType(i), apk.getParameterType(i), tplMapAppClass);
        }

        // 3. all possible <init> parameters
        for (MethodAttr tplInit : tplMethod.m.declaredClass.methods) {
            if (!tplInit.subSignature.contains("<init>"))
                continue;
            MarkedMethod matchedAppInitMethod = candidateInitMap.get(tplInit);
            if (matchedAppInitMethod != null) {
                SootMethod tplInitSootMethod = tplInit.body.getMethod();
                SootMethod apkInitSootMethod = matchedAppInitMethod.m.body.getMethod();
                if (apkInitSootMethod == null)
                    continue;
                for (int i = 0; i < tplInitSootMethod.getParameterTypes().size(); i++) {
                    mapType(tplInitSootMethod.getParameterType(i),
                            apkInitSootMethod.getParameterType(i), tplMapAppClass);
                }
            }
        }
        return tplMapAppClass;
    }

    private void mapType(Type tplType, Type appType, Map<String, String> map) {
        if (appType instanceof RefType) {
            RefType refType = (RefType) appType;
            if (refType.getSootClass().isApplicationClass()
                    || refType.getSootClass().isPhantomClass()) {
                StringBuilder tplTypeSb = new StringBuilder();
                if (tplType instanceof ArrayType) {
                    tplTypeSb.append(((ArrayType) tplType).baseType);
                } else {
                    tplTypeSb.append(tplType);
                }
                map.put(refType.toString(), tplTypeSb.toString());
            }
        }
    }

    private float MethodSimilarityCompute(MethodDigest_new2 tpl, MethodDigest_new2 app,
                                          Map<String, String> typeRecoveryMap) {
        int tplPathCount = tpl.realUnitsPaths.size();
        int appPathCount = app.realUnitsPaths.size();

        float[][] sim = new float[tplPathCount][appPathCount];
        float[][] predicateSimMatrix = new float[tplPathCount][appPathCount];
        float[][] signatureSimMatrix = new float[tplPathCount][appPathCount];
        float[][] variableSimMatrix = new float[tplPathCount][appPathCount];
        float[][] denominatorMatrix = new float[tplPathCount][appPathCount];

        // do recovery
        for (int i = 0; i < appPathCount; i++) {
            List<Node<PredicateNodeData>> appPredicateList = app.realPredicates.get(i);
            List<String> appSignature = app.realSignatures.get(i);
            List<String> appVariable = app.realVariables.get(i);
            for (Node<PredicateNodeData> node : appPredicateList)
                doRecoveryNode(node, typeRecoveryMap);

            doRecovery(appSignature, typeRecoveryMap);
            doRecovery(appVariable, typeRecoveryMap);

        }

        for (int i = 0; i < tplPathCount; i++) {
            Stmt tplLastStmt = (Stmt) tpl.realUnitsPaths.get(i).get(tpl.realUnitsPaths.get(i).size() - 1);
            List<Node<PredicateNodeData>> tplPredicateList = tpl.realPredicates.get(i);
            List<String> tplSignature = tpl.realSignatures.get(i);
            List<String> tplVariable = tpl.realVariables.get(i);

            for (int j = 0; j < appPathCount; j++) {
                Stmt appLastStmt = (Stmt) app.realUnitsPaths.get(j).get(app.realUnitsPaths.get(j).size() - 1);
                if ((tplLastStmt instanceof ReturnStmt && appLastStmt instanceof ReturnStmt)
                        || (tplLastStmt instanceof ReturnVoidStmt && appLastStmt instanceof ReturnVoidStmt)
                        || (tplLastStmt instanceof ThrowStmt && appLastStmt instanceof ThrowStmt)) {
                    List<Node<PredicateNodeData>> appPredicateList = app.realPredicates.get(j);
                    List<String> appSignature = app.realSignatures.get(j);
                    List<String> appVariable = app.realVariables.get(j);

                    float predicateSim = predicateSimilarityCompute(tplPredicateList, appPredicateList);
                    float signatureSim = signatureOrVariableSimilarityCompute(tplSignature, appSignature);
                    float variableSim = signatureOrVariableSimilarityCompute(tplVariable, appVariable);

                    predicateSimMatrix[i][j] = predicateSim;
                    signatureSimMatrix[i][j] = signatureSim;
                    variableSimMatrix[i][j] = variableSim;

//                    sim[i][j] = (predicateSim * 3 + signatureSim * 2 + variableSim) / 6;
                    sim[i][j] = (predicateSim + signatureSim + variableSim) / 3;

//                    int denominator = 3;
//                    if (tplPredicateList.isEmpty())
//                        denominator -= 1;
//                    if (tplSignature.isEmpty())
//                        denominator -= 1;
//                    if (tplVariable.isEmpty())
//                        denominator -= 1;
//                    denominatorMatrix[i][j] = denominator;
                }
            }
        }
//        for (int i = 0; i < tplPathCount; i++) {
//            for (int j = 0; j < appPathCount; j++) {
////                float predicate = maxPredicate == 0 ? 0 : predicateSimMatrix[i][j] / maxPredicate;
////                float signature = maxSignature == 0 ? 0 : signatureSimMatrix[i][j] / maxSignature;
////                float variable = maxVariable == 0 ? 0 : variableSimMatrix[i][j] / maxVariable;
//                sim[i][j] = denominatorMatrix[i][j] == 0 ? 0 :
//                        (predicateSimMatrix[i][j] + signatureSimMatrix[i][j] + variableSimMatrix[i][j]) / denominatorMatrix[i][j];
//            }
//        }

        return hgAlgorithm(sim, "max") / Math.max(appPathCount, tplPathCount); // set similarity
    }

    private float predicateSimilarityCompute(List<Node<PredicateNodeData>> tplList,
                                             List<Node<PredicateNodeData>> appList) {
        if (tplList.isEmpty() && appList.isEmpty())
            return 1.0f;

        if (tplList.isEmpty() || appList.isEmpty())
            return 0.0f;

        List<Node<PredicateNodeData>> shorter, longer;
        if (tplList.size() >= appList.size()) {
            shorter = appList;
            longer = tplList;
        } else {
            shorter = tplList;
            longer = appList;
        }

        float[][] predicateSim = new float[shorter.size()][longer.size()];
//        float min = Float.MAX_VALUE;
        for (int i = 0; i < shorter.size(); i++) {
            for (int j = i; j <= longer.size() - shorter.size() + i && j < longer.size(); j++) {
                APTED<PredicateCostModel_back, PredicateNodeData> apted = new APTED<>(new PredicateCostModel_back());
                float distance = apted.computeEditDistance_spfTest(shorter.get(i),
                        longer.get(j), 0);
//                if (distance < min)
//                    min = distance;
                predicateSim[i][j] = 1 / (1 + distance / 4);
            }
        }
//        for (int i = 0; i < shorter.size(); i++) {
//            for (int j = i; j <= longer.size() - shorter.size() + i && j < longer.size(); j++) {
////                predicateSim[i][j] = 1 - predicateSim[i][j] / max;
//                predicateSim[i][j] = predicateSim[i][j] == 0 ? 1 : min / predicateSim[i][j];
//            }
//        }

        CombinationSelect combinationSelect = new CombinationSelect(predicateSim,
                shorter.size(), longer.size());

        predicateSim = null; // free the memory
//        return combinationSelect.max / Math.max(appList.size(), tplList.size()); // in practice, each path in tpl should have a counterpart in app
        return combinationSelect.max / tplList.size(); // in practice, each path in tpl should have a counterpart in app
    }

    private float signatureOrVariableSimilarityCompute(List<String> tplList, List<String> appList) {
        if (tplList.isEmpty() && appList.isEmpty())
            return 1.0f;
        if (tplList.isEmpty() || appList.isEmpty())
            return 0f;
        // simply use Jaccard similarity, otherwise the overhead is tooooo large
        boolean[] libFlag = new boolean[tplList.size()];
        boolean[] appFlag = new boolean[appList.size()];

        float mergeNum;//Number of union elements
        float commonNum = 0;//Number of same elements
        for (int i = 0; i < tplList.size(); i++) {
            if (libFlag[i])
                continue;
            for (int j = 0; j < appList.size(); j++) {
                if (appFlag[j])
                    continue;
                if (isSignatureMatch(tplList.get(i), appList.get(j))) {
                    libFlag[i] = appFlag[j] = true;
                    commonNum++;
                    break;
                }
            }
        }
        mergeNum = tplList.size() + appList.size() - commonNum;
        libFlag = appFlag = null; // free the memory
        return commonNum / mergeNum;
    }


//    private float signatureOrVariableSimilarityCompute(List<String> tplList, List<String> appList) {
//        List<String> shorter, longer;
//        if (tplList.size() >= appList.size()) {
//            shorter = appList;
//            longer = tplList;
//        } else {
//            shorter = tplList;
//            longer = appList;
//        }
//
//        float[][] sim = new float[shorter.size()][longer.size()];
//        float max = -1;
//        for (int i = 0; i < shorter.size(); i++) {
//            for (int j = i; j <= longer.size() - i && j < longer.size(); j++) {
//                sim[i][j] = signatureSimilarity(shorter.get(i), longer.get(j));
//            }
//        }
//
//        CombinationSelect combinationSelect = new CombinationSelect(sim,
//                shorter.size(), longer.size());
//
//        sim = null; // free the memory
//        return combinationSelect.max;
//    }

    private boolean isSignatureMatch(String sig1, String sig2) {
        String[] split1 = sig1.split(",");
        String[] split2 = sig2.split(",");
        if (split1.length != split2.length)
            return false;
        for (int i = 0; i < split1.length; i++) {
            if (!split2[i].equals("X") && !split1[i].equals(split2[i]))
                return false;
        }
        return true;
    }

    private void doRecoveryNode(Node<PredicateNodeData> node, Map<String, String> typeRecoveryMap) {
        PredicateNodeData data = node.getNodeData();
        switch (data.getNodeType()) {
            case InstanceOf: // ensure class is not array form, like A[]
            case Class: // ensure class is not array form, like A[]
            case Array: // ensure array type is base type
                if (!isJavaLibraryClass(data.getData()))
                    data.setData(typeRecoveryMap.getOrDefault(data.getData(), "X"));
                break;
            case Invoke:
                String[] sigSplit = data.getData().split(",");
                StringBuilder invokeSb = new StringBuilder();
                for (String s : sigSplit) {
                    if (isJavaLibraryClass(s))
                        invokeSb.append(s).append(",");
                    else
                        invokeSb.append(typeRecoveryMap.getOrDefault(s, "X")).append(",");
                }
                data.setData(invokeSb.toString());
                break;
            case Field:
                String[] fieldSplit = data.getData().split("#");
                StringBuilder fieldSb = new StringBuilder();
                for (int i = 0; i < fieldSplit.length; i++) {
                    if (isJavaLibraryClass(fieldSplit[i]))
                        fieldSb.append(fieldSplit[i]);
                    else
                        fieldSb.append(typeRecoveryMap.getOrDefault(fieldSplit[i], "X"));
                    if (i == 0)
                        fieldSb.append("#");
                }
                data.setData(fieldSb.toString());
                break;
        }
        for (Node<PredicateNodeData> child : node.getChildren()) {
            doRecoveryNode(child, typeRecoveryMap);
        }
    }

    private void doRecovery(List<String> appSigList,
                            Map<String, String> typeRecoveryMap) {
        for (int i = 0; i < appSigList.size(); i++) {
            StringBuilder sb = new StringBuilder();
            String[] split = appSigList.get(i).split(",");
            for (String s : split) {
                if (isJavaLibraryClass(s))
                    sb.append(s).append(",");
                else if (s.contains("[]"))
                    sb.append(typeRecoveryMap.getOrDefault(s, "X[]")).append(",");
                else
                    sb.append(typeRecoveryMap.getOrDefault(s, "X")).append(",");
            }
            appSigList.set(i, sb.toString());
        }
    }


//    private void report() {
//        try {
//            File out = new File(config.getOutputFile());
//            FileWriter fw = new FileWriter(out);
//            for (Map.Entry<MethodAttr, List<MethodWithSimilarity>> entry : candidateMatchedMethods.entrySet()) {
//                MethodAttr src = entry.getKey();
//                fw.write(src.declaredClass.name + "->" + src.signature + "\n");
//                for (MethodWithSimilarity can : entry.getValue()) {
//                    fw.write(can.sim + " " + can.m.declaredClass.name + "->" + can.m.signature + "#" + can.m.fuzzy + "\n");
//                }
//                fw.write("\n");
//            }
//            fw.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}


