package com.noty215.notycaption.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper for Spleeter separation
 */
public class SpleeterWrapper {

    private static final Logger logger = LoggerFactory.getLogger(SpleeterWrapper.class);
    private static boolean spleeterAvailable = false;
    private static String pythonCommand = "python";

    static {
        // Try to find Python
        try {
            Process process = Runtime.getRuntime().exec("python --version");
            process.waitFor(2, TimeUnit.SECONDS);
            if (process.exitValue() == 0) {
                spleeterAvailable = true;
                pythonCommand = "python";
            } else {
                process = Runtime.getRuntime().exec("python3 --version");
                process.waitFor(2, TimeUnit.SECONDS);
                if (process.exitValue() == 0) {
                    spleeterAvailable = true;
                    pythonCommand = "python3";
                }
            }

            if (spleeterAvailable) {
                // Check if spleeter is installed
                process = Runtime.getRuntime().exec(pythonCommand + " -c \"import spleeter\"");
                process.waitFor(2, TimeUnit.SECONDS);
                spleeterAvailable = (process.exitValue() == 0);
                if (spleeterAvailable) {
                    logger.info("Spleeter available");
                } else {
                    logger.warn("Spleeter not installed");
                }
            }
        } catch (Exception e) {
            spleeterAvailable = false;
            logger.warn("Spleeter not available: {}", e.getMessage());
        }
    }

    public static boolean isAvailable() {
        return spleeterAvailable;
    }

    public static File separateVocals(File audioFile, File outputDir) throws Exception {
        if (!spleeterAvailable) {
            throw new RuntimeException("Spleeter not available");
        }

        String baseName = audioFile.getName().replaceFirst("[.][^.]+$", "");
        File vocalsFile = new File(outputDir, baseName + File.separator + "vocals.wav");

        // Check if already separated
        if (vocalsFile.exists()) {
            logger.info("Vocals file already exists: {}", vocalsFile.getAbsolutePath());
            return vocalsFile;
        }

        logger.info("Starting Spleeter separation for: {}", audioFile.getAbsolutePath());

        // Use Spleeter to separate vocals
        ProcessBuilder pb = new ProcessBuilder(
                pythonCommand, "-c",
                "import spleeter; from spleeter.separator import Separator; " +
                        "separator = Separator('spleeter:2stems'); " +
                        "separator.separate_to_file('" + audioFile.getAbsolutePath().replace("\\", "/") + "', '" +
                        outputDir.getAbsolutePath().replace("\\", "/") + "', synchronous=True)"
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Read output
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.debug("Spleeter: {}", line);
            }
        }

        boolean success = process.waitFor(10, TimeUnit.MINUTES);
        if (!success) {
            process.destroy();
            throw new RuntimeException("Spleeter timed out");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Spleeter failed with exit code: " + exitCode);
        }

        // Check for vocals file
        File vocalsDir = new File(outputDir, baseName);
        File vocalsFileAlt = new File(vocalsDir, "vocals.wav");
        if (vocalsFileAlt.exists()) {
            logger.info("Vocals extracted to: {}", vocalsFileAlt.getAbsolutePath());
            return vocalsFileAlt;
        }

        // Try alternative location
        File vocalsFileRoot = new File(outputDir, "vocals.wav");
        if (vocalsFileRoot.exists()) {
            logger.info("Vocals extracted to: {}", vocalsFileRoot.getAbsolutePath());
            return vocalsFileRoot;
        }

        throw new RuntimeException("Vocals file not generated");
    }

    public static File separateBackground(File audioFile, File outputDir) throws Exception {
        if (!spleeterAvailable) {
            throw new RuntimeException("Spleeter not available");
        }

        String baseName = audioFile.getName().replaceFirst("[.][^.]+$", "");
        File backgroundFile = new File(outputDir, baseName + File.separator + "accompaniment.wav");

        // Check if already separated
        if (backgroundFile.exists()) {
            logger.info("Background file already exists: {}", backgroundFile.getAbsolutePath());
            return backgroundFile;
        }

        logger.info("Starting Spleeter separation for: {}", audioFile.getAbsolutePath());

        // Use Spleeter to separate background
        ProcessBuilder pb = new ProcessBuilder(
                pythonCommand, "-c",
                "import spleeter; from spleeter.separator import Separator; " +
                        "separator = Separator('spleeter:2stems'); " +
                        "separator.separate_to_file('" + audioFile.getAbsolutePath().replace("\\", "/") + "', '" +
                        outputDir.getAbsolutePath().replace("\\", "/") + "', synchronous=True)"
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Read output
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.debug("Spleeter: {}", line);
            }
        }

        boolean success = process.waitFor(10, TimeUnit.MINUTES);
        if (!success) {
            process.destroy();
            throw new RuntimeException("Spleeter timed out");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Spleeter failed with exit code: " + exitCode);
        }

        // Check for accompaniment file
        File vocalsDir = new File(outputDir, baseName);
        File backgroundFileAlt = new File(vocalsDir, "accompaniment.wav");
        if (backgroundFileAlt.exists()) {
            logger.info("Background extracted to: {}", backgroundFileAlt.getAbsolutePath());
            return backgroundFileAlt;
        }

        throw new RuntimeException("Background file not generated");
    }
}