package com.noty215.notycaption.hardware;

import com.noty215.notycaption.models.*;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

public class HardwareMonitor {
    private static HardwareMonitor instance;
    private List<GPUInfo> gpus;
    private CPUInfo cpu;
    private RAMInfo ram;
    private List<DiskInfo> disks;
    private List<NetworkInfo> networks;
    private BatteryInfo battery;

    private Queue<HardwareSnapshot> history;
    private List<HardwareSnapshot> snapshots;
    private boolean monitoring;
    private Thread monitorThread;
    private volatile boolean stopMonitoring;
    private int maxHistorySize = 3600;

    private static final Logger logger = Logger.getLogger(HardwareMonitor.class.getName());

    private HardwareMonitor() {
        gpus = new ArrayList<>();
        disks = new ArrayList<>();
        networks = new ArrayList<>();
        history = new ConcurrentLinkedQueue<>();
        snapshots = new ArrayList<>();

        detectAll();
        logger.info("Hardware monitor initialized");
    }

    public static synchronized HardwareMonitor getInstance() {
        if (instance == null) {
            instance = new HardwareMonitor();
        }
        return instance;
    }

    public void detectAll() {
        detectCPU();
        detectRAM();
        detectDisks();
        detectNetworks();
        detectBattery();
        detectGPUs();

        logger.info(String.format("Hardware detection complete: %d GPU(s), %d disk(s), %d network(s)",
                gpus.size(), disks.size(), networks.size()));
    }

    private void detectGPUs() {
        gpus.clear();

        // Try NVIDIA detection
        List<GPUInfo> nvidiaGPUs = NVIDIAHandler.detectGPUs();
        gpus.addAll(nvidiaGPUs);

        // Try AMD detection
        List<GPUInfo> amdGPUs = AMDHandler.detectGPUs();
        gpus.addAll(amdGPUs);

        // Try Intel detection
        List<GPUInfo> intelGPUs = IntelHandler.detectGPUs();
        gpus.addAll(intelGPUs);

        // Remove duplicates by name and vendor
        Set<String> seen = new HashSet<>();
        List<GPUInfo> unique = new ArrayList<>();
        for (GPUInfo gpu : gpus) {
            String key = gpu.getName() + "_" + gpu.getVendor();
            if (!seen.contains(key)) {
                seen.add(key);
                unique.add(gpu);
            }
        }
        gpus = unique;

        if (gpus.isEmpty()) {
            GPUInfo mockGpu = new GPUInfo();
            mockGpu.setName("No GPU Detected");
            mockGpu.setType(GPUType.SOFTWARE);
            mockGpu.setVendor("Unknown");
            mockGpu.setMemoryTotal(0);
            mockGpu.setMemoryUsed(0);
            mockGpu.setMemoryFree(0);
            mockGpu.setTemperature(0);
            mockGpu.setUtilization(0);
            mockGpu.setPowerUsage(0);
            gpus.add(mockGpu);
        }
    }

    private void detectCPU() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        cpu = new CPUInfo();
        cpu.setName(System.getProperty("os.arch"));
        cpu.setVendor("Unknown");
        cpu.setArchitecture(System.getProperty("os.arch"));
        cpu.setCoresPhysical(Runtime.getRuntime().availableProcessors());
        cpu.setCoresLogical(Runtime.getRuntime().availableProcessors());
        cpu.setThreads(Runtime.getRuntime().availableProcessors());
        cpu.setBaseFrequency(2000.0);
        cpu.setMaxFrequency(3000.0);
        cpu.setCurrentFrequency(2500.0);
        cpu.setTemperature(40.0);
        cpu.setUtilization(getSystemCpuLoad());
        cpu.setPowerUsage(65.0);
        cpu.setCacheL1(32 * 1024);
        cpu.setCacheL2(256 * 1024);
        cpu.setCacheL3(8 * 1024 * 1024);

