package com.hyperfactions.util;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * UUID parsing utilities.
 */
public final class UuidUtil {

  private UuidUtil() {}

  /**
   * Parses a UUID string, returning null on failure instead of throwing.
   *
   * @param value the string to parse
   * @return the parsed UUID, or null if invalid/null/empty
   */
  @Nullable
  public static UUID parseOrNull(@Nullable String value) {
    if (value == null || value.isEmpty()) {
      return null;
    }
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
