package com.noty215.notycaption.utils;

import com.noty215.notycaption.App;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class SessionManager {
    private static final Logger logger = Logger.getLogger(SessionManager.class.getName());
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private File sessionFile;
    
    public SessionManager() {
        this.sessionFile = new File(App.SESSION_FILE);
        logger.info("Session manager initialized with file: " + sessionFile.getAbsolutePath());
    }
    
    public boolean saveSession(Map<String, Object> sessionData) {
        try (Writer writer = new FileWriter(sessionFile)) {
            sessionData.put("last_saved", new java.util.Date().toString());
            sessionData.put("app_version", App.VERSION);
            gson.toJson(sessionData, writer);
            logger.info("Session saved successfully");
            return true;
        } catch (IOException e) {
            logger.severe("Failed to save session: " + e.getMessage());
            return false;
        }
    }
    
    public Map<String, Object> loadSession() {
        if (!sessionFile.exists()) {
            logger.info("No session file found");
            return null;
        }
        
        try (Reader reader = new FileReader(sessionFile)) {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> sessionData = gson.fromJson(reader, type);
            logger.info("Session loaded successfully");
            return sessionData;
        } catch (IOException e) {
            logger.severe("Failed to load session: " + e.getMessage());
            return null;
        }
    }
    
    public boolean clearSession() {
        if (sessionFile.exists()) {
            if (sessionFile.delete()) {
                logger.info("Session cleared");
                return true;
            } else {
                logger.warning("Failed to clear session");
                return false;
            }
        }
        return true;
    }
    
    public boolean saveOperationState(String operationType, Map<String, Object> data) {
        Map<String, Object> session = loadSession();
        if (session == null) {
            session = new HashMap<>();
        }
        
        if (!session.containsKey("operations")) {
            session.put("operations", new java.util.ArrayList<Map<String, Object>>());
        }
        
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> operations = 
            (java.util.List<Map<String, Object>>) session.get("operations");
        
        // Remove existing operation of same type
        operations.removeIf(op -> operationType.equals(op.get("type")));
        
        Map<String, Object> operation = new HashMap<>();
        operation.put("type", operationType);
        operation.put("data", data);
        operation.put("timestamp", new java.util.Date().toString());
        operations.add(operation);
        
        return saveSession(session);
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> getOperationState(String operationType) {
        Map<String, Object> session = loadSession();
        if (session != null && session.containsKey("operations")) {
            java.util.List<Map<String, Object>> operations = 
                (java.util.List<Map<String, Object>>) session.get("operations");
            for (Map<String, Object> op : operations) {
                if (operationType.equals(op.get("type"))) {
                    return (Map<String, Object>) op.get("data");
                }
            }
        }
        return null;
    }
    
    public boolean clearOperationState(String operationType) {
        Map<String, Object> session = loadSession();
        if (session != null && session.containsKey("operations")) {
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> operations = 
                (java.util.List<Map<String, Object>>) session.get("operations");
            operations.removeIf(op -> operationType.equals(op.get("type")));
            return saveSession(session);
        }
        return true;
    }
}