package com.hyperfactions.gui.faction.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Leave Faction Confirmation modal.
 */
public class LeaveConfirmData {

  /** The button/action that triggered the event ("Cancel" or "Leave"). */
  public String button;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<LeaveConfirmData> CODEC = BuilderCodec
      .builder(LeaveConfirmData.class, LeaveConfirmData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .build();

  /** Creates a new LeaveConfirmData. */
  public LeaveConfirmData() {
  }
}
