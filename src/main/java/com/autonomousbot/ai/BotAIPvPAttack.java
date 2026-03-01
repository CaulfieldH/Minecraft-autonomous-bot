package com.autonomousbot.ai;

import com.autonomousbot.ConfigHandler;
import com.autonomousbot.entity.EntityAutonomousBot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * AI-задача: преследование и атака игрока в PvP-режиме (Minecraft 1.21.1).
 *
 * Логика:
 *  - Берёт цель, выбранную BotAIPvPTarget (через getTarget()).
 *  - При отсутствии цели — ищет ближайшего игрока в радиусе pvpRange.
 *  - Преследует до дальности ближнего боя (2.5 блока).
 *  - Атакует с кулдауном pvpAttackCooldown тиков.
 *  - Отступает при HP < pvpRetreatHealthFraction от максимума.
 *  - Игнорирует игроков в Creative-режиме.
 */
public class BotAIPvPAttack extends Goal {

    private static final double ATTACK_RANGE_SQ = 2.5 * 2.5;

    private final EntityAutonomousBot bot;
    private Player target;
    private int    attackCooldown = 0;
    private int    retreatTimer   = 0;

    public BotAIPvPAttack(EntityAutonomousBot bot) {
        this.bot = bot;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    @Override
    public boolean canUse() {
        if (bot.getBotMode() != BotMode.PVP) return false;

        LivingEntity currentTarget = bot.getTarget();
        target = (currentTarget instanceof Player p) ? p : null;
        if (target == null) {
            target = bot.level().getNearestPlayer(bot, ConfigHandler.getPvpRange());
        }
        return target != null && !target.isDeadOrDying() && !target.isCreative();
    }

    @Override
    public boolean canContinueToUse() {
        if (bot.getBotMode() != BotMode.PVP) return false;
        if (target == null || target.isDeadOrDying() || target.isCreative()) return false;

        // Отступление при низком HP
        float healthFraction = bot.getHealth() / bot.getMaxHealth();
        if (healthFraction < ConfigHandler.getPvpRetreatHealthFraction()) {
            retreatTimer = 60; // 3 секунды
            return false;
        }

        // Гистерезис дальности (×2 от обнаружения)
        double maxRangeSq = (ConfigHandler.getPvpRange() * 2.0) * (ConfigHandler.getPvpRange() * 2.0);
        return bot.distanceToSqr(target) <= maxRangeSq;
    }

    @Override
    public void start() { attackCooldown = 0; }

    @Override
    public void tick() {
        if (target == null) return;

        bot.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (attackCooldown > 0) attackCooldown--;
        if (retreatTimer  > 0) retreatTimer--;

        double distSq = bot.distanceToSqr(target);

        if (distSq <= ATTACK_RANGE_SQ) {
            // Ближний бой
            bot.getNavigation().stop();
            if (attackCooldown == 0 && ConfigHandler.isAllowDamagePlayers()) {
                bot.doHurtTarget(target);
                attackCooldown = ConfigHandler.getPvpAttackCooldown();
            }
        } else {
            // Преследование
            bot.getNavigation().moveTo(target, 0.85);
        }

        bot.setTarget(target);
    }

    @Override
    public void stop() {
        target = null;
        attackCooldown = 0;
        bot.setTarget(null);
        bot.getNavigation().stop();
    }
}
