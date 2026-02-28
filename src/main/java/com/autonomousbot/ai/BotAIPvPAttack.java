package com.autonomousbot.ai;

import com.autonomousbot.ConfigHandler;
import com.autonomousbot.entity.EntityAutonomousBot;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;

/**
 * AI-задача: преследование и атака игрока в PvP-режиме.
 *
 * Логика:
 *  - Ищет ближайшего игрока в радиусе pvpRange (конфиг).
 *  - Преследует цель до дальности ближнего боя (2.5 блока).
 *  - Атакует с кулдауном pvpAttackCooldown тиков.
 *  - Отступает, если HP < pvpRetreatHealthFraction от максимума.
 *  - Не атакует игроков в Creative-режиме.
 */
public class BotAIPvPAttack extends EntityAIBase {

    private static final double ATTACK_RANGE_SQ = 2.5 * 2.5;

    private final EntityAutonomousBot bot;
    private EntityPlayer target;
    private int attackCooldown = 0;
    private int retreatTimer   = 0;

    public BotAIPvPAttack(EntityAutonomousBot bot) {
        this.bot = bot;
        this.setMutexBits(3); // Блокирует другие задачи движения и взгляда
    }

    // ─── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    public boolean shouldExecute() {
        if (bot.getBotMode() != BotMode.PVP) return false;

        // Берём цель, выбранную BotAIPvPTarget
        target = (EntityPlayer) bot.getAttackTarget();
        if (target == null) {
            target = bot.worldObj.getClosestPlayerToEntity(bot, ConfigHandler.pvpRange);
        }

        return target != null && !target.isDead && !target.isCreativeMode();
    }

    @Override
    public boolean continueExecuting() {
        if (bot.getBotMode() != BotMode.PVP) return false;
        if (target == null || target.isDead || target.isCreativeMode()) return false;

        // Отступление при низком HP
        float healthFraction = bot.getHealth() / bot.getMaxHealth();
        if (healthFraction < ConfigHandler.pvpRetreatHealthFraction) {
            retreatTimer = 60; // отступаем 3 секунды
            return false;
        }

        // Проверяем дальность (× 2 для гистерезиса)
        double maxRangeSq = (ConfigHandler.pvpRange * 2.0) * (ConfigHandler.pvpRange * 2.0);
        return bot.getDistanceSqToEntity(target) <= maxRangeSq;
    }

    @Override
    public void startExecuting() {
        attackCooldown = 0;
    }

    @Override
    public void updateTask() {
        if (target == null) return;

        // Смотрим на цель
        bot.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);

        if (attackCooldown > 0) attackCooldown--;
        if (retreatTimer  > 0) retreatTimer--;

        double distSq = bot.getDistanceSqToEntity(target);

        if (distSq <= ATTACK_RANGE_SQ) {
            // ─── Ближний бой ───────────────────────────────────────────────
            bot.getNavigator().clearPathEntity();

            if (attackCooldown == 0 && ConfigHandler.allowDamagePlayers) {
                bot.attackEntityAsMob(target);
                attackCooldown = ConfigHandler.pvpAttackCooldown;
            }
        } else {
            // ─── Преследование ─────────────────────────────────────────────
            bot.getNavigator().tryMoveToEntityLiving(target, 0.85);
        }

        bot.setAttackTarget(target);
    }

    @Override
    public void resetTask() {
        target = null;
        attackCooldown = 0;
        bot.setAttackTarget(null);
        bot.getNavigator().clearPathEntity();
    }
}
