package com.hyperfactions.command.territory;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.util.MessageKeys;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.manager.TeleportManager;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Subcommand: /f stuck
 * Teleports the player to the nearest safe chunk when stuck in enemy territory.
 * Uses extended warmup and is executed by TerritoryTickingSystem on the world thread.
 */
public class StuckSubCommand extends FactionSubCommand {

  /** Creates a new StuckSubCommand. */
  public StuckSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("stuck", "Escape from enemy territory", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.STUCK)) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Home.STUCK_NO_PERMISSION));
      return;
    }

    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    if (transform == null) {
      return;
    }

    Vector3d pos = transform.getPosition();
    UUID playerUuid = player.getUuid();

    int chunkX = ChunkUtil.toChunkCoord(pos.getX());
    int chunkZ = ChunkUtil.toChunkCoord(pos.getZ());

    // Check if in claimed territory (any faction's claims)
    UUID claimOwner = hyperFactions.getClaimManager().getClaimOwner(currentWorld.getName(), chunkX, chunkZ);
    Faction playerFaction = hyperFactions.getFactionManager().getPlayerFaction(playerUuid);

    if (claimOwner == null) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Home.STUCK_NOT_STUCK));
      return;
    }

    // Combat check
    if (hyperFactions.getCombatTagManager().isTagged(playerUuid)) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Home.STUCK_COMBAT_TAGGED));
      return;
    }

    // Find nearest safe chunk
    int[] safeChunk = findNearestSafeChunk(currentWorld.getName(), chunkX, chunkZ);
    if (safeChunk == null) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Home.STUCK_NO_SAFE));
      return;
    }

    // Create teleport location (center of safe chunk, fitted to safe ground)
    double targetX = ChunkUtil.chunkToCenter(safeChunk[0]);
    double targetZ = ChunkUtil.chunkToCenter(safeChunk[1]);
    double targetY = findSafeY(currentWorld, targetX, targetZ, pos.getY());

    // Use extended warmup for stuck (30 seconds by default)
    int warmupSeconds = ConfigManager.get().getStuckWarmupSeconds();

    // Create start location for movement checking
    TeleportManager.StartLocation startLoc = new TeleportManager.StartLocation(
      currentWorld.getName(), pos.getX(), pos.getY(), pos.getZ()
    );

    // Create destination
    TeleportManager.TeleportDestination destination = new TeleportManager.TeleportDestination(
      currentWorld.getName(), targetX, targetY, targetZ, 0, 0
    );

    // Schedule teleport - will be executed by TerritoryTickingSystem on the world thread
    hyperFactions.getTeleportManager().scheduleTeleport(
      playerUuid,
      startLoc,
      destination,
      warmupSeconds,
      () -> hyperFactions.getCombatTagManager().isTagged(playerUuid),
      "Teleported to safety!"
    );

    ctx.sendMessage(MessageUtil.info(player, MessageKeys.Home.STUCK_TELEPORTING, COLOR_YELLOW, warmupSeconds));
  }

  /**
   * Finds a random unclaimed (wilderness) chunk by picking random directions and
   * walking outward from the player until finding unclaimed land.
   *
   * <p>
   * Algorithm:
   * 1. Pick a random direction (angle) and walk outward chunk-by-chunk starting at minRadius
   * 2. Return the first unclaimed chunk along that ray
   * 3. If the ray hits maxRadius without finding one, try another random direction
   * 4. Repeat up to maxAttempts random directions
   *
   * <p>
   * This ensures directional randomness — the player won't always end up on the same side.
   *
   * <p>
   * Config: stuck.minRadius (default 3), stuck.radiusIncrease (default 1), stuck.maxAttempts (default 10)
   */
  @Nullable
  private int[] findNearestSafeChunk(String world, int startX, int startZ) {
    ConfigManager config = ConfigManager.get();
    int minRadius = config.getStuckMinRadius();
    int radiusStep = config.getStuckRadiusIncrease();
    int maxAttempts = config.getStuckMaxAttempts();
    int maxRadius = minRadius + (radiusStep * 30); // hard cap to avoid infinite walk

    var random = java.util.concurrent.ThreadLocalRandom.current();

    Logger.debugTerritory("Stuck search: start=(%d,%d), minRadius=%d, step=%d, maxAttempts=%d, maxRadius=%d",
      startX, startZ, minRadius, radiusStep, maxAttempts, maxRadius);

    for (int attempt = 0; attempt < maxAttempts; attempt++) {
      // Pick a random direction
      double angle = random.nextDouble() * 2 * Math.PI;
      double dirX = Math.cos(angle);
      double dirZ = Math.sin(angle);
      String compassDir = compassDirection(angle);

      Logger.debugTerritory("Stuck attempt %d: angle=%.1f° (%s), dir=(%.2f, %.2f)",
        attempt, Math.toDegrees(angle), compassDir, dirX, dirZ);

      // Walk outward along this direction
      for (int dist = minRadius; dist <= maxRadius; dist += radiusStep) {
        int checkX = startX + (int) Math.round(dirX * dist);
        int checkZ = startZ + (int) Math.round(dirZ * dist);

        UUID owner = hyperFactions.getClaimManager().getClaimOwner(world, checkX, checkZ);
        if (owner == null) {
          Logger.debugTerritory("Stuck found wilderness at (%d,%d), dist=%d, direction=%s",
            checkX, checkZ, dist, compassDir);
          return new int[]{checkX, checkZ};
        } else {
          // Log each claimed chunk we skip (helps debug claim layout)
          String factionName = "unknown";
          var faction = hyperFactions.getFactionManager().getFaction(owner);
          if (faction != null) {
            factionName = faction.name();
          }
          Logger.debugTerritory("Stuck skip (%d,%d) dist=%d: claimed by %s", checkX, checkZ, dist, factionName);
        }
      }

      Logger.debugTerritory("Stuck attempt %d: no wilderness found along %s within %d chunks",
        attempt, compassDir, maxRadius);
    }

    Logger.debugTerritory("Stuck search FAILED: no wilderness found in any direction");
    return null;
  }

  /**
   * Finds a safe Y coordinate at the given X/Z position.
   * Scans down from the heightmap looking for a solid block with 2 empty blocks above it
   * (enough room for the player to stand). Avoids tree canopies and underground caves.
   *
   * @param world   the world to check
   * @param x       block X coordinate
   * @param z       block Z coordinate
   * @param fallbackY fallback Y if chunk isn't loaded
   * @return safe Y coordinate to teleport the player to
   */
  private double findSafeY(World world, double x, double z, double fallbackY) {
    int blockX = com.hypixel.hytale.math.util.MathUtil.floor(x);
    int blockZ = com.hypixel.hytale.math.util.MathUtil.floor(z);

    long chunkIndex = com.hypixel.hytale.math.util.ChunkUtil.indexChunkFromBlock(x, z);
    WorldChunk chunk = world.getNonTickingChunk(chunkIndex);
    if (chunk == null) {
      return fallbackY;
    }

    // Start from heightmap and scan downward
    int topY = chunk.getHeight(blockX, blockZ);

    for (int y = topY; y > 0; y--) {
      BlockType blockBelow = world.getBlockType(blockX, y, blockZ);
      BlockType blockAt = world.getBlockType(blockX, y + 1, blockZ);
      BlockType blockAbove = world.getBlockType(blockX, y + 2, blockZ);

      // Need: solid ground, 2 empty blocks above for player
      boolean groundSolid = blockBelow != null && blockBelow.getMaterial() == BlockMaterial.Solid;
      boolean feetClear = blockAt == null || blockAt.getMaterial() == BlockMaterial.Empty;
      boolean headClear = blockAbove == null || blockAbove.getMaterial() == BlockMaterial.Empty;

      if (groundSolid && feetClear && headClear) {
        return y + 1;
      }
    }

    // No safe position found — use heightmap + 1 as last resort
    return topY + 1;
  }

  /**
   * Converts an angle (radians) to a compass direction string for debug logging.
   * Hytale: +X = East, +Z = South (same as Minecraft).
   */
  private static String compassDirection(double angle) {
    // Normalize to 0-360
    double deg = Math.toDegrees(angle) % 360;
    if (deg < 0) {
      deg += 360;
    }

    // cos(angle) = X direction, sin(angle) = Z direction
    // 0° = East (+X), 90° = South (+Z), 180° = West (-X), 270° = North (-Z)
    if (deg < 22.5 || deg >= 337.5) {
      return "E";
    }
    if (deg < 67.5) {
      return "SE";
    }
    if (deg < 112.5) {
      return "S";
    }
    if (deg < 157.5) {
      return "SW";
    }
    if (deg < 202.5) {
      return "W";
    }
    if (deg < 247.5) {
      return "NW";
    }
    if (deg < 292.5) {
      return "N";
    }
    return "NE";
  }
}
