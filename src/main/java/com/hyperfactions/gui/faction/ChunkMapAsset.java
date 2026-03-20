package com.hyperfactions.gui.faction;

import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.common.util.ArrayUtil;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.packets.setup.AssetFinalize;
import com.hypixel.hytale.protocol.packets.setup.AssetInitialize;
import com.hypixel.hytale.protocol.packets.setup.AssetPart;
import com.hypixel.hytale.protocol.packets.setup.RequestCommonAssetsRebuild;
import com.hypixel.hytale.server.core.asset.common.CommonAsset;
import com.hypixel.hytale.server.core.asset.common.CommonAssetRegistry;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.protocol.packets.worldmap.MapImage;
import com.hypixel.hytale.server.core.universe.world.worldmap.provider.chunk.ChunkWorldMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import org.jetbrains.annotations.Nullable;

/**
 * Dynamic terrain image asset for the chunk map GUI.
 * Generates a composite terrain PNG for a grid of chunks and sends it to a single player.
 *
 * <p>
 * Uses ChunkWorldMap.INSTANCE for pure terrain rendering (no claim overlays baked in).
 * Claim colors are overlaid via semi-transparent UI elements in ChunkMapPage.
 *
 * <p>
 * Based on SimpleClaims' ChunkInfoMapAsset pattern (MIT license).
 */
public class ChunkMapAsset extends CommonAsset {

  // "HyperFactionsMap" in hex, padded with leading 00 byte and trailing zeros to 32 bytes (64 hex chars)
  private static final String HASH = "004879706572466163746f6e734d617000000000000000000000000000000000";

  private static final String PATH = "UI/Custom/HyperFactions/Map.png";

  private final byte[] data;

  private ChunkMapAsset(byte[] data) {
    super(PATH, HASH, data);
    this.data = data;
  }

  /** Returns the blob0. */
  @Override
  protected CompletableFuture<byte[]> getBlob0() {
    return CompletableFuture.completedFuture(data);
  }

  /**
   * Returns the placeholder asset registered from the static Map.png in the JAR.
   * The server auto-loads JAR resources into CommonAssetRegistry on startup.
   * This placeholder is sent to the player before terrain generation starts,
   * ensuring a dark background is visible while terrain loads.
   *
   * @return the placeholder CommonAsset
   */
  public static CommonAsset empty() {
    return CommonAssetRegistry.getByName(PATH);
  }

