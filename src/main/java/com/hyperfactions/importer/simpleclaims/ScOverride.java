package com.hyperfactions.importer.simpleclaims;

import org.jetbrains.annotations.Nullable;

/**
 * Gson-mapped record for a SimpleClaims party override entry.
 *
 * @param Type  the override key string (e.g. "simpleclaims.party.protection.place_blocks")
 * @param Value the typed value
 */
public record ScOverride(
  @Nullable String Type,
  @Nullable ScOverrideValue Value
) {}
