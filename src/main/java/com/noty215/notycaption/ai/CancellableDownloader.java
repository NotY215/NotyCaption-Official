package com.noty215.notycaption.ai;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class CancellableDownloader {
    private static final Logger logger = Logger.getLogger(CancellableDownloader.class.getName());
    private final AtomicBoolean canceled;
    private final AtomicBoolean paused;
    private HttpURLConnection connection;
    private File tempFile;
    private Consumer<ProgressInfo> progressCallback;
    private long totalBytes;
    private long downloadedBytes;
    private long startTime;

    public CancellableDownloader() {
        this.canceled = new AtomicBoolean(false);
        this.paused = new AtomicBoolean(false);
    }

    public void setProgressCallback(Consumer<ProgressInfo> callback) {
        this.progressCallback = callback;
    }

    public void cancel() {
        canceled.set(true);
        if (connection != null) {
            connection.disconnect();
        }
        logger.info("Download cancellation requested");
    }

    public void pause() {
        paused.set(true);
        logger.info("Download paused");
    }

    public void resume() {
        paused.set(false);
        logger.info("Download resumed");
    }

    public boolean isCanceled() {
        return canceled.get();
    }

    public boolean isPaused() {
        return paused.get();
    }

    public File download(String urlString, String destinationPath) throws Exception {
        return download(urlString, destinationPath, null);
    }

    public File download(String urlString, String destinationPath, String expectedHash) throws Exception {
        canceled.set(false);
        paused.set(false);
        startTime = System.currentTimeMillis();

        URI uri = new URI(urlString);
        URL url = uri.toURL();
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("Accept-Encoding", "identity");

        totalBytes = connection.getContentLengthLong();
        File outputFile = new File(destinationPath);
        tempFile = new File(destinationPath + ".tmp");

        logger.info("Starting download: " + urlString + " (" + formatSize(totalBytes) + ")");

        if (progressCallback != null) {
            ProgressInfo info = new ProgressInfo(0, "Connecting...");
            info.setTotalBytes(totalBytes);
            progressCallback.accept(info);
        }

        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            downloadedBytes = 0;
            long lastProgressTime = System.currentTimeMillis();

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                if (canceled.get()) {
                    logger.info("Download canceled");
                    cleanup();
                    throw new Exception("Download canceled by user");
                }

                while (paused.get()) {
                    Thread.sleep(100);
                    if (canceled.get()) {
                        cleanup();
                        throw new Exception("Download canceled by user");
                    }
                }

                outputStream.write(buffer, 0, bytesRead);
                downloadedBytes += bytesRead;

                long now = System.currentTimeMillis();
                if (now - lastProgressTime > 500 || downloadedBytes == totalBytes) {
                    updateProgress();
                    lastProgressTime = now;
                }
            }
        }

        // Verify file integrity
        if (expectedHash != null && !verifyHash(tempFile, expectedHash)) {
            cleanup();
            throw new Exception("File hash verification failed");
        }

        // Rename temp file to final name
        if (tempFile.exists() && tempFile.length() == totalBytes) {
            if (outputFile.exists()) {
                outputFile.delete();
            }
            Files.move(tempFile.toPath(), outputFile.toPath());
            logger.info("Download completed: " + outputFile.getAbsolutePath());

            if (progressCallback != null) {
                ProgressInfo info = new ProgressInfo(100, "Download completed!");
                info.setTotalBytes(totalBytes);
                progressCallback.accept(info);
            }
        } else {
            cleanup();
            throw new Exception("Download incomplete: " + formatSize(tempFile.length()) + " of " + formatSize(totalBytes));
        }

        return outputFile;
    }

    private void updateProgress() {
        if (progressCallback != null) {
            int percent = totalBytes > 0 ? (int) ((downloadedBytes * 100) / totalBytes) : 0;
            long elapsed = System.currentTimeMillis() - startTime;
            double speed = elapsed > 0 ? (downloadedBytes / (elapsed / 1000.0)) : 0;
            long remainingBytes = totalBytes - downloadedBytes;
            long etaSeconds = speed > 0 ? (long) (remainingBytes / speed) : 0;

            ProgressInfo info = new ProgressInfo(percent, "Downloading...");
            info.setDownloadedBytes(downloadedBytes);
            info.setTotalBytes(totalBytes);
            info.setSpeed(speed);
            info.setEta(formatDuration(etaSeconds));
            progressCallback.accept(info);
        }
    }

    private boolean verifyHash(File file, String expectedHash) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    md.update(buffer, 0, read);
                }
            }
            byte[] hashBytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            String actualHash = sb.toString();
            return actualHash.equalsIgnoreCase(expectedHash);
        } catch (Exception e) {
            logger.warning("Hash verification failed: " + e.getMessage());
            return false;
        }
    }

    private void cleanup() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
        if (connection != null) {
            connection.disconnect();
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String formatDuration(long seconds) {
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
    }

    public static class ProgressInfo {
        private int percent;
        private String message;
        private long downloadedBytes;
        private long totalBytes;
        private double speed;
        private String eta;

        public ProgressInfo(int percent, String message) {
            this.percent = percent;
            this.message = message;
        }

        public int getPercent() { return percent; }
        public void setPercent(int percent) { this.percent = percent; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public long getDownloadedBytes() { return downloadedBytes; }
        public void setDownloadedBytes(long downloadedBytes) { this.downloadedBytes = downloadedBytes; }

        public long getTotalBytes() { return totalBytes; }
        public void setTotalBytes(long totalBytes) { this.totalBytes = totalBytes; }

        public double getSpeed() { return speed; }
        public void setSpeed(double speed) { this.speed = speed; }

        public String getEta() { return eta; }
        public void setEta(String eta) { this.eta = eta; }

        public String getFormattedProgress() {
            return String.format("%d%% - %s: %s/%s @ %s/s (ETA: %s)",
                    percent, message, formatSize(downloadedBytes), formatSize(totalBytes),
                    formatSize((long)speed), eta);
        }

        private String formatSize(long bytes) {
            if (bytes < 1024) return bytes + "B";
            if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1fMB", bytes / (1024.0 * 1024));
            return String.format("%.2fGB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}