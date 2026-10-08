# Architecture

NotyCaption Pro uses Google Drive and Google Colab for cloud-assisted processing.

## Processing flow

Website -> Google Drive -> Google Colab -> Google Drive result -> Website

1. Audio is uploaded to Google Drive.
2. A processing notebook is generated.
3. The notebook is opened in Google Colab.
4. Colab processes the audio.
5. The result is uploaded to Google Drive.
6. The website polls Drive for the matching operation.
7. The result is displayed automatically.
8. The Colab tab is closed after completion.

## Main components

- index.html: public entry page
- app.html: application interface
- drive.js: Google Drive operations
- ipynb.js: notebook generation
- colab.js: Colab lifecycle and result tracking
- audioManager.js: audio upload management
- caption.ipynb: Whisper workflow
- enhance.ipynb: vocal enhancement workflow

## Operation metadata

Results are identified using Google Drive appProperties:

- notycaption_operation_id
- notycaption_operation_type
- notycaption_status

The website does not rely on cross-origin sessionStorage communication with Colab.

> Placeholder: Add architecture diagrams and detailed module relationships here.
