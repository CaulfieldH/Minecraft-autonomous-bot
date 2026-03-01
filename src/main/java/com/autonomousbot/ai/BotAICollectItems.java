package com.autonomousbot.ai;

import com.autonomousbot.entity.EntityAutonomousBot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * AI-задача: подбор выпавших предметов с земли (Minecraft 1.21.1).
 *
 * Активна во всех режимах, кроме PvP с активной целью.
 * Ищет ближайший ItemEntity в радиусе COLLECT_RANGE,
 * идёт к нему и добавляет в инвентарь бота.
 */
public class BotAICollectItems extends Goal {

    private static final double COLLECT_RANGE   = 12.0;
    private static final double PICKUP_RANGE_SQ = 1.0;

    private final EntityAutonomousBot bot;
    private ItemEntity targetItem;

    public BotAICollectItems(EntityAutonomousBot bot) {
        this.bot = bot;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    @Override
    public boolean canUse() {
        // В PvP с активной целью не отвлекаемся на предметы
        if (bot.getBotMode() == BotMode.PVP && bot.getTarget() != null) return false;
        targetItem = findNearestItem();
        return targetItem != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (bot.getBotMode() == BotMode.PVP && bot.getTarget() != null) return false;
        return targetItem != null && !targetItem.isRemoved();
    }

    @Override
    public void tick() {
        if (targetItem == null || targetItem.isRemoved()) {
            targetItem = findNearestItem();
            return;
        }

        bot.getLookControl().setLookAt(targetItem, 10.0F, 10.0F);
        double distSq = bot.distanceToSqr(targetItem);

        if (distSq <= PICKUP_RANGE_SQ) {
            ItemStack stack = targetItem.getItem();
            if (!stack.isEmpty()) {
                bot.addToInventory(stack.copy());
                targetItem.discard();
            }
            targetItem = null;
        } else {
            bot.getNavigation().moveTo(targetItem, 0.9);
        }
    }

    @Override
    public void stop() {
        targetItem = null;
        bot.getNavigation().stop();
    }

    // ─── Поиск ближайшего предмета ───────────────────────────────────────────────

    private ItemEntity findNearestItem() {
        AABB searchBox = new AABB(
            bot.getX() - COLLECT_RANGE, bot.getY() - 3, bot.getZ() - COLLECT_RANGE,
            bot.getX() + COLLECT_RANGE, bot.getY() + 3, bot.getZ() + COLLECT_RANGE
        );

        List<ItemEntity> items = bot.level().getEntitiesOfClass(ItemEntity.class, searchBox);

        ItemEntity nearest    = null;
        double     nearestDist = Double.MAX_VALUE;

        for (ItemEntity item : items) {
            if (item.isRemoved()) continue;
            double dist = bot.distanceToSqr(item);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest     = item;
            }
        }
        return nearest;
    }
}
