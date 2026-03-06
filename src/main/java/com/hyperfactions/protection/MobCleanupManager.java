package com.hyperfactions.protection;

import com.hyperfactions.data.Faction;
import com.hyperfactions.manager.ClaimManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Optional mob cleanup system for removing hostile mobs from claimed territory.
 *
 * <p>Addresses the race condition where mobs can spawn before protection hooks register,
 * or mobs can follow players into claimed territory. Works entirely via Hytale's
 * plugin entity APIs (no mixin needed).
 *
 * <p>All cleanup operations are gated by both global config and per-faction flags.
 * This class is a foundation — actual entity iteration and removal will be implemented
 * when the Hytale entity cleanup APIs are verified in testing.
 *
 * <p>Configuration (future, in config.yml under protection.mob-cleanup):
 * <pre>
 * protection:
 *   mob-cleanup:
 *     enabled: false                  # Master switch — disabled by default
 *     startup-sweep: true             # Clean claimed chunks after hooks register
 *     on-claim: true                  # Clean chunk when newly claimed
 * </pre>
 */
public class MobCleanupManager {

  private final FactionManager factionManager;
  private final ClaimManager claimManager;

  private volatile boolean enabled = false;

  public MobCleanupManager(
      @NotNull FactionManager factionManager,
      @NotNull ClaimManager claimManager
  ) {
    this.factionManager = factionManager;
    this.claimManager = claimManager;
  }

  /**
   * Called after protection hooks register on startup.
   * Sweeps all claimed chunks if startup-sweep is enabled.
   */
  public void startupSweep() {
    if (!enabled) {
      return;
    }

    Logger.debug("[MobCleanup] Starting post-registration sweep of claimed chunks...");

    // TODO: Implement actual entity cleanup when Hytale's Store.forEachChunk() API
    // and entity removal via CommandBuffer are verified in testing.
    // Approach:
    // 1. Iterate all claimed chunks via ClaimManager
    // 2. For each chunk, query entities using Store.forEachChunk(Query, BiConsumer)
    // 3. Filter out players, keep hostile mobs
    // 4. Remove via CommandBuffer.removeEntity(ref, RemoveReason.REMOVE)
  }

  /**
   * Called when a faction claims a new chunk.
   * Cleans mobs from the newly claimed chunk if on-claim cleanup is enabled.
   *
   * @param faction the faction claiming the chunk
   * @param worldName the world name
   * @param chunkX the chunk X coordinate
   * @param chunkZ the chunk Z coordinate
   */
  public void onClaimCreated(@NotNull Faction faction, @NotNull String worldName,
                int chunkX, int chunkZ) {
    if (!enabled) return;

    Logger.debug("[MobCleanup] Cleanup requested for newly claimed chunk (%d,%d) by faction %s",
      chunkX, chunkZ, faction.name());

    // TODO: Implement actual entity cleanup
  }

  /**
   * Enables or disables the mob cleanup system.
   *
   * @param enabled true to enable
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    Logger.debug("[MobCleanup] %s", enabled ? "Enabled" : "Disabled");
  }

  /**
   * Whether the mob cleanup system is currently enabled.
   */
  public boolean isEnabled() {
    return enabled;
  }
}
