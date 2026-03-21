package com.noty215.notycaption.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class ProgressTracker {
    private static final Logger logger = Logger.getLogger(ProgressTracker.class.getName());
    private final AtomicInteger progress;
    private final AtomicBoolean completed;
    private final AtomicBoolean canceled;
    private String currentMessage;
    private Instant startTime;
    private Instant lastUpdateTime;
    private long lastProgressBytes;
    private Consumer<ProgressInfo> callback;
    
    public ProgressTracker() {
        this.progress = new AtomicInteger(0);
        this.completed = new AtomicBoolean(false);
        this.canceled = new AtomicBoolean(false);
        this.currentMessage = "";
    }
    
    public void start() {
        startTime = Instant.now();
        lastUpdateTime = startTime;
        lastProgressBytes = 0;
        logger.info("Progress tracking started");
    }
    
    public void update(int percent, String message) {
        if (canceled.get()) return;
        
        progress.set(Math.min(100, Math.max(0, percent)));
        currentMessage = message;
        lastUpdateTime = Instant.now();
        
        if (callback != null) {
            ProgressInfo info = new ProgressInfo(percent, message);
            info.setElapsed(getElapsed());
            callback.accept(info);
        }
        
        logger.fine(String.format("Progress: %d%% - %s", percent, message));
    }
    
    public void updateWithBytes(long downloaded, long total) {
        if (total > 0) {
            int percent = (int) ((downloaded * 100) / total);
            update(percent, "Downloading...");
            
            long bytesSinceLast = downloaded - lastProgressBytes;
            Duration timeSinceLast = Duration.between(lastUpdateTime, Instant.now());
            double speed = timeSinceLast.getSeconds() > 0 ? bytesSinceLast / timeSinceLast.getSeconds() : 0;
            
            if (callback != null && speed > 0) {
                long remaining = total - downloaded;
                Duration eta = Duration.ofSeconds((long) (remaining / speed));
                ProgressInfo info = new ProgressInfo(percent, "Downloading...");
                info.setSpeed(speed);
                info.setEta(eta);
                info.setDownloaded(downloaded);
                info.setTotal(total);
                callback.accept(info);
            }
            
            lastProgressBytes = downloaded;
            lastUpdateTime = Instant.now();
        } else {
            update(progress.get(), "Downloading...");
        }
    }
    
    public void complete() {
        completed.set(true);
        update(100, "Completed!");
        logger.info("Progress tracking completed");
    }
    
    public void cancel() {
        canceled.set(true);
        logger.info("Progress tracking canceled");
    }
    
    public void reset() {
        progress.set(0);
        completed.set(false);
        canceled.set(false);
        currentMessage = "";
        startTime = null;
        lastUpdateTime = null;
        lastProgressBytes = 0;
    }
    
    public boolean isCompleted() {
        return completed.get();
    }
    
    public boolean isCanceled() {
        return canceled.get();
    }
    
    public int getProgress() {
        return progress.get();
    }
    
    public String getCurrentMessage() {
        return currentMessage;
    }
    
    public Duration getElapsed() {
        if (startTime == null) return Duration.ZERO;
        return Duration.between(startTime, Instant.now());
    }
    
    public Duration getEstimatedRemaining() {
        if (progress.get() <= 0 || progress.get() >= 100) return Duration.ZERO;
        
        Duration elapsed = getElapsed();
        long totalEstimated = (elapsed.toMillis() * 100) / progress.get();
        return Duration.ofMillis(totalEstimated - elapsed.toMillis());
    }
    
    public void setCallback(Consumer<ProgressInfo> callback) {
        this.callback = callback;
    }
    
    public static class ProgressInfo {
        private final int percent;
        private final String message;
        private Duration elapsed;
        private Duration eta;
        private double speed;
        private long downloaded;
        private long total;
        
        public ProgressInfo(int percent, String message) {
            this.percent = percent;
            this.message = message;
        }
        
        public int getPercent() { return percent; }
        public String getMessage() { return message; }
        public Duration getElapsed() { return elapsed; }
        public void setElapsed(Duration elapsed) { this.elapsed = elapsed; }
        public Duration getEta() { return eta; }
        public void setEta(Duration eta) { this.eta = eta; }
        public double getSpeed() { return speed; }
        public void setSpeed(double speed) { this.speed = speed; }
        public long getDownloaded() { return downloaded; }
        public void setDownloaded(long downloaded) { this.downloaded = downloaded; }
        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }
        
        public String getFormattedSpeed() {
            if (speed < 1024) return String.format("%.0f B/s", speed);
            if (speed < 1024 * 1024) return String.format("%.2f KB/s", speed / 1024);
            return String.format("%.2f MB/s", speed / (1024 * 1024));
        }
        
        public String getFormattedSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
        
        public String getFormattedEta() {
            if (eta == null) return "calculating...";
            long seconds = eta.getSeconds();
            if (seconds < 60) return seconds + "s";
            if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
            return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        }
        
        @Override
        public String toString() {
            if (total > 0) {
                return String.format("%d%% - %s - %s/%s @ %s (ETA: %s)",
                    percent, message,
                    getFormattedSize(downloaded), getFormattedSize(total),
                    getFormattedSpeed(), getFormattedEta());
            } else {
                return String.format("%d%% - %s (Elapsed: %s)",
                    percent, message, formatDuration(elapsed));
            }
        }
        
        private String formatDuration(Duration duration) {
            if (duration == null) return "0s";
            long seconds = duration.getSeconds();
            if (seconds < 60) return seconds + "s";
            if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
            return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        }
    }
}