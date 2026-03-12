package com.hyperfactions.manager;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.data.ChunkKey;
import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneFlags;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.builtin.tagset.TagSetPlugin;
import com.hypixel.hytale.builtin.tagset.config.NPCGroup;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages periodic mob clearing for zones based on mob_clear flags.
 * Complements SpawnSuppressionManager by actively removing mobs that
 * already exist in zones (e.g., wandered in from outside, or present
 * before zone creation).
 *
 * <p>Uses the same NPCGroup resolution as SpawnSuppressionManager to
 * categorize mobs into hostile/passive/neutral groups.
 */
public class ZoneMobClearManager {

  private static final String GROUP_HOSTILE = "aggressive";
  private static final String GROUP_PASSIVE = "passive";
  private static final String GROUP_NEUTRAL = "neutral";

  private final ZoneManager zoneManager;
  private final HyperFactions hyperFactions;

  // Cache of NPC group indices (resolved once at startup)
  private int hostileGroupIndex = -1;
  private int passiveGroupIndex = -1;
  private int neutralGroupIndex = -1;
  private boolean groupsResolved = false;

  /** Creates a new ZoneMobClearManager. */
  public ZoneMobClearManager(@NotNull ZoneManager zoneManager, @NotNull HyperFactions hyperFactions) {
    this.zoneManager = zoneManager;
    this.hyperFactions = hyperFactions;
  }

  /**
   * Initializes the manager and resolves NPC group indices.
   * Call this after the TagSetPlugin has been initialized.
   */
  public void initialize() {
    resolveNPCGroups();
    Logger.info("[MobClear] Initialized - hostile=%d, passive=%d, neutral=%d",
      hostileGroupIndex, passiveGroupIndex, neutralGroupIndex);

    // Log all available NPCGroup names for diagnostics
    try {
      var assetMap = NPCGroup.getAssetMap();
      int nextIndex = assetMap.getNextIndex();
      StringBuilder sb = new StringBuilder("[MobClear] Available NPCGroups (");
      sb.append(nextIndex).append(" total): ");
      for (int i = 0; i < nextIndex; i++) {
        NPCGroup group = assetMap.getAsset(i);
        if (group != null) {
          if (i > 0) sb.append(", ");
          sb.append(group.getId()).append("(").append(i).append(")");
        }
      }
      Logger.info(sb.toString());

      TagSetPlugin.TagSetLookup lookup = TagSetPlugin.get(NPCGroup.class);
      if (hostileGroupIndex >= 0) {
        IntSet hostileRoles = lookup.getSet(hostileGroupIndex);
        Logger.info("[MobClear] Hostile group has %d roles", hostileRoles != null ? hostileRoles.size() : 0);
      }
      if (passiveGroupIndex >= 0) {
        IntSet passiveRoles = lookup.getSet(passiveGroupIndex);
        Logger.info("[MobClear] Passive group has %d roles", passiveRoles != null ? passiveRoles.size() : 0);
      }
      if (neutralGroupIndex >= 0) {
        IntSet neutralRoles = lookup.getSet(neutralGroupIndex);
        Logger.info("[MobClear] Neutral group has %d roles", neutralRoles != null ? neutralRoles.size() : 0);
      }
    } catch (Exception e) {
      ErrorHandler.report("[MobClear] Failed to log group info", e);
    }
  }

  private void resolveNPCGroups() {
    try {
      var assetMap = NPCGroup.getAssetMap();
      hostileGroupIndex = assetMap.getIndex(GROUP_HOSTILE);
      passiveGroupIndex = assetMap.getIndex(GROUP_PASSIVE);
      neutralGroupIndex = assetMap.getIndex(GROUP_NEUTRAL);
      groupsResolved = true;
    } catch (Exception e) {
      ErrorHandler.report("[MobClear] Failed to resolve NPC groups", e);
      groupsResolved = false;
    }
  }

