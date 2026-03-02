package com.hyperfactions.manager;

import com.hyperfactions.data.ChunkKey;
import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneFlags;
import com.hyperfactions.data.ZoneType;
import com.hyperfactions.storage.ZoneStorage;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages SafeZones and WarZones.
 * Zones can span multiple chunks and are managed as named entities.
 */
public class ZoneManager {

  private final ZoneStorage storage;

  private final ClaimManager claimManager;

  // Index: ChunkKey -> Zone (for quick lookup by chunk)
  private final Map<ChunkKey, Zone> zoneIndex = new ConcurrentHashMap<>();

  // All zones by ID
  private final Map<UUID, Zone> zonesById = new ConcurrentHashMap<>();

  // Zones by name (case-insensitive)
  private final Map<String, Zone> zonesByName = new ConcurrentHashMap<>();

  // Callback for when zones change (used to refresh world map)
  // Accepts set of affected chunks, or null for full refresh
  @Nullable
  private Consumer<Set<ChunkKey>> onZoneChangeCallback;

  // Batch mode for bulk operations - defers notifyZoneChange until endBatch
  private boolean batchMode = false;

  private boolean pendingNotification = false;

  private Set<ChunkKey> pendingChunks = new HashSet<>();

  /** Creates a new ZoneManager. */
  public ZoneManager(@NotNull ZoneStorage storage, @NotNull ClaimManager claimManager) {
    this.storage = storage;
    this.claimManager = claimManager;
  }

  /**
   * Starts batch mode for bulk operations.
   * While in batch mode, zone change notifications are deferred until endBatch is called.
   * This prevents excessive map refreshes during imports.
   */
  public void startBatch() {
    batchMode = true;
    pendingNotification = false;
    pendingChunks.clear();
    Logger.debug("Zone batch mode started");
  }

  /**
   * Ends batch mode and fires any pending notifications.
   * Should be called in a finally block after startBatch.
   */
  public void endBatch() {
    batchMode = false;
    if (pendingNotification) {
      Logger.debug("Zone batch mode ended - firing deferred notification for %d chunks",
        pendingChunks.size());
      // Pass accumulated chunks (may be empty if zones were created empty)
      Set<ChunkKey> chunks = pendingChunks.isEmpty() ? null : new HashSet<>(pendingChunks);
      notifyZoneChange(chunks);
      pendingNotification = false;
      pendingChunks.clear();
    } else {
      Logger.debug("Zone batch mode ended - no pending notifications");
    }
  }

  /**
   * Checks if batch mode is active.
   *
   * @return true if in batch mode
   */
  public boolean isInBatchMode() {
    return batchMode;
  }

  /**
   * Sets a callback to be invoked when zones change.
   * Used to trigger world map refresh.
   * The callback receives the set of affected chunks, or null for full refresh.
   *
   * @param callback the callback to run on zone changes
   */
  public void setOnZoneChangeCallback(@Nullable Consumer<Set<ChunkKey>> callback) {
    this.onZoneChangeCallback = callback;
  }

  /**
   * Notifies that zones have changed (triggers world map refresh).
   * In batch mode, notifications are deferred until endBatch is called.
   *
   * @param affectedChunks the chunks that changed, or null for full refresh
   */
  private void notifyZoneChange(@Nullable Set<ChunkKey> affectedChunks) {
    if (batchMode) {
      pendingNotification = true;
      if (affectedChunks != null) {
        pendingChunks.addAll(affectedChunks);
      }
      Logger.debugTerritory("Zone change notification deferred (batch mode), %d chunks accumulated",
        pendingChunks.size());
      return;
    }

    Logger.debugTerritory("Zone change notification triggered for %s chunks",
      affectedChunks == null ? "all" : String.valueOf(affectedChunks.size()));
    if (onZoneChangeCallback != null) {
      try {
        onZoneChangeCallback.accept(affectedChunks);
      } catch (Exception e) {
        Logger.warn("Error in zone change callback: %s", e.getMessage());
      }
    }
  }

