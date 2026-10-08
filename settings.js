(function () {
  'use strict';

  const token = () => {
    try {
      return localStorage.getItem('notycaption_access_token') || '';
    } catch (_) {
      return '';
    }
  };

  if (!token()) {
    window.location.replace('/home');
    return;
  }

  const message = document.getElementById('settingsMessage');

  function showMessage(text, error) {
    message.textContent = text || '';
    message.classList.toggle('error', Boolean(error));
  }

  async function api(path, options) {
    const headers = Object.assign(
      { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token() },
      (options && options.headers) || {}
    );
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

  async function loadProfile() {
    const profile = await api('profile.php');
    document.getElementById('usernameInput').value = profile.username || '';
    document.getElementById('emailValue').textContent = profile.email || '';
    document.getElementById('accountAvatar').textContent = (profile.username || 'U').charAt(0).toUpperCase();
    renderScores(profile);
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
      input.value = data.username;
      document.getElementById('accountAvatar').textContent = data.username.charAt(0).toUpperCase();
      showMessage('Username updated.');
      await loadLeaderboard();
    } catch (error) {
      showMessage(error.message, true);
    } finally {
      button.disabled = false;
    }
  }

  async function loadLeaderboard() {
    const difficulty = document.getElementById('leaderboardDifficulty').value;
    const body = document.getElementById('leaderboardBody');
    body.innerHTML = '<tr><td colspan="4">Loading leaderboard...</td></tr>';

    try {
      const data = await fetch('/api/leaderboard.php?difficulty=' + encodeURIComponent(difficulty)).then(async response => {
        const value = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(value.error || 'Could not load leaderboard');
        return value;
      });

      const rows = Array.isArray(data.leaderboard) ? data.leaderboard : [];
      if (!rows.length) {
        body.innerHTML = '<tr><td colspan="4">No scores yet.</td></tr>';
        return;
      }

      body.innerHTML = rows.map(row =>
        '<tr>' +
          '<td>#' + Number(row.rank || 0) + '</td>' +
          '<td>' + escapeHtml(row.username || 'Player') + '</td>' +
          '<td>' + Math.max(1, Number(row.stage || 1)) + '</td>' +
          '<td>' + Number(row.score || 0).toLocaleString() + '</td>' +
        '</tr>'
      ).join('');
    } catch (error) {
      body.innerHTML = '<tr><td colspan="4">' + escapeHtml(error.message) + '</td></tr>';
    }
  }

  function signOut() {
    [
      'notycaption_access_token',
      'notycaption_username',
      'notycaption_user',
      'notycaption_email'
    ].forEach(key => localStorage.removeItem(key));

    document.cookie.split(';').forEach(cookie => {
      const name = cookie.split('=')[0].trim();
      if (name) document.cookie = name + '=; Max-Age=0; path=/; SameSite=Lax';
    });

    window.location.replace('/home');
  }

  function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, char => ({
      '&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#039;'
    }[char]));
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
  document.getElementById('leaderboardDifficulty').addEventListener('change', loadLeaderboard);
  document.getElementById('signOut').addEventListener('click', signOut);

  applyTheme(localStorage.getItem('notycaption_theme') || 'dark');

  if (location.hash === '#leaderboard') {
    setTimeout(() => document.getElementById('scoresSection')?.scrollIntoView({behavior:'smooth', block:'start'}), 100);
  }

  Promise.all([loadProfile(), loadLeaderboard()]).catch(error => {
    showMessage(error.message, true);
    if (/authentication|session|expired/i.test(error.message)) {
      setTimeout(signOut, 700);
    }
  });
})();