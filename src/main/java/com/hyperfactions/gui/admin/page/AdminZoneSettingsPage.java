package com.hyperfactions.gui.admin.page;

import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneFlags;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminZoneSettingsData;
import com.hyperfactions.integration.protection.OrbisMixinsIntegration;
import com.hyperfactions.manager.ZoneManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

/**
 * Admin Zone Settings page - configure zone flags visually.
 * Shows all 24 zone flags with toggle buttons and default indicators in 2-column layout.
 *
 * Layout (indices):
 * - Left column: Combat (0-3), Damage (4-5), Death (6-7), Building (8), Items (9-12)
 * - Right column: Interaction (13-18), Spawning (19-23)
 *
 * Mixin-dependent flags are disabled when OrbisGuard-Mixins is not installed:
 * - ITEM_PICKUP_MANUAL: F-key pickup (requires pickup mixin)
 * - INVINCIBLE_ITEMS: Item durability (requires durability mixin)
 * - KEEP_INVENTORY: Keep items on death (requires death mixin)
 */
public class AdminZoneSettingsPage extends InteractiveCustomUIPage<AdminZoneSettingsData> {

    private final PlayerRef playerRef;
    private final UUID zoneId;
    private final ZoneManager zoneManager;
    private final GuiManager guiManager;

    public AdminZoneSettingsPage(PlayerRef playerRef,
                                 UUID zoneId,
                                 ZoneManager zoneManager,
                                 GuiManager guiManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminZoneSettingsData.CODEC);
        this.playerRef = playerRef;
        this.zoneId = zoneId;
        this.zoneManager = zoneManager;
        this.guiManager = guiManager;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the admin zone settings template
        cmd.append("HyperFactions/admin/admin_zone_settings.ui");

        // Setup admin nav bar
        AdminNavBarHelper.setupBar(playerRef, "zones", cmd, events);

        // Get the zone
        Zone zone = zoneManager.getZoneById(zoneId);
        if (zone == null) {
            cmd.set("#ZoneName.Text", "Zone Not Found");
            cmd.set("#FlagsContainer.Visible", false);
            return;
        }

        // Zone info header
        cmd.set("#ZoneName.Text", zone.name());
        cmd.set("#ZoneType.Text", zone.type().name());
        cmd.set("#ZoneChunks.Text", zone.getChunkCount() + " chunks");

        // Type indicator color
        String typeColor = zone.isSafeZone() ? "#55FF55" : "#FF5555";
        cmd.set("#ZoneType.Style.TextColor", typeColor);

        // Build flag toggles by category (matching 2-column UI layout)
        // Left column: Combat (0-3), Damage (4-5), Death (6-7), Building (8), Items (9-12)
        buildFlagCategory(cmd, events, zone, "Combat", ZoneFlags.COMBAT_FLAGS, 0);
        buildFlagCategory(cmd, events, zone, "Damage", ZoneFlags.DAMAGE_FLAGS, 4);
        buildFlagCategory(cmd, events, zone, "Death", ZoneFlags.DEATH_FLAGS, 6);
        buildFlagCategory(cmd, events, zone, "Building", ZoneFlags.BUILDING_FLAGS, 8);
        buildFlagCategory(cmd, events, zone, "Items", ZoneFlags.ITEM_FLAGS, 9);
        // Right column: Interaction (13-18), Spawning (19-23)
        buildFlagCategory(cmd, events, zone, "Interaction", ZoneFlags.INTERACTION_FLAGS, 13);
        buildFlagCategory(cmd, events, zone, "Spawning", ZoneFlags.SPAWNING_FLAGS, 19);

