package com.hyperfactions.gui.faction.page;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionEconomy;
import com.hyperfactions.data.FactionEconomy.TreasuryLimits;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionPermissions;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.faction.data.TreasurySettingsData;
import com.hyperfactions.manager.EconomyManager;
import com.hyperfactions.manager.FactionManager;
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

import java.util.UUID;

/**
 * Treasury settings sub-page — officer permissions, withdrawal/transfer limits,
 * and upkeep auto-pay toggle.
 */
public class TreasurySettingsPage extends InteractiveCustomUIPage<TreasurySettingsData> {

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final EconomyManager economyManager;
    private final GuiManager guiManager;
    private final Faction faction;

    public TreasurySettingsPage(PlayerRef playerRef,
                                FactionManager factionManager,
                                EconomyManager economyManager,
                                GuiManager guiManager,
                                Faction faction) {
        super(playerRef, CustomPageLifetime.CanDismiss, TreasurySettingsData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.economyManager = economyManager;
        this.guiManager = guiManager;
        this.faction = faction;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        cmd.append("HyperFactions/faction/treasury_settings.ui");

        FactionPermissions perms = faction.getEffectivePermissions();
        FactionEconomy economy = economyManager.getEconomy(faction.id());

        // Officer permission toggles
        cmd.set("#OfficerWithdrawToggle #CheckBox.Value", perms.get(FactionPermissions.TREASURY_WITHDRAW));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged,
                "#OfficerWithdrawToggle #CheckBox",
                EventData.of("Button", "TogglePerm").append("Perm", FactionPermissions.TREASURY_WITHDRAW),
                false);

        cmd.set("#OfficerTransferToggle #CheckBox.Value", perms.get(FactionPermissions.TREASURY_TRANSFER));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged,
                "#OfficerTransferToggle #CheckBox",
                EventData.of("Button", "TogglePerm").append("Perm", FactionPermissions.TREASURY_TRANSFER),
                false);

        // Limit fields
        TreasuryLimits limits = economy != null ? economy.limits() : TreasuryLimits.defaults();
        cmd.set("#MaxWithdrawAmountInput.Value", formatLimit(limits.maxWithdrawAmount()));
        cmd.set("#MaxWithdrawPeriodInput.Value", formatLimit(limits.maxWithdrawPerPeriod()));
        cmd.set("#MaxTransferAmountInput.Value", formatLimit(limits.maxTransferAmount()));
        cmd.set("#MaxTransferPeriodInput.Value", formatLimit(limits.maxTransferPerPeriod()));
        cmd.set("#PeriodHoursInput.Value", String.valueOf(limits.periodHours()));

        // Upkeep settings (if enabled)
        if (ConfigManager.get().isUpkeepEnabled()) {
            cmd.set("#UpkeepSettings.Visible", true);
            boolean autoPay = economy != null ? economy.upkeepAutoPay() : true;
            cmd.set("#AutoPayToggle #CheckBox.Value", autoPay);
            events.addEventBinding(CustomUIEventBindingType.ValueChanged,
                    "#AutoPayToggle #CheckBox",
                    EventData.of("Button", "ToggleAutoPay"),
                    false);
        }

