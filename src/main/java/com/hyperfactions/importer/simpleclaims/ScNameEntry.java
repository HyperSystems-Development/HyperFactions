package com.hyperfactions.importer.simpleclaims;

import org.jetbrains.annotations.Nullable;

/**
 * Gson-mapped record for a name cache entry in SimpleClaims {@code NameCache.json}.
 */
public record ScNameEntry(
  @Nullable String UUID,
  @Nullable String Name
) {}
