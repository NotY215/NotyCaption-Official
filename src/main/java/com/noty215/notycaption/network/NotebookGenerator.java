package com.noty215.notycaption.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.*;

public class NotebookGenerator {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    public String generateNotebook(String audioFileName, int wordsPerLine, String format, 
                                   String outputName, String langCode, String task) {
        Map<String, Object> notebook = new LinkedHashMap<>();
        notebook.put("nbformat", 4);
        notebook.put("nbformat_minor", 0);
        
        Map<String, Object> metadata = new HashMap<>();
        Map<String, Object> kernelspec = new HashMap<>();
        kernelspec.put("name", "python3");
        kernelspec.put("display_name", "Python 3");
        metadata.put("kernelspec", kernelspec);
        
        Map<String, Object> languageInfo = new HashMap<>();
        languageInfo.put("name", "python");
        metadata.put("language_info", languageInfo);
        metadata.put("accelerator", "GPU");
        notebook.put("metadata", metadata);
        
        List<Map<String, Object>> cells = new ArrayList<>();
        
        // Cell 1: Install dependencies
        cells.add(createCodeCell(new String[]{
            "%%capture",
            "!apt update -qq",
            "!apt install -y ffmpeg -qq",
            "!pip install -q openai-whisper",
            "!pip install -q pysrt pysubs2",
            "import os",
            "import shutil",
            "print('Dependencies installed successfully')"
        }));
        
        // Cell 2: Mount Google Drive
        cells.add(createCodeCell(new String[]{
            "from google.colab import drive",
            "drive.mount('/content/drive', force_remount=True)",
            "print('Drive mounted')"
        }));
        
        // Cell 3: Import libraries
        cells.add(createCodeCell(new String[]{
            "import whisper",
            "import pysrt",
            "import pysubs2",
            "from datetime import timedelta",
            "import os",
            "import time",
            "print('Libraries imported')"
        }));
        
        // Cell 4: Load model
        cells.add(createCodeCell(new String[]{
            "model_name = 'medium'",
            "print('Loading Whisper model...')",
            "model = whisper.load_model(model_name)",
            "print('Model loaded successfully')"
        }));
        
        // Cell 5: Verify audio file
        cells.add(createCodeCell(new String[]{
            "audio_path = f'/content/drive/My Drive/uploads/{audioFileName}'",
            "if not os.path.exists(audio_path):",
            "    raise FileNotFoundError(f'Audio file not found at {{audio_path}}. Ensure upload succeeded.')",
            "print(f'Audio file verified: {{audio_path}}')"
        }));
        
        // Cell 6: Transcribe
        cells.add(createCodeCell(new String[]{
            "result = model.transcribe(",
            "    audio_path,",
            f"    language='{langCode}',",
            f"    task='{task}',",
            "    word_timestamps=True",
            ")",
            "print('Transcription completed')"
        }));
        
        // Cell 7: Process words into subtitles
        cells.add(createCodeCell(new String[]{
            "subtitles = []",
            "idx = 1",
            "for seg in result['segments']:",
            "    words = seg.get('words', [])",
            "    if not words: continue",
            f"    for i in range(0, len(words), {wordsPerLine}):",
            f"        chunk = words[i:i+{wordsPerLine}]",
            "        if not chunk: continue",
            "        text = ' '.join([w['word'].strip() for w in chunk])",
            "        start = chunk[0]['start']",
            "        end = chunk[-1]['end']",
            "        subtitles.append((idx, start, end, text))",
            "        idx += 1",
            "print(f'Generated {len(subtitles)} subtitle lines')"
        }));
        
        // Cell 8: Save subtitles
        cells.add(createCodeCell(new String[]{
            String.format("fmt = '%s'", format),
            String.format("output_path = '/content/drive/My Drive/%s'", outputName),
            "if fmt == '.srt':",
            "    srt = pysrt.SubRipFile()",
            "    for idx, start, end, text in subtitles:",
            "        item = pysrt.SubRipItem(",
            "            index=idx,",
            "            start=pysrt.SubRipTime(milliseconds=int(start*1000)),",
            "            end=pysrt.SubRipTime(milliseconds=int(end*1000)),",
            "            text=text",
            "        )",
            "        srt.append(item)",
            "    srt.save(output_path, encoding='utf-8')",
            "    print(f'SRT saved: {output_path}')",
            "else:",
            "    ass = pysubs2.SSAFile()",
            "    style = pysubs2.SSAStyle()",
            "    ass.styles['Default'] = style",
            "    for idx, start, end, text in subtitles:",
            "        event = pysubs2.SSAEvent(",
            "            start=int(start*1000),",
            "            end=int(end*1000),",
            "            text=text",
            "        )",
            "        ass.events.append(event)",
            "    ass.save(output_path)",
            "    print(f'ASS saved: {output_path}')",
            "print('Processing complete - Download ready')"
        }));
        
        // Cell 9: Cleanup
        cells.add(createCodeCell(new String[]{
            "# Cleanup - Delete temporary files",
            "print('Cleaning up temporary files...')",
            "try:",
            "    if os.path.exists(audio_path):",
            "        os.remove(audio_path)",
            "        print(f'Deleted audio file: {audio_path}')",
            "    ",
            "    notebook_path = '/content/drive/My Drive/NotyCaption_Generator.ipynb'",
            "    if os.path.exists(notebook_path):",
            "        os.remove(notebook_path)",
            "        print(f'Deleted notebook: {notebook_path}')",
            "        ",
            "    print('Cleanup completed successfully')",
            "except Exception as e:",
            "    print(f'Cleanup error: {e}')",
            "    pass",
            "print('All temporary files have been deleted from Google Drive')"
        }));
        
        notebook.put("cells", cells);
        
        return gson.toJson(notebook);
    }
    
    private Map<String, Object> createCodeCell(String[] sourceLines) {
        Map<String, Object> cell = new LinkedHashMap<>();
        cell.put("cell_type", "code");
        cell.put("metadata", new HashMap<>());
        cell.put("execution_count", null);
        cell.put("outputs", new ArrayList<>());
        cell.put("source", sourceLines);
        return cell;
    }
    
    private Map<String, Object> createMarkdownCell(String[] sourceLines) {
        Map<String, Object> cell = new LinkedHashMap<>();
        cell.put("cell_type", "markdown");
        cell.put("metadata", new HashMap<>());
        cell.put("source", sourceLines);
        return cell;
    }
}