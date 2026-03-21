package com.noty215.notycaption.subtitle;

import java.time.Duration;

public class SubtitleEntry {
    private int index;
    private Duration start;
    private Duration end;
    private String text;
    private String translation;
    private String notes;
    private int layer;
    private String style;
    
    public SubtitleEntry() {
        this.start = Duration.ZERO;
        this.end = Duration.ZERO;
        this.text = "";
    }
    
    public SubtitleEntry(int index, Duration start, Duration end, String text) {
        this.index = index;
        this.start = start;
        this.end = end;
        this.text = text;
    }
    
    public int getIndex() {
        return index;
    }
    
    public void setIndex(int index) {
        this.index = index;
    }
    
    public Duration getStart() {
        return start;
    }
    
    public void setStart(Duration start) {
        this.start = start;
    }
    
    public Duration getEnd() {
        return end;
    }
    
    public void setEnd(Duration end) {
        this.end = end;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getTranslation() {
        return translation;
    }
    
    public void setTranslation(String translation) {
        this.translation = translation;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public int getLayer() {
        return layer;
    }
    
    public void setLayer(int layer) {
        this.layer = layer;
    }
    
    public String getStyle() {
        return style;
    }
    
    public void setStyle(String style) {
        this.style = style;
    }
    
    public Duration getDuration() {
        return end.minus(start);
    }
    
    public boolean containsTime(Duration time) {
        return !start.minus(time).isPositive() && !end.minus(time).isNegative();
    }
    
    public void shift(Duration offset) {
        start = start.plus(offset);
        end = end.plus(offset);
    }
    
    public void scale(double factor) {
        long startNanos = (long) (start.toNanos() * factor);
        long endNanos = (long) (end.toNanos() * factor);
        start = Duration.ofNanos(startNanos);
        end = Duration.ofNanos(endNanos);
    }
    
    @Override
    public String toString() {
        return String.format("%d: %s --> %s\n%s", 
            index, 
            formatDuration(start),
            formatDuration(end),
            text);
    }
    
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        long millis = duration.toMillis() % 1000;
        
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis);
    }
}