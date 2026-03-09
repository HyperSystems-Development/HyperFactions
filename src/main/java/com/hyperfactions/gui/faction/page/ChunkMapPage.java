package com.hyperfactions.gui.faction.page;

import com.hyperfactions.Permissions;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.*;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.gui.ActivePageTracker;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.RefreshablePage;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.faction.ChunkMapAsset;
import com.hyperfactions.gui.faction.NavBarHelper;
import com.hyperfactions.gui.faction.data.ChunkMapData;
import com.hyperfactions.gui.newplayer.NewPlayerNavBarHelper;
import com.hyperfactions.integration.protection.OrbisGuardIntegration;
import com.hyperfactions.manager.*;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.MessageKeys;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
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
 * Interactive Chunk Map page - displays a territory grid.
 * Supports two modes: flat colored grid (29x17) and terrain map with overlays (17x17).
 * Left-click to claim, right-click to unclaim.
 */
public class ChunkMapPage extends InteractiveCustomUIPage<ChunkMapData> implements RefreshablePage {

  private static final String PAGE_ID = "map";

  // Flat mode grid (29x17 at 16px)
  private static final int GRID_RADIUS_X = 14; // 29 columns (-14 to +14)
  private static final int GRID_RADIUS_Z = 8;  // 17 rows (-8 to +8)
  private static final int CELL_SIZE = 16;     // pixels per cell

  // Terrain mode grid (17x17 at 32px, matching ChunkWorldMap pixel size)
  private static final int TERRAIN_GRID_RADIUS = 8; // 17 cells (-8 to +8)

  // Color constants - clean flat design (opaque, for flat mode)
  private static final String COLOR_OWN = "#4ade80";        // Bright green - your territory
  private static final String COLOR_ALLY = "#60a5fa";       // Bright blue - ally territory
  private static final String COLOR_ENEMY = "#f87171";      // Bright red - enemy territory
  private static final String COLOR_OTHER = "#fbbf24";      // Yellow/gold - neutral faction
  private static final String COLOR_WILDERNESS = "#1e293b"; // Dark slate - unclaimed (darker for contrast)
  private static final String COLOR_SAFEZONE = "#2dd4bf";   // Teal - safe zone
  private static final String COLOR_WARZONE = "#c084fc";    // Purple - war zone
  private static final String COLOR_OG_PROTECTED = "#FF8C00"; // Dark orange - OrbisGuard region

  // Semi-transparent overlay colors for terrain mode (RRGGBBAA hex)
  private static final String ALPHA_OWN = "#4ade8080";       // 50% green
  private static final String ALPHA_ALLY = "#60a5fa80";      // 50% blue
  private static final String ALPHA_ENEMY = "#f8717180";     // 50% red
  private static final String ALPHA_OTHER = "#fbbf2480";     // 50% gold
  private static final String ALPHA_WILDERNESS = "#00000000"; // Fully transparent
  private static final String ALPHA_SAFEZONE = "#2dd4bf80";  // 50% teal
  private static final String ALPHA_WARZONE = "#c084fc80";   // 50% purple
  private static final String ALPHA_OG_PROTECTED = "#FF8C0080"; // 50% dark orange

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final ClaimManager claimManager;

  private final RelationManager relationManager;

  private final ZoneManager zoneManager;

  private final GuiManager guiManager;

  // Terrain asset generation state (per page instance)
  private CompletableFuture<ChunkMapAsset> terrainAssetFuture = null;

