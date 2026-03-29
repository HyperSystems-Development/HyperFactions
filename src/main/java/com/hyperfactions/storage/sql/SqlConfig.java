package com.hyperfactions.storage.sql;

import org.jetbrains.annotations.NotNull;

/**
 * Configuration for SQL database connections.
 *
 * @param dialect           the SQL dialect (MySQL or PostgreSQL)
 * @param host              database host
 * @param port              database port
 * @param database          database name
 * @param username          database username
 * @param password          database password
 * @param tablePrefix       prefix for all table names (default: "hf")
 * @param poolMaxSize       maximum connection pool size (default: 10)
 * @param poolMinIdle       minimum idle connections (default: 2)
 * @param idleTimeoutMs     idle connection timeout in ms (default: 300,000 = 5 min)
 * @param maxLifetimeMs     max connection lifetime in ms (default: 1,800,000 = 30 min)
 * @param connectionTimeoutMs connection acquisition timeout in ms (default: 10,000 = 10 sec)
 * @param keepaliveTimeMs   keepalive ping interval in ms (default: 60,000 = 1 min)
 * @param maxRetries        max connection retry attempts on startup (default: 3)
 * @param retryBackoffMs    base backoff between retries in ms (default: 1,000)
 */
public record SqlConfig(
    @NotNull SqlDialect dialect,
    @NotNull String host,
    int port,
    @NotNull String database,
    @NotNull String username,
    @NotNull String password,
    @NotNull String tablePrefix,
    int poolMaxSize,
    int poolMinIdle,
    long idleTimeoutMs,
    long maxLifetimeMs,
    long connectionTimeoutMs,
    long keepaliveTimeMs,
    int maxRetries,
    long retryBackoffMs
) {

  /** Creates a config with sensible defaults for the given dialect. */
  public static SqlConfig defaults(@NotNull SqlDialect dialect) {
    return new SqlConfig(
        dialect,
        "localhost",
        dialect.getDefaultPort(),
        "hyperfactions",
        "root",
        "",
        "hf",
        10,
        2,
        300_000L,
        1_800_000L,
        10_000L,
        60_000L,
        3,
        1_000L
    );
  }
}
