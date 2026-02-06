package dev.wulnry.wchunkblocklimiter.command;

import dev.wulnry.wchunkblocklimiter.manager.ChunkDataManager;
import dev.wulnry.wchunkblocklimiter.manager.ConfigManager;
import dev.wulnry.wchunkblocklimiter.manager.MessageManager;
import dev.wulnry.wchunkblocklimiter.wChunkBlockLimiter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the /wchunkblocklimiter command and its subcommands.
 * 
 * <p>
 * Currently supports:
 * </p>
 * <ul>
 * <li>reload - Reloads configuration and rescans all loaded chunks</li>
 * </ul>
 * 
 * @author wulnrydev
 * @version 1.0.0
 */
public class ReloadCommand implements CommandExecutor {

    private final wChunkBlockLimiter plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final ChunkDataManager chunkDataManager;

    /**
     * Constructs a new ReloadCommand.
     * 
     * @param plugin           The main plugin instance
     * @param configManager    The config manager
     * @param messageManager   The message manager
     * @param chunkDataManager The chunk data manager
     */
    public ReloadCommand(
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
        // Check if sender has permission
        if (!sender.hasPermission(configManager.getReloadPermission())) {
            if (sender instanceof Player) {
                messageManager.sendMessage((Player) sender, "no-permission");
            } else {
                sender.sendMessage(messageManager.getMessage("no-permission"));
            }
            return true;
        }

        // Handle reload subcommand
        if (args.length == 0 || args[0].equalsIgnoreCase("reload")) {
            handleReload(sender);
            return true;
        }

        // Unknown subcommand - show usage
        sender.sendMessage("§6wChunkBlockLimiter v" + plugin.getDescription().getVersion());
        sender.sendMessage("§7Usage: /" + label + " reload");
        return true;
    }

    /**
     * Handles the reload subcommand.
     * 
     * <p>
     * Performs the following steps:
     * </p>
     * <ol>
     * <li>Reloads config.yml and messages.yml</li>
     * <li>Clears the chunk cache</li>
     * <li>Rescans all currently loaded chunks</li>
     * <li>Sends success/failure message</li>
     * </ol>
     * 
     * @param sender The command sender
     */
    private void handleReload(CommandSender sender) {
        try {
            // Send in-progress message
            sender.sendMessage(messageManager.getMessage("reload.in-progress"));

            // Reload configurations
            configManager.loadConfig();
            messageManager.loadMessages();

            // Clear cache
            chunkDataManager.clearCache();

            // Rescan all loaded chunks
            int chunksScanned = rescanLoadedChunks();

            // Send success message
            String successMessage = messageManager.getMessage("reload.success");
            sender.sendMessage(successMessage + " §7(" + chunksScanned + " chunks rescanned)");

            plugin.getLogger().info(String.format(
                    "Configuration reloaded by %s - %d chunks rescanned",
                    sender.getName(),
                    chunksScanned));

        } catch (Exception e) {
            // Send failure message
            if (sender instanceof Player) {
                messageManager.sendMessage((Player) sender, "reload.failed");
            } else {
                sender.sendMessage(messageManager.getMessage("reload.failed"));
            }

            plugin.getLogger().severe("Failed to reload configuration:");
            e.printStackTrace();
        }
    }

    /**
     * Rescans all currently loaded chunks across all worlds.
     * 
     * <p>
     * This ensures the cache is consistent with the new configuration
     * after a reload.
     * </p>
     * 
     * @return The number of chunks rescanned
     */
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
}
