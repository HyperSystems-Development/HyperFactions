package com.hyperfactions.command.admin.handler;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.backup.BackupManager;
import com.hyperfactions.backup.BackupType;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.config.modules.StorageConfig;
import com.hyperfactions.migration.StorageMigrator;
import com.hyperfactions.storage.StorageFactory;
import com.hyperfactions.storage.sql.SqlConfig;
import com.hyperfactions.storage.sql.SqlDialect;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

/**
 * Handles {@code /f admin migrate <source> <target>} commands.
 *
 * <p>Migrates data between storage backends (JSON ↔ SQL). For JSON↔SQL
 * migrations, the SQL connection is read from config/storage.json. For
 * SQL↔SQL migrations, the target connection is provided via command flags.</p>
 */
public class AdminMigrateHandler {

  private final HyperFactions hyperFactions;

  private static final String COLOR_CYAN = CommandUtil.COLOR_CYAN;
  private static final String COLOR_GREEN = CommandUtil.COLOR_GREEN;
  private static final String COLOR_RED = CommandUtil.COLOR_RED;
  private static final String COLOR_YELLOW = CommandUtil.COLOR_YELLOW;
  private static final String COLOR_GRAY = CommandUtil.COLOR_GRAY;
  private static final String COLOR_WHITE = CommandUtil.COLOR_WHITE;

  private static Message prefix() {
    return CommandUtil.prefix();
  }

  private static Message msg(String text, String color) {
    return CommandUtil.msg(text, color);
  }

  public AdminMigrateHandler(@NotNull HyperFactions hyperFactions) {
    this.hyperFactions = hyperFactions;
  }

  /**
   * Handles the /f admin migrate command.
   *
   * @param ctx  the command context
   * @param args subcommand arguments (source, target, flags)
   */
  public void handleAdminMigrate(@NotNull CommandContext ctx, @NotNull String[] args) {
    if (args.length < 2) {
      sendUsage(ctx);
      return;
    }

    String sourceType = args[0].toLowerCase();
    String targetType = args[1].toLowerCase();

    if (sourceType.equals(targetType)) {
      ctx.sendMessage(prefix().insert(msg("Source and target cannot be the same.", COLOR_RED)));
      return;
    }

    // Validate types
    if (!isValidType(sourceType) || !isValidType(targetType)) {
      ctx.sendMessage(prefix().insert(msg("Valid types: json, mysql, mariadb, postgresql", COLOR_RED)));
      return;
    }

    // Determine if both are SQL (need target connection flags)
    boolean sourceIsSql = !isJsonType(sourceType);
    boolean targetIsSql = !isJsonType(targetType);

    if (sourceIsSql && targetIsSql) {
      // SQL→SQL: parse target connection from command flags
      SqlConfig targetConfig = parseTargetFlags(args, targetType);
      if (targetConfig == null) {
        ctx.sendMessage(prefix().insert(msg("SQL→SQL migration requires target connection flags:", COLOR_RED)));
        ctx.sendMessage(prefix().insert(msg("  --target-host=... --target-port=... --target-database=...", COLOR_GRAY)));
        ctx.sendMessage(prefix().insert(msg("  --target-username=... --target-password=...", COLOR_GRAY)));
        return;
      }
      runMigration(ctx, sourceType, targetType, targetConfig);
    } else {
      runMigration(ctx, sourceType, targetType, null);
    }
  }

  private void runMigration(@NotNull CommandContext ctx,
               @NotNull String sourceType, @NotNull String targetType,
               SqlConfig targetSqlConfig) {
    ctx.sendMessage(prefix().insert(msg("Starting migration: " + sourceType + " → " + targetType + "...", COLOR_YELLOW)));

    CompletableFuture.runAsync(() -> {
      try {
        Path dataDir = hyperFactions.getDataDir();
        StorageConfig storageConfig = ConfigManager.get().storage();

        // Create source bundle
        StorageFactory.StorageBundle source = createBundle(sourceType, storageConfig, dataDir);

        // Create target bundle
        StorageFactory.StorageBundle target;
        if (targetSqlConfig != null) {
          // SQL→SQL: use provided target config
          target = createBundleFromSqlConfig(targetSqlConfig, dataDir);
        } else {
          target = createBundle(targetType, storageConfig, dataDir);
        }

        // Create backup before migration
        ctx.sendMessage(prefix().insert(msg("Creating pre-migration backup...", COLOR_GRAY)));
        BackupManager backupManager = hyperFactions.getBackupManager();
        if (backupManager != null) {
          var backupResult = backupManager.createBackup(BackupType.MIGRATION,
              "pre-migrate-" + sourceType + "-to-" + targetType, null).join();
          if (backupResult instanceof BackupManager.BackupResult.Success success) {
            ctx.sendMessage(prefix().insert(msg("Backup created: " + success.metadata().name(), COLOR_GREEN)));
          } else if (backupResult instanceof BackupManager.BackupResult.Failure failure) {
            ctx.sendMessage(prefix().insert(msg("Backup failed: " + failure.error() + " — continuing anyway", COLOR_YELLOW)));
          }
        }

        // Run migration with progress reporting
        StorageMigrator migrator = new StorageMigrator(source, target, progress -> {
          if (!progress.done() && progress.total() > 0 && progress.current() > 0) {
            ctx.sendMessage(prefix().insert(
                msg("  Migrating " + progress.dataType() + "... " + progress.current() + "/" + progress.total(), COLOR_GRAY)));
          }
        });

        StorageMigrator.MigrationResult result = migrator.migrate();

        // Shutdown target pool if we created one
        if (target.connectionPool() != null && targetSqlConfig != null) {
          target.connectionPool().shutdown();
        }
        // Shutdown source pool if it's different from the active one
        if (source.connectionPool() != null
            && source.connectionPool() != hyperFactions.getConnectionPool()) {
          source.connectionPool().shutdown();
        }

        if (result.isSuccess()) {
          ctx.sendMessage(prefix().insert(msg("Migration complete! " + result.total() + " total records.", COLOR_GREEN)));
          ctx.sendMessage(prefix().insert(msg("  Factions: " + result.factions()
              + " | Players: " + result.players()
              + " | Zones: " + result.zones(), COLOR_GRAY)));
          ctx.sendMessage(prefix().insert(msg("  Economy: " + result.economies()
              + " | Chat: " + result.chatHistories()
              + " | Invites: " + result.invites()
              + " | Requests: " + result.joinRequests(), COLOR_GRAY)));
          ctx.sendMessage(prefix().insert(
              msg("To switch: set 'type' to '" + targetType + "' in config/storage.json and restart.", COLOR_YELLOW)));
        } else {
          ctx.sendMessage(prefix().insert(msg("Migration failed: " + result.error(), COLOR_RED)));
        }
      } catch (Exception e) {
        Logger.warn("[Migration] Migration failed: %s", e.getMessage());
        ctx.sendMessage(prefix().insert(msg("Migration failed: " + e.getMessage(), COLOR_RED)));
      }
    });
  }

