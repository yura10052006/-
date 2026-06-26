'use strict';

// Одноразовий скрипт: налаштовує кнопку меню бота, щоб відкривати Mini App.
// One-off script: sets the bot menu button to open the Mini App.
// Запуск / Run:  npm run setup-bot

require('dotenv').config();

const BOT_TOKEN = process.env.BOT_TOKEN;
const WEBAPP_URL = process.env.WEBAPP_URL;

async function main() {
  if (!BOT_TOKEN) {
    console.error('❌ Задайте BOT_TOKEN у файлі .env');
    process.exit(1);
  }
  if (!WEBAPP_URL || !WEBAPP_URL.startsWith('https://')) {
    console.error('❌ Задайте WEBAPP_URL (https://...) у файлі .env');
    process.exit(1);
  }

  const resp = await fetch(`https://api.telegram.org/bot${BOT_TOKEN}/setChatMenuButton`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      menu_button: {
        type: 'web_app',
        text: '🏋️ Трекер',
        web_app: { url: WEBAPP_URL },
      },
    }),
  });

  const data = await resp.json();
  if (data.ok) {
    console.log('✅ Кнопку меню налаштовано! Відкрийте бота в Telegram.');
  } else {
    console.error('❌ Помилка:', data);
    process.exit(1);
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
