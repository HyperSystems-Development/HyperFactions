package com.hyperfactions.importer.simpleclaims;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Gson-mapped record for the SimpleClaims {@code Parties.json} root object.
 */
public record ScParties(
  @Nullable List<ScParty> Parties
) {}
