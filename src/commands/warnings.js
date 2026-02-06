const { SlashCommandBuilder } = require('discord.js');
const { assertStaff } = require('../utils/permissions');
const { infoEmbed, successEmbed, errorEmbed } = require('../utils/embeds');
const warningsService = require('../services/warningsService');

module.exports = {
  data: new SlashCommandBuilder()
    .setName('warnings')
    .setDescription('Предупреждения')
    .addSubcommand((sub) => sub
      .setName('list')
      .setDescription('Список предупреждений')
      .addUserOption((opt) => opt.setName('user').setDescription('Пользователь').setRequired(false)))
    .addSubcommand((sub) => sub
      .setName('add')
      .setDescription('Добавить предупреждение')
      .addUserOption((opt) => opt.setName('user').setDescription('Пользователь').setRequired(true))
      .addStringOption((opt) => opt.setName('type').setDescription('Тип').setRequired(true))
      .addStringOption((opt) => opt.setName('reason').setDescription('Причина').setRequired(true)))
    .addSubcommand((sub) => sub
      .setName('remove')
      .setDescription('Удалить предупреждение')
      .addIntegerOption((opt) => opt.setName('id').setDescription('ID предупреждения').setRequired(true))),
  async execute(interaction) {
    const sub = interaction.options.getSubcommand();
    const accessKey = `warnings_${sub}`;
    const check = assertStaff(interaction.member, accessKey);
    if (!check.ok) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription(check.reason)] });
    }

    if (sub === 'list') {
      const user = interaction.options.getUser('user');
      const list = warningsService.listWarnings(user?.id ?? null);
      const description = list.map((w) => `#${w.id} <@${w.userId}> ${w.type} (${w.reason})`).join('\n') || 'Нет предупреждений.';
      return interaction.reply({ ephemeral: true, embeds: [infoEmbed('Предупреждения').setDescription(description)] });
    }

    if (sub === 'add') {
      const user = interaction.options.getUser('user');
      const type = interaction.options.getString('type');
      const reason = interaction.options.getString('reason');
      warningsService.addWarning({ actorId: interaction.user.id, userId: user.id, type, reason });
      return interaction.reply({ ephemeral: true, embeds: [successEmbed('Предупреждение добавлено.')] });
    }

    if (sub === 'remove') {
      const id = interaction.options.getInteger('id');
      warningsService.removeWarning(id);
      return interaction.reply({ ephemeral: true, embeds: [successEmbed('Предупреждение удалено.')] });
    }

    return null;
  },
};
