package com.hyperfactions.gui.newplayer.page;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.RelationType;
import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneType;
import com.hyperfactions.gui.ActivePageTracker;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.RefreshablePage;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.faction.ChunkMapAsset;
import com.hyperfactions.gui.newplayer.NewPlayerNavBarHelper;
import com.hyperfactions.gui.newplayer.data.NewPlayerPageData;
import com.hyperfactions.integration.protection.OrbisGuardIntegration;
import com.hyperfactions.manager.*;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.MessageKeys;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Read-only Territory Map page for new players.
 * Uses the same template and rendering as ChunkMapPage but with:
 * - New player navigation bar
 * - No click events (read-only)
 * - Different hint text
 * Supports both flat grid and terrain map modes via config.
 */
public class NewPlayerMapPage extends InteractiveCustomUIPage<NewPlayerPageData> implements RefreshablePage {

  private static final String PAGE_ID = "map";

  // Flat mode grid (29x17 at 16px)
  private static final int GRID_RADIUS_X = 14; // 29 columns (-14 to +14)
  private static final int GRID_RADIUS_Z = 8;  // 17 rows (-8 to +8)
  private static final int CELL_SIZE = 16;     // pixels per cell

  // Terrain mode grid (17x17 at 32px, matching ChunkWorldMap pixel size)
  private static final int TERRAIN_GRID_RADIUS = 8; // 17 cells (-8 to +8)

  // Flat mode colors (opaque)
  private static final String COLOR_OTHER = "#fbbf24";      // Yellow/gold - faction territory
  private static final String COLOR_WILDERNESS = "#1e293b"; // Dark slate - unclaimed
  private static final String COLOR_SAFEZONE = "#2dd4bf";   // Teal - safe zone
  private static final String COLOR_WARZONE = "#c084fc";    // Light purple - war zone
  private static final String COLOR_OG_PROTECTED = "#FF8C00"; // Dark orange - OrbisGuard region

  // Semi-transparent overlay colors for terrain mode (RRGGBBAA hex)
  private static final String ALPHA_OTHER = "#fbbf2480";       // 50% gold
  private static final String ALPHA_WILDERNESS = "#00000000";  // Fully transparent
  private static final String ALPHA_SAFEZONE = "#2dd4bf80";    // 50% teal
  private static final String ALPHA_WARZONE = "#c084fc80";     // 50% purple
  private static final String ALPHA_OG_PROTECTED = "#FF8C0080"; // 50% dark orange

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final ClaimManager claimManager;

  private final RelationManager relationManager;

  private final ZoneManager zoneManager;

  private final GuiManager guiManager;

  // Terrain asset generation state (per page instance)
  private CompletableFuture<ChunkMapAsset> terrainAssetFuture = null;

  // Saved from build() for use in refreshContent() redirect
  private Ref<EntityStore> savedRef;

  private Store<EntityStore> savedStore;

