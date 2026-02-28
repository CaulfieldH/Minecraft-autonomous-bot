package com.autonomousbot;

import com.autonomousbot.ai.BotMode;
import net.minecraftforge.common.config.Configuration;

import java.io.File;

/**
 * Обработчик конфигурационного файла.
 * Файл создаётся в папке: config/autonomousbot.cfg
 *
 * Для изменения режима бота во время игры используйте команду:
 *   /bot mode <id> <pvp|resource_gathering|building>
 */
public class ConfigHandler {

    // ─── Параметры режима ──────────────────────────────────────────────────────

    /** Режим бота по умолчанию при спавне */
    public static BotMode defaultMode = BotMode.RESOURCE_GATHERING;

    /** Радиус поиска ресурсов (блоки) */
    public static int gatheringRange = 32;

    /** Радиус обнаружения игроков в PvP (блоки) */
    public static int pvpRange = 20;

    /** Задержка между атаками в PvP (тиков; 20 = 1 сек) */
    public static int pvpAttackCooldown = 20;

    /** Порог здоровья для отступления в PvP (0.0–1.0) */
    public static float pvpRetreatHealthFraction = 0.25f;

    /** Разрешить боту наносить урон игрокам в PvP */
    public static boolean allowDamagePlayers = true;

    /** Скорость передвижения (0.1–1.0; стандарт игрока ~0.1) */
    public static double moveSpeed = 0.3;

    /** Здоровье бота (единицы; 20 = 10 сердец) */
    public static double maxHealth = 20.0;

    /** Урон бота за удар (единицы; 3 = 1.5 сердца) */
    public static double attackDamage = 3.0;

    // ─── Инициализация ─────────────────────────────────────────────────────────

    public static void init(File configFile) {
        Configuration config = new Configuration(configFile);
        try {
            config.load();

            // --- Раздел: General ---
            defaultMode = BotMode.fromString(config.getString(
                "defaultMode",
                Configuration.CATEGORY_GENERAL,
                "resource_gathering",
                "Default bot mode on spawn. Options: resource_gathering, building, pvp"
            ));

            gatheringRange = config.getInt(
                "gatheringRange",
                Configuration.CATEGORY_GENERAL,
                32, 8, 64,
                "Search radius (blocks) for resources in gathering mode"
            );

            pvpRange = config.getInt(
                "pvpRange",
                Configuration.CATEGORY_GENERAL,
                20, 5, 50,
                "Detection radius (blocks) for enemy players in PvP mode"
            );

            pvpAttackCooldown = config.getInt(
                "pvpAttackCooldown",
                Configuration.CATEGORY_GENERAL,
                20, 5, 60,
                "Ticks between PvP melee attacks (20 ticks = 1 second)"
            );

            pvpRetreatHealthFraction = config.getFloat(
                "pvpRetreatHealthFraction",
                Configuration.CATEGORY_GENERAL,
                0.25f, 0.0f, 0.9f,
                "Health fraction (0.0-1.0) below which bot retreats in PvP"
            );

            allowDamagePlayers = config.getBoolean(
                "allowDamagePlayers",
                Configuration.CATEGORY_GENERAL,
                true,
                "Allow the bot to deal damage to players in PvP mode"
            );

            moveSpeed = config.getFloat(
                "moveSpeed",
                Configuration.CATEGORY_GENERAL,
                0.3f, 0.1f, 1.0f,
                "Bot movement speed (0.1-1.0)"
            );

            maxHealth = config.getFloat(
                "maxHealth",
                Configuration.CATEGORY_GENERAL,
                20.0f, 1.0f, 200.0f,
                "Bot max health in units (20 = 10 hearts)"
            );

            attackDamage = config.getFloat(
                "attackDamage",
                Configuration.CATEGORY_GENERAL,
                3.0f, 0.5f, 30.0f,
                "Bot attack damage per hit in units"
            );

        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }
}
