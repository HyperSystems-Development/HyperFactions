package com.hyperfactions.manager;

import com.hyperfactions.Permissions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.ChunkKey;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionClaim;
import com.hyperfactions.data.FactionLog;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.integration.protection.OrbisGuardIntegration;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Manages territory claims with O(1) chunk lookups.
 */
public class ClaimManager {

    private final FactionManager factionManager;
    private final PowerManager powerManager;

    // Zone manager for checking safezones/warzones (injected after construction)
    @Nullable
    private ZoneManager zoneManager;

    // Index: ChunkKey -> faction ID for fast lookups
    private final Map<ChunkKey, UUID> claimIndex = new ConcurrentHashMap<>();

    // Reverse index: faction ID -> Set<ChunkKey> for O(1) getFactionClaims()
    private final Map<UUID, Set<ChunkKey>> factionClaimsIndex = new ConcurrentHashMap<>();

    // Callback for when claims change (used to refresh world map)
    @Nullable
    private Runnable onClaimChangeCallback;

    // Chunk-specific callback for optimized world map refresh
    @Nullable
    private ChunkChangeCallback onChunkChangeCallback;

    // GUI-specific chunk change callback for real-time GUI map refresh
    @Nullable
    private ChunkChangeCallback onGuiChunkChangeCallback;

    // Callback for notifying faction members (used for overclaim alerts)
    @Nullable
    private FactionNotificationCallback notificationCallback;

    // Callback for overclaim announcements
    @Nullable
    private BiConsumer<String, String> onOverclaimCallback;

    /**
     * Functional interface for sending notifications to faction members.
     */
    @FunctionalInterface
    public interface FactionNotificationCallback {
        void notifyFaction(UUID factionId, String message, String hexColor);
    }

    /**
     * Functional interface for chunk-specific change notifications.
     * Used for optimized world map refresh.
     */
    @FunctionalInterface
    public interface ChunkChangeCallback {
        void onChunkChange(String worldName, int chunkX, int chunkZ);
    }

    public ClaimManager(@NotNull FactionManager factionManager, @NotNull PowerManager powerManager) {
        this.factionManager = factionManager;
        this.powerManager = powerManager;
    }

    /**
     * Sets the zone manager for checking safezones/warzones.
     * This is injected after construction to avoid circular dependencies.
     *
     * @param zoneManager the zone manager
     */
    public void setZoneManager(@Nullable ZoneManager zoneManager) {
        this.zoneManager = zoneManager;
    }

    /**
     * Sets a callback for notifying faction members.
     * Used to alert defenders when territory is overclaimed.
     *
     * @param callback the notification callback
     */
    public void setNotificationCallback(@Nullable FactionNotificationCallback callback) {
        this.notificationCallback = callback;
    }

    /**
     * Sets a callback for when a faction overclaims territory from another.
     * Params: attackerFactionName, defenderFactionName
     */
    public void setOnOverclaimCallback(@Nullable BiConsumer<String, String> callback) {
        this.onOverclaimCallback = callback;
    }

    /**
     * Notifies all online members of a faction.
     *
     * @param factionId the faction ID
     * @param message   the message text
     * @param hexColor  the message color
     */
    private void notifyFactionMembers(@NotNull UUID factionId, @NotNull String message, @NotNull String hexColor) {
        if (notificationCallback != null) {
            try {
                notificationCallback.notifyFaction(factionId, message, hexColor);
            } catch (Exception e) {
                Logger.warn("Failed to notify faction members: %s", e.getMessage());
            }
        }
    }

    /**
     * Sets a callback to be invoked when claims change.
     * Used to trigger world map refresh.
     *
     * @param callback the callback to run on claim changes
     * @deprecated Use {@link #setOnChunkChangeCallback} for chunk-specific updates
     */
    @Deprecated
    public void setOnClaimChangeCallback(@Nullable Runnable callback) {
        this.onClaimChangeCallback = callback;
    }

    /**
     * Sets a callback for chunk-specific change notifications.
     * Preferred over setOnClaimChangeCallback for optimized world map refresh.
     *
     * @param callback the callback to run on chunk changes
     */
    public void setOnChunkChangeCallback(@Nullable ChunkChangeCallback callback) {
        this.onChunkChangeCallback = callback;
    }

