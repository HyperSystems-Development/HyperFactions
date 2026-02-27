package com.hyperfactions.worldmap;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.Zone;
import com.hyperfactions.integration.protection.OrbisGuardIntegration;
import com.hyperfactions.manager.ClaimManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.ZoneManager;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.packets.worldmap.MapImage;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkColumn;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds world map images with claim overlays baked in.
 * This is a port of Hyfaction's CustomImageBuilder approach which generates
 * the entire map image from scratch with claim colors integrated during
 * pixel generation, rather than post-processing.
 *
 * <p>This approach is more reliable than the overlay post-process method
 * because it doesn't depend on pixel format assumptions or timing of
 * when image data is populated.
 */
public class ClaimImageBuilder {

  // SafeZone/WarZone colors (RGB format) - using distinct colors to avoid faction conflicts
  private static final int COLOR_SAFEZONE = 0x00CED1;      // Dark Turquoise (distinct from green 'a')
  private static final int COLOR_WARZONE = 0xFF5555;       // Red (matches GUI theme)
  private static final int COLOR_FACTION_DEFAULT = 0x55FFFF; // Cyan (default faction color)

  // Overlay transparency (0.0 = fully transparent, 1.0 = fully opaque)
  private static final float OVERLAY_OPACITY = 0.45f;       // 45% opacity for territory fill
  private static final float BORDER_OPACITY = 0.75f;        // 75% opacity for borders (more visible)

  // OrbisGuard region overlay (same color palette as OG's OrbisGuardMapImageBuilder)
  private static final int[] OG_DEFAULT_COLORS = {4886745, 8311585, 16098851, 13632027, 9442302, 5301186, 16312092, 12390624};

  private static final float OG_OVERLAY_OPACITY = 0.50f;    // 50% for region fill
  private static final float OG_BORDER_OPACITY = 0.70f;     // 70% for region borders

  // Border size in pixels
  private static final int BORDER_SIZE = 2;

  // Simple 3x5 bitmap font for uppercase letters and numbers
  // Each character is represented as 5 rows of 3 bits
  private static final int[][] FONT_3X5 = {
    // A-Z
    {0b010, 0b101, 0b111, 0b101, 0b101}, // A
    {0b110, 0b101, 0b110, 0b101, 0b110}, // B
    {0b011, 0b100, 0b100, 0b100, 0b011}, // C
    {0b110, 0b101, 0b101, 0b101, 0b110}, // D
    {0b111, 0b100, 0b110, 0b100, 0b111}, // E
    {0b111, 0b100, 0b110, 0b100, 0b100}, // F
    {0b011, 0b100, 0b101, 0b101, 0b011}, // G
    {0b101, 0b101, 0b111, 0b101, 0b101}, // H
    {0b111, 0b010, 0b010, 0b010, 0b111}, // I
    {0b001, 0b001, 0b001, 0b101, 0b010}, // J
    {0b101, 0b110, 0b100, 0b110, 0b101}, // K
    {0b100, 0b100, 0b100, 0b100, 0b111}, // L
    {0b101, 0b111, 0b111, 0b101, 0b101}, // M
    {0b101, 0b111, 0b111, 0b111, 0b101}, // N
    {0b010, 0b101, 0b101, 0b101, 0b010}, // O
    {0b110, 0b101, 0b110, 0b100, 0b100}, // P
    {0b010, 0b101, 0b101, 0b110, 0b011}, // Q
    {0b110, 0b101, 0b110, 0b101, 0b101}, // R
    {0b011, 0b100, 0b010, 0b001, 0b110}, // S
    {0b111, 0b010, 0b010, 0b010, 0b010}, // T
    {0b101, 0b101, 0b101, 0b101, 0b011}, // U
    {0b101, 0b101, 0b101, 0b010, 0b010}, // V
    {0b101, 0b101, 0b111, 0b111, 0b101}, // W
    {0b101, 0b101, 0b010, 0b101, 0b101}, // X
    {0b101, 0b101, 0b010, 0b010, 0b010}, // Y
    {0b111, 0b001, 0b010, 0b100, 0b111}, // Z
    // 0-9
    {0b010, 0b101, 0b101, 0b101, 0b010}, // 0
    {0b010, 0b110, 0b010, 0b010, 0b111}, // 1
    {0b110, 0b001, 0b010, 0b100, 0b111}, // 2
    {0b110, 0b001, 0b010, 0b001, 0b110}, // 3
    {0b101, 0b101, 0b111, 0b001, 0b001}, // 4
    {0b111, 0b100, 0b110, 0b001, 0b110}, // 5
    {0b011, 0b100, 0b110, 0b101, 0b010}, // 6
    {0b111, 0b001, 0b010, 0b010, 0b010}, // 7
    {0b010, 0b101, 0b010, 0b101, 0b010}, // 8
    {0b010, 0b101, 0b011, 0b001, 0b110}, // 9
  };

  private static final int FONT_CHAR_WIDTH = 3;

  private static final int FONT_CHAR_HEIGHT = 5;

  private static final int FONT_CHAR_SPACING = 1;

  private final long index;

  private final World world;

  @NotNull
  private final MapImage image;

  private final int sampleWidth;

  private final int sampleHeight;

  private final int blockStepX;

  private final int blockStepZ;

  @NotNull
  private final short[] heightSamples;

  @NotNull
  private final int[] tintSamples;

  @NotNull
  private final int[] blockSamples;

  @NotNull
  private final short[] neighborHeightSamples;

