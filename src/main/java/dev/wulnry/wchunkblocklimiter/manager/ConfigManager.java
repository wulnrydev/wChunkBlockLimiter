package dev.wulnry.wchunkblocklimiter.manager;

import dev.wulnry.wchunkblocklimiter.wChunkBlockLimiter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages configuration loading and validation for the plugin.
 * 
 * <p>
 * Handles parsing of config.yml, validates Material names, and provides
 * efficient access to block limits and settings.
 * </p>
 * 
 * <p>
 * <b>Key Responsibilities:</b>
 * </p>
 * <ul>
 * <li>Load and reload config.yml</li>
 * <li>Parse block limits into optimized HashMap</li>
 * <li>Validate material names with user-friendly warnings</li>
 * <li>Provide cached access to settings (enabled state, sounds,
 * permissions)</li>
 * </ul>
 * 
 * @author wulnrydev
 * @version 1.0.0
 */
public class ConfigManager {

    private final wChunkBlockLimiter plugin;

    // Cached configuration values
    private boolean enabled;
    private int defaultLimit;
    private Map<Material, Integer> blockLimits;
    private Map<String, Map<Material, Integer>> worldLimits; // Per-world overrides

    // Sound settings
    private boolean soundEnabled;
    private String soundType;
    private float soundVolume;
    private float soundPitch;

    // Action bar settings
    private boolean actionBarEnabled;

    // Chat message settings (Issue #4)
    private boolean chatMessageEnabled;

    // Placement counter settings (Issue #5)
    private boolean placementCounterEnabled;

    // Piston protection settings
    private boolean pistonProtectionEnabled;

    // Permission nodes
    private String bypassPermission;
    private String reloadPermission;

    /**
     * Constructs a new ConfigManager.
     * 
     * @param plugin The main plugin instance
     */
    public ConfigManager(wChunkBlockLimiter plugin) {
        this.plugin = plugin;
        this.blockLimits = new HashMap<>();
        this.worldLimits = new HashMap<>();
        loadConfig();
    }

    /**
     * Loads (or reloads) the configuration from config.yml.
     * 
     * <p>
     * This method is safe to call multiple times and will update
     * all cached values.
     * </p>
     */
    public void loadConfig() {
        // Reload config from disk
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // Load global settings
        enabled = config.getBoolean("enabled", true);
        defaultLimit = config.getInt("default-limit", -1);

        // Load block limits
        loadBlockLimits(config);

        // Load per-world limit overrides
        loadWorldLimits(config);

        // Load sound settings
        soundEnabled = config.getBoolean("sounds.enabled", true);
        soundType = config.getString("sounds.type", "BLOCK_NOTE_BLOCK_PLING");
        soundVolume = (float) config.getDouble("sounds.volume", 1.0);
        soundPitch = (float) config.getDouble("sounds.pitch", 0.5);

        // Load action bar settings
        actionBarEnabled = config.getBoolean("action-bar.enabled", true);

        // Load chat message settings (Issue #4)
        chatMessageEnabled = config.getBoolean("chat-message.enabled", false);

        // Load placement counter settings (Issue #5)
        placementCounterEnabled = config.getBoolean("placement-counter.enabled", true);

        // Load piston protection settings
        pistonProtectionEnabled = config.getBoolean("piston-protection.enabled", true);

        // Load permission nodes
        bypassPermission = config.getString("permissions.bypass", "wchunkblocklimiter.bypass");
        reloadPermission = config.getString("permissions.reload", "wchunkblocklimiter.reload");

        plugin.getLogger().info(String.format(
                "Configuration loaded! Tracking %d block types with limits.",
                blockLimits.size()));
    }

    /**
     * Loads block limits from the configuration.
     * 
     * <p>
     * Validates each material name and logs warnings for invalid entries.
     * Only valid materials are added to the limits map.
     * </p>
     * 
     * @param config The file configuration
     */
    private void loadBlockLimits(FileConfiguration config) {
        blockLimits.clear();

        ConfigurationSection limitsSection = config.getConfigurationSection("limits");
        if (limitsSection == null) {
            plugin.getLogger().warning("No 'limits' section found in config.yml!");
            return;
        }

        for (String key : limitsSection.getKeys(false)) {
            int limit = limitsSection.getInt(key, -1);

            // Try to parse material
            Material material = parseMaterial(key);
            if (material == null) {
                plugin.getLogger().log(Level.WARNING, String.format(
                        "Invalid material name in config: '%s' - skipping. " +
                                "Check https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html",
                        key));
                continue;
            }

            // Only add if limit is set (even if -1 for unlimited)
            blockLimits.put(material, limit);
        }
    }

