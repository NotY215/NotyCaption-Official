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
 * Layout manager for saving and loading UI layouts
 */
public class LayoutManager {

    private static final Logger logger = LoggerFactory.getLogger(LayoutManager.class);
    private static final String LAYOUTS_DIR = SettingsManager.getAppDataDir() + File.separator + "layouts";

    private final Gson gson;

    public LayoutManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            Files.createDirectories(Paths.get(LAYOUTS_DIR));
        } catch (IOException e) {
            logger.error("Failed to create layouts directory", e);
        }

        logger.info("Layout manager initialized: {}", LAYOUTS_DIR);
    }

    public boolean saveLayout(String name, Map<String, Object> settings) {
        try {
            String layoutFile = LAYOUTS_DIR + File.separator + name + ".layout";
            String json = gson.toJson(settings);
            Files.write(Paths.get(layoutFile), json.getBytes());
            logger.info("Layout saved: {}", name);
            return true;
        } catch (IOException e) {
            logger.error("Failed to save layout: {}", name, e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> loadLayout(String name) {
        try {
            String layoutFile = LAYOUTS_DIR + File.separator + name + ".layout";
            Path path = Paths.get(layoutFile);
            if (Files.exists(path)) {
                String json = new String(Files.readAllBytes(path));
                Map<String, Object> settings = gson.fromJson(json, Map.class);
                logger.info("Layout loaded: {}", name);
                return settings;
            }
        } catch (IOException e) {
            logger.error("Failed to load layout: {}", name, e);
        }
        return null;
    }

    public List<String> getLayouts() {
        List<String> layouts = new ArrayList<>();
        try {
            File dir = new File(LAYOUTS_DIR);
            File[] files = dir.listFiles((d, name) -> name.endsWith(".layout"));
            if (files != null) {
                for (File file : files) {
                    String name = file.getName();
                    layouts.add(name.substring(0, name.length() - 7));
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get layouts", e);
        }
        Collections.sort(layouts);
        return layouts;
    }

    public boolean deleteLayout(String name) {
        try {
            String layoutFile = LAYOUTS_DIR + File.separator + name + ".layout";
            Path path = Paths.get(layoutFile);
            if (Files.exists(path)) {
                Files.delete(path);
                logger.info("Layout deleted: {}", name);
                return true;
            }
        } catch (IOException e) {
            logger.error("Failed to delete layout: {}", name, e);
        }
        return false;
    }
}