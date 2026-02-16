package com.hyperfactions.manager;

import com.hyperfactions.Permissions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.util.Logger;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.util.TimeUtil;
import com.hypixel.hytale.server.core.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Manages faction home teleportation with warmup and cooldown.
 *
 * Threading note: Teleport execution must happen on the world thread.
 * This manager stores pending teleports and their destinations, but the
 * actual teleport is executed by TerritoryTickingSystem which runs on
 * the correct thread.
 */
public class TeleportManager {

    private final FactionManager factionManager;

    // Pending teleports: player UUID -> PendingTeleport
    private final Map<UUID, PendingTeleport> pendingTeleports = new ConcurrentHashMap<>();

    // Cooldowns: player UUID -> cooldown end time
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    /**
     * Represents a pending teleport with destination information.
     * The teleport will be executed by the ticking system when warmup completes.
     */
    public static class PendingTeleport {
        private final UUID playerUuid;
        private final UUID factionId;
        private final StartLocation startLocation;
        private final TeleportDestination destination;
        private final long executeAt;  // System time when teleport should execute
        private final Supplier<Boolean> isTagged;  // Combat tag checker
        private int lastAnnouncedSecond = -1;  // Track countdown announcements

        public PendingTeleport(UUID playerUuid, UUID factionId, StartLocation startLocation,
                               TeleportDestination destination, long executeAt, Supplier<Boolean> isTagged) {
            this.playerUuid = playerUuid;
            this.factionId = factionId;
            this.startLocation = startLocation;
            this.destination = destination;
            this.executeAt = executeAt;
            this.isTagged = isTagged;
        }

        public UUID playerUuid() { return playerUuid; }
        public UUID factionId() { return factionId; }
        public StartLocation startLocation() { return startLocation; }
        public TeleportDestination destination() { return destination; }
        public long executeAt() { return executeAt; }
        public Supplier<Boolean> isTagged() { return isTagged; }

        /**
         * Checks if the warmup has completed and teleport is ready.
         */
        public boolean isReady() {
            return System.currentTimeMillis() >= executeAt;
        }

        /**
         * Gets remaining seconds until teleport.
         */
        public int getRemainingSeconds() {
            long remaining = executeAt - System.currentTimeMillis();
            return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
        }

        /**
         * Checks if countdown should be announced and updates tracking.
         * Returns the second to announce, or -1 if no announcement needed.
         *
         * Countdown intervals:
         * - Above 30s: announce at 30
         * - 15-30s: announce at 15
         * - 10-15s: announce at 10
         * - Below 10s: announce every second (9, 8, 7, ... 1)
         */
        public int checkCountdown() {
            int remaining = getRemainingSeconds();

            // Already announced this second or teleport ready
            if (remaining <= 0 || remaining == lastAnnouncedSecond) {
                return -1;
            }

            // Determine if we should announce
            boolean shouldAnnounce = false;
            if (remaining <= 10) {
                // Announce every second at 10 and below
                shouldAnnounce = true;
            } else if (remaining == 15 || remaining == 30) {
                // Announce at 15 and 30
                shouldAnnounce = true;
            }

            if (shouldAnnounce) {
                lastAnnouncedSecond = remaining;
                return remaining;
            }

            return -1;
        }
    }

    /**
     * Represents a starting location for movement checking.
     */
    public record StartLocation(String world, double x, double y, double z) {}

    /**
     * Represents a teleport destination.
     */
    public record TeleportDestination(
        String world,
        double x, double y, double z,
        float pitch, float yaw
    ) {}

    /**
     * Result of a teleport attempt.
     */
    public enum TeleportResult {
        SUCCESS_INSTANT,     // Teleport completed immediately (no warmup)
        SUCCESS_WARMUP,      // Warmup scheduled, teleport pending
        NO_PERMISSION,
        NO_HOME,
        NOT_IN_FACTION,
        ON_COOLDOWN,
        COMBAT_TAGGED,
        CANCELLED_MOVED,
        CANCELLED_DAMAGE,
        CANCELLED_MANUAL,
        WORLD_NOT_FOUND
    }

