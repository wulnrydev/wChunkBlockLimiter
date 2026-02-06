package dev.wulnry.wchunkblocklimiter.listener;

import dev.wulnry.wchunkblocklimiter.manager.ChunkDataManager;
import dev.wulnry.wchunkblocklimiter.wChunkBlockLimiter;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * Listens for chunk load and unload events to maintain cache consistency.
 * 
 * <p>
 * This listener ensures that:
 * </p>
 * <ul>
 * <li>Chunks are scanned and cached when loaded</li>
 * <li>Chunk data is removed from cache when unloaded (prevents memory
 * leaks)</li>
 * <li>Cache stays synchronized with the world state</li>
 * </ul>
 * 
 * <p>
 * <b>Performance Considerations:</b>
 * </p>
 * <ul>
 * <li>Chunk scanning is O(n) where n = chunk volume, but only happens once per
 * load</li>
 * <li>Only materials with configured limits are counted during scan</li>
 * <li>Unload is O(1) - simple HashMap removal</li>
 * </ul>
 * 
 * @author wulnrydev
 * @version 1.0.0
 */
public class ChunkListener implements Listener {

    private final wChunkBlockLimiter plugin;
    private final ChunkDataManager chunkDataManager;

    /**
     * Constructs a new ChunkListener.
     * 
     * @param plugin           The main plugin instance
     * @param chunkDataManager The chunk data manager
     */
    public ChunkListener(wChunkBlockLimiter plugin, ChunkDataManager chunkDataManager) {
        this.plugin = plugin;
        this.chunkDataManager = chunkDataManager;
    }

    /**
     * Handles chunk load events.
     * 
     * <p>
     * Scans the chunk to populate the cache with block counts.
     * Uses MONITOR priority to ensure the chunk is fully loaded.
     * </p>
     * 
     * @param event The chunk load event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();

        // Scan the chunk and populate cache
        // This is done synchronously as chunk load events are safe for world access
        chunkDataManager.scanChunk(chunk);
    }

    /**
     * Handles chunk unload events.
     * 
     * <p>
     * Removes the chunk from cache to prevent memory leaks.
     * Data will be rebuilt when the chunk loads again.
     * </p>
     * 
     * @param event The chunk unload event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();

        // Remove from cache
        chunkDataManager.handleChunkUnload(chunk);
    }
}
