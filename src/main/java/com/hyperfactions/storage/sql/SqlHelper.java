package com.hyperfactions.storage.sql;

import org.jetbrains.annotations.NotNull;

/**
 * Utility methods for building dialect-aware SQL statements.
 */
public final class SqlHelper {

  private SqlHelper() {}

  /**
   * Returns the table name with prefix applied.
   *
   * @param prefix    the table prefix (e.g., "hf")
   * @param tableName the base table name (e.g., "factions")
   * @return the prefixed table name (e.g., "hf_factions")
   */
  @NotNull
  public static String table(@NotNull String prefix, @NotNull String tableName) {
    return prefix + "_" + tableName;
  }

  /**
   * Builds a dialect-aware upsert statement.
   *
   * <p>MySQL: {@code INSERT INTO ... ON DUPLICATE KEY UPDATE ...}
   * <p>PostgreSQL: {@code INSERT INTO ... ON CONFLICT (keys) DO UPDATE SET ...}
   *
   * @param dialect     the SQL dialect
   * @param table       the full table name (with prefix)
   * @param columns     column names
   * @param params      JDBI named parameter placeholders (e.g., ":id", ":name")
   * @param conflictKey the conflict/primary key columns (for PostgreSQL ON CONFLICT)
   * @param updateCols  columns to update on conflict (subset of columns)
   * @return the upsert SQL string
   */
  @NotNull
  public static String upsert(@NotNull SqlDialect dialect,
                @NotNull String table,
                @NotNull String[] columns,
                @NotNull String[] params,
                @NotNull String[] conflictKey,
                @NotNull String[] updateCols) {
    StringBuilder sb = new StringBuilder();
    sb.append("INSERT INTO ").append(table).append(" (");
    sb.append(String.join(", ", columns));
    sb.append(") VALUES (");
    sb.append(String.join(", ", params));
    sb.append(")");

    if (dialect == SqlDialect.MYSQL) {
      sb.append(" ON DUPLICATE KEY UPDATE ");
      for (int i = 0; i < updateCols.length; i++) {
        if (i > 0) sb.append(", ");
        sb.append(updateCols[i]).append(" = VALUES(").append(updateCols[i]).append(")");
      }
    } else {
      sb.append(" ON CONFLICT (");
      sb.append(String.join(", ", conflictKey));
      sb.append(") DO UPDATE SET ");
      for (int i = 0; i < updateCols.length; i++) {
        if (i > 0) sb.append(", ");
        sb.append(updateCols[i]).append(" = EXCLUDED.").append(updateCols[i]);
      }
    }

    return sb.toString();
  }
}
