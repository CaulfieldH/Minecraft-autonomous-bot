package com.autonomousbot.ai;

import com.autonomousbot.entity.EntityAutonomousBot;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * AI-задача: постройка простого укрытия 5×5×4 (Minecraft 1.21.1).
 *
 * Схема:
 *  y=0..1 : стены-периметр (дверной проём: dz=0, dx=2, y=0 и y=1)
 *  y=2    : стены-периметр (полностью)
 *  y=3    : полная крыша 5×5
 *
 * Материал: Dirt (можно расширить на Cobblestone при наличии в инвентаре).
 * После постройки задача отключается (shelterBuilt = true).
 * Сброс флага: /bot buildreset <id>
 */
public class BotAIBuildShelter extends Goal {

    private static final int    PLACE_DELAY_TICKS = 5;
    private static final double REACH_SQ          = 16.0; // 4 блока

    private final EntityAutonomousBot bot;

    private boolean       shelterBuilt = false;
    private int           originX, originY, originZ;
    private final List<int[]> buildQueue = new ArrayList<>();
    private int           queueIndex   = 0;
    private int           placeCooldown = 0;

    public BotAIBuildShelter(EntityAutonomousBot bot) {
        this.bot = bot;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    @Override
    public boolean canUse() {
        return bot.getBotMode() == BotMode.BUILDING && !shelterBuilt;
    }

    @Override
    public boolean canContinueToUse() {
        return bot.getBotMode() == BotMode.BUILDING
            && !shelterBuilt
            && queueIndex < buildQueue.size();
    }

    @Override
    public void start() { findOriginAndPlan(); }

    @Override
    public void tick() {
        if (placeCooldown > 0) { placeCooldown--; return; }

        if (queueIndex >= buildQueue.size()) {
            shelterBuilt = true;
            return;
        }

        int[] rel   = buildQueue.get(queueIndex);
        int wx      = originX + rel[0];
        int wy      = originY + rel[1];
        int wz      = originZ + rel[2];
        BlockPos bp = new BlockPos(wx, wy, wz);

        double distSq = bot.distanceToSqr(wx + 0.5, wy + 0.5, wz + 0.5);

        if (distSq > REACH_SQ) {
            bot.getNavigation().moveTo(wx + 0.5, wy, wz + 0.5, 0.7);
        } else {
            bot.getNavigation().stop();
            Level level = bot.level();
            if (level.getBlockState(bp).isAir()) {
                level.setBlock(bp, Blocks.DIRT.defaultBlockState(), 3);
                placeCooldown = PLACE_DELAY_TICKS;
            }
            queueIndex++;
        }
    }

    @Override
    public void stop() {
        queueIndex    = 0;
        placeCooldown = 0;
        bot.getNavigation().stop();
    }

    /** Сброс флага «укрытие построено» (вызывается через EntityAutonomousBot). */
    public void resetShelterFlag() {
        shelterBuilt = false;
        queueIndex   = 0;
        buildQueue.clear();
    }

    // ─── Планирование постройки ──────────────────────────────────────────────────

    private void findOriginAndPlan() {
        originX = Mth.floor(bot.getX()) + 10;
        originZ = Mth.floor(bot.getZ());
        originY = Mth.floor(bot.getY());

        Level level = bot.level();
        for (int dy = 5; dy > -10; dy--) {
            int y = originY + dy;
            BlockPos base  = new BlockPos(originX, y,     originZ);
            BlockPos above = new BlockPos(originX, y + 1, originZ);
            if (!level.getBlockState(base).isAir() && level.getBlockState(above).isAir()) {
                originY = y + 1;
                break;
            }
        }
        generateBuildPlan();
    }

    /**
     * Генерирует список позиций (dx, dy, dz) для укрытия 5×5×4.
     * Слои y=0,1 — периметр без дверного проёма; y=2 — периметр; y=3 — крыша.
     */
    private void generateBuildPlan() {
        buildQueue.clear();
        queueIndex = 0;

        for (int dy = 0; dy <= 3; dy++) {
            for (int dx = 0; dx <= 4; dx++) {
                for (int dz = 0; dz <= 4; dz++) {
                    boolean isPerimeter = (dx == 0 || dx == 4 || dz == 0 || dz == 4);
                    boolean isRoof      = (dy == 3);
                    boolean isDoorGap   = (dz == 0 && dx == 2 && (dy == 0 || dy == 1));

                    if ((isPerimeter && !isDoorGap) || isRoof) {
                        buildQueue.add(new int[]{dx, dy, dz});
                    }
                }
            }
        }
    }
}
