package com.hyperfactions.storage;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tracks the health of storage write operations.
 * Provides monitoring for success/failure rates and recent errors.
 */
public final class StorageHealth {

  private static final StorageHealth INSTANCE = new StorageHealth();

  /** Maximum number of recent failures to track. */
  private static final int MAX_RECENT_FAILURES = 50;

  /** Time window for rate calculation (milliseconds) - 5 minutes. */
  private static final long RATE_WINDOW_MS = 5 * 60 * 1000;

  /** Success counts per file path. */
  private final Map<String, AtomicLong> successCounts = new ConcurrentHashMap<>();

  /** Failure counts per file path. */
  private final Map<String, AtomicLong> failureCounts = new ConcurrentHashMap<>();

  /** Total success count. */
  private final AtomicLong totalSuccesses = new AtomicLong(0);

  /** Total failure count. */
  private final AtomicLong totalFailures = new AtomicLong(0);

  /** Recent failures for debugging. */
  private final LinkedList<FailureRecord> recentFailures = new LinkedList<>();

  /** Timestamped writes for rate calculation. */
  private final LinkedList<TimestampedWrite> recentWrites = new LinkedList<>();

  private StorageHealth() {}

  /**
   * Gets the singleton instance.
   *
   * @return the StorageHealth instance
   */
  @NotNull
  public static StorageHealth get() {
    return INSTANCE;
  }

  /**
   * Records a successful write operation.
   *
   * @param filePath the path of the file that was written
   */
  public void recordSuccess(@NotNull String filePath) {
    totalSuccesses.incrementAndGet();
    successCounts.computeIfAbsent(filePath, k -> new AtomicLong(0)).incrementAndGet();
    recordTimestampedWrite(true);
  }

  /**
   * Records a failed write operation.
   *
   * @param filePath the path of the file that failed
   * @param error    human-readable error message
   */
  public void recordFailure(@NotNull String filePath, @NotNull String error) {
    totalFailures.incrementAndGet();
    failureCounts.computeIfAbsent(filePath, k -> new AtomicLong(0)).incrementAndGet();
    recordTimestampedWrite(false);

    synchronized (recentFailures) {
      recentFailures.addFirst(new FailureRecord(filePath, error, Instant.now()));
      while (recentFailures.size() > MAX_RECENT_FAILURES) {
        recentFailures.removeLast();
      }
    }
  }

  /**
   * Records a timestamped write for rate calculation.
   */
  private void recordTimestampedWrite(boolean success) {
    long now = System.currentTimeMillis();
    synchronized (recentWrites) {
      recentWrites.addFirst(new TimestampedWrite(now, success));
      // Prune old entries outside the rate window
      while (!recentWrites.isEmpty() && (now - recentWrites.getLast().timestamp) > RATE_WINDOW_MS) {
        recentWrites.removeLast();
      }
    }
  }

  /**
   * Checks if the storage system is healthy.
   * Returns false if the recent failure rate exceeds 10%.
   *
   * @return true if storage is healthy
   */
  public boolean isHealthy() {
    return getRecentFailureRate() < 0.10;
  }

  /**
   * Gets the failure rate in the recent time window.
   *
   * @return failure rate as a decimal (0.0 - 1.0)
   */
  public double getRecentFailureRate() {
    long now = System.currentTimeMillis();
    int successes = 0;
    int failures = 0;

    synchronized (recentWrites) {
      for (TimestampedWrite write : recentWrites) {
        if ((now - write.timestamp) <= RATE_WINDOW_MS) {
          if (write.success) {
            successes++;
          } else {
            failures++;
          }
        }
      }
    }

    int total = successes + failures;
    if (total == 0) {
      return 0.0; // No writes = healthy
    }

    return (double) failures / total;
  }

  /**
   * Gets the total number of successful writes.
   *
   * @return success count
   */
  public long getTotalSuccesses() {
    return totalSuccesses.get();
  }

  /**
   * Gets the total number of failed writes.
   *
   * @return failure count
   */
  public long getTotalFailures() {
    return totalFailures.get();
  }

  /**
   * Gets the success count for a specific file path.
   *
   * @param filePath the file path
   * @return success count for that path
   */
  public long getSuccessCount(@NotNull String filePath) {
    AtomicLong count = successCounts.get(filePath);
    return count != null ? count.get() : 0;
  }

  /**
   * Gets the failure count for a specific file path.
   *
   * @param filePath the file path
   * @return failure count for that path
   */
  public long getFailureCount(@NotNull String filePath) {
    AtomicLong count = failureCounts.get(filePath);
    return count != null ? count.get() : 0;
  }

  /**
   * Gets the list of recent failures for debugging.
   *
   * @return copy of recent failures (most recent first)
   */
  @NotNull
  public List<FailureRecord> getRecentFailures() {
    synchronized (recentFailures) {
      return new LinkedList<>(recentFailures);
    }
  }

  /**
   * Gets the most recent failure, if any.
   *
   * @return the most recent failure, or null if none
   */
  @Nullable
  public FailureRecord getMostRecentFailure() {
    synchronized (recentFailures) {
      return recentFailures.isEmpty() ? null : recentFailures.getFirst();
    }
  }

  /**
   * Gets a summary status string for monitoring/logging.
   *
   * @return status summary
   */
  @NotNull
  public String getStatusSummary() {
    double failureRate = getRecentFailureRate();
    String healthStatus = isHealthy() ? "HEALTHY" : "UNHEALTHY";
    return String.format("%s - Success: %d, Failure: %d, Recent Rate: %.1f%% failures",
      healthStatus, totalSuccesses.get(), totalFailures.get(), failureRate * 100);
  }

  /**
   * Resets all counters. Use for testing.
   */
  public void reset() {
    totalSuccesses.set(0);
    totalFailures.set(0);
    successCounts.clear();
    failureCounts.clear();
    synchronized (recentFailures) {
      recentFailures.clear();
    }

    synchronized (recentWrites) {
      recentWrites.clear();
    }
  }

  /**
   * Record of a failed write operation.
   */
  public record FailureRecord(
    @NotNull String filePath,
    @NotNull String error,
    @NotNull Instant timestamp
  ) {}

  /**
   * Timestamped write for rate calculation.
   */
  private record TimestampedWrite(long timestamp, boolean success) {}
}
