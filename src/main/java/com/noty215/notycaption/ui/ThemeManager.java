package com.noty215.notycaption.ui;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Map;

public class ThemeManager {
    private static ThemeManager instance;
    private Map<String, Color> currentTheme;
    private Color accentColor;
    private int glowIntensity;
    
    public static final Map<String, Color> DARK_THEME = Map.ofEntries(
        Map.entry("background_dark", new Color(10, 10, 10)),
        Map.entry("background_medium", new Color(26, 26, 38)),
        Map.entry("background_light", new Color(42, 42, 58)),
        Map.entry("accent_primary", new Color(74, 111, 165)),
        Map.entry("accent_secondary", new Color(107, 74, 156)),
        Map.entry("accent_tertiary", new Color(59, 142, 165)),
        Map.entry("text_primary", Color.WHITE),
        Map.entry("text_secondary", new Color(176, 176, 176)),
        Map.entry("success", new Color(46, 204, 113)),
        Map.entry("warning", new Color(243, 156, 18)),
        Map.entry("error", new Color(231, 76, 60)),
        Map.entry("info", new Color(52, 152, 219)),
        Map.entry("border", new Color(51, 51, 51))
    );
    
    public static final Map<String, Color> LIGHT_THEME = Map.ofEntries(
        Map.entry("background_dark", new Color(240, 240, 240)),
        Map.entry("background_medium", Color.WHITE),
        Map.entry("background_light", new Color(224, 224, 224)),
        Map.entry("accent_primary", new Color(0, 120, 212)),
        Map.entry("accent_secondary", new Color(134, 97, 197)),
        Map.entry("accent_tertiary", new Color(0, 183, 195)),
        Map.entry("text_primary", Color.BLACK),
        Map.entry("text_secondary", new Color(51, 51, 51)),
        Map.entry("success", new Color(16, 124, 16)),
        Map.entry("warning", new Color(255, 140, 0)),
        Map.entry("error", new Color(209, 52, 56)),
        Map.entry("info", new Color(0, 120, 212)),
        Map.entry("border", new Color(204, 204, 204))
    );
    
