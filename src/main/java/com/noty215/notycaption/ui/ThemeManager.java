package com.noty215.notycaption.ui;

import com.noty215.notycaption.models.Theme;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages application themes and stylesheets
 */
public class ThemeManager {

    private static final Map<String, String> DARK_THEME = new HashMap<>();
    private static final Map<String, String> LIGHT_THEME = new HashMap<>();

    static {
        // Dark theme
        DARK_THEME.put("background_dark", "#0a0a0a");
        DARK_THEME.put("background_medium", "#1a1a1a");
        DARK_THEME.put("background_light", "#2a2a2a");
        DARK_THEME.put("accent_primary", "#4a6fa5");
        DARK_THEME.put("accent_secondary", "#6b4a9c");
        DARK_THEME.put("accent_tertiary", "#3b8ea5");
        DARK_THEME.put("glow_blue", "#4a90e2");
        DARK_THEME.put("glow_purple", "#9b59b6");
        DARK_THEME.put("glow_cyan", "#00d4ff");
        DARK_THEME.put("text_primary", "#ffffff");
        DARK_THEME.put("text_secondary", "#b0b0b0");
        DARK_THEME.put("text_accent", "#4a90e2");
        DARK_THEME.put("success", "#2ecc71");
        DARK_THEME.put("warning", "#f39c12");
        DARK_THEME.put("error", "#e74c3c");
        DARK_THEME.put("info", "#3498db");
        DARK_THEME.put("border", "#333333");
        DARK_THEME.put("hover", "#3a3a3a");
        DARK_THEME.put("overlay", "rgba(10, 10, 10, 0.95)");

        // Light theme
        LIGHT_THEME.put("background_dark", "#f0f0f0");
        LIGHT_THEME.put("background_medium", "#ffffff");
        LIGHT_THEME.put("background_light", "#e0e0e0");
        LIGHT_THEME.put("accent_primary", "#0078d4");
        LIGHT_THEME.put("accent_secondary", "#8661c5");
        LIGHT_THEME.put("accent_tertiary", "#00b7c3");
        LIGHT_THEME.put("glow_blue", "#0078d4");
        LIGHT_THEME.put("glow_purple", "#8661c5");
        LIGHT_THEME.put("glow_cyan", "#00b7c3");
        LIGHT_THEME.put("text_primary", "#000000");
        LIGHT_THEME.put("text_secondary", "#333333");
        LIGHT_THEME.put("text_accent", "#0078d4");
        LIGHT_THEME.put("success", "#107c10");
        LIGHT_THEME.put("warning", "#ff8c00");
        LIGHT_THEME.put("error", "#d13438");
        LIGHT_THEME.put("info", "#0078d4");
        LIGHT_THEME.put("border", "#cccccc");
        LIGHT_THEME.put("hover", "#e5e5e5");
        LIGHT_THEME.put("overlay", "rgba(255, 255, 255, 0.95)");
    }

