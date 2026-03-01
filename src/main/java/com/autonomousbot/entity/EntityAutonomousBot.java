package com.autonomousbot.entity;

import com.autonomousbot.ConfigHandler;
import com.autonomousbot.ai.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.Objects;

/**
 * Основная сущность автономного бота (Minecraft 1.21.1 / NeoForge).
 *
 * Наследует Monster для получения:
 *  - Системы AI-целей (goalSelector / targetSelector)
 *  - Навигации и pathfinding
 *  - Системы атаки (doHurtTarget)
 *  - Атрибутов здоровья, скорости, урона
 *
 * Инвентарь: 27 слотов ItemStack[].
 * Режим: BotMode (RESOURCE_GATHERING / BUILDING / PVP).
 */
public class EntityAutonomousBot extends Monster {

    // ─── Инвентарь ──────────────────────────────────────────────────────────────
    private static final int INVENTORY_SIZE = 27;
    private final ItemStack[] botInventory  = new ItemStack[INVENTORY_SIZE];

    // ─── Режим ──────────────────────────────────────────────────────────────────
    private BotMode mode = BotMode.RESOURCE_GATHERING;

    // ─── Конструктор ────────────────────────────────────────────────────────────

    public EntityAutonomousBot(EntityType<? extends EntityAutonomousBot> type, Level level) {
        super(type, level);
        Arrays.fill(botInventory, ItemStack.EMPTY);
    }

    // ─── Атрибуты (вызывается при регистрации через EntityAttributeCreationEvent) ─

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH,          20.0)
            .add(Attributes.MOVEMENT_SPEED,       0.3)
            .add(Attributes.ATTACK_DAMAGE,         3.0)
            .add(Attributes.FOLLOW_RANGE,         40.0)
            .add(Attributes.KNOCKBACK_RESISTANCE,  0.0);
    }

    // ─── AI-задачи ───────────────────────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        // Движение/выживание
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // PvP (высший приоритет среди активных режимов)
        this.goalSelector.addGoal(1, new BotAIPvPAttack(this));

        // Добыча ресурсов
        this.goalSelector.addGoal(2, new BotAIGatherResources(this));

        // Строительство укрытия
        this.goalSelector.addGoal(3, new BotAIBuildShelter(this));

        // Подбор предметов (параллельно с основным занятием)
        this.goalSelector.addGoal(4, new BotAICollectItems(this));

        // Бездействие: блуждание, слежение за игроком, случайный взгляд
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.5));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // Таргетинг в PvP
        this.targetSelector.addGoal(1, new BotAIPvPTarget(this));
    }

    // ─── Применение параметров из конфига ────────────────────────────────────────

    public void applyConfigStats(double health, double speed, double damage) {
        Objects.requireNonNull(this.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(health);
        this.setHealth((float) health);
        Objects.requireNonNull(this.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(speed);
        Objects.requireNonNull(this.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(damage);
    }

    // ─── Звуки ──────────────────────────────────────────────────────────────────

    @Override protected SoundEvent getAmbientSound()                    { return null; }
    @Override protected SoundEvent getHurtSound(DamageSource source)   { return SoundEvents.PLAYER_HURT; }
    @Override protected SoundEvent getDeathSound()                      { return SoundEvents.PLAYER_DEATH; }
    @Override protected float     getSoundVolume()                      { return 1.0F; }

    // ─── NBT (сохранение / загрузка) ─────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putString("BotMode", mode.getConfigName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("BotMode")) {
            this.mode = BotMode.fromString(nbt.getString("BotMode"));
        }
    }

    // ─── Режим ───────────────────────────────────────────────────────────────────

    public BotMode getBotMode() { return mode; }

    public void setBotMode(BotMode newMode) {
        this.mode = newMode;
        this.setTarget(null);
        this.getNavigation().stop();
    }

    // ─── Инвентарь ───────────────────────────────────────────────────────────────

    public boolean addToInventory(ItemStack stack) {
        if (stack.isEmpty()) return false;

        // Дополнить существующий стак
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (!botInventory[i].isEmpty()
                    && botInventory[i].getItem() == stack.getItem()
                    && botInventory[i].getCount() < botInventory[i].getMaxStackSize()) {
                int space = botInventory[i].getMaxStackSize() - botInventory[i].getCount();
                int toAdd = Math.min(space, stack.getCount());
                botInventory[i].grow(toAdd);
                stack.shrink(toAdd);
                if (stack.isEmpty()) return true;
            }
        }

        // Найти пустой слот
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (botInventory[i].isEmpty()) {
                botInventory[i] = stack.copy();
                return true;
            }
        }

        return false; // Инвентарь заполнен
    }

    public int countItem(Item item) {
        int count = 0;
        for (ItemStack s : botInventory) {
            if (!s.isEmpty() && s.getItem() == item) count += s.getCount();
        }
        return count;
    }

    public boolean hasItems(Item item, int minCount) {
        return countItem(item) >= minCount;
    }

    public ItemStack[] getBotInventory() { return botInventory; }

    /**
     * Сбросить флаг «укрытие построено» у BotAIBuildShelter.
     * Вызывается командой /bot buildreset.
     * @return true — флаг сброшен, false — AI-задача не найдена.
     */
    public boolean resetBuildShelterFlag() {
        for (net.minecraft.world.entity.ai.goal.WrappedGoal entry
                : this.goalSelector.getAvailableGoals()) {
            if (entry.getGoal() instanceof BotAIBuildShelter buildAI) {
                buildAI.resetShelterFlag();
                return true;
            }
        }
        return false;
    }
}
