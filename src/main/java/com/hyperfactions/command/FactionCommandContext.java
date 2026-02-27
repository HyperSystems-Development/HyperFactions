package com.hyperfactions.command;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Context wrapper for faction commands that handles flag parsing.
 * Supports --text / -t flag for text-only mode (no GUI).
 */
public class FactionCommandContext {

  private final String[] cleanedArgs;

  private final boolean textMode;

  /**
   * Creates a command context from parsed results.
   */
  private FactionCommandContext(String[] cleanedArgs, boolean textMode) {
    this.cleanedArgs = cleanedArgs;
    this.textMode = textMode;
  }

  /**
   * Parses command arguments and extracts flags.
   *
   * @param args The raw command arguments
   * @return A FactionCommandContext with cleaned args and flag states
   */
  @NotNull
  public static FactionCommandContext parse(@NotNull String[] args) {
    List<String> cleaned = new ArrayList<>();
    boolean textMode = false;

    for (String arg : args) {
      if (arg.equalsIgnoreCase("--text") || arg.equalsIgnoreCase("-t")) {
        textMode = true;
      } else {
        cleaned.add(arg);
      }
    }

    return new FactionCommandContext(cleaned.toArray(new String[0]), textMode);
  }

  /**
   * Gets the cleaned arguments (without flags).
   *
   * @return The arguments with flags removed
   */
  @NotNull
  public String[] getArgs() {
    return cleanedArgs;
  }

  /**
   * Checks if text mode is enabled (--text or -t flag was present).
   * When true, commands should output to chat instead of opening GUIs.
   *
   * @return true if text mode is enabled
   */
  public boolean isTextMode() {
    return textMode;
  }

  /**
   * Checks if there are any arguments (after flag removal).
   *
   * @return true if there are arguments
   */
  public boolean hasArgs() {
    return cleanedArgs.length > 0;
  }

  /**
   * Gets the number of arguments (after flag removal).
   *
   * @return The argument count
   */
  public int argCount() {
    return cleanedArgs.length;
  }

  /**
   * Gets an argument at the specified index, or null if out of bounds.
   *
   * @param index The argument index
   * @return The argument or null
   */
  public String getArg(int index) {
    if (index < 0 || index >= cleanedArgs.length) {
      return null;
    }
    return cleanedArgs[index];
  }

  /**
   * Joins all arguments into a single string.
   *
   * @return All arguments joined with spaces
   */
  @NotNull
  public String joinArgs() {
    return String.join(" ", cleanedArgs);
  }

  /**
   * Joins arguments from the specified index onwards.
   *
   * @param startIndex The starting index (inclusive)
   * @return The joined arguments
   */
  @NotNull
  public String joinArgs(int startIndex) {
    if (startIndex >= cleanedArgs.length) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (int i = startIndex; i < cleanedArgs.length; i++) {
      if (i > startIndex) {
        sb.append(" ");
      }
      sb.append(cleanedArgs[i]);
    }
    return sb.toString();
  }

  /**
   * Determines if GUI mode should be used based on context.
   * GUI mode is used when:
   * - Text mode is NOT enabled (no --text/-t flag)
   * - AND there are no arguments (default behavior opens GUI)
   *
   * @return true if GUI should be opened
   */
  public boolean shouldOpenGui() {
    return !textMode && !hasArgs();
  }

  /**
   * Determines if a GUI should be opened for a specific action.
   * Use this for commands that always perform an action but may show GUI after.
   * Opens GUI when text mode is NOT enabled.
   *
   * @return true if GUI should be opened after action
   */
  public boolean shouldOpenGuiAfterAction() {
    return !textMode;
  }

  /** Converts to string. */
  @Override
  public String toString() {
    return "FactionCommandContext{"
        + "cleanedArgs=" + String.join(", ", cleanedArgs) +
        ", textMode=" + textMode
        + '}';
  }
}
