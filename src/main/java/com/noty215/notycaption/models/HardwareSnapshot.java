package com.noty215.notycaption.models;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class HardwareSnapshot {
    private LocalDateTime timestamp;
    private double cpuUsage;
    private double cpuTemp;
    private double cpuFreq;
    private double cpuPower;
    private List<Double> gpuUsage;
    private List<Double> gpuTemp;
    private List<Long> gpuMemory;
    private List<Double> gpuPower;
    private double ramUsage;
    private long ramAvailable;
    private double swapUsage;
    private Map<String, Double> diskUsage;
    private Map<String, long[]> diskIO;
    private long[] networkIO;
    private Double batteryPercent;
    private int processCount;
    private int threadCount;
    private int handleCount;
    private long uptime;
    private double[] loadAverage;
    
    public HardwareSnapshot() {}
    
    public HardwareSnapshot(LocalDateTime timestamp, double cpuUsage, double cpuTemp, 
                            double cpuFreq, double cpuPower, List<Double> gpuUsage,
                            List<Double> gpuTemp, List<Long> gpuMemory, List<Double> gpuPower,
                            double ramUsage, long ramAvailable, double swapUsage,
                            Map<String, Double> diskUsage, Map<String, long[]> diskIO,
                            long[] networkIO) {
        this.timestamp = timestamp;
        this.cpuUsage = cpuUsage;
        this.cpuTemp = cpuTemp;
        this.cpuFreq = cpuFreq;
        this.cpuPower = cpuPower;
        this.gpuUsage = gpuUsage;
        this.gpuTemp = gpuTemp;
        this.gpuMemory = gpuMemory;
        this.gpuPower = gpuPower;
        this.ramUsage = ramUsage;
        this.ramAvailable = ramAvailable;
        this.swapUsage = swapUsage;
        this.diskUsage = diskUsage;
        this.diskIO = diskIO;
        this.networkIO = networkIO;
    }
    
    // Getters and Setters
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }
    
    public double getCpuTemp() { return cpuTemp; }
    public void setCpuTemp(double cpuTemp) { this.cpuTemp = cpuTemp; }
    
    public double getCpuFreq() { return cpuFreq; }
    public void setCpuFreq(double cpuFreq) { this.cpuFreq = cpuFreq; }
    
    public double getCpuPower() { return cpuPower; }
    public void setCpuPower(double cpuPower) { this.cpuPower = cpuPower; }
    
    public List<Double> getGpuUsage() { return gpuUsage; }
    public void setGpuUsage(List<Double> gpuUsage) { this.gpuUsage = gpuUsage; }
    
    public List<Double> getGpuTemp() { return gpuTemp; }
    public void setGpuTemp(List<Double> gpuTemp) { this.gpuTemp = gpuTemp; }
    
    public List<Long> getGpuMemory() { return gpuMemory; }
    public void setGpuMemory(List<Long> gpuMemory) { this.gpuMemory = gpuMemory; }
    
    public List<Double> getGpuPower() { return gpuPower; }
    public void setGpuPower(List<Double> gpuPower) { this.gpuPower = gpuPower; }
    
    public double getRamUsage() { return ramUsage; }
    public void setRamUsage(double ramUsage) { this.ramUsage = ramUsage; }
    
    public long getRamAvailable() { return ramAvailable; }
    public void setRamAvailable(long ramAvailable) { this.ramAvailable = ramAvailable; }
    
    public double getSwapUsage() { return swapUsage; }
    public void setSwapUsage(double swapUsage) { this.swapUsage = swapUsage; }
    
    public Map<String, Double> getDiskUsage() { return diskUsage; }
    public void setDiskUsage(Map<String, Double> diskUsage) { this.diskUsage = diskUsage; }
    
    public Map<String, long[]> getDiskIO() { return diskIO; }
    public void setDiskIO(Map<String, long[]> diskIO) { this.diskIO = diskIO; }
    
    public long[] getNetworkIO() { return networkIO; }
    public void setNetworkIO(long[] networkIO) { this.networkIO = networkIO; }
    
    public Double getBatteryPercent() { return batteryPercent; }
    public void setBatteryPercent(Double batteryPercent) { this.batteryPercent = batteryPercent; }
    
    public int getProcessCount() { return processCount; }
    public void setProcessCount(int processCount) { this.processCount = processCount; }
    
    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }
    
    public int getHandleCount() { return handleCount; }
    public void setHandleCount(int handleCount) { this.handleCount = handleCount; }
    
    public long getUptime() { return uptime; }
    public void setUptime(long uptime) { this.uptime = uptime; }
    
    public double[] getLoadAverage() { return loadAverage; }
    public void setLoadAverage(double[] loadAverage) { this.loadAverage = loadAverage; }
}