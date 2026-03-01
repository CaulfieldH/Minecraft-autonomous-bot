package com.autonomousbot;

import com.autonomousbot.ai.BotMode;
import com.autonomousbot.entity.EntityAutonomousBot;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Команда /bot — управление автономными ботами.
 *
 * Использование (требуется уровень оператора 2):
 *   /bot spawn [x y z]                              — спавнит бота
 *   /bot kill <id>                                   — удаляет бота
 *   /bot mode <id> <pvp|resource_gathering|building> — переключает режим
 *   /bot info <id>                                   — информация о боте
 *   /bot list                                        — список всех ботов
 *   /bot buildreset <id>                             — сбросить флаг постройки
 *
 * Реализован на Brigadier (стандарт с Minecraft 1.13+).
 */
public class CommandBotControl {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("bot")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> showHelp(ctx.getSource()))

                .then(Commands.literal("spawn")
                    .executes(ctx -> spawnBot(ctx.getSource(), null))
                    .then(Commands.argument("pos", Vec3Argument.vec3())
                        .executes(ctx -> spawnBot(ctx.getSource(),
                            Vec3Argument.getVec3(ctx, "pos")))))

                .then(Commands.literal("kill")
                    .then(Commands.argument("id", IntegerArgumentType.integer(0))
                        .executes(ctx -> killBot(ctx.getSource(),
                            IntegerArgumentType.getInteger(ctx, "id")))))

                .then(Commands.literal("mode")
                    .then(Commands.argument("id", IntegerArgumentType.integer(0))
                        .then(Commands.argument("mode", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                builder.suggest("pvp");
                                builder.suggest("resource_gathering");
                                builder.suggest("building");
                                return builder.buildFuture();
                            })
                            .executes(ctx -> setMode(ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "id"),
                                StringArgumentType.getString(ctx, "mode"))))))

                .then(Commands.literal("info")
                    .then(Commands.argument("id", IntegerArgumentType.integer(0))
                        .executes(ctx -> botInfo(ctx.getSource(),
                            IntegerArgumentType.getInteger(ctx, "id")))))

                .then(Commands.literal("list")
                    .executes(ctx -> listBots(ctx.getSource())))

                .then(Commands.literal("buildreset")
                    .then(Commands.argument("id", IntegerArgumentType.integer(0))
                        .executes(ctx -> buildReset(ctx.getSource(),
                            IntegerArgumentType.getInteger(ctx, "id")))))
        );
    }

    // ─── Подкоманды ─────────────────────────────────────────────────────────────

    private static int spawnBot(CommandSourceStack source, Vec3 pos) {
        ServerLevel level = source.getLevel();
        Vec3 spawnPos = (pos != null) ? pos : source.getPosition().add(0, 0, 2);

        EntityAutonomousBot bot = new EntityAutonomousBot(AutonomousBot.BOT_TYPE.get(), level);
        bot.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0, 0);
        bot.setBotMode(ConfigHandler.getDefaultMode());
        bot.applyConfigStats(
            ConfigHandler.getMaxHealth(),
            ConfigHandler.getMoveSpeed(),
            ConfigHandler.getAttackDamage()
        );

        if (level.addFreshEntity(bot)) {
            source.sendSuccess(() -> Component.literal(
                ChatFormatting.GREEN + "Bot spawned! " +
                ChatFormatting.AQUA + "ID: " + bot.getId() +
                ChatFormatting.WHITE + " | Mode: " +
                ChatFormatting.YELLOW + bot.getBotMode().getDisplayName() +
                ChatFormatting.WHITE + " | Pos: " + fmt(spawnPos.x) + ", " + fmt(spawnPos.y) + ", " + fmt(spawnPos.z)
            ), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to spawn bot at those coordinates!"));
            return 0;
        }
    }

    private static int killBot(CommandSourceStack source, int id) {
        EntityAutonomousBot bot = findBot(source, id);
        if (bot == null) return 0;
        bot.discard();
        source.sendSuccess(() -> Component.literal(
            ChatFormatting.GREEN + "Bot " + id + " removed."
        ), false);
        return 1;
    }

    private static int setMode(CommandSourceStack source, int id, String modeStr) {
        EntityAutonomousBot bot = findBot(source, id);
        if (bot == null) return 0;
        BotMode newMode = BotMode.fromString(modeStr);
        bot.setBotMode(newMode);
        source.sendSuccess(() -> Component.literal(
            ChatFormatting.GREEN + "Bot " + id + " → mode: " +
            ChatFormatting.YELLOW + newMode.getDisplayName()
        ), false);
        return 1;
    }

    private static int botInfo(CommandSourceStack source, int id) {
        EntityAutonomousBot bot = findBot(source, id);
        if (bot == null) return 0;

        int usedSlots = 0;
        for (net.minecraft.world.item.ItemStack s : bot.getBotInventory()) {
            if (!s.isEmpty()) usedSlots++;
        }
        int finalUsed = usedSlots;

        source.sendSuccess(() -> Component.literal(
            ChatFormatting.YELLOW + "=== Bot #" + id + " ===\n" +
            ChatFormatting.AQUA + "Mode:      " + ChatFormatting.WHITE + bot.getBotMode().getDisplayName() + "\n" +
            ChatFormatting.AQUA + "HP:        " + ChatFormatting.WHITE + fmt(bot.getHealth()) + " / " + fmt(bot.getMaxHealth()) + "\n" +
            ChatFormatting.AQUA + "Pos:       " + ChatFormatting.WHITE + fmt(bot.getX()) + ", " + fmt(bot.getY()) + ", " + fmt(bot.getZ()) + "\n" +
            ChatFormatting.AQUA + "Inventory: " + ChatFormatting.WHITE + finalUsed + " / 27 slots used"
        ), false);
        return 1;
    }

    private static int listBots(CommandSourceStack source) {
        List<EntityAutonomousBot> bots = getAllBots(source);
        if (bots.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                ChatFormatting.YELLOW + "No active bots."
            ), false);
            return 0;
        }
        StringBuilder sb = new StringBuilder(ChatFormatting.YELLOW + "Active Bots (" + bots.size() + "):\n");
        for (EntityAutonomousBot b : bots) {
            sb.append(ChatFormatting.AQUA).append("  #").append(b.getId())
              .append(ChatFormatting.WHITE).append(" | ").append(b.getBotMode().getDisplayName())
              .append(" | HP ").append(fmt(b.getHealth())).append("/").append(fmt(b.getMaxHealth()))
              .append(" | ").append(fmt(b.getX())).append(", ").append(fmt(b.getY())).append(", ").append(fmt(b.getZ()))
              .append("\n");
        }
        String msg = sb.toString();
        source.sendSuccess(() -> Component.literal(msg), false);
        return bots.size();
    }

    private static int buildReset(CommandSourceStack source, int id) {
        EntityAutonomousBot bot = findBot(source, id);
        if (bot == null) return 0;

        if (bot.resetBuildShelterFlag()) {
            source.sendSuccess(() -> Component.literal(
                ChatFormatting.GREEN + "Shelter flag reset for bot " + id + "."
            ), false);
            return 1;
        }
        source.sendFailure(Component.literal("Could not find build task on bot " + id));
        return 0;
    }

    private static int showHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(
            ChatFormatting.YELLOW + "=== Autonomous Bot Commands ===\n" +
            ChatFormatting.GREEN + "/bot spawn [x y z]" + ChatFormatting.WHITE + " — spawn a bot\n" +
            ChatFormatting.GREEN + "/bot kill <id>" + ChatFormatting.WHITE + " — remove a bot\n" +
            ChatFormatting.GREEN + "/bot mode <id> <pvp|resource_gathering|building>" + ChatFormatting.WHITE + " — set mode\n" +
            ChatFormatting.GREEN + "/bot info <id>" + ChatFormatting.WHITE + " — show bot details\n" +
            ChatFormatting.GREEN + "/bot list" + ChatFormatting.WHITE + " — list all bots\n" +
            ChatFormatting.GREEN + "/bot buildreset <id>" + ChatFormatting.WHITE + " — allow bot to build again"
        ), false);
        return 1;
    }

    // ─── Утилиты ────────────────────────────────────────────────────────────────

    private static List<EntityAutonomousBot> getAllBots(CommandSourceStack source) {
        List<EntityAutonomousBot> result = new ArrayList<>();
        for (ServerLevel level : source.getServer().getAllLevels()) {
            for (Entity e : level.getEntities().getAll()) {
                if (e instanceof EntityAutonomousBot bot) result.add(bot);
            }
        }
        return result;
    }

    private static EntityAutonomousBot findBot(CommandSourceStack source, int id) {
        for (EntityAutonomousBot b : getAllBots(source)) {
            if (b.getId() == id) return b;
        }
        source.sendFailure(Component.literal("Bot with ID " + id + " not found!"));
        return null;
    }

    private static String fmt(double v) { return String.valueOf((int) v); }
    private static String fmt(float  v) { return String.valueOf((int) v); }
}
