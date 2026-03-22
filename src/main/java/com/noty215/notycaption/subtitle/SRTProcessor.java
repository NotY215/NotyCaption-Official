package com.noty215.notycaption.subtitle;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * SRT subtitle processor
 */
public class SRTProcessor {

    public static List<SubtitleEntry> parse(File file) throws IOException {
        List<SubtitleEntry> subtitles = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
            String line;
            int index = 0;
            SubtitleEntry current = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    if (current != null) {
                        subtitles.add(current);
                        current = null;
                    }
                    continue;
                }

                if (index == 0) {
                    // Index line
                    try {
                        current = new SubtitleEntry();
                        current.setIndex(Integer.parseInt(line));
                        index++;
                    } catch (NumberFormatException e) {
                        // Not a number, might be continuation of text
                        if (current != null) {
                            current.setText(current.getText() + "\n" + line);
                        }
                    }
                } else if (index == 1) {
                    // Timestamp line
                    String[] parts = line.split(" --> ");
                    if (parts.length == 2) {
                        current.setStart(parseTimestamp(parts[0]));
                        current.setEnd(parseTimestamp(parts[1]));
                    }
                    index++;
                } else {
                    // Text lines
                    if (current.getText() == null) {
                        current.setText(line);
                    } else {
                        current.setText(current.getText() + "\n" + line);
                    }
                }
            }

            if (current != null) {
                subtitles.add(current);
            }
        }

        return subtitles;
    }

    public static void write(List<SubtitleEntry> subtitles, File file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath())) {
            for (SubtitleEntry entry : subtitles) {
                writer.write(String.valueOf(entry.getIndex()));
                writer.newLine();
                writer.write(formatTimestamp(entry.getStart()) + " --> " + formatTimestamp(entry.getEnd()));
                writer.newLine();
                writer.write(entry.getText());
                writer.newLine();
                writer.newLine();
            }
        }
    }

    private static double parseTimestamp(String timestamp) {
        // Format: HH:MM:SS,mmm or HH:MM:SS.mmm
        String[] parts = timestamp.split("[,.]");
        if (parts.length >= 4) {
            long hours = Long.parseLong(parts[0]);
            long minutes = Long.parseLong(parts[1]);
            long seconds = Long.parseLong(parts[2]);
            long millis = Long.parseLong(parts[3]);
            return hours * 3600 + minutes * 60 + seconds + millis / 1000.0;
        }
        return 0;
    }

    private static String formatTimestamp(double seconds) {
        Duration duration = Duration.ofMillis((long) (seconds * 1000));
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long secs = duration.getSeconds() % 60;
        long millis = duration.toMillis() % 1000;
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, secs, millis);
    }

    public static void shiftTimings(List<SubtitleEntry> subtitles, double offset) {
        for (SubtitleEntry entry : subtitles) {
            entry.setStart(entry.getStart() + offset);
            entry.setEnd(entry.getEnd() + offset);
        }
    }

    public static void adjustDuration(List<SubtitleEntry> subtitles, double factor) {
        for (SubtitleEntry entry : subtitles) {
            double duration = entry.getEnd() - entry.getStart();
            duration *= factor;
            entry.setEnd(entry.getStart() + duration);
        }
    }
}