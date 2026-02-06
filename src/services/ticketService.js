const { ChannelType, ActionRowBuilder, ButtonBuilder, ButtonStyle } = require('discord.js');
const { getConfig } = require('../config');
const { infoEmbed } = require('../utils/embeds');

async function createTicket({ client, purchaseId, buyerId, itemName, details }) {
  const config = getConfig();
  const journal = await client.channels.fetch(config.channels.staffJournal);
  const embed = infoEmbed('Магазин: заявка')
    .setDescription(`Покупка **${itemName}**\nПокупатель: <@${buyerId}>\nID покупки: **${purchaseId}**`)
    .addFields({ name: 'Детали', value: details || 'Не указано.' });

  const row = new ActionRowBuilder().addComponents(
    new ButtonBuilder()
      .setCustomId(`shop_done:${purchaseId}`)
      .setLabel('✅ Выполнено')
      .setStyle(ButtonStyle.Success),
    new ButtonBuilder()
      .setCustomId(`shop_deny:${purchaseId}`)
      .setLabel('❌ Отказ')
      .setStyle(ButtonStyle.Danger),
    new ButtonBuilder()
      .setCustomId(`shop_refund:${purchaseId}`)
      .setLabel('↩ Возврат Coins')
      .setStyle(ButtonStyle.Secondary),
    new ButtonBuilder()
      .setCustomId(`shop_edit:${purchaseId}`)
      .setLabel('🕒 Изменить данные')
      .setStyle(ButtonStyle.Primary)
  );

  const message = await journal.send({ embeds: [embed], components: [row] });
  const thread = await message.startThread({
    name: `Тикет покупки #${purchaseId}`,
    type: ChannelType.PrivateThread,
    reason: 'Магазин: обработка заявки',
  });
  await thread.members.add(buyerId);
  return { messageId: message.id, threadId: thread.id };
}

module.exports = {
  createTicket,
};
