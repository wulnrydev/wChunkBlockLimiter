package dev.wulnry.wchunkblocklimiter;

import dev.wulnry.wchunkblocklimiter.cache.ChunkCache;
import dev.wulnry.wchunkblocklimiter.command.MainCommand;
import dev.wulnry.wchunkblocklimiter.integration.PlaceholderAPIExpansion;
import dev.wulnry.wchunkblocklimiter.listener.BlockPlaceListener;
import dev.wulnry.wchunkblocklimiter.listener.ChunkListener;
import dev.wulnry.wchunkblocklimiter.listener.PistonListener;
import dev.wulnry.wchunkblocklimiter.manager.ChunkDataManager;
import dev.wulnry.wchunkblocklimiter.manager.ConfigManager;
import dev.wulnry.wchunkblocklimiter.manager.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for wChunkBlockLimiter.
 * 
 * <p>
 * wChunkBlockLimiter is an enterprise-grade Minecraft plugin that enforces
 * configurable block placement limits per chunk. Designed for high-performance
 * servers with 500+ concurrent players.
 * </p>
 * 
 * <p>
 * <b>Key Features:</b>
 * </p>
 * <ul>
 * <li>O(1) block placement validation via in-memory cache</li>
 * <li>Configurable per-material and per-world limits</li>
 * <li>Bypass permissions for staff/VIPs</li>
 * <li>Action bar feedback and sound notifications</li>
 * <li>Piston protection to prevent limit bypass</li>
 * <li>PlaceholderAPI integration</li>
 * <li>Hot-reload support without server restart</li>
 * <li>Zero lag impact on block placement</li>
 * </ul>
 * 
 * <p>
 * <b>Architecture:</b>
 * </p>
 * <ul>
 * <li>ConfigManager - Configuration loading and validation</li>
 * <li>MessageManager - Message formatting and delivery</li>
 * <li>ChunkCache - High-performance in-memory storage</li>
 * <li>ChunkDataManager - Business logic for chunk data</li>
 * <li>BlockPlaceListener - Event enforcement</li>
 * <li>ChunkListener - Cache lifecycle management</li>
 * <li>PistonListener - Piston protection</li>
 * <li>MainCommand - All plugin commands</li>
 * </ul>
 * 
 * @author wulnrydev
 * @version 1.3.0
 */
public final class wChunkBlockLimiter extends JavaPlugin {

    // Core components
    private ConfigManager configManager;
    private MessageManager messageManager;
    private ChunkCache chunkCache;
    private ChunkDataManager chunkDataManager;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        // Print startup header
        getLogger().info("=================================");
        getLogger().info("  wChunkBlockLimiter v" + getDescription().getVersion());
        getLogger().info("  Author: wulnrydev");
        getLogger().info("=================================");

        // Initialize core components
        initializeComponents();

        // Register listeners
        registerListeners();

        // Register commands
        registerCommands();

        // Register PlaceholderAPI if available
        registerPlaceholderAPI();

        // Scan all currently loaded chunks (for hot-reload scenarios)
        scanLoadedChunks();

        long loadTime = System.currentTimeMillis() - startTime;
        getLogger().info("=================================");
        getLogger().info("  Plugin enabled successfully!");
        getLogger().info("  Load time: " + loadTime + "ms");
        getLogger().info("  Cached chunks: " + chunkDataManager.getCachedChunkCount());
        getLogger().info("=================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("=================================");
        getLogger().info("  wChunkBlockLimiter disabled");
        getLogger().info("  Clearing cache...");

        // Clear cache to free memory
        if (chunkDataManager != null) {
            chunkDataManager.clearCache();
        }

        getLogger().info("  Goodbye!");
        getLogger().info("=================================");
    }

    /**
     * Initializes all core components in the correct dependency order.
     */
    private void initializeComponents() {
        getLogger().info("Initializing components...");

        // Save default config if it doesn't exist
        saveDefaultConfig();

        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        this.chunkCache = new ChunkCache();
        this.chunkDataManager = new ChunkDataManager(this, chunkCache, configManager);

        getLogger().info("Components initialized");
    }

    /**
     * Registers all event listeners.
     */
    private void registerListeners() {
        getLogger().info("Registering listeners...");

        // Block placement listener
        getServer().getPluginManager().registerEvents(
                new BlockPlaceListener(this, configManager, messageManager, chunkDataManager),
                this);

        // Chunk lifecycle listener
        getServer().getPluginManager().registerEvents(
                new ChunkListener(this, chunkDataManager),
                this);

        // Piston protection listener
        getServer().getPluginManager().registerEvents(
                new PistonListener(this, configManager, chunkDataManager),
                this);

        getLogger().info("Listeners registered");
    }

    /**
     * Registers all commands.
     */
    private void registerCommands() {
        getLogger().info("Registering commands...");

        MainCommand mainCommand = new MainCommand(
                this,
                configManager,
                messageManager,
                chunkDataManager);

        getCommand("wchunkblocklimiter").setExecutor(mainCommand);
        getCommand("wchunkblocklimiter").setTabCompleter(mainCommand);

        getLogger().info("Commands registered");
    }

    /**
     * Registers PlaceholderAPI expansion if the plugin is available.
     */
    private void registerPlaceholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIExpansion(this, chunkDataManager).register();
            getLogger().info("PlaceholderAPI expansion registered");
        }
    }

    /**
     * Scans all currently loaded chunks.
     * 
     * <p>
     * This is called on plugin enable to handle server restarts
     * or plugin reloads when chunks are already loaded.
     * </p>
     */
    private void scanLoadedChunks() {
        getLogger().info("Scanning loaded chunks...");

        int totalChunks = 0;
        for (World world : Bukkit.getWorlds()) {
            Chunk[] chunks = world.getLoadedChunks();
            for (Chunk chunk : chunks) {
                chunkDataManager.scanChunk(chunk);
            }
            totalChunks += chunks.length;
        }

        getLogger().info("Scanned " + totalChunks + " chunks");
    }

    // ==================== Public Getters ====================

    /**
     * Gets the ConfigManager instance.
     * 
     * @return The config manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Gets the MessageManager instance.
     * 
     * @return The message manager
     */
    public MessageManager getMessageManager() {
        return messageManager;
    }

    /**
     * Gets the ChunkCache instance.
     * 
     * @return The chunk cache
     */
    public ChunkCache getChunkCache() {
        return chunkCache;
    }

    /**
     * Gets the ChunkDataManager instance.
     * 
     * @return The chunk data manager
     */
    public ChunkDataManager getChunkDataManager() {
        return chunkDataManager;
    }
}
