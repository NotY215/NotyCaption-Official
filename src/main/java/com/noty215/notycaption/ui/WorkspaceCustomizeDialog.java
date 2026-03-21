package com.noty215.notycaption.ui;

import com.noty215.notycaption.utils.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;
import java.util.logging.Logger;

public class WorkspaceCustomizeDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(WorkspaceCustomizeDialog.class.getName());
    private Map<String, Object> settings;
    private NotyCaptionWindow parent;
    private Translator translator;
    private LayoutManager layoutManager;
    private PresetManager presetManager;
    
    // UI Components
    private JButton accentColorBtn;
    private JSlider glowSlider;
    private JSlider opacitySlider;
    private JComboBox<String> fontCombo;
    private JSpinner fontSizeSpin;
    private JCheckBox enableAnimationsCheck;
    private JComboBox<String> speedCombo;
    private JTextField layoutNameField;
    private JComboBox<String> layoutCombo;
    private JTextField presetNameField;
    private JComboBox<String> presetCombo;
    private JLabel previewLabel;
    private Color currentColor;
    
    public WorkspaceCustomizeDialog(NotyCaptionWindow parent, Map<String, Object> settings) {
        super(parent, Translator.getInstance().tr("workspace_customize"), true);
        this.parent = parent;
        this.settings = settings;
        this.translator = Translator.getInstance();
        this.layoutManager = new LayoutManager();
        this.presetManager = new PresetManager();
        this.currentColor = Color.decode((String) settings.getOrDefault("accent_color", "#4a6fa5"));
        
        setSize(700, 600);
        setMinimumSize(new Dimension(650, 550));
        setLocationRelativeTo(parent);
        
        initUI();
        loadCurrentValues();
        refreshLayouts();
        refreshPresets();
        updatePreview();
    }
    
    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Colors & Presets tab
        tabbedPane.addTab(translator.tr("presets_tab"), createColorsTab());
        
        // Animation tab
        tabbedPane.addTab(translator.tr("animation_control"), createAnimationTab());
        
        // Appearance tab
        tabbedPane.addTab(translator.tr("workspace_tab"), createAppearanceTab());
        
        // Layouts tab
        tabbedPane.addTab(translator.tr("save_layout"), createLayoutsTab());
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // Preview panel
        JPanel previewPanel = createPreviewPanel();
        mainPanel.add(previewPanel, BorderLayout.NORTH);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton resetBtn = new JButton(translator.tr("reset_defaults"));
        JButton applyBtn = new JButton(translator.tr("apply_restart"));
        JButton cancelBtn = new JButton(translator.tr("cancel"));
        
        resetBtn.addActionListener(e -> resetDefaults());
        applyBtn.addActionListener(e -> applySettings());
        cancelBtn.addActionListener(e -> dispose());
        
        buttonPanel.add(resetBtn);
        buttonPanel.add(applyBtn);
        buttonPanel.add(cancelBtn);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(translator.tr("preview")));
        
        previewLabel = new JLabel("NotyCaption Pro - Preview", SwingConstants.CENTER);
        previewLabel.setPreferredSize(new Dimension(400, 100));
        previewLabel.setOpaque(true);
        panel.add(previewLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createColorsTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Accent color
        JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        colorPanel.add(new JLabel(translator.tr("accent_color")));
        accentColorBtn = new JButton();
        accentColorBtn.setPreferredSize(new Dimension(50, 30));
        accentColorBtn.addActionListener(e -> chooseAccentColor());
        colorPanel.add(accentColorBtn);
        panel.add(colorPanel);
        panel.add(Box.createVerticalStrut(10));
        
        // Presets
        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        presetPanel.add(new JLabel(translator.tr("presets")));
        presetCombo = new JComboBox<>();
        presetPanel.add(presetCombo);
        JButton applyPresetBtn = new JButton(translator.tr("apply_preset"));
        applyPresetBtn.addActionListener(e -> applySelectedPreset());
        presetPanel.add(applyPresetBtn);
        panel.add(presetPanel);
        panel.add(Box.createVerticalStrut(10));
        
        // Save custom preset
        JPanel savePresetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        presetNameField = new JTextField(15);
        presetNameField.setToolTipText(translator.tr("preset_name"));
        savePresetPanel.add(presetNameField);
        JButton savePresetBtn = new JButton(translator.tr("save_preset"));
        savePresetBtn.addActionListener(e -> saveCustomPreset());
        savePresetPanel.add(savePresetBtn);
        panel.add(savePresetPanel);
        
        panel.add(Box.createVerticalGlue());
        return panel;
    }
    
    private JPanel createAnimationTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        enableAnimationsCheck = new JCheckBox(translator.tr("enable_animations"));
        panel.add(enableAnimationsCheck);
        panel.add(Box.createVerticalStrut(10));
        
        JPanel speedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        speedPanel.add(new JLabel(translator.tr("animation_speed")));
        speedCombo = new JComboBox<>(new String[]{
            translator.tr("animation_speed_slow"),
            translator.tr("animation_speed_normal"),
            translator.tr("animation_speed_fast")
        });
        speedPanel.add(speedCombo);
        panel.add(speedPanel);
        panel.add(Box.createVerticalStrut(10));
        
        JPanel glowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        glowPanel.add(new JLabel(translator.tr("glow_intensity")));
        glowSlider = new JSlider(0, 100, 50);
        glowSlider.addChangeListener(e -> updatePreview());
        glowPanel.add(glowSlider);
        panel.add(glowPanel);
        
        panel.add(Box.createVerticalGlue());
        return panel;
    }
    
    private JPanel createAppearanceTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JPanel opacityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        opacityPanel.add(new JLabel(translator.tr("card_opacity")));
        opacitySlider = new JSlider(30, 100, 80);
        opacitySlider.addChangeListener(e -> updatePreview());
        opacityPanel.add(opacitySlider);
        panel.add(opacityPanel);
        panel.add(Box.createVerticalStrut(10));
        
        JPanel fontPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fontPanel.add(new JLabel(translator.tr("font_family")));
        fontCombo = new JComboBox<>(new String[]{
            "Segoe UI", "Arial", "Helvetica", "Verdana", "Tahoma", "Consolas", "Courier New"
        });
        fontCombo.addActionListener(e -> updatePreview());
        fontPanel.add(fontCombo);
        panel.add(fontPanel);
        panel.add(Box.createVerticalStrut(10));
        
        JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sizePanel.add(new JLabel(translator.tr("font_size")));
        fontSizeSpin = new JSpinner(new SpinnerNumberModel(14, 8, 24, 1));
        fontSizeSpin.addChangeListener(e -> updatePreview());
        sizePanel.add(fontSizeSpin);
        panel.add(sizePanel);
        
        panel.add(Box.createVerticalGlue());
        return panel;
    }
    
    private JPanel createLayoutsTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Save layout
        JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        savePanel.add(new JLabel(translator.tr("layout_name")));
        layoutNameField = new JTextField(15);
        savePanel.add(layoutNameField);
        JButton saveLayoutBtn = new JButton(translator.tr("save_layout"));
        saveLayoutBtn.addActionListener(e -> saveLayout());
        savePanel.add(saveLayoutBtn);
        panel.add(savePanel);
        panel.add(Box.createVerticalStrut(10));
        
        // Load layout
        JPanel loadPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        loadPanel.add(new JLabel(translator.tr("select_layout")));
        layoutCombo = new JComboBox<>();
        loadPanel.add(layoutCombo);
        JButton loadLayoutBtn = new JButton(translator.tr("load_layout"));
        loadLayoutBtn.addActionListener(e -> loadSelectedLayout());
        loadPanel.add(loadLayoutBtn);
        JButton deleteLayoutBtn = new JButton(translator.tr("delete_preset"));
        deleteLayoutBtn.addActionListener(e -> deleteSelectedLayout());
        loadPanel.add(deleteLayoutBtn);
        panel.add(loadPanel);
        
        panel.add(Box.createVerticalGlue());
        return panel;
    }
    
    private void chooseAccentColor() {
        Color color = JColorChooser.showDialog(this, translator.tr("accent_color"), currentColor);
        if (color != null) {
            currentColor = color;
            updateColorButton();
            updatePreview();
        }
    }
    
    private void updateColorButton() {
        accentColorBtn.setBackground(currentColor);
        accentColorBtn.setOpaque(true);
        accentColorBtn.setBorder(BorderFactory.createLineBorder(Color.WHITE));
    }
    
    private void applySelectedPreset() {
        String presetName = (String) presetCombo.getSelectedItem();
        Map<String, Object> preset = presetManager.getPreset(presetName);
        if (preset != null && preset.containsKey("colors")) {
            Map<String, String> colors = (Map<String, String>) preset.get("colors");
            String colorHex = colors.get("accent_primary");
            if (colorHex != null) {
                currentColor = Color.decode(colorHex);
                updateColorButton();
                updatePreview();
            }
        }
    }
    
    private void saveCustomPreset() {
        String name = presetNameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a preset name.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Map<String, Object> preset = new java.util.HashMap<>();
        Map<String, String> colors = new java.util.HashMap<>();
        colors.put("accent_primary", String.format("#%06x", currentColor.getRGB() & 0xFFFFFF));
        preset.put("name", name);
        preset.put("colors", colors);
        
        if (presetManager.savePreset(preset)) {
            JOptionPane.showMessageDialog(this, "Preset saved: " + name, "Success", JOptionPane.INFORMATION_MESSAGE);
            presetNameField.setText("");
            refreshPresets();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to save preset.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void saveLayout() {
        String name = layoutNameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a layout name.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Map<String, Object> layoutSettings = getCurrentSettings();
        if (layoutManager.saveLayout(name, layoutSettings)) {
            JOptionPane.showMessageDialog(this, "Layout saved: " + name, "Success", JOptionPane.INFORMATION_MESSAGE);
            layoutNameField.setText("");
            refreshLayouts();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to save layout.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadSelectedLayout() {
        String name = (String) layoutCombo.getSelectedItem();
        if (name != null) {
            Map<String, Object> layoutSettings = layoutManager.loadLayout(name);
            if (layoutSettings != null) {
                applyLayoutSettings(layoutSettings);
                JOptionPane.showMessageDialog(this, "Layout loaded: " + name, "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
    
    private void deleteSelectedLayout() {
        String name = (String) layoutCombo.getSelectedItem();
        if (name != null) {
            int result = JOptionPane.showConfirmDialog(this,
                "Delete layout '" + name + "'?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                if (layoutManager.deleteLayout(name)) {
                    refreshLayouts();
                    JOptionPane.showMessageDialog(this, "Layout deleted: " + name, "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    }
    
    private void applyLayoutSettings(Map<String, Object> layoutSettings) {
        if (layoutSettings.containsKey("accent_color")) {
            currentColor = Color.decode((String) layoutSettings.get("accent_color"));
            updateColorButton();
        }
        if (layoutSettings.containsKey("glow_intensity")) {
            glowSlider.setValue((int) layoutSettings.get("glow_intensity"));
        }
        if (layoutSettings.containsKey("card_opacity")) {
            opacitySlider.setValue((int) layoutSettings.get("card_opacity"));
        }
        if (layoutSettings.containsKey("font_family")) {
            fontCombo.setSelectedItem(layoutSettings.get("font_family"));
        }
        if (layoutSettings.containsKey("font_size")) {
            fontSizeSpin.setValue(layoutSettings.get("font_size"));
        }
        if (layoutSettings.containsKey("enable_animations")) {
            enableAnimationsCheck.setSelected((boolean) layoutSettings.get("enable_animations"));
        }
        if (layoutSettings.containsKey("animation_speed")) {
            String speed = (String) layoutSettings.get("animation_speed");
            if (speed.equals("slow")) speedCombo.setSelectedIndex(0);
            else if (speed.equals("fast")) speedCombo.setSelectedIndex(2);
            else speedCombo.setSelectedIndex(1);
        }
        updatePreview();
    }
    
    private void refreshLayouts() {
        layoutCombo.removeAllItems();
        for (String name : layoutManager.getLayouts()) {
            layoutCombo.addItem(name);
        }
    }
    
    private void refreshPresets() {
        presetCombo.removeAllItems();
        for (Map<String, Object> preset : presetManager.getPresets()) {
            presetCombo.addItem((String) preset.get("name"));
        }
    }
    
    private void updatePreview() {
        int opacity = opacitySlider.getValue();
        int glow = glowSlider.getValue();
        String fontFamily = (String) fontCombo.getSelectedItem();
        int fontSize = (int) fontSizeSpin.getValue();
        String colorHex = String.format("#%06x", currentColor.getRGB() & 0xFFFFFF);
        
        String style = String.format(
            "background: linear-gradient(135deg, #1a1a2e, #2a2a3a); " +
            "border: 2px solid %s; " +
            "border-radius: 15px; " +
            "font-family: '%s'; " +
            "font-size: %dpx; " +
            "font-weight: bold; " +
            "color: white; " +
            "padding: 20px;",
            colorHex, fontFamily, fontSize
        );
        
        previewLabel.setText("<html><div style='text-align: center;'>" +
            "<span style='font-size: 24px;'>NotyCaption Pro</span><br>" +
            "<span style='font-size: 12px; color: #aaa;'>Preview</span></div></html>");
        previewLabel.setOpaque(false);
        previewLabel.setBorder(BorderFactory.createLineBorder(currentColor, 2, true));
    }
    
    private void loadCurrentValues() {
        glowSlider.setValue((int) settings.getOrDefault("glow_intensity", 50));
        opacitySlider.setValue((int) settings.getOrDefault("card_opacity", 80));
        fontCombo.setSelectedItem(settings.getOrDefault("font_family", "Segoe UI"));
        fontSizeSpin.setValue(settings.getOrDefault("font_size", 14));
        enableAnimationsCheck.setSelected((boolean) settings.getOrDefault("enable_animations", true));
        
        String speed = (String) settings.getOrDefault("animation_speed", "normal");
        if (speed.equals("slow")) speedCombo.setSelectedIndex(0);
        else if (speed.equals("fast")) speedCombo.setSelectedIndex(2);
        else speedCombo.setSelectedIndex(1);
        
        updateColorButton();
    }
    
    private void resetDefaults() {
        currentColor = Color.decode("#4a6fa5");
        glowSlider.setValue(50);
        opacitySlider.setValue(80);
        fontCombo.setSelectedItem("Segoe UI");
        fontSizeSpin.setValue(14);
        enableAnimationsCheck.setSelected(true);
        speedCombo.setSelectedIndex(1);
        updateColorButton();
        updatePreview();
    }
    
    private Map<String, Object> getCurrentSettings() {
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("accent_color", String.format("#%06x", currentColor.getRGB() & 0xFFFFFF));
        result.put("glow_intensity", glowSlider.getValue());
        result.put("card_opacity", opacitySlider.getValue());
        result.put("font_family", fontCombo.getSelectedItem());
        result.put("font_size", fontSizeSpin.getValue());
        result.put("enable_animations", enableAnimationsCheck.isSelected());
        
        String speed;
        switch (speedCombo.getSelectedIndex()) {
            case 0: speed = "slow"; break;
            case 2: speed = "fast"; break;
            default: speed = "normal";
        }
        result.put("animation_speed", speed);
        
        return result;
    }
    
    private void applySettings() {
        Map<String, Object> newSettings = getCurrentSettings();
        settings.putAll(newSettings);
        EncryptionUtils.saveSettings(settings);
        
        if (parent != null) {
            parent.updateFromSettings(settings);
        }
        
        dispose();
    }
}