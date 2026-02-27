package com.hyperfactions.gui.admin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Event data for the Admin Faction Info page.
 */
public class AdminFactionInfoData implements AdminNavAwareData {

  /** The button/action that triggered the event. */
  public String button;

  /** Target faction ID (for navigation). */
  public String factionId;

  /** Amount value for bulk power operations. */
  public String amount;

  /** Admin nav bar target (for navigation). */
  public String adminNavBar;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<AdminFactionInfoData> CODEC = BuilderCodec
      .builder(AdminFactionInfoData.class, AdminFactionInfoData::new)
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
          new KeyedCodec<>("Amount", Codec.STRING),
          (data, value) -> data.amount = value,
          data -> data.amount
      )
      .addField(
          new KeyedCodec<>("AdminNavBar", Codec.STRING),
          (data, value) -> data.adminNavBar = value,
          data -> data.adminNavBar
      )
      .build();

  /** Creates a new AdminFactionInfoData. */
  public AdminFactionInfoData() {
  }

  /** Returns the admin nav bar. */
  @Override
  @Nullable
  public String getAdminNavBar() {
    return adminNavBar;
  }
}
