// The hotel page: choose rooms by type, say how many people are coming, see who pays what.
// The same split rules as the server (leftover agorot go to the first people).
(() => {
  const form = document.getElementById('groupForm');
  if (!form) return;
  const $ = id => document.getElementById(id);
  const aside = document.querySelector('.summary');
  const heb = document.documentElement.lang === 'he';
  const money = agorot => {
    const v = agorot / 100;
    return '₪' + v.toLocaleString('en-US', { minimumFractionDigits: Number.isInteger(v) ? 0 : 2, maximumFractionDigits: 2 });
  };
  const bump = el => { el.classList.remove('bump'); void el.offsetWidth; el.classList.add('bump'); };
  const boxes = [...form.querySelectorAll('input[name=roomIds]')];
  const rows = [...document.querySelectorAll('.member-row')];
  const splitType = () => form.querySelector('input[name=splitType]:checked')?.value;

  // ---------------------------------------------------------------- 1. rooms: a counter per room type
  const types = [...document.querySelectorAll('.rtype')];
  const syncType = t => {
    const n = t.querySelectorAll('input[name=roomIds]:checked').length;
    const out = t.querySelector('.rt-count output');
    if (out.textContent !== String(n)) { out.textContent = n; bump(out); }
    t.classList.toggle('picked', n > 0);
    t.querySelector('[data-d="-1"]').disabled = n === 0;
    t.querySelector('[data-d="1"]').disabled = n >= +t.querySelector('.rt-count').dataset.max;
  };
  types.forEach(t => {
    t.querySelector('.rt-count').addEventListener('click', e => {
      const b = e.target.closest('[data-d]');
      if (!b) return;
      const all = [...t.querySelectorAll('input[name=roomIds]')];
      if (+b.dataset.d > 0) { const free = all.find(x => !x.checked); if (free) free.checked = true; }
      else { const last = all.filter(x => x.checked).pop(); if (last) last.checked = false; }
      syncType(t); update();
    });
    syncType(t);
  });

  // ---------------------------------------------------------------- 3. people: choose how many, one row each, × removes
  const peopleOut = $('peopleOut'), peopleInput = $('peopleInput'), counter = $('peopleCounter');
  const max = rows.length;
  let count = Math.max(1, Math.min(max, +peopleInput.value || 1));
  // rows that already have something in them (after a server error) are never hidden
  const filled = rows.reduce((n, r, i) => [...r.querySelectorAll('input')].some(x => x.value.trim()) ? i + 1 : n, 0);
  count = Math.max(count, filled);

  const showRows = (focusNew) => {
    rows.forEach((r, i) => {
      const was = r.hidden;
      r.hidden = i >= count;
      r.querySelectorAll('input,select').forEach(el => el.disabled = r.hidden);  // hidden rows are not sent
      if (was && !r.hidden && focusNew) { r.classList.add('pop-in'); }
    });
    peopleOut.textContent = count; peopleInput.value = count;
    counter.querySelector('[data-d="-1"]').disabled = count <= 1;
    counter.querySelector('[data-d="1"]').disabled = count >= max;
    rows.forEach((r, i) => r.querySelector('.row-remove').hidden = count <= 1);
  };
  counter.addEventListener('click', e => {
    const b = e.target.closest('[data-d]');
    if (!b) return;
    const before = count;
    count = Math.max(1, Math.min(max, count + +b.dataset.d));
    if (count < before) rows[count].querySelectorAll('input,select').forEach(el => el.value = '');
    showRows(true); bump(peopleOut);
    if (count > before) rows[count - 1].querySelector('input').focus();
    update();
  });
  rows.forEach((r, i) => r.querySelector('.row-remove').addEventListener('click', () => {
    if (count <= 1) return;
    // move the following rows up so there are no gaps, then drop the last one
    for (let k = i; k < count - 1; k++) {
      const a = rows[k].querySelectorAll('input,select'), b = rows[k + 1].querySelectorAll('input,select');
      a.forEach((el, j) => el.value = b[j].value);
    }
    rows[count - 1].querySelectorAll('input,select').forEach(el => el.value = '');
    count--;
    if (i === 0) delete rows[0].querySelector('input').dataset.touched;
    showRows(false); bump(peopleOut);
    (rows[Math.min(i, count - 1)].querySelector('input')).focus();
    update();
  }));

  // the organizer is usually the first person paying
  const first = rows[0].querySelectorAll('input');
  [first[0], first[1]].forEach(i => i.addEventListener('input', () => i.dataset.touched = '1'));
  const copy = (from, to) => from.addEventListener('input', () => { if (!to.dataset.touched) { to.value = from.value; update(); } });
  copy($('orgName'), first[0]);
  copy($('orgEmail'), first[1]);
  if (!first[0].value && $('orgName').value) first[0].value = $('orgName').value;
  if (!first[1].value && $('orgEmail').value) first[1].value = $('orgEmail').value;

  // ---------------------------------------------------------------- 4. split
  const syncColumns = () => {
    form.classList.toggle('show-room', splitType() === 'ROOM_BASED');
    form.classList.toggle('show-amount', splitType() === 'CUSTOM');
    const hint = $('splitHint');
    hint.textContent = { EQUAL: aside.dataset.hintEqual, ROOM_BASED: aside.dataset.hintRoom, CUSTOM: aside.dataset.hintCustom }[splitType()] || '';
  };
  const divide = (amount, n) => {
    const base = Math.floor(amount / n), shares = Array(n).fill(base);
    for (let i = 0; i < amount - base * n; i++) shares[i]++;
    return shares;
  };
  const parseAmount = t => {
    const v = parseFloat(String(t).replace(/[₪,\s]/g, ''));
    return isFinite(v) && v > 0 ? Math.round(v * 100) : 0;
  };
  const isEmail = v => /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(v.trim());

  // ---------------------------------------------------------------- live summary + step dots
  const visibleRows = () => rows.slice(0, count);
  const update = () => {
    const chosen = boxes.filter(b => b.checked);
    let total = 0, beds = 0;
    const roomList = $('sumRooms');
    roomList.innerHTML = '';
    chosen.forEach(b => {
      total += +b.dataset.cost; beds += +b.dataset.capacity;
      const li = document.createElement('li');
      li.innerHTML = '<span></span><b dir="ltr"></b>';
      li.firstChild.textContent = b.dataset.label;
      li.lastChild.textContent = money(+b.dataset.cost);
      roomList.appendChild(li);
    });
    if (!chosen.length) roomList.innerHTML = `<li class="muted">${roomList.dataset.empty}</li>`;
    const t = $('total'); if (t.textContent !== money(total)) { t.textContent = money(total); bump(t); }
    $('capacity').textContent = beds;
    const st = $('sumTotal'); if (st.textContent !== money(total)) { st.textContent = money(total); bump(st); }

    // keep each room select limited to the rooms that are chosen
    rows.forEach(r => r.querySelectorAll('select option[value]').forEach(o => {
      if (o.value) o.hidden = !chosen.some(b => b.value === o.value);
    }));

    const ppl = visibleRows();
    $('bedsText').textContent = `${ppl.length} / ${beds}`;
    $('bedsBar').style.width = (beds ? Math.min(100, ppl.length / beds * 100) : 0) + '%';
    const over = ppl.length > beds && beds > 0;
    $('bedsBar').classList.toggle('over', over);
    $('bedsWarn').hidden = !over;

    const list = $('sumShares'), note = $('sumNote');
    list.innerHTML = ''; note.textContent = ''; note.className = 'sum-note';
    const label = (r, i) => r.querySelector('[name$=".name"]').value.trim() || r.querySelector('[name$=".email"]').value.trim() || `${aside.dataset.person} ${i + 1}`;
    let shares = [], splitOk = !!splitType();
    if (ppl.length && total) {
      if (splitType() === 'EQUAL') {
        shares = divide(total, ppl.length);
      } else if (splitType() === 'ROOM_BASED') {
        const byRoom = {};
        ppl.forEach((r, i) => { const id = r.querySelector('select').value; (byRoom[id] = byRoom[id] || []).push(i); });
        shares = ppl.map(() => null);
        chosen.forEach(b => {
          const who = byRoom[b.value] || [];
          divide(+b.dataset.cost, Math.max(1, who.length)).forEach((s, k) => { if (who[k] !== undefined) shares[who[k]] = s; });
        });
        const missing = chosen.some(b => !(byRoom[b.value] || []).length) || ppl.some(r => !r.querySelector('select').value);
        if (missing) { note.textContent = aside.dataset.roomNeeded; note.classList.add('warn'); splitOk = false; }
      } else if (splitType() === 'CUSTOM') {
        shares = ppl.map(r => parseAmount(r.querySelector('[name$=".amount"]').value));
        const diff = total - shares.reduce((a, b) => a + b, 0);
        note.textContent = diff === 0 ? aside.dataset.exact : (diff > 0 ? `${aside.dataset.left} ${money(diff)}` : `${aside.dataset.over} ${money(-diff)}`);
        note.classList.add(diff === 0 ? 'ok' : 'warn');
        splitOk = diff === 0;
      }
      ppl.forEach((r, i) => {
        const li = document.createElement('li');
        li.innerHTML = '<span class="avatar"></span><span class="nm"></span><b dir="ltr"></b>';
        const name = label(r, i);
        li.querySelector('.avatar').textContent = name.charAt(0).toUpperCase();
        li.querySelector('.nm').textContent = name;
        li.querySelector('b').textContent = shares[i] ? money(shares[i]) : '—';
        list.appendChild(li);
      });
    }

    const orgOk = $('orgName').value.trim() && isEmail($('orgEmail').value);
    const peopleOk = ppl.length > 0 && ppl.every(r => isEmail(r.querySelector('[name$=".email"]').value));
    const checks = { rooms: chosen.length > 0 && !over, organizer: !!orgOk, people: peopleOk, split: splitOk && chosen.length > 0 && peopleOk };
    document.querySelectorAll('#checklist [data-check]').forEach(li => li.classList.toggle('done', !!checks[li.dataset.check]));
    // the dots: done ✓, or "current" for the first step that is not done yet
    let current = Object.keys(checks).find(k => !checks[k]);
    document.querySelectorAll('[data-step]').forEach(el => {
      const k = el.dataset.step;
      el.classList.toggle('is-done', !!checks[k]);
      el.classList.toggle('is-current', k === current);
      if (el.tagName === 'A') el.setAttribute('aria-current', k === current ? 'step' : 'false');
    });
  };

  form.addEventListener('input', update);
  form.addEventListener('change', e => {
    if (e.target.name === 'roomIds') syncType(e.target.closest('.rtype'));
    syncColumns(); update();
  });
  showRows(false);
  syncColumns();
  update();
})();
