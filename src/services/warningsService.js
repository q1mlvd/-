const { db } = require('../db');
const { getConfig } = require('../config');

function addWarning({ actorId, userId, type, reason }) {
  const config = getConfig();
  const now = Date.now();
  const expiresAt = now + config.warnings.expireDays * 24 * 60 * 60 * 1000;
  db.prepare(`
    INSERT INTO warnings (userId, type, reason, createdAt, expiresAt, actorId)
    VALUES (?, ?, ?, ?, ?, ?)
  `).run(userId, type, reason, now, expiresAt, actorId);
}

function removeWarning(id) {
  db.prepare('DELETE FROM warnings WHERE id = ?').run(id);
}

function listWarnings(userId = null) {
  if (userId) {
    return db.prepare('SELECT * FROM warnings WHERE userId = ? ORDER BY createdAt DESC').all(userId);
  }
  return db.prepare('SELECT * FROM warnings ORDER BY createdAt DESC').all();
}

function cleanupExpired() {
  db.prepare('DELETE FROM warnings WHERE expiresAt <= ?').run(Date.now());
}

function hasActiveWarning(userId) {
  const row = db.prepare('SELECT COUNT(*) as cnt FROM warnings WHERE userId = ? AND expiresAt > ?')
    .get(userId, Date.now());
  return (row?.cnt ?? 0) > 0;
}

module.exports = {
  addWarning,
  removeWarning,
  listWarnings,
  cleanupExpired,
  hasActiveWarning,
};
