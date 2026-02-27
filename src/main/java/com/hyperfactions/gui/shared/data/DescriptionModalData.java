package com.hyperfactions.gui.shared.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Description modal.
 */
public class DescriptionModalData {

  /** The button/action that triggered the event. */
  public String button;

  /** The new faction description entered by user. */
  public String description;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<DescriptionModalData> CODEC = BuilderCodec
      .builder(DescriptionModalData.class, DescriptionModalData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .addField(
          new KeyedCodec<>("@Description", Codec.STRING),
          (data, value) -> data.description = value,
          data -> data.description
      )
      .build();

  /** Creates a new DescriptionModalData. */
  public DescriptionModalData() {
  }
}
