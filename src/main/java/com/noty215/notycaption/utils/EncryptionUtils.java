package com.noty215.notycaption.utils;

import com.noty215.notycaption.App;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class EncryptionUtils {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    
    private static SecretKey key;
    private static java.util.logging.Logger logger = 
        java.util.logging.Logger.getLogger(EncryptionUtils.class.getName());
    
    static {
        try {
            key = loadOrCreateKey();
            logger.info("Encryption initialized successfully");
        } catch (Exception e) {
            logger.severe("Failed to initialize encryption: " + e.getMessage());
            // Generate fallback key
            try {
                KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
                keyGen.init(KEY_SIZE);
                key = keyGen.generateKey();
                logger.warning("Using fallback encryption key");
            } catch (Exception ex) {
                logger.severe("Critical: Cannot create fallback key: " + ex.getMessage());
            }
        }
    }
    
    public static SecretKey generateKeyFromPassword(String password, byte[] salt) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt);
        byte[] keyBytes = digest.digest(password.getBytes("UTF-8"));
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
    
    public static byte[] generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }
    
    public static SecretKey loadOrCreateKey() throws Exception {
        File keyFile = new File(App.KEY_FILE);
        
        // Check if running in packaged mode
        if (EncryptionUtils.class.getResource("/") != null && 
            EncryptionUtils.class.getResource("/").toString().contains("file:/")) {
            
            InputStream keyStream = EncryptionUtils.class.getResourceAsStream("/key.notcapz");
            if (keyStream != null) {
                byte[] keyData = keyStream.readAllBytes();
                logger.info("Bundled key loaded successfully");
                return new SecretKeySpec(keyData, ALGORITHM);
            } else {
                logger.warning("Bundled key not found, checking app data");
            }
        }
        
        if (keyFile.exists()) {
            byte[] keyData = Files.readAllBytes(keyFile.toPath());
            logger.info("Local key loaded");
            return new SecretKeySpec(keyData, ALGORITHM);
        }
        
        logger.info("No key found - generating new one");
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(KEY_SIZE);
        SecretKey newKey = keyGen.generateKey();
        Files.write(keyFile.toPath(), newKey.getEncoded());
        return newKey;
    }
    
    public static String encryptData(Object data) {
        try {
            // Serialize to JSON
            String json = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(data);
            
            // Compress
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
                gzipOut.write(json.getBytes("UTF-8"));
            }
            byte[] compressed = baos.toByteArray();
            
            // Encrypt
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new javax.crypto.spec.GCMParameterSpec(GCM_TAG_LENGTH, iv));
            
            byte[] encrypted = cipher.doFinal(compressed);
            
            // Combine IV and encrypted data
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            logger.severe("Encryption failed: " + e.getMessage());
            // Fallback to base64 encoding
            return Base64.getEncoder().encodeToString(new com.google.gson.Gson().toJson(data).getBytes());
        }
    }
    
    public static Object decryptData(String encryptedB64) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedB64);
            
            // Extract IV and encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
            
            // Decrypt
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new javax.crypto.spec.GCMParameterSpec(GCM_TAG_LENGTH, iv));
            
            byte[] decrypted = cipher.doFinal(encrypted);
            
            // Decompress
            try (ByteArrayInputStream bais = new ByteArrayInputStream(decrypted);
                 GZIPInputStream gzipIn = new GZIPInputStream(bais)) {
                String json = new String(gzipIn.readAllBytes(), "UTF-8");
                return new com.google.gson.Gson().fromJson(json, Object.class);
            }
        } catch (Exception e) {
            logger.severe("Decryption failed: " + e.getMessage());
            // Try fallback
            try {
                String json = new String(Base64.getDecoder().decode(encryptedB64));
                return new com.google.gson.Gson().fromJson(json, Object.class);
            } catch (Exception ex) {
                return null;
            }
        }
    }
    
    public static boolean saveSettings(java.util.Map<String, Object> settings) {
        try {
            String encrypted = encryptData(settings);
            Files.write(new File(App.SETTINGS_FILE).toPath(), encrypted.getBytes("UTF-8"));
            logger.info("Settings saved securely to " + App.SETTINGS_FILE);
            return true;
        } catch (Exception e) {
            logger.severe("Failed to save settings: " + e.getMessage());
            return false;
        }
    }
    
    @SuppressWarnings("unchecked")
    public static java.util.Map<String, Object> loadSettings() {
        java.util.Map<String, Object> defaults = SettingsManager.getDefaultSettings();
        File settingsFile = new File(App.SETTINGS_FILE);
        
        if (!settingsFile.exists()) {
            logger.info("No settings file found, using defaults");
            saveSettings(defaults);
            return defaults;
        }
        
        try {
            String encrypted = new String(Files.readAllBytes(settingsFile.toPath()), "UTF-8").trim();
            java.util.Map<String, Object> loaded = (java.util.Map<String, Object>) decryptData(encrypted);
            
            if (loaded != null) {
                // Merge with defaults
                defaults.putAll(loaded);
                logger.info("Settings loaded and merged with defaults");
                return defaults;
            } else {
                logger.warning("Decryption failed, saving defaults");
                saveSettings(defaults);
                return defaults;
            }
        } catch (Exception e) {
            logger.severe("Failed to load settings: " + e.getMessage());
            saveSettings(defaults);
            return defaults;
        }
    }
    
    public static java.util.Map<String, Object> loadClientSecrets() {
        // First check in resources (packaged)
        try {
            InputStream clientStream = EncryptionUtils.class.getResourceAsStream("/client.json");
            if (clientStream != null) {
                String json = new String(clientStream.readAllBytes(), "UTF-8");
                return new com.google.gson.Gson().fromJson(json, java.util.Map.class);
            }
        } catch (Exception e) {
            logger.warning("Failed to load client.json from resources: " + e.getMessage());
        }
        
        // Then check in app data
        File clientFile = new File(App.CLIENT_JSON);
        if (clientFile.exists()) {
            try {
                String json = new String(Files.readAllBytes(clientFile.toPath()), "UTF-8");
                return new com.google.gson.Gson().fromJson(json, java.util.Map.class);
            } catch (Exception e) {
                logger.warning("Failed to load client.json from app data: " + e.getMessage());
            }
        }
        
        logger.warning("No valid client secrets found");
        return null;
    }
}