  @NotNull
  private final short[] fluidDepthSamples;

  @NotNull
  private final int[] environmentSamples;

  @NotNull
  private final int[] fluidSamples;

  private final Color outColor = new Color();

  @Nullable
  private WorldChunk worldChunk;

  private FluidSection[] fluidSections;

  // Manager references for claim lookups
  private final FactionManager factionManager;

  private final ClaimManager claimManager;

  private final ZoneManager zoneManager;

  /** Creates a new ClaimImageBuilder. */
  public ClaimImageBuilder(long index, int imageWidth, int imageHeight, World world,
              FactionManager factionManager, ClaimManager claimManager,
              ZoneManager zoneManager) {
    this.index = index;
    this.world = world;
    this.factionManager = factionManager;
    this.claimManager = claimManager;
    this.zoneManager = zoneManager;

    this.image = new MapImage(imageWidth, imageHeight, new int[imageWidth * imageHeight]);
    this.sampleWidth = Math.min(32, this.image.width);
    this.sampleHeight = Math.min(32, this.image.height);
    this.blockStepX = Math.max(1, 32 / this.image.width);
    this.blockStepZ = Math.max(1, 32 / this.image.height);
    this.heightSamples = new short[this.sampleWidth * this.sampleHeight];
    this.tintSamples = new int[this.sampleWidth * this.sampleHeight];
    this.blockSamples = new int[this.sampleWidth * this.sampleHeight];
    this.neighborHeightSamples = new short[(this.sampleWidth + 2) * (this.sampleHeight + 2)];
    this.fluidDepthSamples = new short[this.sampleWidth * this.sampleHeight];
    this.environmentSamples = new int[this.sampleWidth * this.sampleHeight];
    this.fluidSamples = new int[this.sampleWidth * this.sampleHeight];
  }

  /** Returns the index. */
  public long getIndex() {
    return this.index;
  }

  /** Returns the image. */
  @NotNull
  public MapImage getImage() {
    return this.image;
  }

  @NotNull
  private CompletableFuture<ClaimImageBuilder> fetchChunk() {
    int chunkX = ChunkUtil.xOfChunkIndex(this.index);
    int chunkZ = ChunkUtil.zOfChunkIndex(this.index);

    return this.world.getChunkStore().getChunkReferenceAsync(this.index).thenApplyAsync(ref -> {
      boolean valid = ref != null && ref.isValid();
      Logger.debugWorldMap("ClaimImageBuilder.fetchChunk() - index: %d (chunk %d,%d), ref found: %s",
          this.index, chunkX, chunkZ, valid);

      if (valid) {
        this.worldChunk = ref.getStore().getComponent(ref, WorldChunk.getComponentType());
        ChunkColumn chunkColumn = ref.getStore().getComponent(ref, ChunkColumn.getComponentType());
        this.fluidSections = new FluidSection[10];
        for (int y = 0; y < 10; ++y) {
          Ref sectionRef = chunkColumn.getSection(y);
          this.fluidSections[y] = this.world.getChunkStore().getStore().getComponent(sectionRef, FluidSection.getComponentType());
        }
        return this;
      }
      return null;
    }, (Executor) this.world);
  }

