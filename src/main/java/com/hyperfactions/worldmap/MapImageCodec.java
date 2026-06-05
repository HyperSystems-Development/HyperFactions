package com.hyperfactions.worldmap;

import com.hypixel.hytale.protocol.packets.worldmap.MapImage;
import com.hypixel.hytale.server.core.universe.world.chunk.palette.BitFieldArr;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Encodes/decodes {@link MapImage} pixel data.
 *
 * <p>As of Hytale 0.5.3, {@link MapImage} no longer carries a raw {@code int[] data}
 * field. Instead it stores a colour {@code palette} plus bit-packed {@code packedIndices}
 * (one palette index per pixel, packed at {@code bitsPerIndex} bits via {@link BitFieldArr}).
 * This mirrors the engine's own {@code worldmap.provider.chunk.ImageBuilder#encodeToPalette}
 * so the client renders our images identically.
 *
 * <p>Pixels are RGBA-packed ints ({@code r<<24 | g<<16 | b<<8 | a}), the same format the
 * engine's {@code ImageBuilder.Color.pack()} produces.
 */
public final class MapImageCodec {

  private MapImageCodec() {}

  /**
   * Encodes a raw RGBA pixel buffer into {@code image}'s palette/packedIndices fields.
   * The image's {@code width}/{@code height} must already be set and match {@code pixels.length}.
   */
  public static void encodeInto(MapImage image, int[] pixels) {
    // Assign a palette index to each distinct colour, in first-seen order.
    LinkedHashMap<Integer, Integer> colorToIndex = new LinkedHashMap<>();
    int next = 0;
    for (int pixel : pixels) {
      if (!colorToIndex.containsKey(pixel)) {
        colorToIndex.put(pixel, next++);
      }
    }

    int[] palette = new int[colorToIndex.size()];
    for (Map.Entry<Integer, Integer> entry : colorToIndex.entrySet()) {
      palette[entry.getValue()] = entry.getKey();
    }

    int bits = calculateBitsRequired(palette.length);
    BitFieldArr indices = new BitFieldArr(bits, pixels.length);
    for (int i = 0; i < pixels.length; i++) {
      indices.set(i, colorToIndex.get(pixels[i]));
    }

    image.palette = palette;
    image.bitsPerIndex = (byte) bits;
    image.packedIndices = indices.get();
  }

  /**
   * Decodes a {@link MapImage}'s palette/packedIndices back into a raw RGBA pixel buffer.
   * Returns {@code null} if the image carries no pixel data (mirrors the old {@code data == null}).
   */
  public static int[] decode(MapImage image) {
    if (image == null || image.palette == null || image.packedIndices == null
        || image.bitsPerIndex <= 0) {
      return null;
    }
    int count = image.width * image.height;
    if (count <= 0) {
      return null;
    }

    BitFieldArr indices = new BitFieldArr(image.bitsPerIndex, count);
    indices.set(image.packedIndices);

    int[] palette = image.palette;
    int[] pixels = new int[count];
    for (int i = 0; i < count; i++) {
      int idx = indices.get(i);
      pixels[i] = (idx >= 0 && idx < palette.length) ? palette[idx] : 0;
    }
    return pixels;
  }

  /**
   * Mirrors the engine's index-width selection so the client decodes our packing correctly.
   */
  private static int calculateBitsRequired(int colorCount) {
    if (colorCount <= 16) {
      return 4;
    } else if (colorCount <= 256) {
      return 8;
    } else {
      return colorCount <= 4096 ? 12 : 16;
    }
  }
}
