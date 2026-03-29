package com.hyperfactions.storage.sql;

import org.jetbrains.annotations.NotNull;

/**
 * Supported SQL database dialects.
 */
public enum SqlDialect {

  MYSQL("mysql", "com.mysql.cj.jdbc.Driver", 3306, "mysql-connector-j", "com.mysql:mysql-connector-j:9.1.0"),
  POSTGRESQL("postgresql", "org.postgresql.Driver", 5432, "postgresql", "org.postgresql:postgresql:42.7.5");

  private final String jdbcScheme;
  private final String driverClass;
  private final int defaultPort;
  private final String driverArtifact;
  private final String mavenCoordinate;

  SqlDialect(String jdbcScheme, String driverClass, int defaultPort, String driverArtifact, String mavenCoordinate) {
    this.jdbcScheme = jdbcScheme;
    this.driverClass = driverClass;
    this.defaultPort = defaultPort;
    this.driverArtifact = driverArtifact;
    this.mavenCoordinate = mavenCoordinate;
  }

  /**
   * Builds a JDBC URL for this dialect.
   *
   * @param host     database host
   * @param port     database port
   * @param database database name
   * @return JDBC URL string
   */
  @NotNull
  public String buildJdbcUrl(@NotNull String host, int port, @NotNull String database) {
    return String.format("jdbc:%s://%s:%d/%s", jdbcScheme, host, port, database);
  }

  /** Returns the fully qualified JDBC driver class name. */
  @NotNull
  public String getDriverClass() {
    return driverClass;
  }

  /** Returns the default port for this dialect. */
  public int getDefaultPort() {
    return defaultPort;
  }

  /** Returns the short artifact name for the JDBC driver. */
  @NotNull
  public String getDriverArtifact() {
    return driverArtifact;
  }

  /** Returns the Maven coordinate for the JDBC driver JAR. */
  @NotNull
  public String getMavenCoordinate() {
    return mavenCoordinate;
  }

  /**
   * Parses a dialect string (case-insensitive).
   *
   * @param name the dialect name (mysql, mariadb, postgresql, postgres)
   * @return the dialect, or null if unknown
   */
  public static SqlDialect fromString(String name) {
    if (name == null) {
      return null;
    }
    return switch (name.toLowerCase().trim()) {
      case "mysql", "mariadb" -> MYSQL;
      case "postgresql", "postgres" -> POSTGRESQL;
      default -> null;
    };
  }
}
