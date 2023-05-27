package analyze;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;

import java.util.*;

public class Analyzer {
    private final Configuration config;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    //    final ConcurrentHashMap<SootMethod, Body> bodies = new ConcurrentHashMap<>();
//    public Map<String, ClassAttr> binAttr = new HashMap<>();
    public Map<String, ClassAttr> allClasses = new HashMap<>();

    public Set<MethodAttr> allMethods = new HashSet<>();

    public Analyzer(Configuration config) {
        this.config = config;
    }

    public void buildCHA(Map<SootClass, ClassAttr> map) {
        for (SootClass sc : map.keySet()) {
            ClassAttr c = map.get(sc);
            SootClass superCls = sc;
            while (superCls.hasSuperclass()) {
                if (superCls.isPhantom())
                    c.has_phantom_superclass_ = true;
                superCls = superCls.getSuperclass();
                if (map.containsKey(superCls)) {
                    c.superClasses.add(map.get(superCls));
                    continue;
                }
                c.superClasses.add(new ClassAttr(superCls));
            }
//            LinkedList<SootClass> worklist = new LinkedList<>();
//            Set<SootClass> visited = new HashSet<>();
//            worklist.add(sc);
//            visited.add(sc);
//            while (!worklist.isEmpty()) {
//                SootClass head = worklist.poll();
//                for (SootClass i : head.getInterfaces()) {
//                    if (i.isPhantom())
//                        c.has_phantom_interface_ = true;
//                    if (visited.contains(i))
//                        continue;
//                    visited.add(i);
//                    worklist.add(i);
//                    if (map.containsKey(i)) {
//                        c.interfaces_.add(map.get(i));
//                        continue;
//                    }
//                    c.interfaces_.add(new ClassAttr(i));
//                }
//            }
        }
    }

    public Map<SootClass, ClassAttr> buildClassAttr(Set<SootClass> allCls) {
        Map<SootClass, ClassAttr> map = new HashMap<>();
        for (SootClass c : allCls) {
            ClassAttr newCls = new ClassAttr(c);
            if (c.isApplicationClass())
                this.allClasses.put(c.getName(), newCls);
            map.put(c, newCls);
        }
        buildCHA(map);
        return map;
    }

    public Map<SootMethod, MethodAttr> buildMethodAttr(Set<SootMethod> all_method,
                                                       Map<SootMethod, MethodAttr> sootMethodMethodAttrMap) {
        Map<SootMethod, MethodAttr> map = new HashMap<>();
        for (SootMethod m : all_method) {
            if (sootMethodMethodAttrMap.containsKey(m)) {
                MethodAttr methodAttr = sootMethodMethodAttrMap.get(m);
                map.put(m, methodAttr);
                allMethods.add(methodAttr);
            }
            else {
                MethodAttr new_method = new MethodAttr(m);
                map.put(m, new_method);
            }
        }
        return map;
    }

    public void buildClassAndMethod(Map<SootClass, ClassAttr> cls_map, Map<SootMethod, MethodAttr> method_map) {
        for (SootMethod m : method_map.keySet()) {
            MethodAttr mn = method_map.get(m);
            mn.declaredClass = cls_map.getOrDefault(m.getDeclaringClass(), null);
        }
//        for (Map.Entry<SootMethod, MethodAttr> entry : method_map.entrySet()) {
//            MethodAttr mn = entry.getValue();
//            mn.declaredClass = cls_map.getOrDefault(entry.getKey().getDeclaringClass(), null);
//            allMethods.add(entry.getValue());
//        }
        for (SootClass c : cls_map.keySet()) {
            ClassAttr cn = cls_map.get(c);
            for (SootMethod m : c.getMethods()) {
//                if (!method_map.containsKey(m) ||
//                        m.isStaticInitializer())
//                    continue;
                if (method_map.containsKey(m))
                    cn.methods.add(method_map.get(m));
            }
        }
    }

