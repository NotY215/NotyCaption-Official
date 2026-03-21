package com.noty215.notycaption.utils;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.logging.Logger;

public class ModelValidator {
    private static final Logger logger = Logger.getLogger(ModelValidator.class.getName());
    
    public static boolean validateModelFile(File modelFile) {
        if (!modelFile.exists()) {
            logger.warning("Model file does not exist: " + modelFile.getAbsolutePath());
            return false;
        }
        
        long fileSize = modelFile.length();
        long expectedSizeMin = 2_500_000_000L; // 2.5 GB
        long expectedSizeMax = 3_100_000_000L; // 3.1 GB
        
        if (fileSize < expectedSizeMin || fileSize > expectedSizeMax) {
            logger.warning("Model file size invalid: " + fileSize + " bytes");
            return false;
        }
        
        // Check file header for PyTorch format
        try (RandomAccessFile raf = new RandomAccessFile(modelFile, "r")) {
            byte[] header = new byte[4];
            raf.read(header);
            
            // PyTorch files typically start with "PK" (zip) or have specific magic numbers
            if (header[0] == 'P' && header[1] == 'K') {
                logger.info("Model file validated: " + formatSize(fileSize));
                return true;
            }
            
            // Check for TorchScript format
            raf.seek(0);
            byte[] torchHeader = new byte[7];
            raf.read(torchHeader);
            String torchHeaderStr = new String(torchHeader);
            if (torchHeaderStr.contains("torch")) {
                logger.info("Model file validated (TorchScript): " + formatSize(fileSize));
                return true;
            }
            
            logger.warning("Model file header validation failed");
            return false;
            
        } catch (Exception e) {
            logger.warning("Failed to validate model file: " + e.getMessage());
            return false;
        }
    }
    
    public static boolean cleanupCorruptModels(String modelsDir) {
        File dir = new File(modelsDir);
        if (!dir.exists()) {
            return false;
        }
        
        boolean removed = false;
        File[] modelFiles = dir.listFiles((d, name) -> 
            name.endsWith(".pt") || name.endsWith(".pt.tmp") || name.contains("large"));
        
        if (modelFiles != null) {
            for (File file : modelFiles) {
                if (file.getName().endsWith(".tmp") || !validateModelFile(file)) {
                    if (file.delete()) {
                        logger.info("Removed corrupt/incomplete model: " + file.getName());
                        removed = true;
                    }
                }
            }
        }
        
        return removed;
    }
    
    public static boolean isModelDownloadComplete(File tempFile, long expectedSize) {
        if (!tempFile.exists()) {
            return false;
        }
        
        long actualSize = tempFile.length();
        if (actualSize >= expectedSize * 0.99) {
            return true;
        }
        
        logger.warning("Download incomplete: " + formatSize(actualSize) + " / " + formatSize(expectedSize));
        return false;
    }
    
    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}