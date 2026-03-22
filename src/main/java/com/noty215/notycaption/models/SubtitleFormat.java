package com.noty215.notycaption.models;

/**
 * Subtitle format enumeration
 */
public enum SubtitleFormat {
    SRT(".srt"),
    ASS(".ass"),
    SSA(".ssa"),
    SUB(".sub"),
    TXT(".txt"),
    VTT(".vtt"),
    SCC(".scc"),
    TTML(".ttml"),
    DFXP(".dfxp"),
    SBV(".sbv"),
    LRC(".lrc"),
    SMI(".smi");

    private final String extension;

    SubtitleFormat(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }

    public static SubtitleFormat fromExtension(String extension) {
        for (SubtitleFormat format : values()) {
            if (format.extension.equalsIgnoreCase(extension)) {
                return format;
            }
        }
        return SRT;
    }
}