  /** Creates a new ChunkMapPage. */
  public ChunkMapPage(PlayerRef playerRef,
            FactionManager factionManager,
            ClaimManager claimManager,
            RelationManager relationManager,
            ZoneManager zoneManager,
            GuiManager guiManager) {
    super(playerRef, CustomPageLifetime.CanDismiss, ChunkMapData.CODEC);
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
    Logger.debugTerritory("[ChunkMapPage] build() for %s", playerRef.getUsername());

    var allZones = zoneManager.getAllZones();
    Logger.debugTerritory("[ChunkMapPage] Zones: %d", allZones.size());

    UUID viewerUuid = playerRef.getUuid();
    Faction viewerFaction = factionManager.getPlayerFaction(viewerUuid);

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
    Logger.debugTerritory("[ChunkMapPage] Player at chunk (%d, %d) in %s", playerChunkX, playerChunkZ, worldName);

    // Choose template based on terrain mode config
    boolean terrainEnabled = ConfigManager.get().isTerrainMapEnabled();
    if (terrainEnabled) {
      cmd.append(UIPaths.CHUNK_MAP_TERRAIN);
    } else {
      cmd.append(UIPaths.CHUNK_MAP);
    }

    // Setup navigation bar - use new player nav when no faction
    if (viewerFaction != null) {
      NavBarHelper.setupBar(playerRef, viewerFaction, PAGE_ID, cmd, events);
    } else {
      NewPlayerNavBarHelper.setupBar(playerRef, PAGE_ID, cmd, events);
    }

    // Current position info
    cmd.set("#PositionInfo.Text", HFMessages.get(playerRef, MessageKeys.MapGui.POSITION, playerChunkX, playerChunkZ));

    // Dynamic legend: add OrbisGuard protected region entry when OG is available
    if (OrbisGuardIntegration.isAvailable()) {
      if (terrainEnabled) {
        // Terrain mode: append to row 2 (#LegendContainer[1])
        cmd.appendInline("#LegendContainer[1]",
            "Group { LayoutMode: Left; Anchor: (Width: 110); "
            + "Group { Anchor: (Width: 10, Height: 10); Background: (Color: " + COLOR_OG_PROTECTED + "); } "
            + "Label { Text: \" " + HFMessages.get(playerRef, MessageKeys.MapGui.LEGEND_PROTECTED) + "\"; Style: (FontSize: 9, TextColor: #cccccc, VerticalAlignment: Center); } }");
      } else {
        // Flat mode: append to column 3 (#LegendContainer[2])
        cmd.appendInline("#LegendContainer[2]",
            "Group { LayoutMode: Left; Anchor: (Height: 16); "
            + "Group { Anchor: (Width: 12, Height: 12); Background: (Color: " + COLOR_OG_PROTECTED + "); } "
            + "Label { Text: \" " + HFMessages.get(playerRef, MessageKeys.MapGui.LEGEND_PROTECTED) + "\"; Style: (FontSize: 10, TextColor: #cccccc, VerticalAlignment: Center); } }");
      }
    }

    // Build the chunk grid (terrain or flat mode)
    if (terrainEnabled) {
      buildTerrainMap(cmd, events, worldName, playerChunkX, playerChunkZ, viewerFaction);
    } else {
      buildChunkGrid(cmd, events, worldName, playerChunkX, playerChunkZ, viewerFaction);
    }

    // Claim and power stats
    if (viewerFaction != null) {
      PowerManager.FactionPowerStats stats = guiManager.getPowerManager().get().getFactionPowerStats(viewerFaction.id());
      int currentClaims = viewerFaction.getClaimCount();
      int maxClaims = stats.maxClaims();
      int available = Math.max(0, maxClaims - currentClaims);

      // Claim stats: "Claims: 23/78 (55 Available)"
      cmd.set("#ClaimStats.Text", HFMessages.get(playerRef, MessageKeys.MapGui.CLAIM_STATS, currentClaims, maxClaims, available));

      // Power status with overclaim warning
      double currentPower = stats.currentPower();
      double maxPower = stats.maxPower();
      boolean isOverclaimed = currentClaims > currentPower;

      if (isOverclaimed) {
        // Show overclaim warning in red
        int overclaimAmount = currentClaims - (int) currentPower;
        cmd.set("#PowerStatus.Text", HFMessages.get(playerRef, MessageKeys.MapGui.OVERCLAIMED, overclaimAmount));
      } else {
        // Normal power display
        cmd.set("#PowerStatus.Text", HFMessages.get(playerRef, MessageKeys.MapGui.POWER_DISPLAY, (int) currentPower, (int) maxPower));
      }
    } else {
      cmd.set("#ClaimStats.Text", HFMessages.get(playerRef, MessageKeys.MapGui.JOIN_TO_CLAIM));
      cmd.set("#PowerStatus.Text", "");
    }

    // Register with active page tracker for real-time updates
    ActivePageTracker activeTracker = guiManager.getActivePageTracker();
    if (activeTracker != null) {
      UUID factionId = viewerFaction != null ? viewerFaction.id() : null;
      activeTracker.register(playerRef.getUuid(), PAGE_ID, factionId, this);
    }
  }

