package com.noty215.notycaption.ai;

import com.noty215.notycaption.subtitle.SubtitleEntry;
import com.noty215.notycaption.utils.ModelValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Whisper model wrapper for local transcription
 */
public class WhisperModel {

    private static final Logger logger = LoggerFactory.getLogger(WhisperModel.class);
    private static final String MODEL_NAME = "large-v1";

    private String modelPath;
    private boolean available;

    public WhisperModel(String modelsDir) {
        this.modelPath = modelsDir + File.separator + MODEL_NAME + ".pt";
        this.available = ModelValidator.validateModelFile(modelPath);

        if (available) {
            logger.info("Whisper model loaded: {}", modelPath);
        } else {
            logger.warn("Whisper model not available at: {}", modelPath);
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public List<SubtitleEntry> transcribe(File audioFile, String language, String task,
                                          int wordsPerLine, ProgressCallback callback) throws Exception {
        if (!available) {
            throw new RuntimeException("Whisper model not available");
        }

        logger.info("Starting transcription: language={}, task={}", language, task);

        if (callback != null) {
            callback.onProgress(10, "Loading model...");
        }

        // Use Python to run Whisper
        ProcessBuilder pb = new ProcessBuilder(
                "python", "-c",
                buildWhisperCommand(audioFile, language, task, wordsPerLine)
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        List<SubtitleEntry> subtitles = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            int segmentCount = 0;
            List<String> lines = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                logger.debug("Whisper: {}", line);

                if (line.startsWith("SEGMENT:")) {
                    // Parse segment line: SEGMENT:start,end,text
                    String[] parts = line.substring(8).split(",", 3);
                    if (parts.length >= 3) {
                        SubtitleEntry entry = new SubtitleEntry();
                        entry.setIndex(++segmentCount);
                        entry.setStart(Double.parseDouble(parts[0]));
                        entry.setEnd(Double.parseDouble(parts[1]));
                        entry.setText(parts[2]);
                        subtitles.add(entry);
                        lines.add(parts[2]);
                    }
                } else if (line.startsWith("PROGRESS:")) {
                    int progress = Integer.parseInt(line.substring(9));
                    if (callback != null) {
                        callback.onProgress(progress, "Processing...");
                    }
                }
            }
        }

        boolean finished = process.waitFor(10, TimeUnit.MINUTES);
        if (!finished) {
            process.destroy();
            throw new RuntimeException("Whisper timed out");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Whisper failed with exit code: " + exitCode);
        }

        if (callback != null) {
            callback.onProgress(100, "Complete");
        }

        logger.info("Transcription complete: {} segments", subtitles.size());
        return subtitles;
    }

    private String buildWhisperCommand(File audioFile, String language, String task, int wordsPerLine) {
        // Build Python command to run Whisper
        return
                "import whisper\n" +
                        "import sys\n" +
                        "import json\n" +
                        "import pysrt\n" +
                        "from datetime import timedelta\n" +
                        "\n" +
                        "model = whisper.load_model('large-v1')\n" +
                        "print('PROGRESS:10')\n" +
                        "sys.stdout.flush()\n" +
                        "\n" +
                        "result = model.transcribe('" + audioFile.getAbsolutePath().replace("\\", "/") + "', " +
                        "language='" + language + "', task='" + task + "', word_timestamps=True)\n" +
                        "print('PROGRESS:50')\n" +
                        "sys.stdout.flush()\n" +
                        "\n" +
                        "for seg in result['segments']:\n" +
                        "    words = seg.get('words', [])\n" +
                        "    if not words:\n" +
                        "        continue\n" +
                        "    for i in range(0, len(words), " + wordsPerLine + "):\n" +
                        "        chunk = words[i:i+" + wordsPerLine + "]\n" +
                        "        if not chunk:\n" +
                        "            continue\n" +
                        "        text = ' '.join([w['word'].strip() for w in chunk])\n" +
                        "        start = chunk[0]['start']\n" +
                        "        end = chunk[-1]['end']\n" +
                        "        print(f'SEGMENT:{start},{end},{text}')\n" +
                        "print('PROGRESS:100')\n";
    }

    public interface ProgressCallback {
        void onProgress(int progress, String message);
    }
}