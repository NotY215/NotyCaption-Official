package com.noty215.notycaption.ai;

import com.noty215.notycaption.utils.ProgressTracker;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cancellable file downloader with progress tracking
 */
public class CancellableDownloader {

    private final ProgressTracker progressTracker;
    private final AtomicBoolean canceled;
    private Thread downloadThread;
    private HttpURLConnection connection;

    public CancellableDownloader() {
        this.progressTracker = new ProgressTracker();
        this.canceled = new AtomicBoolean(false);
    }

    public void download(String urlString, Path destination, DownloadCallback callback) {
        canceled.set(false);

        downloadThread = new Thread(() -> {
            try {
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.connect();

                long contentLength = connection.getContentLengthLong();
                progressTracker.updateStatus("Downloading...");

                // Create temporary file
                Path tempPath = destination.resolveSibling(destination.getFileName() + ".tmp");

                try (InputStream in = connection.getInputStream();
                     FileOutputStream out = new FileOutputStream(tempPath.toFile())) {

                    byte[] buffer = new byte[8192];
                    long downloaded = 0;
                    int bytesRead;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        if (canceled.get()) {
                            out.close();
                            Files.deleteIfExists(tempPath);
                            throw new InterruptedException("Download canceled");
                        }

                        out.write(buffer, 0, bytesRead);
                        downloaded += bytesRead;

                        if (contentLength > 0) {
                            int progress = (int) (downloaded * 100 / contentLength);
                            progressTracker.updateProgress(progress);

                            // Update speed and ETA
                            double elapsed = (System.currentTimeMillis() - progressTracker.getStartTime()) / 1000.0;
                            if (elapsed > 0) {
                                double speed = downloaded / elapsed;
                                progressTracker.updateSpeed(speed);
                            }
                        }
                    }
                }

                // Move temp file to destination
                Files.move(tempPath, destination);

                progressTracker.updateStatus("Complete");
                progressTracker.updateProgress(100);
                if (callback != null) {
                    callback.onComplete(destination.toFile());
                }

            } catch (InterruptedException e) {
                progressTracker.updateStatus("Canceled");
                if (callback != null) {
                    callback.onCanceled();
                }
            } catch (Exception e) {
                progressTracker.updateStatus("Failed: " + e.getMessage());
                if (callback != null) {
                    callback.onError(e);
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });

        downloadThread.setDaemon(true);
        downloadThread.start();
    }

    public void cancel() {
        canceled.set(true);
        if (connection != null) {
            connection.disconnect();
        }
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
        void onComplete(File file);
        void onError(Exception e);
        void onCanceled();
    }
}