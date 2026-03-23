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
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.noty215.notycaption.utils.ResourcePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles online mode with Google Drive and Colab
 */
public class OnlineHandler {

    private static final Logger logger = LoggerFactory.getLogger(OnlineHandler.class);
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private Drive driveService;
    private String currentNotebookUrl;
    private AtomicBoolean canceled;

    public OnlineHandler() {
        this.canceled = new AtomicBoolean(false);
    }

    public boolean login() {
        try {
            InputStream in = new FileInputStream(ResourcePath.getFile("client.json"));
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                    .setAccessType("offline")
                    .build();

            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(0).build();
            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

            driveService = new Drive.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName("NotyCaption")
                    .build();

            logger.info("Google Drive login successful");
            return true;

        } catch (Exception e) {
            logger.error("Google Drive login failed", e);
            return false;
        }
    }

    public boolean isLoggedIn() {
        return driveService != null;
    }

    public String getNotebookUrl() {
        return currentNotebookUrl;
    }

    public void generateCaptions(java.io.File audioFile, String languageCode, String task,
                                 int wordsPerLine, String formatExt, java.io.File outputFile,
                                 GenerationCallback callback) {
        canceled.set(false);

        new Thread(() -> {
            try {
                if (driveService == null) {
                    throw new RuntimeException("Not logged in");
                }

                callback.onProgress(10, "Uploading audio...");
                String audioFileId = uploadFile(audioFile, "uploads");
                if (canceled.get()) {
                    cleanup(audioFileId, null);
                    callback.onCanceled();
                    return;
                }

                callback.onProgress(30, "Generating notebook...");
                String notebookId = createNotebook(audioFile.getName(), languageCode, task, wordsPerLine, formatExt);
                if (canceled.get()) {
                    cleanup(audioFileId, notebookId);
                    callback.onCanceled();
                    return;
                }

                currentNotebookUrl = "https://colab.research.google.com/drive/" + notebookId;
                callback.onNotebookUrl(currentNotebookUrl);

                callback.onProgress(50, "Waiting for Colab...");
                String outputFileId = pollForOutput(outputFile.getName());
                if (canceled.get()) {
                    cleanup(audioFileId, notebookId);
                    callback.onCanceled();
                    return;
                }

                callback.onProgress(80, "Downloading result...");
                downloadFile(outputFileId, outputFile);
                if (canceled.get()) {
                    cleanup(audioFileId, notebookId);
                    callback.onCanceled();
                    return;
                }

                cleanup(audioFileId, notebookId);

                callback.onProgress(100, "Complete");
                callback.onComplete(outputFile);

            } catch (Exception e) {
                logger.error("Online generation failed", e);
                callback.onError(e);
            }
        }).start();
    }

    private String uploadFile(java.io.File localFile, String folderName) throws Exception {
        String folderId = getOrCreateFolder(folderName);

        File fileMetadata = new File();
        fileMetadata.setName(localFile.getName());
        fileMetadata.setParents(Collections.singletonList(folderId));

        FileContent mediaContent = new FileContent("audio/wav", localFile);
        File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();

        logger.info("Uploaded file: {}", uploadedFile.getId());
        return uploadedFile.getId();
    }

    private String getOrCreateFolder(String folderName) throws Exception {
        String query = "name='" + folderName + "' and mimeType='application/vnd.google-apps.folder' and trashed=false";
        FileList result = driveService.files().list().setQ(query).setFields("files(id, name)").execute();
        List<File> files = result.getFiles();

        if (files != null && !files.isEmpty()) {
            return files.get(0).getId();
        }

        File folderMetadata = new File();
        folderMetadata.setName(folderName);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");

        File folder = driveService.files().create(folderMetadata).setFields("id").execute();
        logger.info("Created folder: {}", folder.getId());
        return folder.getId();
    }

    private String createNotebook(String audioFileName, String languageCode, String task,
                                  int wordsPerLine, String formatExt) throws Exception {
        String notebookContent = generateNotebookContent(audioFileName, languageCode, task, wordsPerLine, formatExt);

        Path tempNotebook = Files.createTempFile("notebook_", ".ipynb");
        Files.write(tempNotebook, notebookContent.getBytes());

        String folderId = getOrCreateFolder("notebooks");
        File fileMetadata = new File();
        fileMetadata.setName("NotyCaption_Generator.ipynb");
        fileMetadata.setParents(Collections.singletonList(folderId));

        FileContent mediaContent = new FileContent("application/json", tempNotebook.toFile());
        File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();

        Files.delete(tempNotebook);

        logger.info("Created notebook: {}", uploadedFile.getId());
        return uploadedFile.getId();
    }

