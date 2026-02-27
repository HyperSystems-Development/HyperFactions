package com.hyperfactions.gui.shared.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Data for the main menu page - the central navigation hub.
 */
public class MainMenuData {

  /** The button/action that triggered the event. */
  public String button;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<MainMenuData> CODEC = BuilderCodec
      .builder(MainMenuData.class, MainMenuData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .build();

  /** Creates a new MainMenuData. */
  public MainMenuData() {
  }
}
