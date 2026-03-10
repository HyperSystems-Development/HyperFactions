package com.hyperfactions.gui.admin.page;

import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.MessageKeys;

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

    // Localize page title
    cmd.set("#PageTitle.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_TITLE_VERSION));

    // Localize version card labels
    cmd.set("#VersionLabelFactions.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_VER_HYPERFACTIONS));
    cmd.set("#VersionLabelServer.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_VER_HYTALE_SERVER));
    cmd.set("#VersionLabelJava.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_VER_JAVA));

    // Localize section headers
    cmd.set("#SectionPermissions.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_VER_PERMISSIONS));
    cmd.set("#SectionPlaceholders.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_VER_PLACEHOLDERS));
    cmd.set("#SectionEconomy.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_VER_ECONOMY_SECTION));
    cmd.set("#SectionProtection.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_VER_PROTECTION));

    // --- Version Info ---
    cmd.set("#FactionsVersion.Text", "v" + HyperFactions.VERSION);

    String serverVersion = ManifestUtil.getVersion();
    cmd.set("#ServerVersion.Text", serverVersion != null ? serverVersion : "Unknown");

    cmd.set("#JavaVersion.Text", System.getProperty("java.version", "Unknown"));

    // --- Permissions ---
    setStatus(cmd, "#HyperPermsStatus", HyperPermsIntegration.isAvailable(), HFMessages.get(playerRef, MessageKeys.AdminGui.VER_ACTIVE), HFMessages.get(playerRef, MessageKeys.AdminGui.VER_NOT_FOUND));

    String providerNames = PermissionManager.get().getProviderNames();

    setStatus(cmd, "#LuckPermsStatus", providerNames.contains("LuckPerms"), HFMessages.get(playerRef, MessageKeys.AdminGui.VER_ACTIVE), HFMessages.get(playerRef, MessageKeys.AdminGui.VER_NOT_FOUND));

    boolean vaultAvailable = providerNames.contains("VaultUnlocked");
    boolean vaultInstalled = false;
    if (!vaultAvailable) {
      try {
        Class.forName("net.cfh.vault.VaultUnlocked");
        vaultInstalled = true;
      } catch (ClassNotFoundException ignored) {}
    }
    if (vaultAvailable) {
      setStatusColor(cmd, "#VaultUnlockedStatus", HFMessages.get(playerRef, MessageKeys.AdminGui.VER_ACTIVE), COLOR_GREEN);
    } else if (vaultInstalled) {
      setStatusColor(cmd, "#VaultUnlockedStatus", HFMessages.get(playerRef, MessageKeys.AdminGui.VER_ACTIVE_PROVIDER), COLOR_YELLOW);
    } else {
      setStatusColor(cmd, "#VaultUnlockedStatus", HFMessages.get(playerRef, MessageKeys.AdminGui.VER_NOT_INSTALLED), COLOR_GRAY);
    }

    setStatus(cmd, "#NativeStatus", providerNames.contains("HytaleNative"), HFMessages.get(playerRef, MessageKeys.AdminGui.VER_ACTIVE), HFMessages.get(playerRef, MessageKeys.AdminGui.VER_NOT_FOUND));

    // --- Protection ---
    ProtectionMixinBridge.MixinProvider provider = ProtectionMixinBridge.getProvider();
    boolean ogApiAvailable = OrbisGuardIntegration.isAvailable();

    switch (provider) {
      case BOTH -> {
        String hpVersion = System.getProperty("hyperprotect.bridge.version", "unknown");
        setStatusColor(cmd, "#HyperProtectStatus", HFMessages.get(playerRef, MessageKeys.AdminGui.VER_ACTIVE) + " (v" + hpVersion + ")", COLOR_GREEN);
        setStatusColor(cmd, "#OrbisGuardMixinsStatus", HFMessages.get(playerRef, MessageKeys.AdminGui.VER_ACTIVE) + " (compatible)", COLOR_GREEN);
      }
      case HYPERPROTECT -> {
        String hpVersion = System.getProperty("hyperprotect.bridge.version", "unknown");
        setStatusColor(cmd, "#HyperProtectStatus", HFMessages.get(playerRef, MessageKeys.AdminGui.VER_ACTIVE) + " (v" + hpVersion + ")", COLOR_GREEN);
        setStatusColor(cmd, "#OrbisGuardMixinsStatus", HFMessages.get(playerRef, MessageKeys.Common.NA), COLOR_GRAY);
      }
      case ORBISGUARD -> {
        setStatusColor(cmd, "#HyperProtectStatus", HFMessages.get(playerRef, MessageKeys.AdminGui.VER_NOT_DETECTED), COLOR_GRAY);
        setStatusColor(cmd, "#OrbisGuardMixinsStatus", HFMessages.get(playerRef, MessageKeys.AdminGui.VER_ACTIVE), COLOR_GREEN);
      }
      case NONE -> {
        setStatusColor(cmd, "#HyperProtectStatus", HFMessages.get(playerRef, MessageKeys.AdminGui.VER_NOT_DETECTED), COLOR_GRAY);
        setStatusColor(cmd, "#OrbisGuardMixinsStatus", HFMessages.get(playerRef, MessageKeys.AdminGui.VER_NOT_DETECTED), COLOR_GRAY);
      }
      default -> throw new IllegalStateException("Unexpected value");
    }

    if (ogApiAvailable) {
      String ogLabel = provider == ProtectionMixinBridge.MixinProvider.NONE
          ? HFMessages.get(playerRef, MessageKeys.AdminGui.VER_ACTIVE) + " (claims only)" : HFMessages.get(playerRef, MessageKeys.AdminGui.VER_ACTIVE);
      String ogColor = provider == ProtectionMixinBridge.MixinProvider.NONE
          ? COLOR_YELLOW : COLOR_GREEN;
      setStatusColor(cmd, "#OrbisGuardApiStatus", ogLabel, ogColor);
    } else {
      setStatusColor(cmd, "#OrbisGuardApiStatus", HFMessages.get(playerRef, MessageKeys.AdminGui.VER_NOT_DETECTED), COLOR_GRAY);
    }

    String mixinStatus = ProtectionMixinBridge.getStatusSummary();
    setStatusColor(cmd, "#MixinHooksStatus", mixinStatus,
        provider != ProtectionMixinBridge.MixinProvider.NONE ? COLOR_GREEN : COLOR_GRAY);

    GravestoneIntegration gs = plugin.getProtectionChecker().getGravestoneIntegration();
    boolean gsAvailable = gs != null && gs.isAvailable();
    boolean gsEnabled = ConfigManager.get().gravestones().isEnabled();
    String gsStatus = !gsAvailable ? HFMessages.get(playerRef, MessageKeys.AdminGui.VER_NOT_FOUND) : (gsEnabled ? HFMessages.get(playerRef, MessageKeys.AdminGui.VER_ACTIVE) : HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_VER_DISABLED));
    String gsColor = gsAvailable && gsEnabled ? COLOR_GREEN : (gsAvailable ? COLOR_YELLOW : COLOR_GRAY);
    setStatusColor(cmd, "#GravestonesStatus", gsStatus, gsColor);

    KyuubiSoftIntegration ks = plugin.getKyuubiSoftIntegration();
    boolean ksAvailable = ks != null && ks.isAvailable();
    setStatus(cmd, "#KyuubiSoftStatus", ksAvailable, HFMessages.get(playerRef, MessageKeys.AdminGui.VER_ACTIVE), HFMessages.get(playerRef, MessageKeys.AdminGui.VER_NOT_FOUND));

    // --- Placeholders ---
    setStatus(cmd, "#PlaceholderAPIStatus", PlaceholderAPIIntegration.isAvailable(), HFMessages.get(playerRef, MessageKeys.AdminGui.VER_ACTIVE), HFMessages.get(playerRef, MessageKeys.AdminGui.VER_NOT_FOUND));

    boolean wiflowAvailable;
    try {
      wiflowAvailable = WiFlowPlaceholderIntegration.isAvailable();
    } catch (NoClassDefFoundError e) {
      wiflowAvailable = false;
    }
    setStatus(cmd, "#WiFlowPAPIStatus", wiflowAvailable, HFMessages.get(playerRef, MessageKeys.AdminGui.VER_ACTIVE), HFMessages.get(playerRef, MessageKeys.AdminGui.VER_NOT_FOUND));

    // --- Economy ---
    if (plugin.isTreasuryEnabled()) {
      String econName = null;
      var econMgr = plugin.getEconomyManager();
      if (econMgr != null) {
        econName = econMgr.getVaultProvider().getEconomyName();
      }
      String treasuryLabel = econName != null ? HFMessages.get(playerRef, MessageKeys.AdminGui.VER_ACTIVE) + " (" + econName + ")" : HFMessages.get(playerRef, MessageKeys.AdminGui.VER_ACTIVE);
      setStatusColor(cmd, "#TreasuryStatus", treasuryLabel, COLOR_GREEN);
    } else {
      setStatusColor(cmd, "#TreasuryStatus", HFMessages.get(playerRef, MessageKeys.AdminGui.VER_NOT_FOUND), COLOR_GRAY);
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