  @NotNull
  private CompletableFuture<ClaimImageBuilder> sampleNeighborsSync() {
    CompletableFuture<Void> north = this.world.getChunkStore().getChunkReferenceAsync(
        ChunkUtil.indexChunk(this.worldChunk.getX(), this.worldChunk.getZ() - 1)
    ).thenAcceptAsync(ref -> {
      if (ref != null && ref.isValid()) {
        WorldChunk worldChunk = ref.getStore().getComponent(ref, WorldChunk.getComponentType());
        int z = (this.sampleHeight - 1) * this.blockStepZ;
        for (int ix = 0; ix < this.sampleWidth; ++ix) {
          int x = ix * this.blockStepX;
          this.neighborHeightSamples[1 + ix] = worldChunk.getHeight(x, z);
        }
      }
    }, (Executor) this.world);

    CompletableFuture<Void> south = this.world.getChunkStore().getChunkReferenceAsync(
        ChunkUtil.indexChunk(this.worldChunk.getX(), this.worldChunk.getZ() + 1)
    ).thenAcceptAsync(ref -> {
      if (ref != null && ref.isValid()) {
        WorldChunk worldChunk = ref.getStore().getComponent(ref, WorldChunk.getComponentType());
        int z = 0;
        int neighbourStartIndex = (this.sampleHeight + 1) * (this.sampleWidth + 2) + 1;
        for (int ix = 0; ix < this.sampleWidth; ++ix) {
          int x = ix * this.blockStepX;
          this.neighborHeightSamples[neighbourStartIndex + ix] = worldChunk.getHeight(x, z);
        }
      }
    }, (Executor) this.world);

    CompletableFuture<Void> west = this.world.getChunkStore().getChunkReferenceAsync(
        ChunkUtil.indexChunk(this.worldChunk.getX() - 1, this.worldChunk.getZ())
    ).thenAcceptAsync(ref -> {
      if (ref != null && ref.isValid()) {
        WorldChunk worldChunk = ref.getStore().getComponent(ref, WorldChunk.getComponentType());
        int x = (this.sampleWidth - 1) * this.blockStepX;
        for (int iz = 0; iz < this.sampleHeight; ++iz) {
          int z = iz * this.blockStepZ;
          this.neighborHeightSamples[(iz + 1) * (this.sampleWidth + 2)] = worldChunk.getHeight(x, z);
        }
      }
    }, (Executor) this.world);

    CompletableFuture<Void> east = this.world.getChunkStore().getChunkReferenceAsync(
        ChunkUtil.indexChunk(this.worldChunk.getX() + 1, this.worldChunk.getZ())
    ).thenAcceptAsync(ref -> {
      if (ref != null && ref.isValid()) {
        WorldChunk worldChunk = ref.getStore().getComponent(ref, WorldChunk.getComponentType());
        int x = 0;
        for (int iz = 0; iz < this.sampleHeight; ++iz) {
          int z = iz * this.blockStepZ;
          this.neighborHeightSamples[(iz + 1) * (this.sampleWidth + 2) + this.sampleWidth + 1] = worldChunk.getHeight(x, z);
        }
      }
    }, (Executor) this.world);

    CompletableFuture<Void> northeast = this.world.getChunkStore().getChunkReferenceAsync(
        ChunkUtil.indexChunk(this.worldChunk.getX() + 1, this.worldChunk.getZ() - 1)
    ).thenAcceptAsync(ref -> {
      if (ref != null && ref.isValid()) {
        WorldChunk worldChunk = ref.getStore().getComponent(ref, WorldChunk.getComponentType());
        int x = 0;
        int z = (this.sampleHeight - 1) * this.blockStepZ;
        this.neighborHeightSamples[0] = worldChunk.getHeight(x, z);
      }
    }, (Executor) this.world);

    CompletableFuture<Void> northwest = this.world.getChunkStore().getChunkReferenceAsync(
        ChunkUtil.indexChunk(this.worldChunk.getX() - 1, this.worldChunk.getZ() - 1)
    ).thenAcceptAsync(ref -> {
      if (ref != null && ref.isValid()) {
        WorldChunk worldChunk = ref.getStore().getComponent(ref, WorldChunk.getComponentType());
        int x = (this.sampleWidth - 1) * this.blockStepX;
        int z = (this.sampleHeight - 1) * this.blockStepZ;
        this.neighborHeightSamples[this.sampleWidth + 1] = worldChunk.getHeight(x, z);
      }
    }, (Executor) this.world);

    CompletableFuture<Void> southeast = this.world.getChunkStore().getChunkReferenceAsync(
        ChunkUtil.indexChunk(this.worldChunk.getX() + 1, this.worldChunk.getZ() + 1)
    ).thenAcceptAsync(ref -> {
      if (ref != null && ref.isValid()) {
        WorldChunk worldChunk = ref.getStore().getComponent(ref, WorldChunk.getComponentType());
        int x = 0;
        int z = 0;
        this.neighborHeightSamples[(this.sampleHeight + 1) * (this.sampleWidth + 2) + this.sampleWidth + 1] = worldChunk.getHeight(x, z);
      }
    }, (Executor) this.world);

    CompletableFuture<Void> southwest = this.world.getChunkStore().getChunkReferenceAsync(
        ChunkUtil.indexChunk(this.worldChunk.getX() - 1, this.worldChunk.getZ() + 1)
    ).thenAcceptAsync(ref -> {
      if (ref != null && ref.isValid()) {
        WorldChunk worldChunk = ref.getStore().getComponent(ref, WorldChunk.getComponentType());
        int x = (this.sampleWidth - 1) * this.blockStepX;
        int z = 0;
        this.neighborHeightSamples[(this.sampleHeight + 1) * (this.sampleWidth + 2)] = worldChunk.getHeight(x, z);
      }
    }, (Executor) this.world);

    return CompletableFuture.allOf(north, south, west, east, northeast, northwest, southeast, southwest)
        .thenApply(v -> this);
  }

