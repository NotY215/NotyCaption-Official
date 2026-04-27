// ========================================
// Audio Manager - Handles file upload, Drive upload, and audio playback
// ========================================

const AudioManager = {
    currentAudioDriveId: null,
    currentAudioFile: null,
    currentAudioBlobUrl: null,
    
    // Initialize audio manager
    init(uploadAreaId, fileInputId, audioPlayerId, fileInfoId) {
        this.uploadArea = document.getElementById(uploadAreaId);
        this.fileInput = document.getElementById(fileInputId);
        this.audioPlayer = document.getElementById(audioPlayerId);
        this.fileInfo = document.getElementById(fileInfoId);
        
        if (!this.uploadArea || !this.fileInput) {
            console.error('Audio Manager: Required elements not found');
            return false;
        }
        
        this.setupEventListeners();
        return true;
    },
    
    setupEventListeners() {
        // Click on upload area
        this.uploadArea.addEventListener('click', () => {
            console.log('Upload area clicked');
            this.fileInput.click();
        });
        
        // File input change
        this.fileInput.addEventListener('change', (e) => {
            console.log('File input changed', e.target.files);
            if (e.target.files && e.target.files[0]) {
                this.handleFile(e.target.files[0]);
            }
        });
        
        // Drag and drop
        this.uploadArea.addEventListener('dragover', (e) => {
            e.preventDefault();
            this.uploadArea.classList.add('drag-over');
        });
        
        this.uploadArea.addEventListener('dragleave', () => {
            this.uploadArea.classList.remove('drag-over');
        });
        
        this.uploadArea.addEventListener('drop', (e) => {
            e.preventDefault();
            this.uploadArea.classList.remove('drag-over');
            const file = e.dataTransfer.files[0];
            if (file && this.isAudioFile(file)) {
                this.handleFile(file);
            } else {
                this.showToast('Please drop an audio file', true);
            }
        });
    },
    
    isAudioFile(file) {
        const allowedTypes = ['audio/mpeg', 'audio/wav', 'audio/x-wav', 'audio/mp4', 'audio/x-m4a', 'audio/flac', 'audio/ogg', 'audio/aac'];
        const allowedExtensions = ['.mp3', '.wav', '.m4a', '.flac', '.ogg', '.aac', '.webm'];
        const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
        
        return allowedTypes.includes(file.type) || allowedExtensions.includes(ext);
    },
    
    async handleFile(file) {
        console.log('Handling file:', file.name);
        
        if (!this.isAudioFile(file)) {
            this.showToast('❌ Please upload an audio file (MP3, WAV, M4A, FLAC, OGG, AAC)', true);
            return;
        }
        
        if (file.size > 50 * 1024 * 1024) {
            this.showToast('❌ File too large! Max 50MB', true);
            return;
        }
        
        this.currentAudioFile = file;
        
        // Clear previous blob URL
        if (this.currentAudioBlobUrl) {
            URL.revokeObjectURL(this.currentAudioBlobUrl);
        }
        
        // Preview audio
        this.currentAudioBlobUrl = URL.createObjectURL(file);
        this.audioPlayer.src = this.currentAudioBlobUrl;
        this.fileInfo.innerHTML = `📄 ${file.name} - Ready to upload`;
        this.showToast(`✅ Audio loaded: ${file.name}`);
        
        // Auto-upload to Drive
        await this.uploadToDrive(file);
    },
    
    async uploadToDrive(file) {
        this.fileInfo.innerHTML = `📄 ${file.name} - Uploading to Google Drive...`;
        this.showProgress(true, 'Uploading to Google Drive...', 20);
        
        try {
            const token = localStorage.getItem('notycaption_access_token');
            if (!token) {
                throw new Error('Please login again');
            }
            
            // Create FormData for multipart upload
            const formData = new FormData();
            const metadata = {
                name: file.name,
                mimeType: file.type,
                parents: ['root']
            };
            formData.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));
            formData.append('file', file);
            
            const response = await fetch('https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart', {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` },
                body: formData
            });
            
            if (!response.ok) {
                const error = await response.text();
                throw new Error(`Upload failed: ${response.status}`);
            }
            
            const data = await response.json();
            this.currentAudioDriveId = data.id;
            
            this.fileInfo.innerHTML = `📄 ${file.name} - ✅ Uploaded to Google Drive`;
            this.showProgress(false);
            this.showToast('✅ Audio uploaded! Click "Generate Captions" to start AI processing');
            
            // Enable generation buttons
            if (typeof window.enableCaptionButtons === 'function') {
                window.enableCaptionButtons(true);
            }
            
            return this.currentAudioDriveId;
            
        } catch (err) {
            console.error('Upload error:', err);
            this.showProgress(false);
            this.showToast('❌ Upload failed: ' + err.message, true);
            this.fileInfo.innerHTML = `📄 ${file.name} - ❌ Upload failed`;
            return null;
        }
    },
    
    getAudioDriveId() {
        return this.currentAudioDriveId;
    },
    
    getAudioFile() {
        return this.currentAudioFile;
    },
    
    showToast(msg, isError = false) {
        if (typeof window.showToast === 'function') {
            window.showToast(msg, isError);
        } else {
            console.log(msg);
        }
    },
    
    showProgress(show, msg, percent) {
        if (typeof window.showProgress === 'function') {
            window.showProgress(show, msg, percent);
        }
    }
};

// Make available globally
window.AudioManager = AudioManager;