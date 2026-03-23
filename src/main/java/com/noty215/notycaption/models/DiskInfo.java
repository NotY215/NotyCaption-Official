package com.noty215.notycaption.models;

/**
 * Disk Information data class
 */
public class DiskInfo {
    private String device;
    private String mountpoint;
    private String filesystem;
    private long total;
    private long used;
    private long free;
    private double utilization;
    private double readSpeed;
    private double writeSpeed;
    private int readIops;
    private int writeIops;
    private double readLatency;
    private double writeLatency;
    private String model;
    private String serial;
    private String firmware;
    private String diskInterface;  // Changed from 'interface' to 'diskInterface'
    private String mediaType;
    private String formFactor;
    private Double temperature;
    private String health;
    private Integer powerOnHours;
    private Integer powerCycleCount;
    private Double wearLevel;
    private Integer badSectors;
    private Integer reallocatedSectors;
    private Integer pendingSectors;
    private Integer uncorrectableSectors;
    private Integer crcErrors;
    private boolean trimSupport;
    private boolean smartSupport;
    private boolean nvmeSupport;
    private boolean ahciSupport;
    private boolean raidSupport;

    // Constructors
    public DiskInfo() {}

    public DiskInfo(String device, String mountpoint, String filesystem, long total, long used, long free,
                    double utilization, double readSpeed, double writeSpeed, int readIops, int writeIops,
                    double readLatency, double writeLatency) {
        this.device = device;
        this.mountpoint = mountpoint;
        this.filesystem = filesystem;
        this.total = total;
        this.used = used;
        this.free = free;
        this.utilization = utilization;
        this.readSpeed = readSpeed;
        this.writeSpeed = writeSpeed;
        this.readIops = readIops;
        this.writeIops = writeIops;
        this.readLatency = readLatency;
        this.writeLatency = writeLatency;
    }

    // Getters and Setters
    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }

    public String getMountpoint() { return mountpoint; }
    public void setMountpoint(String mountpoint) { this.mountpoint = mountpoint; }

    public String getFilesystem() { return filesystem; }
    public void setFilesystem(String filesystem) { this.filesystem = filesystem; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public long getUsed() { return used; }
    public void setUsed(long used) { this.used = used; }

    public long getFree() { return free; }
    public void setFree(long free) { this.free = free; }

    public double getUtilization() { return utilization; }
    public void setUtilization(double utilization) { this.utilization = utilization; }

    public double getReadSpeed() { return readSpeed; }
    public void setReadSpeed(double readSpeed) { this.readSpeed = readSpeed; }

    public double getWriteSpeed() { return writeSpeed; }
    public void setWriteSpeed(double writeSpeed) { this.writeSpeed = writeSpeed; }

    public int getReadIops() { return readIops; }
    public void setReadIops(int readIops) { this.readIops = readIops; }

    public int getWriteIops() { return writeIops; }
    public void setWriteIops(int writeIops) { this.writeIops = writeIops; }

    public double getReadLatency() { return readLatency; }
    public void setReadLatency(double readLatency) { this.readLatency = readLatency; }

    public double getWriteLatency() { return writeLatency; }
    public void setWriteLatency(double writeLatency) { this.writeLatency = writeLatency; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }

    public String getFirmware() { return firmware; }
    public void setFirmware(String firmware) { this.firmware = firmware; }

    public String getDiskInterface() { return diskInterface; }
    public void setDiskInterface(String diskInterface) { this.diskInterface = diskInterface; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public String getFormFactor() { return formFactor; }
    public void setFormFactor(String formFactor) { this.formFactor = formFactor; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public String getHealth() { return health; }
    public void setHealth(String health) { this.health = health; }

    public Integer getPowerOnHours() { return powerOnHours; }
    public void setPowerOnHours(Integer powerOnHours) { this.powerOnHours = powerOnHours; }

    public Integer getPowerCycleCount() { return powerCycleCount; }
    public void setPowerCycleCount(Integer powerCycleCount) { this.powerCycleCount = powerCycleCount; }

    public Double getWearLevel() { return wearLevel; }
    public void setWearLevel(Double wearLevel) { this.wearLevel = wearLevel; }

    public Integer getBadSectors() { return badSectors; }
    public void setBadSectors(Integer badSectors) { this.badSectors = badSectors; }

    public Integer getReallocatedSectors() { return reallocatedSectors; }
    public void setReallocatedSectors(Integer reallocatedSectors) { this.reallocatedSectors = reallocatedSectors; }

    public Integer getPendingSectors() { return pendingSectors; }
    public void setPendingSectors(Integer pendingSectors) { this.pendingSectors = pendingSectors; }

    public Integer getUncorrectableSectors() { return uncorrectableSectors; }
    public void setUncorrectableSectors(Integer uncorrectableSectors) { this.uncorrectableSectors = uncorrectableSectors; }

    public Integer getCrcErrors() { return crcErrors; }
    public void setCrcErrors(Integer crcErrors) { this.crcErrors = crcErrors; }

    public boolean isTrimSupport() { return trimSupport; }
    public void setTrimSupport(boolean trimSupport) { this.trimSupport = trimSupport; }

    public boolean isSmartSupport() { return smartSupport; }
    public void setSmartSupport(boolean smartSupport) { this.smartSupport = smartSupport; }

    public boolean isNvmeSupport() { return nvmeSupport; }
    public void setNvmeSupport(boolean nvmeSupport) { this.nvmeSupport = nvmeSupport; }

    public boolean isAhciSupport() { return ahciSupport; }
    public void setAhciSupport(boolean ahciSupport) { this.ahciSupport = ahciSupport; }

    public boolean isRaidSupport() { return raidSupport; }
    public void setRaidSupport(boolean raidSupport) { this.raidSupport = raidSupport; }

    @Override
    public String toString() {
        return String.format("Disk: %s (%s) - %.1f/%.1f GB (%.1f%%)",
                device, mountpoint, used / (1024.0 * 1024 * 1024),
                total / (1024.0 * 1024 * 1024), utilization);
    }
}