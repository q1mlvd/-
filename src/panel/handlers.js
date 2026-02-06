const {
  ModalBuilder,
  TextInputBuilder,
  TextInputStyle,
  ActionRowBuilder,
} = require('discord.js');
const { assertStaff, hasStaffAccess, getStaffRank, getRoleKeyByRank } = require('../utils/permissions');
const { successEmbed, errorEmbed, infoEmbed } = require('../utils/embeds');
const pointsService = require('../services/pointsService');
const coinsService = require('../services/coinsService');
const punishService = require('../services/punishService');
const promoService = require('../services/promoService');
const { parseDuration } = require('../utils/timeParse');
const logger = require('../logger');
const { loadConfig, getConfig } = require('../config');

function buildModal(customId, title, inputs) {
  const modal = new ModalBuilder().setCustomId(customId).setTitle(title);
  const rows = inputs.map((input) => new ActionRowBuilder().addComponents(input));
  modal.addComponents(...rows);
  return modal;
}

function handlePanelButton(interaction) {
  const mapping = {
    panel_sp_add: 'sp_add',
    panel_sp_remove: 'sp_remove',
    panel_sp_penalty: 'sp_penalty',
    panel_coins: 'coins_add',
    panel_mute: 'punish_mute',
    panel_ban: 'punish_ban',
    panel_revoke: 'punish_revoke',
    panel_reload: 'config_reload',
  };
  const accessKey = mapping[interaction.customId];
  if (accessKey) {
    const check = assertStaff(interaction.member, accessKey);
    if (!check.ok) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription(check.reason)] });
    }
  }

  if (interaction.customId === 'panel_sp_add' || interaction.customId === 'panel_sp_remove') {
    const modal = buildModal(
      interaction.customId,
      interaction.customId === 'panel_sp_add' ? 'Начислить SP' : 'Списать SP',
      [
        new TextInputBuilder().setCustomId('target').setLabel('ID пользователя').setStyle(TextInputStyle.Short).setRequired(true),
        new TextInputBuilder().setCustomId('value').setLabel('Количество').setStyle(TextInputStyle.Short).setRequired(true),
        new TextInputBuilder().setCustomId('reason').setLabel('Причина').setStyle(TextInputStyle.Paragraph).setRequired(true),
        new TextInputBuilder().setCustomId('meta').setLabel('metaType (опц.)').setStyle(TextInputStyle.Short).setRequired(false),
      ]
    );
    return interaction.showModal(modal);
  }

  if (interaction.customId === 'panel_sp_penalty') {
    const modal = buildModal('panel_sp_penalty', 'Штраф SP', [
      new TextInputBuilder().setCustomId('target').setLabel('ID пользователя').setStyle(TextInputStyle.Short).setRequired(true),
      new TextInputBuilder().setCustomId('type').setLabel('Тип штрафа').setStyle(TextInputStyle.Short).setRequired(true),
      new TextInputBuilder().setCustomId('reason').setLabel('Причина').setStyle(TextInputStyle.Paragraph).setRequired(true),
    ]);
    return interaction.showModal(modal);
  }

  if (interaction.customId === 'panel_coins') {
    const modal = buildModal('panel_coins', 'Начислить/списать Coins', [
      new TextInputBuilder().setCustomId('target').setLabel('ID пользователя').setStyle(TextInputStyle.Short).setRequired(true),
      new TextInputBuilder().setCustomId('value').setLabel('Delta (+/-)').setStyle(TextInputStyle.Short).setRequired(true),
      new TextInputBuilder().setCustomId('reason').setLabel('Причина').setStyle(TextInputStyle.Paragraph).setRequired(true),
    ]);
    return interaction.showModal(modal);
  }

  if (interaction.customId === 'panel_mute' || interaction.customId === 'panel_ban') {
    const modal = buildModal(interaction.customId, interaction.customId === 'panel_mute' ? 'Мут' : 'Бан', [
      new TextInputBuilder().setCustomId('target').setLabel('ID пользователя').setStyle(TextInputStyle.Short).setRequired(true),
      new TextInputBuilder().setCustomId('duration').setLabel('Длительность (10m/2h/1d/perm)').setStyle(TextInputStyle.Short).setRequired(true),
      new TextInputBuilder().setCustomId('reason').setLabel('Причина').setStyle(TextInputStyle.Paragraph).setRequired(true),
      new TextInputBuilder().setCustomId('proof').setLabel('Доказательство (ссылка)').setStyle(TextInputStyle.Short).setRequired(interaction.customId === 'panel_ban'),
    ]);
    return interaction.showModal(modal);
  }

  if (interaction.customId === 'panel_revoke') {
    const modal = buildModal('panel_revoke', 'Отменить наказание', [
      new TextInputBuilder().setCustomId('caseId').setLabel('ID кейса').setStyle(TextInputStyle.Short).setRequired(true),
      new TextInputBuilder().setCustomId('reason').setLabel('Причина').setStyle(TextInputStyle.Paragraph).setRequired(true),
    ]);
    return interaction.showModal(modal);
  }

  if (interaction.customId === 'panel_reload') {
    loadConfig();
    return interaction.reply({ ephemeral: true, embeds: [infoEmbed('Конфигурация перезагружена.')] });
  }

  return null;
}

