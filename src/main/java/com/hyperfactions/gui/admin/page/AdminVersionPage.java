package com.hyperfactions.gui.admin.page;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminVersionData;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.integration.permissions.HyperPermsIntegration;
import com.hyperfactions.integration.placeholder.PlaceholderAPIIntegration;
import com.hyperfactions.integration.placeholder.WiFlowPlaceholderIntegration;
import com.hyperfactions.integration.protection.GravestoneIntegration;
import com.hyperfactions.integration.protection.KyuubiSoftIntegration;
import com.hyperfactions.integration.protection.OrbisGuardIntegration;
import com.hyperfactions.integration.protection.ProtectionMixinBridge;
import com.hypixel.hytale.common.util.java.ManifestUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Admin Version page — displays mod version info and integration statuses.
 */
public class AdminVersionPage extends InteractiveCustomUIPage<AdminVersionData> {

  private static final String COLOR_GREEN = "#55FF55";

  private static final String COLOR_YELLOW = "#FFFF55";

  private static final String COLOR_GRAY = "#888888";

  private final PlayerRef playerRef;

  private final HyperFactions plugin;

  private final GuiManager guiManager;

  /** Creates a new AdminVersionPage. */
  public AdminVersionPage(PlayerRef playerRef, HyperFactions plugin, GuiManager guiManager) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminVersionData.CODEC);
    this.playerRef = playerRef;
    this.plugin = plugin;
    this.guiManager = guiManager;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {
    cmd.append(UIPaths.ADMIN_VERSION);

    // Setup admin nav bar
    AdminNavBarHelper.setupBar(playerRef, "version", cmd, events);

    // --- Version Info ---
    cmd.set("#FactionsVersion.Text", "v" + HyperFactions.VERSION);

    String serverVersion = ManifestUtil.getVersion();
    cmd.set("#ServerVersion.Text", serverVersion != null ? serverVersion : "Unknown");

    cmd.set("#JavaVersion.Text", System.getProperty("java.version", "Unknown"));

    // --- Permissions ---
    setStatus(cmd, "#HyperPermsStatus", HyperPermsIntegration.isAvailable(), "Active", "Not Found");

    String providerNames = PermissionManager.get().getProviderNames();

    setStatus(cmd, "#LuckPermsStatus", providerNames.contains("LuckPerms"), "Active", "Not Found");

    boolean vaultAvailable = providerNames.contains("VaultUnlocked");
    boolean vaultInstalled = false;
    if (!vaultAvailable) {
      try {
        Class.forName("net.cfh.vault.VaultUnlocked");
        vaultInstalled = true;
      } catch (ClassNotFoundException ignored) {}
    }
    if (vaultAvailable) {
      setStatusColor(cmd, "#VaultUnlockedStatus", "Active", COLOR_GREEN);
    } else if (vaultInstalled) {
      setStatusColor(cmd, "#VaultUnlockedStatus", "Installed (no perm provider)", COLOR_YELLOW);
    } else {
      setStatusColor(cmd, "#VaultUnlockedStatus", "Not Installed", COLOR_GRAY);
    }

    setStatus(cmd, "#NativeStatus", providerNames.contains("HytaleNative"), "Active", "Not Found");

    // --- Protection ---
    ProtectionMixinBridge.MixinProvider provider = ProtectionMixinBridge.getProvider();
    boolean ogApiAvailable = OrbisGuardIntegration.isAvailable();

    switch (provider) {
      case BOTH -> {
        String hpVersion = System.getProperty("hyperprotect.bridge.version", "unknown");
        setStatusColor(cmd, "#HyperProtectStatus", "Active (v" + hpVersion + ")", COLOR_GREEN);
        setStatusColor(cmd, "#OrbisGuardMixinsStatus", "Active (compatible)", COLOR_GREEN);
      }
      case HYPERPROTECT -> {
        String hpVersion = System.getProperty("hyperprotect.bridge.version", "unknown");
        setStatusColor(cmd, "#HyperProtectStatus", "Active (v" + hpVersion + ")", COLOR_GREEN);
        setStatusColor(cmd, "#OrbisGuardMixinsStatus", "N/A", COLOR_GRAY);
      }
      case ORBISGUARD -> {
        setStatusColor(cmd, "#HyperProtectStatus", "Not Detected", COLOR_GRAY);
        setStatusColor(cmd, "#OrbisGuardMixinsStatus", "Active", COLOR_GREEN);
      }
      case NONE -> {
        setStatusColor(cmd, "#HyperProtectStatus", "Not Detected", COLOR_GRAY);
        setStatusColor(cmd, "#OrbisGuardMixinsStatus", "Not Detected", COLOR_GRAY);
      }
      default -> throw new IllegalStateException("Unexpected value");
    }

    if (ogApiAvailable) {
      String ogLabel = provider == ProtectionMixinBridge.MixinProvider.NONE
          ? "Active (claims only)" : "Active";
      String ogColor = provider == ProtectionMixinBridge.MixinProvider.NONE
          ? COLOR_YELLOW : COLOR_GREEN;
      setStatusColor(cmd, "#OrbisGuardApiStatus", ogLabel, ogColor);
    } else {
      setStatusColor(cmd, "#OrbisGuardApiStatus", "Not Detected", COLOR_GRAY);
    }

    String mixinStatus = ProtectionMixinBridge.getStatusSummary();
    setStatusColor(cmd, "#MixinHooksStatus", mixinStatus,
        provider != ProtectionMixinBridge.MixinProvider.NONE ? COLOR_GREEN : COLOR_GRAY);

    GravestoneIntegration gs = plugin.getProtectionChecker().getGravestoneIntegration();
    boolean gsAvailable = gs != null && gs.isAvailable();
    boolean gsEnabled = ConfigManager.get().gravestones().isEnabled();
    String gsStatus = !gsAvailable ? "Not Found" : (gsEnabled ? "Active" : "Disabled");
    String gsColor = gsAvailable && gsEnabled ? COLOR_GREEN : (gsAvailable ? COLOR_YELLOW : COLOR_GRAY);
    setStatusColor(cmd, "#GravestonesStatus", gsStatus, gsColor);

    KyuubiSoftIntegration ks = plugin.getKyuubiSoftIntegration();
    boolean ksAvailable = ks != null && ks.isAvailable();
    setStatus(cmd, "#KyuubiSoftStatus", ksAvailable, "Active", "Not Found");

    // --- Placeholders ---
    setStatus(cmd, "#PlaceholderAPIStatus", PlaceholderAPIIntegration.isAvailable(), "Active", "Not Found");

    boolean wiflowAvailable;
    try {
      wiflowAvailable = WiFlowPlaceholderIntegration.isAvailable();
    } catch (NoClassDefFoundError e) {
      wiflowAvailable = false;
    }
    setStatus(cmd, "#WiFlowPAPIStatus", wiflowAvailable, "Active", "Not Found");

    // --- Economy ---
    if (plugin.isTreasuryEnabled()) {
      String econName = null;
      var econMgr = plugin.getEconomyManager();
      if (econMgr != null) {
        econName = econMgr.getVaultProvider().getEconomyName();
      }
      String treasuryLabel = econName != null ? "Active (" + econName + ")" : "Active";
      setStatusColor(cmd, "#TreasuryStatus", treasuryLabel, COLOR_GREEN);
    } else {
      setStatusColor(cmd, "#TreasuryStatus", "Not Found", COLOR_GRAY);
    }
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                AdminVersionData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null) {
      return;
    }

    // Handle admin nav bar navigation
    if (AdminNavBarHelper.handleNavEvent(data, player, ref, store, playerRef, guiManager)) {
      return;
    }

    if (data.button != null) {
      switch (data.button) {
        case "Back" -> guiManager.closePage(player, ref, store);
        default -> throw new IllegalStateException("Unexpected value");
      }
    }
  }

  // --- Helpers ---

  private void setStatus(UICommandBuilder cmd, String selector, boolean active,
             String activeText, String inactiveText) {
    setStatusColor(cmd, selector, active ? activeText : inactiveText,
        active ? COLOR_GREEN : COLOR_GRAY);
  }

  private void setStatusColor(UICommandBuilder cmd, String selector, String text, String color) {
    cmd.set(selector + ".Text", text);
    cmd.set(selector + ".Style.TextColor", color);
  }
}
