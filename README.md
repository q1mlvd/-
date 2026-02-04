# CasinoFame

Плагин казино для сервера **FameCube** (Minecraft 1.16.5). Полностью GUI, валюта — **CHIPS**.

## Установка
1. Соберите JAR по инструкции ниже.
2. Скопируйте `CasinoFame-1.0.0.jar` в папку `plugins/` сервера.
3. Установите Vault и экономику (например, EssentialsX) для работы магазина.
4. Запустите сервер.
5. Проверьте, что созданы файлы `config.yml`, `messages.yml`, `gui.yml`, `players.yml`, `fairness.yml`, `casino-fairness.log`.

## Сборка
1. Установите Java 8 или 16 и Maven.
2. В корне проекта выполните: `mvn clean package`.
3. Готовый файл будет в `target/CasinoFame-1.0.0.jar`.

## Команды
- `/casino` — открыть главное меню.
- `/chips` — открыть магазин фишек.
- `/casino reload` — перезагрузить конфиги.
- `/casino fairness <roundId>` — проверить честность раунда.
- `/casino chips give <player> <amount>` — выдать фишки.
- `/casino chips take <player> <amount>` — забрать фишки.

## Права
- `casinofame.use`
- `casinofame.admin`
- `casinofame.reload`
- `casinofame.fairness`
- `casinofame.chips.admin`
- `casinofame.bypass.cooldown`

## Java
Код совместим с **Java 8** (компиляция) и корректно работает на **Java 8/16**.
