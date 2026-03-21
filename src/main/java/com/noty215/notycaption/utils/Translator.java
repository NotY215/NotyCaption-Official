package com.noty215.notycaption.utils;

import java.util.HashMap;
import java.util.Map;

public class Translator {
    private String language;
    private Map<String, Map<String, String>> translations;
    private static Translator instance;
    
    private Translator() {
        this.language = "en";
        initializeTranslations();
    }
    
    public static Translator getInstance() {
        if (instance == null) {
            instance = new Translator();
        }
        return instance;
    }
    
    private void initializeTranslations() {
        translations = new HashMap<>();
        
        // English
        Map<String, String> en = new HashMap<>();
        en.put("window_title", "NotyCaption Pro - Professional AI Caption Generator");
        en.put("app_name", "NotyCaption Pro");
        en.put("app_subtitle", "AI-Powered Caption Generator");
        en.put("ready", "Ready");
        en.put("processing", "Processing...");
        en.put("canceled", "Canceled");
        en.put("completed", "Completed");
        en.put("failed", "Failed");
        en.put("edit_captions", "✏️ Edit Captions");
        en.put("save_exit_edit", "💾 Save & Exit Edit");
        en.put("settings", "⚙️ Settings");
        en.put("download_model", "📥 Download Model");
        en.put("login_google", "🔐 Google Login");
        en.put("import_media", "📁 Import Media");
        en.put("browse_output", "📂 Browse Output");
        en.put("enhance_audio", "🎤 Enhance Audio");
        en.put("play_pause", "▶️ Play / ⏸️ Pause");
        en.put("playing", "⏸️ Playing...");
        en.put("paused", "▶️ Play / ⏸️ Pause");
        en.put("generate", "🚀 Generate Captions");
        en.put("cancel", "Cancel Operation");
        en.put("force_cancel", "⚠️ Force Cancel");
        en.put("workspace", "🎨 Workspace");
        en.put("hardware", "🖥️ Hardware");
        en.put("monitor", "📊 Monitor");
        en.put("performance", "📈 Performance");
        en.put("export", "📤 Export");
        en.put("import", "📥 Import");
        en.put("preview", "👁️ Preview");
        en.put("refresh", "🔄 Refresh");
        en.put("processing_mode", "Mode:");
        en.put("language", "Language:");
        en.put("words_per_line", "Words/Line:");
        en.put("output_format", "Format:");
        en.put("output_folder", "Output Folder:");
        en.put("status", "Status:");
        en.put("idle", "Idle");
        en.put("speed", "Speed:");
        en.put("eta", "ETA:");
        en.put("downloading", "Downloading...");
        en.put("uploading", "Uploading...");
        en.put("waiting", "Waiting...");
        en.put("normal_mode", "🖥️ Local");
        en.put("online_mode", "☁️ Online");
        en.put("srt_format", "📄 SRT");
        en.put("ass_format", "🎨 ASS");
        en.put("settings_title", "Settings - Professional");
        en.put("general_tab", "General");
        en.put("paths_tab", "Paths");
        en.put("features_tab", "Features");
        en.put("advanced_tab", "Advanced");
        en.put("workspace_tab", "Workspace");
        en.put("presets_tab", "Presets");
        en.put("hardware_tab", "Hardware");
        en.put("visual_theme", "Theme");
        en.put("system_default", "System");
        en.put("light_mode", "Light");
        en.put("dark_mode", "Dark");
        en.put("ui_scaling", "UI Scale");
        en.put("scale_factor", "Scale:");
        en.put("temp_dir", "Temp Directory");
        en.put("browse_folder", "Browse");
        en.put("models_dir", "Models Directory");
        en.put("auto_features", "Auto Features");
        en.put("auto_enhance", "Auto-Enhance");
        en.put("default_language", "Default Language:");
        en.put("default_wpl", "Default Words/Line");
        en.put("words", "Words:");
        en.put("default_format", "Default Format");
        en.put("format", "Format:");
        en.put("cancel_options", "Cancel Options");
        en.put("confirm_cancel", "Confirm before cancel");
        en.put("force_cancel_timeout", "Force cancel after:");
        en.put("seconds", "s");
        en.put("max_retry", "Max retries:");
        en.put("ui_options", "UI Options");
        en.put("minimize_tray", "Minimize to tray");
        en.put("show_tooltips", "Show tooltips");
        en.put("ui_language", "Language:");
        en.put("apply_restart", "Apply & Restart");
        en.put("hardware_acceleration", "Hardware");
        en.put("gpu_info", "GPU: {}");
        en.put("cpu_info", "CPU: {}");
        en.put("memory_info", "RAM: {:.1f} GB");
        en.put("using_gpu", "GPU Mode");
        en.put("using_cpu", "CPU Mode");
        en.put("gpu_temp", "GPU Temp: {}°C");
        en.put("gpu_usage", "GPU Usage: {}%");
        en.put("cpu_usage", "CPU Usage: {}%");
        en.put("ram_usage", "RAM Usage: {:.1f}/{:.1f} GB ({:.0f}%)");
        en.put("hardware_monitor", "Hardware Monitor");
        en.put("cpu_tab", "CPU");
        en.put("gpu_tab", "GPU");
        en.put("ram_tab", "RAM");
        en.put("disk_tab", "Disk");
        en.put("network_tab", "Network");
        en.put("performance_tab", "Performance");
        en.put("start_monitoring", "Start Monitoring");
        en.put("stop_monitoring", "Stop Monitoring");
        en.put("export_data", "Export Data");
        en.put("import_data", "Import Data");
        en.put("clear_history", "Clear History");
        en.put("refresh_rate", "Refresh Rate:");
        en.put("monitor_manager", "Monitor Manager");
        en.put("detect_monitors", "Detect Monitors");
        en.put("move_to_monitor", "Move to Monitor");
        en.put("create_window", "Create Window");
        en.put("close_window", "Close Window");
        en.put("monitor", "Monitor {}");
        en.put("primary", "Primary");
        en.put("resolution", "Resolution: {}x{}");
        en.put("workspace_customize", "Customize Workspace");
        en.put("accent_color", "Accent Color");
        en.put("glow_intensity", "Glow Intensity");
        en.put("animation_speed", "Animation Speed");
        en.put("card_opacity", "Card Opacity");
        en.put("font_family", "Font Family");
        en.put("font_size", "Font Size");
        en.put("reset_defaults", "Reset to Defaults");
        en.put("save_layout", "Save Layout");
        en.put("load_layout", "Load Layout");
        en.put("layout_name", "Layout Name:");
        en.put("select_layout", "Select Layout:");
        en.put("preset_name", "Preset Name:");
        en.put("apply_preset", "Apply Preset");
        en.put("save_preset", "Save Preset");
        en.put("delete_preset", "Delete Preset");
        en.put("enable_animations", "Enable Animations");
        translations.put("en", en);
        
        // Add other languages as needed (Japanese, Russian, German, Hindi, Urdu, Arabic)
        // Similar structure for each language...
    }
    
    public String tr(String key) {
        Map<String, String> langMap = translations.getOrDefault(language, translations.get("en"));
        return langMap.getOrDefault(key, translations.get("en").getOrDefault(key, key));
    }
    
    public void setLanguage(String lang) {
        if (translations.containsKey(lang)) {
            this.language = lang;
        } else {
            this.language = "en";
        }
    }
    
    public String getLanguage() {
        return language;
    }
}