  /** Refresh Content. */
  @Override
  public void refreshContent() {
    rebuild();
  }

  /**
   * Builds the 15x9 chunk grid dynamically.
   * Creates cells inline with color baked in, then adds button overlay for clicks.
   */
  private void buildChunkGrid(UICommandBuilder cmd, UIEventBuilder events,
                String worldName, int centerX, int centerZ,
                Faction viewerFaction) {
    UUID viewerFactionId = viewerFaction != null ? viewerFaction.id() : null;
    boolean isOfficer = false;
    if (viewerFaction != null) {
      var member = viewerFaction.getMember(playerRef.getUuid());
      isOfficer = member != null && member.isOfficerOrHigher();
    }

    // Compute permission booleans once (officer role + server permission)
    UUID viewerUuid = playerRef.getUuid();
    boolean canClaim = isOfficer && PermissionManager.get().hasPermission(viewerUuid, Permissions.CLAIM);
    boolean canUnclaim = isOfficer && PermissionManager.get().hasPermission(viewerUuid, Permissions.UNCLAIM);
    boolean canOverclaim = isOfficer && PermissionManager.get().hasPermission(viewerUuid, Permissions.OVERCLAIM);

    // Fetch OG regions once for the entire grid (avoids per-chunk reflection calls)
    List<OrbisGuardIntegration.RegionInfo> ogRegions = OrbisGuardIntegration.isAvailable()
        ? OrbisGuardIntegration.getRegionsForWorld(worldName) : List.of();

    // Build 9 rows (z-4 to z+4)
    for (int zOffset = -GRID_RADIUS_Z; zOffset <= GRID_RADIUS_Z; zOffset++) {
      int rowIndex = zOffset + GRID_RADIUS_Z; // 0-8
      int chunkZ = centerZ + zOffset;

      // Create row container
      cmd.appendInline("#ChunkGrid", "Group { LayoutMode: Left; }");

      // Build 15 cells per row (x-7 to x+7)
      for (int xOffset = -GRID_RADIUS_X; xOffset <= GRID_RADIUS_X; xOffset++) {
        int colIndex = xOffset + GRID_RADIUS_X; // 0-14
        int chunkX = centerX + xOffset;

        // Get chunk info and color
        ChunkInfo info = getChunkInfo(worldName, chunkX, chunkZ, viewerFactionId, ogRegions);
        boolean isPlayerPos = (xOffset == 0 && zOffset == 0);

        // Create cell with territory color (always show real chunk color)
        cmd.appendInline("#ChunkGrid[" + rowIndex + "]",
            "Group { Anchor: (Width: " + CELL_SIZE + ", Height: " + CELL_SIZE + "); "
            + "Background: (Color: " + info.color + "); }");

        // Add "+" marker for player position (overlaid on chunk color)
        if (isPlayerPos) {
          String cellSelector = "#ChunkGrid[" + rowIndex + "][" + colIndex + "]";
          cmd.append(cellSelector, UIPaths.CHUNK_MARKER);
        }

        // Add button overlay for click events
        String cellSelector = "#ChunkGrid[" + rowIndex + "][" + colIndex + "]";
        cmd.append(cellSelector, UIPaths.CHUNK_BTN);

        // Bind click events based on chunk ownership (requires officer role + permission)
        if (canClaim || canUnclaim || canOverclaim) {
          bindChunkEvents(events, cellSelector + " #Btn", chunkX, chunkZ, info, viewerFactionId,
              canClaim, canUnclaim, canOverclaim);
        }
      }
    }
  }

