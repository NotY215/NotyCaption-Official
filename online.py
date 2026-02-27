# online.py
# Handles online mode for NotyCaption: Drive upload, notebook generation, polling

import os
import json
import shutil
from datetime import timedelta
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload, MediaIoBaseDownload
from io import FileIO
import pysrt
import pysubs2
from PyQt5.QtCore import QTimer
from PyQt5.QtWidgets import QMessageBox
import webbrowser

def handle_online(self, audio_to_use, lang_code, task, wpl, fmt, base, out_path):
    if not self.service:
        QMessageBox.warning(self, "Error", "Please login with Google first.")
        return False

    try:
        uploads_id = get_or_create_folder(self.service, "uploads")
        audio_filename = os.path.basename(audio_to_use)
        audio_id = upload_file(self.service, audio_to_use, audio_filename, uploads_id)

        notebook_content = generate_notebook_content(audio_filename, wpl, fmt, f"{base}_captions{fmt}")

        temp_dir = self.settings.get("temp_dir", os.path.join(os.path.dirname(__file__), "temp"))
        os.makedirs(temp_dir, exist_ok=True)
        temp_ipynb = os.path.join(temp_dir, "notycaption_generator.ipynb")
        with open(temp_ipynb, 'w', encoding='utf-8') as f:
            json.dump(notebook_content, f, ensure_ascii=False, indent=2)

        notebook_id = upload_file(self.service, temp_ipynb, "NotyCaption_Generator.ipynb")
        os.remove(temp_ipynb)

        colab_url = f"https://colab.research.google.com/drive/{notebook_id}"
        webbrowser.open(colab_url)

        QMessageBox.information(self, "Colab Notebook Opened",
                                "A Colab notebook has been opened in your browser.\n\n"
                                "**Important steps to avoid loading errors:**\n"
                                "1. Wait 45–90 seconds after the tab opens\n"
                                "2. If you see 'Cannot read properties of undefined (reading 'id')':\n"
                                "   → Press F5 to refresh the page\n"
                                "   → Or go to Runtime → Disconnect and delete runtime\n"
                                "   → Then Runtime → Run all (Ctrl+F9)\n"
                                "3. Let it run completely — app will auto-download the result\n\n"
                                "Tip: Using 'medium' model makes it faster and more stable.")

        self.poll_audio_id = audio_id
        self.poll_notebook_id = notebook_id
        self.poll_output_name = f"{base}_captions{fmt}"
        self.poll_local_out = out_path

        self.poll_timer.timeout.connect(lambda: poll_for_output(self))
        self.poll_timer.start(8000)  # Poll every 8 seconds

    except Exception as e:
        QMessageBox.critical(self, "Online Mode Setup Failed", f"Error during setup:\n{str(e)}")
        return False

    return True


def get_or_create_folder(service, name):
    query = f"name='{name}' and mimeType='application/vnd.google-apps.folder' and trashed=false"
    results = service.files().list(q=query, fields="files(id, name)").execute()
    files = results.get('files', [])
    for f in files:
        if f.get('name') == name:
            return f['id']
    metadata = {'name': name, 'mimeType': 'application/vnd.google-apps.folder'}
    file = service.files().create(body=metadata, fields='id').execute()
    return file.get('id')


def upload_file(service, filepath, filename, parent_id=None):
    metadata = {'name': filename}
    if parent_id:
        metadata['parents'] = [parent_id]
    media = MediaFileUpload(filepath, resumable=True)
    file = service.files().create(body=metadata, media_body=media, fields='id').execute()
    return file.get('id')