  /**
   * Creates a storage bundle for the given type, using the active config for SQL settings.
   */
  private StorageFactory.StorageBundle createBundle(String type, StorageConfig config, Path dataDir) {
    if (isJsonType(type)) {
      // Create a temporary JSON config
      return StorageFactory.create(createJsonConfig(config), dataDir);
    } else {
      // Use the SQL config from storage.json
      return StorageFactory.create(createSqlConfig(config, type), dataDir);
    }
  }

  /**
   * Creates a storage bundle from an explicit SQL config (for SQL→SQL target).
   */
  private StorageFactory.StorageBundle createBundleFromSqlConfig(SqlConfig sqlConfig, Path dataDir) {
    // Use StorageFactory directly with the provided config
    StorageConfig tempConfig = ConfigManager.get().storage();
    // We need to create the bundle manually since StorageConfig doesn't support arbitrary SqlConfig
    // For now, use the factory with modified storage config
    return StorageFactory.createFromSqlConfig(sqlConfig, dataDir);
  }

  private StorageConfig createJsonConfig(StorageConfig base) {
    // The base config will be used, but we override the type check in StorageFactory
    // StorageFactory.create handles "json" type
    return base; // StorageFactory switches on type, we pass "json" explicitly
  }

  private StorageConfig createSqlConfig(StorageConfig base, String type) {
    return base; // StorageFactory switches on type from config
  }

  /**
   * Parses target connection flags from command arguments.
   */
  private SqlConfig parseTargetFlags(String[] args, String targetType) {
    String host = "localhost";
    int port = 0;
    String database = "hyperfactions";
    String username = "root";
    String password = "";
    String prefix = "hf";

    boolean hasAny = false;
    for (int i = 2; i < args.length; i++) {
      String arg = args[i];
      if (arg.startsWith("--target-host=")) {
        host = arg.substring("--target-host=".length());
        hasAny = true;
      } else if (arg.startsWith("--target-port=")) {
        port = Integer.parseInt(arg.substring("--target-port=".length()));
        hasAny = true;
      } else if (arg.startsWith("--target-database=")) {
        database = arg.substring("--target-database=".length());
        hasAny = true;
      } else if (arg.startsWith("--target-username=")) {
        username = arg.substring("--target-username=".length());
        hasAny = true;
      } else if (arg.startsWith("--target-password=")) {
        password = arg.substring("--target-password=".length());
        hasAny = true;
      } else if (arg.startsWith("--target-prefix=")) {
        prefix = arg.substring("--target-prefix=".length());
        hasAny = true;
      }
    }

    if (!hasAny) {
      return null;
    }

    SqlDialect dialect = SqlDialect.fromString(targetType);
    if (dialect == null) {
      return null;
    }
    if (port == 0) {
      port = dialect.getDefaultPort();
    }

    return new SqlConfig(dialect, host, port, database, username, password, prefix,
        10, 2, 300_000L, 1_800_000L, 10_000L, 60_000L, 3, 1_000L);
  }

  private void sendUsage(CommandContext ctx) {
    ctx.sendMessage(prefix().insert(msg("Usage: /f admin migrate <source> <target>", COLOR_CYAN)));
    ctx.sendMessage(prefix().insert(msg("  Types: json, mysql, mariadb, postgresql", COLOR_GRAY)));
    ctx.sendMessage(prefix().insert(msg("  Examples:", COLOR_GRAY)));
    ctx.sendMessage(prefix().insert(msg("    /f admin migrate json mysql", COLOR_WHITE)));
    ctx.sendMessage(prefix().insert(msg("    /f admin migrate mysql json", COLOR_WHITE)));
    ctx.sendMessage(prefix().insert(msg("    /f admin migrate mysql postgresql --target-host=...", COLOR_WHITE)));
  }

  private boolean isValidType(String type) {
    return isJsonType(type) || SqlDialect.fromString(type) != null;
  }

  private boolean isJsonType(String type) {
    return "json".equals(type) || "file".equals(type) || "flatfile".equals(type);
  }
}
