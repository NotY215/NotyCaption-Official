package com.noty215.notycaption.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Preset manager for color schemes and settings presets
 */
public class PresetManager {

    private static final Logger logger = LoggerFactory.getLogger(PresetManager.class);
    private static final String PRESETS_DIR = SettingsManager.getAppDataDir() + File.separator + "presets";

    private final Gson gson;
    private final List<Map<String, Object>> defaultPresets;

    public PresetManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.defaultPresets = createDefaultPresets();

        try {
            Files.createDirectories(Paths.get(PRESETS_DIR));
        } catch (IOException e) {
            logger.error("Failed to create presets directory", e);
        }

        logger.info("Preset manager initialized: {}", PRESETS_DIR);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> createDefaultPresets() {
        List<Map<String, Object>> presets = new ArrayList<>();

        Map<String, Object> professionalBlue = new HashMap<>();
        professionalBlue.put("name", "Professional Blue");
        Map<String, String> colors1 = new HashMap<>();
        colors1.put("accent_primary", "#0078d4");
        colors1.put("accent_secondary", "#8661c5");
        colors1.put("glow_blue", "#0078d4");
        colors1.put("glow_purple", "#8661c5");
        colors1.put("glow_cyan", "#00b7c3");
        professionalBlue.put("colors", colors1);
        presets.add(professionalBlue);

        Map<String, Object> cyberPurple = new HashMap<>();
        cyberPurple.put("name", "Cyber Purple");
        Map<String, String> colors2 = new HashMap<>();
        colors2.put("accent_primary", "#9b59b6");
        colors2.put("accent_secondary", "#3498db");
        colors2.put("glow_blue", "#9b59b6");
        colors2.put("glow_purple", "#e74c3c");
        colors2.put("glow_cyan", "#3498db");
        cyberPurple.put("colors", colors2);
        presets.add(cyberPurple);

        Map<String, Object> tealDream = new HashMap<>();
        tealDream.put("name", "Teal Dream");
        Map<String, String> colors3 = new HashMap<>();
        colors3.put("accent_primary", "#1abc9c");
        colors3.put("accent_secondary", "#3498db");
        colors3.put("glow_blue", "#1abc9c");
        colors3.put("glow_purple", "#9b59b6");
        colors3.put("glow_cyan", "#3498db");
        tealDream.put("colors", colors3);
        presets.add(tealDream);

        Map<String, Object> sunsetOrange = new HashMap<>();
        sunsetOrange.put("name", "Sunset Orange");
        Map<String, String> colors4 = new HashMap<>();
        colors4.put("accent_primary", "#e67e22");
        colors4.put("accent_secondary", "#e74c3c");
        colors4.put("glow_blue", "#e67e22");
        colors4.put("glow_purple", "#e74c3c");
        colors4.put("glow_cyan", "#f39c12");
        sunsetOrange.put("colors", colors4);
        presets.add(sunsetOrange);

        Map<String, Object> forestGreen = new HashMap<>();
        forestGreen.put("name", "Forest Green");
        Map<String, String> colors5 = new HashMap<>();
        colors5.put("accent_primary", "#27ae60");
        colors5.put("accent_secondary", "#2980b9");
        colors5.put("glow_blue", "#27ae60");
        colors5.put("glow_purple", "#8e44ad");
        colors5.put("glow_cyan", "#2980b9");
        forestGreen.put("colors", colors5);
        presets.add(forestGreen);

        Map<String, Object> rubyRed = new HashMap<>();
        rubyRed.put("name", "Ruby Red");
        Map<String, String> colors6 = new HashMap<>();
        colors6.put("accent_primary", "#e74c3c");
        colors6.put("accent_secondary", "#c0392b");
        colors6.put("glow_blue", "#e74c3c");
        colors6.put("glow_purple", "#8e44ad");
        colors6.put("glow_cyan", "#3498db");
        rubyRed.put("colors", colors6);
        presets.add(rubyRed);

        Map<String, Object> amberGold = new HashMap<>();
        amberGold.put("name", "Amber Gold");
        Map<String, String> colors7 = new HashMap<>();
        colors7.put("accent_primary", "#f39c12");
        colors7.put("accent_secondary", "#e67e22");
        colors7.put("glow_blue", "#f39c12");
        colors7.put("glow_purple", "#e67e22");
        colors7.put("glow_cyan", "#27ae60");
        amberGold.put("colors", colors7);
        presets.add(amberGold);

        Map<String, Object> deepSpace = new HashMap<>();
        deepSpace.put("name", "Deep Space");
        Map<String, String> colors8 = new HashMap<>();
        colors8.put("accent_primary", "#34495e");
        colors8.put("accent_secondary", "#2c3e50");
        colors8.put("glow_blue", "#34495e");
        colors8.put("glow_purple", "#7f8c8d");
        colors8.put("glow_cyan", "#95a5a6");
        deepSpace.put("colors", colors8);
        presets.add(deepSpace);

        return presets;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPresets() {
        List<Map<String, Object>> presets = new ArrayList<>(defaultPresets);

        try {
            File dir = new File(PRESETS_DIR);
            File[] files = dir.listFiles((d, name) -> name.endsWith(".preset"));
            if (files != null) {
                for (File file : files) {
                    String json = new String(Files.readAllBytes(file.toPath()));
                    Map<String, Object> preset = gson.fromJson(json, Map.class);
                    presets.add(preset);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to load presets", e);
        }

        return presets;
    }

    public boolean savePreset(String name, Map<String, String> colors) {
        try {
            Map<String, Object> preset = new HashMap<>();
            preset.put("name", name);
            preset.put("colors", colors);

            String presetFile = PRESETS_DIR + File.separator + name + ".preset";
            String json = gson.toJson(preset);
            Files.write(Paths.get(presetFile), json.getBytes());
            logger.info("Preset saved: {}", name);
            return true;
        } catch (IOException e) {
            logger.error("Failed to save preset: {}", name, e);
            return false;
        }
    }

    public boolean deletePreset(String name) {
        try {
            String presetFile = PRESETS_DIR + File.separator + name + ".preset";
            Path path = Paths.get(presetFile);
            if (Files.exists(path)) {
                Files.delete(path);
                logger.info("Preset deleted: {}", name);
                return true;
            }
        } catch (IOException e) {
            logger.error("Failed to delete preset: {}", name, e);
        }
        return false;
    }
}