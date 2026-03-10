package com.hyperfactions.gui.shared.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.faction.NavBarHelper;
import com.hyperfactions.gui.newplayer.NewPlayerNavBarHelper;
import com.hyperfactions.gui.shared.data.PlayerSettingsData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.storage.PlayerStorage;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.Logger;
import com.hyperfactions.util.MessageKeys;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Player Settings page for personal preferences.
 * Allows players to configure language and notification preferences.
 * Works for both faction members and players without a faction.
 */
public class PlayerSettingsPage extends InteractiveCustomUIPage<PlayerSettingsData> {

  private static final String PAGE_ID = "player_settings";

  /** Available locale codes. New locales are added here as translations are completed. */
  private static final List<String> AVAILABLE_LOCALES = List.of(
      "en-US"
  );

  /** Display names for available locales (parallel to AVAILABLE_LOCALES). */
  private static final List<String> LOCALE_DISPLAY_NAMES = List.of(
      "English (US)"
  );

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final PlayerStorage playerStorage;

  private final GuiManager guiManager;

  private final Faction faction;

  // Cached preferences (loaded from player data)
  private boolean territoryAlerts = true;

  private boolean deathAnnouncements = true;

  private boolean powerNotifications = true;

  private String languagePreference; // null = auto-detect

  /** Creates a new PlayerSettingsPage. */
  public PlayerSettingsPage(@NotNull PlayerRef playerRef,
               @NotNull FactionManager factionManager,
               @NotNull PlayerStorage playerStorage,
               @NotNull GuiManager guiManager) {
    super(playerRef, CustomPageLifetime.CanDismiss, PlayerSettingsData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.playerStorage = playerStorage;
    this.guiManager = guiManager;
    this.faction = factionManager.getPlayerFaction(playerRef.getUuid());

    // Load current preferences
    loadPreferences();
  }

  private void loadPreferences() {
    playerStorage.loadPlayerData(playerRef.getUuid()).thenAccept(opt -> {
      opt.ifPresent(data -> {
        this.territoryAlerts = data.isTerritoryAlertsEnabled();
        this.deathAnnouncements = data.isDeathAnnouncementsEnabled();
        this.powerNotifications = data.isPowerNotificationsEnabled();
        this.languagePreference = data.getLanguagePreference();
      });
    });
  }

  /** Builds the page. */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    // Load the template
    cmd.append(UIPaths.PLAYER_SETTINGS);

    // Setup nav bar based on faction status
    if (faction != null) {
      NavBarHelper.setupBar(playerRef, faction, PAGE_ID, cmd, events);
    } else {
      NewPlayerNavBarHelper.setupBar(playerRef, PAGE_ID, cmd, events);
    }

    // Page title
    cmd.set("#PageTitle.Text", HFMessages.get(playerRef, MessageKeys.PlayerSettings.TITLE));

    // === Language Section ===
    cmd.set("#LanguageSectionTitle.Text",
        HFMessages.get(playerRef, MessageKeys.PlayerSettings.LANGUAGE_SECTION));
    cmd.set("#AutoDetectDesc.Text",
        HFMessages.get(playerRef, MessageKeys.PlayerSettings.AUTO_DETECT_DESC));
    cmd.set("#LanguageLabel.Text",
        HFMessages.get(playerRef, MessageKeys.PlayerSettings.LANGUAGE_LABEL));

    // Auto-detect checkbox
    boolean autoDetect = (languagePreference == null);
    cmd.set("#AutoDetectCB #CheckBox.Value", autoDetect);

    // Auto-detect checkbox event
    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        "#AutoDetectCB #CheckBox",
        EventData.of("Button", "ToggleAutoDetect"),
        false
    );

    // Language dropdown
    cmd.set("#LanguageDropdown.Entries", LOCALE_DISPLAY_NAMES);
    int selectedIndex = 0;
    if (languagePreference != null) {
      int idx = AVAILABLE_LOCALES.indexOf(languagePreference);
      if (idx >= 0) {
        selectedIndex = idx;
      }
    }
    cmd.set("#LanguageDropdown.Value", selectedIndex);

    // Disable dropdown when auto-detect is on
    cmd.set("#LanguageRow.Visible", !autoDetect);