def generate_notebook_content(audio_filename, words_per_line, fmt, output_name):
    def code_cell(source_lines):
        return {
            "cell_type": "code",
            "metadata": {},
            "execution_count": None,
            "outputs": [],
            "source": source_lines
        }

    notebook = {
        "nbformat": 4,
        "nbformat_minor": 0,
        "metadata": {
            "colab": {"provenance": []},
            "kernelspec": {"name": "python3", "display_name": "Python 3"},
            "language_info": {"name": "python"}
        },
        "cells": [

            # Cell 1: Install required packages
            code_cell([
                "%%capture\n",
                "!pip install --quiet openai-whisper==20231117\n",
                "!pip install --quiet pysrt pysubs2\n",
                "print('Packages installed successfully')"
            ]),

            # Cell 2: Mount Google Drive
            code_cell([
                "from google.colab import drive\n",
                "drive.mount('/content/drive', force_remount=True)\n",
                "print('Google Drive mounted')"
            ]),

            # Cell 3: Imports
            code_cell([
                "import whisper\n",
                "import pysrt\n",
                "import pysubs2\n",
                "from datetime import timedelta\n",
                "import os\n",
                "import shutil\n",
                "print('All imports successful')"
            ]),

            # Cell 4: Model management - persist in Drive/SrtModels
            code_cell([
                "model_name = 'medium'  # Using medium for faster loading and stability\n",
                "drive_model_folder = '/content/drive/My Drive/SrtModels'\n",
                "os.makedirs(drive_model_folder, exist_ok=True)\n",
                "drive_model_path = os.path.join(drive_model_folder, f'{model_name}.pt')\n",
                "local_model_path = f'/content/{model_name}.pt'\n",
                "\n",
                "if os.path.exists(drive_model_path):\n",
                "    print('Found model in Drive → copying to Colab...')\n",
                "    shutil.copyfile(drive_model_path, local_model_path)\n",
                "else:\n",
                "    print('Model not found in Drive → downloading now... (may take 2–5 min)')\n",
                "    temp_model = whisper.load_model(model_name)\n",
                "    cache_path = f'/root/.cache/whisper/{model_name}.pt'\n",
                "    if os.path.exists(cache_path):\n",
                "        shutil.copyfile(cache_path, drive_model_path)\n",
                "        shutil.copyfile(cache_path, local_model_path)\n",
                "    else:\n",
                "        print('Warning: Model cache not found. Retrying download...')\n",
                "        temp_model = whisper.load_model(model_name)\n",
                "        shutil.copyfile(cache_path, drive_model_path)\n",
                "        shutil.copyfile(cache_path, local_model_path)\n",
                "    del temp_model\n",
                "    print('Model downloaded and saved to Drive for next time')\n",
                "\n",
                "print('Loading model from local copy...')\n",
                "model = whisper.load_model(local_model_path)\n",
                "print('Model loaded successfully!')"
            ]),

            # Cell 5: Transcription
            code_cell([
                f"audio_path = '/content/drive/My Drive/uploads/{audio_filename}'\n",
                "print(f'Starting transcription: {audio_filename}')\n",
                "result = model.transcribe(\n",
                "    audio_path,\n",
                "    language='en',\n",
                "    task='transcribe',\n",
                "    word_timestamps=True,\n",
                "    verbose=True\n",
                ")\n",
                "print('Transcription finished')"
            ]),

            # Cell 6: Subtitle generation
            code_cell([
                "subtitles = []\n",
                "idx = 1\n",
                "for seg in result.get('segments', []):\n",
                "    txt = seg.get('text', '').strip()\n",
                "    if not txt: continue\n",
                "    s = seg.get('start', 0)\n",
                "    e = seg.get('end', s + 1)\n",
                "    words = seg.get('words', [])\n",
                "    if words:\n",
                "        w_txt = [w['word'].strip() for w in words]\n",
                "        w_s = [w.get('start', s) for w in words]\n",
                "        w_e = [w.get('end', e) for w in words]\n",
                "    else:\n",
                "        w_txt = txt.split()\n",
                "        dur = e - s\n",
                "        w_s = [s + i * dur / max(1, len(w_txt)) for i in range(len(w_txt))]\n",
                "        w_e = w_s[1:] + [e]\n",
                f"    for i in range(0, len(w_txt), {words_per_line}):\n",
                "        chunk = w_txt[i:i + {words_per_line}]\n",
                "        line = ' '.join(chunk).strip()\n",
                "        if not line: continue\n",
                "        st = w_s[i]\n",
                "        en = w_e[min(i + {words_per_line} - 1, len(w_e) - 1)]\n",
                "        subtitles.append({\n",
                "            'index': idx,\n",
                "            'start': timedelta(seconds=st),\n",
                "            'end': timedelta(seconds=en),\n",
                "            'text': line\n",
                "        })\n",
                "        idx += 1\n",
                "print(f'Generated {len(subtitles)} subtitle lines')"
            ]),

            # Cell 7: Save SRT to Drive
            code_cell([
                f"output_path = '/content/drive/My Drive/{output_name}'\n",
                "srt = pysrt.SubRipFile()\n",
                "for s in subtitles:\n",
                "    item = pysrt.SubRipItem(\n",
                "        index=s['index'],\n",
                "        start=pysrt.SubRipTime(milliseconds=int(s['start'].total_seconds()*1000)),\n",
                "        end=pysrt.SubRipTime(milliseconds=int(s['end'].total_seconds()*1000)),\n",
                "        text=s['text']\n",
                "    )\n",
                "    srt.append(item)\n",
                "srt.save(output_path, encoding='utf-8')\n",
                "print(f'SRT file saved to: {output_name}')\n",
                "print('All done! You can close this tab. The desktop app will detect the file soon.')"
            ])
        ]
    }

    return notebook


