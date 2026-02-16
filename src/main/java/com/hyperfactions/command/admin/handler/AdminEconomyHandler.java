package com.hyperfactions.command.admin.handler;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.api.EconomyAPI;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
import com.hyperfactions.manager.EconomyManager;
import com.hyperfactions.util.CommandHelp;
import com.hyperfactions.util.HelpFormatter;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles /f admin economy commands.
 */
public class AdminEconomyHandler {

    private final HyperFactions hyperFactions;

    private static final String COLOR_CYAN = CommandUtil.COLOR_CYAN;
    private static final String COLOR_GREEN = CommandUtil.COLOR_GREEN;
    private static final String COLOR_RED = CommandUtil.COLOR_RED;
    private static final String COLOR_YELLOW = CommandUtil.COLOR_YELLOW;
    private static final String COLOR_GRAY = CommandUtil.COLOR_GRAY;
    private static final String COLOR_WHITE = CommandUtil.COLOR_WHITE;
    private static final String COLOR_GOLD = "#FFD700";

    private static Message prefix() { return CommandUtil.prefix(); }
    private static Message msg(String text, String color) { return CommandUtil.msg(text, color); }

    private boolean hasPermission(@Nullable PlayerRef player, String permission) {
        if (player == null) return true;
        return CommandUtil.hasPermission(player, permission);
    }

    public AdminEconomyHandler(HyperFactions hyperFactions) {
        this.hyperFactions = hyperFactions;
    }

