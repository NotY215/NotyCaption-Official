package com.noty215.notycaption.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class LayoutManager {
    private static final Logger logger = Logger.getLogger(LayoutManager.class.getName());
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private File layoutsDir;
    
    public LayoutManager() {
        this.layoutsDir = new File(com.noty215.notycaption.App.LAYOUTS_DIR);
        if (!layoutsDir.exists()) {
            layoutsDir.mkdirs();
        }
        logger.info("Layout manager initialized: " + layoutsDir.getAbsolutePath());
    }
    
    public boolean saveLayout(String name, Map<String, Object> settings) {
        File layoutFile = new File(layoutsDir, name + ".layout");
        try (Writer writer = new FileWriter(layoutFile)) {
            gson.toJson(settings, writer);
            logger.info("Layout saved: " + name);
            return true;
        } catch (IOException e) {
            logger.severe("Failed to save layout: " + e.getMessage());
            return false;
        }
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> loadLayout(String name) {
        File layoutFile = new File(layoutsDir, name + ".layout");
        if (!layoutFile.exists()) {
            return null;
        }
        
        try (Reader reader = new FileReader(layoutFile)) {
            Map<String, Object> settings = gson.fromJson(reader, Map.class);
            logger.info("Layout loaded: " + name);
            return settings;
        } catch (IOException e) {
            logger.severe("Failed to load layout: " + e.getMessage());
            return null;
        }
    }
    
    public List<String> getLayouts() {
        List<String> layouts = new ArrayList<>();
        File[] files = layoutsDir.listFiles((dir, name) -> name.endsWith(".layout"));
        if (files != null) {
            for (File file : files) {
                layouts.add(file.getName().replace(".layout", ""));
            }
        }
        Collections.sort(layouts);
        return layouts;
    }
    
    public boolean deleteLayout(String name) {
        File layoutFile = new File(layoutsDir, name + ".layout");
        if (layoutFile.exists()) {
            if (layoutFile.delete()) {
                logger.info("Layout deleted: " + name);
                return true;
            } else {
                logger.warning("Failed to delete layout: " + name);
                return false;
            }
        }
        return false;
    }
    
    public boolean layoutExists(String name) {
        return new File(layoutsDir, name + ".layout").exists();
    }
}