def poll_for_output(self):
    if not hasattr(self, 'poll_output_name') or not self.poll_output_name:
        return

    query = f"name='{self.poll_output_name}' and trashed=false"
    results = self.service.files().list(q=query, fields="files(id, name)").execute()
    files = results.get('files', [])

    if files:
        file_id = files[0]['id']
        print(f"Found output file: {files[0]['name']} (ID: {file_id})")

        try:
            with open(self.poll_local_out, 'wb') as f:
                request = self.service.files().get_media(fileId=file_id)
                downloader = MediaIoBaseDownload(f, request)
                done = False
                while not done:
                    status, done = downloader.next_chunk()
                    if status:
                        print(f"Download {int(status.progress() * 100)}% complete")
        except Exception as download_err:
            print(f"Download failed: {str(download_err)}")
            return

        # Cleanup temporary files (audio + notebook + output)
        try:
            self.service.files().delete(fileId=self.poll_audio_id).execute()
            self.service.files().delete(fileId=self.poll_notebook_id).execute()
            self.service.files().delete(fileId=file_id).execute()
            print("Temporary files cleaned up from Drive")
        except Exception as cleanup_err:
            print(f"Cleanup failed (non-critical): {str(cleanup_err)}")

        self.poll_timer.stop()

        # Load SRT into app
        try:
            srt = pysrt.open(self.poll_local_out)
            self.subtitles = []
            self.display_lines = []
            for item in srt:
                self.subtitles.append({
                    "index": item.index,
                    "start": timedelta(seconds=item.start.ordinal / 1000.0),
                    "end": timedelta(seconds=item.end.ordinal / 1000.0),
                    "text": item.text
                })
                self.display_lines.append(item.text)

            self.caption_edit.setText("\n".join(self.display_lines).strip())
            self.generated = True
            self.edit_btn.setEnabled(True)

            QMessageBox.information(self, "Success",
                                    f"Subtitles generated remotely!\n"
                                    f"Downloaded and loaded:\n{self.poll_local_out}\n\n"
                                    f"Lines: {len(self.display_lines)}")
        except Exception as load_err:
            QMessageBox.warning(self, "Load Warning",
                                f"File downloaded but could not load SRT:\n{str(load_err)}\n\n"
                                "Check the file manually or try again.")