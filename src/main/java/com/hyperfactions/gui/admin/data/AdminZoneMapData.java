package com.hyperfactions.gui.admin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Admin Zone Map page.
 * Used for claiming/unclaiming chunks for a specific zone.
 */
public class AdminZoneMapData {

  /** The button/action that triggered the event. */
  public String button;

  /** Zone ID being edited. */
  public String zoneId;

  /** Target chunk X coordinate. */
  public int chunkX;

  /** Target chunk Z coordinate. */
  public int chunkZ;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<AdminZoneMapData> CODEC = BuilderCodec
      .builder(AdminZoneMapData.class, AdminZoneMapData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .addField(
          new KeyedCodec<>("ZoneId", Codec.STRING),
          (data, value) -> data.zoneId = value,
          data -> data.zoneId
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
      .build();

  /** Creates a new AdminZoneMapData. */
  public AdminZoneMapData() {
  }
}
