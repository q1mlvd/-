const fs = require('fs');
const path = require('path');
const { SlashCommandBuilder, AttachmentBuilder } = require('discord.js');
const { assertStaff } = require('../utils/permissions');
const { successEmbed, errorEmbed } = require('../utils/embeds');
const { getConfig } = require('../config');

module.exports = {
  data: new SlashCommandBuilder()
    .setName('backup')
    .setDescription('Бэкап базы данных')
    .addSubcommand((sub) => sub.setName('export').setDescription('Экспорт БД'))
    .addSubcommand((sub) => sub.setName('import').setDescription('Импорт БД')),
  async execute(interaction) {
    const sub = interaction.options.getSubcommand();
    if (sub === 'export') {
      const check = assertStaff(interaction.member, 'backup_export');
      if (!check.ok) {
        return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription(check.reason)] });
      }
      const config = getConfig();
      const dbPath = path.join(__dirname, '..', '..', 'staff.db');
      const buffer = fs.readFileSync(dbPath);
      const attachment = new AttachmentBuilder(buffer, { name: `staff-backup-${Date.now()}.db` });
      const journal = await interaction.client.channels.fetch(config.channels.staffJournal);
      await journal.send({ content: 'Экспорт базы данных.', files: [attachment] });
      return interaction.reply({ ephemeral: true, embeds: [successEmbed('Бэкап отправлен в журнал.')] });
    }
    if (sub === 'import') {
      const check = assertStaff(interaction.member, 'backup_import');
      if (!check.ok) {
        return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Доступ запрещён').setDescription(check.reason)] });
      }
      return interaction.reply({ ephemeral: true, embeds: [errorEmbed('Отключено').setDescription('Импорт пока не реализован.')] });
    }
    return null;
  },
};
