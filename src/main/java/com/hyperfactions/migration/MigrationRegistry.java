package com.hyperfactions.migration;

import com.hyperfactions.migration.migrations.config.ConfigV1ToV2Migration;
import com.hyperfactions.migration.migrations.config.ConfigV2ToV3Migration;
import com.hyperfactions.migration.migrations.config.ConfigV3ToV4Migration;
import com.hyperfactions.migration.migrations.config.ConfigV4ToV5Migration;
import com.hyperfactions.migration.migrations.config.ConfigV5ToV6Migration;
import com.hyperfactions.migration.migrations.config.ConfigV6ToV7Migration;
import com.hyperfactions.migration.migrations.data.DataV0ToV1Migration;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

/**
 * Registry for all available migrations.
 *
 * <p>
 * Migrations are registered by type and can be queried for applicable
 * migrations based on current data state.
 */
public class MigrationRegistry {

  private static final MigrationRegistry INSTANCE = new MigrationRegistry();

  private final Map<MigrationType, List<Migration>> migrations = new ConcurrentHashMap<>();

  private MigrationRegistry() {
    // Register all migrations
    registerBuiltInMigrations();
  }

  /**
   * Gets the singleton registry instance.
   *
   * @return registry instance
   */
  @NotNull
  public static MigrationRegistry get() {
    return INSTANCE;
  }

  /**
   * Registers built-in migrations.
   */
  private void registerBuiltInMigrations() {
    // Config migrations
    register(new ConfigV1ToV2Migration());
    register(new ConfigV2ToV3Migration());
    register(new ConfigV3ToV4Migration());
    register(new ConfigV4ToV5Migration());
    register(new ConfigV5ToV6Migration());
    register(new ConfigV6ToV7Migration());

    // Data migrations
    register(new DataV0ToV1Migration());
  }

  /**
   * Registers a migration.
   *
   * @param migration the migration to register
   */
  public void register(@NotNull Migration migration) {
    migrations.computeIfAbsent(migration.type(), k -> new ArrayList<>()).add(migration);
  }

  /**
   * Gets all migrations of a specific type.
   *
   * @param type migration type
   * @return list of migrations (sorted by fromVersion)
   */
  @NotNull
  public List<Migration> getMigrations(@NotNull MigrationType type) {
    List<Migration> typeMigrations = migrations.get(type);
    if (typeMigrations == null) {
      return List.of();
    }
    return typeMigrations.stream()
        .sorted(Comparator.comparingInt(Migration::fromVersion))
        .toList();
  }

  /**
   * Gets all applicable migrations of a specific type for the given data directory.
   *
   * <p>
   * Returns migrations in execution order (sorted by fromVersion).
   *
   * @param type    migration type
   * @param dataDir the plugin data directory
   * @return list of applicable migrations
   */
  @NotNull
  public List<Migration> getApplicableMigrations(@NotNull MigrationType type, @NotNull Path dataDir) {
    return getMigrations(type).stream()
        .filter(m -> m.isApplicable(dataDir))
        .toList();
  }

  /**
   * Builds a migration chain from the current version to the latest version.
   *
   * <p>
   * This chains together migrations that can be applied sequentially
   * (e.g., v1 -> v2 -> v3).
   *
   * @param type    migration type
   * @param dataDir the plugin data directory
   * @return ordered list of migrations to apply
   */
  @NotNull
  public List<Migration> buildMigrationChain(@NotNull MigrationType type, @NotNull Path dataDir) {
    List<Migration> applicable = getApplicableMigrations(type, dataDir);
    if (applicable.isEmpty()) {
      return List.of();
    }

    // For now, just return applicable migrations in order
    // In the future, this could build a proper chain checking version continuity
    return applicable;
  }

  /**
   * Checks if any migrations are pending for a type.
   *
   * @param type    migration type
   * @param dataDir the plugin data directory
   * @return true if migrations need to be run
   */
  public boolean hasPendingMigrations(@NotNull MigrationType type, @NotNull Path dataDir) {
    return !getApplicableMigrations(type, dataDir).isEmpty();
  }
}
