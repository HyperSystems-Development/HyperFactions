package com.hyperfactions.importer.simpleclaims;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Gson-mapped record for the SimpleClaims {@code NameCache.json} root object.
 */
public record ScNameCache(
  @Nullable List<ScNameEntry> Values
) {}
