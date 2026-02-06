const { db } = require('../db');
const { getConfig } = require('../config');
const { getUser, ensureUser, setStaffRole } = require('./pointsService');
const { hasActiveWarning } = require('./warningsService');

function getRoleKeyById(roleId) {
  const config = getConfig();
  return config.roles.ladder.find((role) => role.roleId === roleId)?.key ?? null;
}

function getRoleByKey(roleKey) {
  const config = getConfig();
  return config.roles.ladder.find((role) => role.key === roleKey) ?? null;
}

function getEligibility(userId) {
  const config = getConfig();
  const user = getUser(userId);
  const currentKey = getRoleKeyById(user.currentStaffRoleId);
  if (!currentKey) {
    return { eligible: false, reason: 'Стафф роль не найдена.' };
  }
  const rule = config.promotions.table[currentKey];
  if (!rule) {
    return { eligible: false, reason: 'Для этой роли нет автоповышения.' };
  }
  if (hasActiveWarning(userId)) {
    return { eligible: false, reason: 'Есть активное предупреждение.' };
  }
  const daysInRole = user.staffRoleSince
    ? Math.floor((Date.now() - user.staffRoleSince) / (24 * 60 * 60 * 1000))
    : 0;
  if (user.sp < rule.sp) {
    return { eligible: false, reason: `Недостаточно SP (${user.sp}/${rule.sp}).` };
  }
  if (daysInRole < rule.days) {
    return { eligible: false, reason: `Недостаточно дней (${daysInRole}/${rule.days}).` };
  }
  return { eligible: true, fromRoleId: user.currentStaffRoleId, toRoleId: getRoleByKey(rule.to)?.roleId };
}

function createRequest(userId) {
  ensureUser(userId);
  const eligibility = getEligibility(userId);
  if (!eligibility.eligible) {
    return { ok: false, reason: eligibility.reason };
  }
  const user = getUser(userId);
  const daysAt = user.staffRoleSince
    ? Math.floor((Date.now() - user.staffRoleSince) / (24 * 60 * 60 * 1000))
    : 0;
  const info = db.prepare(`
    INSERT INTO promo_requests (ts, userId, fromRoleId, toRoleId, spAt, daysAt, status)
    VALUES (?, ?, ?, ?, ?, ?, 'pending')
  `).run(Date.now(), userId, eligibility.fromRoleId, eligibility.toRoleId, user.sp, daysAt);
  return { ok: true, requestId: info.lastInsertRowid, eligibility };
}

function listRequests(status = 'pending') {
  return db.prepare('SELECT * FROM promo_requests WHERE status = ? ORDER BY ts DESC').all(status);
}

function getRequest(id) {
  return db.prepare('SELECT * FROM promo_requests WHERE id = ?').get(id);
}

function reviewRequest(id, reviewerId, status, note = '') {
  db.prepare(`
    UPDATE promo_requests SET status = ?, reviewerId = ?, reviewedAt = ?, note = ? WHERE id = ?
  `).run(status, reviewerId, Date.now(), note, id);
}

function promoteUser(userId, toRoleId) {
  setStaffRole(userId, toRoleId, Date.now());
}

function demoteUser(userId, toRoleId) {
  setStaffRole(userId, toRoleId, Date.now());
}

module.exports = {
  getEligibility,
  createRequest,
  listRequests,
  getRequest,
  reviewRequest,
  promoteUser,
  demoteUser,
};
