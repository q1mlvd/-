const fs = require('fs');
const path = require('path');
const YAML = require('yaml');

let cachedConfig = null;

function loadConfig() {
  const configPath = path.join(__dirname, '..', 'config.yml');
  const raw = fs.readFileSync(configPath, 'utf8');
  cachedConfig = YAML.parse(raw);
  return cachedConfig;
}

function getConfig() {
  if (!cachedConfig) {
    return loadConfig();
  }
  return cachedConfig;
}

module.exports = {
  loadConfig,
  getConfig,
};