  /** Creates a new NewPlayerMapPage. */
  public NewPlayerMapPage(PlayerRef playerRef,
              FactionManager factionManager,
              ClaimManager claimManager,
              RelationManager relationManager,
              ZoneManager zoneManager,
              GuiManager guiManager) {
    super(playerRef, CustomPageLifetime.CanDismiss, NewPlayerPageData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.claimManager = claimManager;
    this.relationManager = relationManager;
    this.zoneManager = zoneManager;
    this.guiManager = guiManager;
  }

  /** Builds . */
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

    // Choose template based on terrain mode config
    boolean terrainEnabled = ConfigManager.get().isTerrainMapEnabled();
    if (terrainEnabled) {
      cmd.append(UIPaths.CHUNK_MAP_TERRAIN);
    } else {
      cmd.append(UIPaths.CHUNK_MAP);
    }

    // Setup navigation bar for new players (instead of faction nav bar)
    NewPlayerNavBarHelper.setupBar(playerRef, PAGE_ID, cmd, events);

    // Update position info
    cmd.set("#PositionInfo.Text", HFMessages.get(playerRef, MessageKeys.MapGui.POSITION, playerChunkX, playerChunkZ));

    // Update hint text for read-only mode
    cmd.set("#ActionHint.Text", HFMessages.get(playerRef, MessageKeys.NewPlayerGui.MAP_HINT));

    // Hide claim/power stats (not relevant for new players)
    cmd.set("#ClaimStats.Text", "");
    cmd.set("#PowerStatus.Text", "");

    // Hide faction-specific legend (only exists in flat mode template)
    if (!terrainEnabled) {
      cmd.set("#FactionLegend.Visible", false);
    }

    // Dynamic legend: add OrbisGuard protected region entry when OG is available
    if (OrbisGuardIntegration.isAvailable()) {
      if (terrainEnabled) {
        cmd.appendInline("#LegendContainer[1]",
            "Group { LayoutMode: Left; Anchor: (Width: 110); "
            + "Group { Anchor: (Width: 10, Height: 10); Background: (Color: " + COLOR_OG_PROTECTED + "); } "
            + "Label { Text: \" " + HFMessages.get(playerRef, MessageKeys.MapGui.LEGEND_PROTECTED) + "\"; Style: (FontSize: 9, TextColor: #cccccc, VerticalAlignment: Center); } }");
      } else {
        cmd.appendInline("#LegendContainer[2]",
            "Group { LayoutMode: Left; Anchor: (Height: 16); "
            + "Group { Anchor: (Width: 12, Height: 12); Background: (Color: " + COLOR_OG_PROTECTED + "); } "
            + "Label { Text: \" " + HFMessages.get(playerRef, MessageKeys.MapGui.LEGEND_PROTECTED) + "\"; Style: (FontSize: 10, TextColor: #cccccc, VerticalAlignment: Center); } }");
      }
    }

    // Build the map grid (terrain or flat mode, read-only — no click events)
    if (terrainEnabled) {
      buildTerrainMap(cmd, worldName, playerChunkX, playerChunkZ);
    } else {
      buildChunkGrid(cmd, worldName, playerChunkX, playerChunkZ);
    }

    // Register with active page tracker for real-time updates
    ActivePageTracker activeTracker = guiManager.getActivePageTracker();
    if (activeTracker != null) {
      activeTracker.register(playerRef.getUuid(), PAGE_ID, null, this);
    }
  }

