const { SlashCommandBuilder } = require('discord.js');
const { assertStaff, getStaffRank, getRoleKeyByRank } = require('../utils/permissions');
const { successEmbed, errorEmbed, infoEmbed } = require('../utils/embeds');
const punishService = require('../services/punishService');
const { parseDuration } = require('../utils/timeParse');
const logger = require('../logger');
const { getConfig } = require('../config');

module.exports = {
  data: new SlashCommandBuilder()
    .setName('punish')
    .setDescription('Наказания')
    .addSubcommand((sub) => sub
      .setName('mute')
      .setDescription('Мут')
      .addUserOption((opt) => opt.setName('user').setDescription('Пользователь').setRequired(true))
      .addStringOption((opt) => opt.setName('duration').setDescription('Длительность').setRequired(true))
      .addStringOption((opt) => opt.setName('reason').setDescription('Причина').setRequired(true)))
    .addSubcommand((sub) => sub
      .setName('ban')
      .setDescription('Бан')
      .addUserOption((opt) => opt.setName('user').setDescription('Пользователь').setRequired(true))
      .addStringOption((opt) => opt.setName('duration').setDescription('Длительность').setRequired(true))
      .addStringOption((opt) => opt.setName('reason').setDescription('Причина').setRequired(true))
      .addStringOption((opt) => opt.setName('proof').setDescription('Доказательство').setRequired(true)))
    .addSubcommand((sub) => sub
      .setName('revoke')
      .setDescription('Отменить наказание')
      .addIntegerOption((opt) => opt.setName('case').setDescription('ID кейса').setRequired(true))
      .addStringOption((opt) => opt.setName('reason').setDescription('Причина').setRequired(true)))
    .addSubcommand((sub) => sub
      .setName('log')
      .setDescription('Лог наказаний')),
  async execute(interaction) {
    const sub = interaction.options.getSubcommand();
    const accessKey = `punish_${sub}`;
    const check = assertStaff(interaction.member, accessKey);
    if (!check.ok) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription(check.reason)] });
    }

    if (sub === 'log') {
      const rows = punishService.listCases(20);
      const description = rows.map((row) => `#${row.id} <@${row.targetId}> ${row.action} ${row.durationMinutes || 'perm'}m`).join('\n') || 'Нет данных.';
      return interaction.reply({ ephemeral: true, embeds: [infoEmbed('Лог наказаний').setDescription(description)] });
    }

    if (sub === 'revoke') {
      const caseId = interaction.options.getInteger('case');
      const reason = interaction.options.getString('reason');
      punishService.revokeCase(caseId, interaction.user.id, reason);
      return interaction.reply({ ephemeral: true, embeds: [successEmbed('Готово').setDescription('Кейс отозван.')] });
    }

    if (sub === 'mute' || sub === 'ban') {
      const target = interaction.options.getUser('user');
      const duration = interaction.options.getString('duration');
      const reason = interaction.options.getString('reason');
      const proof = sub === 'ban' ? interaction.options.getString('proof') : '';
      const roleKey = getRoleKeyByRank(getStaffRank(interaction.member));
      const limitCheck = punishService.checkLimit(roleKey, sub, duration);
      if (!limitCheck.ok) {
        logger.warn(`Limit exceeded by ${interaction.user.id}.`);
        const config = getConfig();
        const channel = await interaction.client.channels.fetch(config.channels.staffJournal);
        await channel.send({ embeds: [errorEmbed('Превышение лимита наказания').setDescription(`Актёр: <@${interaction.user.id}>\\nТип: ${sub}\\nДлительность: ${duration}`)] });
        return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Отказ').setDescription(limitCheck.reason)] });
      }
      const parsed = parseDuration(duration);
      if (sub === 'mute') {
        const member = await interaction.guild.members.fetch(target.id).catch(() => null);
        if (!member) {
          return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('Пользователь не найден.')] });
        }
        if (parsed.isPerm || parsed.minutes === null) {
          return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('Неверная длительность.')] });
        }
        await member.timeout(parsed.minutes * 60 * 1000, reason);
      } else {
        await interaction.guild.members.ban(target.id, { reason });
      }
      const caseId = punishService.logCase({
        actorId: interaction.user.id,
        targetId: target.id,
        action: sub,
        durationMinutes: parsed.minutes,
        isPerm: parsed.isPerm,
        reason,
        proof,
      });
      const spAward = punishService.awardPunishSp({
        actorId: interaction.user.id,
        targetId: target.id,
        action: sub,
        minutes: parsed.minutes,
        isPerm: parsed.isPerm,
        proof,
        logger,
      });
      const extra = spAward.awarded ? `\nSP: +${spAward.sp}` : '';
      return interaction.reply({ ephemeral: true, embeds: [successEmbed('Наказание применено').setDescription(`Кейс #${caseId}${extra}`)] });
    }

    return null;
  },
};
