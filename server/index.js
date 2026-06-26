'use strict';

require('dotenv').config();
const path = require('path');
const express = require('express');

const db = require('./db');
const { verifyTelegramInitData } = require('./auth');
const { EXERCISES, PROGRAM } = require('./program');

const PORT = process.env.PORT || 3000;
const BOT_TOKEN = process.env.BOT_TOKEN || '';
const DEV_MODE = process.env.DEV_MODE === '1';

const app = express();
app.use(express.json());

// ---- Підготовлені запити / Prepared statements ---------------------------
const q = {
  upsertUser: db.prepare(`
    INSERT INTO users (id, first_name, username)
    VALUES (@id, @first_name, @username)
    ON CONFLICT(id) DO UPDATE SET first_name = excluded.first_name,
                                  username   = excluded.username
  `),
  getUser: db.prepare('SELECT * FROM users WHERE id = ?'),
  updateUser: db.prepare('UPDATE users SET lang = @lang, unit = @unit WHERE id = @id'),

  insertWorkout: db.prepare('INSERT INTO workouts (user_id, date, notes) VALUES (?, ?, ?)'),
  insertEntry: db.prepare(`
    INSERT INTO entries (workout_id, exercise, weight, reps, sets, position)
    VALUES (@workout_id, @exercise, @weight, @reps, @sets, @position)
  `),
  listWorkouts: db.prepare('SELECT * FROM workouts WHERE user_id = ? ORDER BY date DESC, id DESC'),
  getWorkout: db.prepare('SELECT * FROM workouts WHERE id = ? AND user_id = ?'),
  listEntries: db.prepare('SELECT * FROM entries WHERE workout_id = ? ORDER BY position, id'),
  deleteWorkout: db.prepare('DELETE FROM workouts WHERE id = ? AND user_id = ?'),

  listRecords: db.prepare('SELECT exercise, value, date FROM records WHERE user_id = ?'),
  upsertRecord: db.prepare(`
    INSERT INTO records (user_id, exercise, value, date)
    VALUES (@user_id, @exercise, @value, @date)
    ON CONFLICT(user_id, exercise) DO UPDATE SET value = excluded.value, date = excluded.date
  `),
  deleteRecord: db.prepare('DELETE FROM records WHERE user_id = ? AND exercise = ?'),
};

// ---- Авторизація / Auth middleware ---------------------------------------
function authMiddleware(req, res, next) {
  const initData = req.get('X-Init-Data') || '';
  let tgUser = BOT_TOKEN ? verifyTelegramInitData(initData, BOT_TOKEN) : null;

  // У режимі розробки дозволяємо тестового користувача без Telegram.
  // In dev mode allow a test user without Telegram.
  if (!tgUser && DEV_MODE) {
    tgUser = { id: 1, first_name: 'Dev', username: 'dev' };
  }

  if (!tgUser) {
    return res.status(401).json({ error: 'unauthorized' });
  }

  q.upsertUser.run({
    id: tgUser.id,
    first_name: tgUser.first_name || '',
    username: tgUser.username || '',
  });
  req.user = q.getUser.get(tgUser.id);
  next();
}

const api = express.Router();
api.use(authMiddleware);

// Профіль / Profile
api.get('/me', (req, res) => res.json(req.user));

api.post('/me', (req, res) => {
  const lang = req.body.lang === 'en' ? 'en' : 'uk';
  const unit = req.body.unit === 'lb' ? 'lb' : 'kg';
  q.updateUser.run({ id: req.user.id, lang, unit });
  res.json(q.getUser.get(req.user.id));
});

// Каталог вправ та готова програма / Exercise catalog & built-in program
api.get('/exercises', (_req, res) => res.json(EXERCISES));
api.get('/program', (_req, res) => res.json(PROGRAM));

// Тренування / Workouts
api.get('/workouts', (req, res) => {
  const workouts = q.listWorkouts.all(req.user.id);
  for (const w of workouts) {
    w.entries = q.listEntries.all(w.id);
  }
  res.json(workouts);
});

