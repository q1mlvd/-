const { SlashCommandBuilder } = require('discord.js');
const { assertStaff } = require('../utils/permissions');
const { infoEmbed, errorEmbed } = require('../utils/embeds');

module.exports = {
  data: new SlashCommandBuilder()
    .setName('health')
    .setDescription('Проверка состояния бота'),
  async execute(interaction) {
    const check = assertStaff(interaction.member, 'health');
    if (!check.ok) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription(check.reason)] });
    }
    return interaction.reply({ ephemeral: true, embeds: [infoEmbed('OK').setDescription('Бот работает.')] });
  },
};
