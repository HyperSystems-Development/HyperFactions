package com.hyperfactions.gui.admin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Event data for the Admin Zone Settings page.
 */
public class AdminZoneSettingsData implements AdminNavAwareData {

  /** The button/action that triggered the event. */
  public String button;

  /** Target zone ID. */
  public String zoneId;

  /** Flag name to toggle. */
  public String flag;

  /** Admin nav bar target (for navigation). */
  public String adminNavBar;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<AdminZoneSettingsData> CODEC = BuilderCodec
      .builder(AdminZoneSettingsData.class, AdminZoneSettingsData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .addField(
          new KeyedCodec<>("ZoneId", Codec.STRING),
          (data, value) -> data.zoneId = value,
          data -> data.zoneId
      )
      .addField(
          new KeyedCodec<>("Flag", Codec.STRING),
          (data, value) -> data.flag = value,
          data -> data.flag
      )
      .addField(
          new KeyedCodec<>("AdminNavBar", Codec.STRING),
          (data, value) -> data.adminNavBar = value,
          data -> data.adminNavBar
      )
      .build();

  /** Creates a new AdminZoneSettingsData. */
  public AdminZoneSettingsData() {
  }

  /** Returns the admin nav bar. */
  @Override
  @Nullable
  public String getAdminNavBar() {
    return adminNavBar;
  }
}
