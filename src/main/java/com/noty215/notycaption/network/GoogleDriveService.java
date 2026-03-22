package com.noty215.notycaption.network;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collections;
import java.util.List;

/**
 * Google Drive API service wrapper
 */
public class GoogleDriveService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDriveService.class);
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private Drive driveService;

    public boolean authenticate(InputStream clientSecretsStream) {
        try {
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(clientSecretsStream));

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                    .setAccessType("offline")
                    .build();

            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(0).build();
            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

            driveService = new Drive.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName("NotyCaption")
                    .build();

            logger.info("Google Drive authentication successful");
            return true;

        } catch (Exception e) {
            logger.error("Google Drive authentication failed", e);
            return false;
        }
    }

    public boolean isAuthenticated() {
        return driveService != null;
    }

    public List<File> listFiles(String query) throws Exception {
        FileList result = driveService.files().list()
                .setQ(query)
                .setFields("files(id, name, mimeType, size, createdTime)")
                .execute();
        return result.getFiles();
    }

    public String uploadFile(File file, String folderId) throws Exception {
        File fileMetadata = new File();
        fileMetadata.setName(file.getName());
        if (folderId != null) {
            fileMetadata.setParents(Collections.singletonList(folderId));
        }

        java.io.FileInputStream stream = new java.io.FileInputStream(file);
        Drive.Files.Create request = driveService.files().create(fileMetadata, new com.google.api.client.http.FileContent("application/octet-stream", file));
        File uploadedFile = request.execute();

        stream.close();
        logger.info("Uploaded file: {} ({})", uploadedFile.getName(), uploadedFile.getId());
        return uploadedFile.getId();
    }

    public void downloadFile(String fileId, File outputFile) throws Exception {
        try (OutputStream out = new FileOutputStream(outputFile)) {
            driveService.files().get(fileId).executeMediaAndDownloadTo(out);
        }
        logger.info("Downloaded file: {}", outputFile.getAbsolutePath());
    }

    public void deleteFile(String fileId) throws Exception {
        driveService.files().delete(fileId).execute();
        logger.info("Deleted file: {}", fileId);
    }

    public String createFolder(String folderName) throws Exception {
        File folderMetadata = new File();
        folderMetadata.setName(folderName);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");

        File folder = driveService.files().create(folderMetadata).setFields("id").execute();
        logger.info("Created folder: {} ({})", folderName, folder.getId());
        return folder.getId();
    }

    public String getOrCreateFolder(String folderName) throws Exception {
        String query = "name='" + folderName + "' and mimeType='application/vnd.google-apps.folder' and trashed=false";
        FileList result = driveService.files().list().setQ(query).setFields("files(id, name)").execute();
        List<File> files = result.getFiles();

        if (files != null && !files.isEmpty()) {
            return files.get(0).getId();
        }

        return createFolder(folderName);
    }

    public void shareFile(String fileId, String email) throws Exception {
        // Create permission
        com.google.api.services.drive.model.Permission permission = new com.google.api.services.drive.model.Permission();
        permission.setType("user");
        permission.setRole("reader");
        permission.setEmailAddress(email);

        driveService.permissions().create(fileId, permission).execute();
        logger.info("Shared file {} with {}", fileId, email);
    }

    public String getFileUrl(String fileId) {
        return "https://drive.google.com/file/d/" + fileId + "/view";
    }

    public void logout() {
        driveService = null;
        logger.info("Logged out from Google Drive");
    }
}