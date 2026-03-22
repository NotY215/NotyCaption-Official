package com.noty215.notycaption.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.noty215.notycaption.models.SubtitleEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Session manager for saving and loading application state
 */
public class SessionManager {

    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);
    private static final String SESSION_FILE = "session.json";

    private final String sessionFilePath;
    private final Gson gson;
    private final ReentrantLock lock;

    public SessionManager() {
        this.sessionFilePath = SettingsManager.getAppDataDir() + File.separator + SESSION_FILE;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.lock = new ReentrantLock();
        logger.info("Session manager initialized with file: {}", sessionFilePath);
    }

    public boolean saveSession(Map<String, Object> sessionData) {
        lock.lock();
        try {
            sessionData.put("last_saved", LocalDateTime.now().toString());
            sessionData.put("app_version", "2026.5.0");

            String json = gson.toJson(sessionData);
            Files.write(Paths.get(sessionFilePath), json.getBytes());
            logger.info("Session saved successfully");
            return true;
        } catch (IOException e) {
            logger.error("Failed to save session", e);
            return false;
        } finally {
            lock.unlock();
        }
    }

    public Map<String, Object> loadSession() {
        lock.lock();
        try {
            Path path = Paths.get(sessionFilePath);
            if (Files.exists(path)) {
                String json = new String(Files.readAllBytes(path));
                Map<String, Object> sessionData = gson.fromJson(json, Map.class);
                logger.info("Session loaded successfully");
                return sessionData;
            } else {
                logger.info("No session file found");
                return null;
            }
        } catch (IOException e) {
            logger.error("Failed to load session", e);
            return null;
        } finally {
            lock.unlock();
        }
    }

    public boolean clearSession() {
        lock.lock();
        try {
            Path path = Paths.get(sessionFilePath);
            if (Files.exists(path)) {
                Files.delete(path);
                logger.info("Session cleared");
            }
            return true;
        } catch (IOException e) {
            logger.error("Failed to clear session", e);
            return false;
        } finally {
            lock.unlock();
        }
    }

    public void saveOperationState(String operationType, Map<String, Object> data) {
        Map<String, Object> session = loadSession();
        if (session == null) {
            session = new HashMap<>();
        }

        List<Map<String, Object>> operations = (List<Map<String, Object>>) session.getOrDefault("operations", new ArrayList<>());

        // Remove existing operation of same type
        operations.removeIf(op -> operationType.equals(op.get("type")));

        Map<String, Object> operation = new HashMap<>();
        operation.put("type", operationType);
        operation.put("data", data);
        operation.put("timestamp", LocalDateTime.now().toString());
        operations.add(operation);

        session.put("operations", operations);
        saveSession(session);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getOperationState(String operationType) {
        Map<String, Object> session = loadSession();
        if (session != null && session.containsKey("operations")) {
            List<Map<String, Object>> operations = (List<Map<String, Object>>) session.get("operations");
            for (Map<String, Object> op : operations) {
                if (operationType.equals(op.get("type"))) {
                    return (Map<String, Object>) op.get("data");
                }
            }
        }
        return null;
    }

    public void clearOperationState(String operationType) {
        Map<String, Object> session = loadSession();
        if (session != null && session.containsKey("operations")) {
            List<Map<String, Object>> operations = (List<Map<String, Object>>) session.get("operations");
            operations.removeIf(op -> operationType.equals(op.get("type")));
            session.put("operations", operations);
            saveSession(session);
        }
    }

    public void saveSubtitles(List<SubtitleEntry> subtitles, String filePath) {
        Map<String, Object> data = new HashMap<>();
        data.put("subtitles", subtitles);
        data.put("file_path", filePath);
        saveOperationState("subtitles", data);
    }

    @SuppressWarnings("unchecked")
    public List<SubtitleEntry> loadSubtitles() {
        Map<String, Object> data = getOperationState("subtitles");
        if (data != null && data.containsKey("subtitles")) {
            return (List<SubtitleEntry>) data.get("subtitles");
        }
        return null;
    }
}