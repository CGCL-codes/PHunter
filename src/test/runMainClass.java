package test;

import org.junit.Test;
import signTPL.MainClass;

public class runMainClass {


    @Test
    public void runSamples1() throws Exception {
        String[] args = {"--preTPL",
                "sample/retrofit-2.4.1-pre.jar",
                "--postTPL",
                "sample/retrofit-2.4.1-post.jar",
                "--threadNum",
                "10",
                "--androidJar",
                "D:\\AndroidSDK\\platforms\\android-31\\android.jar",
//                "E:\\Android_SDK\\platforms\\android-30\\android.jar",
                "--patchFiles",
                "samples/CVE-2018-1000850_b9a7f6ad72073ddd40254c0058710e87a073047d.diff",
                "--targetAPK",
//                "samples/"
                "sample/de.devmil.muzei.bingimageofthedayartsource-allatori.apk"};
//                "C:\\Users\\xzf\\Desktop\\apks\\dasho\\apk\\com.xabber.android.apk"};
//                "samples/de.devmil.muzei.bingimageofthedayartsource-dasho.apk"};
        MainClass.main(args);
    }


    @Test
    public void runSamples2() throws Exception {
        String[] args = {"--preTPL",
                "sample2/commons-compress-CVE-2018-1324-pre.jar",
                "--postTPL",
                "sample2/commons-compress-CVE-2018-1324-post.jar",
                "--threadNum",
                "10",
                "--androidJar",
                "D:\\AndroidSDK\\platforms\\android-31\\android.jar",
                "--patchFiles",
                "sample2/CVE-2018-1324_2a2f1dc48e22a34ddb72321a4db211da91aa933b.diff",
                "--targetAPK",
                "sample2/com.greenaddress.abcore-dasho.apk"};
        MainClass.main(args);
    }

    @Test
    public void runSamples3() throws Exception {
        String[] args = {"--preTPL",
                "sample3/netty-CVE-2014-3488-pre.jar",
                "--postTPL",
                "sample3/netty-CVE-2014-3488-post.jar",
                "--threadNum",
                "10",
                "--androidJar",
                "D:\\AndroidSDK\\platforms\\android-31\\android.jar",
                "--patchFiles",
                "sample3/CVE-2014-3488_2fa9400a59d0563a66908aba55c41e7285a04994.diff",
                "--targetAPK",
                "sample3/de.vier_bier.habpanelviewer-proguard.apk"};
        MainClass.main(args);
    }

    @Test
    public void runSamples4() throws Exception {
        String[] args = {"--preTPL",
                "sample4/bcprov-jdk15on-CVE-2016-1000352-pre.jar",
                "--postTPL",
                "sample4/bcprov-jdk15on-CVE-2016-1000352-post.jar",
                "--threadNum",
                "10",
                "--androidJar",
                "D:\\AndroidSDK\\platforms\\android-31\\android.jar",
                "--patchFiles",
                "sample4/9385b0ebd277724b167fe1d1456e3c112112be1f.diff",
                "--targetAPK",
                "sample4/com.kunzisoft.keepass.libre.apk"};
//                "E:\\apps_dataset/apks\\net.schueller.peertube.apk"};
        MainClass.main(args);
    }

    @Test
    public void runSamples5() throws Exception {
        String[] args = {"--preTPL",
                "sample5/androidsvg-CVE-2017-1000498-pre.aar",
                "--postTPL",
                "sample5/androidsvg-CVE-2017-1000498-post.aar",
                "--threadNum",
                "10",
                "--androidJar",
                "D:\\AndroidSDK\\platforms\\android-31\\android.jar",
                "--patchFiles",
                "sample5/CVE-2017-1000498_44e4fbf1d0f6db295df34601972741d4cf706cbd.diff",
                "--targetAPK",
                "sample5/ch.bailu.aat-dasho.apk"};
        MainClass.main(args);
    }

