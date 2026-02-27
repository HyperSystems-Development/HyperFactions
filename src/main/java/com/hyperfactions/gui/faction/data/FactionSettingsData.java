package com.hyperfactions.gui.faction.data;

import com.hyperfactions.gui.shared.data.NavAwareData;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Data for the unified Faction Settings page (two-column layout).
 * Handles permission toggles, general settings, and standard page events.
 */
public class FactionSettingsData implements NavAwareData {

  /** The button/action that triggered the event. */
  public String button;

  /** Navigation target from NavBar button. */
  public String navBar;

  /** Permission name for toggle actions (e.g., "outsiderBreak", "pvpEnabled"). */
  public String perm;

  /** Selected color hex code. */
  public String color;

  /** Recruitment dropdown value (OPEN or INVITE_ONLY). */
  public String recruitment;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<FactionSettingsData> CODEC = BuilderCodec
      .builder(FactionSettingsData.class, FactionSettingsData::new)
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
          new KeyedCodec<>("Perm", Codec.STRING),
          (data, value) -> data.perm = value,
          data -> data.perm
      )
      .addField(
          new KeyedCodec<>("@Color", Codec.STRING),
          (data, value) -> data.color = value,
          data -> data.color
      )
      .addField(
          new KeyedCodec<>("@Recruitment", Codec.STRING),
          (data, value) -> data.recruitment = value,
          data -> data.recruitment
      )
      .build();

  /** Creates a new FactionSettingsData. */
  public FactionSettingsData() {
  }

  /** Returns the nav bar. */
  @Override
  public String getNavBar() {
    return navBar;
  }
}
