package com.noty215.notycaption.models;

import java.time.LocalDateTime;
import java.util.List;

public class NetworkInfo {
    private String interface_;
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
    private int errorsIn;
    private int errorsOut;
    private int dropsIn;
    private int dropsOut;
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

    public NetworkInfo() {}

    public String getInterface() { return interface_; }
    public void setInterface(String interface_) { this.interface_ = interface_; }

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

    public int getErrorsIn() { return errorsIn; }
    public void setErrorsIn(int errorsIn) { this.errorsIn = errorsIn; }

    public int getErrorsOut() { return errorsOut; }
    public void setErrorsOut(int errorsOut) { this.errorsOut = errorsOut; }

    public int getDropsIn() { return dropsIn; }
    public void setDropsIn(int dropsIn) { this.dropsIn = dropsIn; }

    public int getDropsOut() { return dropsOut; }
    public void setDropsOut(int dropsOut) { this.dropsOut = dropsOut; }

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
}