  /**
   * Builds the 17x17 terrain map grid with semi-transparent claim overlays.
   * The terrain image is generated asynchronously and sent to the client.
   * On first build, the grid appears over a loading background; once the terrain
   * image is delivered, the page rebuilds with terrain visible.
   */
  private void buildTerrainMap(UICommandBuilder cmd, UIEventBuilder events,
                String worldName, int centerX, int centerZ,
                Faction viewerFaction) {
    // Start terrain generation if not already started for this page instance.
    // The dark placeholder PNG is registered at startup, so Background: "../Map.png"
    // always resolves (no red X). When the real terrain is ready, we send it and
    // delay the rebuild to let the client process the new asset.
    if (terrainAssetFuture == null) {
      // Send the placeholder asset to the client via the asset protocol.
      // The static Map.png in the JAR has a content-based hash, but our
      // generated terrain uses a fixed hash. Sending the placeholder first
      // ensures the client has a consistent asset entry at this path.
      ChunkMapAsset.sendToPlayer(playerRef.getPacketHandler(), ChunkMapAsset.empty());

      terrainAssetFuture = ChunkMapAsset.generate(playerRef, centerX, centerZ, TERRAIN_GRID_RADIUS);
      if (terrainAssetFuture != null) {
        terrainAssetFuture.thenAccept(asset -> {
          if (asset != null) {
            // Send real terrain image, then sendUpdate() to refresh the
            // existing page. sendUpdate() (clear=false) runs on the world
            // thread and does NOT rebuild the page from scratch — it just
            // triggers the client to re-render with the new asset.
            ChunkMapAsset.sendToPlayer(playerRef.getPacketHandler(), asset);
            this.sendUpdate();
          }
        });
      }
    }

    UUID viewerFactionId = viewerFaction != null ? viewerFaction.id() : null;
    boolean isOfficer = false;
    if (viewerFaction != null) {
      var member = viewerFaction.getMember(playerRef.getUuid());
      isOfficer = member != null && member.isOfficerOrHigher();
    }

    // Compute permission booleans once (officer role + server permission)
    UUID viewerUuid = playerRef.getUuid();
    boolean canClaim = isOfficer && PermissionManager.get().hasPermission(viewerUuid, Permissions.CLAIM);
    boolean canUnclaim = isOfficer && PermissionManager.get().hasPermission(viewerUuid, Permissions.UNCLAIM);
    boolean canOverclaim = isOfficer && PermissionManager.get().hasPermission(viewerUuid, Permissions.OVERCLAIM);

    // Fetch OG regions once for the entire grid
    List<OrbisGuardIntegration.RegionInfo> ogRegions = OrbisGuardIntegration.isAvailable()
        ? OrbisGuardIntegration.getRegionsForWorld(worldName) : List.of();

    // Build 17x17 grid using inline Groups (no borders, unlike TextButtons)
    for (int zOffset = -TERRAIN_GRID_RADIUS; zOffset <= TERRAIN_GRID_RADIUS; zOffset++) {
      int rowIndex = zOffset + TERRAIN_GRID_RADIUS;
      int chunkZ = centerZ + zOffset;

      // Row container - exactly 32px tall, children flow left-to-right
      cmd.appendInline("#ChunkGrid", "Group { LayoutMode: Left; Anchor: (Height: 32); }");

      for (int xOffset = -TERRAIN_GRID_RADIUS; xOffset <= TERRAIN_GRID_RADIUS; xOffset++) {
        int colIndex = xOffset + TERRAIN_GRID_RADIUS;
        int chunkX = centerX + xOffset;

        ChunkInfo info = getChunkInfo(worldName, chunkX, chunkZ, viewerFactionId, ogRegions);
        String alphaColor = getTerrainOverlayColor(info.type);

        // Create cell as inline Group with baked-in overlay color.
        // Groups have no inherent borders/outlines, eliminating grid lines.
        cmd.appendInline("#ChunkGrid[" + rowIndex + "]",
            "Group { Anchor: (Width: 32, Height: 32); Background: (Color: " + alphaColor + "); }");

        String cellSel = "#ChunkGrid[" + rowIndex + "][" + colIndex + "]";

        // Player marker at center
        if (xOffset == 0 && zOffset == 0) {
          cmd.append(cellSel, UIPaths.CHUNK_MAP_MARKER);
        }

        // Click events need a TextButton overlay (Activating doesn't work on Groups).
        // Only add the button on actionable cells where the player has the matching permission.
        boolean actionable = info.type != ChunkType.OG_PROTECTED
            && ((info.type == ChunkType.WILDERNESS && canClaim)
                || (info.type == ChunkType.OWN && canUnclaim)
                || (info.type == ChunkType.ENEMY && canOverclaim));
        if (actionable) {
          cmd.append(cellSel, UIPaths.CHUNK_MAP_TERRAIN_BTN);
          bindChunkEvents(events, cellSel + " #Btn", chunkX, chunkZ, info, viewerFactionId,
              canClaim, canUnclaim, canOverclaim);
        }
      }
    }
  }

