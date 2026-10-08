(function(){
'use strict';
const API='/api/';
const GUEST_KEY='notycaption_guest_name';
const PROFILE_KEY='notycaption_game_profile';

function token(){try{return localStorage.getItem('notycaption_access_token')||'';}catch(_){return '';}}
function guestName(){
  let name=''; try{name=localStorage.getItem(GUEST_KEY)||'';}catch(_){}
  if(!name){name='Guest-'+Math.floor(1000+Math.random()*9000);try{localStorage.setItem(GUEST_KEY,name);}catch(_){}}
  return name;
}
function browserProfile(){try{return JSON.parse(localStorage.getItem(PROFILE_KEY)||'{}');}catch(_){return {};}}
function escapeHtml(value){return String(value).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[c]));}

async function load(difficulty){
  const status=document.getElementById('boardStatus'), body=document.getElementById('leaderboardBody');
  status.textContent='Loading '+difficulty+' leaderboard...'; body.innerHTML='';
  let remote=[], serverError=false;
  try{
    const response=await fetch(API+'leaderboard.php?difficulty='+encodeURIComponent(difficulty),{
      headers:token()?{'Authorization':'Bearer '+token()}:{}
    });
    const data=await response.json().catch(()=>({}));
    if(!response.ok) throw new Error(data.error||'Leaderboard request failed');
    remote=Array.isArray(data.leaderboard)?data.leaderboard:[];
  }catch(_){serverError=true;}

  const profile=browserProfile();
  const logged=Boolean(token());
  const name=logged?(profile.username||'Player'):guestName();
  const score=Number(profile.score&&profile.score[difficulty]||0);
  const stage=Number(profile.stage&&profile.stage[difficulty]||1);

  if(!logged) remote.push({username:name,score,stage,guest:true});
  remote.sort((a,b)=>Number(b.score||0)-Number(a.score||0));
  const rows=remote.slice(0,100);

  if(!rows.length) body.innerHTML='<tr><td colspan="4" class="empty">No scores yet.</td></tr>';
  else body.innerHTML=rows.map((row,index)=>{
    const username=escapeHtml(row.username||'Player');
    const me=String(row.username||'').toLowerCase()===String(name).toLowerCase();
    return '<tr class="'+(me?'me':'')+'"><td>'+(index+1)+'</td><td>'+username+(row.guest?' <span class="guest">(GUEST)</span>':'')+'</td><td>'+Number(row.stage||1)+'</td><td>'+Number(row.score||0).toLocaleString()+'</td></tr>';
  }).join('');

  status.textContent=serverError
    ? 'Server leaderboard unavailable. Showing your local score.'
    : (logged?'Live server leaderboard':'Viewing as guest. Sign in to save your name to the server leaderboard.');
}

document.querySelectorAll('.difficulty-tab').forEach(button=>{
  button.addEventListener('click',()=>{
    document.querySelectorAll('.difficulty-tab').forEach(x=>x.classList.remove('active'));
    button.classList.add('active');
    load(button.dataset.difficulty);
  });
});
load('easy');
})();
