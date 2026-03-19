package com.hyperfactions.gui.shared.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Data for the Player Settings page.
 * Handles notification toggles, language selection, and navigation.
 */
public class PlayerSettingsData implements NavAwareData {

  /** The button/action that triggered the event. */
  public String button;

  /** Navigation target from NavBar button. */
  public String navBar;

  /** Language selected from dropdown (dynamic @-prefixed value). */
  public String language;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<PlayerSettingsData> CODEC = BuilderCodec
      .builder(PlayerSettingsData.class, PlayerSettingsData::new)
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
          new KeyedCodec<>("@Language", Codec.STRING),
          (data, value) -> data.language = value,
          data -> data.language
      )
      .build();

  /** Creates a new PlayerSettingsData. */
  public PlayerSettingsData() {
  }

  /** Returns the nav bar. */
  @Override
  public String getNavBar() {
    return navBar;
  }
}
