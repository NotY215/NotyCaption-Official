package com.noty215.notycaption.ai;

import com.noty215.notycaption.utils.ModelValidator;
import com.noty215.notycaption.utils.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Downloads Whisper model from OpenAI
 */
public class ModelDownloader {

    private static final Logger logger = LoggerFactory.getLogger(ModelDownloader.class);
    private static final String MODEL_URL = "https://openaipublic.azureedge.net/main/whisper/models/";
    private static final String MODEL_FILE = "large-v1.pt";
    private static final long MODEL_EXPECTED_SIZE = 2_900_000_000L; // ~2.9 GB

    private final String modelsDir;
    private final ProgressTracker progressTracker;
    private final AtomicBoolean canceled;
    private Thread downloadThread;

    public ModelDownloader(String modelsDir) {
        this.modelsDir = modelsDir;
        this.progressTracker = new ProgressTracker();
        this.canceled = new AtomicBoolean(false);
    }

    public void download(DownloadCallback callback) {
        canceled.set(false);

        downloadThread = new Thread(() -> {
            try {
                Path modelPath = Path.of(modelsDir, MODEL_FILE);

                // Check if model already exists
                if (ModelValidator.validateModelFile(modelPath.toString())) {
                    logger.info("Model already exists: {}", modelPath);
                    progressTracker.updateStatus("Model already exists");
                    progressTracker.updateProgress(100);
                    if (callback != null) {
                        callback.onComplete(modelPath.toFile());
                    }
                    return;
                }

                // Create directory if needed
                Files.createDirectories(Path.of(modelsDir));

                // Download model
                URL url = new URL(MODEL_URL + MODEL_FILE);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.connect();

                long contentLength = connection.getContentLengthLong();
                progressTracker.updateStatus("Downloading model...");

                try (InputStream in = connection.getInputStream();
                     FileOutputStream out = new FileOutputStream(modelPath.toFile())) {

                    byte[] buffer = new byte[8192];
                    long downloaded = 0;
                    int bytesRead;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        if (canceled.get()) {
                            out.close();
                            Files.deleteIfExists(modelPath);
                            throw new InterruptedException("Download canceled");
                        }

                        out.write(buffer, 0, bytesRead);
                        downloaded += bytesRead;

                        if (contentLength > 0) {
                            int progress = (int) (downloaded * 100 / contentLength);
                            progressTracker.updateProgress(progress);

                            // Update speed and ETA
                            double speed = downloaded / (System.currentTimeMillis() / 1000.0);
                            progressTracker.updateSpeed(speed);
                        }
                    }
                }

                // Validate downloaded model
                if (ModelValidator.validateModelFile(modelPath.toString())) {
                    progressTracker.updateStatus("Download complete");
                    progressTracker.updateProgress(100);
                    if (callback != null) {
                        callback.onComplete(modelPath.toFile());
                    }
                } else {
                    Files.deleteIfExists(modelPath);
                    throw new IOException("Downloaded model is corrupt");
                }

            } catch (InterruptedException e) {
                logger.info("Download canceled");
                progressTracker.updateStatus("Canceled");
                if (callback != null) {
                    callback.onCanceled();
                }
            } catch (Exception e) {
                logger.error("Download failed", e);
                progressTracker.updateStatus("Failed: " + e.getMessage());
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });

        downloadThread.setDaemon(true);
        downloadThread.start();
    }

    public void cancel() {
        canceled.set(true);
        if (downloadThread != null) {
            downloadThread.interrupt();
        }
        progressTracker.cancel();
    }

    public boolean isDownloading() {
        return downloadThread != null && downloadThread.isAlive();
    }

    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    public interface DownloadCallback {
        void onComplete(File modelFile);
        void onError(Exception e);
        void onCanceled();
    }
}