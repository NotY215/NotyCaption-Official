package com.noty215.notycaption.hardware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Handler for AMD GPU monitoring
 */
public class AMDHandler {

    private static final Logger logger = LoggerFactory.getLogger(AMDHandler.class);
    private static boolean amdAvailable = false;

    static {
        try {
            // Check for rocm-smi
            Process process = Runtime.getRuntime().exec("rocm-smi --version");
            int exitCode = process.waitFor();
            amdAvailable = (exitCode == 0);
            if (amdAvailable) {
                logger.info("AMD GPU monitoring available");
            } else {
                logger.debug("AMD GPU not available");
            }
        } catch (Exception e) {
            amdAvailable = false;
            logger.debug("AMD GPU not available: {}", e.getMessage());
        }
    }

    public static boolean isAvailable() {
        return amdAvailable;
    }

    public static String getGPUs() {
        if (!amdAvailable) return "";

        try {
            Process process = Runtime.getRuntime().exec("rocm-smi --showproductname --showmemuse --showuse --showtemp");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            reader.close();
            process.waitFor();

            return output.toString();
        } catch (Exception e) {
            logger.error("Failed to get AMD GPU info", e);
            return "";
        }
    }

    public static String getGPUName(int index) {
        if (!amdAvailable) return "";

        try {
            Process process = Runtime.getRuntime().exec("rocm-smi --showproductname");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            int currentIndex = 0;
            while ((line = reader.readLine()) != null) {
                if (line.contains("GPU") && line.contains("Product Name:")) {
                    if (currentIndex == index) {
                        String name = line.substring(line.indexOf("Product Name:") + 13).trim();
                        reader.close();
                        process.waitFor();
                        return name;
                    }
                    currentIndex++;
                }
            }
            reader.close();
            process.waitFor();
        } catch (Exception e) {
            logger.error("Failed to get AMD GPU name", e);
        }
        return "";
    }

    public static long getGPUMemoryUsed(int index) {
        if (!amdAvailable) return 0;

        try {
            Process process = Runtime.getRuntime().exec("rocm-smi --showmemuse");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            int currentIndex = 0;
            while ((line = reader.readLine()) != null) {
                if (line.contains("GPU") && line.contains("Memory Used:")) {
                    if (currentIndex == index) {
                        String memStr = line.substring(line.indexOf("Memory Used:") + 12).trim();
                        if (memStr.contains("MB")) {
                            long memMB = Long.parseLong(memStr.replace("MB", "").trim());
                            reader.close();
                            process.waitFor();
                            return memMB * 1024 * 1024;
                        }
                    }
                    currentIndex++;
                }
            }
            reader.close();
            process.waitFor();
        } catch (Exception e) {
            logger.error("Failed to get AMD GPU memory", e);
        }
        return 0;
    }

    public static double getGPUUtilization(int index) {
        if (!amdAvailable) return 0;

        try {
            Process process = Runtime.getRuntime().exec("rocm-smi --showuse");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            int currentIndex = 0;
            while ((line = reader.readLine()) != null) {
                if (line.contains("GPU") && line.contains("Use:")) {
                    if (currentIndex == index) {
                        String useStr = line.substring(line.indexOf("Use:") + 4).trim();
                        if (useStr.contains("%")) {
                            double usage = Double.parseDouble(useStr.replace("%", "").trim());
                            reader.close();
                            process.waitFor();
                            return usage;
                        }
                    }
                    currentIndex++;
                }
            }
            reader.close();
            process.waitFor();
        } catch (Exception e) {
            logger.error("Failed to get AMD GPU utilization", e);
        }
        return 0;
    }

    public static double getGPUTemperature(int index) {
        if (!amdAvailable) return 0;

        try {
            Process process = Runtime.getRuntime().exec("rocm-smi --showtemp");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            int currentIndex = 0;
            while ((line = reader.readLine()) != null) {
                if (line.contains("GPU") && line.contains("Temperature:")) {
                    if (currentIndex == index) {
                        String tempStr = line.substring(line.indexOf("Temperature:") + 12).trim();
                        if (tempStr.contains("°C")) {
                            double temp = Double.parseDouble(tempStr.replace("°C", "").trim());
                            reader.close();
                            process.waitFor();
                            return temp;
                        }
                    }
                    currentIndex++;
                }
            }
            reader.close();
            process.waitFor();
        } catch (Exception e) {
            logger.error("Failed to get AMD GPU temperature", e);
        }
        return 0;
    }
}