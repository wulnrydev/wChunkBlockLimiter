package dev.wulnry.wchunkblocklimiter.util;

import org.bukkit.Chunk;

import java.util.Objects;

/**
 * Immutable chunk identifier used as a HashMap key.
 * 
 * <p>This class provides a stable, hashable representation of a chunk's location
 * across world and coordinates. Unlike using Chunk objects directly (which can be
 * garbage collected), ChunkKey provides consistent hashing and equality.</p>
 * 
 * <p><b>Performance Considerations:</b></p>
 * <ul>
 *   <li>Immutable design ensures thread-safety for concurrent reads</li>
 *   <li>Cached hashCode prevents recomputation on every HashMap operation</li>
 *   <li>Lightweight (3 fields + cached int) minimizes memory overhead</li>
 * </ul>
 * 
 * @author wulnrydev
 * @version 1.0.0
 */
public final class ChunkKey {
    
    private final String world;
    private final int x;
    private final int z;
    private final int hashCode; // Cached for performance
    
    /**
     * Constructs a new ChunkKey from a Bukkit Chunk.
     * 
     * @param chunk The chunk to create a key for
     * @throws NullPointerException if chunk or its world is null
     */
    public ChunkKey(Chunk chunk) {
        this(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }
    
    /**
     * Constructs a new ChunkKey from world name and coordinates.
     * 
     * @param world The world name (case-sensitive)
     * @param x The chunk X coordinate
     * @param z The chunk Z coordinate
     * @throws NullPointerException if world is null
     */
    public ChunkKey(String world, int x, int z) {
        this.world = Objects.requireNonNull(world, "World cannot be null");
        this.x = x;
        this.z = z;
        this.hashCode = computeHashCode();
    }
    
    /**
     * Computes the hash code for this chunk key.
     * Cached on construction for optimal HashMap performance.
     * 
     * @return The computed hash code
     */
    private int computeHashCode() {
        return Objects.hash(world, x, z);
    }
    
    /**
     * Gets the world name.
     * 
     * @return The world name
     */
    public String getWorld() {
        return world;
    }
    
    /**
     * Gets the chunk X coordinate.
     * 
     * @return The X coordinate
     */
    public int getX() {
        return x;
    }
    
    /**
     * Gets the chunk Z coordinate.
     * 
     * @return The Z coordinate
     */
    public int getZ() {
        return z;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ChunkKey chunkKey = (ChunkKey) o;
        return x == chunkKey.x && 
               z == chunkKey.z && 
               world.equals(chunkKey.world);
    }
    
    @Override
    public int hashCode() {
        return hashCode; // Return cached value
    }
    
    @Override
    public String toString() {
        return String.format("ChunkKey{world='%s', x=%d, z=%d}", world, x, z);
    }
}
