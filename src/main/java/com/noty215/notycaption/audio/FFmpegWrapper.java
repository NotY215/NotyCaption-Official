package com.noty215.notycaption.audio;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class FFmpegWrapper {
    private static final Logger logger = Logger.getLogger(FFmpegWrapper.class.getName());
    private static String ffmpegPath = "ffmpeg";
    private static boolean available = false;

    static {
        checkAvailability();
    }

    private static void checkAvailability() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{ffmpegPath, "-version"});
            process.waitFor();
            available = process.exitValue() == 0;
            logger.info("FFmpeg " + (available ? "available" : "not available"));
        } catch (Exception e) {
            logger.warning("FFmpeg not found: " + e.getMessage());
            available = false;
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    public static void setFFmpegPath(String path) {
        ffmpegPath = path;
        checkAvailability();
    }

    public static boolean extractAudio(String videoPath, String outputPath, String format) {
        if (!available) {
            logger.severe("FFmpeg not available for audio extraction");
            return false;
        }

        try {
            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-i");
            command.add(videoPath);
            command.add("-vn");
            command.add("-acodec");
            command.add("pcm_s16le");
            command.add("-ar");
            command.add("16000");
            command.add("-ac");
            command.add("1");
            command.add("-y");
            command.add(outputPath);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // Read output
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.fine("FFmpeg: " + line);
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                logger.info("Audio extracted successfully: " + outputPath);
                return true;
            } else {
                logger.warning("FFmpeg extraction failed with exit code: " + exitCode);
                return false;
            }
        } catch (Exception e) {
            logger.severe("Error extracting audio: " + e.getMessage());
            return false;
        }
    }

    public static boolean convertAudio(String inputPath, String outputPath, int sampleRate, int channels) {
        if (!available) return false;

        try {
            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-i");
            command.add(inputPath);
            command.add("-acodec");
            command.add("pcm_s16le");
            command.add("-ar");
            command.add(String.valueOf(sampleRate));
            command.add("-ac");
            command.add(String.valueOf(channels));
            command.add("-y");
            command.add(outputPath);

            Process process = Runtime.getRuntime().exec(command.toArray(new String[0]));
            process.waitFor();
            
            if (process.exitValue() == 0) {
                logger.info("Audio converted: " + outputPath);
                return true;
            }
        } catch (Exception e) {
            logger.severe("Error converting audio: " + e.getMessage());
        }
        
        return false;
    }

    public static boolean getAudioInfo(String audioPath, AudioInfo info) {
        if (!available) return false;

        try {
            Process process = Runtime.getRuntime().exec(
                new String[]{ffmpegPath, "-i", audioPath});
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Duration:")) {
                    String duration = line.split("Duration:")[1].split(",")[0].trim();
                    info.duration = parseDuration(duration);
                }
                if (line.contains("Audio:")) {
                    String[] parts = line.split(",");
                    for (String part : parts) {
                        if (part.trim().endsWith("Hz")) {
                            info.sampleRate = Integer.parseInt(part.trim().replace("Hz", ""));
                        }
                        if (part.trim().contains("channels")) {
                            info.channels = Integer.parseInt(part.trim().replace("channels", "").trim());
                        }
                    }
                }
            }
            
            process.waitFor();
            return true;
        } catch (Exception e) {
            logger.warning("Error getting audio info: " + e.getMessage());
            return false;
        }
    }

    private static double parseDuration(String duration) {
        String[] parts = duration.split(":");
        double hours = Double.parseDouble(parts[0]);
        double minutes = Double.parseDouble(parts[1]);
        double seconds = Double.parseDouble(parts[2]);
        return hours * 3600 + minutes * 60 + seconds;
    }

    public static class AudioInfo {
        public double duration;
        public int sampleRate;
        public int channels;
    }
}