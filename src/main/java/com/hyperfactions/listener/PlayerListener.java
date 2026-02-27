package com.hyperfactions.listener;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.protection.ProtectionChecker;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.Logger;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Handles player-related events for HyperFactions.
 */
public class PlayerListener {

  private final HyperFactions hyperFactions;

  /** Creates a new PlayerListener. */
  public PlayerListener(@NotNull HyperFactions hyperFactions) {
    this.hyperFactions = hyperFactions;
  }

  /**
   * Called when a player takes damage from another player.
   * Handles combat tagging and PvP protection.
   *
   * @param attackerUuid the attacker's UUID
   * @param defenderUuid the defender's UUID
   * @param world        the world name
   * @param x            the location X
   * @param z            the location Z
   * @return true if damage should be cancelled
   */
  public boolean onPlayerDamage(@NotNull UUID attackerUuid, @NotNull UUID defenderUuid,
                 @NotNull String world, double x, double z) {
    ProtectionChecker checker = hyperFactions.getProtectionChecker();
    ProtectionChecker.PvPResult result = checker.canDamagePlayer(attackerUuid, defenderUuid, world, x, z);

    if (!checker.isAllowed(result)) {
      return true; // Cancel damage
    }

    // Combat tag both players
    hyperFactions.getCombatTagManager().tagCombat(attackerUuid, defenderUuid);

    return false; // Allow damage
  }

  /**
   * Called when a player dies.
   * Handles power loss.
   *
   * @param playerUuid the player's UUID
   */
  public void onPlayerDeath(@NotNull UUID playerUuid) {
    double newPower = hyperFactions.getPowerManager().applyDeathPenalty(playerUuid);
    Logger.debugPower("Player %s died, power now %.2f", playerUuid, newPower);
  }

  /**
   * Called when a player moves to a new chunk.
   * Displays territory info in action bar.
   *
   * @param playerUuid the player's UUID
   * @param world      the world name
   * @param oldChunkX  the old chunk X
   * @param oldChunkZ  the old chunk Z
   * @param newChunkX  the new chunk X
   * @param newChunkZ  the new chunk Z
   * @return the territory description for action bar, or null if same chunk
   */
  public String onChunkChange(@NotNull UUID playerUuid, @NotNull String world,
                int oldChunkX, int oldChunkZ, int newChunkX, int newChunkZ) {
    // Only trigger if chunk actually changed
    if (oldChunkX == newChunkX && oldChunkZ == newChunkZ) {
      return null;
    }

    // Get player's faction ID for relation coloring
    UUID playerFactionId = hyperFactions.getFactionManager().getPlayerFactionId(playerUuid);

    return hyperFactions.getProtectionChecker().getLocationDescription(
      world, newChunkX, newChunkZ, playerFactionId
    );
  }

  /**
   * Called when a player attempts to use a command while combat tagged.
   * Blocks certain commands during combat.
   *
   * @param playerUuid the player's UUID
   * @param command    the command being executed
   * @return true if command should be blocked
   */
  public boolean onCommandPreprocess(@NotNull UUID playerUuid, @NotNull String command) {
    if (!hyperFactions.getCombatTagManager().isTagged(playerUuid)) {
      return false;
    }

    // Block teleport commands during combat
    String lowerCommand = command.toLowerCase();
    if (lowerCommand.startsWith("/f home")
      || lowerCommand.startsWith("/faction home")
      || lowerCommand.startsWith("/home")
      || lowerCommand.startsWith("/spawn")
      || lowerCommand.startsWith("/tp")
      || lowerCommand.startsWith("/tpa")) {
      return true;
    }

    return false;
  }

  /**
   * Called when a player respawns.
   * Clears combat tag and applies spawn protection.
   *
   * @param playerUuid the player's UUID
   * @param world      the respawn world
   * @param x          the respawn X coordinate
   * @param z          the respawn Z coordinate
   */
  public void onPlayerRespawn(@NotNull UUID playerUuid, @NotNull String world, double x, double z) {
    // Clear combat tag
    hyperFactions.getCombatTagManager().clearTag(playerUuid);

    // Apply spawn protection if enabled
    ConfigManager config = ConfigManager.get();
    if (config.isSpawnProtectionEnabled()) {
      int chunkX = ChunkUtil.toChunkCoord(x);
      int chunkZ = ChunkUtil.toChunkCoord(z);
      int duration = config.getSpawnProtectionDurationSeconds();

      hyperFactions.getCombatTagManager().applySpawnProtection(
        playerUuid, duration, world, chunkX, chunkZ
      );
      Logger.debug("Applied %ds spawn protection to %s at chunk %d, %d",
        duration, playerUuid, chunkX, chunkZ);
    }
  }

  /**
   * Called when a player respawns (legacy version without location).
   * Clears combat tag on respawn.
   *
   * @param playerUuid the player's UUID
   * @deprecated Use {@link #onPlayerRespawn(UUID, String, double, double)} instead
   */
  @Deprecated(since = "0.9.0", forRemoval = true)
  public void onPlayerRespawn(@NotNull UUID playerUuid) {
    hyperFactions.getCombatTagManager().clearTag(playerUuid);
  }

  /**
   * Called when a player moves chunks.
   * Checks if spawn protection should be broken.
   *
   * @param playerUuid the player's UUID
   * @param world      the world name
   * @param chunkX     the new chunk X
   * @param chunkZ     the new chunk Z
   * @return true if spawn protection was broken
   */
  public boolean onChunkEnter(@NotNull UUID playerUuid, @NotNull String world, int chunkX, int chunkZ) {
    return hyperFactions.getCombatTagManager().checkSpawnProtectionMove(playerUuid, world, chunkX, chunkZ);
  }
}
