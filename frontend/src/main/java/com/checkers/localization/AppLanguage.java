package com.checkers.localization;

import java.util.Map;

/** Languages currently available in the app language selector. */
public enum AppLanguage {
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    ARABIC("ar", "العربية"),
    THAI("th", "ไทย"),
    SENEGALESE("wo", "Senegalese"),
    CANTONESE("yue", "Cantonese");

    private final String code;
    private final String displayName;

    AppLanguage(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    public static AppLanguage fromCode(String code) {
        for (AppLanguage language : values()) {
            if (language.code.equalsIgnoreCase(code)) {
                return language;
            }
        }
        return ENGLISH;
    }

    public static Map<String, AppLanguage> byDisplayName() {
        return Map.of(
                ENGLISH.displayName, ENGLISH,
                SPANISH.displayName, SPANISH,
                ARABIC.displayName, ARABIC,
                THAI.displayName, THAI,
                SENEGALESE.displayName, SENEGALESE,
                CANTONESE.displayName, CANTONESE);
    }
}
