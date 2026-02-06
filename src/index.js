require('dotenv').config();
const {
  Client,
  GatewayIntentBits,
  Partials,
  Collection,
  Events,
  ActionRowBuilder,
  ModalBuilder,
  TextInputBuilder,
  TextInputStyle,
} = require('discord.js');
const { loadConfig, getConfig } = require('./config');
const logger = require('./logger');
const { initDb, db } = require('./db');
const { assertStaff, getStaffRank, getRoleKeyByRank } = require('./utils/permissions');
const { infoEmbed, successEmbed, errorEmbed } = require('./utils/embeds');
const profile = require('./commands/profile');
const sp = require('./commands/sp');
const coins = require('./commands/coins');
const promo = require('./commands/promo');
const punish = require('./commands/punish');
const warnings = require('./commands/warnings');
const shop = require('./commands/shop');
const panel = require('./commands/panel');
const configCmd = require('./commands/config');
const backup = require('./commands/backup');
const health = require('./commands/health');
const { handlePanelButton, handlePanelModal, handlePanelSelect } = require('./panel/handlers');
const pointsService = require('./services/pointsService');
const coinsService = require('./services/coinsService');
const promoService = require('./services/promoService');
const warningsService = require('./services/warningsService');
const shopService = require('./services/shopService');
const ticketService = require('./services/ticketService');
const { createCustomRole, removeCustomRole } = require('./services/customRoleService');

const commands = [profile, sp, coins, promo, punish, warnings, shop, panel, configCmd, backup, health];

const client = new Client({
  intents: [
    GatewayIntentBits.Guilds,
    GatewayIntentBits.GuildMembers,
    GatewayIntentBits.GuildModeration,
    GatewayIntentBits.GuildMessages,
    GatewayIntentBits.GuildVoiceStates,
  ],
  partials: [Partials.Channel],
});

client.commands = new Collection();
for (const command of commands) {
  client.commands.set(command.data.name, command);
}

function buildModal(customId, title, inputs) {
  const modal = new ModalBuilder().setCustomId(customId).setTitle(title);
  const rows = inputs.map((input) => new ActionRowBuilder().addComponents(input));
  modal.addComponents(...rows);
  return modal;
}

async function sendStaffLog(clientRef, embed) {
  const config = getConfig();
  const channel = await clientRef.channels.fetch(config.channels.staffJournal).catch(() => null);
  if (channel) {
    await channel.send({ embeds: [embed] });
  }
}

async function ensureStaffRoleRecord(member) {
  const rank = getStaffRank(member);
  if (rank < 0) return;
  const config = getConfig();
  const roleId = config.roles.ladder[rank].roleId;
  pointsService.setStaffRole(member.id, roleId, Date.now());
}

async function handleShopSelect(interaction) {
  const check = assertStaff(interaction.member, 'shop_open');
  if (!check.ok) {
    return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription(check.reason)] });
  }
  const itemKey = interaction.values[0];
  const items = shopService.getItems();
  const item = items[itemKey];
  if (!item) {
    return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('Товар не найден.')] });
  }
  const userData = pointsService.getUser(interaction.user.id);
  if (userData.coins < item.price) {
    return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Недостаточно Coins').setDescription('Пополните баланс.')] });
  }

  if (itemKey === 'custom_role_7d' && shopService.hasActivePurchase(interaction.user.id, itemKey)) {
    return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('У вас уже есть активная кастом роль.')] });
  }

  if (item.type === 'AUTO') {
    const modal = buildModal(`shop_auto:${itemKey}`, 'Кастом роль', [
      new TextInputBuilder().setCustomId('roleName').setLabel('Название роли').setStyle(TextInputStyle.Short).setRequired(true),
      new TextInputBuilder().setCustomId('hexColor').setLabel('HEX цвет (#ff0000)').setStyle(TextInputStyle.Short).setRequired(true),
      new TextInputBuilder().setCustomId('iconLink').setLabel('Ссылка на иконку').setStyle(TextInputStyle.Short).setRequired(false),
    ]);
    return interaction.showModal(modal);
  }

  if (item.type === 'TICKET') {
    const modal = buildModal(`shop_ticket:${itemKey}`, 'Данные покупки', [
      new TextInputBuilder().setCustomId('details').setLabel('Детали').setStyle(TextInputStyle.Paragraph).setRequired(true),
    ]);
    return interaction.showModal(modal);
  }

  return null;
}

