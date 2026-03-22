package com.noty215.notycaption.models;

/**
 * RAM Information data class
 */
public class RAMInfo {
    private long total;
    private long available;
    private long used;
    private long free;
    private long cached;
    private long buffers;
    private long shared;
    private long swapTotal;
    private long swapUsed;
    private long swapFree;
    private double utilization;
    private double swapUtilization;
    private Integer speed;
    private String type;
    private Integer channels;
    private Integer slots;
    private Integer slotsUsed;
    private String formFactor;
    private String manufacturer;
    private String partNumber;
    private String serialNumber;
    private Double voltage;
    private String timing;
    private boolean ecc;
    private boolean registered;
    private boolean buffered;

    // Constructors
    public RAMInfo() {}

    public RAMInfo(long total, long available, long used, long free, long cached, long buffers, long shared,
                   long swapTotal, long swapUsed, long swapFree, double utilization, double swapUtilization) {
        this.total = total;
        this.available = available;
        this.used = used;
        this.free = free;
        this.cached = cached;
        this.buffers = buffers;
        this.shared = shared;
        this.swapTotal = swapTotal;
        this.swapUsed = swapUsed;
        this.swapFree = swapFree;
        this.utilization = utilization;
        this.swapUtilization = swapUtilization;
    }

    // Getters and Setters
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public long getAvailable() { return available; }
    public void setAvailable(long available) { this.available = available; }

    public long getUsed() { return used; }
    public void setUsed(long used) { this.used = used; }

    public long getFree() { return free; }
    public void setFree(long free) { this.free = free; }

    public long getCached() { return cached; }
    public void setCached(long cached) { this.cached = cached; }

    public long getBuffers() { return buffers; }
    public void setBuffers(long buffers) { this.buffers = buffers; }

    public long getShared() { return shared; }
    public void setShared(long shared) { this.shared = shared; }

    public long getSwapTotal() { return swapTotal; }
    public void setSwapTotal(long swapTotal) { this.swapTotal = swapTotal; }

    public long getSwapUsed() { return swapUsed; }
    public void setSwapUsed(long swapUsed) { this.swapUsed = swapUsed; }

    public long getSwapFree() { return swapFree; }
    public void setSwapFree(long swapFree) { this.swapFree = swapFree; }

    public double getUtilization() { return utilization; }
    public void setUtilization(double utilization) { this.utilization = utilization; }

    public double getSwapUtilization() { return swapUtilization; }
    public void setSwapUtilization(double swapUtilization) { this.swapUtilization = swapUtilization; }

    public Integer getSpeed() { return speed; }
    public void setSpeed(Integer speed) { this.speed = speed; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getChannels() { return channels; }
    public void setChannels(Integer channels) { this.channels = channels; }

    public Integer getSlots() { return slots; }
    public void setSlots(Integer slots) { this.slots = slots; }

    public Integer getSlotsUsed() { return slotsUsed; }
    public void setSlotsUsed(Integer slotsUsed) { this.slotsUsed = slotsUsed; }

    public String getFormFactor() { return formFactor; }
    public void setFormFactor(String formFactor) { this.formFactor = formFactor; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getPartNumber() { return partNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public Double getVoltage() { return voltage; }
    public void setVoltage(Double voltage) { this.voltage = voltage; }

    public String getTiming() { return timing; }
    public void setTiming(String timing) { this.timing = timing; }

    public boolean isEcc() { return ecc; }
    public void setEcc(boolean ecc) { this.ecc = ecc; }

    public boolean isRegistered() { return registered; }
    public void setRegistered(boolean registered) { this.registered = registered; }

    public boolean isBuffered() { return buffered; }
    public void setBuffered(boolean buffered) { this.buffered = buffered; }

    @Override
    public String toString() {
        return String.format("RAM: %.1f/%.1f GB (%.1f%%) | Swap: %.1f/%.1f GB (%.1f%%)",
                used / (1024.0 * 1024 * 1024),
                total / (1024.0 * 1024 * 1024),
                utilization,
                swapUsed / (1024.0 * 1024 * 1024),
                swapTotal / (1024.0 * 1024 * 1024),
                swapUtilization);
    }
}