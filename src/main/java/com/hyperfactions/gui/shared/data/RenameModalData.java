package com.hyperfactions.gui.shared.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Rename Faction modal.
 */
public class RenameModalData {

  /** The button/action that triggered the event. */
  public String button;

  /** The new faction name entered by user. */
  public String name;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<RenameModalData> CODEC = BuilderCodec
      .builder(RenameModalData.class, RenameModalData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .addField(
          new KeyedCodec<>("@Name", Codec.STRING),
          (data, value) -> data.name = value,
          data -> data.name
      )
      .build();

  /** Creates a new RenameModalData. */
  public RenameModalData() {
  }
}