  private ClaimImageBuilder generateImageAsync() {
    // First pass: sample terrain data from the chunk
    for (int ix = 0; ix < this.sampleWidth; ++ix) {
      for (int iz = 0; iz < this.sampleHeight; ++iz) {
        int sampleIndex = iz * this.sampleWidth + ix;
        int x = ix * this.blockStepX;
        int z = iz * this.blockStepZ;
        int height = this.worldChunk.getHeight(x, z);
        int tint = this.worldChunk.getTint(x, z);

        this.heightSamples[sampleIndex] = (short) height;
        this.tintSamples[sampleIndex] = tint;
        this.blockSamples[sampleIndex] = this.worldChunk.getBlock(x, height, z);

        // Sample fluid data
        int fluidId = 0;
        int fluidTop = 320;
        Fluid fluid = null;
        int chunkYGround = ChunkUtil.chunkCoordinate(height);
        int chunkY = 9;

        block2:
        while (chunkY >= 0 && chunkY >= chunkYGround) {
          FluidSection fluidSection = this.fluidSections[chunkY];
          if (fluidSection != null && !fluidSection.isEmpty()) {
            int minBlockY = Math.max(ChunkUtil.minBlock(chunkY), height);
            int maxBlockY = ChunkUtil.maxBlock(chunkY);
            for (int blockY = maxBlockY; blockY >= minBlockY; --blockY) {
              fluidId = fluidSection.getFluidId(x, blockY, z);
              if (fluidId != 0) {
                fluid = Fluid.getAssetMap().getAsset(fluidId);
                fluidTop = blockY;
                break block2;
              }
            }
          }
          --chunkY;
        }

        int fluidBottom = height;
        block4:
        while (chunkY >= 0 && chunkY >= chunkYGround) {
          FluidSection fluidSection = this.fluidSections[chunkY];
          if (fluidSection == null || fluidSection.isEmpty()) {
            fluidBottom = Math.min(ChunkUtil.maxBlock(chunkY) + 1, fluidTop);
            break;
          }
          int minBlockY = Math.max(ChunkUtil.minBlock(chunkY), height);
          int maxBlockY = Math.min(ChunkUtil.maxBlock(chunkY), fluidTop - 1);
          for (int blockY = maxBlockY; blockY >= minBlockY; --blockY) {
            int nextFluidId = fluidSection.getFluidId(x, blockY, z);
            if (nextFluidId != fluidId) {
              Fluid nextFluid = Fluid.getAssetMap().getAsset(nextFluidId);
              if (!Objects.equals(fluid.getParticleColor(), nextFluid.getParticleColor())) {
                fluidBottom = blockY + 1;
                break block4;
              }
            }
          }
          --chunkY;
        }

        short fluidDepth = fluidId != 0 ? (short) (fluidTop - fluidBottom + 1) : 0;
        int environmentId = this.worldChunk.getBlockChunk().getEnvironment(x, fluidTop, z);
        this.fluidDepthSamples[sampleIndex] = fluidDepth;
        this.environmentSamples[sampleIndex] = environmentId;
        this.fluidSamples[sampleIndex] = fluidId;
      }
    }

    // Setup for pixel generation
    float imageToSampleRatioWidth = (float) this.sampleWidth / (float) this.image.width;
    float imageToSampleRatioHeight = (float) this.sampleHeight / (float) this.image.height;
    int blockPixelWidth = Math.max(1, this.image.width / this.sampleWidth);
    int blockPixelHeight = Math.max(1, this.image.height / this.sampleHeight);

    // Copy height samples for neighbor lookup
    for (int iz = 0; iz < this.sampleHeight; ++iz) {
      System.arraycopy(this.heightSamples, iz * this.sampleWidth,
          this.neighborHeightSamples, (iz + 1) * (this.sampleWidth + 2) + 1, this.sampleWidth);
    }

    // Get chunk coordinates for claim lookups
    int chunkX = ChunkUtil.xOfChunkIndex(this.index);
    int chunkZ = ChunkUtil.zOfChunkIndex(this.index);
    String worldName = this.worldChunk.getWorld().getName();

    // Check claim status for this chunk
    boolean showClaimsOnMap = ConfigManager.get().isWorldMapMarkersEnabled();
    boolean isSafeZone = zoneManager.isInSafeZone(worldName, chunkX, chunkZ);
    boolean isWarZone = zoneManager.isInWarZone(worldName, chunkX, chunkZ);
    UUID claimOwner = claimManager.getClaimOwner(worldName, chunkX, chunkZ);
    Faction factionInfo = claimOwner != null ? factionManager.getFaction(claimOwner) : null;

    // Debug logging: log world name and claim lookup info periodically
    // Only log for chunk 0,0 and claimed chunks to reduce spam
    if (chunkX == 0 && chunkZ == 0) {
      // Log once per map generation for diagnostic info
      int totalClaims = 0;
      int totalFactions = 0;
      for (Faction f : factionManager.getAllFactions()) {
        totalFactions++;
        totalClaims += f.getClaimCount();
      }
      Logger.debugWorldMap("ClaimImageBuilder: world='%s', totalFactions=%d, totalClaims=%d, showClaims=%s",
          worldName, totalFactions, totalClaims, showClaimsOnMap);
    }

    // Debug logging for claimed chunks only
    if (isSafeZone || isWarZone || factionInfo != null) {
      Logger.debugWorldMap("ClaimImageBuilder: chunk %d,%d world='%s' - zone=%s, faction=%s",
          chunkX, chunkZ, worldName,
          isSafeZone ? "safe" : (isWarZone ? "war" : "none"),
          factionInfo != null ? factionInfo.name() : "none");
    }

    // Pre-calculate neighbor claim data for border detection
    boolean[] nearbySafeZones = new boolean[]{
        zoneManager.isInSafeZone(worldName, chunkX, chunkZ + 1),
        zoneManager.isInSafeZone(worldName, chunkX, chunkZ - 1),
        zoneManager.isInSafeZone(worldName, chunkX + 1, chunkZ),
        zoneManager.isInSafeZone(worldName, chunkX - 1, chunkZ)
    };
    boolean[] nearbyWarZones = new boolean[]{
        zoneManager.isInWarZone(worldName, chunkX, chunkZ + 1),
        zoneManager.isInWarZone(worldName, chunkX, chunkZ - 1),
        zoneManager.isInWarZone(worldName, chunkX + 1, chunkZ),
        zoneManager.isInWarZone(worldName, chunkX - 1, chunkZ)
    };

    // Pre-calculate neighbor faction claims for border detection
    UUID[] nearbyChunkOwners = new UUID[]{
        claimManager.getClaimOwner(worldName, chunkX, chunkZ + 1),
        claimManager.getClaimOwner(worldName, chunkX, chunkZ - 1),
        claimManager.getClaimOwner(worldName, chunkX + 1, chunkZ),
        claimManager.getClaimOwner(worldName, chunkX - 1, chunkZ)
    };

    // OrbisGuard region candidates for this chunk (block-level, not chunk-level)
    List<OrbisGuardIntegration.RegionInfo> ogCandidates = null;
    int ogMinBlockX = 0, ogMinBlockZ = 0;
    if (OrbisGuardIntegration.isAvailable()) {
      List<OrbisGuardIntegration.RegionInfo> allRegions = OrbisGuardIntegration.getRegionsForWorld(worldName);
      if (!allRegions.isEmpty()) {
        ogMinBlockX = ChunkUtil.minBlock(chunkX);
        ogMinBlockZ = ChunkUtil.minBlock(chunkZ);
        int ogMaxBlockX = ogMinBlockX + 31;
        int ogMaxBlockZ = ogMinBlockZ + 31;
        ogCandidates = new ArrayList<>();
        for (OrbisGuardIntegration.RegionInfo region : allRegions) {
          // 2D AABB overlap test (ignore Y for map rendering)
          if (region.maxX() >= ogMinBlockX && region.minX() <= ogMaxBlockX
            && region.maxZ() >= ogMinBlockZ && region.minZ() <= ogMaxBlockZ) {
            ogCandidates.add(region);
          }
        }
        if (ogCandidates.isEmpty()) {
          ogCandidates = null;
        }
      }
    }

    // Second pass: generate pixels with claim overlays
    for (int ix = 0; ix < this.image.width; ++ix) {
      for (int iz = 0; iz < this.image.height; ++iz) {
        int sampleX = Math.min((int) ((float) ix * imageToSampleRatioWidth), this.sampleWidth - 1);
        int sampleZ = Math.min((int) ((float) iz * imageToSampleRatioHeight), this.sampleHeight - 1);
        int sampleIndex = sampleZ * this.sampleWidth + sampleX;
        int blockPixelX = ix % blockPixelWidth;
        int blockPixelZ = iz % blockPixelHeight;
        short height = this.heightSamples[sampleIndex];
        int tint = this.tintSamples[sampleIndex];
        int blockId = this.blockSamples[sampleIndex];

        // Get base block color
        getBlockColor(blockId, tint, this.outColor);

        // Apply OrbisGuard region overlay (per-pixel, block-level)
        if (showClaimsOnMap && ogCandidates != null) {
          int blockX = ogMinBlockX + ix * 32 / this.image.width;
          int blockZ = ogMinBlockZ + iz * 32 / this.image.height;
          OrbisGuardIntegration.RegionInfo topOgRegion = getTopOgRegion(ogCandidates, blockX, blockZ);
          if (topOgRegion != null) {
            int regionColor = getOgRegionColor(topOgRegion);
            boolean ogBorder = isOgRegionBorder(topOgRegion, blockX, blockZ);
            blendOgColor(regionColor, this.outColor, ogBorder);
          }
        }

        // Apply claim overlay if enabled (renders ON TOP of OG regions)
        if (showClaimsOnMap) {
          if (isSafeZone) {
            boolean isBorder = isBorderPixel(ix, iz, nearbySafeZones);
            getForceBlockColor(blockId, COLOR_SAFEZONE, this.outColor, isBorder);
          } else if (isWarZone) {
            boolean isBorder = isBorderPixel(ix, iz, nearbyWarZones);
            getForceBlockColor(blockId, COLOR_WARZONE, this.outColor, isBorder);
          } else if (factionInfo != null) {
            boolean isBorder = isFactionBorderPixel(ix, iz, factionInfo.id(), nearbyChunkOwners);
            int factionColor = colorCodeToHex(factionInfo.color());
            getForceBlockColor(blockId, factionColor, this.outColor, isBorder);
          }
        }

        // Calculate terrain shading
        short north = this.neighborHeightSamples[sampleZ * (this.sampleWidth + 2) + sampleX + 1];
        short south = this.neighborHeightSamples[(sampleZ + 2) * (this.sampleWidth + 2) + sampleX + 1];
        short west = this.neighborHeightSamples[(sampleZ + 1) * (this.sampleWidth + 2) + sampleX];
        short east = this.neighborHeightSamples[(sampleZ + 1) * (this.sampleWidth + 2) + sampleX + 2];
        short northWest = this.neighborHeightSamples[sampleZ * (this.sampleWidth + 2) + sampleX];
        short northEast = this.neighborHeightSamples[sampleZ * (this.sampleWidth + 2) + sampleX + 2];
        short southWest = this.neighborHeightSamples[(sampleZ + 2) * (this.sampleWidth + 2) + sampleX];
        short southEast = this.neighborHeightSamples[(sampleZ + 2) * (this.sampleWidth + 2) + sampleX + 2];

        float shade = shadeFromHeights(blockPixelX, blockPixelZ, blockPixelWidth, blockPixelHeight,
            height, north, south, west, east, northWest, northEast, southWest, southEast);
        this.outColor.multiply(shade);

        // Apply fluid color if present
        int fluidId = this.fluidSamples[sampleIndex];
        if (height < 320 && fluidId != 0) {
          short fluidDepth = this.fluidDepthSamples[sampleIndex];
          int environmentId = this.environmentSamples[sampleIndex];
          getFluidColor(fluidId, environmentId, fluidDepth, this.outColor);
        }

        // Pack pixel
        this.image.data[iz * this.image.width + ix] = this.outColor.pack();
      }
    }

    // Draw faction tag text on claimed chunks (upper-left corner)
    if (showClaimsOnMap && factionInfo != null && ConfigManager.get().worldMap().isShowFactionTags()) {
      String tag = factionInfo.tag();
      if (tag != null && !tag.isEmpty()) {
        // Use white text with dark outline for visibility on any color
        drawTextUpperLeft(tag, 0xFFFFFF);
      }
    }

    return this;
  }