    public void buildCG(SootCallGraph sootcg) {
        Map<SootClass, ClassAttr> cls_map = buildClassAttr(sootcg.allSootClasses);
        Map<SootMethod, MethodAttr> map = buildMethodAttr(sootcg.callGraph.keySet(), sootcg.sootMethodMethodAttrMap);
        buildClassAndMethod(cls_map, map);
        for (SootMethod src : sootcg.callGraph.keySet()) {
            if (!map.containsKey(src))
                continue;
            List<SootMethod> tgts = sootcg.callGraph.get(src);
            for (SootMethod tgt : tgts) {
                if (!map.containsKey(tgt))
                    continue;
                if (tgt.equals(src))
                    continue;
                map.get(src).callee.add(map.get(tgt));
                map.get(tgt).caller.add(map.get(src));
            }
        }
    }


//    public void extractSignatures(Chain<SootClass> allClasses) {
//        int threadNum = Integer.parseInt(config.getThreadNumber());
//
//        CountingThreadPoolExecutor executor = new CountingThreadPoolExecutor(threadNum,
//                threadNum, 30, TimeUnit.SECONDS,
//                new LinkedBlockingQueue<Runnable>());
//        try {
//            for (final SootClass c : allClasses) {
//                executor.execute(() -> extractSignatureWorker(c, bodies));
//            }
//
//            // Wait till all packs have been executed
//            executor.awaitCompletion();
//            executor.shutdown();
//        } catch (InterruptedException e) {
//            // Something went horribly wrong
//            throw new RuntimeException("Could not wait for extract threads to "
//                    + "finish: " + e.getMessage(), e);
//        }
//
//        // If something went wrong, we tell the world
//        if (executor.getException() != null)
//            throw (RuntimeException) executor.getException();
//    }

//    private void extractSignatureWorker(SootClass sootClass,
//                                        Map<SootMethod, Body> bodies) {
//        if (!sootClass.isApplicationClass()) return;
//        String clazzName = sootClass.getName();
//        ClassAttr clazz = new ClassAttr(clazzName);
//
//        // super class, innner classes
//        if (sootClass.hasSuperclass())
//            clazz.setSuperClassName(sootClass.getSuperclass().getName());
//        else
//            System.out.println(clazzName + " doesn't have super class! This is weird!");
//        clazz.setPackageName(sootClass.getPackageName());
//        clazz.setModifiers(Modifier.toString(sootClass.getModifiers()));
//        if (sootClass.hasOuterClass()) {
//            clazz.setOuterClassName(sootClass.getOuterClass().getName());
//        } else if (clazzName.contains("$")) {
//            // TODO: This is a temporary fix to the outer class relationship
//            try {
//                // Get the outer class, and strip trailing extra $ characters!
//                String possibleOuterClass = clazzName.contains("$") ? clazzName :
//                        clazzName.substring(0, clazzName.lastIndexOf('$'));
//                SootClass outerClass = Scene.v().getSootClass(possibleOuterClass);
////                if (outerClass != null) {
////                    clazz.setOuterClassName(sootClass.getOuterClass().getName());
////                }
//            } catch (Exception e) {
//                // The soot class may not exist
//                e.printStackTrace();
//            }
//        }
//
//        // methods, <init>, <clinit>, and other methods
//        // constant strings, each method, as well as in <init> and <clinit>
//        List<SootMethod> methods = sootClass.getMethods();
//        for (SootMethod method : methods) {
//            MethodAttr md = new MethodAttr(method);
////            System.out.println(method.getJavaSourceStartLineNumber());
////            md.setStartLinenumber(method.getJavaSourceStartLineNumber()); // always return -1
//            Body body = bodies.get(method);
//            // even the method don't have a body, just record it
//            if (body == null) {
//                clazz.addMethod(md);
//                continue;
//            }
//            getStartEndNumber(body, md);
//            PatchingChain<Unit> units = body.getUnits();
//            for (Unit unit : units) {
//                if (unit instanceof AssignStmt) {
//                    AssignStmt assignStmt = (AssignStmt) unit;
//                    Value lV = assignStmt.getLeftOp();
//                    Value rV = assignStmt.getRightOp();
//                    if (lV instanceof StaticFieldRef || lV instanceof InstanceFieldRef)
//                        handleField(md, lV, true);
//                    if (rV instanceof StaticFieldRef || rV instanceof InstanceFieldRef)
//                        handleField(md, rV, false);
//                    if (rV instanceof InvokeExpr) {
//                        handleInvokeExpr(md, (Expr) rV);
//                    }
//                } else if (unit instanceof InvokeStmt) {
//                    InvokeStmt invokeStmt = (InvokeStmt) unit;
//                    InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
//                    handleInvokeExpr(md, invokeExpr);
//                }
//            }
//            clazz.addMethod(md);
//        }
//        binAttr.put(clazz.name, clazz);
////        try {
////            fw.write(clazz.toString());
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
//    }

//    private void handleInvokeExpr(MethodAttr methodAttr, Expr expr) {
//        InvokeExpr invokeExpr = (InvokeExpr) expr;
//        SootMethod targetMethod = null;
//        try {
//            targetMethod = invokeExpr.getMethod();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        if (targetMethod == null) {
//            return;
//        }
//        if (targetMethod.isJavaLibraryMethod())
//            methodAttr.addJavaLibraryCall(targetMethod.getSignature());
//        else if (isAndroidLibraryCall(targetMethod))
//            methodAttr.addAndroidFrameworkCall(targetMethod.getSignature());
//        else {
//            if (!callSites.containsKey(methodAttr))
//                callSites.put(methodAttr, new HashSet<>());
//            callSites.get(methodAttr).add(targetMethod);
//        }
//    }


//    private void handleField(MethodAttr methodAttr, Value v, boolean isLeft) {
//        String sig = null;
//        if (v instanceof StaticFieldRef) {
//            StaticFieldRef staticFieldRef = (StaticFieldRef) v;
//            sig = staticFieldRef.getField().getSignature();
//        } else if (v instanceof InstanceFieldRef) {
//            InstanceFieldRef instanceFieldRef = (InstanceFieldRef) v;
//            sig = instanceFieldRef.getField().getSignature();
//        }
//        if (isLeft)
//            methodAttr.addWriteField(sig);
//        else methodAttr.addReadField(sig);
//    }

    public void getStartEndNumber(Body body, MethodAttr method) {
        int s = -1;
        int e = -1;
//        if (body == null) return line;
        PatchingChain<Unit> units = body.getUnits();
        for (Unit unit : units) {
            int l = unit.getJavaSourceStartLineNumber();
            if (l > -1) {
                method.setStartLinenumber(l);
                break;
            }
        }
        method.setEndLinenumber(units.getLast().getJavaSourceStartLineNumber());
    }

    public ParsePatchFiles readPatchFile() {
        String path = config.getPatchFiles();
        return new ParsePatchFiles(path);
    }

//    private void writePatchSummary(PatchSummary summary) {
//        try {
//            String path = commandLine.getOptionValue("sigPath");
//            File file = new File(path);
//            File fileParent = file.getParentFile();
//            if (!fileParent.exists())
//                fileParent.mkdir();
//            FileWriter out = new FileWriter(file);
//            out.write(summary.toString());
//            out.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}