    public TeleportManager(@NotNull FactionManager factionManager) {
        this.factionManager = factionManager;
    }

    // === Queries ===

    /**
     * Checks if a player has a pending teleport.
     *
     * @param playerUuid the player's UUID
     * @return true if pending
     */
    public boolean hasPending(@NotNull UUID playerUuid) {
        return pendingTeleports.containsKey(playerUuid);
    }

    /**
     * Gets the pending teleport for a player.
     *
     * @param playerUuid the player's UUID
     * @return the pending teleport, or null
     */
    @Nullable
    public PendingTeleport getPending(@NotNull UUID playerUuid) {
        return pendingTeleports.get(playerUuid);
    }

    /**
     * Checks if a player is on cooldown.
     *
     * @param playerUuid the player's UUID
     * @return true if on cooldown
     */
    public boolean isOnCooldown(@NotNull UUID playerUuid) {
        Long endTime = cooldowns.get(playerUuid);
        if (endTime == null) {
            return false;
        }
        if (System.currentTimeMillis() >= endTime) {
            cooldowns.remove(playerUuid);
            return false;
        }
        return true;
    }

    /**
     * Gets the remaining cooldown time in seconds.
     *
     * @param playerUuid the player's UUID
     * @return remaining seconds, 0 if not on cooldown
     */
    public int getCooldownRemaining(@NotNull UUID playerUuid) {
        Long endTime = cooldowns.get(playerUuid);
        if (endTime == null) {
            return 0;
        }
        long remaining = endTime - System.currentTimeMillis();
        if (remaining <= 0) {
            cooldowns.remove(playerUuid);
            return 0;
        }
        return (int) Math.ceil(remaining / 1000.0);
    }

    // === Operations ===

    /**
     * Initiates a teleport to faction home.
     * For instant teleport (warmup=0), executes immediately via doTeleport.
     * For warmup teleport, stores destination and returns SUCCESS_WARMUP.
     * The actual warmup teleport will be executed by TerritoryTickingSystem.
     *
     * @param playerUuid    the player's UUID
     * @param startLocation the player's starting location
     * @param doTeleport    function to perform instant teleport (warmup=0 only)
     * @param sendMessage   function to send a Message to the player
     * @param isTagged      supplier to check if player is combat tagged
     * @return the initial result
     */
    public TeleportResult teleportToHome(
        @NotNull UUID playerUuid,
        @NotNull StartLocation startLocation,
        @NotNull TeleportExecutor doTeleport,
        @NotNull Consumer<Message> sendMessage,
        @NotNull Supplier<Boolean> isTagged
    ) {
        // Check permission first
        if (!PermissionManager.get().hasPermission(playerUuid, Permissions.HOME)) {
            return TeleportResult.NO_PERMISSION;
        }

        // Get player's faction
        Faction faction = factionManager.getPlayerFaction(playerUuid);
        if (faction == null) {
            return TeleportResult.NOT_IN_FACTION;
        }

        // Check if faction has home
        if (!faction.hasHome()) {
            return TeleportResult.NO_HOME;
        }

        Faction.FactionHome home = faction.home();

        // Check combat tag
        if (isTagged.get()) {
            return TeleportResult.COMBAT_TAGGED;
        }

        // Check cooldown
        if (!PermissionManager.get().hasPermission(playerUuid, Permissions.BYPASS_COOLDOWN)) {
            if (isOnCooldown(playerUuid)) {
                int remaining = getCooldownRemaining(playerUuid);
                sendMessage.accept(MessageUtil.error("You must wait " +
                    TimeUtil.formatDurationSeconds(remaining) + " before teleporting again."));
                return TeleportResult.ON_COOLDOWN;
            }
        }

        // Cancel any existing pending teleport
        removePending(playerUuid);

        // Check if warmup is needed
        int warmup = getWarmupSeconds(playerUuid);
        if (warmup <= 0) {
            // Instant teleport - execute immediately (we're on the right thread)
            TeleportResult result = doTeleport.execute(faction);
            if (result == TeleportResult.SUCCESS_INSTANT) {
                applyCooldown(playerUuid);
            }
            return result;
        }

        // Create destination from faction home
        TeleportDestination destination = new TeleportDestination(
            home.world(), home.x(), home.y(), home.z(), home.pitch(), home.yaw()
        );

        // Store pending teleport - will be executed by TerritoryTickingSystem
        long executeAt = System.currentTimeMillis() + (warmup * 1000L);
        PendingTeleport pending = new PendingTeleport(
            playerUuid, faction.id(), startLocation, destination, executeAt, isTagged
        );
        pendingTeleports.put(playerUuid, pending);

        // Send warmup message
        sendMessage.accept(MessageUtil.info("Teleporting to faction home in " + warmup + " seconds...", MessageUtil.COLOR_YELLOW));

        Logger.debug("Scheduled teleport for %s, will execute at %d", playerUuid, executeAt);
        return TeleportResult.SUCCESS_WARMUP;
    }