  /**
   * Checks if a pixel is on the border of a zone claim.
   */
  private boolean isBorderPixel(int ix, int iz, boolean[] neighborsSameZone) {
    // South, North, East, West
    if (iz >= this.image.height - BORDER_SIZE - 1 && !neighborsSameZone[0]) {
      return true;
    }
    if (iz <= BORDER_SIZE && !neighborsSameZone[1]) {
      return true;
    }
    if (ix >= this.image.width - BORDER_SIZE - 1 && !neighborsSameZone[2]) {
      return true;
    }
    if (ix <= BORDER_SIZE && !neighborsSameZone[3]) {
      return true;
    }
    return false;
  }

  /**
   * Checks if a pixel is on the border of a faction claim.
   */
  private boolean isFactionBorderPixel(int ix, int iz, UUID factionId, UUID[] nearbyOwners) {
    // Check if at chunk edge and neighbor is different faction
    if (iz >= this.image.height - BORDER_SIZE - 1 && !factionId.equals(nearbyOwners[0])) {
      return true;
    }
    if (iz <= BORDER_SIZE && !factionId.equals(nearbyOwners[1])) {
      return true;
    }
    if (ix >= this.image.width - BORDER_SIZE - 1 && !factionId.equals(nearbyOwners[2])) {
      return true;
    }
    if (ix <= BORDER_SIZE && !factionId.equals(nearbyOwners[3])) {
      return true;
    }
    return false;
  }

