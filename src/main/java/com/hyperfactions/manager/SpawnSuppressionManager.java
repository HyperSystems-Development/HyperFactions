package com.hyperfactions.manager;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.*;
import com.hyperfactions.util.Logger;
import com.hypixel.fastutil.longs.Long2ObjectConcurrentHashMap;
import com.hypixel.hytale.builtin.tagset.TagSetPlugin;
import com.hypixel.hytale.builtin.tagset.config.NPCGroup;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.spawning.suppression.component.ChunkSuppressionEntry;
import com.hypixel.hytale.server.spawning.suppression.component.SpawnSuppressionController;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages spawn suppression for zones based on mob spawning flags.
 * Integrates with Hytale's SpawnSuppressionController to block mob spawning
 * at the chunk level using the native suppression system.
 *
 * <p>Suppression is chunk-based and uses the following flags:
 * - MOB_SPAWNING: Master toggle (false = block all mobs)
 * - HOSTILE_MOB_SPAWNING: Controls hostile mob spawning
 * - PASSIVE_MOB_SPAWNING: Controls passive mob spawning
 * - NEUTRAL_MOB_SPAWNING: Controls neutral mob spawning
 *
 * <p>The suppression uses a unique ID prefix to identify HyperFactions suppressions,
 * allowing them to be updated without affecting other plugins.
 */
public class SpawnSuppressionManager {

  /**
   * Prefix for HyperFactions zone suppression IDs.
   * "HFAC" in ASCII hex: 0x48464143
   */
  private static final UUID ZONE_SUPPRESSION_PREFIX =
    UUID.fromString("48464143-0000-0000-0000-000000000000");

  /**
   * Prefix for HyperFactions claim suppression IDs.
   * "HFCL" in ASCII hex: 0x4846434C
   */
  private static final UUID CLAIM_SUPPRESSION_PREFIX =
    UUID.fromString("4846434C-0000-0000-0000-000000000000");

  /**
   * Default Y range for suppression spans.
   * Covers the entire vertical world range.
   */
  private static final int MIN_Y = -64;

  private static final int MAX_Y = 320;

  /**
   * NPCGroup names for mob categorization.
   * These must match Hytale's built-in group definitions.
   */
  private static final String GROUP_HOSTILE = "hostile";

  private static final String GROUP_PASSIVE = "passive";

  private static final String GROUP_NEUTRAL = "neutral";

  private final ZoneManager zoneManager;

  private final ClaimManager claimManager;

  private final FactionManager factionManager;

  // Cache of suppressor IDs by zone/faction ID
  private final Map<UUID, UUID> zoneSuppressorIds = new ConcurrentHashMap<>();

  private final Map<UUID, UUID> claimSuppressorIds = new ConcurrentHashMap<>();

  // Cache of NPC group indices (resolved once at startup)
  private int hostileGroupIndex = -1;

  private int passiveGroupIndex = -1;

  private int neutralGroupIndex = -1;

  private boolean groupsResolved = false;

  /** Creates a new SpawnSuppressionManager. */
  public SpawnSuppressionManager(@NotNull ZoneManager zoneManager,
                  @NotNull ClaimManager claimManager,
                  @NotNull FactionManager factionManager) {
    this.zoneManager = zoneManager;
    this.claimManager = claimManager;
    this.factionManager = factionManager;
  }

  /**
   * Initializes the manager and resolves NPC group indices.
   * Call this after the TagSetPlugin has been initialized.
   */
  public void initialize() {
    resolveNPCGroups();
    Logger.info("[Startup] Spawn suppression initialized - hostile=%d, passive=%d, neutral=%d",
      hostileGroupIndex, passiveGroupIndex, neutralGroupIndex);
  }

  /**
   * Resolves NPC group names to their integer indices.
   */
  private void resolveNPCGroups() {
    try {
      var assetMap = NPCGroup.getAssetMap();
      hostileGroupIndex = assetMap.getIndex(GROUP_HOSTILE);
      passiveGroupIndex = assetMap.getIndex(GROUP_PASSIVE);
      neutralGroupIndex = assetMap.getIndex(GROUP_NEUTRAL);
      groupsResolved = true;

      Logger.debugSpawning("Resolved NPC groups: hostile=%d, passive=%d, neutral=%d",
        hostileGroupIndex, passiveGroupIndex, neutralGroupIndex);
    } catch (Exception e) {
      Logger.warn("Failed to resolve NPC groups: %s", e.getMessage());
      groupsResolved = false;
    }
  }