  /**
   * Loads all zones from storage.
   *
   * <p>SAFETY: This method will NOT clear existing data if loading fails or returns
   * suspiciously empty results when data was expected.
   *
   * @return a future that completes when loading is done
   */
  public CompletableFuture<Void> loadAll() {
    final int previousZoneCount = zonesById.size();

    final int previousChunkCount = zoneIndex.size();

    return storage.loadAllZones().thenAccept(loaded -> {
      // SAFETY CHECK: If we had data before but loading returned nothing,
      // this is likely a load failure - DO NOT clear existing data
      if (previousZoneCount > 0 && loaded.isEmpty()) {
        Logger.severe("CRITICAL: Load returned 0 zones but %d were previously loaded!",
          previousZoneCount);
        Logger.severe("Keeping existing in-memory data to prevent data loss.");
        return;
      }

      // Build new indices before clearing old ones
      Map<ChunkKey, Zone> newZoneIndex = new HashMap<>();
      Map<UUID, Zone> newZonesById = new HashMap<>();
      Map<String, Zone> newZonesByName = new HashMap<>();

      for (Zone zone : loaded) {
        newZonesById.put(zone.id(), zone);
        newZonesByName.put(zone.name().toLowerCase(), zone);
        // Index all chunks belonging to this zone
        for (ChunkKey chunk : zone.chunks()) {
          newZoneIndex.put(chunk, zone);
        }
      }

      // Atomic swap
      zoneIndex.clear();
      zoneIndex.putAll(newZoneIndex);

      zonesById.clear();
      zonesById.putAll(newZonesById);

      zonesByName.clear();
      zonesByName.putAll(newZonesByName);

      Logger.info("[Startup] Loaded %d zones with %d total chunks", zonesById.size(), zoneIndex.size());
    }).exceptionally(ex -> {
      ErrorHandler.report("CRITICAL: Exception during zone loading - keeping existing data", ex);
      return null;
    });
  }

  /**
   * Saves all zones to storage.
   *
   * @return a future that completes when saving is done
   */
  public CompletableFuture<Void> saveAll() {
    return storage.saveAllZones(zonesById.values());
  }

  // === Queries ===

  /**
   * Gets the zone at a chunk location.
   *
   * @param world  the world name
   * @param chunkX the chunk X
   * @param chunkZ the chunk Z
   * @return the zone, or null if none
   */
  @Nullable
  public Zone getZone(@NotNull String world, int chunkX, int chunkZ) {
    ChunkKey key = new ChunkKey(world, chunkX, chunkZ);
    Zone zone = zoneIndex.get(key);

    // If not found, try case-insensitive lookup
    if (zone == null) {
      // Try lowercase
      ChunkKey lowercaseKey = new ChunkKey(world.toLowerCase(), chunkX, chunkZ);
      zone = zoneIndex.get(lowercaseKey);

      // Try "default" if world looks like the main world
      if (zone == null && (world.equalsIgnoreCase("World") || world.equalsIgnoreCase("default"))) {
        zone = zoneIndex.get(new ChunkKey("default", chunkX, chunkZ));
        if (zone == null) {
          zone = zoneIndex.get(new ChunkKey("World", chunkX, chunkZ));
        }
      }

      // DEBUG: Log lookup attempts (only when debug enabled)
      if (zone == null && !zoneIndex.isEmpty()) {
        Logger.debug("[ZoneManager] Zone lookup failed for world='%s' chunk=(%d,%d)", world, chunkX, chunkZ);
        zoneIndex.keySet().stream()
          .filter(k -> k.chunkX() == chunkX && k.chunkZ() == chunkZ)
          .forEach(k -> Logger.debug("[ZoneManager] Same coords exists with world='%s'", k.world()));
      }
    }

    return zone;
  }

