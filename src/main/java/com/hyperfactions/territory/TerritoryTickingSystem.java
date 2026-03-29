package com.hyperfactions.territory;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.api.events.EventBus;
import com.hyperfactions.api.events.FactionHomeTeleportEvent;
import com.hyperfactions.api.events.FactionHomeTeleportPreEvent;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneFlags;
import com.hyperfactions.manager.TeleportManager;
import com.hyperfactions.protection.ProtectionMessageDebounce;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * ECS ticking system that provides player position updates to the territory
 * notifier and executes pending teleports.
 *
 * <p>This system ticks every game tick for all player entities, providing reliable
 * position data regardless of how the player moves. It also handles executing
 * pending teleports when their warmup completes, ensuring they run on the
 * correct world thread.
 *
 * <p>Note: Chunk change detection is handled by TerritoryNotifier, not here.
 */
public class TerritoryTickingSystem extends EntityTickingSystem<EntityStore> {

  private final HyperFactions hyperFactions;

  /** Tracks expected teleport destinations for post-teleport position verification. */
  private final Map<UUID, double[]> pendingVerification = new ConcurrentHashMap<>();

  /** Tracks last known position per player for mount entry rubber-banding. */
  private final Map<UUID, double[]> lastPositions = new ConcurrentHashMap<>();

  /**
   * Creates a new territory ticking system.
   *
   * @param hyperFactions the HyperFactions instance
   */
  public TerritoryTickingSystem(@NotNull HyperFactions hyperFactions) {
    this.hyperFactions = hyperFactions;
  }