    @Test
    public void runSamples6() throws Exception {
        String[] args = {"--preTPL",
                "sample6/retrofit-CVE-2018-1000850-pre.jar",
                "--postTPL",
                "sample6/retrofit-CVE-2018-1000850-post.jar",
                "--threadNum",
                "10",
                "--androidJar",
                "D:\\AndroidSDK\\platforms\\android-31\\android.jar",
                "--patchFiles",
                "sample6/CVE-2018-1000850_b9a7f6ad72073ddd40254c0058710e87a073047d.diff",
                "--targetAPK",
                "sample6/fr.nuage.souvenirs-dasho.apk"};
        MainClass.main(args);
    }
    @Test
    public void runSamples7() throws Exception {
        String[] args = {"--preTPL",
                "sample7/jsoup-CVE-2015-6748-pre.jar",
                "--postTPL",
                "sample7/jsoup-CVE-2015-6748-post.jar",
                "--threadNum",
                "10",
                "--androidJar",
                "D:\\AndroidSDK\\platforms\\android-31\\android.jar",
                "--patchFiles",
                "sample7/CVE-2015-6748_4edb78991f8d0bf87dafde5e01ccd8922065c9b2.diff",
                "--targetAPK",
                "sample7/com.adonai.manman-dasho.apk"};
        MainClass.main(args);
    }
    @Test
    public void runSamples8() throws Exception {
        String[] args = {"--preTPL",
                "sample8/retrofit-CVE-2018-1000850-pre.jar",
                "--postTPL",
                "sample8/retrofit-CVE-2018-1000850-post.jar",
                "--threadNum",
                "10",
                "--androidJar",
                "D:\\AndroidSDK\\platforms\\android-31\\android.jar",
                "--patchFiles",
                "sample8/CVE-2018-1000850_b9a7f6ad72073ddd40254c0058710e87a073047d.diff",
                "--targetAPK",
                "sample8/com.rtbishop.look4sat-dasho.apk"};
        MainClass.main(args);
    }

    @Test
    public void runSamples9() throws Exception {
        String[] args = {"--preTPL",
                "sample9/okhttp-CVE-2021-0341-pre.jar",
                "--postTPL",
                "sample9/okhttp-CVE-2021-0341-post.jar",
                "--threadNum",
                "10",
                "--androidJar",
                "E:\\Android_SDK\\platforms\\android-31\\android.jar",
                "--patchFiles",
                "sample9/CVE-2021-0341_f574ea2f5259d9040f264ddeb582fb1ce563f10c.diff",
                "--targetAPK",
                "C:\\Users\\A\\Desktop\\app-release-unsigned.apk"};
        MainClass.main(args);
    }

    @Test
    public void runSamples10() throws Exception {
        String[] args = {"--preTPL",
                "sample10/jackson-databind-CVE-2017-17485-pre.jar",
                "--postTPL",
                "sample10/jackson-databind-CVE-2017-17485-post.jar",
                "--threadNum",
                "10",
                "--androidJar",
                "E:\\Android_SDK\\platforms\\android-31\\android.jar",
                "--patchFiles",
                "sample10/CVE-2017-17485_2235894210c75f624a3d0cd60bfb0434a20a18bf.diff",
                "--targetAPK",
                "C:\\Users\\A\\Desktop\\org.schabi.newpipe.apk"};
        MainClass.main(args);
    }

    @Test
    public void log4j2_CVE_2021_44228() throws Exception {
        String[] args = {"--preTPL",
                "log4j-samples/log4j-core-2.15.0-CVE-2021-44228-pre.jar",
                "--postTPL",
                "log4j-samples/log4j-core-2.15.0-CVE-2021-44228-post.jar",
                "--threadNum",
                "10",
                "--output",
                "res/",
                "--androidJar",
                "D:\\AndroidSDK\\platforms\\android-31\\android.jar",
                "--patchFiles",
                "log4j-samples/CVE-2021-44228-Restrict_LDAP_access_via_JNDI_(#608).diff",
                "--targetAPK",
                "log4j-samples/edu.sharif.ce.apyugioh-dasho.apk"};
        MainClass.main(args);
    }


    @Test
    public void log4j2_CVE_2021_45046() throws Exception {
        String[] args = {"--preTPL",
                "log4j-samples/log4j-core-2.16.0-CVE-2021-45046-pre.jar",
                "--postTPL",
                "log4j-samples/log4j-core-2.16.0-CVE-2021-45046-post.jar",
                "--threadNum",
                "10",
                "--output",
                "res/",
                "--androidJar",
                "D:\\AndroidSDK\\platforms\\android-31\\android.jar",
                "--patchFiles",
                "log4j-samples/CVE-2021-45046-LOG4J2-3208_-_Disable_JNDI_by_default.diff",
                "--targetAPK",
                "log4j-samples/edu.sharif.ce.apyugioh-dasho.apk"};
        MainClass.main(args);
    }

    @Test
    public void debug() throws Exception {
        String[] args = {"--preTPL",
                "C:\\Users\\xzf\\Desktop\\bcprov-jdk15on-CVE-2020-26939-pre.jar",
                "--postTPL",
                "C:\\Users\\xzf\\Desktop\\bcprov-jdk15on-CVE-2020-26939-post.jar",
                "--threadNum",
                "10",
                "--androidJar",
                "D:\\AndroidSDK\\platforms\\android-30\\android.jar",
                "--patchFiles",
                "C:\\Users\\xzf\\Desktop\\CVE-2020-26939_930f8b274c4f1f3a46e68b5441f1e7fadb57e8c1.diff",
                "--targetAPK",
                "C:\\Users\\xzf\\Desktop\\org.fdroid.nearby.apk"};
        MainClass.main(args);
    }


}
