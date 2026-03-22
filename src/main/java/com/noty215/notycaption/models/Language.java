package com.noty215.notycaption.models;

/**
 * Language enumeration
 */
public enum Language {
    ENGLISH("en"),
    JAPANESE("ja"),
    RUSSIAN("ru"),
    GERMAN("de"),
    HINDI("hi"),
    URDU("ur"),
    ARABIC("ar"),
    SPANISH("es"),
    FRENCH("fr"),
    CHINESE("zh"),
    KOREAN("ko"),
    PORTUGUESE("pt"),
    ITALIAN("it"),
    DUTCH("nl"),
    POLISH("pl"),
    TURKISH("tr"),
    VIETNAMESE("vi"),
    THAI("th"),
    BENGALI("bn"),
    PUNJABI("pa"),
    TAMIL("ta"),
    TELUGU("te"),
    MARATHI("mr"),
    GUJARATI("gu"),
    KANNADA("kn"),
    MALAYALAM("ml"),
    ORIYA("or");

    private final String code;

    Language(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static Language fromCode(String code) {
        for (Language lang : values()) {
            if (lang.code.equals(code)) {
                return lang;
            }
        }
        return ENGLISH;
    }
}