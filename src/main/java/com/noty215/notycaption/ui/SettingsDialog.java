package com.noty215.notycaption.ui;

import com.noty215.notycaption.Main;
import com.noty215.notycaption.App;
import com.noty215.notycaption.utils.EncryptionUtils;
import com.noty215.notycaption.utils.Translator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class SettingsDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(SettingsDialog.class.getName());
    private Map<String, Object> settings;
    private Map<String, Object> newSettings;
    private NotyCaptionWindow parent;
    private Translator translator;

    // UI Components
    private JComboBox<String> themeCombo;
    private JComboBox<String> scaleCombo;
    private JTextField tempDirField;
    private JTextField modelsDirField;
    private JCheckBox autoEnhanceCheck;
    private JSpinner wordsPerLineSpin;
    private JComboBox<String> formatCombo;
    private JCheckBox confirmCancelCheck;
    private JSpinner forceTimeoutSpin;
    private JSpinner maxRetrySpin;
    private JCheckBox minimizeTrayCheck;
    private JCheckBox showTooltipsCheck;
    private JComboBox<String> languageCombo;

    public SettingsDialog(NotyCaptionWindow parent, Map<String, Object> settings) {
        super(parent, Translator.getInstance().tr("settings_title"), true);
        this.parent = parent;
        this.settings = settings;
        this.newSettings = new HashMap<>(settings);
        this.translator = Translator.getInstance();

        setSize(800, 700);
        setMinimumSize(new Dimension(700, 600));
        setLocationRelativeTo(parent);

        initUI();
        loadCurrentValues();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab(translator.tr("general_tab"), createGeneralTab());
        tabbedPane.addTab(translator.tr("paths_tab"), createPathsTab());
        tabbedPane.addTab(translator.tr("features_tab"), createFeaturesTab());
        tabbedPane.addTab(translator.tr("advanced_tab"), createAdvancedTab());
        tabbedPane.addTab(translator.tr("workspace_tab"), createWorkspaceTab());

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton applyButton = new JButton(translator.tr("apply_restart"));
        JButton cancelButton = new JButton(translator.tr("cancel"));

        applyButton.addActionListener(e -> applySettings());
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel createGeneralTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel themePanel = createGroupPanel(translator.tr("visual_theme"));
        themeCombo = new JComboBox<>(new String[]{
                translator.tr("system_default"),
                translator.tr("light_mode"),
                translator.tr("dark_mode")
        });
        themePanel.add(themeCombo);
        panel.add(themePanel);
        panel.add(Box.createVerticalStrut(10));

        JPanel scalePanel = createGroupPanel(translator.tr("ui_scaling"));
        scaleCombo = new JComboBox<>(new String[]{"75%", "87%", "100%", "125%", "150%", "175%", "200%"});
        scalePanel.add(new JLabel(translator.tr("scale_factor")));
        scalePanel.add(scaleCombo);
        panel.add(scalePanel);
        panel.add(Box.createVerticalStrut(10));

        JPanel langPanel = createGroupPanel(translator.tr("ui_language"));
        languageCombo = new JComboBox<>(new String[]{"English", "日本語", "Русский", "Deutsch", "हिंदी", "اردو", "العربية"});
        langPanel.add(languageCombo);
        panel.add(langPanel);

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel createPathsTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel tempPanel = createGroupPanel(translator.tr("temp_dir"));
        tempDirField = new JTextField(30);
        tempPanel.add(tempDirField);
        JButton tempBrowse = new JButton(translator.tr("browse_folder"));
        tempBrowse.addActionListener(e -> browseFolder(tempDirField));
        tempPanel.add(tempBrowse);
        panel.add(tempPanel);
        panel.add(Box.createVerticalStrut(10));

        JPanel modelsPanel = createGroupPanel(translator.tr("models_dir"));
        modelsDirField = new JTextField(30);
        modelsPanel.add(modelsDirField);
        JButton modelsBrowse = new JButton(translator.tr("browse_folder"));
        modelsBrowse.addActionListener(e -> browseFolder(modelsDirField));
        modelsPanel.add(modelsBrowse);
        panel.add(modelsPanel);

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel createFeaturesTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel autoPanel = createGroupPanel(translator.tr("auto_features"));
        autoEnhanceCheck = new JCheckBox(translator.tr("auto_enhance"));
        autoPanel.add(autoEnhanceCheck);
        panel.add(autoPanel);
        panel.add(Box.createVerticalStrut(10));

        JPanel wplPanel = createGroupPanel(translator.tr("default_wpl"));
        wplPanel.add(new JLabel(translator.tr("words")));
        wordsPerLineSpin = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
        wplPanel.add(wordsPerLineSpin);
        panel.add(wplPanel);
        panel.add(Box.createVerticalStrut(10));

        JPanel formatPanel = createGroupPanel(translator.tr("default_format"));
        formatPanel.add(new JLabel(translator.tr("format")));
        formatCombo = new JComboBox<>(new String[]{
                translator.tr("srt_format"),
                translator.tr("ass_format")
        });
        formatPanel.add(formatCombo);
        panel.add(formatPanel);

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel createAdvancedTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel cancelPanel = createGroupPanel(translator.tr("cancel_options"));
        confirmCancelCheck = new JCheckBox(translator.tr("confirm_cancel"));
        cancelPanel.add(confirmCancelCheck);

        JPanel timeoutPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        timeoutPanel.add(new JLabel(translator.tr("force_cancel_timeout")));
        forceTimeoutSpin = new JSpinner(new SpinnerNumberModel(30, 10, 120, 5));
        timeoutPanel.add(forceTimeoutSpin);
        timeoutPanel.add(new JLabel(translator.tr("seconds")));
        cancelPanel.add(timeoutPanel);

        JPanel retryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        retryPanel.add(new JLabel(translator.tr("max_retry")));
        maxRetrySpin = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
        retryPanel.add(maxRetrySpin);
        cancelPanel.add(retryPanel);
        panel.add(cancelPanel);
        panel.add(Box.createVerticalStrut(10));

        JPanel uiPanel = createGroupPanel(translator.tr("ui_options"));
        minimizeTrayCheck = new JCheckBox(translator.tr("minimize_tray"));
        uiPanel.add(minimizeTrayCheck);
        showTooltipsCheck = new JCheckBox(translator.tr("show_tooltips"));
        uiPanel.add(showTooltipsCheck);
        panel.add(uiPanel);

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel createWorkspaceTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton customizeBtn = new JButton(translator.tr("workspace_customize"));
        customizeBtn.addActionListener(e -> {
            WorkspaceCustomizeDialog dialog = new WorkspaceCustomizeDialog(parent, settings);
            dialog.setVisible(true);
        });
        panel.add(customizeBtn);

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel createGroupPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }

    private void browseFolder(JTextField field) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            field.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void loadCurrentValues() {
        String theme = (String) settings.getOrDefault("theme", "Dark");
        if (theme.equals("System")) themeCombo.setSelectedIndex(0);
        else if (theme.equals("Light")) themeCombo.setSelectedIndex(1);
        else themeCombo.setSelectedIndex(2);

        scaleCombo.setSelectedItem(settings.getOrDefault("ui_scale", "100%"));
        tempDirField.setText((String) settings.getOrDefault("temp_dir", System.getProperty("java.io.tmpdir")));
        modelsDirField.setText((String) settings.getOrDefault("models_dir", App.APP_DATA_DIR));
        autoEnhanceCheck.setSelected((boolean) settings.getOrDefault("auto_enhance", false));
        wordsPerLineSpin.setValue(settings.getOrDefault("words_per_line", 5));
        formatCombo.setSelectedIndex(((String) settings.getOrDefault("output_format", "SRT")).equals("SRT") ? 0 : 1);
        confirmCancelCheck.setSelected((boolean) settings.getOrDefault("confirm_cancel", true));
        forceTimeoutSpin.setValue(settings.getOrDefault("force_cancel_timeout", 30));
        maxRetrySpin.setValue(settings.getOrDefault("max_retry_attempts", 5));
        minimizeTrayCheck.setSelected((boolean) settings.getOrDefault("minimize_to_tray", true));
        showTooltipsCheck.setSelected((boolean) settings.getOrDefault("show_tooltips", true));

        String lang = (String) settings.getOrDefault("language", "en");
        if (lang.equals("en")) languageCombo.setSelectedIndex(0);
        else if (lang.equals("ja")) languageCombo.setSelectedIndex(1);
        else if (lang.equals("ru")) languageCombo.setSelectedIndex(2);
        else if (lang.equals("de")) languageCombo.setSelectedIndex(3);
        else if (lang.equals("hi")) languageCombo.setSelectedIndex(4);
        else if (lang.equals("ur")) languageCombo.setSelectedIndex(5);
        else if (lang.equals("ar")) languageCombo.setSelectedIndex(6);
        else languageCombo.setSelectedIndex(0);
    }

    private void applySettings() {
        String theme;
        switch (themeCombo.getSelectedIndex()) {
            case 0: theme = "System"; break;
            case 1: theme = "Light"; break;
            default: theme = "Dark";
        }

        String lang;
        switch (languageCombo.getSelectedIndex()) {
            case 0: lang = "en"; break;
            case 1: lang = "ja"; break;
            case 2: lang = "ru"; break;
            case 3: lang = "de"; break;
            case 4: lang = "hi"; break;
            case 5: lang = "ur"; break;
            case 6: lang = "ar"; break;
            default: lang = "en";
        }

        newSettings.put("theme", theme);
        newSettings.put("ui_scale", scaleCombo.getSelectedItem());
        newSettings.put("temp_dir", tempDirField.getText());
        newSettings.put("models_dir", modelsDirField.getText());
        newSettings.put("auto_enhance", autoEnhanceCheck.isSelected());
        newSettings.put("words_per_line", wordsPerLineSpin.getValue());
        newSettings.put("output_format", formatCombo.getSelectedIndex() == 0 ? "SRT" : "ASS");
        newSettings.put("confirm_cancel", confirmCancelCheck.isSelected());
        newSettings.put("force_cancel_timeout", forceTimeoutSpin.getValue());
        newSettings.put("max_retry_attempts", maxRetrySpin.getValue());
        newSettings.put("minimize_to_tray", minimizeTrayCheck.isSelected());
        newSettings.put("show_tooltips", showTooltipsCheck.isSelected());
        newSettings.put("language", lang);

        EncryptionUtils.saveSettings(newSettings);

        translator.setLanguage(lang);
        if (parent != null) {
            parent.updateFromSettings(newSettings);
        }

        int result = JOptionPane.showConfirmDialog(this,
                "Settings applied. Restart the application for full effect?\nSome settings require restart.",
                "Restart Required",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            dispose();
            if (parent != null) {
                parent.dispose();
            }
            try {
                String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
                String jarPath = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
                ProcessBuilder pb = new ProcessBuilder(javaBin, "-jar", jarPath);
                pb.start();
            } catch (Exception e) {
                logger.severe("Failed to restart: " + e.getMessage());
            }
            System.exit(0);
        } else {
            dispose();
        }
    }
}