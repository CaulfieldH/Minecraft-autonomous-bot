# AutonomousBot — Руководство по настройке и запуску

> Мод для **MinecraftEdu 1.7.10** (Forge `10.13.4.1614`)
> Автономный NPC-бот с тремя режимами: сбор ресурсов, строительство, PvP.

---

## Содержание

- [Требования](#требования)
- [Пошаговая сборка мода](#пошаговая-сборка-мода)
- [Установка на сервер / клиент](#установка-на-сервер--клиент)
- [Конфигурационный файл](#конфигурационный-файл)
- [Игровые команды](#игровые-команды)
- [Структура проекта](#структура-проекта)
- [Часто задаваемые вопросы](#часто-задаваемые-вопросы)

---

## Требования

| Компонент          | Версия / ссылка                                          |
|--------------------|----------------------------------------------------------|
| Java JDK           | **8** (1.8.x)                                            |
| Minecraft          | **1.7.10** (через MinecraftEdu или обычный лаунчер)      |
| Minecraft Forge    | **10.13.4.1614** (рекомендуемая сборка для 1.7.10)       |
| Gradle             | Включён в Forge MDK (wrapper), или установите **2.14**   |
| Git (опционально)  | Для клонирования репозитория                             |

---

## Пошаговая сборка мода

### Шаг 1 — Скачать Forge MDK

1. Перейдите на https://files.minecraftforge.net/net/minecraftforge/forge/index_1.7.10.html
2. Найдите версию **10.13.4.1614** → **Mdk** (или Src) → скачайте zip.
3. Распакуйте MDK в любую папку, например `C:\forge-mdk\`.

### Шаг 2 — Скопировать исходники мода

Скопируйте всю папку `autonomousbot-mod/` в корень MDK, заменив `build.gradle`,
`gradle.properties`, `settings.gradle` и папку `src/`:

```
forge-mdk/
├── build.gradle          ← из нашего проекта
├── gradle.properties     ← из нашего проекта
├── settings.gradle       ← из нашего проекта
├── gradlew
├── gradlew.bat
└── src/
    └── main/
        ├── java/com/autonomousbot/   ← наш код
        └── resources/                ← mcmod.info, pack.mcmeta
```

### Шаг 3 — Настроить рабочее пространство Forge

Откройте терминал/командную строку в папке MDK:

```bash
# Windows
gradlew.bat setupDecompWorkspace

# Linux / macOS
chmod +x gradlew
./gradlew setupDecompWorkspace
```

> ⏳ Первый запуск займёт 10–20 минут — Gradle скачивает зависимости и декомпилирует Minecraft.

### Шаг 4 — Собрать JAR

```bash
# Windows
gradlew.bat build

# Linux / macOS
./gradlew build
```

Готовый файл появится в:

```
build/libs/AutonomousBot-1.7.10-1.0.0.jar
```

> Если сборка завершается ошибкой `Could not resolve net.minecraftforge.gradle:ForgeGradle:1.2-SNAPSHOT`,
> замените строку в `build.gradle`:
> ```groovy
> classpath 'net.minecraftforge.gradle:ForgeGradle:1.2-SNAPSHOT'
> ```
> на форк с поддержкой современных JVM:
> ```groovy
> classpath 'com.anatawa12.forge:ForgeGradle:1.2-1.0.3'
> ```
> и добавьте репозиторий:
> ```groovy
> maven { url 'https://maven.minecraftforge.net' }
> ```

---

## Установка на сервер / клиент

### Серверная установка (MinecraftEdu Server / Forge Server)

1. Убедитесь, что сервер запущен с **Forge 10.13.4.1614** для 1.7.10.
2. Положите `AutonomousBot-1.7.10-1.0.0.jar` в папку `mods/` сервера.
3. Запустите сервер — конфиг создастся автоматически:
   ```
   server/config/autonomousbot.cfg
   ```
4. При необходимости отредактируйте конфиг и перезапустите сервер.

### Клиентская установка

1. Откройте папку `.minecraft` (или `.minecraftEdu`):
   - Windows: `%AppData%\MinecraftEdu\`
   - macOS:   `~/Library/Application Support/MinecraftEdu/`
   - Linux:   `~/.minecraftEdu/`
2. Скопируйте JAR в папку `mods/`.
3. Запустите клиент с профилем Forge 1.7.10.

> **Важно:** мод нужен **и на клиенте, и на сервере**. Серверная сторона управляет AI,
> клиентская — рендером.

---

## Конфигурационный файл

Путь: `config/autonomousbot.cfg`

Файл создаётся автоматически при первом запуске. Пример с пояснениями:

```properties
####################
# general
####################
general {
    # Default bot mode on spawn. Options: resource_gathering, building, pvp
    S:defaultMode=resource_gathering

    # Search radius (blocks) for resources in gathering mode [range: 8 ~ 64, default: 32]
    I:gatheringRange=32

    # Detection radius (blocks) for enemy players in PvP mode [range: 5 ~ 50, default: 20]
    I:pvpRange=20

    # Ticks between PvP melee attacks (20 ticks = 1 second) [range: 5 ~ 60, default: 20]
    I:pvpAttackCooldown=20

    # Health fraction (0.0–1.0) below which bot retreats in PvP [range: 0.0 ~ 0.9, default: 0.25]
    S:pvpRetreatHealthFraction=0.25

    # Allow the bot to deal damage to players in PvP mode [default: true]
    B:allowDamagePlayers=true

    # Bot movement speed (0.1–1.0) [default: 0.3]
    S:moveSpeed=0.3

    # Bot max health in units (20 = 10 hearts) [range: 1.0 ~ 200.0, default: 20.0]
    S:maxHealth=20.0

    # Bot attack damage per hit in units [range: 0.5 ~ 30.0, default: 3.0]
    S:attackDamage=3.0
}
```

### Быстрое переключение режима по умолчанию

Отредактируйте `S:defaultMode=pvp` и перезапустите сервер.
Уже спавненных ботов переключайте командой `/bot mode`.

---

## Игровые команды

> Требуется право оператора (уровень 2). Введите `/op <ник>` для выдачи прав.

| Команда | Описание |
|---------|----------|
| `/bot spawn` | Спавнит бота рядом с вами |
| `/bot spawn <x> <y> <z>` | Спавнит бота по координатам |
| `/bot kill <id>` | Удаляет бота с указанным ID |
| `/bot mode <id> resource_gathering` | Режим сбора ресурсов |
| `/bot mode <id> building` | Режим строительства |
| `/bot mode <id> pvp` | Режим PvP |
| `/bot info <id>` | Показывает HP, режим, позицию и инвентарь бота |
| `/bot list` | Список всех активных ботов |
| `/bot buildreset <id>` | Сброс флага «убежище построено» (бот построит ещё одно) |

### Примеры

```
/bot spawn
/bot spawn 100 64 200
/bot list
/bot mode 42 pvp
/bot mode 42 resource_gathering
/bot info 42
/bot kill 42
/bot buildreset 42
```

---

## Структура проекта

```
autonomousbot-mod/
├── build.gradle                                    ← Forge/Gradle сборка
├── gradle.properties                               ← Версии
├── settings.gradle                                 ← Имя проекта
└── src/main/
    ├── java/com/autonomousbot/
    │   ├── AutonomousBot.java          ← @Mod: точка входа, регистрация
    │   ├── ConfigHandler.java          ← Чтение/запись autonomousbot.cfg
    │   ├── CommandBotControl.java      ← Команда /bot
    │   ├── proxy/
    │   │   ├── CommonProxy.java        ← Серверный прокси
    │   │   └── ClientProxy.java        ← Клиентский прокси (рендер)
    │   ├── entity/
    │   │   └── EntityAutonomousBot.java ← Сущность бота + инвентарь
    │   └── ai/
    │       ├── BotMode.java            ← Enum режимов (3 штуки)
    │       ├── BotAIGatherResources.java ← AI: добыча ресурсов
    │       ├── BotAIBuildShelter.java  ← AI: строительство 5×5×4 дома
    │       ├── BotAIPvPAttack.java     ← AI: преследование и атака
    │       ├── BotAIPvPTarget.java     ← AI: выбор цели в PvP
    │       └── BotAICollectItems.java  ← AI: подбор выпавших предметов
    └── resources/
        ├── mcmod.info                  ← Метаданные мода
        └── pack.mcmeta                 ← Формат ресурс-пака
```

---

## Поведение бота

### Режим `resource_gathering` (Сбор ресурсов)

- Сканирует блоки в радиусе `gatheringRange` (по умолчанию 32 блока).
- Приоритет добычи: **алмазная руда → золото → железо → уголь → дерево → камень/песок**.
- Идёт к блоку, ждёт 2–4 секунды (имитация ломания) и убирает блок.
- Выпавшие предметы подбирает автоматически (AI BotAICollectItems).
- Ищет следующую цель сразу после.

### Режим `building` (Строительство)

- Отходит на ~10 блоков к востоку, находит уровень земли.
- Строит **5×5×4 коробку** из блоков грязи (dirt):
  - Стены по периметру, высота 3 блока.
  - Дверной проём (2×1) на южной стене.
  - Полная крыша сверху.
- После завершения переходит в режим ожидания.
- Сбросить и построить заново: `/bot buildreset <id>`.

### Режим `pvp` (PvP-бой)

- Ищет ближайшего игрока в радиусе `pvpRange` (20 блоков).
- **Не атакует игроков в Creative-режиме.**
- Преследует и атакует в ближнем бою, кулдаун `pvpAttackCooldown` тиков.
- При HP < 25% отступает на 3 секунды, затем возобновляет атаку.
- Не подбирает предметы во время активного боя.

---

## Часто задаваемые вопросы

**Q: Бот не двигается / стоит на месте**
A: Убедитесь, что вокруг нет блоков-преград. Бот использует стандартный
   патфайндинг Forge — он не обходит очень узкие проходы.

**Q: Ошибка при сборке: `Could not resolve ForgeGradle`**
A: Старые серверы Maven недоступны. Используйте форк anatawa12 (см. Шаг 4).

**Q: Несколько ботов конфликтуют**
A: Нормально — боты независимы, каждый ищет свои ресурсы.
   В PvP-режиме они могут атаковать одну цель.

**Q: Могу ли я сделать бота дружественным (не атаковать конкретных игроков)?**
A: В текущей версии — нет. Бот в PvP атакует всех игроков не в Creative.
   Для белого списка потребуется доработка `BotAIPvPTarget`.

**Q: Мод работает на чистом Minecraft 1.7.10 без MinecraftEdu?**
A: Да. MinecraftEdu 1.7.10 основан на стандартном Forge-Minecraft, поэтому мод
   совместим с обоими вариантами.

**Q: Как добавить больше ботов?**
A: Вызовите `/bot spawn` несколько раз. Каждый бот получает уникальный ID.
   Смотрите список через `/bot list`.

---

## Лицензия

Исходный код предоставляется «как есть» для образовательных целей в рамках MinecraftEdu.
Свободно используйте и модифицируйте для своих проектов.
