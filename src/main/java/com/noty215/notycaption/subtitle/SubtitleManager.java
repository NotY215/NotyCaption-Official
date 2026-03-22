package com.noty215.notycaption.subtitle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages subtitle operations
 */
public class SubtitleManager {

    private static final Logger logger = LoggerFactory.getLogger(SubtitleManager.class);

    public void saveSRT(List<SubtitleEntry> subtitles, File outputFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"))) {
            for (SubtitleEntry entry : subtitles) {
                writer.write(String.valueOf(entry.getIndex()));
                writer.newLine();
                writer.write(formatSRTTime(entry.getStart()) + " --> " + formatSRTTime(entry.getEnd()));
                writer.newLine();
                writer.write(entry.getText());
                writer.newLine();
                writer.newLine();
            }
        }
        logger.info("Saved SRT file: {}", outputFile.getAbsolutePath());
    }

    public List<SubtitleEntry> loadSRT(File inputFile) throws IOException {
        List<SubtitleEntry> subtitles = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "UTF-8"))) {
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
                    current = new SubtitleEntry();
                    current.setIndex(Integer.parseInt(line));
                    index++;
                } else if (index == 1) {
                    // Timestamp line
                    String[] parts = line.split(" --> ");
                    if (parts.length == 2) {
                        current.setStart(parseSRTTime(parts[0]));
                        current.setEnd(parseSRTTime(parts[1]));
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

        logger.info("Loaded SRT file: {} ({} entries)", inputFile.getAbsolutePath(), subtitles.size());
        return subtitles;
    }

    public void saveASS(List<SubtitleEntry> subtitles, File outputFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"))) {
            // Write ASS header
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
                writer.write(formatASSTime(entry.getStart()) + ",");
                writer.write(formatASSTime(entry.getEnd()) + ",");
                writer.write("Default,,0,0,0,,");
                writer.write(entry.getText());
                writer.newLine();
            }
        }

        logger.info("Saved ASS file: {}", outputFile.getAbsolutePath());
    }

    public List<SubtitleEntry> loadASS(File inputFile) throws IOException {
        List<SubtitleEntry> subtitles = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "UTF-8"))) {
            String line;
            int index = 1;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Dialogue:")) {
                    String[] parts = line.split(",");
                    if (parts.length >= 10) {
                        SubtitleEntry entry = new SubtitleEntry();
                        entry.setIndex(index++);
                        entry.setStart(parseASSTime(parts[1]));
                        entry.setEnd(parseASSTime(parts[2]));

                        // Text starts after the 9th comma
                        StringBuilder text = new StringBuilder();
                        for (int i = 9; i < parts.length; i++) {
                            if (i > 9) text.append(",");
                            text.append(parts[i]);
                        }
                        entry.setText(text.toString());

                        subtitles.add(entry);
                    }
                }
            }
        }

        logger.info("Loaded ASS file: {} ({} entries)", inputFile.getAbsolutePath(), subtitles.size());
        return subtitles;
    }

    private String formatSRTTime(double seconds) {
        Duration duration = Duration.ofMillis((long) (seconds * 1000));
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long secs = duration.getSeconds() % 60;
        long millis = duration.toMillis() % 1000;
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, secs, millis);
    }

    private double parseSRTTime(String timeStr) {
        String[] parts = timeStr.split("[:,]");
        if (parts.length >= 4) {
            long hours = Long.parseLong(parts[0]);
            long minutes = Long.parseLong(parts[1]);
            long seconds = Long.parseLong(parts[2]);
            long millis = Long.parseLong(parts[3]);
            return hours * 3600 + minutes * 60 + seconds + millis / 1000.0;
        }
        return 0;
    }

    private String formatASSTime(double seconds) {
        Duration duration = Duration.ofMillis((long) (seconds * 1000));
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long secs = duration.getSeconds() % 60;
        long millis = duration.toMillis() % 1000;
        return String.format("%d:%02d:%02d.%02d", hours, minutes, secs, millis / 10);
    }

    private double parseASSTime(String timeStr) {
        String[] parts = timeStr.split("[:.]");
        if (parts.length >= 4) {
            long hours = Long.parseLong(parts[0]);
            long minutes = Long.parseLong(parts[1]);
            long seconds = Long.parseLong(parts[2]);
            long centis = Long.parseLong(parts[3]);
            return hours * 3600 + minutes * 60 + seconds + centis / 100.0;
        }
        return 0;
    }

    public void mergeSubtitles(List<SubtitleEntry> first, List<SubtitleEntry> second, File outputFile, String format) throws IOException {
        List<SubtitleEntry> merged = new ArrayList<>(first);
        int index = merged.size();

        for (SubtitleEntry entry : second) {
            entry.setIndex(++index);
            merged.add(entry);
        }

        merged.sort((a, b) -> Double.compare(a.getStart(), b.getStart()));

        for (int i = 0; i < merged.size(); i++) {
            merged.get(i).setIndex(i + 1);
        }

        if (format.equalsIgnoreCase("srt")) {
            saveSRT(merged, outputFile);
        } else {
            saveASS(merged, outputFile);
        }
    }

    public void shiftTimings(List<SubtitleEntry> subtitles, double offset) {
        for (SubtitleEntry entry : subtitles) {
            entry.setStart(entry.getStart() + offset);
            entry.setEnd(entry.getEnd() + offset);
        }
    }

    public void adjustDuration(List<SubtitleEntry> subtitles, double factor) {
        for (SubtitleEntry entry : subtitles) {
            double duration = entry.getEnd() - entry.getStart();
            duration *= factor;
            entry.setEnd(entry.getStart() + duration);
        }
    }
}