package com.noty215.notycaption.models;

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
    SMI(".smi"),
    RT(".rt"),
    STL(".stl"),
    PAC(".pac"),
    XML(".xml");
    
    private final String extension;
    
    SubtitleFormat(String extension) {
        this.extension = extension;
    }
    
    public String getExtension() {
        return extension;
    }
    
    public static SubtitleFormat fromExtension(String ext) {
        for (SubtitleFormat format : SubtitleFormat.values()) {
            if (format.extension.equals(ext)) {
                return format;
            }
        }
        return SRT;
    }
}