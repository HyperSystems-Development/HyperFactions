package com.hyperfactions.gui.faction.data;

import com.hyperfactions.gui.shared.data.NavAwareData;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Faction Dashboard page.
 * Handles navigation, quick actions, and activity feed interactions.
 */
public class FactionDashboardData implements NavAwareData {

  /** The button/action that triggered the event (e.g., "Nav", "Home", "Claim", "Leave"). */
  public String button;

  /** NavBar target (page ID when nav button clicked). */
  public String navBar;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<FactionDashboardData> CODEC = BuilderCodec
      .builder(FactionDashboardData.class, FactionDashboardData::new)
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

  /** Creates a new FactionDashboardData. */
  public FactionDashboardData() {
  }

  /** Returns the nav bar. */
  @Override
  public String getNavBar() {
    return navBar;
  }
}
