package com.hyperfactions.command.admin.handler;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.config.modules.GravestoneConfig;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.integration.permissions.HyperPermsIntegration;
import com.hyperfactions.integration.placeholder.PlaceholderAPIIntegration;
import com.hyperfactions.integration.placeholder.WiFlowPlaceholderIntegration;
import com.hyperfactions.integration.protection.GravestoneIntegration;
import com.hyperfactions.integration.protection.OrbisGuardIntegration;
import com.hyperfactions.integration.protection.ProtectionMixinBridge;
import com.hyperfactions.update.UpdateChecker;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;

/**
 * Handles /f admin integrations and /f admin integration {@code <name>} commands.
 */
public class AdminIntegrationHandler {

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

  /** Creates a new AdminIntegrationHandler. */
  public AdminIntegrationHandler(HyperFactions hyperFactions) {
    this.hyperFactions = hyperFactions;
  }

  /** Handles integrations. */
  public void handleIntegrations(CommandContext ctx) {
    ctx.sendMessage(prefix().insert(msg("Integration Status", COLOR_CYAN)));

    // --- Permissions ---
    ctx.sendMessage(msg("  Permissions:", COLOR_YELLOW));

    boolean hpAvailable = HyperPermsIntegration.isAvailable();
    ctx.sendMessage(msg("    HyperPerms: " + (hpAvailable ? "Active" : "Not Found"),
        hpAvailable ? COLOR_GREEN : COLOR_GRAY));

    // Provider names are filtered by availability (triggers lazy init)
    String providerNames = PermissionManager.get().getProviderNames();

    boolean luckPermsAvailable = providerNames.contains("LuckPerms");
    ctx.sendMessage(msg("    LuckPerms: " + (luckPermsAvailable ? "Active" : "Not Found"),
        luckPermsAvailable ? COLOR_GREEN : COLOR_GRAY));

    boolean vaultAvailable = providerNames.contains("VaultUnlocked");
    boolean vaultInstalled = false;
    if (!vaultAvailable) {
      try {
        Class.forName("net.cfh.vault.VaultUnlocked");
        vaultInstalled = true;
      } catch (ClassNotFoundException ignored) {}
    }
    String vuStatus = vaultAvailable ? "Active" :
        (vaultInstalled ? "Installed (no perm provider)" : "Not Installed");
    ctx.sendMessage(msg("    VaultUnlocked: " + vuStatus,
        vaultAvailable ? COLOR_GREEN : (vaultInstalled ? COLOR_YELLOW : COLOR_GRAY)));

    boolean nativeAvailable = providerNames.contains("HytaleNative");
    ctx.sendMessage(msg("    HytaleNative: " + (nativeAvailable ? "Active" : "Not Found"),
        nativeAvailable ? COLOR_GREEN : COLOR_GRAY));

    // --- Protection ---
    ctx.sendMessage(msg("  Protection:", COLOR_YELLOW));

    ProtectionMixinBridge.MixinProvider provider = ProtectionMixinBridge.getProvider();
    boolean ogApiAvailable = OrbisGuardIntegration.isAvailable();

    switch (provider) {
      case HYPERPROTECT -> {
        String hpVersion = System.getProperty("hyperprotect.bridge.version", "unknown");
        ctx.sendMessage(msg("    HyperProtect-Mixin: Active (v" + hpVersion + ")", COLOR_GREEN));
        if (ogApiAvailable) {
          ctx.sendMessage(msg("    OrbisGuard API: Active (claim conflicts)", COLOR_GREEN));
        }
      }
      case ORBISGUARD -> {
        ctx.sendMessage(msg("    HyperProtect-Mixin: Not Detected", COLOR_GRAY));
        ctx.sendMessage(msg("    OrbisGuard-Mixins: Active", COLOR_GREEN));
        if (ogApiAvailable) {
          ctx.sendMessage(msg("    OrbisGuard API: Active (claim conflicts)", COLOR_GREEN));
        }
      }
      case NONE -> {
        ctx.sendMessage(msg("    HyperProtect-Mixin: Not Detected", COLOR_GRAY));
        ctx.sendMessage(msg("    OrbisGuard-Mixins: Not Detected", COLOR_GRAY));
        if (ogApiAvailable) {
          ctx.sendMessage(msg("    OrbisGuard API: Active (claim conflicts only)", COLOR_YELLOW));
        }
      }
      default -> throw new IllegalStateException("Unexpected value");
    }

    String mixinStatus = ProtectionMixinBridge.getStatusSummary();
    ctx.sendMessage(msg("    Mixin Hooks: " + mixinStatus,
        provider != ProtectionMixinBridge.MixinProvider.NONE ? COLOR_GREEN : COLOR_GRAY));

    GravestoneIntegration gs = hyperFactions.getProtectionChecker().getGravestoneIntegration();
    boolean gsAvailable = gs != null && gs.isAvailable();
    boolean gsEnabled = ConfigManager.get().gravestones().isEnabled();
    String gsStatus = !gsAvailable ? "Not Found" : (gsEnabled ? "Active" : "Disabled");
    String gsColor = gsAvailable && gsEnabled ? COLOR_GREEN : (gsAvailable ? COLOR_YELLOW : COLOR_GRAY);
    ctx.sendMessage(msg("    Gravestones: " + gsStatus, gsColor));

    // --- Placeholders ---
    ctx.sendMessage(msg("  Placeholders:", COLOR_YELLOW));

    boolean papiAvailable = PlaceholderAPIIntegration.isAvailable();
    ctx.sendMessage(msg("    PlaceholderAPI: " + (papiAvailable ? "Active" : "Not Found"),
        papiAvailable ? COLOR_GREEN : COLOR_GRAY));

    boolean wiflowAvailable;
    try {
      wiflowAvailable = WiFlowPlaceholderIntegration.isAvailable();
    } catch (NoClassDefFoundError e) {
      wiflowAvailable = false;
    }
    ctx.sendMessage(msg("    WiFlow PAPI: " + (wiflowAvailable ? "Active" : "Not Found"),
        wiflowAvailable ? COLOR_GREEN : COLOR_GRAY));

    ctx.sendMessage(msg("  Use /f admin integration <name> for details", COLOR_GRAY));
  }

