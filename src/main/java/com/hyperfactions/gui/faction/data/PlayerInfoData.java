package com.hyperfactions.gui.faction.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Data for the Player Info page.
 */
public class PlayerInfoData {

  /** The button/action that triggered the event. */
  public String button;

  /** Player UUID (for lookups). */
  public String playerUuid;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<PlayerInfoData> CODEC = BuilderCodec
      .builder(PlayerInfoData.class, PlayerInfoData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .addField(
          new KeyedCodec<>("PlayerUuid", Codec.STRING),
          (data, value) -> data.playerUuid = value,
          data -> data.playerUuid
      )
      .build();

  /** Creates a new PlayerInfoData. */
  public PlayerInfoData() {
  }
}
