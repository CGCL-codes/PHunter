package analyze;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeRecorder {
    private static final Logger logger = LoggerFactory.getLogger(TimeRecorder.class);

    public static long beforePre;
    public static long afterPre;

    public static long beforePost;
    public static long afterPost;

    public static long beforeAPK;
    public static long afterAPK;

    public static long beforeMatching;
    public static long afterMatching;

    public static long beforeTotal;
    public static long afterTotal;

//    static {
////      hook addShutdownHook，like @AfterClass in junit
//        Runtime.getRuntime().addShutdownHook(new Thread(TimeRecorder::generateReport));
//    }

    public static void generateReport() {
        long pre = (afterPre - beforePre) / 1000;
        long post = (afterPost - beforePost) / 1000;
        long apk = (afterAPK - beforeAPK) / 1000;
        long match = (afterMatching - beforeMatching) / 1000;
        long total = (afterTotal - beforeTotal) / 1000;

        String sb = "total" + "\t" +
                "pre" + "\t" +
                "post" + "\t" +
                "apk" + "\t" +
                "match" + "\n" +
                total + "\t" +
                pre + "\t" +
                post + "\t" +
                apk + "\t" +
                match + "\n\n";
        logger.info(sb);
    }
}
