package com.hyperfactions.storage.sql;

import com.hyperfactions.util.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLClassLoader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Driver;
import java.sql.DriverManager;
import org.jetbrains.annotations.NotNull;

/**
 * Downloads JDBC driver JARs at runtime from Maven Central.
 *
 * <p>JDBC drivers are not bundled in the plugin JAR to reduce size for
 * servers using JSON storage. When SQL storage is configured, the required
 * driver is downloaded to {@code libs/} on first use and loaded via a
 * custom classloader.</p>
 */
public final class DriverDownloader {

  private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";

  private DriverDownloader() {}

  /**
   * Ensures the JDBC driver for the given dialect is available.
   * Downloads from Maven Central if not already present in the libs directory.
   *
   * @param dialect the SQL dialect requiring a driver
   * @param libsDir the directory to store downloaded driver JARs
   * @throws IOException if download or classloading fails
   */
  public static void ensureDriver(@NotNull SqlDialect dialect, @NotNull Path libsDir) throws IOException {
    String coordinate = dialect.getMavenCoordinate(); // e.g. "com.mysql:mysql-connector-j:9.1.0"
    String[] parts = coordinate.split(":");
    String groupId = parts[0];
    String artifactId = parts[1];
    String version = parts[2];

    String fileName = artifactId + "-" + version + ".jar";
    Path driverJar = libsDir.resolve(fileName);

    // Check if already loaded
    try {
      Class.forName(dialect.getDriverClass());
      Logger.debug("[Storage] JDBC driver already loaded: %s", dialect.getDriverClass());
      return;
    } catch (ClassNotFoundException ignored) {
      // Need to download/load
    }

    // Download if not present
    if (!Files.exists(driverJar)) {
      Files.createDirectories(libsDir);

      String groupPath = groupId.replace('.', '/');
      String url = String.format("%s/%s/%s/%s/%s", MAVEN_CENTRAL, groupPath, artifactId, version, fileName);

      Logger.info("[Storage] Downloading JDBC driver: %s", fileName);
      Logger.info("[Storage] From: %s", url);

      Path tempFile = libsDir.resolve(fileName + ".tmp");
      try (InputStream in = URI.create(url).toURL().openStream()) {
        Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
      }
      Files.move(tempFile, driverJar, StandardCopyOption.REPLACE_EXISTING);

      Logger.info("[Storage] Downloaded JDBC driver: %s (%d KB)", fileName, Files.size(driverJar) / 1024);
    } else {
      Logger.info("[Storage] Using cached JDBC driver: %s", fileName);
    }

    // Load the JAR via URLClassLoader and register the driver
    loadDriverJar(driverJar, dialect.getDriverClass());
  }

  /**
   * Loads a JDBC driver JAR and registers its driver with the DriverManager.
   */
  private static void loadDriverJar(@NotNull Path jarPath, @NotNull String driverClassName) throws IOException {
    URL jarUrl = jarPath.toUri().toURL();
    URLClassLoader classLoader = new URLClassLoader(
        new URL[]{jarUrl},
        DriverDownloader.class.getClassLoader()
    );

    try {
      Class<?> driverClass = Class.forName(driverClassName, true, classLoader);
      Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
      // Wrap in a shim so DriverManager accepts it (DriverManager checks classloader)
      DriverManager.registerDriver(new DriverShim(driver));
      Logger.info("[Storage] Registered JDBC driver: %s", driverClassName);
    } catch (Exception e) {
      throw new IOException("Failed to load JDBC driver: " + driverClassName, e);
    }
  }

  /**
   * Shim wrapper that delegates to a real Driver instance.
   * Required because DriverManager rejects drivers loaded by a different classloader.
   */
  private static class DriverShim implements Driver {
    private final Driver delegate;

    DriverShim(Driver delegate) {
      this.delegate = delegate;
    }

    @Override
    public java.sql.Connection connect(String url, java.util.Properties info) throws java.sql.SQLException {
      return delegate.connect(url, info);
    }

    @Override
    public boolean acceptsURL(String url) throws java.sql.SQLException {
      return delegate.acceptsURL(url);
    }

    @Override
    public java.sql.DriverPropertyInfo[] getPropertyInfo(String url, java.util.Properties info) throws java.sql.SQLException {
      return delegate.getPropertyInfo(url, info);
    }

    @Override
    public int getMajorVersion() {
      return delegate.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
      return delegate.getMinorVersion();
    }

    @Override
    public boolean jdbcCompliant() {
      return delegate.jdbcCompliant();
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws java.sql.SQLFeatureNotSupportedException {
      return delegate.getParentLogger();
    }
  }
}
