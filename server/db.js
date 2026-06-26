'use strict';

const path = require('path');
const fs = require('fs');
const Database = require('better-sqlite3');

// База даних зберігається у файлі data/tracker.db
// The database is stored in the file data/tracker.db
const dataDir = path.join(__dirname, '..', 'data');
if (!fs.existsSync(dataDir)) {
  fs.mkdirSync(dataDir, { recursive: true });
}

const db = new Database(path.join(dataDir, 'tracker.db'));
db.pragma('journal_mode = WAL');
db.pragma('foreign_keys = ON');

// Схема бази даних / Database schema
db.exec(`
  CREATE TABLE IF NOT EXISTS users (
    id          INTEGER PRIMARY KEY,            -- Telegram user id
    first_name  TEXT,
    username    TEXT,
    lang        TEXT NOT NULL DEFAULT 'uk',
    unit        TEXT NOT NULL DEFAULT 'kg',
    created_at  TEXT NOT NULL DEFAULT (datetime('now'))
  );

  CREATE TABLE IF NOT EXISTS workouts (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date        TEXT NOT NULL,                  -- YYYY-MM-DD
    notes       TEXT,
    created_at  TEXT NOT NULL DEFAULT (datetime('now'))
  );

  CREATE TABLE IF NOT EXISTS entries (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    workout_id  INTEGER NOT NULL REFERENCES workouts(id) ON DELETE CASCADE,
    exercise    TEXT NOT NULL,                  -- ключ вправи / exercise key
    weight      REAL NOT NULL DEFAULT 0,        -- вага в кг / weight in kg
    reps        INTEGER NOT NULL DEFAULT 1,     -- повторення / reps
    sets        INTEGER NOT NULL DEFAULT 1,     -- підходи / sets
    position    INTEGER NOT NULL DEFAULT 0
  );

  CREATE TABLE IF NOT EXISTS records (
    user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exercise    TEXT NOT NULL,
    value       REAL NOT NULL,                  -- 1ПМ в кг / 1RM in kg
    date        TEXT NOT NULL,
    PRIMARY KEY (user_id, exercise)
  );

  CREATE INDEX IF NOT EXISTS idx_workouts_user ON workouts(user_id, date);
  CREATE INDEX IF NOT EXISTS idx_entries_workout ON entries(workout_id);
`);

module.exports = db;
