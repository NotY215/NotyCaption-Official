package com.noty215.notycaption.hardware;

import com.noty215.notycaption.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.OperatingSystem;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Comprehensive hardware monitoring with all GPU support
 */
public class HardwareMonitor {

    private static final Logger logger = LoggerFactory.getLogger(HardwareMonitor.class);

    private final SystemInfo systemInfo;
    private final HardwareAbstractionLayer hal;
    private final OperatingSystem os;

    private List<GPUInfo> gpus;
    private CPUInfo cpu;
    private RAMInfo ram;
    private List<DiskInfo> disks;
    private List<NetworkInfo> networks;
    private BatteryInfo battery;

    private final Deque<HardwareSnapshot> history;
    private final List<HardwareSnapshot> snapshots;
    private boolean monitoring;
    private Thread monitorThread;
    private int monitorInterval;
    private volatile boolean stopMonitoring;

    public HardwareMonitor() {
        this.systemInfo = new SystemInfo();
        this.hal = systemInfo.getHardware();
        this.os = systemInfo.getOperatingSystem();

        this.gpus = new ArrayList<>();
        this.disks = new ArrayList<>();
        this.networks = new ArrayList<>();
        this.history = new ConcurrentLinkedDeque<>();
        this.snapshots = new ArrayList<>();
        this.monitoring = false;
        this.stopMonitoring = false;
        this.monitorInterval = 1000;

        detectAll();

        logger.info("Hardware monitor initialized");
    }

    private void detectAll() {
        detectCPU();
        detectRAM();
        detectDisks();
        detectNetworks();
        detectBattery();
        detectGPU();

        logger.info("Hardware detection complete: {} GPU(s), {} disk(s), {} network(s)",
                gpus.size(), disks.size(), networks.size());
    }

    private void detectCPU() {
        CentralProcessor processor = hal.getProcessor();

        cpu = new CPUInfo();
        cpu.setName(processor.getProcessorIdentifier().getName());
        cpu.setVendor(processor.getProcessorIdentifier().getVendor());
        cpu.setArchitecture(System.getProperty("os.arch"));
        cpu.setCoresPhysical(processor.getPhysicalProcessorCount());
        cpu.setCoresLogical(processor.getLogicalProcessorCount());
        cpu.setThreads(processor.getLogicalProcessorCount());

        long[] freqs = processor.getCurrentFreq();
        if (freqs.length > 0) {
            cpu.setCurrentFrequency(freqs[0] / 1_000_000.0);
        }
        cpu.setMaxFrequency(processor.getMaxFreq() / 1_000_000.0);

        List<String> features = new ArrayList<>();
        features.addAll(processor.getProcessorIdentifier().getFeatures());
        cpu.setInstructionsSets(features);

        cpu.setCacheL1(processor.getProcessorInfo().getL1CacheSize());
        cpu.setCacheL2(processor.getProcessorInfo().getL2CacheSize());
        cpu.setCacheL3(processor.getProcessorInfo().getL3CacheSize());

        logger.info("CPU detected: {} ({} cores)", cpu.getName(), cpu.getCoresPhysical());
    }

    private void detectRAM() {
        GlobalMemory memory = hal.getMemory();

        ram = new RAMInfo();
        ram.setTotal(memory.getTotal());
        ram.setAvailable(memory.getAvailable());
        ram.setUsed(memory.getTotal() - memory.getAvailable());
        ram.setFree(memory.getAvailable());

        if (memory.getPageSize() > 0) {
            ram.setSwapTotal(memory.getVirtualMemory().getSwapTotal());
            ram.setSwapUsed(memory.getVirtualMemory().getSwapUsed());
            ram.setSwapFree(memory.getVirtualMemory().getSwapTotal() - memory.getVirtualMemory().getSwapUsed());
        }

        ram.setUtilization((double) ram.getUsed() / ram.getTotal() * 100);
        if (ram.getSwapTotal() > 0) {
            ram.setSwapUtilization((double) ram.getSwapUsed() / ram.getSwapTotal() * 100);
        }

        logger.info("RAM detected: {:.1f} GB", ram.getTotal() / (1024.0 * 1024 * 1024));
    }

