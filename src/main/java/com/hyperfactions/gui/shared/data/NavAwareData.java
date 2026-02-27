package com.hyperfactions.gui.shared.data;

import org.jetbrains.annotations.Nullable;

/**
 * Interface for event data classes that support navigation bar events.
 * Implementing this interface allows NavBarHelper to handle navigation
 * events uniformly across different data types.
 */
public interface NavAwareData {

  /**
   * Gets the navigation bar target page ID.
   * This is set when a nav bar button is clicked.
   *
   * @return The target page ID, or null if not a nav event
   */
  @Nullable
  String getNavBar();
}
