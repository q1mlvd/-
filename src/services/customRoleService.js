const { getConfig } = require('../config');

async function createCustomRole({ guild, member, name, color, icon }) {
  const role = await guild.roles.create({
    name,
    color,
    icon: icon || null,
    reason: 'Магазин: кастом роль',
  });
  await member.roles.add(role.id, 'Магазин: кастом роль');
  return role;
}

async function removeCustomRole({ guild, member, roleId }) {
  const role = await guild.roles.fetch(roleId).catch(() => null);
  if (role) {
    await member.roles.remove(role.id, 'Магазин: истекла кастом роль').catch(() => null);
    await role.delete('Магазин: истекла кастом роль').catch(() => null);
  }
}

async function hasActiveCustomRole({ guild, member }) {
  const config = getConfig();
  const items = config.shop.items;
  const roleIds = new Set(Object.values(items).map((item) => item.roleId).filter(Boolean));
  const match = member.roles.cache.find((r) => roleIds.has(r.id));
  return Boolean(match);
}

module.exports = {
  createCustomRole,
  removeCustomRole,
  hasActiveCustomRole,
};
