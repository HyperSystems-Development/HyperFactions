package com.hyperfactions.command.economy;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Subcommand: /f deposit <amount>
 * Deposits money from player wallet into faction treasury.
 */
public class DepositSubCommand extends FactionSubCommand {

    public DepositSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
        super("deposit", "Deposit money into faction treasury", hyperFactions, plugin);
        addAliases("dep");
    }

    @Override
    protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef player,
                          @NotNull World currentWorld) {
        String input = ctx.getInputString();
        String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
        String[] args = parts.length > 2 ? Arrays.copyOfRange(parts, 2, parts.length) : new String[0];

        TreasuryCommandHandler.handleDeposit(ctx, player, hyperFactions, args);
    }
}