    public static String getStylesheet(Theme theme, String accentColor, int glowIntensity,
                                       int cardOpacity, String fontFamily, int fontSize) {
        Map<String, String> colors = (theme == Theme.LIGHT) ? LIGHT_THEME : DARK_THEME;

        if (accentColor != null && !accentColor.isEmpty()) {
            colors.put("accent_primary", accentColor);
        }

        // Build CSS string
        StringBuilder css = new StringBuilder();

        css.append(".root { -fx-background-color: ").append(colors.get("background_dark")).append("; }\n");
        css.append(".label { -fx-text-fill: ").append(colors.get("text_primary")).append("; ")
                .append("-fx-font-family: '").append(fontFamily).append("'; ")
                .append("-fx-font-size: ").append(fontSize).append("px; }\n");
        css.append(".button { -fx-background-color: linear-gradient(to bottom right, ")
                .append(colors.get("background_light")).append(", ").append(colors.get("background_medium")).append("); ")
                .append("-fx-text-fill: ").append(colors.get("text_primary")).append("; ")
                .append("-fx-border-color: ").append(colors.get("border")).append("; ")
                .append("-fx-border-radius: 10; -fx-background-radius: 10; ")
                .append("-fx-padding: 8 16; -fx-font-weight: 600; -fx-font-size: 13px; }\n");
        css.append(".button:hover { -fx-background-color: linear-gradient(to bottom right, ")
                .append(colors.get("background_light")).append(", ").append(colors.get("background_dark")).append("); ")
                .append("-fx-border-width: 2; -fx-border-color: ").append(colors.get("accent_primary")).append("; }\n");
        css.append(".button:pressed { -fx-background-color: ").append(colors.get("background_dark")).append("; ")
                .append("-fx-border-width: 2; -fx-border-color: ").append(colors.get("accent_secondary")).append("; }\n");
        css.append(".button:primary { -fx-background-color: linear-gradient(to bottom right, ")
                .append(colors.get("accent_primary")).append(", ").append(colors.get("accent_secondary")).append("); ")
                .append("-fx-text-fill: white; }\n");
        css.append(".button:success { -fx-background-color: linear-gradient(to bottom right, ")
                .append(colors.get("success")).append(", #27ae60); -fx-text-fill: white; }\n");
        css.append(".button:warning { -fx-background-color: linear-gradient(to bottom right, ")
                .append(colors.get("warning")).append(", #e67e22); -fx-text-fill: white; }\n");
        css.append(".button:danger { -fx-background-color: linear-gradient(to bottom right, ")
                .append(colors.get("error")).append(", #c0392b); -fx-text-fill: white; }\n");
        css.append(".combo-box, .text-field, .text-area, .spinner { -fx-background-color: ")
                .append(colors.get("background_medium")).append("; -fx-text-fill: ").append(colors.get("text_primary"))
                .append("; -fx-border-color: ").append(colors.get("border")).append("; ")
                .append("-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 10; }\n");
        css.append(".combo-box:focused, .text-field:focused, .text-area:focused, .spinner:focused { ")
                .append("-fx-border-width: 2; -fx-border-color: ").append(colors.get("accent_primary")).append("; }\n");
        css.append(".progress-bar { -fx-background-color: ").append(colors.get("background_medium"))
                .append("; -fx-border-color: ").append(colors.get("border")).append("; ")
                .append("-fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: ").append(colors.get("text_primary"))
                .append("; -fx-font-weight: bold; -fx-padding: 4; }\n");
        css.append(".progress-bar .bar { -fx-background-color: linear-gradient(to right, ")
                .append(colors.get("accent_primary")).append(", ").append(colors.get("accent_secondary")).append(", ")
                .append(colors.get("accent_tertiary")).append("); -fx-background-radius: 7; }\n");
        css.append(".tab-pane .tab-header-area .tab-header-background { -fx-background-color: ")
                .append(colors.get("background_dark")).append("; }\n");
        css.append(".tab { -fx-background-color: ").append(colors.get("background_medium"))
                .append("; -fx-text-fill: ").append(colors.get("text_secondary")).append("; ")
                .append("-fx-padding: 10 20; }\n");
        css.append(".tab:selected { -fx-background-color: ").append(colors.get("background_light"))
                .append("; -fx-text-fill: ").append(colors.get("accent_primary")).append("; "
                        + "-fx-border-width: 0 0 2 0; -fx-border-color: ").append(colors.get("accent_primary")).append("; }\n");
        css.append(".scroll-bar:vertical { -fx-background-color: ").append(colors.get("background_medium"))
                .append("; -fx-background-radius: 6; }\n");
        css.append(".scroll-bar:vertical .thumb { -fx-background-color: linear-gradient(to right, ")
                .append(colors.get("accent_primary")).append(", ").append(colors.get("accent_secondary")).append("); "
                        + "-fx-background-radius: 6; }\n");
        css.append(".menu-bar { -fx-background-color: ").append(colors.get("background_dark")).append("; }\n");
        css.append(".menu-item { -fx-text-fill: ").append(colors.get("text_primary")).append("; }\n");
        css.append(".menu-item:focused { -fx-background-color: ").append(colors.get("accent_primary"))
                .append("; -fx-text-fill: white; }\n");
        css.append(".tooltip { -fx-background-color: ").append(colors.get("background_medium"))
                .append("; -fx-text-fill: ").append(colors.get("text_primary")).append("; "
                        + "-fx-border-color: ").append(colors.get("accent_primary")).append("; "
                        + "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; }\n");

        return css.toString();
    }

    public static Map<String, String> getDarkTheme() {
        return new HashMap<>(DARK_THEME);
    }

    public static Map<String, String> getLightTheme() {
        return new HashMap<>(LIGHT_THEME);
    }
}