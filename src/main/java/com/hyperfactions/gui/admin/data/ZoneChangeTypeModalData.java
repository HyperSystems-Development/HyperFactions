package com.hyperfactions.gui.admin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Zone Change Type modal.
 */
public class ZoneChangeTypeModalData {

  /** The button/action that triggered the event. */
  public String button;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<ZoneChangeTypeModalData> CODEC = BuilderCodec
      .builder(ZoneChangeTypeModalData.class, ZoneChangeTypeModalData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .build();

  /** Creates a new ZoneChangeTypeModalData. */
  public ZoneChangeTypeModalData() {
  }
}
