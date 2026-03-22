package com.noty215.notycaption.hardware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * System information utility
 */
public class SystemInfo {

    private static final Logger logger = LoggerFactory.getLogger(SystemInfo.class);

    public static String getOSName() {
        return System.getProperty("os.name");
    }

    public static String getOSArch() {
        return System.getProperty("os.arch");
    }

    public static String getOSVersion() {
        return System.getProperty("os.version");
    }

    public static String getJavaVersion() {
        return System.getProperty("java.version");
    }

    public static String getJavaVendor() {
        return System.getProperty("java.vendor");
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

    public static int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    public static long getTotalMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    public static long getFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    public static String getHostName() {
        try {
            Process process = Runtime.getRuntime().exec("hostname");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String hostname = reader.readLine();
            reader.close();
            process.waitFor();
            return hostname != null ? hostname.trim() : "localhost";
        } catch (Exception e) {
            logger.debug("Failed to get hostname", e);
            return "localhost";
        }
    }

    public static boolean isWindows() {
        return getOSName().toLowerCase().contains("windows");
    }

    public static boolean isMac() {
        return getOSName().toLowerCase().contains("mac");
    }

    public static boolean isLinux() {
        return getOSName().toLowerCase().contains("linux");
    }

    public static boolean is64Bit() {
        return getOSArch().contains("64") || System.getProperty("sun.arch.data.model").contains("64");
    }
}