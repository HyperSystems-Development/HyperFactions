package com.hyperfactions.gui.shared.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for placeholder pages.
 * Minimal data class supporting navigation.
 */
public class PlaceholderData implements NavAwareData {

  /** The button/action that triggered the event. */
  public String button;

  /** NavBar target (page ID when nav button clicked). */
  public String navBar;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<PlaceholderData> CODEC = BuilderCodec
      .builder(PlaceholderData.class, PlaceholderData::new)
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
      .build();

  /** Creates a new PlaceholderData. */
  public PlaceholderData() {
  }

  /** Returns the nav bar. */
  @Override
  public String getNavBar() {
    return navBar;
  }
}
