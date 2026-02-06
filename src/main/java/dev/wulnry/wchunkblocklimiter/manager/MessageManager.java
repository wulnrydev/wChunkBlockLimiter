package dev.wulnry.wchunkblocklimiter.manager;

import dev.wulnry.wchunkblocklimiter.wChunkBlockLimiter;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages message loading, formatting, and delivery.
 * 
 * <p>
 * Handles messages.yml parsing, color code translation, placeholder
 * replacement, and sending messages to players via chat or action bar.
 * </p>
 * 
 * <p>
 * <b>Supported Placeholders:</b>
 * </p>
 * <ul>
 * <li>%block% - Block type (Material name)</li>
 * <li>%limit% - Maximum allowed count</li>
 * <li>%chunk_x% - Chunk X coordinate</li>
 * <li>%chunk_z% - Chunk Z coordinate</li>
 * <li>%world% - World name</li>
 * <li>%player% - Player name</li>
 * </ul>
 * 
 * @author wulnrydev
 * @version 1.0.0
 */
public class MessageManager {

    private final wChunkBlockLimiter plugin;
    private FileConfiguration messages;
    private File messagesFile;

    /**
     * Constructs a new MessageManager.
     * 
     * @param plugin The main plugin instance
     */
    public MessageManager(wChunkBlockLimiter plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    /**
     * Loads (or reloads) messages from messages.yml.
     * 
     * <p>
     * Creates the file from the plugin resources if it doesn't exist.
     * </p>
     */
    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        // Create messages.yml if it doesn't exist
        if (!messagesFile.exists()) {
            try {
                messagesFile.getParentFile().mkdirs();
                try (InputStream in = plugin.getResource("messages.yml")) {
                    if (in != null) {
                        Files.copy(in, messagesFile.toPath());
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create messages.yml", e);
            }
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);
        plugin.getLogger().info("Messages loaded from messages.yml");
    }

    /**
     * Gets a raw message from messages.yml.
     * 
     * @param path The config path (e.g., "limit-reached.action-bar")
     * @return The raw message, or empty string if not found
     */
    public String getRawMessage(String path) {
        return messages.getString(path, "");
    }

    /**
     * Gets a formatted message with color codes translated.
     * 
     * @param path The config path
     * @return The formatted message
     */
    public String getMessage(String path) {
        String message = getRawMessage(path);
        return translateColors(message);
    }

    /**
     * Gets a formatted message with placeholders replaced.
     * 
     * @param path         The config path
     * @param placeholders Map of placeholder → value
     * @return The formatted message with placeholders replaced
     */
    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        return replacePlaceholders(message, placeholders);
    }

    /**
     * Translates color codes from & to §.
     * 
     * @param text The text to translate
     * @return The translated text
     */
    private String translateColors(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Replaces placeholders in a message.
     * 
     * @param message      The message with placeholders
     * @param placeholders Map of placeholder → value
     * @return The message with placeholders replaced
     */
    private String replacePlaceholders(String message, Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) {
            return message;
        }

        String result = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Sends a chat message to a player.
     * 
     * @param player The player
     * @param path   The message path
     */
    public void sendMessage(Player player, String path) {
        String message = getMessage(path);
        if (!message.isEmpty()) {
            player.sendMessage(message);
        }
    }

    /**
     * Sends a chat message to a player with placeholders.
     * 
     * @param player       The player
     * @param path         The message path
     * @param placeholders The placeholders
     */
    public void sendMessage(Player player, String path, Map<String, String> placeholders) {
        String message = getMessage(path, placeholders);
        if (!message.isEmpty()) {
            player.sendMessage(message);
        }
    }

    /**
     * Sends an action bar message to a player.
     * 
     * <p>
     * Action bar messages appear above the hotbar.
     * </p>
     * 
     * @param player The player
     * @param path   The message path
     */
    public void sendActionBar(Player player, String path) {
        String message = getMessage(path);
        if (!message.isEmpty()) {
            sendActionBarRaw(player, message);
        }
    }

    /**
     * Sends an action bar message to a player with placeholders.
     * 
     * @param player       The player
     * @param path         The message path
     * @param placeholders The placeholders
     */
    public void sendActionBar(Player player, String path, Map<String, String> placeholders) {
        String message = getMessage(path, placeholders);
        if (!message.isEmpty()) {
            sendActionBarRaw(player, message);
        }
    }

    /**
     * Sends a raw action bar message (already formatted).
     * 
     * @param player  The player
     * @param message The formatted message
     */
    private void sendActionBarRaw(Player player, String message) {
        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(message));
    }

    /**
     * Gets the configured prefix.
     * 
     * @return The formatted prefix
     */
    public String getPrefix() {
        return getMessage("prefix");
    }
}