    /**
     * Sets a GUI-specific callback for chunk change notifications.
     * Used for real-time GUI map refresh, separate from world map callback.
     *
     * @param callback the callback to run on chunk changes
     */
    public void setOnGuiChunkChangeCallback(@Nullable ChunkChangeCallback callback) {
        this.onGuiChunkChangeCallback = callback;
    }

    /**
     * Notifies that claims have changed (triggers world map refresh).
     * @deprecated Use {@link #notifyChunkChange(String, int, int)} for chunk-specific updates
     */
    @Deprecated
    private void notifyClaimChange() {
        Logger.debugClaim("Claim change notification triggered (legacy)");
        if (onClaimChangeCallback != null) {
            try {
                onClaimChangeCallback.run();
            } catch (Exception e) {
                Logger.warn("Error in claim change callback: %s", e.getMessage());
            }
        }
    }

    /**
     * Notifies that a specific chunk claim has changed.
     * This enables optimized world map refresh by targeting only the affected chunk.
     *
     * @param worldName the world name
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     */
    private void notifyChunkChange(@NotNull String worldName, int chunkX, int chunkZ) {
        Logger.debugClaim("Chunk change notification: world=%s, chunk=(%d,%d)", worldName, chunkX, chunkZ);

        // Try chunk-specific callback first (preferred for performance)
        if (onChunkChangeCallback != null) {
            try {
                onChunkChangeCallback.onChunkChange(worldName, chunkX, chunkZ);
            } catch (Exception e) {
                Logger.warn("Error in chunk change callback: %s", e.getMessage());
            }
        }
        // Fall back to legacy callback if no chunk-specific callback set
        else if (onClaimChangeCallback != null) {
            try {
                onClaimChangeCallback.run();
            } catch (Exception e) {
                Logger.warn("Error in claim change callback: %s", e.getMessage());
            }
        }

        // GUI map refresh callback (separate from world map)
        if (onGuiChunkChangeCallback != null) {
            try {
                onGuiChunkChangeCallback.onChunkChange(worldName, chunkX, chunkZ);
            } catch (Exception e) {
                Logger.warn("Error in GUI chunk change callback: %s", e.getMessage());
            }
        }
    }

    /**
     * Builds the claim index from all factions.
     * Call after FactionManager.loadAll()
     */
    public void buildIndex() {
        claimIndex.clear();
        factionClaimsIndex.clear();

        for (Faction faction : factionManager.getAllFactions()) {
            Set<ChunkKey> factionClaims = ConcurrentHashMap.newKeySet();
            for (FactionClaim claim : faction.claims()) {
                ChunkKey key = claim.toChunkKey();
                claimIndex.put(key, faction.id());
                factionClaims.add(key);
            }
            if (!factionClaims.isEmpty()) {
                factionClaimsIndex.put(faction.id(), factionClaims);
            }
        }

        Logger.info("Built claim index with %d claims for %d factions", claimIndex.size(), factionClaimsIndex.size());
    }

    /**
     * Result of a claim operation.
     */
    public enum ClaimResult {
        SUCCESS,
        NO_PERMISSION,
        NOT_IN_FACTION,
        NOT_OFFICER,
        ALREADY_CLAIMED_SELF,
        ALREADY_CLAIMED_OTHER,
        ALREADY_CLAIMED_ALLY,
        ALREADY_CLAIMED_ENEMY,
        NOT_ADJACENT,
        MAX_CLAIMS_REACHED,
        INSUFFICIENT_POWER,
        WORLD_NOT_ALLOWED,
        CHUNK_NOT_CLAIMED,
        CANNOT_UNCLAIM_HOME,
        NOT_YOUR_CLAIM,
        OVERCLAIM_NOT_ALLOWED,
        TARGET_HAS_POWER,
        ORBISGUARD_PROTECTED,
        ZONE_PROTECTED,
        WOULD_DISCONNECT
    }

    // === Queries ===

    /**
     * Gets the faction ID that owns a chunk.
     *
     * @param world  the world name
     * @param chunkX the chunk X
     * @param chunkZ the chunk Z
     * @return the faction ID, or null if unclaimed
     */
    @Nullable
    public UUID getClaimOwner(@NotNull String world, int chunkX, int chunkZ) {
        return claimIndex.get(new ChunkKey(world, chunkX, chunkZ));
    }

