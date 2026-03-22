package com.noty215.notycaption.ui;

import com.noty215.notycaption.utils.LayoutManager;
import com.noty215.notycaption.utils.PresetManager;
import com.noty215.notycaption.utils.SettingsManager;
import com.noty215.notycaption.utils.Translator;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Workspace customization dialog
 */
public class WorkspaceCustomizeDialog extends Dialog<SettingsManager> {

    private final SettingsManager settings;
    private final Translator translator;
    private final LayoutManager layoutManager;
    private final PresetManager presetManager;

    // UI Components
    private ColorPicker accentColorPicker;
    private Slider glowSlider;
    private Slider opacitySlider;
    private ComboBox<String> fontCombo;
    private Spinner<Integer> fontSizeSpinner;
    private CheckBox enableAnimationsCheck;
    private ComboBox<String> speedCombo;
    private ComboBox<String> presetCombo;
    private TextField layoutNameField;
    private ComboBox<String> layoutCombo;
    private TextField presetNameField;

    public WorkspaceCustomizeDialog(SettingsManager settings, Translator translator) {
        this.settings = settings;
        this.translator = translator;
        this.layoutManager = new LayoutManager();
        this.presetManager = new PresetManager();

        setTitle(translator.tr("workspace_customize"));
        initModality(Modality.APPLICATION_MODAL);

        DialogPane dialogPane = getDialogPane();
        dialogPane.setPrefWidth(700);
        dialogPane.setPrefHeight(600);

        TabPane tabPane = new TabPane();

        // Presets tab
        Tab presetsTab = new Tab(translator.tr("presets_tab"));
        presetsTab.setContent(createPresetsTab());
        tabPane.getTabs().add(presetsTab);

        // Animation tab
        Tab animTab = new Tab(translator.tr("animation_control"));
        animTab.setContent(createAnimationTab());
        tabPane.getTabs().add(animTab);

        // Appearance tab
        Tab appearanceTab = new Tab(translator.tr("workspace_tab"));
        appearanceTab.setContent(createAppearanceTab());
        tabPane.getTabs().add(appearanceTab);

        // Layouts tab
        Tab layoutsTab = new Tab(translator.tr("save_layout"));
        layoutsTab.setContent(createLayoutsTab());
        tabPane.getTabs().add(layoutsTab);

        dialogPane.setContent(tabPane);

        // Preview
        Label previewLabel = new Label(translator.tr("preview"));
        previewLabel.setStyle("-fx-font-weight: bold;");

        Label previewText = new Label(translator.tr("app_name"));
        previewText.setAlignment(javafx.geometry.Pos.CENTER);
        previewText.setMinHeight(100);
        previewText.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #4a6fa5; " +
                "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 20;");

        VBox previewBox = new VBox(5, previewLabel, previewText);
        previewBox.setPadding(new Insets(10));
        dialogPane.getChildren().add(previewBox);

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