  /**
   * Gets the semi-transparent overlay color for a chunk type in terrain mode.
   */
  private String getTerrainOverlayColor(ChunkType type) {
    return switch (type) {
      case OWN -> ALPHA_OWN;
      case ALLY -> ALPHA_ALLY;
      case ENEMY -> ALPHA_ENEMY;
      case OTHER -> ALPHA_OTHER;
      case SAFEZONE -> ALPHA_SAFEZONE;
      case WARZONE -> ALPHA_WARZONE;
      case OG_PROTECTED -> ALPHA_OG_PROTECTED;
      case WILDERNESS -> ALPHA_WILDERNESS;
    };
  }

  /**
   * Binds click events to a chunk cell based on its ownership state.
   * Events are bound to the #Cell Button element.
   * Only binds events for actions the player has permission to perform.
   */
  private void bindChunkEvents(UIEventBuilder events, String cellSelector,
                int chunkX, int chunkZ, ChunkInfo info, UUID viewerFactionId,
                boolean canClaim, boolean canUnclaim, boolean canOverclaim) {
    switch (info.type) {
      case WILDERNESS:
        if (canClaim) {
          // Left-click wilderness to claim
          events.addEventBinding(
              CustomUIEventBindingType.Activating,
              cellSelector,
              EventData.of("Button", "Claim")
                  .append("ChunkX", String.valueOf(chunkX))
                  .append("ChunkZ", String.valueOf(chunkZ)),
              false
          );
        }
        break;

      case OWN:
        if (canUnclaim) {
          // Right-click own territory to unclaim
          events.addEventBinding(
              CustomUIEventBindingType.RightClicking,
              cellSelector,
              EventData.of("Button", "Unclaim")
                  .append("ChunkX", String.valueOf(chunkX))
                  .append("ChunkZ", String.valueOf(chunkZ)),
              false
          );
        }
        break;

      case ENEMY:
        if (canOverclaim) {
          // Left-click enemy territory to attempt overclaim
          events.addEventBinding(
              CustomUIEventBindingType.Activating,
              cellSelector,
              EventData.of("Button", "Overclaim")
                  .append("ChunkX", String.valueOf(chunkX))
                  .append("ChunkZ", String.valueOf(chunkZ)),
              false
          );
        }
        break;

      // ALLY, OTHER, SAFEZONE, WARZONE - no click actions
      default:
        break;
    }
  }

