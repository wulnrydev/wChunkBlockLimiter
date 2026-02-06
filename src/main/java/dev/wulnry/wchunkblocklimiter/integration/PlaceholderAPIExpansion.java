package dev.wulnry.wchunkblocklimiter.integration;

import dev.wulnry.wchunkblocklimiter.manager.ChunkDataManager;
import dev.wulnry.wchunkblocklimiter.util.ChunkKey;
import dev.wulnry.wchunkblocklimiter.wChunkBlockLimiter;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion for wChunkBlockLimiter.
 * 
 * <p>
 * Provides placeholders for use in other plugins like DeluxeMenus,
 * FeatherBoard, etc.
 * </p>
 * 
 * <p>
 * <b>Available Placeholders:</b>
 * </p>
 * <ul>
 * <li>%wcbl_cached_chunks% - Number of chunks currently cached</li>
 * <li>%wcbl_chunk_x% - Current chunk X coordinate</li>
 * <li>%wcbl_chunk_z% - Current chunk Z coordinate</li>
 * <li>%wcbl_chunk_count_<material>% - Block count in current chunk</li>
 * </ul>
 * 
 * @author wulnrydev
 * @version 1.3.0
 */
public class PlaceholderAPIExpansion extends PlaceholderExpansion {

    private final wChunkBlockLimiter plugin;
    private final ChunkDataManager chunkDataManager;

    public PlaceholderAPIExpansion(wChunkBlockLimiter plugin, ChunkDataManager chunkDataManager) {
        this.plugin = plugin;
        this.chunkDataManager = chunkDataManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "wcbl";
    }

    @Override
    public @NotNull String getAuthor() {
        return "wulnrydev";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // %wcbl_cached_chunks%
        if (params.equals("cached_chunks")) {
            return String.valueOf(chunkDataManager.getCachedChunkCount());
        }

        // %wcbl_chunk_x%
        if (params.equals("chunk_x")) {
            return String.valueOf(player.getLocation().getChunk().getX());
        }

        // %wcbl_chunk_z%
        if (params.equals("chunk_z")) {
            return String.valueOf(player.getLocation().getChunk().getZ());
        }

        // %wcbl_chunk_count_<material>%
        if (params.startsWith("chunk_count_")) {
            String materialName = params.substring("chunk_count_".length()).toUpperCase();
            try {
                org.bukkit.Material material = org.bukkit.Material.valueOf(materialName);
                Chunk chunk = player.getLocation().getChunk();
                int count = chunkDataManager.getBlockCount(chunk, material);
                return String.valueOf(count);
            } catch (IllegalArgumentException e) {
                return "Invalid Material";
            }
        }

        return null;
    }
}