    /**
     * Gets the faction ID that owns a chunk at world coordinates.
     *
     * @param world the world name
     * @param x     the world X coordinate
     * @param z     the world Z coordinate
     * @return the faction ID, or null if unclaimed
     */
    @Nullable
    public UUID getClaimOwnerAt(@NotNull String world, double x, double z) {
        return claimIndex.get(ChunkKey.fromWorldCoords(world, x, z));
    }

    /**
     * Checks if a chunk is claimed.
     *
     * @param world  the world name
     * @param chunkX the chunk X
     * @param chunkZ the chunk Z
     * @return true if claimed
     */
    public boolean isClaimed(@NotNull String world, int chunkX, int chunkZ) {
        return claimIndex.containsKey(new ChunkKey(world, chunkX, chunkZ));
    }

    /**
     * Checks if any adjacent chunk is owned by the given faction.
     *
     * @param world     the world name
     * @param chunkX    the chunk X
     * @param chunkZ    the chunk Z
     * @param factionId the faction ID
     * @return true if at least one adjacent chunk is owned by the faction
     */
    public boolean hasAdjacentClaim(@NotNull String world, int chunkX, int chunkZ, @NotNull UUID factionId) {
        ChunkKey key = new ChunkKey(world, chunkX, chunkZ);
        return factionId.equals(claimIndex.get(key.north())) ||
               factionId.equals(claimIndex.get(key.south())) ||
               factionId.equals(claimIndex.get(key.east())) ||
               factionId.equals(claimIndex.get(key.west()));
    }

    /**
     * Gets the total claim count.
     *
     * @return number of claimed chunks
     */
    public int getTotalClaimCount() {
        return claimIndex.size();
    }

    // === Operations ===

    /**
     * Claims a chunk for a faction.
     *
     * @param playerUuid the player claiming (must be in faction)
     * @param world      the world name
     * @param chunkX     the chunk X
     * @param chunkZ     the chunk Z
     * @return the result
     */
    public ClaimResult claim(@NotNull UUID playerUuid, @NotNull String world, int chunkX, int chunkZ) {
        // Check permission first
        if (!PermissionManager.get().hasPermission(playerUuid, Permissions.CLAIM)) {
            return ClaimResult.NO_PERMISSION;
        }

        // Get player's faction
        Faction faction = factionManager.getPlayerFaction(playerUuid);
        if (faction == null) {
            return ClaimResult.NOT_IN_FACTION;
        }

        // Check permission
        var member = faction.getMember(playerUuid);
        if (member == null || !member.isOfficerOrHigher()) {
            return ClaimResult.NOT_OFFICER;
        }

        // Check world
        if (!ConfigManager.get().isWorldAllowed(world)) {
            return ClaimResult.WORLD_NOT_ALLOWED;
        }

        // Check OrbisGuard protection (if OrbisGuard is installed)
        if (OrbisGuardIntegration.isChunkProtected(world, chunkX, chunkZ)) {
            Logger.debugClaim("Claim blocked: chunk=%s/%d/%d is protected by OrbisGuard", world, chunkX, chunkZ);
            return ClaimResult.ORBISGUARD_PROTECTED;
        }

        // Check zone protection (safezones and warzones)
        if (zoneManager != null && zoneManager.getZone(world, chunkX, chunkZ) != null) {
            Logger.debugClaim("Claim blocked: chunk=%s/%d/%d is in a safezone or warzone", world, chunkX, chunkZ);
            return ClaimResult.ZONE_PROTECTED;
        }

        ChunkKey key = new ChunkKey(world, chunkX, chunkZ);

        // Check if already claimed
        UUID existingOwner = claimIndex.get(key);
        if (existingOwner != null) {
            if (existingOwner.equals(faction.id())) {
                return ClaimResult.ALREADY_CLAIMED_SELF;
            }
            return ClaimResult.ALREADY_CLAIMED_OTHER;
        }

        // Check max claims
        double factionPower = powerManager.getFactionPower(faction.id());
        int maxClaims = ConfigManager.get().calculateMaxClaims(factionPower);
        if (faction.getClaimCount() >= maxClaims) {
            return ClaimResult.MAX_CLAIMS_REACHED;
        }

        // Check adjacency if required
        ConfigManager config = ConfigManager.get();
        if (config.isOnlyAdjacent() && faction.getClaimCount() > 0) {
            if (!hasAdjacentClaim(world, chunkX, chunkZ, faction.id())) {
                return ClaimResult.NOT_ADJACENT;
            }
        }

        // Create claim
        FactionClaim claim = FactionClaim.create(world, chunkX, chunkZ, playerUuid);
        Faction updated = faction.withClaim(claim)
            .withLog(FactionLog.create(FactionLog.LogType.CLAIM,
                String.format("Claimed chunk at %d, %d in %s", chunkX, chunkZ, world), playerUuid));

        // Update indices and faction
        claimIndex.put(key, faction.id());
        factionClaimsIndex.computeIfAbsent(faction.id(), k -> ConcurrentHashMap.newKeySet()).add(key);
        factionManager.updateFaction(updated);

        Logger.debugClaim("Claim success: chunk=%s, faction=%s, player=%s, claimCount=%d/%d",
            key, faction.name(), playerUuid, updated.getClaimCount(), maxClaims);
        notifyChunkChange(world, chunkX, chunkZ);
        return ClaimResult.SUCCESS;
    }

