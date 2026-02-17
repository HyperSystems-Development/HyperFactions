package com.hyperfactions.gui.faction;

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
import com.hypixel.hytale.server.core.universe.world.worldmap.provider.chunk.ChunkWorldMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Dynamic terrain image asset for the chunk map GUI.
 * Generates a composite terrain PNG for a grid of chunks and sends it to a single player.
 * <p>
 * Uses ChunkWorldMap.INSTANCE for pure terrain rendering (no claim overlays baked in).
 * Claim colors are overlaid via semi-transparent UI elements in ChunkMapPage.
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
        if (worldId == null) return null;
        var world = Universe.get().getWorld(worldId);
        if (world == null) return null;

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
            int gridWidth = maxX - minX + 1;
            int gridHeight = maxZ - minZ + 1;
            var image = new BufferedImage(
                    partSize * gridWidth,
                    partSize * gridHeight,
                    BufferedImage.TYPE_INT_ARGB
            );

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    var index = ChunkUtil.indexChunk(x, z);
                    var chunkImage = map.getChunks().get(index);
                    if (chunkImage != null) {
                        var pixels = chunkImage.data;
                        var width = chunkImage.width;
                        var height = chunkImage.height;

                        if (pixels == null) continue;
                        if (width != partSize || height != partSize) continue;

                        int imageX = (x - minX) * partSize;
                        int imageZ = (z - minZ) * partSize;

                        for (var i = 0; i < pixels.length; i++) {
                            // MapImage uses RGBA format (alpha in low byte)
                            // BufferedImage TYPE_INT_ARGB uses ARGB format (alpha in high byte)
                            var pixel = pixels[i];
                            var argb = pixel << 24 | (pixel >> 8 & 0x00FFFFFF);

                            var pixelX = i % width;
                            var pixelY = i / width;
                            image.setRGB(imageX + pixelX, imageZ + pixelY, argb);
                        }
                    }
                }
            }

            try {
                var baos = new ByteArrayOutputStream();
                ImageIO.write(image, "PNG", baos);
                return new ChunkMapAsset(baos.toByteArray());
            } catch (IOException e) {
                Logger.severe("Failed to encode terrain map PNG", e);
                return null;
            }
        });
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
