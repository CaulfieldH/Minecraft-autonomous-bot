package com.autonomousbot.ai;

import com.autonomousbot.entity.EntityAutonomousBot;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;

import java.util.List;

/**
 * AI-задача: подбор выпавших предметов с земли.
 *
 * Активна во всех режимах, кроме PvP с активной целью.
 * Ищет ближайший EntityItem в радиусе COLLECT_RANGE,
 * идёт к нему и добавляет в инвентарь бота.
 */
public class BotAICollectItems extends EntityAIBase {

    private static final double COLLECT_RANGE  = 12.0;
    private static final double PICKUP_RANGE_SQ = 1.0; // 1 блок

    private final EntityAutonomousBot bot;
    private EntityItem targetItem;

    public BotAICollectItems(EntityAutonomousBot bot) {
        this.bot = bot;
        this.setMutexBits(1); // Использует движение, но не взгляд
    }

    // ─── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    public boolean shouldExecute() {
        // В PvP с активной целью не отвлекаемся на предметы
        if (bot.getBotMode() == BotMode.PVP && bot.getAttackTarget() != null) {
            return false;
        }
        targetItem = findNearestItem();
        return targetItem != null;
    }

    @Override
    public boolean continueExecuting() {
        if (bot.getBotMode() == BotMode.PVP && bot.getAttackTarget() != null) {
            return false;
        }
        return targetItem != null && !targetItem.isDead;
    }

    @Override
    public void startExecuting() {
        // Навигация начнётся в updateTask
    }

    @Override
    public void updateTask() {
        if (targetItem == null || targetItem.isDead) {
            targetItem = findNearestItem();
            return;
        }

        bot.getLookHelper().setLookPositionWithEntity(targetItem, 10.0F, 10.0F);
        double distSq = bot.getDistanceSqToEntity(targetItem);

        if (distSq <= PICKUP_RANGE_SQ) {
            // Подбираем предмет
            ItemStack stack = targetItem.getEntityItem();
            if (stack != null && stack.stackSize > 0) {
                bot.addToInventory(stack.copy());
                targetItem.setDead();
            }
            targetItem = null;
        } else {
            bot.getNavigator().tryMoveToEntityLiving(targetItem, 0.9);
        }
    }

    @Override
    public void resetTask() {
        targetItem = null;
        bot.getNavigator().clearPathEntity();
    }

    // ─── Вспомогательные методы ─────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private EntityItem findNearestItem() {
        AxisAlignedBB searchBox = AxisAlignedBB.getBoundingBox(
            bot.posX - COLLECT_RANGE, bot.posY - 3, bot.posZ - COLLECT_RANGE,
            bot.posX + COLLECT_RANGE, bot.posY + 3, bot.posZ + COLLECT_RANGE
        );

        List<EntityItem> items = (List<EntityItem>)
            bot.worldObj.getEntitiesWithinAABB(EntityItem.class, searchBox);

        EntityItem nearest     = null;
        double     nearestDist = Double.MAX_VALUE;

        for (EntityItem item : items) {
            if (item == null || item.isDead) continue;
            double dist = bot.getDistanceSqToEntity(item);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest     = item;
            }
        }
        return nearest;
    }
}
