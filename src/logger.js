const fs = require('fs');
const path = require('path');

function getLogStream() {
  const logDir = path.join(__dirname, '..', 'logs');
  if (!fs.existsSync(logDir)) {
    fs.mkdirSync(logDir, { recursive: true });
  }
  const date = new Date().toISOString().slice(0, 10);
  const logPath = path.join(logDir, `bot-${date}.log`);
  return fs.createWriteStream(logPath, { flags: 'a' });
}

const stream = getLogStream();

function logLine(level, message) {
  const line = `[${new Date().toISOString()}] [${level}] ${message}`;
  stream.write(`${line}\n`);
  // eslint-disable-next-line no-console
  console.log(line);
}

module.exports = {
  info: (message) => logLine('INFO', message),
  warn: (message) => logLine('WARN', message),
  error: (message) => logLine('ERROR', message),
};