  /**
   * Applies spawn suppression for all zones in the given world.
   * Call this on world load or when zones are modified.
   *
   * @param world the world to apply suppression to
   * @return true if suppression was applied successfully, false if world not ready
   */
  public boolean applyToWorld(@NotNull World world) {
    if (!groupsResolved) {
      resolveNPCGroups();
      if (!groupsResolved) {
        Logger.warn("Cannot apply spawn suppression - NPC groups not resolved");
        return false;
      }
    }

    String worldName = world.getName();

    // Get entity store - may be null for worlds that don't support entities
    EntityStore entityStoreHolder = world.getEntityStore();
    if (entityStoreHolder == null) {
      Logger.debugSpawning("No EntityStore for world '%s', will retry when world is ready", worldName);
      return false;
    }

    Store<EntityStore> entityStore = entityStoreHolder.getStore();
    if (entityStore == null) {
      Logger.debugSpawning("EntityStore not initialized for world '%s', will retry when world is ready", worldName);
      return false;
    }

    SpawnSuppressionController controller = entityStore.getResource(
      SpawnSuppressionController.getResourceType()
    );

    if (controller == null) {
      Logger.warn("SpawnSuppressionController not found for world '%s' - spawning module may not be loaded", worldName);
      return false;
    }

    Long2ObjectConcurrentHashMap<ChunkSuppressionEntry> chunkMap = controller.getChunkSuppressionMap();

    // Clear existing HyperFactions suppressions
    clearExistingSuppressions(chunkMap);

    // Apply suppression for each zone in this world
    int zonesProcessed = 0;
    int zoneChunks = 0;

    for (Zone zone : zoneManager.getAllZones()) {
      if (!zone.world().equalsIgnoreCase(worldName)) {
        continue;
      }

      // Determine what to suppress based on zone flags
      SuppressionConfig config = getZoneSuppressionConfig(zone);
      if (config == null) {
        // No suppression needed for this zone
        continue;
      }

      UUID suppressorId = getZoneSuppressorId(zone.id());

      for (ChunkKey chunk : zone.chunks()) {
        long chunkIndex = ChunkUtil.indexChunk(chunk.chunkX(), chunk.chunkZ());
        applySuppressionToChunk(chunkMap, chunkIndex, suppressorId, config);
        zoneChunks++;
      }
      zonesProcessed++;
    }

    // Apply suppression for each faction's claimed territory
    int factionsProcessed = 0;
    int claimChunks = 0;

    for (Faction faction : factionManager.getAllFactions()) {
      Set<ChunkKey> claims = claimManager.getFactionClaims(faction.id());
      if (claims.isEmpty()) {
        continue;
      }

      FactionPermissions perms = ConfigManager.get().getEffectiveFactionPermissions(
        faction.getEffectivePermissions()
      );

      SuppressionConfig config = getClaimSuppressionConfig(perms);
      if (config == null) {
        continue;
      }

      UUID suppressorId = getClaimSuppressorId(faction.id());
      int chunksForFaction = 0;

      for (ChunkKey chunk : claims) {
        if (!chunk.world().equalsIgnoreCase(worldName)) {
          continue;
        }
        long chunkIndex = ChunkUtil.indexChunk(chunk.chunkX(), chunk.chunkZ());
        applySuppressionToChunk(chunkMap, chunkIndex, suppressorId, config);
        chunksForFaction++;
      }
      claimChunks += chunksForFaction;
      if (chunksForFaction > 0) {
        factionsProcessed++;
      }
    }

    Logger.info("[SpawnSuppression] Applied to world '%s': %d zones (%d chunks), %d factions (%d claim chunks)",
      worldName, zonesProcessed, zoneChunks, factionsProcessed, claimChunks);
    return true;
  }

