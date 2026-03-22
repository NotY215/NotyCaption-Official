package com.noty215.notycaption.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class NetworkMonitor {
    private static final Logger logger = Logger.getLogger(NetworkMonitor.class.getName());
    private static NetworkMonitor instance;
    private boolean monitoring;
    private Thread monitorThread;
    private volatile boolean stopMonitoring;
    private long lastBytesSent;
    private long lastBytesReceived;
    private double downloadSpeed;
    private double uploadSpeed;

    private NetworkMonitor() {
        lastBytesSent = 0;
        lastBytesReceived = 0;
        downloadSpeed = 0;
        uploadSpeed = 0;
    }

    public static synchronized NetworkMonitor getInstance() {
        if (instance == null) {
            instance = new NetworkMonitor();
        }
        return instance;
    }

    public void startMonitoring(int intervalMs) {
        if (monitoring) return;

        monitoring = true;
        stopMonitoring = false;
        monitorThread = new Thread(() -> monitorLoop(intervalMs));
        monitorThread.setDaemon(true);
        monitorThread.start();

        logger.info("Network monitoring started");
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
        logger.info("Network monitoring stopped");
    }

    private void monitorLoop(int intervalMs) {
        try {
            updateNetworkStats();
            lastBytesSent = getTotalBytesSent();
            lastBytesReceived = getTotalBytesReceived();

            while (!stopMonitoring) {
                Thread.sleep(intervalMs);

                long currentSent = getTotalBytesSent();
                long currentReceived = getTotalBytesReceived();

                long sentDiff = currentSent - lastBytesSent;
                long receivedDiff = currentReceived - lastBytesReceived;

                downloadSpeed = (receivedDiff * 1000.0) / intervalMs;
                uploadSpeed = (sentDiff * 1000.0) / intervalMs;

                lastBytesSent = currentSent;
                lastBytesReceived = currentReceived;
            }
        } catch (Exception e) {
            logger.warning("Network monitoring error: " + e.getMessage());
        }
    }

    private long getTotalBytesSent() {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return getWindowsNetworkStats(true);
        } else {
            return getLinuxNetworkStats(true);
        }
    }

    private long getTotalBytesReceived() {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return getWindowsNetworkStats(false);
        } else {
            return getLinuxNetworkStats(false);
        }
    }

    private long getWindowsNetworkStats(boolean sent) {
        try {
            ProcessBuilder pb = new ProcessBuilder("netstat", "-e");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Bytes")) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 3) {
                        long bytes = Long.parseLong(parts[sent ? 1 : 2].replace(",", ""));
                        process.waitFor();
                        return bytes;
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            logger.warning("Error getting Windows network stats: " + e.getMessage());
        }
        return 0;
    }

    private long getLinuxNetworkStats(boolean sent) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cat", "/proc/net/dev");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String line;
            long total = 0;
            while ((line = reader.readLine()) != null) {
                if (line.contains("eth") || line.contains("wlan") || line.contains("enp") || line.contains("wl")) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 10) {
                        total += Long.parseLong(parts[sent ? 9 : 1]);
                    }
                }
            }
            process.waitFor();
            return total;
        } catch (Exception e) {
            logger.warning("Error getting Linux network stats: " + e.getMessage());
            return 0;
        }
    }

    private void updateNetworkStats() {
        try {
            NetworkInterface.getNetworkInterfaces().asIterator()
                    .forEachRemaining(netIf -> {
                        try {
                            if (netIf.isUp() && !netIf.isLoopback()) {
                                logger.fine("Interface: " + netIf.getName());
                            }
                        } catch (Exception e) {
                            logger.fine("Error getting interface info: " + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            logger.warning("Error updating network stats: " + e.getMessage());
        }
    }

    public double getDownloadSpeed() {
        return downloadSpeed;
    }

    public double getUploadSpeed() {
        return uploadSpeed;
    }

    public String getDownloadSpeedString() {
        return formatSpeed(downloadSpeed);
    }

    public String getUploadSpeedString() {
        return formatSpeed(uploadSpeed);
    }

    private String formatSpeed(double bytesPerSecond) {
        if (bytesPerSecond > 1024 * 1024) {
            return String.format("%.2f MB/s", bytesPerSecond / (1024 * 1024));
        } else if (bytesPerSecond > 1024) {
            return String.format("%.2f KB/s", bytesPerSecond / 1024);
        } else {
            return String.format("%.0f B/s", bytesPerSecond);
        }
    }

    public static boolean isInternetAvailable() {
        try {
            InetAddress address = InetAddress.getByName("8.8.8.8");
            return address.isReachable(5000);
        } catch (Exception e) {
            return false;
        }
    }

    public static String getPublicIP() {
        try {
            URI uri = new URI("https://api.ipify.org");
            URL url = uri.toURL();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(url.openStream()));
            String ip = reader.readLine();
            reader.close();
            return ip;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public static List<NetworkInterface> getActiveInterfaces() {
        List<NetworkInterface> active = new ArrayList<>();
        try {
            NetworkInterface.getNetworkInterfaces().asIterator()
                    .forEachRemaining(netIf -> {
                        try {
                            if (netIf.isUp() && !netIf.isLoopback()) {
                                active.add(netIf);
                            }
                        } catch (Exception e) {
                            logger.fine("Error checking interface: " + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            logger.warning("Error getting active interfaces: " + e.getMessage());
        }
        return active;
    }
}