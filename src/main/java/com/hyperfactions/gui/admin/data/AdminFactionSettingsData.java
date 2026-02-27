package com.hyperfactions.gui.admin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Event data for the Admin Faction Settings page.
 */
public class AdminFactionSettingsData implements AdminNavAwareData {

  /** The button/action that triggered the event. */
  public String button;

  /** Target faction ID. */
  public String factionId;

  /** Permission name to toggle. */
  public String perm;

  /** Admin nav bar target (for navigation). */
  public String adminNavBar;

  /** Selected color hex code from ColorPicker. */
  public String color;

  /** Recruitment dropdown value (OPEN or INVITE_ONLY). */
  public String recruitment;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<AdminFactionSettingsData> CODEC = BuilderCodec
      .builder(AdminFactionSettingsData.class, AdminFactionSettingsData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .addField(
          new KeyedCodec<>("FactionId", Codec.STRING),
          (data, value) -> data.factionId = value,
          data -> data.factionId
      )
      .addField(
          new KeyedCodec<>("Perm", Codec.STRING),
          (data, value) -> data.perm = value,
          data -> data.perm
      )
      .addField(
          new KeyedCodec<>("AdminNavBar", Codec.STRING),
          (data, value) -> data.adminNavBar = value,
          data -> data.adminNavBar
      )
      .addField(
          new KeyedCodec<>("@Color", Codec.STRING),
          (data, value) -> data.color = value,
          data -> data.color
      )
      .addField(
          new KeyedCodec<>("@Recruitment", Codec.STRING),
          (data, value) -> data.recruitment = value,
          data -> data.recruitment
      )
      .build();

  /** Creates a new AdminFactionSettingsData. */
  public AdminFactionSettingsData() {
  }

  /** Returns the admin nav bar. */
  @Override
  @Nullable
  public String getAdminNavBar() {
    return adminNavBar;
  }
}