api.post('/workouts', (req, res) => {
  const { date, notes, entries } = req.body;
  if (!date || !/^\d{4}-\d{2}-\d{2}$/.test(date)) {
    return res.status(400).json({ error: 'invalid_date' });
  }
  if (!Array.isArray(entries) || entries.length === 0) {
    return res.status(400).json({ error: 'no_entries' });
  }

  const tx = db.transaction(() => {
    const info = q.insertWorkout.run(req.user.id, date, notes || null);
    const workoutId = info.lastInsertRowid;
    entries.forEach((e, i) => {
      q.insertEntry.run({
        workout_id: workoutId,
        exercise: String(e.exercise || '').slice(0, 64),
        weight: Number(e.weight) || 0,
        reps: Math.max(1, parseInt(e.reps, 10) || 1),
        sets: Math.max(1, parseInt(e.sets, 10) || 1),
        position: i,
      });
    });
    return workoutId;
  });

  const id = tx();
  const workout = q.getWorkout.get(id, req.user.id);
  workout.entries = q.listEntries.all(id);
  res.status(201).json(workout);
});

api.delete('/workouts/:id', (req, res) => {
  const info = q.deleteWorkout.run(req.params.id, req.user.id);
  if (info.changes === 0) return res.status(404).json({ error: 'not_found' });
  res.json({ ok: true });
});

// Особисті рекорди / Personal records
api.get('/records', (req, res) => res.json(q.listRecords.all(req.user.id)));

api.put('/records/:exercise', (req, res) => {
  const value = Number(req.body.value);
  if (!(value > 0)) return res.status(400).json({ error: 'invalid_value' });
  const date = /^\d{4}-\d{2}-\d{2}$/.test(req.body.date)
    ? req.body.date
    : new Date().toISOString().slice(0, 10);
  q.upsertRecord.run({
    user_id: req.user.id,
    exercise: String(req.params.exercise).slice(0, 64),
    value,
    date,
  });
  res.json(q.listRecords.all(req.user.id));
});

api.delete('/records/:exercise', (req, res) => {
  q.deleteRecord.run(req.user.id, req.params.exercise);
  res.json(q.listRecords.all(req.user.id));
});

// Статистика та прогрес / Statistics & progress
api.get('/stats', (req, res) => {
  const workouts = q.listWorkouts.all(req.user.id);
  let totalTonnage = 0;
  const tonnageByDate = {};
  const bestByExercise = {};
  const progress = {}; // exercise -> [{date, maxWeight}]

  for (const w of workouts) {
    const entries = q.listEntries.all(w.id);
    let dayMax = {};
    for (const e of entries) {
      const volume = e.weight * e.reps * e.sets;
      totalTonnage += volume;
      tonnageByDate[w.date] = (tonnageByDate[w.date] || 0) + volume;
      if (!bestByExercise[e.exercise] || e.weight > bestByExercise[e.exercise]) {
        bestByExercise[e.exercise] = e.weight;
      }
      if (!dayMax[e.exercise] || e.weight > dayMax[e.exercise]) {
        dayMax[e.exercise] = e.weight;
      }
    }
    for (const [ex, mw] of Object.entries(dayMax)) {
      (progress[ex] = progress[ex] || []).push({ date: w.date, weight: mw });
    }
  }

  // Сортуємо точки прогресу за датою (зростання).
  for (const ex of Object.keys(progress)) {
    progress[ex].sort((a, b) => a.date.localeCompare(b.date));
  }

  res.json({
    workoutsCount: workouts.length,
    totalTonnage: Math.round(totalTonnage),
    tonnageByDate,
    bestByExercise,
    progress,
  });
});

app.use('/api', api);

// Статичний фронтенд / Static frontend
app.use(express.static(path.join(__dirname, '..', 'public')));

app.listen(PORT, () => {
  console.log(`✅ Сервер запущено / Server running: http://localhost:${PORT}`);
  if (!BOT_TOKEN) {
    console.log('⚠️  BOT_TOKEN не задано. Працює лише DEV_MODE у браузері.');
    console.log('⚠️  BOT_TOKEN is empty. Only DEV_MODE in a browser will work.');
  }
});
