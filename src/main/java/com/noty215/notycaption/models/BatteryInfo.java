package com.noty215.notycaption.models;

/**
 * Battery Information data class
 */
public class BatteryInfo {
    private boolean present;
    private boolean charging;
    private double percent;
    private Integer timeRemaining;
    private Integer energyFull;
    private Integer energyFullDesign;
    private Integer energyNow;
    private Integer powerNow;
    private Integer voltageNow;
    private Integer cycleCount;
    private String technology;
    private String manufacturer;
    private String model;
    private String serial;
    private Double temperature;
    private String health;
    private String status;

    // Constructors
    public BatteryInfo() {}

    public BatteryInfo(boolean present, boolean charging, double percent, Integer timeRemaining) {
        this.present = present;
        this.charging = charging;
        this.percent = percent;
        this.timeRemaining = timeRemaining;
    }

    // Getters and Setters
    public boolean isPresent() { return present; }
    public void setPresent(boolean present) { this.present = present; }

    public boolean isCharging() { return charging; }
    public void setCharging(boolean charging) { this.charging = charging; }

    public double getPercent() { return percent; }
    public void setPercent(double percent) { this.percent = percent; }

    public Integer getTimeRemaining() { return timeRemaining; }
    public void setTimeRemaining(Integer timeRemaining) { this.timeRemaining = timeRemaining; }

    public Integer getEnergyFull() { return energyFull; }
    public void setEnergyFull(Integer energyFull) { this.energyFull = energyFull; }

    public Integer getEnergyFullDesign() { return energyFullDesign; }
    public void setEnergyFullDesign(Integer energyFullDesign) { this.energyFullDesign = energyFullDesign; }

    public Integer getEnergyNow() { return energyNow; }
    public void setEnergyNow(Integer energyNow) { this.energyNow = energyNow; }

    public Integer getPowerNow() { return powerNow; }
    public void setPowerNow(Integer powerNow) { this.powerNow = powerNow; }

    public Integer getVoltageNow() { return voltageNow; }
    public void setVoltageNow(Integer voltageNow) { this.voltageNow = voltageNow; }

    public Integer getCycleCount() { return cycleCount; }
    public void setCycleCount(Integer cycleCount) { this.cycleCount = cycleCount; }

    public String getTechnology() { return technology; }
    public void setTechnology(String technology) { this.technology = technology; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public String getHealth() { return health; }
    public void setHealth(String health) { this.health = health; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("Battery: %.1f%% - %s",
                percent, charging ? "Charging" : "Discharging");
    }
}