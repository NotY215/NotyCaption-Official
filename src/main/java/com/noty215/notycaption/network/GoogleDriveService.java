package com.noty215.notycaption.network;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import javax.swing.*;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class GoogleDriveService {
    private static final Logger logger = Logger.getLogger(GoogleDriveService.class.getName());
    private static final String APPLICATION_NAME = "NotyCaption Pro";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);
    private static final String TOKENS_DIRECTORY_PATH = System.getProperty("user.home") + "/.notycaption/tokens";
    
    private Drive service;
    private Credential credential;
    
    public GoogleDriveService() {
        new File(TOKENS_DIRECTORY_PATH).mkdirs();
    }
    
    public void login(JFrame parent) {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            
            // Load client secrets
            InputStream in = getClass().getResourceAsStream("/client.json");
            if (in == null) {
                JOptionPane.showMessageDialog(parent, 
                    "Client secrets file not found. Please place client.json in the resources folder.",
                    "Missing Credentials",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
            
            // Build flow
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
            
            // Authorize
            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
            
            // Build service
            service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
            
            logger.info("Google Drive login successful");
            JOptionPane.showMessageDialog(parent, "Google Drive connected successfully!", "Login Success", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (Exception e) {
            logger.severe("Google Drive login failed: " + e.getMessage());
            JOptionPane.showMessageDialog(parent, 
                "Login failed: " + e.getMessage(),
                "Login Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public boolean loadExistingCredentials() {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH));
            
            credential = dataStoreFactory.getCredential("user");
            if (credential != null) {
                service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
                logger.info("Existing credentials loaded");
                return true;
            }
        } catch (Exception e) {
            logger.warning("Failed to load existing credentials: " + e.getMessage());
        }
        return false;
    }
    
    public boolean isLoggedIn() {
        return service != null && credential != null;
    }
    
    public String uploadFile(File file, String folderName) {
        if (!isLoggedIn()) return null;
        
        try {
            // Get or create folder
            String folderId = getOrCreateFolder(folderName);
            
            // Create file metadata
            File fileMetadata = new File();
            fileMetadata.setName(file.getName());
            fileMetadata.setParents(Collections.singletonList(folderId));
            
            // Upload file
            FileContent mediaContent = new FileContent("audio/wav", file);
            File uploadedFile = service.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();
            
            String fileId = uploadedFile.getId();
            logger.info("File uploaded: " + fileId);
            return fileId;
            
        } catch (Exception e) {
            logger.severe("Upload failed: " + e.getMessage());
            return null;
        }
    }
    
    public String uploadNotebook(String content) {
        if (!isLoggedIn()) return null;
        
        try {
            File fileMetadata = new File();
            fileMetadata.setName("NotyCaption_Generator.ipynb");
            fileMetadata.setMimeType("application/x-ipynb+json");
            
            java.io.File tempFile = java.io.File.createTempFile("notebook", ".ipynb");
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(content);
            }
            
            FileContent mediaContent = new FileContent("application/x-ipynb+json", tempFile);
            File uploadedFile = service.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();
            
            tempFile.delete();
            return uploadedFile.getId();
            
        } catch (Exception e) {
            logger.severe("Notebook upload failed: " + e.getMessage());
            return null;
        }
    }
    
    public File downloadFile(String fileId, File destination) {
        if (!isLoggedIn()) return null;
        
        try {
            try (OutputStream outputStream = new FileOutputStream(destination)) {
                service.files().get(fileId).executeMediaAndDownloadTo(outputStream);
            }
            logger.info("File downloaded: " + destination.getAbsolutePath());
            return destination;
        } catch (Exception e) {
            logger.severe("Download failed: " + e.getMessage());
            return null;
        }
    }
    
    public void deleteFile(String fileId) {
        if (!isLoggedIn()) return;
        
        try {
            service.files().delete(fileId).execute();
            logger.info("File deleted: " + fileId);
        } catch (Exception e) {
            logger.warning("Delete failed: " + e.getMessage());
        }
    }
    
    public String findFile(String fileName) {
        if (!isLoggedIn()) return null;
        
        try {
            FileList result = service.files().list()
                .setQ("name='" + fileName + "' and trashed=false")
                .setFields("files(id, name)")
                .execute();
            
            List<File> files = result.getFiles();
            if (files != null && !files.isEmpty()) {
                return files.get(0).getId();
            }
        } catch (Exception e) {
            logger.warning("File search failed: " + e.getMessage());
        }
        return null;
    }
    
    private String getOrCreateFolder(String folderName) throws Exception {
        // Search for existing folder
        FileList result = service.files().list()
            .setQ("name='" + folderName + "' and mimeType='application/vnd.google-apps.folder' and trashed=false")
            .setFields("files(id, name)")
            .execute();
        
        List<File> files = result.getFiles();
        if (files != null && !files.isEmpty()) {
            return files.get(0).getId();
        }
        
        // Create new folder
        File folderMetadata = new File();
        folderMetadata.setName(folderName);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");
        
        File folder = service.files().create(folderMetadata)
            .setFields("id")
            .execute();
        
        return folder.getId();
    }
    
    public void cleanupOldFiles(int daysOld) {
        if (!isLoggedIn()) return;
        
        try {
            long cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L);
            
            FileList result = service.files().list()
                .setQ("createdTime < '" + cutoffTime + "' and trashed=false")
                .setFields("files(id, name)")
                .execute();
            
            for (File file : result.getFiles()) {
                service.files().delete(file.getId()).execute();
                logger.info("Cleaned up old file: " + file.getName());
            }
        } catch (Exception e) {
            logger.warning("Cleanup failed: " + e.getMessage());
        }
    }
}