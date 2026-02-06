const { SlashCommandBuilder } = require('discord.js');
const { assertStaff, getStaffRank, getRankByRoleKey } = require('../utils/permissions');
const { successEmbed, errorEmbed, infoEmbed } = require('../utils/embeds');
const pointsService = require('../services/pointsService');

module.exports = {
  data: new SlashCommandBuilder()
    .setName('sp')
    .setDescription('Управление SP')
    .addSubcommand((sub) => sub
      .setName('add')
      .setDescription('Начислить SP')
      .addUserOption((opt) => opt.setName('user').setDescription('Пользователь').setRequired(true))
      .addIntegerOption((opt) => opt.setName('value').setDescription('Количество').setRequired(true))
      .addStringOption((opt) => opt.setName('reason').setDescription('Причина').setRequired(true))
      .addStringOption((opt) => opt.setName('meta').setDescription('metaType').setRequired(false)))
    .addSubcommand((sub) => sub
      .setName('remove')
      .setDescription('Списать SP')
      .addUserOption((opt) => opt.setName('user').setDescription('Пользователь').setRequired(true))
      .addIntegerOption((opt) => opt.setName('value').setDescription('Количество').setRequired(true))
      .addStringOption((opt) => opt.setName('reason').setDescription('Причина').setRequired(true)))
    .addSubcommand((sub) => sub
      .setName('set')
      .setDescription('Установить SP')
      .addUserOption((opt) => opt.setName('user').setDescription('Пользователь').setRequired(true))
      .addIntegerOption((opt) => opt.setName('value').setDescription('Количество').setRequired(true))
      .addStringOption((opt) => opt.setName('reason').setDescription('Причина').setRequired(true)))
    .addSubcommand((sub) => sub
      .setName('log')
      .setDescription('Лог SP')
      .addUserOption((opt) => opt.setName('user').setDescription('Пользователь').setRequired(false)))
    .addSubcommand((sub) => sub
      .setName('top')
      .setDescription('Топ SP'))
    .addSubcommand((sub) => sub
      .setName('penalty')
      .setDescription('Штраф SP')
      .addUserOption((opt) => opt.setName('user').setDescription('Пользователь').setRequired(true))
      .addStringOption((opt) => opt.setName('type').setDescription('Тип').setRequired(true))
      .addStringOption((opt) => opt.setName('reason').setDescription('Причина').setRequired(true))),
  async execute(interaction) {
    const sub = interaction.options.getSubcommand();
    const accessKey = `sp_${sub}`;
    const check = assertStaff(interaction.member, accessKey);
    if (!check.ok) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription(check.reason)] });
    }

    if (['add', 'remove', 'set', 'penalty'].includes(sub)) {
      const target = interaction.options.getUser('user');
      if (target.id === interaction.user.id) {
        return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('Нельзя начислять себе.')] });
      }
      const reason = interaction.options.getString('reason');

      if (sub === 'penalty') {
        const type = interaction.options.getString('type');
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
        pointsService.adjustSp({ actorId: interaction.user.id, targetId: target.id, delta, reason, kind: 'penalty', metaType: type });
        return interaction.reply({ ephemeral: true, embeds: [successEmbed('Штраф применён').setDescription(`SP ${delta}.`)] });
      }

      const value = interaction.options.getInteger('value');
      if (sub === 'add') {
        const metaType = interaction.options.getString('meta') || '';
        if (metaType && !pointsService.canAwardMeta(interaction.user.id, metaType)) {
          return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('Дневной лимит по metaType исчерпан.')] });
        }
        pointsService.adjustSp({ actorId: interaction.user.id, targetId: target.id, delta: value, reason, kind: 'manual', metaType });
        return interaction.reply({ ephemeral: true, embeds: [successEmbed('Готово').setDescription(`SP +${value}.`)] });
      }
      if (sub === 'remove') {
        pointsService.adjustSp({ actorId: interaction.user.id, targetId: target.id, delta: -value, reason, kind: 'manual' });
        return interaction.reply({ ephemeral: true, embeds: [successEmbed('Готово').setDescription(`SP -${value}.`)] });
      }
      if (sub === 'set') {
        pointsService.setSp({ actorId: interaction.user.id, targetId: target.id, value, reason });
        return interaction.reply({ ephemeral: true, embeds: [successEmbed('Готово').setDescription(`SP = ${value}.`)] });
      }
    }

    if (sub === 'log') {
      const target = interaction.options.getUser('user');
      const deputyRank = getRankByRoleKey('deputy_curator');
      const memberRank = getStaffRank(interaction.member);
      if (!target || (target.id !== interaction.user.id && memberRank < deputyRank)) {
        return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription('Просмотр всех логов доступен только Зам.куратора+.')] });
      }
      const rows = pointsService.listSpLog(20, target?.id ?? null);
      const description = rows.map((row) => `#${row.id} <@${row.targetId}> ${row.delta} (${row.reason})`).join('\n') || 'Нет данных.';
      return interaction.reply({ ephemeral: true, embeds: [infoEmbed('Лог SP').setDescription(description)] });
    }

    if (sub === 'top') {
      const top = pointsService.topSp(10);
      const description = top.map((row, idx) => `**${idx + 1}.** <@${row.userId}> — ${row.sp} SP`).join('\n') || 'Нет данных.';
      return interaction.reply({ ephemeral: true, embeds: [infoEmbed('Топ SP').setDescription(description)] });
    }

    return null;
  },
};
