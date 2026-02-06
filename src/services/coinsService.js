const { db } = require('../db');
const { getConfig } = require('../config');
const { ensureUser, getUser } = require('./pointsService');

function adjustCoins({ actorId, targetId, delta, reason, kind = 'manual' }) {
  ensureUser(targetId);
  db.prepare('UPDATE users SET coins = coins + ? WHERE userId = ?').run(delta, targetId);
  db.prepare(`
    INSERT INTO coins_log (ts, actorId, targetId, delta, reason, kind)
    VALUES (?, ?, ?, ?, ?, ?)
  `).run(Date.now(), actorId, targetId, delta, reason, kind);
}

function setCoins({ actorId, targetId, value, reason }) {
  ensureUser(targetId);
  const current = getUser(targetId).coins;
  const delta = value - current;
  adjustCoins({ actorId, targetId, delta, reason, kind: 'set' });
}

function listCoinsLog(limit = 20, targetId = null) {
  if (targetId) {
    return db.prepare('SELECT * FROM coins_log WHERE targetId = ? ORDER BY ts DESC LIMIT ?').all(targetId, limit);
  }
  return db.prepare('SELECT * FROM coins_log ORDER BY ts DESC LIMIT ?').all(limit);
}

function topCoins(limit = 10) {
  return db.prepare('SELECT userId, coins FROM users ORDER BY coins DESC LIMIT ?').all(limit);
}

function checkSoftLimit(actorId, delta) {
  const config = getConfig();
  if (delta <= 0) return false;
  const dayKey = new Date().toISOString().slice(0, 10);
  const start = new Date(`${dayKey}T00:00:00Z`).getTime();
  const row = db.prepare(`
    SELECT SUM(delta) as sum FROM coins_log
    WHERE actorId = ? AND delta > 0 AND ts >= ?
  `).get(actorId, start);
  const sum = row?.sum ?? 0;
  return sum + delta > (config.coins?.dailySoftLimit ?? 80);
}

module.exports = {
  adjustCoins,
  setCoins,
  listCoinsLog,
  topCoins,
  checkSoftLimit,
};
