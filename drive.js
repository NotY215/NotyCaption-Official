// ========================================
// Google Drive Operations
// ========================================

let currentAudioDriveId = null;

async function ensureValidToken() {
    let token = localStorage.getItem('notycaption_access_token');
    if (!token) {
        const cookies = document.cookie.split(';');
        for (let cookie of cookies) {
            const [name, value] = cookie.trim().split('=');
            if (name === 'notycaption_access_token') {
                token = decodeURIComponent(value);
                localStorage.setItem('notycaption_access_token', token);
                break;
            }
        }
    }
    if (!token) {
        throw new Error('No access token available. Please login first.');
    }
    return token;
}

async function getOrCreateFolder(folderName) {
    const token = await ensureValidToken();
    
    const query = `name='${folderName}' and mimeType='application/vnd.google-apps.folder' and trashed=false`;
    const response = await fetch(`https://www.googleapis.com/drive/v3/files?q=${encodeURIComponent(query)}&fields=files(id,name)`, {
        headers: { 'Authorization': `Bearer ${token}` }
    });
    
    const data = await response.json();
    
    if (data.files && data.files.length > 0) {
        return data.files[0].id;
    }
    
    const metadata = {
        name: folderName,
        mimeType: 'application/vnd.google-apps.folder'
    };
    
    const createResponse = await fetch('https://www.googleapis.com/drive/v3/files', {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(metadata)
    });
    
    const folderData = await createResponse.json();
    return folderData.id;
}

async function uploadNotebookToDrive(notebookJSONString, notebookName) {
    const token = await ensureValidToken();
    
    const folderId = await getOrCreateFolder(CONFIG.NOTEBOOK_FOLDER_NAME);
    
    const metadata = {
        name: notebookName,
        mimeType: 'application/x-ipynb+json',
        parents: [folderId]
    };
    
    const formData = new FormData();
    formData.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));
    formData.append('file', new Blob([notebookJSONString], { type: 'application/json' }));
    
    const response = await fetch('https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` },
        body: formData
    });
    
    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Notebook upload failed: ${response.status} - ${errorText}`);
    }
    
    const data = await response.json();
    console.log(`✅ Notebook uploaded: ${notebookName} (ID: ${data.id})`);
    return data.id;
}

async function getFileContent(fileId) {
    const token = await ensureValidToken();
    
    const response = await fetch(`https://www.googleapis.com/drive/v3/files/${fileId}?alt=media`, {
        headers: { 'Authorization': `Bearer ${token}` }
    });
    
    if (!response.ok) {
        throw new Error(`Failed to get file content: ${response.status}`);
    }
    
    return await response.text();
}

async function getFileDownloadUrl(fileId) {
    const token = await ensureValidToken();
    return `https://www.googleapis.com/drive/v3/files/${fileId}?alt=media&access_token=${token}`;
}

async function deleteDriveFile(fileId) {
    const token = await ensureValidToken();
    
    try {
        await fetch(`https://www.googleapis.com/drive/v3/files/${fileId}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        console.log(`Deleted file: ${fileId}`);
    } catch (error) {
        console.error('Delete error:', error);
    }
}

async function cleanupOldNotebooks() {
    try {
        const folderId = await getOrCreateFolder(CONFIG.NOTEBOOK_FOLDER_NAME);
        const token = await ensureValidToken();
        const query = `'${folderId}' in parents and name contains 'NotyCaption_' and trashed=false`;
        const response = await fetch(`https://www.googleapis.com/drive/v3/files?q=${encodeURIComponent(query)}&fields=files(id,name)`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const data = await response.json();
        
        if (data.files && data.files.length > 0) {
            for (const file of data.files) {
                await deleteDriveFile(file.id);
                console.log(`🗑️ Deleted old notebook: ${file.name}`);
            }
            return data.files.length;
        }
        return 0;
    } catch (error) {
        console.log('No old notebooks to delete:', error);
        return 0;
    }
}

// Export functions
window.uploadNotebookToDrive = uploadNotebookToDrive;
window.getFileContent = getFileContent;
window.getFileDownloadUrl = getFileDownloadUrl;
window.deleteDriveFile = deleteDriveFile;
window.cleanupOldNotebooks = cleanupOldNotebooks;