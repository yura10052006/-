'use strict';

// Каталог вправ важкої атлетики. Ключ -> назви двома мовами.
// Weightlifting exercise catalog. key -> names in both languages.
const EXERCISES = [
  { key: 'snatch',            uk: 'Ривок',                      en: 'Snatch' },
  { key: 'power_snatch',      uk: 'Ривок у стійку',             en: 'Power Snatch' },
  { key: 'clean_and_jerk',    uk: 'Поштовх',                    en: 'Clean & Jerk' },
  { key: 'clean',             uk: 'Взяття на груди',            en: 'Clean' },
  { key: 'power_clean',       uk: 'Взяття у стійку',            en: 'Power Clean' },
  { key: 'jerk',              uk: 'Поштовх від грудей',         en: 'Jerk' },
  { key: 'front_squat',       uk: 'Присід зі штангою спереду',  en: 'Front Squat' },
  { key: 'back_squat',        uk: 'Присід зі штангою на спині', en: 'Back Squat' },
  { key: 'overhead_squat',    uk: 'Присід зі штангою над головою', en: 'Overhead Squat' },
  { key: 'snatch_pull',       uk: 'Ривкова тяга',               en: 'Snatch Pull' },
  { key: 'clean_pull',        uk: 'Поштовхова тяга',            en: 'Clean Pull' },
  { key: 'deadlift',          uk: 'Станова тяга',               en: 'Deadlift' },
  { key: 'push_press',        uk: 'Швунг жимовий',              en: 'Push Press' },
  { key: 'press',             uk: 'Жим стоячи',                 en: 'Overhead Press' },
];

// Готова базова програма для важкоатлетів: 4 тижні, 3 дні/тиждень.
// Навантаження вказано у % від одноповторного максимуму (1ПМ) відповідної вправи.
// Built-in weightlifting program: 4 weeks, 3 days/week. Load is % of 1RM.
const PROGRAM = {
  id: 'classic_4w',
  name: { uk: 'Класичний цикл (4 тижні)', en: 'Classic Cycle (4 weeks)' },
  description: {
    uk: 'Базовий 4-тижневий цикл для важкоатлетів: ривок, поштовх, присіди та тяги. ' +
        'Ваги розраховуються від вашого 1ПМ. Останній тиждень — розвантаження.',
    en: 'A base 4-week weightlifting cycle: snatch, clean & jerk, squats and pulls. ' +
        'Weights are based on your 1RM. The last week is a deload.',
  },
  weeks: [
    {
      week: 1,
      label: { uk: 'Тиждень 1 — обʼєм', en: 'Week 1 — Volume' },
      days: [
        { day: { uk: 'День 1', en: 'Day 1' }, items: [
          { exercise: 'snatch',         sets: 5, reps: 3, percent: 70 },
          { exercise: 'clean_and_jerk', sets: 5, reps: 2, percent: 70 },
          { exercise: 'back_squat',     sets: 4, reps: 5, percent: 75 },
        ]},
        { day: { uk: 'День 2', en: 'Day 2' }, items: [
          { exercise: 'power_snatch',   sets: 5, reps: 3, percent: 65 },
          { exercise: 'clean_pull',     sets: 4, reps: 4, percent: 90 },
          { exercise: 'front_squat',    sets: 4, reps: 4, percent: 70 },
        ]},
        { day: { uk: 'День 3', en: 'Day 3' }, items: [
          { exercise: 'snatch',         sets: 4, reps: 2, percent: 75 },
          { exercise: 'clean_and_jerk', sets: 4, reps: 2, percent: 75 },
          { exercise: 'snatch_pull',    sets: 4, reps: 4, percent: 90 },
        ]},
      ],
    },
    {
      week: 2,
      label: { uk: 'Тиждень 2 — інтенсивність', en: 'Week 2 — Intensity' },
      days: [
        { day: { uk: 'День 1', en: 'Day 1' }, items: [
          { exercise: 'snatch',         sets: 5, reps: 2, percent: 78 },
          { exercise: 'clean_and_jerk', sets: 5, reps: 1, percent: 80 },
          { exercise: 'back_squat',     sets: 5, reps: 3, percent: 82 },
        ]},
        { day: { uk: 'День 2', en: 'Day 2' }, items: [
          { exercise: 'power_clean',    sets: 5, reps: 2, percent: 72 },
          { exercise: 'push_press',     sets: 4, reps: 3, percent: 75 },
          { exercise: 'front_squat',    sets: 4, reps: 3, percent: 78 },
        ]},
        { day: { uk: 'День 3', en: 'Day 3' }, items: [
          { exercise: 'snatch',         sets: 5, reps: 1, percent: 82 },
          { exercise: 'clean_and_jerk', sets: 5, reps: 1, percent: 82 },
          { exercise: 'clean_pull',     sets: 4, reps: 3, percent: 95 },
        ]},
      ],
    },
    {
      week: 3,
      label: { uk: 'Тиждень 3 — пік', en: 'Week 3 — Peak' },
      days: [
        { day: { uk: 'День 1', en: 'Day 1' }, items: [
          { exercise: 'snatch',         sets: 4, reps: 1, percent: 85 },
          { exercise: 'clean_and_jerk', sets: 4, reps: 1, percent: 85 },
          { exercise: 'back_squat',     sets: 4, reps: 2, percent: 88 },
        ]},
        { day: { uk: 'День 2', en: 'Day 2' }, items: [
          { exercise: 'power_snatch',   sets: 4, reps: 2, percent: 75 },
          { exercise: 'jerk',           sets: 4, reps: 2, percent: 80 },
          { exercise: 'front_squat',    sets: 3, reps: 2, percent: 85 },
        ]},
        { day: { uk: 'День 3', en: 'Day 3' }, items: [
          { exercise: 'snatch',         sets: 3, reps: 1, percent: 90 },
          { exercise: 'clean_and_jerk', sets: 3, reps: 1, percent: 90 },
          { exercise: 'snatch_pull',    sets: 3, reps: 3, percent: 100 },
        ]},
      ],
    },
    {
      week: 4,
      label: { uk: 'Тиждень 4 — розвантаження', en: 'Week 4 — Deload' },
      days: [
        { day: { uk: 'День 1', en: 'Day 1' }, items: [
          { exercise: 'snatch',         sets: 3, reps: 2, percent: 60 },
          { exercise: 'clean_and_jerk', sets: 3, reps: 2, percent: 60 },
          { exercise: 'back_squat',     sets: 3, reps: 4, percent: 65 },
        ]},
        { day: { uk: 'День 2', en: 'Day 2' }, items: [
          { exercise: 'power_snatch',   sets: 3, reps: 3, percent: 55 },
          { exercise: 'front_squat',    sets: 3, reps: 3, percent: 60 },
        ]},
        { day: { uk: 'День 3 (контроль)', en: 'Day 3 (test)' }, items: [
          { exercise: 'snatch',         sets: 1, reps: 1, percent: 95 },
          { exercise: 'clean_and_jerk', sets: 1, reps: 1, percent: 95 },
        ]},
      ],
    },
  ],
};

module.exports = { EXERCISES, PROGRAM };
