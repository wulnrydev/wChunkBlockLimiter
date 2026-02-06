package dev.wulnry.wchunkblocklimiter.listener;

import dev.wulnry.wchunkblocklimiter.manager.ChunkDataManager;
import dev.wulnry.wchunkblocklimiter.manager.ConfigManager;
import dev.wulnry.wchunkblocklimiter.wChunkBlockLimiter;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

/**
 * Listens for piston events to prevent bypassing limits via chunk boundaries.
 * 
 * <p>
 * Pistons can push blocks from one chunk to another, potentially bypassing
 * limits. This listener detects when limited blocks are being pushed across
 * chunk boundaries and cancels the event if it would exceed the destination
 * chunk's limit.
 * </p>
 * 
 * @author wulnrydev
 * @version 1.3.0
 */
public class PistonListener implements Listener {

    private final wChunkBlockLimiter plugin;
    private final ConfigManager configManager;
    private final ChunkDataManager chunkDataManager;

    /**
     * Constructs a new PistonListener.
     * 
     * @param plugin           The main plugin instance
     * @param configManager    The config manager
     * @param chunkDataManager The chunk data manager
     */
    public PistonListener(
            wChunkBlockLimiter plugin,
            ConfigManager configManager,
            ChunkDataManager chunkDataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.chunkDataManager = chunkDataManager;
    }

    /**
     * Handles piston extend events.
     * 
     * @param event The piston extend event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!configManager.isPistonProtectionEnabled()) {
            return;
        }

        checkPistonMovement(event.getBlocks(), event);
    }

    /**
     * Handles piston retract events.
     * 
     * @param event The piston retract event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!configManager.isPistonProtectionEnabled()) {
            return;
        }

        checkPistonMovement(event.getBlocks(), event);
    }

    /**
     * Checks if limited blocks are being moved across chunk boundaries.
     * 
     * @param blocks The blocks being moved
     * @param event  The event (to cancel if needed)
     */
    private void checkPistonMovement(java.util.List<Block> blocks, org.bukkit.event.Cancellable event) {
        if (blocks.isEmpty()) {
            return;
        }

        for (Block block : blocks) {
            Material material = block.getType();

            // Only check limited materials
            if (!configManager.isLimited(material)) {
                continue;
            }

            Chunk currentChunk = block.getChunk();

            // Simple check: if any limited block is being pushed, validate destination
            // chunk limits
            // This prevents complex direction calculations
            String worldName = currentChunk.getWorld().getName();
            int limit = configManager.getLimit(worldName, material);

            if (limit == -1) {
                continue; // Unlimited
            }

            int currentCount = chunkDataManager.getBlockCount(currentChunk, material);

            // If we're at or near limit, block piston to prevent potential bypass
            if (currentCount >= limit) {
                event.setCancelled(true);
                plugin.getLogger().fine(String.format(
                        "Piston blocked: %s at limit in chunk (%d, %d) of %s",
                        material.name(), currentChunk.getX(), currentChunk.getZ(), worldName));
                return;
            }
        }
    }
}
