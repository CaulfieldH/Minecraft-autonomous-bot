package com.autonomousbot.ai;

import com.autonomousbot.ConfigHandler;
import com.autonomousbot.entity.EntityAutonomousBot;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;

/**
 * AI-задача: поиск ближайшей цели в PvP-режиме.
 * Запускается один раз и передаёт цель боту через setAttackTarget().
 * Непосредственное преследование и атака — в BotAIPvPAttack.
 */
public class BotAIPvPTarget extends EntityAIBase {

    private final EntityAutonomousBot bot;

    public BotAIPvPTarget(EntityAutonomousBot bot) {
        this.bot = bot;
        this.setMutexBits(0); // Таргетинг не блокирует движение
    }

    @Override
    public boolean shouldExecute() {
        if (bot.getBotMode() != BotMode.PVP) return false;
        if (bot.getAttackTarget() != null && !bot.getAttackTarget().isDead) return false;

        EntityPlayer nearest = bot.worldObj.getClosestPlayerToEntity(
            bot, ConfigHandler.pvpRange
        );

        if (nearest != null && !nearest.isCreativeMode() && !nearest.isDead) {
            bot.setAttackTarget(nearest);
            return true;
        }
        return false;
    }

    @Override
    public boolean continueExecuting() {
        // Только устанавливает цель, не продолжает сам
        return false;
    }
}
