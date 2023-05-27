package analyze;

public class Configuration {
    public boolean enableDebugLevel = false;
    private String targetAPKFile = "";
    private String androidPlatformJar = "";
    private String preBinary = "";
    private String postBinary = "";
    private String threadNumber = "";
    private String patchFiles = "";

    public Configuration() {
    }


    public String getPatchFiles() {
        return patchFiles;
    }

    public void setPatchFiles(String patchFiles) {
        this.patchFiles = patchFiles;
    }

    public int getThreadNumber() {
        return Integer.parseInt(threadNumber);
    }

    public void setThreadNumber(String threadNumber) {
        this.threadNumber = threadNumber;
    }

    public String getTargetAPKFile() {
        return targetAPKFile;
    }

    public void setTargetAPKFile(String targetAPKFile) {
        this.targetAPKFile = targetAPKFile;
    }

    public String getAndroidPlatformJar() {
        return androidPlatformJar;
    }

    public void setAndroidPlatformJar(String androidPlatformJar) {
        this.androidPlatformJar = androidPlatformJar;
    }

    public String getPreBinary() {
        return preBinary;
    }

    public void setPreBinary(String preBinary) {
        this.preBinary = preBinary;
    }

    public String getPostBinary() {
        return postBinary;
    }

    public void setPostBinary(String postBinary) {
        this.postBinary = postBinary;
    }

    public boolean isEnableDebugLevel() {
        return enableDebugLevel;
    }

    public void setEnableDebugLevel(boolean enableDebugLevel) {
        this.enableDebugLevel = enableDebugLevel;
    }
}
