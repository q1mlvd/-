const { db } = require('../db');
const { getConfig } = require('../config');
const { addWarning } = require('./warningsService');

function ensureUser(userId) {
  const row = db.prepare('SELECT userId FROM users WHERE userId = ?').get(userId);
  if (!row) {
    db.prepare('INSERT INTO users (userId, sp, coins, joinedAt) VALUES (?, 0, 0, ?)')
      .run(userId, Date.now());
  }
}

function getUser(userId) {
  ensureUser(userId);
  return db.prepare('SELECT * FROM users WHERE userId = ?').get(userId);
}

function setStaffRole(userId, roleId, sinceTs) {
  ensureUser(userId);
  db.prepare('UPDATE users SET currentStaffRoleId = ?, staffRoleSince = ? WHERE userId = ?')
    .run(roleId, sinceTs, userId);
}

function adjustSp({ actorId, targetId, delta, reason, proof = '', kind = 'manual', metaType = '' }) {
  ensureUser(targetId);
  db.prepare('UPDATE users SET sp = sp + ? WHERE userId = ?').run(delta, targetId);
  db.prepare(`
    INSERT INTO sp_log (ts, actorId, targetId, delta, reason, proof, kind, metaType)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
  `).run(Date.now(), actorId, targetId, delta, reason, proof, kind, metaType);
  evaluateThresholds(targetId, actorId);
}

function setSp({ actorId, targetId, value, reason }) {
  ensureUser(targetId);
  const current = getUser(targetId).sp;
  const delta = value - current;
  adjustSp({ actorId, targetId, delta, reason, kind: 'set' });
}

function listSpLog(limit = 20, targetId = null) {
  if (targetId) {
    return db.prepare('SELECT * FROM sp_log WHERE targetId = ? ORDER BY ts DESC LIMIT ?').all(targetId, limit);
  }
  return db.prepare('SELECT * FROM sp_log ORDER BY ts DESC LIMIT ?').all(limit);
}

function topSp(limit = 10) {
  return db.prepare('SELECT userId, sp FROM users ORDER BY sp DESC LIMIT ?').all(limit);
}

function getDailyMetaCount(actorId, metaType) {
  const dayKey = new Date().toISOString().slice(0, 10);
  const row = db.prepare(`
    SELECT COUNT(*) as cnt FROM sp_log
    WHERE actorId = ? AND metaType = ? AND ts >= ?
  `).get(actorId, metaType, new Date(`${dayKey}T00:00:00Z`).getTime());
  return row?.cnt ?? 0;
}

function canAwardMeta(actorId, metaType) {
  const config = getConfig();
  const count = getDailyMetaCount(actorId, metaType);
  return count < (config.metaTypeDailyLimit ?? 3);
}

function evaluateThresholds(targetId, actorId) {
  const user = getUser(targetId);
  if (user.sp <= -30) {
    addWarning({ actorId, userId: targetId, type: 'auto', reason: 'Авто-предупреждение: SP <= -30' });
  }
  if (user.sp <= -50) {
    const config = getConfig();
    const ladder = config.roles.ladder;
    const currentIndex = ladder.findIndex((role) => role.roleId === user.currentStaffRoleId);
    if (currentIndex > 0) {
      const newRole = ladder[currentIndex - 1];
      setStaffRole(targetId, newRole.roleId, Date.now());
      db.prepare(`
        INSERT INTO sp_log (ts, actorId, targetId, delta, reason, proof, kind, metaType)
        VALUES (?, ?, ?, 0, ?, '', 'auto', 'auto_demote')
      `).run(Date.now(), actorId, targetId, 'Авто-понижение: SP <= -50');
    }
  }
}

module.exports = {
  ensureUser,
  getUser,
  setStaffRole,
  adjustSp,
  setSp,
  listSpLog,
  topSp,
  canAwardMeta,
};
