/* Arrow Dash infinite progression, account persistence and leaderboard. */
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
      .leaderboard-btn{position:fixed;right:18px;bottom:18px;z-index:450;padding:12px 18px;border:1px solid #00f5ff;border-radius:10px;background:rgba(5,5,16,.92);color:#00f5ff;font:700 11px Orbitron;letter-spacing:2px;cursor:pointer}
      .leaderboard-panel{position:fixed;inset:0;z-index:10050;background:rgba(3,4,12,.94);backdrop-filter:blur(14px);display:none;align-items:center;justify-content:center;padding:20px}
      .leaderboard-panel.open{display:flex}
      .leaderboard-card{width:min(720px,96vw);max-height:86vh;overflow:auto;border:1px solid rgba(0,245,255,.25);border-radius:18px;background:#0a0d1c;padding:22px;box-shadow:0 0 50px rgba(0,245,255,.12)}
      .leaderboard-head{display:flex;justify-content:space-between;align-items:center;gap:12px;margin-bottom:15px}
      .leaderboard-head h2{font:900 22px Orbitron;color:#00f5ff}
      .leaderboard-close{border:1px solid rgba(255,255,255,.15);background:transparent;color:#fff;border-radius:8px;padding:8px 12px;cursor:pointer}
      .leaderboard-tabs{display:flex;gap:8px;margin-bottom:16px}
      .leaderboard-tab{flex:1;padding:10px;border:1px solid rgba(255,255,255,.12);background:rgba(255,255,255,.04);color:#aaa;border-radius:8px;cursor:pointer;font:700 10px Orbitron}
      .leaderboard-tab.active{color:#fff;border-color:#00f5ff;background:rgba(0,245,255,.12)}
      .leaderboard-row{display:grid;grid-template-columns:60px 1fr 100px 80px;gap:10px;padding:11px 8px;border-bottom:1px solid rgba(255,255,255,.06);font:600 12px Rajdhani}
      .leaderboard-row.head{color:#6f7890;font:700 9px Orbitron;text-transform:uppercase}
      .leaderboard-row.me{background:rgba(255,229,0,.08);border-radius:8px}
      .username-modal{position:fixed;inset:0;z-index:10060;background:rgba(0,0,0,.75);display:none;align-items:center;justify-content:center;padding:20px}
      .username-modal.open{display:flex}.username-card{width:min(440px,94vw);background:#101225;border:1px solid rgba(0,245,255,.25);border-radius:18px;padding:25px}
      .username-card h2{font:800 20px Orbitron;color:#00f5ff;margin-bottom:8px}.username-card p{color:#9ca5bd;font:13px Rajdhani;margin-bottom:15px}
      .username-card input{width:100%;padding:12px;border-radius:9px;border:1px solid #30364e;background:#080a15;color:#fff;margin-bottom:10px}
      .username-actions{display:flex;gap:10px}.username-actions button{flex:1;padding:11px;border-radius:9px;cursor:pointer;border:1px solid rgba(255,255,255,.15);background:rgba(255,255,255,.05);color:#fff}
      .username-actions .save{border-color:#00f5ff;color:#00f5ff}
      @media(max-width:600px){.leaderboard-row{grid-template-columns:42px 1fr 80px 55px}.leaderboard-card{padding:15px}}
    `;
    document.head.appendChild(style);
  }

  function injectUI() {
    injectStyles();

    const badge = document.createElement('div');
    badge.className = 'infinite-badges';
    badge.innerHTML = '<div class="infinite-badge">∞ INFINITE STAGES</div><div class="infinite-badge">RANDOMIZED EVERY STAGE</div><div class="infinite-badge">PROGRESS SAVED</div>';
    const cards = document.querySelector('.diff-cards');
    if (cards) cards.after(badge);

    const button = document.createElement('button');
    button.className = 'leaderboard-btn';
    button.textContent = 'LEADERBOARD';
    button.onclick = openLeaderboard;
    document.body.appendChild(button);

    const panel = document.createElement('div');
    panel.className = 'leaderboard-panel';
    panel.id = 'leaderboardPanel';
    panel.innerHTML = '<div class="leaderboard-card"><div class="leaderboard-head"><h2>LEADERBOARD</h2><button class="leaderboard-close">CLOSE</button></div><div class="leaderboard-tabs"><button class="leaderboard-tab active" data-diff="easy">EASY</button><button class="leaderboard-tab" data-diff="normal">NORMAL</button><button class="leaderboard-tab" data-diff="hard">HARD</button></div><div id="leaderboardRows"></div></div>';
    document.body.appendChild(panel);
    panel.querySelector('.leaderboard-close').onclick = () => panel.classList.remove('open');
    panel.addEventListener('click', e => { if (e.target === panel) panel.classList.remove('open'); });
    panel.querySelectorAll('.leaderboard-tab').forEach(btn => btn.onclick = () => loadLeaderboard(btn.dataset.diff));

    const modal = document.createElement('div');
    modal.className = 'username-modal';
    modal.id = 'usernameModal';
    modal.innerHTML = '<div class="username-card"><h2>YOUR USERNAME</h2><p>Choose a unique username. It is not case-sensitive and can be changed any time.</p><input id="usernameInput" maxlength="20" pattern="[A-Za-z0-9_]{3,20}" placeholder="3-20 letters, numbers or underscores"><div id="usernameError" style="min-height:20px;color:#ff6688;font:12px Rajdhani"></div><div class="username-actions"><button id="usernameCancel">CANCEL</button><button id="usernameSave" class="save">SAVE</button></div></div>';
    document.body.appendChild(modal);
    modal.querySelector('#usernameCancel').onclick = () => modal.classList.remove('open');
    modal.querySelector('#usernameSave').onclick = async () => {
      const input = modal.querySelector('#usernameInput');
      const error = modal.querySelector('#usernameError');
      const name = input.value.trim();
      if (!/^[A-Za-z0-9_]{3,20}$/.test(name)) { error.textContent = 'Use 3-20 letters, numbers or underscores.'; return; }
      try { await saveUsername(name); modal.classList.remove('open'); if(window.showToast) window.showToast('Username updated'); } catch(e) { error.textContent = e.message; }
    };

    if (loggedIn()) {
      const profileButton = document.createElement('button');
      profileButton.className = 'leaderboard-btn';
      profileButton.style.bottom = '68px';
      profileButton.textContent = 'USERNAME';
      profileButton.onclick = async () => {
        const p = await loadProfile();
        modal.querySelector('#usernameInput').value = p.username || '';
        modal.classList.add('open');
      };
      document.body.appendChild(profileButton);
    }
  }

  async function openLeaderboard() {
    document.getElementById('leaderboardPanel').classList.add('open');
    await loadLeaderboard(state.difficulty);
  }

  async function loadLeaderboard(difficulty) {
    const rows = document.getElementById('leaderboardRows');
    rows.innerHTML = '<p style="padding:20px;text-align:center;color:#777">Loading...</p>';
    document.querySelectorAll('.leaderboard-tab').forEach(x => x.classList.toggle('active', x.dataset.diff === difficulty));
    let remote = [];
    try {
      const data = await api('leaderboard.php?difficulty=' + encodeURIComponent(difficulty));
      remote = data.leaderboard || [];
    } catch (_) {}

    const profile = browserProfile();
    const guest = !loggedIn();
    const myName = guest ? guestName() : (profile.username || 'Player');
    const myScore = Number(profile.score && profile.score[difficulty] || 0);
    const myStage = Number(profile.stage && profile.stage[difficulty] || 1);

    if (!loggedIn() && myScore > 0) {
      remote = remote.slice();
      remote.push({rank:0,username:myName,score:myScore,stage:myStage,guest:true});
      remote.sort((a,b)=>b.score-a.score);
    }

    rows.innerHTML = '<div class="leaderboard-row head"><span>#</span><span>PLAYER</span><span>SCORE</span><span>STAGE</span></div>' +
      (remote.length ? remote.map((r,i)=>'<div class="leaderboard-row '+((r.username===myName)?'me':'')+'"><span>'+(i+1)+'</span><span>'+escapeHtml(r.username)+(r.guest?' <small>(GUEST)</small>':'')+'</span><span>'+Number(r.score).toLocaleString()+'</span><span>'+Number(r.stage)+'</span></div>').join('') : '<p style="padding:20px;color:#777;text-align:center">No scores yet.</p>');
  }

  function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[c]));
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

  window.startGame = async function () {
    const profile = await loadProfile();
    state.stageIndex = Math.max(0, Number(profile.stage && profile.stage[state.difficulty] || 1) - 1);
    state.score = 0;
    state.totalScore = Number(profile.score && profile.score[state.difficulty] || 0);
    state.attempts = 0;
    await originalLoad();
  };

  window.nextStage = async function () {
    await saveProgress();
    state.stageIndex++;
    hideOverlays();
    await originalLoad();
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

    await saveProgress();

    spawnParticles(player.x + player.w/2, player.y + player.h/2, '#39ff14', 40, 6);
    spawnParticles(player.x + player.w/2, player.y + player.h/2, '#ffe500', 30, 5);
    playWinSound();

    setTimeout(() => {
      document.getElementById('stageSub').textContent = `STAGE ${state.stageIndex+1} COMPLETE`;
      document.getElementById('sScore').textContent = state.score;
      document.getElementById('sRating').textContent = '∞';
      document.getElementById('stageOverlay').classList.add('active');
    }, 400);
  };

  function randomStageSeed() {
    const difficultySeed = {easy: 113, normal: 227, hard: 419}[state.difficulty];
    return (state.stageIndex + 1) * 1000003 + difficultySeed;
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
    platforms.push({x:0,y:groundY,w:stageLen+600,h:80,isGround:true});

    const rng = seededRandom(randomStageSeed());
    let x = 500;
    const gapBase = state.difficulty === 'easy' ? 180 : state.difficulty === 'normal' ? 210 : 235;

    while (x < stageLen - 450) {
      const roll = rng();
      if (roll < 0.27) {
        const count = 1 + Math.floor(rng() * (state.difficulty === 'hard' ? 3 : 2));
        for (let i=0;i<count;i++) obstacles.push({type:'spike',x:x+i*34,y:groundY-30,w:30,h:30});
        x += count*34 + gapBase + rng()*110;
      } else if (roll < 0.52) {
        const h = 40 + rng()*55;
        obstacles.push({type:'block',x,y:groundY-h,w:36,h});
        x += 36 + gapBase*.75 + rng()*90;
      } else if (roll < 0.73) {
        const pw = 100+rng()*110;
        const py = groundY-115-rng()*110;
        platforms.push({x,y:py,w:pw,h:16,isGround:false});
        if (rng()<.45) obstacles.push({type:'spike',x:x+pw/2-15,y:py-30,w:30,h:30});
        for(let c=0;c<4;c++) coins.push({x:x+18+c*28,y:py-35,collected:false});
        x += pw + gapBase*(cfg.maxGapMult||.8) + rng()*80;
      } else if (roll < 0.88) {
        const h=40+rng()*45;
        obstacles.push({type:'moving',x,y:groundY-h,w:32,h,baseX:x,amplitude:50+rng()*90,speed:.025+rng()*.035,phase:rng()*Math.PI*2});
        x += 32+gapBase+rng()*110;
      } else {
        const gapW=55+rng()*(state.difficulty==='hard'?105:75);
        const ground=platforms.find(p=>p.isGround);
        if(ground && x>ground.x+50) {
          const rightX=x+gapW, rightW=ground.x+ground.w-rightX;
          ground.w=x-ground.x;
          if(rightW>0) platforms.push({x:rightX,y:groundY,w:rightW,h:80,isGround:true});
        }
        x += gapW+gapBase*.55;
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
    hideOverlays();
    stopMusic();
    if (animId) cancelAnimationFrame(animId);
    state.running=false;
    state.dead=false;
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