  /**
   * Applies spawn suppression to all worlds in the universe.
   * Call this on plugin enable.
   *
   * @param universe the universe to apply suppression to
   */
  public void applyToAllWorlds(@NotNull Universe universe) {
    universe.getWorlds().values().forEach(this::applyToWorld);
  }

  /**
   * Updates suppression for a specific zone.
   * Call this when a zone's flags change.
   *
   * @param zone the zone that was updated
   */
  public void updateZoneSuppression(@NotNull Zone zone) {
    // Find the world for this zone
    // This requires iterating worlds since we don't have direct world access
    // In practice, this would be called with a world reference
    Logger.debugSpawning("Zone '%s' suppression update requested - apply via world refresh", zone.name());
  }

  /**
   * Clears all HyperFactions suppressions (both zone and claim) from the chunk map.
   */
  private void clearExistingSuppressions(@NotNull Long2ObjectConcurrentHashMap<ChunkSuppressionEntry> chunkMap) {
    Set<UUID> ourIds = new HashSet<>(zoneSuppressorIds.values());
    ourIds.addAll(claimSuppressorIds.values());

    for (var entry : chunkMap.long2ObjectEntrySet()) {
      ChunkSuppressionEntry chunkEntry = entry.getValue();
      List<ChunkSuppressionEntry.SuppressionSpan> spans = chunkEntry.getSuppressionSpans();

      boolean hasOurs = spans.stream()
        .anyMatch(span -> ourIds.contains(span.getSuppressorId()));

      if (hasOurs) {
        List<ChunkSuppressionEntry.SuppressionSpan> filteredSpans = spans.stream()
          .filter(span -> !ourIds.contains(span.getSuppressorId()))
          .toList();

        if (filteredSpans.isEmpty()) {
          chunkMap.remove(entry.getLongKey());
        } else {
          chunkMap.put(entry.getLongKey(), new ChunkSuppressionEntry(filteredSpans));
        }
      }
    }
  }

  /**
   * Generates a unique suppressor ID for a zone.
   */
  @NotNull
  private UUID getZoneSuppressorId(@NotNull UUID zoneId) {
    return zoneSuppressorIds.computeIfAbsent(zoneId, id ->
      new UUID(
        ZONE_SUPPRESSION_PREFIX.getMostSignificantBits() ^ id.getMostSignificantBits(),
        ZONE_SUPPRESSION_PREFIX.getLeastSignificantBits() ^ id.getLeastSignificantBits()
      )
    );
  }

  /**
   * Generates a unique suppressor ID for a faction's claims.
   */
  @NotNull
  private UUID getClaimSuppressorId(@NotNull UUID factionId) {
    return claimSuppressorIds.computeIfAbsent(factionId, id ->
      new UUID(
        CLAIM_SUPPRESSION_PREFIX.getMostSignificantBits() ^ id.getMostSignificantBits(),
        CLAIM_SUPPRESSION_PREFIX.getLeastSignificantBits() ^ id.getLeastSignificantBits()
      )
    );
  }

  /**
   * Determines the suppression configuration for a zone based on its flags.
   *
   * @param zone the zone
   * @return suppression config, or null if no suppression needed
   */
  @Nullable
  private SuppressionConfig getZoneSuppressionConfig(@NotNull Zone zone) {
    // Check master toggle
    boolean mobSpawning = zone.getEffectiveFlag(ZoneFlags.MOB_SPAWNING);
    if (!mobSpawning) {
      // Block ALL mobs
      return new SuppressionConfig(null); // null = suppress all roles
    }

    // Check group flags
    boolean hostileAllowed = zone.getEffectiveFlag(ZoneFlags.HOSTILE_MOB_SPAWNING);
    boolean passiveAllowed = zone.getEffectiveFlag(ZoneFlags.PASSIVE_MOB_SPAWNING);
    boolean neutralAllowed = zone.getEffectiveFlag(ZoneFlags.NEUTRAL_MOB_SPAWNING);

    // If all groups allowed, no suppression needed
    if (hostileAllowed && passiveAllowed && neutralAllowed) {
      return null;
    }

    // Build set of suppressed roles
    IntSet suppressedRoles = new IntOpenHashSet();
    TagSetPlugin.TagSetLookup lookup = TagSetPlugin.get(NPCGroup.class);

    if (!hostileAllowed && hostileGroupIndex >= 0) {
      IntSet hostileRoles = lookup.getSet(hostileGroupIndex);
      if (hostileRoles != null) {
        suppressedRoles.addAll(hostileRoles);
      }
    }

    if (!passiveAllowed && passiveGroupIndex >= 0) {
      IntSet passiveRoles = lookup.getSet(passiveGroupIndex);
      if (passiveRoles != null) {
        suppressedRoles.addAll(passiveRoles);
      }
    }

    if (!neutralAllowed && neutralGroupIndex >= 0) {
      IntSet neutralRoles = lookup.getSet(neutralGroupIndex);
      if (neutralRoles != null) {
        suppressedRoles.addAll(neutralRoles);
      }
    }

    if (suppressedRoles.isEmpty()) {
      return null;
    }

    return new SuppressionConfig(suppressedRoles);
  }

