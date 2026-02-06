const { db } = require('../db');
const { getConfig } = require('../config');
const { adjustCoins } = require('./coinsService');

function getItems() {
  return getConfig().shop.items;
}

function createPurchase({ userId, itemKey, price, details = {} }) {
  const info = db.prepare(`
    INSERT INTO shop_purchases (ts, userId, itemKey, price, status, detailsJson)
    VALUES (?, ?, ?, ?, 'pending', ?)
  `).run(Date.now(), userId, itemKey, price, JSON.stringify(details));
  return info.lastInsertRowid;
}

function updatePurchase(id, fields) {
  const updates = Object.keys(fields).map((key) => `${key} = ?`).join(', ');
  const values = Object.values(fields);
  db.prepare(`UPDATE shop_purchases SET ${updates} WHERE id = ?`).run(...values, id);
}

function setPurchaseStatus(id, status, handledBy = null) {
  updatePurchase(id, { status, handledBy, handledAt: Date.now() });
}

function listPurchases(status = null, limit = 20) {
  if (status) {
    return db.prepare('SELECT * FROM shop_purchases WHERE status = ? ORDER BY ts DESC LIMIT ?')
      .all(status, limit);
  }
  return db.prepare('SELECT * FROM shop_purchases ORDER BY ts DESC LIMIT ?').all(limit);
}

function listActivePurchases(limit = 20) {
  return db.prepare('SELECT * FROM shop_purchases WHERE status = ? ORDER BY ts DESC LIMIT ?')
    .all('active', limit);
}

function getPurchase(id) {
  return db.prepare('SELECT * FROM shop_purchases WHERE id = ?').get(id);
}

function hasActivePurchase(userId, itemKey) {
  const row = db.prepare('SELECT * FROM shop_purchases WHERE userId = ? AND itemKey = ? AND status = ?')\n    .get(userId, itemKey, 'active');
  return Boolean(row);
}

function refundPurchase(id, actorId, reason) {
  const purchase = getPurchase(id);
  if (!purchase) return { ok: false, reason: 'Покупка не найдена.' };
  if (purchase.status === 'refunded') return { ok: false, reason: 'Покупка уже возвращена.' };
  adjustCoins({ actorId, targetId: purchase.userId, delta: purchase.price, reason: `Refund: ${reason}`, kind: 'refund' });
  setPurchaseStatus(id, 'refunded', actorId);
  return { ok: true, purchase };
}

function revokePurchase(id, actorId, reason) {
  const purchase = getPurchase(id);
  if (!purchase) return { ok: false, reason: 'Покупка не найдена.' };
  if (purchase.status === 'revoked') return { ok: false, reason: 'Покупка уже откачена.' };
  adjustCoins({ actorId, targetId: purchase.userId, delta: purchase.price, reason: `Revoke: ${reason}`, kind: 'revoke' });
  setPurchaseStatus(id, 'revoked', actorId);
  return { ok: true, purchase };
}

module.exports = {
  getItems,
  createPurchase,
  updatePurchase,
  setPurchaseStatus,
  listPurchases,
  listActivePurchases,
  getPurchase,
  hasActivePurchase,
  refundPurchase,
  revokePurchase,
};