  /** Refresh Content. */
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
    sendUpdate();
  }

  /**
   * Builds the terrain map grid (17x17) with semi-transparent claim overlays.
   * Terrain imagery is generated asynchronously via ChunkMapAsset.
   * Read-only — no click events.
   */
  private void buildTerrainMap(UICommandBuilder cmd, String worldName, int centerX, int centerZ) {
    // Start terrain generation if not already started for this page instance
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

    // Build 17x17 grid with alpha overlays (read-only — no click events)
    for (int zOffset = -TERRAIN_GRID_RADIUS; zOffset <= TERRAIN_GRID_RADIUS; zOffset++) {
      int rowIndex = zOffset + TERRAIN_GRID_RADIUS;
      int chunkZ = centerZ + zOffset;

      cmd.appendInline("#ChunkGrid", "Group { LayoutMode: Left; Anchor: (Height: 32); }");

      for (int xOffset = -TERRAIN_GRID_RADIUS; xOffset <= TERRAIN_GRID_RADIUS; xOffset++) {
        int colIndex = xOffset + TERRAIN_GRID_RADIUS;
        int chunkX = centerX + xOffset;

        String alphaColor = getTerrainOverlayColor(worldName, chunkX, chunkZ, ogRegions);

        cmd.appendInline("#ChunkGrid[" + rowIndex + "]",
            "Group { Anchor: (Width: 32, Height: 32); Background: (Color: " + alphaColor + "); }");

        // Player marker at center
        if (xOffset == 0 && zOffset == 0) {
          String cellSel = "#ChunkGrid[" + rowIndex + "][" + colIndex + "]";
          cmd.append(cellSel, UIPaths.CHUNK_MAP_MARKER);
        }
      }
    }
  }

  /**
   * Gets the semi-transparent overlay color for a chunk in terrain mode.
   * New players have no faction, so OWN/ALLY/ENEMY never apply — all claims show as OTHER.
   */
  private String getTerrainOverlayColor(String worldName, int chunkX, int chunkZ,
                     List<OrbisGuardIntegration.RegionInfo> ogRegions) {
    // Check zones first
    Zone zone = zoneManager.getZone(worldName, chunkX, chunkZ);
    if (zone != null) {
      return zone.type() == ZoneType.SAFE ? ALPHA_SAFEZONE : ALPHA_WARZONE;
    }

    // Check OrbisGuard protection
    if (!ogRegions.isEmpty()) {
      int minBlockX = chunkX << 5;
      int maxBlockX = minBlockX + 31;
      int minBlockZ = chunkZ << 5;
      int maxBlockZ = minBlockZ + 31;
      for (OrbisGuardIntegration.RegionInfo region : ogRegions) {
        if (region.maxX() >= minBlockX && region.minX() <= maxBlockX
          && region.maxZ() >= minBlockZ && region.minZ() <= maxBlockZ) {
          return ALPHA_OG_PROTECTED;
        }
      }
    }

    // Check claims — new players see all claims as OTHER
    UUID ownerId = claimManager.getClaimOwner(worldName, chunkX, chunkZ);
    if (ownerId != null) {
      return ALPHA_OTHER;
    }

    return ALPHA_WILDERNESS;
  }

  /**
   * Builds the flat chunk grid (29x17) using the same pattern as ChunkMapPage.
   * No click events are bound (read-only for new players).
   */
  private void buildChunkGrid(UICommandBuilder cmd, String worldName, int centerX, int centerZ) {
    // Fetch OG regions once for the entire grid
    List<OrbisGuardIntegration.RegionInfo> ogRegions = OrbisGuardIntegration.isAvailable()
        ? OrbisGuardIntegration.getRegionsForWorld(worldName) : List.of();

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

        // Get cell color
        boolean isPlayerPos = (xOffset == 0 && zOffset == 0);
        String cellColor = getCellColor(worldName, chunkX, chunkZ, ogRegions);

        // Create cell with territory color
        cmd.appendInline("#ChunkGrid[" + rowIndex + "]",
            "Group { Anchor: (Width: " + CELL_SIZE + ", Height: " + CELL_SIZE + "); "
            + "Background: (Color: " + cellColor + "); }");

        String cellSelector = "#ChunkGrid[" + rowIndex + "][" + colIndex + "]";

        // Add "+" marker for player position (overlaid on chunk color)
        if (isPlayerPos) {
          cmd.append(cellSelector, UIPaths.CHUNK_MARKER);
        }

        // Add button overlay for visual consistency (but no events bound)
        cmd.append(cellSelector, UIPaths.CHUNK_BTN);
      }
    }
  }

  /**
   * Gets the opaque cell color for flat mode.
   * Includes OrbisGuard region awareness.
   */
  private String getCellColor(String worldName, int chunkX, int chunkZ,
                List<OrbisGuardIntegration.RegionInfo> ogRegions) {
    // Check for admin zones first
    Zone zone = zoneManager.getZone(worldName, chunkX, chunkZ);
    if (zone != null) {
      return zone.type() == ZoneType.SAFE ? COLOR_SAFEZONE : COLOR_WARZONE;
    }

    // Check OrbisGuard protection
    if (!ogRegions.isEmpty()) {
      int minBlockX = chunkX << 5;
      int maxBlockX = minBlockX + 31;
      int minBlockZ = chunkZ << 5;
      int maxBlockZ = minBlockZ + 31;
      for (OrbisGuardIntegration.RegionInfo region : ogRegions) {
        if (region.maxX() >= minBlockX && region.minX() <= maxBlockX
          && region.maxZ() >= minBlockZ && region.minZ() <= maxBlockZ) {
          return COLOR_OG_PROTECTED;
        }
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


  /** Handles data event. */
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

  /** Called when dismiss. */
  @Override
  public void onDismiss(Ref<EntityStore> ref, Store<EntityStore> store) {
    super.onDismiss(ref, store);
    ActivePageTracker activeTracker = guiManager.getActivePageTracker();
    if (activeTracker != null) {
      activeTracker.unregister(playerRef.getUuid());
    }
  }
}