        // Reset to Defaults button
        if (!zone.getFlags().isEmpty()) {
            cmd.set("#ResetBtn.Disabled", false);
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ResetBtn",
                    EventData.of("Button", "ResetDefaults")
                            .append("ZoneId", zoneId.toString()),
                    false
            );
        } else {
            cmd.set("#ResetBtn.Disabled", true);
        }

        // Integration Flags button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#IntegrationFlagsBtn",
                EventData.of("Button", "IntegrationFlags")
                        .append("ZoneId", zoneId.toString()),
                false
        );

        // Back button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BackBtn",
                EventData.of("Button", "Back"),
                false
        );

    }

    private void buildFlagCategory(UICommandBuilder cmd, UIEventBuilder events,
                                    Zone zone, String categoryName, String[] flags, int startIndex) {
        for (int i = 0; i < flags.length; i++) {
            String flagName = flags[i];
            int index = startIndex + i;
            buildFlagToggle(cmd, events, index, flagName, zone);
        }
    }

    private void buildFlagToggle(UICommandBuilder cmd, UIEventBuilder events,
                                  int index, String flagName, Zone zone) {
        String idx = "#Flag" + index;

        boolean currentValue = zone.getEffectiveFlag(flagName);
        boolean isDefault = !zone.hasFlagSet(flagName);

        // Check if this is a child flag and if parent is OFF
        String parentFlag = ZoneFlags.getParentFlag(flagName);
        boolean parentDisabled = parentFlag != null && !zone.getEffectiveFlag(parentFlag);

        // Check if this flag requires a mixin that isn't available
        boolean mixinUnavailable = false;
        String mixinType = ZoneFlags.getMixinType(flagName);
        if (mixinType != null) {
            mixinUnavailable = !isMixinAvailable(mixinType);
        }

        // Flag name (display name from ZoneFlags)
        String displayName = ZoneFlags.getDisplayName(flagName);
        cmd.set(idx + "Name.Text", displayName);

        // Set checkbox value via child selector
        // When parent is off, show children as unchecked for clearer visual state
        boolean shouldDisable = parentDisabled || mixinUnavailable;
        boolean displayValue = parentDisabled ? false : currentValue;
        cmd.set(idx + "Toggle #CheckBox.Value", displayValue);
        cmd.set(idx + "Toggle #CheckBox.Disabled", shouldDisable);

        // Default indicator (shows "(default)" or "(custom)" or "(mixin)")
        if (mixinUnavailable) {
            cmd.set(idx + "Default.Text", "(mixin)");
            cmd.set(idx + "Default.Style.TextColor", "#FF5555");
        } else if (isDefault) {
            cmd.set(idx + "Default.Text", "(default)");
            cmd.set(idx + "Default.Style.TextColor", "#555555");
        } else {
            cmd.set(idx + "Default.Text", "(custom)");
            cmd.set(idx + "Default.Style.TextColor", "#FFAA00");
        }

        // ValueChanged event (disabled checkboxes won't fire)
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                idx + "Toggle #CheckBox",
                EventData.of("Button", "ToggleFlag")
                        .append("Flag", flagName)
                        .append("ZoneId", zoneId.toString()),
                false
        );
    }

    /**
     * Checks if a specific mixin type is available.
     *
     * Note: Individual mixin properties are only set when their target class loads.
     * For example, the pickup mixin property is set when PlayerItemEntityPickupSystem
     * is first used. However, if OrbisGuard-Mixins is installed (detected via durability
     * mixin which loads early), we trust that ALL mixins will work when needed.
     */
    private boolean isMixinAvailable(String mixinType) {
        // If OrbisGuard-Mixins is installed, trust all mixin features will work
        // This handles the case where specific mixin classes haven't loaded yet
        if (OrbisMixinsIntegration.isMixinsAvailable()) {
            return true;
        }
        // Fallback to specific mixin checks (shouldn't normally reach here)
        return switch (mixinType) {
            case ZoneFlags.MIXIN_PICKUP -> OrbisMixinsIntegration.isPickupMixinLoaded();
            case ZoneFlags.MIXIN_DEATH -> OrbisMixinsIntegration.isDeathMixinLoaded();
            case ZoneFlags.MIXIN_DURABILITY -> OrbisMixinsIntegration.isDurabilityMixinLoaded();
            case ZoneFlags.MIXIN_SEATING -> OrbisMixinsIntegration.isSeatingMixinLoaded();
            case ZoneFlags.MIXIN_SPAWN -> OrbisMixinsIntegration.isMixinsAvailable(); // No specific detection
            default -> false;
        };
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                AdminZoneSettingsData data) {
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

        if (data.button == null) {
            return;
        }

        switch (data.button) {
            case "ToggleFlag" -> handleToggleFlag(player, data);
            case "ResetDefaults" -> handleResetDefaults(player, data);
            case "IntegrationFlags" -> {
                UUID zId = UUID.fromString(data.zoneId);
                guiManager.openAdminZoneIntegrationFlags(player, ref, store, playerRef, zId);
            }
            case "Back" -> guiManager.openAdminZone(player, ref, store, playerRef);
        }
    }

    private void handleToggleFlag(Player player, AdminZoneSettingsData data) {
        String flagName = data.flag;
        if (flagName == null || !ZoneFlags.isValidFlag(flagName)) {
            player.sendMessage(MessageUtil.adminError("Invalid flag."));
            sendUpdate();
            return;
        }

        Zone zone = zoneManager.getZoneById(zoneId);
        if (zone == null) {
            player.sendMessage(MessageUtil.adminError("Zone not found."));
            sendUpdate();
            return;
        }

        // Toggle the flag
        boolean currentValue = zone.getEffectiveFlag(flagName);
        boolean newValue = !currentValue;
        boolean defaultValue = ZoneFlags.getDefault(flagName, zone.type());

        ZoneManager.ZoneResult result;
        if (newValue == defaultValue) {
            // Clear the flag to use default
            result = zoneManager.clearZoneFlag(zoneId, flagName);
        } else {
            // Set custom value
            result = zoneManager.setZoneFlag(zoneId, flagName, newValue);
        }

        // No chat message - UI feedback is sufficient

        rebuildPage();
    }

    private void handleResetDefaults(Player player, AdminZoneSettingsData data) {
        ZoneManager.ZoneResult result = zoneManager.clearAllZoneFlags(zoneId);

        if (result == ZoneManager.ZoneResult.SUCCESS) {
            player.sendMessage(MessageUtil.adminSuccess("Reset all flags to defaults."));
        } else {
            player.sendMessage(MessageUtil.adminError("Failed to reset flags: " + result));
        }

        rebuildPage();
    }

    private void rebuildPage() {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        Zone zone = zoneManager.getZoneById(zoneId);
        if (zone == null) {
            sendUpdate();
            return;
        }

        // Rebuild flag toggles (matching 2-column UI layout)
        // Left column: Combat (0-3), Damage (4-5), Death (6-7), Building (8), Items (9-12)
        buildFlagCategory(cmd, events, zone, "Combat", ZoneFlags.COMBAT_FLAGS, 0);
        buildFlagCategory(cmd, events, zone, "Damage", ZoneFlags.DAMAGE_FLAGS, 4);
        buildFlagCategory(cmd, events, zone, "Death", ZoneFlags.DEATH_FLAGS, 6);
        buildFlagCategory(cmd, events, zone, "Building", ZoneFlags.BUILDING_FLAGS, 8);
        buildFlagCategory(cmd, events, zone, "Items", ZoneFlags.ITEM_FLAGS, 9);
        // Right column: Interaction (13-18), Spawning (19-23)
        buildFlagCategory(cmd, events, zone, "Interaction", ZoneFlags.INTERACTION_FLAGS, 13);
        buildFlagCategory(cmd, events, zone, "Spawning", ZoneFlags.SPAWNING_FLAGS, 19);

        // Update reset button state
        if (!zone.getFlags().isEmpty()) {
            cmd.set("#ResetBtn.Disabled", false);
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ResetBtn",
                    EventData.of("Button", "ResetDefaults")
                            .append("ZoneId", zoneId.toString()),
                    false
            );
        } else {
            cmd.set("#ResetBtn.Disabled", true);
        }

        sendUpdate(cmd, events, false);
    }
}