  /**
   * Performs a sweep of all zones that have mob_clear enabled.
   * Called periodically by PeriodicTaskManager.
   */
  public void sweep() {
    if (!groupsResolved) {
      resolveNPCGroups();
      if (!groupsResolved) {
        return;
      }
    }

    // Pre-compute a map of chunkIndex -> ClearConfig for all zones with mob_clear=true
    Map<Long, ClearConfig> chunkConfigs = new Long2ObjectOpenHashMap<>();
    int zonesWithClearing = 0;

    for (Zone zone : zoneManager.getAllZones()) {
      if (!zone.getEffectiveFlag(ZoneFlags.MOB_CLEAR)) {
        continue;
      }

      ClearConfig config = getClearConfig(zone);
      if (config == null) {
        continue;
      }

      zonesWithClearing++;
      for (ChunkKey chunk : zone.chunks()) {
        long chunkIndex = ChunkUtil.indexChunk(chunk.chunkX(), chunk.chunkZ());
        // Merge configs if multiple zones overlap the same chunk
        ClearConfig existing = chunkConfigs.get(chunkIndex);
        if (existing != null) {
          chunkConfigs.put(chunkIndex, existing.merge(config));
        } else {
          chunkConfigs.put(chunkIndex, config);
        }
      }
    }

    if (zonesWithClearing == 0 || chunkConfigs.isEmpty()) {
      return; // Nothing to clear
    }

    // Dispatch sweep to each world
    try {
      Map<String, World> worlds = Universe.get().getWorlds();
      for (World world : worlds.values()) {
        sweepWorld(world, chunkConfigs);
      }
    } catch (Exception e) {
      Logger.warn("[MobClear] Sweep failed: %s", e.getMessage());
      ErrorHandler.report("[MobClear] Sweep failed", e);
    }
  }

  /**
   * Sweeps a single world, removing mobs in chunks that match the clear config.
   * Dispatches to the world thread via {@code world.execute()} since ECS operations
   * (forEachEntityParallel, removeEntity) must run on the owning world thread.
   */
  private void sweepWorld(@NotNull World world, @NotNull Map<Long, ClearConfig> chunkConfigs) {
    try {
    world.execute(() -> {
      EntityStore entityStoreHolder = world.getEntityStore();
      if (entityStoreHolder == null) {
        return;
      }

      Store<EntityStore> store = entityStoreHolder.getStore();
      if (store == null) {
        return;
      }

      int[] removed = {0};

      store.forEachEntityParallel(NPCEntity.getComponentType(),
        (index, archetypeChunk, commandBuffer) -> {
        // Get the NPC entity component
        NPCEntity npc = archetypeChunk.getComponent(index, NPCEntity.getComponentType());
        if (npc == null) {
          return;
        }

        // Get entity position to determine chunk
        TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        if (transform == null) {
          return;
        }

        double posX = transform.getPosition().getX();
        double posZ = transform.getPosition().getZ();
        long chunkIndex = ChunkUtil.indexChunkFromBlock((int) posX, (int) posZ);

        ClearConfig config = chunkConfigs.get(chunkIndex);
        if (config == null) {
          return; // Not in a clearing zone
        }

        // Check if this NPC's role should be cleared
        int roleIndex = npc.getRoleIndex();
        String group = resolveGroupName(roleIndex);
        if (config.shouldClear(roleIndex)) {
          Logger.debugSpawning("[MobClear] Clearing NPC role=%s (index=%d) group=%s at (%.1f, %.1f) in world '%s'",
            npc.getRoleName(), roleIndex, group, posX, posZ, world.getName());
          commandBuffer.removeEntity(archetypeChunk.getReferenceTo(index), RemoveReason.REMOVE);
          removed[0]++;
        } else {
          Logger.debugSpawning("[MobClear] SKIPPED NPC role=%s (index=%d) group=%s at (%.1f, %.1f) in world '%s' - not in clear set",
            npc.getRoleName(), roleIndex, group, posX, posZ, world.getName());
        }
      }
    );

    if (removed[0] > 0) {
      Logger.debug("[MobClear] Removed %d mobs from world '%s'", removed[0], world.getName());
    }
    });
    } catch (Exception e) {
      // World thread not accepting tasks (e.g., dungeon instances shutting down) — safe to skip
      Logger.debugSpawning("[MobClear] Skipping world '%s': %s", world.getName(), e.getMessage());
    }
  }

