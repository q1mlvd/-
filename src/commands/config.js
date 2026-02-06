const { SlashCommandBuilder } = require('discord.js');
const { assertStaff } = require('../utils/permissions');
const { successEmbed, errorEmbed, infoEmbed } = require('../utils/embeds');
const { loadConfig } = require('../config');

module.exports = {
  data: new SlashCommandBuilder()
    .setName('config')
    .setDescription('Админ команды')
    .addSubcommand((sub) => sub.setName('reload').setDescription('Перезагрузить конфиг')),
  async execute(interaction) {
    const sub = interaction.options.getSubcommand();
    if (sub === 'reload') {
      const check = assertStaff(interaction.member, 'config_reload');
      if (!check.ok) {
        return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription(check.reason)] });
      }
      loadConfig();
      return interaction.reply({ ephemeral: true, embeds: [successEmbed('Конфиг обновлён.')] });
    }

    return null;
  },
};
