package test;

import soot.*;
import soot.options.Options;
import soot.toolkits.graph.BriefBlockGraph;
import symbolicExec.MethodDigest;

import java.util.Collections;

public class MethodDigestTest {

    public static void main(String[] args) {
        // for dasho
        readJimple("tmp/de.devmil.muzei.bingimageofthedayartsource-dasho");
        debug();

        // for proguard
//        readJimple("tmp/de.devmil.muzei.bingimageofthedayartsource-proguard");
//        debug("wy.rb", "c");
    }

    private static void readJimple(String src) {
        G.reset();
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_allow_phantom_elms(true);
        Options.v().set_ast_metrics(true);
        Options.v().set_whole_program(true);
        Options.v().set_prepend_classpath(true);
        Options.v().set_src_prec(Options.src_prec_jimple);
        Options.v().set_verbose(true);
        Options.v().set_process_dir(Collections.singletonList(src));
//        Options.v().set_java_version(Options.java_version_8);
        Scene.v().loadNecessaryClasses();
    }

    private static void debug() {
        for (SootClass clazz : Scene.v().getApplicationClasses()) {
            for (SootMethod method : clazz.getMethods()) {

                    try {
                        BriefBlockGraph bg = new BriefBlockGraph(method.retrieveActiveBody());
                        if (bg.getBlocks().size() <= 100) {
                        System.out.println(method.getSignature());
                        Body body = method.retrieveActiveBody();
                        MethodDigest md = new MethodDigest(body, null);}
                    } catch (Exception ignore) {

                    }
                }
            }

    }
}
