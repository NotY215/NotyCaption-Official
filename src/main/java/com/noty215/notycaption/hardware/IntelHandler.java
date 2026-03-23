package com.noty215.notycaption.hardware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Handler for Intel GPU monitoring
 */
public class IntelHandler {

    private static final Logger logger = LoggerFactory.getLogger(IntelHandler.class);
    private static boolean intelAvailable = false;

    static {
        try {
            // Check for Intel GPU via system info
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
            ProcessBuilder pb = new ProcessBuilder("wmic", "path", "win32_videocontroller", "get", "name,adapterram");
            Process process = pb.start();
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
            ProcessBuilder pb = new ProcessBuilder("wmic", "path", "win32_videocontroller", "where", "name like '%Intel%'", "get", "name");
            Process process = pb.start();
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
            ProcessBuilder pb = new ProcessBuilder("wmic", "path", "win32_videocontroller", "where", "name like '%Intel%'", "get", "adapterram");
            Process process = pb.start();
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
}