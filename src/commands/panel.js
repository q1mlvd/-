const { SlashCommandBuilder } = require('discord.js');
const { assertStaff } = require('../utils/permissions');
const { errorEmbed, successEmbed } = require('../utils/embeds');
const { getConfig } = require('../config');
const { buildPanelMessage } = require('../panel/panelMessage');

module.exports = {
  data: new SlashCommandBuilder()
    .setName('panel')
    .setDescription('Панель стаффа')
    .addSubcommand((sub) => sub.setName('send').setDescription('Отправить панель')),
  async execute(interaction) {
    const check = assertStaff(interaction.member, 'panel_send');
    if (!check.ok) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription(check.reason)] });
    }
    const config = getConfig();
    const staffChat = await interaction.client.channels.fetch(config.channels.staffChat);
    const payload = buildPanelMessage();
    await staffChat.send(payload);
    return interaction.reply({ ephemeral: true, embeds: [successEmbed('Панель отправлена.')] });
  },
};
