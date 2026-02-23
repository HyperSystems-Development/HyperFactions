package com.hyperfactions.gui.admin.page;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.*;
import com.hyperfactions.gui.ActivePageTracker;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.RefreshablePage;
import com.hyperfactions.gui.admin.data.AdminZoneMapData;
import com.hyperfactions.gui.faction.ChunkMapAsset;
import com.hyperfactions.integration.protection.OrbisGuardIntegration;
import com.hyperfactions.manager.ClaimManager;
import com.hyperfactions.manager.ZoneManager;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Admin Zone Map page - interactive map for claiming/unclaiming chunks for a specific zone.
 * Left-click unclaimed to claim for zone, right-click zone chunk to unclaim.
 */
public class AdminZoneMapPage extends InteractiveCustomUIPage<AdminZoneMapData> implements RefreshablePage {

    // Flat mode grid (29x17 at 16px)
    private static final int GRID_RADIUS_X = 14; // 29 columns (-14 to +14)
    private static final int GRID_RADIUS_Z = 8;  // 17 rows (-8 to +8)
    private static final int CELL_SIZE = 16;     // pixels per cell

    // Terrain mode grid (17x17 at 32px, matching ChunkWorldMap pixel size)
    private static final int TERRAIN_GRID_RADIUS = 8; // 17 cells (-8 to +8)

    // Color constants - opaque, for flat mode
    private static final String COLOR_CURRENT_SAFE = "#14b8a6";     // Bright teal - current zone (SafeZone)
    private static final String COLOR_CURRENT_WAR = "#c084fc";      // Purple - current zone (WarZone)
    private static final String COLOR_OTHER_SAFE = "#2dd4bf80";     // Light teal - other SafeZone
    private static final String COLOR_OTHER_WAR = "#c084fc80";      // Light purple - other WarZone
    private static final String COLOR_FACTION = "#6b7280";          // Gray - faction claims
    private static final String COLOR_WILDERNESS = "#1e293b";       // Dark slate - unclaimed
    private static final String COLOR_OG_PROTECTED = "#FF8C00";     // Dark orange - OrbisGuard region

    // Semi-transparent overlay colors for terrain mode (RRGGBBAA hex)
    private static final String ALPHA_CURRENT_SAFE = "#14b8a6C0";   // 75% teal
    private static final String ALPHA_CURRENT_WAR = "#c084fcC0";    // 75% purple
    private static final String ALPHA_OTHER_SAFE = "#2dd4bf80";     // 50% teal
    private static final String ALPHA_OTHER_WAR = "#c084fc80";      // 50% purple
    private static final String ALPHA_FACTION = "#6b7280A0";        // 63% gray
    private static final String ALPHA_WILDERNESS = "#00000000";     // Fully transparent
    private static final String ALPHA_OG_PROTECTED = "#FF8C00A0";   // 63% dark orange

    private final PlayerRef playerRef;
    private final UUID zoneId;
    private final ZoneManager zoneManager;
    private final ClaimManager claimManager;
    private final GuiManager guiManager;
    private final boolean openFlagsAfter;
    private CompletableFuture<ChunkMapAsset> terrainAssetFuture = null;

    public AdminZoneMapPage(PlayerRef playerRef,
                            Zone zone,
                            ZoneManager zoneManager,
                            ClaimManager claimManager,
                            GuiManager guiManager) {
        this(playerRef, zone, zoneManager, claimManager, guiManager, false);
    }

