package com.noty215.notycaption.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Tracks progress for long-running operations
 */
public class ProgressTracker {

    private int progress; // 0-100
    private String status;
    private Instant startTime;
    private double speed; // bytes per second or items per second
    private String eta;
    private boolean canceled;
    private final List<Consumer<ProgressUpdate>> listeners;

    public ProgressTracker() {
        this.progress = 0;
        this.status = "Starting...";
        this.startTime = Instant.now();
        this.speed = 0;
        this.eta = "calculating...";
        this.canceled = false;
        this.listeners = new ArrayList<>();
    }

    public synchronized void updateProgress(int progress) {
        this.progress = Math.min(100, Math.max(0, progress));
        updateETA();
        notifyListeners();
    }

    public synchronized void updateStatus(String status) {
        this.status = status;
        notifyListeners();
    }

    public synchronized void updateSpeed(double speed) {
        this.speed = speed;
        updateETA();
        notifyListeners();
    }

    private synchronized void updateETA() {
        if (progress > 0 && progress < 100 && startTime != null) {
            long elapsed = Duration.between(startTime, Instant.now()).getSeconds();
            if (elapsed > 0) {
                long remainingSeconds = (long) (elapsed * (100 - progress) / progress);
                this.eta = formatDuration(remainingSeconds);
            } else {
                this.eta = "calculating...";
            }
        } else if (progress >= 100) {
            this.eta = "completed";
        } else {
            this.eta = "calculating...";
        }
    }

    public synchronized long getStartTime() {
        return startTime.toEpochMilli();
    }

    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }

    public synchronized void cancel() {
        this.canceled = true;
        notifyListeners();
    }

    public synchronized boolean isCanceled() {
        return canceled;
    }

    public synchronized void reset() {
        this.progress = 0;
        this.status = "Ready";
        this.startTime = Instant.now();
        this.speed = 0;
        this.eta = "calculating...";
        this.canceled = false;
        notifyListeners();
    }

    public synchronized void addListener(Consumer<ProgressUpdate> listener) {
        listeners.add(listener);
    }

    public synchronized void removeListener(Consumer<ProgressUpdate> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        ProgressUpdate update = new ProgressUpdate(progress, status, speed, eta);
        for (Consumer<ProgressUpdate> listener : listeners) {
            try {
                listener.accept(update);
            } catch (Exception e) {
                // Ignore listener errors
            }
        }
    }

    public synchronized int getProgress() {
        return progress;
    }

    public synchronized String getStatus() {
        return status;
    }

    public synchronized double getSpeed() {
        return speed;
    }

    public synchronized String getETA() {
        return eta;
    }

    /**
     * Progress update data class
     */
    public static class ProgressUpdate {
        private final int progress;
        private final String status;
        private final double speed;
        private final String eta;

        public ProgressUpdate(int progress, String status, double speed, String eta) {
            this.progress = progress;
            this.status = status;
            this.speed = speed;
            this.eta = eta;
        }

        public int getProgress() { return progress; }
        public String getStatus() { return status; }
        public double getSpeed() { return speed; }
        public String getETA() { return eta; }

        public String getSpeedString() {
            if (speed > 1024 * 1024) {
                return String.format("%.1f MB/s", speed / (1024 * 1024));
            } else if (speed > 1024) {
                return String.format("%.1f KB/s", speed / 1024);
            } else {
                return String.format("%.1f B/s", speed);
            }
        }
    }
}