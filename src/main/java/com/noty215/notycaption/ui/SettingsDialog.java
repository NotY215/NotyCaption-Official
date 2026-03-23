package com.noty215.notycaption.ui;

import com.noty215.notycaption.utils.SettingsManager;
import com.noty215.notycaption.utils.Translator;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Settings dialog for application configuration
 */
public class SettingsDialog extends Dialog<SettingsManager> {

    private final SettingsManager settings;
    private final Translator translator;

    // UI Components
    private ComboBox<String> themeCombo;
    private ComboBox<String> scaleCombo;
    private TextField tempDirField;
    private TextField modelsDirField;
    private CheckBox autoEnhanceCheck;
    private Spinner<Integer> wordsSpinner;
    private ComboBox<String> formatCombo;
    private CheckBox confirmCancelCheck;
    private Spinner<Integer> timeoutSpinner;
    private Spinner<Integer> retrySpinner;
    private CheckBox minimizeTrayCheck;
    private CheckBox showTooltipsCheck;
    private ComboBox<String> languageCombo;

    public SettingsDialog(SettingsManager settings, Translator translator) {
        this.settings = settings;
        this.translator = translator;

        setTitle(translator.tr("settings_title"));
        initModality(Modality.APPLICATION_MODAL);

        DialogPane dialogPane = getDialogPane();
        dialogPane.setPrefWidth(800);
        dialogPane.setPrefHeight(600);

        TabPane tabPane = new TabPane();

        // General tab
        Tab generalTab = new Tab(translator.tr("general_tab"));
        generalTab.setContent(createGeneralTab());
        tabPane.getTabs().add(generalTab);

        // Paths tab
        Tab pathsTab = new Tab(translator.tr("paths_tab"));
        pathsTab.setContent(createPathsTab());
        tabPane.getTabs().add(pathsTab);

        // Features tab
        Tab featuresTab = new Tab(translator.tr("features_tab"));
        featuresTab.setContent(createFeaturesTab());
        tabPane.getTabs().add(featuresTab);

        // Advanced tab
        Tab advancedTab = new Tab(translator.tr("advanced_tab"));
        advancedTab.setContent(createAdvancedTab());
        tabPane.getTabs().add(advancedTab);

        dialogPane.setContent(tabPane);

        // Buttons
        ButtonType applyButton = new ButtonType(translator.tr("apply_restart"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType(translator.tr("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().addAll(applyButton, cancelButton);

        setResultConverter(buttonType -> {
            if (buttonType == applyButton) {
                applySettings();
                return settings;
            }
            return null;
        });

        loadCurrentSettings();
    }

    private VBox createGeneralTab() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(15));

        // Theme
        TitledPane themeBox = new TitledPane();
        themeBox.setText(translator.tr("visual_theme"));
        themeBox.setCollapsible(false);
        VBox themeLayout = new VBox(5);
        themeCombo = new ComboBox<>();
        themeCombo.getItems().addAll(translator.tr("system_default"), translator.tr("light_mode"), translator.tr("dark_mode"));
        themeLayout.getChildren().add(themeCombo);
        themeBox.setContent(themeLayout);

        // UI Scaling
        TitledPane scaleBox = new TitledPane();
        scaleBox.setText(translator.tr("ui_scaling"));
        scaleBox.setCollapsible(false);
        VBox scaleLayout = new VBox(5);
        scaleCombo = new ComboBox<>();
        scaleCombo.getItems().addAll("75%", "87%", "100%", "125%", "150%", "175%", "200%");
        scaleLayout.getChildren().add(scaleCombo);
        scaleBox.setContent(scaleLayout);

        vbox.getChildren().addAll(themeBox, scaleBox);

        return vbox;
    }

    private VBox createPathsTab() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(15));

        // Temp directory
        TitledPane tempBox = new TitledPane();
        tempBox.setText(translator.tr("temp_dir"));
        tempBox.setCollapsible(false);
        GridPane tempLayout = new GridPane();
        tempLayout.setHgap(5);
        tempLayout.setVgap(5);
        tempDirField = new TextField();
        tempDirField.setPromptText(translator.tr("temp_dir_placeholder"));
        Button tempBrowseBtn = new Button(translator.tr("browse_folder"));
        tempBrowseBtn.setOnAction(e -> browseFolder(tempDirField));
        tempLayout.add(tempDirField, 0, 0);
        tempLayout.add(tempBrowseBtn, 1, 0);
        tempBox.setContent(tempLayout);

        // Models directory
        TitledPane modelsBox = new TitledPane();
        modelsBox.setText(translator.tr("models_dir"));
        modelsBox.setCollapsible(false);
        GridPane modelsLayout = new GridPane();
        modelsLayout.setHgap(5);
        modelsLayout.setVgap(5);
        modelsDirField = new TextField();
        modelsDirField.setPromptText(translator.tr("models_dir_placeholder"));
        Button modelsBrowseBtn = new Button(translator.tr("browse_folder"));
        modelsBrowseBtn.setOnAction(e -> browseFolder(modelsDirField));
        modelsLayout.add(modelsDirField, 0, 0);
        modelsLayout.add(modelsBrowseBtn, 1, 0);
        modelsBox.setContent(modelsLayout);

        vbox.getChildren().addAll(tempBox, modelsBox);

        return vbox;
    }