async function handlePanelModal(interaction) {
  const member = interaction.member;
  const userId = interaction.user.id;
  if (interaction.customId === 'panel_sp_add' || interaction.customId === 'panel_sp_remove') {
    const check = assertStaff(member, interaction.customId === 'panel_sp_add' ? 'sp_add' : 'sp_remove');
    if (!check.ok) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription(check.reason)] });
    }
    const targetId = interaction.fields.getTextInputValue('target');
    if (targetId === userId) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('Нельзя начислять или списывать себе.')] });
    }
    const value = Number(interaction.fields.getTextInputValue('value'));
    const reason = interaction.fields.getTextInputValue('reason');
    const metaType = interaction.fields.getTextInputValue('meta') || '';
    if (Number.isNaN(value) || value <= 0) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('Некорректное значение.')] });
    }
    const delta = interaction.customId === 'panel_sp_add' ? value : -value;
    if (metaType && !pointsService.canAwardMeta(userId, metaType)) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('Дневной лимит по metaType исчерпан.')] });
    }
    pointsService.adjustSp({ actorId: userId, targetId, delta, reason, kind: 'manual', metaType });
    return interaction.reply({ ephemeral: true, embeds: [successEmbed('Готово').setDescription(`SP изменены на ${delta}.`)] });
  }

  if (interaction.customId === 'panel_sp_penalty') {
    const check = assertStaff(member, 'sp_penalty');
    if (!check.ok) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription(check.reason)] });
    }
    const targetId = interaction.fields.getTextInputValue('target');
    const type = interaction.fields.getTextInputValue('type');
    const reason = interaction.fields.getTextInputValue('reason');
    const penalties = {
      ignore: -5,
      rude: -6,
      wrong_punish: -8,
      confirmed_complaint: -12,
      abuse_power: -25,
    };
    const delta = penalties[type];
    if (!delta) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('Неизвестный тип штрафа.')] });
    }
    pointsService.adjustSp({ actorId: userId, targetId, delta, reason, kind: 'penalty', metaType: type });
    return interaction.reply({ ephemeral: true, embeds: [successEmbed('Штраф применён').setDescription(`SP ${delta}.`)] });
  }

  if (interaction.customId === 'panel_coins') {
    const check = assertStaff(member, 'coins_add');
    if (!check.ok) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription(check.reason)] });
    }
    const targetId = interaction.fields.getTextInputValue('target');
    if (targetId === userId) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('Нельзя начислять или списывать себе.')] });
    }
    const delta = Number(interaction.fields.getTextInputValue('value'));
    const reason = interaction.fields.getTextInputValue('reason');
    if (Number.isNaN(delta) || delta === 0) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('Некорректное значение.')] });
    }
    const softLimitExceeded = coinsService.checkSoftLimit(userId, delta);
    coinsService.adjustCoins({ actorId: userId, targetId, delta, reason, kind: 'manual' });
    if (softLimitExceeded) {
      logger.warn(`Coins daily soft limit exceeded by ${userId}.`);
      const config = getConfig();
      const channel = await interaction.client.channels.fetch(config.channels.staffJournal);
      await channel.send({ embeds: [infoEmbed('⚠ Превышен лимит Coins').setDescription(`Актёр: <@${userId}>\\nDelta: ${delta}`)] });
    }
    return interaction.reply({ ephemeral: true, embeds: [successEmbed('Готово').setDescription(`Coins изменены на ${delta}.`)] });
  }

  if (interaction.customId === 'panel_mute' || interaction.customId === 'panel_ban') {
    const accessKey = interaction.customId === 'panel_mute' ? 'punish_mute' : 'punish_ban';
    const check = assertStaff(member, accessKey);
    if (!check.ok) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription(check.reason)] });
    }
    const targetId = interaction.fields.getTextInputValue('target');
    const duration = interaction.fields.getTextInputValue('duration');
    const reason = interaction.fields.getTextInputValue('reason');
    const proof = interaction.fields.getTextInputValue('proof');

    const roleKey = getRoleKeyByRank(getStaffRank(member));
    const limitCheck = punishService.checkLimit(roleKey, interaction.customId === 'panel_mute' ? 'mute' : 'ban', duration);
    if (!limitCheck.ok) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Отказ').setDescription(limitCheck.reason)] });
    }
    const parsed = parseDuration(duration);
    punishService.logCase({
      actorId: userId,
      targetId,
      action: interaction.customId === 'panel_mute' ? 'mute' : 'ban',
      durationMinutes: parsed.minutes,
      isPerm: parsed.isPerm,
      reason,
      proof,
    });
    return interaction.reply({ ephemeral: true, embeds: [successEmbed('Наказание записано').setDescription('Выполните действие через модерацию.')] });
  }

  if (interaction.customId === 'panel_revoke') {
    const check = assertStaff(member, 'punish_revoke');
    if (!check.ok) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription(check.reason)] });
    }
    const caseId = Number(interaction.fields.getTextInputValue('caseId'));
    const reason = interaction.fields.getTextInputValue('reason');
    if (Number.isNaN(caseId)) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('Некорректный ID кейса.')] });
    }
    punishService.revokeCase(caseId, userId, reason);
    return interaction.reply({ ephemeral: true, embeds: [successEmbed('Готово').setDescription('Кейс отозван.')] });
  }

  if (interaction.customId === 'panel_reload') {
    if (!hasStaffAccess(member, 'config_reload')) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription('Недостаточно прав.')] });
    }
    return interaction.reply({ ephemeral: true, embeds: [successEmbed('Конфиг обновлён.')] });
  }

  return null;
}

