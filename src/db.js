const path = require('path');
const Database = require('better-sqlite3');

const dbPath = path.join(__dirname, '..', 'staff.db');
const db = new Database(dbPath);

function initDb() {
  db.exec(`
    CREATE TABLE IF NOT EXISTS users(
      userId TEXT PRIMARY KEY,
      sp INTEGER DEFAULT 0,
      coins INTEGER DEFAULT 0,
      currentStaffRoleId TEXT,
      staffRoleSince INTEGER,
      joinedAt INTEGER
    );
    CREATE TABLE IF NOT EXISTS warnings(
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      userId TEXT,
      type TEXT,
      reason TEXT,
      createdAt INTEGER,
      expiresAt INTEGER,
      actorId TEXT
    );
    CREATE TABLE IF NOT EXISTS sp_log(
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      ts INTEGER,
      actorId TEXT,
      targetId TEXT,
      delta INTEGER,
      reason TEXT,
      proof TEXT,
      kind TEXT,
      metaType TEXT
    );
    CREATE TABLE IF NOT EXISTS coins_log(
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      ts INTEGER,
      actorId TEXT,
      targetId TEXT,
      delta INTEGER,
      reason TEXT,
      kind TEXT
    );
    CREATE TABLE IF NOT EXISTS punish_cases(
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      ts INTEGER,
      actorId TEXT,
      targetId TEXT,
      action TEXT,
      durationMinutes INTEGER,
      isPerm INTEGER,
      reason TEXT,
      proof TEXT,
      revoked INTEGER,
      revokedAt INTEGER,
      revokedReason TEXT
    );
    CREATE TABLE IF NOT EXISTS punish_sp_counters(
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      actorId TEXT,
      dayKey TEXT,
      mutesCount INTEGER,
      bansCount INTEGER
    );
    CREATE TABLE IF NOT EXISTS promo_requests(
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      ts INTEGER,
      userId TEXT,
      fromRoleId TEXT,
      toRoleId TEXT,
      spAt INTEGER,
      daysAt INTEGER,
      status TEXT,
      reviewerId TEXT,
      reviewedAt INTEGER,
      note TEXT
    );
    CREATE TABLE IF NOT EXISTS shop_purchases(
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      ts INTEGER,
      userId TEXT,
      itemKey TEXT,
      price INTEGER,
      status TEXT,
      detailsJson TEXT,
      expiresAt INTEGER,
      roleId TEXT,
      ticketMessageId TEXT,
      ticketThreadId TEXT,
      handledBy TEXT,
      handledAt INTEGER
    );
    CREATE TABLE IF NOT EXISTS shop_cooldowns(
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      userId TEXT,
      itemKey TEXT,
      lastBuyAt INTEGER
    );
  `);
}

module.exports = {
  db,
  initDb,
};
