package com.hyperfactions.gui.admin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Event data for the Admin Factions page.
 */
public class AdminFactionsData implements AdminNavAwareData {

  /** The button/action that triggered the event. */
  public String button;

  /** Target faction ID (if any). */
  public String factionId;

  /** Target faction UUID for expansion toggle. */
  public String factionUuid;

  /** Target faction name (if any). */
  public String factionName;

  /** Search query (if any). */
  public String searchQuery;

  /** Current page number (for pagination). */
  public int page;

  /** Sort mode. */
  public String sortMode;

  /** Admin nav bar target (for navigation). */
  public String adminNavBar;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<AdminFactionsData> CODEC = BuilderCodec
      .builder(AdminFactionsData.class, AdminFactionsData::new)
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
          new KeyedCodec<>("FactionUuid", Codec.STRING),
          (data, value) -> data.factionUuid = value,
          data -> data.factionUuid
      )
      .addField(
          new KeyedCodec<>("FactionName", Codec.STRING),
          (data, value) -> data.factionName = value,
          data -> data.factionName
      )
      .addField(
          new KeyedCodec<>("@SearchQuery", Codec.STRING),
          (data, value) -> data.searchQuery = value,
          data -> data.searchQuery
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
          new KeyedCodec<>("@SortMode", Codec.STRING),
          (data, value) -> data.sortMode = value,
          data -> data.sortMode
      )
      .addField(
          new KeyedCodec<>("AdminNavBar", Codec.STRING),
          (data, value) -> data.adminNavBar = value,
          data -> data.adminNavBar
      )
      .build();

  /** Creates a new AdminFactionsData. */
  public AdminFactionsData() {
  }

  /** Returns the admin nav bar. */
  @Override
  @Nullable
  public String getAdminNavBar() {
    return adminNavBar;
  }
}
