'use strict';

// ---- Telegram WebApp --------------------------------------------------------
const tg = window.Telegram && window.Telegram.WebApp ? window.Telegram.WebApp : null;
if (tg) {
  tg.ready();
  tg.expand();
}
const INIT_DATA = tg ? tg.initData : '';

// ---- Стан / State -----------------------------------------------------------
const state = {
  lang: 'uk',
  exercises: [],
  exMap: {},      // key -> {uk, en}
  workouts: [],
  records: [],    // [{exercise, value, date}]
  recMap: {},     // key -> value
  stats: null,
  program: null,
  tab: 'log',
};

// ---- Хелпери / Helpers ------------------------------------------------------
const $ = (sel, root = document) => root.querySelector(sel);
const el = (tag, props = {}, ...kids) => {
  const n = document.createElement(tag);
  Object.entries(props).forEach(([k, v]) => {
    if (k === 'class') n.className = v;
    else if (k === 'html') n.innerHTML = v;
    else if (k.startsWith('on') && typeof v === 'function') n.addEventListener(k.slice(2), v);
    else if (v !== null && v !== undefined) n.setAttribute(k, v);
  });
  kids.flat().forEach((c) => n.append(c.nodeType ? c : document.createTextNode(c)));
  return n;
};
const t = (key) => (window.I18N[state.lang] && window.I18N[state.lang][key]) || key;
const exName = (key) => (state.exMap[key] ? state.exMap[key][state.lang] : key);
const today = () => new Date().toISOString().slice(0, 10);

async function api(path, options = {}) {
  const res = await fetch('/api' + path, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'X-Init-Data': INIT_DATA,
      ...(options.headers || {}),
    },
  });
  if (!res.ok) throw new Error('API ' + res.status);
  return res.json();
}

let toastTimer;
function toast(msg) {
  const box = $('#toast');
  box.textContent = msg;
  box.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => box.classList.remove('show'), 1800);
}

// Формула Еплі: 1ПМ = вага * (1 + повтори/30)
function epley1RM(weight, reps) {
  return reps <= 1 ? weight : weight * (1 + reps / 30);
}

// ---- Завантаження даних / Data loading -------------------------------------
async function loadAll() {
  const [me, exercises, program] = await Promise.all([
    api('/me'),
    api('/exercises'),
    api('/program'),
  ]);
  state.lang = me.lang || 'uk';
  state.exercises = exercises;
  state.exMap = Object.fromEntries(exercises.map((e) => [e.key, e]));
  state.program = program;
  await refreshUserData();
}

async function refreshUserData() {
  const [workouts, records, stats] = await Promise.all([
    api('/workouts'),
    api('/records'),
    api('/stats'),
  ]);
  state.workouts = workouts;
  state.records = records;
  state.recMap = Object.fromEntries(records.map((r) => [r.exercise, r.value]));
  state.stats = stats;
}

// ---- Переклад статичних елементів / Apply translations ----------------------
function applyI18n() {
  document.documentElement.lang = state.lang;
  $('#appTitle').textContent = t('appTitle');
  document.querySelectorAll('[data-i18n]').forEach((n) => {
    n.textContent = t(n.dataset.i18n);
  });
}

// ============================================================================
//  ВКЛАДКИ / TABS
// ============================================================================
const screen = () => $('#screen');

function render() {
  applyI18n();
  document.querySelectorAll('.tab').forEach((b) =>
    b.classList.toggle('active', b.dataset.tab === state.tab)
  );
  const map = { log: renderLog, program: renderProgram, progress: renderProgress, records: renderRecords, calc: renderCalc };
  screen().innerHTML = '';
  (map[state.tab] || renderLog)();
}

