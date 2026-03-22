package com.noty215.notycaption.models;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Network Information data class
 */
public class NetworkInfo {
    private String interface;
    private String macAddress;
    private List<String> ipAddresses;
    private String gateway;
    private List<String> dnsServers;
    private boolean dhcpEnabled;
    private String dhcpServer;
    private LocalDateTime leaseObtained;
    private LocalDateTime leaseExpires;
    private int speed;
    private int mtu;
    private long bytesSent;
    private long bytesReceived;
    private long packetsSent;
    private long packetsReceived;
    private long errorsIn;
    private long errorsOut;
    private long dropsIn;
    private long dropsOut;
    private boolean linkStatus;
    private boolean wireless;
    private String ssid;
    private Integer signalStrength;
    private Integer channel;
    private Integer frequency;
    private String encryption;
    private String mode;
    private String bssid;
    private Double quality;

    // Constructors
    public NetworkInfo() {}

    public NetworkInfo(String interface, String macAddress, List<String> ipAddresses, String gateway,
                       List<String> dnsServers, boolean dhcpEnabled, int speed, int mtu, long bytesSent,
                       long bytesReceived, long packetsSent, long packetsReceived, long errorsIn,
                       long errorsOut, long dropsIn, long dropsOut, boolean linkStatus) {
        this.interface = interface;
        this.macAddress = macAddress;
        this.ipAddresses = ipAddresses;
        this.gateway = gateway;
        this.dnsServers = dnsServers;
        this.dhcpEnabled = dhcpEnabled;
        this.speed = speed;
        this.mtu = mtu;
        this.bytesSent = bytesSent;
        this.bytesReceived = bytesReceived;
        this.packetsSent = packetsSent;
        this.packetsReceived = packetsReceived;
        this.errorsIn = errorsIn;
        this.errorsOut = errorsOut;
        this.dropsIn = dropsIn;
        this.dropsOut = dropsOut;
        this.linkStatus = linkStatus;
    }

    // Getters and Setters
    public String getInterface() { return interface; }
    public void setInterface(String interface) { this.interface = interface; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public List<String> getIpAddresses() { return ipAddresses; }
    public void setIpAddresses(List<String> ipAddresses) { this.ipAddresses = ipAddresses; }

    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; }

    public List<String> getDnsServers() { return dnsServers; }
    public void setDnsServers(List<String> dnsServers) { this.dnsServers = dnsServers; }

    public boolean isDhcpEnabled() { return dhcpEnabled; }
    public void setDhcpEnabled(boolean dhcpEnabled) { this.dhcpEnabled = dhcpEnabled; }

    public String getDhcpServer() { return dhcpServer; }
    public void setDhcpServer(String dhcpServer) { this.dhcpServer = dhcpServer; }

    public LocalDateTime getLeaseObtained() { return leaseObtained; }
    public void setLeaseObtained(LocalDateTime leaseObtained) { this.leaseObtained = leaseObtained; }

    public LocalDateTime getLeaseExpires() { return leaseExpires; }
    public void setLeaseExpires(LocalDateTime leaseExpires) { this.leaseExpires = leaseExpires; }

    public int getSpeed() { return speed; }
    public void setSpeed(int speed) { this.speed = speed; }

    public int getMtu() { return mtu; }
    public void setMtu(int mtu) { this.mtu = mtu; }

    public long getBytesSent() { return bytesSent; }
    public void setBytesSent(long bytesSent) { this.bytesSent = bytesSent; }

    public long getBytesReceived() { return bytesReceived; }
    public void setBytesReceived(long bytesReceived) { this.bytesReceived = bytesReceived; }

    public long getPacketsSent() { return packetsSent; }
    public void setPacketsSent(long packetsSent) { this.packetsSent = packetsSent; }

    public long getPacketsReceived() { return packetsReceived; }
    public void setPacketsReceived(long packetsReceived) { this.packetsReceived = packetsReceived; }

    public long getErrorsIn() { return errorsIn; }
    public void setErrorsIn(long errorsIn) { this.errorsIn = errorsIn; }

    public long getErrorsOut() { return errorsOut; }
    public void setErrorsOut(long errorsOut) { this.errorsOut = errorsOut; }

    public long getDropsIn() { return dropsIn; }
    public void setDropsIn(long dropsIn) { this.dropsIn = dropsIn; }

    public long getDropsOut() { return dropsOut; }
    public void setDropsOut(long dropsOut) { this.dropsOut = dropsOut; }

    public boolean isLinkStatus() { return linkStatus; }
    public void setLinkStatus(boolean linkStatus) { this.linkStatus = linkStatus; }

    public boolean isWireless() { return wireless; }
    public void setWireless(boolean wireless) { this.wireless = wireless; }

    public String getSsid() { return ssid; }
    public void setSsid(String ssid) { this.ssid = ssid; }

    public Integer getSignalStrength() { return signalStrength; }
    public void setSignalStrength(Integer signalStrength) { this.signalStrength = signalStrength; }

    public Integer getChannel() { return channel; }
    public void setChannel(Integer channel) { this.channel = channel; }

    public Integer getFrequency() { return frequency; }
    public void setFrequency(Integer frequency) { this.frequency = frequency; }

    public String getEncryption() { return encryption; }
    public void setEncryption(String encryption) { this.encryption = encryption; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getBssid() { return bssid; }
    public void setBssid(String bssid) { this.bssid = bssid; }

    public Double getQuality() { return quality; }
    public void setQuality(Double quality) { this.quality = quality; }

    @Override
    public String toString() {
        return String.format("Network: %s - %s (%.1f Mbps) - %s",
        interface, macAddress, speed, linkStatus ? "UP" : "DOWN");
    }
}