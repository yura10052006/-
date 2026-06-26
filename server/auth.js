'use strict';

const crypto = require('crypto');

// Перевірка initData від Telegram Web App.
// Validation of Telegram Web App initData.
// Документація: https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app
function verifyTelegramInitData(initData, botToken) {
  if (!initData || !botToken) return null;

  const params = new URLSearchParams(initData);
  const hash = params.get('hash');
  if (!hash) return null;
  params.delete('hash');

  // Формуємо рядок для перевірки (відсортовані пари key=value).
  const dataCheckString = [...params.entries()]
    .map(([k, v]) => `${k}=${v}`)
    .sort()
    .join('\n');

  const secretKey = crypto
    .createHmac('sha256', 'WebAppData')
    .update(botToken)
    .digest();

  const computedHash = crypto
    .createHmac('sha256', secretKey)
    .update(dataCheckString)
    .digest('hex');

  if (computedHash !== hash) return null;

  // Дані дійсні максимум 24 години.
  const authDate = Number(params.get('auth_date'));
  if (authDate && Date.now() / 1000 - authDate > 86400) return null;

  try {
    return JSON.parse(params.get('user'));
  } catch {
    return null;
  }
}

module.exports = { verifyTelegramInitData };
