(function () {
  'use strict';

  const LINKS = [
    { key: 'home', href: '/', label: 'Home' },
    { key: 'app', href: '/app', label: 'App' },
    { key: 'game', href: '/game', label: 'Game' },
    { key: 'documentation', href: '/documentation', label: 'Documentation' },
    { key: 'contact', href: '/contact', label: 'Contact' }
  ];

  function isLoggedIn() {
    try {
      return Boolean(localStorage.getItem('notycaption_access_token'));
    } catch (_) {
      return false;
    }
  }

  function applyTheme() {
    let theme = 'dark';
    try {
      theme = localStorage.getItem('notycaption_theme') || 'dark';
    } catch (_) {}

    const resolved = theme === 'system'
      ? (window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark')
      : theme;

    document.documentElement.dataset.notyTheme = resolved;
    document.documentElement.classList.toggle('noty-theme-light', resolved === 'light');
  }

  function currentKey() {
    const path = location.pathname.replace(/\\/+$/, '') || '/';
    if (path === '/' || path === '/home') return 'home';
    const key = path.split('/')[1].toLowerCase();
    if (key === 'privacy') return 'privacy';
    if (key === 'terms' || key === 'tc') return 'terms';
    return key;
  }

  function appHref() {
    return isLoggedIn() ? '/app' : '/home';
  }

  function settingsMarkup() {
    return isLoggedIn()
      ? '<a class="noty-nav-settings" data-nav-settings href="/settings">Settings</a>'
      : '';
  }

  function render() {
    if (document.querySelector('.noty-global-nav')) return;

    const active = currentKey();
    const loggedIn = isLoggedIn();

    const nav = document.createElement('header');
    nav.className = 'noty-global-nav';
    nav.innerHTML =
      '<div class="noty-nav-inner">' +
        '<button class="noty-nav-menu" type="button" aria-label="Open menu" aria-expanded="false" data-noty-menu>' +
          '<span></span><span></span><span></span>' +
        '</button>' +
        '<a class="noty-nav-brand" href="/" data-noty-home>' +
          '<img src="/App.ico" alt="NotyCaption Pro">' +
          '<span>NotyCaption Pro</span>' +
        '</a>' +
        '<nav class="noty-nav-links" aria-label="Primary navigation">' +
          LINKS.map(x => '<a data-nav-key="' + x.key + '" href="' + (x.key === 'app' ? appHref() : x.href) + '">' + x.label + '</a>').join('') +
        '</nav>' +
        '<div class="noty-nav-actions">' +
          settingsMarkup() +
          '<a class="noty-nav-primary" data-nav-app href="' + appHref() + '">' + (loggedIn ? 'Open App' : 'Sign In') + '</a>' +
        '</div>' +
      '</div>';

    const backdrop = document.createElement('div');
    backdrop.className = 'noty-nav-drawer-backdrop';
    backdrop.innerHTML =
      '<aside class="noty-nav-drawer" data-noty-drawer role="dialog" aria-label="Navigation menu">' +
        '<div class="noty-nav-drawer-head">' +
          '<a class="noty-nav-drawer-brand" href="/"><img src="/App.ico" alt=""> <span>NotyCaption Pro</span></a>' +
          '<button class="noty-nav-close" type="button" data-noty-close aria-label="Close menu">×</button>' +
        '</div>' +
        '<nav class="noty-nav-drawer-list" aria-label="Mobile navigation">' +
          LINKS.map(x => '<a data-nav-key="' + x.key + '" href="' + (x.key === 'app' ? appHref() : x.href) + '"><span>' + x.label + '</span><span aria-hidden="true">›</span></a>').join('') +
          (loggedIn ? '<a data-nav-key="settings" href="/settings"><span>Settings</span><span aria-hidden="true">›</span></a>' : '') +
          '<a data-nav-key="privacy" href="/privacy"><span>Privacy Policy</span><span aria-hidden="true">›</span></a>' +
          '<a data-nav-key="terms" href="/terms"><span>Terms & Conditions</span><span aria-hidden="true">›</span></a>' +
        '</nav>' +
        '<div class="noty-nav-drawer-actions">' +
          '<a href="https://github.com/NotY215/NotyCaption-Official" target="_blank" rel="noopener">Source code</a>' +
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
      document.body.classList.remove('noty-drawer-open');
    };

    const open = () => {
      drawer.classList.add('open');
      backdrop.classList.add('open');
      menu.setAttribute('aria-expanded', 'true');
      document.body.classList.add('noty-drawer-open');
    };

    menu.addEventListener('click', () => drawer.classList.contains('open') ? close() : open());
    backdrop.addEventListener('click', event => {
      if (event.target === backdrop || event.target.closest('[data-noty-close]')) close();
    });
    document.addEventListener('keydown', event => {
      if (event.key === 'Escape') close();
    });

    window.NotyCaptionNav = {
      loggedIn: isLoggedIn,
      applyTheme
    };
  }

  applyTheme();

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', render);
  } else {
    render();
  }

  window.NotyCaptionNav = window.NotyCaptionNav || { loggedIn: isLoggedIn, applyTheme };
})();

(function loadArrowDashEnhancements() {
  const path = location.pathname.replace(/\\/+$/, '');
  if (path !== '/game') return;
  const script = document.createElement('script');
  script.src = '/game-infinite.js';
  script.defer = false;
  document.body.appendChild(script);
})();