  /**
   * Gets the zone at world coordinates.
   *
   * @param world the world name
   * @param x     the world X coordinate
   * @param z     the world Z coordinate
   * @return the zone, or null if none
   */
  @Nullable
  public Zone getZoneAt(@NotNull String world, double x, double z) {
    return zoneIndex.get(ChunkKey.fromWorldCoords(world, x, z));
  }

  /**
   * Checks if a location is in a SafeZone.
   *
   * @param world  the world name
   * @param chunkX the chunk X
   * @param chunkZ the chunk Z
   * @return true if in SafeZone
   */
  public boolean isInSafeZone(@NotNull String world, int chunkX, int chunkZ) {
    Zone zone = getZone(world, chunkX, chunkZ);
    return zone != null && zone.isSafeZone();
  }

  /**
   * Checks if a location is in a WarZone.
   *
   * @param world  the world name
   * @param chunkX the chunk X
   * @param chunkZ the chunk Z
   * @return true if in WarZone
   */
  public boolean isInWarZone(@NotNull String world, int chunkX, int chunkZ) {
    Zone zone = getZone(world, chunkX, chunkZ);
    return zone != null && zone.isWarZone();
  }

  /**
   * Checks if world coordinates are in a SafeZone.
   *
   * @param world the world name
   * @param x     the world X
   * @param z     the world Z
   * @return true if in SafeZone
   */
  public boolean isInSafeZoneAt(@NotNull String world, double x, double z) {
    Zone zone = getZoneAt(world, x, z);
    return zone != null && zone.isSafeZone();
  }

  /**
   * Checks if world coordinates are in a WarZone.
   *
   * @param world the world name
   * @param x     the world X
   * @param z     the world Z
   * @return true if in WarZone
   */
  public boolean isInWarZoneAt(@NotNull String world, double x, double z) {
    Zone zone = getZoneAt(world, x, z);
    return zone != null && zone.isWarZone();
  }

  /**
   * Gets a zone by ID.
   *
   * @param zoneId the zone ID
   * @return the zone, or null if not found
   */
  @Nullable
  public Zone getZoneById(@NotNull UUID zoneId) {
    return zonesById.get(zoneId);
  }

  /**
   * Gets a zone by name (case-insensitive).
   *
   * @param name the zone name
   * @return the zone, or null if not found
   */
  @Nullable
  public Zone getZoneByName(@NotNull String name) {
    return zonesByName.get(name.toLowerCase());
  }

  /**
   * Gets all zones.
   *
   * @return collection of all zones
   */
  @NotNull
  public Collection<Zone> getAllZones() {
    return Collections.unmodifiableCollection(zonesById.values());
  }

  /**
   * Gets all zones of a specific type.
   *
   * @param type the zone type
   * @return list of zones
   */
  @NotNull
  public List<Zone> getZonesByType(@NotNull ZoneType type) {
    return zonesById.values().stream()
      .filter(z -> z.type() == type)
      .collect(Collectors.toList());
  }

  /**
   * Gets the total zone count.
   *
   * @return number of zones
   */
  public int getZoneCount() {
    return zonesById.size();
  }

  /**
   * Gets the total number of chunks claimed by zones.
   *
   * @return total chunk count
   */
  public int getTotalChunkCount() {
    return zoneIndex.size();
  }

  // === Operations ===

  /**
   * Result of a zone operation.
   */
  public enum ZoneResult {
    SUCCESS,
    ALREADY_EXISTS,
    NOT_FOUND,
    CHUNK_CLAIMED,
    CHUNK_HAS_FACTION,
    CHUNK_HAS_ZONE,
    CHUNK_NOT_IN_ZONE,
    NAME_TAKEN,
    INVALID_NAME
  }

