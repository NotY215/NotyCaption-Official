package com.noty215.notycaption.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Generates Google Colab notebooks for online processing
 */
public class NotebookGenerator {

    private static final Logger logger = LoggerFactory.getLogger(NotebookGenerator.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static File generateNotebook(String audioFileName, String languageCode, String task,
                                        int wordsPerLine, String formatExt) throws IOException {
        Map<String, Object> notebook = new HashMap<>();
        notebook.put("nbformat", 4);
        notebook.put("nbformat_minor", 0);

        // Metadata
        Map<String, Object> metadata = new HashMap<>();
        Map<String, Object> kernelspec = new HashMap<>();
        kernelspec.put("name", "python3");
        kernelspec.put("display_name", "Python 3");
        metadata.put("kernelspec", kernelspec);

        Map<String, Object> languageInfo = new HashMap<>();
        languageInfo.put("name", "python");
        metadata.put("language_info", languageInfo);

        Map<String, Object> accelerator = new HashMap<>();
        accelerator.put("accelerator", "GPU");
        metadata.put("accelerator", accelerator);

        notebook.put("metadata", metadata);

        // Cells
        List<Map<String, Object>> cells = new ArrayList<>();

        // Cell 1: Install dependencies
        cells.add(createCodeCell(
                "%%capture\n" +
                        "!apt update -qq\n" +
                        "!apt install -y ffmpeg -qq\n" +
                        "!pip install -q openai-whisper\n" +
                        "!pip install -q pysrt pysubs2\n" +
                        "import os\n" +
                        "print('Dependencies installed')"
        ));

        // Cell 2: Mount Drive
        cells.add(createCodeCell(
                "from google.colab import drive\n" +
                        "drive.mount('/content/drive', force_remount=True)\n" +
                        "print('Drive mounted')"
        ));

        // Cell 3: Load model
        cells.add(createCodeCell(
                "import whisper\n" +
                        "print('Loading model...')\n" +
                        "model = whisper.load_model('large-v3')\n" +
                        "print('Model loaded')"
        ));

        // Cell 4: Transcribe
        cells.add(createCodeCell(
                "audio_path = '/content/drive/My Drive/uploads/" + audioFileName + "'\n" +
                        "result = model.transcribe(audio_path, language='" + languageCode + "', task='" + task + "', word_timestamps=True)\n" +
                        "print('Transcription complete')"
        ));

        // Cell 5: Generate subtitles
        cells.add(createCodeCell(
                "import pysrt\n" +
                        "import pysubs2\n" +
                        "from datetime import timedelta\n" +
                        "subtitles = []\n" +
                        "idx = 1\n" +
                        "for seg in result['segments']:\n" +
                        "    words = seg.get('words', [])\n" +
                        "    if not words: continue\n" +
                        "    for i in range(0, len(words), " + wordsPerLine + "):\n" +
                        "        chunk = words[i:i+" + wordsPerLine + "]\n" +
                        "        if not chunk: continue\n" +
                        "        text = ' '.join([w['word'].strip() for w in chunk])\n" +
                        "        start = chunk[0]['start']\n" +
                        "        end = chunk[-1]['end']\n" +
                        "        subtitles.append((idx, start, end, text))\n" +
                        "        idx += 1\n" +
                        "output_path = '/content/drive/My Drive/captions" + formatExt + "'\n" +
                        "if '" + formatExt + "' == '.srt':\n" +
                        "    srt = pysrt.SubRipFile()\n" +
                        "    for idx, start, end, text in subtitles:\n" +
                        "        item = pysrt.SubRipItem(index=idx, start=pysrt.SubRipTime(milliseconds=int(start*1000)), end=pysrt.SubRipTime(milliseconds=int(end*1000)), text=text)\n" +
                        "        srt.append(item)\n" +
                        "    srt.save(output_path)\n" +
                        "else:\n" +
                        "    ass = pysubs2.SSAFile()\n" +
                        "    for idx, start, end, text in subtitles:\n" +
                        "        event = pysubs2.SSAEvent(start=int(start*1000), end=int(end*1000), text=text)\n" +
                        "        ass.events.append(event)\n" +
                        "    ass.save(output_path)\n" +
                        "print('Captions saved')"
        ));

        // Cell 6: Cleanup
        cells.add(createCodeCell(
                "# Cleanup\n" +
                        "print('Cleaning up...')\n" +
                        "try:\n" +
                        "    if os.path.exists(audio_path):\n" +
                        "        os.remove(audio_path)\n" +
                        "        print(f'Deleted audio file')\n" +
                        "    notebook_path = '/content/drive/My Drive/NotyCaption_Generator.ipynb'\n" +
                        "    if os.path.exists(notebook_path):\n" +
                        "        os.remove(notebook_path)\n" +
                        "        print(f'Deleted notebook')\n" +
                        "    print('Cleanup complete')\n" +
                        "except Exception as e:\n" +
                        "    print(f'Cleanup error: {e}')"
        ));

        notebook.put("cells", cells);

        // Write to file
        File tempFile = File.createTempFile("notebook_", ".ipynb");
        try (FileWriter writer = new FileWriter(tempFile)) {
            gson.toJson(notebook, writer);
        }

        logger.info("Generated notebook: {}", tempFile.getAbsolutePath());
        return tempFile;
    }

    private static Map<String, Object> createCodeCell(String source) {
        Map<String, Object> cell = new HashMap<>();
        cell.put("cell_type", "code");
        cell.put("metadata", new HashMap<>());
        cell.put("execution_count", null);
        cell.put("outputs", new ArrayList<>());
        cell.put("source", Arrays.asList(source.split("\n")));
        return cell;
    }
}