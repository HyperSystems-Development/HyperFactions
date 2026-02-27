package com.hyperfactions.gui.shared;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Common interface for navigation bar entries across all page registries.
 */
public interface NavEntry {
  @NotNull String id();

  @NotNull String displayName();

  @Nullable String permission();
}
