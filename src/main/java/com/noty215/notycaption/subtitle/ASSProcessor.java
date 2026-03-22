package com.noty215.notycaption.subtitle;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * ASS/SSA subtitle processor
 */
public class ASSProcessor {

    public static List<SubtitleEntry> parse(File file) throws IOException {
        List<SubtitleEntry> subtitles = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
            String line;
            int index = 1;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Dialogue:")) {
                    String[] parts = line.split(",");
                    if (parts.length >= 10) {
                        SubtitleEntry entry = new SubtitleEntry();
                        entry.setIndex(index++);
                        entry.setStart(parseTimestamp(parts[1]));
                        entry.setEnd(parseTimestamp(parts[2]));

                        // Text starts after the 9th comma
                        StringBuilder text = new StringBuilder();
                        for (int i = 9; i < parts.length; i++) {
                            if (i > 9) text.append(",");
                            text.append(parts[i]);
                        }
                        entry.setText(text.toString().replaceAll("\\{\\\\.+?\\}", "")); // Remove ASS tags

                        subtitles.add(entry);
                    }
                }
            }
        }

        return subtitles;
    }

    public static void write(List<SubtitleEntry> subtitles, File file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath())) {
            // Write header
            writer.write("[Script Info]");
            writer.newLine();
            writer.write("Title: NotyCaption Pro Subtitles");
            writer.newLine();
            writer.write("ScriptType: v4.00+");
            writer.newLine();
            writer.write("Collisions: Normal");
            writer.newLine();
            writer.write("PlayResX: 1920");
            writer.newLine();
            writer.write("PlayResY: 1080");
            writer.newLine();
            writer.write("Timer: 100.0000");
            writer.newLine();
            writer.newLine();

            writer.write("[V4+ Styles]");
            writer.newLine();
            writer.write("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding");
            writer.newLine();
            writer.write("Style: Default,Arial,20,&H00FFFFFF,&H0000FFFF,&H00000000,&H80000000,0,0,0,0,100,100,0,0,1,2,0,2,20,20,20,1");
            writer.newLine();
            writer.newLine();

            writer.write("[Events]");
            writer.newLine();
            writer.write("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text");
            writer.newLine();

            for (SubtitleEntry entry : subtitles) {
                writer.write("Dialogue: 0,");
                writer.write(formatTimestamp(entry.getStart()) + ",");
                writer.write(formatTimestamp(entry.getEnd()) + ",");
                writer.write("Default,,0,0,0,,");
                writer.write(entry.getText());
                writer.newLine();
            }
        }
    }

    private static double parseTimestamp(String timestamp) {
        // Format: H:MM:SS.cc
        String[] parts = timestamp.split("[:.]");
        if (parts.length >= 4) {
            long hours = Long.parseLong(parts[0]);
            long minutes = Long.parseLong(parts[1]);
            long seconds = Long.parseLong(parts[2]);
            long centis = Long.parseLong(parts[3]);
            return hours * 3600 + minutes * 60 + seconds + centis / 100.0;
        }
        return 0;
    }

    private static String formatTimestamp(double seconds) {
        long totalSeconds = (long) seconds;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long secs = totalSeconds % 60;
        long centis = (long) ((seconds - totalSeconds) * 100);
        return String.format("%d:%02d:%02d.%02d", hours, minutes, secs, centis);
    }
}