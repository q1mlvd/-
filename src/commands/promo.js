const { SlashCommandBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle } = require('discord.js');
const { assertStaff } = require('../utils/permissions');
const { successEmbed, errorEmbed, infoEmbed } = require('../utils/embeds');
const promoService = require('../services/promoService');
const { getConfig } = require('../config');
const { setStaffRole } = require('../services/pointsService');

async function applyRoleChange(guild, userId, newRoleId) {
  const member = await guild.members.fetch(userId);
  const config = getConfig();
  const ladderRoleIds = config.roles.ladder.map((r) => r.roleId);
  const rolesToRemove = member.roles.cache.filter((role) => ladderRoleIds.includes(role.id));
  if (rolesToRemove.size > 0) {
    await member.roles.remove(rolesToRemove, 'Смена стафф роли');
  }
  if (newRoleId) {
    await member.roles.add(newRoleId, 'Смена стафф роли');
  }
  setStaffRole(userId, newRoleId, Date.now());
}

module.exports = {
  data: new SlashCommandBuilder()
    .setName('promo')
    .setDescription('Повышения')
    .addSubcommand((sub) => sub
      .setName('request')
      .setDescription('Запросить повышение'))
    .addSubcommand((sub) => sub
      .setName('list')
      .setDescription('Список заявок'))
    .addSubcommand((sub) => sub
      .setName('approve')
      .setDescription('Одобрить заявку')
      .addIntegerOption((opt) => opt.setName('id').setDescription('ID заявки').setRequired(true))
      .addStringOption((opt) => opt.setName('note').setDescription('Комментарий').setRequired(false)))
    .addSubcommand((sub) => sub
      .setName('deny')
      .setDescription('Отклонить заявку')
      .addIntegerOption((opt) => opt.setName('id').setDescription('ID заявки').setRequired(true))
      .addStringOption((opt) => opt.setName('note').setDescription('Комментарий').setRequired(false)))
    .addSubcommand((sub) => sub
      .setName('promote')
      .setDescription('Ручное повышение')
      .addUserOption((opt) => opt.setName('user').setDescription('Пользователь').setRequired(true))
      .addStringOption((opt) => opt.setName('role').setDescription('roleKey').setRequired(true)))
    .addSubcommand((sub) => sub
      .setName('demote')
      .setDescription('Ручное понижение')
      .addUserOption((opt) => opt.setName('user').setDescription('Пользователь').setRequired(true))
      .addStringOption((opt) => opt.setName('role').setDescription('roleKey').setRequired(true))),
  async execute(interaction) {
    const sub = interaction.options.getSubcommand();
    const accessKey = `promo_${sub}`;
    const check = assertStaff(interaction.member, accessKey);
    if (!check.ok) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription(check.reason)] });
    }

    if (sub === 'request') {
      const result = promoService.createRequest(interaction.user.id);
      if (!result.ok) {
        return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription(result.reason)] });
      }
      const config = getConfig();
      const journal = await interaction.client.channels.fetch(config.channels.staffJournal);
      const row = new ActionRowBuilder().addComponents(
        new ButtonBuilder().setCustomId(`promo_approve:${result.requestId}`).setLabel('✅ Одобрить').setStyle(ButtonStyle.Success),
        new ButtonBuilder().setCustomId(`promo_deny:${result.requestId}`).setLabel('❌ Отказать').setStyle(ButtonStyle.Danger)
      );
      await journal.send({
        embeds: [infoEmbed('Заявка на повышение').setDescription(`Пользователь: <@${interaction.user.id}>\\nID: ${result.requestId}`)],
        components: [row],
      });
      return interaction.reply({ ephemeral: true, embeds: [successEmbed('Заявка создана').setDescription(`ID заявки: ${result.requestId}`)] });
    }

    if (sub === 'list') {
      const list = promoService.listRequests('pending');
      const description = list.map((req) => `#${req.id} <@${req.userId}>`).join('\n') || 'Нет заявок.';
      return interaction.reply({ ephemeral: true, embeds: [infoEmbed('Заявки').setDescription(description)] });
    }

    if (sub === 'approve' || sub === 'deny') {
      const id = interaction.options.getInteger('id');
      const note = interaction.options.getString('note') || '';
      const request = promoService.getRequest(id);
      if (!request) {
        return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('Заявка не найдена.')] });
      }
      promoService.reviewRequest(id, interaction.user.id, sub === 'approve' ? 'approved' : 'denied', note);
      if (sub === 'approve') {
        await applyRoleChange(interaction.guild, request.userId, request.toRoleId);
      }
      return interaction.reply({ ephemeral: true, embeds: [successEmbed('Готово').setDescription('Заявка обработана.')] });
    }

    if (sub === 'promote' || sub === 'demote') {
      const user = interaction.options.getUser('user');
      const roleKey = interaction.options.getString('role');
      const config = getConfig();
      const role = config.roles.ladder.find((r) => r.key === roleKey);
      if (!role) {
        return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('Неизвестный roleKey.')] });
      }
      await applyRoleChange(interaction.guild, user.id, role.roleId);
      return interaction.reply({ ephemeral: true, embeds: [successEmbed('Готово').setDescription(`Роль обновлена: ${role.name}.`)] });
    }

    return null;
  },
};
