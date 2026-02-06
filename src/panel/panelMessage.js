const { ActionRowBuilder, ButtonBuilder, ButtonStyle, StringSelectMenuBuilder } = require('discord.js');
const { infoEmbed } = require('../utils/embeds');

function buildPanelMessage() {
  const embed = infoEmbed('Панель стаффа')
    .setDescription('Быстрые действия стаффа.');

  const row1 = new ActionRowBuilder().addComponents(
    new ButtonBuilder().setCustomId('panel_sp_add').setLabel('➕ SP').setStyle(ButtonStyle.Success),
    new ButtonBuilder().setCustomId('panel_sp_remove').setLabel('➖ SP').setStyle(ButtonStyle.Danger),
    new ButtonBuilder().setCustomId('panel_sp_penalty').setLabel('🧾 Штраф').setStyle(ButtonStyle.Secondary),
    new ButtonBuilder().setCustomId('panel_coins').setLabel('🪙 Coins').setStyle(ButtonStyle.Primary)
  );

  const row2 = new ActionRowBuilder().addComponents(
    new ButtonBuilder().setCustomId('panel_shop').setLabel('🛒 Магазин').setStyle(ButtonStyle.Primary),
    new ButtonBuilder().setCustomId('panel_profile').setLabel('📈 Профиль').setStyle(ButtonStyle.Secondary),
    new ButtonBuilder().setCustomId('panel_top').setLabel('🏅 Топ SP/Coins').setStyle(ButtonStyle.Secondary),
    new ButtonBuilder().setCustomId('panel_promos').setLabel('🧾 Заявки').setStyle(ButtonStyle.Secondary)
  );

  const row3 = new ActionRowBuilder().addComponents(
    new ButtonBuilder().setCustomId('panel_mute').setLabel('🔇 Мут').setStyle(ButtonStyle.Secondary),
    new ButtonBuilder().setCustomId('panel_ban').setLabel('🚫 Бан').setStyle(ButtonStyle.Secondary),
    new ButtonBuilder().setCustomId('panel_revoke').setLabel('♻️ Отменить наказание').setStyle(ButtonStyle.Secondary),
    new ButtonBuilder().setCustomId('panel_reload').setLabel('⚙️ Reload').setStyle(ButtonStyle.Primary)
  );

  const row4 = new ActionRowBuilder().addComponents(
    new StringSelectMenuBuilder()
      .setCustomId('panel_select')
      .setPlaceholder('Быстрые действия')
      .addOptions([
        { label: 'Профиль: я', value: 'profile_me' },
        { label: 'Профиль: выбрать', value: 'profile_user' },
        { label: 'Топ SP', value: 'top_sp' },
        { label: 'Топ Coins', value: 'top_coins' },
      ])
  );

  return { embeds: [embed], components: [row1, row2, row3, row4] };
}

module.exports = {
  buildPanelMessage,
};
