package com.hyperfactions.gui;

/**
 * Interface for GUI pages that support real-time content refresh.
 * Pages implementing this interface can be updated when external data changes
 * (e.g., invite received, member joined, territory claimed).
 */
public interface RefreshablePage {

  /**
   * Refreshes the page content with current data.
   * Must be called on the world thread.
   */
  void refreshContent();
}
