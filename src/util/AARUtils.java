package util;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class AARUtils {
    // handle aar, extract the class.jar from aar, rename it to aar_name.jar then return it
    public static String aarToJar(String aarPath) {
        if (!aarPath.endsWith("aar"))
            throw new RuntimeException("the input is not end with .aar");
        String jar = aarPath.replace(".aar", ".jar");
        File jarFile = new File(jar);
        if(jarFile.exists())
            return jar;
        try {
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(aarPath));
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                if (zipEntry.getName().equals("classes.jar")) {
                    BufferedOutputStream bufferedOutputStream =
                            new BufferedOutputStream(new FileOutputStream(jar));
                    byte[] bytes = new byte[1024];
                    int readLen;
                    while ((readLen = zipInputStream.read(bytes)) != -1) {
                        bufferedOutputStream.write(bytes, 0, readLen);
                    }
                    bufferedOutputStream.close();
                    break;
                }
                zipInputStream.closeEntry();
                zipEntry = zipInputStream.getNextEntry();
            }
            zipInputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return jar;
    }
}
