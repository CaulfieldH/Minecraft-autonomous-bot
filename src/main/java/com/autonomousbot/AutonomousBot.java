package com.autonomousbot;

import com.autonomousbot.entity.EntityAutonomousBot;
import com.autonomousbot.proxy.CommonProxy;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.EntityRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Точка входа мода AutonomousBot для Minecraft 1.7.10 + Forge.
 *
 * Мод добавляет автономного NPC-бота с тремя режимами работы:
 *   - resource_gathering : добыча ресурсов (дерево, руда, камень)
 *   - building           : строительство убежища
 *   - pvp                : поиск и атака игроков
 *
 * Управление через команду /bot <spawn|kill|mode|info|list|buildreset>
 * Настройки через config/autonomousbot.cfg
 */
@Mod(
    modid   = AutonomousBot.MODID,
    name    = AutonomousBot.NAME,
    version = AutonomousBot.VERSION
)
public class AutonomousBot {

    public static final String MODID   = "autonomousbot";
    public static final String NAME    = "Autonomous Bot";
    public static final String VERSION = "1.0.0";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @SidedProxy(
        clientSide = "com.autonomousbot.proxy.ClientProxy",
        serverSide = "com.autonomousbot.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

    @Mod.Instance(MODID)
    public static AutonomousBot instance;

    // ─── FML Events ─────────────────────────────────────────────────────────────

    /**
     * Pre-init: загрузка конфига, регистрация сущности.
     */
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ConfigHandler.init(event.getSuggestedConfigurationFile());

        EntityRegistry.registerModEntity(
            EntityAutonomousBot.class,  // класс сущности
            "AutonomousBot",            // имя (уникальное в рамках мода)
            1,                          // mod-local ID
            this,                       // экземпляр мода
            64,                         // дальность трекинга (блоки)
            3,                          // частота обновлений (тиков)
            true                        // отправлять пакеты скорости
        );

        LOGGER.info("[AutonomousBot] Pre-init complete. Default mode: {}", ConfigHandler.defaultMode.getConfigName());
    }

    /**
     * Init: регистрация рендереров (клиент) / ничего (сервер).
     */
    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.registerRenderers();
        LOGGER.info("[AutonomousBot] Init complete.");
    }

    /**
     * Server starting: регистрация команды /bot.
     */
    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandBotControl());
        LOGGER.info("[AutonomousBot] Command '/bot' registered.");
    }
}
