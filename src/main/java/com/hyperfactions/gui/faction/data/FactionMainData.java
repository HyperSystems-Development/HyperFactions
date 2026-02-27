package com.hyperfactions.gui.faction.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Faction Main page (dashboard).
 */
public class FactionMainData {

  /** The button/action that triggered the event. */
  public String button;

  /** Target faction ID (if any). */
  public String factionId;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<FactionMainData> CODEC = BuilderCodec
      .builder(FactionMainData.class, FactionMainData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .addField(
          new KeyedCodec<>("FactionId", Codec.STRING),
          (data, value) -> data.factionId = value,
          data -> data.factionId
      )
      .build();

  /** Creates a new FactionMainData. */
  public FactionMainData() {
  }
}