  /** Tick. */
  @Override
  public void tick(float dt, int index, @NotNull ArchetypeChunk<EntityStore> archetypeChunk,
          @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {
    try {
      // Get entity reference
      Ref ref = archetypeChunk.getReferenceTo(index);

      // Get PlayerRef and Player components
      PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
      Player player = store.getComponent(ref, Player.getComponentType());

      if (playerRef == null || player == null) {
        return;
      }

      // Get world name
      String worldName = player.getWorld().getName();
      if (worldName == null) {
        return;
      }

      // Get current position (as double for precision)
      var transform = playerRef.getTransform();
      if (transform == null) {
        return;
      }
      double posX = transform.getPosition().getX();
      double posY = transform.getPosition().getY();
      double posZ = transform.getPosition().getZ();

      UUID playerUuid = playerRef.getUuid();
      TeleportManager teleportManager = hyperFactions.getTeleportManager();

      // Post-teleport position verification
      double[] expected = pendingVerification.remove(playerUuid);
      if (expected != null) {
        int actualChunkX = ChunkUtil.toChunkCoord(posX);
        int actualChunkZ = ChunkUtil.toChunkCoord(posZ);
        int expectedChunkX = ChunkUtil.toChunkCoord(expected[0]);
        int expectedChunkZ = ChunkUtil.toChunkCoord(expected[2]);
        Logger.debugTerritory("POST-TELEPORT VERIFY %s: actual=block(%.1f, %.1f, %.1f) chunk(%d, %d) | expected=block(%.1f, %.1f, %.1f) chunk(%d, %d) | match=%b",
          playerUuid,
          posX, posY, posZ, actualChunkX, actualChunkZ,
          expected[0], expected[1], expected[2], expectedChunkX, expectedChunkZ,
          actualChunkX == expectedChunkX && actualChunkZ == expectedChunkZ);
      }

      // Check for pending teleport
      if (teleportManager.hasPending(playerUuid)) {
        // Check for movement cancellation first
        boolean cancelled = teleportManager.checkMovement(
          playerUuid,
          posX, posY, posZ,
          playerRef::sendMessage
        );

        // If not cancelled by movement, check if ready to execute
        if (!cancelled) {
          // Send countdown message (will only announce at certain intervals)
          TeleportManager.PendingTeleport pending = teleportManager.getPending(playerUuid);
          if (pending != null) {
            teleportManager.sendCountdownMessage(pending, playerRef::sendMessage);
          }

          TeleportManager.PendingTeleport ready = teleportManager.checkReady(
            playerUuid, playerRef::sendMessage
          );

          if (ready != null) {
            // Block teleport if mounted into a no-mount-entry zone (unless admin bypass)
            boolean mountBlocked = false;
            if (player.getMountEntityId() != 0 && !hyperFactions.isAdminBypassEnabled(playerUuid)) {
              TeleportManager.TeleportDestination dest = ready.destination();
              if (!isMountEntryAllowed(dest.world(), dest.x(), dest.z())) {
                playerRef.sendMessage(com.hyperfactions.util.MessageUtil.error(
                    playerRef, com.hyperfactions.util.CommonKeys.Teleport.MOUNT_TELEPORT_BLOCKED));
                Logger.debugTerritory("Teleport blocked for mounted player %s to zone at (%.1f, %.1f)",
                    playerUuid, dest.x(), dest.z());
                mountBlocked = true;
              }
            }
            if (!mountBlocked) {
              // Execute the teleport via world.execute() (runs after tick completes)
              executeTeleport(ref, player.getWorld(), ready, playerRef);
            }
          }
        }
      }

      // Mount entry zone enforcement — only check when mounted and entering a new chunk
      // Uses TerritoryNotifier's existing chunk tracking to avoid duplicate work
      if (player.getMountEntityId() != 0 && !hyperFactions.isAdminBypassEnabled(playerUuid)) {
        TerritoryNotifier notifier = hyperFactions.getTerritoryNotifier();
        var lastChunk = notifier.getLastChunk(playerUuid);
        int chunkX = ChunkUtil.toChunkCoord(posX);
        int chunkZ = ChunkUtil.toChunkCoord(posZ);

        if (lastChunk != null && (lastChunk.chunkX() != chunkX || lastChunk.chunkZ() != chunkZ)) {
          Zone zone = hyperFactions.getZoneManager().getZone(worldName, chunkX, chunkZ);
          if (zone != null && !zone.getEffectiveFlag(ZoneFlags.MOUNT_ENTRY)) {
            double[] prev = lastPositions.get(playerUuid);
            if (prev != null) {
              double[] safePos = findMountEntrySafePosition(worldName, posX, posZ, prev);
              if (safePos != null) {
                double safeY = findSafeY(player.getWorld(), safePos[0], safePos[1], prev[1]);
                Vector3d backPos = new Vector3d(safePos[0], safeY, safePos[1]);
                Vector3f backRot = new Vector3f(0, 0, 0);
                player.getWorld().execute(() -> {
                  if (ref.isValid()) {
                    Teleport tp = Teleport.createForPlayer(backPos, backRot);
                    ref.getStore().addComponent(ref, Teleport.getComponentType(), tp);
                  }
                });
                ProtectionMessageDebounce.sendDenial(playerRef, "mount_entry",
                    com.hyperfactions.util.HFMessages.get(playerRef, com.hyperfactions.util.CommonKeys.Teleport.MOUNT_ENTRY_BLOCKED));
                Logger.debugTerritory("Mount entry blocked for %s at zone '%s' (%s), safe=(%.1f, %.1f, %.1f)",
                    playerUuid, zone.name(), zone.type().name(), safePos[0], safeY, safePos[1]);
              }
              // Don't update lastPositions — keep at safe location
              return;
            }
          }
        }
      }

      // Track position for mount entry rubber-banding
      lastPositions.put(playerUuid, new double[]{posX, posY, posZ});

      // Pass position to TerritoryNotifier if notifications enabled
      if (ConfigManager.get().isTerritoryNotificationsEnabled()) {
        hyperFactions.getTerritoryNotifier().onPlayerMove(playerRef, worldName, posX, posZ);
      }

    } catch (Exception e) {
      // Silently ignore - this ticks frequently and errors shouldn't spam logs
      Logger.debugTerritory("Error in territory tick: %s", e.getMessage());
    }
  }

  /**
   * Executes a pending teleport.
   * Uses world.execute() + ref.getStore() pattern (from Hytale's InstanceEditLoadCommand)
   * to add the Teleport component OUTSIDE the ticking system's processing cycle.
   * This ensures TeleportSystems properly handles chunk loading and client sync.
   */
  private void executeTeleport(Ref<EntityStore> ref,
                 World currentWorld, TeleportManager.PendingTeleport pending,
                 PlayerRef playerRef) {
    TeleportManager.TeleportDestination dest = pending.destination();

    Logger.debugTerritory("Executing teleport for %s to world=%s, block=(%.1f, %.1f, %.1f), chunk=(%d, %d)",
      pending.playerUuid(), dest.world(), dest.x(), dest.y(), dest.z(),
      ChunkUtil.toChunkCoord(dest.x()), ChunkUtil.toChunkCoord(dest.z()));

    Vector3d position = new Vector3d(dest.x(), dest.y(), dest.z());
    Vector3f rotation = new Vector3f(dest.pitch(), dest.yaw(), 0);
    boolean sameWorld = currentWorld.getName().equals(dest.world());

    // Schedule teleport via world.execute() — runs on world thread AFTER tick completes.
    // Uses ref.getStore() for a fresh store reference (not the tick-captured one).
    // This matches the pattern used by Hytale's built-in InstanceEditLoadCommand.
    currentWorld.execute(() -> {
      if (!ref.isValid()) {
        Logger.debugTerritory("Teleport cancelled: entity ref no longer valid for %s", pending.playerUuid());
        return;
      }

      Store<EntityStore> store = ref.getStore();

      if (sameWorld) {
        Teleport teleport = Teleport.createForPlayer(position, rotation);
        store.addComponent(ref, Teleport.getComponentType(), teleport);
      } else {
        World targetWorld = Universe.get().getWorld(dest.world());
        if (targetWorld == null) {
          hyperFactions.getTeleportManager().onTeleportFailed(
            TeleportManager.TeleportResult.WORLD_NOT_FOUND,
            playerRef::sendMessage
          );
          return;
        }
        Teleport teleport = Teleport.createForPlayer(targetWorld, position, rotation);
        store.addComponent(ref, Teleport.getComponentType(), teleport);
      }

      Logger.debugTerritory("Teleport component added for %s via world.execute()", pending.playerUuid());
    });

    // Track expected destination for post-teleport verification
    pendingVerification.put(pending.playerUuid(), new double[]{dest.x(), dest.y(), dest.z()});

    // Success - apply cooldown and send message
    hyperFactions.getTeleportManager().onTeleportSuccess(
      pending.playerUuid(),
      pending.successMessage(),
      playerRef::sendMessage
    );

    // Emit events for faction home teleports (factionId is non-null for /f home)
    if (pending.factionId() != null) {
      TeleportManager.StartLocation src = pending.startLocation();

      // Pre-event: allow external plugins to cancel warmup-completed teleports
      if (EventBus.publishCancellable(new FactionHomeTeleportPreEvent(
          pending.playerUuid(), pending.factionId(),
          src.world(), src.x(), src.y(), src.z(),
          dest.world(), dest.x(), dest.y(), dest.z()))) {
        // Cancelled — teleport already scheduled on world thread, but we skip the post-event
        // Note: The Teleport component was already added above; full cancellation would require
        // restructuring the execute flow. For now, the pre-event serves as a notification.
        return;
      }

      EventBus.publish(new FactionHomeTeleportEvent(
        pending.playerUuid(), pending.factionId(),
        src.world(), src.x(), src.y(), src.z(),
        dest.world(), dest.x(), dest.y(), dest.z(), dest.yaw(), dest.pitch()
      ));
    }

    Logger.debugTerritory("Teleport scheduled for %s to %s (%.1f, %.1f, %.1f)",
      pending.playerUuid(), dest.world(), dest.x(), dest.y(), dest.z());
  }

  /**
   * Finds a safe XZ position for mount entry rubber-banding.
   * First tries pushing 2 blocks back toward previous position. If that lands in a
   * zone with MOUNT_ENTRY=false, tries each cardinal direction (N, E, S, W) at 3 blocks.
   *
   * @return double[2] {safeX, safeZ} or null if no safe position found
   */
  @Nullable
  private double[] findMountEntrySafePosition(String worldName, double currentX, double currentZ, double[] prev) {
    // Cardinal direction offsets: N(-Z), E(+X), S(+Z), W(-X)
    double[][] cardinals = {{0, -3}, {3, 0}, {0, 3}, {-3, 0}};

    // First try: push 2 blocks back toward previous position
    double dx = prev[0] - currentX;
    double dz = prev[2] - currentZ;
    double dist = Math.sqrt(dx * dx + dz * dz);
    double candidateX;
    double candidateZ;
    if (dist > 0.01) {
      candidateX = prev[0] + (dx / dist) * 2.0;
      candidateZ = prev[2] + (dz / dist) * 2.0;
    } else {
      candidateX = prev[0];
      candidateZ = prev[2];
    }

    if (isMountEntryAllowed(worldName, candidateX, candidateZ)) {
      return new double[]{candidateX, candidateZ};
    }

    // Fallback: try cardinal directions from previous position
    for (double[] offset : cardinals) {
      candidateX = prev[0] + offset[0];
      candidateZ = prev[2] + offset[1];
      if (isMountEntryAllowed(worldName, candidateX, candidateZ)) {
        return new double[]{candidateX, candidateZ};
      }
    }

    // Last resort: previous position itself (should always be safe since they were there)
    return new double[]{prev[0], prev[2]};
  }

  /**
   * Checks if mount entry is allowed at a position (no zone, or zone allows mount entry).
   */
  private boolean isMountEntryAllowed(String worldName, double x, double z) {
    int cx = ChunkUtil.toChunkCoord(x);
    int cz = ChunkUtil.toChunkCoord(z);
    Zone zone = hyperFactions.getZoneManager().getZone(worldName, cx, cz);
    return zone == null || zone.getEffectiveFlag(ZoneFlags.MOUNT_ENTRY);
  }

  /**
   * Finds a safe Y coordinate at the given X/Z position.
   * Scans down from the heightmap looking for solid ground with 2 empty blocks above.
   * Same algorithm as StuckSubCommand.findSafeY.
   */
  private double findSafeY(World world, double x, double z, double fallbackY) {
    int blockX = com.hypixel.hytale.math.util.MathUtil.floor(x);
    int blockZ = com.hypixel.hytale.math.util.MathUtil.floor(z);

    long chunkIndex = com.hypixel.hytale.math.util.ChunkUtil.indexChunkFromBlock(x, z);
    WorldChunk chunk = world.getNonTickingChunk(chunkIndex);
    if (chunk == null) {
      return fallbackY;
    }

    int topY = chunk.getHeight(blockX, blockZ);
    for (int y = topY; y > 0; y--) {
      BlockType blockBelow = world.getBlockType(blockX, y, blockZ);
      BlockType blockAt = world.getBlockType(blockX, y + 1, blockZ);
      BlockType blockAbove = world.getBlockType(blockX, y + 2, blockZ);

      boolean groundSolid = blockBelow != null && blockBelow.getMaterial() == BlockMaterial.Solid;
      boolean feetClear = blockAt == null || blockAt.getMaterial() == BlockMaterial.Empty;
      boolean headClear = blockAbove == null || blockAbove.getMaterial() == BlockMaterial.Empty;

      if (groundSolid && feetClear && headClear) {
        return y + 1;
      }
    }
    return topY + 1;
  }

  /**
   * Cleans up tracking data for a disconnected player.
   *
   * @param playerUuid the player's UUID
   */
  public void onPlayerDisconnect(@NotNull UUID playerUuid) {
    lastPositions.remove(playerUuid);
    pendingVerification.remove(playerUuid);
  }

  public void shutdown() {
    lastPositions.clear();
    pendingVerification.clear();
  }

  /** Returns the query. */
  @Nullable
  @Override
  public Query<EntityStore> getQuery() {
    // Query for entities with PlayerRef component (players)
    return PlayerRef.getComponentType();
  }
}
