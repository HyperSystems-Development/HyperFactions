package com.hyperfactions.gui.page.admin;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionLog;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.data.PlayerData;
import com.hyperfactions.data.PlayerPower;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminPlayerInfoData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.FactionManager.FactionResult;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.util.Logger;
import com.hyperfactions.util.TimeUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

/**
 * Admin Player Info page - displays detailed player information with admin power controls.
 * Allows admin to set/adjust/reset power, set max override, toggle bypass flags.
 */
public class AdminPlayerInfoPage extends InteractiveCustomUIPage<AdminPlayerInfoData> {

    private final PlayerRef playerRef;
    private final UUID targetPlayerUuid;
    private final String targetPlayerName;
    private final UUID factionId;
    private final FactionManager factionManager;
    private final PowerManager powerManager;
    private final GuiManager guiManager;

    public AdminPlayerInfoPage(PlayerRef playerRef,
                               UUID targetPlayerUuid,
                               String targetPlayerName,
                               UUID factionId,
                               FactionManager factionManager,
                               PowerManager powerManager,
                               GuiManager guiManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminPlayerInfoData.CODEC);
        this.playerRef = playerRef;
        this.targetPlayerUuid = targetPlayerUuid;
        this.targetPlayerName = targetPlayerName;
        this.factionId = factionId;
        this.factionManager = factionManager;
        this.powerManager = powerManager;
        this.guiManager = guiManager;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {
        cmd.append("HyperFactions/admin/admin_player_info.ui");
        AdminNavBarHelper.setupBar(playerRef, "factions", cmd, events);
        buildContent(cmd, events);
    }