async function handlePanelSelect(interaction) {
  const check = assertStaff(interaction.member, 'profile_me');
  if (!check.ok) {
    return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription(check.reason)] });
  }
  const value = interaction.values[0];
  if (value === 'top_sp') {
    const top = pointsService.topSp(10);
    const description = top.map((row, index) => `**${index + 1}.** <@${row.userId}> — ${row.sp} SP`).join('\n');
    return interaction.reply({ ephemeral: true, embeds: [infoEmbed('Топ SP').setDescription(description || 'Нет данных.')] });
  }
  if (value === 'top_coins') {
    const top = coinsService.topCoins(10);
    const description = top.map((row, index) => `**${index + 1}.** <@${row.userId}> — ${row.coins} Coins`).join('\n');
    return interaction.reply({ ephemeral: true, embeds: [infoEmbed('Топ Coins').setDescription(description || 'Нет данных.')] });
  }
  if (value === 'profile_me') {
    const user = pointsService.getUser(interaction.user.id);
    return interaction.reply({
      ephemeral: true,
      embeds: [infoEmbed('Профиль').setDescription(`SP: **${user.sp}**\nCoins: **${user.coins}**`)],
    });
  }
  if (value === 'profile_user') {
    return interaction.reply({ ephemeral: true, embeds: [infoEmbed('Профиль').setDescription('Используйте /profile user.')] });
  }
  if (value === 'profile_promos') {
    const requests = promoService.listRequests('pending');
    const description = requests.map((req) => `#${req.id} <@${req.userId}>`).join('\n') || 'Нет заявок.';
    return interaction.reply({ ephemeral: true, embeds: [infoEmbed('Заявки').setDescription(description)] });
  }
  return null;
}

module.exports = {
  handlePanelButton,
  handlePanelModal,
  handlePanelSelect,
};
