package com.autonomousbot.ai;

import com.autonomousbot.ConfigHandler;
import com.autonomousbot.entity.EntityAutonomousBot;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 * AI-задача: автономный сбор ресурсов (Minecraft 1.21.1).
 *
 * Приоритет добычи: алмаз → золото → железо → уголь → дерево → камень/песок/гравий
 * Использует теги блоков (BlockTags) — автоматически охватывает обычную и
 * глубинную (deepslate) разновидности руды.
 *
 * Алгоритм:
 *  1. Обход сетки блоков в радиусе gatheringRange (шаг 2 для экономии CPU).
 *  2. Выбор цели с наибольшим приоритетом (при равном — ближайшей).
 *  3. Движение к цели.
 *  4. «Добыча» за breakTime тиков (анимационная задержка).
 *  5. Блок удаляется, дроп появляется в мире (подберёт BotAICollectItems).
 */
public class BotAIGatherResources extends Goal {

    private static final int    BREAK_TICKS_WOOD  = 40;
    private static final int    BREAK_TICKS_ORE   = 80;
    private static final int    BREAK_TICKS_STONE = 60;
    private static final int    SEARCH_COOLDOWN   = 80;
    private static final double NEAR_RANGE_SQ     = 9.0;

    private final EntityAutonomousBot bot;

    private BlockPos target        = null;
    private int      breakTimer    = 0;
    private int      searchCooldown = 0;

    public BotAIGatherResources(EntityAutonomousBot bot) {
        this.bot = bot;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    @Override public boolean canUse()           { return bot.getBotMode() == BotMode.RESOURCE_GATHERING; }
    @Override public boolean canContinueToUse() { return bot.getBotMode() == BotMode.RESOURCE_GATHERING; }

    @Override
    public void start() { findResource(); }

    @Override
    public void tick() {
        if (searchCooldown > 0) { searchCooldown--; return; }

        if (target == null) {
            findResource();
            searchCooldown = SEARCH_COOLDOWN;
            return;
        }

        Level level = bot.level();
        BlockState state = level.getBlockState(target);

        if (!isTargetResource(state)) {
            target = null; breakTimer = 0;
            return;
        }

        double distSq = bot.distanceToSqr(
            target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5
        );

        if (distSq > NEAR_RANGE_SQ) {
            bot.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.7);
            breakTimer = 0;
        } else {
            bot.getNavigation().stop();
            bot.getLookControl().setLookAt(
                target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 10, 10
            );
            breakTimer++;
            if (breakTimer >= getBreakTime(state)) {
                breakBlock(level, target, state);
                target = null; breakTimer = 0;
            }
        }
    }

    @Override
    public void stop() {
        target = null; breakTimer = 0; searchCooldown = 0;
        bot.getNavigation().stop();
    }

    // ─── Поиск ресурса ───────────────────────────────────────────────────────────

    private void findResource() {
        Level level  = bot.level();
        int bx       = Mth.floor(bot.getX());
        int by       = Mth.floor(bot.getY());
        int bz       = Mth.floor(bot.getZ());
        int range    = ConfigHandler.getGatheringRange();

        BlockPos bestPos  = null;
        double   bestDist = Double.MAX_VALUE;
        int      bestPrio = Integer.MAX_VALUE;

        for (int dx = -range; dx <= range; dx += 2) {
            for (int dy = -5; dy <= 15; dy++) {
                for (int dz = -range; dz <= range; dz += 2) {
                    BlockPos pos   = new BlockPos(bx + dx, by + dy, bz + dz);
                    BlockState state = level.getBlockState(pos);
                    if (!isTargetResource(state)) continue;

                    int    prio = getResourcePriority(state);
                    double dist = bot.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

                    if (prio < bestPrio || (prio == bestPrio && dist < bestDist)) {
                        bestPrio = prio; bestDist = dist; bestPos = pos;
                    }
                }
            }
        }
        target = bestPos;
    }

    private boolean isTargetResource(BlockState state) {
        return state.is(BlockTags.LOGS)
            || state.is(BlockTags.COAL_ORES)
            || state.is(BlockTags.IRON_ORES)
            || state.is(BlockTags.GOLD_ORES)
            || state.is(BlockTags.DIAMOND_ORES)
            || state.is(Blocks.STONE)
            || state.is(Blocks.SAND)
            || state.is(Blocks.GRAVEL);
    }

    /** Меньший приоритет = добываем первым */
    private int getResourcePriority(BlockState state) {
        if (state.is(BlockTags.DIAMOND_ORES)) return 1;
        if (state.is(BlockTags.GOLD_ORES))    return 2;
        if (state.is(BlockTags.IRON_ORES))    return 3;
        if (state.is(BlockTags.COAL_ORES))    return 4;
        if (state.is(BlockTags.LOGS))         return 5;
        return 10;
    }

    private int getBreakTime(BlockState state) {
        if (state.is(BlockTags.LOGS))
            return BREAK_TICKS_WOOD;
        if (state.is(BlockTags.COAL_ORES) || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.GOLD_ORES) || state.is(BlockTags.DIAMOND_ORES))
            return BREAK_TICKS_ORE;
        return BREAK_TICKS_STONE;
    }

    private void breakBlock(Level level, BlockPos pos, BlockState state) {
        if (state.isAir()) return;
        // Частицы и звук разрушения блока
        level.levelEvent(2001, pos, Block.getId(state));
        // Выпадение предметов (подберёт BotAICollectItems)
        Block.dropResources(state, level, pos);
        // Удаление блока
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }
}
