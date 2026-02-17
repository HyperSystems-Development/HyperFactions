package com.hyperfactions.command.util;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.data.Faction;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.Logger;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Shared utility methods for faction commands.
 * <p>
 * Color constants and message builders delegate to {@link MessageUtil}.
 */
public final class CommandUtil {

    // Color constants — delegate to MessageUtil
    public static final String COLOR_CYAN = MessageUtil.COLOR_CYAN;
    public static final String COLOR_GREEN = MessageUtil.COLOR_GREEN;
    public static final String COLOR_RED = MessageUtil.COLOR_RED;
    public static final String COLOR_YELLOW = MessageUtil.COLOR_YELLOW;
    public static final String COLOR_GRAY = MessageUtil.COLOR_GRAY;
    public static final String COLOR_WHITE = MessageUtil.COLOR_WHITE;

    private CommandUtil() {}

    /**
     * Creates the standard HyperFactions message prefix using configured values.
     * Format: [PrefixText] with configurable colors for brackets and text.
     *
     * @return the prefix message
     */
    @NotNull
    public static Message prefix() {
        return MessageUtil.prefix();
    }

    /**
     * Creates a colored message.
     *
     * @param text the text content
     * @param color the hex color code
     * @return the colored message
     */
    @NotNull
    public static Message msg(@NotNull String text, @NotNull String color) {
        return MessageUtil.text(text, color);
    }

    /**
     * Checks if a player has a specific permission.
     *
     * @param player the player to check
     * @param permission the permission node
     * @return true if the player has the permission
     */
    public static boolean hasPermission(@NotNull PlayerRef player, @NotNull String permission) {
        return PermissionManager.get().hasPermission(player.getUuid(), permission);
    }

    /**
     * Finds an online player by name (case-insensitive).
     *
     * @param plugin the plugin instance
     * @param name the player name to search for
     * @return the PlayerRef if found online, null otherwise
     */
    @Nullable
    public static PlayerRef findOnlinePlayer(@NotNull HyperFactionsPlugin plugin, @NotNull String name) {
        var tracked = plugin.getTrackedPlayers();
        Logger.debug("findOnlinePlayer: searching for '%s' among %d tracked players", name, tracked.size());
        for (PlayerRef player : tracked.values()) {
            String username = player.getUsername();
            if (username == null) {
                Logger.debug("findOnlinePlayer: tracked player %s has NULL username", player.getUuid());
                continue;
            }
            if (username.equalsIgnoreCase(name)) {
                Logger.debug("findOnlinePlayer: matched '%s' -> %s (%s)", name, username, player.getUuid());
                return player;
            }
        }
        Logger.debug("findOnlinePlayer: no match for '%s'", name);
        return null;
    }

    /**
     * Broadcasts a message to all online members of a faction.
     *
     * @param hyperFactions the HyperFactions instance
     * @param plugin the plugin instance
     * @param factionId the faction UUID
     * @param message the message to broadcast
     */
    public static void broadcastToFaction(@NotNull HyperFactions hyperFactions,
                                          @NotNull HyperFactionsPlugin plugin,
                                          @NotNull UUID factionId,
                                          @NotNull Message message) {
        Faction faction = hyperFactions.getFactionManager().getFaction(factionId);
        if (faction == null) return;

        for (UUID memberUuid : faction.members().keySet()) {
            PlayerRef member = plugin.getTrackedPlayer(memberUuid);
            if (member != null) {
                member.sendMessage(message);
            }
        }
    }
}