    private void detectDisks() {
        disks.clear();
        List<HWDiskStore> diskStores = hal.getDiskStores();

        for (HWDiskStore disk : diskStores) {
            DiskInfo diskInfo = new DiskInfo();
            diskInfo.setDevice(disk.getName());
            diskInfo.setModel(disk.getModel());
            diskInfo.setSerial(disk.getSerial());
            diskInfo.setTotal(disk.getSize());
            diskInfo.setReadSpeed(disk.getReadBytes());
            diskInfo.setWriteSpeed(disk.getWriteBytes());

            disks.add(diskInfo);
            logger.info("Disk detected: {} ({:.1f} TB)", diskInfo.getDevice(),
                    diskInfo.getTotal() / (1024.0 * 1024 * 1024 * 1024));
        }
    }

    private void detectNetworks() {
        networks.clear();
        List<NetworkIF> networkIFs = hal.getNetworkIFs();

        for (NetworkIF net : networkIFs) {
            NetworkInfo netInfo = new NetworkInfo();
            netInfo.setInterface(net.getName());
            netInfo.setMacAddress(net.getMacaddr());
            netInfo.setIpAddresses(Arrays.asList(net.getIPv4addr()));
            netInfo.setSpeed(net.getSpeed() / 1_000_000);
            netInfo.setBytesSent(net.getBytesSent());
            netInfo.setBytesReceived(net.getBytesReceived());
            netInfo.setPacketsSent(net.getPacketsSent());
            netInfo.setPacketsReceived(net.getPacketsReceived());
            netInfo.setLinkStatus(net.isConnectorPresent());

            networks.add(netInfo);
            logger.info("Network interface detected: {}", netInfo.getInterface());
        }
    }

    private void detectBattery() {
        List<PowerSource> powerSources = hal.getPowerSources();
        if (!powerSources.isEmpty()) {
            PowerSource ps = powerSources.get(0);
            battery = new BatteryInfo();
            battery.setPresent(true);
            battery.setPercent(ps.getRemainingCapacityPercent() * 100);
            battery.setCharging(ps.isCharging());
            battery.setTimeRemaining(ps.getTimeRemaining());
            battery.setManufacturer(ps.getName());

            logger.info("Battery detected: {:.1f}%", battery.getPercent());
        } else {
            battery = null;
        }
    }

    private void detectGPU() {
        gpus.clear();

        // Try NVIDIA via nvidia-smi
        detectNvidiaGPU();

        // Try AMD via rocm-smi
        detectAMDGPU();

        // Try Intel via system info
        detectIntelGPU();

        // If no GPUs found, add placeholder
        if (gpus.isEmpty()) {
            GPUInfo softwareGPU = new GPUInfo();
            softwareGPU.setName("Software Renderer");
            softwareGPU.setType(GPUType.SOFTWARE);
            softwareGPU.setVendor("CPU");
            gpus.add(softwareGPU);
        }
    }