  /**
   * Creates a new empty zone.
   *
   * @param name      the zone name
   * @param type      the zone type
   * @param world     the world name
   * @param createdBy the creator's UUID
   * @return the result
   */
  public ZoneResult createZone(@NotNull String name, @NotNull ZoneType type,
                @NotNull String world, @NotNull UUID createdBy) {
    // Validate name
    if (name.isBlank() || name.length() > 32) {
      return ZoneResult.INVALID_NAME;
    }

    // Check if name is taken
    if (zonesByName.containsKey(name.toLowerCase())) {
      return ZoneResult.NAME_TAKEN;
    }

    Zone zone = Zone.create(name, type, world, createdBy);

    zonesById.put(zone.id(), zone);
    zonesByName.put(name.toLowerCase(), zone);

    // Save async
    saveAll();

    Logger.info("[Zone] Created empty %s '%s' in %s", type.getDisplayName(), name, world);
    notifyZoneChange(null); // Empty zone, full refresh for map update
    return ZoneResult.SUCCESS;
  }

  /**
   * Creates a new zone with all its chunks atomically.
   * This is the preferred method for bulk imports as it avoids race conditions
   * and only saves/notifies once at the end.
   *
   * @param name      the zone name
   * @param type      the zone type
   * @param world     the world name
   * @param createdBy the creator's UUID
   * @param chunks    the set of chunks to include in the zone
   * @return a future that completes with the result
   */
  public CompletableFuture<ZoneResult> createZoneWithChunks(@NotNull String name, @NotNull ZoneType type,
                               @NotNull String world, @NotNull UUID createdBy,
                               @NotNull Set<ChunkKey> chunks) {
    return createZoneWithChunks(name, type, world, createdBy, chunks, null);
  }

  /**
   * Creates a zone with multiple chunks and optional explicit flags.
   * Used by import to apply default flags explicitly.
   *
   * @param name      the zone name
   * @param type      the zone type
   * @param world     the world name
   * @param createdBy UUID of creator
   * @param chunks    the chunks to include
   * @param flags     explicit flags (null = use lazy defaults)
   * @return future with result
   */
  public CompletableFuture<ZoneResult> createZoneWithChunks(@NotNull String name, @NotNull ZoneType type,
                               @NotNull String world, @NotNull UUID createdBy,
                               @NotNull Set<ChunkKey> chunks,
                               @Nullable Map<String, Boolean> flags) {
    // Validate name
    if (name.isBlank() || name.length() > 32) {
      return CompletableFuture.completedFuture(ZoneResult.INVALID_NAME);
    }

    // Check if name is taken
    if (zonesByName.containsKey(name.toLowerCase())) {
      return CompletableFuture.completedFuture(ZoneResult.NAME_TAKEN);
    }

    // Check all chunks for conflicts
    for (ChunkKey chunk : chunks) {
      if (zoneIndex.containsKey(chunk)) {
        return CompletableFuture.completedFuture(ZoneResult.CHUNK_HAS_ZONE);
      }
      if (claimManager.getClaimOwner(chunk.world(), chunk.chunkX(), chunk.chunkZ()) != null) {
        return CompletableFuture.completedFuture(ZoneResult.CHUNK_HAS_FACTION);
      }
    }

    // Create zone with all chunks
    Zone zone = new Zone(
        UUID.randomUUID(),
        name,
        type,
        world,
        new java.util.HashSet<>(chunks),
        System.currentTimeMillis(),
        createdBy,
        flags,
        null, null, null
    );

    // Update all indexes atomically
    zonesById.put(zone.id(), zone);
    zonesByName.put(name.toLowerCase(), zone);
    for (ChunkKey chunk : chunks) {
      zoneIndex.put(chunk, zone);
    }

    Logger.info("[Zone] Created %s '%s' with %d chunks in %s", type.getDisplayName(), name, chunks.size(), world);

    // Save and notify
    return saveAll().thenApply(v -> {
      notifyZoneChange(chunks);
      return ZoneResult.SUCCESS;
    });
  }

