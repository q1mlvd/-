function parseDuration(input) {
  if (!input) return { minutes: null, isPerm: false };
  if (input === 'perm' || input === 'permanent') {
    return { minutes: null, isPerm: true };
  }
  const match = String(input).trim().match(/^(\d+)(m|h|d)$/i);
  if (!match) return { minutes: null, isPerm: false };
  const value = Number(match[1]);
  const unit = match[2].toLowerCase();
  let minutes = value;
  if (unit === 'h') minutes = value * 60;
  if (unit === 'd') minutes = value * 60 * 24;
  return { minutes, isPerm: false };
}

function formatDuration(minutes, isPerm = false) {
  if (isPerm) return 'perm';
  if (minutes === null || minutes === undefined) return 'unknown';
  if (minutes % (60 * 24) === 0) return `${minutes / (60 * 24)}d`;
  if (minutes % 60 === 0) return `${minutes / 60}h`;
  return `${minutes}m`;
}

module.exports = {
  parseDuration,
  formatDuration,
};