  /**
   * Gets information about a chunk's ownership and display color.
   */
  private ChunkInfo getChunkInfo(String worldName, int chunkX, int chunkZ, UUID viewerFactionId,
                  List<OrbisGuardIntegration.RegionInfo> ogRegions) {
    // Check for zone first (zones can overlap with OG regions — show zone)
    Zone zone = zoneManager.getZone(worldName, chunkX, chunkZ);
    if (zone != null) {
      Logger.debug("[ChunkMapPage] Found zone '%s' at (%d, %d) in %s", zone.name(), chunkX, chunkZ, worldName);
      if (zone.type() == ZoneType.SAFE) {
        return new ChunkInfo(ChunkType.SAFEZONE, COLOR_SAFEZONE, null);
      } else {
        return new ChunkInfo(ChunkType.WARZONE, COLOR_WARZONE, null);
      }
    }

    // Check OrbisGuard protection (render whole chunk as protected if any OG region overlaps)
    if (!ogRegions.isEmpty()) {
      int minBlockX = chunkX << 5; // chunkX * 32
      int maxBlockX = minBlockX + 31;
      int minBlockZ = chunkZ << 5;
      int maxBlockZ = minBlockZ + 31;
      for (OrbisGuardIntegration.RegionInfo region : ogRegions) {
        if (region.maxX() >= minBlockX && region.minX() <= maxBlockX
          && region.maxZ() >= minBlockZ && region.minZ() <= maxBlockZ) {
          return new ChunkInfo(ChunkType.OG_PROTECTED, COLOR_OG_PROTECTED, null);
        }
      }
    }

    // Check claim ownership
    UUID ownerId = claimManager.getClaimOwner(worldName, chunkX, chunkZ);
    if (ownerId == null) {
      return new ChunkInfo(ChunkType.WILDERNESS, COLOR_WILDERNESS, null);
    }

    // It's claimed - determine relation
    if (viewerFactionId != null && ownerId.equals(viewerFactionId)) {
      return new ChunkInfo(ChunkType.OWN, COLOR_OWN, ownerId);
    }

    if (viewerFactionId != null) {
      RelationType relation = relationManager.getRelation(viewerFactionId, ownerId);
      return switch (relation) {
        case OWN -> new ChunkInfo(ChunkType.OWN, COLOR_OWN, ownerId);
        case ALLY -> new ChunkInfo(ChunkType.ALLY, COLOR_ALLY, ownerId);
        case ENEMY -> new ChunkInfo(ChunkType.ENEMY, COLOR_ENEMY, ownerId);
        case NEUTRAL -> new ChunkInfo(ChunkType.OTHER, COLOR_OTHER, ownerId);
      };
    }

    // Viewer has no faction, show as other
    return new ChunkInfo(ChunkType.OTHER, COLOR_OTHER, ownerId);
  }

  /**
   * Chunk type for event binding decisions.
   */
  private enum ChunkType {
    WILDERNESS, OWN, ALLY, ENEMY, OTHER, SAFEZONE, WARZONE, OG_PROTECTED
  }

