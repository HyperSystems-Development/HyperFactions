package com.hyperfactions.gui.admin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Event data for the Admin Faction Relations page.
 */
public class AdminFactionRelationsData implements AdminNavAwareData {

  /** The button/action that triggered the event. */
  public String button;

  /** Source faction ID. */
  public String factionId;

  /** Target faction ID (for setting relations). */
  public String targetFactionId;

  /** Search query (for faction search). */
  public String searchQuery;

  /** Admin nav bar target (for navigation). */
  public String adminNavBar;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<AdminFactionRelationsData> CODEC = BuilderCodec
      .builder(AdminFactionRelationsData.class, AdminFactionRelationsData::new)
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
          new KeyedCodec<>("TargetFactionId", Codec.STRING),
          (data, value) -> data.targetFactionId = value,
          data -> data.targetFactionId
      )
      .addField(
          new KeyedCodec<>("SearchQuery", Codec.STRING),
          (data, value) -> data.searchQuery = value,
          data -> data.searchQuery
      )
      .addField(
          new KeyedCodec<>("AdminNavBar", Codec.STRING),
          (data, value) -> data.adminNavBar = value,
          data -> data.adminNavBar
      )
      .build();

  /** Creates a new AdminFactionRelationsData. */
  public AdminFactionRelationsData() {
  }

  /** Returns the admin nav bar. */
  @Override
  @Nullable
  public String getAdminNavBar() {
    return adminNavBar;
  }
}
