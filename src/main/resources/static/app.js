// DiviStay – small motion helpers shared by every page
(() => {
  const root = document.documentElement;
  const calm = window.matchMedia('(prefers-reduced-motion: reduce)').matches || root.classList.contains('a11y-motion');
  const store = {
    get(k, d) { try { const v = localStorage.getItem(k); return v === null ? d : v; } catch (e) { return d; } },
    set(k, v) { try { localStorage.setItem(k, v); } catch (e) { /* private mode: settings last for this page only */ } }
  };

  // 0. Light / dark mode (the saved choice is applied in <head> before paint)
  const themeBtn = document.getElementById('themeToggle');
  const syncTheme = () => themeBtn && themeBtn.setAttribute('aria-pressed', String(root.dataset.theme === 'dark'));
  syncTheme();
  if (themeBtn) themeBtn.addEventListener('click', () => {
    root.dataset.theme = root.dataset.theme === 'dark' ? 'light' : 'dark';
    store.set('ds-theme', root.dataset.theme);
    syncTheme();
  });

  // 0b. Accessibility menu
  const btn = document.getElementById('a11yBtn');
  const panel = document.getElementById('a11yPanel');
  if (btn && panel) {
    let prefs = {};
    try { prefs = JSON.parse(store.get('ds-a11y', '{}')) || {}; } catch (e) { prefs = {}; }
    const toggles = ['contrast', 'links', 'font', 'motion', 'cursor'];
    const sizeOut = document.getElementById('a11ySize');
    const apply = () => {
      toggles.forEach(k => {
        root.classList.toggle('a11y-' + k, !!prefs[k]);
        const b = panel.querySelector('[data-a11y="' + k + '"]');
        if (b) b.setAttribute('aria-pressed', String(!!prefs[k]));
      });
      const size = prefs.size || 0;
      root.style.fontSize = size ? (100 + size * 12.5) + '%' : '';
      if (sizeOut) sizeOut.textContent = (100 + size * 12.5) + '%';
      store.set('ds-a11y', JSON.stringify(prefs));
    };
    const open = (yes) => {
      panel.hidden = !yes;
      btn.setAttribute('aria-expanded', String(yes));
      if (yes) panel.querySelector('button').focus(); else btn.focus();
    };
    btn.addEventListener('click', () => open(panel.hidden));
    panel.addEventListener('click', e => {
      const b = e.target.closest('[data-a11y]');
      if (!b) return;
      const k = b.dataset.a11y;
      if (k === 'close') return open(false);
      if (k === 'reset') prefs = {};
      else if (k === 'bigger') prefs.size = Math.min(4, (prefs.size || 0) + 1);
      else if (k === 'smaller') prefs.size = Math.max(-1, (prefs.size || 0) - 1);
      else prefs[k] = !prefs[k];
      apply();
    });
    document.addEventListener('keydown', e => { if (e.key === 'Escape' && !panel.hidden) open(false); });
    document.addEventListener('click', e => {
      if (!panel.hidden && !panel.contains(e.target) && !btn.contains(e.target)) { panel.hidden = true; btn.setAttribute('aria-expanded', 'false'); }
    });
    apply();
  }

  // 1. Soft fade-up of cards as they scroll into view, staggered inside each group
  const targets = document.querySelectorAll('.card, .step, .hotel-card, .t-step, .feature, .faq details, .cta, [data-reveal]');
  if (!calm && 'IntersectionObserver' in window) {
    const seen = new Map();
    targets.forEach(el => {
      const parent = el.parentElement;
      const i = seen.get(parent) || 0;
      seen.set(parent, i + 1);
      el.style.setProperty('--d', Math.min(i, 6) * 0.08 + 's');
      el.classList.add('reveal');
    });
    const io = new IntersectionObserver(entries => entries.forEach(e => {
      if (e.isIntersecting) { e.target.classList.add('in'); io.unobserve(e.target); }
    }), { threshold: 0.12, rootMargin: '0px 0px -30px 0px' });
    targets.forEach(el => io.observe(el));
    // safety net: never leave content hidden (e.g. background tabs where the observer is paused)
    setTimeout(() => targets.forEach(el => el.classList.add('in')), 2500);
  }

  // 2. Confetti in the logo colours (used when a booking is confirmed / a share is paid)
  window.divistayConfetti = (count = 90) => {
    if (calm) return;
    const colors = ['#8FA48D', '#E2AE88', '#253447', '#F4DCD3', '#E5EDE2', '#FAE8DA'];
    const box = document.createElement('div');
    box.className = 'confetti';
    for (let i = 0; i < count; i++) {
      const p = document.createElement('i');
      p.style.left = Math.random() * 100 + 'vw';
      p.style.background = colors[i % colors.length];
      p.style.animationDuration = 2.2 + Math.random() * 2 + 's';
      p.style.animationDelay = Math.random() * 0.6 + 's';
      p.style.transform = `rotate(${Math.random() * 360}deg)`;
      if (i % 3 === 0) p.style.borderRadius = '50%';
      box.appendChild(p);
    }
    document.body.appendChild(box);
    setTimeout(() => box.remove(), 5000);
  };
  const flag = document.body.dataset.confetti;
  if (flag) {
    // only celebrate once per page/state, not on every auto-refresh
    try {
      if (!sessionStorage.getItem('confetti:' + flag)) {
        sessionStorage.setItem('confetti:' + flag, '1');
        setTimeout(() => window.divistayConfetti(), 400);
      }
    } catch (e) { setTimeout(() => window.divistayConfetti(), 400); }
  }

  // 3. Countdown timers ("12:34 left"), turning red in the last 5 minutes
  document.querySelectorAll('.countdown[data-expires]').forEach(el => {
    const end = new Date(el.dataset.expires).getTime();
    const out = el.querySelector('.cd');
    const tick = () => {
      const s = Math.max(0, Math.round((end - Date.now()) / 1000));
      out.textContent = String(Math.floor(s / 60)).padStart(2, '0') + ':' + String(s % 60).padStart(2, '0');
      el.classList.toggle('urgent', s <= 300);
      if (s === 0 && el.dataset.reload) setTimeout(() => location.reload(), 12000);
      return s;
    };
    tick();
    const t = setInterval(() => { if (tick() === 0) clearInterval(t); }, 1000);
  });

  // 4. Page polish: reading progress, compact header on scroll, back-to-top
  const heb = root.lang === 'he';
  const bar = document.createElement('div');
  bar.className = 'progress-top'; bar.setAttribute('aria-hidden', 'true');
  document.body.prepend(bar);
  const topBtn = document.createElement('button');
  topBtn.type = 'button'; topBtn.className = 'to-top';
  topBtn.setAttribute('aria-label', heb ? 'חזרה לראש העמוד' : 'Back to top');
  topBtn.innerHTML = '<span aria-hidden="true">↑</span>';
  topBtn.addEventListener('click', () => window.scrollTo({ top: 0, behavior: calm ? 'auto' : 'smooth' }));
  document.body.appendChild(topBtn);
  const header = document.querySelector('.topbar');
  let ticking = false;
  const onScroll = () => {
    ticking = false;
    const max = document.documentElement.scrollHeight - innerHeight;
    bar.style.transform = `scaleX(${max > 0 ? Math.min(1, scrollY / max) : 0})`;
    if (header) header.classList.toggle('scrolled', scrollY > 12);
    topBtn.classList.toggle('show', scrollY > 700);
  };
  addEventListener('scroll', () => { if (!ticking) { ticking = true; requestAnimationFrame(onScroll); } }, { passive: true });
  onScroll();

  // 5. Toasts (small messages that slide in and disappear)
  let toastBox;
  window.divistayToast = (text, kind) => {
    if (!toastBox) {
      toastBox = document.createElement('div');
      toastBox.className = 'toasts'; toastBox.setAttribute('role', 'status'); toastBox.setAttribute('aria-live', 'polite');
      document.body.appendChild(toastBox);
    }
    const t = document.createElement('div');
    t.className = 'toast' + (kind ? ' ' + kind : '');
    t.textContent = text;
    toastBox.appendChild(t);
    setTimeout(() => t.classList.add('out'), 3200);
    setTimeout(() => t.remove(), 3700);
  };

  // 6. Copy buttons: <button data-copy="text" data-copied="Copied!">
  document.addEventListener('click', async e => {
    const b = e.target.closest('[data-copy]');
    if (!b) return;
    try {
      await navigator.clipboard.writeText(b.dataset.copy);
    } catch (err) {
      const ta = document.createElement('textarea');
      ta.value = b.dataset.copy; document.body.appendChild(ta); ta.select();
      try { document.execCommand('copy'); } catch (e2) { /* ignore */ }
      ta.remove();
    }
    window.divistayToast(b.dataset.copied || (heb ? 'הקישור הועתק' : 'Link copied'), 'ok');
  });

  // 7. Soft ripple on buttons
  document.addEventListener('pointerdown', e => {
    const b = e.target.closest('.btn');
    if (!b || calm) return;
    const r = b.getBoundingClientRect();
    const s = document.createElement('span');
    s.className = 'ripple';
    s.style.left = (e.clientX - r.left) + 'px'; s.style.top = (e.clientY - r.top) + 'px';
    b.appendChild(s);
    setTimeout(() => s.remove(), 650);
  });

  // 8. Photos: if one can't load (offline, blocked), show a soft pastel picture instead of a broken image
  const photoFail = img => { img.classList.add('ph-fail'); img.removeAttribute('src'); img.alt = ''; };
  document.querySelectorAll('img.ph').forEach(img => {
    if (img.complete && img.naturalWidth === 0 && img.getAttribute('src')) photoFail(img);
    img.addEventListener('error', () => photoFail(img), { once: true });
  });

  // 9. Search box: dates stay in order; who's coming (adults, children with ages, how many pay)
  document.querySelectorAll('form.search3').forEach(form => {
    const cin = form.querySelector('.js-in'), cout = form.querySelector('.js-out');
    const iso = d => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
    cin && cin.addEventListener('change', () => {
      if (!cin.value) return;
      const next = new Date(cin.value + 'T00:00:00'); next.setDate(next.getDate() + 1);
      cout.min = iso(next);
      if (!cout.value || cout.value <= cin.value) { next.setDate(next.getDate() + 1); cout.value = iso(next); }
    });

    const pop = form.querySelector('.sb-pop');
    if (!pop) return;
    const details = form.querySelector('.sb-guests');
    const ages = pop.querySelector('.ages');
    const counter = name => pop.querySelector(`.counter[data-name="${name}"]`);
    const value = name => +counter(name).querySelector('output').value || +counter(name).querySelector('output').textContent;
    const fmt = (tpl, n) => tpl.replace('{0}', n);
    const summary = () => {
      const a = value('adults'), c = value('children'), p = value('payers');
      let t = a === 1 ? pop.dataset.adults1 : fmt(pop.dataset.adults, a);
      if (c) t += ' · ' + (c === 1 ? pop.dataset.child1 : fmt(pop.dataset.children, c));
      t += ' · ' + fmt(pop.dataset.payers, p);
      form.querySelector('.sb-guests-text').textContent = t;
    };
    const setAges = n => {
      const rows = [...ages.querySelectorAll('label')];
      for (let i = rows.length; i < n; i++) {
        const l = document.createElement('label');
        const span = document.createElement('span');
        span.textContent = pop.dataset.ageLabel + ' ' + (i + 1);
        const sel = document.createElement('select');
        sel.name = 'ages';
        for (let a = 0; a <= 17; a++) { const o = document.createElement('option'); o.value = o.textContent = a; if (a === 8) o.selected = true; sel.appendChild(o); }
        l.append(span, sel); ages.appendChild(l);
        l.classList.add('pop-in');
      }
      rows.slice(n).forEach(r => r.remove());
    };
    const set = (name, v) => {
      const c = counter(name);
      v = Math.max(+c.dataset.min, Math.min(+c.dataset.max, v));
      const out = c.querySelector('output');
      out.value = v; out.textContent = v;
      out.classList.remove('bump'); void out.offsetWidth; out.classList.add('bump');
      const hidden = c.querySelector('input[type=hidden]');
      if (hidden) hidden.value = v;
      c.querySelector('[data-d="-1"]').disabled = v <= +c.dataset.min;
      c.querySelector('[data-d="1"]').disabled = v >= +c.dataset.max;
      return v;
    };
    let payersTouched = value('payers') !== value('adults');
    pop.addEventListener('click', e => {
      const b = e.target.closest('[data-d]');
      if (!b) return;
      const c = b.closest('.counter'), name = c.dataset.name;
      const v = set(name, value(name) + +b.dataset.d);
      if (name === 'children') setAges(v);
      if (name === 'payers') payersTouched = true;
      if (name === 'adults' && !payersTouched) set('payers', v);   // by default every adult pays
      summary();
    });
    ['adults', 'children', 'payers'].forEach(n => set(n, value(n)));
    pop.querySelector('.sb-done').addEventListener('click', () => { details.open = false; details.querySelector('summary').focus(); });
    document.addEventListener('click', e => { if (details.open && !details.contains(e.target)) details.open = false; });
    details.addEventListener('keydown', e => { if (e.key === 'Escape') { details.open = false; details.querySelector('summary').focus(); } });
  });

  // 10. Photo gallery: click a photo to see it big; arrows and Escape work
  const gallery = document.querySelector('[data-gallery]');
  if (gallery) {
    const shots = [...gallery.querySelectorAll('[data-full]')];
    const box = document.createElement('div');
    box.className = 'lightbox'; box.hidden = true; box.setAttribute('role', 'dialog'); box.setAttribute('aria-modal', 'true');
    box.innerHTML = '<button type="button" class="lb-x" aria-label="✕">✕</button><button type="button" class="lb-prev" aria-label="‹">‹</button><img alt=""><button type="button" class="lb-next" aria-label="›">›</button><span class="lb-count"></span>';
    document.body.appendChild(box);
    let at = 0, opener = null;
    const show = i => {
      at = (i + shots.length) % shots.length;
      box.querySelector('img').src = shots[at].dataset.full;
      box.querySelector('.lb-count').textContent = (at + 1) + ' / ' + shots.length;
    };
    const close = () => { box.hidden = true; document.body.classList.remove('no-scroll'); opener && opener.focus(); };
    shots.forEach((s, i) => s.addEventListener('click', () => { opener = s; show(i); box.hidden = false; document.body.classList.add('no-scroll'); box.querySelector('.lb-x').focus(); }));
    box.querySelector('.lb-x').addEventListener('click', close);
    box.querySelector('.lb-prev').addEventListener('click', () => show(at - 1));
    box.querySelector('.lb-next').addEventListener('click', () => show(at + 1));
    box.addEventListener('click', e => { if (e.target === box) close(); });
    document.addEventListener('keydown', e => {
      if (box.hidden) return;
      if (e.key === 'Escape') close();
      if (e.key === 'ArrowLeft') show(at + (heb ? 1 : -1));
      if (e.key === 'ArrowRight') show(at + (heb ? -1 : 1));
    });
  }

  // 11. Room photos: small dots switch the picture on each room-type card
  document.querySelectorAll('.rt-photos').forEach(box => {
    const imgs = [...box.querySelectorAll('img')], dots = [...box.querySelectorAll('.rt-dot')];
    const go = i => { imgs.forEach((im, k) => im.classList.toggle('on', k === i)); dots.forEach((d, k) => d.setAttribute('aria-pressed', k === i)); };
    dots.forEach((d, i) => d.addEventListener('click', () => go(i)));
  });

  // 12. Search filters apply as soon as they change
  document.querySelectorAll('form[data-autosubmit]').forEach(f => f.addEventListener('change', e => {
    if (e.target.matches('input,select')) { f.querySelector('input[name=page]') && (f.querySelector('input[name=page]').value = 1); f.requestSubmit ? f.requestSubmit() : f.submit(); }
  }));

  // 13. Main menu: mark the page we're on, close the phone menu after choosing
  document.querySelectorAll('.menu-links a').forEach(a => {
    const u = new URL(a.href, location.href);
    if (!u.hash && u.pathname === location.pathname) a.setAttribute('aria-current', 'page');
    a.addEventListener('click', () => { const d = document.querySelector('.menu-toggle'); if (d) d.open = false; });
  });
  document.addEventListener('click', e => {
    const d = document.querySelector('.menu-toggle');
    if (d && d.open && !e.target.closest('.mainnav')) d.open = false;
  });
})();
