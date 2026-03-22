package com.noty215.notycaption.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Audio processing utilities using FFmpeg
 */
public class AudioProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AudioProcessor.class);

    public static File extractAudioFromVideo(File videoFile, File outputDir) throws Exception {
        String baseName = videoFile.getName().replaceFirst("[.][^.]+$", "");
        File audioFile = new File(outputDir, baseName + ".temp.wav");

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-i", videoFile.getAbsolutePath(),
                "-vn", "-acodec", "pcm_s16le", "-ar", "16000",
                "-ac", "1", audioFile.getAbsolutePath(), "-y"
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
            throw new RuntimeException("FFmpeg failed with exit code: " + exitCode);
        }

        if (!audioFile.exists()) {
            throw new FileNotFoundException("Audio file not generated");
        }

        logger.info("Audio extracted to: {}", audioFile.getAbsolutePath());
        return audioFile;
    }

    public static File convertToWav(File audioFile, File outputDir) throws Exception {
        String baseName = audioFile.getName().replaceFirst("[.][^.]+$", "");
        File wavFile = new File(outputDir, baseName + ".temp.wav");

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-i", audioFile.getAbsolutePath(),
                "-acodec", "pcm_s16le", "-ar", "16000",
                "-ac", "1", wavFile.getAbsolutePath(), "-y"
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
            throw new RuntimeException("FFmpeg failed with exit code: " + exitCode);
        }

        if (!wavFile.exists()) {
            throw new FileNotFoundException("WAV file not generated");
        }

        logger.info("Audio converted to: {}", wavFile.getAbsolutePath());
        return wavFile;
    }

    public static boolean isVideoFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") ||
                name.endsWith(".mov") || name.endsWith(".webm") || name.endsWith(".flv") ||
                name.endsWith(".wmv");
    }

    public static boolean isAudioFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a") ||
                name.endsWith(".aac") || name.endsWith(".flac") || name.endsWith(".ogg") ||
                name.endsWith(".wma");
    }

    public static long getAudioDuration(File audioFile) throws Exception {
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

    public static File normalizeAudio(File audioFile, File outputDir) throws Exception {
        String baseName = audioFile.getName().replaceFirst("[.][^.]+$", "");
        File normalizedFile = new File(outputDir, baseName + "_normalized.wav");

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-i", audioFile.getAbsolutePath(),
                "-af", "loudnorm=I=-16:LRA=11:TP=-1.5",
                "-ar", "16000", "-ac", "1", normalizedFile.getAbsolutePath(), "-y"
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
            throw new RuntimeException("FFmpeg normalization failed with exit code: " + exitCode);
        }

        return normalizedFile;
    }
}