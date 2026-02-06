package dev.wulnry.wchunkblocklimiter.command;

import dev.wulnry.wchunkblocklimiter.manager.ChunkDataManager;
import dev.wulnry.wchunkblocklimiter.manager.ConfigManager;
import dev.wulnry.wchunkblocklimiter.manager.MessageManager;
import dev.wulnry.wchunkblocklimiter.wChunkBlockLimiter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Handles all wChunkBlockLimiter commands.
 * 
 * <p>
 * Commands:
 * </p>
 * <ul>
 * <li>/wcbl reload - Reload configuration</li>
 * <li>/wcbl stats - View plugin statistics</li>
 * <li>/wcbl check [chunk] - Check block counts in a chunk</li>
 * <li>/wcbl reset &lt;chunk&gt; - Reset a specific chunk</li>
 * <li>/wcbl resetworld &lt;world&gt; - Reset all chunks in a world</li>
 * </ul>
 * 
 * @author wulnrydev
 * @version 1.3.0
 */
public class MainCommand implements CommandExecutor, TabCompleter {

    private final wChunkBlockLimiter plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final ChunkDataManager chunkDataManager;

    public MainCommand(
            wChunkBlockLimiter plugin,
            ConfigManager configManager,
            MessageManager messageManager,
            ChunkDataManager chunkDataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.chunkDataManager = chunkDataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender, label);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "reload":
                return handleReload(sender);
            case "stats":
                return handleStats(sender);
            case "check":
                return handleCheck(sender, args);
            case "reset":
                return handleReset(sender, args);
            case "resetworld":
                return handleResetWorld(sender, args);
            default:
                showHelp(sender, label);
                return true;
        }
    }

    private void showHelp(CommandSender sender, String label) {
        sender.sendMessage("§8§m                                                    §r");
        sender.sendMessage("  §6§lwChunkBlockLimiter §8v" + plugin.getDescription().getVersion());
        sender.sendMessage("");
        sender.sendMessage("  §7/§e" + label + " reload §8- §7Reload configuration");
        sender.sendMessage("  §7/§e" + label + " stats §8- §7View statistics");
        sender.sendMessage("  §7/§e" + label + " check [chunk] §8- §7Check chunks");
        sender.sendMessage("  §7/§e" + label + " reset <chunk> §8- §7Reset chunk");
        sender.sendMessage("  §7/§e" + label + " resetworld <world> §8- §7Reset world");
        sender.sendMessage("§8§m                                                    §r");
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("wchunkblocklimiter.reload")) {
            sendNoPermission(sender);
            return true;
        }

        try {
            sender.sendMessage(messageManager.getMessage("reload.in-progress"));

            configManager.loadConfig();
            messageManager.loadMessages();
            chunkDataManager.clearCache();

            int chunksScanned = rescanLoadedChunks();

            String successMessage = messageManager.getMessage("reload.success");
            sender.sendMessage(successMessage + " §7(§a" + chunksScanned + " §7chunks rescanned)");

            plugin.getLogger().info(String.format(
                    "Configuration reloaded by %s - %d chunks rescanned",
                    sender.getName(), chunksScanned));

        } catch (Exception e) {
            sender.sendMessage(messageManager.getMessage("reload.failed"));
            plugin.getLogger().severe("Failed to reload configuration:");
            e.printStackTrace();
        }

        return true;
    }

    private boolean handleStats(CommandSender sender) {
        if (!sender.hasPermission("wchunkblocklimiter.admin")) {
            sendNoPermission(sender);
            return true;
        }

        int cachedChunks = chunkDataManager.getCachedChunkCount();
        int loadedChunks = 0;
        int totalWorlds = Bukkit.getWorlds().size();

        for (World world : Bukkit.getWorlds()) {
            loadedChunks += world.getLoadedChunks().length;
        }

        sender.sendMessage("§8§m                                                    §r");
        sender.sendMessage("  §6§lwChunkBlockLimiter §eStatistics");
        sender.sendMessage("");
        sender.sendMessage("  §7Plugin Status: " + (configManager.isEnabled() ? "§aEnabled" : "§cDisabled"));
        sender.sendMessage("  §7Cached Chunks: §e" + cachedChunks);
        sender.sendMessage("  §7Loaded Chunks: §e" + loadedChunks);
        sender.sendMessage("  §7Worlds: §e" + totalWorlds);
        sender.sendMessage(
                "  §7Piston Protection: " + (configManager.isPistonProtectionEnabled() ? "§aEnabled" : "§cDisabled"));
        sender.sendMessage("§8§m                                                    §r");

        return true;
    }

    private boolean handleCheck(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wchunkblocklimiter.admin")) {
            sendNoPermission(sender);
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;
        Chunk chunk = player.getLocation().getChunk();

        sender.sendMessage("§8§m                                                    §r");
        sender.sendMessage("  §6§lChunk Information");
        sender.sendMessage("");
        sender.sendMessage("  §7World: §e" + chunk.getWorld().getName());
        sender.sendMessage("  §7Coordinates: §e" + chunk.getX() + "§7, §e" + chunk.getZ());
        sender.sendMessage("");
        sender.sendMessage("  §7Limited Blocks:");

        Map<Material, Integer> limits = configManager.getAllLimits();
        boolean foundLimited = false;

        for (Map.Entry<Material, Integer> entry : limits.entrySet()) {
            Material material = entry.getKey();
            int count = chunkDataManager.getBlockCount(chunk, material);

            if (count > 0) {
                int limit = configManager.getLimit(chunk.getWorld().getName(), material);
                String limitStr = limit == -1 ? "∞" : String.valueOf(limit);
                sender.sendMessage("    §e" + material.name() + "§7: §a" + count + "§7/§6" + limitStr);
                foundLimited = true;
            }
        }

        if (!foundLimited) {
            sender.sendMessage("    §7No limited blocks in this chunk");
        }

        sender.sendMessage("§8§m                                                    §r");

        return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wchunkblocklimiter.admin")) {
            sendNoPermission(sender);
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;
        Chunk chunk = player.getLocation().getChunk();

        chunkDataManager.handleChunkUnload(chunk);
        chunkDataManager.scanChunk(chunk);

        sender.sendMessage(messageManager.getPrefix() + " §aChunk reset and rescanned at §e(" +
                chunk.getX() + ", " + chunk.getZ() + ")§a in world §e" + chunk.getWorld().getName());

        return true;
    }

    private boolean handleResetWorld(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wchunkblocklimiter.admin")) {
            sendNoPermission(sender);
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /wcbl resetworld <world>");
            return true;
        }

        String worldName = args[1];
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            sender.sendMessage("§cWorld not found: §e" + worldName);
            return true;
        }

        int count = 0;
        for (Chunk chunk : world.getLoadedChunks()) {
            chunkDataManager.handleChunkUnload(chunk);
            chunkDataManager.scanChunk(chunk);
            count++;
        }

        sender.sendMessage(messageManager.getPrefix() + " §aReset §e" + count +
                " §achunks in world §e" + worldName);

        plugin.getLogger().info(String.format(
                "%s reset %d chunks in world %s",
                sender.getName(), count, worldName));

        return true;
    }

    private int rescanLoadedChunks() {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                chunkDataManager.scanChunk(chunk);
                count++;
            }
        }
        return count;
    }

    private void sendNoPermission(CommandSender sender) {
        if (sender instanceof Player) {
            messageManager.sendMessage((Player) sender, "no-permission");
        } else {
            sender.sendMessage(messageManager.getMessage("no-permission"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("reload", "stats", "check", "reset", "resetworld");
            for (String sub : subcommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("resetworld")) {
            for (World world : Bukkit.getWorlds()) {
                if (world.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(world.getName());
                }
            }
        }

        return completions;
    }
}
