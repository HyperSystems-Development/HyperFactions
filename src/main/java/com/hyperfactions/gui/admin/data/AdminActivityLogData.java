package com.hyperfactions.gui.admin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Event data for the Admin Activity Log page.
 */
public class AdminActivityLogData implements AdminNavAwareData {

  /** The button/action that triggered the event. */
  public String button;

  /** Admin nav bar target (for navigation). */
  public String adminNavBar;

  /** Filter by log type (from dropdown). */
  public String filterType;

  /** Time filter (from dropdown). */
  public String timeFilter;

  /** Page number for pagination. */
  public int page;

  /** Player name filter (from search input). */
  public String playerFilter;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<AdminActivityLogData> CODEC = BuilderCodec
      .builder(AdminActivityLogData.class, AdminActivityLogData::new)
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
          new KeyedCodec<>("@FilterType", Codec.STRING),
          (data, value) -> data.filterType = value,
          data -> data.filterType
      )
      .addField(
          new KeyedCodec<>("@TimeFilter", Codec.STRING),
          (data, value) -> data.timeFilter = value,
          data -> data.timeFilter
      )
      .addField(
          new KeyedCodec<>("Page", Codec.STRING),
          (data, value) -> {
            try {
              data.page = value != null ? Integer.parseInt(value) : 0;
            } catch (NumberFormatException e) {
              data.page = 0;
            }
          },
          data -> String.valueOf(data.page)
      )
      .addField(
          new KeyedCodec<>("@PlayerFilter", Codec.STRING),
          (data, value) -> data.playerFilter = value,
          data -> data.playerFilter
      )
      .build();

  /** Creates a new AdminActivityLogData. */
  public AdminActivityLogData() {
  }

  /** Returns the admin nav bar. */
  @Override
  @Nullable
  public String getAdminNavBar() {
    return adminNavBar;
  }
}
