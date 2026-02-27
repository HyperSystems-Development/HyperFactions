package com.hyperfactions.gui.faction.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Set Relation modal.
 */
public class SetRelationModalData {

  /** The button/action that triggered the event. */
  public String button;

  /** Search query text. */
  public String searchQuery;

  /** Target faction ID. */
  public String factionId;

  /** Target faction name. */
  public String factionName;

  /** Page number for pagination. */
  public int page;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<SetRelationModalData> CODEC = BuilderCodec
      .builder(SetRelationModalData.class, SetRelationModalData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .addField(
          new KeyedCodec<>("@SearchQuery", Codec.STRING),
          (data, value) -> data.searchQuery = value,
          data -> data.searchQuery
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
      .build();

  /** Creates a new SetRelationModalData. */
  public SetRelationModalData() {
  }
}
