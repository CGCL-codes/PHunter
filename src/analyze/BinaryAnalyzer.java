package analyze;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.G;
import soot.Main;
import soot.PackManager;
import soot.Transform;
import soot.options.Options;

import java.io.File;
import java.io.IOException;

/**
 * Hello world!
 */
public class BinaryAnalyzer extends Analyzer {
    private final boolean isPre;
    private final Configuration config;
    private final Logger logger = LoggerFactory.getLogger(getClass());

//    final ConcurrentHashMap<SootMethod, Body> pre_bodies = new ConcurrentHashMap<>();
//    final ConcurrentHashMap<SootMethod, Body> post_bodies = new ConcurrentHashMap<>();
//    public Map<String, ClassAttr> preBinAttr = new HashMap<>();
//    public Map<String, ClassAttr> postBinAttr = new HashMap<>();

    public BinaryAnalyzer(Configuration config, boolean isPre) throws IOException {
        super(config);
        this.isPre = isPre;
        this.config = config;
        SootCallGraph cg = analyze(isPre);
        buildCG(cg);
    }

    private void initializeSoot() {
        G.reset();
        Options.v().set_whole_program(true);
        Options.v().set_src_prec(soot.options.Options.src_prec_only_class);
        Options.v().set_output_format(soot.options.Options.output_format_n);
        Options.v().set_allow_phantom_refs(true);

    }

    private SootCallGraph analyze(boolean isPre) throws IOException {
        initializeSoot();
        SootCallGraph cg = new SootCallGraph(false);
        String inputPath;
        if (isPre) {
            inputPath = config.getPreBinary();
            PackManager.v().getPack("jtp").add(new Transform("jtp.pre", new CallGraphTransform(cg)));
            logger.info(String.format("Analyzing the pre-patched binary %s", config.getPreBinary()));
        } else {
            inputPath = config.getPostBinary();
            PackManager.v().getPack("jtp").add(new Transform("jtp.post", new CallGraphTransform(cg)));
            logger.info(String.format("Analyzing the post-patched binary %s", config.getPostBinary()));
        }
        // analyze the pre-patch binary
        File inputFile = new File(inputPath);
        String sootOutDir = "tmp" + File.separator +
                inputFile.getName().substring(0, inputFile.getName().indexOf(".jar"));
        String[] sootArgs = new String[]{
                "-keep-line-number",
                "-p", "jb", "use-original-names:true",
                "-process-dir",
                inputFile.getCanonicalPath(),
//                "-d",
//                sootOutDir,
        };
        Main.main(sootArgs);
        cg.buildSootCallGraph();
        return cg;
    }

    public String getTPLName() {
        return isPre ? config.getPreBinary() : config.getPostBinary();
    }

////    public static void main(String[] args) throws Exception {
////        // 1. enable assertion and build options
////        ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
////        BinaryInfo bi = new BinaryInfo();
////        bi.buildOptions();
////
////        // 2. set soot options
////        bi.setSootOptions(args);
////
////        // 3.1 call analyze pre-patch binary
////        bi.analyze(true);
////
////        // 3.2 reset the environment and analyze post-patch binary
////        G.reset();
////        bi.setSootOptions(args);
////        bi.analyze(false);
////
////        // 4 analyze the patch to extract patch-related method for location
////        PatchSummary patchSummary = new PatchSummary(bi);
////
////        // 5 output the summary
////        bi.writePatchSummary(patchSummary);
////        System.out.println(patchSummary);
////    }
//
//    /**
//     * @param allClasses allClasses from .jar
//     * @description: extract 1) class inheritance 2) field reference 3) method call xref
//     * output format like: classname parent \t method sig \t callee list \t field xref
//     * @author: Zifan Xie
//     * @time: 2021/9/13 11:07
//     */
//    private void extractSignatures(Chain<SootClass> allClasses, final boolean isPre) {
//        int threadNum = Integer.parseInt(config.getThreadNumber());
//        final ConcurrentHashMap<SootMethod, Body> bodies = isPre ? this.pre_bodies : this.post_bodies;
//
//        CountingThreadPoolExecutor executor = new CountingThreadPoolExecutor(threadNum,
//                threadNum, 30, TimeUnit.SECONDS,
//                new LinkedBlockingQueue<Runnable>());
//        try {
//            for (final SootClass c : allClasses) {
//                executor.execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        extractSignatureWorker(c, bodies, isPre);
//                    }
//                });
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
//
//    private void extractSignatureWorker(SootClass sootClass,
//                                        Map<SootMethod, Body> bodies,
//                                        boolean isPre) {
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
//                if (outerClass != null) {
//                    clazz.setOuterClassName(sootClass.getOuterClass().getName());
//                }
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
//            MethodAttr md = new MethodAttr();
//            md.setName(method.getName());
//            md.setSubSignature(method.getSubSignature());
//            md.setSignature(method.getSignature());
//            md.setClassName(sootClass.getName());
//            md.setModifiers(soot.Modifier.toString(method.getModifiers()));
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
//        if (isPre)
//            preBinAttr.put(clazz.name, clazz);
//        else postBinAttr.put(clazz.name, clazz);
////        try {
////            fw.write(clazz.toString());
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
//    }
//
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
//        else methodAttr.addCallee(targetMethod.getSignature());
//
//    }
//
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
