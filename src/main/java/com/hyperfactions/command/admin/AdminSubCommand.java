package com.hyperfactions.command.admin;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.admin.handler.AdminBackupHandler;
import com.hyperfactions.command.admin.handler.AdminDebugHandler;
import com.hyperfactions.command.admin.handler.AdminEconomyHandler;
import com.hyperfactions.command.admin.handler.AdminImportHandler;
import com.hyperfactions.command.admin.handler.AdminIntegrationHandler;
import com.hyperfactions.command.admin.handler.AdminMapDecayHandler;
import com.hyperfactions.command.admin.handler.AdminPowerHandler;
import com.hyperfactions.command.admin.handler.AdminUpdateHandler;
import com.hyperfactions.command.admin.handler.AdminZoneHandler;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.CommandHelp;
import com.hyperfactions.util.HelpFormatter;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Container subcommand: /f admin
 * Routes to admin subcommands: reload, sync, zones, backup, debug, import, update, etc.
 * Opens admin GUI when called with no arguments.
 */
public class AdminSubCommand extends AbstractAsyncCommand {

    private final HyperFactions hyperFactions;
    private final HyperFactionsPlugin plugin;

    private static final UUID CONSOLE_UUID = new UUID(0L, 0L);

    // Handler instances
    private final AdminIntegrationHandler integrationHandler;
    private final AdminZoneHandler zoneHandler;
    private final AdminUpdateHandler updateHandler;
    private final AdminBackupHandler backupHandler;
    private final AdminImportHandler importHandler;
    private final AdminDebugHandler debugHandler;
    private final AdminPowerHandler powerHandler;
    private final AdminEconomyHandler economyHandler;
    private final AdminMapDecayHandler mapDecayHandler;

    public AdminSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
        super("admin", "Admin commands");
        this.hyperFactions = hyperFactions;
        this.plugin = plugin;
        setAllowsExtraArguments(true);

