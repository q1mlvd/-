const { SlashCommandBuilder } = require('discord.js');
const { assertStaff } = require('../utils/permissions');
const { infoEmbed, errorEmbed } = require('../utils/embeds');
const pointsService = require('../services/pointsService');

module.exports = {
  data: new SlashCommandBuilder()
    .setName('profile')
    .setDescription('Профиль стаффа')
    .addSubcommand((sub) => sub
      .setName('me')
      .setDescription('Показать свой профиль'))
    .addSubcommand((sub) => sub
      .setName('user')
      .setDescription('Показать профиль пользователя')
      .addUserOption((opt) => opt.setName('user').setDescription('Пользователь').setRequired(true))),
  async execute(interaction) {
    const sub = interaction.options.getSubcommand();
    const accessKey = sub === 'me' ? 'profile_me' : 'profile_user';
    const check = assertStaff(interaction.member, accessKey);
    if (!check.ok) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription(check.reason)] });
    }

    const user = sub === 'me'
      ? interaction.user
      : interaction.options.getUser('user');
    const data = pointsService.getUser(user.id);
    const embed = infoEmbed('Профиль')
      .setDescription(`Пользователь: <@${user.id}>\nSP: **${data.sp}**\nCoins: **${data.coins}**`);
    return interaction.reply({ ephemeral: true, embeds: [embed] });
  },
};
