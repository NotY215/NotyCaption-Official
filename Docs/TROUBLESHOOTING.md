# Troubleshooting

## Colab does not open

Allow popups for the NotyCaption website.

## Processing does not complete

Verify that:

- The Colab notebook was executed.
- The notebook completed without an exception.
- Google Drive authentication succeeded.
- The application page remains open.
- The result was uploaded successfully.

## Captions are not displayed

Check the browser console for Drive API errors and verify the result file has the expected operation metadata.

## Progress appears stuck

The progress bar represents processing stages rather than an exact AI percentage. The website does not receive exact Whisper or Spleeter progress information.

> Placeholder: Add known error messages and fixes here.
