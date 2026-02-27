package com.hyperfactions.gui.admin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Admin Unclaim All Confirmation modal.
 */
public class AdminUnclaimAllConfirmData {

  /** The button that was clicked (Cancel or Confirm). */
  public String button;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<AdminUnclaimAllConfirmData> CODEC = BuilderCodec
      .builder(AdminUnclaimAllConfirmData.class, AdminUnclaimAllConfirmData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .build();

  /** Creates a new AdminUnclaimAllConfirmData. */
  public AdminUnclaimAllConfirmData() {
  }
}
