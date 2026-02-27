package com.hyperfactions.gui.admin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Zone Rename modal.
 */
public class ZoneRenameModalData {

  /** The button/action that triggered the event. */
  public String button;

  /** The new zone name entered by user. */
  public String name;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<ZoneRenameModalData> CODEC = BuilderCodec
      .builder(ZoneRenameModalData.class, ZoneRenameModalData::new)
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

  /** Creates a new ZoneRenameModalData. */
  public ZoneRenameModalData() {
  }
}