    private String generateNotebookContent(String audioFileName, String languageCode, String task,
                                           int wordsPerLine, String formatExt) {
        return "{\n" +
                "  \"nbformat\": 4,\n" +
                "  \"nbformat_minor\": 0,\n" +
                "  \"metadata\": {\n" +
                "    \"kernelspec\": {\"name\": \"python3\", \"display_name\": \"Python 3\"},\n" +
                "    \"language_info\": {\"name\": \"python\"},\n" +
                "    \"accelerator\": \"GPU\"\n" +
                "  },\n" +
                "  \"cells\": [\n" +
                "    {\"cell_type\": \"code\", \"metadata\": {}, \"source\": [\"!pip install -q openai-whisper pysrt pysubs2\\n\", \"import whisper\\n\"]},\n" +
                "    {\"cell_type\": \"code\", \"metadata\": {}, \"source\": [\"from google.colab import drive\\n\", \"drive.mount('/content/drive')\\n\"]},\n" +
                "    {\"cell_type\": \"code\", \"metadata\": {}, \"source\": [\"model = whisper.load_model('large-v3')\\n\"]},\n" +
                "    {\"cell_type\": \"code\", \"metadata\": {}, \"source\": [\"result = model.transcribe('/content/drive/MyDrive/uploads/" + audioFileName + "', language='" + languageCode + "', task='" + task + "')\\n\"]},\n" +
                "    {\"cell_type\": \"code\", \"metadata\": {}, \"source\": [\"import pysrt\\n\", \"from datetime import timedelta\\n\", \"subtitles = []\\n\", \"idx = 1\\n\", \"for seg in result['segments']:\\n\", \"    words = seg.get('words', [])\\n\", \"    for i in range(0, len(words), " + wordsPerLine + "):\\n\", \"        chunk = words[i:i+" + wordsPerLine + "]\\n\", \"        text = ' '.join([w['word'].strip() for w in chunk])\\n\", \"        start = chunk[0]['start']\\n\", \"        end = chunk[-1]['end']\\n\", \"        subtitles.append((idx, start, end, text))\\n\", \"        idx += 1\\n\", \"output_path = '/content/drive/MyDrive/captions" + formatExt + "'\\n\", \"srt = pysrt.SubRipFile()\\n\", \"for idx, start, end, text in subtitles:\\n\", \"    item = pysrt.SubRipItem(index=idx, start=pysrt.SubRipTime(milliseconds=int(start*1000)), end=pysrt.SubRipTime(milliseconds=int(end*1000)), text=text)\\n\", \"    srt.append(item)\\n\", \"srt.save(output_path)\\n\", \"print('Done')\\n\"]}\n" +
                "  ]\n" +
                "}";
    }

    private String pollForOutput(String outputFileName) throws Exception {
        int attempts = 0;
        int maxAttempts = 180;

        while (attempts < maxAttempts && !canceled.get()) {
            String query = "name='" + outputFileName + "' and trashed=false";
            FileList result = driveService.files().list().setQ(query).setFields("files(id, name)").execute();
            List<File> files = result.getFiles();

            if (files != null && !files.isEmpty()) {
                return files.get(0).getId();
            }

            Thread.sleep(1000);
            attempts++;
        }

        throw new RuntimeException("Timeout waiting for output");
    }

    private void downloadFile(String fileId, java.io.File outputFile) throws Exception {
        try (OutputStream out = new FileOutputStream(outputFile)) {
            driveService.files().get(fileId).executeMediaAndDownloadTo(out);
        }
        logger.info("Downloaded file: {}", outputFile.getAbsolutePath());
    }

    private void cleanup(String audioFileId, String notebookId) {
        try {
            if (audioFileId != null) {
                driveService.files().delete(audioFileId).execute();
                logger.info("Deleted audio file");
            }
            if (notebookId != null) {
                driveService.files().delete(notebookId).execute();
                logger.info("Deleted notebook");
            }
        } catch (Exception e) {
            logger.warn("Cleanup failed", e);
        }
    }

    public void cancel() {
        canceled.set(true);
    }

    public interface GenerationCallback {
        void onProgress(int progress, String message);
        void onNotebookUrl(String url);
        void onComplete(java.io.File outputFile);
        void onError(Exception e);
        void onCanceled();
    }
}