  /**
   * Creates a new zone with a single initial chunk (backwards compatible).
   *
   * @param name      the zone name
   * @param type      the zone type
   * @param world     the world name
   * @param chunkX    the chunk X
   * @param chunkZ    the chunk Z
   * @param createdBy the creator's UUID
   * @return the result
   */
  public ZoneResult createZone(@NotNull String name, @NotNull ZoneType type,
                @NotNull String world, int chunkX, int chunkZ,
                @NotNull UUID createdBy) {
    ChunkKey key = new ChunkKey(world, chunkX, chunkZ);

    // Check if zone already exists at location
    if (zoneIndex.containsKey(key)) {
      return ZoneResult.ALREADY_EXISTS;
    }

    // Check if chunk is claimed by a faction
    if (claimManager.getClaimOwner(world, chunkX, chunkZ) != null) {
      return ZoneResult.CHUNK_CLAIMED;
    }

    // Check if name is taken
    if (zonesByName.containsKey(name.toLowerCase())) {
      return ZoneResult.NAME_TAKEN;
    }

    Zone zone = Zone.create(name, type, world, chunkX, chunkZ, createdBy);

    zonesById.put(zone.id(), zone);
    zonesByName.put(name.toLowerCase(), zone);
    zoneIndex.put(key, zone);

    // Save async
    saveAll();

    Logger.info("[Zone] Created %s '%s' at %d, %d in %s", type.getDisplayName(), name, chunkX, chunkZ, world);
    notifyZoneChange(Set.of(key));
    return ZoneResult.SUCCESS;
  }

  /**
   * Claims a chunk for an existing zone.
   *
   * @param zoneId the zone ID
   * @param world  the world name
   * @param chunkX the chunk X
   * @param chunkZ the chunk Z
   * @return the result
   */
  public ZoneResult claimChunk(@NotNull UUID zoneId, @NotNull String world, int chunkX, int chunkZ) {
    Zone zone = zonesById.get(zoneId);
    if (zone == null) {
      return ZoneResult.NOT_FOUND;
    }

    // Check world matches
    if (!zone.world().equals(world)) {
      return ZoneResult.NOT_FOUND; // Different world
    }

    ChunkKey key = new ChunkKey(world, chunkX, chunkZ);

    // Check if already claimed by a zone
    if (zoneIndex.containsKey(key)) {
      Zone existingZone = zoneIndex.get(key);
      if (existingZone.id().equals(zoneId)) {
        return ZoneResult.SUCCESS; // Already in this zone
      }
      return ZoneResult.CHUNK_HAS_ZONE;
    }

    // Check if claimed by a faction
    if (claimManager.getClaimOwner(world, chunkX, chunkZ) != null) {
      return ZoneResult.CHUNK_HAS_FACTION;
    }

    // Add chunk to zone
    Zone updated = zone.withChunk(chunkX, chunkZ);
    updateZone(updated);

    Logger.info("[Zone] Claimed chunk (%d, %d) for zone '%s'", chunkX, chunkZ, zone.name());
    notifyZoneChange(Set.of(key));
    return ZoneResult.SUCCESS;
  }

  /**
   * Unclaims a chunk from its zone.
   *
   * @param zoneId the zone ID
   * @param world  the world name
   * @param chunkX the chunk X
   * @param chunkZ the chunk Z
   * @return the result
   */
  public ZoneResult unclaimChunk(@NotNull UUID zoneId, @NotNull String world, int chunkX, int chunkZ) {
    Zone zone = zonesById.get(zoneId);
    if (zone == null) {
      return ZoneResult.NOT_FOUND;
    }

    ChunkKey key = new ChunkKey(world, chunkX, chunkZ);

    // Check if chunk belongs to this zone
    Zone existingZone = zoneIndex.get(key);
    if (existingZone == null || !existingZone.id().equals(zoneId)) {
      return ZoneResult.CHUNK_NOT_IN_ZONE;
    }

    // Remove chunk from zone
    Zone updated = zone.withoutChunk(chunkX, chunkZ);
    updateZone(updated);

    Logger.info("[Zone] Unclaimed chunk (%d, %d) from zone '%s'", chunkX, chunkZ, zone.name());
    notifyZoneChange(Set.of(key));
    return ZoneResult.SUCCESS;
  }

