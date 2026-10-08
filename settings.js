(function () {
  'use strict';

  const token = () => {
    try {
      const local = localStorage.getItem('notycaption_access_token');
      if (local) return local;
      for (const cookie of document.cookie.split(';')) {
        const item = cookie.trim();
        if (item.startsWith('notycaption_access_token=')) {
          return decodeURIComponent(item.slice('notycaption_access_token='.length));
        }
      }
    } catch (_) {}
    return '';
  };

  const message = document.getElementById('settingsMessage');

  function showMessage(text, error) {
    if (!message) return;
    message.textContent = text || '';
    message.classList.toggle('error', Boolean(error));
  }

  async function api(path, options) {
    const headers = Object.assign(
      { 'Content-Type': 'application/json' },
      (options && options.headers) || {}
    );
    const accessToken = token();
    if (accessToken) headers.Authorization = 'Bearer ' + accessToken;

    const response = await fetch('/api/' + path, Object.assign({}, options || {}, { headers }));
    const data = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(data.error || 'Request failed');
    return data;
  }

  function applyTheme(theme) {
    const value = theme === 'light' || theme === 'system' ? theme : 'dark';
    localStorage.setItem('notycaption_theme', value);

    const resolved = value === 'system'
      ? (window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark')
      : value;

    document.documentElement.dataset.notyTheme = resolved;
    document.documentElement.classList.toggle('noty-theme-light', resolved === 'light');

    document.querySelectorAll('.theme-option').forEach(button => {
      button.classList.toggle('active', button.dataset.theme === value);
    });
  }

  function setAccountLocked(locked) {
    const input = document.getElementById('usernameInput');
    const save = document.getElementById('saveUsername');
    if (input) input.disabled = locked;
    if (save) save.disabled = locked;
  }

  async function loadProfile() {
    if (!token()) {
      document.getElementById('emailValue').textContent = 'Not signed in';
      setAccountLocked(true);
      showMessage('Sign in to edit your username. The leaderboard is available without signing in.', true);
      renderScores({ score: {}, stage: {} });
      return;
    }

    try {
      const profile = await api('profile.php');
      document.getElementById('usernameInput').value = profile.username || '';
      document.getElementById('emailValue').textContent = profile.email || '';
      document.getElementById('accountAvatar').textContent =
        (profile.username || 'U').charAt(0).toUpperCase();
      setAccountLocked(false);
      renderScores(profile);
      showMessage('');
      localStorage.setItem('notycaption_username', profile.username || '');
    } catch (error) {
      setAccountLocked(true);
      document.getElementById('emailValue').textContent = 'Session unavailable';
      showMessage('Your Google session could not be verified. Sign in again to edit your username.', true);
    }
  }

  function renderScores(profile) {
    const score = profile.score || {};
    const stage = profile.stage || {};
    const difficulties = ['easy', 'normal', 'hard'];

    document.getElementById('scoreCards').innerHTML = difficulties.map(difficulty =>
      '<article class="score-card">' +
        '<div class="difficulty">' + difficulty + '</div>' +
        '<div class="score">' + Number(score[difficulty] || 0).toLocaleString() + '</div>' +
        '<div class="stage">Best stage: ' + Math.max(1, Number(stage[difficulty] || 1)) + '</div>' +
      '</article>'
    ).join('');
  }

  async function saveUsername() {
    const input = document.getElementById('usernameInput');
    const name = input.value.trim();

    if (!/^[A-Za-z0-9_]{3,20}$/.test(name)) {
      showMessage('Username must be 3 to 20 characters and use only letters, numbers or underscores.', true);
      return;
    }

    const button = document.getElementById('saveUsername');
    button.disabled = true;

    try {
      const data = await api('profile.php', {
        method: 'POST',
        body: JSON.stringify({ username: name })
      });

      localStorage.setItem('notycaption_username', data.username);
      localStorage.setItem('notycaption_game_profile',
        JSON.stringify(Object.assign({}, JSON.parse(localStorage.getItem('notycaption_game_profile') || '{}'), {
          username: data.username
        }))
      );

      input.value = data.username;
      document.getElementById('accountAvatar').textContent = data.username.charAt(0).toUpperCase();
      showMessage('Username updated successfully.');
    } catch (error) {
      showMessage(error.message, true);
    } finally {
      button.disabled = false;
    }
  }

  function signOut() {
    [
      'notycaption_access_token',
      'notycaption_token_expiry',
      'notycaption_username',
      'notycaption_user',
      'notycaption_email',
      'notycaption_user_info'
    ].forEach(key => localStorage.removeItem(key));

    document.cookie.split(';').forEach(cookie => {
      const name = cookie.split('=')[0].trim();
      if (name) {
        document.cookie = name + '=; Max-Age=0; path=/; SameSite=Lax';
      }
    });

    window.location.replace('/');
  }

  document.querySelectorAll('.theme-option').forEach(button => {
    button.addEventListener('click', () => {
      applyTheme(button.dataset.theme);
      showMessage('Theme updated.');
    });
  });

  document.getElementById('saveUsername').addEventListener('click', saveUsername);
  document.getElementById('usernameInput').addEventListener('keydown', event => {
    if (event.key === 'Enter') saveUsername();
  });
  document.getElementById('signOut').addEventListener('click', signOut);

  applyTheme(localStorage.getItem('notycaption_theme') || 'dark');
  loadProfile();
})();
