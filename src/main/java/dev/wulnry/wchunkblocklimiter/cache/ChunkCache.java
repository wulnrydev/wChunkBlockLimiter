package dev.wulnry.wchunkblocklimiter.cache;

import dev.wulnry.wchunkblocklimiter.util.ChunkKey;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance in-memory cache for chunk block counts.
 * 
 * <p>
 * This cache stores the count of limited blocks per chunk using a nested
 * HashMap structure for O(1) lookup performance. The outer map uses chunk
 * coordinates as keys, while the inner map tracks material counts.
 * </p>
 * 
 * <p>
 * <b>Data Structure:</b>
 * </p>
 * 
 * <pre>
 * ConcurrentHashMap&lt;ChunkKey, HashMap&lt;Material, Integer&gt;&gt;
 * 
 * Example:
 * {
 *   ChunkKey(world, 10, 5): { HOPPER: 3, CHEST: 12 },
 *   ChunkKey(world, 11, 5): { SPAWNER: 1, FURNACE: 5 }
 * }
 * </pre>
 * 
 * <p>
 * <b>Thread Safety:</b>
 * </p>
 * <ul>
 * <li>ConcurrentHashMap allows safe concurrent reads</li>
 * <li>All modifications should occur on the main thread (via event
 * listeners)</li>
 * <li>No actual thread contention expected in this use case</li>
 * </ul>
 * 
 * <p>
 * <b>Memory Optimization:</b>
 * </p>
 * <ul>
 * <li>Only stores non-zero counts (lazy initialization)</li>
 * <li>Chunks with no limited blocks have no HashMap entry</li>
 * <li>Cleared on chunk unload to prevent memory leaks</li>
 * </ul>
 * 
 * @author wulnrydev
 * @version 1.0.0
 */
public class ChunkCache {

    /**
     * Main cache storage.
     * Key: ChunkKey (world + coordinates)
     * Value: HashMap of Material → count
     */
    private final ConcurrentHashMap<ChunkKey, Map<Material, Integer>> chunkData;

    /**
     * Constructs a new ChunkCache with default initial capacity.
     */
    public ChunkCache() {
        this.chunkData = new ConcurrentHashMap<>(256); // Initial capacity for ~256 chunks
    }

    /**
     * Gets the count of a specific material in a chunk.
     * 
     * @param key      The chunk key
     * @param material The material to check
     * @return The count, or 0 if not tracked
     */
    public int getCount(ChunkKey key, Material material) {
        Map<Material, Integer> materialCounts = chunkData.get(key);
        if (materialCounts == null) {
            return 0;
        }
        return materialCounts.getOrDefault(material, 0);
    }

    /**
     * Sets the count of a specific material in a chunk.
     * 
     * <p>
     * If count is 0 or negative, the entry is removed to save memory.
     * </p>
     * 
     * @param key      The chunk key
     * @param material The material
     * @param count    The count to set
     */
    public void setCount(ChunkKey key, Material material, int count) {
        if (count <= 0) {
            // Remove entry to save memory
            Map<Material, Integer> materialCounts = chunkData.get(key);
            if (materialCounts != null) {
                materialCounts.remove(material);
                // If chunk has no more blocks, remove the chunk entry entirely
                if (materialCounts.isEmpty()) {
                    chunkData.remove(key);
                }
            }
            return;
        }

        chunkData.computeIfAbsent(key, k -> new HashMap<>()).put(material, count);
    }

    /**
     * Increments the count of a specific material in a chunk.
     * 
     * @param key      The chunk key
     * @param material The material
     */
    public void incrementCount(ChunkKey key, Material material) {
        int currentCount = getCount(key, material);
        setCount(key, material, currentCount + 1);
    }

    /**
     * Decrements the count of a specific material in a chunk.
     * 
     * <p>
     * Count will not go below 0.
     * </p>
     * 
     * @param key      The chunk key
     * @param material The material
     */
    public void decrementCount(ChunkKey key, Material material) {
        int currentCount = getCount(key, material);
        if (currentCount > 0) {
            setCount(key, material, currentCount - 1);
        }
    }

    /**
     * Removes all cached data for a specific chunk.
     * 
     * <p>
     * Called when a chunk unloads to prevent memory leaks.
     * </p>
     * 
     * @param key The chunk key to remove
     */
    public void removeChunk(ChunkKey key) {
        chunkData.remove(key);
    }

    /**
     * Checks if a chunk is currently cached.
     * 
     * @param key The chunk key
     * @return true if the chunk has cached data
     */
    public boolean isChunkCached(ChunkKey key) {
        return chunkData.containsKey(key);
    }

    /**
     * Gets the total number of chunks currently cached.
     * 
     * @return The chunk count
     */
    public int getCachedChunkCount() {
        return chunkData.size();
    }

    /**
     * Clears all cached chunk data.
     * 
     * <p>
     * Used when reloading configuration to ensure fresh state.
     * </p>
     */
    public void clear() {
        chunkData.clear();
    }

    /**
     * Gets an unmodifiable view of the material counts for a chunk.
     * 
     * <p>
     * Useful for debugging or admin commands.
     * </p>
     * 
     * @param key The chunk key
     * @return Unmodifiable map of material counts, or empty map if chunk not cached
     */
    public Map<Material, Integer> getChunkData(ChunkKey key) {
        Map<Material, Integer> data = chunkData.get(key);
        return data == null ? Map.of() : Map.copyOf(data);
    }
}
