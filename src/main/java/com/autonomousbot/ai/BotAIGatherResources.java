package com.autonomousbot.ai;

import com.autonomousbot.ConfigHandler;
import com.autonomousbot.entity.EntityAutonomousBot;
import net.minecraft.block.Block;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * AI-задача: автономный сбор ресурсов.
 *
 * Приоритет добычи (от высшего к низшему):
 *   алмазная руда → золотая → железная → угольная → дерево → камень/песок
 *
 * Алгоритм:
 *  1. Обход сетки блоков в радиусе gatheringRange (конфиг).
 *  2. Выбор цели с наибольшим приоритетом (ближайшей при равном приоритете).
 *  3. Движение к цели.
 *  4. Когда бот рядом — "ломает" блок за breakTime тиков.
 *  5. Блок удаляется, дроп появляется в мире (будет подобран BotAICollectItems).
 */
public class BotAIGatherResources extends EntityAIBase {

    // Тиков на разрушение блока (при 20 TPS: 40 тиков = 2 сек)
    private static final int BREAK_TICKS_WOOD  = 40;
    private static final int BREAK_TICKS_STONE = 60;
    private static final int BREAK_TICKS_ORE   = 80;

    // Задержка перед повторным поиском при отсутствии ресурсов
    private static final int SEARCH_COOLDOWN = 80;

    // Радиус «рядом» (в квадратных блоках)
    private static final double NEAR_RANGE_SQ = 9.0;

    private final EntityAutonomousBot bot;

    private int targetX, targetY, targetZ;
    private boolean hasTarget    = false;
    private int     breakTimer   = 0;
    private int     searchCooldown = 0;

    public BotAIGatherResources(EntityAutonomousBot bot) {
        this.bot = bot;
        this.setMutexBits(3);
    }

    // ─── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    public boolean shouldExecute() {
        return bot.getBotMode() == BotMode.RESOURCE_GATHERING;
    }

    @Override
    public boolean continueExecuting() {
        return bot.getBotMode() == BotMode.RESOURCE_GATHERING;
    }

    @Override
    public void startExecuting() {
        findResource();
    }

    @Override
    public void updateTask() {
        // Кулдаун поиска
        if (searchCooldown > 0) {
            searchCooldown--;
            return;
        }

        if (!hasTarget) {
            findResource();
            searchCooldown = SEARCH_COOLDOWN;
            return;
        }

        World world = bot.worldObj;
        Block currentBlock = world.getBlock(targetX, targetY, targetZ);

        // Блок уже сломан (кем-то ещё или самим ботом)
        if (!isTargetResource(currentBlock)) {
            hasTarget  = false;
            breakTimer = 0;
            return;
        }

        double distSq = bot.getDistanceSq(
            targetX + 0.5, targetY + 0.5, targetZ + 0.5
        );

        if (distSq > NEAR_RANGE_SQ) {
            // Двигаемся к блоку
            bot.getNavigator().tryMoveToXYZ(targetX + 0.5, targetY, targetZ + 0.5, 0.7);
            breakTimer = 0;
        } else {
            // Стоим рядом — ломаем
            bot.getNavigator().clearPathEntity();
            bot.getLookHelper().setLookPosition(
                targetX + 0.5, targetY + 0.5, targetZ + 0.5, 10, 10
            );

            breakTimer++;
            if (breakTimer >= getBreakTime(currentBlock)) {
                breakBlock(world, targetX, targetY, targetZ);
                hasTarget  = false;
                breakTimer = 0;
            }
        }
    }

    @Override
    public void resetTask() {
        hasTarget     = false;
        breakTimer    = 0;
        searchCooldown = 0;
        bot.getNavigator().clearPathEntity();
    }

    // ─── Вспомогательные методы ─────────────────────────────────────────────────

    private void findResource() {
        World world = bot.worldObj;
        int bx = MathHelper.floor_double(bot.posX);
        int by = MathHelper.floor_double(bot.posY);
        int bz = MathHelper.floor_double(bot.posZ);
        int range = ConfigHandler.gatheringRange;

        int    bestX = -1, bestY = -1, bestZ = -1;
        double bestDist    = Double.MAX_VALUE;
        int    bestPriority = Integer.MAX_VALUE;

        // Шаг 2 для снижения нагрузки на CPU (пропускаем каждый второй блок)
        for (int dx = -range; dx <= range; dx += 2) {
            for (int dy = -5; dy <= 15; dy++) {
                for (int dz = -range; dz <= range; dz += 2) {
                    int x = bx + dx, y = by + dy, z = bz + dz;
                    Block block = world.getBlock(x, y, z);
                    if (!isTargetResource(block)) continue;

                    int    priority = getResourcePriority(block);
                    double dist     = bot.getDistanceSq(x + 0.5, y + 0.5, z + 0.5);

                    if (priority < bestPriority ||
                       (priority == bestPriority && dist < bestDist)) {
                        bestPriority = priority;
                        bestDist     = dist;
                        bestX = x; bestY = y; bestZ = z;
                    }
                }
            }
        }

        if (bestX != -1) {
            targetX = bestX; targetY = bestY; targetZ = bestZ;
            hasTarget = true;
        } else {
            hasTarget = false;
        }
    }

    private boolean isTargetResource(Block block) {
        return block == Blocks.log        || block == Blocks.log2       ||
               block == Blocks.diamond_ore || block == Blocks.gold_ore   ||
               block == Blocks.iron_ore   || block == Blocks.coal_ore   ||
               block == Blocks.stone      || block == Blocks.sand        ||
               block == Blocks.gravel;
    }

    /** Меньший приоритет = добываем в первую очередь */
    private int getResourcePriority(Block block) {
        if (block == Blocks.diamond_ore) return 1;
        if (block == Blocks.gold_ore)    return 2;
        if (block == Blocks.iron_ore)    return 3;
        if (block == Blocks.coal_ore)    return 4;
        if (block == Blocks.log || block == Blocks.log2) return 5;
        return 10; // stone, sand, gravel
    }

    private int getBreakTime(Block block) {
        if (block == Blocks.log || block == Blocks.log2)      return BREAK_TICKS_WOOD;
        if (block == Blocks.diamond_ore || block == Blocks.gold_ore ||
            block == Blocks.iron_ore    || block == Blocks.coal_ore) return BREAK_TICKS_ORE;
        return BREAK_TICKS_STONE;
    }

    private void breakBlock(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block == Blocks.air) return;

        int meta = world.getBlockMetadata(x, y, z);

        // Звук разрушения блока
        world.playAuxSFXAtEntity(
            null, 2001, x, y, z,
            Block.getIdFromBlock(block) + (meta << 12)
        );

        // Выпадение предметов (подберёт BotAICollectItems)
        block.dropBlockAsItem(world, x, y, z, meta, 0);

        // Удалить блок
        world.setBlockToAir(x, y, z);
    }
}
