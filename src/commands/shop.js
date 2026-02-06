const { SlashCommandBuilder, ActionRowBuilder, StringSelectMenuBuilder } = require('discord.js');
const { assertStaff } = require('../utils/permissions');
const { infoEmbed, errorEmbed } = require('../utils/embeds');
const shopService = require('../services/shopService');

module.exports = {
  data: new SlashCommandBuilder()
    .setName('shop')
    .setDescription('Магазин стаффа')
    .addSubcommand((sub) => sub.setName('open').setDescription('Открыть магазин'))
    .addSubcommand((sub) => sub.setName('inventory').setDescription('Мои покупки'))
    .addSubcommand((sub) => sub.setName('log').setDescription('Логи покупок'))
    .addSubcommand((sub) => sub.setName('active').setDescription('Активные покупки'))
    .addSubcommand((sub) => sub
      .setName('revoke')
      .setDescription('Откат покупки')
      .addIntegerOption((opt) => opt.setName('purchase').setDescription('ID покупки').setRequired(true))
      .addStringOption((opt) => opt.setName('reason').setDescription('Причина').setRequired(true))),
  async execute(interaction) {
    const sub = interaction.options.getSubcommand();
    const accessKey = `shop_${sub}`;
    const check = assertStaff(interaction.member, accessKey);
    if (!check.ok) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription(check.reason)] });
    }

    if (sub === 'open') {
      const items = shopService.getItems();
      const options = Object.entries(items).map(([key, item]) => ({
        label: `${item.name} (${item.price} Coins)`,
        value: key,
      }));
      const row = new ActionRowBuilder().addComponents(
        new StringSelectMenuBuilder().setCustomId('shop_select').setPlaceholder('Выберите товар').addOptions(options)
      );
      const description = Object.entries(items).map(([key, item]) => `• **${item.name}** — ${item.price} Coins (key: ${key})`).join('\n');
      return interaction.reply({ ephemeral: true, embeds: [infoEmbed('Магазин').setDescription(description)], components: [row] });
    }

    if (sub === 'inventory') {
      const list = shopService.listPurchases(null, 20).filter((p) => p.userId === interaction.user.id);
      const description = list.map((p) => `#${p.id} ${p.itemKey} — ${p.status}`).join('\n') || 'Нет покупок.';
      return interaction.reply({ ephemeral: true, embeds: [infoEmbed('Мои покупки').setDescription(description)] });
    }

    if (sub === 'log') {
      const list = shopService.listPurchases(null, 20);
      const description = list.map((p) => `#${p.id} <@${p.userId}> ${p.itemKey} — ${p.status}`).join('\n') || 'Нет данных.';
      return interaction.reply({ ephemeral: true, embeds: [infoEmbed('Лог магазина').setDescription(description)] });
    }

    if (sub === 'active') {
      const list = shopService.listActivePurchases(20);
      const description = list.map((p) => `#${p.id} <@${p.userId}> ${p.itemKey} до ${p.expiresAt || 'n/a'}`).join('\n') || 'Нет активных.';
      return interaction.reply({ ephemeral: true, embeds: [infoEmbed('Активные покупки').setDescription(description)] });
    }

    if (sub === 'revoke') {
      const purchaseId = interaction.options.getInteger('purchase');
      const reason = interaction.options.getString('reason');
      const result = shopService.revokePurchase(purchaseId, interaction.user.id, reason);
      if (!result.ok) {
        return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription(result.reason)] });
      }
      return interaction.reply({ ephemeral: true, embeds: [infoEmbed('Откат выполнен').setDescription('Покупка откачена и Coins возвращены.')] });
    }

    return null;
  },
};
