package com.noty215.notycaption.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for FFmpeg command line operations
 */
public class FFmpegWrapper {

    private static final Logger logger = LoggerFactory.getLogger(FFmpegWrapper.class);
    private static boolean ffmpegAvailable = false;

    static {
        try {
            Process process = Runtime.getRuntime().exec("ffmpeg -version");
            int exitCode = process.waitFor();
            ffmpegAvailable = (exitCode == 0);
            if (ffmpegAvailable) {
                logger.info("FFmpeg available");
            } else {
                logger.warn("FFmpeg not available - some features will be limited");
            }
        } catch (Exception e) {
            ffmpegAvailable = false;
            logger.warn("FFmpeg not available: {}", e.getMessage());
        }
    }

    public static boolean isAvailable() {
        return ffmpegAvailable;
    }

    public static String getVersion() {
        if (!ffmpegAvailable) return "Not available";

        try {
            Process process = Runtime.getRuntime().exec("ffmpeg -version");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String firstLine = reader.readLine();
            reader.close();
            process.waitFor();
            return firstLine != null ? firstLine : "Unknown";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public static File extractAudio(File inputFile, File outputFile, String format, int sampleRate, int channels) throws Exception {
        if (!ffmpegAvailable) {
            throw new RuntimeException("FFmpeg not available");
        }

        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-i");
        command.add(inputFile.getAbsolutePath());
        command.add("-vn");

        if (format != null) {
            command.add("-acodec");
            command.add(format);
        }

        if (sampleRate > 0) {
            command.add("-ar");
            command.add(String.valueOf(sampleRate));
        }

        if (channels > 0) {
            command.add("-ac");
            command.add(String.valueOf(channels));
        }

        command.add("-y");
        command.add(outputFile.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.debug("FFmpeg: {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg failed with exit code: " + exitCode);
        }

        return outputFile;
    }

    public static File applyAudioFilter(File inputFile, File outputFile, String filter) throws Exception {
        if (!ffmpegAvailable) {
            throw new RuntimeException("FFmpeg not available");
        }

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-i", inputFile.getAbsolutePath(),
                "-af", filter, outputFile.getAbsolutePath(), "-y"
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.debug("FFmpeg: {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg filter failed with exit code: " + exitCode);
        }

        return outputFile;
    }

    public static File normalizeAudio(File inputFile, File outputFile) throws Exception {
        return applyAudioFilter(inputFile, outputFile, "loudnorm=I=-16:LRA=11:TP=-1.5");
    }

    public static File changeSpeed(File inputFile, File outputFile, double speed) throws Exception {
        return applyAudioFilter(inputFile, outputFile, "atempo=" + speed);
    }

    public static File fadeInOut(File inputFile, File outputFile, double fadeInSeconds, double fadeOutSeconds) throws Exception {
        String filter = String.format("afade=t=in:st=0:d=%f,afade=t=out:st=%f:d=%f",
                fadeInSeconds,
                (getDuration(inputFile) / 1000.0) - fadeOutSeconds,
                fadeOutSeconds);
        return applyAudioFilter(inputFile, outputFile, filter);
    }

    public static long getDuration(File audioFile) throws Exception {
        if (!ffmpegAvailable) return 0;

        ProcessBuilder pb = new ProcessBuilder(
                "ffprobe", "-v", "error", "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1", audioFile.getAbsolutePath()
        );

        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            if (line != null) {
                return (long) (Double.parseDouble(line.trim()) * 1000);
            }
        }

        process.waitFor();
        return 0;
    }
}