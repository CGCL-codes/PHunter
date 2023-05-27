package analyze;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Main;
import soot.PackManager;
import soot.Transform;
import soot.options.Options;

import java.io.File;
import java.io.IOException;

public class APKAnalyzer extends Analyzer {
    private final Configuration config;
    private final Logger logger = LoggerFactory.getLogger(getClass());


    public APKAnalyzer(Configuration config) throws IOException {
        super(config);
        this.config = config;
        SootCallGraph cg = analyze();
        buildCG(cg);
    }

    private void initializeSoot() {
        soot.G.reset();
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_whole_program(true); //get ICFG
        Options.v().set_no_bodies_for_excluded(true);


        // Read (APK Dex-to-Jimple) Options
        Options.v().set_force_android_jar(config.getAndroidPlatformJar()); // The path to Android Platforms
        Options.v().set_src_prec(Options.src_prec_apk); // Determine the input is an APK
        Options.v().set_process_multiple_dex(true);  // Inform Dexpler that the APK may have more than one .dex files
        Options.v().set_keep_line_number(false);  //do not record linenumber
        Options.v().set_keep_offset(false); // do not keep offset
        Options.v().set_throw_analysis(Options.throw_analysis_dalvik);
        Options.v().set_ignore_resolution_errors(true);
//        Options.v().set_process_dir(Collections.singletonList(config.getTargetAPKFile()));

//        Options.v().setPhaseOption("cg.spark", "on");


        // Write (APK Generation) Options
//        Options.v().set_output_format(Options.output_format_J);
        Options.v().set_output_format(Options.output_format_n);
//        Options.v().set_output_format(Options.output_format_c);
//        Options.v().set_output_format(Options.output_format_dex);
//        Scene.v().loadNecessaryClasses();

        // Resolve required classes
//        Scene.v().addBasicClass("java.io.PrintStream", SootClass.SIGNATURES);
//        Scene.v().addBasicClass("java.lang.System", SootClass.SIGNATURES);
//        Scene.v().loadNecessaryClasses();
    }

    private SootCallGraph analyze() throws IOException {
        initializeSoot();
        SootCallGraph cg = new SootCallGraph(true);
        File inputFile = new File(config.getTargetAPKFile());
        PackManager.v().getPack("jtp").add(new Transform("jtp.apk", new CallGraphTransform(cg)));
        logger.info(String.format("Analyzing the apk %s", config.getTargetAPKFile()));

        String[] sootArgs = new String[]{
                "-process-dir",
                inputFile.getCanonicalPath(),
//                "-d",
//                sootOutDir,
        };
        Main.main(sootArgs);
        cg.buildSootCallGraph();
        return cg;
    }

    public String getAPKName() {
        return config.getTargetAPKFile();
    }
}
