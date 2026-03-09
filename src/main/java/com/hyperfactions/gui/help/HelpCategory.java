package com.hyperfactions.gui.help;

import com.hyperfactions.util.HFMessages;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;

/**
 * Categories for organizing help content.
 * Each category represents a conceptual area with an accent color for UI rendering.
 */
public enum HelpCategory {
  WELCOME("welcome", "hyperfactions_gui.help.category.welcome", "#00FFFF", 0),
  YOUR_FACTION("your_faction", "hyperfactions_gui.help.category.your_faction", "#44CC44", 1),
  POWER_AND_LAND("power_land", "hyperfactions_gui.help.category.power_land", "#FFD700", 2),
  DIPLOMACY("diplomacy", "hyperfactions_gui.help.category.diplomacy", "#55AAFF", 3),
  COMBAT("combat", "hyperfactions_gui.help.category.combat", "#FF5555", 4),
  ECONOMY("economy", "hyperfactions_gui.help.category.economy", "#FFAA00", 5),
  QUICK_REFERENCE("quick_ref", "hyperfactions_gui.help.category.quick_ref", "#888888", 6);

  private final String id;

  private final String displayNameKey;

  private final String color;

  private final int order;

  HelpCategory(@NotNull String id, @NotNull String displayNameKey,
        @NotNull String color, int order) {
    this.id = id;
    this.displayNameKey = displayNameKey;
    this.color = color;
    this.order = order;
  }

  /**
   * Gets the unique identifier for this category.
   */
  @NotNull
  public String id() {
    return id;
  }

  /**
   * Gets the display name shown in the UI, resolved via i18n.
   */
  @NotNull
  public String displayName() {
    return HFMessages.get((PlayerRef) null, displayNameKey);
  }

  /**
   * Gets the accent color hex string (e.g. "#00FFFF") for UI rendering.
   */
  @NotNull
  public String color() {
    return color;
  }

  /**
   * Gets the display order (lower = higher in list).
   */
  public int order() {
    return order;
  }

  /**
   * Finds a category by its ID.
   *
   * @param id The category ID
   * @return The matching category, or WELCOME if not found
   */
  @NotNull
  public static HelpCategory fromId(@NotNull String id) {
    for (HelpCategory category : values()) {
      if (category.id.equals(id)) {
        return category;
      }
    }
    return WELCOME;
  }
}
