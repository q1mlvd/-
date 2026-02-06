const { db } = require('../db');
const { getConfig } = require('../config');
const { parseDuration } = require('../utils/timeParse');
const { adjustSp } = require('./pointsService');

function getDayKey() {
  return new Date().toISOString().slice(0, 10);
}

function getCounters(actorId) {
  const dayKey = getDayKey();
  let row = db.prepare('SELECT * FROM punish_sp_counters WHERE actorId = ? AND dayKey = ?')
    .get(actorId, dayKey);
  if (!row) {
    db.prepare('INSERT INTO punish_sp_counters (actorId, dayKey, mutesCount, bansCount) VALUES (?, ?, 0, 0)')
      .run(actorId, dayKey);
    row = db.prepare('SELECT * FROM punish_sp_counters WHERE actorId = ? AND dayKey = ?')
      .get(actorId, dayKey);
  }
  return row;
}

function incrementCounter(actorId, type) {
  const dayKey = getDayKey();
  if (type === 'mute') {
    db.prepare('UPDATE punish_sp_counters SET mutesCount = mutesCount + 1 WHERE actorId = ? AND dayKey = ?')
      .run(actorId, dayKey);
  } else if (type === 'ban') {
    db.prepare('UPDATE punish_sp_counters SET bansCount = bansCount + 1 WHERE actorId = ? AND dayKey = ?')
      .run(actorId, dayKey);
  }
}

function logCase({ actorId, targetId, action, durationMinutes, isPerm, reason, proof }) {
  const info = db.prepare(`
    INSERT INTO punish_cases (ts, actorId, targetId, action, durationMinutes, isPerm, reason, proof, revoked)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)
  `).run(Date.now(), actorId, targetId, action, durationMinutes, isPerm ? 1 : 0, reason, proof || '');
  return info.lastInsertRowid;
}

function revokeCase(id, actorId, reason) {
  db.prepare('UPDATE punish_cases SET revoked = 1, revokedAt = ?, revokedReason = ? WHERE id = ?')
    .run(Date.now(), reason, id);
}

function listCases(limit = 20) {
  return db.prepare('SELECT * FROM punish_cases ORDER BY ts DESC LIMIT ?').all(limit);
}

function getLimitKeyForRole(configLimits, roleKey) {
  const keys = Object.keys(configLimits);
  if (keys.includes(roleKey)) return roleKey;
  return null;
}

function checkLimit(roleKey, action, durationInput) {
  const config = getConfig();
  const limits = config.punish.limits[action];
  if (!limits) return { ok: false, reason: 'Лимиты не настроены.' };
  const limitKey = getLimitKeyForRole(limits, roleKey);
  if (!limitKey) {
    return { ok: false, reason: 'Недостаточно прав на это наказание.' };
  }
  const parsed = parseDuration(durationInput);
  const allowed = parseDuration(limits[limitKey]);
  if (allowed.isPerm) {
    return { ok: true, parsed };
  }
  if (parsed.isPerm) {
    return { ok: false, reason: 'Перм наказание запрещено для вашей роли.' };
  }
  if (parsed.minutes === null) {
    return { ok: false, reason: 'Неверный формат длительности.' };
  }
  if (parsed.minutes > allowed.minutes) {
    return { ok: false, reason: 'Превышен лимит наказания.' };
  }
  return { ok: true, parsed };
}

function calculateSpForMute(minutes) {
  if (minutes <= 10) return 1;
  if (minutes <= 30) return 2;
  if (minutes <= 120) return 3;
  if (minutes <= 1440) return 4;
  return 0;
}

function calculateSpForBan(minutes, isPerm) {
  if (isPerm) return 12;
  if (minutes <= 1440) return 5;
  if (minutes <= 4320) return 7;
  if (minutes <= 10080) return 9;
  return 0;
}

function awardPunishSp({ actorId, targetId, action, minutes, isPerm, proof, logger }) {
  const config = getConfig();
  const counters = getCounters(actorId);
  const cooldownHours = config.sameTargetCooldownHours ?? 24;
  const since = Date.now() - cooldownHours * 60 * 60 * 1000;
  const recent = db.prepare(`
    SELECT COUNT(*) as cnt FROM sp_log
    WHERE actorId = ? AND targetId = ? AND metaType = ? AND ts >= ?
  `).get(actorId, targetId, action, since);
  if ((recent?.cnt ?? 0) > 0) {
    logger?.warn?.('SP cooldown triggered for punish.');
    return { awarded: false, reason: 'Кулдаун на SP за того же нарушителя.' };
  }

  if (action === 'mute') {
    if (counters.mutesCount >= 3) {
      return { awarded: false, reason: 'Лимит SP за муты на сегодня исчерпан.' };
    }
    const sp = calculateSpForMute(minutes);
    if (sp > 0) {
      adjustSp({ actorId, targetId: actorId, delta: sp, reason: `SP за мут (${minutes}m)`, kind: 'auto', metaType: 'mute' });
      incrementCounter(actorId, 'mute');
      return { awarded: true, sp };
    }
  }

  if (action === 'ban') {
    if (!proof) {
      return { awarded: false, reason: 'Для бана требуется доказательство.' };
    }
    if (counters.bansCount >= 2) {
      return { awarded: false, reason: 'Лимит SP за баны на сегодня исчерпан.' };
    }
    const sp = calculateSpForBan(minutes, isPerm);
    if (sp > 0) {
      adjustSp({ actorId, targetId: actorId, delta: sp, reason: `SP за бан (${isPerm ? 'perm' : `${minutes}m`})`, kind: 'auto', metaType: 'ban' });
      incrementCounter(actorId, 'ban');
      return { awarded: true, sp };
    }
  }

  return { awarded: false, reason: 'SP не начислены.' };
}

module.exports = {
  logCase,
  revokeCase,
  listCases,
  checkLimit,
  awardPunishSp,
};