// ---- 1. ЖУРНАЛ / LOG --------------------------------------------------------
function renderLog() {
  const s = screen();
  s.append(el('button', { class: 'btn', onclick: openWorkoutForm }, t('addWorkout')));

  if (state.workouts.length === 0) {
    s.append(el('div', { class: 'empty' }, t('noWorkouts')));
    return;
  }

  for (const w of state.workouts) {
    const card = el('div', { class: 'card workout' });
    const head = el('div', { class: 'w-head' },
      el('span', { class: 'w-date' }, w.date),
      el('button', { class: 'btn danger small', onclick: () => deleteWorkout(w.id) }, '🗑')
    );
    card.append(head);
    if (w.notes) card.append(el('div', { class: 'hint', style: 'margin:4px 0' }, w.notes));
    for (const e of w.entries) {
      const vol = e.weight * e.reps * e.sets;
      card.append(el('div', { class: 'w-entry' },
        el('span', {}, exName(e.exercise)),
        el('span', {},
          el('span', { class: 'tag' }, `${e.sets}×${e.reps} · ${e.weight}${t('kg')}`),
          el('span', { class: 'vol', style: 'margin-left:8px' }, `${Math.round(vol)} ${t('kg')}`)
        )
      ));
    }
    s.append(card);
  }
}

function openWorkoutForm() {
  const s = screen();
  s.innerHTML = '';
  const form = el('div', { class: 'card' });
  form.append(el('h2', {}, t('addWorkout')));

  form.append(el('label', {}, t('date')));
  const dateInput = el('input', { type: 'date', value: today() });
  form.append(dateInput);

  form.append(el('label', {}, t('notes')));
  const notesInput = el('input', { type: 'text', placeholder: t('notes') });
  form.append(notesInput);

  form.append(el('label', {}, t('exercise')));
  const list = el('div', {});
  form.append(list);

  const addRow = () => {
    const select = el('select', {});
    state.exercises.forEach((ex) =>
      select.append(el('option', { value: ex.key }, ex[state.lang]))
    );
    const wInput = el('input', { type: 'number', step: '0.5', min: '0', value: '0' });
    const rInput = el('input', { type: 'number', min: '1', value: '3' });
    const sInput = el('input', { type: 'number', min: '1', value: '5' });
    const row = el('div', { class: 'entry-row' },
      el('div', {}, el('span', { class: 'mini-label' }, t('exercise')), select),
      el('div', {}, el('span', { class: 'mini-label' }, t('weight')), wInput),
      el('div', {}, el('span', { class: 'mini-label' }, t('reps')), rInput),
      el('div', {}, el('span', { class: 'mini-label' }, t('sets')), sInput),
      el('button', { class: 'del', onclick: () => row.remove() }, '×')
    );
    list.append(row);
  };
  addRow();

  form.append(el('button', { class: 'btn ghost', onclick: addRow }, t('addExercise')));

  form.append(el('button', { class: 'btn', onclick: async () => {
    const entries = [...list.querySelectorAll('.entry-row')].map((row) => {
      const [sel, w, r, st] = row.querySelectorAll('select, input');
      return { exercise: sel.value, weight: parseFloat(w.value) || 0, reps: parseInt(r.value) || 1, sets: parseInt(st.value) || 1 };
    });
    if (entries.length === 0) return toast(t('fillExercises'));
    try {
      await api('/workouts', { method: 'POST', body: JSON.stringify({ date: dateInput.value, notes: notesInput.value, entries }) });
      await refreshUserData();
      toast(t('workoutSaved'));
      state.tab = 'log';
      render();
    } catch (e) { toast(t('loadError')); }
  } }, t('save')));

  form.append(el('button', { class: 'btn secondary', onclick: render }, t('cancel')));
  s.append(form);
}

async function deleteWorkout(id) {
  if (tg && tg.showConfirm) {
    tg.showConfirm(t('confirmDelete'), async (ok) => { if (ok) await doDelete(id); });
  } else if (confirm(t('confirmDelete'))) {
    await doDelete(id);
  }
}
async function doDelete(id) {
  await api('/workouts/' + id, { method: 'DELETE' });
  await refreshUserData();
  render();
}

