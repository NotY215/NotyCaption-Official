package com.noty215.notycaption.network;

import com.noty215.notycaption.ui.NotyCaptionWindow;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class OnlineHandler {
    private static final Logger logger = Logger.getLogger(OnlineHandler.class.getName());
    private NotyCaptionWindow parent;
    private GoogleDriveService driveService;
    private NotebookGenerator notebookGenerator;
    private AtomicBoolean canceled;
    private AtomicBoolean pollingActive;
    private String currentNotebookUrl;
    private String currentStatus;

    public OnlineHandler(NotyCaptionWindow parent) {
        this.parent = parent;
        this.driveService = new GoogleDriveService();
        this.notebookGenerator = new NotebookGenerator();
        this.canceled = new AtomicBoolean(false);
        this.pollingActive = new AtomicBoolean(false);
        this.currentStatus = "idle";
    }

    public void initiateLogin() {
        logger.info("Initiating Google login");
        driveService.login(parent);
    }

    public void loadExistingCredentials() {
        if (driveService.loadExistingCredentials()) {
            currentStatus = "ready";
            logger.info("Existing credentials loaded");
        }
    }

    public boolean isLoggedIn() {
        return driveService.isLoggedIn();
    }

    public void cancelOperation() {
        canceled.set(true);
        pollingActive.set(false);
        currentStatus = "canceled";
        logger.info("Online operation canceled");
    }

    public boolean handleOnline(String audioPath, String langCode, String task,
                                int wordsPerLine, String format, String baseName, String outputPath) {
        canceled.set(false);
        currentStatus = "uploading";
        updateStatus("uploading");

        try {
            // Upload audio file
            File audioFile = new File(audioPath);
            String audioId = driveService.uploadFile(audioFile, "uploads");
            if (audioId == null) {
                throw new Exception("Failed to upload audio file");
            }
            logger.info("Audio uploaded: " + audioId);

            if (canceled.get()) {
                cleanup(audioId, null);
                return false;
            }

            // Generate and upload notebook
            String notebookContent = notebookGenerator.generateNotebook(
                    audioFile.getName(), wordsPerLine, format, outputPath, langCode, task);
            String notebookId = driveService.uploadNotebook(notebookContent);
            if (notebookId == null) {
                throw new Exception("Failed to upload notebook");
            }
            logger.info("Notebook uploaded: " + notebookId);

            if (canceled.get()) {
                cleanup(audioId, notebookId);
                return false;
            }

            // Get Colab URL
            currentNotebookUrl = "https://colab.research.google.com/drive/" + notebookId;

            // Open in browser
            try {
                Desktop.getDesktop().browse(new URI(currentNotebookUrl));
            } catch (Exception e) {
                logger.warning("Failed to open browser: " + e.getMessage());
            }

            currentStatus = "waiting";
            updateStatus("waiting");

            // Start polling for output
            startPolling(outputPath);

            return true;

        } catch (Exception e) {
            logger.severe("Online handler error: " + e.getMessage());
            currentStatus = "failed";
            updateStatus("failed");
            return false;
        }
    }

    private void startPolling(String outputPath) {
        pollingActive.set(true);
        new Thread(() -> {
            int attempts = 0;
            int maxAttempts = 180;

            while (pollingActive.get() && !canceled.get() && attempts < maxAttempts) {
                try {
                    Thread.sleep(10000);
                    attempts++;

                    String outputId = driveService.findFile(outputPath);
                    if (outputId != null) {
                        currentStatus = "downloading";
                        updateStatus("downloading");

                        File outputFile = driveService.downloadFile(outputId, new File(outputPath));
                        if (outputFile != null && outputFile.exists()) {
                            pollingActive.set(false);
                            currentStatus = "completed";
                            updateStatus("completed");

                            // Load subtitles
                            JOptionPane.showMessageDialog(parent,
                                    "Captions downloaded successfully!\n" + outputFile.getAbsolutePath(),
                                    "Download Complete",
                                    JOptionPane.INFORMATION_MESSAGE);

                            // Cleanup
                            cleanup(null, null);
                        }
                    }
                } catch (Exception e) {
                    logger.warning("Polling error: " + e.getMessage());
                }
            }

            if (attempts >= maxAttempts && !canceled.get()) {
                currentStatus = "timeout";
                updateStatus("timeout");
                JOptionPane.showMessageDialog(parent,
                        "No result file found after 30 minutes.",
                        "Colab Timeout",
                        JOptionPane.WARNING_MESSAGE);
            }
        }).start();
    }

    private void cleanup(String audioId, String notebookId) {
        if (audioId != null) {
            driveService.deleteFile(audioId);
        }
        if (notebookId != null) {
            driveService.deleteFile(notebookId);
        }
    }

    private void updateStatus(String status) {
        currentStatus = status;
        // Status update would be handled by parent window
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public String getCurrentNotebookUrl() {
        return currentNotebookUrl;
    }
}