    private VBox createFeaturesTab() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(15));

        // Auto features
        TitledPane autoBox = new TitledPane();
        autoBox.setText(translator.tr("auto_features"));
        autoBox.setCollapsible(false);
        VBox autoLayout = new VBox(5);
        autoEnhanceCheck = new CheckBox(translator.tr("auto_enhance"));
        autoLayout.getChildren().add(autoEnhanceCheck);
        autoBox.setContent(autoLayout);

        // Default words per line
        TitledPane wplBox = new TitledPane();
        wplBox.setText(translator.tr("default_wpl"));
        wplBox.setCollapsible(false);
        GridPane wplLayout = new GridPane();
        wplLayout.setHgap(5);
        wplLayout.setVgap(5);
        wplLayout.add(new Label(translator.tr("words")), 0, 0);
        wordsSpinner = new Spinner<>(1, 20, 5);
        wplLayout.add(wordsSpinner, 1, 0);
        wplBox.setContent(wplLayout);

        // Default format
        TitledPane formatBox = new TitledPane();
        formatBox.setText(translator.tr("default_format"));
        formatBox.setCollapsible(false);
        GridPane formatLayout = new GridPane();
        formatLayout.setHgap(5);
        formatLayout.setVgap(5);
        formatLayout.add(new Label(translator.tr("format")), 0, 0);
        formatCombo = new ComboBox<>();
        formatCombo.getItems().addAll(translator.tr("srt_format"), translator.tr("ass_format"));
        formatLayout.add(formatCombo, 1, 0);
        formatBox.setContent(formatLayout);

        vbox.getChildren().addAll(autoBox, wplBox, formatBox);

        return vbox;
    }

    private VBox createAdvancedTab() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(15));

        // Cancel options
        TitledPane cancelBox = new TitledPane();
        cancelBox.setText(translator.tr("cancel_options"));
        cancelBox.setCollapsible(false);
        VBox cancelLayout = new VBox(5);
        confirmCancelCheck = new CheckBox(translator.tr("confirm_cancel"));
        cancelLayout.getChildren().add(confirmCancelCheck);

        GridPane timeoutLayout = new GridPane();
        timeoutLayout.setHgap(5);
        timeoutLayout.setVgap(5);
        timeoutLayout.add(new Label(translator.tr("force_cancel_timeout")), 0, 0);
        timeoutSpinner = new Spinner<>(10, 120, 30);
        timeoutLayout.add(timeoutSpinner, 1, 0);
        timeoutLayout.add(new Label(translator.tr("seconds")), 2, 0);
        cancelLayout.getChildren().add(timeoutLayout);

        GridPane retryLayout = new GridPane();
        retryLayout.setHgap(5);
        retryLayout.setVgap(5);
        retryLayout.add(new Label(translator.tr("max_retry")), 0, 0);
        retrySpinner = new Spinner<>(1, 20, 5);
        retryLayout.add(retrySpinner, 1, 0);
        cancelLayout.getChildren().add(retryLayout);

        cancelBox.setContent(cancelLayout);

        // UI Options
        TitledPane uiBox = new TitledPane();
        uiBox.setText(translator.tr("ui_options"));
        uiBox.setCollapsible(false);
        VBox uiLayout = new VBox(5);
        minimizeTrayCheck = new CheckBox(translator.tr("minimize_tray"));
        uiLayout.getChildren().add(minimizeTrayCheck);
        showTooltipsCheck = new CheckBox(translator.tr("show_tooltips"));
        uiLayout.getChildren().add(showTooltipsCheck);

        GridPane languageLayout = new GridPane();
        languageLayout.setHgap(5);
        languageLayout.setVgap(5);
        languageLayout.add(new Label(translator.tr("ui_language")), 0, 0);
        languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll("English", "日本語", "Русский", "Deutsch", "हिन्दी", "اردو", "العربية");
        languageLayout.add(languageCombo, 1, 0);
        uiLayout.getChildren().add(languageLayout);

        uiBox.setContent(uiLayout);

        vbox.getChildren().addAll(cancelBox, uiBox);

        return vbox;
    }

    private void browseFolder(TextField textField) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(translator.tr("browse_folder"));
        File currentDir = new File(textField.getText());
        if (currentDir.exists()) {
            fileChooser.setInitialDirectory(currentDir);
        }

        File selectedDir = fileChooser.showOpenDialog(getDialogPane().getScene().getWindow());
        if (selectedDir != null && selectedDir.isDirectory()) {
            textField.setText(selectedDir.getAbsolutePath());
        }
    }

    private void loadCurrentSettings() {
        // General
        String theme = settings.getString("theme");
        if ("LIGHT".equals(theme)) {
            themeCombo.setValue(translator.tr("light_mode"));
        } else if ("SYSTEM".equals(theme)) {
            themeCombo.setValue(translator.tr("system_default"));
        } else {
            themeCombo.setValue(translator.tr("dark_mode"));
        }

        scaleCombo.setValue(settings.getString("ui_scale"));

        // Paths
        tempDirField.setText(settings.getString("temp_dir"));
        modelsDirField.setText(settings.getString("models_dir"));

        // Features
        autoEnhanceCheck.setSelected(settings.getBoolean("auto_enhance"));
        wordsSpinner.getValueFactory().setValue(settings.getInt("words_per_line"));
        formatCombo.setValue(settings.getString("output_format"));

        // Advanced
        confirmCancelCheck.setSelected(settings.getBoolean("confirm_cancel"));
        timeoutSpinner.getValueFactory().setValue(settings.getInt("force_cancel_timeout"));
        retrySpinner.getValueFactory().setValue(settings.getInt("max_retry_attempts"));
        minimizeTrayCheck.setSelected(settings.getBoolean("minimize_to_tray"));
        showTooltipsCheck.setSelected(settings.getBoolean("show_tooltips"));

        String language = settings.getString("language");
        switch (language) {
            case "ja": languageCombo.setValue("日本語"); break;
            case "ru": languageCombo.setValue("Русский"); break;
            case "de": languageCombo.setValue("Deutsch"); break;
            case "hi": languageCombo.setValue("हिन्दी"); break;
            case "ur": languageCombo.setValue("اردو"); break;
            case "ar": languageCombo.setValue("العربية"); break;
            default: languageCombo.setValue("English");
        }
    }

    private void applySettings() {
        // General
        String themeValue = themeCombo.getValue();
        if (translator.tr("light_mode").equals(themeValue)) {
            settings.set("theme", "LIGHT");
        } else if (translator.tr("system_default").equals(themeValue)) {
            settings.set("theme", "SYSTEM");
        } else {
            settings.set("theme", "DARK");
        }

        settings.set("ui_scale", scaleCombo.getValue());

        // Paths
        settings.set("temp_dir", tempDirField.getText());
        settings.set("models_dir", modelsDirField.getText());

        // Features
        settings.set("auto_enhance", autoEnhanceCheck.isSelected());
        settings.set("words_per_line", wordsSpinner.getValue());
        settings.set("output_format", formatCombo.getValue());

        // Advanced
        settings.set("confirm_cancel", confirmCancelCheck.isSelected());
        settings.set("force_cancel_timeout", timeoutSpinner.getValue());
        settings.set("max_retry_attempts", retrySpinner.getValue());
        settings.set("minimize_to_tray", minimizeTrayCheck.isSelected());
        settings.set("show_tooltips", showTooltipsCheck.isSelected());

        String language = languageCombo.getValue();
        switch (language) {
            case "日本語": settings.set("language", "ja"); break;
            case "Русский": settings.set("language", "ru"); break;
            case "Deutsch": settings.set("language", "de"); break;
            case "हिन्दी": settings.set("language", "hi"); break;
            case "اردو": settings.set("language", "ur"); break;
            case "العربية": settings.set("language", "ar"); break;
            default: settings.set("language", "en");
        }

        settings.save();
    }
}