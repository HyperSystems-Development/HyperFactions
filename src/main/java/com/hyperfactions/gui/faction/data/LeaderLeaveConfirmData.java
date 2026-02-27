package com.hyperfactions.gui.faction.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Leader Leave Faction Confirmation modal.
 * Used when a leader wants to leave - shows succession info.
 */
public class LeaderLeaveConfirmData {

  /** The button/action that triggered the event ("Cancel", "Leave", or "Disband"). */
  public String button;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<LeaderLeaveConfirmData> CODEC = BuilderCodec
      .builder(LeaderLeaveConfirmData.class, LeaderLeaveConfirmData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .build();

  /** Creates a new LeaderLeaveConfirmData. */
  public LeaderLeaveConfirmData() {
  }
}
