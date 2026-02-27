package com.hyperfactions.gui.faction.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Treasury deposit/withdraw modal.
 */
public class DepositModalData {

  /** The button/action that triggered the event. */
  public String button;

  /** Amount value from TextField. */
  public String amount;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<DepositModalData> CODEC = BuilderCodec
      .builder(DepositModalData.class, DepositModalData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .addField(
          new KeyedCodec<>("@Amount", Codec.STRING),
          (data, value) -> data.amount = value,
          data -> data.amount
      )
      .build();

  /** Creates a new DepositModalData. */
  public DepositModalData() {
  }
}
