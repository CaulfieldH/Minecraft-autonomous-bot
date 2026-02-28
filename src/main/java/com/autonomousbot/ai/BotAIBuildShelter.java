package com.autonomousbot.ai;

import com.autonomousbot.entity.EntityAutonomousBot;
import net.minecraft.block.Block;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * AI-задача: постройка простого укрытия.
 *
 * Схема постройки — 5×5×4 коробка:
 *  - y=0..2 : стены по периметру (зазор в двери: dz=0, dx=2, y=0 и y=1)
 *  - y=3    : полная крыша 5×5
 *
 * Материал: сначала берём Dirt, затем Cobblestone (если нарыли камня).
 * После постройки задача отключается (shelterBuilt = true).
 * Сброс флага: /bot buildreset <id>  или смена режима.
 */
public class BotAIBuildShelter extends EntityAIBase {

    private static final int PLACE_DELAY_TICKS = 5;   // задержка между укладкой блоков
    private static final double REACH_SQ       = 16.0; // 4 блока

    private final EntityAutonomousBot bot;

    private boolean shelterBuilt   = false;
    private int     originX, originY, originZ;
    private final List<int[]> buildQueue = new ArrayList<int[]>();
    private int  queueIndex   = 0;
    private int  placeCooldown = 0;

    public BotAIBuildShelter(EntityAutonomousBot bot) {
        this.bot = bot;
        this.setMutexBits(3);
    }

    // ─── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    public boolean shouldExecute() {
        return bot.getBotMode() == BotMode.BUILDING && !shelterBuilt;
    }

    @Override
    public boolean continueExecuting() {
        return bot.getBotMode() == BotMode.BUILDING
            && !shelterBuilt
            && queueIndex < buildQueue.size();
    }

    @Override
    public void startExecuting() {
        findOriginAndPlan();
    }

    @Override
    public void updateTask() {
        if (placeCooldown > 0) {
            placeCooldown--;
            return;
        }

        if (queueIndex >= buildQueue.size()) {
            shelterBuilt = true;
            return;
        }

        int[] pos    = buildQueue.get(queueIndex);
        int worldX   = originX + pos[0];
        int worldY   = originY + pos[1];
        int worldZ   = originZ + pos[2];

        double distSq = bot.getDistanceSq(worldX + 0.5, worldY + 0.5, worldZ + 0.5);

        if (distSq > REACH_SQ) {
            // Двигаемся ближе к месту укладки
            bot.getNavigator().tryMoveToXYZ(worldX + 0.5, worldY, worldZ + 0.5, 0.7);
        } else {
            // Укладываем блок
            bot.getNavigator().clearPathEntity();
            World world = bot.worldObj;

            if (world.getBlock(worldX, worldY, worldZ) == Blocks.air) {
                world.setBlock(worldX, worldY, worldZ, chooseMaterial());
                placeCooldown = PLACE_DELAY_TICKS;
            }
            queueIndex++;
        }
    }

    @Override
    public void resetTask() {
        queueIndex    = 0;
        placeCooldown = 0;
        bot.getNavigator().clearPathEntity();
    }

    /** Сброс флага «укрытие построено» (вызывается командой /bot buildreset) */
    public void resetShelterFlag() {
        shelterBuilt = false;
        queueIndex   = 0;
        buildQueue.clear();
    }

    // ─── Вспомогательные методы ─────────────────────────────────────────────────

    private void findOriginAndPlan() {
        // Ставим укрытие на 10 блоков к востоку от текущей позиции
        originX = MathHelper.floor_double(bot.posX) + 10;
        originZ = MathHelper.floor_double(bot.posZ);
        originY = MathHelper.floor_double(bot.posY);

        // Ищем уровень земли
        World world = bot.worldObj;
        for (int dy = 5; dy > -10; dy--) {
            int y = originY + dy;
            if (world.getBlock(originX, y, originZ) != Blocks.air &&
                world.getBlock(originX, y + 1, originZ) == Blocks.air) {
                originY = y + 1;
                break;
            }
        }

        generateBuildPlan();
    }

    /**
     * Генерирует очередь позиций для 5×5×4 укрытия.
     *
     * Слои:
     *   y=0,1 : стены-периметр (кроме дверного проёма dz=0, dx=2)
     *   y=2   : стены-периметр (полностью)
     *   y=3   : полная крыша
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

    private Block chooseMaterial() {
        // Если набрали булыжник — строим из него; иначе из земли
        if (bot.countItem(net.minecraft.init.Items.coal) > 0) {
            // placeholder: используем dirt как базовый материал
        }
        return Blocks.dirt;
    }
}
