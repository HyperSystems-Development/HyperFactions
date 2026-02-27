package com.hyperfactions.gui.faction.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Treasury transfer search modal.
 */
public class TransferSearchData {

  /** The button/action that triggered the event. */
  public String button;

  /** Search query text. */
  public String searchQuery;

  /** Target ID (UUID for player, faction ID for faction). */
  public String targetId;

  /** Target display name. */
  public String targetName;

  /** Target type: "player" or "faction". */
  public String targetType;

  /** Page number for pagination. */
  public int page;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<TransferSearchData> CODEC = BuilderCodec
      .builder(TransferSearchData.class, TransferSearchData::new)
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
          new KeyedCodec<>("TargetId", Codec.STRING),
          (data, value) -> data.targetId = value,
          data -> data.targetId
      )
      .addField(
          new KeyedCodec<>("TargetName", Codec.STRING),
          (data, value) -> data.targetName = value,
          data -> data.targetName
      )
      .addField(
          new KeyedCodec<>("TargetType", Codec.STRING),
          (data, value) -> data.targetType = value,
          data -> data.targetType
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

  /** Creates a new TransferSearchData. */
  public TransferSearchData() {
  }
}
