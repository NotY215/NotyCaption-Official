// ========================================
// NotyCaption Pro - Configuration
// ========================================

const CONFIG = {
    CLIENT_ID: null,
    CLIENT_SECRET: null,
    AUTH_URI: null,
    TOKEN_URI: null,
    SCOPES: null,
    REDIRECT_URI: null,

    MAX_FILE_SIZE: 50 * 1024 * 1024,
    TEMP_FOLDER_NAME: "NotyCaption_Temp",
    OUTPUT_FOLDER_NAME: "NotyCaption_Output",
    ENHANCED_FOLDER_NAME: "NotyCaption_Enhanced",
    NOTEBOOK_FOLDER_NAME: "NotyCaption_Notebooks"
};

const STORAGE_KEYS = {
    ACCESS_TOKEN: "notycaption_access_token",
    TOKEN_EXPIRY: "notycaption_token_expiry",
    USER_INFO: "notycaption_user_info",
    SETTINGS: "notycaption_settings"
};

let configReady = null;

async function loadConfig() {
    if (configReady) return configReady;

    configReady = (async () => {
        try {
            const response = await fetch("client.json", {
                method: "GET",
                cache: "no-store"
            });

            if (!response.ok) {
                throw new Error(`Unable to load client.json: HTTP ${response.status}`);
            }

            const clientConfig = await response.json();

            if (!clientConfig || typeof clientConfig !== "object") {
                throw new Error("client.json contains invalid configuration.");
            }

            Object.assign(CONFIG, clientConfig);

            const required = [
                "CLIENT_ID",
                "AUTH_URI",
                "TOKEN_URI",
                "SCOPES",
                "REDIRECT_URI"
            ];

            const missing = required.filter(
                key => !CONFIG[key] || typeof CONFIG[key] !== "string"
            );

            if (missing.length) {
                throw new Error(
                    `Missing required configuration: ${missing.join(", ")}`
                );
            }

            console.log("✅ client.json loaded");
            console.log("Domain:", window.location.origin);
            console.log("Redirect URI:", CONFIG.REDIRECT_URI);

            return CONFIG;
        } catch (error) {
            console.error("❌ Failed to load client.json:", error);
            throw error;
        }
    })();

    return configReady;
}

window.CONFIG = CONFIG;
window.STORAGE_KEYS = STORAGE_KEYS;
window.configReady = loadConfig();

console.log("✅ config.js loaded");
