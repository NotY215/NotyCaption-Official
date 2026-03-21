package com.noty215.notycaption.hardware;

import com.noty215.notycaption.models.*;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    
    private static final java.util.logging.Logger logger = 
        java.util.logging.Logger.getLogger(HardwareMonitor.class.getName());
    
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
        // Placeholder for GPU detection - would use JNA or native libraries
        gpus.clear();
        
        // Add sample GPU for testing
        GPUInfo gpu = new GPUInfo();
        gpu.setName("Sample GPU");
        gpu.setType(GPUType.NVIDIA_CUDA);
        gpu.setVendor("NVIDIA");
        gpu.setMemoryTotal(8L * 1024 * 1024 * 1024);
        gpu.setMemoryUsed(2L * 1024 * 1024 * 1024);
        gpu.setMemoryFree(6L * 1024 * 1024 * 1024);
        gpu.setTemperature(45.0);
        gpu.setUtilization(30.0);
        gpu.setPowerUsage(120.0);
        gpu.setClockCore(1500);
        gpu.setClockMemory(5000);
        gpu.setDriverVersion("535.104.05");
        gpus.add(gpu);
    }
    
    private void detectCPU() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        
        cpu = new CPUInfo();
        cpu.setName(System.getProperty("os.arch"));
        cpu.setVendor("Unknown");
        cpu.setArchitecture(System.getProperty("os.arch"));
        cpu.setCoresPhysical(Runtime.getRuntime().availableProcessors() / 2);
        cpu.setCoresLogical(Runtime.getRuntime().availableProcessors());
        cpu.setThreads(Runtime.getRuntime().availableProcessors());
        cpu.setBaseFrequency(2000.0);
        cpu.setMaxFrequency(3000.0);
        cpu.setCurrentFrequency(2500.0);
        cpu.setTemperature(40.0);
        cpu.setUtilization(25.0);
        cpu.setPowerUsage(65.0);
        cpu.setCacheL1(32 * 1024);
        cpu.setCacheL2(256 * 1024);
        cpu.setCacheL3(8 * 1024 * 1024);
        
        logger.info("CPU detected: " + cpu.getName());
    }
    
    private void detectRAM() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        com.sun.management.OperatingSystemMXBean sunOsBean = 
            (com.sun.management.OperatingSystemMXBean) osBean;
        
        ram = new RAMInfo();
        ram.setTotal(sunOsBean.getTotalMemorySize());
        ram.setFree(sunOsBean.getFreeMemorySize());
        ram.setAvailable(sunOsBean.getFreeMemorySize());
        ram.setUsed(ram.getTotal() - ram.getFree());
        ram.setUtilization((double) ram.getUsed() / ram.getTotal() * 100);
        
        logger.info(String.format("RAM detected: %.1f GB", ram.getTotal() / (1024.0 * 1024 * 1024)));
    }
    
    private void detectDisks() {
        disks.clear();
        
        File[] roots = File.listRoots();
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
    
    private void detectNetworks() {
        networks.clear();
        
        try {
            java.net.NetworkInterface.getNetworkInterfaces().asIterator()
                .forEachRemaining(netIf -> {
                    try {
                        NetworkInfo net = new NetworkInfo();
                        net.setInterface(netIf.getName());
                        net.setMacAddress(Arrays.toString(netIf.getHardwareAddress()));
                        net.setIpAddresses(new ArrayList<>());
                        net.setLinkStatus(netIf.isUp());
                        net.setSpeed(1000);
                        net.setMtu(netIf.getMTU());
                        
                        networks.add(net);
                        logger.info("Network interface detected: " + net.getInterface());
                    } catch (Exception e) {
                        logger.warning("Error detecting network: " + e.getMessage());
                    }
                });
        } catch (Exception e) {
            logger.warning("Error detecting networks: " + e.getMessage());
        }
    }
    
    private void detectBattery() {
        // Placeholder for battery detection
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
                
                // Limit history size
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
        snapshot.setTimestamp(java.time.LocalDateTime.now());
        
        // CPU
        snapshot.setCpuUsage(cpu != null ? cpu.getUtilization() : 0);
        snapshot.setCpuTemp(cpu != null ? cpu.getTemperature() : 0);
        snapshot.setCpuFreq(cpu != null ? cpu.getCurrentFrequency() : 0);
        
        // RAM
        if (ram != null) {
            snapshot.setRamUsage(ram.getUtilization());
            snapshot.setRamAvailable(ram.getAvailable());
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
        
        // Disk
        Map<String, Double> diskUsage = new HashMap<>();
        Map<String, long[]> diskIO = new HashMap<>();
        
        for (DiskInfo disk : disks) {
            diskUsage.put(disk.getMountpoint(), disk.getUtilization());
            diskIO.put(disk.getMountpoint(), new long[]{0, 0});
        }
        
        snapshot.setDiskUsage(diskUsage);
        snapshot.setDiskIO(diskIO);
        
        // Network
        snapshot.setNetworkIO(new long[]{0, 0});
        
        return snapshot;
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
            sb.append(String.format("  Memory: %.1fGB / %.1fGB\n",
                    gpu.getMemoryUsed() / (1024.0 * 1024 * 1024),
                    gpu.getMemoryTotal() / (1024.0 * 1024 * 1024)));
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
    
    public List<GPUInfo> getGpus() { return gpus; }
    public CPUInfo getCpu() { return cpu; }
    public RAMInfo getRam() { return ram; }
    public List<DiskInfo> getDisks() { return disks; }
    public List<NetworkInfo> getNetworks() { return networks; }
    public Queue<HardwareSnapshot> getHistory() { return history; }
    public List<HardwareSnapshot> getSnapshots() { return snapshots; }
}