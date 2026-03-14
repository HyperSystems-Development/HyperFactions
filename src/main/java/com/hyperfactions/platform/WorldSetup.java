package com.hyperfactions.platform;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.integration.protection.OrbisGuardIntegration;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import com.hyperfactions.worldmap.HyperFactionsWorldMapProvider;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.AddWorldEvent;
import com.hypixel.hytale.server.core.universe.world.events.RemoveWorldEvent;
import com.hypixel.hytale.server.core.universe.world.worldmap.provider.IWorldMapProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Handles world map provider registration, spawn suppression initialization,
 * and world add/remove event handling for HyperFactions.
 * Extracted from HyperFactionsPlugin to reduce class complexity.
 */
public class WorldSetup {

  private final HyperFactionsPlugin plugin;

  private final HyperFactions hyperFactions;

  /** Creates a new WorldSetup. */
  public WorldSetup(HyperFactionsPlugin plugin, HyperFactions hyperFactions) {
    this.plugin = plugin;
    this.hyperFactions = hyperFactions;
  }

  /**
   * Registers the world map provider CODEC with Hytale.
   * This allows our custom world map generator to be used.
   */
  public void registerWorldMapProvider() {
    try {
      IWorldMapProvider.CODEC.register(
          HyperFactionsWorldMapProvider.ID,
          HyperFactionsWorldMapProvider.class,
          HyperFactionsWorldMapProvider.CODEC
      );
      Logger.debug("Registered world map provider (ID: %s)", HyperFactionsWorldMapProvider.ID);
    } catch (Exception e) {
      plugin.getLogger().at(Level.WARNING).withCause(e).log("Failed to register world map provider");
    }
  }

  /**
   * Applies the world map provider to any worlds that were loaded before
   * our AddWorldEvent listener was registered.
   */
  public void applyToExistingWorlds() {
    if (!ConfigManager.get().isWorldMapMarkersEnabled()) {
      Logger.debug("World map markers disabled, skipping provider setup for existing worlds");
      return;
    }

    try {
      Map<String, World> worlds = Universe.get().getWorlds();
      Logger.debug("Checking %d existing worlds for world map provider setup", worlds.size());

      for (World world : worlds.values()) {
        try {
          // Skip temporary worlds
          if (world.getWorldConfig().isDeleteOnRemove()) {
            Logger.debug("Skipping temporary world: %s", world.getName());
            continue;
          }

          // Set our provider on WorldConfig so the server uses it during world map init
          world.getWorldConfig().setWorldMapProvider(
              new com.hyperfactions.worldmap.HyperFactionsWorldMapProvider());

        } catch (Exception e) {
          Logger.warn("Failed to set world map provider for world %s: %s",
              world.getName(), e.getMessage());
          ErrorHandler.report("Failed to set world map provider for world " + world.getName(), e);
        }
      }

      // Schedule delayed registration — WorldMapManager generators are initialized
      // AFTER plugin enable (during "Getting Hytale Universe ready"), so we need
      // to wait for them to be ready before capturing original settings
      hyperFactions.scheduleDelayedTask(60, () -> { // 60 ticks = 2 seconds
        Logger.debug("[WorldMap] Delayed registration: applying to existing worlds");
        for (World world : Universe.get().getWorlds().values()) {
          try {
            if (world.getWorldConfig().isDeleteOnRemove()) {
              continue;
            }
            hyperFactions.getWorldMapService().registerProviderIfNeeded(world);
          } catch (Exception e) {
            Logger.warn("Failed to register world map for world %s: %s",
                world.getName(), e.getMessage());
            ErrorHandler.report("Failed to register world map for world " + world.getName(), e);
          }
        }
        // Apply map player filters after registration
        hyperFactions.getMapPlayerFilterService().applyToAll();
      });

      // Also apply filters immediately for any already-online players
      hyperFactions.getMapPlayerFilterService().applyToAll();

    } catch (Exception e) {
      Logger.warn("Failed to apply world map provider to existing worlds: %s", e.getMessage());
      ErrorHandler.report("Failed to apply world map provider to existing worlds", e);
    }
  }

