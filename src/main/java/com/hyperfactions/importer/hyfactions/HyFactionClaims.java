package com.hyperfactions.importer.hyfactions;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the Claims.json file structure from HyFactions.
 *
 * @param Dimensions list of dimension entries with their claims
 */
public record HyFactionClaims(
  @Nullable List<HyFactionDimension> Dimensions
) {
}