    /**
     * Unclaims a chunk.
     *
     * @param playerUuid the player unclaiming
     * @param world      the world name
     * @param chunkX     the chunk X
     * @param chunkZ     the chunk Z
     * @return the result
     */
    public ClaimResult unclaim(@NotNull UUID playerUuid, @NotNull String world, int chunkX, int chunkZ) {
        // Check permission first
        if (!PermissionManager.get().hasPermission(playerUuid, Permissions.UNCLAIM)) {
            return ClaimResult.NO_PERMISSION;
        }

        Faction faction = factionManager.getPlayerFaction(playerUuid);
        if (faction == null) {
            return ClaimResult.NOT_IN_FACTION;
        }

        var member = faction.getMember(playerUuid);
        if (member == null || !member.isOfficerOrHigher()) {
            return ClaimResult.NOT_OFFICER;
        }

        ChunkKey key = new ChunkKey(world, chunkX, chunkZ);
        UUID owner = claimIndex.get(key);

        if (owner == null) {
            return ClaimResult.CHUNK_NOT_CLAIMED;
        }

        if (!owner.equals(faction.id())) {
            return ClaimResult.NOT_YOUR_CLAIM;
        }

        // Check if home is in this chunk
        if (faction.hasHome()) {
            var home = faction.home();
            int homeChunkX = ChunkUtil.toChunkCoord(home.x());
            int homeChunkZ = ChunkUtil.toChunkCoord(home.z());
            if (home.world().equals(world) && homeChunkX == chunkX && homeChunkZ == chunkZ) {
                return ClaimResult.CANNOT_UNCLAIM_HOME;
            }
        }

        // Check if unclaiming would disconnect territory
        if (ConfigManager.get().isPreventDisconnect()) {
            if (wouldDisconnectClaims(faction.id(), key)) {
                return ClaimResult.WOULD_DISCONNECT;
            }
        }

        // Remove claim
        Faction updated = faction.withoutClaimAt(world, chunkX, chunkZ)
            .withLog(FactionLog.create(FactionLog.LogType.UNCLAIM,
                String.format("Unclaimed chunk at %d, %d in %s", chunkX, chunkZ, world), playerUuid));

        claimIndex.remove(key);
        Set<ChunkKey> factionClaims = factionClaimsIndex.get(faction.id());
        if (factionClaims != null) {
            factionClaims.remove(key);
            if (factionClaims.isEmpty()) {
                factionClaimsIndex.remove(faction.id());
            }
        }
        factionManager.updateFaction(updated);

        Logger.debugClaim("Unclaim success: chunk=%s, faction=%s, player=%s",
            key, faction.name(), playerUuid);
        notifyChunkChange(world, chunkX, chunkZ);
        return ClaimResult.SUCCESS;
    }