    public void handleAdminEconomy(CommandContext ctx, @Nullable PlayerRef player, UUID senderUuid, String[] args) {
        if (!hasPermission(player, Permissions.ADMIN_ECONOMY)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        EconomyManager econ = hyperFactions.getEconomyManager();
        if (econ == null) {
            ctx.sendMessage(prefix().insert(msg("Economy system is not enabled.", COLOR_RED)));
            return;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            showAdminEconomyHelp(ctx);
            return;
        }

        String subCmd = args[0].toLowerCase();

        switch (subCmd) {
            case "balance", "bal" -> handleBalance(ctx, econ, args);
            case "set" -> handleSet(ctx, econ, senderUuid, args);
            case "add" -> handleAdd(ctx, econ, senderUuid, args);
            case "take", "remove" -> handleTake(ctx, econ, senderUuid, args);
            case "total" -> handleTotal(ctx, econ);
            case "reset" -> handleReset(ctx, econ, senderUuid, args);
            default -> ctx.sendMessage(prefix().insert(msg("Unknown economy command. Use /f admin economy help", COLOR_RED)));
        }
    }

    private void showAdminEconomyHelp(CommandContext ctx) {
        List<CommandHelp> commands = new ArrayList<>();
        commands.add(new CommandHelp("/f admin economy balance <faction>", "Show faction balance"));
        commands.add(new CommandHelp("/f admin economy set <faction> <amount>", "Set exact balance"));
        commands.add(new CommandHelp("/f admin economy add <faction> <amount>", "Add to balance"));
        commands.add(new CommandHelp("/f admin economy take <faction> <amount>", "Deduct from balance"));
        commands.add(new CommandHelp("/f admin economy total", "Show server total balance"));
        commands.add(new CommandHelp("/f admin economy reset <faction>", "Reset balance to 0"));
        ctx.sendMessage(HelpFormatter.buildHelp("Admin Economy", "Manage faction treasuries", commands, null));
    }

    // /f admin economy balance <faction>
    private void handleBalance(CommandContext ctx, EconomyManager econ, String[] args) {
        if (args.length < 2) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin economy balance <faction>", COLOR_YELLOW)));
            return;
        }
        String factionName = args[1];
        Faction faction = hyperFactions.getFactionManager().getFactionByName(factionName);
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("Faction not found: " + factionName, COLOR_RED)));
            return;
        }

        double balance = econ.getFactionBalance(faction.id());
        ctx.sendMessage(prefix().insert(msg(faction.name(), COLOR_CYAN))
                .insert(msg("'s treasury: ", COLOR_GRAY))
                .insert(msg(econ.formatCurrency(balance), COLOR_GOLD)));
    }

    // /f admin economy set <faction> <amount>
    private void handleSet(CommandContext ctx, EconomyManager econ, UUID senderUuid, String[] args) {
        if (args.length < 3) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin economy set <faction> <amount>", COLOR_YELLOW)));
            return;
        }
        Faction faction = resolveFaction(ctx, args[1]);
        if (faction == null) return;

        double amount = parseDouble(ctx, args[2]);
        if (Double.isNaN(amount)) return;

        if (amount < 0) {
            ctx.sendMessage(prefix().insert(msg("Balance cannot be negative.", COLOR_RED)));
            return;
        }

        double oldBalance = econ.getFactionBalance(faction.id());
        Logger.debugEconomy("Admin set balance: faction=%s amount=%.2f oldBalance=%.2f by=%s",
                faction.name(), amount, oldBalance, senderUuid);

        econ.setBalance(faction.id(), amount, senderUuid).thenAccept(result -> {
            if (result == EconomyAPI.TransactionResult.SUCCESS) {
                ctx.sendMessage(prefix().insert(msg("Set ", COLOR_GREEN))
                        .insert(msg(faction.name(), COLOR_CYAN))
                        .insert(msg("'s balance to ", COLOR_GREEN))
                        .insert(msg(econ.formatCurrency(amount), COLOR_GOLD))
                        .insert(msg(" (was " + econ.formatCurrency(oldBalance) + ")", COLOR_GRAY)));
            } else {
                ctx.sendMessage(prefix().insert(msg("Failed: " + result.name(), COLOR_RED)));
            }
        }).exceptionally(ex -> {
            Logger.severe("Admin economy set balance failed for %s", ex, faction.name());
            ctx.sendMessage(prefix().insert(msg("An error occurred.", COLOR_RED)));
            return null;
        });
    }

    // /f admin economy add <faction> <amount>
    private void handleAdd(CommandContext ctx, EconomyManager econ, UUID senderUuid, String[] args) {
        if (args.length < 3) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin economy add <faction> <amount>", COLOR_YELLOW)));
            return;
        }
        Faction faction = resolveFaction(ctx, args[1]);
        if (faction == null) return;

        double amount = parseDouble(ctx, args[2]);
        if (Double.isNaN(amount)) return;

        if (amount <= 0) {
            ctx.sendMessage(prefix().insert(msg("Amount must be positive.", COLOR_RED)));
            return;
        }

        String desc = "Admin added " + econ.formatCurrency(amount);
        Logger.debugEconomy("Admin add: faction=%s amount=%.2f by=%s", faction.name(), amount, senderUuid);

        econ.adminAdjust(faction.id(), amount, senderUuid, desc).thenAccept(result -> {
            if (result == EconomyAPI.TransactionResult.SUCCESS) {
                double newBalance = econ.getFactionBalance(faction.id());
                ctx.sendMessage(prefix().insert(msg("Added ", COLOR_GREEN))
                        .insert(msg(econ.formatCurrency(amount), COLOR_GOLD))
                        .insert(msg(" to ", COLOR_GREEN))
                        .insert(msg(faction.name(), COLOR_CYAN))
                        .insert(msg(" (balance: " + econ.formatCurrency(newBalance) + ")", COLOR_GRAY)));
            } else {
                ctx.sendMessage(prefix().insert(msg("Failed: " + result.name(), COLOR_RED)));
            }
        }).exceptionally(ex -> {
            Logger.severe("Admin economy add failed for %s", ex, faction.name());
            ctx.sendMessage(prefix().insert(msg("An error occurred.", COLOR_RED)));
            return null;
        });
    }

    // /f admin economy take <faction> <amount>
    private void handleTake(CommandContext ctx, EconomyManager econ, UUID senderUuid, String[] args) {
        if (args.length < 3) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin economy take <faction> <amount>", COLOR_YELLOW)));
            return;
        }
        Faction faction = resolveFaction(ctx, args[1]);
        if (faction == null) return;

        double amount = parseDouble(ctx, args[2]);
        if (Double.isNaN(amount)) return;

        if (amount <= 0) {
            ctx.sendMessage(prefix().insert(msg("Amount must be positive.", COLOR_RED)));
            return;
        }

        String desc = "Admin deducted " + econ.formatCurrency(amount);
        Logger.debugEconomy("Admin take: faction=%s amount=%.2f by=%s", faction.name(), amount, senderUuid);

        econ.adminAdjust(faction.id(), -amount, senderUuid, desc).thenAccept(result -> {
            if (result == EconomyAPI.TransactionResult.SUCCESS) {
                double newBalance = econ.getFactionBalance(faction.id());
                ctx.sendMessage(prefix().insert(msg("Deducted ", COLOR_GREEN))
                        .insert(msg(econ.formatCurrency(amount), COLOR_GOLD))
                        .insert(msg(" from ", COLOR_GREEN))
                        .insert(msg(faction.name(), COLOR_CYAN))
                        .insert(msg(" (balance: " + econ.formatCurrency(newBalance) + ")", COLOR_GRAY)));
            } else {
                ctx.sendMessage(prefix().insert(msg("Failed: " + result.name(), COLOR_RED)));
            }
        }).exceptionally(ex -> {
            Logger.severe("Admin economy take failed for %s", ex, faction.name());
            ctx.sendMessage(prefix().insert(msg("An error occurred.", COLOR_RED)));
            return null;
        });
    }

    // /f admin economy total
    private void handleTotal(CommandContext ctx, EconomyManager econ) {
        double total = econ.getServerTotalBalance();
        int count = econ.getFactionEconomyCount();
        double avg = count > 0 ? total / count : 0;

        ctx.sendMessage(prefix().insert(msg("Server Economy Statistics", COLOR_CYAN)));
        ctx.sendMessage(msg("  Total Balance: ", COLOR_GRAY)
                .insert(msg(econ.formatCurrency(total), COLOR_GOLD)));
        ctx.sendMessage(msg("  Factions: ", COLOR_GRAY)
                .insert(msg(String.valueOf(count), COLOR_WHITE)));
        ctx.sendMessage(msg("  Average Balance: ", COLOR_GRAY)
                .insert(msg(econ.formatCurrency(avg), COLOR_WHITE)));
    }

    // /f admin economy reset <faction>
    private void handleReset(CommandContext ctx, EconomyManager econ, UUID senderUuid, String[] args) {
        if (args.length < 2) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin economy reset <faction>", COLOR_YELLOW)));
            return;
        }
        Faction faction = resolveFaction(ctx, args[1]);
        if (faction == null) return;

        double oldBalance = econ.getFactionBalance(faction.id());
        Logger.debugEconomy("Admin reset: faction=%s oldBalance=%.2f by=%s",
                faction.name(), oldBalance, senderUuid);

        econ.setBalance(faction.id(), 0, senderUuid).thenAccept(result -> {
            if (result == EconomyAPI.TransactionResult.SUCCESS) {
                ctx.sendMessage(prefix().insert(msg("Reset ", COLOR_GREEN))
                        .insert(msg(faction.name(), COLOR_CYAN))
                        .insert(msg("'s balance to ", COLOR_GREEN))
                        .insert(msg(econ.formatCurrency(0), COLOR_GOLD))
                        .insert(msg(" (was " + econ.formatCurrency(oldBalance) + ")", COLOR_GRAY)));
            } else {
                ctx.sendMessage(prefix().insert(msg("Failed: " + result.name(), COLOR_RED)));
            }
        }).exceptionally(ex -> {
            Logger.severe("Admin economy reset failed for %s", ex, faction.name());
            ctx.sendMessage(prefix().insert(msg("An error occurred.", COLOR_RED)));
            return null;
        });
    }

    @Nullable
    private Faction resolveFaction(CommandContext ctx, String name) {
        Faction faction = hyperFactions.getFactionManager().getFactionByName(name);
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("Faction not found: " + name, COLOR_RED)));
        }
        return faction;
    }

    private double parseDouble(CommandContext ctx, String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            ctx.sendMessage(prefix().insert(msg("Invalid number: " + value, COLOR_RED)));
            return Double.NaN;
        }
    }
}
