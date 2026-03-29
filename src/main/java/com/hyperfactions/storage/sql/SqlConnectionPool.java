package com.hyperfactions.storage.sql;

import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.sentry.Sentry;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;

/**
 * HikariCP connection pool wrapper with retry logic and Sentry context.
 *
 * <p>Handles JDBC URL construction, dialect-specific optimizations, and
 * connection retry with exponential backoff on startup failure.</p>
 */
public class SqlConnectionPool {

  private final HikariDataSource dataSource;
  private final Jdbi jdbi;
  private final SqlConfig config;
  private String databaseProductName;
  private String databaseProductVersion;

  /**
   * Creates and initializes a connection pool.
   *
   * @param config the SQL configuration
   * @throws SqlConnectionException if connection cannot be established after retries
   */
  public SqlConnectionPool(@NotNull SqlConfig config) {
    this.config = config;

    HikariConfig hikari = new HikariConfig();
    hikari.setPoolName("HyperFactions-SQL");
    hikari.setJdbcUrl(config.dialect().buildJdbcUrl(config.host(), config.port(), config.database()));
    hikari.setUsername(config.username());
    hikari.setPassword(config.password());
    hikari.setMaximumPoolSize(config.poolMaxSize());
    hikari.setMinimumIdle(config.poolMinIdle());
    hikari.setIdleTimeout(config.idleTimeoutMs());
    hikari.setMaxLifetime(config.maxLifetimeMs());
    hikari.setConnectionTimeout(config.connectionTimeoutMs());
    hikari.setKeepaliveTime(config.keepaliveTimeMs());

    // Don't fail immediately on startup — we handle retries ourselves
    hikari.setInitializationFailTimeout(-1);

    // Dialect-specific optimizations
    if (config.dialect() == SqlDialect.MYSQL) {
      hikari.addDataSourceProperty("cachePrepStmts", "true");
      hikari.addDataSourceProperty("prepStmtCacheSize", "250");
      hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
      hikari.addDataSourceProperty("useServerPrepStmts", "true");
      hikari.addDataSourceProperty("useSSL", "false");
      hikari.addDataSourceProperty("allowPublicKeyRetrieval", "true");
    }

    this.dataSource = createWithRetry(hikari, config);
    this.jdbi = JdbiFactory.create(dataSource, config);

    // Fetch and tag database metadata for Sentry
    tagSentryContext();
  }

  /**
   * Creates the HikariDataSource with retry logic.
   */
  private HikariDataSource createWithRetry(HikariConfig hikari, SqlConfig config) {
    HikariDataSource ds = new HikariDataSource(hikari);

    // Validate we can actually connect
    long backoff = config.retryBackoffMs();
    for (int attempt = 1; attempt <= config.maxRetries(); attempt++) {
      try (Connection conn = ds.getConnection()) {
        Logger.info("[Storage] Connected to %s at %s:%d/%s (pool: %s)",
            config.dialect().name(), config.host(), config.port(), config.database(), ds.getPoolName());
        return ds;
      } catch (SQLException e) {
        Logger.warn("[Storage] Connection attempt %d/%d failed: %s", attempt, config.maxRetries(), e.getMessage());
        if (attempt < config.maxRetries()) {
          try {
            Thread.sleep(backoff);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            break;
          }
          backoff *= 2; // Exponential backoff
        }
      }
    }

    // All retries exhausted — close pool and throw
    ds.close();
    throw new SqlConnectionException(String.format(
        "Could not connect to %s at %s:%d/%s after %d attempts. Check config/storage.json.",
        config.dialect().name(), config.host(), config.port(), config.database(), config.maxRetries()));
  }

  /**
   * Fetches database metadata and sets Sentry tags for error context.
   */
  private void tagSentryContext() {
    try (Connection conn = dataSource.getConnection()) {
      DatabaseMetaData meta = conn.getMetaData();
      this.databaseProductName = meta.getDatabaseProductName();
      this.databaseProductVersion = meta.getDatabaseProductVersion();

      Sentry.configureScope(scope -> {
        scope.setTag("storage.type", config.dialect().name().toLowerCase());
        scope.setTag("storage.database", databaseProductName);
        scope.setTag("storage.version", databaseProductVersion);
        scope.setTag("storage.prefix", config.tablePrefix());
      });

      Logger.info("[Storage] Database: %s %s", databaseProductName, databaseProductVersion);
    } catch (Exception e) {
      Logger.warn("[Storage] Could not fetch database metadata for Sentry: %s", e.getMessage());
    }
  }

  /** Returns the JDBI instance for fluent SQL operations. */
  @NotNull
  public Jdbi getJdbi() {
    return jdbi;
  }

  /** Returns the underlying HikariCP data source. */
  @NotNull
  public HikariDataSource getDataSource() {
    return dataSource;
  }

  /** Returns the SQL configuration. */
  @NotNull
  public SqlConfig getConfig() {
    return config;
  }

  /** Returns the database product name (e.g., "MySQL", "MariaDB", "PostgreSQL"). */
  public String getDatabaseProductName() {
    return databaseProductName;
  }

  /** Returns the database product version string. */
  public String getDatabaseProductVersion() {
    return databaseProductVersion;
  }

  /**
   * Gracefully shuts down the connection pool.
   * Waits for active connections to complete.
   */
  public void shutdown() {
    if (dataSource != null && !dataSource.isClosed()) {
      Logger.info("[Storage] Shutting down SQL connection pool...");
      dataSource.close();
      Logger.info("[Storage] SQL connection pool closed");
    }
  }

  /**
   * Thrown when the connection pool cannot be established after retries.
   */
  public static class SqlConnectionException extends RuntimeException {
    public SqlConnectionException(String message) {
      super(message);
    }
  }
}