    private ThemeManager() {
        currentTheme = DARK_THEME;
        accentColor = currentTheme.get("accent_primary");
        glowIntensity = 50;
    }
    
    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }
    
    public void setTheme(String themeName) {
        switch (themeName) {
            case "Light":
                currentTheme = LIGHT_THEME;
                break;
            case "Dark":
            default:
                currentTheme = DARK_THEME;
                break;
        }
        accentColor = currentTheme.get("accent_primary");
        applyTheme();
    }
    
    public void setAccentColor(Color color) {
        this.accentColor = color;
        applyTheme();
    }
    
    public void setGlowIntensity(int intensity) {
        this.glowIntensity = Math.max(0, Math.min(100, intensity));
    }
    
    public Color getColor(String key) {
        return currentTheme.getOrDefault(key, Color.GRAY);
    }
    
    public Color getAccentColor() {
        return accentColor;
    }
    
    public int getGlowIntensity() {
        return glowIntensity;
    }
    
    private void applyTheme() {
        UIManager.put("Panel.background", getColor("background_dark"));
        UIManager.put("OptionPane.background", getColor("background_medium"));
        UIManager.put("OptionPane.messageForeground", getColor("text_primary"));
        
        UIManager.put("Button.background", getColor("background_light"));
        UIManager.put("Button.foreground", getColor("text_primary"));
        UIManager.put("Button.select", getColor("accent_primary"));
        
        UIManager.put("ToggleButton.background", getColor("background_light"));
        UIManager.put("ToggleButton.foreground", getColor("text_primary"));
        
        UIManager.put("RadioButton.background", getColor("background_dark"));
        UIManager.put("RadioButton.foreground", getColor("text_primary"));
        
        UIManager.put("CheckBox.background", getColor("background_dark"));
        UIManager.put("CheckBox.foreground", getColor("text_primary"));
        
        UIManager.put("ComboBox.background", getColor("background_medium"));
        UIManager.put("ComboBox.foreground", getColor("text_primary"));
        UIManager.put("ComboBox.selectionBackground", getColor("accent_primary"));
        
        UIManager.put("TextField.background", getColor("background_medium"));
        UIManager.put("TextField.foreground", getColor("text_primary"));
        UIManager.put("TextField.selectionBackground", getColor("accent_primary"));
        
        UIManager.put("TextArea.background", getColor("background_medium"));
        UIManager.put("TextArea.foreground", getColor("text_primary"));
        UIManager.put("TextArea.selectionBackground", getColor("accent_primary"));
        
        UIManager.put("EditorPane.background", getColor("background_medium"));
        UIManager.put("EditorPane.foreground", getColor("text_primary"));
        
        UIManager.put("FormattedTextField.background", getColor("background_medium"));
        UIManager.put("FormattedTextField.foreground", getColor("text_primary"));
        
        UIManager.put("PasswordField.background", getColor("background_medium"));
        UIManager.put("PasswordField.foreground", getColor("text_primary"));
        
        UIManager.put("List.background", getColor("background_medium"));
        UIManager.put("List.foreground", getColor("text_primary"));
        UIManager.put("List.selectionBackground", getColor("accent_primary"));
        
        UIManager.put("Table.background", getColor("background_medium"));
        UIManager.put("Table.foreground", getColor("text_primary"));
        UIManager.put("Table.selectionBackground", getColor("accent_primary"));
        UIManager.put("Table.gridColor", getColor("border"));
        
        UIManager.put("Tree.background", getColor("background_medium"));
        UIManager.put("Tree.foreground", getColor("text_primary"));
        UIManager.put("Tree.selectionBackground", getColor("accent_primary"));
        
        UIManager.put("ProgressBar.background", getColor("background_medium"));
        UIManager.put("ProgressBar.foreground", getColor("accent_primary"));
        UIManager.put("ProgressBar.selectionForeground", getColor("text_primary"));
        
        UIManager.put("TabbedPane.background", getColor("background_dark"));
        UIManager.put("TabbedPane.foreground", getColor("text_secondary"));
        UIManager.put("TabbedPane.selected", getColor("accent_primary"));
        
        UIManager.put("MenuBar.background", getColor("background_medium"));
        UIManager.put("MenuBar.foreground", getColor("text_primary"));
        UIManager.put("Menu.background", getColor("background_medium"));
        UIManager.put("Menu.foreground", getColor("text_primary"));
        UIManager.put("MenuItem.background", getColor("background_medium"));
        UIManager.put("MenuItem.foreground", getColor("text_primary"));
        UIManager.put("MenuItem.selectionBackground", getColor("accent_primary"));
        
        UIManager.put("ScrollBar.background", getColor("background_medium"));
        UIManager.put("ScrollBar.thumb", getColor("accent_primary"));
        UIManager.put("ScrollBar.track", getColor("background_dark"));
        
        UIManager.put("ToolTip.background", getColor("background_medium"));
        UIManager.put("ToolTip.foreground", getColor("text_primary"));
        
        UIManager.put("TitledBorder.titleColor", getColor("accent_primary"));
        
        // Update fonts
        Font defaultFont = new Font("Segoe UI", Font.PLAIN, 12);
        UIManager.put("Button.font", defaultFont);
        UIManager.put("Label.font", defaultFont);
        UIManager.put("TextField.font", defaultFont);
        UIManager.put("TextArea.font", defaultFont);
        UIManager.put("ComboBox.font", defaultFont);
        UIManager.put("List.font", defaultFont);
        UIManager.put("Table.font", defaultFont);
        UIManager.put("Tree.font", defaultFont);
        UIManager.put("MenuBar.font", defaultFont);
        UIManager.put("MenuItem.font", defaultFont);
        
        // Force UI update
        SwingUtilities.updateComponentTreeUI(JOptionPane.getRootFrame());
    }
    
    public String getStylesheet() {
        String bgDark = colorToHex(getColor("background_dark"));
        String bgMedium = colorToHex(getColor("background_medium"));
        String bgLight = colorToHex(getColor("background_light"));
        String accent = colorToHex(accentColor);
        String textPrimary = colorToHex(getColor("text_primary"));
        String textSecondary = colorToHex(getColor("text_secondary"));
        String border = colorToHex(getColor("border"));
        
        return String.format("""
            QMainWindow, QDialog { background: %s; }
            QWidget { color: %s; font-family: 'Segoe UI', 'Arial', sans-serif; }
            QLabel { color: %s; background: transparent; }
            QPushButton {
                background: qlineargradient(x1:0, y1:0, x2:1, y2:1, stop:0 %s, stop:1 %s);
                color: %s;
                border: 1px solid %s;
                border-radius: 10px;
                padding: 8px 16px;
                font-weight: 600;
                font-size: 13px;
                min-height: 30px;
            }
            QPushButton:hover {
                border: 2px solid %s;
            }
            QComboBox {
                background: %s;
                color: %s;
                border: 1px solid %s;
                border-radius: 6px;
                padding: 6px 12px;
                min-height: 30px;
            }
            QLineEdit, QTextEdit, QSpinBox {
                background: %s;
                color: %s;
                border: 1px solid %s;
                border-radius: 6px;
                padding: 6px 10px;
            }
            QProgressBar {
                background: %s;
                border: 1px solid %s;
                border-radius: 8px;
                text-align: center;
                color: %s;
                font-weight: bold;
                height: 25px;
            }
            QProgressBar::chunk {
                background: qlineargradient(x1:0, y1:0, x2:1, y2:0, stop:0 %s, stop:0.5 %s, stop:1 %s);
                border-radius: 7px;
            }
            """,
            bgDark, textPrimary, textPrimary, bgLight, bgMedium, textPrimary, border, accent,
            bgMedium, textPrimary, border, bgMedium, textPrimary, border,
            bgMedium, border, textPrimary, accent, getColor("accent_secondary"), getColor("accent_tertiary")
        );
    }
    
    private String colorToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}