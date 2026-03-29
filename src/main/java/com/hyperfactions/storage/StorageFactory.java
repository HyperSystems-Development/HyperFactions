package com.hyperfactions.storage;

import com.hyperfactions.config.modules.StorageConfig;
import com.hyperfactions.storage.json.*;
import com.hyperfactions.storage.sql.*;
import com.hyperfactions.util.Logger;
import io.sentry.Sentry;
import java.io.IOException;
import java.nio.file.Path;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Factory that creates the appropriate storage backend based on configuration.
 *
 * <p>Returns a {@link StorageBundle} containing all storage interfaces wired
 * to either JSON or SQL implementations.</p>
 */
public final class StorageFactory {

  private StorageFactory() {}

  /**
   * Bundle of all storage interfaces plus optional SQL connection pool.
   */
  public record StorageBundle(
      @NotNull FactionStorage factionStorage,
      @NotNull PlayerStorage playerStorage,
      @NotNull ZoneStorage zoneStorage,
      @NotNull ChatHistoryStorage chatHistoryStorage,
      @NotNull EconomyStorage economyStorage,
      @NotNull InviteStorage inviteStorage,
      @NotNull JoinRequestStorage joinRequestStorage,
      @Nullable SqlConnectionPool connectionPool,
      @NotNull String storageType
  ) {}

  /**
   * Creates the storage bundle based on configuration.
   *
   * @param config  the storage configuration
   * @param dataDir the plugin data directory (for JSON storage paths)
   * @return the storage bundle
   * @throws SqlConnectionPool.SqlConnectionException if SQL connection fails
   */
  @NotNull
  public static StorageBundle create(@NotNull StorageConfig config, @NotNull Path dataDir) {
    String type = config.getType().toLowerCase().trim();

    return switch (type) {
      case "mysql", "mariadb" -> createSqlBundle(config, SqlDialect.MYSQL, dataDir);
      case "postgresql", "postgres" -> createSqlBundle(config, SqlDialect.POSTGRESQL, dataDir);
      default -> {
        if (!"json".equals(type) && !"file".equals(type) && !"flatfile".equals(type)) {
          Logger.warn("[Storage] Unknown storage type '%s', falling back to JSON", type);
        }
        yield createJsonBundle(dataDir);
      }
    };
  }

  /**
   * Creates the JSON storage bundle (default, zero dependencies).
   */
  @NotNull
  private static StorageBundle createJsonBundle(@NotNull Path dataDir) {
    Logger.info("[Storage] Using JSON file storage");

    // Tag Sentry with storage type
    Sentry.configureScope(scope -> scope.setTag("storage.type", "json"));

    return new StorageBundle(
        new JsonFactionStorage(dataDir),
        new JsonPlayerStorage(dataDir),
        new JsonZoneStorage(dataDir),
        new JsonChatHistoryStorage(dataDir),
        new JsonEconomyStorage(dataDir),
        new JsonInviteStorage(dataDir),
        new JsonJoinRequestStorage(dataDir),
        null,
        "json"
    );
  }

  /**
   * Creates the SQL storage bundle with connection pooling, schema migration,
   * and runtime driver download.
   */
  @NotNull
  private static StorageBundle createSqlBundle(@NotNull StorageConfig config,
                         @NotNull SqlDialect dialect,
                         @NotNull Path dataDir) {
    SqlConfig sqlConfig = config.toSqlConfig();

    Logger.info("[Storage] Using %s storage at %s:%d/%s (prefix: %s)",
        dialect.name(), sqlConfig.host(), sqlConfig.port(),
        sqlConfig.database(), sqlConfig.tablePrefix());

    // Download JDBC driver if needed
    Path libsDir = dataDir.resolve("libs");
    try {
      DriverDownloader.ensureDriver(dialect, libsDir);
    } catch (IOException e) {
      throw new RuntimeException("Failed to download JDBC driver for " + dialect.name()
          + ". Check your internet connection or manually place the driver JAR in " + libsDir, e);
    }

    // Create connection pool (with retry)
    SqlConnectionPool pool = new SqlConnectionPool(sqlConfig);

    // Run Flyway schema migrations
    FlywayMigrator.migrate(pool.getDataSource(), dialect, sqlConfig.tablePrefix());

    // Get JDBI instance
    Jdbi jdbi = pool.getJdbi();
    String prefix = sqlConfig.tablePrefix();

    return new StorageBundle(
        new SqlFactionStorage(jdbi, dialect, prefix),
        new SqlPlayerStorage(jdbi, dialect, prefix),
        new SqlZoneStorage(jdbi, prefix),
        new SqlChatHistoryStorage(jdbi, prefix),
        new SqlEconomyStorage(jdbi, dialect, prefix),
        new SqlInviteStorage(jdbi, prefix),
        new SqlJoinRequestStorage(jdbi, prefix),
        pool,
        dialect.name().toLowerCase()
    );
  }
}
