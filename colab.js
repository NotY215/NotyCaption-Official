// ========================================
// NotyCaption Pro - Colab Processing Handler
// ========================================

let colabWindow = null;
let pollInterval = null;

async function openNotebookInColab(notebookDriveId) {
    const colabUrl = `https://colab.research.google.com/drive/${notebookDriveId}`;
    console.log(`📓 Opening Colab notebook: ${colabUrl}`);
    colabWindow = window.open(colabUrl, '_blank');
    return colabWindow;
}

function startPolling(operationId, operationType, onSuccess, onError) {
    let attempts = 0;
    const maxAttempts = 180; // 15 minutes max (5 seconds * 180)
    
    if (pollInterval) clearInterval(pollInterval);
    
    console.log(`📡 Starting polling for operation: ${operationId}`);
    
    pollInterval = setInterval(async () => {
        attempts++;
        
        // Check sessionStorage for result
        const resultKey = `colab_result_${operationId}`;
        const result = sessionStorage.getItem(resultKey);
        
        if (result) {
            console.log(`📥 Result received for operation ${operationId}`);
            clearInterval(pollInterval);
            sessionStorage.removeItem(resultKey);
            
            try {
                const data = JSON.parse(result);
                if (data.success) {
                    if (onSuccess) await onSuccess(data.file_id, data.file_name);
                    
                    // Clean up notebook file from Drive
                    const opData = sessionStorage.getItem(`colab_op_${operationId}`);
                    if (opData) {
                        const opDataObj = JSON.parse(opData);
                        if (opDataObj.notebookDriveId && typeof deleteDriveFile === 'function') {
                            await deleteDriveFile(opDataObj.notebookDriveId);
                            console.log(`🗑️ Deleted notebook: ${opDataObj.notebookDriveId}`);
                        }
                        sessionStorage.removeItem(`colab_op_${operationId}`);
                    }
                    
                    // Close Colab tab if still open
                    if (colabWindow && !colabWindow.closed) {
                        colabWindow.close();
                    }
                } else {
                    if (onError) onError(data.error || 'Processing failed');
                }
            } catch(e) {
                console.error('Error parsing result:', e);
                if (onError) onError('Error processing result');
            }
            pollInterval = null;
            return;
        }
        
        if (attempts >= maxAttempts) {
            console.error(`⏰ Timeout for operation ${operationId}`);
            clearInterval(pollInterval);
            if (onError) onError('Timeout! Please check Colab for results.');
            pollInterval = null;
        } else if (attempts % 12 === 0) { // Log every minute
            console.log(`⏳ Waiting for results... (${attempts * 5} seconds elapsed)`);
        }
    }, 5000);
}

async function createAndOpenColabNotebook(operationType, params, onSuccess, onError) {
    console.log('createAndOpenColabNotebook called with:', operationType, params);
    
    // Check if getNotebookContent exists
    if (typeof getNotebookContent === 'undefined') {
        console.error('getNotebookContent is undefined. ipynb.js may not be loaded.');
        if (onError) onError('ipynb.js not loaded properly. Please refresh the page.');
        return null;
    }
    
    const operationId = Date.now().toString() + '_' + Math.random().toString(36).substr(2, 8);
    console.log(`🚀 Creating ${operationType} notebook with ID: ${operationId}`);
    
    try {
        // Get notebook content from ipynb.js
        const notebookJSONString = getNotebookContent(operationType, {
            audioId: params.audioId,
            language: params.language || 'en',
            wordsPerLine: params.wordsPerLine || '5',
            outputFormat: params.outputFormat || 'srt',
            operationId: operationId
        });
        
        // Validate notebook content
        try {
            JSON.parse(notebookJSONString);
            console.log('✅ Notebook JSON is valid');
        } catch (e) {
            console.error('Invalid notebook JSON:', e);
            throw new Error('Failed to create notebook: Invalid JSON');
        }
        
        const notebookName = `NotyCaption_${operationType}_${operationId}.ipynb`;
        console.log(`📝 Creating notebook: ${notebookName}`);
        
        // Upload notebook to Drive
        const notebookDriveId = await uploadNotebookToDrive(notebookJSONString, notebookName);
        console.log(`✅ Notebook uploaded to Drive: ${notebookDriveId}`);
        
        // Store operation data
        sessionStorage.setItem(`colab_op_${operationId}`, JSON.stringify({
            ...params,
            operationId,
            operationType,
            notebookDriveId,
            timestamp: Date.now()
        }));
        
        // Open in Colab
        await openNotebookInColab(notebookDriveId);
        
        // Start polling for results
        startPolling(operationId, operationType, onSuccess, onError);
        
        return operationId;
    } catch (error) {
        console.error('❌ Failed to create notebook:', error);
        if (onError) onError(error.message);
        return null;
    }
}

// Export functions
window.createAndOpenColabNotebook = createAndOpenColabNotebook;
window.openNotebookInColab = openNotebookInColab;
window.startPolling = startPolling;

console.log('✅ colab.js loaded successfully');