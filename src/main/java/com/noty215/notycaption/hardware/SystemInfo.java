package com.noty215.notycaption.hardware;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class SystemInfo {
    private static final Logger logger = Logger.getLogger(SystemInfo.class.getName());

    public static String getOSName() {
        return System.getProperty("os.name");
    }

    public static String getOSVersion() {
        return System.getProperty("os.version");
    }

    public static String getOSArchitecture() {
        return System.getProperty("os.arch");
    }

    public static String getJavaVersion() {
        return System.getProperty("java.version");
    }

    public static String getJavaVendor() {
        return System.getProperty("java.vendor");
    }

    public static long getTotalMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    public static long getFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    public static long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    public static int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static Map<String, String> getEnvironmentVariables() {
        return System.getenv();
    }

    public static String getHostName() {
        try {
            Process process = Runtime.getRuntime().exec("hostname");
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String hostname = reader.readLine();
            process.waitFor();
            return hostname != null ? hostname.trim() : "Unknown";
        } catch (Exception e) {
            return System.getenv("COMPUTERNAME");
        }
    }

    public static String getUserName() {
        return System.getProperty("user.name");
    }

    public static String getUserHome() {
        return System.getProperty("user.home");
    }

    public static String getUserDir() {
        return System.getProperty("user.dir");
    }

    public static Map<String, String> getSystemProperties() {
        return new HashMap<>(System.getProperties());
    }

    public static boolean isWindows() {
        return getOSName().toLowerCase().contains("win");
    }

    public static boolean isMac() {
        return getOSName().toLowerCase().contains("mac");
    }

    public static boolean isLinux() {
        return getOSName().toLowerCase().contains("nix") ||
               getOSName().toLowerCase().contains("nux") ||
               getOSName().toLowerCase().contains("aix");
    }

    public static boolean is64Bit() {
        return getOSArchitecture().contains("64");
    }

    public static String getSystemSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("OS: ").append(getOSName()).append(" ").append(getOSVersion()).append("\n");
        sb.append("Architecture: ").append(getOSArchitecture()).append("\n");
        sb.append("Java: ").append(getJavaVendor()).append(" ").append(getJavaVersion()).append("\n");
        sb.append("Processors: ").append(getAvailableProcessors()).append("\n");
        sb.append("Memory: ").append(getFreeMemory() / (1024 * 1024)).append("MB / ")
          .append(getTotalMemory() / (1024 * 1024)).append("MB (Max: ")
          .append(getMaxMemory() / (1024 * 1024)).append("MB)\n");
        sb.append("User: ").append(getUserName()).append("@").append(getHostName()).append("\n");
        return sb.toString();
    }
}