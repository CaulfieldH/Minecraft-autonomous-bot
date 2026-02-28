package com.autonomousbot;

import com.autonomousbot.ai.BotMode;
import com.autonomousbot.entity.EntityAutonomousBot;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Команда /bot — управление автономными ботами в игре.
 *
 * Использование:
 *   /bot spawn [x y z]                          — спавнит бота (рядом или по координатам)
 *   /bot kill <id>                               — удаляет бота
 *   /bot mode <id> <pvp|resource_gathering|building> — переключает режим
 *   /bot info <id>                               — информация о боте
 *   /bot list                                    — список всех ботов
 *   /bot buildreset <id>                         — сбросить флаг «убежище построено»
 *
 * Требует права оператора (уровень 2).
 */
public class CommandBotControl extends CommandBase {

    private static final String COLOR_HEADER  = EnumChatFormatting.YELLOW + "";
    private static final String COLOR_OK      = EnumChatFormatting.GREEN  + "";
    private static final String COLOR_ERROR   = EnumChatFormatting.RED    + "";
    private static final String COLOR_INFO    = EnumChatFormatting.AQUA   + "";
    private static final String COLOR_RESET   = EnumChatFormatting.WHITE  + "";

    // ─── ICommand ───────────────────────────────────────────────────────────────

    @Override
    public String getCommandName() { return "bot"; }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/bot <spawn|kill|mode|info|list|buildreset>";
    }

    @Override
    public int getRequiredPermissionLevel() { return 2; }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) { return true; }

    // ─── Dispatch ───────────────────────────────────────────────────────────────

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) { showHelp(sender); return; }

        switch (args[0].toLowerCase()) {
            case "spawn":      cmdSpawn(sender, args);      break;
            case "kill":       cmdKill(sender, args);       break;
            case "mode":       cmdMode(sender, args);       break;
            case "info":       cmdInfo(sender, args);       break;
            case "list":       cmdList(sender);             break;
            case "buildreset": cmdBuildReset(sender, args); break;
            default:           showHelp(sender);
        }
    }

    // ─── Подкоманды ─────────────────────────────────────────────────────────────

    /** /bot spawn [x y z] */
    private void cmdSpawn(ICommandSender sender, String[] args) {
        double x = sender.getPlayerCoordinates().posX;
        double y = sender.getPlayerCoordinates().posY;
        double z = sender.getPlayerCoordinates().posZ + 2.0;

        if (args.length >= 4) {
            try {
                x = Double.parseDouble(args[1]);
                y = Double.parseDouble(args[2]);
                z = Double.parseDouble(args[3]);
            } catch (NumberFormatException e) {
                sender.addChatMessage(msg(COLOR_ERROR + "Invalid coordinates: " + args[1] + " " + args[2] + " " + args[3]));
                return;
            }
        }

        WorldServer world = getOverworld();
        EntityAutonomousBot bot = new EntityAutonomousBot(world);
        bot.setPosition(x, y, z);
        bot.setBotMode(ConfigHandler.defaultMode);
        bot.applyConfigStats(
            ConfigHandler.maxHealth,
            ConfigHandler.moveSpeed,
            ConfigHandler.attackDamage
        );

        if (world.spawnEntityInWorld(bot)) {
            sender.addChatMessage(msg(
                COLOR_OK + "Bot spawned! " +
                COLOR_INFO + "ID: " + bot.getEntityId() +
                COLOR_RESET + " | Mode: " + COLOR_HEADER + bot.getBotMode().getDisplayName() +
                COLOR_RESET + " | Pos: " + fmt(x) + ", " + fmt(y) + ", " + fmt(z)
            ));
        } else {
            sender.addChatMessage(msg(COLOR_ERROR + "Failed to spawn bot at those coordinates!"));
        }
    }

    /** /bot kill <id> */
    private void cmdKill(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.addChatMessage(msg(COLOR_ERROR + "Usage: /bot kill <id>")); return;
        }
        EntityAutonomousBot bot = findBot(sender, args[1]);
        if (bot == null) return;

        bot.setDead();
        sender.addChatMessage(msg(COLOR_OK + "Bot " + bot.getEntityId() + " removed."));
    }

    /** /bot mode <id> <pvp|resource_gathering|building> */
    private void cmdMode(ICommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.addChatMessage(msg(COLOR_ERROR + "Usage: /bot mode <id> <pvp|resource_gathering|building>")); return;
        }
        EntityAutonomousBot bot = findBot(sender, args[1]);
        if (bot == null) return;

        BotMode newMode = BotMode.fromString(args[2]);
        bot.setBotMode(newMode);
        sender.addChatMessage(msg(
            COLOR_OK + "Bot " + bot.getEntityId() + " → mode: " +
            COLOR_HEADER + newMode.getDisplayName()
        ));
    }

    /** /bot info <id> */
    private void cmdInfo(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.addChatMessage(msg(COLOR_ERROR + "Usage: /bot info <id>")); return;
        }
        EntityAutonomousBot bot = findBot(sender, args[1]);
        if (bot == null) return;

        sender.addChatMessage(msg(COLOR_HEADER + "=== Bot #" + bot.getEntityId() + " ==="));
        sender.addChatMessage(msg(COLOR_INFO + "Mode:  " + COLOR_RESET + bot.getBotMode().getDisplayName()));
        sender.addChatMessage(msg(COLOR_INFO + "HP:    " + COLOR_RESET + fmt(bot.getHealth()) + " / " + fmt(bot.getMaxHealth())));
        sender.addChatMessage(msg(COLOR_INFO + "Pos:   " + COLOR_RESET + fmt(bot.posX) + ", " + fmt(bot.posY) + ", " + fmt(bot.posZ)));

        int itemCount = 0;
        for (net.minecraft.item.ItemStack s : bot.getBotInventory()) { if (s != null) itemCount++; }
        sender.addChatMessage(msg(COLOR_INFO + "Inventory slots used: " + COLOR_RESET + itemCount + " / 27"));
    }

    /** /bot list */
    private void cmdList(ICommandSender sender) {
        List<EntityAutonomousBot> bots = getAllBots();
        if (bots.isEmpty()) {
            sender.addChatMessage(msg(COLOR_HEADER + "No active bots.")); return;
        }
        sender.addChatMessage(msg(COLOR_HEADER + "Active Bots (" + bots.size() + "):"));
        for (EntityAutonomousBot b : bots) {
            sender.addChatMessage(msg(
                COLOR_INFO + "  #" + b.getEntityId() +
                COLOR_RESET + " | " + b.getBotMode().getDisplayName() +
                " | HP " + fmt(b.getHealth()) + "/" + fmt(b.getMaxHealth()) +
                " | " + fmt(b.posX) + "," + fmt(b.posY) + "," + fmt(b.posZ)
            ));
        }
    }

    /** /bot buildreset <id> */
    private void cmdBuildReset(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.addChatMessage(msg(COLOR_ERROR + "Usage: /bot buildreset <id>")); return;
        }
        EntityAutonomousBot bot = findBot(sender, args[1]);
        if (bot == null) return;

        // Найдём BotAIBuildShelter в задачах бота и сбросим флаг
        for (Object taskEntry : bot.tasks.taskEntries) {
            net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry entry =
                (net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry) taskEntry;
            if (entry.action instanceof com.autonomousbot.ai.BotAIBuildShelter) {
                ((com.autonomousbot.ai.BotAIBuildShelter) entry.action).resetShelterFlag();
                sender.addChatMessage(msg(COLOR_OK + "Shelter flag reset for bot " + bot.getEntityId() + "."));
                return;
            }
        }
        sender.addChatMessage(msg(COLOR_ERROR + "Could not find build task on bot " + bot.getEntityId()));
    }

    // ─── Help ───────────────────────────────────────────────────────────────────

    private void showHelp(ICommandSender sender) {
        sender.addChatMessage(msg(COLOR_HEADER + "=== Autonomous Bot Commands ==="));
        sender.addChatMessage(msg(COLOR_OK + "/bot spawn [x y z]" + COLOR_RESET + " — spawn a bot"));
        sender.addChatMessage(msg(COLOR_OK + "/bot kill <id>" + COLOR_RESET + " — remove a bot"));
        sender.addChatMessage(msg(COLOR_OK + "/bot mode <id> <pvp|resource_gathering|building>" + COLOR_RESET + " — set mode"));
        sender.addChatMessage(msg(COLOR_OK + "/bot info <id>" + COLOR_RESET + " — show bot details"));
        sender.addChatMessage(msg(COLOR_OK + "/bot list" + COLOR_RESET + " — list all bots"));
        sender.addChatMessage(msg(COLOR_OK + "/bot buildreset <id>" + COLOR_RESET + " — allow bot to build again"));
    }

    // ─── Утилиты ────────────────────────────────────────────────────────────────

    private ChatComponentText msg(String text) {
        return new ChatComponentText(text);
    }

    private String fmt(double v) { return String.valueOf((int) v); }
    private String fmt(float  v) { return String.valueOf((int) v); }

    @SuppressWarnings("unchecked")
    private List<EntityAutonomousBot> getAllBots() {
        List<EntityAutonomousBot> result = new ArrayList<EntityAutonomousBot>();
        for (WorldServer world : MinecraftServer.getServer().worldServers) {
            if (world == null) continue;
            for (Object e : (List<?>) world.loadedEntityList) {
                if (e instanceof EntityAutonomousBot) {
                    result.add((EntityAutonomousBot) e);
                }
            }
        }
        return result;
    }

    private EntityAutonomousBot findBot(ICommandSender sender, String idStr) {
        int id;
        try { id = Integer.parseInt(idStr); }
        catch (NumberFormatException e) {
            sender.addChatMessage(msg(COLOR_ERROR + "Invalid ID: " + idStr));
            return null;
        }
        for (EntityAutonomousBot b : getAllBots()) {
            if (b.getEntityId() == id) return b;
        }
        sender.addChatMessage(msg(COLOR_ERROR + "Bot with ID " + id + " not found!"));
        return null;
    }

    private WorldServer getOverworld() {
        return MinecraftServer.getServer().worldServerForDimension(0);
    }

    // ─── Tab completion ──────────────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args,
                "spawn", "kill", "mode", "info", "list", "buildreset");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("mode")) {
            return getListOfStringsMatchingLastWord(args,
                "pvp", "resource_gathering", "building");
        }
        return null;
    }
}
