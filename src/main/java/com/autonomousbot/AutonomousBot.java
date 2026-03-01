package com.autonomousbot;

import com.autonomousbot.entity.EntityAutonomousBot;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Точка входа мода AutonomousBot для Minecraft 1.21.1 + NeoForge.
 *
 * Мод добавляет автономного NPC-бота с тремя режимами работы:
 *   - resource_gathering : добыча ресурсов (руда, дерево, камень)
 *   - building           : строительство укрытия 5×5×4
 *   - pvp                : поиск и атака игроков
 *
 * Управление: /bot <spawn|kill|mode|info|list|buildreset>
 * Конфиг: config/autonomousbot-common.toml
 */
@Mod(AutonomousBot.MODID)
public class AutonomousBot {

    public static final String MODID  = "autonomousbot";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    // ─── Регистрация сущности ────────────────────────────────────────────────────

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<EntityAutonomousBot>> BOT_TYPE =
        ENTITY_TYPES.register("autonomous_bot",
            () -> EntityType.Builder.<EntityAutonomousBot>of(EntityAutonomousBot::new, MobCategory.MONSTER)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(8)
                .updateInterval(3)
                .build("autonomousbot:autonomous_bot")
        );

    // ─── Конструктор (вызывается NeoForge при загрузке) ─────────────────────────

    public AutonomousBot(IEventBus modEventBus, ModContainer modContainer) {
        // Регистрируем EntityType
        ENTITY_TYPES.register(modEventBus);

        // Атрибуты сущности (здоровье, скорость, урон)
        modEventBus.addListener(this::onEntityAttributeCreation);

        // Команда /bot (серверная шина событий)
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);

        // Конфиг (config/autonomousbot-common.toml)
        modContainer.registerConfig(ModConfig.Type.COMMON, ConfigHandler.SPEC);

        LOGGER.info("[AutonomousBot] Initialized for Minecraft 1.21.1 (NeoForge {})", MODID);
    }

    private void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(BOT_TYPE.get(), EntityAutonomousBot.createAttributes().build());
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        CommandBotControl.register(event.getDispatcher());
    }
}
