package com.hyperfactions.importer.simpleclaims;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Gson-mapped record for the SimpleClaims {@code Claims.json} root object.
 */
public record ScClaims(
  @Nullable List<ScDimension> Dimensions
) {}