    /**
     * Loads per-world limit overrides from the configuration.
     * 
     * <p>
     * World-specific limits take precedence over global limits.
     * Each world can have its own material limits.
     * </p>
     * 
     * @param config The file configuration
     */
    private void loadWorldLimits(FileConfiguration config) {
        worldLimits.clear();

        ConfigurationSection worldLimitsSection = config.getConfigurationSection("world-limits");
        if (worldLimitsSection == null) {
            // No world limits configured - this is optional
            return;
        }

        for (String worldName : worldLimitsSection.getKeys(false)) {
            ConfigurationSection worldSection = worldLimitsSection.getConfigurationSection(worldName);
            if (worldSection == null) {
                continue;
            }

            Map<Material, Integer> worldBlockLimits = new HashMap<>();

            for (String key : worldSection.getKeys(false)) {
                int limit = worldSection.getInt(key, -1);

                Material material = parseMaterial(key);
                if (material == null) {
                    plugin.getLogger().log(Level.WARNING, String.format(
                            "Invalid material name in world-limits for world '%s': '%s' - skipping.",
                            worldName, key));
                    continue;
                }

                worldBlockLimits.put(material, limit);
            }

            if (!worldBlockLimits.isEmpty()) {
                worldLimits.put(worldName, worldBlockLimits);
                plugin.getLogger().info(String.format(
                        "Loaded %d material overrides for world '%s'",
                        worldBlockLimits.size(), worldName));
            }
        }
    }

    /**
     * Parses a material name from the config.
     * 
     * <p>
     * Attempts to match the material name case-insensitively.
     * </p>
     * 
     * @param name The material name from config
     * @return The Material, or null if invalid
     */
    private Material parseMaterial(String name) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Checks if the plugin is enabled.
     * 
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the limit for a specific material in a specific world.
     * 
     * <p>
     * Priority order:
     * </p>
     * <ol>
     * <li>World-specific limit (if configured)</li>
     * <li>Global material limit (if configured)</li>
     * <li>Default limit</li>
     * </ol>
     * 
     * @param world    The world name
     * @param material The material to check
     * @return The limit (-1 for unlimited)
     */
    public int getLimit(String world, Material material) {
        // Check world-specific limits first
        Map<Material, Integer> worldSpecificLimits = worldLimits.get(world);
        if (worldSpecificLimits != null && worldSpecificLimits.containsKey(material)) {
            return worldSpecificLimits.get(material);
        }

        // Fall back to global limit
        return blockLimits.getOrDefault(material, defaultLimit);
    }

    /**
     * Gets the limit for a specific material (global limit).
     * 
     * <p>
     * Returns the default limit if the material is not explicitly configured.
     * </p>
     * 
     * @param material The material to check
     * @return The limit (-1 for unlimited)
     * @deprecated Use {@link #getLimit(String, Material)} for world-aware limits
     */
    @Deprecated
    public int getLimit(Material material) {
        return blockLimits.getOrDefault(material, defaultLimit);
    }

    /**
     * Checks if a material has a configured limit.
     * 
     * <p>
     * Returns false if the material uses the default limit.
     * </p>
     * 
     * @param material The material to check
     * @return true if explicitly limited
     */
    public boolean isLimited(Material material) {
        return blockLimits.containsKey(material) || defaultLimit != -1;
    }

    /**
     * Gets all configured block limits.
     * 
     * @return Unmodifiable map of material limits
     */
    public Map<Material, Integer> getAllLimits() {
        return Map.copyOf(blockLimits);
    }

    /**
     * Checks if sounds are enabled.
     * 
     * @return true if sounds should play
     */
    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    /**
     * Gets the sound type.
     * 
     * @return The sound type name
     */
    public String getSoundType() {
        return soundType;
    }

    /**
     * Gets the sound volume.
     * 
     * @return The volume
     */
    public float getSoundVolume() {
        return soundVolume;
    }

    /**
     * Gets the sound pitch.
     * 
     * @return The pitch
     */
    public float getSoundPitch() {
        return soundPitch;
    }

    /**
     * Checks if action bar messages are enabled.
     * 
     * @return true if action bar should be shown
     */
    public boolean isActionBarEnabled() {
        return actionBarEnabled;
    }

    /**
     * Checks if chat messages are enabled (Issue #4).
     * 
     * @return true if chat messages should be sent
     */
    public boolean isChatMessageEnabled() {
        return chatMessageEnabled;
    }

    /**
     * Checks if placement counter is enabled (Issue #5).
     * 
     * @return true if placement counter should be shown
     */
    public boolean isPlacementCounterEnabled() {
        return placementCounterEnabled;
    }

    /**
     * Checks if piston protection is enabled.
     * 
     * @return true if pistons should be blocked from pushing across chunks
     */
    public boolean isPistonProtectionEnabled() {
        return pistonProtectionEnabled;
    }

    /**
     * Gets the bypass permission node.
     * 
     * @return The permission string
     */
    public String getBypassPermission() {
        return bypassPermission;
    }

    /**
     * Gets the reload permission node.
     * 
     * @return The permission string
     */
    public String getReloadPermission() {
        return reloadPermission;
    }
}
