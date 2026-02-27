package com.hyperfactions.gui.faction.data;

import com.hyperfactions.gui.shared.data.NavAwareData;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Faction Treasury page.
 * Simplified v2: only handles navigation and quick-action buttons.
 * Deposit/withdraw/transfer/settings are handled by their own modal pages.
 */
public class TreasuryData implements NavAwareData {

  /** The button/action that triggered the event. */
  public String button;

  /** NavBar target (page ID when nav button clicked). */
  public String navBar;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<TreasuryData> CODEC = BuilderCodec
      .builder(TreasuryData.class, TreasuryData::new)
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

  /** Creates a new TreasuryData. */
  public TreasuryData() {
  }

  /** Returns the nav bar. */
  @Override
  public String getNavBar() {
    return navBar;
  }
}