    public AdminZoneMapPage(PlayerRef playerRef,
                            Zone zone,
                            ZoneManager zoneManager,
                            ClaimManager claimManager,
                            GuiManager guiManager,
                            boolean openFlagsAfter) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminZoneMapData.CODEC);
        this.playerRef = playerRef;
        this.zoneId = zone.id();
        this.zoneManager = zoneManager;
        this.claimManager = claimManager;
        this.guiManager = guiManager;
        this.openFlagsAfter = openFlagsAfter;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {
        // Always fetch fresh zone data from manager
        Zone zone = zoneManager.getZoneById(zoneId);
        if (zone == null) {
            Logger.warn("[AdminZoneMapPage] Zone %s no longer exists", zoneId);
            return;
        }
        Logger.debug("[AdminZoneMapPage] build() for zone '%s' with %d chunks", zone.name(), zone.getChunkCount());

        // Get player's current position
        Player player = store.getComponent(ref, Player.getComponentType());
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        World world = player != null ? player.getWorld() : null;
        String worldName = world != null ? world.getName() : "world";

        // Check if player is in the same world as the zone
        boolean sameWorld = zone.world().equals(worldName);

        int playerChunkX = 0;
        int playerChunkZ = 0;
        if (transform != null) {
            var position = transform.getPosition();
            playerChunkX = ChunkUtil.blockToChunk((int) position.x);
            playerChunkZ = ChunkUtil.blockToChunk((int) position.z);
        }

        // Choose template based on terrain mode config
        boolean terrainEnabled = ConfigManager.get().isTerrainMapEnabled();
        if (terrainEnabled) {
            cmd.append("HyperFactions/admin/admin_zone_map_terrain.ui");
        } else {
            cmd.append("HyperFactions/admin/admin_zone_map.ui");
        }

        // Zone header info
        cmd.set("#ZoneTitle.Text", zone.name() + " (" + zone.type().getDisplayName() + ")");
        cmd.set("#ZoneStats.Text", zone.getChunkCount() + " chunks in " + zone.world());

        // Show world mismatch warning if player is in different world
        if (!sameWorld) {
            cmd.set("#PositionInfo.Text", "WARNING: You are in '" + worldName + "' - zone is in '" + zone.world() + "'");
        } else {
            cmd.set("#PositionInfo.Text", "Your Position: Chunk (" + playerChunkX + ", " + playerChunkZ + ")");
        }

        // Dynamic legend: add OrbisGuard protected region entry when OG is available
        if (OrbisGuardIntegration.isAvailable()) {
            if (terrainEnabled) {
                // Terrain mode: append to row 2 (#LegendContainer[1])
                cmd.appendInline("#LegendContainer[1]",
                        "Group { LayoutMode: Left; Anchor: (Width: 110); " +
                        "Group { Anchor: (Width: 10, Height: 10); Background: (Color: " + COLOR_OG_PROTECTED + "); } " +
                        "Label { Text: \" Protected\"; Style: (FontSize: 9, TextColor: #cccccc, VerticalAlignment: Center); } }");
            } else {
                // Flat mode: append to column 3 (#LegendContainer[2])
                cmd.appendInline("#LegendContainer[2]",
                        "Group { LayoutMode: Left; Anchor: (Height: 16); " +
                        "Group { Anchor: (Width: 12, Height: 12); Background: (Color: " + COLOR_OG_PROTECTED + "); } " +
                        "Label { Text: \" Protected\"; Style: (FontSize: 10, TextColor: #cccccc, VerticalAlignment: Center); } }");
            }
        }

        // Build the chunk grid (terrain or flat mode)
        if (terrainEnabled) {
            buildTerrainGrid(cmd, events, zone, playerChunkX, playerChunkZ);
        } else {
            buildChunkGrid(cmd, events, zone, playerChunkX, playerChunkZ);
        }

        // Confirm/Done button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ConfirmBtn",
                EventData.of("Button", "Confirm"),
                false
        );

        // Register with active page tracker for real-time updates
        ActivePageTracker activeTracker = guiManager.getActivePageTracker();
        if (activeTracker != null) {
            activeTracker.register(playerRef.getUuid(), "admin_zone_map", null, this);
        }
    }

    @Override
    public void refreshContent() {
        rebuild();
    }

    /**
     * Builds the flat 29x17 chunk grid with zone-specific coloring and click events.
     */
    private void buildChunkGrid(UICommandBuilder cmd, UIEventBuilder events,
                                Zone zone, int centerX, int centerZ) {
        String worldName = zone.world();

        // Fetch OG regions once for the entire grid
        List<OrbisGuardIntegration.RegionInfo> ogRegions = OrbisGuardIntegration.isAvailable()
                ? OrbisGuardIntegration.getRegionsForWorld(worldName) : List.of();

        for (int zOffset = -GRID_RADIUS_Z; zOffset <= GRID_RADIUS_Z; zOffset++) {
            int rowIndex = zOffset + GRID_RADIUS_Z;
            int chunkZ = centerZ + zOffset;

            // Create row container
            cmd.appendInline("#ChunkGrid", "Group { LayoutMode: Left; }");

            for (int xOffset = -GRID_RADIUS_X; xOffset <= GRID_RADIUS_X; xOffset++) {
                int colIndex = xOffset + GRID_RADIUS_X;
                int chunkX = centerX + xOffset;

                // Get chunk info (always use territory color)
                ChunkInfo info = getChunkInfo(zone, worldName, chunkX, chunkZ, ogRegions);
                boolean isPlayerPos = (xOffset == 0 && zOffset == 0);

                // Create cell with territory color
                cmd.appendInline("#ChunkGrid[" + rowIndex + "]",
                        "Group { Anchor: (Width: " + CELL_SIZE + ", Height: " + CELL_SIZE + "); " +
                        "Background: (Color: " + info.color + "); }");

                String cellSelector = "#ChunkGrid[" + rowIndex + "][" + colIndex + "]";

                // Add "+" marker for player position (overlaid on chunk color)
                if (isPlayerPos) {
                    cmd.append(cellSelector, "HyperFactions/faction/chunk_marker.ui");
                }

                // Add button overlay for actionable cells
                if (info.type != ChunkType.OG_PROTECTED) {
                    cmd.append(cellSelector, "HyperFactions/faction/chunk_btn.ui");
                    bindChunkEvents(events, cellSelector + " #Btn", chunkX, chunkZ, info);
                }
            }
        }
    }

    /**
     * Builds the 17x17 terrain map grid with semi-transparent zone overlays.
     * The terrain image is generated asynchronously and sent to the client.
     */
    private void buildTerrainGrid(UICommandBuilder cmd, UIEventBuilder events,
                                  Zone zone, int centerX, int centerZ) {
        String worldName = zone.world();

        // Start terrain generation if not already started for this page instance.
        if (terrainAssetFuture == null) {
            ChunkMapAsset.sendToPlayer(playerRef.getPacketHandler(), ChunkMapAsset.empty());

            terrainAssetFuture = ChunkMapAsset.generate(playerRef, centerX, centerZ, TERRAIN_GRID_RADIUS);
            if (terrainAssetFuture != null) {
                terrainAssetFuture.thenAccept(asset -> {
                    if (asset != null) {
                        ChunkMapAsset.sendToPlayer(playerRef.getPacketHandler(), asset);
                        this.sendUpdate();
                    }
                });
            }
        }

        // Fetch OG regions once for the entire grid
        List<OrbisGuardIntegration.RegionInfo> ogRegions = OrbisGuardIntegration.isAvailable()
                ? OrbisGuardIntegration.getRegionsForWorld(worldName) : List.of();

        // Build 17x17 grid with alpha overlay colors
        for (int zOffset = -TERRAIN_GRID_RADIUS; zOffset <= TERRAIN_GRID_RADIUS; zOffset++) {
            int rowIndex = zOffset + TERRAIN_GRID_RADIUS;
            int chunkZ = centerZ + zOffset;

            // Row container - exactly 32px tall, children flow left-to-right
            cmd.appendInline("#ChunkGrid", "Group { LayoutMode: Left; Anchor: (Height: 32); }");

            for (int xOffset = -TERRAIN_GRID_RADIUS; xOffset <= TERRAIN_GRID_RADIUS; xOffset++) {
                int colIndex = xOffset + TERRAIN_GRID_RADIUS;
                int chunkX = centerX + xOffset;

                ChunkInfo info = getChunkInfo(zone, worldName, chunkX, chunkZ, ogRegions);

                // Create cell with semi-transparent overlay color
                cmd.appendInline("#ChunkGrid[" + rowIndex + "]",
                        "Group { Anchor: (Width: 32, Height: 32); Background: (Color: " + info.alphaColor() + "); }");

                String cellSel = "#ChunkGrid[" + rowIndex + "][" + colIndex + "]";

                // Player marker at center
                if (xOffset == 0 && zOffset == 0) {
                    cmd.append(cellSel, "HyperFactions/faction/chunk_map_marker.ui");
                }

                // Click events on actionable cells only (wilderness → claim, current zone → unclaim)
                if (info.type != ChunkType.OG_PROTECTED &&
                    (info.type == ChunkType.WILDERNESS || info.type == ChunkType.CURRENT_ZONE)) {
                    cmd.append(cellSel, "HyperFactions/faction/chunk_map_terrain_btn.ui");
                    bindChunkEvents(events, cellSel + " #Btn", chunkX, chunkZ, info);
                }
            }
        }
    }


    /**
     * Binds click events based on chunk state.
     */
    private void bindChunkEvents(UIEventBuilder events, String cellSelector,
                                 int chunkX, int chunkZ, ChunkInfo info) {
        switch (info.type) {
            case WILDERNESS:
                // Left-click wilderness to claim for this zone
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        cellSelector,
                        EventData.of("Button", "Claim")
                                .append("ChunkX", String.valueOf(chunkX))
                                .append("ChunkZ", String.valueOf(chunkZ))
                                .append("ZoneId", zoneId.toString()),
                        false
                );
                break;

            case CURRENT_ZONE:
                // Right-click current zone chunk to unclaim
                events.addEventBinding(
                        CustomUIEventBindingType.RightClicking,
                        cellSelector,
                        EventData.of("Button", "Unclaim")
                                .append("ChunkX", String.valueOf(chunkX))
                                .append("ChunkZ", String.valueOf(chunkZ))
                                .append("ZoneId", zoneId.toString()),
                        false
                );
                break;

            case OTHER_ZONE:
                // Click other zone - show error
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        cellSelector,
                        EventData.of("Button", "OtherZone")
                                .append("ChunkX", String.valueOf(chunkX))
                                .append("ChunkZ", String.valueOf(chunkZ)),
                        false
                );
                break;

            case FACTION:
                // Click faction claim - show error
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        cellSelector,
                        EventData.of("Button", "Faction")
                                .append("ChunkX", String.valueOf(chunkX))
                                .append("ChunkZ", String.valueOf(chunkZ)),
                        false
                );
                break;
        }
    }

    /**
     * Gets information about a chunk's state relative to this zone.
     */
    private ChunkInfo getChunkInfo(Zone zone, String worldName, int chunkX, int chunkZ,
                                   List<OrbisGuardIntegration.RegionInfo> ogRegions) {
        // Check if chunk belongs to current zone
        if (zone.containsChunk(chunkX, chunkZ)) {
            boolean safe = zone.isSafeZone();
            return new ChunkInfo(ChunkType.CURRENT_ZONE,
                    safe ? COLOR_CURRENT_SAFE : COLOR_CURRENT_WAR,
                    safe ? ALPHA_CURRENT_SAFE : ALPHA_CURRENT_WAR);
        }

        // Check if chunk belongs to another zone
        Zone otherZone = zoneManager.getZone(worldName, chunkX, chunkZ);
        if (otherZone != null) {
            boolean safe = otherZone.isSafeZone();
            return new ChunkInfo(ChunkType.OTHER_ZONE,
                    safe ? COLOR_OTHER_SAFE : COLOR_OTHER_WAR,
                    safe ? ALPHA_OTHER_SAFE : ALPHA_OTHER_WAR);
        }

        // Check OrbisGuard protection (render whole chunk as protected if any OG region overlaps)
        if (!ogRegions.isEmpty()) {
            int minBlockX = chunkX << 5; // chunkX * 32
            int maxBlockX = minBlockX + 31;
            int minBlockZ = chunkZ << 5;
            int maxBlockZ = minBlockZ + 31;
            for (OrbisGuardIntegration.RegionInfo region : ogRegions) {
                if (region.maxX() >= minBlockX && region.minX() <= maxBlockX &&
                    region.maxZ() >= minBlockZ && region.minZ() <= maxBlockZ) {
                    return new ChunkInfo(ChunkType.OG_PROTECTED, COLOR_OG_PROTECTED, ALPHA_OG_PROTECTED);
                }
            }
        }

        // Check if chunk is claimed by a faction
        UUID factionId = claimManager.getClaimOwner(worldName, chunkX, chunkZ);
        if (factionId != null) {
            return new ChunkInfo(ChunkType.FACTION, COLOR_FACTION, ALPHA_FACTION);
        }

        // Wilderness - unclaimed
        return new ChunkInfo(ChunkType.WILDERNESS, COLOR_WILDERNESS, ALPHA_WILDERNESS);
    }

    private enum ChunkType {
        WILDERNESS, CURRENT_ZONE, OTHER_ZONE, FACTION, OG_PROTECTED
    }

    private record ChunkInfo(ChunkType type, String color, String alphaColor) {}

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                AdminZoneMapData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            return;
        }

        // Get fresh zone data
        Zone zone = zoneManager.getZoneById(zoneId);
        if (zone == null) {
            player.sendMessage(MessageUtil.errorText("Zone no longer exists."));
            guiManager.openAdminZone(player, ref, store, playerRef);
            return;
        }

        // Always use zone's world for operations (not player's current world)
        String zoneWorld = zone.world();

        switch (data.button) {
            case "Confirm" -> {
                if (openFlagsAfter) {
                    // Navigate to flags settings as requested during zone creation
                    guiManager.openAdminZoneSettings(player, ref, store, playerRef, zoneId);
                } else {
                    guiManager.openAdminZone(player, ref, store, playerRef);
                }
            }

            case "Claim" -> {
                ZoneManager.ZoneResult result = zoneManager.claimChunk(zoneId, zoneWorld, data.chunkX, data.chunkZ);
                if (result == ZoneManager.ZoneResult.SUCCESS) {
                    player.sendMessage(MessageUtil.text("Claimed chunk (" + data.chunkX + ", " + data.chunkZ + ") for " + zone.name(), "#44cc44"));
                } else {
                    player.sendMessage(MessageUtil.errorText("Failed to claim chunk: " + result));
                }
                // Refresh by opening new page with fresh zone data, preserving openFlagsAfter
                Zone freshZone = zoneManager.getZoneById(zoneId);
                if (freshZone != null) {
                    guiManager.openAdminZoneMap(player, ref, store, playerRef, freshZone, openFlagsAfter);
                }
            }

            case "Unclaim" -> {
                ZoneManager.ZoneResult result = zoneManager.unclaimChunk(zoneId, zoneWorld, data.chunkX, data.chunkZ);
                if (result == ZoneManager.ZoneResult.SUCCESS) {
                    player.sendMessage(MessageUtil.text("Unclaimed chunk (" + data.chunkX + ", " + data.chunkZ + ") from " + zone.name(), "#44cc44"));
                } else {
                    player.sendMessage(MessageUtil.errorText("Failed to unclaim chunk: " + result));
                }
                // Refresh by opening new page with fresh zone data, preserving openFlagsAfter
                Zone freshZone = zoneManager.getZoneById(zoneId);
                if (freshZone != null) {
                    guiManager.openAdminZoneMap(player, ref, store, playerRef, freshZone, openFlagsAfter);
                }
            }

            case "OtherZone" -> {
                Zone otherZone = zoneManager.getZone(zoneWorld, data.chunkX, data.chunkZ);
                String zoneName = otherZone != null ? otherZone.name() : "another zone";
                player.sendMessage(MessageUtil.text("This chunk belongs to " + zoneName + ".", MessageUtil.COLOR_GOLD));
            }

            case "Faction" -> {
                player.sendMessage(MessageUtil.text("This chunk is claimed by a faction.", MessageUtil.COLOR_GOLD));
            }

            case "Protected" -> {
                player.sendMessage(MessageUtil.text("This chunk is in a protected region.", MessageUtil.COLOR_GOLD));
            }

            default -> {}
        }
    }

    @Override
    public void onDismiss(Ref<EntityStore> ref, Store<EntityStore> store) {
        super.onDismiss(ref, store);
        ActivePageTracker activeTracker = guiManager.getActivePageTracker();
        if (activeTracker != null) {
            activeTracker.unregister(playerRef.getUuid());
        }
    }
}
