/* Arrow Dash infinite progression and account persistence. */
(function () {
  'use strict';

  const API = '/api/';
  const CACHE_KEY = 'notycaption_arrow_dash_profile_v2';
  const GUEST_KEY = 'notycaption_arrow_dash_guest_v2';

  function token() {
    return localStorage.getItem('notycaption_access_token') || '';
  }

  function loggedIn() {
    return !!token();
  }

  function browserProfile() {
    try { return JSON.parse(localStorage.getItem(CACHE_KEY) || '{}'); } catch (_) { return {}; }
  }

  function saveBrowserProfile(profile) {
    localStorage.setItem(CACHE_KEY, JSON.stringify(profile));
  }

  function guestName() {
    let name = localStorage.getItem(GUEST_KEY);
    if (!name) {
      name = 'Guest-' + Math.floor(1000 + Math.random() * 9000);
      localStorage.setItem(GUEST_KEY, name);
    }
    return name;
  }

  async function api(path, options) {
    const headers = Object.assign({'Content-Type':'application/json'}, (options && options.headers) || {});
    if (token()) headers.Authorization = 'Bearer ' + token();
    const response = await fetch(API + path, Object.assign({}, options || {}, {headers}));
    const data = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(data.error || ('Request failed: ' + response.status));
    return data;
  }

  async function loadProfile() {
    if (!loggedIn()) return browserProfile();
    try {
      const data = await api('profile.php');
      const profile = {
        username: data.username,
        stage: data.stage || {easy:1,normal:1,hard:1},
        score: data.score || {easy:0,normal:0,hard:0}
      };
      saveBrowserProfile(profile);
      return profile;
    } catch (_) {
      return browserProfile();
    }
  }

  async function saveProgress() {
    const profile = browserProfile();
    const difficulty = state.difficulty;
    const score = Math.max(0, Math.floor(Math.max(state.score, state.totalScore)));
    const stage = Math.max(1, state.stageIndex + (state.dead ? 2 : 1));
    profile.stage = profile.stage || {easy:1,normal:1,hard:1};
    profile.score = profile.score || {easy:0,normal:0,hard:0};
    profile.stage[difficulty] = Math.max(profile.stage[difficulty] || 1, stage);
    profile.score[difficulty] = Math.max(profile.score[difficulty] || 0, score);
    profile.username = profile.username || (loggedIn() ? 'Player' : guestName());
    saveBrowserProfile(profile);

    if (loggedIn()) {
      try {
        await api('score.php', {method:'POST', body:JSON.stringify({
          difficulty, score: profile.score[difficulty], stage: profile.stage[difficulty]
        })});
      } catch (error) {
        console.warn('Server score save failed:', error);
      }
    }
  }

  async function saveUsername(name) {
    if (!loggedIn()) return;
    const data = await api('profile.php', {method:'POST', body:JSON.stringify({username:name})});
    const profile = browserProfile();
    profile.username = data.username;
    saveBrowserProfile(profile);
    refreshProfileUI();
  }

  function injectStyles() {
    if (document.getElementById('arrowInfiniteStyle')) return;
    const style = document.createElement('style');
    style.id = 'arrowInfiniteStyle';
    style.textContent = `
      .infinite-badges{display:flex;gap:8px;flex-wrap:wrap;justify-content:center;margin:14px 0 20px}
      .infinite-badge{padding:7px 11px;border:1px solid rgba(255,255,255,.14);border-radius:999px;background:rgba(255,255,255,.05);font:600 10px Orbitron;letter-spacing:1px;color:#dce7ff}
      #musicCredit{display:none!important}
      @media(max-width:600px){.leaderboard-row{grid-template-columns:42px 1fr 80px 55px}.leaderboard-card{padding:15px}}
    `;
    document.head.appendChild(style);
  }

  // Audio-only royalty-free API playback is used; no video or song label is displayed.
  const MUSIC_API = 'https://api.freetouse.com/v3/music/tracks/all';
  let musicRequest = null, apiMusicFailed = false;
  let currentApiTrackUrl = '';
  try { currentApiTrackUrl = localStorage.getItem('arrowDashMusicUrl') || ''; } catch (_) {}
  function extractPlayableTracks(payload) {
    const list = Array.isArray(payload && payload.data) ? payload.data : [];
    return list.filter(t => !t.is_premium && t.status === 1).map(t => {
      const firstArtist = Array.isArray(t.artists) && t.artists[0];
      const artist = Array.isArray(firstArtist) ? firstArtist[1] : firstArtist;
      return {
        title: t.title || 'Royalty-free track',
        artist: artist && artist.name ? artist.name : 'Unknown artist',
        url: t.files && typeof t.files.mp3 === 'string' ? t.files.mp3 : ''
      };
    }).filter(t => typeof t.url === 'string' && /^https:\/\//i.test(t.url) && /\.mp3(\?|$)/i.test(t.url));
  }
  async function loadApiMusic() {
    if (musicRequest || apiMusicFailed) return musicRequest;
    musicRequest = (async () => {
      try {
        const response = await fetch(MUSIC_API + '?limit=100&order=random', {mode:'cors',credentials:'omit'});
        if (!response.ok) throw new Error('Music API returned ' + response.status);
        const tracks = extractPlayableTracks(await response.json());
        if (!tracks.length) throw new Error('API returned no playable non-premium tracks.');
        return tracks;
      } catch (error) {
        apiMusicFailed = true;
        console.info('Music API unavailable; game sound effects remain enabled.', error);
        return [];
      }
    })();
    return musicRequest;
  }
  function playApiTrackForDiff(diff) {
    loadApiMusic().then(tracks => {
      if (!tracks || !tracks.length) return;
      const audio = window.__arrowDashApiAudio || (window.__arrowDashApiAudio = new Audio());
      if (!audio.__arrowDashPositionTracking) {
        audio.__arrowDashPositionTracking = true;
        audio.addEventListener('timeupdate', () => {
          try {
            const time = Number.isFinite(audio.duration) && audio.duration - audio.currentTime < 1.5 ? 0 : audio.currentTime;
            localStorage.setItem('arrowDashMusicTime', String(time));
          } catch (_) {}
        });
        audio.addEventListener('pause', () => {
          try { localStorage.setItem('arrowDashMusicTime', String(audio.currentTime || 0)); } catch (_) {}
        });
        audio.addEventListener('loadedmetadata', () => {
          try {
            if (localStorage.getItem('arrowDashMusicUrl') === audio.src) {
              const saved = Number(localStorage.getItem('arrowDashMusicTime') || 0);
              if (Number.isFinite(saved) && saved > 0 && saved < audio.duration) audio.currentTime = saved;
            }
          } catch (_) {}
        });
      }
      let choices = tracks;
      if (tracks.length > 1 && currentApiTrackUrl) {
        choices = tracks.filter(item => item.url !== currentApiTrackUrl);
        if (!choices.length) choices = tracks;
      }
      const track = choices[Math.floor(Math.random() * choices.length)];
      if (!track) return;
      // Select once, then keep the same song and its currentTime across pauses and retries.
      if (!currentApiTrackUrl) currentApiTrackUrl = track.url;
      if (!audio.src || audio.src !== currentApiTrackUrl) audio.src = currentApiTrackUrl;
      try { localStorage.setItem('arrowDashMusicUrl', currentApiTrackUrl); } catch (_) {}
      audio.loop = true;
      audio.volume = 0.38;
      audio.play().catch(() => {});
    });
  }
  function stopApiTrack() {
    const audio = window.__arrowDashApiAudio;
    if (audio) audio.pause();
  }
  function setGameplayNav(active) {
    document.body.classList.toggle('arrow-dash-playing', Boolean(active));
  }

  function injectUI() {
    injectStyles();
    const badge = document.createElement('div');
    badge.className = 'infinite-badges';
    badge.innerHTML = '<div class="infinite-badge">∞ INFINITE STAGES</div><div class="infinite-badge">RANDOMIZED EVERY STAGE</div><div class="infinite-badge">PROGRESS SAVED</div>';
    document.querySelectorAll('.diff-stages').forEach(el => el.textContent = '∞ INFINITE STAGES');
    const cards = document.querySelector('.diff-cards');
    if (cards) cards.after(badge);

  }
  function refreshProfileUI() {
    const p = browserProfile();
    const name = p.username || 'Player';
    const el = document.getElementById('userName');
    const avatar = document.getElementById('userAvatar');
    if (el) el.textContent = name;
    if (avatar) avatar.textContent = name.charAt(0).toUpperCase();
  }

  const originalLoad = window.loadAndStartStage;
  const originalNext = window.nextStage;
  const originalStart = window.startGame;
  const originalStartMusic = window.startGameMusic;
  const originalStopMusic = window.stopMusic;
  const originalPlayMusicForDiff = window.playMusicForDiff;
  const originalKillPlayer = window.killPlayer;
  window.startGameMusic = function () {
    if (typeof originalStartMusic === 'function') originalStartMusic();
    playApiTrackForDiff(state.difficulty);
  };
  window.stopMusic = function () {
    if (typeof originalStopMusic === 'function') originalStopMusic();
    stopApiTrack();
  };
  window.playMusicForDiff = function (diff) {
    if (typeof originalPlayMusicForDiff === 'function') originalPlayMusicForDiff(diff);
    stopApiTrack();
  };
  window.killPlayer = function () {
    if (typeof originalKillPlayer === 'function') originalKillPlayer();
    setTimeout(() => setGameplayNav(false), 650);
  };

  window.startGame = async function () {
    setGameplayNav(true);
    const profile = await loadProfile();
    state.stageIndex = Math.max(0, Number(profile.stage && profile.stage[state.difficulty] || 1) - 1);
    state.score = 0;
    state.totalScore = Number(profile.score && profile.score[state.difficulty] || 0);
    state.attempts = 0;
    await originalLoad();
  };

  window.nextStage = async function () {
    setGameplayNav(true);
    await saveProgress();
    state.stageIndex++;
    hideOverlays();
    await originalLoad();
  };
  const originalRestartStage = window.restartStage;
  window.restartStage = function () {
    setGameplayNav(true);
    if (typeof originalRestartStage === 'function') originalRestartStage();
  };

  window.stageComplete = async function () {
    if (state.dead) return;
    state.dead = true;
    state.running = false;
    stopMusic();

    const cfg = DIFFICULTIES[state.difficulty];
    const bonus = 1000 + state.stageIndex * 100;
    state.score += bonus;
    state.totalScore += state.score;
    if (typeof updateHUD === 'function') updateHUD();

    await saveProgress();

    const banner = document.getElementById('stageClearBanner');
    const bannerBonus = document.getElementById('stageClearBonus');
    if (banner && bannerBonus) {
      bannerBonus.textContent = '+' + bonus;
      banner.classList.remove('show');
      void banner.offsetWidth;
      banner.classList.add('show');
    }
    spawnParticles(player.x + player.w/2, player.y + player.h/2, '#39ff14', 40, 6);
    spawnParticles(player.x + player.w/2, player.y + player.h/2, '#ffe500', 30, 5);
    playWinSound();

    setTimeout(() => {
      document.getElementById('stageSub').textContent = `STAGE ${state.stageIndex+1} COMPLETE`;
      document.getElementById('sScore').textContent = state.score;
      document.getElementById('sRating').textContent = '∞';
      document.getElementById('stageOverlay').classList.add('active');
      setGameplayNav(false);
    }, 400);
  };

  const stageSeeds = new Map();
  function randomStageSeed() {
    const key = state.difficulty + ':' + state.stageIndex;
    if (!stageSeeds.has(key)) {
      let seed;
      try { const values = new Uint32Array(1); crypto.getRandomValues(values); seed = values[0]; }
      catch (_) { seed = (Date.now() ^ Math.floor(Math.random() * 0xffffffff) ^ Math.floor(performance.now() * 1000)) >>> 0; }
      stageSeeds.set(key, seed || 1);
    }
    state.stageSeed = stageSeeds.get(key);
    return state.stageSeed;
  }

  window.buildStageLayout = function () {
    const cfg = DIFFICULTIES[state.difficulty];
    groundY = canvas.height - 80;
    platforms = [];
    obstacles = [];
    coins = [];
    powerups = [];
    const stageLen = 12000 + Math.min(state.stageIndex, 1000) * 80;
    levelEnd = stageLen;
    // Continuous ground is the guaranteed safe route; floating platforms are optional.
    platforms.push({x:0,y:groundY,w:stageLen+600,h:80,isGround:true});

    const rng = seededRandom(randomStageSeed());
    let x = 500;
    const gapBase = state.difficulty === 'easy' ? 240 : state.difficulty === 'normal' ? 270 : 300;

    while (x < stageLen - 450) {
      const roll = rng();
      if (roll < 0.27) {
        const count = 1 + Math.floor(rng() * 2);
        for (let i=0;i<count;i++) obstacles.push({type:'spike',x:x+i*34,y:groundY-30,w:30,h:30});
        x += count*34 + gapBase + rng()*100;
      } else if (roll < 0.52) {
        const h = 40 + rng()*55;
        obstacles.push({type:'block',x,y:groundY-h,w:36,h});
        x += 36 + gapBase*.75 + rng()*90;
      } else if (roll < 0.73) {
        const pw = 100+rng()*110;
        const py = groundY-115-rng()*110;
        platforms.push({x,y:py,w:pw,h:16,isGround:false});
        // No spikes on top of platforms or their landing points.
        for(let c=0;c<4;c++) coins.push({x:x+18+c*28,y:py-35,collected:false});
        x += pw + gapBase + rng()*100;
      } else if (roll < 0.88) {
        const h=40+rng()*45;
        obstacles.push({type:'moving',x,y:groundY-h,w:32,h,baseX:x,amplitude:50+rng()*90,speed:.025+rng()*.035,phase:rng()*Math.PI*2});
        x += 32+gapBase+rng()*100;
      } else {
        // Void gaps are excluded so every stage retains a continuous, solvable route.
        x += gapBase + rng()*120;
      }
    }

    let puX=700;
    const types=['speed','superjump','shield'];
    while(puX<stageLen-600) {
      if(rng()<.32) {
        const type=types[Math.floor(rng()*types.length)];
        powerups.push({type,x:puX,y:groundY-70-rng()*110,collected:false,bob:rng()*Math.PI*2});
      }
      puX += 300+rng()*420;
    }

    const doorX = stageLen - 170;
    platforms.push({x:doorX,y:groundY-110,w:170,h:110,isEnd:true});
  };

  const originalDrawPlatforms = window.drawPlatforms;
  window.drawPlatforms = function () {
    originalDrawPlatforms();
    const px = levelEnd - 170 - state.cameraX;
    const y = groundY - 110;
    if (px < -220 || px > canvas.width + 220) return;
    const pulse = 1 + Math.sin(tick*.08)*.04;
    ctx.save();
    ctx.shadowBlur=28; ctx.shadowColor='#ffe500';
    ctx.strokeStyle='#ffe500'; ctx.lineWidth=3;
    ctx.strokeRect(px+20,y+18,130,92);
    ctx.fillStyle='rgba(255,229,0,.10)';
    ctx.fillRect(px+20,y+18,130,92);
    ctx.fillStyle='#ffe500';
    ctx.fillRect(px+82,y-20,6,38);
    ctx.beginPath(); ctx.arc(px+85,y+24,30*pulse,0,Math.PI*2); ctx.stroke();
    ctx.fillStyle='#ffe500'; ctx.font='900 13px Orbitron'; ctx.textAlign='center';
    ctx.fillText('FINISH',px+85,y+72);
    ctx.restore();
  };

  window.goMenu = function () {
    setGameplayNav(false);
    hideOverlays();
    stopMusic();
    if (animId) cancelAnimationFrame(animId);
    state.running=false;
    state.dead=false;
    const leaderboardButton = document.getElementById('gameLeaderboardButton');
    if (leaderboardButton) leaderboardButton.style.display='block';
    document.getElementById('menu').style.display='flex';
    document.getElementById('hud').style.display='none';
    document.getElementById('progressWrap').style.display='none';
    ctx.clearRect(0,0,canvas.width,canvas.height);
    particles=[];
    player.trail=[];
    playMusicForDiff(state.difficulty);
  };

  function escapeInitialResume() {
    if (!loggedIn()) return;
    loadProfile().then(refreshProfileUI);
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', injectUI);
  else injectUI();
  escapeInitialResume();
})();