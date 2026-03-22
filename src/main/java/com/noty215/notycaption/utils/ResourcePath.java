package com.noty215.notycaption.utils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility for getting resource paths in both development and packaged environments
 */
public class ResourcePath {

    private static boolean isPackaged = false;
    private static String basePath;

    static {
        try {
            // Check if running from JAR
            String jarPath = ResourcePath.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            isPackaged = jarPath.endsWith(".jar");

            if (isPackaged) {
                // Running from JAR - resources are in JAR root
                basePath = "";
            } else {
                // Running from IDE - resources are in src/main/resources
                basePath = "src/main/resources/";
            }
        } catch (URISyntaxException e) {
            basePath = "";
        }
    }

    /**
     * Get the path to a resource file
     * @param relativePath relative path from resources directory
     * @return absolute path to the resource
     */
    public static String getPath(String relativePath) {
        if (isPackaged) {
            // In JAR, resources are at root
            return relativePath;
        } else {
            return basePath + relativePath;
        }
    }

    /**
     * Get a File object for a resource
     * @param relativePath relative path from resources directory
     * @return File object
     */
    public static File getFile(String relativePath) {
        return new File(getPath(relativePath));
    }

    /**
     * Check if a resource exists
     * @param relativePath relative path from resources directory
     * @return true if resource exists
     */
    public static boolean exists(String relativePath) {
        return getFile(relativePath).exists();
    }

    /**
     * Get the base directory for application data
     * @return path to app data directory
     */
    public static String getAppDataDir() {
        return SettingsManager.getAppDataDir();
    }

    /**
     * Get the path to a file in the app data directory
     * @param filename name of the file
     * @return full path
     */
    public static String getAppDataFile(String filename) {
        return SettingsManager.getAppDataDir() + File.separator + filename;
    }

    /**
     * Get the path to the temporary directory
     * @return temp directory path
     */
    public static String getTempDir() {
        return System.getProperty("java.io.tmpdir");
    }

    /**
     * Create a temporary file with the given prefix and suffix
     * @param prefix file prefix
     * @param suffix file suffix (including dot)
     * @return File object
     * @throws IOException if creation fails
     */
    public static File createTempFile(String prefix, String suffix) throws IOException {
        return File.createTempFile(prefix, suffix);
    }

    /**
     * Create a temporary directory
     * @param prefix directory prefix
     * @return File object for the directory
     * @throws IOException if creation fails
     */
    public static File createTempDir(String prefix) throws IOException {
        File tempDir = File.createTempFile(prefix, "");
        tempDir.delete();
        tempDir.mkdir();
        return tempDir;
    }
}