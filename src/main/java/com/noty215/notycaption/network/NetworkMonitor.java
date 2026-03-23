package com.noty215.notycaption.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.*;

/**
 * Network monitoring utilities
 */
public class NetworkMonitor {

    private static final Logger logger = LoggerFactory.getLogger(NetworkMonitor.class);

    public static boolean isInternetAvailable() {
        try {
            InetAddress address = InetAddress.getByName("8.8.8.8");
            return address.isReachable(5000);
        } catch (Exception e) {
            return false;
        }
    }

    public static long getDownloadSpeed() {
        try {
            long startTime = System.currentTimeMillis();
            URL url = new URL("https://speedtest.tele2.net/1MB.zip");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.connect();
            long contentLength = connection.getContentLengthLong();
            connection.disconnect();

            if (contentLength <= 0) return 0;

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            long downloaded = 0;
            byte[] buffer = new byte[8192];
            int bytesRead;
            try (java.io.InputStream in = connection.getInputStream()) {
                while ((bytesRead = in.read(buffer)) != -1) {
                    downloaded += bytesRead;
                }
            }

            long endTime = System.currentTimeMillis();
            long elapsed = endTime - startTime;
            if (elapsed > 0) {
                return (downloaded * 1000) / elapsed;
            }
        } catch (Exception e) {
            logger.debug("Download speed test failed", e);
        }
        return 0;
    }

    public static long getUploadSpeed() {
        return 0;
    }

    public static double getNetworkLatency(String host) {
        try {
            long startTime = System.currentTimeMillis();
            InetAddress address = InetAddress.getByName(host);
            boolean reachable = address.isReachable(5000);
            long endTime = System.currentTimeMillis();

            if (reachable) {
                return endTime - startTime;
            }
        } catch (Exception e) {
            logger.debug("Latency test failed for {}", host, e);
        }
        return -1;
    }

    public static List<NetworkInterface> getNetworkInterfaces() {
        try {
            return Collections.list(NetworkInterface.getNetworkInterfaces());
        } catch (Exception e) {
            logger.error("Failed to get network interfaces", e);
            return new ArrayList<>();
        }
    }

    public static String getMACAddress() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            NetworkInterface ni = NetworkInterface.getByInetAddress(localHost);
            if (ni != null) {
                byte[] mac = ni.getHardwareAddress();
                if (mac != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get MAC address", e);
        }
        return "Unknown";
    }

    public static String getLocalIPAddress() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            return localHost.getHostAddress();
        } catch (Exception e) {
            logger.error("Failed to get local IP", e);
            return "127.0.0.1";
        }
    }

    public static List<String> getAllIPAddresses() {
        List<String> addresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress addr = inetAddresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress()) {
                        addresses.add(addr.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get IP addresses", e);
        }
        return addresses;
    }
}