  /**
   * Converts a hex color string (#RRGGBB) to RGB int.
   * Falls back to legacy single-char codes for backwards compatibility.
   */
  private int colorCodeToHex(@Nullable String color) {
    if (color == null || color.isEmpty()) {
      return COLOR_FACTION_DEFAULT;
    }
    if (color.startsWith("#") && color.length() >= 7) {
      return com.hyperfactions.util.LegacyColorParser.hexToRgbInt(color);
    }

    // Fallback: legacy single-char code
    String hex = com.hyperfactions.util.LegacyColorParser.codeToHex(color.charAt(0));
    return com.hyperfactions.util.LegacyColorParser.hexToRgbInt(hex);
  }

  private static float shadeFromHeights(int blockPixelX, int blockPixelZ, int blockPixelWidth, int blockPixelHeight,
                     short height, short north, short south, short west, short east,
                     short northWest, short northEast, short southWest, short southEast) {
    float u = ((float) blockPixelX + 0.5f) / (float) blockPixelWidth;
    float v = ((float) blockPixelZ + 0.5f) / (float) blockPixelHeight;
    float ud = (u + v) / 2.0f;
    float vd = (1.0f - u + v) / 2.0f;
    float dhdx1 = (float) (height - west) * (1.0f - u) + (float) (east - height) * u;
    float dhdz1 = (float) (height - north) * (1.0f - v) + (float) (south - height) * v;
    float dhdx2 = (float) (height - northWest) * (1.0f - ud) + (float) (southEast - height) * ud;
    float dhdz2 = (float) (height - northEast) * (1.0f - vd) + (float) (southWest - height) * vd;
    float dhdx = dhdx1 * 2.0f + dhdx2;
    float dhdz = dhdz1 * 2.0f + dhdz2;
    float dy = 3.0f;
    float invS = 1.0f / (float) Math.sqrt(dhdx * dhdx + dy * dy + dhdz * dhdz);
    float nx = dhdx * invS;
    float ny = dy * invS;
    float nz = dhdz * invS;
    float lx = -0.2f;
    float ly = 0.8f;
    float lz = 0.5f;
    float invL = 1.0f / (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
    lx *= invL;
    ly *= invL;
    lz *= invL;
    float lambert = Math.max(0.0f, nx * lx + ny * ly + nz * lz);
    float ambient = 0.4f;
    float diffuse = 0.6f;
    return ambient + diffuse * lambert;
  }

  private static void getBlockColor(int blockId, int biomeTintColor, @NotNull Color outColor) {
    BlockType block = BlockType.getAssetMap().getAsset(blockId);
    int biomeTintR = (biomeTintColor >> 16) & 0xFF;
    int biomeTintG = (biomeTintColor >> 8) & 0xFF;
    int biomeTintB = biomeTintColor & 0xFF;
    com.hypixel.hytale.protocol.Color[] tintUp = block.getTintUp();
    boolean hasTint = tintUp != null && tintUp.length > 0;
    int selfTintR = hasTint ? tintUp[0].red & 0xFF : 255;
    int selfTintG = hasTint ? tintUp[0].green & 0xFF : 255;
    int selfTintB = hasTint ? tintUp[0].blue & 0xFF : 255;
    float biomeTintMultiplier = (float) block.getBiomeTintUp() / 100.0f;
    int tintColorR = (int) ((float) selfTintR + (float) (biomeTintR - selfTintR) * biomeTintMultiplier);
    int tintColorG = (int) ((float) selfTintG + (float) (biomeTintG - selfTintG) * biomeTintMultiplier);
    int tintColorB = (int) ((float) selfTintB + (float) (biomeTintB - selfTintB) * biomeTintMultiplier);
    com.hypixel.hytale.protocol.Color particleColor = block.getParticleColor();
    if (particleColor != null && biomeTintMultiplier < 1.0f) {
      tintColorR = tintColorR * (particleColor.red & 0xFF) / 255;
      tintColorG = tintColorG * (particleColor.green & 0xFF) / 255;
      tintColorB = tintColorB * (particleColor.blue & 0xFF) / 255;
    }
    outColor.r = tintColorR & 0xFF;
    outColor.g = tintColorG & 0xFF;
    outColor.b = tintColorB & 0xFF;
    outColor.a = 255;
  }

  /**
   * Blends the overlay color with the existing terrain color using alpha blending.
   * This creates a semi-transparent overlay effect that preserves terrain detail.
   */
  private static void getForceBlockColor(int blockId, int forceColor, @NotNull Color outColor, boolean isBorder) {
    // Extract overlay RGB
    int overlayR = (forceColor >> 16) & 0xFF;
    int overlayG = (forceColor >> 8) & 0xFF;
    int overlayB = forceColor & 0xFF;

    // Use higher opacity for borders to make them more visible
    float opacity = isBorder ? BORDER_OPACITY : OVERLAY_OPACITY;

    // Alpha blend: result = overlay * opacity + base * (1 - opacity)
    outColor.r = (int) (overlayR * opacity + outColor.r * (1.0f - opacity));
    outColor.g = (int) (overlayG * opacity + outColor.g * (1.0f - opacity));
    outColor.b = (int) (overlayB * opacity + outColor.b * (1.0f - opacity));
    outColor.a = 255;

    // Darken borders slightly for better definition
    if (isBorder) {
      outColor.multiply(0.85f);
    }
  }

  private static void getFluidColor(int fluidId, int environmentId, int fluidDepth, @NotNull Color outColor) {
    int tintColorR = 255;
    int tintColorG = 255;
    int tintColorB = 255;
    Environment environment = Environment.getAssetMap().getAsset(environmentId);
    com.hypixel.hytale.protocol.Color waterTint = environment.getWaterTint();
    if (waterTint != null) {
      tintColorR = tintColorR * (waterTint.red & 0xFF) / 255;
      tintColorG = tintColorG * (waterTint.green & 0xFF) / 255;
      tintColorB = tintColorB * (waterTint.blue & 0xFF) / 255;
    }
    Fluid fluid = Fluid.getAssetMap().getAsset(fluidId);
    com.hypixel.hytale.protocol.Color particleColor = fluid.getParticleColor();
    if (particleColor != null) {
      tintColorR = tintColorR * (particleColor.red & 0xFF) / 255;
      tintColorG = tintColorG * (particleColor.green & 0xFF) / 255;
      tintColorB = tintColorB * (particleColor.blue & 0xFF) / 255;
    }
    float depthMultiplier = Math.min(1.0f, 1.0f / (float) fluidDepth);
    outColor.r = (int) ((float) tintColorR + (float) ((outColor.r & 0xFF) - tintColorR) * depthMultiplier) & 0xFF;
    outColor.g = (int) ((float) tintColorG + (float) ((outColor.g & 0xFF) - tintColorG) * depthMultiplier) & 0xFF;
    outColor.b = (int) ((float) tintColorB + (float) ((outColor.b & 0xFF) - tintColorB) * depthMultiplier) & 0xFF;
  }

  /**
   * Builds a map image asynchronously.
   */
  @NotNull
  public static CompletableFuture<ClaimImageBuilder> build(long index, int imageWidth, int imageHeight, World world,
                               FactionManager factionManager, ClaimManager claimManager,
                               ZoneManager zoneManager) {
    ClaimImageBuilder builder = new ClaimImageBuilder(index, imageWidth, imageHeight, world,
        factionManager, claimManager, zoneManager);
    return CompletableFuture.completedFuture(builder)
        .thenCompose(ClaimImageBuilder::fetchChunk)
        .thenCompose(b -> b != null ? b.sampleNeighborsSync() : CompletableFuture.completedFuture(null))
        .thenApplyAsync(b -> b != null ? b.generateImageAsync() : null);
  }

  // ============ OrbisGuard Region Rendering ============

  /**
   * Finds the highest-priority OrbisGuard region containing the given block position (2D).
   */
  @Nullable
  private static OrbisGuardIntegration.RegionInfo getTopOgRegion(
      List<OrbisGuardIntegration.RegionInfo> candidates, int blockX, int blockZ) {
    OrbisGuardIntegration.RegionInfo top = null;
    int highestPriority = Integer.MIN_VALUE;
    for (OrbisGuardIntegration.RegionInfo region : candidates) {
      if (blockX >= region.minX() && blockX <= region.maxX()
        && blockZ >= region.minZ() && blockZ <= region.maxZ()
        && region.priority() > highestPriority) {
        highestPriority = region.priority();
        top = region;
      }
    }
    return top;
  }

  /**
   * Gets the color for an OrbisGuard region using OG's hash-based palette.
   */
  private static int getOgRegionColor(OrbisGuardIntegration.RegionInfo region) {
    int colorIndex = Math.abs(region.id().hashCode()) % OG_DEFAULT_COLORS.length;
    return OG_DEFAULT_COLORS[colorIndex];
  }

  /**
   * Checks if a block position is on the edge of an OrbisGuard region (2D).
   */
  private static boolean isOgRegionBorder(OrbisGuardIntegration.RegionInfo region, int blockX, int blockZ) {
    return blockX == region.minX() || blockX == region.maxX()
       || blockZ == region.minZ() || blockZ == region.maxZ();
  }

  /**
   * Blends an OrbisGuard region color onto the terrain color with opacity.
   */
  private static void blendOgColor(int color, Color outColor, boolean isBorder) {
    int r = (color >> 16) & 0xFF;
    int g = (color >> 8) & 0xFF;
    int b = color & 0xFF;
    float opacity = isBorder ? OG_BORDER_OPACITY : OG_OVERLAY_OPACITY;
    // Borders are darkened for visual definition (matches OG's 0.6x multiplier)
    if (isBorder) {
      r = (int) (r * 0.6f);
      g = (int) (g * 0.6f);
      b = (int) (b * 0.6f);
    }
    outColor.r = (int) (outColor.r * (1.0f - opacity) + r * opacity);
    outColor.g = (int) (outColor.g * (1.0f - opacity) + g * opacity);
    outColor.b = (int) (outColor.b * (1.0f - opacity) + b * opacity);
  }

  /**
   * Internal color utility class.
   */
  private static class Color {
    public int r;

    public int g;

    public int b;

    public int a;

    /** Pack. */
    public int pack() {
      return ((r & 0xFF) << 24) | ((g & 0xFF) << 16) | ((b & 0xFF) << 8) | (a & 0xFF);
    }

    public void multiply(float value) {
      this.r = Math.min(255, Math.max(0, (int) ((float) this.r * value)));
      this.g = Math.min(255, Math.max(0, (int) ((float) this.g * value)));
      this.b = Math.min(255, Math.max(0, (int) ((float) this.b * value)));
    }
  }

  // ============ Text Rendering Methods ============

  /**
   * Draws text in the upper-left corner of the image, just inside the border.
   *
   * @param text      the text to draw (uppercase letters and numbers only)
   * @param textColor the color of the text (RGB)
   */
  private void drawTextUpperLeft(@Nullable String text, int textColor) {
    if (text == null || text.isEmpty()) {
      return;
    }

    // Convert to uppercase and limit length
    text = text.toUpperCase();
    if (text.length() > 5) {
      text = text.substring(0, 5);
    }

    // Position just inside the border (BORDER_SIZE + 1 pixel padding)
    int startX = BORDER_SIZE + 1;
    int startY = BORDER_SIZE + 1;

    // Draw each character
    int cursorX = startX;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      int[] charBitmap = getCharBitmap(c);
      if (charBitmap != null) {
        drawChar(cursorX, startY, charBitmap, textColor);
      }
      cursorX += FONT_CHAR_WIDTH + FONT_CHAR_SPACING;
    }
  }

