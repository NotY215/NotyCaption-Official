package com.noty215.notycaption.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Validates Whisper model files
 */
public class ModelValidator {

    private static final Logger logger = LoggerFactory.getLogger(ModelValidator.class);
    private static final long EXPECTED_MIN_SIZE = 2_500_000_000L; // 2.5 GB
    private static final long EXPECTED_MAX_SIZE = 3_100_000_000L; // 3.1 GB

    public static boolean validateModelFile(String modelPath) {
        File modelFile = new File(modelPath);

        if (!modelFile.exists()) {
            logger.info("Model file does not exist: {}", modelPath);
            return false;
        }

        try {
            long fileSize = modelFile.length();
            boolean valid = fileSize >= EXPECTED_MIN_SIZE && fileSize <= EXPECTED_MAX_SIZE;

            if (valid) {
                logger.info("Model file validated: {:.2f} GB", fileSize / (1024.0 * 1024 * 1024));
            } else {
                logger.warn("Model file size invalid: {:.2f} GB", fileSize / (1024.0 * 1024 * 1024));
            }

            return valid;
        } catch (Exception e) {
            logger.error("Error validating model file: {}", modelPath, e);
            return false;
        }
    }

    public static boolean cleanupCorruptModels(String modelsDir) {
        File dir = new File(modelsDir);
        if (!dir.exists()) {
            return false;
        }

        boolean removed = false;
        String[] modelFiles = {
                "large-v1.pt",
                "large.pt",
                "large-v1.pt.tmp"
        };

        for (String filename : modelFiles) {
            File modelFile = new File(dir, filename);
            if (modelFile.exists()) {
                try {
                    // Check if file is readable
                    try (RandomAccessFile raf = new RandomAccessFile(modelFile, "r")) {
                        raf.read();
                    } catch (IOException e) {
                        logger.warn("Cannot read model file: {}", modelFile.getPath());
                    }

                    if (!validateModelFile(modelFile.getPath()) || filename.endsWith(".tmp")) {
                        Files.delete(modelFile.toPath());
                        logger.info("Removed corrupt/incomplete model: {}", filename);
                        removed = true;
                    }
                } catch (IOException e) {
                    logger.warn("Cannot access model file: {}", modelFile.getPath());
                }
            }
        }

        return removed;
    }

    public static boolean isModelDownloaded(String modelsDir, String modelName) {
        String modelPath = modelsDir + File.separator + modelName + ".pt";
        return validateModelFile(modelPath);
    }

    public static long getModelSize(String modelPath) {
        File file = new File(modelPath);
        if (file.exists()) {
            return file.length();
        }
        return -1;
    }

    public static String getModelSizeString(String modelPath) {
        long size = getModelSize(modelPath);
        if (size > 0) {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
        return "Unknown";
    }
}