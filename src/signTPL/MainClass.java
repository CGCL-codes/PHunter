package signTPL;

import analyze.*;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import symbolicExec.MethodDigest;

import java.io.File;
import java.io.IOException;

import static util.AARUtils.aarToJar;

public class MainClass {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Options options = new Options();
    protected CommandLine cmd = null;

    private static final String OPTION_APK_FILE = "targetAPK";
    private static final String OPTION_PRE_BINARY = "preTPL";
    private static final String OPTION_POST_BINARY = "postTPL";
    private static final String OPTION_PATCH_FILE = "patchFiles";
    private static final String OPTION_THREAD_NUMBER = "threadNum";
    //    private static final String OPTION_OUTPUT_FILE = "output";
    private static final String OPTION_ANDROID_JAR = "androidJar";
    private static final String OPTION_ENABLE_DEBUG = "enableDebug";

    protected MainClass() {
        initializeCommandLineOptions();
    }

    /**
     * Initializes the set of available command-line options
     */
    private void initializeCommandLineOptions() {
        options.addOption("?", "help", false, "Print this help message");
        options.addOption(OPTION_APK_FILE, true, "Path to pre-patched binary(.apk), " +
                "or a directory contains multi apks");
        options.addOption(OPTION_PRE_BINARY, true, "Path to pre-patched binary(.jar/.aar)");
        options.addOption(OPTION_POST_BINARY, true, "Path to post-patched binary(.jar/.aar)");
        options.addOption(OPTION_PATCH_FILE, true, "Path to patch files, " +
                "if exist more than 1 files, split by the ';’ (e.g., 1.diff;2.diff)");
//        options.addOption(OPTION_OUTPUT_FILE, true, "path to output, if have multi apks," +
//                " the path should be a directory, each of apk will have a res file in the dir");
        options.addOption(OPTION_THREAD_NUMBER, true, "The number of threads to use");
        options.addOption(OPTION_ANDROID_JAR, true, "The path to android.jar");
        options.addOption(OPTION_ENABLE_DEBUG, false, "Is enable debug level");
    }

    public static void main(String[] args) throws Exception {
//        TimeRecorder.beforeTotal = System.currentTimeMillis();
        MainClass main = new MainClass();
        main.run(args);
//        TimeRecorder.afterTotal = System.currentTimeMillis();
    }

    protected void run(String[] args) throws Exception {
        HelpFormatter hf = new HelpFormatter();
        hf.setWidth(110);

        // We need proper parameters
        final HelpFormatter formatter = new HelpFormatter();
        if (args.length == 0) {
            hf.printHelp("help", options, true);
            System.exit(0);
        }

        // Parse the command-line parameters
        try {
            CommandLineParser parser = new PosixParser();
            try {
                cmd = parser.parse(options, args);
                cmd.getArgs();
            } catch (ParseException ex) {
                ex.printStackTrace();
            }

            // Do we need to display the user manual?
            if (cmd.hasOption("?") || cmd.hasOption("help")) {
                formatter.printHelp("signTPL [OPTIONS]", options);
                return;
            }

            Configuration config = new Configuration();
            parseCommandOptions(cmd, config);

            // 1 analyze the pre-patch and post-patch binary
            TimeRecorder.beforePre = System.currentTimeMillis();
            BinaryAnalyzer pre = new BinaryAnalyzer(config, true);
            TimeRecorder.afterPre = System.currentTimeMillis();

            TimeRecorder.beforePost = System.currentTimeMillis();
            BinaryAnalyzer post = new BinaryAnalyzer(config, false);
            TimeRecorder.afterPost = System.currentTimeMillis();

//            // 2 analyze the patch to extract patch-related method for location
            PatchSummary patchSummary = new PatchSummary(config, pre, post);

//             3 analyze the target obfuscated app(maybe more than one)

            TimeRecorder.beforeAPK = System.currentTimeMillis();
            APKAnalyzer apk = new APKAnalyzer(config);
            TimeRecorder.afterAPK = System.currentTimeMillis();

            PatchPresentTest_new ppt = new PatchPresentTest_new(config, patchSummary, apk);


        } catch (AbortAnalysisException e) {
            // Silently return
        } catch (Exception e) {
            System.err.printf("The analysis has failed. Error message: %s\n", e.getMessage());
            e.printStackTrace();
        }
    }


    protected void DataflowAnalysisDebug(Analyzer pre, Analyzer post, String className, String methodName) {
        MethodDigest preD = null, postD = null;
        for (ClassAttr clazz : pre.allClasses.values())
            if (clazz.name.contains(className)) {
                for (MethodAttr method : clazz.methods) {
                    if (method.body.getMethod().getName().equals(methodName)) {
                        preD = new MethodDigest(method.body, null);
                        int a = 0;
                    }
                }
                if (preD != null)
                    break;
            }
        for (ClassAttr clazz : post.allClasses.values())
            if (clazz.name.endsWith(className)) {
                for (MethodAttr method : clazz.methods) {
                    if (method.body.getMethod().getName().equals(methodName)) {
                        postD = new MethodDigest(method.body, null);
                        break;
                    }
                }
                if (postD != null)
                    break;
            }
    }

    protected void parseCommandOptions(CommandLine cmd, Configuration config) throws IOException {
        String apkFile = cmd.getOptionValue(OPTION_APK_FILE);
        if (apkFile != null && !apkFile.isEmpty()) {
            File targetFile = new File(apkFile);
            if (!targetFile.exists()) {
                System.err.printf("Target APK file %s does not exist%n", targetFile.getCanonicalPath());
                return;
            }
            config.setTargetAPKFile(apkFile);
        }

        String preBinary = cmd.getOptionValue(OPTION_PRE_BINARY);
        if (preBinary != null && !preBinary.isEmpty()) {
            if (preBinary.endsWith(".aar")) {
                logger.info(String.format("Convert pre-patched binary %s to %s",
                        preBinary, preBinary.replace(".aar", ".jar")));
                preBinary = aarToJar(preBinary);
            }
            config.setPreBinary(preBinary);
        }

        String postBinary = cmd.getOptionValue(OPTION_POST_BINARY);
        if (postBinary != null && !postBinary.isEmpty()) {
            if (postBinary.endsWith(".aar")) {
                logger.info(String.format("Convert post-patched binary %s to %s",
                        postBinary, postBinary.replace(".aar", ".jar")));
                postBinary = aarToJar(postBinary);
            }
            config.setPostBinary(postBinary);
        }

        String androidJAR = cmd.getOptionValue(OPTION_ANDROID_JAR);
        if (androidJAR != null && !androidJAR.isEmpty())
            config.setAndroidPlatformJar(androidJAR);

        String threadNumber = cmd.getOptionValue(OPTION_THREAD_NUMBER);
        if (threadNumber != null && !threadNumber.isEmpty())
            config.setThreadNumber(threadNumber);

        String patch = cmd.getOptionValue(OPTION_PATCH_FILE);
        if (patch != null && !patch.isEmpty())
            config.setPatchFiles(patch);

        if (cmd.hasOption(OPTION_ENABLE_DEBUG)) {
            String debug = cmd.getOptionValue(OPTION_ENABLE_DEBUG);
            if (debug != null)
                config.setEnableDebugLevel(Boolean.parseBoolean(debug));
        } else {
            config.setEnableDebugLevel(true); // default do not enable debug level
        }
    }
}
