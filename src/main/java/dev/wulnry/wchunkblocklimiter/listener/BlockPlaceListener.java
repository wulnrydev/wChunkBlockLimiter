package dev.wulnry.wchunkblocklimiter.listener;

import dev.wulnry.wchunkblocklimiter.manager.ChunkDataManager;
import dev.wulnry.wchunkblocklimiter.manager.ConfigManager;
import dev.wulnry.wchunkblocklimiter.manager.MessageManager;
import dev.wulnry.wchunkblocklimiter.util.SoundUtil;
import dev.wulnry.wchunkblocklimiter.wChunkBlockLimiter;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Listens for block placement and break events to enforce limits.
 * 
 * <p>
 * This listener is the core enforcement mechanism for the plugin. It:
 * </p>
 * <ul>
 * <li>Validates block placements against chunk limits</li>
 * <li>Cancels placement if limit is exceeded</li>
 * <li>Sends feedback (action bar + sound) to players</li>
 * <li>Updates the cache on successful placement/break</li>
 * <li>Respects bypass permissions</li>
 * </ul>
 * 
 * <p>
 * <b>Performance:</b> O(1) cache lookup on each block place event.
 * Uses HIGHEST priority to have final say after other plugins.
 * </p>
 * 
 * @author wulnrydev
 * @version 1.0.0
 */
public class BlockPlaceListener implements Listener {

    private final wChunkBlockLimiter plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final ChunkDataManager chunkDataManager;

    /**
     * Constructs a new BlockPlaceListener.
     * 
     * @param plugin           The main plugin instance
     * @param configManager    The config manager
     * @param messageManager   The message manager
     * @param chunkDataManager The chunk data manager
     */
    public BlockPlaceListener(
            wChunkBlockLimiter plugin,
            ConfigManager configManager,
            MessageManager messageManager,
            ChunkDataManager chunkDataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.chunkDataManager = chunkDataManager;
    }

    /**
     * Handles block placement events.
     * 
     * <p>
     * Priority is HIGHEST to ensure we have the final say after
     * protection plugins and other systems.
     * </p>
     * 
     * @param event The block place event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        // Check if plugin is enabled
        if (!configManager.isEnabled()) {
            return;
        }

        Player player = event.getPlayer();

        // Check bypass permission
        if (player.hasPermission(configManager.getBypassPermission())) {
            return;
        }

        Material material = event.getBlock().getType();

        // Check if this material is limited
        if (!configManager.isLimited(material)) {
            return;
        }

        Chunk chunk = event.getBlock().getChunk();

        // Validate against limit (O(1) cache lookup)
        if (!chunkDataManager.canPlaceBlock(chunk, material)) {
            // Limit exceeded - cancel placement
            event.setCancelled(true);

            // Send feedback to player
            sendLimitReachedFeedback(player, chunk, material);
            return;
        }

        // Placement allowed - update cache
        chunkDataManager.handleBlockPlace(chunk, material);

        // Send placement counter feedback (Issue #5 fix)
        sendPlacementFeedback(player, chunk, material);
    }

    /**
     * Handles block break events to update the cache.
     * 
     * @param event The block break event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        // Check if plugin is enabled
        if (!configManager.isEnabled()) {
            return;
        }

        Material material = event.getBlock().getType();

        // Only track limited materials
        if (!configManager.isLimited(material)) {
            return;
        }

        Chunk chunk = event.getBlock().getChunk();
        chunkDataManager.handleBlockBreak(chunk, material);
    }

    /**
     * Sends feedback to a player when the limit is reached.
     * 
     * <p>
     * Includes action bar message, optional chat message, and optional sound
     * notification.
     * </p>
     * 
     * @param player   The player
     * @param chunk    The chunk
     * @param material The material that exceeded the limit
     */
    private void sendLimitReachedFeedback(Player player, Chunk chunk, Material material) {
        // Get world-aware limit (Issue #2 fix)
        String worldName = chunk.getWorld().getName();
        int limit = configManager.getLimit(worldName, material);
        int currentCount = chunkDataManager.getBlockCount(chunk, material);

        // Prepare placeholders (Issue #3 fix - added %prefix%)
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%prefix%", messageManager.getPrefix());
        placeholders.put("%block%", material.name());
        placeholders.put("%limit%", String.valueOf(limit));
        placeholders.put("%current%", String.valueOf(currentCount));
        placeholders.put("%chunk_x%", String.valueOf(chunk.getX()));
        placeholders.put("%chunk_z%", String.valueOf(chunk.getZ()));
        placeholders.put("%world%", chunk.getWorld().getName());
        placeholders.put("%player%", player.getName());

        // Send action bar message
        if (configManager.isActionBarEnabled()) {
            messageManager.sendActionBar(player, "limit-reached.action-bar", placeholders);
        }

        // Send chat message (Issue #4 fix - optional chat message)
        if (configManager.isChatMessageEnabled()) {
            messageManager.sendMessage(player, "limit-reached.chat", placeholders);
        }

        // Play sound
        if (configManager.isSoundEnabled()) {
            SoundUtil.playSound(
                    player,
                    configManager.getSoundType(),
                    configManager.getSoundVolume(),
                    configManager.getSoundPitch(),
                    plugin.getLogger());
        }
    }

    /**
     * Sends placement success feedback showing current count (Issue #5 fix).
     * 
     * @param player   The player
     * @param chunk    The chunk
     * @param material The material placed
     */
    private void sendPlacementFeedback(Player player, Chunk chunk, Material material) {
        // Only show if enabled in config
        if (!configManager.isPlacementCounterEnabled()) {
            return;
        }

        String worldName = chunk.getWorld().getName();
        int limit = configManager.getLimit(worldName, material);
        int currentCount = chunkDataManager.getBlockCount(chunk, material);

        // Prepare placeholders
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%current%", String.valueOf(currentCount));
        placeholders.put("%limit%", String.valueOf(limit));
        placeholders.put("%block%", material.name());

        // Send action bar with count
        messageManager.sendActionBar(player, "placement-success.action-bar", placeholders);
    }
}