    /**
     * Schedules a generic teleport with warmup (for /f stuck, etc.).
     * The teleport will be executed by TerritoryTickingSystem when warmup completes.
     *
     * @param playerUuid    the player's UUID
     * @param startLocation the player's starting location (for movement cancellation)
     * @param destination   the teleport destination
     * @param warmupSeconds the warmup time in seconds
     * @param isTagged      supplier to check if player is combat tagged
     */
    public void scheduleTeleport(
        @NotNull UUID playerUuid,
        @NotNull StartLocation startLocation,
        @NotNull TeleportDestination destination,
        int warmupSeconds,
        @NotNull Supplier<Boolean> isTagged
    ) {
        // Cancel any existing pending teleport
        removePending(playerUuid);

        // Store pending teleport - will be executed by TerritoryTickingSystem
        long executeAt = System.currentTimeMillis() + (warmupSeconds * 1000L);
        PendingTeleport pending = new PendingTeleport(
            playerUuid, null, startLocation, destination, executeAt, isTagged
        );
        pendingTeleports.put(playerUuid, pending);

        Logger.debug("Scheduled generic teleport for %s, will execute at %d", playerUuid, executeAt);
    }

    /**
     * Checks if a pending teleport is ready and returns it for execution.
     * Called by TerritoryTickingSystem on the world thread.
     *
     * @param playerUuid the player's UUID
     * @param sendMessage function to send messages
     * @return the pending teleport if ready, null otherwise
     */
    @Nullable
    public PendingTeleport checkReady(@NotNull UUID playerUuid, @NotNull Consumer<Message> sendMessage) {
        PendingTeleport pending = pendingTeleports.get(playerUuid);
        if (pending == null || !pending.isReady()) {
            return null;
        }

        // Remove from pending
        pendingTeleports.remove(playerUuid);

        // Check combat tag
        if (pending.isTagged().get()) {
            sendMessage.accept(MessageUtil.error("Teleportation cancelled - you are in combat!"));
            return null;
        }

        return pending;
    }

    /**
     * Called after successful teleport execution to apply cooldown and send message.
     *
     * @param playerUuid the player's UUID
     * @param sendMessage function to send messages
     */
    public void onTeleportSuccess(@NotNull UUID playerUuid, @NotNull Consumer<Message> sendMessage) {
        applyCooldown(playerUuid);
        sendMessage.accept(MessageUtil.success("Teleported to faction home!"));
    }

    /**
     * Called after failed teleport execution.
     *
     * @param result the failure result
     * @param sendMessage function to send messages
     */
    public void onTeleportFailed(@NotNull TeleportResult result, @NotNull Consumer<Message> sendMessage) {
        switch (result) {
            case NO_HOME -> sendMessage.accept(MessageUtil.error("Your faction has no home set."));
            case WORLD_NOT_FOUND -> sendMessage.accept(MessageUtil.error("World not found."));
            default -> sendMessage.accept(MessageUtil.error("Teleportation failed."));
        }
    }

