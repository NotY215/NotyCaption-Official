package com.noty215.notycaption.hardware;

import com.noty215.notycaption.models.GPUInfo;
import com.noty215.notycaption.models.GPUType;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class NVIDIAHandler {
    private static final Logger logger = Logger.getLogger(NVIDIAHandler.class.getName());
    private static boolean nvmlInitialized = false;

    static {
        try {
            // Try to load NVML library
            System.loadLibrary("nvml");
            nvmlInit();
            nvmlInitialized = true;
            logger.info("NVML initialized successfully");
        } catch (UnsatisfiedLinkError e) {
            logger.fine("NVML not available: " + e.getMessage());
        }
    }

    public static List<GPUInfo> detectGPUs() {
        List<GPUInfo> gpus = new ArrayList<>();

        if (nvmlInitialized) {
            try {
                List<GPUInfo> nvmlGPUs = detectNVMLGPUs();
                gpus.addAll(nvmlGPUs);
            } catch (Exception e) {
                logger.warning("NVML GPU detection failed: " + e.getMessage());
            }
        }

        // Fallback to nvidia-smi
        try {
            List<GPUInfo> smiGPUs = detectNvidiaSMI();
            gpus.addAll(smiGPUs);
        } catch (Exception e) {
            logger.fine("nvidia-smi detection failed: " + e.getMessage());
        }

        return gpus;
    }

    private static native void nvmlInit();
    private static native int nvmlDeviceGetCount();
    private static native long nvmlDeviceGetHandleByIndex(int index);
    private static native String nvmlDeviceGetName(long handle);
    private static native String nvmlDeviceGetUUID(long handle);
    private static native String nvmlDeviceGetSerial(long handle);
    private static native long[] nvmlDeviceGetMemoryInfo(long handle);
    private static native int[] nvmlDeviceGetUtilizationRates(long handle);
    private static native int nvmlDeviceGetTemperature(long handle);
    private static native long nvmlDeviceGetPowerUsage(long handle);
    private static native long nvmlDeviceGetEnforcedPowerLimit(long handle);
    private static native int nvmlDeviceGetClockInfo(long handle, int clockType);
    private static native int nvmlDeviceGetMaxPcieLinkGeneration(long handle);
    private static native int nvmlDeviceGetMaxPcieLinkWidth(long handle);
    private static native int nvmlDeviceGetFanSpeed(long handle);
    private static native String nvmlSystemGetDriverVersion();

    private static List<GPUInfo> detectNVMLGPUs() {
        List<GPUInfo> gpus = new ArrayList<>();

        try {
            int deviceCount = nvmlDeviceGetCount();
            
            for (int i = 0; i < deviceCount; i++) {
                long handle = nvmlDeviceGetHandleByIndex(i);
                
                String name = nvmlDeviceGetName(handle);
                String uuid = nvmlDeviceGetUUID(handle);
                String serial = nvmlDeviceGetSerial(handle);
                
                long[] memoryInfo = nvmlDeviceGetMemoryInfo(handle);
                long memoryTotal = memoryInfo[0];
                long memoryUsed = memoryInfo[1];
                long memoryFree = memoryInfo[2];
                
                int[] utilization = nvmlDeviceGetUtilizationRates(handle);
                int gpuUtil = utilization[0];
                
                int temp = nvmlDeviceGetTemperature(handle);
                
                long powerUsage = nvmlDeviceGetPowerUsage(handle);
                long powerLimit = nvmlDeviceGetEnforcedPowerLimit(handle);
                
                int clockCore = nvmlDeviceGetClockInfo(handle, 0); // NVML_CLOCK_GRAPHICS
                int clockMemory = nvmlDeviceGetClockInfo(handle, 1); // NVML_CLOCK_MEM
                
                int pcieGen = nvmlDeviceGetMaxPcieLinkGeneration(handle);
                int pcieWidth = nvmlDeviceGetMaxPcieLinkWidth(handle);
                
                int fanSpeed = nvmlDeviceGetFanSpeed(handle);
                
                String driverVersion = nvmlSystemGetDriverVersion();
                
                GPUInfo gpu = new GPUInfo();
                gpu.setName(name);
                gpu.setType(GPUType.NVIDIA_CUDA);
                gpu.setVendor("NVIDIA");
                gpu.setMemoryTotal(memoryTotal);
                gpu.setMemoryUsed(memoryUsed);
                gpu.setMemoryFree(memoryFree);
                gpu.setTemperature(temp);
                gpu.setUtilization(gpuUtil);
                gpu.setPowerUsage(powerUsage);
                gpu.setClockCore(clockCore);
                gpu.setClockMemory(clockMemory);
                gpu.setDriverVersion(driverVersion);
                gpu.setPcieVersion("PCIe Gen" + pcieGen);
                gpu.setPcieLanes(pcieWidth);
                gpu.setUuid(uuid);
                gpu.setSerial(serial);
                gpu.setFanSpeed(fanSpeed);
                gpu.setPowerLimit((double) powerLimit);
                
                gpus.add(gpu);
                logger.info("NVIDIA GPU detected via NVML: " + gpu.getName());
            }
        } catch (Exception e) {
            logger.warning("Error in NVML detection: " + e.getMessage());
        }

        return gpus;
    }

    private static List<GPUInfo> detectNvidiaSMI() {
        List<GPUInfo> gpus = new ArrayList<>();

        try {
            Process process = Runtime.getRuntime().exec(new String[]{
                "nvidia-smi",
                "--query-gpu=name,memory.total,memory.used,memory.free,temperature.gpu,utilization.gpu,power.draw,clocks.current.graphics,clocks.current.memory,driver_version",
                "--format=csv,noheader,nounits"
            });
            
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                String[] parts = line.split(",");
                if (parts.length >= 9) {
                    GPUInfo gpu = new GPUInfo();
                    gpu.setName(parts[0].trim());
                    gpu.setType(GPUType.NVIDIA_CUDA);
                    gpu.setVendor("NVIDIA");
                    
                    long memoryTotal = (long) (Double.parseDouble(parts[1].trim()) * 1024 * 1024);
                    long memoryUsed = (long) (Double.parseDouble(parts[2].trim()) * 1024 * 1024);
                    long memoryFree = (long) (Double.parseDouble(parts[3].trim()) * 1024 * 1024);
                    
                    gpu.setMemoryTotal(memoryTotal);
                    gpu.setMemoryUsed(memoryUsed);
                    gpu.setMemoryFree(memoryFree);
                    gpu.setTemperature(Double.parseDouble(parts[4].trim()));
                    gpu.setUtilization(Double.parseDouble(parts[5].trim()));
                    gpu.setPowerUsage(Double.parseDouble(parts[6].trim()) * 1000);
                    gpu.setClockCore(Integer.parseInt(parts[7].trim()));
                    gpu.setClockMemory(Integer.parseInt(parts[8].trim()));
                    
                    if (parts.length > 9) {
                        gpu.setDriverVersion(parts[9].trim());
                    }
                    
                    gpus.add(gpu);
                    logger.info("NVIDIA GPU detected via nvidia-smi: " + gpu.getName());
                }
            }
            
            process.waitFor();
        } catch (Exception e) {
            logger.warning("Error in nvidia-smi detection: " + e.getMessage());
        }

        return gpus;
    }
}