package com.hyperfactions.importer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Results of an import operation.
 */
public record ImportResult(
  int factionsImported,
  int factionsSkipped,
  int claimsImported,
  int zonesCreated,
  int playersWithPower,
  @NotNull List<String> warnings,
  @NotNull List<String> errors,
  boolean dryRun
) {
  /**
   * Creates a builder for constructing an ImportResult.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Checks if the import had any errors.
   *
   * @return true if there were errors
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  /**
   * Checks if the import had any warnings.
   *
   * @return true if there were warnings
   */
  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  /**
   * Gets the total number of items imported.
   *
   * @return total count
   */
  public int getTotalImported() {
    return factionsImported + claimsImported + zonesCreated + playersWithPower;
  }

  /**
   * Builder for ImportResult.
   */
  public static class Builder {
    private int factionsImported = 0;

    private int factionsSkipped = 0;

    private int claimsImported = 0;

    private int zonesCreated = 0;

    private int playersWithPower = 0;

    private final List<String> warnings = new ArrayList<>();

    private final List<String> errors = new ArrayList<>();

    private boolean dryRun = false;

    /** Factions Imported. */
    public Builder factionsImported(int count) {
      this.factionsImported = count;
      return this;
    }

    /** Increment Factions Imported. */
    public Builder incrementFactionsImported() {
      this.factionsImported++;
      return this;
    }

    /** Factions Skipped. */
    public Builder factionsSkipped(int count) {
      this.factionsSkipped = count;
      return this;
    }

    /** Increment Factions Skipped. */
    public Builder incrementFactionsSkipped() {
      this.factionsSkipped++;
      return this;
    }

    /** Claims Imported. */
    public Builder claimsImported(int count) {
      this.claimsImported = count;
      return this;
    }

    public Builder addClaimsImported(int count) {
      this.claimsImported += count;
      return this;
    }

    /** Zones Created. */
    public Builder zonesCreated(int count) {
      this.zonesCreated = count;
      return this;
    }

    /** Increment Zones Created. */
    public Builder incrementZonesCreated() {
      this.zonesCreated++;
      return this;
    }

    /** Players With Power. */
    public Builder playersWithPower(int count) {
      this.playersWithPower = count;
      return this;
    }

    /** Adds players with power. */
    public Builder addPlayersWithPower(int count) {
      this.playersWithPower += count;
      return this;
    }

    /** Warning. */
    public Builder warning(String message) {
      this.warnings.add(message);
      return this;
    }

    /** Error. */
    public Builder error(String message) {
      this.errors.add(message);
      return this;
    }

    public Builder dryRun(boolean dryRun) {
      this.dryRun = dryRun;
      return this;
    }

    /** Builds . */
    public ImportResult build() {
      return new ImportResult(
        factionsImported,
        factionsSkipped,
        claimsImported,
        zonesCreated,
        playersWithPower,
        Collections.unmodifiableList(new ArrayList<>(warnings)),
        Collections.unmodifiableList(new ArrayList<>(errors)),
        dryRun
      );
    }
  }
}
