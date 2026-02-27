package com.hyperfactions.gui.faction.data;

import com.hyperfactions.gui.shared.data.NavAwareData;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Faction Members page.
 * Supports search, expansion, sorting, and member management actions.
 */
public class FactionMembersData implements NavAwareData {

  /** The button/action that triggered the event. */
  public String button;

  /** NavBar target (page ID when nav button clicked). */
  public String navBar;

  /** Search query from text input (uses @SearchQuery binding). */
  public String searchQuery;

  /** Target player UUID. */
  public String playerUuid;

  /** Target player name. */
  public String target;

  /** Sort mode from dropdown. */
  public String sortMode;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<FactionMembersData> CODEC = BuilderCodec
      .builder(FactionMembersData.class, FactionMembersData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .addField(
          new KeyedCodec<>("NavBar", Codec.STRING),
          (data, value) -> data.navBar = value,
          data -> data.navBar
      )
      .addField(
          new KeyedCodec<>("@SearchQuery", Codec.STRING),
          (data, value) -> data.searchQuery = value,
          data -> data.searchQuery
      )
      .addField(
          new KeyedCodec<>("PlayerUuid", Codec.STRING),
          (data, value) -> data.playerUuid = value,
          data -> data.playerUuid
      )
      .addField(
          new KeyedCodec<>("Target", Codec.STRING),
          (data, value) -> data.target = value,
          data -> data.target
      )
      .addField(
          new KeyedCodec<>("@SortMode", Codec.STRING),
          (data, value) -> data.sortMode = value,
          data -> data.sortMode
      )
      .build();

  /** Creates a new FactionMembersData. */
  public FactionMembersData() {
  }

  /** Returns the nav bar. */
  @Override
  public String getNavBar() {
    return navBar;
  }
}
