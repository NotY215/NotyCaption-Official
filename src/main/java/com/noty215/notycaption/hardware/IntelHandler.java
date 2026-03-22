package com.noty215.notycaption.hardware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for Intel GPU monitoring
 */
public class IntelHandler {

    private static final Logger logger = LoggerFactory.getLogger(IntelHandler.class);
    private static boolean intelAvailable = false;

    static {
        try {
            // Check for Intel GPU via system info
            // This is a placeholder - actual detection requires platform-specific code
            intelAvailable = System.getProperty("os.name").toLowerCase().contains("windows");
            if (intelAvailable) {
                logger.info("Intel GPU monitoring available (Windows)");
            } else {
                logger.debug("Intel GPU monitoring not available on this platform");
            }
        } catch (Exception e) {
            intelAvailable = false;
            logger.debug("Intel GPU not available: {}", e.getMessage());
        }
    }

    public static boolean isAvailable() {
        return intelAvailable;
    }

    public static String getGPUInfo() {
        if (!intelAvailable) return "";

        try {
            // On Windows, use WMI
            Process process = Runtime.getRuntime().exec("wmic path win32_videocontroller get name,adapterram");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            boolean intelFound = false;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Intel")) {
                    intelFound = true;
                    output.append(line).append("\n");
                }
            }
            reader.close();
            process.waitFor();

            if (intelFound) {
                return output.toString();
            }
        } catch (Exception e) {
            logger.error("Failed to get Intel GPU info", e);
        }
        return "";
    }

    public static String getGPUName() {
        if (!intelAvailable) return "";

        try {
            Process process = Runtime.getRuntime().exec("wmic path win32_videocontroller where name like '%Intel%' get name");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Intel")) {
                    reader.close();
                    process.waitFor();
                    return line.trim();
                }
            }
            reader.close();
            process.waitFor();
        } catch (Exception e) {
            logger.error("Failed to get Intel GPU name", e);
        }
        return "";
    }

    public static long getGPUMemory() {
        if (!intelAvailable) return 0;

        try {
            Process process = Runtime.getRuntime().exec("wmic path win32_videocontroller where name like '%Intel%' get adapterram");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.matches("\\d+")) {
                    long memory = Long.parseLong(line.trim());
                    reader.close();
                    process.waitFor();
                    return memory;
                }
            }
            reader.close();
            process.waitFor();
        } catch (Exception e) {
            logger.error("Failed to get Intel GPU memory", e);
        }
        return 0;
    }

    private static class BufferedReader {
        private final java.io.BufferedReader reader;

        public BufferedReader(java.io.Reader reader) {
            this.reader = new java.io.BufferedReader(reader);
        }

        public String readLine() throws java.io.IOException {
            return reader.readLine();
        }

        public void close() throws java.io.IOException {
            reader.close();
        }
    }
}