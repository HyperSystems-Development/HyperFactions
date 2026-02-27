package com.hyperfactions.gui.help;

import org.jetbrains.annotations.NotNull;

/**
 * Categories for organizing help content.
 * Each category represents a conceptual area with an accent color for UI rendering.
 */
public enum HelpCategory {
  WELCOME("welcome", "Welcome", "#00FFFF", 0),
  YOUR_FACTION("your_faction", "Your Faction", "#44CC44", 1),
  POWER_AND_LAND("power_land", "Power & Land", "#FFD700", 2),
  DIPLOMACY("diplomacy", "Diplomacy", "#55AAFF", 3),
  COMBAT("combat", "Combat & Safety", "#FF5555", 4),
  ECONOMY("economy", "Economy", "#FFAA00", 5),
  QUICK_REFERENCE("quick_ref", "Quick Reference", "#888888", 6);

  private final String id;

  private final String displayName;

  private final String color;

  private final int order;

  HelpCategory(@NotNull String id, @NotNull String displayName,
        @NotNull String color, int order) {
    this.id = id;
    this.displayName = displayName;
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
   * Gets the display name shown in the UI.
   */
  @NotNull
  public String displayName() {
    return displayName;
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
