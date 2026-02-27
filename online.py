# online.py
# Handles online mode: uploads audio → generates Colab notebook → polls for SRT/ASS

import os
import json
import shutil
from datetime import timedelta
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload, MediaIoBaseDownload
from io import FileIO
import pysrt
import pysubs2
import webbrowser
from PyQt5.QtWidgets import QMessageBox

def handle_online(self, audio_to_use, lang_code, task, wpl, fmt, base, out_path):
    if not self.service:
        QMessageBox.warning(self, "Error", "Please login with Google first for Online mode.")
        return False

    try:
        # 1. Upload audio
        uploads_folder_id = get_or_create_folder(self.service, "uploads")
        audio_filename = os.path.basename(audio_to_use)
        audio_id = upload_file(self.service, audio_to_use, audio_filename, uploads_folder_id)

        # 2. Generate notebook
        notebook_content = generate_notebook_content(
            audio_filename=audio_filename,
            lang_code=lang_code,
            task=task,
            wpl=wpl,
            fmt=fmt,
            output_name=f"{base}_captions{fmt}"
        )

        # Save notebook temporarily
        temp_dir = self.settings.get("temp_dir", os.path.join(os.path.dirname(__file__), "temp"))
        os.makedirs(temp_dir, exist_ok=True)
        temp_nb_path = os.path.join(temp_dir, "NotyCaption_Generator.ipynb")
        with open(temp_nb_path, "w", encoding="utf-8") as f:
            json.dump(notebook_content, f, ensure_ascii=False, indent=2)

        # 3. Upload notebook to Drive
        notebook_id = upload_file(self.service, temp_nb_path, "NotyCaption_Generator.ipynb")
        os.remove(temp_nb_path)  # clean local temp

        # 4. Open in browser
        colab_url = f"https://colab.research.google.com/drive/{notebook_id}"
        webbrowser.open(colab_url)

        QMessageBox.information(
            self, "Colab Ready",
            "Google Colab notebook has been opened in your browser.\n\n"
            "Please do one of the following:\n"
            "  • Click Runtime → Run all (fastest)\n"
            "  • Run cells one by one (safer if errors occur)\n\n"
            "The desktop app will automatically download the subtitle file once it's ready.\n"
            "Keep this window open."
        )

        # 5. Start polling
        self.poll_audio_id = audio_id
        self.poll_notebook_id = notebook_id
        self.poll_output_name = f"{base}_captions{fmt}"
        self.poll_local_out = out_path

        self.poll_timer.timeout.connect(lambda: poll_for_output(self))
        self.poll_timer.start(8000)  # check every 8 seconds

        return True

    except Exception as e:
        QMessageBox.critical(self, "Online Mode Failed", f"Setup error:\n{str(e)}")
        return False


def get_or_create_folder(service, name):
    """Find or create folder in root of My Drive"""
    query = f"name='{name}' and mimeType='application/vnd.google-apps.folder' and trashed=false"
    res = service.files().list(q=query, fields="files(id, name)").execute()
    files = res.get('files', [])
    for f in files:
        if f['name'] == name:
            return f['id']

    metadata = {'name': name, 'mimeType': 'application/vnd.google-apps.folder'}
    folder = service.files().create(body=metadata, fields='id').execute()
    return folder.get('id')


def upload_file(service, local_path, drive_name, parent_id=None):
    """Upload file to Drive"""
    metadata = {'name': drive_name}
    if parent_id:
        metadata['parents'] = [parent_id]

    media = MediaFileUpload(local_path, resumable=True)
    uploaded = service.files().create(body=metadata, media_body=media, fields='id').execute()
    return uploaded.get('id')