        // Initialize handlers
        this.integrationHandler = new AdminIntegrationHandler(hyperFactions);
        this.zoneHandler = new AdminZoneHandler(hyperFactions);
        this.updateHandler = new AdminUpdateHandler(hyperFactions);
        this.backupHandler = new AdminBackupHandler(hyperFactions);
        this.importHandler = new AdminImportHandler(hyperFactions);
        this.debugHandler = new AdminDebugHandler(hyperFactions);
        this.powerHandler = new AdminPowerHandler(hyperFactions, plugin);
        this.economyHandler = new AdminEconomyHandler(hyperFactions);
        this.mapDecayHandler = new AdminMapDecayHandler(hyperFactions);
    }

    @Override
    protected boolean canGeneratePermission() { return false; }

    private static Message prefix() { return CommandUtil.prefix(); }
    private static Message msg(String text, String color) { return CommandUtil.msg(text, color); }

    private boolean hasPermission(@Nullable PlayerRef player, String permission) {
        if (player == null) return true; // console always permitted
        return CommandUtil.hasPermission(player, permission);
    }

    private boolean requirePlayer(CommandContext ctx, boolean isPlayer) {
        if (!isPlayer) {
            ctx.sendMessage(prefix().insert(msg("This command can only be used by a player.", COLOR_RED)));
            return false;
        }
        return true;
    }

    private static final String COLOR_CYAN = CommandUtil.COLOR_CYAN;
    private static final String COLOR_GREEN = CommandUtil.COLOR_GREEN;
    private static final String COLOR_RED = CommandUtil.COLOR_RED;
    private static final String COLOR_YELLOW = CommandUtil.COLOR_YELLOW;
    private static final String COLOR_GRAY = CommandUtil.COLOR_GRAY;
    private static final String COLOR_WHITE = CommandUtil.COLOR_WHITE;

    @Override
    @NotNull
    protected CompletableFuture<Void> executeAsync(@NotNull CommandContext ctx) {
        boolean isPlayer = ctx.isPlayer();

        if (isPlayer) {
            // Player path — dispatch to world thread for safe ECS access
            // (same pattern as AbstractPlayerCommand)
            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) {
                ctx.sendMessage(prefix().insert(msg("Player context unavailable.", COLOR_RED)));
                return CompletableFuture.completedFuture(null);
            }
            Store<EntityStore> store = ref.getStore();
            World currentWorld = store.getExternalData().getWorld();

            return runAsync(ctx, () -> {
                PlayerRef player = store.getComponent(ref, PlayerRef.getComponentType());
                if (player == null) {
                    ctx.sendMessage(prefix().insert(msg("Could not find player entity.", COLOR_RED)));
                    return;
                }
                dispatchCommand(ctx, store, ref, player, currentWorld, true);
            }, currentWorld);
        } else {
            // Console path — no ECS access needed, run directly
            dispatchCommand(ctx, null, null, null, null, false);
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Dispatches the admin command after thread-safe context resolution.
     * For players, this runs on the world thread. For console, runs on the calling thread.
     */
    private void dispatchCommand(@NotNull CommandContext ctx, @Nullable Store<EntityStore> store,
                                 @Nullable Ref<EntityStore> ref, @Nullable PlayerRef player,
                                 @Nullable World currentWorld, boolean isPlayer) {

        if (!hasPermission(player, Permissions.ADMIN)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        String input = ctx.getInputString();
        String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
        // parts[0] = "faction/f/hf", parts[1] = "admin", parts[2+] = admin args
        String[] args = parts.length > 2 ? Arrays.copyOfRange(parts, 2, parts.length) : new String[0];

        // No args - open admin GUI (player) or show help (console)
        if (args.length == 0) {
            if (!isPlayer) {
                showAdminHelp(ctx);
            } else {
                Player playerEntity = store.getComponent(ref, Player.getComponentType());
                if (playerEntity == null) {
                    ctx.sendMessage(prefix().insert(msg("Could not find player entity.", COLOR_RED)));
                } else {
                    hyperFactions.getGuiManager().openAdminMain(playerEntity, ref, store, player);
                }
            }
            return;
        }

        String adminCmd = args[0].toLowerCase();

        // Show help for admin commands
        if (adminCmd.equals("help") || adminCmd.equals("?")) {
            showAdminHelp(ctx);
            return;
        }

        // Extract player position (only if player)
        int chunkX = 0, chunkZ = 0;
        if (isPlayer) {
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) {
                return;
            }
            Vector3d pos = transform.getPosition();
            chunkX = ChunkUtil.toChunkCoord(pos.getX());
            chunkZ = ChunkUtil.toChunkCoord(pos.getZ());
        }

        // Sender UUID for operations that track who performed them
        UUID senderUuid = isPlayer ? player.getUuid() : CONSOLE_UUID;

        // Sub-args for handler delegation
        String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

        switch (adminCmd) {
            // Console-compatible commands
            case "reload" -> handleReload(ctx, player);
            case "sync" -> handleSync(ctx, player);
            case "integrations" -> integrationHandler.handleIntegrations(ctx);
            case "integration" -> integrationHandler.handleIntegrationDetail(ctx, subArgs);
            case "update" -> updateHandler.handleAdminUpdate(ctx, senderUuid);
            case "rollback" -> updateHandler.handleAdminRollback(ctx);
            case "backup" -> backupHandler.handleAdminBackup(ctx, player, senderUuid, subArgs);
            case "import" -> importHandler.handleAdminImport(ctx, subArgs);
            case "debug" -> debugHandler.handleDebug(ctx, store, ref, player, currentWorld, subArgs);
            case "decay" -> mapDecayHandler.handleAdminDecay(ctx, player, subArgs);
            case "map" -> mapDecayHandler.handleAdminMap(ctx, player, subArgs);
            case "zones", "zone" -> zoneHandler.handleAdminZone(ctx, store, ref, player, currentWorld,
                    chunkX, chunkZ, subArgs, isPlayer, senderUuid);

            // Player-only commands (GUI / location)
            case "factions" -> {
                if (!requirePlayer(ctx, isPlayer)) break;
                Player playerEntity = store.getComponent(ref, Player.getComponentType());
                if (playerEntity != null) {
                    hyperFactions.getGuiManager().openAdminFactions(playerEntity, ref, store, player);
                }
            }
            case "config" -> {
                if (!requirePlayer(ctx, isPlayer)) break;
                Player playerEntity = store.getComponent(ref, Player.getComponentType());
                if (playerEntity != null) {
                    hyperFactions.getGuiManager().openAdminConfig(playerEntity, ref, store, player);
                }
            }
            case "backups" -> {
                if (!requirePlayer(ctx, isPlayer)) break;
                Player playerEntity = store.getComponent(ref, Player.getComponentType());
                if (playerEntity != null) {
                    hyperFactions.getGuiManager().openAdminBackups(playerEntity, ref, store, player);
                }
            }
            case "testgui" -> {
                if (!requirePlayer(ctx, isPlayer)) break;
                Player playerEntity = store.getComponent(ref, Player.getComponentType());
                if (playerEntity != null) {
                    hyperFactions.getGuiManager().openButtonTestPage(playerEntity, ref, store, player);
                }
            }
            case "safezone" -> { if (requirePlayer(ctx, isPlayer)) zoneHandler.handleSafezone(ctx, player, currentWorld, chunkX, chunkZ, args); }
            case "warzone" -> { if (requirePlayer(ctx, isPlayer)) zoneHandler.handleWarzone(ctx, player, currentWorld, chunkX, chunkZ, args); }
            case "removezone" -> { if (requirePlayer(ctx, isPlayer)) zoneHandler.handleRemovezone(ctx, currentWorld, chunkX, chunkZ); }
            case "zoneflag" -> { if (requirePlayer(ctx, isPlayer)) zoneHandler.handleZoneFlag(ctx, currentWorld.getName(), chunkX, chunkZ, subArgs); }
            case "clearhistory" -> powerHandler.handleClearHistory(ctx, player, subArgs);
            case "power" -> powerHandler.handleAdminPower(ctx, player, senderUuid, subArgs);
            case "economy", "econ", "treasury" -> economyHandler.handleAdminEconomy(ctx, player, senderUuid, subArgs);
            default -> ctx.sendMessage(prefix().insert(msg("Unknown admin command. Use /f admin help", COLOR_RED)));
        }
    }

    private void showAdminHelp(CommandContext ctx) {
        List<CommandHelp> commands = new ArrayList<>();
        commands.add(new CommandHelp("/f admin", "Open admin dashboard GUI"));
        commands.add(new CommandHelp("/f admin factions", "Manage all factions"));
        commands.add(new CommandHelp("/f admin zone", "Zone management"));
        commands.add(new CommandHelp("/f admin config", "Server configuration"));
        commands.add(new CommandHelp("/f admin backup", "Backup management"));
        commands.add(new CommandHelp("/f admin import", "Import from other plugins"));
        commands.add(new CommandHelp("/f admin update", "Check for updates"));
        commands.add(new CommandHelp("/f admin rollback", "Rollback to previous version"));
        commands.add(new CommandHelp("/f admin reload", "Reload configuration"));
        commands.add(new CommandHelp("/f admin sync", "Sync data from disk"));
        commands.add(new CommandHelp("/f admin debug", "Debug commands"));
        commands.add(new CommandHelp("/f admin decay", "Claim decay management"));
        commands.add(new CommandHelp("/f admin map", "World map management"));
        commands.add(new CommandHelp("/f admin safezone [name]", "Create SafeZone + claim chunk"));
        commands.add(new CommandHelp("/f admin warzone [name]", "Create WarZone + claim chunk"));
        commands.add(new CommandHelp("/f admin removezone", "Unclaim chunk from zone"));
        commands.add(new CommandHelp("/f admin zoneflag <flag> <value>", "Set zone flag"));
        commands.add(new CommandHelp("/f admin integrations", "Summary of all integrations"));
        commands.add(new CommandHelp("/f admin integration <name>", "Detailed integration status"));
        commands.add(new CommandHelp("/f admin clearhistory <player>", "Clear player membership history"));
        commands.add(new CommandHelp("/f admin power", "Admin power management"));
        commands.add(new CommandHelp("/f admin economy", "Economy/treasury management"));
        ctx.sendMessage(HelpFormatter.buildHelp("Admin Commands", "Server administration", commands, null));
    }

    // === Reload ===
    private void handleReload(CommandContext ctx, PlayerRef player) {
        if (!hasPermission(player, Permissions.ADMIN)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        plugin.reloadConfig();
        ctx.sendMessage(prefix().insert(msg("Configuration reloaded.", COLOR_GREEN)));
    }

    // === Sync ===
    private void handleSync(CommandContext ctx, PlayerRef player) {
        if (!hasPermission(player, Permissions.ADMIN)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        ctx.sendMessage(prefix().insert(msg("Syncing faction data from disk...", COLOR_CYAN)));

        hyperFactions.getFactionManager().syncFromDisk().thenAccept(result -> {
            ctx.sendMessage(prefix().insert(Message.join(
                msg("Sync complete: ", COLOR_GREEN),
                msg(result.factionsUpdated() + " factions updated, ", COLOR_GRAY),
                msg(result.membersAdded() + " members added, ", COLOR_GRAY),
                msg(result.membersUpdated() + " members updated.", COLOR_GRAY)
            )));
        }).exceptionally(e -> {
            ctx.sendMessage(prefix().insert(msg("Sync failed: " + e.getMessage(), COLOR_RED)));
            return null;
        });
    }
}