  /**
   * Determines the suppression configuration for a faction's claims based on its permissions.
   *
   * @param perms the effective faction permissions
   * @return suppression config, or null if no suppression needed
   */
  @Nullable
  private SuppressionConfig getClaimSuppressionConfig(@NotNull FactionPermissions perms) {
    // Check master toggle
    if (!perms.get(FactionPermissions.MOB_SPAWNING)) {
      return new SuppressionConfig(null); // null = suppress all roles
    }

    // Check group flags
    boolean hostileAllowed = perms.get(FactionPermissions.HOSTILE_MOB_SPAWNING);
    boolean passiveAllowed = perms.get(FactionPermissions.PASSIVE_MOB_SPAWNING);
    boolean neutralAllowed = perms.get(FactionPermissions.NEUTRAL_MOB_SPAWNING);

    if (hostileAllowed && passiveAllowed && neutralAllowed) {
      return null;
    }

    IntSet suppressedRoles = new IntOpenHashSet();
    TagSetPlugin.TagSetLookup lookup = TagSetPlugin.get(NPCGroup.class);

    if (!hostileAllowed && hostileGroupIndex >= 0) {
      IntSet hostileRoles = lookup.getSet(hostileGroupIndex);
      if (hostileRoles != null) {
        suppressedRoles.addAll(hostileRoles);
      }
    }

    if (!passiveAllowed && passiveGroupIndex >= 0) {
      IntSet passiveRoles = lookup.getSet(passiveGroupIndex);
      if (passiveRoles != null) {
        suppressedRoles.addAll(passiveRoles);
      }
    }

    if (!neutralAllowed && neutralGroupIndex >= 0) {
      IntSet neutralRoles = lookup.getSet(neutralGroupIndex);
      if (neutralRoles != null) {
        suppressedRoles.addAll(neutralRoles);
      }
    }

    if (suppressedRoles.isEmpty()) {
      return null;
    }

    return new SuppressionConfig(suppressedRoles);
  }

  /**
   * Applies suppression to a single chunk.
   */
  private void applySuppressionToChunk(
      @NotNull Long2ObjectConcurrentHashMap<ChunkSuppressionEntry> chunkMap,
      long chunkIndex,
      @NotNull UUID suppressorId,
      @NotNull SuppressionConfig config
  ) {
    ChunkSuppressionEntry.SuppressionSpan newSpan = new ChunkSuppressionEntry.SuppressionSpan(
      suppressorId,
      MIN_Y,
      MAX_Y,
      config.suppressedRoles
    );

    ChunkSuppressionEntry existingEntry = chunkMap.get(chunkIndex);

    List<ChunkSuppressionEntry.SuppressionSpan> spans;
    if (existingEntry != null) {
      // Add to existing spans
      spans = new ArrayList<>(existingEntry.getSuppressionSpans());
      spans.add(newSpan);
    } else {
      spans = List.of(newSpan);
    }

    chunkMap.put(chunkIndex, new ChunkSuppressionEntry(spans));
  }

  /**
   * Configuration for what mobs to suppress.
   *
   * @param suppressedRoles the set of NPC role indices to suppress, or null for all roles
   */
  private record SuppressionConfig(@Nullable IntSet suppressedRoles) {}
}
