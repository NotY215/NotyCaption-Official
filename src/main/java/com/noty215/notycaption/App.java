package com.noty215.notycaption;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class App {
    public static final String APP_NAME = "NotyCaption Pro";
    public static final String APP_AUTHOR = "NotY215";
    public static final String VERSION = "2026.5.0";
    public static final String BUILD = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    public static final String COPYRIGHT = "Copyright © 2026 " + APP_AUTHOR + ". All rights reserved.";
    public static final String[] SCOPES = {"https://www.googleapis.com/auth/drive"};
    
    // Application directories
    public static String APP_DATA_DIR;
    public static String SETTINGS_FILE;
    public static String KEY_FILE;
    public static String SESSION_FILE;
    public static String TOKEN_FILE;
    public static String CLIENT_JSON;
    public static String LAYOUTS_DIR;
    public static String PRESETS_DIR;
    public static String CACHE_DIR;
    public static String THEMES_DIR;
    public static String PLUGINS_DIR;
    public static String EXPORTS_DIR;
    public static String BACKUPS_DIR;
    public static String PROFILES_DIR;
    public static String MONITORING_DIR;
    public static String GRAPHS_DIR;
    public static String TEMP_DIR;
    
    static {
        initializeDirectories();
    }
    
    private static void initializeDirectories() {
        boolean isFrozen = Main.class.getResource("/") != null && 
                           Main.class.getResource("/").toString().contains("file:/");
        
        if (isFrozen) {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                APP_DATA_DIR = System.getenv("APPDATA") + "/" + APP_NAME.replace(" ", "") + "Saves";
            } else {
                APP_DATA_DIR = System.getProperty("user.home") + "/." + 
                               APP_NAME.toLowerCase().replace(" ", "") + "saves";
            }
        } else {
            APP_DATA_DIR = System.getProperty("user.dir");
        }
        
        // Ensure directory exists
        new java.io.File(APP_DATA_DIR).mkdirs();
        
        SETTINGS_FILE = APP_DATA_DIR + "/settings.notcapz";
        KEY_FILE = APP_DATA_DIR + "/key.notcapz";
        SESSION_FILE = APP_DATA_DIR + "/session.json";
        TOKEN_FILE = APP_DATA_DIR + "/token.json";
        CLIENT_JSON = APP_DATA_DIR + "/client.json";
        LAYOUTS_DIR = APP_DATA_DIR + "/layouts";
        PRESETS_DIR = APP_DATA_DIR + "/presets";
        CACHE_DIR = APP_DATA_DIR + "/cache";
        THEMES_DIR = APP_DATA_DIR + "/themes";
        PLUGINS_DIR = APP_DATA_DIR + "/plugins";
        EXPORTS_DIR = APP_DATA_DIR + "/exports";
        BACKUPS_DIR = APP_DATA_DIR + "/backups";
        PROFILES_DIR = APP_DATA_DIR + "/profiles";
        MONITORING_DIR = APP_DATA_DIR + "/monitoring";
        GRAPHS_DIR = APP_DATA_DIR + "/graphs";
        TEMP_DIR = APP_DATA_DIR + "/temp";
        
        // Create all directories
        String[] dirs = {LAYOUTS_DIR, PRESETS_DIR, CACHE_DIR, THEMES_DIR, PLUGINS_DIR,
                         EXPORTS_DIR, BACKUPS_DIR, PROFILES_DIR, MONITORING_DIR,
                         GRAPHS_DIR, TEMP_DIR};
        for (String dir : dirs) {
            new java.io.File(dir).mkdirs();
        }
    }
}