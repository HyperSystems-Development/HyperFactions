package com.hyperfactions.gui.shared.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Disband Confirmation modal.
 */
public class DisbandConfirmData {

  /** The button/action that triggered the event. */
  public String button;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<DisbandConfirmData> CODEC = BuilderCodec
      .builder(DisbandConfirmData.class, DisbandConfirmData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .build();

  /** Creates a new DisbandConfirmData. */
  public DisbandConfirmData() {
  }
}