    private void buildContent(UICommandBuilder cmd, UIEventBuilder events) {
        // === Header ===
        cmd.set("#PlayerName.Text", targetPlayerName);

        // Online status
        boolean isOnline = isOnline(targetPlayerUuid);
        cmd.set("#OnlineStatus.Text", isOnline ? "Online" : "Offline");
        cmd.set("#OnlineStatus.Style.TextColor", isOnline ? "#55FF55" : "#FF5555");

        // Faction info
        Faction faction = factionId != null ? factionManager.getFaction(factionId) : null;
        if (faction == null) {
            faction = factionManager.getPlayerFaction(targetPlayerUuid);
        }
        if (faction != null) {
            cmd.set("#FactionName.Text", faction.name());
            FactionMember member = faction.getMember(targetPlayerUuid);
            if (member != null) {
                String roleText = formatRole(member.role()) + " - Joined " + TimeUtil.formatRelative(member.joinedAt());
                cmd.set("#RoleLabel.Text", roleText);
                cmd.set("#RoleLabel.Style.TextColor", getRoleColor(member.role()));
            }
        } else {
            cmd.set("#FactionName.Text", "No Faction");
            cmd.set("#FactionName.Style.TextColor", "#888888");
        }

        // === Power Management ===
        PlayerPower power = powerManager.getPlayerPower(targetPlayerUuid);
        double effectiveMax = power.getEffectiveMaxPower();

        // Power display
        cmd.set("#PowerValue.Text", String.format("%.1f / %.1f", power.power(), effectiveMax));
        int powerPercent = power.getPowerPercent();
        String powerColor = powerPercent >= 80 ? "#55FF55" : powerPercent >= 40 ? "#FFAA00" : "#FF5555";
        cmd.set("#PowerValue.Style.TextColor", powerColor);

        // Max override indicator
        if (power.maxPowerOverride() != null) {
            cmd.set("#MaxOverrideLabel.Text", "(custom max)");
        } else {
            cmd.set("#MaxOverrideLabel.Text", "(default max)");
            cmd.set("#MaxOverrideLabel.Style.TextColor", "#666666");
        }

        // Power bar
        float powerRatio = effectiveMax > 0 ? (float) (power.power() / effectiveMax) : 0f;
        cmd.set("#PowerBar.Value", powerRatio);
        cmd.set("#PowerBar.Bar.Color", powerColor);

        // Max power display
        cmd.set("#MaxPowerValue.Text", String.format("%.1f", effectiveMax));

        // === Bypass Toggles ===
        cmd.set("#NoLossCheck #CheckBox.Value", power.powerLossDisabled());
        cmd.set("#NoLossStatus.Text", power.powerLossDisabled() ? "Active" : "Off");
        cmd.set("#NoLossStatus.Style.TextColor", power.powerLossDisabled() ? "#55FF55" : "#888888");

        cmd.set("#NoDecayCheck #CheckBox.Value", power.claimDecayExempt());
        cmd.set("#NoDecayStatus.Text", power.claimDecayExempt() ? "Active" : "Off");
        cmd.set("#NoDecayStatus.Style.TextColor", power.claimDecayExempt() ? "#55FF55" : "#888888");

        // === Stats ===
        PlayerData cachedData = loadPlayerDataSync();
        if (cachedData != null) {
            cmd.set("#KillsValue.Text", String.valueOf(cachedData.getKills()));
            cmd.set("#DeathsValue.Text", String.valueOf(cachedData.getDeaths()));
            double kdr = cachedData.getDeaths() > 0 ? (double) cachedData.getKills() / cachedData.getDeaths() : cachedData.getKills();
            cmd.set("#KDRValue.Text", String.format("%.2f", kdr));
        }

        cmd.set("#UuidValue.Text", targetPlayerUuid.toString());

        // === Kick button ===
        if (faction == null) {
            cmd.set("#KickBtn.Disabled", true);
        } else {
            FactionMember targetMember = faction.getMember(targetPlayerUuid);
            if (targetMember != null && targetMember.isLeader() && faction.getMemberCount() == 1) {
                // Last member is the leader — offer disband instead
                cmd.set("#KickBtn.Text", "Disband Faction");
            } else if (targetMember != null && targetMember.isLeader()) {
                cmd.set("#KickBtn.Text", "Kick Leader");
            }
        }

        // === Event Bindings ===
        // Quick adjust buttons (use "Delta" key for static values)
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SubFive",
                EventData.of("Button", "AdjustPower").append("Delta", "-5"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SubOne",
                EventData.of("Button", "AdjustPower").append("Delta", "-1"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AddOne",
                EventData.of("Button", "AdjustPower").append("Delta", "1"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AddFive",
                EventData.of("Button", "AdjustPower").append("Delta", "5"), false);

        // Set power button (reads from text field)
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SetPowerBtn",
                EventData.of("Button", "SetPower").append("@PowerInput", "#PowerInput.Value"), false);

        // Reset power button
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ResetPowerBtn",
                EventData.of("Button", "ResetPower"), false);

        // Set max button (reads from max text field)
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SetMaxBtn",
                EventData.of("Button", "SetMax").append("@PowerInput", "#MaxPowerInput.Value"), false);

