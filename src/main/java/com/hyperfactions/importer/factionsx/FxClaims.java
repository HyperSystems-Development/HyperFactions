package com.hyperfactions.importer.factionsx;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Gson-mapped record for FactionsX Claims.json root object.
 */
public record FxClaims(
  @Nullable List<FxDimension> Dimensions
) {}
