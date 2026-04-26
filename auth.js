// ========================================
// NotyCaption Pro - Google Authentication Handler
// ========================================

let accessToken = null;
let tokenExpiry = null;
let userInfo = null;

// Cookie management functions
function setCookie(name, value, days = 7) {
    const expires = new Date();
    expires.setTime(expires.getTime() + (days * 24 * 60 * 60 * 1000));
    document.cookie = `${name}=${encodeURIComponent(value)};expires=${expires.toUTCString()};path=/;SameSite=Lax`;
}

function getCookie(name) {
    const cookies = document.cookie.split(';');
    for (let cookie of cookies) {
        const [cookieName, cookieValue] = cookie.trim().split('=');
        if (cookieName === name) {
            return decodeURIComponent(cookieValue);
        }
    }
    return null;
}

function deleteCookie(name) {
    document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 UTC;path=/;`;
}

// Save token to both localStorage and cookie
function saveToken(token, expirySeconds = 3600) {
    accessToken = token;
    tokenExpiry = Date.now() + (expirySeconds * 1000);
    
    localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, token);
    localStorage.setItem(STORAGE_KEYS.TOKEN_EXPIRY, tokenExpiry.toString());
    setCookie(STORAGE_KEYS.ACCESS_TOKEN, token, 7);
    setCookie(STORAGE_KEYS.TOKEN_EXPIRY, tokenExpiry.toString(), 7);
    
    console.log('✅ Token saved to localStorage and cookie');
    return true;
}

// Load token from localStorage or cookie fallback
function loadSavedToken() {
    let savedToken = localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN);
    let savedExpiry = localStorage.getItem(STORAGE_KEYS.TOKEN_EXPIRY);
    
    if (!savedToken) {
        savedToken = getCookie(STORAGE_KEYS.ACCESS_TOKEN);
        savedExpiry = getCookie(STORAGE_KEYS.TOKEN_EXPIRY);
        if (savedToken) {
            console.log('📌 Loaded token from cookie backup');
        }
    }
    
    if (savedToken && savedExpiry && Date.now() < parseInt(savedExpiry)) {
        accessToken = savedToken;
        tokenExpiry = parseInt(savedExpiry);
        
        if (!localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN)) {
            localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, savedToken);
            localStorage.setItem(STORAGE_KEYS.TOKEN_EXPIRY, savedExpiry);
        }
        
        const savedUserInfo = localStorage.getItem(STORAGE_KEYS.USER_INFO);
        if (savedUserInfo) {
            try {
                userInfo = JSON.parse(savedUserInfo);
                console.log('📌 Loaded user info from storage');
            } catch(e) {}
        }
        
        console.log('✅ Loaded valid saved token');
        return true;
    }
    
    console.log('⚠️ No valid saved token found');
    return false;
}

// Clear token from all storage locations
function clearToken() {
    accessToken = null;
    tokenExpiry = null;
    userInfo = null;
    
    localStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN);
    localStorage.removeItem(STORAGE_KEYS.TOKEN_EXPIRY);
    localStorage.removeItem(STORAGE_KEYS.USER_INFO);
    
    deleteCookie(STORAGE_KEYS.ACCESS_TOKEN);
    deleteCookie(STORAGE_KEYS.TOKEN_EXPIRY);
    deleteCookie(STORAGE_KEYS.USER_INFO);
    
    sessionStorage.clear();
    
    console.log('🗑️ Cleared all authentication data');
}

// Get current access token
function getAccessToken() {
    if (!accessToken) {
        loadSavedToken();
    }
    return accessToken;
}

// Login with Google
function loginWithGoogle() {
    console.log('🔐 Starting Google login...');
    
    const redirectUri = encodeURIComponent(CONFIG.REDIRECT_URI);
    const clientId = CONFIG.CLIENT_ID;
    const scope = encodeURIComponent(CONFIG.SCOPES);
    
    const authUrl = `https://accounts.google.com/o/oauth2/auth?client_id=${clientId}&redirect_uri=${redirectUri}&response_type=token&scope=${scope}&include_granted_scopes=true&prompt=select_account&state=notycaption_login`;
    
    console.log('Redirecting to Google OAuth');
    window.location.href = authUrl;
}

// Handle OAuth callback
function handleOAuthCallback() {
    const hash = window.location.hash.substring(1);
    if (!hash) return false;
    
    console.log('📨 Processing OAuth callback');
    
    const params = new URLSearchParams(hash);
    const access_token = params.get('access_token');
    const expires_in = params.get('expires_in');
    
    if (access_token && expires_in) {
        console.log('✅ OAuth callback successful');
        saveToken(access_token, parseInt(expires_in));
        
        if (window.history && window.history.replaceState) {
            window.history.replaceState({}, document.title, window.location.pathname + window.location.search);
        }
        
        // Fetch user info
        fetchUserInfo(access_token).then(() => {
            window.location.hash = 'home';
            setTimeout(() => window.location.reload(), 500);
        });
        
        return true;
    }
    
    return false;
}

// Fetch user info from Google
async function fetchUserInfo(token) {
    try {
        const response = await fetch('https://www.googleapis.com/oauth2/v2/userinfo', {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (response.ok) {
            userInfo = await response.json();
            localStorage.setItem(STORAGE_KEYS.USER_INFO, JSON.stringify(userInfo));
            setCookie(STORAGE_KEYS.USER_INFO, JSON.stringify(userInfo), 7);
            console.log('✅ User info fetched:', userInfo.name);
            return userInfo;
        }
        return null;
    } catch (error) {
        console.error('Error fetching user info:', error);
        return null;
    }
}

// Get user info
async function getUserInfo() {
    const token = getAccessToken();
    if (!token) return null;
    
    if (userInfo) return userInfo;
    
    return await fetchUserInfo(token);
}

// Check if user is logged in
function isAuthenticated() {
    const token = getAccessToken();
    if (!token) return false;
    if (tokenExpiry && Date.now() >= tokenExpiry) return false;
    return true;
}

// Logout function
function logout() {
    console.log('🔓 Logging out...');
    clearToken();
    
    if (typeof updateUIBasedOnAuth === 'function') {
        updateUIBasedOnAuth();
    }
    
    if (typeof showToast === 'function') {
        showToast('✅ Logged out successfully');
    }
}

// Initialize auth
async function initAuth() {
    console.log('🔐 Initializing authentication system...');
    
    if (window.location.hash && window.location.hash.includes('access_token')) {
        handleOAuthCallback();
        return false;
    }
    
    const hasToken = loadSavedToken();
    if (hasToken) {
        await getUserInfo();
        return true;
    }
    
    return false;
}

// Export functions
window.loginWithGoogle = loginWithGoogle;
window.logout = logout;
window.isAuthenticated = isAuthenticated;
window.initAuth = initAuth;
window.getUserInfo = getUserInfo;
window.getAccessToken = getAccessToken;
window.loadSavedToken = loadSavedToken;
window.clearToken = clearToken;