  /**
   * Resolves which group (aggressive/passive/neutral) a role index belongs to.
   */
  @NotNull
  private String resolveGroupName(int roleIndex) {
    try {
      TagSetPlugin.TagSetLookup lookup = TagSetPlugin.get(NPCGroup.class);
      if (hostileGroupIndex >= 0) {
        IntSet roles = lookup.getSet(hostileGroupIndex);
        if (roles != null && roles.contains(roleIndex)) return GROUP_HOSTILE;
      }
      if (passiveGroupIndex >= 0) {
        IntSet roles = lookup.getSet(passiveGroupIndex);
        if (roles != null && roles.contains(roleIndex)) return GROUP_PASSIVE;
      }
      if (neutralGroupIndex >= 0) {
        IntSet roles = lookup.getSet(neutralGroupIndex);
        if (roles != null && roles.contains(roleIndex)) return GROUP_NEUTRAL;
      }
    } catch (Exception e) {
      // Ignore lookup failures in diagnostics
    }
    return "unknown";
  }

  /**
   * Determines the clear configuration for a zone based on its effective flags.
   *
   * @param zone the zone
   * @return clear config, or null if no clearing needed
   */
  @Nullable
  private ClearConfig getClearConfig(@NotNull Zone zone) {
    boolean clearHostile = zone.getEffectiveFlag(ZoneFlags.HOSTILE_MOB_CLEAR);
    boolean clearPassive = zone.getEffectiveFlag(ZoneFlags.PASSIVE_MOB_CLEAR);
    boolean clearNeutral = zone.getEffectiveFlag(ZoneFlags.NEUTRAL_MOB_CLEAR);

    if (!clearHostile && !clearPassive && !clearNeutral) {
      return null;
    }

    // Build set of role indices to clear
    IntSet rolesToClear = new IntOpenHashSet();
    TagSetPlugin.TagSetLookup lookup = TagSetPlugin.get(NPCGroup.class);

    if (clearHostile && hostileGroupIndex >= 0) {
      IntSet roles = lookup.getSet(hostileGroupIndex);
      if (roles != null) {
        rolesToClear.addAll(roles);
      }
    }

    if (clearPassive && passiveGroupIndex >= 0) {
      IntSet roles = lookup.getSet(passiveGroupIndex);
      if (roles != null) {
        rolesToClear.addAll(roles);
      }
    }

    if (clearNeutral && neutralGroupIndex >= 0) {
      IntSet roles = lookup.getSet(neutralGroupIndex);
      if (roles != null) {
        rolesToClear.addAll(roles);
      }
    }

    if (rolesToClear.isEmpty()) {
      return null;
    }

    return new ClearConfig(rolesToClear);
  }

  /**
   * Configuration for what mobs to clear from a chunk.
   *
   * @param rolesToClear the set of NPC role indices to remove
   */
  private record ClearConfig(@NotNull IntSet rolesToClear) {

    /** Check if a given role index should be cleared. */
    boolean shouldClear(int roleIndex) {
      return rolesToClear.contains(roleIndex);
    }

    /** Merge two configs (union of roles to clear). */
    @NotNull
    ClearConfig merge(@NotNull ClearConfig other) {
      IntSet merged = new IntOpenHashSet(rolesToClear);
      merged.addAll(other.rolesToClear);
      return new ClearConfig(merged);
    }
  }
}
