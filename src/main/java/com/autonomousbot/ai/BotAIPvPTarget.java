package com.autonomousbot.ai;

import com.autonomousbot.ConfigHandler;
import com.autonomousbot.entity.EntityAutonomousBot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * AI-задача: поиск ближайшей цели в PvP-режиме (Minecraft 1.21.1).
 *
 * Запускается однократно, передаёт цель боту через setTarget(),
 * после чего немедленно завершает работу (canContinueToUse = false).
 * Непосредственное преследование и атака — в BotAIPvPAttack.
 */
public class BotAIPvPTarget extends Goal {

    private final EntityAutonomousBot bot;

    public BotAIPvPTarget(EntityAutonomousBot bot) {
        this.bot = bot;
        this.setFlags(EnumSet.noneOf(Goal.Flag.class)); // не блокирует движение
    }

    @Override
    public boolean canUse() {
        if (bot.getBotMode() != BotMode.PVP) return false;

        // Не перебиваем живую цель
        if (bot.getTarget() != null && !bot.getTarget().isDeadOrDying()) return false;

        Player nearest = bot.level().getNearestPlayer(bot, ConfigHandler.getPvpRange());
        if (nearest != null && !nearest.isCreative() && !nearest.isDeadOrDying()) {
            bot.setTarget(nearest);
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        // Только устанавливает цель, не продолжает сам
        return false;
    }
}
