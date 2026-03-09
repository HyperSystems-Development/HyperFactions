package com.hyperfactions.gui.admin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Event data for the Admin Bulk Economy Adjust modal.
 */
public class AdminBulkEconomyData implements AdminNavAwareData {

  /** The button/action that triggered the event. */
  public String button;

  /** Amount value from the text input (dynamic via @). */
  public String amount;

  /** Admin nav bar target (for navigation). */
  public String adminNavBar;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<AdminBulkEconomyData> CODEC = BuilderCodec
      .builder(AdminBulkEconomyData.class, AdminBulkEconomyData::new)
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
      .addField(
          new KeyedCodec<>("AdminNavBar", Codec.STRING),
          (data, value) -> data.adminNavBar = value,
          data -> data.adminNavBar
      )
      .build();

  /** Creates a new AdminBulkEconomyData. */
  public AdminBulkEconomyData() {
  }

  /** Returns the admin nav bar. */
  @Override
  @Nullable
  public String getAdminNavBar() {
    return adminNavBar;
  }
}
