package com.noty215.notycaption.models;

public class BatteryInfo {
    private boolean present;
    private boolean charging;
    private double percent;
    private Integer timeRemaining;
    private Long energyFull;
    private Long energyFullDesign;
    private Long energyNow;
    private Long powerNow;
    private Long voltageNow;
    private Integer cycleCount;
    private String technology;
    private String manufacturer;
    private String model;
    private String serial;
    private Double temperature;
    private String health;
    private String status;

    public BatteryInfo() {}

    public boolean isPresent() { return present; }
    public void setPresent(boolean present) { this.present = present; }

    public boolean isCharging() { return charging; }
    public void setCharging(boolean charging) { this.charging = charging; }

    public double getPercent() { return percent; }
    public void setPercent(double percent) { this.percent = percent; }

    public Integer getTimeRemaining() { return timeRemaining; }
    public void setTimeRemaining(Integer timeRemaining) { this.timeRemaining = timeRemaining; }

    public Long getEnergyFull() { return energyFull; }
    public void setEnergyFull(Long energyFull) { this.energyFull = energyFull; }

    public Long getEnergyFullDesign() { return energyFullDesign; }
    public void setEnergyFullDesign(Long energyFullDesign) { this.energyFullDesign = energyFullDesign; }

    public Long getEnergyNow() { return energyNow; }
    public void setEnergyNow(Long energyNow) { this.energyNow = energyNow; }

    public Long getPowerNow() { return powerNow; }
    public void setPowerNow(Long powerNow) { this.powerNow = powerNow; }

    public Long getVoltageNow() { return voltageNow; }
    public void setVoltageNow(Long voltageNow) { this.voltageNow = voltageNow; }

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
        return String.format("Battery: %.1f%% %s", percent, charging ? "(Charging)" : "(Discharging)");
    }
}