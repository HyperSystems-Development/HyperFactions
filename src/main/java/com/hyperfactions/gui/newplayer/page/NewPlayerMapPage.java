package com.hyperfactions.gui.newplayer.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneType;
import com.hyperfactions.gui.ActivePageTracker;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.RefreshablePage;
import com.hyperfactions.gui.newplayer.NewPlayerNavBarHelper;
import com.hyperfactions.gui.newplayer.data.NewPlayerPageData;
import com.hyperfactions.manager.*;
import com.hyperfactions.util.ChunkUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

/**
 * Read-only Territory Map page for new players.
 * Uses the same template and rendering as ChunkMapPage but with:
 * - New player navigation bar
 * - No click events (read-only)
 * - Different hint text
 */
public class NewPlayerMapPage extends InteractiveCustomUIPage<NewPlayerPageData> implements RefreshablePage {

    private static final String PAGE_ID = "map";
    private static final int GRID_RADIUS_X = 14; // 29 columns (-14 to +14)
    private static final int GRID_RADIUS_Z = 8;  // 17 rows (-8 to +8)
    private static final int CELL_SIZE = 16;     // pixels per cell

    // Color constants - same as ChunkMapPage
    private static final String COLOR_OTHER = "#fbbf24";      // Yellow/gold - faction territory
    private static final String COLOR_WILDERNESS = "#1e293b"; // Dark slate - unclaimed
    private static final String COLOR_SAFEZONE = "#2dd4bf";   // Teal - safe zone
    private static final String COLOR_WARZONE = "#c084fc";    // Light purple - war zone
    // Player marker: white "+" overlay defined in chunk_marker.ui

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final ClaimManager claimManager;
    private final ZoneManager zoneManager;
    private final GuiManager guiManager;

    // Saved from build() for use in refreshContent() redirect
    private Ref<EntityStore> savedRef;
    private Store<EntityStore> savedStore;

    public NewPlayerMapPage(PlayerRef playerRef,
                            FactionManager factionManager,
                            ClaimManager claimManager,
                            ZoneManager zoneManager,
                            GuiManager guiManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, NewPlayerPageData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.claimManager = claimManager;
        this.zoneManager = zoneManager;
        this.guiManager = guiManager;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Save ref/store for refreshContent() redirect
        this.savedRef = ref;
        this.savedStore = store;

        // Get player's current position
        Player player = store.getComponent(ref, Player.getComponentType());
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        World world = player != null ? player.getWorld() : null;
        String worldName = world != null ? world.getName() : "world";

        int playerChunkX = 0;
        int playerChunkZ = 0;
        if (transform != null) {
            var position = transform.getPosition();
            playerChunkX = ChunkUtil.blockToChunk((int) position.x);
            playerChunkZ = ChunkUtil.blockToChunk((int) position.z);
        }

        // Use the same template as ChunkMapPage
        cmd.append("HyperFactions/faction/chunk_map.ui");

        // Setup navigation bar for new players (instead of faction nav bar)
        NewPlayerNavBarHelper.setupBar(playerRef, PAGE_ID, cmd, events);

        // Update position info
        cmd.set("#PositionInfo.Text", "Your Position: Chunk (" + playerChunkX + ", " + playerChunkZ + ")");

        // Update hint text for read-only mode
        cmd.set("#ActionHint.Text", "View Only - Join a faction to claim territory!");

        // Hide claim/power stats (not relevant for new players)
        cmd.set("#ClaimStats.Text", "");
        cmd.set("#PowerStatus.Text", "");

        // Hide faction-specific legend (Your/Ally/Enemy territory)
        cmd.set("#FactionLegend.Visible", false);

        // Build the map grid (same as ChunkMapPage but without click events)
        buildChunkGrid(cmd, worldName, playerChunkX, playerChunkZ);

        // Register with active page tracker for real-time updates
        ActivePageTracker activeTracker = guiManager.getActivePageTracker();
        if (activeTracker != null) {
            activeTracker.register(playerRef.getUuid(), PAGE_ID, null, this);
        }
    }

    @Override
    public void refreshContent() {
        // If player joined a faction (e.g., request accepted by someone else), redirect to dashboard
        if (factionManager.isInFaction(playerRef.getUuid())) {
            Faction faction = factionManager.getPlayerFaction(playerRef.getUuid());
            if (faction != null && savedRef != null && savedStore != null) {
                Player player = savedStore.getComponent(savedRef, Player.getComponentType());
                if (player != null) {
                    ActivePageTracker activeTracker = guiManager.getActivePageTracker();
                    if (activeTracker != null) {
                        activeTracker.unregister(playerRef.getUuid());
                    }
                    guiManager.openFactionDashboard(player, savedRef, savedStore, playerRef, faction);
                    return;
                }
            }
        }
        rebuild();
    }

    /**
     * Builds the chunk grid using the same pattern as ChunkMapPage.
     * No click events are bound (read-only for new players).
     */
    private void buildChunkGrid(UICommandBuilder cmd, String worldName, int centerX, int centerZ) {
        // Build rows (same pattern as ChunkMapPage)
        for (int zOffset = -GRID_RADIUS_Z; zOffset <= GRID_RADIUS_Z; zOffset++) {
            int rowIndex = zOffset + GRID_RADIUS_Z;
            int chunkZ = centerZ + zOffset;

            // Create row container
            cmd.appendInline("#ChunkGrid", "Group { LayoutMode: Left; }");

            // Build cells per row
            for (int xOffset = -GRID_RADIUS_X; xOffset <= GRID_RADIUS_X; xOffset++) {
                int colIndex = xOffset + GRID_RADIUS_X;
                int chunkX = centerX + xOffset;

                // Get cell color (always use territory color)
                boolean isPlayerPos = (xOffset == 0 && zOffset == 0);
                String cellColor = getCellColor(worldName, chunkX, chunkZ);

                // Create cell with territory color
                cmd.appendInline("#ChunkGrid[" + rowIndex + "]",
                        "Group { Anchor: (Width: " + CELL_SIZE + ", Height: " + CELL_SIZE + "); " +
                        "Background: (Color: " + cellColor + "); }");

                String cellSelector = "#ChunkGrid[" + rowIndex + "][" + colIndex + "]";

                // Add "+" marker for player position (overlaid on chunk color)
                if (isPlayerPos) {
                    cmd.append(cellSelector, "HyperFactions/faction/chunk_marker.ui");
                }

                // Add button overlay for visual consistency (but no events bound)
                cmd.append(cellSelector, "HyperFactions/faction/chunk_btn.ui");
            }
        }
    }

    private String getCellColor(String worldName, int chunkX, int chunkZ) {
        // Check for admin zones first
        Zone zone = zoneManager.getZone(worldName, chunkX, chunkZ);
        if (zone != null) {
            if (zone.type() == ZoneType.SAFE) {
                return COLOR_SAFEZONE;
            } else {
                return COLOR_WARZONE;
            }
        }

        // Check for faction claims
        UUID ownerId = claimManager.getClaimOwner(worldName, chunkX, chunkZ);
        if (ownerId != null) {
            Faction faction = factionManager.getFaction(ownerId);
            if (faction != null && faction.color() != null) {
                return faction.color();
            }
            return COLOR_OTHER;
        }

        return COLOR_WILDERNESS;
    }


    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                NewPlayerPageData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            sendUpdate();
            return;
        }

        // Handle navigation only - no map interactions for new players
        if (NewPlayerNavBarHelper.handleNavEvent(data, player, ref, store, playerRef, guiManager)) {
            return;
        }

        // Default - just refresh
        sendUpdate();
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