        // Reset max button
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ResetMaxBtn",
                EventData.of("Button", "ResetMax"), false);

        // Checkbox toggles — use ValueChanged without reading value (toggle from server state)
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NoLossCheck #CheckBox",
                EventData.of("Button", "ToggleNoLoss"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NoDecayCheck #CheckBox",
                EventData.of("Button", "ToggleNoDecay"), false);

        // Kick button
        events.addEventBinding(CustomUIEventBindingType.Activating, "#KickBtn",
                EventData.of("Button", "Kick"), false);

        // Back button
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BackBtn",
                EventData.of("Button", "Back"), false);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                AdminPlayerInfoData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null) return;

        // Handle admin nav bar navigation
        if (AdminNavBarHelper.handleNavEvent(data, player, ref, store, playerRef, guiManager)) {
            return;
        }

        if (data.button == null) return;

        UUID adminUuid = playerRef.getUuid();

        switch (data.button) {
            case "AdjustPower" -> {
                double delta = parseDoubleOrZero(data.delta);
                if (delta == 0) return;
                double oldPower = powerManager.getPlayerPower(targetPlayerUuid).power();
                double newPower = powerManager.adjustPlayerPower(targetPlayerUuid, delta);
                logAdminPowerChange(adminUuid,
                    "Admin adjusted " + targetPlayerName + "'s power by " + String.format("%.1f", delta)
                    + " (" + String.format("%.1f", oldPower) + " -> " + String.format("%.1f", newPower) + ")");
                reopenPage(player, ref, store, playerRef);
            }

            case "SetPower" -> {
                double amount = parseDoubleOrNaN(data.powerInput);
                if (Double.isNaN(amount)) {
                    player.sendMessage(Message.raw("[Admin] Enter a valid number.").color("#FF5555"));
                    return;
                }
                double oldPower = powerManager.getPlayerPower(targetPlayerUuid).power();
                double newPower = powerManager.setPlayerPower(targetPlayerUuid, amount);
                logAdminPowerChange(adminUuid,
                    "Admin set " + targetPlayerName + "'s power to " + String.format("%.1f", newPower)
                    + " (was " + String.format("%.1f", oldPower) + ")");
                reopenPage(player, ref, store, playerRef);
            }

            case "ResetPower" -> {
                double oldPower = powerManager.getPlayerPower(targetPlayerUuid).power();
                double newPower = powerManager.resetPlayerPower(targetPlayerUuid);
                logAdminPowerChange(adminUuid,
                    "Admin reset " + targetPlayerName + "'s power to " + String.format("%.1f", newPower)
                    + " (was " + String.format("%.1f", oldPower) + ")");
                reopenPage(player, ref, store, playerRef);
            }

            case "SetMax" -> {
                double amount = parseDoubleOrNaN(data.powerInput);
                if (Double.isNaN(amount) || amount <= 0) {
                    player.sendMessage(Message.raw("[Admin] Enter a valid positive number.").color("#FF5555"));
                    return;
                }
                PlayerPower old = powerManager.getPlayerPower(targetPlayerUuid);
                double oldMax = old.getEffectiveMaxPower();
                powerManager.setPlayerMaxPower(targetPlayerUuid, amount);
                logAdminPowerChange(adminUuid,
                    "Admin set " + targetPlayerName + "'s max power to " + String.format("%.1f", amount)
                    + " (was " + String.format("%.1f", oldMax) + ")");
                reopenPage(player, ref, store, playerRef);
            }

            case "ResetMax" -> {
                PlayerPower old = powerManager.getPlayerPower(targetPlayerUuid);
                double oldMax = old.getEffectiveMaxPower();
                powerManager.resetPlayerMaxPower(targetPlayerUuid);
                logAdminPowerChange(adminUuid,
                    "Admin reset " + targetPlayerName + "'s max power to global default");
                reopenPage(player, ref, store, playerRef);
            }

            case "ToggleNoLoss" -> {
                // Toggle from current server state (don't read checkbox value from EventData)
                PlayerPower current = powerManager.getPlayerPower(targetPlayerUuid);
                boolean newState = !current.powerLossDisabled();
                powerManager.setPlayerPowerLossDisabled(targetPlayerUuid, newState);
                logAdminPowerChange(adminUuid,
                    "Admin " + (newState ? "disabled" : "enabled") + " power loss for " + targetPlayerName);
                reopenPage(player, ref, store, playerRef);
            }

            case "ToggleNoDecay" -> {
                // Toggle from current server state (don't read checkbox value from EventData)
                PlayerPower current = powerManager.getPlayerPower(targetPlayerUuid);
                boolean newState = !current.claimDecayExempt();
                powerManager.setPlayerClaimDecayExempt(targetPlayerUuid, newState);
                logAdminPowerChange(adminUuid,
                    "Admin " + (newState ? "enabled" : "disabled") + " claim decay exemption for " + targetPlayerName);
                reopenPage(player, ref, store, playerRef);
            }

            case "Kick" -> {
                Faction faction = factionManager.getPlayerFaction(targetPlayerUuid);
                if (faction == null) return;

                FactionMember targetMember = faction.getMember(targetPlayerUuid);
                if (targetMember == null) return;

                if (targetMember.isLeader()) {
                    if (faction.getMemberCount() == 1) {
                        // Last member — disband the faction
                        factionManager.forceDisband(faction.id(),
                            "[Admin] Disbanded via admin kick of last member " + targetPlayerName);
                        player.sendMessage(Message.raw("[Admin] Faction '" + faction.name()
                            + "' disbanded (last member kicked).").color("#FFAA00"));
                        // Navigate back to factions list since faction no longer exists
                        guiManager.openAdminFactions(player, ref, store, playerRef);
                    } else {
                        // Leader with other members — transfer leadership then kick
                        FactionMember successor = faction.findSuccessor();
                        if (successor != null) {
                            // Transfer leadership
                            FactionMember promoted = successor.withRole(FactionRole.LEADER);
                            FactionMember demoted = targetMember.withRole(FactionRole.MEMBER);
                            Faction updated = faction.withMember(promoted).withMember(demoted)
                                .withLog(FactionLog.create(FactionLog.LogType.LEADER_TRANSFER,
                                    "[Admin] Leadership transferred from " + targetPlayerName
                                    + " to " + successor.username() + " (admin kick)",
                                    adminUuid));
                            factionManager.updateFaction(updated);

                            // Now kick the demoted member
                            factionManager.adminRemoveMember(faction.id(), targetPlayerUuid);
                            player.sendMessage(Message.raw("[Admin] Kicked leader " + targetPlayerName
                                + ". Leadership transferred to " + successor.username() + ".").color("#55FF55"));
                        }
                        reopenPage(player, ref, store, playerRef);
                    }
                } else {
                    // Normal kick
                    FactionResult result = factionManager.adminRemoveMember(faction.id(), targetPlayerUuid);
                    if (result == FactionResult.SUCCESS) {
                        player.sendMessage(Message.raw("[Admin] Kicked " + targetPlayerName
                            + " from " + faction.name() + ".").color("#55FF55"));
                    }
                    reopenPage(player, ref, store, playerRef);
                }
            }

            case "Back" -> {
                if (factionId != null) {
                    guiManager.openAdminFactionMembers(player, ref, store, playerRef, factionId);
                } else {
                    guiManager.openAdminFactions(player, ref, store, playerRef);
                }
            }
        }
    }

    /**
     * Reopens the page with fresh data via GuiManager (avoids sendUpdate state issues).
     */
    private void reopenPage(Player player, Ref<EntityStore> ref, Store<EntityStore> store, PlayerRef playerRef) {
        guiManager.openAdminPlayerInfo(player, ref, store, playerRef,
                targetPlayerUuid, targetPlayerName, factionId);
    }

    private void logAdminPowerChange(UUID adminUuid, String message) {
        Faction faction = factionManager.getPlayerFaction(targetPlayerUuid);
        if (faction != null) {
            Faction updated = faction.withLog(FactionLog.create(FactionLog.LogType.ADMIN_POWER, message, adminUuid));
            factionManager.updateFaction(updated);
        }
    }

    private PlayerData loadPlayerDataSync() {
        try {
            return guiManager.getPlugin().get().getPlayerStorage()
                    .loadPlayerData(targetPlayerUuid).join().orElse(null);
        } catch (Exception e) {
            Logger.debug("Failed to load player data for %s: %s", targetPlayerUuid, e.getMessage());
            return null;
        }
    }

    private boolean isOnline(UUID uuid) {
        try {
            PlayerRef ref = Universe.get().getPlayer(uuid);
            return ref != null && ref.isValid();
        } catch (Exception e) {
            return false;
        }
    }

    private String formatRole(FactionRole role) {
        return switch (role) {
            case LEADER -> "Leader";
            case OFFICER -> "Officer";
            case MEMBER -> "Member";
        };
    }

    private String getRoleColor(FactionRole role) {
        return switch (role) {
            case LEADER -> "#FFD700";
            case OFFICER -> "#87CEEB";
            case MEMBER -> "#888888";
        };
    }

    private double parseDoubleOrZero(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double parseDoubleOrNaN(String value) {
        if (value == null || value.isEmpty()) return Double.NaN;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
