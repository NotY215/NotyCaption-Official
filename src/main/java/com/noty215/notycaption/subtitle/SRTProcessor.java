package com.noty215.notycaption.subtitle;

import java.io.*;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class SRTProcessor {
    private static final Logger logger = Logger.getLogger(SRTProcessor.class.getName());
    private static final DateTimeFormatter TIME_FORMAT = 
        DateTimeFormatter.ofPattern("HH:mm:ss,SSS");

    public static List<SubtitleEntry> parse(String content) {
        List<SubtitleEntry> subtitles = new ArrayList<>();
        String[] blocks = content.split("\n\n");
        
        for (String block : blocks) {
            String[] lines = block.split("\n");
            if (lines.length >= 3) {
                try {
                    int index = Integer.parseInt(lines[0].trim());
                    String[] timeParts = lines[1].split(" --> ");
                    LocalTime start = parseTime(timeParts[0]);
                    LocalTime end = parseTime(timeParts[1]);
                    
                    StringBuilder text = new StringBuilder();
                    for (int i = 2; i < lines.length; i++) {
                        if (text.length() > 0) text.append("\n");
                        text.append(lines[i]);
                    }
                    
                    SubtitleEntry entry = new SubtitleEntry();
                    entry.setIndex(index);
                    entry.setStart(Duration.ofNanos(start.toNanoOfDay()));
                    entry.setEnd(Duration.ofNanos(end.toNanoOfDay()));
                    entry.setText(text.toString());
                    subtitles.add(entry);
                } catch (Exception e) {
                    logger.warning("Error parsing SRT block: " + e.getMessage());
                }
            }
        }
        
        return subtitles;
    }
    
    private static LocalTime parseTime(String timeStr) {
        String[] parts = timeStr.split(",");
        String time = parts[0];
        int millis = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        
        String[] timeParts = time.split(":");
        int hours = Integer.parseInt(timeParts[0]);
        int minutes = Integer.parseInt(timeParts[1]);
        int seconds = Integer.parseInt(timeParts[2]);
        
        return LocalTime.of(hours, minutes, seconds, millis * 1000000);
    }
    
    public static String format(List<SubtitleEntry> subtitles) {
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < subtitles.size(); i++) {
            SubtitleEntry entry = subtitles.get(i);
            sb.append(i + 1).append("\n");
            sb.append(formatTime(entry.getStart())).append(" --> ")
              .append(formatTime(entry.getEnd())).append("\n");
            sb.append(entry.getText()).append("\n");
            if (i < subtitles.size() - 1) {
                sb.append("\n");
            }
        }
        
        return sb.toString();
    }
    
    private static String formatTime(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        long millis = duration.toMillis() % 1000;
        
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis);
    }
    
    public static void saveToFile(List<SubtitleEntry> subtitles, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), "UTF-8"))) {
            writer.write(format(subtitles));
        }
        logger.info("SRT saved to: " + file.getAbsolutePath());
    }
    
    public static List<SubtitleEntry> loadFromFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (content.length() > 0) content.append("\n");
                content.append(line);
            }
        }
        return parse(content.toString());
    }
    
    public static List<SubtitleEntry> mergeSubtitles(List<SubtitleEntry> subs1, List<SubtitleEntry> subs2) {
        List<SubtitleEntry> merged = new ArrayList<>();
        int i = 0, j = 0;
        
        while (i < subs1.size() && j < subs2.size()) {
            SubtitleEntry s1 = subs1.get(i);
            SubtitleEntry s2 = subs2.get(j);
            
            if (s1.getStart().compareTo(s2.getStart()) <= 0) {
                merged.add(s1);
                i++;
            } else {
                merged.add(s2);
                j++;
            }
        }
        
        while (i < subs1.size()) {
            merged.add(subs1.get(i++));
        }
        
        while (j < subs2.size()) {
            merged.add(subs2.get(j++));
        }
        
        // Update indices
        for (int k = 0; k < merged.size(); k++) {
            merged.get(k).setIndex(k + 1);
        }
        
        return merged;
    }
    
    public static void shiftTimings(List<SubtitleEntry> subtitles, Duration offset) {
        for (SubtitleEntry entry : subtitles) {
            entry.setStart(entry.getStart().plus(offset));
            entry.setEnd(entry.getEnd().plus(offset));
        }
    }
    
    public static void scaleTimings(List<SubtitleEntry> subtitles, double factor) {
        for (SubtitleEntry entry : subtitles) {
            long startNanos = (long) (entry.getStart().toNanos() * factor);
            long endNanos = (long) (entry.getEnd().toNanos() * factor);
            entry.setStart(Duration.ofNanos(startNanos));
            entry.setEnd(Duration.ofNanos(endNanos));
        }
    }
}