  /**
   * Gets the bitmap data for a character.
   */
  @Nullable
  private static int[] getCharBitmap(char c) {
    if (c >= 'A' && c <= 'Z') {
      return FONT_3X5[c - 'A'];
    } else if (c >= '0' && c <= '9') {
      return FONT_3X5[26 + (c - '0')];
    }
    return null; // Unknown character
  }

  /**
   * Draws a single character at the specified position.
   */
  private void drawChar(int x, int y, int[] charBitmap, int textColor) {
    int textR = (textColor >> 16) & 0xFF;
    int textG = (textColor >> 8) & 0xFF;
    int textB = textColor & 0xFF;

    // Draw with outline for better visibility
    for (int row = 0; row < FONT_CHAR_HEIGHT; row++) {
      int bits = charBitmap[row];
      for (int col = 0; col < FONT_CHAR_WIDTH; col++) {
        boolean isSet = ((bits >> (FONT_CHAR_WIDTH - 1 - col)) & 1) == 1;
        int px = x + col;
        int py = y + row;

        if (px >= 0 && px < this.image.width && py >= 0 && py < this.image.height) {
          if (isSet) {
            // Draw text pixel
            setPixel(px, py, textR, textG, textB);
          } else {
            // Draw outline (darker background) for visibility
            drawOutlineIfNeeded(px, py, x, y, charBitmap);
          }
        }
      }
    }
  }