        // Back button
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BackBtn",
                EventData.of("Button", "Back"), false);

        // Save button
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SaveBtn",
                EventData.of("Button", "Save")
                        .append("@MaxWithdrawAmount", "#MaxWithdrawAmountInput.Value")
                        .append("@MaxWithdrawPeriod", "#MaxWithdrawPeriodInput.Value")
                        .append("@MaxTransferAmount", "#MaxTransferAmountInput.Value")
                        .append("@MaxTransferPeriod", "#MaxTransferPeriodInput.Value")
                        .append("@PeriodHours", "#PeriodHoursInput.Value"),
                false);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                TreasurySettingsData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            sendUpdate();
            return;
        }

        UUID uuid = playerRef.getUuid();

        switch (data.button) {
            case "Back" -> {
                Faction fresh = factionManager.getFaction(faction.id());
                if (fresh != null) {
                    guiManager.openFactionTreasury(player, ref, store, playerRef, fresh);
                }
            }
            case "TogglePerm" -> handleTogglePerm(player, ref, store, playerRef, uuid, data);
            case "ToggleAutoPay" -> handleToggleAutoPay(player, ref, store, playerRef, uuid);
            case "Save" -> handleSave(player, ref, store, playerRef, uuid, data);
            default -> sendUpdate();
        }
    }

    private void handleTogglePerm(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                   PlayerRef playerRef, UUID uuid, TreasurySettingsData data) {
        String permName = data.perm;
        if (permName == null) {
            sendUpdate();
            return;
        }

        FactionMember member = faction.getMember(uuid);
        if (member == null || member.role() != FactionRole.LEADER) {
            player.sendMessage(MessageUtil.errorText("Only the leader can change treasury permissions."));
            sendUpdate();
            return;
        }

        FactionPermissions current = faction.getEffectivePermissions();
        FactionPermissions updated = current.toggle(permName);

        Faction updatedFaction = faction.withPermissions(updated);
        factionManager.updateFaction(updatedFaction);

        guiManager.openTreasurySettings(player, ref, store, playerRef,
                factionManager.getFaction(faction.id()));
    }

    private void handleToggleAutoPay(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                      PlayerRef playerRef, UUID uuid) {
        FactionMember member = faction.getMember(uuid);
        if (member == null || member.role() != FactionRole.LEADER) {
            player.sendMessage(MessageUtil.errorText("Only the leader can change upkeep settings."));
            sendUpdate();
            return;
        }

        FactionEconomy economy = economyManager.getEconomy(faction.id());
        if (economy != null) {
            boolean newValue = !economy.upkeepAutoPay();
            FactionEconomy updated = economy.withUpkeepAutoPay(newValue);
            economyManager.updateEconomy(faction.id(), updated);
        }

        guiManager.openTreasurySettings(player, ref, store, playerRef,
                factionManager.getFaction(faction.id()));
    }

    private void handleSave(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                             PlayerRef playerRef, UUID uuid, TreasurySettingsData data) {
        FactionMember member = faction.getMember(uuid);
        if (member == null || member.role() != FactionRole.LEADER) {
            player.sendMessage(MessageUtil.errorText("Only the leader can configure limits."));
            sendUpdate();
            return;
        }

        try {
            double maxWithdrawAmt = parseLimit(data.maxWithdrawAmount);
            double maxWithdrawPer = parseLimit(data.maxWithdrawPeriod);
            double maxTransferAmt = parseLimit(data.maxTransferAmount);
            double maxTransferPer = parseLimit(data.maxTransferPeriod);
            int period = parsePeriod(data.periodHours);

            TreasuryLimits newLimits = new TreasuryLimits(
                    maxWithdrawAmt, maxWithdrawPer,
                    maxTransferAmt, maxTransferPer,
                    period
            );

            economyManager.updateLimits(faction.id(), newLimits);
            player.sendMessage(MessageUtil.successText("Treasury limits updated."));

            guiManager.openTreasurySettings(player, ref, store, playerRef,
                    factionManager.getFaction(faction.id()));
        } catch (NumberFormatException e) {
            player.sendMessage(MessageUtil.errorText("Invalid number in limit fields. Use 0 for unlimited."));
            sendUpdate();
        }
    }

    private static String formatLimit(double value) {
        if (value == 0) return "0";
        return String.format("%.0f", value);
    }

    private static double parseLimit(String value) {
        if (value == null || value.isBlank()) return 0;
        double v = Double.parseDouble(value.trim());
        return Math.max(0, v);
    }

    private static int parsePeriod(String value) {
        if (value == null || value.isBlank()) return 24;
        int v = Integer.parseInt(value.trim());
        return Math.max(1, v);
    }
}
