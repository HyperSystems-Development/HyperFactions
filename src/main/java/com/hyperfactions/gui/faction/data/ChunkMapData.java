package com.hyperfactions.gui.faction.data;

import com.hyperfactions.gui.shared.data.NavAwareData;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Chunk Map page (9x9 interactive territory grid).
 */
public class ChunkMapData implements NavAwareData {

  /** The button/action that triggered the event. */
  public String button;

  /** NavBar target (AdminUI pattern - page ID when nav button clicked). */
  public String navBar;

  /** Target chunk X coordinate. */
  public int chunkX;

  /** Target chunk Z coordinate. */
  public int chunkZ;

  /** Offset X for panning the map. */
  public int offsetX;

  /** Offset Z for panning the map. */
  public int offsetZ;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<ChunkMapData> CODEC = BuilderCodec
      .builder(ChunkMapData.class, ChunkMapData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .addField(
          new KeyedCodec<>("NavBar", Codec.STRING),
          (data, value) -> data.navBar = value,
          data -> data.navBar
      )
      .addField(
          new KeyedCodec<>("ChunkX", Codec.STRING),
          (data, value) -> {
            try {
              data.chunkX = value != null ? Integer.parseInt(value) : 0;
            } catch (NumberFormatException e) {
              data.chunkX = 0;
            }
          },
          data -> String.valueOf(data.chunkX)
      )
      .addField(
          new KeyedCodec<>("ChunkZ", Codec.STRING),
          (data, value) -> {
            try {
              data.chunkZ = value != null ? Integer.parseInt(value) : 0;
            } catch (NumberFormatException e) {
              data.chunkZ = 0;
            }
          },
          data -> String.valueOf(data.chunkZ)
      )
      .addField(
          new KeyedCodec<>("OffsetX", Codec.STRING),
          (data, value) -> {
            try {
              data.offsetX = value != null ? Integer.parseInt(value) : 0;
            } catch (NumberFormatException e) {
              data.offsetX = 0;
            }
          },
          data -> String.valueOf(data.offsetX)
      )
      .addField(
          new KeyedCodec<>("OffsetZ", Codec.STRING),
          (data, value) -> {
            try {
              data.offsetZ = value != null ? Integer.parseInt(value) : 0;
            } catch (NumberFormatException e) {
              data.offsetZ = 0;
            }
          },
          data -> String.valueOf(data.offsetZ)
      )
      .build();

  /** Creates a new ChunkMapData. */
  public ChunkMapData() {
  }

  /** Returns the nav bar. */
  @Override
  public String getNavBar() {
    return navBar;
  }
}