// ---- 2. ПРОГРАМА / PROGRAM --------------------------------------------------
function renderProgram() {
  const s = screen();
  const p = state.program;
  const head = el('div', { class: 'card' });
  head.append(el('h2', {}, p.name[state.lang]));
  head.append(el('div', { class: 'hint' }, p.description[state.lang]));
  head.append(el('div', { class: 'hint', style: 'margin-top:8px' }, t('programHint')));
  s.append(head);

  for (const week of p.weeks) {
    const block = el('div', { class: 'week-block' });
    block.append(el('div', { class: 'week-title' }, week.label[state.lang]));
    for (const day of week.days) {
      const d = el('div', { class: 'day-block' });
      d.append(el('div', { class: 'day-title' }, day.day[state.lang]));
      for (const item of day.items) {
        const rm = state.recMap[item.exercise];
        const calc = rm ? `${Math.round((rm * item.percent) / 100)} ${t('kg')}` : `— ${t('kg')}`;
        d.append(el('div', { class: 'prog-item' },
          el('span', {}, `${exName(item.exercise)} · ${item.sets}×${item.reps} · ${item.percent}%`),
          el('span', { class: 'calc' }, calc)
        ));
      }
      block.append(d);
    }
    s.append(block);
  }
}

// ---- 3. ПРОГРЕС / PROGRESS --------------------------------------------------
function renderProgress() {
  const s = screen();
  const st = state.stats;

  const grid = el('div', { class: 'card stat-grid' },
    el('div', { class: 'stat' }, el('div', { class: 'num' }, String(st.workoutsCount)), el('div', { class: 'lbl' }, t('totalWorkouts'))),
    el('div', { class: 'stat' }, el('div', { class: 'num' }, String(st.totalTonnage)), el('div', { class: 'lbl' }, `${t('totalTonnage')} (${t('kg')})`))
  );
  s.append(grid);

  // Тоннаж по днях / Tonnage by day
  const dates = Object.keys(st.tonnageByDate).sort();
  if (dates.length > 0) {
    const card = el('div', { class: 'card' });
    card.append(el('h3', {}, t('tonnageOverTime')));
    const max = Math.max(...dates.map((d) => st.tonnageByDate[d]));
    dates.slice(-12).forEach((d) => {
      const v = st.tonnageByDate[d];
      card.append(el('div', { class: 'bar' },
        el('span', { class: 'name' }, d.slice(5)),
        el('span', { class: 'track' }, el('span', { class: 'fill', style: `width:${(v / max) * 100}%` })),
        el('span', { class: 'val' }, `${Math.round(v)}`)
      ));
    });
    s.append(card);
  }

  // Прогрес у вправі / Exercise progress
  const card = el('div', { class: 'card' });
  card.append(el('h3', {}, t('exerciseProgress')));
  const sel = el('select', {});
  const exWithData = Object.keys(st.progress);
  if (exWithData.length === 0) {
    card.append(el('div', { class: 'empty' }, t('noData')));
    s.append(card);
    return;
  }
  exWithData.forEach((k) => sel.append(el('option', { value: k }, exName(k))));
  card.append(sel);
  const chart = el('div', { style: 'margin-top:10px' });
  card.append(chart);

  const drawChart = () => {
    chart.innerHTML = '';
    const points = st.progress[sel.value];
    const max = Math.max(...points.map((p) => p.weight));
    points.slice(-12).forEach((p) => {
      chart.append(el('div', { class: 'bar' },
        el('span', { class: 'name' }, p.date.slice(5)),
        el('span', { class: 'track' }, el('span', { class: 'fill', style: `width:${(p.weight / max) * 100}%` })),
        el('span', { class: 'val' }, `${p.weight} ${t('kg')}`)
      ));
    });
  };
  sel.addEventListener('change', drawChart);
  drawChart();
  s.append(card);
}