    private VBox createPresetsTab() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(15));

        // Accent color
        GridPane colorLayout = new GridPane();
        colorLayout.setHgap(10);
        colorLayout.setVgap(5);
        colorLayout.add(new Label(translator.tr("accent_color")), 0, 0);
        accentColorPicker = new ColorPicker();
        colorLayout.add(accentColorPicker, 1, 0);

        // Presets
        GridPane presetLayout = new GridPane();
        presetLayout.setHgap(10);
        presetLayout.setVgap(5);
        presetLayout.add(new Label(translator.tr("presets")), 0, 0);
        presetCombo = new ComboBox<>();
        loadPresets();
        presetLayout.add(presetCombo, 1, 0);
        Button applyPresetBtn = new Button(translator.tr("apply_preset"));
        applyPresetBtn.setOnAction(e -> applyPreset());
        presetLayout.add(applyPresetBtn, 2, 0);

        // Save preset
        GridPane savePresetLayout = new GridPane();
        savePresetLayout.setHgap(10);
        savePresetLayout.setVgap(5);
        presetNameField = new TextField();
        presetNameField.setPromptText(translator.tr("preset_name"));
        savePresetLayout.add(presetNameField, 0, 0);
        Button savePresetBtn = new Button(translator.tr("save_preset"));
        savePresetBtn.setOnAction(e -> savePreset());
        savePresetLayout.add(savePresetBtn, 1, 0);

        vbox.getChildren().addAll(colorLayout, presetLayout, savePresetLayout);

        return vbox;
    }

    private VBox createAnimationTab() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(15));

        // Enable animations
        enableAnimationsCheck = new CheckBox(translator.tr("enable_animations"));

        // Animation speed
        GridPane speedLayout = new GridPane();
        speedLayout.setHgap(10);
        speedLayout.setVgap(5);
        speedLayout.add(new Label(translator.tr("animation_speed")), 0, 0);
        speedCombo = new ComboBox<>();
        speedCombo.getItems().addAll(
                translator.tr("animation_speed_slow"),
                translator.tr("animation_speed_normal"),
                translator.tr("animation_speed_fast")
        );
        speedLayout.add(speedCombo, 1, 0);

        // Glow intensity
        GridPane glowLayout = new GridPane();
        glowLayout.setHgap(10);
        glowLayout.setVgap(5);
        glowLayout.add(new Label(translator.tr("glow_intensity")), 0, 0);
        glowSlider = new Slider(0, 100, 50);
        glowSlider.setShowTickLabels(true);
        glowSlider.setShowTickMarks(true);
        glowLayout.add(glowSlider, 1, 0);

        vbox.getChildren().addAll(enableAnimationsCheck, speedLayout, glowLayout);

        return vbox;
    }

    private VBox createAppearanceTab() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(15));

        // Card opacity
        GridPane opacityLayout = new GridPane();
        opacityLayout.setHgap(10);
        opacityLayout.setVgap(5);
        opacityLayout.add(new Label(translator.tr("card_opacity")), 0, 0);
        opacitySlider = new Slider(30, 100, 80);
        opacitySlider.setShowTickLabels(true);
        opacitySlider.setShowTickMarks(true);
        opacityLayout.add(opacitySlider, 1, 0);

        // Font family
        GridPane fontLayout = new GridPane();
        fontLayout.setHgap(10);
        fontLayout.setVgap(5);
        fontLayout.add(new Label(translator.tr("font_family")), 0, 0);
        fontCombo = new ComboBox<>();
        fontCombo.getItems().addAll("Segoe UI", "Arial", "Helvetica", "Verdana", "Tahoma", "Consolas");
        fontLayout.add(fontCombo, 1, 0);

        // Font size
        GridPane fontSizeLayout = new GridPane();
        fontSizeLayout.setHgap(10);
        fontSizeLayout.setVgap(5);
        fontSizeLayout.add(new Label(translator.tr("font_size")), 0, 0);
        fontSizeSpinner = new Spinner<>(8, 24, 14);
        fontSizeLayout.add(fontSizeSpinner, 1, 0);

        vbox.getChildren().addAll(opacityLayout, fontLayout, fontSizeLayout);

        return vbox;
    }

    private VBox createLayoutsTab() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(15));

        // Save layout
        GridPane saveLayoutLayout = new GridPane();
        saveLayoutLayout.setHgap(10);
        saveLayoutLayout.setVgap(5);
        saveLayoutLayout.add(new Label(translator.tr("layout_name")), 0, 0);
        layoutNameField = new TextField();
        saveLayoutLayout.add(layoutNameField, 1, 0);
        Button saveLayoutBtn = new Button(translator.tr("save_layout"));
        saveLayoutBtn.setOnAction(e -> saveLayout());
        saveLayoutLayout.add(saveLayoutBtn, 2, 0);

        // Load layout
        GridPane loadLayoutLayout = new GridPane();
        loadLayoutLayout.setHgap(10);
        loadLayoutLayout.setVgap(5);
        loadLayoutLayout.add(new Label(translator.tr("select_layout")), 0, 0);
        layoutCombo = new ComboBox<>();
        refreshLayouts();
        loadLayoutLayout.add(layoutCombo, 1, 0);

        Button loadLayoutBtn = new Button(translator.tr("load_layout"));
        loadLayoutBtn.setOnAction(e -> loadLayout());
        loadLayoutLayout.add(loadLayoutBtn, 2, 0);

        Button deleteLayoutBtn = new Button(translator.tr("delete_preset"));
        deleteLayoutBtn.setOnAction(e -> deleteLayout());
        loadLayoutLayout.add(deleteLayoutBtn, 3, 0);

        vbox.getChildren().addAll(saveLayoutLayout, loadLayoutLayout);

        return vbox;
    }

    private void loadPresets() {
        presetCombo.getItems().clear();
        List<Map<String, Object>> presets = presetManager.getPresets();
        for (Map<String, Object> preset : presets) {
            presetCombo.getItems().add((String) preset.get("name"));
        }
    }

    private void refreshLayouts() {
        layoutCombo.getItems().clear();
        List<String> layouts = layoutManager.getLayouts();
        layoutCombo.getItems().addAll(layouts);
    }

    private void applyPreset() {
        String presetName = presetCombo.getValue();
        if (presetName != null) {
            List<Map<String, Object>> presets = presetManager.getPresets();
            for (Map<String, Object> preset : presets) {
                if (presetName.equals(preset.get("name"))) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> colors = (Map<String, String>) preset.get("colors");
                    String accentColor = colors.get("accent_primary");
                    if (accentColor != null) {
                        accentColorPicker.setValue(Color.web(accentColor));
                    }
                    break;
                }
            }
        }
    }

    private void savePreset() {
        String name = presetNameField.getText().trim();
        if (!name.isEmpty()) {
            Map<String, String> colors = new HashMap<>();
            colors.put("accent_primary", toHex(accentColorPicker.getValue()));

            if (presetManager.savePreset(name, colors)) {
                showInfo("Success", "Preset '" + name + "' saved!");
                presetNameField.clear();
                loadPresets();
            } else {
                showError("Error", "Failed to save preset");
            }
        }
    }

    private void saveLayout() {
        String name = layoutNameField.getText().trim();
        if (!name.isEmpty()) {
            Map<String, Object> layoutSettings = getCurrentSettings();
            if (layoutManager.saveLayout(name, layoutSettings)) {
                showInfo("Success", "Layout '" + name + "' saved!");
                layoutNameField.clear();
                refreshLayouts();
            } else {
                showError("Error", "Failed to save layout");
            }
        }
    }

    private void loadLayout() {
        String name = layoutCombo.getValue();
        if (name != null) {
            Map<String, Object> layoutSettings = layoutManager.loadLayout(name);
            if (layoutSettings != null) {
                applyLayoutSettings(layoutSettings);
                showInfo("Success", "Layout '" + name + "' loaded!");
            } else {
                showError("Error", "Failed to load layout");
            }
        }
    }

    private void deleteLayout() {
        String name = layoutCombo.getValue();
        if (name != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(translator.tr("confirm"));
            alert.setHeaderText("Delete layout '" + name + "'?");
            alert.setContentText(translator.tr("delete_preset"));

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                if (layoutManager.deleteLayout(name)) {
                    refreshLayouts();
                    showInfo("Success", "Layout '" + name + "' deleted!");
                } else {
                    showError("Error", "Failed to delete layout");
                }
            }
        }
    }

    private void applyLayoutSettings(Map<String, Object> layoutSettings) {
        if (layoutSettings.containsKey("accent_color")) {
            accentColorPicker.setValue(Color.web((String) layoutSettings.get("accent_color")));
        }
        if (layoutSettings.containsKey("glow_intensity")) {
            glowSlider.setValue(((Number) layoutSettings.get("glow_intensity")).doubleValue());
        }
        if (layoutSettings.containsKey("card_opacity")) {
            opacitySlider.setValue(((Number) layoutSettings.get("card_opacity")).doubleValue());
        }
        if (layoutSettings.containsKey("font_family")) {
            fontCombo.setValue((String) layoutSettings.get("font_family"));
        }
        if (layoutSettings.containsKey("font_size")) {
            fontSizeSpinner.getValueFactory().setValue(((Number) layoutSettings.get("font_size")).intValue());
        }
        if (layoutSettings.containsKey("enable_animations")) {
            enableAnimationsCheck.setSelected((Boolean) layoutSettings.get("enable_animations"));
        }
        if (layoutSettings.containsKey("animation_speed")) {
            String speed = (String) layoutSettings.get("animation_speed");
            switch (speed) {
                case "slow": speedCombo.setValue(translator.tr("animation_speed_slow")); break;
                case "fast": speedCombo.setValue(translator.tr("animation_speed_fast")); break;
                default: speedCombo.setValue(translator.tr("animation_speed_normal"));
            }
        }
    }

    private void loadCurrentSettings() {
        String accentColor = settings.getString("accent_color");
        if (accentColor != null) {
            accentColorPicker.setValue(Color.web(accentColor));
        }

        glowSlider.setValue(settings.getInt("glow_intensity"));
        opacitySlider.setValue(settings.getInt("card_opacity"));
        fontCombo.setValue(settings.getString("font_family"));
        fontSizeSpinner.getValueFactory().setValue(settings.getInt("font_size"));
        enableAnimationsCheck.setSelected(settings.getBoolean("enable_animations"));

        String speed = settings.getString("animation_speed");
        switch (speed) {
            case "slow": speedCombo.setValue(translator.tr("animation_speed_slow")); break;
            case "fast": speedCombo.setValue(translator.tr("animation_speed_fast")); break;
            default: speedCombo.setValue(translator.tr("animation_speed_normal"));
        }
    }

    private Map<String, Object> getCurrentSettings() {
        Map<String, Object> currentSettings = new HashMap<>();
        currentSettings.put("accent_color", toHex(accentColorPicker.getValue()));
        currentSettings.put("glow_intensity", (int) glowSlider.getValue());
        currentSettings.put("card_opacity", (int) opacitySlider.getValue());
        currentSettings.put("font_family", fontCombo.getValue());
        currentSettings.put("font_size", fontSizeSpinner.getValue());
        currentSettings.put("enable_animations", enableAnimationsCheck.isSelected());

        String speed = speedCombo.getValue();
        if (translator.tr("animation_speed_slow").equals(speed)) {
            currentSettings.put("animation_speed", "slow");
        } else if (translator.tr("animation_speed_fast").equals(speed)) {
            currentSettings.put("animation_speed", "fast");
        } else {
            currentSettings.put("animation_speed", "normal");
        }

        return currentSettings;
    }

    private void applySettings() {
        Map<String, Object> currentSettings = getCurrentSettings();
        for (Map.Entry<String, Object> entry : currentSettings.entrySet()) {
            settings.set(entry.getKey(), entry.getValue());
        }
        settings.save();
    }

    private String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}