def generate_notebook_content(audio_filename, lang_code, task, wpl, fmt, output_name):
    cells = []

    # ── Cell 1: Install packages ─────────────────────────────────────
    cells.append({
        "cell_type": "code",
        "source": [
            "%%capture\n",
            "!pip install --quiet openai-whisper==20231117\n",
            "!pip install --quiet pysrt pysubs2\n",
            "print('Packages installed')"
        ]
    })

    # ── Cell 2: Mount Drive ──────────────────────────────────────────
    cells.append({
        "cell_type": "code",
        "source": [
            "from google.colab import drive\n",
            "drive.mount('/content/drive', force_remount=True)"
        ]
    })

    # ── Cell 3: Imports ──────────────────────────────────────────────
    cells.append({
        "cell_type": "code",
        "source": [
            "import whisper\n",
            "import pysrt\n",
            "import pysubs2\n",
            "from datetime import timedelta\n",
            "import os\n",
            "import shutil\n",
            "print('Imports done')"
        ]
    })

    # ── Cell 4: Model handling (persistent in Drive/SrtModels) ───────
    cells.append({
        "cell_type": "code",
        "source": [
            "model_name = 'large-v3'\n",
            "drive_model_folder = '/content/drive/My Drive/SrtModels'\n",
            "os.makedirs(drive_model_folder, exist_ok=True)\n",
            "drive_model_path = os.path.join(drive_model_folder, f'{model_name}.pt')\n",
            "local_model_path = f'/{model_name}.pt'\n\n",

            "if os.path.exists(drive_model_path):\n",
            "    print('Found model in Drive → copying to Colab...')\n",
            "    shutil.copyfile(drive_model_path, local_model_path)\n",
            "else:\n",
            "    print('Model not found in Drive → downloading... (first time only)')\n",
            "    model_temp = whisper.load_model(model_name)\n",
            "    print('Copying downloaded model to Drive for future use...')\n",
            "    shutil.copyfile(f'/root/.cache/whisper/{model_name}.pt', drive_model_path)\n",
            "    shutil.copyfile(f'/root/.cache/whisper/{model_name}.pt', local_model_path)\n",
            "    del model_temp  # free memory\n\n",

            "print('Loading model from local copy...')\n",
            "model = whisper.load_model(local_model_path)\n",
            "print('Model ready!')"
        ]
    })

    # ── Cell 5: Transcription ────────────────────────────────────────
    cells.append({
        "cell_type": "code",
        "source": [
            f"audio_path = '/content/drive/My Drive/uploads/{audio_filename}'\n",
            "print(f'Transcribing file: {audio_filename}')\n",
            f"result = model.transcribe(audio_path, language='{lang_code}', task='{task}', verbose=True, word_timestamps=True)\n",
            "print('Transcription finished')"
        ]
    })

    # ── Cell 6: Build subtitle lines ─────────────────────────────────
    cells.append({
        "cell_type": "code",
        "source": [
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
            "        w_e = w_s[1:] + [e]\n\n",
            f"    for i in range(0, len(w_txt), {wpl}):\n",
            "        chunk = w_txt[i:i + {wpl}]\n",
            "        line = ' '.join(chunk).strip()\n",
            "        if not line: continue\n",
            "        st = w_s[i]\n",
            "        en = w_e[min(i + {wpl} - 1, len(w_e) - 1)]\n",
            "        subtitles.append({\n",
            "            'index': idx,\n",
            "            'start': timedelta(seconds=st),\n",
            "            'end': timedelta(seconds=en),\n",
            "            'text': line\n",
            "        })\n",
            "        idx += 1\n",
            "print(f'Generated {len(subtitles)} subtitle lines')"
        ]
    })

    # ── Cell 7: Save final file to Drive ─────────────────────────────
    cells.append({
        "cell_type": "code",
        "source": [
            f"output_path = '/content/drive/My Drive/{output_name}'\n",
            f"fmt = '{fmt}'\n",
            "if fmt == '.srt':\n",
            "    srt = pysrt.SubRipFile()\n",
            "    for s in subtitles:\n",
            "        item = pysrt.SubRipItem(\n",
            "            index=s['index'],\n",
            "            start=pysrt.SubRipTime.from_ordinal(s['start'].total_seconds()*1000),\n",
            "            end=pysrt.SubRipTime.from_ordinal(s['end'].total_seconds()*1000),\n",
            "            text=s['text']\n",
            "        )\n",
            "        srt.append(item)\n",
            "    srt.save(output_path, encoding='utf-8')\n",
            "    print(f'SRT saved → {output_name}')\n",
            "else:\n",
            "    ass = pysubs2.SSAFile()\n",
            "    for s in subtitles:\n",
            "        ev = pysubs2.SSAEvent(\n",
            "            start=int(s['start'].total_seconds()*1000),\n",
            "            end=int(s['end'].total_seconds()*1000),\n",
            "            text=s['text']\n",
            "        )\n",
            "        ass.events.append(ev)\n",
            "    ass.save(output_path)\n",
            "    print(f'ASS saved → {output_name}')\n",
            "print('\\nDone! You can close this tab now.')"
        ]
    })

    return {
        "nbformat": 4,
        "nbformat_minor": 0,
        "metadata": {
            "colab": {"provenance": []},
            "kernelspec": {"name": "python3", "display_name": "Python 3"},
            "language_info": {"name": "python"}
        },
        "cells": cells
    }


def poll_for_output(self):
    if not hasattr(self, 'poll_output_name') or not self.poll_output_name:
        return

    query = f"name = '{self.poll_output_name}' and trashed = false"
    res = self.service.files().list(q=query, fields="files(id)").execute()
    files = res.get('files', [])

    if files:
        file_id = files[0]['id']

        # Download result
        request = self.service.files().get_media(fileId=file_id)
        with open(self.poll_local_out, 'wb') as fh:
            downloader = MediaIoBaseDownload(fh, request)
            done = False
            while not done:
                status, done = downloader.next_chunk()

        # Cleanup temporary files (audio + notebook + output — but NOT model)
        try:
            self.service.files().delete(fileId=self.poll_audio_id).execute()
            self.service.files().delete(fileId=self.poll_notebook_id).execute()
            self.service.files().delete(fileId=file_id).execute()
        except:
            pass

        self.poll_timer.stop()

        # Load subtitles into app
        ext = os.path.splitext(self.poll_output_name)[1].lower()
        if ext == '.srt':
            subs = pysrt.open(self.poll_local_out)
            self.subtitles = [{
                'index': item.index,
                'start': timedelta(seconds=item.start.ordinal / 1000.0),
                'end': timedelta(seconds=item.end.ordinal / 1000.0),
                'text': item.text
            } for item in subs]
            self.display_lines = [item.text for item in subs]
        else:  # .ass
            ass = pysubs2.load(self.poll_local_out)
            self.subtitles = [{
                'index': i+1,
                'start': timedelta(milliseconds=ev.start),
                'end': timedelta(milliseconds=ev.end),
                'text': ev.text
            } for i, ev in enumerate(ass.events)]
            self.display_lines = [ev.text for ev in ass.events]

        self.caption_edit.setText('\n'.join(self.display_lines).strip())
        self.generated = True
        self.edit_btn.setEnabled(True)

        QMessageBox.information(self, "Success",
                                f"Subtitles generated remotely!\nSaved locally to:\n{self.poll_local_out}")