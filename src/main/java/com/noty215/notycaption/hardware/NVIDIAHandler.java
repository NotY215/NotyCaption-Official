package com.noty215.notycaption.hardware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Handler for NVIDIA GPU monitoring
 */
public class NVIDIAHandler {

    private static final Logger logger = LoggerFactory.getLogger(NVIDIAHandler.class);
    private static boolean nvidiaAvailable = false;

    static {
        try {
            Process process = Runtime.getRuntime().exec("nvidia-smi --version");
            int exitCode = process.waitFor();
            nvidiaAvailable = (exitCode == 0);
            if (nvidiaAvailable) {
                logger.info("NVIDIA GPU monitoring available");
            } else {
                logger.debug("NVIDIA GPU not available");
            }
        } catch (Exception e) {
            nvidiaAvailable = false;
            logger.debug("NVIDIA GPU not available: {}", e.getMessage());
        }
    }

    public static boolean isAvailable() {
        return nvidiaAvailable;
    }

    public static String getGPUs() {
        if (!nvidiaAvailable) return "";

        try {
            Process process = Runtime.getRuntime().exec(
                    "nvidia-smi --query-gpu=name,memory.total,memory.used,memory.free," +
                            "temperature.gpu,utilization.gpu,power.draw,clocks.current.graphics," +
                            "clocks.current.memory,driver_version --format=csv,noheader,nounits"
            );

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
            logger.error("Failed to get NVIDIA GPU info", e);
            return "";
        }
    }

    public static String getGPUName() {
        if (!nvidiaAvailable) return "";

        try {
            Process process = Runtime.getRuntime().exec("nvidia-smi --query-gpu=name --format=csv,noheader");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String name = reader.readLine();
            reader.close();
            process.waitFor();
            return name != null ? name.trim() : "";
        } catch (Exception e) {
            logger.error("Failed to get NVIDIA GPU name", e);
            return "";
        }
    }

    public static long getGPUMemoryTotal() {
        if (!nvidiaAvailable) return 0;

        try {
            Process process = Runtime.getRuntime().exec("nvidia-smi --query-gpu=memory.total --format=csv,noheader,nounits");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            reader.close();
            process.waitFor();

            if (line != null) {
                return (long) (Double.parseDouble(line.trim()) * 1024 * 1024);
            }
        } catch (Exception e) {
            logger.error("Failed to get NVIDIA GPU memory", e);
        }
        return 0;
    }

    public static double getGPUTemperature() {
        if (!nvidiaAvailable) return 0;

        try {
            Process process = Runtime.getRuntime().exec("nvidia-smi --query-gpu=temperature.gpu --format=csv,noheader,nounits");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            reader.close();
            process.waitFor();

            if (line != null) {
                return Double.parseDouble(line.trim());
            }
        } catch (Exception e) {
            logger.error("Failed to get NVIDIA GPU temperature", e);
        }
        return 0;
    }

    public static double getGPUUtilization() {
        if (!nvidiaAvailable) return 0;

        try {
            Process process = Runtime.getRuntime().exec("nvidia-smi --query-gpu=utilization.gpu --format=csv,noheader,nounits");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            reader.close();
            process.waitFor();

            if (line != null) {
                return Double.parseDouble(line.trim());
            }
        } catch (Exception e) {
            logger.error("Failed to get NVIDIA GPU utilization", e);
        }
        return 0;
    }

    public static double getGPUPowerDraw() {
        if (!nvidiaAvailable) return 0;

        try {
            Process process = Runtime.getRuntime().exec("nvidia-smi --query-gpu=power.draw --format=csv,noheader,nounits");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            reader.close();
            process.waitFor();

            if (line != null) {
                return Double.parseDouble(line.trim());
            }
        } catch (Exception e) {
            logger.error("Failed to get NVIDIA GPU power draw", e);
        }
        return 0;
    }

    public static int getGPUFanSpeed() {
        if (!nvidiaAvailable) return 0;

        try {
            Process process = Runtime.getRuntime().exec("nvidia-smi --query-gpu=fan.speed --format=csv,noheader,nounits");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            reader.close();
            process.waitFor();

            if (line != null) {
                return (int) Double.parseDouble(line.trim());
            }
        } catch (Exception e) {
            logger.error("Failed to get NVIDIA GPU fan speed", e);
        }
        return 0;
    }
}