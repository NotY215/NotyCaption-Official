package com.noty215.notycaption.ai;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class ModelDownloader {
    private static final Logger logger = Logger.getLogger(ModelDownloader.class.getName());
    private static final String WHISPER_MODEL_URL = "https://openaipublic.azureedge.net/main/whisper/models/";
    private static final Map<String, String> MODEL_URLS = new HashMap<>();

    static {
        MODEL_URLS.put("tiny", "tiny.pt");
        MODEL_URLS.put("base", "base.pt");
        MODEL_URLS.put("small", "small.pt");
        MODEL_URLS.put("medium", "medium.pt");
        MODEL_URLS.put("large", "large-v1.pt");
        MODEL_URLS.put("large-v1", "large-v1.pt");
        MODEL_URLS.put("large-v2", "large-v2.pt");
        MODEL_URLS.put("large-v3", "large-v3.pt");
    }

    private String modelName;
    private String downloadDir;
    private AtomicBoolean canceled;
    private AtomicBoolean paused;
    private Consumer<ProgressInfo> progressCallback;
    private long totalBytes;
    private long downloadedBytes;
    private long startTime;

    public ModelDownloader(String modelName, String downloadDir) {
        this.modelName = modelName;
        this.downloadDir = downloadDir;
        this.canceled = new AtomicBoolean(false);
        this.paused = new AtomicBoolean(false);
        this.totalBytes = 0;
        this.downloadedBytes = 0;
    }

    public void setProgressCallback(Consumer<ProgressInfo> callback) {
        this.progressCallback = callback;
    }

    public void cancel() {
        canceled.set(true);
        logger.info("Model download cancellation requested");
    }

    public void pause() {
        paused.set(true);
        logger.info("Model download paused");
    }

    public void resume() {
        paused.set(false);
        logger.info("Model download resumed");
    }

    public boolean isCanceled() {
        return canceled.get();
    }

    public boolean isPaused() {
        return paused.get();
    }

    public File download() throws Exception {
        String modelUrl = WHISPER_MODEL_URL + MODEL_URLS.getOrDefault(modelName, modelName + ".pt");
        return downloadFromUrl(modelUrl);
    }

    private File downloadFromUrl(String urlString) throws Exception {
        canceled.set(false);
        paused.set(false);
        startTime = System.currentTimeMillis();

        URI uri = new URI(urlString);
        URL url = uri.toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);

        totalBytes = connection.getContentLengthLong();
        logger.info("Starting download: " + modelName + " (" + formatSize(totalBytes) + ")");

        if (progressCallback != null) {
            ProgressInfo info = new ProgressInfo(0, "Starting download...");
            info.setTotalBytes(totalBytes);
            progressCallback.accept(info);
        }

        File outputFile = new File(downloadDir, modelName + ".pt");
        File tempFile = new File(downloadDir, modelName + ".pt.tmp");

        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            downloadedBytes = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                if (canceled.get()) {
                    logger.info("Download canceled");
                    tempFile.delete();
                    throw new Exception("Download canceled by user");
                }

                while (paused.get()) {
                    Thread.sleep(100);
                }

                outputStream.write(buffer, 0, bytesRead);
                downloadedBytes += bytesRead;

                if (progressCallback != null) {
                    int percent = (int) ((downloadedBytes * 100) / totalBytes);
                    long elapsed = System.currentTimeMillis() - startTime;
                    double speed = downloadedBytes / (elapsed / 1000.0);
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
            tempFile.delete();
            throw new Exception("Download incomplete or corrupted");
        }

        return outputFile;
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private static String formatDuration(long seconds) {
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
    }

    public static boolean validateModelFile(File modelFile) {
        if (!modelFile.exists()) return false;

        long fileSize = modelFile.length();
        long expectedSizeMin = 2_500_000_000L;
        long expectedSizeMax = 3_100_000_000L;

        if (fileSize >= expectedSizeMin && fileSize <= expectedSizeMax) {
            logger.info("Model file validated: " + formatSize(fileSize));
            return true;
        }

        logger.warning("Model file size invalid: " + formatSize(fileSize));
        return false;
    }

    public static void cleanupCorruptModels(String modelsDir) {
        File dir = new File(modelsDir);
        if (!dir.exists()) return;

        File[] files = dir.listFiles((d, name) ->
                name.endsWith(".pt") || name.endsWith(".pt.tmp"));

        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".tmp") || !validateModelFile(file)) {
                    if (file.delete()) {
                        logger.info("Removed corrupt/incomplete model: " + file.getName());
                    }
                }
            }
        }
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

        public String getProgressString() {
            return String.format("%d%% - %s - %s/%s - %s/s - ETA: %s",
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