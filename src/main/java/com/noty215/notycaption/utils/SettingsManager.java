package com.noty215.notycaption.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.noty215.notycaption.models.Language;
import com.noty215.notycaption.models.ProcessingMode;
import com.noty215.notycaption.models.SubtitleFormat;
import com.noty215.notycaption.models.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Settings manager for application configuration
 */
public class SettingsManager {

    private static final Logger logger = LoggerFactory.getLogger(SettingsManager.class);
    private static final String SETTINGS_FILE = "settings.notcapz";
    private static final String KEY_FILE = "key.notcapz";
    private static final String APP_DATA_DIR;

    static {
        String userHome = System.getProperty("user.home");
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                APP_DATA_DIR = appData + File.separator + "NotyCaptionSaves";
            } else {
                APP_DATA_DIR = userHome + File.separator + "NotyCaptionSaves";
            }
        } else {
            APP_DATA_DIR = userHome + File.separator + ".notycaptionsaves";
        }

        try {
            Files.createDirectories(Paths.get(APP_DATA_DIR));
        } catch (IOException e) {
            logger.error("Failed to create app data directory", e);
        }
    }

    private Map<String, Object> settings;
    private final Gson gson;

    public SettingsManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.settings = new HashMap<>();
        loadDefaults();
    }

    private void loadDefaults() {
        settings.put("ui_scale", "100%");
        settings.put("theme", Theme.DARK.name());
        settings.put("temp_dir", System.getProperty("java.io.tmpdir"));
        settings.put("models_dir", APP_DATA_DIR);
        settings.put("cache_dir", APP_DATA_DIR + File.separator + "cache");
        settings.put("last_mode", ProcessingMode.LOCAL.name());
        settings.put("auto_enhance", false);
        settings.put("default_lang", Language.ENGLISH.getCode());
        settings.put("force_cancel_timeout", 30);
        settings.put("max_retry_attempts", 5);
        settings.put("confirm_cancel", true);
        settings.put("minimize_to_tray", true);
        settings.put("show_tooltips", true);
        settings.put("words_per_line", 5);
        settings.put("output_format", SubtitleFormat.SRT.name());
        settings.put("last_input_file", "");
        settings.put("last_output_folder", "");
        settings.put("language", "en");
        settings.put("window_width", 1280);
        settings.put("window_height", 800);
        settings.put("window_maximized", false);
        settings.put("accent_color", "#4a6fa5");
        settings.put("glow_intensity", 50);
        settings.put("animation_speed", "normal");
        settings.put("enable_animations", true);
        settings.put("card_opacity", 80);
        settings.put("font_family", "Segoe UI");
        settings.put("font_size", 14);
        settings.put("hardware_monitoring", true);
        settings.put("monitoring_interval", 1000);
        settings.put("performance_graphs", true);
        settings.put("graph_history", 300);
        settings.put("multi_monitor", false);
        settings.put("monitor_index", 0);
        settings.put("preview_widget_enabled", true);
        settings.put("auto_save", true);
        settings.put("auto_save_interval", 300);
        settings.put("backup_count", 10);
        settings.put("export_format", "SRT");
        settings.put("export_encoding", "utf-8");
        settings.put("export_newline", "\r\n");
        settings.put("export_bom", false);
        settings.put("timestamp_format", "HH:MM:SS,mmm");
        settings.put("subtitle_offset", 0);
        settings.put("subtitle_duration_factor", 1.0);
        settings.put("max_line_length", 42);
        settings.put("max_lines", 2);
        settings.put("word_break", true);
        settings.put("highlight_current", true);
        settings.put("highlight_color", "#ffd700");
        settings.put("highlight_opacity", 180);
        settings.put("player_volume", 100);
        settings.put("player_speed", 1.0);
        settings.put("player_loop", false);
        settings.put("player_shuffle", false);
        settings.put("auto_scroll", true);
        settings.put("scroll_smooth", true);
        settings.put("scroll_speed", 50);
        settings.put("keyboard_shortcuts", true);
        settings.put("mouse_wheel", true);
        settings.put("touch_support", true);
        settings.put("gesture_support", true);
        settings.put("high_dpi", true);
        settings.put("opengl_rendering", true);
        settings.put("vsync", true);
        settings.put("fps_limit", 60);
        settings.put("smooth_animation", true);
        settings.put("particle_effects", false);
        settings.put("transparency_effects", true);
        settings.put("shadow_effects", true);
        settings.put("blur_effects", false);
        settings.put("colorize_effects", false);
        settings.put("custom_cursor", false);
        settings.put("custom_scrollbar", true);
        settings.put("custom_titlebar", false);
        settings.put("native_menubar", true);
        settings.put("native_dialogs", true);
        settings.put("system_tray", true);
        settings.put("start_minimized", false);
        settings.put("auto_update", true);
        settings.put("update_check_interval", 86400);
        settings.put("beta_updates", false);
        settings.put("telemetry", false);
        settings.put("crash_reporting", true);
        settings.put("log_level", "INFO");
        settings.put("log_rotation", true);
        settings.put("log_max_size", 10485760);
        settings.put("log_backup_count", 5);
        settings.put("log_compress", true);
        settings.put("debug_mode", false);
        settings.put("developer_mode", false);
        settings.put("plugin_enabled", true);
        settings.put("plugin_auto_load", false);
        settings.put("plugin_sandbox", true);
        settings.put("scripting_enabled", false);
        settings.put("macro_enabled", false);
        settings.put("batch_mode", false);
        settings.put("headless_mode", false);
        settings.put("remote_control", false);
        settings.put("web_interface", false);
        settings.put("api_enabled", false);
        settings.put("api_port", 8080);
        settings.put("api_key", "");
        settings.put("api_ssl", false);
        settings.put("api_cert", "");
        settings.put("api_key_file", "");
        settings.put("database_enabled", false);
        settings.put("database_type", "sqlite");
        settings.put("database_host", "localhost");
        settings.put("database_port", 3306);
        settings.put("database_name", "notycaption");
        settings.put("database_user", "");
        settings.put("database_password", "");
        settings.put("database_ssl", false);
        settings.put("cloud_sync", false);
        settings.put("cloud_provider", "google");
        settings.put("cloud_folder", "NotyCaption");
        settings.put("cloud_auto_sync", false);
        settings.put("cloud_sync_interval", 3600);
        settings.put("encryption_enabled", true);
        settings.put("encryption_method", "fernet");
        settings.put("password_protect", false);
        settings.put("master_password", "");
        settings.put("session_restore", true);
        settings.put("session_autosave", true);
        settings.put("session_max", 10);
        settings.put("undo_depth", 100);
        settings.put("redo_depth", 100);
        settings.put("history_enabled", true);
        settings.put("history_max", 1000);
        settings.put("recent_files", new java.util.ArrayList<>());
        settings.put("recent_max", 10);
        settings.put("favorite_files", new java.util.ArrayList<>());
        settings.put("favorite_folders", new java.util.ArrayList<>());
        settings.put("bookmarks", new java.util.ArrayList<>());
        settings.put("notes", new java.util.ArrayList<>());
        settings.put("tags", new java.util.ArrayList<>());
        settings.put("categories", new java.util.ArrayList<>());
        settings.put("collections", new java.util.ArrayList<>());
        settings.put("playlists", new java.util.ArrayList<>());
        settings.put("queues", new java.util.ArrayList<>());
        settings.put("templates", new java.util.ArrayList<>());
        settings.put("snippets", new java.util.ArrayList<>());
        settings.put("macros", new java.util.ArrayList<>());
        settings.put("scripts", new java.util.ArrayList<>());
        settings.put("plugins", new java.util.ArrayList<>());
        settings.put("themes", new java.util.ArrayList<>());
        settings.put("layouts", new java.util.ArrayList<>());
        settings.put("presets", new java.util.ArrayList<>());
        settings.put("profiles", new java.util.ArrayList<>());
        settings.put("workspaces", new java.util.ArrayList<>());
        settings.put("projects", new java.util.ArrayList<>());

        // Create necessary directories
        createDirectories();
    }

    private void createDirectories() {
        String[] dirs = {
                (String) settings.get("cache_dir"),
                APP_DATA_DIR + File.separator + "layouts",
                APP_DATA_DIR + File.separator + "presets",
                APP_DATA_DIR + File.separator + "themes",
                APP_DATA_DIR + File.separator + "plugins",
                APP_DATA_DIR + File.separator + "exports",
                APP_DATA_DIR + File.separator + "backups",
                APP_DATA_DIR + File.separator + "profiles",
                APP_DATA_DIR + File.separator + "monitoring",
                APP_DATA_DIR + File.separator + "graphs",
                APP_DATA_DIR + File.separator + "temp"
        };

        for (String dir : dirs) {
            try {
                Files.createDirectories(Paths.get(dir));
            } catch (IOException e) {
                logger.warn("Failed to create directory: {}", dir, e);
            }
        }
    }

    public void load() {
        Path settingsPath = Paths.get(APP_DATA_DIR, SETTINGS_FILE);

        if (!Files.exists(settingsPath)) {
            logger.info("No settings file found, using defaults");
            save();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(settingsPath)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String encrypted = sb.toString();
            String decrypted = EncryptionUtils.decryptAndDecompress(encrypted);
            if (decrypted != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> loaded = gson.fromJson(decrypted, Map.class);
                if (loaded != null) {
                    for (Map.Entry<String, Object> entry : loaded.entrySet()) {
                        if (settings.containsKey(entry.getKey())) {
                            settings.put(entry.getKey(), entry.getValue());
                        } else {
                            logger.warn("Unknown setting in saved config: {}", entry.getKey());
                        }
                    }
                }
                logger.info("Settings loaded successfully");
            } else {
                logger.warn("Decryption failed, using defaults");
                save();
            }
        } catch (IOException e) {
            logger.error("Failed to load settings", e);
        }
    }

    public void save() {
        Path settingsPath = Paths.get(APP_DATA_DIR, SETTINGS_FILE);

        try {
            String json = gson.toJson(settings);
            String encrypted = EncryptionUtils.compressAndEncrypt(json);
            if (encrypted != null) {
                Files.write(settingsPath, encrypted.getBytes());
                logger.info("Settings saved successfully");
            } else {
                logger.error("Failed to encrypt settings");
            }
        } catch (IOException e) {
            logger.error("Failed to save settings", e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = settings.get(key);
        if (value == null) {
            return null;
        }

        if (type == Integer.class) {
            return type.cast(((Number) value).intValue());
        } else if (type == Double.class) {
            return type.cast(((Number) value).doubleValue());
        } else if (type == Boolean.class) {
            return type.cast(value);
        } else if (type == String.class) {
            return type.cast(value);
        } else if (type == Map.class) {
            return type.cast(value);
        } else if (type == java.util.List.class) {
            return type.cast(value);
        }

        return type.cast(value);
    }

    public void set(String key, Object value) {
        settings.put(key, value);
    }

    public String getString(String key) {
        return get(key, String.class);
    }

    public int getInt(String key) {
        return get(key, Integer.class);
    }

    public boolean getBoolean(String key) {
        return get(key, Boolean.class);
    }

    public double getDouble(String key) {
        return get(key, Double.class);
    }

    public Theme getTheme() {
        String themeStr = getString("theme");
        try {
            return Theme.valueOf(themeStr);
        } catch (IllegalArgumentException e) {
            return Theme.DARK;
        }
    }

    public ProcessingMode getProcessingMode() {
        String modeStr = getString("last_mode");
        try {
            return ProcessingMode.valueOf(modeStr);
        } catch (IllegalArgumentException e) {
            return ProcessingMode.LOCAL;
        }
    }

    public Language getDefaultLanguage() {
        String langCode = getString("default_lang");
        return Language.fromCode(langCode);
    }

    public SubtitleFormat getOutputFormat() {
        String formatStr = getString("output_format");
        try {
            return SubtitleFormat.valueOf(formatStr);
        } catch (IllegalArgumentException e) {
            return SubtitleFormat.SRT;
        }
    }

    public String getLanguage() {
        return getString("language");
    }

    public void setLanguage(String language) {
        set("language", language);
    }

    public static String getAppDataDir() {
        return APP_DATA_DIR;
    }

    public static String getKeyFilePath() {
        return APP_DATA_DIR + File.separator + KEY_FILE;
    }
}