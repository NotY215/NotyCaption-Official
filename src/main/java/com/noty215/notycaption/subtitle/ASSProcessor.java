package com.noty215.notycaption.subtitle;

import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ASSProcessor {
    private static final Logger logger = Logger.getLogger(ASSProcessor.class.getName());
    
    public static class ASSStyle {
        public String name = "Default";
        public String fontName = "Arial";
        public int fontSize = 20;
        public int primaryColor = 0xFFFFFF;
        public int secondaryColor = 0xFFFFFF;
        public int outlineColor = 0x000000;
        public int backColor = 0x000000;
        public boolean bold = false;
        public boolean italic = false;
        public boolean underline = false;
        public boolean strikeOut = false;
        public double scaleX = 100;
        public double scaleY = 100;
        public double spacing = 0;
        public double angle = 0;
        public int borderStyle = 1;
        public int outline = 2;
        public int shadow = 2;
        public int alignment = 2;
        public int marginL = 10;
        public int marginR = 10;
        public int marginV = 10;
        public int encoding = 1;
        
        public String toFormat() {
            return String.format(
                "Style: %s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d",
                name, fontName, fontSize, primaryColor, secondaryColor, outlineColor,
                backColor, bold ? 1 : 0, italic ? 1 : 0, underline ? 1 : 0, 
                strikeOut ? 1 : 0, scaleX, scaleY, spacing, angle, borderStyle, 
                outline, shadow, alignment, marginL, marginR, marginV, encoding
            );
        }
    }
    
    public static class ASSEvent {
        public String layer = "0";
        public String start;
        public String end;
        public String style = "Default";
        public String name = "";
        public String marginL = "0";
        public String marginR = "0";
        public String marginV = "0";
        public String effect = "";
        public String text;
        
        public String toFormat() {
            return String.format(
                "Dialogue: %s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                layer, start, end, style, name, marginL, marginR, marginV, effect, text
            );
        }
    }
    
    private Map<String, ASSStyle> styles = new HashMap<>();
    private List<ASSEvent> events = new ArrayList<>();
    private String title = "NotyCaption Subtitles";
    private String originalScript = "NotyCaption Pro";
    private String scriptType = "v4.00+";
    private int playResX = 1280;
    private int playResY = 720;
    
    public ASSProcessor() {
        // Add default style
        ASSStyle defaultStyle = new ASSStyle();
        defaultStyle.name = "Default";
        styles.put("Default", defaultStyle);
    }
    
    public List<SubtitleEntry> parse(String content) {
        List<SubtitleEntry> subtitles = new ArrayList<>();
        String[] lines = content.split("\n");
        
        for (String line : lines) {
            if (line.startsWith("Dialogue:")) {
                try {
                    String[] parts = line.substring(9).split(",");
                    if (parts.length >= 10) {
                        SubtitleEntry entry = new SubtitleEntry();
                        entry.setIndex(subtitles.size() + 1);
                        entry.setStart(parseTime(parts[1]));
                        entry.setEnd(parseTime(parts[2]));
                        entry.setText(parts[9]);
                        subtitles.add(entry);
                    }
                } catch (Exception e) {
                    logger.warning("Error parsing ASS line: " + e.getMessage());
                }
            } else if (line.startsWith("Style:")) {
                parseStyle(line);
            }
        }
        
        return subtitles;
    }
    
    private void parseStyle(String line) {
        String[] parts = line.substring(6).split(",");
        if (parts.length >= 18) {
            ASSStyle style = new ASSStyle();
            style.name = parts[0];
            style.fontName = parts[1];
            style.fontSize = Integer.parseInt(parts[2]);
            style.primaryColor = Integer.parseInt(parts[3], 16);
            style.secondaryColor = Integer.parseInt(parts[4], 16);
            style.outlineColor = Integer.parseInt(parts[5], 16);
            style.backColor = Integer.parseInt(parts[6], 16);
            style.bold = parts[7].equals("1");
            style.italic = parts[8].equals("1");
            style.underline = parts[9].equals("1");
            style.strikeOut = parts[10].equals("1");
            style.scaleX = Double.parseDouble(parts[11]);
            style.scaleY = Double.parseDouble(parts[12]);
            style.spacing = Double.parseDouble(parts[13]);
            style.angle = Double.parseDouble(parts[14]);
            style.borderStyle = Integer.parseInt(parts[15]);
            style.outline = Integer.parseInt(parts[16]);
            style.shadow = Integer.parseInt(parts[17]);
            style.alignment = Integer.parseInt(parts[18]);
            if (parts.length > 19) style.marginL = Integer.parseInt(parts[19]);
            if (parts.length > 20) style.marginR = Integer.parseInt(parts[20]);
            if (parts.length > 21) style.marginV = Integer.parseInt(parts[21]);
            if (parts.length > 22) style.encoding = Integer.parseInt(parts[22]);
            
            styles.put(style.name, style);
        }
    }
    
    private Duration parseTime(String timeStr) {
        String[] parts = timeStr.split(":");
        long hours = Long.parseLong(parts[0]);
        long minutes = Long.parseLong(parts[1]);
        String[] secParts = parts[2].split("\\.");
        long seconds = Long.parseLong(secParts[0]);
        long millis = secParts.length > 1 ? Long.parseLong(secParts[1]) : 0;
        
        return Duration.ofHours(hours)
            .plusMinutes(minutes)
            .plusSeconds(seconds)
            .plusMillis(millis);
    }
    
    private String formatTime(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        long millis = duration.toMillis() % 1000;
        
        return String.format("%d:%02d:%02d.%02d", hours, minutes, seconds, millis / 10);
    }
    
    public String format(List<SubtitleEntry> subtitles) {
        StringBuilder sb = new StringBuilder();
        
        // Header
        sb.append("[Script Info]\n");
        sb.append("Title: ").append(title).append("\n");
        sb.append("Original Script: ").append(originalScript).append("\n");
        sb.append("ScriptType: ").append(scriptType).append("\n");
        sb.append("PlayResX: ").append(playResX).append("\n");
        sb.append("PlayResY: ").append(playResY).append("\n\n");
        
        // Styles
        sb.append("[V4+ Styles]\n");
        sb.append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, ")
          .append("OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ")
          .append("ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, ")
          .append("Alignment, MarginL, MarginR, MarginV, Encoding\n");
        
        for (ASSStyle style : styles.values()) {
            sb.append(style.toFormat()).append("\n");
        }
        sb.append("\n");
        
        // Events
        sb.append("[Events]\n");
        sb.append("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n");
        
        int index = 1;
        for (SubtitleEntry entry : subtitles) {
            ASSEvent event = new ASSEvent();
            event.start = formatTime(entry.getStart());
            event.end = formatTime(entry.getEnd());
            event.text = entry.getText();
            sb.append(event.toFormat()).append("\n");
        }
        
        return sb.toString();
    }
    
    public void saveToFile(List<SubtitleEntry> subtitles, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), "UTF-8"))) {
            writer.write(format(subtitles));
        }
        logger.info("ASS saved to: " + file.getAbsolutePath());
    }
    
    public List<SubtitleEntry> loadFromFile(File file) throws IOException {
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
    
    public void addStyle(ASSStyle style) {
        styles.put(style.name, style);
    }
    
    public ASSStyle getStyle(String name) {
        return styles.get(name);
    }
    
    public void setPlayResX(int playResX) {
        this.playResX = playResX;
    }
    
    public void setPlayResY(int playResY) {
        this.playResY = playResY;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
}