  /**
   * Initializes the spawn suppression manager and applies suppression to existing worlds.
   */
  public void initializeSpawnSuppression() {
    try {
      // Initialize the manager (resolves NPC groups)
      hyperFactions.getSpawnSuppressionManager().initialize();

      // Wire up zone change callback to re-apply suppression
      hyperFactions.getZoneManager().setOnZoneChangeCallback(affectedChunks -> {
        // Refresh world maps (respects configured refresh mode)
        // Pass affected chunks for optimized refresh, or null for full refresh
        hyperFactions.getWorldMapService().triggerFactionWideRefresh(affectedChunks);
        // Re-apply spawn suppression to all worlds
        applySpawnSuppressionToAllWorlds();
      });

      // Apply to existing worlds - some may not be ready yet during startup
      List<String> failedWorlds = applySpawnSuppressionToAllWorlds();

      if (!failedWorlds.isEmpty()) {
        // Schedule a retry for worlds that weren't ready
        Logger.debug("Scheduling spawn suppression retry for %d worlds: %s",
          failedWorlds.size(), String.join(", ", failedWorlds));

        hyperFactions.scheduleDelayedTask(60, () -> { // Retry after 60 ticks (3 seconds)
          Logger.debug("Retrying spawn suppression for worlds that weren't ready at startup");
          List<String> stillFailed = applySpawnSuppressionToAllWorlds();
          if (!stillFailed.isEmpty()) {
            Logger.warn("Some worlds still not ready for spawn suppression: %s (will be handled by AddWorldEvent)",
              String.join(", ", stillFailed));
          }
        });
      }

      Logger.info("[Startup] Spawn suppression initialized");
    } catch (Exception e) {
      ErrorHandler.report("Failed to initialize spawn suppression", e);
    }
  }

  /**
   * Applies spawn suppression to all loaded worlds.
   * Returns a list of world names that weren't ready and should be retried.
   */
  List<String> applySpawnSuppressionToAllWorlds() {
    List<String> failedWorlds = new ArrayList<>();
    try {
      Map<String, World> worlds = Universe.get().getWorlds();
      for (World world : worlds.values()) {
        boolean success = hyperFactions.getSpawnSuppressionManager().applyToWorld(world);
        if (!success) {
          failedWorlds.add(world.getName());
        }
      }
    } catch (Exception e) {
      Logger.warn("Failed to apply spawn suppression to worlds: %s", e.getMessage());
    }
    return failedWorlds;
  }

  /**
   * Initializes the zone mob clear manager.
   * Call this after initializeSpawnSuppression().
   */
  public void initializeMobClearing() {
    try {
      hyperFactions.getZoneMobClearManager().initialize();
      Logger.info("[Startup] Mob clearing initialized");
    } catch (Exception e) {
      plugin.getLogger().at(Level.WARNING).withCause(e).log("Failed to initialize mob clearing");
    }
  }

  /**
   * Handles world add event - registers world map provider.
   */
  public void onWorldAdd(AddWorldEvent event) {
    World world = event.getWorld();
    Logger.debug("AddWorldEvent received for world: %s", world.getName());
    try {
      // Skip temporary worlds
      if (world.getWorldConfig().isDeleteOnRemove()) {
        Logger.debug("Skipping world %s (temporary/delete-on-remove)", world.getName());
        return;
      }

      // Register world map provider (WorldMapService handles all setup)
      boolean worldMapEnabled = ConfigManager.get().isWorldMapMarkersEnabled();
      if (worldMapEnabled) {
        hyperFactions.getWorldMapService().registerProviderIfNeeded(world);
      }

      // Apply spawn suppression to the new world
      hyperFactions.getSpawnSuppressionManager().applyToWorld(world);
    } catch (Exception e) {
      plugin.getLogger().at(Level.WARNING).log("Error in AddWorldEvent handler for %s: %s",
          world.getName(), e.getMessage());
      ErrorHandler.report(String.format("AddWorldEvent error for %s", world.getName()), e);
    }
  }

  /**
   * Handles world remove event - cleanup.
   */
  public void onWorldRemove(RemoveWorldEvent event) {
    World world = event.getWorld();
    try {
      hyperFactions.getWorldMapService().unregisterProvider(world.getName());
    } catch (Exception e) {
      plugin.getLogger().at(Level.WARNING).log("Error in RemoveWorldEvent handler for %s: %s",
          world.getName(), e.getMessage());
    }
  }

  /**
   * Logs a concise summary of protection coverage at startup.
   */
  public void logProtectionCoverage() {
    var parts = new java.util.ArrayList<String>();
    parts.add("ECS events");
    parts.add("interaction codecs");

    // Mixin provider (HyperProtect-Mixin or OrbisGuard-Mixins)
    var provider = com.hyperfactions.integration.protection.ProtectionMixinBridge.getProvider();
    if (provider != com.hyperfactions.integration.protection.ProtectionMixinBridge.MixinProvider.NONE) {
      parts.add("mixin hooks (" + com.hyperfactions.integration.protection.ProtectionMixinBridge.getStatusSummary() + ")");
    }

    // OrbisGuard API (claim conflict detection — separate from mixin hooks)
    if (OrbisGuardIntegration.isAvailable()) {
      parts.add("OrbisGuard API");
    }

    // GravestonePlugin integration
    boolean gsAvailable = hyperFactions.getProtectionChecker().getGravestoneIntegration() != null
        && hyperFactions.getProtectionChecker().getGravestoneIntegration().isAvailable();
    if (gsAvailable) {
      parts.add("GravestonePlugin");
    }

    Logger.info("[Protection] Active: %s", String.join(", ", parts));
  }
}
