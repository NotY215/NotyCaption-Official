package com.noty215.notycaption.subtitle;

import com.noty215.notycaption.models.SubtitleFormat;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class SubtitleManager {
    private static final Logger logger = Logger.getLogger(SubtitleManager.class.getName());
    private List<SubtitleEntry> subtitles;
    private SubtitleFormat currentFormat;
    
    public SubtitleManager() {
        this.subtitles = new ArrayList<>();
        this.currentFormat = SubtitleFormat.SRT;
    }
    
    public List<SubtitleEntry> getSubtitles() {
        return new ArrayList<>(subtitles);
    }
    
    public void setSubtitles(List<SubtitleEntry> subtitles) {
        this.subtitles = new ArrayList<>(subtitles);
    }
    
    public void addSubtitle(SubtitleEntry entry) {
        subtitles.add(entry);
        reindex();
    }
    
    public void removeSubtitle(int index) {
        if (index >= 0 && index < subtitles.size()) {
            subtitles.remove(index);
            reindex();
        }
    }
    
    public void updateSubtitle(int index, SubtitleEntry entry) {
        if (index >= 0 && index < subtitles.size()) {
            subtitles.set(index, entry);
            reindex();
        }
    }
    
    public void clear() {
        subtitles.clear();
    }
    
    private void reindex() {
        for (int i = 0; i < subtitles.size(); i++) {
            subtitles.get(i).setIndex(i + 1);
        }
    }
    
    public void loadFromFile(File file) throws IOException {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".srt")) {
            subtitles = SRTProcessor.loadFromFile(file);
            currentFormat = SubtitleFormat.SRT;
        } else if (fileName.endsWith(".ass") || fileName.endsWith(".ssa")) {
            ASSProcessor assProcessor = new ASSProcessor();
            subtitles = assProcessor.loadFromFile(file);
            currentFormat = SubtitleFormat.ASS;
        } else {
            throw new IOException("Unsupported subtitle format: " + fileName);
        }
        
        logger.info("Loaded " + subtitles.size() + " subtitles from: " + file.getAbsolutePath());
    }
    
    public void saveToFile(File file) throws IOException {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".srt")) {
            SRTProcessor.saveToFile(subtitles, file);
            currentFormat = SubtitleFormat.SRT;
        } else if (fileName.endsWith(".ass") || fileName.endsWith(".ssa")) {
            ASSProcessor assProcessor = new ASSProcessor();
            assProcessor.saveToFile(subtitles, file);
            currentFormat = SubtitleFormat.ASS;
        } else {
            throw new IOException("Unsupported subtitle format: " + fileName);
        }
        
        logger.info("Saved " + subtitles.size() + " subtitles to: " + file.getAbsolutePath());
    }
    
    public void exportToFormat(File file, SubtitleFormat format) throws IOException {
        switch (format) {
            case SRT:
                SRTProcessor.saveToFile(subtitles, file);
                break;
            case ASS:
                ASSProcessor assProcessor = new ASSProcessor();
                assProcessor.saveToFile(subtitles, file);
                break;
            default:
                throw new IOException("Export to " + format + " not implemented");
        }
    }
    
    public List<SubtitleEntry> search(String query) {
        List<SubtitleEntry> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        
        for (SubtitleEntry entry : subtitles) {
            if (entry.getText().toLowerCase().contains(lowerQuery)) {
                results.add(entry);
            }
        }
        
        return results;
    }
    
    public void shiftAll(Duration offset) {
        for (SubtitleEntry entry : subtitles) {
            entry.shift(offset);
        }
    }
    
    public void scaleAll(double factor) {
        for (SubtitleEntry entry : subtitles) {
            entry.scale(factor);
        }
    }
    
    public void merge(List<SubtitleEntry> other, Duration offset) {
        List<SubtitleEntry> adjusted = new ArrayList<>();
        for (SubtitleEntry entry : other) {
            SubtitleEntry copy = new SubtitleEntry();
            copy.setStart(entry.getStart().plus(offset));
            copy.setEnd(entry.getEnd().plus(offset));
            copy.setText(entry.getText());
            adjusted.add(copy);
        }
        
        List<SubtitleEntry> merged = SRTProcessor.mergeSubtitles(subtitles, adjusted);
        subtitles = merged;
    }
    
    public SubtitleEntry getSubtitleAtTime(Duration time) {
        for (SubtitleEntry entry : subtitles) {
            if (entry.containsTime(time)) {
                return entry;
            }
        }
        return null;
    }
    
    public List<SubtitleEntry> getSubtitlesInRange(Duration start, Duration end) {
        List<SubtitleEntry> results = new ArrayList<>();
        for (SubtitleEntry entry : subtitles) {
            if (!entry.getEnd().isBefore(start) && !entry.getStart().isAfter(end)) {
                results.add(entry);
            }
        }
        return results;
    }
    
    public String getPlainText() {
        StringBuilder sb = new StringBuilder();
        for (SubtitleEntry entry : subtitles) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append(entry.getText());
        }
        return sb.toString();
    }
    
    public int size() {
        return subtitles.size();
    }
    
    public boolean isEmpty() {
        return subtitles.isEmpty();
    }
    
    public SubtitleFormat getCurrentFormat() {
        return currentFormat;
    }
}