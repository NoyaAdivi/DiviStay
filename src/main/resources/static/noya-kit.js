/*!
 * noya-kit.js  ·  Accessibility menu you can drag anywhere + dark mode
 * One shared file for all of Noya Adivi's sites. No dependencies.
 *
 * Full widget:   <script src="noya-kit.js" defer></script>
 * Options on the script tag:
 *   data-theme-btn="#themeBtn"   site already has its own dark toggle: the kit clicks it to stay in sync
 *   data-attach="#a11yBtn"       site already has an accessibility button: only make it draggable
 *   data-panel="#a11yPanel"      (with data-attach) the panel that should follow the button
 *   data-grid=".a11y-grid"       (with data-attach) add a dark mode switch into this grid
 */
(function () {
  'use strict';
  var me = document.currentScript || document.querySelector('script[src*="noya-kit"]');
  var opt = (me && me.dataset) || {};
  var root = document.documentElement;
  var KEY = 'noya-kit';
  var store = {
    get: function (k, d) { try { var v = localStorage.getItem(KEY + ':' + k); return v == null ? d : JSON.parse(v); } catch (e) { return d; } },
    set: function (k, v) { try { localStorage.setItem(KEY + ':' + k, JSON.stringify(v)); } catch (e) {} }
  };
  var he = function () { return (root.lang || '').toLowerCase().indexOf('he') === 0; };
  var T = function (h, e) { return he() ? h : e; };

  /* ---------- theme ---------- */
  function currentTheme() { return root.getAttribute('data-theme') === 'dark' ? 'dark' : 'light'; }
  function setTheme(t) {
    if (opt.themeBtn) {
      var b = document.querySelector(opt.themeBtn);
      if (b && currentTheme() !== t) { b.click(); return; }
    }
    root.setAttribute('data-theme', t);
    store.set('theme', t);
    var m = document.querySelector('meta[name="theme-color"]');
    if (m) m.content = t === 'dark' ? '#1E1A2E' : '#FFF4EF';
  }
  if (!opt.themeBtn && !opt.attach) {
    var saved = store.get('theme', null);
    var sys = window.matchMedia && matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    root.setAttribute('data-theme', saved || sys);
  }

  /* ---------- styles ---------- */
  var css = '' +
    '.nk-tab{position:fixed;z-index:2147483000;width:56px;height:56px;border-radius:50%;border:3px solid #fff;cursor:grab;touch-action:none;' +
    'background:linear-gradient(135deg,#8B7CF6,#F28C7A);color:#fff;display:grid;place-items:center;box-shadow:0 12px 28px rgba(85,72,200,.35);transition:transform .2s}' +
    '.nk-tab:hover{transform:scale(1.07)}.nk-tab:focus-visible{outline:3px solid #2C2668;outline-offset:3px}' +
    '.nk-tab svg{width:30px;height:30px;fill:none;stroke:#fff;stroke-width:2.2;stroke-linecap:round;stroke-linejoin:round}.nk-tab svg circle{fill:#fff;stroke:none}' +
    '.nk-dragging{cursor:grabbing!important;transform:scale(1.12)!important;transition:none!important}' +
    '.nk-panel{position:fixed;z-index:2147483001;width:320px;max-width:calc(100vw - 24px);max-height:calc(100vh - 24px);overflow:auto;' +
    'background:#FFFDFC;color:#2C2668;border:2px solid #D9D3FF;border-radius:26px;padding:18px;box-shadow:0 24px 60px rgba(44,38,104,.28);' +
    'font:500 15px/1.4 Rubik,system-ui,sans-serif;animation:nkPop .22s ease-out}' +
    '@keyframes nkPop{from{transform:translateY(8px) scale(.97)}}' +
    '.nk-panel[hidden]{display:none}.nk-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px}' +
    '.nk-head h2{margin:0;font:700 1.2rem Fredoka,Rubik,system-ui,sans-serif;color:#5548C8}' +
    '.nk-x{border:0;background:#EEEAFF;color:#2C2668;border-radius:12px;width:34px;height:34px;font-size:1rem;cursor:pointer}' +
    '.nk-lbl{display:block;font-size:.8rem;font-weight:700;color:#7A6B88;margin:12px 0 6px;text-transform:uppercase;letter-spacing:.04em}' +
    '.nk-row{display:flex;gap:8px;align-items:center}.nk-row .nk-val{flex:1;text-align:center;font-weight:700}' +
    '.nk-b{flex:1;border:2px solid #E8E4FF;background:#FBFAFF;color:#2C2668;border-radius:14px;padding:10px 6px;font:700 .86rem Rubik,system-ui,sans-serif;cursor:pointer;display:flex;flex-direction:column;align-items:center;gap:3px}' +
    '.nk-b:hover{border-color:#B3A9FA}.nk-b[aria-pressed=true]{background:#5548C8;border-color:#5548C8;color:#fff}.nk-b span{font-size:1.15rem}' +
    '.nk-grid{display:grid;grid-template-columns:1fr 1fr;gap:8px}' +
    '.nk-reset{width:100%;margin-top:12px;border:0;border-radius:14px;padding:11px;background:linear-gradient(135deg,#FFE6DE,#E8E4FF);color:#2C2668;font:700 .9rem Rubik,system-ui,sans-serif;cursor:pointer}' +
    '.nk-hint{margin:10px 0 0;font-size:.78rem;color:#7A6B88;text-align:center}' +
    'html[data-theme=dark] .nk-panel{background:#251F3A;color:#F1EEFF;border-color:#4A4170}html[data-theme=dark] .nk-head h2{color:#C9C1FF}' +
    'html[data-theme=dark] .nk-b{background:#2F2847;border-color:#3E3560;color:#F1EEFF}html[data-theme=dark] .nk-b[aria-pressed=true]{background:#8B7CF6;border-color:#8B7CF6}' +
    'html[data-theme=dark] .nk-x{background:#3E3560;color:#fff}html[data-theme=dark] .nk-reset{background:linear-gradient(135deg,#4A3A52,#3E3560);color:#fff}' +
    'html[data-theme=dark] .nk-lbl,html[data-theme=dark] .nk-hint{color:#B9AFD6}' +
    /* effects */
    'html.nk-links a{text-decoration:underline!important;text-underline-offset:3px;outline:2px dashed #E36E57;outline-offset:2px}' +
    'html.nk-font body,html.nk-font body *:not(svg):not(svg *){font-family:"Atkinson Hyperlegible",Arial,"Segoe UI",sans-serif!important;letter-spacing:.02em}' +
    'html.nk-still *,html.nk-still *::before,html.nk-still *::after{animation:none!important;transition:none!important;scroll-behavior:auto!important}' +
    'html.nk-cursor,html.nk-cursor *{cursor:url("data:image/svg+xml,%3Csvg xmlns=%27http://www.w3.org/2000/svg%27 width=%2740%27 height=%2740%27%3E%3Cpath d=%27M4 2l26 14-11 3 7 13-5 3-7-13-8 8z%27 fill=%27%232C2668%27 stroke=%27white%27 stroke-width=%272%27/%3E%3C/svg%3E") 4 2,auto!important}' +
    'html.nk-space body p,html.nk-space body li,html.nk-space body span,html.nk-space body label{line-height:1.9!important;word-spacing:.12em}' +
    'html.nk-contrast body>*:not(.nk-tab):not(.nk-panel){filter:contrast(1.35) saturate(1.15)}' +
    'html.nk-gray body>*:not(.nk-tab):not(.nk-panel){filter:grayscale(1)}' +
    'html.nk-contrast.nk-gray body>*:not(.nk-tab):not(.nk-panel){filter:contrast(1.35) grayscale(1)}';
  var st = document.createElement('style'); st.id = 'noya-kit-css'; st.textContent = css; document.head.appendChild(st);

  /* ---------- prefs ---------- */
  var P = store.get('prefs', { scale: 1, on: [] });
  function apply() {
    var z = document.getElementById('noya-kit-zoom');
    if (!z) { z = document.createElement('style'); z.id = 'noya-kit-zoom'; document.head.appendChild(z); }
    z.textContent = P.scale === 1 ? '' : 'body>*:not(.nk-tab):not(.nk-panel):not(script):not(style){zoom:' + P.scale + '}';
    ['links', 'font', 'still', 'cursor', 'space', 'contrast', 'gray'].forEach(function (k) { root.classList.toggle('nk-' + k, P.on.indexOf(k) > -1); });
    store.set('prefs', P);
    refresh();
  }

  /* ---------- drag (works for our tab and for an existing button) ---------- */
  function draggable(btn, panel) {
    var pos = store.get('pos', null), start = null, moved = false;
    function place(x, y) {
      var w = btn.offsetWidth || 56, h = btn.offsetHeight || 56;
      x = Math.max(8, Math.min(innerWidth - w - 8, x)); y = Math.max(8, Math.min(innerHeight - h - 8, y));
      btn.style.left = x + 'px'; btn.style.top = y + 'px'; btn.style.right = 'auto'; btn.style.bottom = 'auto';
      btn.style.insetInlineStart = 'auto'; btn.style.insetInlineEnd = 'auto'; btn.style.position = 'fixed';
      return { x: x, y: y };
    }
    function snap() { var r = btn.getBoundingClientRect(); return { fx: (r.left + r.width / 2) / innerWidth, fy: r.top / innerHeight }; }
    function restore() {
      if (pos) place(pos.fx * innerWidth - (btn.offsetWidth || 56) / 2, pos.fy * innerHeight);
      else if (btn.classList.contains('nk-tab')) place(he() ? innerWidth - 76 : 20, innerHeight - 96);
    }
    btn.title = T('אפשר לגרור את הכפתור לכל מקום במסך', 'You can drag this button anywhere on the screen');
    btn.style.touchAction = 'none';
    btn.addEventListener('pointerdown', function (e) {
      if (e.button !== 0) return;
      var r = btn.getBoundingClientRect();
      start = { x: e.clientX, y: e.clientY, ox: e.clientX - r.left, oy: e.clientY - r.top }; moved = false;
      btn.setPointerCapture(e.pointerId);
    });
    btn.addEventListener('pointermove', function (e) {
      if (!start) return;
      if (!moved && Math.hypot(e.clientX - start.x, e.clientY - start.y) < 6) return;
      moved = true; btn.classList.add('nk-dragging');
      place(e.clientX - start.ox, e.clientY - start.oy); follow(btn, panel);
    });
    btn.addEventListener('pointerup', function () {
      btn.classList.remove('nk-dragging');
      if (moved) { pos = snap(); store.set('pos', pos); }
      start = null;
    });
    btn.addEventListener('click', function (e) { if (moved) { e.stopImmediatePropagation(); e.preventDefault(); moved = false; } }, true);
    btn.addEventListener('keydown', function (e) {
      if (!e.shiftKey || ['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].indexOf(e.key) < 0) return;
      e.preventDefault(); var r = btn.getBoundingClientRect(), d = 24;
      place(r.left + (e.key === 'ArrowLeft' ? -d : e.key === 'ArrowRight' ? d : 0), r.top + (e.key === 'ArrowUp' ? -d : e.key === 'ArrowDown' ? d : 0));
      pos = snap(); store.set('pos', pos); follow(btn, panel);
    });
    addEventListener('resize', function () { restore(); follow(btn, panel); });
    btn._nkReset = function () { pos = null; store.set('pos', null); btn.removeAttribute('style'); restore(); follow(btn, panel); };
    restore();
    if (panel) new MutationObserver(function () { follow(btn, panel); }).observe(panel, { attributes: true, attributeFilter: ['class', 'hidden'] });
  }
  function follow(btn, panel) {
    if (!panel || !btn.style.left) return;
    var r = btn.getBoundingClientRect(), pw = Math.min(panel.offsetWidth || 320, innerWidth - 24), ph = panel.offsetHeight || 420;
    var x = r.left + r.width / 2 > innerWidth / 2 ? r.right - pw : r.left;
    var y = r.top > ph + 16 ? r.top - ph - 12 : Math.min(innerHeight - ph - 12, r.bottom + 12);
    panel.style.left = Math.max(12, Math.min(innerWidth - pw - 12, x)) + 'px';
    panel.style.top = Math.max(12, y) + 'px';
    panel.style.right = 'auto'; panel.style.bottom = 'auto'; panel.style.insetInlineStart = 'auto';
  }

  /* ---------- attach mode: existing accessibility menu ---------- */
  var refresh = function () {};
  function attach() {
    var btn = document.querySelector(opt.attach); if (!btn) return;
    var panel = opt.panel ? document.querySelector(opt.panel) : null;
    draggable(btn, panel);
    var grid = opt.grid ? document.querySelector(opt.grid) : null;
    if (grid && !opt.noDark) {
      var b = document.createElement('button'); b.type = 'button'; b.className = 'nk-dark-opt';
      var paint = function () { var on = currentTheme() === 'dark'; b.setAttribute('aria-pressed', on); b.innerHTML = '<span aria-hidden="true">' + (on ? '☀️' : '🌙') + '</span><b>' + (on ? T('מצב בהיר', 'Light mode') : T('מצב כהה', 'Dark mode')) + '</b>'; };
      b.onclick = function () { setTheme(currentTheme() === 'dark' ? 'light' : 'dark'); paint(); };
      grid.insertBefore(b, grid.firstChild); paint();
      new MutationObserver(paint).observe(root, { attributes: true, attributeFilter: ['lang', 'data-theme'] });
      var saved = store.get('theme', null); if (saved) root.setAttribute('data-theme', saved);
    }
  }

  /* ---------- full widget ---------- */
  function build() {
    var tab = document.createElement('button');
    tab.type = 'button'; tab.className = 'nk-tab'; tab.setAttribute('aria-controls', 'nk-panel'); tab.setAttribute('aria-expanded', 'false');
    tab.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="4" r="2"/><path d="M5 8l7 1.5L19 8M12 9.5V14l-3 7M12 14l3 7"/></svg>';
    var pn = document.createElement('div'); pn.className = 'nk-panel'; pn.id = 'nk-panel'; pn.setAttribute('role', 'dialog'); pn.hidden = true;
    document.body.appendChild(tab); document.body.appendChild(pn);
    var toggles = [['links', '🔗', 'הדגשת קישורים', 'Highlight links'], ['font', '🔤', 'גופן קריא', 'Readable font'], ['still', '⏸️', 'עצירת אנימציות', 'Stop animations'],
      ['cursor', '🖱️', 'סמן גדול', 'Big cursor'], ['space', '↕️', 'ריווח שורות', 'Line spacing'], ['contrast', '🌗', 'ניגודיות גבוהה', 'High contrast'], ['gray', '⚫', 'גווני אפור', 'Grayscale']];
    function render() {
      tab.setAttribute('aria-label', T('תפריט נגישות', 'Accessibility menu'));
      pn.setAttribute('aria-label', T('הגדרות נגישות', 'Accessibility settings')); pn.dir = he() ? 'rtl' : 'ltr';
      var dark = currentTheme() === 'dark';
      pn.innerHTML = '<div class="nk-head"><h2>' + T('נגישות', 'Accessibility') + '</h2><button type="button" class="nk-x" data-k="close" aria-label="' + T('סגירה', 'Close') + '">✕</button></div>' +
        '<span class="nk-lbl">' + T('גודל טקסט', 'Text size') + '</span><div class="nk-row"><button type="button" class="nk-b" data-k="smaller" aria-label="' + T('הקטנת טקסט', 'Smaller text') + '">' + T('א-', 'A-') + '</button>' +
        '<span class="nk-val" aria-live="polite">' + Math.round(P.scale * 100) + '%</span><button type="button" class="nk-b" data-k="bigger" aria-label="' + T('הגדלת טקסט', 'Larger text') + '">' + T('א+', 'A+') + '</button></div>' +
        '<span class="nk-lbl">' + T('מצב תצוגה', 'Display mode') + '</span><div class="nk-row" role="group"><button type="button" class="nk-b" data-k="light" aria-pressed="' + !dark + '"><span>☀️</span>' + T('בהיר', 'Light') + '</button>' +
        '<button type="button" class="nk-b" data-k="dark" aria-pressed="' + dark + '"><span>🌙</span>' + T('כהה', 'Dark') + '</button></div>' +
        '<span class="nk-lbl">' + T('התאמות', 'Adjustments') + '</span><div class="nk-grid">' +
        toggles.map(function (t) { return '<button type="button" class="nk-b" data-t="' + t[0] + '" aria-pressed="' + (P.on.indexOf(t[0]) > -1) + '"><span aria-hidden="true">' + t[1] + '</span>' + T(t[2], t[3]) + '</button>'; }).join('') +
        '<button type="button" class="nk-b" data-k="home"><span aria-hidden="true">📍</span>' + T('החזרת הכפתור', 'Reset button spot') + '</button></div>' +
        '<button type="button" class="nk-reset" data-k="reset">' + T('איפוס כל ההגדרות', 'Reset all settings') + '</button>' +
        '<p class="nk-hint">' + T('טיפ: גוררים את כפתור הנגישות לכל מקום, או Shift + חיצים', 'Tip: drag the accessibility button anywhere, or use Shift + arrows') + '</p>';
    }
    refresh = function () { if (!pn.hidden) { var f = document.activeElement && document.activeElement.getAttribute('data-k') || document.activeElement && document.activeElement.getAttribute('data-t'); render(); if (f) { var el = pn.querySelector('[data-k="' + f + '"],[data-t="' + f + '"]'); if (el) el.focus(); } } };
    function open(v) { pn.hidden = !v; tab.setAttribute('aria-expanded', v); if (v) { render(); follow(tab, pn); var c = pn.querySelector('[data-k=smaller]'); if (c) c.focus(); } }
    tab.addEventListener('click', function () { open(pn.hidden); });
    pn.addEventListener('click', function (e) {
      var b = e.target.closest('button'); if (!b) return;
      var k = b.getAttribute('data-k'), t = b.getAttribute('data-t');
      if (t) { P.on = P.on.indexOf(t) > -1 ? P.on.filter(function (x) { return x !== t; }) : P.on.concat(t); apply(); return; }
      if (k === 'close') { open(false); tab.focus(); }
      if (k === 'bigger') { P.scale = Math.min(1.6, +(P.scale + .1).toFixed(1)); apply(); }
      if (k === 'smaller') { P.scale = Math.max(.8, +(P.scale - .1).toFixed(1)); apply(); }
      if (k === 'light' || k === 'dark') { setTheme(k); setTimeout(refresh, 30); }
      if (k === 'home') tab._nkReset();
      if (k === 'reset') { P = { scale: 1, on: [] }; apply(); }
    });
    document.addEventListener('keydown', function (e) { if (e.key === 'Escape' && !pn.hidden) { open(false); tab.focus(); } });
    document.addEventListener('pointerdown', function (e) { if (!pn.hidden && !pn.contains(e.target) && !tab.contains(e.target)) open(false); });
    new MutationObserver(function () { render(); tab.setAttribute('aria-label', T('תפריט נגישות', 'Accessibility menu')); }).observe(root, { attributes: true, attributeFilter: ['lang', 'data-theme'] });
    draggable(tab, pn); render();
  }

  /* any button with data-nk-theme becomes a dark/light switch */
  function themeButtons() {
    var paint = function () {
      var dark = currentTheme() === 'dark';
      document.querySelectorAll('[data-nk-theme]').forEach(function (b) {
        b.setAttribute('aria-pressed', dark); b.setAttribute('aria-label', dark ? T('מעבר למצב בהיר', 'Switch to light mode') : T('מעבר למצב כהה', 'Switch to dark mode'));
        b.title = b.getAttribute('aria-label'); var i = b.querySelector('[data-nk-icon]') || b; i.textContent = dark ? '☀️' : '🌙';
      });
    };
    document.addEventListener('click', function (e) { var b = e.target.closest && e.target.closest('[data-nk-theme]'); if (b) { setTheme(currentTheme() === 'dark' ? 'light' : 'dark'); paint(); } });
    new MutationObserver(paint).observe(root, { attributes: true, attributeFilter: ['lang', 'data-theme'] }); paint();
  }

  function init() { themeButtons(); if (opt.attach) attach(); else { build(); apply(); } }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init); else init();
})();