async function handleShopModal(interaction) {
  const [mode, itemKey] = interaction.customId.split(':');
  const items = shopService.getItems();
  const item = items[itemKey];
  if (!item) {
    return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('Товар не найден.')] });
  }
  const userData = pointsService.getUser(interaction.user.id);
  if (userData.coins < item.price) {
    return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Недостаточно Coins').setDescription('Пополните баланс.')] });
  }
  if (mode === 'shop_auto') {
    const roleName = interaction.fields.getTextInputValue('roleName');
    const hexColor = interaction.fields.getTextInputValue('hexColor');
    const iconLink = interaction.fields.getTextInputValue('iconLink');
    const purchaseId = shopService.createPurchase({ userId: interaction.user.id, itemKey, price: item.price, details: { roleName, hexColor, iconLink } });
    coinsService.adjustCoins({ actorId: interaction.user.id, targetId: interaction.user.id, delta: -item.price, reason: `Покупка ${item.name}`, kind: 'purchase' });
    const role = await createCustomRole({
      guild: interaction.guild,
      member: await interaction.guild.members.fetch(interaction.user.id),
      name: roleName,
      color: hexColor,
      icon: iconLink || null,
    });
    const expiresAt = Date.now() + item.durationDays * 24 * 60 * 60 * 1000;
    shopService.updatePurchase(purchaseId, { status: 'active', roleId: role.id, expiresAt });
    await sendStaffLog(interaction.client, infoEmbed('Магазин: покупка')
      .setDescription(`AUTO товар: **${item.name}**\nПокупка #${purchaseId} для <@${interaction.user.id}>`));
    return interaction.reply({ ephemeral: true, embeds: [successEmbed('Покупка завершена').setDescription('Кастом роль создана.')] });
  }

  if (mode === 'shop_ticket') {
    const details = interaction.fields.getTextInputValue('details');
    const purchaseId = shopService.createPurchase({ userId: interaction.user.id, itemKey, price: item.price, details: { details } });
    coinsService.adjustCoins({ actorId: interaction.user.id, targetId: interaction.user.id, delta: -item.price, reason: `Покупка ${item.name}`, kind: 'purchase' });
    const ticket = await ticketService.createTicket({ client: interaction.client, purchaseId, buyerId: interaction.user.id, itemName: item.name, details });
    shopService.updatePurchase(purchaseId, { ticketMessageId: ticket.messageId, ticketThreadId: ticket.threadId, status: 'pending' });
    await sendStaffLog(interaction.client, infoEmbed('Магазин: создан тикет')
      .setDescription(`TICKET товар: **${item.name}**\nПокупка #${purchaseId} для <@${interaction.user.id}>`));
    return interaction.reply({ ephemeral: true, embeds: [successEmbed('Заявка создана').setDescription('Ожидайте обработки.')] });
  }

  return null;
}

async function handleShopTicketButton(interaction) {
  const [action, purchaseIdRaw] = interaction.customId.split(':');
  const purchaseId = Number(purchaseIdRaw);
  const purchase = shopService.getPurchase(purchaseId);
  if (!purchase) {
    return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('Покупка не найдена.')] });
  }

  if (action === 'shop_edit') {
    if (purchase.userId !== interaction.user.id) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('Вы не автор заявки.')] });
    }
    const modal = buildModal(`shop_edit:${purchaseId}`, 'Изменить данные', [
      new TextInputBuilder().setCustomId('details').setLabel('Новые детали').setStyle(TextInputStyle.Paragraph).setRequired(true),
    ]);
    return interaction.showModal(modal);
  }

  if (action === 'shop_done') {
    const check = assertStaff(interaction.member, 'ticket_done');
    if (!check.ok) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription(check.reason)] });
    }
    const item = shopService.getItems()[purchase.itemKey];
    const expiresAt = Date.now() + item.durationDays * 24 * 60 * 60 * 1000;
    shopService.updatePurchase(purchaseId, { status: 'active', handledBy: interaction.user.id, handledAt: Date.now(), expiresAt });
    await sendStaffLog(interaction.client, successEmbed('Магазин: выполнено')
      .setDescription(`Покупка #${purchaseId} выполнена <@${interaction.user.id}>`));
    return interaction.reply({ ephemeral: true, embeds: [successEmbed('Готово').setDescription('Отмечено как выполнено.')] });
  }

  if (action === 'shop_deny') {
    const check = assertStaff(interaction.member, 'ticket_deny');
    if (!check.ok) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription(check.reason)] });
    }
    shopService.updatePurchase(purchaseId, { status: 'denied', handledBy: interaction.user.id, handledAt: Date.now() });
    await sendStaffLog(interaction.client, errorEmbed('Магазин: отказ')
      .setDescription(`Покупка #${purchaseId} отказана <@${interaction.user.id}>`));
    return interaction.reply({ ephemeral: true, embeds: [successEmbed('Готово').setDescription('Покупка отклонена.')] });
  }

  if (action === 'shop_refund') {
    const check = assertStaff(interaction.member, 'ticket_refund');
    if (!check.ok) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription(check.reason)] });
    }
    const result = shopService.refundPurchase(purchaseId, interaction.user.id, 'Возврат по тикету');
    if (!result.ok) {
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription(result.reason)] });
    }
    await sendStaffLog(interaction.client, infoEmbed('Магазин: возврат')
      .setDescription(`Покупка #${purchaseId} возвращена <@${interaction.user.id}>`));
    return interaction.reply({ ephemeral: true, embeds: [successEmbed('Возврат выполнен').setDescription('Coins возвращены.')] });
  }

  return null;
}

