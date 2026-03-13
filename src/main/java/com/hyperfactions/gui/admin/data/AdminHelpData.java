package com.hyperfactions.gui.admin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Event data for the Admin Help page.
 */
public class AdminHelpData implements AdminNavAwareData {

  /** The button/action that triggered the event. */
  public String button;

  /** Admin nav bar target (for navigation). */
  public String adminNavBar;

  /** Selected category ID (for category switching). */
  public String category;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<AdminHelpData> CODEC = BuilderCodec
      .builder(AdminHelpData.class, AdminHelpData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .addField(
          new KeyedCodec<>("AdminNavBar", Codec.STRING),
          (data, value) -> data.adminNavBar = value,
          data -> data.adminNavBar
      )
      .addField(
          new KeyedCodec<>("Category", Codec.STRING),
          (data, value) -> data.category = value,
          data -> data.category
      )
      .build();

  /** Creates a new AdminHelpData. */
  public AdminHelpData() {
  }

  /** Returns the admin nav bar. */
  @Override
  @Nullable
  public String getAdminNavBar() {
    return adminNavBar;
  }
}