  /**
   * Unclaims the chunk at a location from whatever zone it belongs to.
   *
   * @param world  the world name
   * @param chunkX the chunk X
   * @param chunkZ the chunk Z
   * @return the result
   */
  public ZoneResult unclaimChunkAt(@NotNull String world, int chunkX, int chunkZ) {
    ChunkKey key = new ChunkKey(world, chunkX, chunkZ);
    Zone zone = zoneIndex.get(key);
    if (zone == null) {
      return ZoneResult.NOT_FOUND;
    }
    return unclaimChunk(zone.id(), world, chunkX, chunkZ);
  }

  /**
   * Claims multiple chunks in a radius for a zone.
   * Skips chunks that are already claimed by zones or factions.
   *
   * @param zoneId   the zone ID
   * @param world    the world name
   * @param centerX  the center chunk X
   * @param centerZ  the center chunk Z
   * @param radius   the radius in chunks (1-20)
   * @param circle   true for circle shape, false for square
   * @return the number of chunks successfully claimed
   */
  public int claimRadius(@NotNull UUID zoneId, @NotNull String world,
             int centerX, int centerZ, int radius, boolean circle) {
    Zone zone = zonesById.get(zoneId);
    if (zone == null) {
      return 0;
    }

    // Clamp radius
    radius = Math.max(1, Math.min(20, radius));

    int claimed = 0;
    Set<ChunkKey> newChunks = new HashSet<>(zone.chunks());

    for (int dx = -radius; dx <= radius; dx++) {
      for (int dz = -radius; dz <= radius; dz++) {
        // Check circle shape
        if (circle && (dx * dx + dz * dz) > radius * radius) {
          continue;
        }

        int chunkX = centerX + dx;
        int chunkZ = centerZ + dz;
        ChunkKey key = new ChunkKey(world, chunkX, chunkZ);

        // Skip if already claimed by zone or faction
        if (zoneIndex.containsKey(key)) {
          continue;
        }
        if (claimManager.getClaimOwner(world, chunkX, chunkZ) != null) {
          continue;
        }

        newChunks.add(key);
        claimed++;
      }
    }

    if (claimed > 0) {
      // Calculate which chunks were actually new (for map refresh)
      Set<ChunkKey> claimedChunks = new HashSet<>(newChunks);
      claimedChunks.removeAll(zone.chunks());

      Zone updated = new Zone(zone.id(), zone.name(), zone.type(), zone.world(),
                 newChunks, zone.createdAt(), zone.createdBy(), zone.flags(),
                 zone.notifyOnEntry(), zone.notifyTitleUpper(), zone.notifyTitleLower());
      updateZone(updated);
      Logger.info("[Zone] Claimed %d chunks in radius for zone '%s'", claimed, zone.name());
      notifyZoneChange(claimedChunks);
    }

    return claimed;
  }

  /**
   * Removes a zone entirely.
   *
   * @param zoneId the zone ID
   * @return the result
   */
  public ZoneResult removeZone(@NotNull UUID zoneId) {
    Zone zone = zonesById.remove(zoneId);
    if (zone == null) {
      return ZoneResult.NOT_FOUND;
    }

    zonesByName.remove(zone.name().toLowerCase());

    // Remove all chunk index entries for this zone
    for (ChunkKey chunk : zone.chunks()) {
      zoneIndex.remove(chunk);
    }

    // Save async
    saveAll();

    Logger.info("[Zone] Removed %s '%s' with %d chunks", zone.type().getDisplayName(), zone.name(), zone.getChunkCount());
    notifyZoneChange(zone.chunks());
    return ZoneResult.SUCCESS;
  }

  /**
   * Removes a zone at a specific location (removes the entire zone, not just the chunk).
   *
   * @param world  the world name
   * @param chunkX the chunk X
   * @param chunkZ the chunk Z
   * @return the result
   */
  public ZoneResult removeZoneAt(@NotNull String world, int chunkX, int chunkZ) {
    ChunkKey key = new ChunkKey(world, chunkX, chunkZ);
    Zone zone = zoneIndex.get(key);
    if (zone == null) {
      return ZoneResult.NOT_FOUND;
    }

    return removeZone(zone.id());
  }

