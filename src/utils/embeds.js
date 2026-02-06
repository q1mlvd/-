const { EmbedBuilder } = require('discord.js');

function baseEmbed(title) {
  return new EmbedBuilder()
    .setTitle(title)
    .setColor(0x2b2d31)
    .setTimestamp(new Date());
}

function successEmbed(title) {
  return baseEmbed(title).setColor(0x2ecc71);
}

function errorEmbed(title) {
  return baseEmbed(title).setColor(0xe74c3c);
}

function infoEmbed(title) {
  return baseEmbed(title).setColor(0x3498db);
}

module.exports = {
  baseEmbed,
  successEmbed,
  errorEmbed,
  infoEmbed,
};
