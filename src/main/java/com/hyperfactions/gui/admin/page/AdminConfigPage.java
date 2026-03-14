package com.hyperfactions.gui.admin.page;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.config.modules.*;
import com.hyperfactions.data.FactionPermissions;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.ConfigSnapshot;
import com.hyperfactions.gui.admin.ConfigValidator;
import com.hyperfactions.gui.admin.data.AdminConfigData;
import com.hyperfactions.util.AdminGuiKeys;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Admin Config Editor page with 11 tabbed sections and two-column layout.
 *
 * <p>
 * Allows admins to view and edit all HyperFactions configuration settings
 * directly from an in-game GUI. Changes are tracked as pending until saved.
 * Uses targeted updates (no full page rebuild) for toggle/increment/text changes.
 */
public class AdminConfigPage extends InteractiveCustomUIPage<AdminConfigData> {

  // ================================================================
  // Edit Session Cache
  // ================================================================

  /** Cached edit sessions per player — survives page close/reopen. */
  private static final ConcurrentHashMap<UUID, EditSession> editSessions = new ConcurrentHashMap<>();

  /** Snapshot of pending edits that survives page close/reopen. */
  private record EditSession(
      String tab,
      Map<String, Object> pendingChanges,
      Map<String, Object> originalValues,
      LinkedHashMap<String, WorldsConfig.WorldSettings> pendingWorldOverrides
  ) {}

  /** Removes the cached edit session for a player. */
  public static void clearSession(UUID playerId) {
    editSessions.remove(playerId);
  }

  private static final String[] TABS = {
      "server", "chat", "announcements", "economy", "factions",
      "factionPerms", "worldmap", "worlds", "backup", "debug", "gravestones"
  };

  private static final String[] TAB_KEYS = {
      AdminGuiKeys.AdminGui.CFG_TAB_SERVER,
      AdminGuiKeys.AdminGui.CFG_TAB_CHAT,
      AdminGuiKeys.AdminGui.CFG_TAB_ANNOUNCEMENTS,
      AdminGuiKeys.AdminGui.CFG_TAB_ECONOMY,
      AdminGuiKeys.AdminGui.CFG_TAB_FACTIONS,
      AdminGuiKeys.AdminGui.CFG_TAB_FACTION_PERMS,
      AdminGuiKeys.AdminGui.CFG_TAB_WORLDMAP,
      AdminGuiKeys.AdminGui.CFG_TAB_WORLDS,
      AdminGuiKeys.AdminGui.CFG_TAB_BACKUP,
      AdminGuiKeys.AdminGui.CFG_TAB_DEBUG,
      AdminGuiKeys.AdminGui.CFG_TAB_GRAVESTONES
  };

  /** Setting type for targeted updates. */
  private enum SettingKind { BOOL, INT, DOUBLE, STRING, COLOR, ENUM }

  /** Layout size for template selection. */
  private enum LayoutSize { NARROW, STANDARD, WIDE }

  private static LayoutSize getLayoutSize(String tab) {
    return switch (tab) {
      case "backup", "gravestones", "worlds" -> LayoutSize.NARROW;
      case "factions", "factionPerms" -> LayoutSize.WIDE;
      default -> LayoutSize.STANDARD;
    };
  }

  private final PlayerRef playerRef;
  private final GuiManager guiManager;

  private String currentTab = "server";
  private final ConcurrentHashMap<String, Object> pendingChanges = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Object> originalValues = new ConcurrentHashMap<>();
  private boolean saveConfirmActive = false;
  private boolean resetConfirmActive = false;
  private final java.util.Set<String> expandedWorlds = ConcurrentHashMap.newKeySet();

  /** Fields with invalid input that must be fixed before saving. */
  private final java.util.Set<String> invalidFields = ConcurrentHashMap.newKeySet();

  /** Pending world overrides map — null means not yet modified from config. */
  private java.util.LinkedHashMap<String, WorldsConfig.WorldSettings> pendingWorldOverrides = null;

  /** Maps settingKey → container selector (e.g. "#LeftContainer[3]") for targeted updates. */
  private final Map<String, String> settingSelectors = new HashMap<>();

  /** Maps settingKey → its SettingKind for targeted updates. */
  private final Map<String, SettingKind> settingKinds = new HashMap<>();

  /** Debounce timestamp for text input — only the latest scheduled refresh runs. */
  private static final long DEBOUNCE_MS = 600;
  private volatile long lastTextInputNanos = 0;

  /** Row counter for indexed selectors. Reset each build cycle. */
  private int leftRowIdx;
  private int rightRowIdx;
  private boolean appendingToLeft = true;

  /** 4-column mode for fac perms tab. */
  private boolean fourColumnMode = false;
  private int[] colRowIdx = new int[4];
  private int activeCol = 0;

  /** Creates a new AdminConfigPage. */
  public AdminConfigPage(PlayerRef playerRef, GuiManager guiManager) {
    this(playerRef, guiManager, "server");
  }