// ---- 4. РЕКОРДИ / RECORDS ---------------------------------------------------
function renderRecords() {
  const s = screen();
  const card = el('div', { class: 'card' });
  card.append(el('h2', {}, t('yourRecords')));

  const sel = el('select', {});
  state.exercises.forEach((ex) => sel.append(el('option', { value: ex.key }, ex[state.lang])));
  const valInput = el('input', { type: 'number', step: '0.5', min: '0', placeholder: t('recordValue') });

  card.append(el('label', {}, t('exercise')));
  card.append(sel);
  card.append(el('label', {}, t('recordValue')));
  card.append(valInput);
  card.append(el('button', { class: 'btn', onclick: async () => {
    const value = parseFloat(valInput.value);
    if (!(value > 0)) return;
    await api('/records/' + sel.value, { method: 'PUT', body: JSON.stringify({ value, date: today() }) });
    await refreshUserData();
    valInput.value = '';
    render();
  } }, t('saveRecord')));
  s.append(card);

  if (state.records.length === 0) {
    s.append(el('div', { class: 'empty' }, t('noRecords')));
    return;
  }
  const list = el('div', { class: 'card' });
  state.records
    .slice()
    .sort((a, b) => b.value - a.value)
    .forEach((r) => {
      list.append(el('div', { class: 'w-entry' },
        el('span', {}, exName(r.exercise)),
        el('span', {},
          el('span', { class: 'tag' }, `${r.value} ${t('kg')}`),
          el('button', { class: 'del', style: 'margin-left:8px', onclick: async () => {
            await api('/records/' + r.exercise, { method: 'DELETE' });
            await refreshUserData();
            render();
          } }, '×')
        )
      ));
    });
  s.append(list);
}

// ---- 5. КАЛЬКУЛЯТОР / CALCULATOR --------------------------------------------
function renderCalc() {
  const s = screen();
  const card = el('div', { class: 'card' });
  card.append(el('h2', {}, t('calcTitle')));

  const wInput = el('input', { type: 'number', step: '0.5', min: '0', value: '100' });
  const rInput = el('input', { type: 'number', min: '1', value: '3' });
  card.append(el('label', {}, t('calcWeight')));
  card.append(wInput);
  card.append(el('label', {}, t('calcReps')));
  card.append(rInput);

  const result = el('div', { class: 'stat', style: 'margin-top:14px' });
  card.append(result);

  const table = el('div', { style: 'margin-top:14px' });
  card.append(table);

  const recompute = () => {
    const w = parseFloat(wInput.value) || 0;
    const r = parseInt(rInput.value) || 1;
    const orm = epley1RM(w, r);
    result.innerHTML = '';
    result.append(
      el('div', { class: 'num' }, `${orm.toFixed(1)} ${t('kg')}`),
      el('div', { class: 'lbl' }, `${t('calc1rm')} · ${t('calcFormulaNote')}`)
    );
    table.innerHTML = '';
    table.append(el('h3', {}, t('calcPercentTable')));
    [95, 90, 85, 80, 75, 70, 65, 60].forEach((pct) => {
      table.append(el('div', { class: 'bar' },
        el('span', { class: 'name' }, `${pct}%`),
        el('span', { class: 'track' }, el('span', { class: 'fill', style: `width:${pct}%` })),
        el('span', { class: 'val' }, `${((orm * pct) / 100).toFixed(1)} ${t('kg')}`)
      ));
    });
  };
  wInput.addEventListener('input', recompute);
  rInput.addEventListener('input', recompute);
  recompute();
  s.append(card);
}

// ---- Перемикання мови / Language toggle -------------------------------------
async function toggleLang() {
  state.lang = state.lang === 'uk' ? 'en' : 'uk';
  try { await api('/me', { method: 'POST', body: JSON.stringify({ lang: state.lang }) }); } catch {}
  render();
}

// ---- Старт / Bootstrap ------------------------------------------------------
document.querySelectorAll('.tab').forEach((b) =>
  b.addEventListener('click', () => { state.tab = b.dataset.tab; render(); })
);
$('#langBtn').addEventListener('click', toggleLang);

(async () => {
  try {
    await loadAll();
    render();
  } catch (e) {
    console.error(e);
    screen().innerHTML = '';
    screen().append(el('div', { class: 'empty' }, t('loadError')));
  }
})();
