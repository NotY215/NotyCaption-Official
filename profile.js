(function () {
  'use strict';
  const token = () => localStorage.getItem('notycaption_access_token') || '';
  if (!token()) return;

  async function request(path, options = {}) {
    const headers = Object.assign({'Content-Type':'application/json','Authorization':'Bearer '+token()}, options.headers || {});
    const response = await fetch('/api/'+path, Object.assign({}, options, {headers}));
    const data = await response.json().catch(()=>({}));
    if (!response.ok) throw new Error(data.error || 'Request failed');
    return data;
  }

  function style() {
    if (document.getElementById('notyProfileStyle')) return;
    const s=document.createElement('style');
    s.id='notyProfileStyle';
    s.textContent=`.noty-account{position:relative}.noty-account-btn{border:1px solid rgba(0,229,255,.25);background:rgba(0,229,255,.08);color:#fff;border-radius:9px;padding:9px 12px;cursor:pointer;font:600 13px Inter,sans-serif}.noty-account-pop{position:absolute;right:0;top:48px;width:260px;padding:16px;border:1px solid rgba(0,229,255,.2);border-radius:14px;background:#101225;box-shadow:0 18px 50px rgba(0,0,0,.35);display:none}.noty-account-pop.open{display:block}.noty-account-name{font-weight:800;color:#fff;margin-bottom:3px}.noty-account-email{font-size:11px;color:#8992aa;margin-bottom:14px;word-break:break-word}.noty-account-pop button{width:100%;padding:9px;border-radius:8px;border:1px solid rgba(255,255,255,.12);background:rgba(255,255,255,.05);color:#fff;cursor:pointer;margin-top:7px}.noty-account-pop button:hover{border-color:#00e5ff;color:#00e5ff}.noty-account-pop .noty-signout{border-color:rgba(255,80,100,.25);color:#ff8fa0}.noty-account-pop .noty-signout:hover{border-color:#ff6688;color:#ff6688}`;
    document.head.appendChild(s);
  }

  async function init() {
    style();
    let profile;
    try { profile = await request('profile.php'); } catch (_) { return; }
    localStorage.setItem('notycaption_username', profile.username || '');
    const appName=document.getElementById('userName'); if(appName) appName.textContent=profile.username || 'User';
    const appAvatar=document.getElementById('userAvatar'); if(appAvatar) appAvatar.textContent=(profile.username || 'U').charAt(0).toUpperCase();
    const navActions=document.querySelector('.noty-nav-actions');
    if (!navActions || document.querySelector('.noty-account')) return;
    const wrap=document.createElement('div');
    wrap.className='noty-account';
    wrap.innerHTML='<button class="noty-account-btn">'+escapeHtml(profile.username || 'Account')+'</button><div class="noty-account-pop"><div class="noty-account-name"></div><div class="noty-account-email"></div><button class="noty-change-name">Change username</button><button class="noty-signout">Sign out</button></div>';
    navActions.prepend(wrap);
    wrap.querySelector('.noty-account-name').textContent=profile.username || 'User';
    wrap.querySelector('.noty-account-email').textContent=profile.email || '';
    wrap.querySelector('.noty-account-btn').onclick=()=>wrap.querySelector('.noty-account-pop').classList.toggle('open');
    wrap.querySelector('.noty-change-name').onclick=()=>openModal(profile.username || '');
    wrap.querySelector('.noty-signout').onclick=signOut;
  }

  async function signOut() {
    try {
      const currentToken = token();
      if (currentToken) {
        await fetch('/api/logout.php', {
          method: 'POST',
          headers: { 'Authorization': 'Bearer ' + currentToken }
        }).catch(() => {});
      }
    } finally {
      [
        'notycaption_access_token',
        'notycaption_username',
        'notycaption_user',
        'notycaption_email'
      ].forEach(key => localStorage.removeItem(key));
      document.cookie = 'notycaption_access_token=; Max-Age=0; path=/; SameSite=Lax';
      window.location.href = '/home';
    }
  }

  function openModal(current) {
    let modal=document.getElementById('notyUsernameModal');
    if (!modal) {
      modal=document.createElement('div');
      modal.id='notyUsernameModal';
      modal.innerHTML='<div style="position:fixed;inset:0;z-index:11000;background:rgba(0,0,0,.75);display:flex;align-items:center;justify-content:center;padding:20px"><div style="width:min(420px,94vw);background:#101225;border:1px solid rgba(0,229,255,.25);border-radius:18px;padding:24px"><h2 style="color:#00e5ff;font:800 20px Orbitron;margin:0 0 8px">Change username</h2><p style="color:#9ca5bd;font:13px Inter;margin:0 0 15px">3-20 letters, numbers or underscores. Usernames are unique without case sensitivity.</p><input id="notyNewUsername" maxlength="20" style="width:100%;padding:11px;box-sizing:border-box;background:#080a15;color:#fff;border:1px solid #30364e;border-radius:9px"><div id="notyUsernameError" style="min-height:20px;color:#ff6688;font-size:12px;margin-top:7px"></div><div style="display:flex;gap:10px;margin-top:10px"><button id="notyCancel" style="flex:1;padding:10px">Cancel</button><button id="notySave" style="flex:1;padding:10px">Save</button></div></div></div>';
      document.body.appendChild(modal);
      modal.querySelector('#notyCancel').onclick=()=>modal.remove();
      modal.querySelector('#notySave').onclick=async()=>{
        const input=modal.querySelector('#notyNewUsername'), err=modal.querySelector('#notyUsernameError'), name=input.value.trim();
        if(!/^[A-Za-z0-9_]{3,20}$/.test(name)){err.textContent='Invalid username.';return;}
        try{
          const data=await request('profile.php',{method:'POST',body:JSON.stringify({username:name})});
          localStorage.setItem('notycaption_username',data.username);
          const button=document.querySelector('.noty-account-btn'); if(button) button.textContent=data.username;
          const n=document.querySelector('.noty-account-name'); if(n) n.textContent=data.username;
          const appName=document.getElementById('userName'); if(appName) appName.textContent=data.username;
          const avatar=document.getElementById('userAvatar'); if(avatar) avatar.textContent=data.username.charAt(0).toUpperCase();
          modal.remove();
          if(window.showToast) window.showToast('Username updated');
        }catch(e){err.textContent=e.message;}
      };
    }
    modal.querySelector('#notyNewUsername').value=current;
  }

  function escapeHtml(value){return String(value).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[c]));}
  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',init); else init();
})();