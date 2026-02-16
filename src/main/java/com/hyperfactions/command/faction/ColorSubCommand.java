package com.hyperfactions.command.faction;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionLog;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand: /f color <code>
 * Sets the faction color (officer+).
 */
public class ColorSubCommand extends FactionSubCommand {

    public ColorSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
        super("color", "Set faction color", hyperFactions, plugin);
    }

    @Override
    protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef player,
                          @NotNull World currentWorld) {

        if (!hasPermission(player, Permissions.COLOR)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        Faction faction = requireFaction(ctx, player);
        if (faction == null) return;

        FactionMember member = faction.getMember(player.getUuid());
        if (member == null || !member.isOfficerOrHigher()) {
            ctx.sendMessage(prefix().insert(msg("You must be an officer to change the color.", COLOR_RED)));
            return;
        }

        if (!ConfigManager.get().isAllowColors()) {
            ctx.sendMessage(prefix().insert(msg("Faction colors are disabled.", COLOR_RED)));
            return;
        }

        String input = ctx.getInputString();
        String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
        String[] rawArgs = parts.length > 2 ? java.util.Arrays.copyOfRange(parts, 2, parts.length) : new String[0];
        FactionCommandContext fctx = parseContext(rawArgs);

        // GUI mode: Open settings page if no args and not text mode
        if (!fctx.hasArgs() && !fctx.isTextMode()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openFactionSettings(playerEntity, ref, store, player, faction);
            }
            return;
        }

        // Text mode requires args
        if (!fctx.hasArgs()) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f color <code|#hex>", COLOR_RED)));
            ctx.sendMessage(msg("Valid codes: 0-9, a-f or #RRGGBB hex", COLOR_GRAY));
            return;
        }

        String colorInput = fctx.getArg(0).toLowerCase();
        String hexColor;
        if (colorInput.startsWith("#") && colorInput.length() == 7 && colorInput.substring(1).matches("[0-9a-f]+")) {
            // Direct hex input
            hexColor = colorInput.toUpperCase();
        } else if (colorInput.length() == 1 && colorInput.matches("[0-9a-f]")) {
            // Legacy color code - convert to hex
            hexColor = com.hyperfactions.util.LegacyColorParser.codeToHex(colorInput.charAt(0));
        } else {
            ctx.sendMessage(prefix().insert(msg("Invalid color. Use 0-9, a-f, or #RRGGBB.", COLOR_RED)));
            return;
        }

        Faction updated = faction.withColor(hexColor)
            .withLog(FactionLog.create(FactionLog.LogType.SETTINGS_CHANGE,
                "Color changed to '" + hexColor + "'", player.getUuid()));

        hyperFactions.getFactionManager().updateFaction(updated);

        // Refresh world maps to show new faction color (respects configured refresh mode)
        hyperFactions.getWorldMapService().triggerFactionWideRefresh(faction.id());

        ctx.sendMessage(prefix().insert(msg("Faction color updated to ", COLOR_GREEN))
            .insert(msg("this color", null).color(hexColor))
            .insert(msg("!", COLOR_GREEN)));

        // After action, open settings page if not text mode
        if (fctx.shouldOpenGuiAfterAction()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                Faction refreshed = hyperFactions.getFactionManager().getFaction(faction.id());
                if (refreshed != null) {
                    hyperFactions.getGuiManager().openFactionSettings(playerEntity, ref, store, player, refreshed);
                }
            }
        }
    }
}
