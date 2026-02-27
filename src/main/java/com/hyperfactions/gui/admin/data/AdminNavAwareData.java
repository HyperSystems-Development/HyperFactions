package com.hyperfactions.gui.admin.data;

import org.jetbrains.annotations.Nullable;

/**
 * Interface for admin event data classes that support navigation bar events.
 * Implementing this interface allows AdminNavBarHelper to handle navigation
 * events uniformly across different admin data types.
 */
public interface AdminNavAwareData {

  /**
   * Gets the admin navigation bar target page ID.
   * This is set when an admin nav bar button is clicked.
   *
   * @return The target page ID, or null if not a nav event
   */
  @Nullable
  String getAdminNavBar();
}