    /**
     * Sends a countdown message for the pending teleport.
     *
     * @param pending the pending teleport
     * @param sendMessage function to send messages
     */
    public void sendCountdownMessage(@NotNull PendingTeleport pending, @NotNull Consumer<Message> sendMessage) {
        int secondsToAnnounce = pending.checkCountdown();
        if (secondsToAnnounce > 0) {
            String timeText = secondsToAnnounce == 1 ? "1 second" : secondsToAnnounce + " seconds";
            sendMessage.accept(MessageUtil.info("Teleporting in " + timeText + "...", MessageUtil.COLOR_YELLOW));
        }
    }

    /**
     * Checks if a player moved and should cancel teleport.
     *
     * @param playerUuid the player's UUID
     * @param currentX   current X position
     * @param currentY   current Y position
     * @param currentZ   current Z position
     * @param sendMessage function to send a Message
     * @return true if teleport was cancelled
     */
    public boolean checkMovement(
        @NotNull UUID playerUuid,
        double currentX, double currentY, double currentZ,
        @NotNull Consumer<Message> sendMessage
    ) {
        if (!ConfigManager.get().isCancelOnMove()) {
            return false;
        }

        PendingTeleport pending = pendingTeleports.get(playerUuid);
        if (pending == null) {
            return false;
        }

        StartLocation start = pending.startLocation();
        double dx = currentX - start.x();
        double dy = currentY - start.y();
        double dz = currentZ - start.z();
        double distSq = dx * dx + dy * dy + dz * dz;

        if (distSq > 0.25) { // 0.5 blocks
            removePending(playerUuid);
            sendMessage.accept(MessageUtil.error("Teleportation cancelled - you moved!"));
            return true;
        }

        return false;
    }

    /**
     * Cancels teleport due to damage.
     *
     * @param playerUuid  the player's UUID
     * @param sendMessage function to send a Message
     * @return true if teleport was cancelled
     */
    public boolean cancelOnDamage(
        @NotNull UUID playerUuid,
        @NotNull Consumer<Message> sendMessage
    ) {
        if (!ConfigManager.get().isCancelOnDamage()) {
            return false;
        }

        if (pendingTeleports.containsKey(playerUuid)) {
            removePending(playerUuid);
            sendMessage.accept(MessageUtil.error("Teleportation cancelled - you took damage!"));
            return true;
        }

        return false;
    }

    /**
     * Removes a pending teleport without notification.
     *
     * @param playerUuid the player's UUID
     */
    public void removePending(@NotNull UUID playerUuid) {
        PendingTeleport pending = pendingTeleports.remove(playerUuid);
        if (pending != null) {
            Logger.debug("Removed pending teleport for %s", playerUuid);
        }
    }

    /**
     * Cancels a pending teleport (legacy method for compatibility).
     *
     * @param playerUuid the player's UUID
     * @param cancelTask ignored (no longer using timers for execution)
     */
    public void cancelPending(@NotNull UUID playerUuid, @NotNull Consumer<Integer> cancelTask) {
        removePending(playerUuid);
    }

    /**
     * Gets the warmup seconds for a player.
     *
     * @param playerUuid the player's UUID
     * @return warmup seconds, 0 if bypassed
     */
    private int getWarmupSeconds(@NotNull UUID playerUuid) {
        if (PermissionManager.get().hasPermission(playerUuid, Permissions.BYPASS_WARMUP)) {
            return 0;
        }
        return ConfigManager.get().getWarmupSeconds();
    }

    /**
     * Applies cooldown to a player.
     *
     * @param playerUuid the player's UUID
     */
    private void applyCooldown(@NotNull UUID playerUuid) {
        if (PermissionManager.get().hasPermission(playerUuid, Permissions.BYPASS_COOLDOWN)) {
            return;
        }
        int cooldownSeconds = ConfigManager.get().getCooldownSeconds();
        if (cooldownSeconds > 0) {
            cooldowns.put(playerUuid, System.currentTimeMillis() + (cooldownSeconds * 1000L));
        }
    }

    /**
     * Functional interface for executing teleports.
     */
    @FunctionalInterface
    public interface TeleportExecutor {
        TeleportResult execute(Faction faction);
    }
}