  /**
   * Changes the type of a zone (SafeZone to WarZone or vice versa).
   *
   * @param zoneId     the zone ID
   * @param resetFlags if true, clears all custom flags (uses new type defaults);
   *                   if false, preserves existing custom flag overrides
   * @return the result
   */
  public ZoneResult changeZoneType(@NotNull UUID zoneId, boolean resetFlags) {
    Zone zone = zonesById.get(zoneId);
    if (zone == null) {
      return ZoneResult.NOT_FOUND;
    }

    // Toggle the type
    ZoneType newType = zone.isSafeZone() ? ZoneType.WAR : ZoneType.SAFE;

    // Preserve or clear flags based on parameter
    Map<String, Boolean> flags = resetFlags ? null : zone.flags();

    Zone updated = new Zone(
        zone.id(),
        zone.name(),
        newType,
        zone.world(),
        zone.chunks(),
        zone.createdAt(),
        zone.createdBy(),
        flags,
        zone.notifyOnEntry(),
        zone.notifyTitleUpper(),
        zone.notifyTitleLower()
    );

    updateZone(updated);

    Logger.info("[Zone] Changed zone '%s' from %s to %s (resetFlags=%s)",
        zone.name(), zone.type().getDisplayName(), newType.getDisplayName(), resetFlags);
    notifyZoneChange(zone.chunks());
    return ZoneResult.SUCCESS;
  }

  /**
   * Renames a zone.
   *
   * @param zoneId  the zone ID
   * @param newName the new name
   * @return the result
   */
  public ZoneResult renameZone(@NotNull UUID zoneId, @NotNull String newName) {
    Zone zone = zonesById.get(zoneId);
    if (zone == null) {
      return ZoneResult.NOT_FOUND;
    }

    // Validate name
    if (newName.isBlank() || newName.length() > 32) {
      return ZoneResult.INVALID_NAME;
    }

    // Check if new name is taken (unless it's the same zone)
    Zone existing = zonesByName.get(newName.toLowerCase());
    if (existing != null && !existing.id().equals(zoneId)) {
      return ZoneResult.NAME_TAKEN;
    }

    // Remove old name mapping
    zonesByName.remove(zone.name().toLowerCase());

    Zone updated = zone.withName(newName);
    updateZone(updated);

    return ZoneResult.SUCCESS;
  }

  // === Flag Management ===

  /**
   * Sets a flag on a zone.
   *
   * @param zoneId   the zone ID
   * @param flagName the flag name (see ZoneFlags)
   * @param value    the flag value
   * @return the result
   */
  public ZoneResult setZoneFlag(@NotNull UUID zoneId, @NotNull String flagName, boolean value) {
    if (!ZoneFlags.isValidFlag(flagName)) {
      return ZoneResult.NOT_FOUND; // Invalid flag
    }

    Zone zone = zonesById.get(zoneId);
    if (zone == null) {
      return ZoneResult.NOT_FOUND;
    }

    Zone updated = zone.withFlag(flagName, value);
    updateZone(updated);

    Logger.info("[Zone] Set flag '%s' to %s on zone '%s'", flagName, value, zone.name());
    return ZoneResult.SUCCESS;
  }

  /**
   * Clears a flag from a zone (reverts to default).
   *
   * @param zoneId   the zone ID
   * @param flagName the flag name
   * @return the result
   */
  public ZoneResult clearZoneFlag(@NotNull UUID zoneId, @NotNull String flagName) {
    Zone zone = zonesById.get(zoneId);
    if (zone == null) {
      return ZoneResult.NOT_FOUND;
    }

    Zone updated = zone.withoutFlag(flagName);
    updateZone(updated);

    Logger.info("[Zone] Cleared flag '%s' from zone '%s' (using default)", flagName, zone.name());
    return ZoneResult.SUCCESS;
  }

