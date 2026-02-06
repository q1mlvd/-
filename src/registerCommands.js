require('dotenv').config();
const { REST, Routes } = require('discord.js');
const { loadConfig } = require('./config');
const profile = require('./commands/profile');
const sp = require('./commands/sp');
const coins = require('./commands/coins');
const promo = require('./commands/promo');
const punish = require('./commands/punish');
const shop = require('./commands/shop');
const panel = require('./commands/panel');
const configCmd = require('./commands/config');
const warnings = require('./commands/warnings');
const backup = require('./commands/backup');
const health = require('./commands/health');

const config = loadConfig();

const commands = [
  profile.data.toJSON(),
  sp.data.toJSON(),
  coins.data.toJSON(),
  promo.data.toJSON(),
  punish.data.toJSON(),
  warnings.data.toJSON(),
  shop.data.toJSON(),
  panel.data.toJSON(),
  configCmd.data.toJSON(),
  backup.data.toJSON(),
  health.data.toJSON(),
];

const rest = new REST({ version: '10' }).setToken(process.env.DISCORD_TOKEN);

(async () => {
  try {
    await rest.put(
      Routes.applicationGuildCommands(process.env.CLIENT_ID, config.guildId),
      { body: commands }
    );
    // eslint-disable-next-line no-console
    console.log('Команды зарегистрированы.');
  } catch (error) {
    // eslint-disable-next-line no-console
    console.error('Ошибка регистрации команд:', error);
  }
})();