    /**
     * Attempts to overclaim an enemy faction's chunk.
     *
     * @param playerUuid the player overclaiming
     * @param world      the world name
     * @param chunkX     the chunk X
     * @param chunkZ     the chunk Z
     * @return the result
     */
    public ClaimResult overclaim(@NotNull UUID playerUuid, @NotNull String world, int chunkX, int chunkZ) {
        // Check permission first
        if (!PermissionManager.get().hasPermission(playerUuid, Permissions.OVERCLAIM)) {
            return ClaimResult.NO_PERMISSION;
        }

        Faction attackerFaction = factionManager.getPlayerFaction(playerUuid);
        if (attackerFaction == null) {
            return ClaimResult.NOT_IN_FACTION;
        }

        var member = attackerFaction.getMember(playerUuid);
        if (member == null || !member.isOfficerOrHigher()) {
            return ClaimResult.NOT_OFFICER;
        }

        ChunkKey key = new ChunkKey(world, chunkX, chunkZ);
        UUID defenderId = claimIndex.get(key);

        if (defenderId == null) {
            return ClaimResult.CHUNK_NOT_CLAIMED;
        }

        if (defenderId.equals(attackerFaction.id())) {
            return ClaimResult.ALREADY_CLAIMED_SELF;
        }

        Faction defenderFaction = factionManager.getFaction(defenderId);
        if (defenderFaction == null) {
            // Orphaned claim, just take it
            return forceClaimChunk(attackerFaction, playerUuid, world, chunkX, chunkZ);
        }

        // Check if enemy
        if (attackerFaction.isAlly(defenderId)) {
            return ClaimResult.ALREADY_CLAIMED_ALLY;
        }

        // Check defender power
        double defenderPower = powerManager.getFactionPower(defenderId);
        int defenderMaxClaims = ConfigManager.get().calculateMaxClaims(defenderPower);

        if (defenderFaction.getClaimCount() < defenderMaxClaims) {
            return ClaimResult.TARGET_HAS_POWER;
        }

        // Check attacker can claim
        double attackerPower = powerManager.getFactionPower(attackerFaction.id());
        int attackerMaxClaims = ConfigManager.get().calculateMaxClaims(attackerPower);
        if (attackerFaction.getClaimCount() >= attackerMaxClaims) {
            return ClaimResult.MAX_CLAIMS_REACHED;
        }

        // Remove from defender
        Faction updatedDefender = defenderFaction.withoutClaimAt(world, chunkX, chunkZ)
            .withLog(FactionLog.create(FactionLog.LogType.OVERCLAIM,
                String.format("Lost chunk at %d, %d to %s", chunkX, chunkZ, attackerFaction.name()), null));

        // Add to attacker
        FactionClaim claim = FactionClaim.create(world, chunkX, chunkZ, playerUuid);
        Faction updatedAttacker = attackerFaction.withClaim(claim)
            .withLog(FactionLog.create(FactionLog.LogType.OVERCLAIM,
                String.format("Overclaimed chunk at %d, %d from %s", chunkX, chunkZ, defenderFaction.name()), playerUuid));

        // Update indices - remove from defender
        Set<ChunkKey> defenderClaims = factionClaimsIndex.get(defenderId);
        if (defenderClaims != null) {
            defenderClaims.remove(key);
            if (defenderClaims.isEmpty()) {
                factionClaimsIndex.remove(defenderId);
            }
        }

        // Update indices - add to attacker
        claimIndex.put(key, attackerFaction.id());
        factionClaimsIndex.computeIfAbsent(attackerFaction.id(), k -> ConcurrentHashMap.newKeySet()).add(key);

        // Update factions
        factionManager.updateFaction(updatedDefender);
        factionManager.updateFaction(updatedAttacker);

        Logger.debugClaim("Overclaim success: chunk=%s, attacker=%s, defender=%s, defenderClaims=%d/%d",
            key, attackerFaction.name(), defenderFaction.name(), defenderFaction.getClaimCount() - 1, defenderMaxClaims);
        Logger.info("Faction '%s' overclaimed chunk from '%s'", attackerFaction.name(), defenderFaction.name());

        // Notify defender faction members that they lost territory
        notifyFactionMembers(defenderId,
            String.format("Territory lost! %s overclaimed chunk at %d, %d", attackerFaction.name(), chunkX, chunkZ),
            "#FF5555");

        if (onOverclaimCallback != null) {
            try { onOverclaimCallback.accept(attackerFaction.name(), defenderFaction.name()); } catch (Exception e) { Logger.warn("Error in overclaim callback: %s", e.getMessage()); }
        }

        notifyChunkChange(world, chunkX, chunkZ);
        return ClaimResult.SUCCESS;
    }