    // Language dropdown change event
    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        "#LanguageDropdown",
        EventData.of("Button", "LanguageChanged")
            .append("@Language", "#LanguageDropdown.Value"),
        false
    );

    // === Notifications Section ===
    cmd.set("#NotifSectionTitle.Text",
        HFMessages.get(playerRef, MessageKeys.PlayerSettings.NOTIFICATIONS_SECTION));

    // Territory Alerts
    buildNotificationToggle(cmd, events, "#TerritoryAlertsCB",
        MessageKeys.PlayerSettings.TERRITORY_ALERTS,
        MessageKeys.PlayerSettings.TERRITORY_ALERTS_DESC,
        "#TerritoryAlertsDesc", territoryAlerts, "ToggleTerritoryAlerts");

    // Death Announcements
    buildNotificationToggle(cmd, events, "#DeathAnnounceCB",
        MessageKeys.PlayerSettings.DEATH_ANNOUNCEMENTS,
        MessageKeys.PlayerSettings.DEATH_ANNOUNCEMENTS_DESC,
        "#DeathAnnounceDesc", deathAnnouncements, "ToggleDeathAnnouncements");

    // Power Notifications
    buildNotificationToggle(cmd, events, "#PowerNotifCB",
        MessageKeys.PlayerSettings.POWER_NOTIFICATIONS,
        MessageKeys.PlayerSettings.POWER_NOTIFICATIONS_DESC,
        "#PowerNotifDesc", powerNotifications, "TogglePowerNotifications");
  }

  private void buildNotificationToggle(UICommandBuilder cmd, UIEventBuilder events,
                     String checkboxId, String labelKey, String descKey,
                     String descId, boolean value, String action) {
    cmd.set(checkboxId + " #CheckBox.Value", value);

    // Set localized label text
    // Note: @Text param is set in .ui, but we override via child label
    // CheckBoxWithLabel template has a Label child we can target

    // Description text
    cmd.set(descId + ".Text", HFMessages.get(playerRef, descKey));

    // ValueChanged event
    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        checkboxId + " #CheckBox",
        EventData.of("Button", action),
        false
    );
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                PlayerSettingsData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null) {
      return;
    }

    // Handle nav bar events
    if (data.navBar != null && !data.navBar.isEmpty()) {
      if (faction != null) {
        if (NavBarHelper.handleNavEvent(data, player, ref, store, playerRef, faction, guiManager)) {
          return;
        }
      } else {
        if (NewPlayerNavBarHelper.handleNavEvent(data, player, ref, store, playerRef, guiManager)) {
          return;
        }
      }
    }

    if (data.button == null) {
      return;
    }

    UUID uuid = playerRef.getUuid();

    switch (data.button) {
      case "ToggleAutoDetect" -> {
        // Toggle auto-detect: if currently auto (null), set to current client language
        // If currently manual, set to null (auto)
        if (languagePreference == null) {
          // Switching to manual - use current client language
          languagePreference = playerRef.getLanguage();
        } else {
          // Switching to auto-detect
          languagePreference = null;
        }
        savePreference(uuid, d -> d.setLanguagePreference(languagePreference));
        HFMessages.setLanguageOverride(uuid, languagePreference);
        sendUpdate();
      }

      case "LanguageChanged" -> {
        // Dropdown value is an index into AVAILABLE_LOCALES
        if (data.language != null) {
          try {
            int index = Integer.parseInt(data.language);
            if (index >= 0 && index < AVAILABLE_LOCALES.size()) {
              languagePreference = AVAILABLE_LOCALES.get(index);
              savePreference(uuid, d -> d.setLanguagePreference(languagePreference));
              HFMessages.setLanguageOverride(uuid, languagePreference);
              player.sendMessage(MessageUtil.successText(playerRef,
                  MessageKeys.PlayerSettings.LANGUAGE_CHANGED,
                  LOCALE_DISPLAY_NAMES.get(index)));
            }
          } catch (NumberFormatException e) {
            // Invalid dropdown value
          }
        }
        sendUpdate();
      }

      case "ToggleTerritoryAlerts" -> {
        territoryAlerts = !territoryAlerts;
        savePreference(uuid, d -> d.setTerritoryAlertsEnabled(territoryAlerts));
        player.sendMessage(territoryAlerts
            ? MessageUtil.successText(playerRef, MessageKeys.PlayerSettings.PREF_ENABLED,
                HFMessages.get(playerRef, MessageKeys.PlayerSettings.TERRITORY_ALERTS))
            : MessageUtil.text(playerRef, MessageKeys.PlayerSettings.PREF_DISABLED, "#FFAA00",
                HFMessages.get(playerRef, MessageKeys.PlayerSettings.TERRITORY_ALERTS)));
        sendUpdate();
      }

      case "ToggleDeathAnnouncements" -> {
        deathAnnouncements = !deathAnnouncements;
        savePreference(uuid, d -> d.setDeathAnnouncementsEnabled(deathAnnouncements));
        player.sendMessage(deathAnnouncements
            ? MessageUtil.successText(playerRef, MessageKeys.PlayerSettings.PREF_ENABLED,
                HFMessages.get(playerRef, MessageKeys.PlayerSettings.DEATH_ANNOUNCEMENTS))
            : MessageUtil.text(playerRef, MessageKeys.PlayerSettings.PREF_DISABLED, "#FFAA00",
                HFMessages.get(playerRef, MessageKeys.PlayerSettings.DEATH_ANNOUNCEMENTS)));
        sendUpdate();
      }

      case "TogglePowerNotifications" -> {
        powerNotifications = !powerNotifications;
        savePreference(uuid, d -> d.setPowerNotificationsEnabled(powerNotifications));
        player.sendMessage(powerNotifications
            ? MessageUtil.successText(playerRef, MessageKeys.PlayerSettings.PREF_ENABLED,
                HFMessages.get(playerRef, MessageKeys.PlayerSettings.POWER_NOTIFICATIONS))
            : MessageUtil.text(playerRef, MessageKeys.PlayerSettings.PREF_DISABLED, "#FFAA00",
                HFMessages.get(playerRef, MessageKeys.PlayerSettings.POWER_NOTIFICATIONS)));
        sendUpdate();
      }

      default -> sendUpdate();
    }
  }

  private void savePreference(UUID uuid,
                java.util.function.Consumer<com.hyperfactions.data.PlayerData> updater) {
    playerStorage.updatePlayerData(uuid, updater);
  }
}
