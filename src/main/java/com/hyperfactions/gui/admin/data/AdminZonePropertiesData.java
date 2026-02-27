package com.hyperfactions.gui.admin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Event data for the Admin Zone Properties page.
 * Handles zone name editing, type changing, and notification settings.
 */
public class AdminZonePropertiesData implements AdminNavAwareData {

  /** The button/action that triggered the event. */
  public String button;

  /** Zone ID (static, passed via EventData). */
  public String zoneId;

  /** Zone name from text field (dynamic @-prefix binding). */
  public String name;

  /** Upper title from text field (dynamic @-prefix binding). */
  public String upperTitle;

  /** Lower title from text field (dynamic @-prefix binding). */
  public String lowerTitle;

  /** Admin nav bar target (for navigation). */
  public String adminNavBar;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<AdminZonePropertiesData> CODEC = BuilderCodec
      .builder(AdminZonePropertiesData.class, AdminZonePropertiesData::new)
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
          new KeyedCodec<>("@Name", Codec.STRING),
          (data, value) -> data.name = value,
          data -> data.name
      )
      .addField(
          new KeyedCodec<>("@UpperTitle", Codec.STRING),
          (data, value) -> data.upperTitle = value,
          data -> data.upperTitle
      )
      .addField(
          new KeyedCodec<>("@LowerTitle", Codec.STRING),
          (data, value) -> data.lowerTitle = value,
          data -> data.lowerTitle
      )
      .addField(
          new KeyedCodec<>("AdminNavBar", Codec.STRING),
          (data, value) -> data.adminNavBar = value,
          data -> data.adminNavBar
      )
      .build();

  /** Creates a new AdminZonePropertiesData. */
  public AdminZonePropertiesData() {
  }

  /** Returns the admin nav bar. */
  @Override
  @Nullable
  public String getAdminNavBar() {
    return adminNavBar;
  }
}
