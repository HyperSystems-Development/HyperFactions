package com.hyperfactions.storage.sql;

import com.hyperfactions.util.Logger;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.jetbrains.annotations.NotNull;

/**
 * Manages SQL schema migrations using Flyway.
 *
 * <p>Migration SQL files are stored in {@code resources/db/migration/mysql/}
 * and {@code resources/db/migration/postgresql/}. Flyway placeholders
 * ({@code ${prefix}}) are used for table prefix substitution.</p>
 */
public final class FlywayMigrator {

  private FlywayMigrator() {}

  /**
   * Runs pending schema migrations.
   *
   * @param dataSource  the HikariCP data source
   * @param dialect     the SQL dialect (determines which migration folder to use)
   * @param tablePrefix the table prefix (injected as Flyway placeholder)
   */
  public static void migrate(@NotNull HikariDataSource dataSource,
                @NotNull SqlDialect dialect,
                @NotNull String tablePrefix) {
    String location = "classpath:db/migration/" + dialect.name().toLowerCase();

    Logger.info("[Storage] Running Flyway migrations from %s (prefix: %s)", location, tablePrefix);

    // Use the plugin's classloader so Flyway can find migration SQL files
    // in the shadow JAR. The Hytale PluginClassLoader doesn't propagate to
    // Flyway's relocated classpath scanner by default.
    Flyway flyway = Flyway.configure(FlywayMigrator.class.getClassLoader())
        .dataSource(dataSource)
        .locations(location)
        .table(tablePrefix + "_flyway_history")
        .baselineOnMigrate(true)
        .placeholders(Map.of("prefix", tablePrefix))
        .load();

    MigrateResult result = flyway.migrate();

    if (result.migrationsExecuted > 0) {
      Logger.info("[Storage] Flyway applied %d migration(s)", result.migrationsExecuted);
    } else {
      Logger.info("[Storage] Flyway schema is up to date");
    }

    // Log current schema version
    MigrationInfo current = flyway.info().current();
    if (current != null) {
      Logger.info("[Storage] Current schema version: %s (%s)", current.getVersion(), current.getDescription());
    }
  }
}
