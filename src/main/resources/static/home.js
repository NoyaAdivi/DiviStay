// DiviStay – home page interactions
(() => {
  const root = document.documentElement;
  const calm = () => matchMedia('(prefers-reduced-motion: reduce)').matches || root.classList.contains('a11y-motion');
  const fine = matchMedia('(pointer: fine)').matches;
  const he = root.lang === 'he';
  const money = (agorot) => {
    const v = agorot / 100;
    return '₪' + v.toLocaleString('en-US', { minimumFractionDigits: Number.isInteger(v) ? 0 : 2, maximumFractionDigits: 2 });
  };

  // ---------------------------------------------------------------- 1. the logo follows the mouse (parallax by layer)
  const stage = document.getElementById('brandStage');
  const layers = stage ? [...stage.querySelectorAll('.ly')] : [];
  if (stage && fine) {
    let raf = 0;
    const move = (x, y) => {
      cancelAnimationFrame(raf);
      raf = requestAnimationFrame(() => layers.forEach(l => {
        const d = +l.dataset.depth || 0;
        l.style.translate = `${(x * d).toFixed(1)}px ${(y * d).toFixed(1)}px`;
      }));
    };
    document.querySelector('.hero2').addEventListener('pointermove', e => {
      if (calm()) return;
      const r = stage.getBoundingClientRect();
      move((e.clientX - (r.left + r.width / 2)) / r.width, (e.clientY - (r.top + r.height / 2)) / r.height);
    });
    document.querySelector('.hero2').addEventListener('pointerleave', () => move(0, 0));
    // a little "bounce" when the logo is clicked
    stage.addEventListener('click', () => {
      if (calm()) return;
      stage.classList.remove('boing'); void stage.offsetWidth; stage.classList.add('boing');
    });
  }

  // ---------------------------------------------------------------- 2. dates: check-out always after check-in
  const cin = document.getElementById('checkIn'), cout = document.getElementById('checkOut');
  if (cin && cout) {
    cin.addEventListener('change', () => {
      if (!cin.value) return;
      const next = new Date(cin.value + 'T00:00:00');
      next.setDate(next.getDate() + 1);
      const min = next.toISOString().slice(0, 10);
      cout.min = min;
      if (!cout.value || cout.value <= cin.value) {
        next.setDate(next.getDate() + 1);
        cout.value = next.toISOString().slice(0, 10);
      }
    });
  }

  // ---------------------------------------------------------------- 3. split calculator (same rule as the server: leftover agorot go to the first people)
  const calc = document.getElementById('calc');
  if (calc) {
    const total = document.getElementById('calcTotal');
    const totalOut = document.getElementById('calcTotalOut');
    const peopleOut = document.getElementById('calcPeople');
    const list = document.getElementById('calcShares');
    const upfront = document.getElementById('calcUpfront');
    const label = calc.dataset.person || (he ? 'משתתף' : 'Person');
    const suiteLabel = calc.dataset.suite || (he ? 'בסוויטה' : 'in the suite');
    let people = 4, mode = 'equal';

    const divide = (amount, weights) => {
      const sum = weights.reduce((a, b) => a + b, 0);
      const shares = weights.map(w => Math.floor(amount * w / sum));
      let left = amount - shares.reduce((a, b) => a + b, 0);
      for (let i = 0; left > 0; i = (i + 1) % shares.length, left--) shares[i]++;
      return shares;
    };
    const render = () => {
      const agorot = Math.round(+total.value * 100);
      totalOut.textContent = money(agorot);
      upfront.textContent = money(agorot);
      peopleOut.textContent = people;
      total.style.setProperty('--fill', ((total.value - total.min) / (total.max - total.min) * 100) + '%');
      const weights = Array.from({ length: people }, (_, i) => (mode === 'suite' && i === 0 ? 2 : 1));
      const shares = divide(agorot, weights);
      const max = Math.max(...shares);
      list.innerHTML = '';
      shares.forEach((s, i) => {
        const li = document.createElement('li');
        li.style.setProperty('--w', (s / max * 100).toFixed(1) + '%');
        li.style.setProperty('--i', i);
        const who = mode === 'suite' && i === 0 ? `${label} 1 · ${suiteLabel}` : `${label} ${i + 1}`;
        li.innerHTML = `<span class="avatar">${String.fromCharCode(65 + i)}</span><span class="who-name"></span><span class="bar-mini"><span></span></span><b dir="ltr">${money(s)}</b>`;
        li.querySelector('.who-name').textContent = who;
        list.appendChild(li);
      });
    };
    total.addEventListener('input', render);
    calc.querySelectorAll('[data-step]').forEach(b => b.addEventListener('click', () => {
      people = Math.min(12, Math.max(2, people + +b.dataset.step));
      peopleOut.classList.remove('bump'); void peopleOut.offsetWidth; peopleOut.classList.add('bump');
      render();
    }));
    calc.querySelectorAll('[data-mode]').forEach(b => b.addEventListener('click', () => {
      mode = b.dataset.mode;
      calc.querySelectorAll('[data-mode]').forEach(x => x.setAttribute('aria-checked', String(x === b)));
      render();
    }));
    render();
  }

  // ---------------------------------------------------------------- 4. live demo: four friends pay, the hotel confirms
  const demo = document.getElementById('demo');
  if (demo) {
    const ring = document.getElementById('demoRing');
    const pct = document.getElementById('demoPct');
    const status = document.getElementById('demoStatus');
    const items = [...demo.querySelectorAll('#demoPeople li')];
    const C = 2 * Math.PI * 52;
    ring.style.strokeDasharray = C;
    const initialStatus = status.textContent;
    let timers = [];
    const setPct = (p) => { ring.style.strokeDashoffset = C * (1 - p); pct.textContent = Math.round(p * 100) + '%'; };
    const reset = () => {
      timers.forEach(clearTimeout); timers = [];
      items.forEach(li => {
        li.classList.remove('paid');
        const pill = li.querySelector('.pill');
        pill.className = 'pill ms-pending'; pill.textContent = demo.dataset.waiting;
      });
      status.textContent = initialStatus; status.className = 'demo-status';
      demo.classList.remove('finished');
      setPct(0);
    };
    const play = () => {
      reset();
      const step = calm() ? 250 : 1100;
      items.forEach((li, i) => timers.push(setTimeout(() => {
        li.classList.add('paid');
        const pill = li.querySelector('.pill');
        pill.className = 'pill ms-authorized'; pill.textContent = demo.dataset.paid;
        setPct((i + 1) / items.length);
      }, step * (i + 1))));
      timers.push(setTimeout(() => {
        status.textContent = demo.dataset.confirmed; status.className = 'demo-status ok';
        demo.classList.add('finished');
        if (window.divistayConfetti) window.divistayConfetti(45);
      }, step * (items.length + 1)));
    };
    document.getElementById('demoReplay').addEventListener('click', play);
    setPct(0);
    if ('IntersectionObserver' in window) {
      const io = new IntersectionObserver(es => es.forEach(e => { if (e.isIntersecting) { play(); io.disconnect(); } }), { threshold: 0.45 });
      io.observe(demo);
    } else play();
  }

  // ---------------------------------------------------------------- 5. hotel cards tilt towards the mouse
  if (fine) {
    document.querySelectorAll('.tilt').forEach(card => {
      card.addEventListener('pointermove', e => {
        if (calm()) return;
        const r = card.getBoundingClientRect();
        const x = (e.clientX - r.left) / r.width - 0.5, y = (e.clientY - r.top) / r.height - 0.5;
        card.style.transform = `perspective(900px) rotateX(${(-y * 7).toFixed(2)}deg) rotateY(${(x * 9).toFixed(2)}deg) translateY(-6px)`;
        card.style.setProperty('--gx', ((x + 0.5) * 100).toFixed(1) + '%');
        card.style.setProperty('--gy', ((y + 0.5) * 100).toFixed(1) + '%');
      });
      card.addEventListener('pointerleave', () => { card.style.transform = ''; });
    });
  }

  // ---------------------------------------------------------------- 6. call-to-action jumps back to the search
  const cta = document.getElementById('ctaTop');
  if (cta) cta.addEventListener('click', e => {
    e.preventDefault();
    window.scrollTo({ top: 0, behavior: calm() ? 'auto' : 'smooth' });
    setTimeout(() => document.querySelector('.search3 input[name=city]')?.focus({ preventScroll: true }), calm() ? 0 : 600);
  });
})();
