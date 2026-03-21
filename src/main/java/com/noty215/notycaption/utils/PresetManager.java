package com.noty215.notycaption.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Logger;

public class PresetManager {
    private static final Logger logger = Logger.getLogger(PresetManager.class.getName());
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private File presetsDir;
    private List<Map<String, Object>> defaultPresets;
    
    public PresetManager() {
        this.presetsDir = new File(com.noty215.notycaption.App.PRESETS_DIR);
        if (!presetsDir.exists()) {
            presetsDir.mkdirs();
        }
        
        initializeDefaultPresets();
        logger.info("Preset manager initialized: " + presetsDir.getAbsolutePath());
    }
    
    private void initializeDefaultPresets() {
        defaultPresets = new ArrayList<>();
        
        // Professional Blue
        Map<String, Object> blue = new HashMap<>();
        blue.put("name", "Professional Blue");
        Map<String, String> blueColors = new HashMap<>();
        blueColors.put("accent_primary", "#0078d4");
        blueColors.put("accent_secondary", "#8661c5");
        blueColors.put("glow_blue", "#0078d4");
        blueColors.put("glow_purple", "#8661c5");
        blueColors.put("glow_cyan", "#00b7c3");
        blue.put("colors", blueColors);
        defaultPresets.add(blue);
        
        // Cyber Purple
        Map<String, Object> purple = new HashMap<>();
        purple.put("name", "Cyber Purple");
        Map<String, String> purpleColors = new HashMap<>();
        purpleColors.put("accent_primary", "#9b59b6");
        purpleColors.put("accent_secondary", "#3498db");
        purpleColors.put("glow_blue", "#9b59b6");
        purpleColors.put("glow_purple", "#e74c3c");
        purpleColors.put("glow_cyan", "#3498db");
        purple.put("colors", purpleColors);
        defaultPresets.add(purple);
        
        // Teal Dream
        Map<String, Object> teal = new HashMap<>();
        teal.put("name", "Teal Dream");
        Map<String, String> tealColors = new HashMap<>();
        tealColors.put("accent_primary", "#1abc9c");
        tealColors.put("accent_secondary", "#3498db");
        tealColors.put("glow_blue", "#1abc9c");
        tealColors.put("glow_purple", "#9b59b6");
        tealColors.put("glow_cyan", "#3498db");
        teal.put("colors", tealColors);
        defaultPresets.add(teal);
        
        // Sunset Orange
        Map<String, Object> orange = new HashMap<>();
        orange.put("name", "Sunset Orange");
        Map<String, String> orangeColors = new HashMap<>();
        orangeColors.put("accent_primary", "#e67e22");
        orangeColors.put("accent_secondary", "#e74c3c");
        orangeColors.put("glow_blue", "#e67e22");
        orangeColors.put("glow_purple", "#e74c3c");
        orangeColors.put("glow_cyan", "#f39c12");
        orange.put("colors", orangeColors);
        defaultPresets.add(orange);
        
        // Forest Green
        Map<String, Object> green = new HashMap<>();
        green.put("name", "Forest Green");
        Map<String, String> greenColors = new HashMap<>();
        greenColors.put("accent_primary", "#27ae60");
        greenColors.put("accent_secondary", "#2980b9");
        greenColors.put("glow_blue", "#27ae60");
        greenColors.put("glow_purple", "#8e44ad");
        greenColors.put("glow_cyan", "#2980b9");
        green.put("colors", greenColors);
        defaultPresets.add(green);
        
        // Ruby Red
        Map<String, Object> red = new HashMap<>();
        red.put("name", "Ruby Red");
        Map<String, String> redColors = new HashMap<>();
        redColors.put("accent_primary", "#e74c3c");
        redColors.put("accent_secondary", "#c0392b");
        redColors.put("glow_blue", "#e74c3c");
        redColors.put("glow_purple", "#8e44ad");
        redColors.put("glow_cyan", "#3498db");
        red.put("colors", redColors);
        defaultPresets.add(red);
        
        // Amber Gold
        Map<String, Object> amber = new HashMap<>();
        amber.put("name", "Amber Gold");
        Map<String, String> amberColors = new HashMap<>();
        amberColors.put("accent_primary", "#f39c12");
        amberColors.put("accent_secondary", "#e67e22");
        amberColors.put("glow_blue", "#f39c12");
        amberColors.put("glow_purple", "#e67e22");
        amberColors.put("glow_cyan", "#27ae60");
        amber.put("colors", amberColors);
        defaultPresets.add(amber);
        
        // Deep Space
        Map<String, Object> space = new HashMap<>();
        space.put("name", "Deep Space");
        Map<String, String> spaceColors = new HashMap<>();
        spaceColors.put("accent_primary", "#34495e");
        spaceColors.put("accent_secondary", "#2c3e50");
        spaceColors.put("glow_blue", "#34495e");
        spaceColors.put("glow_purple", "#7f8c8d");
        spaceColors.put("glow_cyan", "#95a5a6");
        space.put("colors", spaceColors);
        defaultPresets.add(space);
    }
    
    public List<Map<String, Object>> getPresets() {
        List<Map<String, Object>> presets = new ArrayList<>(defaultPresets);
        
        File[] files = presetsDir.listFiles((dir, name) -> name.endsWith(".preset"));
        if (files != null) {
            for (File file : files) {
                try (Reader reader = new FileReader(file)) {
                    Type type = new TypeToken<Map<String, Object>>(){}.getType();
                    Map<String, Object> preset = gson.fromJson(reader, type);
                    presets.add(preset);
                } catch (IOException e) {
                    logger.warning("Failed to load preset: " + file.getName());
                }
            }
        }
        
        return presets;
    }
    
    public Map<String, Object> getPreset(String name) {
        for (Map<String, Object> preset : getPresets()) {
            if (name.equals(preset.get("name"))) {
                return preset;
            }
        }
        return null;
    }
    
    public boolean savePreset(Map<String, Object> preset) {
        String name = (String) preset.get("name");
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        File presetFile = new File(presetsDir, name + ".preset");
        try (Writer writer = new FileWriter(presetFile)) {
            gson.toJson(preset, writer);
            logger.info("Preset saved: " + name);
            return true;
        } catch (IOException e) {
            logger.severe("Failed to save preset: " + e.getMessage());
            return false;
        }
    }
    
    public boolean deletePreset(String name) {
        File presetFile = new File(presetsDir, name + ".preset");
        if (presetFile.exists()) {
            if (presetFile.delete()) {
                logger.info("Preset deleted: " + name);
                return true;
            }
        }
        return false;
    }
    
    public boolean presetExists(String name) {
        return new File(presetsDir, name + ".preset").exists();
    }
}