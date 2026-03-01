package com.autonomousbot;

import com.autonomousbot.ai.BotMode;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Конфигурация мода через NeoForge ModConfigSpec.
 * Файл создаётся в: config/autonomousbot-common.toml
 */
public class ConfigHandler {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.EnumValue<BotMode>  DEFAULT_MODE;
    private static final ModConfigSpec.IntValue             GATHERING_RANGE;
    private static final ModConfigSpec.IntValue             PVP_RANGE;
    private static final ModConfigSpec.IntValue             PVP_ATTACK_COOLDOWN;
    private static final ModConfigSpec.DoubleValue          PVP_RETREAT_HEALTH_FRACTION;
    private static final ModConfigSpec.BooleanValue         ALLOW_DAMAGE_PLAYERS;
    private static final ModConfigSpec.DoubleValue          MOVE_SPEED;
    private static final ModConfigSpec.DoubleValue          MAX_HEALTH;
    private static final ModConfigSpec.DoubleValue          ATTACK_DAMAGE;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.comment("AutonomousBot Configuration").push("general");

        DEFAULT_MODE = BUILDER
            .comment("Default bot mode on spawn. Options: resource_gathering, building, pvp")
            .defineEnum("defaultMode", BotMode.RESOURCE_GATHERING);

        GATHERING_RANGE = BUILDER
            .comment("Search radius (blocks) for resources in gathering mode")
            .defineInRange("gatheringRange", 32, 8, 64);

        PVP_RANGE = BUILDER
            .comment("Detection radius (blocks) for enemy players in PvP mode")
            .defineInRange("pvpRange", 20, 5, 50);

        PVP_ATTACK_COOLDOWN = BUILDER
            .comment("Ticks between melee attacks in PvP (20 ticks = 1 second)")
            .defineInRange("pvpAttackCooldown", 20, 5, 60);

        PVP_RETREAT_HEALTH_FRACTION = BUILDER
            .comment("Health fraction (0.0-1.0) below which bot retreats in PvP")
            .defineInRange("pvpRetreatHealthFraction", 0.25, 0.0, 0.9);

        ALLOW_DAMAGE_PLAYERS = BUILDER
            .comment("Allow the bot to deal damage to players in PvP mode")
            .define("allowDamagePlayers", true);

        MOVE_SPEED = BUILDER
            .comment("Bot movement speed (0.1-1.0; default player ~0.1)")
            .defineInRange("moveSpeed", 0.3, 0.1, 1.0);

        MAX_HEALTH = BUILDER
            .comment("Bot max health in units (20 = 10 hearts)")
            .defineInRange("maxHealth", 20.0, 1.0, 200.0);

        ATTACK_DAMAGE = BUILDER
            .comment("Bot attack damage per hit in units (3 = 1.5 hearts)")
            .defineInRange("attackDamage", 3.0, 0.5, 30.0);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static BotMode getDefaultMode()              { return DEFAULT_MODE.get(); }
    public static int     getGatheringRange()           { return GATHERING_RANGE.get(); }
    public static int     getPvpRange()                 { return PVP_RANGE.get(); }
    public static int     getPvpAttackCooldown()        { return PVP_ATTACK_COOLDOWN.get(); }
    public static double  getPvpRetreatHealthFraction() { return PVP_RETREAT_HEALTH_FRACTION.get(); }
    public static boolean isAllowDamagePlayers()        { return ALLOW_DAMAGE_PLAYERS.get(); }
    public static double  getMoveSpeed()                { return MOVE_SPEED.get(); }
    public static double  getMaxHealth()                { return MAX_HEALTH.get(); }
    public static double  getAttackDamage()             { return ATTACK_DAMAGE.get(); }
}
