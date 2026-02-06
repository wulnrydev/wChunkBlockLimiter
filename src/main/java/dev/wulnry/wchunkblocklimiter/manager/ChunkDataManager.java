package dev.wulnry.wchunkblocklimiter.manager;

import dev.wulnry.wchunkblocklimiter.cache.ChunkCache;
import dev.wulnry.wchunkblocklimiter.util.ChunkKey;
import dev.wulnry.wchunkblocklimiter.wChunkBlockLimiter;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.Map;

/**
 * Manages chunk data scanning, validation, and business logic.
 * 
 * <p>
 * This manager is responsible for:
 * </p>
 * <ul>
 * <li>Scanning chunks on load to populate the cache</li>
 * <li>Validating block placements against configured limits</li>
 * <li>Coordinating with ChunkCache for data storage</li>
 * <li>Handling chunk unload events</li>
 * </ul>
 * 
 * <p>
 * <b>Performance Optimization:</b>
 * </p>
 * Chunk scanning only counts materials that are configured with limits,
 * ignoring all other blocks to minimize iteration overhead.
 * 
 * @author wulnrydev
 * @version 1.0.0
 */
public class ChunkDataManager {

    private final wChunkBlockLimiter plugin;
    private final ChunkCache chunkCache;
    private final ConfigManager configManager;

    /**
     * Constructs a new ChunkDataManager.
     * 
     * @param plugin        The main plugin instance
     * @param chunkCache    The chunk cache instance
     * @param configManager The config manager instance
     */
    public ChunkDataManager(wChunkBlockLimiter plugin, ChunkCache chunkCache, ConfigManager configManager) {
        this.plugin = plugin;
        this.chunkCache = chunkCache;
        this.configManager = configManager;
    }

    /**
     * Scans a chunk and populates the cache with block counts.
     * 
     * <p>
     * This method iterates through all blocks in the chunk (16x16 horizontally,
     * with world height vertically) and counts only the materials that have
     * configured limits.
     * </p>
     * 
     * <p>
     * <b>Performance:</b> O(n) where n = chunk volume, but only increments
     * counters for limited materials. Called once per chunk load.
     * </p>
     * 
     * @param chunk The chunk to scan
     */
    public void scanChunk(Chunk chunk) {
        ChunkKey key = new ChunkKey(chunk);

        // Get all configured limits to know which materials to count
        Map<Material, Integer> limits = configManager.getAllLimits();
        if (limits.isEmpty()) {
            return; // Nothing to track
        }

        // Get world height boundaries
        int minHeight = chunk.getWorld().getMinHeight();
        int maxHeight = chunk.getWorld().getMaxHeight();

        // Scan all blocks in the chunk
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minHeight; y < maxHeight; y++) {
                    Block block = chunk.getBlock(x, y, z);
                    Material material = block.getType();

                    // Only count if this material is limited
                    if (limits.containsKey(material)) {
                        chunkCache.incrementCount(key, material);
                    }
                }
            }
        }
    }

    /**
     * Checks if a block can be placed in the chunk without exceeding limits.
     * 
     * <p>
     * This method performs an O(1) cache lookup to validate placement.
     * Automatically checks world-specific limits first, then falls back to global
     * limits.
     * </p>
     * 
     * @param chunk    The chunk where the block will be placed
     * @param material The material being placed
     * @return true if placement is allowed, false if limit exceeded
     */
    public boolean canPlaceBlock(Chunk chunk, Material material) {
        String worldName = chunk.getWorld().getName();
        int limit = configManager.getLimit(worldName, material);

        // -1 means unlimited
        if (limit == -1) {
            return true;
        }

        ChunkKey key = new ChunkKey(chunk);
        int currentCount = chunkCache.getCount(key, material);

        return currentCount < limit;
    }

    /**
     * Handles a successful block placement by incrementing the cache.
     * 
     * @param chunk    The chunk where the block was placed
     * @param material The material that was placed
     */
    public void handleBlockPlace(Chunk chunk, Material material) {
        // Only track limited materials
        if (!configManager.isLimited(material)) {
            return;
        }

        ChunkKey key = new ChunkKey(chunk);
        chunkCache.incrementCount(key, material);
    }

    /**
     * Handles a block break by decrementing the cache.
     * 
     * @param chunk    The chunk where the block was broken
     * @param material The material that was broken
     */
    public void handleBlockBreak(Chunk chunk, Material material) {
        // Only track limited materials
        if (!configManager.isLimited(material)) {
            return;
        }

        ChunkKey key = new ChunkKey(chunk);
        chunkCache.decrementCount(key, material);
    }

    /**
     * Handles chunk unload by removing it from the cache.
     * 
     * <p>
     * This prevents memory leaks from inactive chunks. Data will be
     * rebuilt when the chunk loads again.
     * </p>
     * 
     * @param chunk The chunk being unloaded
     */
    public void handleChunkUnload(Chunk chunk) {
        ChunkKey key = new ChunkKey(chunk);
        chunkCache.removeChunk(key);
    }

    /**
     * Gets the current count of a material in a chunk.
     * 
     * <p>
     * Useful for debugging or admin commands.
     * </p>
     * 
     * @param chunk    The chunk
     * @param material The material
     * @return The current count
     */
    public int getBlockCount(Chunk chunk, Material material) {
        ChunkKey key = new ChunkKey(chunk);
        return chunkCache.getCount(key, material);
    }

    /**
     * Clears all cached chunk data.
     * 
     * <p>
     * Used when reloading configuration to ensure fresh state.
     * </p>
     */
    public void clearCache() {
        chunkCache.clear();
        plugin.getLogger().info("Chunk cache cleared");
    }

    /**
     * Gets the number of chunks currently cached.
     * 
     * @return The cached chunk count
     */
    public int getCachedChunkCount() {
        return chunkCache.getCachedChunkCount();
    }
}
