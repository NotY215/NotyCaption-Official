package com.noty215.notycaption.audio;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class SpleeterWrapper {
    private static final Logger logger = Logger.getLogger(SpleeterWrapper.class.getName());
    private static boolean available = false;
    private static String pythonPath = "python";
    private static String spleeterModule = "spleeter";
    
    static {
        checkAvailability();
    }
    
    private static void checkAvailability() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{pythonPath, "-c", "import spleeter; print('OK')"});
            int exitCode = process.waitFor();
            available = (exitCode == 0);
            logger.info("Spleeter " + (available ? "available" : "not available"));
        } catch (Exception e) {
            logger.warning("Spleeter not found: " + e.getMessage());
            available = false;
        }
    }
    
    public static boolean isAvailable() {
        return available;
    }
    
    public static void setPythonPath(String path) {
        pythonPath = path;
        checkAvailability();
    }
    
    public static SeparationResult separate(File audioFile, File outputDir, String stems) throws Exception {
        return separate(audioFile, outputDir, stems, null);
    }
    
    public static SeparationResult separate(File audioFile, File outputDir, String stems, 
                                           Consumer<ProgressInfo> progressCallback) throws Exception {
        if (!available) {
            throw new Exception("Spleeter not available");
        }
        
        logger.info("Starting Spleeter separation: " + audioFile.getAbsolutePath());
        
        List<String> command = new ArrayList<>();
        command.add(pythonPath);
        command.add("-m");
        command.add(spleeterModule);
        command.add("separate");
        command.add("-i");
        command.add(audioFile.getAbsolutePath());
        command.add("-o");
        command.add(outputDir.getAbsolutePath());
        command.add("-p");
        command.add("spleeter:" + stems);
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        
        // Read output in a separate thread
        Thread outputReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.fine("Spleeter: " + line);
                    if (progressCallback != null && line.contains("%")) {
                        // Parse progress from output
                        int percent = parseProgress(line);
                        if (percent >= 0) {
                            progressCallback.accept(new ProgressInfo(percent, line));
                        }
                    }
                }
            } catch (Exception e) {
                logger.warning("Error reading Spleeter output: " + e.getMessage());
            }
        });
        outputReader.start();
        
        int exitCode = process.waitFor();
        outputReader.join();
        
        SeparationResult result = new SeparationResult();
        result.setSuccess(exitCode == 0);
        
        if (exitCode == 0) {
            String baseName = audioFile.getName().replaceFirst("[.][^.]+$", "");
            File vocalsFile = new File(outputDir, baseName + "/vocals.wav");
            if (!vocalsFile.exists()) {
                vocalsFile = new File(outputDir, "vocals.wav");
            }
            
            if (vocalsFile.exists()) {
                result.setVocalsFile(vocalsFile);
                result.setAccompanimentFile(new File(outputDir, baseName + "/accompaniment.wav"));
                logger.info("Separation completed: " + vocalsFile.getAbsolutePath());
            } else {
                result.setSuccess(false);
                result.setErrorMessage("Vocals file not found");
            }
        } else {
            result.setErrorMessage("Spleeter process exited with code: " + exitCode);
            logger.warning("Spleeter separation failed");
        }
        
        return result;
    }
    
    private static int parseProgress(String line) {
        // Parse progress from Spleeter output like "Progress: 45%"
        if (line.contains("%")) {
            try {
                int percentIndex = line.indexOf("%");
                for (int i = percentIndex - 1; i >= 0; i--) {
                    if (!Character.isDigit(line.charAt(i))) {
                        String percentStr = line.substring(i + 1, percentIndex);
                        return Integer.parseInt(percentStr.trim());
                    }
                }
            } catch (Exception e) {
                // Ignore parse errors
            }
        }
        return -1;
    }
    
    public static class SeparationResult {
        private boolean success;
        private String errorMessage;
        private File vocalsFile;
        private File accompanimentFile;
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public File getVocalsFile() { return vocalsFile; }
        public void setVocalsFile(File vocalsFile) { this.vocalsFile = vocalsFile; }
        
        public File getAccompanimentFile() { return accompanimentFile; }
        public void setAccompanimentFile(File accompanimentFile) { this.accompanimentFile = accompanimentFile; }
    }
    
    public static class ProgressInfo {
        private int percent;
        private String message;
        
        public ProgressInfo(int percent, String message) {
            this.percent = percent;
            this.message = message;
        }
        
        public int getPercent() { return percent; }
        public String getMessage() { return message; }
    }
}