    /**
     * Unclaims all chunks for a faction (used when disbanding or admin unclaim).
     *
     * @param factionId the faction ID
     */
    public void unclaimAll(@NotNull UUID factionId) {
        // Get the faction to update its record
        Faction faction = factionManager.getFaction(factionId);

        // Remove from main index
        claimIndex.entrySet().removeIf(entry -> entry.getValue().equals(factionId));
        // Remove from reverse index
        factionClaimsIndex.remove(factionId);

        // Update the Faction record to clear claims (if faction still exists)
        if (faction != null && faction.getClaimCount() > 0) {
            Faction updated = faction.withoutAllClaims()
                .withLog(FactionLog.create(FactionLog.LogType.UNCLAIM,
                    "All territory unclaimed", null));
            factionManager.updateFaction(updated);
            Logger.debugClaim("Unclaim all: faction=%s, claims removed=%d", faction.name(), faction.getClaimCount());
        }

        // For bulk operations like unclaimAll, use the legacy callback for full refresh
        // This is appropriate since all chunks are affected
        notifyClaimChange();
    }

    /**
     * Gets all claims for a faction.
     * O(1) lookup using the reverse index.
     *
     * @param factionId the faction ID
     * @return set of chunk keys (unmodifiable view)
     */
    @NotNull
    public Set<ChunkKey> getFactionClaims(@NotNull UUID factionId) {
        Set<ChunkKey> claims = factionClaimsIndex.get(factionId);
        if (claims == null) {
            return Collections.emptySet();
        }
        // Return unmodifiable view to prevent external modification
        return Collections.unmodifiableSet(claims);
    }

