package com.hyperfactions.platform;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.integration.protection.OrbisGuardIntegration;
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
            plugin.getLogger().at(Level.INFO).log("Registered HyperFactions world map provider (ID: %s)",
                    HyperFactionsWorldMapProvider.ID);
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

                    // Set our world map generator directly on the WorldMapManager
                    // This is critical - setWorldMapProvider() only affects future loads,
                    // but setGenerator() updates the live WorldMapManager
                    world.getWorldMapManager().setGenerator(
                            com.hyperfactions.worldmap.HyperFactionsWorldMap.INSTANCE);
                    Logger.debug("Applied HyperFactions world map generator to existing world: %s", world.getName());

                    // Also register with WorldMapService to track it
                    hyperFactions.getWorldMapService().registerProviderIfNeeded(world);

                } catch (Exception e) {
                    Logger.warn("Failed to apply world map provider to world %s: %s",
                            world.getName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            Logger.warn("Failed to apply world map provider to existing worlds: %s", e.getMessage());
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

            plugin.getLogger().at(Level.INFO).log("Spawn suppression initialized");
        } catch (Exception e) {
            plugin.getLogger().at(Level.WARNING).withCause(e).log("Failed to initialize spawn suppression");
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

            // Register our world map provider for this world
            boolean worldMapEnabled = ConfigManager.get().isWorldMapMarkersEnabled();
            Logger.debug("World map markers enabled: %s for world: %s", worldMapEnabled, world.getName());

            if (worldMapEnabled) {
                HyperFactionsWorldMapProvider provider = new HyperFactionsWorldMapProvider();
                world.getWorldConfig().setWorldMapProvider((IWorldMapProvider) provider);
                Logger.debug("World map provider set successfully for: %s (provider class: %s)",
                        world.getName(), provider.getClass().getName());
            }

            // Track the world in WorldMapService
            hyperFactions.getWorldMapService().registerProviderIfNeeded(world);

            // Apply spawn suppression to the new world
            hyperFactions.getSpawnSuppressionManager().applyToWorld(world);
        } catch (Exception e) {
            plugin.getLogger().at(Level.WARNING).log("Error in AddWorldEvent handler for %s: %s",
                    world.getName(), e.getMessage());
            Logger.severe("AddWorldEvent error for %s", e, world.getName());
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
     * Logs a summary of protection coverage at startup.
     * Informs admins which protections are active and which require additional plugins.
     */
    public void logProtectionCoverage() {
        boolean orbisGuardAvailable = OrbisGuardIntegration.isAvailable();

        plugin.getLogger().at(Level.INFO).log("=== HyperFactions Protection Coverage ===");
        plugin.getLogger().at(Level.INFO).log("ECS Events (native): Block break/place, Use, Harvest drops, Damage - ENABLED");
        plugin.getLogger().at(Level.INFO).log("Interaction Codecs: Fluid place/pickup protection - ENABLED");
        plugin.getLogger().at(Level.INFO).log("Mixin Hooks (registered): F-key pickup, Auto pickup, NPC Spawn control");
        plugin.getLogger().at(Level.INFO).log("  -> Requires Hyxin + OrbisGuard-Mixins in earlyplugins/ to activate");

        if (orbisGuardAvailable) {
            plugin.getLogger().at(Level.INFO).log("OrbisGuard API: Claim conflict detection - ENABLED");
        } else {
            plugin.getLogger().at(Level.INFO).log("OrbisGuard: Not detected (optional)");
        }

        boolean gsAvailable = hyperFactions.getProtectionChecker().getGravestoneIntegration() != null
                && hyperFactions.getProtectionChecker().getGravestoneIntegration().isAvailable();
        if (gsAvailable) {
            plugin.getLogger().at(Level.INFO).log("GravestonePlugin: v2 API — AccessChecker + events - ENABLED");
        } else {
            plugin.getLogger().at(Level.INFO).log("GravestonePlugin: Not detected (optional)");
        }

        plugin.getLogger().at(Level.INFO).log("=========================================");
    }
}
