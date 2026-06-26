# 🏋️ Трекер важкоатлета / Weightlifting Tracker

Telegram Mini App — трекер тренувань для важкоатлетів з готовою програмою,
журналом тренувань, обліком прогресу, особистими рекордами та калькулятором 1ПМ.

Telegram Mini App — a training tracker for weightlifters with a built-in program,
workout log, progress tracking, personal records and a 1RM calculator.

---

## ✨ Можливості / Features

- 📒 **Журнал тренувань** — записуйте вправи: вага, повтори, підходи, дата, нотатки.
- 📋 **Готова програма** — 4-тижневий цикл (ривок, поштовх, присіди, тяги).
  Робочі ваги автоматично рахуються від ваших рекордів (1ПМ).
- 📈 **Прогрес** — загальний тоннаж, кількість тренувань, графіки по вправах.
- 🏅 **Рекорди** — особисті максимуми (1ПМ) по кожній вправі.
- 🧮 **Калькулятор** — розрахунок 1ПМ і робочих ваг у відсотках (формула Еплі).
- 🌍 **Дві мови** — українська та англійська (перемикач у верхньому правому куті).

---

## 🚀 Як запустити / How to run

### 1. Встановіть залежності / Install dependencies
```bash
npm install
```

### 2. Налаштуйте `.env` / Configure `.env`
```bash
cp .env.example .env
```
Для першого тесту нічого змінювати не треба — `DEV_MODE=1` дозволяє
відкривати додаток у звичайному браузері.

For a first test you don't need to change anything — `DEV_MODE=1`
lets you open the app in a regular browser.

### 3. Запустіть сервер / Start the server
```bash
npm start
```
Відкрийте в браузері / Open in a browser: **http://localhost:3000**

> У режимі розробки використовується тестовий користувач, тож ви одразу
> побачите інтерфейс і зможете все спробувати.

---

## 📱 Підключення до Telegram / Connecting to Telegram

Коли захочете запустити саме як Telegram Mini App:

1. Створіть бота у [@BotFather](https://t.me/BotFather) → отримаєте **токен**.
2. Впишіть токен у `.env`:
   ```
   BOT_TOKEN=ваш_токен_тут
   DEV_MODE=0
   ```
3. Викладіть додаток у публічний HTTPS (наприклад, через
   [ngrok](https://ngrok.com), Railway, Render тощо) і впишіть адресу:
   ```
   WEBAPP_URL=https://ваша-адреса
   ```
4. Налаштуйте кнопку меню бота:
   ```bash
   npm run setup-bot
   ```
5. Відкрийте свого бота в Telegram і натисніть кнопку **🏋️ Трекер**.

> ⚠️ Telegram Mini App вимагає **HTTPS**. Локальний `http://localhost`
> підходить лише для тесту у браузері (DEV_MODE).

---

## 🗂 Структура проєкту / Project structure

```
.
├── server/
│   ├── index.js      # Express-сервер + API
│   ├── db.js         # База даних SQLite + схема
│   ├── auth.js       # Перевірка підпису Telegram
│   └── program.js    # Каталог вправ і готова програма
├── public/           # Фронтенд (Mini App)
│   ├── index.html
│   ├── app.js
│   ├── styles.css
│   └── i18n.js       # Переклади (uk / en)
├── bot/
│   └── setup.js      # Налаштування кнопки меню бота
├── data/             # Тут створюється файл бази даних (tracker.db)
├── .env.example
└── package.json
```

## 🛠 Технології / Tech stack

- **Node.js + Express** — сервер та API
- **SQLite (better-sqlite3)** — база даних (один файл, без окремого сервера БД)
- **Vanilla JS + Telegram WebApp SDK** — фронтенд

## 📜 Ліцензія / License
MIT