async function handleShopEditModal(interaction) {
  const purchaseId = Number(interaction.customId.split(':')[1]);
  const purchase = shopService.getPurchase(purchaseId);
  if (!purchase) {
    return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('Покупка не найдена.')] });
  }
  if (purchase.userId !== interaction.user.id) {
    return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('Вы не автор заявки.')] });
  }
  const details = interaction.fields.getTextInputValue('details');
  shopService.updatePurchase(purchaseId, { detailsJson: JSON.stringify({ details }) });
  return interaction.reply({ ephemeral: true, embeds: [successEmbed('Данные обновлены.')] });
}

client.once(Events.ClientReady, async () => {
  logger.info(`Logged in as ${client.user.tag}`);
  loadConfig();
  initDb();
});

client.on(Events.GuildMemberAdd, (member) => {
  pointsService.ensureUser(member.id);
});

client.on(Events.GuildMemberUpdate, (oldMember, newMember) => {
  ensureStaffRoleRecord(newMember).catch(() => null);
});

client.on(Events.InteractionCreate, async (interaction) => {
  const config = getConfig();
  if (interaction.guildId !== config.guildId) {
    return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('Гильдия не поддерживается.')] });
  }

  if (interaction.isChatInputCommand()) {
    const command = client.commands.get(interaction.commandName);
    if (!command) return;
    try {
      await command.execute(interaction);
    } catch (error) {
      logger.error(`Command error: ${error.message}`);
      if (interaction.deferred || interaction.replied) {
        await interaction.followUp({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('Команда завершилась ошибкой.')] });
      } else {
        await interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('Команда завершилась ошибкой.')] });
      }
    }
    return;
  }

  if (interaction.isStringSelectMenu()) {
    if (interaction.customId === 'shop_select') {
      return handleShopSelect(interaction);
    }
    if (interaction.customId === 'panel_select') {
      return handlePanelSelect(interaction);
    }
  }

  if (interaction.isButton()) {
    if (interaction.customId.startsWith('shop_')) {
      return handleShopTicketButton(interaction);
    }
    if (interaction.customId.startsWith('promo_')) {
      const [action, idRaw] = interaction.customId.split(':');
      const check = assertStaff(interaction.member, action === 'promo_approve' ? 'promo_approve' : 'promo_deny');
      if (!check.ok) {
        return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription(check.reason)] });
      }
      const request = promoService.getRequest(Number(idRaw));
      if (!request) {
        return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Ошибка').setDescription('Заявка не найдена.')] });
      }
      promoService.reviewRequest(request.id, interaction.user.id, action === 'promo_approve' ? 'approved' : 'denied', '');
      if (action === 'promo_approve') {
        const guild = interaction.guild;
        const target = await guild.members.fetch(request.userId).catch(() => null);
        if (target) {
          const ladderRoleIds = getConfig().roles.ladder.map((r) => r.roleId);
          const rolesToRemove = target.roles.cache.filter((role) => ladderRoleIds.includes(role.id));
          if (rolesToRemove.size > 0) {
            await target.roles.remove(rolesToRemove, 'Смена стафф роли');
          }
          await target.roles.add(request.toRoleId, 'Смена стафф роли');
          pointsService.setStaffRole(request.userId, request.toRoleId, Date.now());
        }
      }
      return interaction.reply({ ephemeral: true, embeds: [successEmbed('Готово').setDescription('Заявка обработана.')] });
    }
    if (interaction.customId.startsWith('panel_')) {
      return handlePanelButton(interaction);
    }
  }

  if (interaction.isModalSubmit()) {
    if (interaction.customId.startsWith('shop_auto') || interaction.customId.startsWith('shop_ticket')) {
      return handleShopModal(interaction);
    }
    if (interaction.customId.startsWith('shop_edit')) {
      return handleShopEditModal(interaction);
    }
    if (interaction.customId.startsWith('panel_')) {
      return handlePanelModal(interaction);
    }
  }
});

setInterval(() => {
  warningsService.cleanupExpired();
}, 24 * 60 * 60 * 1000);

setInterval(async () => {
  const rows = db.prepare('SELECT * FROM shop_purchases WHERE status = ? AND expiresAt IS NOT NULL AND expiresAt <= ?')
    .all('active', Date.now());
  for (const row of rows) {
    if (row.roleId) {
      const guild = await client.guilds.fetch(getConfig().guildId).catch(() => null);
      if (guild) {
        const member = await guild.members.fetch(row.userId).catch(() => null);
        if (member) {
          await removeCustomRole({ guild, member, roleId: row.roleId });
        }
      }
    }
    shopService.updatePurchase(row.id, { status: 'expired' });
  }
}, 5 * 60 * 1000);

setInterval(() => {
  const config = getConfig();
  if (!config.autoCreateRequestOnEligible) return;
  const staffRows = db.prepare('SELECT userId FROM users WHERE currentStaffRoleId IS NOT NULL').all();
  for (const row of staffRows) {
    const eligibility = promoService.getEligibility(row.userId);
    if (eligibility.eligible) {
      const existing = db.prepare('SELECT * FROM promo_requests WHERE userId = ? AND status = ?')
        .get(row.userId, 'pending');
      if (!existing) {
        promoService.createRequest(row.userId);
      }
    }
  }
}, 10 * 60 * 1000);

client.login(process.env.DISCORD_TOKEN);