    private void detectNvidiaGPU() {
        try {
            Process process = Runtime.getRuntime().exec("nvidia-smi --query-gpu=name,memory.total,utilization.gpu,temperature.gpu --format=csv,noheader");
            process.waitFor();

            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    GPUInfo gpu = new GPUInfo();
                    gpu.setName(parts[0].trim());
                    gpu.setType(GPUType.NVIDIA_CUDA);
                    gpu.setVendor("NVIDIA");

                    String memStr = parts[1].trim();
                    if (memStr.contains("MiB")) {
                        long memMiB = Long.parseLong(memStr.replace("MiB", "").trim());
                        gpu.setMemoryTotal(memMiB * 1024 * 1024);
                    }

                    String utilStr = parts[2].trim();
                    if (utilStr.contains("%")) {
                        gpu.setUtilization(Double.parseDouble(utilStr.replace("%", "").trim()));
                    }

                    String tempStr = parts[3].trim();
                    if (tempStr.contains("°C")) {
                        gpu.setTemperature(Double.parseDouble(tempStr.replace("°C", "").trim()));
                    }

                    gpus.add(gpu);
                    logger.info("NVIDIA GPU detected: {}", gpu.getName());
                }
            }
            reader.close();
        } catch (Exception e) {
            logger.debug("No NVIDIA GPU detected: {}", e.getMessage());
        }
    }

    private void detectAMDGPU() {
        try {
            Process process = Runtime.getRuntime().exec("rocm-smi --showproductname --showmemuse --showuse --showtemp");
            process.waitFor();

            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("GPU")) {
                    GPUInfo gpu = new GPUInfo();
                    gpu.setType(GPUType.AMD_ROCM);
                    gpu.setVendor("AMD");

                    // Parse name
                    if (line.contains("Product Name:")) {
                        gpu.setName(line.substring(line.indexOf("Product Name:") + 13).trim());
                    }

                    // Parse memory
                    if (line.contains("Memory Used:")) {
                        String memStr = line.substring(line.indexOf("Memory Used:") + 12).trim();
                        if (memStr.contains("MB")) {
                            long memMB = Long.parseLong(memStr.replace("MB", "").trim());
                            gpu.setMemoryUsed(memMB * 1024 * 1024);
                        }
                    }

                    gpus.add(gpu);
                    logger.info("AMD GPU detected: {}", gpu.getName());
                }
            }
            reader.close();
        } catch (Exception e) {
            logger.debug("No AMD GPU detected: {}", e.getMessage());
        }
    }

    private void detectIntelGPU() {
        // Intel GPUs are typically integrated and detected via system info
        // This is a placeholder - actual detection would require additional libraries
        logger.debug("Intel GPU detection would require additional libraries");
    }

    public void startMonitoring(int interval) {
        if (monitoring) return;

        this.monitorInterval = interval;
        this.monitoring = true;
        this.stopMonitoring = false;

        monitorThread = new Thread(() -> {
            while (!stopMonitoring) {
                try {
                    takeSnapshot();
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Monitoring error", e);
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();

        logger.info("Hardware monitoring started (interval: {}ms)", interval);
    }

    public void stopMonitoring() {
        monitoring = false;
        stopMonitoring = true;
        if (monitorThread != null) {
            monitorThread.interrupt();
            try {
                monitorThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        logger.info("Hardware monitoring stopped");
    }

    private void takeSnapshot() {
        HardwareSnapshot snapshot = new HardwareSnapshot();
        snapshot.setTimestamp(LocalDateTime.now());

        // CPU
        CentralProcessor processor = hal.getProcessor();
        long[] prevTicks = processor.getSystemCpuLoadTicks();
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        long[] ticks = processor.getSystemCpuLoadTicks();

        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
        snapshot.setCpuUsage(cpuLoad);

        // CPU Temperature (if available)
        try {
            Process process = Runtime.getRuntime().exec("wmic /namespace:\\\\root\\wmi PATH MSAcpi_ThermalZoneTemperature get CurrentTemperature");
            process.waitFor();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.matches("\\d+")) {
                    double tempK = Double.parseDouble(line.trim()) / 10.0;
                    double tempC = tempK - 273.15;
                    snapshot.setCpuTemp(tempC);
                    break;
                }
            }
            reader.close();
        } catch (Exception e) {
            // Temperature not available
        }

        // CPU Frequency
        long[] freqs = processor.getCurrentFreq();
        if (freqs.length > 0) {
            snapshot.setCpuFreq(freqs[0] / 1_000_000.0);
        }

        // GPU
        List<Double> gpuUsage = new ArrayList<>();
        List<Double> gpuTemp = new ArrayList<>();
        List<Long> gpuMemory = new ArrayList<>();
        List<Double> gpuPower = new ArrayList<>();

        for (GPUInfo gpu : gpus) {
            gpuUsage.add(gpu.getUtilization());
            gpuTemp.add(gpu.getTemperature());
            gpuMemory.add(gpu.getMemoryUsed());
            gpuPower.add(gpu.getPowerUsage());
        }
        snapshot.setGpuUsage(gpuUsage);
        snapshot.setGpuTemp(gpuTemp);
        snapshot.setGpuMemory(gpuMemory);
        snapshot.setGpuPower(gpuPower);

        // RAM
        GlobalMemory memory = hal.getMemory();
        snapshot.setRamUsage((double) (memory.getTotal() - memory.getAvailable()) / memory.getTotal() * 100);
        snapshot.setRamAvailable(memory.getAvailable());

        if (memory.getPageSize() > 0) {
            snapshot.setSwapUsage((double) memory.getVirtualMemory().getSwapUsed() /
                    memory.getVirtualMemory().getSwapTotal() * 100);
        }

        // Disk
        Map<String, Double> diskUsage = new HashMap<>();
        Map<String, long[]> diskIo = new HashMap<>();
        for (HWDiskStore disk : hal.getDiskStores()) {
            diskUsage.put(disk.getName(), (double) disk.getReadBytes() / disk.getSize() * 100);
            diskIo.put(disk.getName(), new long[]{disk.getReadBytes(), disk.getWriteBytes()});
        }
        snapshot.setDiskUsage(diskUsage);
        snapshot.setDiskIo(diskIo);

        // Network
        long totalSent = 0, totalReceived = 0;
        for (NetworkIF net : hal.getNetworkIFs()) {
            net.updateAttributes();
            totalSent += net.getBytesSent();
            totalReceived += net.getBytesReceived();
        }
        snapshot.setNetworkIo(new long[]{totalSent, totalReceived});

        // Battery
        if (battery != null) {
            snapshot.setBatteryPercent(battery.getPercent());
        }

        // Process count
        snapshot.setProcessCount(hal.getProcessor().getLogicalProcessorCount());

        // Uptime
        snapshot.setUptime(hal.getProcessor().getSystemUptime());

        // Load average (Unix-like systems only)
        if (System.getProperty("os.name").toLowerCase().contains("nix") ||
                System.getProperty("os.name").toLowerCase().contains("nux") ||
                System.getProperty("os.name").toLowerCase().contains("mac")) {
            snapshot.setLoadAverage(new double[]{processor.getSystemLoadAverage(1),
                    processor.getSystemLoadAverage(5),
                    processor.getSystemLoadAverage(15)});
        }

        history.add(snapshot);
        snapshots.add(snapshot);

        // Limit history size
        while (history.size() > 3600) {
            history.removeFirst();
        }
        while (snapshots.size() > 10000) {
            snapshots.remove(0);
        }
    }

    public CPUInfo getCPUInfo() {
        if (cpu == null) detectCPU();
        updateCPU();
        return cpu;
    }

    public List<GPUInfo> getGPUInfo() {
        return gpus;
    }

    public RAMInfo getRAMInfo() {
        if (ram == null) detectRAM();
        updateRAM();
        return ram;
    }

    public List<DiskInfo> getDiskInfo() {
        return disks;
    }

    public List<NetworkInfo> getNetworkInfo() {
        return networks;
    }

    public BatteryInfo getBatteryInfo() {
        return battery;
    }

    public List<HardwareSnapshot> getHistory() {
        return new ArrayList<>(history);
    }

    public List<HardwareSnapshot> getSnapshots() {
        return new ArrayList<>(snapshots);
    }

    private void updateCPU() {
        CentralProcessor processor = hal.getProcessor();
        long[] prevTicks = processor.getSystemCpuLoadTicks();
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        long[] ticks = processor.getSystemCpuLoadTicks();

        cpu.setUtilization(processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100);

        long[] freqs = processor.getCurrentFreq();
        if (freqs.length > 0) {
            cpu.setCurrentFrequency(freqs[0] / 1_000_000.0);
        }

        // Try to get temperature
        try {
            Process process = Runtime.getRuntime().exec("wmic /namespace:\\\\root\\wmi PATH MSAcpi_ThermalZoneTemperature get CurrentTemperature");
            process.waitFor();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.matches("\\d+")) {
                    double tempK = Double.parseDouble(line.trim()) / 10.0;
                    double tempC = tempK - 273.15;
                    cpu.setTemperature(tempC);
                    break;
                }
            }
            reader.close();
        } catch (Exception e) {
            // Temperature not available
        }
    }

    private void updateRAM() {
        GlobalMemory memory = hal.getMemory();
        ram.setAvailable(memory.getAvailable());
        ram.setUsed(memory.getTotal() - memory.getAvailable());
        ram.setFree(memory.getAvailable());
        ram.setUtilization((double) ram.getUsed() / ram.getTotal() * 100);

        if (memory.getPageSize() > 0) {
            ram.setSwapUsed(memory.getVirtualMemory().getSwapUsed());
            ram.setSwapFree(memory.getVirtualMemory().getSwapTotal() - memory.getVirtualMemory().getSwapUsed());
            ram.setSwapUtilization((double) ram.getSwapUsed() / ram.getSwapTotal() * 100);
        }
    }

    public String getCPUInfoString() {
        return cpu != null ? cpu.toString() : "CPU: Unknown";
    }

    public String getGPUInfoString() {
        if (gpus.isEmpty()) return "No GPU detected";
        return gpus.get(0).toString();
    }

    public String getRAMInfoString() {
        return ram != null ? ram.toString() : "RAM: Unknown";
    }
}