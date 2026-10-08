/* NotyCaption Pro shared navigation - polished & buttery smooth */
(function () {
  'use strict';
  const BASE = 'https://notycaptiongen.free.nf';
  const LINKS = [
    { key: 'home', href: '/', label: 'Home' },
    { key: 'app', href: '/app', label: 'App' },
    { key: 'game', href: '/game', label: 'Game' },
    { key: 'documentation', href: '/documentation', label: 'Documentation' },
    { key: 'contact', href: '/contact', label: 'Contact' }
  ];
  const DRAWER = [
    { key: 'home', href: '/', label: 'Home' },
    { key: 'app', href: '/app', label: 'App' },
    { key: 'game', href: '/game', label: 'Arrow Dash' },
    { key: 'documentation', href: '/documentation', label: 'Documentation' },
    { key: 'privacy', href: '/privacy', label: 'Privacy Policy' },
    { key: 'terms', href: '/terms', label: 'Terms & Conditions' },
    { key: 'contact', href: '/contact', label: 'Contact' }
  ];

  function loggedIn() {
    try {
      if (localStorage.getItem('notycaption_access_token')) return true;
      return document.cookie.split(';').some(c => c.trim().startsWith('notycaption_access_token='));
    } catch (e) {
      return false;
    }
  }

  function currentKey() {
    const p = location.pathname.replace(/\/+$/, '') || '/';
    if (p === '/' || p === '/home') return 'home';
    const key = p.split('/')[1].toLowerCase();
    return key === 'privacy' ? 'privacy' : key === 'terms' ? 'terms' : key;
  }

  function appHref() {
    return loggedIn() ? '/app' : '/home';
  }

  function render() {
    if (document.querySelector('.noty-global-nav')) return;
    const active = currentKey();
    const nav = document.createElement('div');
    nav.className = 'noty-global-nav';
    nav.innerHTML =
      '<div class="noty-nav-inner">' +
        '<button class="noty-nav-menu" type="button" aria-label="Open menu" aria-expanded="false" data-noty-menu><span></span><span></span><span></span></button>' +
        '<a class="noty-nav-brand" href="/" data-noty-home><img src="/App.ico" alt="NotyCaption Pro">NotyCaption Pro</a>' +
        '<div class="noty-nav-links">' + LINKS.map(x => '<a data-nav-key="' + x.key + '" href="' + (x.key === 'app' ? appHref() : x.href) + '">' + x.label + '</a>').join('') + '</div>' +
        '<div class="noty-nav-actions"><a class="noty-nav-primary" data-nav-app href="' + appHref() + '">' + (loggedIn() ? 'Open App' : 'Sign In') + '</a></div>' +
      '</div>';

    const backdrop = document.createElement('div');
    backdrop.className = 'noty-nav-drawer-backdrop';
    backdrop.innerHTML =
      '<aside class="noty-nav-drawer" data-noty-drawer role="dialog" aria-label="Navigation menu">' +
        '<div class="noty-nav-drawer-head">' +
          '<span class="noty-nav-drawer-title">NotyCaption Pro</span>' +
          '<button class="noty-nav-close" type="button" data-noty-close aria-label="Close menu">✕</button>' +
        '</div>' +
        '<div class="noty-nav-drawer-list">' +
          DRAWER.map(x => '<a data-nav-key="' + x.key + '" href="' + (x.key === 'app' ? appHref() : x.href) + '"><span>' + x.label + '</span><span>›</span></a>').join('') +
        '</div>' +
        '<div class="noty-nav-drawer-actions">' +
          '<a href="https://github.com/NotY215/NotyCaption-Official" target="_blank" rel="noopener">⌘ Source</a>' +
        '</div>' +
      '</aside>';

    document.body.prepend(backdrop);
    document.body.prepend(nav);

    const spacer = document.createElement('div');
    spacer.className = 'noty-nav-spacer';
    if (document.body.classList.contains('noty-nav-overlay')) spacer.style.display = 'none';
    nav.after(spacer);

    document.querySelectorAll('[data-nav-key="' + active + '"]').forEach(a => a.classList.add('active'));

    const drawer = backdrop.querySelector('[data-noty-drawer]');
    const menu = nav.querySelector('[data-noty-menu]');

    const close = () => {
      drawer.classList.remove('open');
      backdrop.classList.remove('open');
      menu.setAttribute('aria-expanded', 'false');
    };
    const open = () => {
      drawer.classList.add('open');
      backdrop.classList.add('open');
      menu.setAttribute('aria-expanded', 'true');
    };

    menu.addEventListener('click', () => (drawer.classList.contains('open') ? close() : open()));
    backdrop.addEventListener('click', e => {
      if (e.target === backdrop || e.target.closest('[data-noty-close]')) close();
    });
    document.addEventListener('keydown', e => {
      if (e.key === 'Escape') close();
    });

    document.querySelectorAll('[data-noty-home]').forEach(a => a.addEventListener('click', () => {}));
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', render);
  } else {
    render();
  }

  window.NotyCaptionNav = { loggedIn };
})();