        logger.info("CPU detected: " + cpu.getName());
    }

    private double getSystemCpuLoad() {
        try {
            com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            // Use getCpuLoad() which is not deprecated
            return osBean.getCpuLoad() * 100;
        } catch (Exception e) {
            return 25.0;
        }
    }

    private void detectRAM() {
        try {
            com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

            ram = new RAMInfo();
            ram.setTotal(osBean.getTotalMemorySize());
            ram.setFree(osBean.getFreeMemorySize());
            ram.setAvailable(osBean.getFreeMemorySize());
            ram.setUsed(ram.getTotal() - ram.getFree());
            ram.setUtilization((double) ram.getUsed() / ram.getTotal() * 100);

            logger.info(String.format("RAM detected: %.1f GB", ram.getTotal() / (1024.0 * 1024 * 1024)));
        } catch (Exception e) {
            ram = new RAMInfo();
            ram.setTotal(8L * 1024 * 1024 * 1024);
            ram.setFree(4L * 1024 * 1024 * 1024);
            ram.setUsed(4L * 1024 * 1024 * 1024);
            ram.setUtilization(50.0);
            logger.warning("Using default RAM info");
        }
    }

    private void detectDisks() {
        disks.clear();

        File[] roots = File.listRoots();
        if (roots != null) {
            for (File root : roots) {
                DiskInfo disk = new DiskInfo();
                disk.setDevice(root.getPath());
                disk.setMountpoint(root.getPath());
                disk.setFilesystem("Unknown");
                disk.setTotal(root.getTotalSpace());
                disk.setFree(root.getFreeSpace());
                disk.setUsed(root.getTotalSpace() - root.getFreeSpace());
                disk.setUtilization((double) disk.getUsed() / disk.getTotal() * 100);
                disks.add(disk);

                logger.info("Disk detected: " + disk.getDevice());
            }
        }
    }

    private void detectNetworks() {
        networks.clear();

        try {
            Enumeration<java.net.NetworkInterface> interfaces =
                    java.net.NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface netIf = interfaces.nextElement();
                try {
                    NetworkInfo net = new NetworkInfo();
                    net.setInterface(netIf.getName());
                    byte[] mac = netIf.getHardwareAddress();
                    if (mac != null) {
                        StringBuilder macBuilder = new StringBuilder();
                        for (byte b : mac) {
                            macBuilder.append(String.format("%02X:", b));
                        }
                        if (macBuilder.length() > 0) {
                            macBuilder.setLength(macBuilder.length() - 1);
                        }
                        net.setMacAddress(macBuilder.toString());
                    }
                    net.setIpAddresses(new ArrayList<>());
                    net.setLinkStatus(netIf.isUp());
                    net.setSpeed(1000);
                    net.setMtu(netIf.getMTU());

                    networks.add(net);
                    logger.info("Network interface detected: " + net.getInterface());
                } catch (Exception e) {
                    logger.fine("Error detecting network: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.warning("Error detecting networks: " + e.getMessage());
        }
    }

    private void detectBattery() {
        battery = null;
    }

    public void startMonitoring(int intervalMs) {
        if (monitoring) return;

        monitoring = true;
        stopMonitoring = false;
        monitorThread = new Thread(() -> monitorLoop(intervalMs));
        monitorThread.setDaemon(true);
        monitorThread.start();

        logger.info("Hardware monitoring started (interval: " + intervalMs + "ms)");
    }

    public void stopMonitoring() {
        monitoring = false;
        stopMonitoring = true;
        if (monitorThread != null) {
            try {
                monitorThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        logger.info("Hardware monitoring stopped");
    }

    private void monitorLoop(int intervalMs) {
        while (!stopMonitoring) {
            try {
                HardwareSnapshot snapshot = takeSnapshot();
                history.offer(snapshot);
                snapshots.add(snapshot);

                while (history.size() > maxHistorySize) {
                    history.poll();
                }

                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.warning("Monitoring error: " + e.getMessage());
            }
        }
    }

    public HardwareSnapshot takeSnapshot() {
        HardwareSnapshot snapshot = new HardwareSnapshot();
        snapshot.setTimestamp(LocalDateTime.now());

        snapshot.setCpuUsage(cpu != null ? cpu.getUtilization() : 0);
        snapshot.setCpuTemp(cpu != null ? cpu.getTemperature() : 0);
        snapshot.setCpuFreq(cpu != null ? cpu.getCurrentFrequency() : 0);
        snapshot.setCpuPower(cpu != null ? cpu.getPowerUsage() : 0);

        if (ram != null) {
            snapshot.setRamUsage(ram.getUtilization());
            snapshot.setRamAvailable(ram.getAvailable());
            snapshot.setSwapUsage(0);
        }

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

        Map<String, Double> diskUsage = new HashMap<>();
        Map<String, long[]> diskIO = new HashMap<>();

        for (DiskInfo disk : disks) {
            diskUsage.put(disk.getMountpoint(), disk.getUtilization());
            diskIO.put(disk.getMountpoint(), new long[]{0, 0});
        }

        snapshot.setDiskUsage(diskUsage);
        snapshot.setDiskIO(diskIO);

        snapshot.setNetworkIO(new long[]{0, 0});
        snapshot.setProcessCount(getProcessCount());
        snapshot.setThreadCount(getThreadCount());
        snapshot.setUptime(getSystemUptime());

        return snapshot;
    }

    private int getProcessCount() {
        try {
            return ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
        } catch (Exception e) {
            return 0;
        }
    }

    private int getThreadCount() {
        try {
            return Thread.activeCount();
        } catch (Exception e) {
            return 0;
        }
    }

    private long getSystemUptime() {
        try {
            return ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        } catch (Exception e) {
            return 0;
        }
    }

    public String getCpuSummary() {
        if (cpu == null) return "CPU: Unknown";

        return String.format("CPU: %s\n  Cores: %d physical, %d logical\n  Usage: %.1f%%\n  Temperature: %.0f°C\n  Frequency: %.0fMHz",
                cpu.getName(), cpu.getCoresPhysical(), cpu.getCoresLogical(),
                cpu.getUtilization(), cpu.getTemperature(), cpu.getCurrentFrequency());
    }

    public String getGpuSummary() {
        if (gpus.isEmpty()) return "No GPU detected";

        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (GPUInfo gpu : gpus) {
            sb.append(String.format("GPU %d: %s\n", i++, gpu.getName()));
            sb.append(String.format("  Type: %s\n", gpu.getType()));
            if (gpu.getMemoryTotal() > 0) {
                sb.append(String.format("  Memory: %.1fGB / %.1fGB\n",
                        gpu.getMemoryUsed() / (1024.0 * 1024 * 1024),
                        gpu.getMemoryTotal() / (1024.0 * 1024 * 1024)));
            }
            sb.append(String.format("  Temperature: %.0f°C\n", gpu.getTemperature()));
            sb.append(String.format("  Utilization: %.1f%%\n", gpu.getUtilization()));
            sb.append(String.format("  Power: %.1fW\n", gpu.getPowerUsage() / 1000.0));
        }
        return sb.toString();
    }

    public String getRamSummary() {
        if (ram == null) return "RAM: Unknown";

        return String.format("RAM: %.1fGB / %.1fGB (%.1f%%)\nSwap: 0.0GB / 0.0GB (0.0%%)",
                ram.getUsed() / (1024.0 * 1024 * 1024),
                ram.getTotal() / (1024.0 * 1024 * 1024),
                ram.getUtilization());
    }

    public List<GPUInfo> getGpus() { return new ArrayList<>(gpus); }
    public CPUInfo getCpu() { return cpu; }
    public RAMInfo getRam() { return ram; }
    public List<DiskInfo> getDisks() { return new ArrayList<>(disks); }
    public List<NetworkInfo> getNetworks() { return new ArrayList<>(networks); }
    public Queue<HardwareSnapshot> getHistory() { return history; }
    public List<HardwareSnapshot> getSnapshots() { return new ArrayList<>(snapshots); }
}