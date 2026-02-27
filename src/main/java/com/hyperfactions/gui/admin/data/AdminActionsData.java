package com.hyperfactions.gui.admin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Event data for the Admin Actions page.
 */
public class AdminActionsData implements AdminNavAwareData {

  /** The button/action that triggered the event. */
  public String button;

  /** Admin nav bar target (for navigation). */
  public String adminNavBar;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<AdminActionsData> CODEC = BuilderCodec
      .builder(AdminActionsData.class, AdminActionsData::new)
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
      .build();

  /** Creates a new AdminActionsData. */
  public AdminActionsData() {
  }

  /** Returns the admin nav bar. */
  @Override
  @Nullable
  public String getAdminNavBar() {
    return adminNavBar;
  }
}
