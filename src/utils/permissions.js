const { getConfig } = require('../config');

function getStaffRank(member) {
  const config = getConfig();
  const ladder = config.roles.ladder;
  let topIndex = -1;
  for (let i = 0; i < ladder.length; i += 1) {
    const roleId = ladder[i].roleId;
    if (member.roles.cache.has(roleId)) {
      topIndex = i;
    }
  }
  return topIndex;
}

function getRoleKeyByRank(rank) {
  const config = getConfig();
  if (rank < 0) return null;
  return config.roles.ladder[rank]?.key ?? null;
}

function getRankByRoleKey(roleKey) {
  const config = getConfig();
  return config.roles.ladder.findIndex((role) => role.key === roleKey);
}

function hasStaffAccess(member, accessKey) {
  const config = getConfig();
  const requiredRoleKey = config.accessMatrix[accessKey];
  if (!requiredRoleKey) return false;
  const memberRank = getStaffRank(member);
  if (memberRank < 0) return false;
  const requiredRank = getRankByRoleKey(requiredRoleKey);
  if (requiredRank < 0) return false;
  return memberRank >= requiredRank;
}

function assertStaff(member, accessKey) {
  if (!member) return { ok: false, reason: 'Команда доступна только стаффу.' };
  const rank = getStaffRank(member);
  if (rank < 0) {
    return { ok: false, reason: 'Команда доступна только стаффу.' };
  }
  if (accessKey && !hasStaffAccess(member, accessKey)) {
    return { ok: false, reason: 'Недостаточно прав для этой команды.' };
  }
  return { ok: true };
}

module.exports = {
  getStaffRank,
  getRoleKeyByRank,
  getRankByRoleKey,
  hasStaffAccess,
  assertStaff,
};