  /** Creates a new AdminConfigPage opened to a specific tab. */
  public AdminConfigPage(PlayerRef playerRef, GuiManager guiManager, String initialTab) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminConfigData.CODEC);
    this.playerRef = playerRef;
    this.guiManager = guiManager;
    this.currentTab = resolveTab(initialTab);
    restoreSession();
  }

  /** Restores cached edit session if one exists for this player. */
  private void restoreSession() {
    EditSession session = editSessions.remove(playerRef.getUuid());
    if (session != null) {
      this.currentTab = session.tab();
      this.pendingChanges.putAll(session.pendingChanges());
      this.originalValues.putAll(session.originalValues());
      this.pendingWorldOverrides = session.pendingWorldOverrides();
    }
  }

  /** Saves current edit session to cache if there are pending changes. */
  private void saveSession() {
    boolean hasChanges = !pendingChanges.isEmpty() || pendingWorldOverrides != null;
    if (hasChanges) {
      editSessions.put(playerRef.getUuid(), new EditSession(
          currentTab,
          new HashMap<>(pendingChanges),
          new HashMap<>(originalValues),
          pendingWorldOverrides != null ? new LinkedHashMap<>(pendingWorldOverrides) : null
      ));
    } else {
      editSessions.remove(playerRef.getUuid());
    }
  }

  /** Resolves a tab name or alias to a valid tab ID. */
  private static String resolveTab(String input) {
    if (input == null) return "server";
    return switch (input.toLowerCase()) {
      case "server", "srv" -> "server";
      case "chat" -> "chat";
      case "announcements", "announce", "ann" -> "announcements";
      case "economy", "eco" -> "economy";
      case "factions", "faction", "fac" -> "factions";
      case "factionperms", "facperms", "perms", "permissions" -> "factionPerms";
      case "worldmap", "map" -> "worldmap";
      case "worlds", "world" -> "worlds";
      case "backup", "backups" -> "backup";
      case "debug", "dbg" -> "debug";
      case "gravestones", "graves", "grave" -> "gravestones";
      default -> "server";
    };
  }

  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                     UIEventBuilder events, Store<EntityStore> store) {
    String template = switch (getLayoutSize(currentTab)) {
      case NARROW -> UIPaths.ADMIN_CONFIG_NARROW;
      case STANDARD -> UIPaths.ADMIN_CONFIG_STANDARD;
      case WIDE -> UIPaths.ADMIN_CONFIG_WIDE;
    };
    cmd.append(template);
    AdminNavBarHelper.setupBar(playerRef, "config", cmd, events);

    cmd.set("#SaveBtn.Text", loc(AdminGuiKeys.AdminGui.CFG_BTN_SAVE));
    cmd.set("#RevertBtn.Text", loc(AdminGuiKeys.AdminGui.CFG_BTN_REVERT));
    cmd.set("#ResetBtn.Text", loc(AdminGuiKeys.AdminGui.CFG_BTN_RESET));

    buildDynamicContent(cmd, events);
  }

  /** Full rebuild of all dynamic content: tabs, settings, status, action bindings. */
  private void buildDynamicContent(UICommandBuilder cmd, UIEventBuilder events) {
    for (int i = 0; i < TABS.length; i++) {
      String tab = TABS[i];
      String tabId = "#Tab" + tab.substring(0, 1).toUpperCase() + tab.substring(1);
      events.addEventBinding(CustomUIEventBindingType.Activating, tabId,
          EventData.of("Button", "TabSwitch").append("Tab", tab), false);
    }

    updatePageTitle(cmd);
    updateStatusLabel(cmd);

    // Determine layout mode from current template
    fourColumnMode = getLayoutSize(currentTab) == LayoutSize.WIDE;

    // Clear only the containers that exist in the current template
    switch (getLayoutSize(currentTab)) {
      case NARROW -> cmd.clear("#LeftContainer");
      case STANDARD -> { cmd.clear("#LeftContainer"); cmd.clear("#RightContainer"); }
      case WIDE -> { cmd.clear("#Col1Container"); cmd.clear("#Col2Container");
                      cmd.clear("#Col3Container"); cmd.clear("#Col4Container"); }
    }
    leftRowIdx = 0;
    rightRowIdx = 0;
    appendingToLeft = true;
    colRowIdx = new int[4];
    activeCol = 0;
    settingSelectors.clear();
    settingKinds.clear();
    buildTabContent(cmd, events);

    events.addEventBinding(CustomUIEventBindingType.Activating, "#SaveBtn",
        EventData.of("Button", "Save"), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#RevertBtn",
        EventData.of("Button", "Revert"), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#ResetBtn",
        EventData.of("Button", "ResetDefaults"), false);

    // Save button: confirm state + disable when invalid
    if (!invalidFields.isEmpty()) {
      cmd.set("#SaveBtn.Disabled", true);
    }
    if (saveConfirmActive) {
      cmd.set("#SaveBtn.Text", "Confirm Save");
    } else {
      cmd.set("#SaveBtn.Text", loc(AdminGuiKeys.AdminGui.CFG_BTN_SAVE));
    }

    if (resetConfirmActive) {
      cmd.set("#ResetBtn.Text", loc(AdminGuiKeys.AdminGui.CFG_RESET_CONFIRM));
    } else {
      cmd.set("#ResetBtn.Text", loc(AdminGuiKeys.AdminGui.CFG_BTN_RESET));
    }
  }

  private void updatePageTitle(UICommandBuilder cmd) {
    String tabName = switch (currentTab) {
      case "server" -> "Server";
      case "chat" -> "Chat";
      case "announcements" -> "Announcements";
      case "economy" -> "Economy";
      case "factions" -> "Factions";
      case "factionPerms" -> "Faction Perms";
      case "worldmap" -> "Worldmap";
      case "worlds" -> "Worlds";
      case "backup" -> "Backup";
      case "debug" -> "Debug";
      case "gravestones" -> "Gravestones";
      default -> currentTab;
    };
    cmd.set("#PageTitle.Text", "Config: " + tabName);
  }

  private void updateStatusLabel(UICommandBuilder cmd) {
    int errorCount = invalidFields.size();
    int changeCount = pendingChanges.size() + (pendingWorldOverrides != null ? 1 : 0);
    if (errorCount > 0) {
      cmd.set("#StatusLabel.Text", errorCount + " invalid");
      cmd.set("#StatusLabel.Style.TextColor", "#FF4444");
    } else if (changeCount > 0) {
      cmd.set("#StatusLabel.Text", changeCount + " " + loc(AdminGuiKeys.AdminGui.CFG_CHANGES_PENDING));
      cmd.set("#StatusLabel.Style.TextColor", "#FFAA00");
    } else {
      cmd.set("#StatusLabel.Text", loc(AdminGuiKeys.AdminGui.CFG_NO_CHANGES));
      cmd.set("#StatusLabel.Style.TextColor", "#888888");
    }
  }

  /**
   * Targeted update: just update the label color for a single setting + status bar.
   * No full page rebuild, no clearing containers, no re-binding events.
   */
  private void updateSettingAndStatus(Ref<EntityStore> ref, Store<EntityStore> store, String key) {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();

    String selector = settingSelectors.get(key);
    if (selector != null) {
      boolean pending = pendingChanges.containsKey(key);
      String color = pending ? "#FFAA00" : "#CCCCCC";
      cmd.set(selector + " #SettingLabel.Style.TextColor", color);

      SettingKind kind = settingKinds.get(key);

      // Update the value display for types where the UI doesn't self-update
      if (kind == SettingKind.INT) {
        Object orig = originalValues.get(key);
        int val = pending ? ((Number) pendingChanges.get(key)).intValue()
            : (orig instanceof Number n ? n.intValue() : 0);
        cmd.set(selector + " #NumInput.Value", String.valueOf(val));
        cmd.set(selector + " #NumInput.Style.TextColor", pending ? "#FFAA00" : "#FFFFFF");
      } else if (kind == SettingKind.DOUBLE) {
        Object orig = originalValues.get(key);
        double val = pending ? ((Number) pendingChanges.get(key)).doubleValue()
            : (orig instanceof Number n ? n.doubleValue() : 0.0);
        cmd.set(selector + " #NumInput.Value", String.format("%.2f", val));
        cmd.set(selector + " #NumInput.Style.TextColor", pending ? "#FFAA00" : "#FFFFFF");
      }
      // BOOL: checkbox handles its own visual toggle
      // ENUM: dropdown handles its own visual update
      // STRING: text field has user input already, don't overwrite
      if (kind == SettingKind.COLOR) {
        Object orig = originalValues.get(key);
        String val = pending ? String.valueOf(pendingChanges.get(key))
            : (orig instanceof String s ? s : "#FFFFFF");
        cmd.set(selector + " #ColorInput.Value", val);
        cmd.set(selector + " #ColorInput.Style.TextColor", pending ? "#FFAA00" : "#FFFFFF");
        cmd.set(selector + " #ColorPicker.Color", val);
      }
    }

    updateStatusLabel(cmd);
    sendUpdate(cmd, events, false);
  }

  private void buildTabContent(UICommandBuilder cmd, UIEventBuilder events) {
    ConfigManager cfg = ConfigManager.get();
    switch (currentTab) {
      case "server" -> buildServerTab(cmd, events, cfg);
      case "chat" -> buildChatTab(cmd, events, cfg);
      case "announcements" -> buildAnnouncementsTab(cmd, events, cfg);
      case "economy" -> buildEconomyTab(cmd, events, cfg);
      case "factions" -> buildFactionsTab(cmd, events, cfg);
      case "factionPerms" -> buildFactionPermsTab(cmd, events, cfg);
      case "worldmap" -> buildWorldmapTab(cmd, events, cfg);
      case "worlds" -> buildWorldsTab(cmd, events, cfg);
      case "backup" -> buildBackupTab(cmd, events, cfg);
      case "debug" -> buildDebugTab(cmd, events, cfg);
      case "gravestones" -> buildGravestonesTab(cmd, events, cfg);
    }
  }

  // ================================================================
  // Column Management
  // ================================================================

  private void setColumn(boolean left) {
    this.appendingToLeft = left;
  }

  /** Sets the active column in 4-column mode (0-3). */
  private void setCol(int col) {
    this.activeCol = col;
  }

  private String getContainerId() {
    if (fourColumnMode) {
      return "#Col" + (activeCol + 1) + "Container";
    }
    return appendingToLeft ? "#LeftContainer" : "#RightContainer";
  }

  private int getRowIdx() {
    if (fourColumnMode) {
      return colRowIdx[activeCol];
    }
    return appendingToLeft ? leftRowIdx : rightRowIdx;
  }

  private void incrementRowIdx() {
    if (fourColumnMode) {
      colRowIdx[activeCol]++;
    } else if (appendingToLeft) {
      leftRowIdx++;
    } else {
      rightRowIdx++;
    }
  }

  // ================================================================
  // Server Tab
  // ================================================================

  private void buildServerTab(UICommandBuilder cmd, UIEventBuilder events, ConfigManager cfg) {
    setColumn(true);
    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_TELEPORT));
    addIntSetting(cmd, events, "server.warmupSeconds", "Warmup Seconds", cfg.getWarmupSeconds());
    addIntSetting(cmd, events, "server.cooldownSeconds", "Cooldown Seconds", cfg.getCooldownSeconds());
    addBooleanSetting(cmd, events, "server.cancelOnMove", "Cancel on Move", cfg.isCancelOnMove());
    addBooleanSetting(cmd, events, "server.cancelOnDamage", "Cancel on Damage", cfg.isCancelOnDamage());

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_AUTOSAVE));
    addBooleanSetting(cmd, events, "server.autoSaveEnabled", "Auto-Save Enabled", cfg.isAutoSaveEnabled());
    addIntSetting(cmd, events, "server.autoSaveIntervalMinutes", "Save Interval (min)", cfg.getAutoSaveIntervalMinutes());

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_MESSAGES));
    addStringSetting(cmd, events, "server.prefixText", "Prefix Text", cfg.getPrefixText());
    addColorSetting(cmd, events, "server.prefixColor", "Prefix Color", cfg.getPrefixColor());
    addColorSetting(cmd, events, "server.prefixBracketColor", "Bracket Color", cfg.server().getPrefixBracketColor());
    addColorSetting(cmd, events, "server.primaryColor", "Primary Color", cfg.getPrimaryColor());

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_MOB_CLEAR));
    addBooleanSetting(cmd, events, "server.mobClearEnabled", "Mob Clear Enabled", cfg.isMobClearEnabled());
    addIntSetting(cmd, events, "server.mobClearIntervalSeconds", "Clear Interval (sec)", cfg.getMobClearIntervalSeconds());

    setColumn(false);
    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_GUI));
    addStringSetting(cmd, events, "server.guiTitle", "GUI Title", cfg.getGuiTitle());
    addBooleanSetting(cmd, events, "server.terrainMapEnabled", "Terrain Map", cfg.isTerrainMapEnabled());
    addIntSetting(cmd, events, "server.leaderboardKdRefreshSeconds", "K/D Refresh (sec)", cfg.server().getLeaderboardKdRefreshSeconds());

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_PERMISSIONS));
    addBooleanSetting(cmd, events, "server.adminRequiresOp", "Admin Requires OP", cfg.isAdminRequiresOp());
    addBooleanSetting(cmd, events, "server.allowWithoutPermissionMod", "Allow Without Perm Mod", cfg.isAllowWithoutPermissionMod());

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_LANGUAGE));
    addLocaleSetting(cmd, events, "server.defaultLanguage", "Default Language", cfg.getDefaultLanguage());
    addBooleanSetting(cmd, events, "server.usePlayerLanguage", "Use Player Language", cfg.isUsePlayerLanguage());

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_UPDATES));
    addBooleanSetting(cmd, events, "server.updateCheckEnabled", "Update Check", cfg.isUpdateCheckEnabled());
    addEnumSetting(cmd, events, "server.releaseChannel", "Release Channel", cfg.getReleaseChannel(), "stable", "prerelease");

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_MIXIN));
    addBooleanSetting(cmd, events, "server.hyperProtectAutoDownload", "Auto Download", cfg.server().isHyperProtectAutoDownload());
    addBooleanSetting(cmd, events, "server.hyperProtectAutoUpdate", "Auto Update", cfg.server().isHyperProtectAutoUpdate());
  }

  // ================================================================
  // Chat Tab
  // ================================================================

  private void buildChatTab(UICommandBuilder cmd, UIEventBuilder events, ConfigManager cfg) {
    setColumn(true);
    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_FORMAT));
    addBooleanSetting(cmd, events, "chat.enabled", "Chat Formatting", cfg.isChatFormattingEnabled());
    addEnumSetting(cmd, events, "chat.tagDisplay", "Tag Display", cfg.getChatTagDisplay(), "tag", "name", "none");
    addStringSetting(cmd, events, "chat.tagFormat", "Tag Format", cfg.getChatTagFormat());
    addStringSetting(cmd, events, "chat.noFactionTag", "Factionless Tag", cfg.getChatNoFactionTag());

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_COLORS));
    addColorSetting(cmd, events, "chat.noFactionTagColor", "Factionless Color", cfg.getChatNoFactionTagColor());
    addColorSetting(cmd, events, "chat.playerNameColor", "Player Name Color", cfg.getChatPlayerNameColor());

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_REL_COLORS));
    addColorSetting(cmd, events, "chat.relationColorOwn", "Own Faction", cfg.getChatRelationColorOwn());
    addColorSetting(cmd, events, "chat.relationColorAlly", "Ally", cfg.getChatRelationColorAlly());
    addColorSetting(cmd, events, "chat.relationColorNeutral", "Neutral", cfg.getChatRelationColorNeutral());
    addColorSetting(cmd, events, "chat.relationColorEnemy", "Enemy", cfg.getChatRelationColorEnemy());

    setColumn(false);
    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_FACTION_CHAT));
    addColorSetting(cmd, events, "chat.factionChatColor", "Faction Chat Color", cfg.getFactionChatColor());
    addStringSetting(cmd, events, "chat.factionChatPrefix", "Faction Chat Prefix", cfg.getFactionChatPrefix());
    addColorSetting(cmd, events, "chat.allyChatColor", "Ally Chat Color", cfg.getAllyChatColor());
    addStringSetting(cmd, events, "chat.allyChatPrefix", "Ally Chat Prefix", cfg.getAllyChatPrefix());
    addColorSetting(cmd, events, "chat.senderNameColor", "Sender Name Color", cfg.getSenderNameColor());
    addColorSetting(cmd, events, "chat.messageColor", "Message Color", cfg.getMessageColor());

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_HISTORY));
    addBooleanSetting(cmd, events, "chat.historyEnabled", "Chat History", cfg.isChatHistoryEnabled());
    addIntSetting(cmd, events, "chat.historyMaxMessages", "Max Messages", cfg.getChatHistoryMaxMessages());
    addIntSetting(cmd, events, "chat.historyRetentionDays", "Retention Days", cfg.getChatHistoryRetentionDays());
    addIntSetting(cmd, events, "chat.historyCleanupIntervalMinutes", "Cleanup (min)", cfg.getChatHistoryCleanupIntervalMinutes());
  }

  // ================================================================
  // Announcements Tab
  // ================================================================

  private void buildAnnouncementsTab(UICommandBuilder cmd, UIEventBuilder events, ConfigManager cfg) {
    setColumn(true);
    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_ANNOUNCE));
    addBooleanSetting(cmd, events, "announce.factionCreated", "Faction Created", cfg.announcements().isFactionCreated());
    addBooleanSetting(cmd, events, "announce.factionDisbanded", "Faction Disbanded", cfg.announcements().isFactionDisbanded());
    addBooleanSetting(cmd, events, "announce.leadershipTransfer", "Leadership Transfer", cfg.announcements().isLeadershipTransfer());
    addBooleanSetting(cmd, events, "announce.overclaim", "Overclaim", cfg.announcements().isOverclaim());
    addBooleanSetting(cmd, events, "announce.warDeclared", "War Declared", cfg.announcements().isWarDeclared());
    addBooleanSetting(cmd, events, "announce.allianceFormed", "Alliance Formed", cfg.announcements().isAllianceFormed());
    addBooleanSetting(cmd, events, "announce.allianceBroken", "Alliance Broken", cfg.announcements().isAllianceBroken());

    setColumn(false);
    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_TERRITORY_NOTIFY));
    addBooleanSetting(cmd, events, "announce.territoryNotificationsEnabled", "Territory Notifications", cfg.isTerritoryNotificationsEnabled());

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_WILDERNESS));
    addBooleanSetting(cmd, events, "announce.wildernessOnLeaveZoneEnabled", "Show on Leave Zone", cfg.announcements().isWildernessOnLeaveZoneEnabled());
    addWideStringSetting(cmd, events, "announce.wildernessOnLeaveZoneUpper", "Zone Upper Text", cfg.announcements().getWildernessOnLeaveZoneUpper());
    addWideStringSetting(cmd, events, "announce.wildernessOnLeaveZoneLower", "Zone Lower Text", cfg.announcements().getWildernessOnLeaveZoneLower());
    addBooleanSetting(cmd, events, "announce.wildernessOnLeaveClaimEnabled", "Show on Leave Claim", cfg.announcements().isWildernessOnLeaveClaimEnabled());
    addWideStringSetting(cmd, events, "announce.wildernessOnLeaveClaimUpper", "Claim Upper Text", cfg.announcements().getWildernessOnLeaveClaimUpper());
    addWideStringSetting(cmd, events, "announce.wildernessOnLeaveClaimLower", "Claim Lower Text", cfg.announcements().getWildernessOnLeaveClaimLower());
  }

  // ================================================================
  // Economy Tab
  // ================================================================

  private void buildEconomyTab(UICommandBuilder cmd, UIEventBuilder events, ConfigManager cfg) {
    EconomyConfig eco = cfg.economy();
    setColumn(true);
    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_CURRENCY));
    addBooleanSetting(cmd, events, "economy.enabled", "Economy Enabled", eco.isEnabled());
    addStringSetting(cmd, events, "economy.currencyName", "Currency Name", eco.getCurrencyName());
    addStringSetting(cmd, events, "economy.currencyNamePlural", "Currency Plural", eco.getCurrencyNamePlural());
    addStringSetting(cmd, events, "economy.currencySymbol", "Currency Symbol", eco.getCurrencySymbol());
    addEnumSetting(cmd, events, "economy.currencySymbolPosition", "Symbol Position", eco.getCurrencySymbolPosition(), "left", "right");
    addDoubleSetting(cmd, events, "economy.startingBalance", "Starting Balance", eco.getStartingBalance().doubleValue());
    addBooleanSetting(cmd, events, "economy.disbandRefundToLeader", "Disband Refund", eco.isDisbandRefundToLeader());

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_TREASURY_LIMITS));
    addDoubleSetting(cmd, events, "economy.defaultMaxWithdrawAmount", "Max Withdraw", eco.getDefaultMaxWithdrawAmount().doubleValue());
    addDoubleSetting(cmd, events, "economy.defaultMaxWithdrawPerPeriod", "Max Withdraw/Period", eco.getDefaultMaxWithdrawPerPeriod().doubleValue());
    addDoubleSetting(cmd, events, "economy.defaultMaxTransferAmount", "Max Transfer", eco.getDefaultMaxTransferAmount().doubleValue());
    addDoubleSetting(cmd, events, "economy.defaultMaxTransferPerPeriod", "Max Transfer/Period", eco.getDefaultMaxTransferPerPeriod().doubleValue());
    addIntSetting(cmd, events, "economy.defaultLimitPeriodHours", "Limit Period (hr)", eco.getDefaultLimitPeriodHours());

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_FEES));
    addDoubleSetting(cmd, events, "economy.depositFeePercent", "Deposit Fee %", eco.getDepositFeePercent().doubleValue());
    addDoubleSetting(cmd, events, "economy.withdrawFeePercent", "Withdraw Fee %", eco.getWithdrawFeePercent().doubleValue());
    addDoubleSetting(cmd, events, "economy.transferFeePercent", "Transfer Fee %", eco.getTransferFeePercent().doubleValue());

    setColumn(false);
    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_UPKEEP));
    addBooleanSetting(cmd, events, "economy.upkeepEnabled", "Upkeep Enabled", eco.isUpkeepEnabled());
    addDoubleSetting(cmd, events, "economy.upkeepCostPerChunk", "Cost Per Chunk", eco.getUpkeepCostPerChunk().doubleValue());
    addIntSetting(cmd, events, "economy.upkeepIntervalHours", "Interval (hr)", eco.getUpkeepIntervalHours());
    addIntSetting(cmd, events, "economy.upkeepGracePeriodHours", "Grace Period (hr)", eco.getUpkeepGracePeriodHours());
    addBooleanSetting(cmd, events, "economy.upkeepAutoPayDefault", "Auto-Pay Default", eco.isUpkeepAutoPayDefault());
    addIntSetting(cmd, events, "economy.upkeepFreeChunks", "Free Chunks", eco.getUpkeepFreeChunks());
    addIntSetting(cmd, events, "economy.upkeepClaimLossPerCycle", "Claim Loss/Cycle", eco.getUpkeepClaimLossPerCycle());
    addIntSetting(cmd, events, "economy.upkeepWarningHours", "Warning (hr)", eco.getUpkeepWarningHours());
    addDoubleSetting(cmd, events, "economy.upkeepMaxCostCap", "Max Cost Cap", eco.getUpkeepMaxCostCap().doubleValue());
    addEnumSetting(cmd, events, "economy.upkeepScalingMode", "Scaling Mode", eco.getUpkeepScalingMode(), "flat", "progressive");
    String effectiveScaling = pendingChanges.containsKey("economy.upkeepScalingMode")
        ? String.valueOf(pendingChanges.get("economy.upkeepScalingMode")) : eco.getUpkeepScalingMode();
    boolean scalingDisabled = "flat".equals(effectiveScaling);
    if (!scalingDisabled) {
      addActionButton(cmd, events, "EditScalingTiers",
          "Edit Tiers (" + eco.getUpkeepScalingTiers().size() + ")", false);
    }
  }

  // ================================================================
  // Factions Tab
  // ================================================================

  private void buildFactionsTab(UICommandBuilder cmd, UIEventBuilder events, ConfigManager cfg) {
    // Col 0: Limits + Power
    setCol(0);
    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_FACTION_LIMITS));
    addIntSetting(cmd, events, "factions.maxMembers", "Max Members", cfg.getMaxMembers());
    addIntSetting(cmd, events, "factions.maxNameLength", "Max Name Len", cfg.getMaxNameLength());
    addIntSetting(cmd, events, "factions.minNameLength", "Min Name Len", cfg.getMinNameLength());
    addBooleanSetting(cmd, events, "factions.allowColors", "Allow Colors", cfg.isAllowColors());
    addIntSetting(cmd, events, "factions.maxMembershipHistory", "History Limit", cfg.factions().getMaxMembershipHistory());

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_POWER));
    addDoubleSetting(cmd, events, "factions.maxPlayerPower", "Max Power", cfg.getMaxPlayerPower());
    addDoubleSetting(cmd, events, "factions.startingPower", "Starting Power", cfg.getStartingPower());
    addDoubleSetting(cmd, events, "factions.powerPerClaim", "Power/Claim", cfg.getPowerPerClaim());
    addDoubleSetting(cmd, events, "factions.deathPenalty", "Death Penalty", cfg.getDeathPenalty());
    addDoubleSetting(cmd, events, "factions.killReward", "Kill Reward", cfg.getKillReward());
    addBooleanSetting(cmd, events, "factions.killRewardRequiresFaction", "Requires Faction", cfg.isKillRewardRequiresFaction());

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_POWER_LOSS));
    addBooleanSetting(cmd, events, "factions.powerLossOnMobDeath", "Mob Death", cfg.isPowerLossOnMobDeath());
    addBooleanSetting(cmd, events, "factions.powerLossOnEnvironmentalDeath", "Env. Death", cfg.isPowerLossOnEnvironmentalDeath());

    // Col 1: Regen + Claims + Decay + Stuck
    setCol(1);
    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_REGEN));
    addDoubleSetting(cmd, events, "factions.regenPerMinute", "Regen/Min", cfg.getRegenPerMinute());
    addBooleanSetting(cmd, events, "factions.regenWhenOffline", "Offline Regen", cfg.isRegenWhenOffline());
    addBooleanSetting(cmd, events, "factions.hardcoreMode", "Hardcore", cfg.isHardcoreMode());

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_CLAIMS));
    addIntSetting(cmd, events, "factions.maxClaims", "Max Claims", cfg.getMaxClaims());
    addBooleanSetting(cmd, events, "factions.onlyAdjacent", "Only Adjacent", cfg.isOnlyAdjacent());
    addBooleanSetting(cmd, events, "factions.preventDisconnect", "No Disconnect", cfg.isPreventDisconnect());

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_DECAY));
    addBooleanSetting(cmd, events, "factions.decayEnabled", "Decay Enabled", cfg.isDecayEnabled());
    addIntSetting(cmd, events, "factions.decayDaysInactive", "Days Inactive", cfg.getDecayDaysInactive());
    addIntSetting(cmd, events, "factions.decayClaimsPerCycle", "Claims/Cycle", cfg.factions().getDecayClaimsPerCycle());

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_STUCK));
    addIntSetting(cmd, events, "factions.stuckMinRadius", "Min Radius", cfg.getStuckMinRadius());
    addIntSetting(cmd, events, "factions.stuckRadiusIncrease", "Radius Inc", cfg.getStuckRadiusIncrease());
    addIntSetting(cmd, events, "factions.stuckMaxAttempts", "Max Attempts", cfg.getStuckMaxAttempts());
    addIntSetting(cmd, events, "factions.stuckWarmupSeconds", "Warmup (sec)", cfg.getStuckWarmupSeconds());
    addIntSetting(cmd, events, "factions.stuckCooldownSeconds", "Cooldown (sec)", cfg.getStuckCooldownSeconds());

    // Col 2: Protection + Friendly Fire
    setCol(2);
    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_PROTECTION));
    addBooleanSetting(cmd, events, "factions.outsiderPickupAllowed", "Outsider Pickup", cfg.isOutsiderPickupAllowed());
    addBooleanSetting(cmd, events, "factions.outsiderDropAllowed", "Outsider Drop", cfg.isOutsiderDropAllowed());
    addBooleanSetting(cmd, events, "factions.factionlessExplosionsAllowed", "F'less Explosions", cfg.isFactionlessExplosionsAllowed());
    addBooleanSetting(cmd, events, "factions.enemyExplosionsAllowed", "Enemy Explosions", cfg.isEnemyExplosionsAllowed());
    addBooleanSetting(cmd, events, "factions.neutralExplosionsAllowed", "Neutral Explosions", cfg.isNeutralExplosionsAllowed());
    addBooleanSetting(cmd, events, "factions.fireSpreadAllowed", "Fire Spread", cfg.isFireSpreadAllowed());
    addBooleanSetting(cmd, events, "factions.factionlessDamageAllowed", "F'less Damage", cfg.isFactionlessDamageAllowed());
    addBooleanSetting(cmd, events, "factions.enemyDamageAllowed", "Enemy Damage", cfg.isEnemyDamageAllowed());
    addBooleanSetting(cmd, events, "factions.neutralDamageAllowed", "Neutral Damage", cfg.isNeutralDamageAllowed());

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_FRIENDLY_FIRE));
    addBooleanSetting(cmd, events, "factions.allyDamage", "Ally Damage", cfg.isAllyDamage());
    addBooleanSetting(cmd, events, "factions.factionDamage", "Faction Damage", cfg.isFactionDamage());

    // Col 3: Combat Tag + Spawn Prot + Relations + Invites
    setCol(3);
    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_COMBAT_TAG));
    addIntSetting(cmd, events, "factions.tagDurationSeconds", "Duration (sec)", cfg.getTagDurationSeconds());
    addBooleanSetting(cmd, events, "factions.taggedLogoutPenalty", "Logout Penalty", cfg.isTaggedLogoutPenalty());
    addDoubleSetting(cmd, events, "factions.logoutPowerLoss", "Logout Loss", cfg.getLogoutPowerLoss());
    addDoubleSetting(cmd, events, "factions.neutralAttackPenalty", "Neutral Pen.", cfg.getNeutralAttackPenalty());

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_SPAWN_PROT));
    addBooleanSetting(cmd, events, "factions.spawnProtectionEnabled", "Enabled", cfg.isSpawnProtectionEnabled());
    addIntSetting(cmd, events, "factions.spawnProtectionDurationSeconds", "Duration (sec)", cfg.getSpawnProtectionDurationSeconds());
    addBooleanSetting(cmd, events, "factions.spawnProtectionBreakOnAttack", "Break on Hit", cfg.isSpawnProtectionBreakOnAttack());
    addBooleanSetting(cmd, events, "factions.spawnProtectionBreakOnMove", "Break on Move", cfg.isSpawnProtectionBreakOnMove());

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_RELATIONS));
    addIntSetting(cmd, events, "factions.maxAllies", "Max Allies", cfg.getMaxAllies());
    addIntSetting(cmd, events, "factions.maxEnemies", "Max Enemies", cfg.getMaxEnemies());

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_INVITES));
    addIntSetting(cmd, events, "factions.inviteExpirationMinutes", "Invite Expiry", cfg.getInviteExpirationMinutes());
    addIntSetting(cmd, events, "factions.joinRequestExpirationHours", "Request Expiry", cfg.getJoinRequestExpirationHours());
  }

  // ================================================================
  // Faction Perms Tab
  // ================================================================

  private void buildFactionPermsTab(UICommandBuilder cmd, UIEventBuilder events, ConfigManager cfg) {
    FactionPermissionsConfig permCfg = cfg.factionPermissions();

    // Col 0: Outsider — Col 1: Ally — Col 2: Member — Col 3: Officer
    setCol(0);
    addFacPermSection(cmd, events, permCfg, FactionPermissions.LEVEL_OUTSIDER);

    setCol(1);
    addFacPermSection(cmd, events, permCfg, FactionPermissions.LEVEL_ALLY);

    setCol(2);
    addFacPermSection(cmd, events, permCfg, FactionPermissions.LEVEL_MEMBER);

    setCol(3);
    addFacPermSection(cmd, events, permCfg, FactionPermissions.LEVEL_OFFICER);

    // Shared sections — distribute across columns to balance height
    setCol(0);
    addSectionHeader(cmd, "Global");
    addFacPermColumnHeader(cmd);
    addFacPermRow(cmd, events, permCfg, FactionPermissions.PVP_ENABLED, false, false);
    addFacPermRow(cmd, events, permCfg, FactionPermissions.OFFICERS_CAN_EDIT, false, false);

    setCol(1);
    addSectionHeader(cmd, "Mob Spawning");
    addFacPermColumnHeader(cmd);
    addFacPermRow(cmd, events, permCfg, FactionPermissions.MOB_SPAWNING, false, true);
    addFacPermRow(cmd, events, permCfg, FactionPermissions.HOSTILE_MOB_SPAWNING, true, false);
    addFacPermRow(cmd, events, permCfg, FactionPermissions.PASSIVE_MOB_SPAWNING, true, false);
    addFacPermRow(cmd, events, permCfg, FactionPermissions.NEUTRAL_MOB_SPAWNING, true, false);

    setCol(2);
    addSectionHeader(cmd, "Treasury");
    addFacPermColumnHeader(cmd);
    addFacPermRow(cmd, events, permCfg, FactionPermissions.TREASURY_DEPOSIT, false, false);
    addFacPermRow(cmd, events, permCfg, FactionPermissions.TREASURY_WITHDRAW, false, false);
    addFacPermRow(cmd, events, permCfg, FactionPermissions.TREASURY_TRANSFER, false, false);
  }

  /** Adds a permission section for a specific level with Def/Lock column header. */
  private void addFacPermSection(UICommandBuilder cmd, UIEventBuilder events,
                                  FactionPermissionsConfig permCfg, String level) {
    String title = level.substring(0, 1).toUpperCase() + level.substring(1);
    addSectionHeader(cmd, title);
    addFacPermColumnHeader(cmd);
    for (String flag : FactionPermissions.getFlagsForLevel(level)) {
      boolean isChild = FactionPermissions.getParentFlag(flag) != null;
      boolean isParent = FactionPermissions.isParentFlag(flag);
      addFacPermRow(cmd, events, permCfg, flag, isChild, isParent);
    }
  }

  /** Adds a Def / Lock column header row. */
  private void addFacPermColumnHeader(UICommandBuilder cmd) {
    String containerId = getContainerId();
    cmd.append(containerId, UIPaths.ADMIN_CONFIG_FACPERM_HEADER);
    incrementRowIdx();
  }

  /** Gets a short display name for a faction permission flag (for the compact 4-col layout). */
  private static String getFacPermDisplayName(String flag) {
    // Non-level flags first — must check before level-prefix loop to avoid
    // "officer" matching as prefix of "officersCanEdit" → "sCanEdit"
    return switch (flag) {
      case "mobSpawning" -> "All Mobs";
      case "hostileMobSpawning" -> "Hostile";
      case "passiveMobSpawning" -> "Passive";
      case "neutralMobSpawning" -> "Neutral";
      case "pvpEnabled" -> "PvP";
      case "officersCanEdit" -> "Officers Edit";
      case "treasuryDeposit" -> "Deposit";
      case "treasuryWithdraw" -> "Withdraw";
      case "treasuryTransfer" -> "Transfer";
      default -> getFacPermLevelDisplayName(flag);
    };
  }

  /** Strips level prefix from a level-based flag to get the display suffix. */
  private static String getFacPermLevelDisplayName(String flag) {
    for (String level : FactionPermissions.ALL_LEVELS) {
      if (flag.startsWith(level)) {
        String suffix = flag.substring(level.length());
        return switch (suffix) {
          case "Break" -> "Break";
          case "Place" -> "Place";
          case "Interact" -> "Interact";
          case "DoorUse" -> "Door Use";
          case "ContainerUse" -> "Container";
          case "BenchUse" -> "Bench Use";
          case "ProcessingUse" -> "Processing";
          case "SeatUse" -> "Seat Use";
          case "TransportUse" -> "Transport";
          case "CrateUse" -> "Crate Use";
          case "NpcTame" -> "NPC Tame";
          case "PveDamage" -> "PvE Damage";
          default -> suffix;
        };
      }
    }
    return flag;
  }

  /** Adds a single faction permission row with Default and Lock checkboxes. */
  private void addFacPermRow(UICommandBuilder cmd, UIEventBuilder events,
                              FactionPermissionsConfig permCfg, String flag,
                              boolean isChild, boolean isParent) {
    String defaultKey = "facperm.default." + flag;
    String lockKey = "facperm.lock." + flag;
    boolean defaultPending = pendingChanges.containsKey(defaultKey);
    boolean lockPending = pendingChanges.containsKey(lockKey);
    boolean defaultVal = defaultPending ? (Boolean) pendingChanges.get(defaultKey) : permCfg.getDefault(flag);
    boolean lockVal = lockPending ? (Boolean) pendingChanges.get(lockKey) : permCfg.isPermissionLocked(flag);
    if (!defaultPending) originalValues.putIfAbsent(defaultKey, permCfg.getDefault(flag));
    if (!lockPending) originalValues.putIfAbsent(lockKey, permCfg.isPermissionLocked(flag));

    String containerId = getContainerId();
    cmd.append(containerId, isChild ? UIPaths.ADMIN_CONFIG_FACPERM_CHILD_ROW : UIPaths.ADMIN_CONFIG_FACPERM_ROW);
    String idx = containerId + "[" + getRowIdx() + "]";

    // Short display name for the compact 4-column layout
    String displayName = getFacPermDisplayName(flag);
    String parentFlag = FactionPermissions.getParentFlag(flag);

    String labelColor = (defaultPending || lockPending) ? "#FFAA00" : "#CCCCCC";
    cmd.set(idx + " #SettingLabel.Text", displayName);
    cmd.set(idx + " #SettingLabel.Style.TextColor", labelColor);

    // Default checkbox
    cmd.set(idx + " #DefaultToggle #CheckBox.Value", defaultVal);
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, idx + " #DefaultToggle #CheckBox",
        EventData.of("Button", "ToggleSetting").append("SettingKey", defaultKey), false);
    settingSelectors.put(defaultKey, idx);
    settingKinds.put(defaultKey, SettingKind.BOOL);

    // Lock checkbox
    cmd.set(idx + " #LockToggle #CheckBox.Value", lockVal);
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, idx + " #LockToggle #CheckBox",
        EventData.of("Button", "ToggleSetting").append("SettingKey", lockKey), false);
    settingSelectors.put(lockKey, idx);
    settingKinds.put(lockKey, SettingKind.BOOL);

    // Disable child Default checkboxes when parent Default is OFF, and force unchecked
    if (isChild && parentFlag != null) {
      String parentDefaultKey = "facperm.default." + parentFlag;
      boolean parentDefault = pendingChanges.containsKey(parentDefaultKey)
          ? (Boolean) pendingChanges.get(parentDefaultKey) : permCfg.getDefault(parentFlag);
      if (!parentDefault) {
        cmd.set(idx + " #DefaultToggle #CheckBox.Value", false);
        cmd.set(idx + " #DefaultToggle #CheckBox.Disabled", true);
      }
    }

    incrementRowIdx();
  }

  // ================================================================
  // Worldmap Tab
  // ================================================================

  private void buildWorldmapTab(UICommandBuilder cmd, UIEventBuilder events, ConfigManager cfg) {
    WorldMapConfig wm = cfg.worldMap();

    // Determine effective refresh mode (pending change or current)
    String effectiveMode = pendingChanges.containsKey("worldmap.refreshMode")
        ? String.valueOf(pendingChanges.get("worldmap.refreshMode"))
        : wm.getRefreshMode().getConfigName();

    setColumn(true);
    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_MAP_DISPLAY));
    addBooleanSetting(cmd, events, "worldmap.enabled", "World Map Markers", wm.isEnabled());
    addBooleanSetting(cmd, events, "worldmap.respectWorldConfig", "Respect World Config", wm.isRespectWorldConfig());
    addEnumSetting(cmd, events, "worldmap.betterMapCompat", "BetterMap Compat", wm.getBetterMapCompat(),
        "auto", "always", "never");
    addBooleanSetting(cmd, events, "worldmap.showFactionTags", "Show Faction Tags", wm.isShowFactionTags());
    addEnumSetting(cmd, events, "worldmap.refreshMode", "Refresh Mode", wm.getRefreshMode().getConfigName(),
        "proximity", "incremental", "debounced", "immediate", "manual");
    addBooleanSetting(cmd, events, "worldmap.autoFallbackOnError", "Auto Fallback", wm.isAutoFallbackOnError());
    addIntSetting(cmd, events, "worldmap.factionWideRefreshThreshold", "Refresh Threshold", wm.getFactionWideRefreshThreshold());

    // Conditional settings based on active refresh mode
    if ("proximity".equals(effectiveMode)) {
      addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_PROXIMITY));
      addIntSetting(cmd, events, "worldmap.proximityChunkRadius", "Chunk Radius", wm.getProximityChunkRadius());
      addIntSetting(cmd, events, "worldmap.proximityBatchIntervalTicks", "Batch Interval", wm.getProximityBatchIntervalTicks());
      addIntSetting(cmd, events, "worldmap.proximityMaxChunksPerBatch", "Max Chunks/Batch", wm.getProximityMaxChunksPerBatch());
    } else if ("incremental".equals(effectiveMode)) {
      addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_INCREMENTAL));
      addIntSetting(cmd, events, "worldmap.incrementalBatchIntervalTicks", "Batch Interval", wm.getIncrementalBatchIntervalTicks());
      addIntSetting(cmd, events, "worldmap.incrementalMaxChunksPerBatch", "Max Chunks/Batch", wm.getIncrementalMaxChunksPerBatch());
    } else if ("debounced".equals(effectiveMode)) {
      addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_DEBOUNCED));
      addIntSetting(cmd, events, "worldmap.debouncedDelaySeconds", "Delay (sec)", wm.getDebouncedDelaySeconds());
    }
    // "immediate" and "manual" have no extra settings

    setColumn(false);
    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_VISIBILITY));
    addBooleanSetting(cmd, events, "worldmap.playerVisibilityEnabled", "Player Visibility", wm.isPlayerVisibilityEnabled());
    addBooleanSetting(cmd, events, "worldmap.showOwnFaction", "Show Own Faction", wm.isShowOwnFaction());
    addBooleanSetting(cmd, events, "worldmap.showAllies", "Show Allies", wm.isShowAllies());
    addBooleanSetting(cmd, events, "worldmap.showNeutrals", "Show Neutrals", wm.isShowNeutrals());
    addBooleanSetting(cmd, events, "worldmap.showEnemies", "Show Enemies", wm.isShowEnemies());
    addBooleanSetting(cmd, events, "worldmap.showFactionlessPlayers", "Show Factionless", wm.isShowFactionlessPlayers());
    addBooleanSetting(cmd, events, "worldmap.showFactionlessToFactionless", "Show to Factionless", wm.isShowFactionlessToFactionless());
  }

  // ================================================================
  // Worlds Tab (info-only)
  // ================================================================

  private void buildWorldsTab(UICommandBuilder cmd, UIEventBuilder events, ConfigManager cfg) {
    WorldsConfig worlds = cfg.worlds();

    setColumn(true);
    addSectionHeader(cmd, "Global Policy");
    String effectivePolicy = pendingChanges.containsKey("worlds.defaultPolicy")
        ? String.valueOf(pendingChanges.get("worlds.defaultPolicy")) : worlds.getDefaultPolicy();
    originalValues.putIfAbsent("worlds.defaultPolicy", worlds.getDefaultPolicy());
    addEnumSetting(cmd, events, "worlds.defaultPolicy", "Default Policy", effectivePolicy, "allow", "deny");

    addSectionHeader(cmd, "Per-World Overrides");

    // Add world input row at the top
    String addWorldContainer = getContainerId();
    cmd.append(addWorldContainer, UIPaths.ADMIN_CONFIG_ADD_ROW);
    String addWorldIdx = addWorldContainer + "[" + getRowIdx() + "]";
    cmd.set(addWorldIdx + " #AddBtn.Text", "Add World");
    events.addEventBinding(CustomUIEventBindingType.Activating, addWorldIdx + " #AddBtn",
        EventData.of("Button", "AddWorld")
            .append("@strInput", addWorldIdx + " #AddInput.Value"), false);
    incrementRowIdx();

    // Spacer between add box and list
    addSectionHeader(cmd, "");

    // World entries
    Map<String, WorldsConfig.WorldSettings> worldMap = getEffectiveWorldOverrides(worlds);
    int worldIdx = 0;
    for (Map.Entry<String, WorldsConfig.WorldSettings> entry : worldMap.entrySet()) {
      String worldKey = entry.getKey();
      WorldsConfig.WorldSettings ws = entry.getValue();
      addWorldOverrideEntry(cmd, events, worldKey, ws, worldIdx);
      worldIdx++;
    }
  }

  private void addWorldOverrideEntry(UICommandBuilder cmd, UIEventBuilder events,
                                      String worldKey, WorldsConfig.WorldSettings ws, int worldIdx) {
    String containerId = getContainerId();
    cmd.append(containerId, UIPaths.ADMIN_CONFIG_WORLD_ENTRY);
    String idx = containerId + "[" + getRowIdx() + "]";
    cmd.set(idx + " #WorldName.Text", worldKey);

    events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #RemoveWorldBtn",
        EventData.of("Button", "RemoveWorld").append("SettingKey", worldKey), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #ExpandBtn",
        EventData.of("Button", "ToggleWorldExpand").append("SettingKey", worldKey), false);

    // Check if this world is expanded
    boolean expanded = expandedWorlds.contains(worldKey);
    if (expanded) {
      cmd.set(idx + " #ExpandBtn.Text", "Hide");
      // Add tri-state dropdowns for each setting
      String[] settings = { "claiming", "powerLoss", "friendlyFireFaction", "friendlyFireAlly" };
      String[] labels = { "Claiming", "Power Loss", "FF Faction", "FF Ally" };
      Boolean[] values = { ws.claiming(), ws.powerLoss(), ws.friendlyFireFaction(), ws.friendlyFireAlly() };

      for (int s = 0; s < settings.length; s++) {
        String settingKey = "worlds.override." + worldKey + "." + settings[s];
        String effectiveVal = triStateToString(values[s]);

        String settingsContainer = idx + " #WorldSettings";
        cmd.append(settingsContainer, UIPaths.ADMIN_CONFIG_TRISTATE_ROW);
        String settingIdx = settingsContainer + "[" + s + "]";
        cmd.set(settingIdx + " #SettingLabel.Text", labels[s]);
        cmd.set(settingIdx + " #TristateSelect.Entries",
            List.of(
                new DropdownEntryInfo(LocalizableString.fromString("Default"), "default"),
                new DropdownEntryInfo(LocalizableString.fromString("Allow"), "allow"),
                new DropdownEntryInfo(LocalizableString.fromString("Deny"), "deny")
            ));
        cmd.set(settingIdx + " #TristateSelect.Value", effectiveVal);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, settingIdx + " #TristateSelect",
            EventData.of("Button", "WorldSettingChanged").append("SettingKey", settingKey)
                .append("@enumValue", settingIdx + " #TristateSelect.Value"), false);
      }
    }

    incrementRowIdx();
  }

  private static String triStateToString(Boolean value) {
    if (value == null) return "default";
    return value ? "allow" : "deny";
  }

  private static Boolean triStateFromString(String value) {
    return switch (value) {
      case "allow" -> true;
      case "deny" -> false;
      default -> null;
    };
  }

  /** Returns the effective world overrides map (pending or from config). */
  private Map<String, WorldsConfig.WorldSettings> getEffectiveWorldOverrides(WorldsConfig worlds) {
    if (pendingWorldOverrides != null) {
      return pendingWorldOverrides;
    }
    return worlds.getWorlds();
  }

  /** Lazily initializes pendingWorldOverrides from config if not yet done. */
  private LinkedHashMap<String, WorldsConfig.WorldSettings> ensurePendingWorldOverrides() {
    if (pendingWorldOverrides == null) {
      pendingWorldOverrides = new LinkedHashMap<>(ConfigManager.get().worlds().getWorlds());
    }
    return pendingWorldOverrides;
  }

  // ================================================================
  // Backup Tab
  // ================================================================

  private void buildBackupTab(UICommandBuilder cmd, UIEventBuilder events, ConfigManager cfg) {
    setColumn(true);
    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_BACKUP));
    addBooleanSetting(cmd, events, "backup.enabled", "Backup Enabled", cfg.isBackupEnabled());
    addIntSetting(cmd, events, "backup.hourlyRetention", "Hourly Retention", cfg.getBackupHourlyRetention());
    addIntSetting(cmd, events, "backup.dailyRetention", "Daily Retention", cfg.getBackupDailyRetention());
    addIntSetting(cmd, events, "backup.weeklyRetention", "Weekly Retention", cfg.getBackupWeeklyRetention());
    addIntSetting(cmd, events, "backup.manualRetention", "Manual Retention", cfg.getBackupManualRetention());
    addBooleanSetting(cmd, events, "backup.onShutdown", "Backup on Shutdown", cfg.isBackupOnShutdown());
    addIntSetting(cmd, events, "backup.shutdownRetention", "Shutdown Retention", cfg.getBackupShutdownRetention());
  }

  // ================================================================
  // Debug Tab
  // ================================================================

  private void buildDebugTab(UICommandBuilder cmd, UIEventBuilder events, ConfigManager cfg) {
    DebugConfig dbg = cfg.debug();
    setColumn(true);
    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_DEBUG_GLOBAL));
    addBooleanSetting(cmd, events, "debug.enabledByDefault", "Enabled by Default", dbg.isEnabledByDefault());
    addBooleanSetting(cmd, events, "debug.logToConsole", "Log to Console", dbg.isLogToConsole());

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_SENTRY));
    addBooleanSetting(cmd, events, "debug.sentryEnabled", "Sentry Enabled", dbg.isSentryEnabled());
    addBooleanSetting(cmd, events, "debug.sentryDebug", "Sentry Debug", dbg.isSentryDebug());
    addDoubleSetting(cmd, events, "debug.sentryTracesSampleRate", "Traces Sample Rate", dbg.getSentryTracesSampleRate());

    setColumn(false);
    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_DEBUG_CATEGORIES));
    addBooleanSetting(cmd, events, "debug.power", "Power", dbg.isPower());
    addBooleanSetting(cmd, events, "debug.claim", "Claim", dbg.isClaim());
    addBooleanSetting(cmd, events, "debug.combat", "Combat", dbg.isCombat());
    addBooleanSetting(cmd, events, "debug.protection", "Protection", dbg.isProtection());
    addBooleanSetting(cmd, events, "debug.relation", "Relation", dbg.isRelation());
    addBooleanSetting(cmd, events, "debug.territory", "Territory", dbg.isTerritory());
    addBooleanSetting(cmd, events, "debug.worldmap", "World Map", dbg.isWorldmap());
    addBooleanSetting(cmd, events, "debug.interaction", "Interaction", dbg.isInteraction());
    addBooleanSetting(cmd, events, "debug.mixin", "Mixin", dbg.isMixin());
    addBooleanSetting(cmd, events, "debug.spawning", "Spawning", dbg.isSpawning());
    addBooleanSetting(cmd, events, "debug.integration", "Integration", dbg.isIntegration());
    addBooleanSetting(cmd, events, "debug.economy", "Economy", dbg.isEconomy());
  }

  // ================================================================
  // Gravestones Tab
  // ================================================================

  private void buildGravestonesTab(UICommandBuilder cmd, UIEventBuilder events, ConfigManager cfg) {
    GravestoneConfig gs = cfg.gravestones();
    setColumn(true);
    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_GRAVESTONE_PROTECTION));
    addBooleanSetting(cmd, events, "gravestone.protectInOwnTerritory", "Protect Own Territory", gs.isProtectInOwnTerritory());
    addBooleanSetting(cmd, events, "gravestone.protectInSafeZone", "Protect Safe Zone", gs.isProtectInSafeZone());
    addBooleanSetting(cmd, events, "gravestone.protectInWarZone", "Protect War Zone", gs.isProtectInWarZone());
    addBooleanSetting(cmd, events, "gravestone.protectInWilderness", "Protect Wilderness", gs.isProtectInWilderness());
    addBooleanSetting(cmd, events, "gravestone.protectInEnemyTerritory", "Protect Enemy Land", gs.isProtectInEnemyTerritory());
    addBooleanSetting(cmd, events, "gravestone.protectInNeutralTerritory", "Protect Neutral Land", gs.isProtectInNeutralTerritory());

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_GRAVESTONE_ACCESS));
    addBooleanSetting(cmd, events, "gravestone.factionMembersCanAccess", "Members Can Access", gs.isFactionMembersCanAccess());
    addBooleanSetting(cmd, events, "gravestone.alliesCanAccess", "Allies Can Access", gs.isAlliesCanAccess());
    addBooleanSetting(cmd, events, "gravestone.enemiesCanLootInOwnTerritory", "Enemies Loot Own", gs.isEnemiesCanLootInOwnTerritory());
    addBooleanSetting(cmd, events, "gravestone.announceDeathLocation", "Announce Death Loc", gs.isAnnounceDeathLocation());

    addSectionHeader(cmd, loc(AdminGuiKeys.AdminGui.CFG_SEC_GRAVESTONE_LOOT));
    addBooleanSetting(cmd, events, "gravestone.allowLootDuringRaid", "Allow During Raid", gs.isAllowLootDuringRaid());
    addBooleanSetting(cmd, events, "gravestone.allowLootDuringWar", "Allow During War", gs.isAllowLootDuringWar());
  }

  // ================================================================
  // Setting Row Builders
  // ================================================================

  private void addSectionHeader(UICommandBuilder cmd, String label) {
    String containerId = getContainerId();
    cmd.append(containerId, UIPaths.ADMIN_CONFIG_SECTION);
    cmd.set(containerId + "[" + getRowIdx() + "] #SectionTitle.Text", label);
    incrementRowIdx();
  }

  private void addBooleanSetting(UICommandBuilder cmd, UIEventBuilder events,
                                  String key, String label, boolean value) {
    boolean pending = pendingChanges.containsKey(key);
    boolean effectiveValue = pending ? (Boolean) pendingChanges.get(key) : value;
    if (!pending) originalValues.putIfAbsent(key, value);
    String color = pending ? "#FFAA00" : "#CCCCCC";

    String containerId = getContainerId();
    cmd.append(containerId, UIPaths.ADMIN_CONFIG_BOOL_ROW);
    String idx = containerId + "[" + getRowIdx() + "]";
    cmd.set(idx + " #SettingLabel.Text", label);
    cmd.set(idx + " #SettingLabel.Style.TextColor", color);
    cmd.set(idx + " #BoolToggle #CheckBox.Value", effectiveValue);
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, idx + " #BoolToggle #CheckBox",
        EventData.of("Button", "ToggleSetting").append("SettingKey", key), false);
    settingSelectors.put(key, idx);
    settingKinds.put(key, SettingKind.BOOL);
    incrementRowIdx();
  }

  private void addIntSetting(UICommandBuilder cmd, UIEventBuilder events,
                              String key, String label, int value) {
    boolean pending = pendingChanges.containsKey(key);
    int effectiveValue = pending ? ((Number) pendingChanges.get(key)).intValue() : value;
    if (!pending) originalValues.putIfAbsent(key, value);
    String color = pending ? "#FFAA00" : "#CCCCCC";

    String containerId = getContainerId();
    cmd.append(containerId, UIPaths.ADMIN_CONFIG_NUM_ROW);
    String idx = containerId + "[" + getRowIdx() + "]";
    cmd.set(idx + " #SettingLabel.Text", label);
    cmd.set(idx + " #SettingLabel.Style.TextColor", color);
    cmd.set(idx + " #NumInput.Value", String.valueOf(effectiveValue));
    if (pending) cmd.set(idx + " #NumInput.Style.TextColor", "#FFAA00");
    events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #DecBtn",
        EventData.of("Button", "DecrementSetting").append("SettingKey", key), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #IncBtn",
        EventData.of("Button", "IncrementSetting").append("SettingKey", key), false);
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, idx + " #NumInput",
        EventData.of("Button", "SetNumericValue").append("SettingKey", key)
            .append("@numInput", idx + " #NumInput.Value"), false);
    settingSelectors.put(key, idx);
    settingKinds.put(key, SettingKind.INT);
    incrementRowIdx();
  }

  private void addDoubleSetting(UICommandBuilder cmd, UIEventBuilder events,
                                 String key, String label, double value) {
    boolean pending = pendingChanges.containsKey(key);
    double effectiveValue = pending ? ((Number) pendingChanges.get(key)).doubleValue() : value;
    if (!pending) originalValues.putIfAbsent(key, value);
    String color = pending ? "#FFAA00" : "#CCCCCC";

    String containerId = getContainerId();
    cmd.append(containerId, UIPaths.ADMIN_CONFIG_NUM_ROW);
    String idx = containerId + "[" + getRowIdx() + "]";
    cmd.set(idx + " #SettingLabel.Text", label);
    cmd.set(idx + " #SettingLabel.Style.TextColor", color);
    cmd.set(idx + " #NumInput.Value", String.format("%.2f", effectiveValue));
    if (pending) cmd.set(idx + " #NumInput.Style.TextColor", "#FFAA00");
    events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #DecBtn",
        EventData.of("Button", "DecrementSetting").append("SettingKey", key), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #IncBtn",
        EventData.of("Button", "IncrementSetting").append("SettingKey", key), false);
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, idx + " #NumInput",
        EventData.of("Button", "SetNumericValue").append("SettingKey", key)
            .append("@numInput", idx + " #NumInput.Value"), false);
    settingSelectors.put(key, idx);
    settingKinds.put(key, SettingKind.DOUBLE);
    incrementRowIdx();
  }

  private void addStringSetting(UICommandBuilder cmd, UIEventBuilder events,
                                 String key, String label, String value) {
    boolean pending = pendingChanges.containsKey(key);
    String effectiveValue = pending ? String.valueOf(pendingChanges.get(key)) : value;
    if (!pending) originalValues.putIfAbsent(key, value);
    String color = pending ? "#FFAA00" : "#CCCCCC";

    String containerId = getContainerId();
    cmd.append(containerId, UIPaths.ADMIN_CONFIG_STR_ROW);
    String idx = containerId + "[" + getRowIdx() + "]";
    cmd.set(idx + " #SettingLabel.Text", label);
    cmd.set(idx + " #SettingLabel.Style.TextColor", color);
    cmd.set(idx + " #StrInput.Value", effectiveValue);
    if (pending) cmd.set(idx + " #StrInput.Style.TextColor", "#FFAA00");
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, idx + " #StrInput",
        EventData.of("Button", "SetStringValue").append("SettingKey", key)
            .append("@strInput", idx + " #StrInput.Value"), false);
    settingSelectors.put(key, idx);
    settingKinds.put(key, SettingKind.STRING);
    incrementRowIdx();
  }

  private void addColorSetting(UICommandBuilder cmd, UIEventBuilder events,
                                 String key, String label, String value) {
    boolean pending = pendingChanges.containsKey(key);
    String effectiveValue = pending ? String.valueOf(pendingChanges.get(key)) : value;
    if (!pending) originalValues.putIfAbsent(key, value);
    String color = pending ? "#FFAA00" : "#CCCCCC";

    String containerId = getContainerId();
    cmd.append(containerId, UIPaths.ADMIN_CONFIG_COLOR_ROW);
    String idx = containerId + "[" + getRowIdx() + "]";
    cmd.set(idx + " #SettingLabel.Text", label);
    cmd.set(idx + " #SettingLabel.Style.TextColor", color);
    cmd.set(idx + " #ColorPicker.Color", effectiveValue);
    cmd.set(idx + " #ColorInput.Value", effectiveValue);
    if (pending) cmd.set(idx + " #ColorInput.Style.TextColor", "#FFAA00");
    // Set button: reads the picker's current Color
    events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #ApplyColorBtn",
        EventData.of("Button", "SetColorValue").append("SettingKey", key)
            .append("@colorValue", idx + " #ColorPicker.Color"), false);
    // Text field: type a hex color manually (debounced)
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, idx + " #ColorInput",
        EventData.of("Button", "TypeColorValue").append("SettingKey", key)
            .append("@strInput", idx + " #ColorInput.Value"), false);
    settingSelectors.put(key, idx);
    settingKinds.put(key, SettingKind.COLOR);
    incrementRowIdx();
  }

  private void addEnumSetting(UICommandBuilder cmd, UIEventBuilder events,
                               String key, String label, String value, String... options) {
    boolean pending = pendingChanges.containsKey(key);
    String effectiveValue = pending ? String.valueOf(pendingChanges.get(key)) : value;
    if (!pending) originalValues.putIfAbsent(key, value);
    String color = pending ? "#FFAA00" : "#CCCCCC";

    String containerId = getContainerId();
    cmd.append(containerId, UIPaths.ADMIN_CONFIG_ENUM_ROW);
    String idx = containerId + "[" + getRowIdx() + "]";
    cmd.set(idx + " #SettingLabel.Text", label);
    cmd.set(idx + " #SettingLabel.Style.TextColor", color);
    cmd.set(idx + " #EnumSelect.Entries",
        java.util.Arrays.stream(options)
            .map(o -> new DropdownEntryInfo(LocalizableString.fromString(o), o))
            .toList());
    cmd.set(idx + " #EnumSelect.Value", effectiveValue);
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, idx + " #EnumSelect",
        EventData.of("Button", "EnumChanged").append("SettingKey", key)
            .append("@enumValue", idx + " #EnumSelect.Value"), false);
    settingSelectors.put(key, idx);
    settingKinds.put(key, SettingKind.ENUM);
    incrementRowIdx();
  }

  private static final List<String> AVAILABLE_LOCALES = List.of(
      "en-US", "es-ES", "de-DE", "fr-FR", "pt-BR",
      "ru-RU", "pl-PL", "it-IT", "nl-NL", "tl-PH"
  );

  private static String nativeDisplayName(String localeCode) {
    Locale locale = Locale.forLanguageTag(localeCode);
    String lang = locale.getDisplayLanguage(locale);
    if (!lang.isEmpty()) {
      lang = Character.toUpperCase(lang.charAt(0)) + lang.substring(1);
    }
    String country = locale.getCountry();
    return country.isEmpty() ? lang : lang + " (" + country + ")";
  }

  private void addLocaleSetting(UICommandBuilder cmd, UIEventBuilder events,
                                 String key, String label, String value) {
    boolean pending = pendingChanges.containsKey(key);
    String effectiveValue = pending ? String.valueOf(pendingChanges.get(key)) : value;
    if (!pending) originalValues.putIfAbsent(key, value);
    String color = pending ? "#FFAA00" : "#CCCCCC";

    String containerId = getContainerId();
    cmd.append(containerId, UIPaths.ADMIN_CONFIG_ENUM_ROW);
    String idx = containerId + "[" + getRowIdx() + "]";
    cmd.set(idx + " #SettingLabel.Text", label);
    cmd.set(idx + " #SettingLabel.Style.TextColor", color);
    cmd.set(idx + " #EnumSelect.Entries",
        AVAILABLE_LOCALES.stream()
            .map(code -> new DropdownEntryInfo(LocalizableString.fromString(nativeDisplayName(code)), code))
            .toList());
    cmd.set(idx + " #EnumSelect.Value", effectiveValue);
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, idx + " #EnumSelect",
        EventData.of("Button", "EnumChanged").append("SettingKey", key)
            .append("@enumValue", idx + " #EnumSelect.Value"), false);
    settingSelectors.put(key, idx);
    settingKinds.put(key, SettingKind.ENUM);
    incrementRowIdx();
  }

  private void addWideStringSetting(UICommandBuilder cmd, UIEventBuilder events,
                                     String key, String label, String value) {
    boolean pending = pendingChanges.containsKey(key);
    String effectiveValue = pending ? String.valueOf(pendingChanges.get(key)) : value;
    if (!pending) originalValues.putIfAbsent(key, value);
    String color = pending ? "#FFAA00" : "#CCCCCC";

    String containerId = getContainerId();
    cmd.append(containerId, UIPaths.ADMIN_CONFIG_STR_WIDE_ROW);
    String idx = containerId + "[" + getRowIdx() + "]";
    cmd.set(idx + " #SettingLabel.Text", label);
    cmd.set(idx + " #SettingLabel.Style.TextColor", color);
    cmd.set(idx + " #StrInput.Value", effectiveValue);
    if (pending) cmd.set(idx + " #StrInput.Style.TextColor", "#FFAA00");
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, idx + " #StrInput",
        EventData.of("Button", "SetStringValue").append("SettingKey", key)
            .append("@strInput", idx + " #StrInput.Value"), false);
    settingSelectors.put(key, idx);
    settingKinds.put(key, SettingKind.STRING);
    incrementRowIdx();
  }

  private void addActionButton(UICommandBuilder cmd, UIEventBuilder events,
                                String action, String label, boolean disabled) {
    String containerId = getContainerId();
    cmd.append(containerId, UIPaths.ADMIN_CONFIG_ACTION_BTN);
    String idx = containerId + "[" + getRowIdx() + "]";
    cmd.set(idx + " #ActionBtn.Text", label);
    if (disabled) {
      cmd.set(idx + " #ActionBtn.Disabled", true);
    }
    events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #ActionBtn",
        EventData.of("Button", action), false);
    incrementRowIdx();
  }

  // ================================================================
  // Event Handling
  // ================================================================

  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                               AdminConfigData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null) return;

    if (AdminNavBarHelper.handleNavEvent(data, player, ref, store, playerRef, guiManager)) {
      return;
    }

    if (data.button != null) {
      switch (data.button) {
        case "Back" -> guiManager.closePage(player, ref, store);

        case "TabSwitch" -> {
          if (data.tab != null) {
            LayoutSize oldSize = getLayoutSize(currentTab);
            currentTab = data.tab;
            saveConfirmActive = false;
            resetConfirmActive = false;
            if (getLayoutSize(currentTab) != oldSize) {
              rebuild();
            } else {
              refresh(ref, store);
            }
          }
        }

        case "ToggleSetting" -> {
          if (data.settingKey != null) {
            handleToggle(data.settingKey);
            // If toggling a parent facperm default, refresh to rebuild child disabled states
            if (data.settingKey.startsWith("facperm.default.")) {
              String flag = data.settingKey.substring("facperm.default.".length());
              if (FactionPermissions.isParentFlag(flag)) {
                refresh(ref, store);
                break;
              }
            }
            updateSettingAndStatus(ref, store, data.settingKey);
          }
        }

        case "IncrementSetting" -> {
          if (data.settingKey != null) {
            handleIncrement(data.settingKey, true);
            updateSettingAndStatus(ref, store, data.settingKey);
          }
        }

        case "DecrementSetting" -> {
          if (data.settingKey != null) {
            handleIncrement(data.settingKey, false);
            updateSettingAndStatus(ref, store, data.settingKey);
          }
        }

        case "SetNumericValue" -> {
          if (data.settingKey != null && data.numInput != null) {
            handleNumericInput(data.settingKey, data.numInput);
            debouncedStatusUpdate(ref, store, data.settingKey);
          }
        }

        case "SetStringValue" -> {
          if (data.settingKey != null && data.strInput != null) {
            handleStringInput(data.settingKey, data.strInput);
            debouncedStatusUpdate(ref, store, data.settingKey);
          }
        }

        case "SetColorValue" -> {
          if (data.settingKey != null && data.colorValue != null) {
            handleColorInput(data.settingKey, data.colorValue);
            updateSettingAndStatus(ref, store, data.settingKey);
          }
        }

        case "TypeColorValue" -> {
          if (data.settingKey != null && data.strInput != null) {
            handleColorInput(data.settingKey, data.strInput);
            debouncedStatusUpdate(ref, store, data.settingKey);
          }
        }

        case "EnumChanged" -> {
          if (data.settingKey != null && data.enumValue != null) {
            handleEnumInput(data.settingKey, data.enumValue);
            // Worldmap refresh mode and scaling mode changes rebuild the tab
            if ("worldmap.refreshMode".equals(data.settingKey)
                || "economy.upkeepScalingMode".equals(data.settingKey)) {
              refresh(ref, store);
            } else {
              updateSettingAndStatus(ref, store, data.settingKey);
            }
          }
        }

        case "EditScalingTiers" -> {
          ScalingTiersModalPage modal = new ScalingTiersModalPage(playerRef, guiManager, this);
          player.getPageManager().openCustomPage(ref, store, modal);
        }

        case "RemoveWorld" -> {
          if (data.settingKey != null) {
            handleRemoveWorld(data.settingKey);
            refresh(ref, store);
          }
        }

        case "AddWorld" -> {
          if (data.strInput != null && !data.strInput.trim().isEmpty()) {
            handleAddWorld(data.strInput.trim());
            refresh(ref, store);
          }
        }

        case "ToggleWorldExpand" -> {
          if (data.settingKey != null) {
            if (expandedWorlds.contains(data.settingKey)) {
              expandedWorlds.remove(data.settingKey);
            } else {
              expandedWorlds.add(data.settingKey);
            }
            refresh(ref, store);
          }
        }

        case "WorldSettingChanged" -> {
          if (data.settingKey != null && data.enumValue != null) {
            handleWorldSettingChanged(data.settingKey, data.enumValue);
            refresh(ref, store);
          }
        }

        case "Save" -> {
          if (!invalidFields.isEmpty()) {
            // Can't save with invalid fields
            break;
          }
          if (!saveConfirmActive) {
            saveConfirmActive = true;
            resetConfirmActive = false;
            refresh(ref, store);
          } else {
            applyAndSave();
            saveConfirmActive = false;
            resetConfirmActive = false;
            editSessions.remove(playerRef.getUuid());
            refresh(ref, store);
          }
        }

        case "Revert" -> {
          pendingChanges.clear();
          invalidFields.clear();
          pendingWorldOverrides = null;
          expandedWorlds.clear();
          saveConfirmActive = false;
          resetConfirmActive = false;
          editSessions.remove(playerRef.getUuid());
          refresh(ref, store);
        }

        case "ResetDefaults" -> {
          if (!resetConfirmActive) {
            resetConfirmActive = true;
            refresh(ref, store);
          } else {
            ConfigManager.get().resetAllDefaults();
            guiManager.getPlugin().get().reloadRuntimeSystems();
            pendingChanges.clear();
            originalValues.clear();
            pendingWorldOverrides = null;
            expandedWorlds.clear();
            resetConfirmActive = false;
            editSessions.remove(playerRef.getUuid());
            refresh(ref, store);
          }
        }

        default -> { }
      }
    }
  }

  private void handleToggle(String key) {
    Object orig = originalValues.get(key);
    boolean current;
    if (pendingChanges.containsKey(key)) {
      current = (Boolean) pendingChanges.get(key);
    } else if (orig instanceof Boolean b) {
      current = b;
    } else {
      return;
    }
    boolean newVal = !current;
    if (orig instanceof Boolean b && newVal == b) {
      pendingChanges.remove(key);
    } else {
      pendingChanges.put(key, newVal);
    }
  }

  private void handleIncrement(String key, boolean increment) {
    Object orig = originalValues.get(key);
    if (orig instanceof Integer origInt) {
      int step = ConfigSnapshot.getIntStep(key);
      int current = pendingChanges.containsKey(key) ? ((Number) pendingChanges.get(key)).intValue() : origInt;
      int newVal = increment ? current + step : current - step;
      newVal = Math.max(ConfigValidator.getIntMin(key), Math.min(ConfigValidator.getIntMax(key), newVal));
      if (newVal == origInt) {
        pendingChanges.remove(key);
      } else {
        pendingChanges.put(key, newVal);
      }
    } else if (orig instanceof Double origDbl) {
      double step = ConfigSnapshot.getDoubleStep(key);
      double current = pendingChanges.containsKey(key) ? ((Number) pendingChanges.get(key)).doubleValue() : origDbl;
      double newVal = increment ? current + step : current - step;
      newVal = Math.max(ConfigValidator.getDoubleMin(key), Math.min(ConfigValidator.getDoubleMax(key), newVal));
      newVal = Math.round(newVal * 100.0) / 100.0;
      if (Math.abs(newVal - origDbl) < 0.001) {
        pendingChanges.remove(key);
      } else {
        pendingChanges.put(key, newVal);
      }
    }
  }

  private void handleNumericInput(String key, String input) {
    Object orig = originalValues.get(key);
    if (input == null || input.isBlank()) {
      invalidFields.remove(key);
      return;
    }
    if (orig instanceof Integer origInt) {
      if (!isValidInt(input)) {
        invalidFields.add(key);
        return;
      }
      invalidFields.remove(key);
      int newVal = ConfigValidator.clampInt(input, origInt,
          ConfigValidator.getIntMin(key), ConfigValidator.getIntMax(key));
      if (newVal == origInt) pendingChanges.remove(key);
      else pendingChanges.put(key, newVal);
    } else if (orig instanceof Double origDbl) {
      if (!isValidDouble(input)) {
        invalidFields.add(key);
        return;
      }
      invalidFields.remove(key);
      double newVal = ConfigValidator.clampDouble(input, origDbl,
          ConfigValidator.getDoubleMin(key), ConfigValidator.getDoubleMax(key));
      if (Math.abs(newVal - origDbl) < 0.001) pendingChanges.remove(key);
      else pendingChanges.put(key, newVal);
    }
  }

  private static boolean isValidInt(String input) {
    try { Integer.parseInt(input.trim()); return true; }
    catch (NumberFormatException e) { return false; }
  }

  private static boolean isValidDouble(String input) {
    try {
      double v = Double.parseDouble(input.trim());
      return !Double.isNaN(v) && !Double.isInfinite(v);
    } catch (NumberFormatException e) { return false; }
  }

  private void handleStringInput(String key, String input) {
    Object orig = originalValues.get(key);
    String validated = ConfigValidator.validateString(input, 256);
    if (orig instanceof String origStr && validated.equals(origStr)) {
      pendingChanges.remove(key);
    } else {
      pendingChanges.put(key, validated);
    }
  }

  private void handleColorInput(String key, String rawColor) {
    Object orig = originalValues.get(key);
    String origStr = orig instanceof String s ? s : "#FFFFFF";
    // ColorPicker returns #RRGGBBAA — strip alpha to get #RRGGBB
    String hex = rawColor != null && rawColor.length() >= 7
        ? rawColor.substring(0, 7).toUpperCase() : rawColor;
    if (hex != null && !hex.isBlank() && !hex.matches("#[0-9A-Fa-f]{6}")) {
      invalidFields.add(key);
      return;
    }
    invalidFields.remove(key);
    String validated = ConfigValidator.validateColor(hex, origStr);
    if (validated.equals(origStr)) {
      pendingChanges.remove(key);
    } else {
      pendingChanges.put(key, validated);
    }
  }

  private void handleEnumInput(String key, String value) {
    Object orig = originalValues.get(key);
    if (orig instanceof String origStr && value.equals(origStr)) {
      pendingChanges.remove(key);
    } else {
      pendingChanges.put(key, value);
    }
  }

  private void handleRemoveWorld(String worldKey) {
    LinkedHashMap<String, WorldsConfig.WorldSettings> overrides = ensurePendingWorldOverrides();
    overrides.remove(worldKey);
    // Remove any pending per-setting overrides for this world
    pendingChanges.keySet().removeIf(k -> k.startsWith("worlds.override." + worldKey + "."));
    expandedWorlds.remove(worldKey);
  }

  private void handleAddWorld(String worldName) {
    LinkedHashMap<String, WorldsConfig.WorldSettings> overrides = ensurePendingWorldOverrides();
    if (!overrides.containsKey(worldName)) {
      overrides.put(worldName, WorldsConfig.WorldSettings.DEFAULTS);
    }
  }

  private void handleWorldSettingChanged(String key, String value) {
    // key format: worlds.override.{worldName}.{setting}
    String remainder = key.substring("worlds.override.".length());
    int dot = remainder.lastIndexOf('.');
    if (dot <= 0) return;
    String worldKey = remainder.substring(0, dot);
    String setting = remainder.substring(dot + 1);

    LinkedHashMap<String, WorldsConfig.WorldSettings> overrides = ensurePendingWorldOverrides();
    WorldsConfig.WorldSettings current = overrides.getOrDefault(worldKey, WorldsConfig.WorldSettings.DEFAULTS);
    Boolean val = triStateFromString(value);
    WorldsConfig.WorldSettings updated = switch (setting) {
      case "claiming" -> new WorldsConfig.WorldSettings(val, current.powerLoss(), current.friendlyFireFaction(), current.friendlyFireAlly());
      case "powerLoss" -> new WorldsConfig.WorldSettings(current.claiming(), val, current.friendlyFireFaction(), current.friendlyFireAlly());
      case "friendlyFireFaction" -> new WorldsConfig.WorldSettings(current.claiming(), current.powerLoss(), val, current.friendlyFireAlly());
      case "friendlyFireAlly" -> new WorldsConfig.WorldSettings(current.claiming(), current.powerLoss(), current.friendlyFireFaction(), val);
      default -> current;
    };
    overrides.put(worldKey, updated);
  }

  /**
   * Debounced status update for text input fields (numeric, string, color).
   * Updates only the label color + status bar after the user stops typing.
   */
  private void debouncedStatusUpdate(Ref<EntityStore> ref, Store<EntityStore> store, String key) {
    long ts = System.nanoTime();
    lastTextInputNanos = ts;
    final var capturedRef = ref;
    final var capturedStore = store;
    final var capturedKey = key;
    CompletableFuture.delayedExecutor(DEBOUNCE_MS, TimeUnit.MILLISECONDS)
        .execute(() -> {
          if (lastTextInputNanos == ts) {
            // Only update label color + status, not the input value
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder events = new UIEventBuilder();
            String selector = settingSelectors.get(capturedKey);
            if (selector != null) {
              boolean invalid = invalidFields.contains(capturedKey);
              boolean pending = pendingChanges.containsKey(capturedKey);
              String labelColor = invalid ? "#FF4444" : (pending ? "#FFAA00" : "#CCCCCC");
              cmd.set(selector + " #SettingLabel.Style.TextColor", labelColor);

              SettingKind kind = settingKinds.get(capturedKey);
              // Show red on the input field itself if invalid
              if (invalid) {
                if (kind == SettingKind.INT || kind == SettingKind.DOUBLE) {
                  cmd.set(selector + " #NumInput.Style.TextColor", "#FF4444");
                } else if (kind == SettingKind.COLOR) {
                  cmd.set(selector + " #ColorInput.Style.TextColor", "#FF4444");
                }
              }
              // Update color picker preview when typing a valid hex color
              if (kind == SettingKind.COLOR && pending && !invalid) {
                String colorVal = String.valueOf(pendingChanges.get(capturedKey));
                cmd.set(selector + " #ColorPicker.Color", colorVal);
              }
            }
            updateStatusLabel(cmd);
            sendUpdate(cmd, events, false);
          }
        });
  }

  @SuppressWarnings("unchecked")
  private void applyAndSave() {
    ConfigManager cfg = ConfigManager.get();

    for (var entry : pendingChanges.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();

      if ("worlds.defaultPolicy".equals(key)) {
        cfg.worlds().setDefaultPolicy(String.valueOf(value));
      } else {
        ConfigSnapshot.applyChange(key, value);
      }
    }
    // Apply pending world overrides (add/remove worlds)
    if (pendingWorldOverrides != null) {
      WorldsConfig worldsCfg = cfg.worlds();
      // Clear existing and replace with pending
      for (String key : new ArrayList<>(worldsCfg.getWorlds().keySet())) {
        worldsCfg.removeWorldSettings(key);
      }
      for (var entry : pendingWorldOverrides.entrySet()) {
        worldsCfg.setWorldSettings(entry.getKey(), entry.getValue());
      }
      pendingWorldOverrides = null;
    }

    cfg.saveAll();

    // Restart interval-based systems to pick up changed values immediately
    guiManager.getPlugin().get().reloadRuntimeSystems();

    for (var entry : pendingChanges.entrySet()) {
      originalValues.put(entry.getKey(), entry.getValue());
    }
    pendingChanges.clear();
    Logger.info("[ConfigEditor] Config changes saved and runtime systems reloaded");
  }

  /** Full page refresh — used for tab switches, save, revert, reset. */
  private void refresh(Ref<EntityStore> ref, Store<EntityStore> store) {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildDynamicContent(cmd, events);
    sendUpdate(cmd, events, false);
  }

  // ================================================================
  // Utilities
  // ================================================================

  private String loc(String key) {
    return HFMessages.get(playerRef, key);
  }

  private static String escUi(String text) {
    if (text == null) return "";
    return text.replace("\"", "'").replace("\\", "");
  }

  @Override
  public void onDismiss(Ref<EntityStore> ref, Store<EntityStore> store) {
    super.onDismiss(ref, store);
    saveSession();
  }
}
