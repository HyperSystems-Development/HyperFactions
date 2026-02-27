package com.hyperfactions.importer;

import java.util.*;
import org.jetbrains.annotations.NotNull;

/**
 * Pre-import validation report for HyFactions data.
 * Identifies potential conflicts and issues before actual import.
 */
public record ImportValidationReport(
  int totalFactions,
  int totalMembers,
  int totalClaims,
  int totalSafeZoneChunks,
  int totalWarZoneChunks,
  @NotNull List<String> nameConflicts,
  @NotNull List<String> idConflicts,
  @NotNull List<String> memberConflicts,
  @NotNull List<String> invalidUuids,
  @NotNull List<String> worldWarnings,
  @NotNull List<String> warnings,
  @NotNull List<String> errors,
  boolean valid
) {
  /**
   * Creates a builder for constructing a validation report.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Checks if there are any blocking issues that prevent import.
   */
  public boolean hasBlockingIssues() {
    return !errors.isEmpty();
  }

  /**
   * Checks if there are any conflicts that require attention.
   */
  public boolean hasConflicts() {
    return !nameConflicts.isEmpty() || !idConflicts.isEmpty() || !memberConflicts.isEmpty();
  }

  /**
   * Gets all issues as a formatted list.
   */
  @NotNull
  public List<String> getAllIssues() {
    List<String> issues = new ArrayList<>();
    errors.forEach(e -> issues.add("[ERROR] " + e));
    nameConflicts.forEach(c -> issues.add("[NAME CONFLICT] " + c));
    idConflicts.forEach(c -> issues.add("[ID CONFLICT] " + c));
    memberConflicts.forEach(c -> issues.add("[MEMBER CONFLICT] " + c));
    invalidUuids.forEach(u -> issues.add("[INVALID UUID] " + u));
    worldWarnings.forEach(w -> issues.add("[WORLD] " + w));
    warnings.forEach(w -> issues.add("[WARNING] " + w));
    return issues;
  }

  /**
   * Gets a summary of the validation.
   */
  @NotNull
  public String getSummary() {
    StringBuilder sb = new StringBuilder();
    sb.append("Validation ").append(valid ? "PASSED" : "FAILED").append("\n");
    sb.append("Factions: ").append(totalFactions).append("\n");
    sb.append("Members: ").append(totalMembers).append("\n");
    sb.append("Claims: ").append(totalClaims).append("\n");
    sb.append("Safe Zones: ").append(totalSafeZoneChunks).append(" chunks\n");
    sb.append("War Zones: ").append(totalWarZoneChunks).append(" chunks\n");

    if (!nameConflicts.isEmpty()) {
      sb.append("Name conflicts: ").append(nameConflicts.size()).append("\n");
    }
    if (!idConflicts.isEmpty()) {
      sb.append("ID conflicts: ").append(idConflicts.size()).append("\n");
    }
    if (!memberConflicts.isEmpty()) {
      sb.append("Member conflicts: ").append(memberConflicts.size()).append("\n");
    }
    if (!invalidUuids.isEmpty()) {
      sb.append("Invalid UUIDs: ").append(invalidUuids.size()).append("\n");
    }
    if (!worldWarnings.isEmpty()) {
      sb.append("World warnings: ").append(worldWarnings.size()).append("\n");
    }
    if (!warnings.isEmpty()) {
      sb.append("Warnings: ").append(warnings.size()).append("\n");
    }
    if (!errors.isEmpty()) {
      sb.append("Errors: ").append(errors.size()).append("\n");
    }

    return sb.toString();
  }

  /**
   * Builder for ImportValidationReport.
   */
  public static class Builder {
    private int totalFactions = 0;

    private int totalMembers = 0;

    private int totalClaims = 0;

    private int totalSafeZoneChunks = 0;

    private int totalWarZoneChunks = 0;

    private final List<String> nameConflicts = new ArrayList<>();

    private final List<String> idConflicts = new ArrayList<>();

    private final List<String> memberConflicts = new ArrayList<>();

    private final List<String> invalidUuids = new ArrayList<>();

    private final List<String> worldWarnings = new ArrayList<>();

    private final List<String> warnings = new ArrayList<>();

    private final List<String> errors = new ArrayList<>();

    /** Converts to tal factions. */
    public Builder totalFactions(int count) {
      this.totalFactions = count;
      return this;
    }

    /** Converts to tal members. */
    public Builder totalMembers(int count) {
      this.totalMembers = count;
      return this;
    }

    /** Adds members. */
    public Builder addMembers(int count) {
      this.totalMembers += count;
      return this;
    }

    /** Converts to tal claims. */
    public Builder totalClaims(int count) {
      this.totalClaims = count;
      return this;
    }

    /** Adds claims. */
    public Builder addClaims(int count) {
      this.totalClaims += count;
      return this;
    }

    public Builder totalSafeZoneChunks(int count) {
      this.totalSafeZoneChunks = count;
      return this;
    }

    /** Converts to tal war zone chunks. */
    public Builder totalWarZoneChunks(int count) {
      this.totalWarZoneChunks = count;
      return this;
    }

    /** Name Conflict. */
    public Builder nameConflict(String message) {
      this.nameConflicts.add(message);
      return this;
    }

    /** Id Conflict. */
    public Builder idConflict(String message) {
      this.idConflicts.add(message);
      return this;
    }

    /** Member Conflict. */
    public Builder memberConflict(String message) {
      this.memberConflicts.add(message);
      return this;
    }

    /** Invalid Uuid. */
    public Builder invalidUuid(String message) {
      this.invalidUuids.add(message);
      return this;
    }

    /** World Warning. */
    public Builder worldWarning(String message) {
      this.worldWarnings.add(message);
      return this;
    }

    public Builder warning(String message) {
      this.warnings.add(message);
      return this;
    }

    /** Error. */
    public Builder error(String message) {
      this.errors.add(message);
      return this;
    }

    /** Builds . */
    public ImportValidationReport build() {
      boolean valid = errors.isEmpty();
      return new ImportValidationReport(
        totalFactions,
        totalMembers,
        totalClaims,
        totalSafeZoneChunks,
        totalWarZoneChunks,
        Collections.unmodifiableList(new ArrayList<>(nameConflicts)),
        Collections.unmodifiableList(new ArrayList<>(idConflicts)),
        Collections.unmodifiableList(new ArrayList<>(memberConflicts)),
        Collections.unmodifiableList(new ArrayList<>(invalidUuids)),
        Collections.unmodifiableList(new ArrayList<>(worldWarnings)),
        Collections.unmodifiableList(new ArrayList<>(warnings)),
        Collections.unmodifiableList(new ArrayList<>(errors)),
        valid
      );
    }
  }
}
