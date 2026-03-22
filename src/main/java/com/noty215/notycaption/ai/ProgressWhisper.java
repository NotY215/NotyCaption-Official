package com.noty215.notycaption.ai;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

public class ProgressWhisper {
    private static final Logger logger = Logger.getLogger(ProgressWhisper.class.getName());
    private final AtomicBoolean canceled;
    private final AtomicBoolean paused;
    private BiConsumer<Integer, String> progressCallback;
    private int currentProgress;
    private String currentMessage;

    public ProgressWhisper() {
        this.canceled = new AtomicBoolean(false);
        this.paused = new AtomicBoolean(false);
        this.currentProgress = 0;
        this.currentMessage = "";
    }

    public ProgressWhisper(BiConsumer<Integer, String> progressCallback) {
        this();
        this.progressCallback = progressCallback;
    }

    public void cancel() {
        canceled.set(true);
        logger.info("Transcription cancellation requested");
    }

    public boolean isCanceled() {
        return canceled.get();
    }

    public void pause() {
        paused.set(true);
        logger.info("Transcription paused");
    }

    public void resume() {
        paused.set(false);
        logger.info("Transcription resumed");
    }

    public boolean isPaused() {
        return paused.get();
    }

    public void updateProgress(int progress, String message) {
        this.currentProgress = progress;
        this.currentMessage = message;

        if (progressCallback != null && !canceled.get()) {
            progressCallback.accept(progress, message);
        }

        logger.fine(String.format("Progress: %d%% - %s", progress, message));
    }

    public void updateProgress(int progress) {
        updateProgress(progress, currentMessage);
    }

    public void updateMessage(String message) {
        updateProgress(currentProgress, message);
    }

    public int getCurrentProgress() {
        return currentProgress;
    }

    public String getCurrentMessage() {
        return currentMessage;
    }

    public void reset() {
        canceled.set(false);
        paused.set(false);
        currentProgress = 0;
        currentMessage = "";
    }

    public WhisperModel.TranscriptionResult transcribeWithProgress(WhisperModel model, String audioPath,
                                                                   WhisperModel.TranscriptionOptions options) throws Exception {
        if (canceled.get()) {
            throw new Exception("Transcription canceled by user");
        }

        updateProgress(5, "Loading audio...");

        // Simulate language detection
        if (options.getLanguage() == null) {
            updateProgress(10, "Detecting language...");
            WhisperModel.LanguageDetectionResult langResult = model.detectLanguage(new java.io.File(audioPath));
            options.setLanguage(langResult.getDetectedLanguage());
            updateProgress(15, "Detected language: " + options.getLanguage());
        }

        if (canceled.get()) {
            throw new Exception("Transcription canceled by user");
        }

        updateProgress(20, "Starting transcription...");

        WhisperModel.TranscriptionResult result = model.transcribe(new java.io.File(audioPath), options);

        int totalSegments = result.getSegments().size();
        for (int i = 0; i < totalSegments; i++) {
            if (canceled.get()) {
                throw new Exception("Transcription canceled by user");
            }

            while (paused.get()) {
                Thread.sleep(100);
            }

            int progress = 20 + (int)((i / (double) totalSegments) * 60);
            updateProgress(progress, "Processing segment " + (i + 1) + "/" + totalSegments);
        }

        updateProgress(90, "Finalizing...");

        return result;
    }

    public static class ProgressInfo {
        private int percent;
        private String message;
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

        public double getSpeed() { return speed; }
        public void setSpeed(double speed) { this.speed = speed; }

        public String getEta() { return eta; }
        public void setEta(String eta) { this.eta = eta; }
    }
}