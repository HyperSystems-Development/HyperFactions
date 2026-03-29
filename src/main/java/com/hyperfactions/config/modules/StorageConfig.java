package com.hyperfactions.config.modules;

import com.google.gson.JsonObject;
import com.hyperfactions.config.ModuleConfig;
import com.hyperfactions.config.ValidationResult;
import com.hyperfactions.storage.sql.SqlConfig;
import com.hyperfactions.storage.sql.SqlDialect;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Configuration for the storage backend.
 *
 * <p>Stored in {@code config/storage.json}. Controls whether HyperFactions
 * uses JSON file storage or a SQL database (MySQL/MariaDB/PostgreSQL).</p>
 */
public class StorageConfig extends ModuleConfig {

  // Storage type: "json", "mysql", "mariadb", "postgresql"
  private String type = "json";

  // SQL settings (only used when type != json)
  private String sqlDialect = "mysql";
  private String sqlHost = "localhost";
  private int sqlPort = 3306;
  private String sqlDatabase = "hyperfactions";
  private String sqlUsername = "root";
  private String sqlPassword = "";
  private String sqlTablePrefix = "hf";

  // Pool settings
  private int poolMaxSize = 10;
  private int poolMinIdle = 2;
  private long poolIdleTimeoutMs = 300_000;
  private long poolMaxLifetimeMs = 1_800_000;
  private long poolConnectionTimeoutMs = 10_000;

  // Retry settings
  private int retryMaxAttempts = 3;
  private long retryBackoffMs = 1_000;

  /**
   * Creates a new storage config.
   *
   * @param filePath path to config/storage.json
   */
  public StorageConfig(@NotNull Path filePath) {
    super(filePath);
  }

  @Override
  @NotNull
  public String getModuleName() {
    return "storage";
  }

  @Override
  protected void createDefaults() {
    enabled = true;
    type = "json";
    sqlDialect = "mysql";
    sqlHost = "localhost";
    sqlPort = 3306;
    sqlDatabase = "hyperfactions";
    sqlUsername = "root";
    sqlPassword = "";
    sqlTablePrefix = "hf";
    poolMaxSize = 10;
    poolMinIdle = 2;
    poolIdleTimeoutMs = 300_000;
    poolMaxLifetimeMs = 1_800_000;
    poolConnectionTimeoutMs = 10_000;
    retryMaxAttempts = 3;
    retryBackoffMs = 1_000;
  }

  @Override
  protected void loadModuleSettings(@NotNull JsonObject root) {
    type = getString(root, "type", type);

    if (root.has("sql")) {
      JsonObject sql = root.getAsJsonObject("sql");
      sqlDialect = getString(sql, "dialect", sqlDialect);
      sqlHost = getString(sql, "host", sqlHost);
      sqlPort = getInt(sql, "port", sqlPort);
      sqlDatabase = getString(sql, "database", sqlDatabase);
      sqlUsername = getString(sql, "username", sqlUsername);
      sqlPassword = getString(sql, "password", sqlPassword);
      sqlTablePrefix = getString(sql, "tablePrefix", sqlTablePrefix);

      if (sql.has("pool")) {
        JsonObject pool = sql.getAsJsonObject("pool");
        poolMaxSize = getInt(pool, "maxSize", poolMaxSize);
        poolMinIdle = getInt(pool, "minIdle", poolMinIdle);
        poolIdleTimeoutMs = getLong(pool, "idleTimeoutMs", poolIdleTimeoutMs);
        poolMaxLifetimeMs = getLong(pool, "maxLifetimeMs", poolMaxLifetimeMs);
        poolConnectionTimeoutMs = getLong(pool, "connectionTimeoutMs", poolConnectionTimeoutMs);
      }

      if (sql.has("retry")) {
        JsonObject retry = sql.getAsJsonObject("retry");
        retryMaxAttempts = getInt(retry, "maxAttempts", retryMaxAttempts);
        retryBackoffMs = getLong(retry, "backoffMs", retryBackoffMs);
      }
    }
  }

  @Override
  protected void writeModuleSettings(@NotNull JsonObject root) {
    root.addProperty("type", type);

    JsonObject sql = new JsonObject();
    sql.addProperty("dialect", sqlDialect);
    sql.addProperty("host", sqlHost);
    sql.addProperty("port", sqlPort);
    sql.addProperty("database", sqlDatabase);
    sql.addProperty("username", sqlUsername);
    sql.addProperty("password", sqlPassword);
    sql.addProperty("tablePrefix", sqlTablePrefix);

    JsonObject pool = new JsonObject();
    pool.addProperty("maxSize", poolMaxSize);
    pool.addProperty("minIdle", poolMinIdle);
    pool.addProperty("idleTimeoutMs", poolIdleTimeoutMs);
    pool.addProperty("maxLifetimeMs", poolMaxLifetimeMs);
    pool.addProperty("connectionTimeoutMs", poolConnectionTimeoutMs);
    sql.add("pool", pool);

    JsonObject retry = new JsonObject();
    retry.addProperty("maxAttempts", retryMaxAttempts);
    retry.addProperty("backoffMs", retryBackoffMs);
    sql.add("retry", retry);

    root.add("sql", sql);
  }

  // === Getters ===

  /** Returns the storage type: "json", "mysql", "mariadb", "postgresql". */
  @NotNull
  public String getType() {
    return type;
  }

  /** Returns true if the configured type is a SQL backend. */
  public boolean isSql() {
    return !"json".equalsIgnoreCase(type) && !"file".equalsIgnoreCase(type)
        && !"flatfile".equalsIgnoreCase(type);
  }

  /**
   * Converts the config fields into a {@link SqlConfig} record.
   *
   * @return the SQL configuration
   * @throws IllegalStateException if the dialect is unknown
   */
  @NotNull
  public SqlConfig toSqlConfig() {
    SqlDialect dialect = SqlDialect.fromString(sqlDialect);
    if (dialect == null) {
      dialect = SqlDialect.fromString(type);
    }
    if (dialect == null) {
      throw new IllegalStateException("Unknown SQL dialect: " + sqlDialect + " (type: " + type + ")");
    }

    int resolvedPort = sqlPort > 0 ? sqlPort : dialect.getDefaultPort();

    return new SqlConfig(
        dialect,
        sqlHost,
        resolvedPort,
        sqlDatabase,
        sqlUsername,
        sqlPassword,
        sqlTablePrefix,
        poolMaxSize,
        poolMinIdle,
        poolIdleTimeoutMs,
        poolMaxLifetimeMs,
        poolConnectionTimeoutMs,
        60_000L,  // keepalive - hardcoded sensible default
        retryMaxAttempts,
        retryBackoffMs
    );
  }

  // === Helper ===

  private long getLong(@NotNull JsonObject obj, @NotNull String key, long defaultValue) {
    return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsLong() : defaultValue;
  }
}
