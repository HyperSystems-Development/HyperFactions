package com.hyperfactions.gui.faction.data;

import com.hyperfactions.gui.shared.data.NavAwareData;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Faction Chat page.
 * Handles tab switching, send button, load-older, and text input capture.
 */
public class FactionChatData implements NavAwareData {

  /** The button/action that triggered the event. */
  public String button;

  /** NavBar target. */
  public String navBar;

  /** Chat input text captured from the TextField. */
  public String chatInput;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<FactionChatData> CODEC = BuilderCodec
      .builder(FactionChatData.class, FactionChatData::new)
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
          new KeyedCodec<>("@ChatInput", Codec.STRING),
          (data, value) -> data.chatInput = value,
          data -> data.chatInput
      )
      .build();

  /** Creates a new FactionChatData. */
  public FactionChatData() {
  }

  /** Returns the nav bar. */
  @Override
  public String getNavBar() {
    return navBar;
  }
}
