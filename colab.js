// ========================================
// Colab Processing Handler
// ========================================

let colabWindow = null;
let pollInterval = null;

function getAccessToken() {
    let token = localStorage.getItem('notycaption_access_token');
    if (!token) {
        const cookies = document.cookie.split(';');
        for (const cookie of cookies) {
            const parts = cookie.trim().split('=');
            if (parts[0] === 'notycaption_access_token') {
                token = decodeURIComponent(parts.slice(1).join('='));
                localStorage.setItem('notycaption_access_token', token);
                break;
            }
        }
    }
    return token;
}

function setProcessingStatus(message, percent) {
    if (typeof window.showProgress === 'function') window.showProgress(true, message, percent);
}

async function findOperationResult(operationId, expectedName, startedAt) {
    const token = getAccessToken();
    if (!token) throw new Error('Authentication token not found');
    const headers = { 'Authorization': 'Bearer ' + token };
    const fields = 'files(id,name,mimeType,createdTime,appProperties)';
    const operationQuery = "trashed=false and appProperties has { key='notycaption_operation_id' and value='" + operationId + "' } and appProperties has { key='notycaption_status' and value='completed' }";
    const operationResponse = await fetch('https://www.googleapis.com/drive/v3/files?q=' + encodeURIComponent(operationQuery) + '&pageSize=10&orderBy=createdTime desc&fields=' + encodeURIComponent(fields), { headers });
    if (!operationResponse.ok) throw new Error('Drive status check failed: ' + operationResponse.status);
    const operationData = await operationResponse.json();
    if (operationData.files && operationData.files.length) return operationData.files[0];

    // Fallback for Drive indexing/metadata issues: match the expected output filename
    // but only accept files created after this operation started.
    if (expectedName && startedAt) {
        const safeName = expectedName.replace(/\\/g, '\\\\').replace(/'/g, "\\'");
        const startedIso = new Date(startedAt - 5000).toISOString();
        const fallbackQuery = "trashed=false and name='" + safeName + "' and createdTime > '" + startedIso + "'";
        const fallbackResponse = await fetch('https://www.googleapis.com/drive/v3/files?q=' + encodeURIComponent(fallbackQuery) + '&pageSize=10&orderBy=createdTime desc&fields=' + encodeURIComponent(fields), { headers });
        if (!fallbackResponse.ok) throw new Error('Drive fallback search failed: ' + fallbackResponse.status);
        const fallbackData = await fallbackResponse.json();
        if (fallbackData.files && fallbackData.files.length) return fallbackData.files[0];
    }
    return null;
}

async function deleteOperationNotebook(operationId) {
    try {
        const raw = sessionStorage.getItem('colab_op_' + operationId);
        if (!raw || typeof deleteDriveFile !== 'function') return;
        const op = JSON.parse(raw);
        if (op.notebookDriveId) await deleteDriveFile(op.notebookDriveId);
        sessionStorage.removeItem('colab_op_' + operationId);
    } catch (error) {
        console.warn('Notebook cleanup failed:', error);
    }
}

async function openNotebookInColab(notebookDriveId) {
    const colabUrl = 'https://colab.research.google.com/drive/' + notebookDriveId;
    console.log('Opening Colab notebook: ' + colabUrl);
    colabWindow = window.open(colabUrl, '_blank');
    if (!colabWindow) alert('Popup blocked. Please allow popups for this site to open Colab.');
    return colabWindow;
}

function stopPolling() {
    if (pollInterval) {
        clearInterval(pollInterval);
        pollInterval = null;
    }
}

function closeColabWindow() {
    if (colabWindow && !colabWindow.closed) {
        try { colabWindow.close(); } catch (error) { console.warn('Could not close Colab tab:', error); }
    }
    colabWindow = null;
}

function startPolling(operationId, operationType, onSuccess, onError, onProgress, tracking = {}) {
    let attempts = 0;
    let checking = false;
    const maxAttempts = 360;
    const pollEveryMs = 5000;
    const startedAt = tracking.startedAt || Date.now();

    stopPolling();
    setProcessingStatus(operationType === 'enhance' ? 'Waiting for vocal extraction...' : 'Waiting for Whisper processing...', 65);

    const check = async () => {
        if (checking) return;
        checking = true;
        attempts++;
        try {
            const result = await findOperationResult(operationId, tracking.expectedName, startedAt);
            if (result) {
                stopPolling();
                if (onProgress) onProgress('Result found. Loading output...', 95);
                await deleteOperationNotebook(operationId);
                if (onSuccess) await onSuccess(result.id, result.name);
                if (typeof window.showProgress === 'function') window.showProgress(true, 'Completed successfully!', 100);
                closeColabWindow();
                return;
            }

            const elapsed = attempts * 5;
            if (elapsed >= 30 && elapsed < 120) {
                setProcessingStatus(operationType === 'enhance' ? 'Spleeter is processing the audio...' : 'Whisper is transcribing the audio...', Math.min(75, 65 + Math.floor((elapsed - 30) / 10)));
            } else if (elapsed >= 120) {
                setProcessingStatus(operationType === 'enhance' ? 'Still processing vocal extraction...' : 'Still processing Whisper transcription...', Math.min(90, 75 + Math.floor((elapsed - 120) / 30)));
            }

            if (attempts >= maxAttempts) {
                stopPolling();
                if (onError) onError('Tracking timed out. The Colab job may still be running.');
            }
        } catch (error) {
            console.warn('Drive tracking check failed:', error);
            if (attempts >= maxAttempts) {
                stopPolling();
                if (onError) onError('Unable to track the Colab result: ' + error.message);
            }
        } finally {
            checking = false;
        }
    };

    check();
    pollInterval = setInterval(check, pollEveryMs);
}

async function createAndOpenColabNotebook(operationType, params, onSuccess, onError, onProgress) {
    if (typeof getNotebookContent === 'undefined') {
        if (onError) onError('ipynb.js not loaded properly. Please refresh the page.');
        return null;
    }

    const operationId = Date.now().toString() + '_' + Math.random().toString(36).substr(2, 8);
    const startedAt = Date.now();
    const audioBaseName = (params.audioName || 'captions').replace(/\\.(mp3|wav|m4a|flac|ogg|aac)$/i, '');
    const expectedName = audioBaseName + '.' + (params.outputFormat || 'srt');

    try {
        if (onProgress) onProgress('Preparing notebook...', 10);

        const result = getNotebookContent(operationType, {
            audioId: params.audioId,
            audioName: params.audioName || 'audio',
            language: params.language || 'en',
            wordsPerLine: params.wordsPerLine || '5',
            outputFormat: params.outputFormat || 'srt',
            operationId: operationId
        });

        let notebookJSONString, notebookName;
        if (typeof result === 'object' && result.content) {
            notebookJSONString = result.content;
            notebookName = result.notebookName;
        } else {
            notebookJSONString = result;
            notebookName = 'NotyCaption_' + operationType + '_' + operationId + '.ipynb';
        }

        if (!notebookJSONString) throw new Error('Failed to get notebook content');

        if (onProgress) onProgress('Uploading notebook to Google Drive...', 25);
        const notebookDriveId = await uploadNotebookToDrive(notebookJSONString, notebookName);

        sessionStorage.setItem('colab_op_' + operationId, JSON.stringify({
            ...params, operationId, operationType, notebookDriveId, notebookName, timestamp: Date.now()
        }));

        if (onProgress) onProgress('Opening Google Colab...', 40);
        await openNotebookInColab(notebookDriveId);

        if (onProgress) onProgress('Colab opened. Waiting for processing...', 60);
        startPolling(operationId, operationType, onSuccess, onError, onProgress, { expectedName, startedAt });
        return operationId;
    } catch (error) {
        console.error('Failed to create notebook:', error);
        if (onError) onError(error.message);
        return null;
    }
}

window.createAndOpenColabNotebook = createAndOpenColabNotebook;
window.openNotebookInColab = openNotebookInColab;
window.startPolling = startPolling;
window.stopPolling = stopPolling;

console.log('Colab module loaded with Drive-based background tracking');
