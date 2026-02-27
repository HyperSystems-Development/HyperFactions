package com.hyperfactions.gui.faction.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Leadership Transfer Confirmation modal.
 */
public class TransferConfirmData {

  /** The button/action that triggered the event. */
  public String button;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<TransferConfirmData> CODEC = BuilderCodec
      .builder(TransferConfirmData.class, TransferConfirmData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .build();

  /** Creates a new TransferConfirmData. */
  public TransferConfirmData() {
  }
}
