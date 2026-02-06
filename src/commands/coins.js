const { SlashCommandBuilder } = require('discord.js');
const { assertStaff, getStaffRank, getRankByRoleKey } = require('../utils/permissions');
const { successEmbed, errorEmbed, infoEmbed } = require('../utils/embeds');
const coinsService = require('../services/coinsService');
const logger = require('../logger');
const { getConfig } = require('../config');

module.exports = {
  data: new SlashCommandBuilder()
    .setName('coins')
    .setDescription('Управление Coins')
    .addSubcommand((sub) => sub
      .setName('add')
      .setDescription('Начислить Coins')
      .addUserOption((opt) => opt.setName('user').setDescription('Пользователь').setRequired(true))
      .addIntegerOption((opt) => opt.setName('value').setDescription('Количество').setRequired(true))
      .addStringOption((opt) => opt.setName('reason').setDescription('Причина').setRequired(true)))
    .addSubcommand((sub) => sub
      .setName('remove')
      .setDescription('Списать Coins')
      .addUserOption((opt) => opt.setName('user').setDescription('Пользователь').setRequired(true))
      .addIntegerOption((opt) => opt.setName('value').setDescription('Количество').setRequired(true))
      .addStringOption((opt) => opt.setName('reason').setDescription('Причина').setRequired(true)))
    .addSubcommand((sub) => sub
      .setName('set')
      .setDescription('Установить Coins')
      .addUserOption((opt) => opt.setName('user').setDescription('Пользователь').setRequired(true))
      .addIntegerOption((opt) => opt.setName('value').setDescription('Количество').setRequired(true))
      .addStringOption((opt) => opt.setName('reason').setDescription('Причина').setRequired(true)))
    .addSubcommand((sub) => sub
      .setName('log')
      .setDescription('Лог Coins')
      .addUserOption((opt) => opt.setName('user').setDescription('Пользователь').setRequired(false)))
    .addSubcommand((sub) => sub
      .setName('top')
      .setDescription('Топ Coins')),
  async execute(interaction) {
    const sub = interaction.options.getSubcommand();
    const accessKey = `coins_${sub}`;
    const check = assertStaff(interaction.member, accessKey);
    if (!check.ok) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription(check.reason)] });
    }

    if (['add', 'remove', 'set'].includes(sub)) {
      const target = interaction.options.getUser('user');
      if (target.id === interaction.user.id) {
        return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('Нельзя начислять себе.')] });
      }
      const value = interaction.options.getInteger('value');
      const reason = interaction.options.getString('reason');
      if (sub === 'add') {
        const softLimitExceeded = coinsService.checkSoftLimit(interaction.user.id, value);
        coinsService.adjustCoins({ actorId: interaction.user.id, targetId: target.id, delta: value, reason, kind: 'manual' });
        if (softLimitExceeded) {
          logger.warn(`Coins daily soft limit exceeded by ${interaction.user.id}.`);
          const config = getConfig();
          const channel = await interaction.client.channels.fetch(config.channels.staffJournal);
          await channel.send({ embeds: [infoEmbed('⚠ Превышен лимит Coins').setDescription(`Актёр: <@${interaction.user.id}>\\nDelta: +${value}`)] });
        }
        return interaction.reply({ ephemeral: true, embeds: [successEmbed('Готово').setDescription(`Coins +${value}.`)] });
      }
      if (sub === 'remove') {
        coinsService.adjustCoins({ actorId: interaction.user.id, targetId: target.id, delta: -value, reason, kind: 'manual' });
        return interaction.reply({ ephemeral: true, embeds: [successEmbed('Готово').setDescription(`Coins -${value}.`)] });
      }
      if (sub === 'set') {
        coinsService.setCoins({ actorId: interaction.user.id, targetId: target.id, value, reason });
        return interaction.reply({ ephemeral: true, embeds: [successEmbed('Готово').setDescription(`Coins = ${value}.`)] });
      }
    }

    if (sub === 'log') {
      const target = interaction.options.getUser('user');
      const deputyRank = getRankByRoleKey('deputy_curator');
      const memberRank = getStaffRank(interaction.member);
      if (!target || (target.id !== interaction.user.id && memberRank < deputyRank)) {
        return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription('Просмотр всех логов доступен только Зам.куратора+.')] });
      }
      const rows = coinsService.listCoinsLog(20, target?.id ?? null);
      const description = rows.map((row) => `#${row.id} <@${row.targetId}> ${row.delta} (${row.reason})`).join('\n') || 'Нет данных.';
      return interaction.reply({ ephemeral: true, embeds: [infoEmbed('Лог Coins').setDescription(description)] });
    }

    if (sub === 'top') {
      const top = coinsService.topCoins(10);
      const description = top.map((row, idx) => `**${idx + 1}.** <@${row.userId}> — ${row.coins} Coins`).join('\n') || 'Нет данных.';
      return interaction.reply({ ephemeral: true, embeds: [infoEmbed('Топ Coins').setDescription(description)] });
    }

    return null;
  },
};
