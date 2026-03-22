package com.noty215.notycaption.subtitle;

import java.io.Serializable;

/**
 * Subtitle entry data class
 */
public class SubtitleEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private int index;
    private double start;
    private double end;
    private String text;

    public SubtitleEntry() {
        this.text = "";
    }

    public SubtitleEntry(int index, double start, double end, String text) {
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

    public double getStart() {
        return start;
    }

    public void setStart(double start) {
        this.start = start;
    }

    public double getEnd() {
        return end;
    }

    public void setEnd(double end) {
        this.end = end;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public double getDuration() {
        return end - start;
    }

    @Override
    public String toString() {
        return String.format("[%d] %.2f-%.2f: %s", index, start, end, text);
    }
}