  /**
   * Generates a terrain image for a square grid of chunks centered on the given position.
   *
   * @param player  the player (used to determine world)
   * @param centerX center chunk X coordinate
   * @param centerZ center chunk Z coordinate
   * @param radius  grid radius (e.g. 8 for a 17x17 grid)
   * @return future that resolves to the terrain asset, or null if world is unavailable
   */
  @Nullable
  public static CompletableFuture<ChunkMapAsset> generate(PlayerRef player, int centerX, int centerZ, int radius) {
    var worldId = player.getWorldUuid();
    if (worldId == null) {
      return null;
    }
    var world = Universe.get().getWorld(worldId);
    if (world == null) {
      return null;
    }

    // Fixed 32px per chunk — must match the 32px cell size in ChunkMapPage.
    // Do NOT use getImageScale() here: the world map scale may differ from 1.0,
    // which would make the terrain image wider/taller than the cell overlay grid.
    var partSize = 32;

    int minX = centerX - radius;
    int maxX = centerX + radius;
    int minZ = centerZ - radius;
    int maxZ = centerZ + radius;

    var chunks = new LongArraySet();
    for (int x = minX; x <= maxX; x++) {
      for (int z = minZ; z <= maxZ; z++) {
        chunks.add(ChunkUtil.indexChunk(x, z));
      }
    }

    return ChunkWorldMap.INSTANCE.generate(world, partSize, partSize, chunks).thenApply(map -> {
      long t0 = System.nanoTime();

      int gridWidth = maxX - minX + 1;
      int gridHeight = maxZ - minZ + 1;
      var image = new BufferedImage(
          partSize * gridWidth,
          partSize * gridHeight,
          BufferedImage.TYPE_INT_ARGB
      );

      // Pre-allocate a row buffer for bulk setRGB — eliminates ~83K individual setRGB calls
      int[] rowBuffer = new int[partSize];

      for (int x = minX; x <= maxX; x++) {
        for (int z = minZ; z <= maxZ; z++) {
          var index = ChunkUtil.indexChunk(x, z);
          var chunkImage = map.getChunks().get(index);
          if (chunkImage != null) {
            var pixels = decodePixels(chunkImage);
            var width = chunkImage.width;
            var height = chunkImage.height;

            if (pixels == null || width != partSize || height != partSize) {
              continue;
            }

            int imageX = (x - minX) * partSize;
            int imageZ = (z - minZ) * partSize;

            // Convert RGBA→ARGB and write one row at a time using bulk setRGB
            for (int row = 0; row < height; row++) {
              int rowOffset = row * width;
              for (int col = 0; col < width; col++) {
                int pixel = pixels[rowOffset + col];
                rowBuffer[col] = pixel << 24 | (pixel >> 8 & 0x00FFFFFF);
              }
              image.setRGB(imageX, imageZ + row, width, 1, rowBuffer, 0, width);
            }
          }
        }
      }

      long t1 = System.nanoTime();

      try {
        var baos = new ByteArrayOutputStream();

        // Use explicit PNG writer with faster compression (default is max compression)
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("PNG");
        if (writers.hasNext()) {
          ImageWriter writer = writers.next();
          ImageWriteParam param = writer.getDefaultWriteParam();
          if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.5f); // Faster compression, slightly larger file
          }
          writer.setOutput(new MemoryCacheImageOutputStream(baos));
          writer.write(null, new IIOImage(image, null, null), param);
          writer.dispose();
        } else {
          ImageIO.write(image, "PNG", baos);
        }

        long t2 = System.nanoTime();
        Logger.debug("[ChunkMap] Generated %dx%d terrain: compose=%dms, encode=%dms, size=%dKB",
          gridWidth, gridHeight,
          (t1 - t0) / 1_000_000, (t2 - t1) / 1_000_000,
          baos.size() / 1024);

        return new ChunkMapAsset(baos.toByteArray());
      } catch (IOException e) {
        ErrorHandler.report("Failed to encode terrain map PNG", e);
        return null;
      }
    });
  }

  /**
   * Decodes a palette-based MapImage back into a flat RGBA pixel array.
   */
  @Nullable
  private static int[] decodePixels(MapImage img) {
    if (img.palette == null || img.packedIndices == null) {
      return null;
    }
    int totalPixels = img.width * img.height;
    int[] pixels = new int[totalPixels];
    int bitsPerIndex = img.bitsPerIndex & 0xFF;
    if (bitsPerIndex == 0) {
      return pixels;
    }
    int mask = (1 << bitsPerIndex) - 1;
    for (int i = 0; i < totalPixels; i++) {
      int bitOffset = i * bitsPerIndex;
      int byteIndex = bitOffset / 8;
      int bitIndex = bitOffset % 8;
      int index = 0;
      if (byteIndex < img.packedIndices.length) {
        index = ((img.packedIndices[byteIndex] & 0xFF) >> bitIndex);
        if (bitIndex + bitsPerIndex > 8 && byteIndex + 1 < img.packedIndices.length) {
          index |= ((img.packedIndices[byteIndex + 1] & 0xFF) << (8 - bitIndex));
        }
        if (bitsPerIndex > 8 && byteIndex + 2 < img.packedIndices.length) {
          index |= ((img.packedIndices[byteIndex + 2] & 0xFF) << (16 - bitIndex));
        }
        index &= mask;
      }
      if (index >= 0 && index < img.palette.length) {
        pixels[i] = img.palette[index];
      }
    }
    return pixels;
  }

  /**
   * Sends a CommonAsset to a single player's client.
   * Splits the asset data into 2.5MB chunks and sends the appropriate packets.
   *
   * @param handler the player's packet handler
   * @param asset   the asset to send
   */
  public static void sendToPlayer(PacketHandler handler, CommonAsset asset) {
    byte[] allBytes = asset.getBlob().join();
    byte[][] parts = ArrayUtil.split(allBytes, 2621440);
    ToClientPacket[] packets = new ToClientPacket[2 + parts.length];
    packets[0] = new AssetInitialize(asset.toPacket(), allBytes.length);

    for (int partIndex = 0; partIndex < parts.length; ++partIndex) {
      packets[1 + partIndex] = new AssetPart(parts[partIndex]);
    }

    packets[packets.length - 1] = new AssetFinalize();
    handler.write(packets);
    handler.writeNoCache(new RequestCommonAssetsRebuild());
  }
}
