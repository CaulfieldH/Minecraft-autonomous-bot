package com.autonomousbot.entity;

import com.autonomousbot.ai.*;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * Основная сущность автономного бота.
 *
 * Наследует EntityMob для получения:
 *  - AI-системы с приоритетами (tasks / targetTasks)
 *  - Системы атаки (attackEntityAsMob)
 *  - Системы здоровья и урона
 *  - Патфайндинга (getNavigator / getLookHelper)
 *
 * Инвентарь: 27 слотов ItemStack[].
 * Режим: BotMode (RESOURCE_GATHERING / BUILDING / PVP).
 */
public class EntityAutonomousBot extends EntityMob {

    // ─── Инвентарь ──────────────────────────────────────────────────────────────
    private static final int INVENTORY_SIZE = 27;
    private final ItemStack[] botInventory  = new ItemStack[INVENTORY_SIZE];

    // ─── Режим ──────────────────────────────────────────────────────────────────
    private BotMode mode = BotMode.RESOURCE_GATHERING;

    // ─── Конструктор ────────────────────────────────────────────────────────────

    public EntityAutonomousBot(World world) {
        super(world);
        this.setSize(0.6F, 1.8F);

        // ── Движение / выживание ──────────────────────────────────────────────
        this.tasks.addTask(0, new EntityAISwimming(this));

        // ── PvP (приоритет 1: самое важное) ──────────────────────────────────
        this.tasks.addTask(1, new BotAIPvPAttack(this));

        // ── Сбор ресурсов ─────────────────────────────────────────────────────
        this.tasks.addTask(2, new BotAIGatherResources(this));

        // ── Строительство убежища ─────────────────────────────────────────────
        this.tasks.addTask(3, new BotAIBuildShelter(this));

        // ── Подбор предметов (всегда) ─────────────────────────────────────────
        this.tasks.addTask(4, new BotAICollectItems(this));

        // ── Бездействие: смотрим на игрока / крутимся ────────────────────────
        this.tasks.addTask(5, new EntityAIWander(this, 0.5));
        this.tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(7, new EntityAILookIdle(this));

        // ── Таргетинг в PvP ───────────────────────────────────────────────────
        this.targetTasks.addTask(1, new BotAIPvPTarget(this));
    }

    // ─── Атрибуты ───────────────────────────────────────────────────────────────

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();

        // Значения берутся из ConfigHandler при спавне через setBotStats()
        this.getEntityAttribute(SharedMonsterAttributes.maxHealth)
            .setBaseValue(20.0D);
        this.getEntityAttribute(SharedMonsterAttributes.movementSpeed)
            .setBaseValue(0.3D);
        this.getEntityAttribute(SharedMonsterAttributes.attackDamage)
            .setBaseValue(3.0D);
        this.getEntityAttribute(SharedMonsterAttributes.followRange)
            .setBaseValue(40.0D);
        this.getEntityAttribute(SharedMonsterAttributes.knockbackResistance)
            .setBaseValue(0.0D);
    }

    /**
     * Применить параметры из ConfigHandler (вызывается после спавна).
     */
    public void applyConfigStats(double health, double speed, double damage) {
        this.getEntityAttribute(SharedMonsterAttributes.maxHealth)
            .setBaseValue(health);
        this.setHealth((float) health);
        this.getEntityAttribute(SharedMonsterAttributes.movementSpeed)
            .setBaseValue(speed);
        this.getEntityAttribute(SharedMonsterAttributes.attackDamage)
            .setBaseValue(damage);
    }

    // ─── Звуки ──────────────────────────────────────────────────────────────────

    @Override
    protected String getLivingSound()   { return null; }
    @Override
    protected String getHurtSound()     { return "game.player.hurt"; }
    @Override
    protected String getDeathSound()    { return "game.player.die";  }
    @Override
    protected float  getSoundVolume()   { return 1.0F; }

    // ─── NBT (сохранение / загрузка) ────────────────────────────────────────────

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setString("BotMode", mode.getConfigName());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        if (nbt.hasKey("BotMode")) {
            this.mode = BotMode.fromString(nbt.getString("BotMode"));
        }
    }

    // ─── Режим ──────────────────────────────────────────────────────────────────

    public BotMode getBotMode() { return mode; }

    public void setBotMode(BotMode newMode) {
        this.mode = newMode;
        this.setAttackTarget(null);
        this.getNavigator().clearPathEntity();
    }

    // ─── Инвентарь ──────────────────────────────────────────────────────────────

    /**
     * Добавить предмет в инвентарь бота.
     * Сначала пытается дополнить существующий стак, потом ищет пустой слот.
     * @return true, если предмет был добавлен полностью или частично.
     */
    public boolean addToInventory(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) return false;

        // Попытка дополнить стак
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (botInventory[i] != null
                    && botInventory[i].isItemEqual(stack)
                    && botInventory[i].stackSize < botInventory[i].getMaxStackSize()) {
                int space = botInventory[i].getMaxStackSize() - botInventory[i].stackSize;
                int toAdd = Math.min(space, stack.stackSize);
                botInventory[i].stackSize += toAdd;
                stack.stackSize -= toAdd;
                if (stack.stackSize <= 0) return true;
            }
        }

        // Поиск пустого слота
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (botInventory[i] == null) {
                botInventory[i] = stack.copy();
                return true;
            }
        }

        return false; // Инвентарь заполнен
    }

    /** Количество предметов данного типа в инвентаре */
    public int countItem(Item item) {
        int count = 0;
        for (ItemStack s : botInventory) {
            if (s != null && s.getItem() == item) count += s.stackSize;
        }
        return count;
    }

    /** Есть ли в инвентаре >= minCount единиц предмета */
    public boolean hasItems(Item item, int minCount) {
        return countItem(item) >= minCount;
    }

    public ItemStack[] getBotInventory() { return botInventory; }

    // ─── Прочее ─────────────────────────────────────────────────────────────────

    @Override
    protected boolean isAIEnabled() { return true; }
}
