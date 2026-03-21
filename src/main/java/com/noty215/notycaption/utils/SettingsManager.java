package com.noty215.notycaption.utils;

import com.noty215.notycaption.App;

import java.io.File;
import java.util.*;

public class SettingsManager {
    private static final java.util.logging.Logger logger = 
        java.util.logging.Logger.getLogger(SettingsManager.class.getName());
    
    public static Map<String, Object> getDefaultSettings() {
        Map<String, Object> defaults = new HashMap<>();
        
        // UI Settings
        defaults.put("ui_scale", "100%");
        defaults.put("theme", "Dark");
        defaults.put("temp_dir", System.getProperty("java.io.tmpdir"));
        defaults.put("models_dir", App.APP_DATA_DIR);
        defaults.put("cache_dir", App.CACHE_DIR);
        defaults.put("last_mode", "normal");
        defaults.put("auto_enhance", false);
        defaults.put("default_lang", "English");
        defaults.put("force_cancel_timeout", 30);
        defaults.put("max_retry_attempts", 5);
        defaults.put("confirm_cancel", true);
        defaults.put("minimize_to_tray", true);
        defaults.put("show_tooltips", true);
        defaults.put("words_per_line", 5);
        defaults.put("output_format", "SRT");
        defaults.put("last_input_file", "");
        defaults.put("last_output_folder", "");
        defaults.put("language", "en");
        defaults.put("window_width", 1280);
        defaults.put("window_height", 800);
        defaults.put("window_maximized", false);
        defaults.put("accent_color", "#4a6fa5");
        defaults.put("glow_intensity", 50);
        defaults.put("animation_speed", "normal");
        defaults.put("enable_animations", true);
        defaults.put("card_opacity", 80);
        defaults.put("font_family", "Segoe UI");
        defaults.put("font_size", 14);
        defaults.put("hardware_monitoring", true);
        defaults.put("monitoring_interval", 1000);
        defaults.put("performance_graphs", true);
        defaults.put("graph_history", 300);
        defaults.put("multi_monitor", false);
        defaults.put("monitor_index", 0);
        defaults.put("preview_widget_enabled", true);
        defaults.put("auto_save", true);
        defaults.put("auto_save_interval", 300);
        defaults.put("backup_count", 10);
        defaults.put("export_format", "SRT");
        defaults.put("export_encoding", "utf-8");
        defaults.put("export_newline", "\r\n");
        defaults.put("export_bom", false);
        defaults.put("timestamp_format", "HH:MM:SS,mmm");
        defaults.put("subtitle_offset", 0);
        defaults.put("subtitle_duration_factor", 1.0);
        defaults.put("max_line_length", 42);
        defaults.put("max_lines", 2);
        defaults.put("word_break", true);
        defaults.put("highlight_current", true);
        defaults.put("highlight_color", "#ffd700");
        defaults.put("highlight_opacity", 180);
        defaults.put("player_volume", 100);
        defaults.put("player_speed", 1.0);
        defaults.put("player_loop", false);
        defaults.put("player_shuffle", false);
        defaults.put("auto_scroll", true);
        defaults.put("scroll_smooth", true);
        defaults.put("scroll_speed", 50);
        defaults.put("keyboard_shortcuts", true);
        defaults.put("mouse_wheel", true);
        defaults.put("touch_support", true);
        defaults.put("gesture_support", true);
        defaults.put("high_dpi", true);
        defaults.put("opengl_rendering", true);
        defaults.put("vsync", true);
        defaults.put("fps_limit", 60);
        defaults.put("smooth_animation", true);
        defaults.put("particle_effects", false);
        defaults.put("transparency_effects", true);
        defaults.put("shadow_effects", true);
        defaults.put("blur_effects", false);
        defaults.put("colorize_effects", false);
        defaults.put("custom_cursor", false);
        defaults.put("custom_scrollbar", true);
        defaults.put("custom_titlebar", false);
        defaults.put("native_menubar", true);
        defaults.put("native_dialogs", true);
        defaults.put("system_tray", true);
        defaults.put("start_minimized", false);
        defaults.put("auto_update", true);
        defaults.put("update_check_interval", 86400);
        defaults.put("beta_updates", false);
        defaults.put("telemetry", false);
        defaults.put("crash_reporting", true);
        defaults.put("log_level", "INFO");
        defaults.put("log_rotation", true);
        defaults.put("log_max_size", 10485760);
        defaults.put("log_backup_count", 5);
        defaults.put("log_compress", true);
        defaults.put("debug_mode", false);
        defaults.put("developer_mode", false);
        defaults.put("plugin_enabled", true);
        defaults.put("plugin_auto_load", false);
        defaults.put("plugin_sandbox", true);
        defaults.put("scripting_enabled", false);
        defaults.put("macro_enabled", false);
        defaults.put("batch_mode", false);
        defaults.put("headless_mode", false);
        defaults.put("remote_control", false);
        defaults.put("web_interface", false);
        defaults.put("api_enabled", false);
        defaults.put("api_port", 8080);
        defaults.put("api_key", "");
        defaults.put("api_ssl", false);
        defaults.put("api_cert", "");
        defaults.put("api_key_file", "");
        defaults.put("database_enabled", false);
        defaults.put("database_type", "sqlite");
        defaults.put("database_host", "localhost");
        defaults.put("database_port", 3306);
        defaults.put("database_name", "notycaption");
        defaults.put("database_user", "");
        defaults.put("database_password", "");
        defaults.put("database_ssl", false);
        defaults.put("cloud_sync", false);
        defaults.put("cloud_provider", "google");
        defaults.put("cloud_folder", "NotyCaption");
        defaults.put("cloud_auto_sync", false);
        defaults.put("cloud_sync_interval", 3600);
        defaults.put("encryption_enabled", true);
        defaults.put("encryption_method", "aes");
        defaults.put("password_protect", false);
        defaults.put("master_password", "");
        defaults.put("session_restore", true);
        defaults.put("session_autosave", true);
        defaults.put("session_max", 10);
        defaults.put("undo_depth", 100);
        defaults.put("redo_depth", 100);
        defaults.put("history_enabled", true);
        defaults.put("history_max", 1000);
        defaults.put("recent_files", new ArrayList<>());
        defaults.put("recent_max", 10);
        defaults.put("favorite_files", new ArrayList<>());
        defaults.put("favorite_folders", new ArrayList<>());
        defaults.put("bookmarks", new ArrayList<>());
        defaults.put("notes", new ArrayList<>());
        defaults.put("tags", new ArrayList<>());
        defaults.put("categories", new ArrayList<>());
        defaults.put("collections", new ArrayList<>());
        defaults.put("playlists", new ArrayList<>());
        defaults.put("queues", new ArrayList<>());
        defaults.put("templates", new ArrayList<>());
        defaults.put("snippets", new ArrayList<>());
        defaults.put("macros", new ArrayList<>());
        defaults.put("scripts", new ArrayList<>());
        defaults.put("plugins", new ArrayList<>());
        defaults.put("themes", new ArrayList<>());
        defaults.put("layouts", new ArrayList<>());
        defaults.put("presets", new ArrayList<>());
        defaults.put("profiles", new ArrayList<>());
        defaults.put("workspaces", new ArrayList<>());
        defaults.put("projects", new ArrayList<>());
        
        return defaults;
    }
    
    public static void validateDirectories(Map<String, Object> settings) {
        String[] pathKeys = {"temp_dir", "models_dir", "cache_dir"};
        for (String key : pathKeys) {
            if (settings.containsKey(key)) {
                String path = (String) settings.get(key);
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                    logger.info("Created missing directory: " + path);
                }
            }
        }
    }
}