  /**
   * Clears all custom flags from a zone (reverts all to defaults).
   *
   * @param zoneId the zone ID
   * @return the result
   */
  public ZoneResult clearAllZoneFlags(@NotNull UUID zoneId) {
    Zone zone = zonesById.get(zoneId);
    if (zone == null) {
      return ZoneResult.NOT_FOUND;
    }

    Zone updated = zone.withFlags(null);
    updateZone(updated);

    Logger.info("[Zone] Cleared all custom flags from zone '%s' (using type defaults)", zone.name());
    return ZoneResult.SUCCESS;
  }

  /**
   * Gets the effective value of a flag for a zone.
   * Returns the zone's custom value if set, otherwise the default for its type.
   *
   * @param zone     the zone
   * @param flagName the flag name
   * @return the effective flag value
   */
  public boolean getEffectiveFlag(@NotNull Zone zone, @NotNull String flagName) {
    return zone.getEffectiveFlag(flagName);
  }

  /**
   * Gets the effective value of a flag at a location.
   * Returns the zone's flag value if in a zone, otherwise returns the default.
   *
   * @param world    the world name
   * @param chunkX   the chunk X
   * @param chunkZ   the chunk Z
   * @param flagName the flag name
   * @param defaultValue the default value if not in a zone
   * @return the effective flag value
   */
  public boolean getEffectiveFlagAt(@NotNull String world, int chunkX, int chunkZ,
                   @NotNull String flagName, boolean defaultValue) {
    Zone zone = getZone(world, chunkX, chunkZ);
    if (zone == null) {
      return defaultValue;
    }
    return zone.getEffectiveFlag(flagName);
  }

  // === Notification Settings ===

  /**
   * Sets whether a zone shows entry notifications.
   *
   * @param zoneId the zone ID
   * @param value  true = show (default), false = suppress, null = reset to default
   * @return the result
   */
  public ZoneResult setZoneNotifyOnEntry(@NotNull UUID zoneId, @Nullable Boolean value) {
    Zone zone = zonesById.get(zoneId);
    if (zone == null) {
      return ZoneResult.NOT_FOUND;
    }
    updateZone(zone.withNotifyOnEntry(value));
    return ZoneResult.SUCCESS;
  }

  /**
   * Sets custom notification title text for a zone.
   *
   * @param zoneId the zone ID
   * @param upper  upper title text (null = don't change, "clear" = reset to default)
   * @param lower  lower title text (null = don't change, "clear" = reset to default)
   * @return the result
   */
  public ZoneResult setZoneNotifyTitle(@NotNull UUID zoneId,
      @Nullable String upper, @Nullable String lower) {
    Zone zone = zonesById.get(zoneId);
    if (zone == null) {
      return ZoneResult.NOT_FOUND;
    }
    Zone updated = zone;
    if (upper != null) {
      updated = updated.withNotifyTitleUpper(upper.equals("clear") ? null : upper);
    }
    if (lower != null) {
      updated = updated.withNotifyTitleLower(lower.equals("clear") ? null : lower);
    }
    updateZone(updated);
    return ZoneResult.SUCCESS;
  }

  // === Internal Helpers ===

  /**
   * Updates a zone and its indexes.
   */
  private void updateZone(@NotNull Zone updated) {
    Zone old = zonesById.get(updated.id());

    // Update main index
    zonesById.put(updated.id(), updated);
    zonesByName.put(updated.name().toLowerCase(), updated);

    // Update chunk index - remove old chunks, add new chunks
    if (old != null) {
      for (ChunkKey chunk : old.chunks()) {
        if (!updated.chunks().contains(chunk)) {
          zoneIndex.remove(chunk);
        }
      }
    }
    for (ChunkKey chunk : updated.chunks()) {
      zoneIndex.put(chunk, updated);
    }

    // Save async
    saveAll();
  }
}
