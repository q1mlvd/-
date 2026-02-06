# Discord бот «Пехота Зенита» (стафф)

## Требования
- Node.js 18+
- discord.js v14
- SQLite (локальный файл `staff.db`)

## Интенты
- Guilds
- GuildMembers
- GuildModeration
- GuildMessages
- GuildVoiceStates

## Права бота
- Manage Roles
- Moderate Members
- Send Messages
- View Channels

**Важно:** роль бота должна быть выше всех стафф-ролей.

## Установка
```bash
npm i
```

## Настройка
1) Создайте `.env` на основе `.env.example`:
```env
DISCORD_TOKEN=ваш_токен
CLIENT_ID=app_client_id
```
2) Проверьте `config.yml` (все ID уже заполнены).

## Регистрация команд
```bash
npm run register
```

## Запуск
```bash
node src/index.js
```

## Логи
- Логи пишутся в `logs/bot-YYYY-MM-DD.log`
- Все события системы пишутся в `STAFF_JOURNAL`