  /**
   * Draws an outline pixel if adjacent to a text pixel.
   */
  private void drawOutlineIfNeeded(int px, int py, int charX, int charY, int[] charBitmap) {
    // Check if any adjacent pixel is part of the text
    int[][] offsets = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    for (int[] offset : offsets) {
      int checkX = px + offset[0] - charX;
      int checkY = py + offset[1] - charY;

      if (checkX >= 0 && checkX < FONT_CHAR_WIDTH && checkY >= 0 && checkY < FONT_CHAR_HEIGHT) {
        int bits = charBitmap[checkY];
        boolean isSet = ((bits >> (FONT_CHAR_WIDTH - 1 - checkX)) & 1) == 1;
        if (isSet) {
          // Adjacent to text, draw dark outline
          darkenPixel(px, py, 0.4f);
          return;
        }
      }
    }
  }

  /**
   * Sets a pixel to a specific color.
   */
  private void setPixel(int x, int y, int r, int g, int b) {
    if (x >= 0 && x < this.image.width && y >= 0 && y < this.image.height) {
      this.image.data[y * this.image.width + x] = ((r & 0xFF) << 24) | ((g & 0xFF) << 16) | ((b & 0xFF) << 8) | 0xFF;
    }
  }

  /**
   * Darkens a pixel by a multiplier for outline effect.
   */
  private void darkenPixel(int x, int y, float multiplier) {
    if (x >= 0 && x < this.image.width && y >= 0 && y < this.image.height) {
      int pixel = this.image.data[y * this.image.width + x];
      int r = (int) (((pixel >> 24) & 0xFF) * multiplier);
      int g = (int) (((pixel >> 16) & 0xFF) * multiplier);
      int b = (int) (((pixel >> 8) & 0xFF) * multiplier);
      int a = pixel & 0xFF;
      this.image.data[y * this.image.width + x] = ((r & 0xFF) << 24) | ((g & 0xFF) << 16) | ((b & 0xFF) << 8) | (a & 0xFF);
    }
  }
}
