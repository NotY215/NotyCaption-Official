package com.noty215.notycaption.ai;

import com.noty215.notycaption.subtitle.SubtitleEntry;
import com.noty215.notycaption.utils.ProgressTracker;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Whisper with progress tracking and cancellation
 */
public class ProgressWhisper {

    private final WhisperModel whisperModel;
    private final ProgressTracker progressTracker;
    private final AtomicBoolean canceled;
    private Thread transcriptionThread;

    public ProgressWhisper(WhisperModel whisperModel) {
        this.whisperModel = whisperModel;
        this.progressTracker = new ProgressTracker();
        this.canceled = new AtomicBoolean(false);
    }

    public void transcribe(File audioFile, String language, String task,
                           int wordsPerLine, TranscriptionCallback callback) {
        canceled.set(false);

        transcriptionThread = new Thread(() -> {
            try {
                progressTracker.updateStatus("Starting transcription...");
                progressTracker.updateProgress(0);

                List<SubtitleEntry> subtitles = whisperModel.transcribe(
                        audioFile, language, task, wordsPerLine,
                        (progress, message) -> {
                            if (canceled.get()) {
                                throw new RuntimeException("Canceled by user");
                            }
                            progressTracker.updateProgress(progress);
                            progressTracker.updateStatus(message);
                        }
                );

                if (canceled.get()) {
                    progressTracker.updateStatus("Canceled");
                    if (callback != null) {
                        callback.onCanceled();
                    }
                } else {
                    progressTracker.updateStatus("Complete");
                    progressTracker.updateProgress(100);
                    if (callback != null) {
                        callback.onComplete(subtitles);
                    }
                }
            } catch (Exception e) {
                progressTracker.updateStatus("Failed: " + e.getMessage());
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });

        transcriptionThread.setDaemon(true);
        transcriptionThread.start();
    }

    public void cancel() {
        canceled.set(true);
        if (transcriptionThread != null) {
            transcriptionThread.interrupt();
        }
        progressTracker.cancel();
    }

    public boolean isCanceled() {
        return canceled.get();
    }

    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    public interface TranscriptionCallback {
        void onComplete(List<SubtitleEntry> subtitles);
        void onError(Exception e);
        void onCanceled();
    }
}