    /**
     * Checks if removing a chunk would disconnect a faction's claims into islands.
     * Uses BFS to verify all remaining claims are still connected.
     *
     * @param factionId the faction ID
     * @param removing  the chunk being removed
     * @return true if removing the chunk would disconnect claims
     */
    private boolean wouldDisconnectClaims(@NotNull UUID factionId, @NotNull ChunkKey removing) {
        Set<ChunkKey> claims = factionClaimsIndex.get(factionId);
        if (claims == null || claims.size() <= 2) {
            // 0 or 1 remaining after removal — can't be disconnected
            return false;
        }

        // Build the remaining set (minus the chunk being removed)
        Set<ChunkKey> remaining = new HashSet<>(claims);
        remaining.remove(removing);

        if (remaining.isEmpty()) {
            return false;
        }

        // BFS from any remaining claim
        ChunkKey start = remaining.iterator().next();
        Set<ChunkKey> visited = new HashSet<>();
        ArrayDeque<ChunkKey> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            ChunkKey current = queue.poll();
            for (ChunkKey neighbor : new ChunkKey[]{
                current.north(), current.south(), current.east(), current.west()
            }) {
                if (remaining.contains(neighbor) && visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        // If BFS didn't reach all remaining claims, removal would disconnect
        return visited.size() < remaining.size();
    }

    private ClaimResult forceClaimChunk(Faction faction, UUID playerUuid, String world, int chunkX, int chunkZ) {
        ChunkKey key = new ChunkKey(world, chunkX, chunkZ);
        FactionClaim claim = FactionClaim.create(world, chunkX, chunkZ, playerUuid);

        Faction updated = faction.withClaim(claim)
            .withLog(FactionLog.create(FactionLog.LogType.CLAIM,
                String.format("Claimed chunk at %d, %d in %s", chunkX, chunkZ, world), playerUuid));

        // Update both indices
        claimIndex.put(key, faction.id());
        factionClaimsIndex.computeIfAbsent(faction.id(), k -> ConcurrentHashMap.newKeySet()).add(key);
        factionManager.updateFaction(updated);

        return ClaimResult.SUCCESS;
    }

    // === Claim Decay ===

    /**
     * Runs claim decay for inactive factions.
     * Removes claims from factions where ALL members have been offline
     * for longer than the configured threshold.
     *
     * This method is called periodically (default: every hour) to clean up
     * territory from abandoned factions.
     */
    public void tickClaimDecay() {
        ConfigManager config = ConfigManager.get();
        if (!config.isDecayEnabled()) {
            return;
        }

        int daysThreshold = config.getDecayDaysInactive();
        long thresholdMs = System.currentTimeMillis() - (daysThreshold * 24L * 60 * 60 * 1000);

        int factionsDecayed = 0;
        int claimsRemoved = 0;

        // Check all factions with claims
        for (UUID factionId : new HashSet<>(factionClaimsIndex.keySet())) {
            Faction faction = factionManager.getFaction(factionId);
            if (faction == null) {
                // Orphaned claims - clean them up
                int orphanedClaims = factionClaimsIndex.getOrDefault(factionId, Collections.emptySet()).size();
                unclaimAll(factionId);
                claimsRemoved += orphanedClaims;
                Logger.info("Claim decay: Removed %d orphaned claims (faction no longer exists)", orphanedClaims);
                continue;
            }

            // Check if all members are inactive (claim decay exempt members count as active)
            boolean allInactive = true;
            long mostRecentLogin = 0;
            for (FactionMember member : faction.members().values()) {
                if (member.lastOnline() > mostRecentLogin) {
                    mostRecentLogin = member.lastOnline();
                }
                // Decay-exempt members are treated as always active
                if (powerManager.getPlayerPower(member.uuid()).claimDecayExempt()) {
                    allInactive = false;
                    break;
                }
                if (member.lastOnline() > thresholdMs) {
                    allInactive = false;
                    break;
                }
            }

            if (allInactive && faction.getClaimCount() > 0) {
                int claims = faction.getClaimCount();
                long daysSinceActive = (System.currentTimeMillis() - mostRecentLogin) / (24L * 60 * 60 * 1000);

                // Log the decay with faction details
                Faction updated = faction.withLog(FactionLog.create(FactionLog.LogType.UNCLAIM,
                    String.format("All %d claims removed due to inactivity (%d days)", claims, daysSinceActive), null));
                factionManager.updateFaction(updated);

                unclaimAll(factionId);
                factionsDecayed++;
                claimsRemoved += claims;

                Logger.info("Claim decay: Faction '%s' lost %d claims (inactive for %d days, threshold: %d days)",
                    faction.name(), claims, daysSinceActive, daysThreshold);
            }
        }

        if (factionsDecayed > 0) {
            Logger.info("Claim decay complete: %d factions affected, %d total claims removed",
                factionsDecayed, claimsRemoved);
        } else {
            Logger.debugClaim("Claim decay tick: no inactive factions found (threshold: %d days)", daysThreshold);
        }
    }

    /**
     * Checks if a faction is considered inactive.
     * A faction is inactive if ALL members have been offline for longer
     * than the configured decay threshold.
     *
     * @param factionId the faction ID to check
     * @return true if all members are inactive, false otherwise
     */
    public boolean isFactionInactive(@NotNull UUID factionId) {
        ConfigManager config = ConfigManager.get();
        if (!config.isDecayEnabled()) {
            return false;
        }

        Faction faction = factionManager.getFaction(factionId);
        if (faction == null) {
            return true; // Non-existent faction is considered inactive
        }

        int daysThreshold = config.getDecayDaysInactive();
        long thresholdMs = System.currentTimeMillis() - (daysThreshold * 24L * 60 * 60 * 1000);

        for (FactionMember member : faction.members().values()) {
            // Decay-exempt members are treated as always active
            if (powerManager.getPlayerPower(member.uuid()).claimDecayExempt()) {
                return false;
            }
            if (member.lastOnline() > thresholdMs) {
                return false; // At least one active member
            }
        }

        return true; // All members inactive
    }

    /**
     * Gets the number of days until a faction's claims will decay.
     * Returns -1 if decay is disabled or faction has active members.
     *
     * @param factionId the faction ID to check
     * @return days until decay, or -1 if not applicable
     */
    public int getDaysUntilDecay(@NotNull UUID factionId) {
        ConfigManager config = ConfigManager.get();
        if (!config.isDecayEnabled()) {
            return -1;
        }

        Faction faction = factionManager.getFaction(factionId);
        if (faction == null || faction.getClaimCount() == 0) {
            return -1;
        }

        int daysThreshold = config.getDecayDaysInactive();

        // Find most recent member login (decay-exempt members prevent decay entirely)
        long mostRecentLogin = 0;
        for (FactionMember member : faction.members().values()) {
            if (powerManager.getPlayerPower(member.uuid()).claimDecayExempt()) {
                return -1; // Exempt member = faction never decays
            }
            if (member.lastOnline() > mostRecentLogin) {
                mostRecentLogin = member.lastOnline();
            }
        }

        long daysSinceActive = (System.currentTimeMillis() - mostRecentLogin) / (24L * 60 * 60 * 1000);
        int daysUntilDecay = daysThreshold - (int) daysSinceActive;

        return Math.max(0, daysUntilDecay);
    }
}
