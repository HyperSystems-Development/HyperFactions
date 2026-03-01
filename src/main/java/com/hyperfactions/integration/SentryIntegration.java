package com.hyperfactions.integration;

import com.hyperfactions.BuildInfo;
import com.hyperfactions.config.modules.SentryConfig;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.common.util.java.ManifestUtil;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.HytaleServerConfig;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import org.jetbrains.annotations.NotNull;

/**
 * Manages Sentry error tracking integration.
 *
 * <p>All operations are wrapped in try/catch to guarantee that Sentry issues
 * never crash the server. The Sentry SDK sends events asynchronously by default
 * (via {@code AsyncHttpTransport}), so event submission is non-blocking.
 */
public final class SentryIntegration {

  private static boolean initialized = false;

  private SentryIntegration() {}

  /**
   * Initializes Sentry from the given config.
   * Does nothing if Sentry is disabled or the DSN is empty.
   * Safe to call multiple times — subsequent calls are ignored.
   *
   * @param config the sentry configuration
   */
  public static void init(@NotNull SentryConfig config) {
    if (initialized) {
      return;
    }

    if (!config.isEnabled()) {
      Logger.info("[Sentry] Sentry is disabled in config");
      return;
    }

    String dsn = config.getDsn();
    if (dsn == null || dsn.isBlank()) {
      Logger.info("[Sentry] No DSN configured — Sentry will not send events");
      return;
    }

    try {
      Sentry.init(options -> {
        options.setDsn(dsn);
        options.setRelease("hyperfactions@" + BuildInfo.VERSION);
        options.setEnvironment(config.getEnvironment());
        options.setDebug(config.isDebug());

        // Performance monitoring (0.0 = off by default)
        double traceRate = config.getTracesSampleRate();
        if (traceRate > 0.0) {
          options.setTracesSampleRate(traceRate);
        }

        // Highlight HyperFactions frames in stack traces
        options.addInAppInclude("com.hyperfactions");

        // Don't install UncaughtExceptionHandler — the Hytale server has its own
        options.setEnableUncaughtExceptionHandler(false);

        // Give 2 seconds to flush pending events on shutdown
        options.setShutdownTimeoutMillis(2000);

        // Limit breadcrumb memory usage
        options.setMaxBreadcrumbs(50);

        // Send PII (player context etc.) — server-side, no browser data
        options.setSendDefaultPii(true);
      });

      // Set global tags — attached to every event
      Sentry.configureScope(scope -> {
        scope.setTag("plugin.version", BuildInfo.VERSION);
        scope.setTag("java.version", System.getProperty("java.version", "unknown"));

        String serverVersion = null;
        try {
          serverVersion = ManifestUtil.getVersion();
        } catch (Exception ignored) {
          // ManifestUtil may not be available yet
        }
        scope.setTag("hytale.server.version", serverVersion != null ? serverVersion : "unknown");

        String osName = System.getProperty("os.name", "unknown");
        String osArch = System.getProperty("os.arch", "unknown");
        scope.setTag("os", osName + " " + osArch);

        // Pull Hytale server config info
        String serverName = "unknown";
        int maxPlayers = 0;
        String motd = "";
        try {
          HytaleServerConfig serverConfig = HytaleServer.get().getConfig();
          serverName = serverConfig.getServerName();
          maxPlayers = serverConfig.getMaxPlayers();
          motd = serverConfig.getMotd();
        } catch (Exception ignored) {
          // Server may not be fully initialized yet
        }

        scope.setTag("server.name", serverName);

        scope.setContexts("server", java.util.Map.of(
            "server_name", serverName,
            "max_players", maxPlayers,
            "motd", motd != null && !motd.isEmpty() ? motd : "(none)",
            "hytale_version", serverVersion != null ? serverVersion : "unknown",
            "plugin_version", BuildInfo.VERSION,
            "java_version", System.getProperty("java.version", "unknown"),
            "os", osName + " " + osArch,
            "available_processors", Runtime.getRuntime().availableProcessors(),
            "max_memory_mb", Runtime.getRuntime().maxMemory() / (1024 * 1024)
        ));
      });

      initialized = true;
      Logger.info("[Sentry] Initialized (env=%s, debug=%s)", config.getEnvironment(), config.isDebug());
    } catch (Exception e) {
      Logger.severe("Failed to initialize Sentry: %s", e.getMessage());
    }
  }

  /**
   * Captures an exception and sends it to Sentry.
   * Also logs the exception to the server console.
   *
   * @param message context message describing what was happening
   * @param throwable the exception to capture
   */
  public static void captureException(@NotNull String message, @NotNull Throwable throwable) {
    // Always log to console first
    Logger.severe("%s: %s", message, throwable.getMessage());

    if (!initialized) {
      return;
    }

    try {
      Sentry.captureException(throwable, scope -> {
        scope.setExtra("context", message);
      });
    } catch (Exception e) {
      // Never let Sentry failures propagate
      Logger.debug("Sentry captureException failed: %s", e.getMessage());
    }
  }

  /**
   * Captures an exception and sends it to Sentry (without additional context message).
   *
   * @param throwable the exception to capture
   */
  public static void captureException(@NotNull Throwable throwable) {
    if (!initialized) {
      return;
    }

    try {
      Sentry.captureException(throwable);
    } catch (Exception e) {
      Logger.debug("Sentry captureException failed: %s", e.getMessage());
    }
  }

  /**
   * Captures a plain message at the given severity level.
   *
   * @param message the message text
   * @param level Sentry severity level
   */
  public static void captureMessage(@NotNull String message, @NotNull SentryLevel level) {
    if (!initialized) {
      return;
    }

    try {
      Sentry.captureMessage(message, level);
    } catch (Exception e) {
      Logger.debug("Sentry captureMessage failed: %s", e.getMessage());
    }
  }

  /**
   * Sends a test exception to Sentry to verify the integration is working.
   *
   * @return true if the event was sent (or at least queued), false if Sentry is not initialized
   */
  public static boolean sendTestEvent() {
    if (!initialized) {
      return false;
    }

    try {
      Exception testException = new RuntimeException("HyperFactions Sentry test event — this is not a real error");
      Sentry.captureException(testException);
      return true;
    } catch (Exception e) {
      Logger.debug("Sentry test event failed: %s", e.getMessage());
      return false;
    }
  }

  /**
   * Flushes pending events and closes the Sentry SDK.
   * Safe to call even if Sentry was never initialized.
   */
  public static void close() {
    if (!initialized) {
      return;
    }

    try {
      Sentry.close();
      initialized = false;
      Logger.info("[Sentry] Closed");
    } catch (Exception e) {
      Logger.debug("Sentry close failed: %s", e.getMessage());
      initialized = false;
    }
  }

  /**
   * Checks whether Sentry has been initialized and is active.
   *
   * @return true if Sentry is sending events
   */
  public static boolean isInitialized() {
    return initialized;
  }
}
