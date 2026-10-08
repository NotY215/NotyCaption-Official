// ========================================
// NotyCaption Pro - Configuration
// ========================================

const CONFIG = {
    CLIENT_ID: null,
    CLIENT_SECRET: null,
    AUTH_URI: null,
    TOKEN_URI: null,
    AUTH_PROVIDER_CERT_URL: null,
    PROJECT_ID: null,
    SCOPES: "https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email",
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

            // Google OAuth client files use the standard:
            // { "web": { ... } } structure.
            const webConfig = clientConfig.web;

            if (!webConfig || typeof webConfig !== "object") {
                throw new Error(
                    'client.json must contain a "web" OAuth configuration object.'
                );
            }

            // Only public/client-side configuration is copied into CONFIG.
            // client_secret is intentionally ignored because this file is
            // fetched by the browser and therefore cannot keep secrets private.
            Object.assign(CONFIG, {
                CLIENT_ID: webConfig.client_id || null,
                AUTH_URI: webConfig.auth_uri || null,
                TOKEN_URI: webConfig.token_uri || null,
                AUTH_PROVIDER_CERT_URL:
                    webConfig.auth_provider_x509_cert_url || null,
                PROJECT_ID: webConfig.project_id || null,
                REDIRECT_URI:
                    Array.isArray(webConfig.redirect_uris) &&
                    webConfig.redirect_uris.length
                        ? webConfig.redirect_uris[0]
                        : null
            });

            if (Array.isArray(webConfig.scopes) && webConfig.scopes.length) {
                CONFIG.SCOPES = webConfig.scopes.join(" ");
            }

            const required = [
                "CLIENT_ID",
                "AUTH_URI",
                "TOKEN_URI",
                "REDIRECT_URI"
            ];

            const missing = required.filter(
                key => !CONFIG[key] || typeof CONFIG[key] !== "string"
            );

            if (missing.length) {
                throw new Error(
                    `Missing required Google OAuth configuration: ${missing.join(", ")}`
                );
            }

            // Never expose or store the Google client secret in CONFIG.
            CONFIG.CLIENT_SECRET = null;

            console.log("client.json loaded");
            console.log("Google OAuth project:", CONFIG.PROJECT_ID || "unknown");
            console.log("Domain:", window.location.origin);
            console.log("Redirect URI:", CONFIG.REDIRECT_URI);

            return CONFIG;
        } catch (error) {
            console.error("Failed to load client.json:", error);
            throw error;
        }
    })();

    return configReady;
}

window.CONFIG = CONFIG;
window.STORAGE_KEYS = STORAGE_KEYS;
window.configReady = loadConfig();

console.log("config.js loaded");
