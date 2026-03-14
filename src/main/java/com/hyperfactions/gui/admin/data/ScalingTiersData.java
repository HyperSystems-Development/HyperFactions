package com.hyperfactions.gui.admin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Scaling Tiers modal page.
 */
public class ScalingTiersData {

  /** The button/action that triggered the event. */
  public String button;

  /** The tier index being acted on. */
  public String tierIndex;

  /** Dynamic chunk count input. */
  public String chunkInput;

  /** Dynamic cost input. */
  public String costInput;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<ScalingTiersData> CODEC = BuilderCodec
      .builder(ScalingTiersData.class, ScalingTiersData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .addField(
          new KeyedCodec<>("TierIndex", Codec.STRING),
          (data, value) -> data.tierIndex = value,
          data -> data.tierIndex
      )
      .addField(
          new KeyedCodec<>("@chunkInput", Codec.STRING),
          (data, value) -> data.chunkInput = value,
          data -> data.chunkInput
      )
      .addField(
          new KeyedCodec<>("@costInput", Codec.STRING),
          (data, value) -> data.costInput = value,
          data -> data.costInput
      )
      .build();

  /** Creates a new ScalingTiersData. */
  public ScalingTiersData() {
  }
}
