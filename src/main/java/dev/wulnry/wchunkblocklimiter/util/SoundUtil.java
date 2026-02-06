package dev.wulnry.wchunkblocklimiter.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

/**
 * Utility class for centralized sound playback.
 * 
 * <p>
 * Handles sound validation, fallback mechanisms, and consistent
 * playback across the plugin.
 * </p>
 * 
 * @author wulnrydev
 * @version 1.0.0
 */
public final class SoundUtil {

    private static final Sound DEFAULT_SOUND = Sound.BLOCK_NOTE_BLOCK_PLING;
    private static final float DEFAULT_VOLUME = 1.0f;
    private static final float DEFAULT_PITCH = 0.5f;

    // Private constructor to prevent instantiation
    private SoundUtil() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    /**
     * Plays a sound to a player with specified parameters.
     * 
     * <p>
     * If the sound name is invalid, falls back to the default sound
     * and logs a warning.
     * </p>
     * 
     * @param player    The player to play the sound to
     * @param soundName The sound name (Bukkit Sound enum value)
     * @param volume    The volume (typically 0.0-1.0, can exceed)
     * @param pitch     The pitch (0.5-2.0 range)
     * @param logger    Logger for error reporting
     */
    public static void playSound(Player player, String soundName, float volume, float pitch, Logger logger) {
        if (player == null || soundName == null) {
            return;
        }

        Sound sound = parseSound(soundName, logger);
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    /**
     * Parses a sound name into a Sound enum.
     * 
     * <p>
     * Falls back to default sound if parsing fails.
     * </p>
     * 
     * @param soundName The sound name to parse
     * @param logger    Logger for error reporting
     * @return The parsed Sound, or default sound if invalid
     */
    public static Sound parseSound(String soundName, Logger logger) {
        try {
            return Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            if (logger != null) {
                logger.warning(String.format(
                        "Invalid sound name '%s' in config. Falling back to %s",
                        soundName, DEFAULT_SOUND));
            }
            return DEFAULT_SOUND;
        }
    }

    /**
     * Gets the default sound used as fallback.
     * 
     * @return The default sound
     */
    public static Sound getDefaultSound() {
        return DEFAULT_SOUND;
    }

    /**
     * Gets the default volume.
     * 
     * @return The default volume
     */
    public static float getDefaultVolume() {
        return DEFAULT_VOLUME;
    }

    /**
     * Gets the default pitch.
     * 
     * @return The default pitch
     */
    public static float getDefaultPitch() {
        return DEFAULT_PITCH;
    }
}
