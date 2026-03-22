package com.noty215.notycaption.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Audio enhancement using Spleeter via Python subprocess
 */
public class AudioEnhancer {

    private static final Logger logger = LoggerFactory.getLogger(AudioEnhancer.class);

    private File inputFile;
    private File outputDir;
    private Process process;
    private volatile boolean canceled;

    public AudioEnhancer(File inputFile, File outputDir) {
        this.inputFile = inputFile;
        this.outputDir = outputDir;
        this.canceled = false;
    }

    public File enhance() throws Exception {
        return enhanceWithSpleeter();
    }

    private File enhanceWithSpleeter() throws Exception {
        // Create output directory
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        String baseName = inputFile.getName().replaceFirst("[.][^.]+$", "");
        File vocalsFile = new File(outputDir, baseName + File.separator + "vocals.wav");

        // Check if already enhanced
        if (vocalsFile.exists()) {
            logger.info("Enhanced audio already exists: {}", vocalsFile.getAbsolutePath());
            return vocalsFile;
        }

        logger.info("Starting Spleeter separation for: {}", inputFile.getAbsolutePath());

        // Use Python to run Spleeter
        ProcessBuilder pb = new ProcessBuilder(
                "python", "-c",
                "import spleeter; from spleeter.separator import Separator; " +
                        "separator = Separator('spleeter:2stems'); " +
                        "separator.separate_to_file('" + inputFile.getAbsolutePath().replace("\\", "/") + "', '" +
                        outputDir.getAbsolutePath().replace("\\", "/") + "', synchronous=True)"
        );

        pb.redirectErrorStream(true);
        process = pb.start();

        // Read output
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.debug("Spleeter: {}", line);
                if (canceled) {
                    process.destroy();
                    throw new InterruptedException("Enhancement canceled");
                }
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
            return vocalsFileAlt;
        }

        // Try alternative location
        File vocalsFileRoot = new File(outputDir, "vocals.wav");
        if (vocalsFileRoot.exists()) {
            return vocalsFileRoot;
        }

        throw new FileNotFoundException("Vocals file not generated");
    }

    public void cancel() {
        canceled = true;
        if (process != null && process.isAlive()) {
            process.destroy();
            logger.info("Spleeter process terminated");
        }
    }

    public boolean isRunning() {
        return process != null && process.isAlive();
    }
}