package com.hyperfactions.util;

import com.hyperfactions.integration.SentryIntegration;
import io.sentry.SentryLevel;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Centralized error handling utility that logs to console and reports to Sentry.
 *
 * <p>All methods are safe to call regardless of whether Sentry is initialized —
 * {@link SentryIntegration#captureException(String, Throwable)} handles both
 * console logging and Sentry reporting internally.
 */
public final class ErrorHandler {

  private ErrorHandler() {}

  /**
   * Reports an exception with context. Use in existing catch blocks
   * that currently only call {@code Logger.severe()}.
   *
   * @param context description of what was happening when the error occurred
   * @param throwable the exception
   */
  public static void report(@NotNull String context, @NotNull Throwable throwable) {
    SentryIntegration.captureException(context, throwable);
  }

  /**
   * Reports a failure that may or may not have an exception cause.
   * If a cause is present, reports it as an exception to Sentry.
   * If no cause, sends a message event so the failure is still visible in Sentry.
   * Always logs to console via {@code Logger.severe()}.
   *
   * @param context description of what failed
   * @param cause the exception, or null if only a string error is available
   */
  public static void report(@NotNull String context, @Nullable Exception cause) {
    if (cause != null) {
      SentryIntegration.captureException(context, cause);
    } else {
      Logger.severe(context);
      SentryIntegration.captureMessage(context, SentryLevel.ERROR);
    }
  }

  /**
   * Wraps a {@link Runnable} so that any exception is caught, logged, and reported.
   * Use for scheduled/timer tasks to prevent exceptions from killing scheduler threads.
   *
   * @param context description of the task for error reports
   * @param task the runnable to wrap
   * @return a wrapped runnable that never throws
   */
  @NotNull
  public static Runnable wrapTask(@NotNull String context, @NotNull Runnable task) {
    return () -> {
      try {
        task.run();
      } catch (Exception e) {
        SentryIntegration.captureException(context, e);
      }
    };
  }

  /**
   * Adds exception handling to a {@link CompletableFuture} chain.
   * Logs and reports any exception, then returns {@code null} to prevent propagation.
   *
   * @param context description of the async operation
   * @param future the future to guard
   * @param <T> the future's result type
   * @return the same future with an {@code exceptionally} handler attached
   */
  @NotNull
  public static <T> CompletableFuture<T> guard(@NotNull String context, @NotNull CompletableFuture<T> future) {
    return future.exceptionally(e -> {
      SentryIntegration.captureException(context, e);
      return null;
    });
  }

  /**
   * Runs an action in isolation, catching and reporting any exception.
   * Use for shutdown/cleanup sequences where each step should run independently.
   *
   * @param context description of the action
   * @param action the action to run
   */
  public static void runSafely(@NotNull String context, @NotNull Runnable action) {
    try {
      action.run();
    } catch (Exception e) {
      SentryIntegration.captureException(context, e);
    }
  }

  /**
   * Runs a supplier in isolation, returning a default value on failure.
   * Use for operations that need a fallback value when they fail.
   *
   * @param context description of the operation
   * @param defaultValue the value to return on failure
   * @param supplier the supplier to run
   * @param <T> the return type
   * @return the supplier's result, or {@code defaultValue} on exception
   */
  public static <T> T runSafely(@NotNull String context, T defaultValue, @NotNull Supplier<T> supplier) {
    try {
      return supplier.get();
    } catch (Exception e) {
      SentryIntegration.captureException(context, e);
      return defaultValue;
    }
  }
}
