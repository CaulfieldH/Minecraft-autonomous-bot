# AutonomousBot — Руководство по настройке и запуску

> Мод для **Minecraft 1.21.1** + **NeoForge 21.1.x**
> Автономный NPC-бот с тремя режимами: сбор ресурсов, строительство, PvP.

---

## Содержание

- [Требования](#требования)
- [Пошаговая сборка мода](#пошаговая-сборка-мода)
- [Установка на сервер / клиент](#установка-на-сервер--клиент)
- [Конфигурационный файл](#конфигурационный-файл)
- [Игровые команды](#игровые-команды)
- [Структура проекта](#структура-проекта)
- [Поведение бота](#поведение-бота)
- [Часто задаваемые вопросы](#часто-задаваемые-вопросы)

---

## Требования

| Компонент        | Версия                                                               |
|------------------|----------------------------------------------------------------------|
| Java JDK         | **21** (Eclipse Temurin / Oracle JDK 21+)                           |
| Minecraft        | **1.21.1**                                                           |
| NeoForge         | **21.1.77** (или любой 21.1.x)                                      |
| Gradle           | Включён в репозиторий через Wrapper (использует Gradle 8.x)          |
| Git              | Для клонирования репозитория                                         |

> **Важно:** Java 8 или 17 **не подходят**. Minecraft 1.21.1 требует Java 21.

---

## Пошаговая сборка мода

### Шаг 1 — Установить Java 21

Скачайте JDK 21 (например, Eclipse Temurin):
https://adoptium.net/temurin/releases/?version=21

Проверьте установку:
```bash
java -version
# Должно быть: openjdk version "21.x.x" ...
```

### Шаг 2 — Скачать NeoForge MDK (опционально)

Если вы хотите использовать официальный MDK как основу:
1. Перейдите на https://neoforged.net/
2. Найдите версию **21.1.77** → скачайте MDK.
3. Распакуйте в любую папку.

Если используете **этот репозиторий** напрямую — Шаг 2 можно пропустить.
Все файлы конфигурации сборки уже находятся в корне проекта.

### Шаг 3 — Клонировать / скопировать исходники

```bash
git clone https://github.com/CaulfieldH/Minecraft-autonomous-bot.git
cd Minecraft-autonomous-bot/autonomousbot-mod
```

Структура совместима с MDK — все файлы уже на своих местах:

```
autonomousbot-mod/
├── build.gradle
├── gradle.properties
├── settings.gradle
├── gradlew  (Linux/macOS)
├── gradlew.bat  (Windows)
└── src/main/
    ├── java/com/autonomousbot/
    └── resources/
```

### Шаг 4 — Собрать JAR

Откройте терминал в папке `autonomousbot-mod/` и выполните:

```bash
# Linux / macOS
chmod +x gradlew
./gradlew build

# Windows
gradlew.bat build
```

> **Первый запуск** займёт несколько минут — Gradle скачает NeoForge и зависимости.

Готовый JAR появится в:
```
build/libs/autonomousbot-2.0.0.jar
```

### Шаг 5 — Запустить тестовый клиент (опционально)

```bash
# Запустить клиент Minecraft с модом для тестирования
./gradlew runClient

# Запустить выделенный сервер
./gradlew runServer
```

---

## Установка на сервер / клиент

### Установка NeoForge-сервера

1. Скачайте NeoForge-инсталлятор для 1.21.1:
   https://neoforged.net/ → Releases → 21.1.77
2. Запустите инсталлятор:
   ```bash
   java -jar neoforge-21.1.77-installer.jar --installServer
   ```
3. Скопируйте `autonomousbot-2.0.0.jar` в папку `mods/`.
4. Запустите сервер — конфиг создастся автоматически:
   ```
   config/autonomousbot-common.toml
   ```

### Установка на клиент

1. Установите NeoForge 21.1.77 через официальный инсталлятор или лаунчер (CurseForge, Modrinth).
2. Скопируйте `autonomousbot-2.0.0.jar` в папку `.minecraft/mods/`.
3. Запустите клиент с профилем NeoForge 1.21.1.

> **Важно:** мод нужен **и на клиенте, и на сервере**.

---

## Конфигурационный файл

Путь: `config/autonomousbot-common.toml`

Файл создаётся автоматически при первом запуске. Пример:

```toml
["general"]
    # Default bot mode on spawn. Options: resource_gathering, building, pvp
    defaultMode = "resource_gathering"

    # Search radius (blocks) for resources in gathering mode [range: 8 ~ 64]
    gatheringRange = 32

    # Detection radius (blocks) for enemy players in PvP mode [range: 5 ~ 50]
    pvpRange = 20

    # Ticks between PvP melee attacks (20 ticks = 1 second) [range: 5 ~ 60]
    pvpAttackCooldown = 20

    # Health fraction (0.0–1.0) below which bot retreats in PvP [range: 0.0 ~ 0.9]
    pvpRetreatHealthFraction = 0.25

    # Allow the bot to deal damage to players in PvP mode
    allowDamagePlayers = true

    # Bot movement speed [range: 0.1 ~ 1.0]
    moveSpeed = 0.3

    # Bot max health in units (20 = 10 hearts) [range: 1.0 ~ 200.0]
    maxHealth = 20.0

    # Bot attack damage per hit in units (3 = 1.5 hearts) [range: 0.5 ~ 30.0]
    attackDamage = 3.0
```

Изменения в конфиге применяются **после перезапуска сервера**.
Для смены режима уже спавненным ботам используйте команду `/bot mode`.

---

## Игровые команды

> Требуется право оператора (уровень 2). Введите `/op <ник>` для выдачи прав.

| Команда | Описание |
|---------|----------|
| `/bot spawn` | Спавнит бота в 2 блоках от вас |
| `/bot spawn <x> <y> <z>` | Спавнит бота по координатам |
| `/bot kill <id>` | Удаляет бота с указанным ID |
| `/bot mode <id> resource_gathering` | Переключить в режим сбора ресурсов |
| `/bot mode <id> building` | Переключить в режим строительства |
| `/bot mode <id> pvp` | Переключить в режим PvP |
| `/bot info <id>` | Показывает HP, режим, позицию и инвентарь |
| `/bot list` | Список всех активных ботов |
| `/bot buildreset <id>` | Сбросить флаг «убежище построено» |

> Команды поддерживают **Tab-автодополнение**: наберите `/bot m` + Tab.

### Примеры

```
/bot spawn
/bot spawn 100 64 200
/bot list
/bot mode 42 pvp
/bot info 42
/bot kill 42
/bot buildreset 42
```

---

## Структура проекта

```
autonomousbot-mod/
├── build.gradle                         ← NeoForge / Gradle 8 сборка
├── gradle.properties                    ← Версии (mod_version, neoforge_version)
├── settings.gradle                      ← Имя проекта, репозитории плагинов
└── src/main/
    ├── java/com/autonomousbot/
    │   ├── AutonomousBot.java           ← @Mod: регистрация, атрибуты, команды
    │   ├── ConfigHandler.java           ← ModConfigSpec → autonomousbot-common.toml
    │   ├── CommandBotControl.java       ← Brigadier-команда /bot
    │   ├── entity/
    │   │   └── EntityAutonomousBot.java ← Сущность бота (Monster + инвентарь)
    │   └── ai/
    │       ├── BotMode.java             ← Enum: RESOURCE_GATHERING / BUILDING / PVP
    │       ├── BotAIGatherResources.java ← Goal: добыча (теги BlockTags)
    │       ├── BotAIBuildShelter.java   ← Goal: постройка укрытия 5×5×4
    │       ├── BotAIPvPAttack.java      ← Goal: преследование и атака
    │       ├── BotAIPvPTarget.java      ← Goal: выбор цели в PvP
    │       └── BotAICollectItems.java   ← Goal: подбор ItemEntity с земли
    └── resources/
        ├── META-INF/
        │   └── neoforge.mods.toml       ← Метаданные мода (заменяет mcmod.info)
        └── pack.mcmeta                  ← Формат ресурс-пака (pack_format: 34)
```

---

## Поведение бота

### Режим `resource_gathering` — Сбор ресурсов

- Сканирует блоки в радиусе `gatheringRange` (по умолчанию 32) с шагом 2 блока.
- Приоритет добычи: **алмазная руда → золото → железо → уголь → дерево → камень/песок**.
- Использует теги блоков (`BlockTags.DIAMOND_ORES` и др.) — автоматически охватывает
  обычную и глубинную (deepslate) разновидности руды.
- Добыча занимает 2–4 секунды в зависимости от типа блока.
- Выпавшие предметы подбирает автоматически (AI `BotAICollectItems`).

### Режим `building` — Строительство

- Отходит ~10 блоков к востоку, находит поверхность земли.
- Строит **5×5×4 коробку** из блоков грязи (Dirt):
  - Стены по периметру, высота 3 блока.
  - Дверной проём (2×1) на северной стене.
  - Полная крыша сверху.
- После завершения переходит в режим ожидания.
- Сбросить и построить заново: `/bot buildreset <id>`.

### Режим `pvp` — PvP-бой

- Ищет ближайшего игрока в радиусе `pvpRange` (20 блоков).
- **Не атакует игроков в Creative-режиме.**
- Преследует и атакует в ближнем бою, кулдаун `pvpAttackCooldown` тиков.
- При HP < 25% от максимума отступает на 3 секунды.
- Не отвлекается на предметы во время активного преследования.

---

## Ключевые отличия от версии 1.7.10

| Аспект | Старая версия (1.7.10 / Forge) | Новая версия (1.21.1 / NeoForge) |
|--------|-------------------------------|----------------------------------|
| Загрузчик | Forge 10.13.x | **NeoForge 21.1.x** |
| Java | 8 | **21** |
| Регистрация сущности | `EntityRegistry.registerModEntity` | **`DeferredRegister<EntityType<?>>`** |
| Метаданные мода | `mcmod.info` | **`META-INF/neoforge.mods.toml`** |
| Конфиг | `Configuration` (`.cfg`) | **`ModConfigSpec`** (`.toml`) |
| Команды | `CommandBase` | **Brigadier** (Tab-дополнение из коробки) |
| AI-задачи | `EntityAIBase` | **`Goal`** |
| Методы AI | `shouldExecute` / `updateTask` | **`canUse` / `tick`** |
| Атрибуты | `applyEntityAttributes()` | **`createAttributes()` static + Event** |
| NBT | `writeEntityToNBT` / `readEntityFromNBT` | **`addAdditionalSaveData` / `readAdditionalSaveData`** |
| Мир | `worldObj` | **`level()`** |
| Навигация | `getNavigator().tryMoveToXYZ()` | **`getNavigation().moveTo()`** |
| Теги блоков | конкретные блоки (`Blocks.gold_ore`) | **`BlockTags.GOLD_ORES`** (все варианты) |
| Удаление сущности | `setDead()` | **`discard()`** |
| Прокси-классы | `ClientProxy / CommonProxy` | **Удалены** (не нужны) |

---

## Часто задаваемые вопросы

**Q: Бот не движется / стоит на месте**
A: Убедитесь, что вокруг нет преград. Бот использует стандартный NeoForge-патфайндинг.
   Также проверьте, что бот находится в загруженном чанке.

**Q: Ошибка сборки: `Could not resolve net.neoforged:neoforge`**
A: Убедитесь, что в `settings.gradle` присутствует репозиторий NeoForge:
   `maven { url = 'https://maven.neoforged.net/releases' }`.

**Q: Несколько ботов конфликтуют**
A: Это нормально — каждый бот независим. В режиме gathering они могут
   пытаться добыть один и тот же блок, но первый успевшего «съест» его.

**Q: Как отключить PvP-урон для тестирования?**
A: Установите в конфиге: `allowDamagePlayers = false`. Бот будет преследовать,
   но не наносить урон.

**Q: Мод работает на клиенте без сервера (singleplayer)?**
A: Да. В одиночной игре серверная логика работает локально.

**Q: Как добавить больше ботов?**
A: Вызывайте `/bot spawn` несколько раз. Каждый бот получает уникальный ID.
   Смотрите список через `/bot list`.

---

## Лицензия

Исходный код предоставляется для образовательных и некоммерческих целей.
Свободно используйте и модифицируйте для своих проектов.