  /**
   * Information about a chunk for display and interaction.
   */
  private record ChunkInfo(ChunkType type, String color, UUID ownerId) {}

  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                ChunkMapData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null || data.button == null) {
      return;
    }

    Faction viewerFaction = factionManager.getPlayerFaction(playerRef.getUuid());
    World world = player.getWorld();
    String worldName = world != null ? world.getName() : "world";

    // Handle navigation - use new player nav when no faction
    if (viewerFaction != null) {
      if (NavBarHelper.handleNavEvent(data, player, ref, store, playerRef, viewerFaction, guiManager)) {
        return;
      }
    } else {
      if (NewPlayerNavBarHelper.handleNavEvent(data, player, ref, store, playerRef, guiManager)) {
        return;
      }
    }

    switch (data.button) {
      case "Claim" -> handleClaim(player, playerRef, worldName, data.chunkX, data.chunkZ, ref, store);
      case "Unclaim" -> handleUnclaim(player, playerRef, worldName, data.chunkX, data.chunkZ, ref, store);
      case "Overclaim" -> handleOverclaim(player, playerRef, worldName, data.chunkX, data.chunkZ, ref, store);
      default -> {}
    }
  }

  private void handleClaim(Player player, PlayerRef playerRef, String worldName,
              int chunkX, int chunkZ, Ref<EntityStore> ref, Store<EntityStore> store) {
    ClaimManager.ClaimResult result = claimManager.claim(playerRef.getUuid(), worldName, chunkX, chunkZ);

    Message message = switch (result) {
      case SUCCESS -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.CLAIM_SUCCESS, chunkX, chunkZ)).color("#55FF55"));
      case NOT_IN_FACTION -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.CLAIM_NOT_IN_FACTION)).color("#FF5555"));
      case NOT_OFFICER -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.CLAIM_NOT_OFFICER)).color("#FF5555"));
      case ALREADY_CLAIMED_SELF -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.CLAIM_ALREADY_YOURS)).color("#FFAA00"));
      case ALREADY_CLAIMED_OTHER -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.CLAIM_ALREADY_CLAIMED)).color("#FF5555"));
      case NOT_ADJACENT -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.CLAIM_NOT_ADJACENT)).color("#FF5555"));
      case MAX_CLAIMS_REACHED -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.CLAIM_MAX)).color("#FF5555"));
      case WORLD_NOT_ALLOWED -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.CLAIM_WORLD_NOT_ALLOWED)).color("#FF5555"));
      case ORBISGUARD_PROTECTED -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.CLAIM_ORBISGUARD)).color("#FF5555"));
      default -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.CLAIM_FAILED)).color("#FF5555"));
    };

    player.sendMessage(message);

    // Rebuild the same page instance to update the overlay grid.
    // This preserves terrainAssetFuture, skipping terrain re-generation.
    rebuild();
  }

  private void handleUnclaim(Player player, PlayerRef playerRef, String worldName,
               int chunkX, int chunkZ, Ref<EntityStore> ref, Store<EntityStore> store) {
    ClaimManager.ClaimResult result = claimManager.unclaim(playerRef.getUuid(), worldName, chunkX, chunkZ);

    Message message = switch (result) {
      case SUCCESS -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.UNCLAIM_SUCCESS, chunkX, chunkZ)).color("#55FF55"));
      case NOT_IN_FACTION -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.UNCLAIM_NOT_IN_FACTION)).color("#FF5555"));
      case NOT_OFFICER -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.UNCLAIM_NOT_OFFICER)).color("#FF5555"));
      case CHUNK_NOT_CLAIMED -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.UNCLAIM_NOT_CLAIMED)).color("#FFAA00"));
      case NOT_YOUR_CLAIM -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.UNCLAIM_NOT_YOURS)).color("#FF5555"));
      case CANNOT_UNCLAIM_HOME -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.UNCLAIM_HOME)).color("#FF5555"));
      default -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.UNCLAIM_FAILED)).color("#FF5555"));
    };

    player.sendMessage(message);

    // Rebuild the same page instance to update the overlay grid.
    // This preserves terrainAssetFuture, skipping terrain re-generation.
    rebuild();
  }

  private void handleOverclaim(Player player, PlayerRef playerRef, String worldName,
                int chunkX, int chunkZ, Ref<EntityStore> ref, Store<EntityStore> store) {
    ClaimManager.ClaimResult result = claimManager.overclaim(playerRef.getUuid(), worldName, chunkX, chunkZ);

    Message message = switch (result) {
      case SUCCESS -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.OVERCLAIM_SUCCESS, chunkX, chunkZ)).color("#55FF55"));
      case NOT_IN_FACTION -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.OVERCLAIM_NOT_IN_FACTION)).color("#FF5555"));
      case NOT_OFFICER -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.OVERCLAIM_NOT_OFFICER)).color("#FF5555"));
      case ALREADY_CLAIMED_SELF -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.OVERCLAIM_ALREADY_YOURS)).color("#FFAA00"));
      case ALREADY_CLAIMED_ALLY -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.OVERCLAIM_ALLY)).color("#FF5555"));
      case TARGET_HAS_POWER -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.OVERCLAIM_HAS_POWER)).color("#FF5555"));
      case MAX_CLAIMS_REACHED -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.OVERCLAIM_MAX)).color("#FF5555"));
      default -> CommandUtil.prefix().insert(Message.raw(HFMessages.get(playerRef, MessageKeys.MapGui.OVERCLAIM_FAILED)).color("#FF5555"));
    };

    player.sendMessage(message);

    // Rebuild the same page instance to update the overlay grid.
    // This preserves terrainAssetFuture, skipping terrain re-generation.
    rebuild();
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
