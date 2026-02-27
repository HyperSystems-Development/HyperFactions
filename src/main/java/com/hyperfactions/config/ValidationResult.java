package com.hyperfactions.config;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Result of configuration validation.
 *
 * <p>
 * Collects warnings and errors found during validation. Warnings indicate
 * invalid values that were corrected to defaults. Errors indicate values
 * that couldn't be corrected but won't crash the plugin.
 */
public class ValidationResult {

  /**
   * Severity level for validation issues.
   */
  public enum Severity {
    /** Value is out of range but was auto-corrected to a valid value. */
    WARNING,
    /** Value is invalid but plugin will continue with potentially unexpected behavior. */
    ERROR
  }

  /**
   * A single validation issue.
   *
   * @param severity    severity of the issue
   * @param configFile  name of the config file
   * @param field       field/setting name
   * @param message     description of the issue
   * @param originalValue the invalid value that was found
   * @param correctedValue the corrected value (null if not corrected)
   */
  public record Issue(
    @NotNull Severity severity,
    @NotNull String configFile,
    @NotNull String field,
    @NotNull String message,
    @NotNull String originalValue,
    String correctedValue
  ) {
    /**
     * Creates a warning issue with auto-correction.
     */
    public static Issue warning(@NotNull String configFile, @NotNull String field,
                  @NotNull String message, Object original, Object corrected) {
      return new Issue(Severity.WARNING, configFile, field, message,
          String.valueOf(original), String.valueOf(corrected));
    }

    /**
     * Creates a warning issue without correction.
     */
    public static Issue warning(@NotNull String configFile, @NotNull String field,
                  @NotNull String message, Object original) {
      return new Issue(Severity.WARNING, configFile, field, message,
          String.valueOf(original), null);
    }

    /**
     * Creates an error issue.
     */
    public static Issue error(@NotNull String configFile, @NotNull String field,
                 @NotNull String message, Object original) {
      return new Issue(Severity.ERROR, configFile, field, message,
          String.valueOf(original), null);
    }

    /** Converts to string. */
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("[").append(severity).append("] ");
      sb.append(configFile).append(" -> ").append(field).append(": ");
      sb.append(message);
      sb.append(" (was: ").append(originalValue);
      if (correctedValue != null) {
        sb.append(", corrected to: ").append(correctedValue);
      }
      sb.append(")");
      return sb.toString();
    }
  }

  private final List<Issue> issues = new ArrayList<>();

  private boolean needsSave = false;

  /**
   * Adds an issue to the result.
   *
   * @param issue the issue to add
   */
  public void addIssue(@NotNull Issue issue) {
    issues.add(issue);
    if (issue.correctedValue() != null) {
      needsSave = true;
    }
  }

  /**
   * Adds a warning with auto-correction.
   *
   * @param configFile name of the config file
   * @param field      field name
   * @param message    description
   * @param original   original invalid value
   * @param corrected  corrected value
   */
  public void addWarning(@NotNull String configFile, @NotNull String field,
             @NotNull String message, Object original, Object corrected) {
    addIssue(Issue.warning(configFile, field, message, original, corrected));
  }

  /**
   * Adds a warning without correction.
   *
   * @param configFile name of the config file
   * @param field      field name
   * @param message    description
   * @param original   original value
   */
  public void addWarning(@NotNull String configFile, @NotNull String field,
             @NotNull String message, Object original) {
    addIssue(Issue.warning(configFile, field, message, original));
  }

  /**
   * Adds an error.
   *
   * @param configFile name of the config file
   * @param field      field name
   * @param message    description
   * @param original   original value
   */
  public void addError(@NotNull String configFile, @NotNull String field,
            @NotNull String message, Object original) {
    addIssue(Issue.error(configFile, field, message, original));
  }

  /**
   * Gets all validation issues.
   *
   * @return list of issues
   */
  @NotNull
  public List<Issue> getIssues() {
    return issues;
  }

  /**
   * Gets only warnings.
   *
   * @return list of warnings
   */
  @NotNull
  public List<Issue> getWarnings() {
    return issues.stream()
        .filter(i -> i.severity() == Severity.WARNING)
        .toList();
  }

  /**
   * Gets only errors.
   *
   * @return list of errors
   */
  @NotNull
  public List<Issue> getErrors() {
    return issues.stream()
        .filter(i -> i.severity() == Severity.ERROR)
        .toList();
  }

  /**
   * Checks if there are any issues.
   *
   * @return true if there are issues
   */
  public boolean hasIssues() {
    return !issues.isEmpty();
  }

  /**
   * Checks if there are any errors.
   *
   * @return true if there are errors
   */
  public boolean hasErrors() {
    return issues.stream().anyMatch(i -> i.severity() == Severity.ERROR);
  }

  /**
   * Checks if any corrections were made that require saving.
   *
   * @return true if config should be re-saved
   */
  public boolean needsSave() {
    return needsSave;
  }

  /**
   * Gets the count of issues.
   *
   * @return issue count
   */
  public int size() {
    return issues.size();
  }

  /**
   * Merges another result into this one.
   *
   * @param other result to merge
   */
  public void merge(@NotNull ValidationResult other) {
    issues.addAll(other.issues);
    if (other.needsSave) {
      needsSave = true;
    }
  }
}
