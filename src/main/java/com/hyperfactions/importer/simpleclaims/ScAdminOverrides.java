package com.hyperfactions.importer.simpleclaims;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Gson-mapped record for the SimpleClaims {@code AdminOverrides.json} root object.
 */
public record ScAdminOverrides(
  @Nullable List<String> AdminOverrides
) {}
