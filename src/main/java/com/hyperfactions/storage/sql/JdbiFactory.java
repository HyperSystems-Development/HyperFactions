package com.hyperfactions.storage.sql;

import com.zaxxer.hikari.HikariDataSource;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.StatementContext;
import org.jetbrains.annotations.NotNull;

/**
 * Creates and configures JDBI instances with custom type mappings.
 */
public final class JdbiFactory {

  private JdbiFactory() {}

  /**
   * Creates a configured JDBI instance for the given data source and config.
   *
   * @param dataSource the HikariCP data source
   * @param config     the SQL configuration (used for table prefix)
   * @return a configured JDBI instance
   */
  @NotNull
  public static Jdbi create(@NotNull HikariDataSource dataSource, @NotNull SqlConfig config) {
    Jdbi jdbi = Jdbi.create(dataSource);

    // Register UUID argument factory (binds UUID as VARCHAR)
    jdbi.registerArgument(new UuidArgumentFactory());

    // Register UUID column mapper (reads VARCHAR as UUID)
    jdbi.registerColumnMapper(UUID.class, (rs, col, ctx) -> {
      String value = rs.getString(col);
      return value != null ? UUID.fromString(value) : null;
    });

    // Register BigDecimal column mapper
    jdbi.registerColumnMapper(BigDecimal.class, (rs, col, ctx) -> rs.getBigDecimal(col));

    // Define the table prefix as a template attribute available to all statements
    jdbi.define("prefix", config.tablePrefix());

    return jdbi;
  }

  /**
   * JDBI argument factory that binds UUID values as their string representation.
   */
  static class UuidArgumentFactory extends AbstractArgumentFactory<UUID> {

    UuidArgumentFactory() {
      super(java.sql.Types.VARCHAR);
    }

    @Override
    protected Argument build(UUID value, ConfigRegistry config) {
      return (pos, stmt, ctx) -> stmt.setString(pos, value.toString());
    }
  }
}