  /** Handles integration detail. */
  public void handleIntegrationDetail(CommandContext ctx, String[] args) {
    if (args.length == 0) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin integration <name>", COLOR_RED)));
      ctx.sendMessage(msg("  Available: hyperperms, luckperms, vaultunlocked, native, hyperprotect, orbisguard, mixins, gravestones, papi, wiflow", COLOR_GRAY));
      return;
    }

    String name = args[0].toLowerCase();
    switch (name) {
      case "gravestones", "gravestone", "gs" -> handleIntegrationGravestones(ctx);
      case "hyperperms", "perms", "hp" -> handleIntegrationPermissions(ctx);
      case "luckperms", "lp" -> handleIntegrationLuckPerms(ctx);
      case "vaultunlocked", "vault", "vu" -> handleIntegrationVaultUnlocked(ctx);
      case "hytale", "native", "hytaleNative" -> handleIntegrationHytaleNative(ctx);
      case "orbisguard", "orbis", "og" -> handleIntegrationOrbisGuard(ctx);
      case "mixins", "orbismixins", "om", "hyperprotect", "hpmixin" -> handleIntegrationMixins(ctx);
      case "placeholderapi", "papi" -> handleIntegrationPAPI(ctx);
      case "wiflow", "wflow" -> handleIntegrationWiFlow(ctx);
      default -> ctx.sendMessage(prefix().insert(msg("Unknown integration: " + name
          + ". Available: hyperperms, luckperms, vaultunlocked, native, orbisguard, mixins, gravestones, papi, wiflow", COLOR_RED)));
    }
  }

  /** Handles integration gravestones. */
  public void handleIntegrationGravestones(CommandContext ctx) {
    GravestoneIntegration gs = hyperFactions.getProtectionChecker().getGravestoneIntegration();
    GravestoneConfig config = ConfigManager.get().gravestones();
    boolean pluginDetected = gs != null && gs.isAvailable();

    ctx.sendMessage(prefix().insert(msg("Gravestone Integration", COLOR_CYAN)));
    ctx.sendMessage(msg("  Plugin: " + (pluginDetected ? "Detected (v2 API)" : "Not Found"),
        pluginDetected ? COLOR_GREEN : COLOR_GRAY));
    ctx.sendMessage(msg("  Integration: " + (config.isEnabled() ? "Enabled" : "Disabled"),
        config.isEnabled() ? COLOR_GREEN : COLOR_RED));

    if (pluginDetected) {
      ctx.sendMessage(msg("  AccessChecker: Registered", COLOR_GREEN));
      ctx.sendMessage(msg("  Event Listeners: Active", COLOR_GREEN));
    }

    ctx.sendMessage(msg("  Territory Config:", COLOR_CYAN));
    ctx.sendMessage(msg("    protectInOwnTerritory: " + config.isProtectInOwnTerritory(), COLOR_GRAY));
    ctx.sendMessage(msg("    factionMembersCanAccess: " + config.isFactionMembersCanAccess(), COLOR_GRAY));
    ctx.sendMessage(msg("    alliesCanAccess: " + config.isAlliesCanAccess(), COLOR_GRAY));
    ctx.sendMessage(msg("    protectInEnemyTerritory: " + config.isProtectInEnemyTerritory(), COLOR_GRAY));
    ctx.sendMessage(msg("    protectInNeutralTerritory: " + config.isProtectInNeutralTerritory(), COLOR_GRAY));
    ctx.sendMessage(msg("    enemiesCanLootInOwnTerritory: " + config.isEnemiesCanLootInOwnTerritory(), COLOR_GRAY));
    ctx.sendMessage(msg("  Zone/Wilderness Config:", COLOR_CYAN));
    ctx.sendMessage(msg("    protectInSafeZone: " + config.isProtectInSafeZone(), COLOR_GRAY));
    ctx.sendMessage(msg("    protectInWarZone: " + config.isProtectInWarZone(), COLOR_GRAY));
    ctx.sendMessage(msg("    protectInWilderness: " + config.isProtectInWilderness(), COLOR_GRAY));
    ctx.sendMessage(msg("  Features:", COLOR_CYAN));
    ctx.sendMessage(msg("    announceDeathLocation: " + config.isAnnounceDeathLocation(), COLOR_GRAY));
    ctx.sendMessage(msg("  Raid/War (placeholder):", COLOR_CYAN));
    ctx.sendMessage(msg("    allowLootDuringRaid: " + config.isAllowLootDuringRaid(), COLOR_GRAY));
    ctx.sendMessage(msg("    allowLootDuringWar: " + config.isAllowLootDuringWar(), COLOR_GRAY));
  }

  /** Handles integration permissions. */
  public void handleIntegrationPermissions(CommandContext ctx) {
    ctx.sendMessage(prefix().insert(msg("Permission Integration", COLOR_CYAN)));

    // HyperPerms direct integration
    boolean hpAvailable = HyperPermsIntegration.isAvailable();
    ctx.sendMessage(msg("  HyperPerms: " + (hpAvailable ? "Available" : "Not Found"),
        hpAvailable ? COLOR_GREEN : COLOR_GRAY));
    if (hpAvailable) {
      ctx.sendMessage(msg("    Status: " + HyperPermsIntegration.getDetailedStatus(), COLOR_GRAY));
    } else {
      String error = HyperPermsIntegration.getInitError();
      if (error != null) {
        ctx.sendMessage(msg("    Error: " + error, COLOR_RED));
      }
    }

    // Provider chain
    PermissionManager pm = PermissionManager.get();
    ctx.sendMessage(msg("  Provider Chain:", COLOR_CYAN));
    ctx.sendMessage(msg("    Priority: VaultUnlocked > HyperPerms > LuckPerms > HytaleNative", COLOR_GRAY));
    ctx.sendMessage(msg("    Active Providers: " + pm.getProviderCount() + " (" + pm.getProviderNames() + ")", COLOR_WHITE));

    // Fallback config
    boolean allowWithout = ConfigManager.get().isAllowWithoutPermissionMod();
    ctx.sendMessage(msg("  Fallback:", COLOR_CYAN));
    ctx.sendMessage(msg("    allowWithoutPermissionMod: " + allowWithout,
        allowWithout ? COLOR_GREEN : COLOR_GRAY));
  }

  /** Handles integration luck perms. */
  public void handleIntegrationLuckPerms(CommandContext ctx) {
    String providerNames = PermissionManager.get().getProviderNames();
    boolean available = providerNames.contains("LuckPerms");

    ctx.sendMessage(prefix().insert(msg("LuckPerms Integration", COLOR_CYAN)));
    ctx.sendMessage(msg("  Provider Status: " + (available ? "Active" : "Not Found"),
        available ? COLOR_GREEN : COLOR_GRAY));

    if (available) {
      ctx.sendMessage(msg("  Features:", COLOR_CYAN));
      ctx.sendMessage(msg("    Permission Checks: Active (direct API)", COLOR_GREEN));
      ctx.sendMessage(msg("    Prefix/Suffix: Active (via CachedMetaData)", COLOR_GREEN));
      ctx.sendMessage(msg("    Primary Group: Active", COLOR_GREEN));
      ctx.sendMessage(msg("    Permission Registration: Active (PermissionRegistry)", COLOR_GREEN));
    } else {
      // Check if LuckPerms is installed but provider failed
      boolean classPresent = false;
      try {
        Class.forName("net.luckperms.api.LuckPermsProvider");
        classPresent = true;
      } catch (ClassNotFoundException ignored) {}

      if (classPresent) {
        boolean nativeAvailable = providerNames.contains("HytaleNative");
        ctx.sendMessage(msg("  LuckPerms is installed but direct API not available", COLOR_YELLOW));
        if (nativeAvailable) {
          ctx.sendMessage(msg("  Permissions routed via HytaleNative provider", COLOR_GREEN));
          ctx.sendMessage(msg("  Note: Prefix/suffix not available through HytaleNative", COLOR_YELLOW));
        }
      } else {
        ctx.sendMessage(msg("  LuckPerms is not installed", COLOR_GRAY));
      }
    }

    ctx.sendMessage(msg("  SoftDependency: manifest.json declares LuckPerms", COLOR_GRAY));
    ctx.sendMessage(msg("  Lazy Init: Retries if LuckPerms loads after HyperFactions", COLOR_GRAY));
  }

  /** Handles integration vault unlocked. */
  public void handleIntegrationVaultUnlocked(CommandContext ctx) {
    String providerNames = PermissionManager.get().getProviderNames();
    boolean available = providerNames.contains("VaultUnlocked");

    ctx.sendMessage(prefix().insert(msg("VaultUnlocked Integration", COLOR_CYAN)));
    ctx.sendMessage(msg("  Provider Status: " + (available ? "Active" : "Not Found"),
        available ? COLOR_GREEN : COLOR_GRAY));

    if (available) {
      ctx.sendMessage(msg("  Features:", COLOR_CYAN));
      ctx.sendMessage(msg("    Permission Checks: Active (PermissionUnlocked.has)", COLOR_GREEN));
      ctx.sendMessage(msg("    Prefix/Suffix: Active (ChatUnlocked)", COLOR_GREEN));
      ctx.sendMessage(msg("    Primary Group: Active", COLOR_GREEN));
      ctx.sendMessage(msg("  API:", COLOR_CYAN));
      ctx.sendMessage(msg("    Main Class: net.cfh.vault.VaultUnlocked", COLOR_GRAY));
      ctx.sendMessage(msg("    Vault2 API: net.milkbowl.vault2.*", COLOR_GRAY));
    } else {
      boolean classPresent = false;
      try {
        Class.forName("net.cfh.vault.VaultUnlocked");
        classPresent = true;
      } catch (ClassNotFoundException ignored) {}

      if (classPresent) {
        ctx.sendMessage(msg("  VaultUnlocked installed but no permission provider registered", COLOR_YELLOW));
        ctx.sendMessage(msg("  LuckPerms does not register with VaultUnlocked on Hytale", COLOR_GRAY));
        ctx.sendMessage(msg("  VaultUnlocked is used for economy (Ecotale) only", COLOR_GRAY));
      } else {
        ctx.sendMessage(msg("  VaultUnlocked is not installed", COLOR_GRAY));
      }
    }

    ctx.sendMessage(msg("  SoftDependency: manifest.json declares VaultUnlocked", COLOR_GRAY));
    ctx.sendMessage(msg("  Lazy Init: Retries if VaultUnlocked loads after HyperFactions", COLOR_GRAY));
  }

  /** Handles integration hytale native. */
  public void handleIntegrationHytaleNative(CommandContext ctx) {
    String providerNames = PermissionManager.get().getProviderNames();
    boolean available = providerNames.contains("HytaleNative");

    ctx.sendMessage(prefix().insert(msg("HytaleNative Integration", COLOR_CYAN)));
    ctx.sendMessage(msg("  Provider Status: " + (available ? "Active" : "Not Found"),
        available ? COLOR_GREEN : COLOR_GRAY));

    ctx.sendMessage(msg("  Description:", COLOR_CYAN));
    ctx.sendMessage(msg("    Fallback provider that delegates to Hytale's PermissionsModule", COLOR_GRAY));
    ctx.sendMessage(msg("    Catches any plugin using PermissionsModule.addProvider()", COLOR_GRAY));

    if (available) {
      ctx.sendMessage(msg("  Features:", COLOR_CYAN));
      ctx.sendMessage(msg("    Permission Checks: Active (via PermissionsModule)", COLOR_GREEN));
      ctx.sendMessage(msg("    Prefix/Suffix: Not supported (PermissionsModule limitation)", COLOR_YELLOW));
      ctx.sendMessage(msg("    Primary Group: Not supported", COLOR_YELLOW));
      ctx.sendMessage(msg("  Note: Lowest priority — only used when direct providers can't answer", COLOR_GRAY));
    }
  }

  /** Handles integration orbis guard. */
  public void handleIntegrationOrbisGuard(CommandContext ctx) {
    boolean available = OrbisGuardIntegration.isAvailable();

    ctx.sendMessage(prefix().insert(msg("OrbisGuard API Integration", COLOR_CYAN)));
    ctx.sendMessage(msg("  API Status: " + (available ? "Detected" : "Not Detected"),
        available ? COLOR_GREEN : COLOR_GRAY));

    if (available) {
      ctx.sendMessage(msg("  Claim Conflict Detection: Active", COLOR_GREEN));
      ctx.sendMessage(msg("    Prevents faction claims in OrbisGuard-protected regions", COLOR_GRAY));
    } else {
      ctx.sendMessage(msg("  Claim Conflict Detection: Disabled", COLOR_GRAY));
      ctx.sendMessage(msg("    OrbisGuard not installed - no region conflict checks", COLOR_GRAY));
    }
  }

  /** Handles integration mixins. */
  public void handleIntegrationMixins(CommandContext ctx) {
    ProtectionMixinBridge.MixinProvider provider = ProtectionMixinBridge.getProvider();
    boolean available = provider != ProtectionMixinBridge.MixinProvider.NONE;

    ctx.sendMessage(prefix().insert(msg("Mixin Protection Integration", COLOR_CYAN)));
    ctx.sendMessage(msg("  Provider: " + provider, available ? COLOR_GREEN : COLOR_GRAY));
    ctx.sendMessage(msg("  Priority: HYPERPROTECT > ORBISGUARD > NONE", COLOR_GRAY));
    ctx.sendMessage(msg("  Status: " + ProtectionMixinBridge.getStatusSummary(),
        available ? COLOR_GREEN : COLOR_GRAY));

    if (provider == ProtectionMixinBridge.MixinProvider.HYPERPROTECT) {
      String hpVersion = System.getProperty("hyperprotect.bridge.version", "unknown");
      ctx.sendMessage(msg("  HyperProtect-Mixin:", COLOR_CYAN));
      ctx.sendMessage(msg("    Version: " + hpVersion, COLOR_WHITE));

      // Show update status if update checker exists
      UpdateChecker hpChecker = hyperFactions.getHyperProtectUpdateChecker();
      if (hpChecker != null && hpChecker.hasUpdateAvailable()) {
        var cached = hpChecker.getCachedUpdate();
        if (cached != null) {
          ctx.sendMessage(msg("    Update: v" + cached.version() + " available!", COLOR_YELLOW));
          ctx.sendMessage(msg("    Run: /f admin update mixin", COLOR_GRAY));
        }
      } else if (hpChecker != null) {
        ctx.sendMessage(msg("    Update: Up-to-date", COLOR_GREEN));
      }
    } else if (provider == ProtectionMixinBridge.MixinProvider.ORBISGUARD) {
      ctx.sendMessage(msg("  OrbisGuard-Mixins:", COLOR_CYAN));
      ctx.sendMessage(msg("    Status: Active (limited feature set)", COLOR_YELLOW));
      ctx.sendMessage(msg("    Recommendation: Install HyperProtect-Mixin for full coverage", COLOR_GRAY));
      ctx.sendMessage(msg("    Run: /f admin update mixin", COLOR_GRAY));
    }

    if (available) {
      ctx.sendMessage(msg("  Feature Availability:", COLOR_CYAN));
      String[] features = {
        "block_break", "block_place", "explosion", "fire_spread",
        "builder_tools", "hammer", "item_pickup", "death_drop",
        "durability", "container_access", "container_open",
        "mob_spawn", "teleporter", "portal", "seat",
        "entity_damage", "command", "respawn"
      };
      int active = 0;
      int total = features.length;
      for (String feature : features) {
        boolean featureAvailable = ProtectionMixinBridge.isMixinFeatureAvailable(feature);
        if (featureAvailable) {
          active++;
        }
        ctx.sendMessage(msg("    " + feature + ": " + (featureAvailable ? "Active" : "N/A"),
            featureAvailable ? COLOR_GREEN : COLOR_GRAY));
      }
      ctx.sendMessage(msg("  Coverage: " + active + "/" + total + " features",
          active == total ? COLOR_GREEN : COLOR_YELLOW));
    } else {
      ctx.sendMessage(msg("  No mixin system detected.", COLOR_GRAY));
      ctx.sendMessage(msg("  Install HyperProtect-Mixin or OrbisGuard-Mixins for:", COLOR_GRAY));
      ctx.sendMessage(msg("    item pickup, death drops, durability, mob spawning,", COLOR_GRAY));
      ctx.sendMessage(msg("    explosions, fire spread, hammers, builder tools,", COLOR_GRAY));
      ctx.sendMessage(msg("    teleporters, portals, seating, and more", COLOR_GRAY));
      ctx.sendMessage(msg("  Auto-download: /f admin update mixin", COLOR_GRAY));
    }
  }

  /** Handles integration p a p i. */
  public void handleIntegrationPAPI(CommandContext ctx) {
    boolean available = PlaceholderAPIIntegration.isAvailable();

    ctx.sendMessage(prefix().insert(msg("PlaceholderAPI Integration", COLOR_CYAN)));
    ctx.sendMessage(msg("  Status: " + (available ? "Active" : "Not Found"),
        available ? COLOR_GREEN : COLOR_GRAY));

    if (available) {
      ctx.sendMessage(msg("  Expansion: Registered", COLOR_GREEN));
      ctx.sendMessage(msg("  Prefix: hyperfactions", COLOR_GRAY));
      ctx.sendMessage(msg("  Placeholders: 73+", COLOR_GRAY));
      ctx.sendMessage(msg("  Categories: faction info, player power, relations, claims, roles", COLOR_GRAY));
    }
  }

  /** Handles integration wi flow. */
  public void handleIntegrationWiFlow(CommandContext ctx) {
    boolean available = WiFlowPlaceholderIntegration.isAvailable();

    ctx.sendMessage(prefix().insert(msg("WiFlow Placeholder Integration", COLOR_CYAN)));
    ctx.sendMessage(msg("  Status: " + (available ? "Active" : "Not Found"),
        available ? COLOR_GREEN : COLOR_GRAY));

    if (available) {
      ctx.sendMessage(msg("  Expansion: Registered", COLOR_GREEN));
      ctx.sendMessage(msg("  API: WiFlowPlaceholderAPI", COLOR_GRAY));
    }
  }
}
