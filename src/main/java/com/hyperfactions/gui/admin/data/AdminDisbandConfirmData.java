package com.hyperfactions.gui.admin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Admin Disband Confirmation modal.
 */
public class AdminDisbandConfirmData {

  /** The button that was clicked (Cancel or Confirm). */
  public String button;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<AdminDisbandConfirmData> CODEC = BuilderCodec
      .builder(AdminDisbandConfirmData.class, AdminDisbandConfirmData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .build();

  /** Creates a new AdminDisbandConfirmData. */
  public AdminDisbandConfirmData() {
  }
}
