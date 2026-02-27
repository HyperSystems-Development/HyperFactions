package com.hyperfactions.gui.faction.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Faction Browser page.
 */
public class FactionBrowserData {

  /** The button/action that triggered the event. */
  public String button;

  /** Target faction ID (if any). */
  public String factionId;

  /** Target faction name (if any). */
  public String factionName;

  /** Search query (if any). */
  public String searchQuery;

  /** Current page number (for pagination). */
  public int page;

  /** Sort mode from dropdown. */
  public String sortMode;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<FactionBrowserData> CODEC = BuilderCodec
      .builder(FactionBrowserData.class, FactionBrowserData::new)
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
          new KeyedCodec<>("FactionName", Codec.STRING),
          (data, value) -> data.factionName = value,
          data -> data.factionName
      )
      .addField(
          new KeyedCodec<>("SearchQuery", Codec.STRING),
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
      .build();

  /** Creates a new FactionBrowserData. */
  public FactionBrowserData() {
  }
}
