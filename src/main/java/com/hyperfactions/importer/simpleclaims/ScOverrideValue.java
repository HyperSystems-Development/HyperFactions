package com.hyperfactions.importer.simpleclaims;

import org.jetbrains.annotations.Nullable;

/**
 * Gson-mapped record for a SimpleClaims override value.
 *
 * @param Type  the value type: {@code "bool"} or {@code "integer"}
 * @param Value the string representation of the value
 */
public record ScOverrideValue(
  @Nullable String Type,
  @Nullable String Value
) {

  /** Returns the value as a boolean (for "bool" type). */
  public boolean asBoolean() {
    return "true".equalsIgnoreCase(Value);
  }

  /** Returns the value as an integer (for "integer" type). */
  public int asInt() {
    try {
      return Value != null ? Integer.parseInt(Value) : 0;
    } catch (NumberFormatException e) {
      return 0;
    }
  }
}
