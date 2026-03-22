package com.noty215.notycaption.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encryption utilities for securing sensitive data
 */
public class EncryptionUtils {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionUtils.class);
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private static SecretKey masterKey;

    static {
        try {
            // Generate or load master key
            masterKey = loadOrCreateMasterKey();
        } catch (Exception e) {
            logger.error("Failed to initialize encryption", e);
            masterKey = generateFallbackKey();
        }
    }

    private static SecretKey loadOrCreateMasterKey() throws Exception {
        String keyFilePath = SettingsManager.getKeyFilePath();
        java.io.File keyFile = new java.io.File(keyFilePath);

        if (keyFile.exists()) {
            // Load existing key
            byte[] keyBytes = java.nio.file.Files.readAllBytes(keyFile.toPath());
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } else {
            // Create new key
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(256);
            SecretKey key = keyGen.generateKey();
            java.nio.file.Files.write(keyFile.toPath(), key.getEncoded());
            logger.info("Created new encryption key");
            return key;
        }
    }

    private static SecretKey generateFallbackKey() {
        try {
            byte[] keyBytes = new byte[32];
            new SecureRandom().nextBytes(keyBytes);
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (Exception e) {
            logger.error("Failed to generate fallback key", e);
            return null;
        }
    }

    public static String encrypt(String data) {
        if (data == null || masterKey == null) return null;

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            logger.error("Encryption failed", e);
            return null;
        }
    }

    public static String decrypt(String encryptedData) {
        if (encryptedData == null || masterKey == null) return null;

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, masterKey);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Decryption failed", e);
            return null;
        }
    }

    public static String compressAndEncrypt(String data) {
        try {
            // Compress with GZIP
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.util.zip.GZIPOutputStream gzipOut = new java.util.zip.GZIPOutputStream(baos);
            gzipOut.write(data.getBytes(StandardCharsets.UTF_8));
            gzipOut.close();
            byte[] compressed = baos.toByteArray();

            // Encrypt
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey);
            byte[] encrypted = cipher.doFinal(compressed);
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            logger.error("Compression and encryption failed", e);
            return null;
        }
    }

    public static String decryptAndDecompress(String encryptedData) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, masterKey);
            byte[] decrypted = cipher.doFinal(decoded);

            // Decompress
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(decrypted);
            java.util.zip.GZIPInputStream gzipIn = new java.util.zip.GZIPInputStream(bais);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = gzipIn.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            gzipIn.close();
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Decryption and decompression failed", e);
            return null;
        }
    }
}