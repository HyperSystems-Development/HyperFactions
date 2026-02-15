package com.hyperfactions.command.admin;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.backup.BackupManager;
import com.hyperfactions.backup.BackupMetadata;
import com.hyperfactions.backup.BackupType;
import com.hyperfactions.command.util.CommandUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.config.modules.GravestoneConfig;
import com.hyperfactions.integration.GravestoneIntegration;
import com.hyperfactions.integration.HyperPermsIntegration;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.integration.orbis.OrbisGuardIntegration;
import com.hyperfactions.integration.orbis.OrbisMixinsIntegration;
import com.hyperfactions.integration.papi.PlaceholderAPIIntegration;
import com.hyperfactions.integration.wiflow.WiFlowPlaceholderIntegration;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionLog;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.MembershipRecord;
import com.hyperfactions.data.PlayerData;
import com.hyperfactions.data.PlayerPower;
import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneFlags;
import com.hyperfactions.data.ZoneType;
import com.hyperfactions.importer.ElbaphFactionsImporter;
import com.hyperfactions.importer.HyFactionsImporter;
import com.hyperfactions.importer.ImportResult;
import com.hyperfactions.manager.ConfirmationManager;
import com.hyperfactions.manager.ZoneManager;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.CommandHelp;
import com.hyperfactions.util.HelpFormatter;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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

    public AdminSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
        super("admin", "Admin commands");
        this.hyperFactions = hyperFactions;
        this.plugin = plugin;
        setAllowsExtraArguments(true);
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

        switch (adminCmd) {
            // Console-compatible commands
            case "reload" -> handleReload(ctx, player);
            case "sync" -> handleSync(ctx, player);
            case "integrations" -> handleIntegrations(ctx);
            case "integration" -> handleIntegrationDetail(ctx, Arrays.copyOfRange(args, 1, args.length));
            case "update" -> handleAdminUpdate(ctx, senderUuid);
            case "rollback" -> handleAdminRollback(ctx);
            case "backup" -> handleAdminBackup(ctx, player, senderUuid, Arrays.copyOfRange(args, 1, args.length));
            case "import" -> handleAdminImport(ctx, Arrays.copyOfRange(args, 1, args.length));
            case "debug" -> handleDebug(ctx, store, ref, player, currentWorld, Arrays.copyOfRange(args, 1, args.length));
            case "decay" -> handleAdminDecay(ctx, player, Arrays.copyOfRange(args, 1, args.length));
            case "map" -> handleAdminMap(ctx, player, Arrays.copyOfRange(args, 1, args.length));
            case "zones", "zone" -> handleAdminZone(ctx, store, ref, player, currentWorld,
                    chunkX, chunkZ, Arrays.copyOfRange(args, 1, args.length), isPlayer, senderUuid);

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
            case "safezone" -> { if (requirePlayer(ctx, isPlayer)) handleSafezone(ctx, player, currentWorld, chunkX, chunkZ, args); }
            case "warzone" -> { if (requirePlayer(ctx, isPlayer)) handleWarzone(ctx, player, currentWorld, chunkX, chunkZ, args); }
            case "removezone" -> { if (requirePlayer(ctx, isPlayer)) handleRemovezone(ctx, currentWorld, chunkX, chunkZ); }
            case "zoneflag" -> { if (requirePlayer(ctx, isPlayer)) handleZoneFlag(ctx, currentWorld.getName(), chunkX, chunkZ, Arrays.copyOfRange(args, 1, args.length)); }
            case "clearhistory" -> handleClearHistory(ctx, player, Arrays.copyOfRange(args, 1, args.length));
            case "power" -> handleAdminPower(ctx, player, senderUuid, Arrays.copyOfRange(args, 1, args.length));
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
        ctx.sendMessage(HelpFormatter.buildHelp("Admin Commands", "Server administration", commands, null));
    }

    // === Integration Commands ===
    private void handleIntegrations(CommandContext ctx) {
        ctx.sendMessage(prefix().insert(msg("Integration Status", COLOR_CYAN)));

        // --- Permissions ---
        ctx.sendMessage(msg("  Permissions:", COLOR_YELLOW));

        boolean hpAvailable = HyperPermsIntegration.isAvailable();
        ctx.sendMessage(msg("    HyperPerms: " + (hpAvailable ? "Active" : "Not Found"),
                hpAvailable ? COLOR_GREEN : COLOR_GRAY));

        // Provider names are filtered by availability (triggers lazy init)
        String providerNames = PermissionManager.get().getProviderNames();

        boolean luckPermsAvailable = providerNames.contains("LuckPerms");
        ctx.sendMessage(msg("    LuckPerms: " + (luckPermsAvailable ? "Active" : "Not Found"),
                luckPermsAvailable ? COLOR_GREEN : COLOR_GRAY));

        boolean vaultAvailable = providerNames.contains("VaultUnlocked");
        boolean vaultInstalled = false;
        if (!vaultAvailable) {
            try {
                Class.forName("net.cfh.vault.VaultUnlocked");
                vaultInstalled = true;
            } catch (ClassNotFoundException ignored) {}
        }
        String vuStatus = vaultAvailable ? "Active" :
                (vaultInstalled ? "Installed (no perm provider)" : "Not Installed");
        ctx.sendMessage(msg("    VaultUnlocked: " + vuStatus,
                vaultAvailable ? COLOR_GREEN : (vaultInstalled ? COLOR_YELLOW : COLOR_GRAY)));

        boolean nativeAvailable = providerNames.contains("HytaleNative");
        ctx.sendMessage(msg("    HytaleNative: " + (nativeAvailable ? "Active" : "Not Found"),
                nativeAvailable ? COLOR_GREEN : COLOR_GRAY));

        // --- Protection ---
        ctx.sendMessage(msg("  Protection:", COLOR_YELLOW));

        boolean ogAvailable = OrbisGuardIntegration.isAvailable();
        ctx.sendMessage(msg("    OrbisGuard API: " + (ogAvailable ? "Active" : "Not Detected"),
                ogAvailable ? COLOR_GREEN : COLOR_GRAY));

        boolean mixinsAvailable = OrbisMixinsIntegration.isMixinsAvailable();
        ctx.sendMessage(msg("    OrbisGuard-Mixins: " + (mixinsAvailable ? "Active (11 hooks)" : "Not Detected"),
                mixinsAvailable ? COLOR_GREEN : COLOR_GRAY));

        GravestoneIntegration gs = hyperFactions.getProtectionChecker().getGravestoneIntegration();
        boolean gsAvailable = gs != null && gs.isAvailable();
        boolean gsEnabled = ConfigManager.get().gravestones().isEnabled();
        String gsStatus = !gsAvailable ? "Not Found" : (gsEnabled ? "Active" : "Disabled");
        String gsColor = gsAvailable && gsEnabled ? COLOR_GREEN : (gsAvailable ? COLOR_YELLOW : COLOR_GRAY);
        ctx.sendMessage(msg("    Gravestones: " + gsStatus, gsColor));

        // --- Placeholders ---
        ctx.sendMessage(msg("  Placeholders:", COLOR_YELLOW));

        boolean papiAvailable = PlaceholderAPIIntegration.isAvailable();
        ctx.sendMessage(msg("    PlaceholderAPI: " + (papiAvailable ? "Active" : "Not Found"),
                papiAvailable ? COLOR_GREEN : COLOR_GRAY));

        boolean wiflowAvailable;
        try {
            wiflowAvailable = WiFlowPlaceholderIntegration.isAvailable();
        } catch (NoClassDefFoundError e) {
            wiflowAvailable = false;
        }
        ctx.sendMessage(msg("    WiFlow PAPI: " + (wiflowAvailable ? "Active" : "Not Found"),
                wiflowAvailable ? COLOR_GREEN : COLOR_GRAY));

        ctx.sendMessage(msg("  Use /f admin integration <name> for details", COLOR_GRAY));
    }

    private void handleIntegrationDetail(CommandContext ctx, String[] args) {
        if (args.length == 0) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin integration <name>", COLOR_RED)));
            ctx.sendMessage(msg("  Available: hyperperms, luckperms, vaultunlocked, native, orbisguard, mixins, gravestones, papi, wiflow", COLOR_GRAY));
            return;
        }

        String name = args[0].toLowerCase();
        switch (name) {
            case "gravestones", "gravestone", "gs" -> handleIntegrationGravestones(ctx);
            case "hyperperms", "perms", "hp" -> handleIntegrationPermissions(ctx);
            case "luckperms", "lp" -> handleIntegrationLuckPerms(ctx);
            case "vaultunlocked", "vault", "vu" -> handleIntegrationVaultUnlocked(ctx);
            case "hytale", "native", "hytaleNative" -> handleIntegrationHytaleNative(ctx);
            case "orbisguard", "orbis", "og" -> handleIntegrationOrbisGuard(ctx);
            case "mixins", "orbismixins", "om" -> handleIntegrationMixins(ctx);
            case "placeholderapi", "papi" -> handleIntegrationPAPI(ctx);
            case "wiflow", "wflow" -> handleIntegrationWiFlow(ctx);
            default -> ctx.sendMessage(prefix().insert(msg("Unknown integration: " + name +
                    ". Available: hyperperms, luckperms, vaultunlocked, native, orbisguard, mixins, gravestones, papi, wiflow", COLOR_RED)));
        }
    }

    private void handleIntegrationGravestones(CommandContext ctx) {
        GravestoneIntegration gs = hyperFactions.getProtectionChecker().getGravestoneIntegration();
        GravestoneConfig config = ConfigManager.get().gravestones();
        boolean pluginDetected = gs != null && gs.isAvailable();

        ctx.sendMessage(prefix().insert(msg("Gravestone Integration", COLOR_CYAN)));
        ctx.sendMessage(msg("  Plugin: " + (pluginDetected ? "Detected (v2 API)" : "Not Found"),
                pluginDetected ? COLOR_GREEN : COLOR_GRAY));
        ctx.sendMessage(msg("  Integration: " + (config.isEnabled() ? "Enabled" : "Disabled"),
                config.isEnabled() ? COLOR_GREEN : COLOR_RED));

        if (pluginDetected) {
            ctx.sendMessage(msg("  AccessChecker: Registered", COLOR_GREEN));
            ctx.sendMessage(msg("  Event Listeners: Active", COLOR_GREEN));
        }

        ctx.sendMessage(msg("  Territory Config:", COLOR_CYAN));
        ctx.sendMessage(msg("    protectInOwnTerritory: " + config.isProtectInOwnTerritory(), COLOR_GRAY));
        ctx.sendMessage(msg("    factionMembersCanAccess: " + config.isFactionMembersCanAccess(), COLOR_GRAY));
        ctx.sendMessage(msg("    alliesCanAccess: " + config.isAlliesCanAccess(), COLOR_GRAY));
        ctx.sendMessage(msg("    protectInEnemyTerritory: " + config.isProtectInEnemyTerritory(), COLOR_GRAY));
        ctx.sendMessage(msg("    protectInNeutralTerritory: " + config.isProtectInNeutralTerritory(), COLOR_GRAY));
        ctx.sendMessage(msg("    enemiesCanLootInOwnTerritory: " + config.isEnemiesCanLootInOwnTerritory(), COLOR_GRAY));
        ctx.sendMessage(msg("  Zone/Wilderness Config:", COLOR_CYAN));
        ctx.sendMessage(msg("    protectInSafeZone: " + config.isProtectInSafeZone(), COLOR_GRAY));
        ctx.sendMessage(msg("    protectInWarZone: " + config.isProtectInWarZone(), COLOR_GRAY));
        ctx.sendMessage(msg("    protectInWilderness: " + config.isProtectInWilderness(), COLOR_GRAY));
        ctx.sendMessage(msg("  Features:", COLOR_CYAN));
        ctx.sendMessage(msg("    announceDeathLocation: " + config.isAnnounceDeathLocation(), COLOR_GRAY));
        ctx.sendMessage(msg("  Raid/War (placeholder):", COLOR_CYAN));
        ctx.sendMessage(msg("    allowLootDuringRaid: " + config.isAllowLootDuringRaid(), COLOR_GRAY));
        ctx.sendMessage(msg("    allowLootDuringWar: " + config.isAllowLootDuringWar(), COLOR_GRAY));
    }

    private void handleIntegrationPermissions(CommandContext ctx) {
        ctx.sendMessage(prefix().insert(msg("Permission Integration", COLOR_CYAN)));

        // HyperPerms direct integration
        boolean hpAvailable = HyperPermsIntegration.isAvailable();
        ctx.sendMessage(msg("  HyperPerms: " + (hpAvailable ? "Available" : "Not Found"),
                hpAvailable ? COLOR_GREEN : COLOR_GRAY));
        if (hpAvailable) {
            ctx.sendMessage(msg("    Status: " + HyperPermsIntegration.getDetailedStatus(), COLOR_GRAY));
        } else {
            String error = HyperPermsIntegration.getInitError();
            if (error != null) {
                ctx.sendMessage(msg("    Error: " + error, COLOR_RED));
            }
        }

        // Provider chain
        PermissionManager pm = PermissionManager.get();
        ctx.sendMessage(msg("  Provider Chain:", COLOR_CYAN));
        ctx.sendMessage(msg("    Priority: VaultUnlocked > HyperPerms > LuckPerms > HytaleNative", COLOR_GRAY));
        ctx.sendMessage(msg("    Active Providers: " + pm.getProviderCount() + " (" + pm.getProviderNames() + ")", COLOR_WHITE));

        // Fallback config
        boolean allowWithout = ConfigManager.get().isAllowWithoutPermissionMod();
        ctx.sendMessage(msg("  Fallback:", COLOR_CYAN));
        ctx.sendMessage(msg("    allowWithoutPermissionMod: " + allowWithout,
                allowWithout ? COLOR_GREEN : COLOR_GRAY));
    }

    private void handleIntegrationLuckPerms(CommandContext ctx) {
        String providerNames = PermissionManager.get().getProviderNames();
        boolean available = providerNames.contains("LuckPerms");

        ctx.sendMessage(prefix().insert(msg("LuckPerms Integration", COLOR_CYAN)));
        ctx.sendMessage(msg("  Provider Status: " + (available ? "Active" : "Not Found"),
                available ? COLOR_GREEN : COLOR_GRAY));

        if (available) {
            ctx.sendMessage(msg("  Features:", COLOR_CYAN));
            ctx.sendMessage(msg("    Permission Checks: Active (direct API)", COLOR_GREEN));
            ctx.sendMessage(msg("    Prefix/Suffix: Active (via CachedMetaData)", COLOR_GREEN));
            ctx.sendMessage(msg("    Primary Group: Active", COLOR_GREEN));
            ctx.sendMessage(msg("    Permission Registration: Active (PermissionRegistry)", COLOR_GREEN));
        } else {
            // Check if LuckPerms is installed but provider failed
            boolean classPresent = false;
            try {
                Class.forName("net.luckperms.api.LuckPermsProvider");
                classPresent = true;
            } catch (ClassNotFoundException ignored) {}

            if (classPresent) {
                boolean nativeAvailable = providerNames.contains("HytaleNative");
                ctx.sendMessage(msg("  LuckPerms is installed but direct API not available", COLOR_YELLOW));
                if (nativeAvailable) {
                    ctx.sendMessage(msg("  Permissions routed via HytaleNative provider", COLOR_GREEN));
                    ctx.sendMessage(msg("  Note: Prefix/suffix not available through HytaleNative", COLOR_YELLOW));
                }
            } else {
                ctx.sendMessage(msg("  LuckPerms is not installed", COLOR_GRAY));
            }
        }

        ctx.sendMessage(msg("  SoftDependency: manifest.json declares LuckPerms", COLOR_GRAY));
        ctx.sendMessage(msg("  Lazy Init: Retries if LuckPerms loads after HyperFactions", COLOR_GRAY));
    }

    private void handleIntegrationVaultUnlocked(CommandContext ctx) {
        String providerNames = PermissionManager.get().getProviderNames();
        boolean available = providerNames.contains("VaultUnlocked");

        ctx.sendMessage(prefix().insert(msg("VaultUnlocked Integration", COLOR_CYAN)));
        ctx.sendMessage(msg("  Provider Status: " + (available ? "Active" : "Not Found"),
                available ? COLOR_GREEN : COLOR_GRAY));

        if (available) {
            ctx.sendMessage(msg("  Features:", COLOR_CYAN));
            ctx.sendMessage(msg("    Permission Checks: Active (PermissionUnlocked.has)", COLOR_GREEN));
            ctx.sendMessage(msg("    Prefix/Suffix: Active (ChatUnlocked)", COLOR_GREEN));
            ctx.sendMessage(msg("    Primary Group: Active", COLOR_GREEN));
            ctx.sendMessage(msg("  API:", COLOR_CYAN));
            ctx.sendMessage(msg("    Main Class: net.cfh.vault.VaultUnlocked", COLOR_GRAY));
            ctx.sendMessage(msg("    Vault2 API: net.milkbowl.vault2.*", COLOR_GRAY));
        } else {
            boolean classPresent = false;
            try {
                Class.forName("net.cfh.vault.VaultUnlocked");
                classPresent = true;
            } catch (ClassNotFoundException ignored) {}

            if (classPresent) {
                ctx.sendMessage(msg("  VaultUnlocked installed but no permission provider registered", COLOR_YELLOW));
                ctx.sendMessage(msg("  LuckPerms does not register with VaultUnlocked on Hytale", COLOR_GRAY));
                ctx.sendMessage(msg("  VaultUnlocked is used for economy (Ecotale) only", COLOR_GRAY));
            } else {
                ctx.sendMessage(msg("  VaultUnlocked is not installed", COLOR_GRAY));
            }
        }

        ctx.sendMessage(msg("  SoftDependency: manifest.json declares VaultUnlocked", COLOR_GRAY));
        ctx.sendMessage(msg("  Lazy Init: Retries if VaultUnlocked loads after HyperFactions", COLOR_GRAY));
    }

    private void handleIntegrationHytaleNative(CommandContext ctx) {
        String providerNames = PermissionManager.get().getProviderNames();
        boolean available = providerNames.contains("HytaleNative");

        ctx.sendMessage(prefix().insert(msg("HytaleNative Integration", COLOR_CYAN)));
        ctx.sendMessage(msg("  Provider Status: " + (available ? "Active" : "Not Found"),
                available ? COLOR_GREEN : COLOR_GRAY));

        ctx.sendMessage(msg("  Description:", COLOR_CYAN));
        ctx.sendMessage(msg("    Fallback provider that delegates to Hytale's PermissionsModule", COLOR_GRAY));
        ctx.sendMessage(msg("    Catches any plugin using PermissionsModule.addProvider()", COLOR_GRAY));

        if (available) {
            ctx.sendMessage(msg("  Features:", COLOR_CYAN));
            ctx.sendMessage(msg("    Permission Checks: Active (via PermissionsModule)", COLOR_GREEN));
            ctx.sendMessage(msg("    Prefix/Suffix: Not supported (PermissionsModule limitation)", COLOR_YELLOW));
            ctx.sendMessage(msg("    Primary Group: Not supported", COLOR_YELLOW));
            ctx.sendMessage(msg("  Note: Lowest priority — only used when direct providers can't answer", COLOR_GRAY));
        }
    }

    private void handleIntegrationOrbisGuard(CommandContext ctx) {
        boolean available = OrbisGuardIntegration.isAvailable();

        ctx.sendMessage(prefix().insert(msg("OrbisGuard API Integration", COLOR_CYAN)));
        ctx.sendMessage(msg("  API Status: " + (available ? "Detected" : "Not Detected"),
                available ? COLOR_GREEN : COLOR_GRAY));

        if (available) {
            ctx.sendMessage(msg("  Claim Conflict Detection: Active", COLOR_GREEN));
            ctx.sendMessage(msg("    Prevents faction claims in OrbisGuard-protected regions", COLOR_GRAY));
        } else {
            ctx.sendMessage(msg("  Claim Conflict Detection: Disabled", COLOR_GRAY));
            ctx.sendMessage(msg("    OrbisGuard not installed - no region conflict checks", COLOR_GRAY));
        }
    }

    private void handleIntegrationMixins(CommandContext ctx) {
        boolean available = OrbisMixinsIntegration.isMixinsAvailable();

        ctx.sendMessage(prefix().insert(msg("OrbisGuard-Mixins Integration", COLOR_CYAN)));
        ctx.sendMessage(msg("  Status: " + (available ? "Active" : "Not Detected"),
                available ? COLOR_GREEN : COLOR_GRAY));

        if (available) {
            ctx.sendMessage(msg("  Registered Hooks:", COLOR_CYAN));
            ctx.sendMessage(msg("    Pickup: " + (OrbisMixinsIntegration.isPickupMixinLoaded() ? "Loaded" : "Pending"),
                    OrbisMixinsIntegration.isPickupMixinLoaded() ? COLOR_GREEN : COLOR_YELLOW));
            ctx.sendMessage(msg("    Death: " + (OrbisMixinsIntegration.isDeathMixinLoaded() ? "Loaded" : "Pending"),
                    OrbisMixinsIntegration.isDeathMixinLoaded() ? COLOR_GREEN : COLOR_YELLOW));
            ctx.sendMessage(msg("    Durability: " + (OrbisMixinsIntegration.isDurabilityMixinLoaded() ? "Loaded" : "Pending"),
                    OrbisMixinsIntegration.isDurabilityMixinLoaded() ? COLOR_GREEN : COLOR_YELLOW));
            ctx.sendMessage(msg("    Seating: " + (OrbisMixinsIntegration.isSeatingMixinLoaded() ? "Loaded" : "Pending"),
                    OrbisMixinsIntegration.isSeatingMixinLoaded() ? COLOR_GREEN : COLOR_YELLOW));
            ctx.sendMessage(msg("  Additional hooks: hammer, explosion, command, use, harvest, place, spawn", COLOR_GRAY));
        } else {
            ctx.sendMessage(msg("  Mixin-dependent zone flags will be disabled:", COLOR_GRAY));
            ctx.sendMessage(msg("    item_pickup_manual, invincible_items, keep_inventory, npc_spawning", COLOR_GRAY));
        }
    }

    private void handleIntegrationPAPI(CommandContext ctx) {
        boolean available = PlaceholderAPIIntegration.isAvailable();

        ctx.sendMessage(prefix().insert(msg("PlaceholderAPI Integration", COLOR_CYAN)));
        ctx.sendMessage(msg("  Status: " + (available ? "Active" : "Not Found"),
                available ? COLOR_GREEN : COLOR_GRAY));

        if (available) {
            ctx.sendMessage(msg("  Expansion: Registered", COLOR_GREEN));
            ctx.sendMessage(msg("  Prefix: hyperfactions", COLOR_GRAY));
            ctx.sendMessage(msg("  Placeholders: 73+", COLOR_GRAY));
            ctx.sendMessage(msg("  Categories: faction info, player power, relations, claims, roles", COLOR_GRAY));
        }
    }

    private void handleIntegrationWiFlow(CommandContext ctx) {
        boolean available = WiFlowPlaceholderIntegration.isAvailable();

        ctx.sendMessage(prefix().insert(msg("WiFlow Placeholder Integration", COLOR_CYAN)));
        ctx.sendMessage(msg("  Status: " + (available ? "Active" : "Not Found"),
                available ? COLOR_GREEN : COLOR_GRAY));

        if (available) {
            ctx.sendMessage(msg("  Expansion: Registered", COLOR_GREEN));
            ctx.sendMessage(msg("  API: WiFlowPlaceholderAPI", COLOR_GRAY));
        }
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
            ctx.sendMessage(prefix().insert(com.hypixel.hytale.server.core.Message.join(
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

    // === Quick zone creation shortcuts ===
    private void handleSafezone(CommandContext ctx, PlayerRef player, World world, int chunkX, int chunkZ, String[] args) {
        String zoneName = args.length > 1 ? args[1] : "SafeZone-" + chunkX + "_" + chunkZ;
        ZoneManager.ZoneResult result = hyperFactions.getZoneManager().createZone(
            zoneName, ZoneType.SAFE, world.getName(), chunkX, chunkZ, player.getUuid()
        );
        if (result == ZoneManager.ZoneResult.SUCCESS) {
            ctx.sendMessage(prefix().insert(msg("Created SafeZone '" + zoneName + "' at " + chunkX + ", " + chunkZ, COLOR_GREEN)));
        } else if (result == ZoneManager.ZoneResult.CHUNK_CLAIMED) {
            ctx.sendMessage(prefix().insert(msg("Cannot create zone: This chunk is claimed by a faction.", COLOR_RED)));
        } else if (result == ZoneManager.ZoneResult.ALREADY_EXISTS) {
            ctx.sendMessage(prefix().insert(msg("A zone already exists at this location.", COLOR_RED)));
        } else if (result == ZoneManager.ZoneResult.NAME_TAKEN) {
            ctx.sendMessage(prefix().insert(msg("A zone with that name already exists.", COLOR_RED)));
        } else {
            ctx.sendMessage(prefix().insert(msg("Failed: " + result, COLOR_RED)));
        }
    }

    private void handleWarzone(CommandContext ctx, PlayerRef player, World world, int chunkX, int chunkZ, String[] args) {
        String zoneName = args.length > 1 ? args[1] : "WarZone-" + chunkX + "_" + chunkZ;
        ZoneManager.ZoneResult result = hyperFactions.getZoneManager().createZone(
            zoneName, ZoneType.WAR, world.getName(), chunkX, chunkZ, player.getUuid()
        );
        if (result == ZoneManager.ZoneResult.SUCCESS) {
            ctx.sendMessage(prefix().insert(msg("Created WarZone '" + zoneName + "' at " + chunkX + ", " + chunkZ, COLOR_GREEN)));
        } else if (result == ZoneManager.ZoneResult.CHUNK_CLAIMED) {
            ctx.sendMessage(prefix().insert(msg("Cannot create zone: This chunk is claimed by a faction.", COLOR_RED)));
        } else if (result == ZoneManager.ZoneResult.ALREADY_EXISTS) {
            ctx.sendMessage(prefix().insert(msg("A zone already exists at this location.", COLOR_RED)));
        } else if (result == ZoneManager.ZoneResult.NAME_TAKEN) {
            ctx.sendMessage(prefix().insert(msg("A zone with that name already exists.", COLOR_RED)));
        } else {
            ctx.sendMessage(prefix().insert(msg("Failed: " + result, COLOR_RED)));
        }
    }

    private void handleRemovezone(CommandContext ctx, World world, int chunkX, int chunkZ) {
        ZoneManager.ZoneResult result = hyperFactions.getZoneManager().unclaimChunkAt(world.getName(), chunkX, chunkZ);
        if (result == ZoneManager.ZoneResult.SUCCESS) {
            ctx.sendMessage(prefix().insert(msg("Unclaimed chunk from zone.", COLOR_GREEN)));
        } else {
            ctx.sendMessage(prefix().insert(msg("No zone chunk found at this location.", COLOR_RED)));
        }
    }

    // === Admin Zone Subcommands ===
    private void handleAdminZone(CommandContext ctx, @Nullable Store<EntityStore> store, @Nullable Ref<EntityStore> ref,
                                 @Nullable PlayerRef player, @Nullable World world,
                                 int chunkX, int chunkZ, String[] args, boolean isPlayer, UUID senderUuid) {
        // No args - open zone GUI (player) or show zone list (console)
        if (args.length == 0) {
            if (!isPlayer) {
                handleZoneList(ctx);
                return;
            }
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openAdminZone(playerEntity, ref, store, player);
            }
            return;
        }

        String subCmd = args[0].toLowerCase();
        String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
        String worldName = world != null ? world.getName() : null;

        switch (subCmd) {
            case "list" -> handleZoneList(ctx);
            case "create" -> handleZoneCreate(ctx, worldName != null ? worldName : "world", senderUuid, subArgs);
            case "delete" -> handleZoneDelete(ctx, subArgs);
            case "rename" -> handleZoneRename(ctx, subArgs);
            case "info" -> {
                if (subArgs.length == 0 && !isPlayer) {
                    ctx.sendMessage(prefix().insert(msg("Usage: /f admin zone info <name>", COLOR_RED)));
                } else {
                    handleZoneInfo(ctx, worldName, chunkX, chunkZ, subArgs);
                }
            }
            case "claim" -> { if (requirePlayer(ctx, isPlayer)) handleZoneClaim(ctx, worldName, chunkX, chunkZ, subArgs); }
            case "unclaim" -> { if (requirePlayer(ctx, isPlayer)) handleZoneUnclaim(ctx, worldName, chunkX, chunkZ); }
            case "radius" -> { if (requirePlayer(ctx, isPlayer)) handleZoneRadius(ctx, worldName, chunkX, chunkZ, subArgs); }
            default -> ctx.sendMessage(prefix().insert(msg("Unknown zone command. Use /f admin help", COLOR_RED)));
        }
    }

    private void handleZoneList(CommandContext ctx) {
        var zones = hyperFactions.getZoneManager().getAllZones();
        if (zones.isEmpty()) {
            ctx.sendMessage(prefix().insert(msg("No zones defined.", COLOR_GRAY)));
            return;
        }

        ctx.sendMessage(msg("=== Zones (" + zones.size() + ") ===", COLOR_CYAN).bold(true));
        for (Zone zone : zones) {
            String typeColor = zone.isSafeZone() ? "#2dd4bf" : "#c084fc";
            ctx.sendMessage(msg("  " + zone.name(), typeColor)
                .insert(msg(" (" + zone.type().name() + ", " + zone.getChunkCount() + " chunks)", COLOR_GRAY)));
        }
    }

    private void handleZoneCreate(CommandContext ctx, String worldName, UUID createdBy, String[] args) {
        if (args.length < 2) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin zone create <safe|war> <name>", COLOR_RED)));
            return;
        }

        String typeStr = args[0].toLowerCase();
        String name = args[1];

        ZoneType type;
        if (typeStr.equals("safe") || typeStr.equals("safezone")) {
            type = ZoneType.SAFE;
        } else if (typeStr.equals("war") || typeStr.equals("warzone")) {
            type = ZoneType.WAR;
        } else {
            ctx.sendMessage(prefix().insert(msg("Invalid zone type. Use 'safe' or 'war'", COLOR_RED)));
            return;
        }

        ZoneManager.ZoneResult result = hyperFactions.getZoneManager().createZone(name, type, worldName, createdBy);
        switch (result) {
            case SUCCESS -> ctx.sendMessage(prefix().insert(msg("Created " + type.getDisplayName() + " '" + name + "' (empty, use claim to add chunks)", COLOR_GREEN)));
            case NAME_TAKEN -> ctx.sendMessage(prefix().insert(msg("A zone with that name already exists.", COLOR_RED)));
            case INVALID_NAME -> ctx.sendMessage(prefix().insert(msg("Invalid zone name. Must be 1-32 characters.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed: " + result, COLOR_RED)));
        }
    }

    private void handleZoneDelete(CommandContext ctx, String[] args) {
        if (args.length < 1) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin zone delete <name>", COLOR_RED)));
            return;
        }

        String name = args[0];
        Zone zone = hyperFactions.getZoneManager().getZoneByName(name);
        if (zone == null) {
            ctx.sendMessage(prefix().insert(msg("Zone '" + name + "' not found.", COLOR_RED)));
            return;
        }

        ZoneManager.ZoneResult result = hyperFactions.getZoneManager().removeZone(zone.id());
        if (result == ZoneManager.ZoneResult.SUCCESS) {
            ctx.sendMessage(prefix().insert(msg("Deleted zone '" + name + "' (" + zone.getChunkCount() + " chunks released)", COLOR_GREEN)));
        } else {
            ctx.sendMessage(prefix().insert(msg("Failed to delete zone: " + result, COLOR_RED)));
        }
    }

    private void handleZoneRename(CommandContext ctx, String[] args) {
        if (args.length < 2) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin zone rename <current-name> <new-name>", COLOR_RED)));
            return;
        }

        String currentName = args[0];
        String newName = args[1];

        Zone zone = hyperFactions.getZoneManager().getZoneByName(currentName);
        if (zone == null) {
            ctx.sendMessage(prefix().insert(msg("Zone '" + currentName + "' not found.", COLOR_RED)));
            return;
        }

        ZoneManager.ZoneResult result = hyperFactions.getZoneManager().renameZone(zone.id(), newName);
        switch (result) {
            case SUCCESS -> ctx.sendMessage(prefix().insert(msg("Renamed zone '" + currentName + "' to '" + newName + "'", COLOR_GREEN)));
            case NAME_TAKEN -> ctx.sendMessage(prefix().insert(msg("A zone with the name '" + newName + "' already exists.", COLOR_RED)));
            case INVALID_NAME -> ctx.sendMessage(prefix().insert(msg("Invalid zone name. Must be 1-32 characters.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed to rename zone: " + result, COLOR_RED)));
        }
    }

    private void handleZoneInfo(CommandContext ctx, String worldName, int chunkX, int chunkZ, String[] args) {
        Zone zone;
        if (args.length > 0) {
            zone = hyperFactions.getZoneManager().getZoneByName(args[0]);
            if (zone == null) {
                ctx.sendMessage(prefix().insert(msg("Zone '" + args[0] + "' not found.", COLOR_RED)));
                return;
            }
        } else {
            zone = hyperFactions.getZoneManager().getZone(worldName, chunkX, chunkZ);
            if (zone == null) {
                ctx.sendMessage(prefix().insert(msg("No zone at your location.", COLOR_RED)));
                return;
            }
        }

        String typeColor = zone.isSafeZone() ? "#2dd4bf" : "#c084fc";
        ctx.sendMessage(msg("=== Zone: " + zone.name() + " ===", typeColor).bold(true));
        ctx.sendMessage(msg("Type: ", COLOR_GRAY).insert(msg(zone.type().getDisplayName(), typeColor)));
        ctx.sendMessage(msg("World: ", COLOR_GRAY).insert(msg(zone.world(), COLOR_WHITE)));
        ctx.sendMessage(msg("Chunks: ", COLOR_GRAY).insert(msg(String.valueOf(zone.getChunkCount()), COLOR_WHITE)));

        if (!zone.getFlags().isEmpty()) {
            ctx.sendMessage(msg("Custom Flags:", COLOR_GRAY));
            for (var entry : zone.getFlags().entrySet()) {
                ctx.sendMessage(msg("  " + entry.getKey() + ": " + entry.getValue(), COLOR_YELLOW));
            }
        }
    }

    private void handleZoneClaim(CommandContext ctx, String worldName, int chunkX, int chunkZ, String[] args) {
        if (args.length < 1) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin zone claim <name>", COLOR_RED)));
            return;
        }

        String name = args[0];
        Zone zone = hyperFactions.getZoneManager().getZoneByName(name);
        if (zone == null) {
            ctx.sendMessage(prefix().insert(msg("Zone '" + name + "' not found.", COLOR_RED)));
            return;
        }

        ZoneManager.ZoneResult result = hyperFactions.getZoneManager().claimChunk(zone.id(), worldName, chunkX, chunkZ);
        switch (result) {
            case SUCCESS -> ctx.sendMessage(prefix().insert(msg("Claimed chunk (" + chunkX + ", " + chunkZ + ") for zone '" + name + "'", COLOR_GREEN)));
            case CHUNK_HAS_ZONE -> ctx.sendMessage(prefix().insert(msg("This chunk already belongs to another zone.", COLOR_RED)));
            case CHUNK_HAS_FACTION -> ctx.sendMessage(prefix().insert(msg("This chunk is claimed by a faction.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed: " + result, COLOR_RED)));
        }
    }

    private void handleZoneUnclaim(CommandContext ctx, String worldName, int chunkX, int chunkZ) {
        ZoneManager.ZoneResult result = hyperFactions.getZoneManager().unclaimChunkAt(worldName, chunkX, chunkZ);
        if (result == ZoneManager.ZoneResult.SUCCESS) {
            ctx.sendMessage(prefix().insert(msg("Unclaimed chunk (" + chunkX + ", " + chunkZ + ") from zone.", COLOR_GREEN)));
        } else {
            ctx.sendMessage(prefix().insert(msg("No zone chunk found at this location.", COLOR_RED)));
        }
    }

    private void handleZoneRadius(CommandContext ctx, String worldName, int chunkX, int chunkZ, String[] args) {
        if (args.length < 2) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin zone radius <name> <radius> [circle|square]", COLOR_RED)));
            ctx.sendMessage(msg("  radius: 1-20 chunks", COLOR_GRAY));
            ctx.sendMessage(msg("  shape: circle (default) or square", COLOR_GRAY));
            return;
        }

        String name = args[0];
        Zone zone = hyperFactions.getZoneManager().getZoneByName(name);
        if (zone == null) {
            ctx.sendMessage(prefix().insert(msg("Zone '" + name + "' not found.", COLOR_RED)));
            return;
        }

        int radius;
        try {
            radius = Integer.parseInt(args[1]);
            if (radius < 1 || radius > 20) {
                ctx.sendMessage(prefix().insert(msg("Radius must be between 1 and 20.", COLOR_RED)));
                return;
            }
        } catch (NumberFormatException e) {
            ctx.sendMessage(prefix().insert(msg("Invalid radius number.", COLOR_RED)));
            return;
        }

        boolean circle = true;
        if (args.length > 2) {
            String shape = args[2].toLowerCase();
            if (shape.equals("square")) {
                circle = false;
            } else if (!shape.equals("circle")) {
                ctx.sendMessage(prefix().insert(msg("Invalid shape. Use 'circle' or 'square'.", COLOR_RED)));
                return;
            }
        }

        int claimed = hyperFactions.getZoneManager().claimRadius(zone.id(), worldName, chunkX, chunkZ, radius, circle);
        if (claimed > 0) {
            ctx.sendMessage(prefix().insert(msg("Claimed " + claimed + " chunks for zone '" + name + "'", COLOR_GREEN)));
        } else {
            ctx.sendMessage(prefix().insert(msg("No chunks could be claimed (all occupied or already in zone).", COLOR_YELLOW)));
        }
    }

    // === Zone Flag Management ===
    private void handleZoneFlag(CommandContext ctx, String worldName, int chunkX, int chunkZ, String[] args) {
        Zone zone = hyperFactions.getZoneManager().getZone(worldName, chunkX, chunkZ);
        if (zone == null) {
            ctx.sendMessage(prefix().insert(msg("No zone at your location. Stand in a zone to manage flags.", COLOR_RED)));
            return;
        }

        if (args.length == 0) {
            ctx.sendMessage(msg("=== Zone Flags: " + zone.name() + " ===", COLOR_CYAN).bold(true));
            ctx.sendMessage(msg("Zone Type: " + zone.type().getDisplayName(), COLOR_GRAY));
            ctx.sendMessage(msg("", COLOR_GRAY));

            for (String flag : ZoneFlags.ALL_FLAGS) {
                boolean effectiveValue = zone.getEffectiveFlag(flag);
                boolean isCustom = zone.hasFlagSet(flag);
                String valueStr = effectiveValue ? "true" : "false";
                String customStr = isCustom ? " (custom)" : " (default)";
                String color = effectiveValue ? COLOR_GREEN : COLOR_RED;
                ctx.sendMessage(msg("  " + flag + ": ", COLOR_GRAY).insert(msg(valueStr, color)).insert(msg(customStr, COLOR_GRAY)));
            }
            ctx.sendMessage(msg("", COLOR_GRAY));
            ctx.sendMessage(msg("Usage: /f admin zoneflag <flag> <true|false|clear>", COLOR_YELLOW));
            ctx.sendMessage(msg("Usage: /f admin zoneflag clearall (reset all to defaults)", COLOR_YELLOW));
            return;
        }

        if (args[0].equalsIgnoreCase("clearall") || args[0].equalsIgnoreCase("resetall")) {
            ZoneManager.ZoneResult result = hyperFactions.getZoneManager().clearAllZoneFlags(zone.id());
            if (result == ZoneManager.ZoneResult.SUCCESS) {
                ctx.sendMessage(prefix().insert(msg("Cleared all custom flags for '" + zone.name() + "' - now using zone type defaults.", COLOR_GREEN)));
            } else {
                ctx.sendMessage(prefix().insert(msg("Failed to clear flags.", COLOR_RED)));
            }
            return;
        }

        String flagName = args[0].toLowerCase();
        if (!ZoneFlags.isValidFlag(flagName)) {
            ctx.sendMessage(prefix().insert(msg("Invalid flag: " + flagName, COLOR_RED)));
            ctx.sendMessage(msg("Valid flags: " + String.join(", ", ZoneFlags.ALL_FLAGS), COLOR_GRAY));
            return;
        }

        if (args.length == 1) {
            boolean effectiveValue = zone.getEffectiveFlag(flagName);
            boolean isCustom = zone.hasFlagSet(flagName);
            ctx.sendMessage(prefix().insert(msg("Flag '" + flagName + "' = " + effectiveValue, effectiveValue ? COLOR_GREEN : COLOR_RED))
                .insert(msg(isCustom ? " (custom)" : " (default)", COLOR_GRAY)));
            return;
        }

        String action = args[1].toLowerCase();
        ZoneManager.ZoneResult result;

        if (action.equals("clear") || action.equals("default") || action.equals("reset")) {
            result = hyperFactions.getZoneManager().clearZoneFlag(zone.id(), flagName);
            if (result == ZoneManager.ZoneResult.SUCCESS) {
                boolean defaultValue = zone.isSafeZone() ? ZoneFlags.getSafeZoneDefault(flagName) : ZoneFlags.getWarZoneDefault(flagName);
                ctx.sendMessage(prefix().insert(msg("Cleared flag '" + flagName + "' (now using default: " + defaultValue + ")", COLOR_GREEN)));
            } else {
                ctx.sendMessage(prefix().insert(msg("Failed to clear flag.", COLOR_RED)));
            }
        } else if (action.equals("true") || action.equals("false")) {
            boolean value = action.equals("true");
            result = hyperFactions.getZoneManager().setZoneFlag(zone.id(), flagName, value);
            if (result == ZoneManager.ZoneResult.SUCCESS) {
                ctx.sendMessage(prefix().insert(msg("Set flag '" + flagName + "' to " + value, COLOR_GREEN)));
            } else {
                ctx.sendMessage(prefix().insert(msg("Failed to set flag.", COLOR_RED)));
            }
        } else {
            ctx.sendMessage(prefix().insert(msg("Invalid value. Use: true, false, or clear", COLOR_RED)));
        }
    }

    // === Admin Update ===
    private void handleAdminUpdate(CommandContext ctx, UUID senderUuid) {
        var updateChecker = hyperFactions.getUpdateChecker();
        if (updateChecker == null) {
            ctx.sendMessage(prefix().insert(msg("Update checker is not available.", COLOR_RED)));
            return;
        }

        if (!updateChecker.hasUpdateAvailable()) {
            ctx.sendMessage(prefix().insert(msg("Checking for updates...", COLOR_YELLOW)));
            updateChecker.checkForUpdates(true).thenAccept(info -> {
                if (info == null) {
                    ctx.sendMessage(prefix().insert(msg("Plugin is already up-to-date (v" + updateChecker.getCurrentVersion() + ")", COLOR_GREEN)));
                } else {
                    ctx.sendMessage(prefix().insert(msg("Update available: v" + info.version(), COLOR_GREEN)));
                    startDownload(ctx, senderUuid, updateChecker, info);
                }
            });
            return;
        }

        var info = updateChecker.getCachedUpdate();
        if (info == null) {
            ctx.sendMessage(prefix().insert(msg("No update information available.", COLOR_RED)));
            return;
        }

        startDownload(ctx, senderUuid, updateChecker, info);
    }

    private void startDownload(CommandContext ctx, UUID senderUuid,
                               com.hyperfactions.update.UpdateChecker updateChecker,
                               com.hyperfactions.update.UpdateChecker.UpdateInfo info) {
        String currentVersion = updateChecker.getCurrentVersion();

        // Step 1: Create a data backup before downloading the update
        ctx.sendMessage(prefix().insert(msg("Creating pre-update backup...", COLOR_YELLOW)));

        hyperFactions.getBackupManager().createBackup(BackupType.MANUAL, "pre-update-" + currentVersion, senderUuid)
            .thenCompose(backupResult -> {
                if (backupResult instanceof BackupManager.BackupResult.Success success) {
                    ctx.sendMessage(prefix().insert(msg("Backup created: " + success.metadata().name(), COLOR_GREEN)));
                } else if (backupResult instanceof BackupManager.BackupResult.Failure failure) {
                    ctx.sendMessage(prefix().insert(msg("Warning: Backup failed - " + failure.error(), COLOR_YELLOW)));
                    ctx.sendMessage(msg("  Continuing with update anyway...", COLOR_GRAY));
                }

                // Step 2: Download the update
                ctx.sendMessage(prefix().insert(msg("Downloading HyperFactions v" + info.version() + "...", COLOR_YELLOW)));
                return updateChecker.downloadUpdate(info);
            })
            .thenAccept(path -> {
                if (path == null) {
                    ctx.sendMessage(prefix().insert(msg("Failed to download update. Check server logs.", COLOR_RED)));
                } else {
                    ctx.sendMessage(prefix().insert(msg("Update downloaded successfully!", COLOR_GREEN)));
                    ctx.sendMessage(msg("  File: " + path.getFileName(), COLOR_GRAY));

                    // Step 3: Clean up old JAR backups (keep only the version we just upgraded from)
                    int cleaned = updateChecker.cleanupOldBackups(currentVersion);
                    if (cleaned > 0) {
                        ctx.sendMessage(msg("  Cleanup: Removed " + cleaned + " old backup(s)", COLOR_GRAY));
                    }
                    ctx.sendMessage(msg("  Kept: HyperFactions-" + currentVersion + ".jar.backup (for rollback)", COLOR_GRAY));

                    // Step 4: Create rollback marker (safe to rollback until server restarts)
                    updateChecker.createRollbackMarker(currentVersion, info.version());

                    ctx.sendMessage(msg("  Restart the server to apply the update.", COLOR_YELLOW));
                    ctx.sendMessage(msg("  Use /f admin rollback to revert before restarting.", COLOR_GRAY));

                    // Run manual backup rotation to respect retention limits
                    hyperFactions.getBackupManager().performRotation();
                }
            });
    }

    // === Admin Rollback ===
    private void handleAdminRollback(CommandContext ctx) {
        var updateChecker = hyperFactions.getUpdateChecker();
        if (updateChecker == null) {
            ctx.sendMessage(prefix().insert(msg("Update checker is not available.", COLOR_RED)));
            return;
        }

        // Check if there's a backup to rollback to
        Path latestBackup = updateChecker.findLatestBackup();
        if (latestBackup == null) {
            ctx.sendMessage(prefix().insert(msg("No backup JAR found to rollback to.", COLOR_RED)));
            return;
        }

        String backupVersion = latestBackup.getFileName().toString()
                .replace("HyperFactions-", "")
                .replace(".jar.backup", "");

        // Check if rollback is safe (server hasn't restarted since update)
        if (!updateChecker.isRollbackSafe()) {
            // Server has restarted - migrations may have run
            ctx.sendMessage(prefix().insert(msg("Cannot automatically rollback!", COLOR_RED)));
            ctx.sendMessage(msg("", COLOR_GRAY));
            ctx.sendMessage(msg("The server has been restarted since the last update.", COLOR_YELLOW));
            ctx.sendMessage(msg("Config/data migrations may have been applied.", COLOR_YELLOW));
            ctx.sendMessage(msg("", COLOR_GRAY));
            ctx.sendMessage(msg("To rollback safely, you must:", COLOR_WHITE));
            ctx.sendMessage(msg("  1. Stop the server", COLOR_GRAY));
            ctx.sendMessage(msg("  2. Restore from the pre-update backup:", COLOR_GRAY));
            ctx.sendMessage(msg("     /f admin backup restore <backup-name>", COLOR_CYAN));
            ctx.sendMessage(msg("  3. Manually replace the JAR file:", COLOR_GRAY));
            ctx.sendMessage(msg("     " + latestBackup.getFileName() + " -> HyperFactions-" + backupVersion + ".jar", COLOR_CYAN));
            ctx.sendMessage(msg("  4. Restart the server", COLOR_GRAY));
            ctx.sendMessage(msg("", COLOR_GRAY));
            ctx.sendMessage(msg("Use /f admin backup list to find the pre-update backup.", COLOR_YELLOW));
            return;
        }

        // Get rollback info
        var rollbackInfo = updateChecker.getRollbackInfo();
        if (rollbackInfo != null) {
            ctx.sendMessage(prefix().insert(msg("Rolling back update...", COLOR_YELLOW)));
            ctx.sendMessage(msg("  From: v" + rollbackInfo.toVersion() + " (new)", COLOR_GRAY));
            ctx.sendMessage(msg("  To: v" + rollbackInfo.fromVersion() + " (previous)", COLOR_GRAY));
        } else {
            ctx.sendMessage(prefix().insert(msg("Rolling back to v" + backupVersion + "...", COLOR_YELLOW)));
        }

        // Perform the rollback
        var result = updateChecker.performRollback();

        if (result.success()) {
            ctx.sendMessage(prefix().insert(msg("Rollback successful!", COLOR_GREEN)));
            ctx.sendMessage(msg("  Restored: HyperFactions-" + result.restoredVersion() + ".jar", COLOR_GRAY));
            if (result.removedVersion() != null) {
                ctx.sendMessage(msg("  Removed: HyperFactions-" + result.removedVersion() + ".jar", COLOR_GRAY));
            }
            ctx.sendMessage(msg("  Restart the server to apply the rollback.", COLOR_YELLOW));
        } else {
            ctx.sendMessage(prefix().insert(msg("Rollback failed: " + result.errorMessage(), COLOR_RED)));
        }
    }

    // === Admin Backup Commands ===
    private void handleAdminBackup(CommandContext ctx, @Nullable PlayerRef player, UUID senderUuid, String[] args) {
        if (!hasPermission(player, Permissions.ADMIN_BACKUP)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to manage backups.", COLOR_RED)));
            return;
        }

        if (args.length == 0) {
            showBackupHelp(ctx);
            return;
        }

        String subCmd = args[0].toLowerCase();
        String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

        switch (subCmd) {
            case "create" -> handleBackupCreate(ctx, senderUuid, subArgs);
            case "list" -> handleBackupList(ctx);
            case "restore" -> handleBackupRestore(ctx, senderUuid, subArgs);
            case "delete" -> handleBackupDelete(ctx, subArgs);
            case "help", "?" -> showBackupHelp(ctx);
            default -> {
                ctx.sendMessage(prefix().insert(msg("Unknown backup command: " + subCmd, COLOR_RED)));
                showBackupHelp(ctx);
            }
        }
    }

    private void showBackupHelp(CommandContext ctx) {
        List<CommandHelp> commands = new ArrayList<>();
        commands.add(new CommandHelp("/f admin backup create [name]", "Create manual backup"));
        commands.add(new CommandHelp("/f admin backup list", "List all backups grouped by type"));
        commands.add(new CommandHelp("/f admin backup restore <name>", "Restore from backup (requires confirmation)"));
        commands.add(new CommandHelp("/f admin backup delete <name>", "Delete a backup"));
        ctx.sendMessage(HelpFormatter.buildHelp("Backup Management", "GFS rotation scheme", commands, null));
    }

    private void handleBackupCreate(CommandContext ctx, UUID senderUuid, String[] args) {
        String customName = args.length > 0 ? String.join("_", args) : null;

        ctx.sendMessage(prefix().insert(msg("Creating backup...", COLOR_YELLOW)));

        hyperFactions.getBackupManager().createBackup(BackupType.MANUAL, customName, senderUuid)
            .thenAccept(result -> {
                if (result instanceof BackupManager.BackupResult.Success success) {
                    ctx.sendMessage(prefix().insert(msg("Backup created successfully!", COLOR_GREEN)));
                    ctx.sendMessage(msg("  Name: " + success.metadata().name(), COLOR_GRAY));
                    ctx.sendMessage(msg("  Size: " + success.metadata().getFormattedSize(), COLOR_GRAY));
                } else if (result instanceof BackupManager.BackupResult.Failure failure) {
                    ctx.sendMessage(prefix().insert(msg("Backup failed: " + failure.error(), COLOR_RED)));
                }
            });
    }

    private void handleBackupList(CommandContext ctx) {
        Map<BackupType, List<BackupMetadata>> grouped = hyperFactions.getBackupManager().getBackupsGroupedByType();

        if (grouped.isEmpty()) {
            ctx.sendMessage(prefix().insert(msg("No backups found.", COLOR_GRAY)));
            return;
        }

        ctx.sendMessage(msg("=== Backups ===", COLOR_CYAN).bold(true));

        for (BackupType type : BackupType.values()) {
            List<BackupMetadata> backups = grouped.getOrDefault(type, List.of());
            if (backups.isEmpty()) continue;

            ctx.sendMessage(msg(type.getDisplayName() + " (" + backups.size() + "):", COLOR_YELLOW));
            for (BackupMetadata backup : backups) {
                ctx.sendMessage(msg("  " + backup.name(), COLOR_WHITE)
                    .insert(msg(" - " + backup.getFormattedTimestamp() + " (" + backup.getFormattedSize() + ")", COLOR_GRAY)));
            }
        }
    }

    private void handleBackupRestore(CommandContext ctx, UUID senderUuid, String[] args) {
        if (args.length < 1) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin backup restore <name>", COLOR_RED)));
            return;
        }

        String backupName = args[0];
        // Find backup by name
        BackupMetadata backup = hyperFactions.getBackupManager().listBackups().stream()
            .filter(b -> b.name().equalsIgnoreCase(backupName))
            .findFirst()
            .orElse(null);
        if (backup == null) {
            ctx.sendMessage(prefix().insert(msg("Backup '" + backupName + "' not found.", COLOR_RED)));
            return;
        }

        ConfirmationManager confirmManager = hyperFactions.getConfirmationManager();
        ConfirmationManager.ConfirmationResult confirmResult = confirmManager.checkOrCreate(
            senderUuid, ConfirmationManager.ConfirmationType.RESTORE_BACKUP, null
        );

        switch (confirmResult) {
            case NEEDS_CONFIRMATION, EXPIRED_RECREATED -> {
                ctx.sendMessage(prefix().insert(msg("WARNING: Restoring backup will overwrite current data!", COLOR_RED)));
                ctx.sendMessage(prefix().insert(msg("Type ", COLOR_YELLOW))
                    .insert(msg("/f admin backup restore " + backupName, COLOR_WHITE))
                    .insert(msg(" again within " + confirmManager.getTimeoutSeconds() + " seconds to confirm.", COLOR_YELLOW)));
            }
            case CONFIRMED -> {
                ctx.sendMessage(prefix().insert(msg("Restoring backup...", COLOR_YELLOW)));
                hyperFactions.getBackupManager().restoreBackup(backup.name())
                    .thenAccept(result -> {
                        if (result instanceof BackupManager.RestoreResult.Success) {
                            ctx.sendMessage(prefix().insert(msg("Backup restored successfully! Data reloaded.", COLOR_GREEN)));
                        } else if (result instanceof BackupManager.RestoreResult.Failure failure) {
                            ctx.sendMessage(prefix().insert(msg("Restore failed: " + failure.error(), COLOR_RED)));
                        }
                    });
            }
            case DIFFERENT_ACTION -> {
                ctx.sendMessage(prefix().insert(msg("Previous confirmation cancelled. Type again to confirm restore.", COLOR_YELLOW)));
            }
        }
    }

    private void handleBackupDelete(CommandContext ctx, String[] args) {
        if (args.length < 1) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin backup delete <name>", COLOR_RED)));
            return;
        }

        String backupName = args[0];
        // Find backup by name
        BackupMetadata backup = hyperFactions.getBackupManager().listBackups().stream()
            .filter(b -> b.name().equalsIgnoreCase(backupName))
            .findFirst()
            .orElse(null);
        if (backup == null) {
            ctx.sendMessage(prefix().insert(msg("Backup '" + backupName + "' not found.", COLOR_RED)));
            return;
        }

        hyperFactions.getBackupManager().deleteBackup(backup.name())
            .thenAccept(deleted -> {
                if (deleted) {
                    ctx.sendMessage(prefix().insert(msg("Deleted backup '" + backupName + "'", COLOR_GREEN)));
                } else {
                    ctx.sendMessage(prefix().insert(msg("Failed to delete backup.", COLOR_RED)));
                }
            });
    }

    // === Admin Import Commands ===
    private void handleAdminImport(CommandContext ctx, String[] args) {
        if (args.length == 0) {
            showImportHelp(ctx);
            return;
        }

        String subCmd = args[0].toLowerCase();
        String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

        switch (subCmd) {
            case "hyfactions" -> handleImportHyFactions(ctx, subArgs);
            case "elbaphfactions" -> handleImportElbaphFactions(ctx, subArgs);
            case "help", "?" -> showImportHelp(ctx);
            default -> {
                ctx.sendMessage(prefix().insert(msg("Unknown import source: " + subCmd, COLOR_RED)));
                showImportHelp(ctx);
            }
        }
    }

    private void showImportHelp(CommandContext ctx) {
        List<CommandHelp> commands = new ArrayList<>();
        commands.add(new CommandHelp("/f admin import hyfactions [path] [flags]", "Import from HyFactions mod"));
        commands.add(new CommandHelp("  Default path: mods/Kaws_Hyfaction", ""));
        commands.add(new CommandHelp("/f admin import elbaphfactions [path] [flags]", "Import from ElbaphFactions mod"));
        commands.add(new CommandHelp("  Default path: mods/ElbaphFactions", ""));
        commands.add(new CommandHelp("  Flags:", ""));
        commands.add(new CommandHelp("    --dry-run / -n", "Simulate without changes"));
        commands.add(new CommandHelp("    --overwrite", "Replace existing factions"));
        commands.add(new CommandHelp("    --no-zones", "Skip zone import"));
        commands.add(new CommandHelp("    --no-power", "Skip power distribution"));
        ctx.sendMessage(HelpFormatter.buildHelp("Import Commands", "Migrate from other faction plugins", commands, null));
    }

    private void handleImportHyFactions(CommandContext ctx, String[] args) {
        // Parse path (optional - default to mods/Kaws_Hyfaction)
        String pathStr = "mods/Kaws_Hyfaction";
        int flagStartIndex = 0;

        if (args.length > 0 && !args[0].startsWith("-")) {
            pathStr = args[0];
            flagStartIndex = 1;
        }

        Path dataPath = Paths.get(pathStr);

        boolean dryRun = false;
        boolean overwrite = false;
        boolean skipZones = false;
        boolean skipPower = false;

        for (int i = flagStartIndex; i < args.length; i++) {
            String flag = args[i].toLowerCase();
            switch (flag) {
                case "--dry-run", "-n" -> dryRun = true;
                case "--overwrite" -> overwrite = true;
                case "--no-zones" -> skipZones = true;
                case "--no-power" -> skipPower = true;
            }
        }

        ctx.sendMessage(prefix().insert(msg("Importing from HyFactions...", COLOR_YELLOW)));
        ctx.sendMessage(msg("  Path: " + dataPath, COLOR_GRAY));
        if (dryRun) {
            ctx.sendMessage(msg("  (Dry run - no changes will be made)", COLOR_GRAY));
        }

        HyFactionsImporter importer = new HyFactionsImporter(
            hyperFactions.getFactionManager(),
            hyperFactions.getClaimManager(),
            hyperFactions.getZoneManager(),
            hyperFactions.getPowerManager(),
            hyperFactions.getBackupManager()
        );

        importer.setDryRun(dryRun);
        importer.setOverwrite(overwrite);
        importer.setSkipZones(skipZones);
        importer.setSkipPower(skipPower);

        final boolean finalDryRun = dryRun;
        CompletableFuture.supplyAsync(() -> importer.importFrom(dataPath))
            .thenAccept(result -> reportImportResult(ctx, result, finalDryRun, "HyFactions"));
    }

    private void handleImportElbaphFactions(CommandContext ctx, String[] args) {
        // Parse path (optional - default to mods/ElbaphFactions)
        String pathStr = "mods/ElbaphFactions";
        int flagStartIndex = 0;

        if (args.length > 0 && !args[0].startsWith("-")) {
            pathStr = args[0];
            flagStartIndex = 1;
        }

        Path dataPath = Paths.get(pathStr);

        boolean dryRun = false;
        boolean overwrite = false;
        boolean skipZones = false;
        boolean skipPower = false;

        for (int i = flagStartIndex; i < args.length; i++) {
            String flag = args[i].toLowerCase();
            switch (flag) {
                case "--dry-run", "-n" -> dryRun = true;
                case "--overwrite" -> overwrite = true;
                case "--no-zones" -> skipZones = true;
                case "--no-power" -> skipPower = true;
            }
        }

        ctx.sendMessage(prefix().insert(msg("Importing from ElbaphFactions...", COLOR_YELLOW)));
        ctx.sendMessage(msg("  Path: " + dataPath, COLOR_GRAY));
        if (dryRun) {
            ctx.sendMessage(msg("  (Dry run - no changes will be made)", COLOR_GRAY));
        }

        ElbaphFactionsImporter importer = new ElbaphFactionsImporter(
            hyperFactions.getFactionManager(),
            hyperFactions.getClaimManager(),
            hyperFactions.getZoneManager(),
            hyperFactions.getPowerManager(),
            hyperFactions.getBackupManager()
        );

        importer.setDryRun(dryRun);
        importer.setOverwrite(overwrite);
        importer.setSkipZones(skipZones);
        importer.setSkipPower(skipPower);

        final boolean finalDryRun = dryRun;
        CompletableFuture.supplyAsync(() -> importer.importFrom(dataPath))
            .thenAccept(result -> reportImportResult(ctx, result, finalDryRun, "ElbaphFactions"));
    }

    private void reportImportResult(CommandContext ctx, ImportResult result, boolean dryRun, String sourceName) {
        if (!result.hasErrors()) {
            ctx.sendMessage(prefix().insert(msg(sourceName + " import " + (dryRun ? "simulation " : "") + "complete!", COLOR_GREEN)));
            ctx.sendMessage(msg("  Factions: " + result.factionsImported(), COLOR_GRAY));
            ctx.sendMessage(msg("  Claims: " + result.claimsImported(), COLOR_GRAY));
            ctx.sendMessage(msg("  Zones: " + result.zonesCreated(), COLOR_GRAY));
            ctx.sendMessage(msg("  Players with power: " + result.playersWithPower(), COLOR_GRAY));
            if (result.factionsSkipped() > 0) {
                ctx.sendMessage(msg("  Skipped: " + result.factionsSkipped(), COLOR_YELLOW));
            }
            if (result.hasWarnings()) {
                ctx.sendMessage(msg("  Warnings: " + result.warnings().size() + " (check logs)", COLOR_YELLOW));
            }
        } else {
            ctx.sendMessage(prefix().insert(msg(sourceName + " import failed with errors:", COLOR_RED)));
            for (String error : result.errors()) {
                ctx.sendMessage(msg("  - " + error, COLOR_RED));
            }
        }
    }

    // === Debug Commands ===
    private void handleDebug(CommandContext ctx, @Nullable Store<EntityStore> store, @Nullable Ref<EntityStore> ref,
                            @Nullable PlayerRef player, @Nullable World world, String[] args) {
        if (!hasPermission(player, Permissions.ADMIN_DEBUG)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to use debug commands.", COLOR_RED)));
            return;
        }

        if (args.length == 0) {
            showDebugHelp(ctx);
            return;
        }

        String subCmd = args[0].toLowerCase();
        String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

        switch (subCmd) {
            case "toggle" -> handleDebugToggle(ctx, subArgs);
            case "status" -> handleDebugStatus(ctx);
            case "power" -> handleDebugPower(ctx, subArgs);
            case "claim" -> {
                if (store == null) {
                    ctx.sendMessage(prefix().insert(msg("This debug command can only be used by a player.", COLOR_RED)));
                } else {
                    handleDebugClaim(ctx, store, ref, world, subArgs);
                }
            }
            case "protection" -> {
                if (store == null) {
                    ctx.sendMessage(prefix().insert(msg("This debug command can only be used by a player.", COLOR_RED)));
                } else {
                    handleDebugProtection(ctx, store, ref, world, subArgs);
                }
            }
            case "combat" -> handleDebugCombat(ctx, subArgs);
            case "relation" -> handleDebugRelation(ctx, subArgs);
            case "help", "?" -> showDebugHelp(ctx);
            default -> {
                ctx.sendMessage(prefix().insert(msg("Unknown debug command: " + subCmd, COLOR_RED)));
                showDebugHelp(ctx);
            }
        }
    }

    private void showDebugHelp(CommandContext ctx) {
        List<CommandHelp> commands = new ArrayList<>();
        commands.add(new CommandHelp("/f admin debug toggle <category> [on|off]", "Toggle debug logging"));
        commands.add(new CommandHelp("/f admin debug status", "Show debug status"));
        commands.add(new CommandHelp("/f admin debug power <player>", "Show power details"));
        commands.add(new CommandHelp("/f admin debug claim [x z]", "Show claim info"));
        commands.add(new CommandHelp("/f admin debug protection <player>", "Show protection info"));
        commands.add(new CommandHelp("/f admin debug combat <player>", "Show combat tag status"));
        commands.add(new CommandHelp("/f admin debug relation <faction1> <faction2>", "Show relation info"));
        ctx.sendMessage(HelpFormatter.buildHelp("Debug Commands", "Diagnostics and troubleshooting", commands, null));
    }

    private void handleDebugToggle(CommandContext ctx, String[] args) {
        var debugConfig = ConfigManager.get().debug();

        if (args.length == 0) {
            // Show current status
            ctx.sendMessage(msg("=== Debug Logging Status ===", COLOR_CYAN).bold(true));
            ctx.sendMessage(msg("Categories:", COLOR_GRAY));
            ctx.sendMessage(msg("  power: ", COLOR_WHITE).insert(msg(debugConfig.isPower() ? "ON" : "OFF", debugConfig.isPower() ? COLOR_GREEN : COLOR_RED)));
            ctx.sendMessage(msg("  claim: ", COLOR_WHITE).insert(msg(debugConfig.isClaim() ? "ON" : "OFF", debugConfig.isClaim() ? COLOR_GREEN : COLOR_RED)));
            ctx.sendMessage(msg("  combat: ", COLOR_WHITE).insert(msg(debugConfig.isCombat() ? "ON" : "OFF", debugConfig.isCombat() ? COLOR_GREEN : COLOR_RED)));
            ctx.sendMessage(msg("  protection: ", COLOR_WHITE).insert(msg(debugConfig.isProtection() ? "ON" : "OFF", debugConfig.isProtection() ? COLOR_GREEN : COLOR_RED)));
            ctx.sendMessage(msg("  relation: ", COLOR_WHITE).insert(msg(debugConfig.isRelation() ? "ON" : "OFF", debugConfig.isRelation() ? COLOR_GREEN : COLOR_RED)));
            ctx.sendMessage(msg("  territory: ", COLOR_WHITE).insert(msg(debugConfig.isTerritory() ? "ON" : "OFF", debugConfig.isTerritory() ? COLOR_GREEN : COLOR_RED)));
            ctx.sendMessage(msg("  worldmap: ", COLOR_WHITE).insert(msg(debugConfig.isWorldmap() ? "ON" : "OFF", debugConfig.isWorldmap() ? COLOR_GREEN : COLOR_RED)));
            ctx.sendMessage(msg("  interaction: ", COLOR_WHITE).insert(msg(debugConfig.isInteraction() ? "ON" : "OFF", debugConfig.isInteraction() ? COLOR_GREEN : COLOR_RED)));
            ctx.sendMessage(msg("  mixin: ", COLOR_WHITE).insert(msg(debugConfig.isMixin() ? "ON" : "OFF", debugConfig.isMixin() ? COLOR_GREEN : COLOR_RED)));
            ctx.sendMessage(msg("  spawning: ", COLOR_WHITE).insert(msg(debugConfig.isSpawning() ? "ON" : "OFF", debugConfig.isSpawning() ? COLOR_GREEN : COLOR_RED)));
            ctx.sendMessage(msg("  integration: ", COLOR_WHITE).insert(msg(debugConfig.isIntegration() ? "ON" : "OFF", debugConfig.isIntegration() ? COLOR_GREEN : COLOR_RED)));
            ctx.sendMessage(msg("Usage: /f admin debug toggle <category|all> [on|off]", COLOR_GRAY));
            return;
        }

        String category = args[0].toLowerCase();

        // Handle "all" category
        if (category.equals("all")) {
            boolean enable = args.length > 1 ? args[1].equalsIgnoreCase("on") : !debugConfig.isEnabledByDefault();
            if (enable) {
                debugConfig.enableAll();
                ctx.sendMessage(prefix().insert(msg("All debug categories enabled.", COLOR_GREEN)));
            } else {
                debugConfig.disableAll();
                ctx.sendMessage(prefix().insert(msg("All debug categories disabled.", COLOR_GREEN)));
            }
            debugConfig.save();
            return;
        }

        // Get current value and determine new value
        boolean currentValue;
        switch (category) {
            case "power" -> currentValue = debugConfig.isPower();
            case "claim" -> currentValue = debugConfig.isClaim();
            case "combat" -> currentValue = debugConfig.isCombat();
            case "protection" -> currentValue = debugConfig.isProtection();
            case "relation" -> currentValue = debugConfig.isRelation();
            case "territory" -> currentValue = debugConfig.isTerritory();
            case "worldmap", "map" -> currentValue = debugConfig.isWorldmap();
            case "interaction" -> currentValue = debugConfig.isInteraction();
            case "mixin" -> currentValue = debugConfig.isMixin();
            case "spawning" -> currentValue = debugConfig.isSpawning();
            case "integration" -> currentValue = debugConfig.isIntegration();
            default -> {
                ctx.sendMessage(prefix().insert(msg("Unknown category: " + category, COLOR_RED)));
                ctx.sendMessage(msg("Valid categories: power, claim, combat, protection, relation, territory, worldmap, interaction, mixin, spawning, integration, all", COLOR_GRAY));
                return;
            }
        }

        // Determine new value: if explicit on/off provided use that, otherwise toggle
        boolean newValue = args.length > 1
            ? args[1].equalsIgnoreCase("on")
            : !currentValue;

        // Apply the change
        switch (category) {
            case "power" -> debugConfig.setPower(newValue);
            case "claim" -> debugConfig.setClaim(newValue);
            case "combat" -> debugConfig.setCombat(newValue);
            case "protection" -> debugConfig.setProtection(newValue);
            case "relation" -> debugConfig.setRelation(newValue);
            case "territory" -> debugConfig.setTerritory(newValue);
            case "worldmap", "map" -> debugConfig.setWorldmap(newValue);
            case "interaction" -> debugConfig.setInteraction(newValue);
            case "mixin" -> debugConfig.setMixin(newValue);
            case "spawning" -> debugConfig.setSpawning(newValue);
            case "integration" -> debugConfig.setIntegration(newValue);
        }

        // Save to persist the change
        debugConfig.save();

        ctx.sendMessage(prefix().insert(
            msg("Debug category '", COLOR_GREEN)
                .insert(msg(category, COLOR_CYAN))
                .insert(msg("' set to ", COLOR_GREEN))
                .insert(msg(newValue ? "ON" : "OFF", newValue ? COLOR_GREEN : COLOR_RED))
                .insert(msg(" (saved)", COLOR_GRAY))
        ));
    }

    private void handleDebugStatus(CommandContext ctx) {
        var debugConfig = ConfigManager.get().debug();

        ctx.sendMessage(msg("=== HyperFactions Debug Status ===", COLOR_CYAN).bold(true));

        // Data counts
        ctx.sendMessage(msg("Data:", COLOR_GRAY));
        ctx.sendMessage(msg("  Factions: " + hyperFactions.getFactionManager().getAllFactions().size(), COLOR_WHITE));
        ctx.sendMessage(msg("  Zones: " + hyperFactions.getZoneManager().getAllZones().size(), COLOR_WHITE));
        ctx.sendMessage(msg("  Claims: " + hyperFactions.getClaimManager().getTotalClaimCount(), COLOR_WHITE));

        // Debug logging status
        ctx.sendMessage(msg("Debug Logging:", COLOR_GRAY));
        ctx.sendMessage(msg("  power: ", COLOR_WHITE).insert(msg(debugConfig.isPower() ? "ON" : "OFF", debugConfig.isPower() ? COLOR_GREEN : COLOR_RED)));
        ctx.sendMessage(msg("  claim: ", COLOR_WHITE).insert(msg(debugConfig.isClaim() ? "ON" : "OFF", debugConfig.isClaim() ? COLOR_GREEN : COLOR_RED)));
        ctx.sendMessage(msg("  combat: ", COLOR_WHITE).insert(msg(debugConfig.isCombat() ? "ON" : "OFF", debugConfig.isCombat() ? COLOR_GREEN : COLOR_RED)));
        ctx.sendMessage(msg("  protection: ", COLOR_WHITE).insert(msg(debugConfig.isProtection() ? "ON" : "OFF", debugConfig.isProtection() ? COLOR_GREEN : COLOR_RED)));
        ctx.sendMessage(msg("  relation: ", COLOR_WHITE).insert(msg(debugConfig.isRelation() ? "ON" : "OFF", debugConfig.isRelation() ? COLOR_GREEN : COLOR_RED)));
        ctx.sendMessage(msg("  territory: ", COLOR_WHITE).insert(msg(debugConfig.isTerritory() ? "ON" : "OFF", debugConfig.isTerritory() ? COLOR_GREEN : COLOR_RED)));
        ctx.sendMessage(msg("  worldmap: ", COLOR_WHITE).insert(msg(debugConfig.isWorldmap() ? "ON" : "OFF", debugConfig.isWorldmap() ? COLOR_GREEN : COLOR_RED)));
        ctx.sendMessage(msg("  interaction: ", COLOR_WHITE).insert(msg(debugConfig.isInteraction() ? "ON" : "OFF", debugConfig.isInteraction() ? COLOR_GREEN : COLOR_RED)));
        ctx.sendMessage(msg("  mixin: ", COLOR_WHITE).insert(msg(debugConfig.isMixin() ? "ON" : "OFF", debugConfig.isMixin() ? COLOR_GREEN : COLOR_RED)));
        ctx.sendMessage(msg("  spawning: ", COLOR_WHITE).insert(msg(debugConfig.isSpawning() ? "ON" : "OFF", debugConfig.isSpawning() ? COLOR_GREEN : COLOR_RED)));
        ctx.sendMessage(msg("  integration: ", COLOR_WHITE).insert(msg(debugConfig.isIntegration() ? "ON" : "OFF", debugConfig.isIntegration() ? COLOR_GREEN : COLOR_RED)));
    }

    private void handleDebugPower(CommandContext ctx, String[] args) {
        if (args.length < 1) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin debug power <player>", COLOR_RED)));
            return;
        }
        ctx.sendMessage(prefix().insert(msg("Debug power info not yet implemented.", COLOR_YELLOW)));
    }

    private void handleDebugClaim(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, World world, String[] args) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        int chunkX = ChunkUtil.toChunkCoord(pos.getX());
        int chunkZ = ChunkUtil.toChunkCoord(pos.getZ());

        if (args.length >= 2) {
            try {
                chunkX = Integer.parseInt(args[0]);
                chunkZ = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                ctx.sendMessage(prefix().insert(msg("Invalid chunk coordinates.", COLOR_RED)));
                return;
            }
        }

        ctx.sendMessage(msg("=== Claim Debug (" + chunkX + ", " + chunkZ + ") ===", COLOR_CYAN).bold(true));
        UUID owner = hyperFactions.getClaimManager().getClaimOwner(world.getName(), chunkX, chunkZ);
        if (owner != null) {
            var faction = hyperFactions.getFactionManager().getFaction(owner);
            ctx.sendMessage(msg("Owner: " + (faction != null ? faction.name() : owner.toString()), COLOR_WHITE));
        } else {
            ctx.sendMessage(msg("Owner: None (wilderness)", COLOR_GRAY));
        }

        Zone zone = hyperFactions.getZoneManager().getZone(world.getName(), chunkX, chunkZ);
        if (zone != null) {
            ctx.sendMessage(msg("Zone: " + zone.name() + " (" + zone.type().getDisplayName() + ")", COLOR_WHITE));
        }
    }

    private void handleDebugProtection(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, World world, String[] args) {
        ctx.sendMessage(prefix().insert(msg("Debug protection info not yet implemented.", COLOR_YELLOW)));
    }

    private void handleDebugCombat(CommandContext ctx, String[] args) {
        if (args.length < 1) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin debug combat <player>", COLOR_RED)));
            return;
        }
        ctx.sendMessage(prefix().insert(msg("Debug combat info not yet implemented.", COLOR_YELLOW)));
    }

    private void handleDebugRelation(CommandContext ctx, String[] args) {
        if (args.length < 2) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin debug relation <faction1> <faction2>", COLOR_RED)));
            return;
        }
        ctx.sendMessage(prefix().insert(msg("Debug relation info not yet implemented.", COLOR_YELLOW)));
    }

    // === Admin Map Commands ===
    private void handleAdminMap(CommandContext ctx, PlayerRef player, String[] args) {
        if (!hasPermission(player, Permissions.ADMIN)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        if (args.length == 0) {
            showMapHelp(ctx);
            return;
        }

        String subCmd = args[0].toLowerCase();

        switch (subCmd) {
            case "refresh" -> handleMapRefresh(ctx, player);
            case "status" -> handleMapStatus(ctx);
            case "help", "?" -> showMapHelp(ctx);
            default -> {
                ctx.sendMessage(prefix().insert(msg("Unknown map command: " + subCmd, COLOR_RED)));
                showMapHelp(ctx);
            }
        }
    }

    private void showMapHelp(CommandContext ctx) {
        List<CommandHelp> commands = new ArrayList<>();
        commands.add(new CommandHelp("/f admin map status", "Show world map status and statistics"));
        commands.add(new CommandHelp("/f admin map refresh", "Force immediate map refresh"));
        ctx.sendMessage(HelpFormatter.buildHelp("World Map", "Map overlay management", commands, null));
    }

    private void handleMapRefresh(CommandContext ctx, PlayerRef player) {
        var worldMapService = hyperFactions.getWorldMapService();
        if (worldMapService == null) {
            ctx.sendMessage(prefix().insert(msg("World map service is not available.", COLOR_RED)));
            return;
        }

        ctx.sendMessage(prefix().insert(msg("Forcing full world map refresh...", COLOR_YELLOW)));

        worldMapService.forceFullRefresh();

        ctx.sendMessage(prefix().insert(msg("World map refresh complete.", COLOR_GREEN)));
    }

    private void handleMapStatus(CommandContext ctx) {
        var worldMapConfig = ConfigManager.get().worldMap();
        var worldMapService = hyperFactions.getWorldMapService();

        ctx.sendMessage(msg("=== World Map Status ===", COLOR_CYAN).bold(true));

        // Config status
        ctx.sendMessage(msg("Enabled: ", COLOR_GRAY)
            .insert(msg(worldMapConfig.isEnabled() ? "Yes" : "No", worldMapConfig.isEnabled() ? COLOR_GREEN : COLOR_RED)));
        ctx.sendMessage(msg("Refresh Mode: ", COLOR_GRAY)
            .insert(msg(worldMapConfig.getRefreshMode().getConfigName(), COLOR_WHITE)));

        if (worldMapService == null) {
            ctx.sendMessage(msg("Service: ", COLOR_GRAY).insert(msg("Not initialized", COLOR_RED)));
            return;
        }

        var scheduler = worldMapService.getRefreshScheduler();
        if (scheduler == null) {
            ctx.sendMessage(msg("Scheduler: ", COLOR_GRAY).insert(msg("Not initialized", COLOR_YELLOW)));
            return;
        }

        // Scheduler status
        ctx.sendMessage(msg("Effective Mode: ", COLOR_GRAY)
            .insert(msg(scheduler.getEffectiveMode().getConfigName(), COLOR_WHITE)));

        if (scheduler.isInFallbackMode()) {
            ctx.sendMessage(msg("Fallback: ", COLOR_GRAY)
                .insert(msg("Active (reflection failed)", COLOR_YELLOW)));
        }

        // Mode-specific settings
        var mode = worldMapConfig.getRefreshMode();
        switch (mode) {
            case PROXIMITY -> {
                ctx.sendMessage(msg("Proximity Radius: ", COLOR_GRAY)
                    .insert(msg(worldMapConfig.getProximityChunkRadius() + " chunks", COLOR_WHITE)));
                ctx.sendMessage(msg("Batch Interval: ", COLOR_GRAY)
                    .insert(msg(worldMapConfig.getProximityBatchIntervalTicks() + " ticks", COLOR_WHITE)));
                ctx.sendMessage(msg("Max Chunks/Batch: ", COLOR_GRAY)
                    .insert(msg(String.valueOf(worldMapConfig.getProximityMaxChunksPerBatch()), COLOR_WHITE)));
            }
            case INCREMENTAL -> {
                ctx.sendMessage(msg("Batch Interval: ", COLOR_GRAY)
                    .insert(msg(worldMapConfig.getIncrementalBatchIntervalTicks() + " ticks", COLOR_WHITE)));
                ctx.sendMessage(msg("Max Chunks/Batch: ", COLOR_GRAY)
                    .insert(msg(String.valueOf(worldMapConfig.getIncrementalMaxChunksPerBatch()), COLOR_WHITE)));
            }
            case DEBOUNCED -> {
                ctx.sendMessage(msg("Debounce Delay: ", COLOR_GRAY)
                    .insert(msg(worldMapConfig.getDebouncedDelaySeconds() + " seconds", COLOR_WHITE)));
            }
            case IMMEDIATE, MANUAL -> {
                // No additional settings to show
            }
        }

        // Statistics
        ctx.sendMessage(msg("", COLOR_GRAY));
        ctx.sendMessage(msg("Statistics:", COLOR_YELLOW));
        ctx.sendMessage(msg("  Pending Chunks: ", COLOR_GRAY)
            .insert(msg(String.valueOf(scheduler.getPendingChunkCount()), COLOR_WHITE)));
        ctx.sendMessage(msg("  Total Refreshes: ", COLOR_GRAY)
            .insert(msg(String.valueOf(scheduler.getTotalRefreshes()), COLOR_WHITE)));
        ctx.sendMessage(msg("  Chunks Processed: ", COLOR_GRAY)
            .insert(msg(String.valueOf(scheduler.getChunksProcessed()), COLOR_WHITE)));
        ctx.sendMessage(msg("  Players Notified: ", COLOR_GRAY)
            .insert(msg(String.valueOf(scheduler.getPlayersNotified()), COLOR_WHITE)));

        var lastRefresh = scheduler.getLastRefreshTime();
        if (lastRefresh != null) {
            long secondsAgo = java.time.Duration.between(lastRefresh, java.time.Instant.now()).getSeconds();
            ctx.sendMessage(msg("  Last Refresh: ", COLOR_GRAY)
                .insert(msg(secondsAgo + " seconds ago", COLOR_WHITE)));
        } else {
            ctx.sendMessage(msg("  Last Refresh: ", COLOR_GRAY)
                .insert(msg("Never", COLOR_GRAY)));
        }
    }

    // === Admin Decay Commands ===
    private void handleAdminDecay(CommandContext ctx, PlayerRef player, String[] args) {
        if (!hasPermission(player, Permissions.ADMIN)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        if (args.length == 0) {
            showDecayStatus(ctx);
            return;
        }

        String subCmd = args[0].toLowerCase();

        switch (subCmd) {
            case "run", "trigger" -> handleDecayRun(ctx);
            case "check" -> handleDecayCheck(ctx, Arrays.copyOfRange(args, 1, args.length));
            case "status" -> showDecayStatus(ctx);
            case "help", "?" -> showDecayHelp(ctx);
            default -> {
                ctx.sendMessage(prefix().insert(msg("Unknown decay command: " + subCmd, COLOR_RED)));
                showDecayHelp(ctx);
            }
        }
    }

    private void showDecayHelp(CommandContext ctx) {
        List<CommandHelp> commands = new ArrayList<>();
        commands.add(new CommandHelp("/f admin decay", "Show decay status"));
        commands.add(new CommandHelp("/f admin decay run", "Manually trigger claim decay"));
        commands.add(new CommandHelp("/f admin decay check <faction>", "Check faction decay status"));
        ctx.sendMessage(HelpFormatter.buildHelp("Claim Decay", "Auto-removes claims from inactive factions", commands, null));
    }

    private void showDecayStatus(CommandContext ctx) {
        ConfigManager config = ConfigManager.get();

        ctx.sendMessage(msg("=== Claim Decay Status ===", COLOR_CYAN).bold(true));
        ctx.sendMessage(msg("Enabled: ", COLOR_GRAY)
            .insert(msg(config.isDecayEnabled() ? "Yes" : "No", config.isDecayEnabled() ? COLOR_GREEN : COLOR_RED)));
        ctx.sendMessage(msg("Inactivity Threshold: ", COLOR_GRAY)
            .insert(msg(config.getDecayDaysInactive() + " days", COLOR_WHITE)));
        ctx.sendMessage(msg("Check Interval: ", COLOR_GRAY)
            .insert(msg("Every hour", COLOR_WHITE)));

        // Count factions at risk
        if (config.isDecayEnabled()) {
            int atRisk = 0;
            int totalWithClaims = 0;
            for (var faction : hyperFactions.getFactionManager().getAllFactions()) {
                if (faction.getClaimCount() > 0) {
                    totalWithClaims++;
                    int daysUntil = hyperFactions.getClaimManager().getDaysUntilDecay(faction.id());
                    if (daysUntil >= 0 && daysUntil <= 7) {
                        atRisk++;
                    }
                }
            }
            ctx.sendMessage(msg("Factions with claims: ", COLOR_GRAY)
                .insert(msg(String.valueOf(totalWithClaims), COLOR_WHITE)));
            ctx.sendMessage(msg("At risk (≤7 days): ", COLOR_GRAY)
                .insert(msg(String.valueOf(atRisk), atRisk > 0 ? COLOR_YELLOW : COLOR_GREEN)));
        }
    }

    private void handleDecayRun(CommandContext ctx) {
        ConfigManager config = ConfigManager.get();

        if (!config.isDecayEnabled()) {
            ctx.sendMessage(prefix().insert(msg("Claim decay is disabled in config.", COLOR_YELLOW)));
            ctx.sendMessage(msg("Set claims.decayEnabled to true to enable.", COLOR_GRAY));
            return;
        }

        ctx.sendMessage(prefix().insert(msg("Running claim decay check...", COLOR_YELLOW)));

        // Run decay on separate thread to avoid blocking
        CompletableFuture.runAsync(() -> {
            try {
                hyperFactions.getClaimManager().tickClaimDecay();
                ctx.sendMessage(prefix().insert(msg("Claim decay check complete. Check console for details.", COLOR_GREEN)));
            } catch (Exception e) {
                ctx.sendMessage(prefix().insert(msg("Error during decay: " + e.getMessage(), COLOR_RED)));
            }
        });
    }

    private void handleDecayCheck(CommandContext ctx, String[] args) {
        if (args.length < 1) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin decay check <faction>", COLOR_RED)));
            return;
        }

        String factionName = args[0];
        var faction = hyperFactions.getFactionManager().getFactionByName(factionName);
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("Faction '" + factionName + "' not found.", COLOR_RED)));
            return;
        }

        ConfigManager config = ConfigManager.get();

        ctx.sendMessage(msg("=== Decay Check: " + faction.name() + " ===", COLOR_CYAN).bold(true));
        ctx.sendMessage(msg("Claims: ", COLOR_GRAY).insert(msg(String.valueOf(faction.getClaimCount()), COLOR_WHITE)));

        if (faction.getClaimCount() == 0) {
            ctx.sendMessage(msg("No claims to decay.", COLOR_GRAY));
            return;
        }

        if (!config.isDecayEnabled()) {
            ctx.sendMessage(msg("Decay Status: ", COLOR_GRAY).insert(msg("Disabled globally", COLOR_YELLOW)));
            return;
        }

        // Find most recent login
        long mostRecentLogin = 0;
        String mostRecentPlayer = "Unknown";
        for (var member : faction.members().values()) {
            if (member.lastOnline() > mostRecentLogin) {
                mostRecentLogin = member.lastOnline();
                mostRecentPlayer = member.username();
            }
        }

        long daysSinceActive = (System.currentTimeMillis() - mostRecentLogin) / (24L * 60 * 60 * 1000);
        int daysUntilDecay = hyperFactions.getClaimManager().getDaysUntilDecay(faction.id());
        boolean isInactive = hyperFactions.getClaimManager().isFactionInactive(faction.id());

        ctx.sendMessage(msg("Last Active: ", COLOR_GRAY)
            .insert(msg(daysSinceActive + " days ago", COLOR_WHITE))
            .insert(msg(" (" + mostRecentPlayer + ")", COLOR_GRAY)));
        ctx.sendMessage(msg("Threshold: ", COLOR_GRAY)
            .insert(msg(config.getDecayDaysInactive() + " days", COLOR_WHITE)));

        if (isInactive) {
            ctx.sendMessage(msg("Status: ", COLOR_GRAY)
                .insert(msg("INACTIVE - Claims will decay on next check!", COLOR_RED).bold(true)));
        } else if (daysUntilDecay <= 7) {
            ctx.sendMessage(msg("Status: ", COLOR_GRAY)
                .insert(msg("AT RISK - " + daysUntilDecay + " days until decay", COLOR_YELLOW)));
        } else {
            ctx.sendMessage(msg("Status: ", COLOR_GRAY)
                .insert(msg("Active - " + daysUntilDecay + " days until decay", COLOR_GREEN)));
        }
    }

    // === Clear History ===

    private void handleClearHistory(CommandContext ctx, @Nullable PlayerRef player, String[] args) {
        if (!hasPermission(player, Permissions.ADMIN)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        if (args.length < 1) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin clearhistory <player>", COLOR_YELLOW)));
            return;
        }

        String targetName = args[0];

        // Resolve player UUID — check online players first, then faction members
        UUID targetUuid = null;
        String resolvedName = targetName;

        for (PlayerRef online : plugin.getTrackedPlayers().values()) {
            if (online.getUsername().equalsIgnoreCase(targetName)) {
                targetUuid = online.getUuid();
                resolvedName = online.getUsername();
                break;
            }
        }

        if (targetUuid == null) {
            for (Faction faction : hyperFactions.getFactionManager().getAllFactions()) {
                for (FactionMember member : faction.getMembersSorted()) {
                    if (member.username().equalsIgnoreCase(targetName)) {
                        targetUuid = member.uuid();
                        resolvedName = member.username();
                        break;
                    }
                }
                if (targetUuid != null) break;
            }
        }

        if (targetUuid == null) {
            ctx.sendMessage(prefix().insert(msg("Player not found.", COLOR_RED)));
            return;
        }

        final String finalName = resolvedName;
        hyperFactions.getPlayerStorage().loadPlayerData(targetUuid).thenAccept(opt -> {
            if (opt.isEmpty()) {
                ctx.sendMessage(prefix().insert(msg("No player data found for " + finalName + ".", COLOR_RED)));
                return;
            }

            PlayerData data = opt.get();
            int count = data.getMembershipHistory().size();

            if (count == 0) {
                ctx.sendMessage(prefix().insert(msg(finalName + " has no membership history.", COLOR_YELLOW)));
                return;
            }

            data.clearHistory();

            // Re-initialize with current faction if the player is in one
            Faction currentFaction = hyperFactions.getFactionManager().getPlayerFaction(data.getUuid());
            if (currentFaction != null) {
                FactionMember member = currentFaction.getMember(data.getUuid());
                if (member != null) {
                    MembershipRecord activeRecord = new MembershipRecord(
                        currentFaction.id(), currentFaction.name(), currentFaction.tag(),
                        member.role(), member.joinedAt(), 0, MembershipRecord.LeaveReason.ACTIVE
                    );
                    data.addRecord(activeRecord, ConfigManager.get().getMaxMembershipHistory());
                }
            }

            hyperFactions.getPlayerStorage().savePlayerData(data).thenRun(() -> {
                if (currentFaction != null) {
                    ctx.sendMessage(prefix().insert(msg("Cleared " + count + " history records for " + finalName
                        + " (re-initialized with current faction: " + currentFaction.name() + ").", COLOR_GREEN)));
                } else {
                    ctx.sendMessage(prefix().insert(msg("Cleared " + count + " history records for " + finalName + ".", COLOR_GREEN)));
                }
            });
        });
    }

    // === Admin Power Commands ===

    private void handleAdminPower(CommandContext ctx, @Nullable PlayerRef player, UUID senderUuid, String[] args) {
        if (!hasPermission(player, Permissions.ADMIN_POWER)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            showAdminPowerHelp(ctx);
            return;
        }

        String subCmd = args[0].toLowerCase();

        switch (subCmd) {
            case "set" -> handlePowerSet(ctx, senderUuid, args);
            case "add" -> handlePowerAdd(ctx, senderUuid, args);
            case "remove" -> handlePowerRemove(ctx, senderUuid, args);
            case "reset" -> handlePowerReset(ctx, senderUuid, args);
            case "setmax" -> handlePowerSetMax(ctx, senderUuid, args);
            case "resetmax" -> handlePowerResetMax(ctx, senderUuid, args);
            case "noloss" -> handlePowerNoLoss(ctx, senderUuid, args);
            case "nodecay" -> handlePowerNoDecay(ctx, senderUuid, args);
            case "faction" -> handlePowerFaction(ctx, senderUuid, args);
            case "info" -> handlePowerInfo(ctx, args);
            default -> ctx.sendMessage(prefix().insert(msg("Unknown power command. Use /f admin power help", COLOR_RED)));
        }
    }

    private void showAdminPowerHelp(CommandContext ctx) {
        List<CommandHelp> commands = new ArrayList<>();
        commands.add(new CommandHelp("/f admin power set <player> <amount>", "Set exact power"));
        commands.add(new CommandHelp("/f admin power add <player> <amount>", "Increase power"));
        commands.add(new CommandHelp("/f admin power remove <player> <amount>", "Decrease power"));
        commands.add(new CommandHelp("/f admin power reset <player>", "Reset to default"));
        commands.add(new CommandHelp("/f admin power setmax <player> <amount>", "Set max power override"));
        commands.add(new CommandHelp("/f admin power resetmax <player>", "Clear max override"));
        commands.add(new CommandHelp("/f admin power noloss <player>", "Toggle power loss bypass"));
        commands.add(new CommandHelp("/f admin power nodecay <player>", "Toggle claim decay exemption"));
        commands.add(new CommandHelp("/f admin power faction <name> <action>", "Faction-wide operations"));
        commands.add(new CommandHelp("/f admin power info <player>", "Show player power details"));
        ctx.sendMessage(HelpFormatter.buildHelp("Admin Power", "Manage player/faction power", commands, null));
    }

    /**
     * Resolves a player UUID from their username. Checks online players first, then faction members.
     * Returns null if not found.
     */
    private record ResolvedPlayer(UUID uuid, String name) {}

    @Nullable
    private ResolvedPlayer resolvePlayer(String targetName) {
        // Check online players
        for (PlayerRef online : plugin.getTrackedPlayers().values()) {
            if (online.getUsername().equalsIgnoreCase(targetName)) {
                return new ResolvedPlayer(online.getUuid(), online.getUsername());
            }
        }
        // Check faction members
        for (Faction faction : hyperFactions.getFactionManager().getAllFactions()) {
            for (FactionMember member : faction.getMembersSorted()) {
                if (member.username().equalsIgnoreCase(targetName)) {
                    return new ResolvedPlayer(member.uuid(), member.username());
                }
            }
        }
        return null;
    }

    /**
     * Logs an admin power change to the player's faction activity log.
     */
    private void logAdminPowerChange(UUID targetUuid, UUID adminUuid, String message) {
        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(targetUuid);
        if (faction != null) {
            Faction updated = faction.withLog(FactionLog.create(FactionLog.LogType.ADMIN_POWER, message, adminUuid));
            hyperFactions.getFactionManager().updateFaction(updated);
        }
    }

    // /f admin power set <player> <amount>
    private void handlePowerSet(CommandContext ctx, UUID senderUuid, String[] args) {
        if (args.length < 3) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin power set <player> <amount>", COLOR_YELLOW)));
            return;
        }
        ResolvedPlayer target = resolvePlayer(args[1]);
        if (target == null) {
            ctx.sendMessage(prefix().insert(msg("Player not found: " + args[1], COLOR_RED)));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            ctx.sendMessage(prefix().insert(msg("Invalid number: " + args[2], COLOR_RED)));
            return;
        }

        double oldPower = hyperFactions.getPowerManager().getPlayerPower(target.uuid()).power();
        double newPower = hyperFactions.getPowerManager().setPlayerPower(target.uuid(), amount);
        logAdminPowerChange(target.uuid(), senderUuid,
            "Admin set " + target.name() + "'s power to " + String.format("%.1f", newPower) + " (was " + String.format("%.1f", oldPower) + ")");
        ctx.sendMessage(prefix().insert(msg("Set ", COLOR_GREEN))
            .insert(msg(target.name(), COLOR_CYAN))
            .insert(msg("'s power to ", COLOR_GREEN))
            .insert(msg(String.format("%.1f", newPower), COLOR_WHITE))
            .insert(msg(" (was " + String.format("%.1f", oldPower) + ")", COLOR_GRAY)));
    }

    // /f admin power add <player> <amount>
    private void handlePowerAdd(CommandContext ctx, UUID senderUuid, String[] args) {
        if (args.length < 3) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin power add <player> <amount>", COLOR_YELLOW)));
            return;
        }
        ResolvedPlayer target = resolvePlayer(args[1]);
        if (target == null) {
            ctx.sendMessage(prefix().insert(msg("Player not found: " + args[1], COLOR_RED)));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            ctx.sendMessage(prefix().insert(msg("Invalid number: " + args[2], COLOR_RED)));
            return;
        }

        double oldPower = hyperFactions.getPowerManager().getPlayerPower(target.uuid()).power();
        double newPower = hyperFactions.getPowerManager().adjustPlayerPower(target.uuid(), amount);
        logAdminPowerChange(target.uuid(), senderUuid,
            "Admin added " + String.format("%.1f", amount) + " power to " + target.name() + " (" + String.format("%.1f", oldPower) + " -> " + String.format("%.1f", newPower) + ")");
        ctx.sendMessage(prefix().insert(msg("Added ", COLOR_GREEN))
            .insert(msg(String.format("%.1f", amount), COLOR_WHITE))
            .insert(msg(" power to ", COLOR_GREEN))
            .insert(msg(target.name(), COLOR_CYAN))
            .insert(msg(" (" + String.format("%.1f", oldPower) + " -> " + String.format("%.1f", newPower) + ")", COLOR_GRAY)));
    }

    // /f admin power remove <player> <amount>
    private void handlePowerRemove(CommandContext ctx, UUID senderUuid, String[] args) {
        if (args.length < 3) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin power remove <player> <amount>", COLOR_YELLOW)));
            return;
        }
        ResolvedPlayer target = resolvePlayer(args[1]);
        if (target == null) {
            ctx.sendMessage(prefix().insert(msg("Player not found: " + args[1], COLOR_RED)));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            ctx.sendMessage(prefix().insert(msg("Invalid number: " + args[2], COLOR_RED)));
            return;
        }

        double oldPower = hyperFactions.getPowerManager().getPlayerPower(target.uuid()).power();
        double newPower = hyperFactions.getPowerManager().adjustPlayerPower(target.uuid(), -amount);
        logAdminPowerChange(target.uuid(), senderUuid,
            "Admin removed " + String.format("%.1f", amount) + " power from " + target.name() + " (" + String.format("%.1f", oldPower) + " -> " + String.format("%.1f", newPower) + ")");
        ctx.sendMessage(prefix().insert(msg("Removed ", COLOR_GREEN))
            .insert(msg(String.format("%.1f", amount), COLOR_WHITE))
            .insert(msg(" power from ", COLOR_GREEN))
            .insert(msg(target.name(), COLOR_CYAN))
            .insert(msg(" (" + String.format("%.1f", oldPower) + " -> " + String.format("%.1f", newPower) + ")", COLOR_GRAY)));
    }

    // /f admin power reset <player>
    private void handlePowerReset(CommandContext ctx, UUID senderUuid, String[] args) {
        if (args.length < 2) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin power reset <player>", COLOR_YELLOW)));
            return;
        }
        ResolvedPlayer target = resolvePlayer(args[1]);
        if (target == null) {
            ctx.sendMessage(prefix().insert(msg("Player not found: " + args[1], COLOR_RED)));
            return;
        }

        double oldPower = hyperFactions.getPowerManager().getPlayerPower(target.uuid()).power();
        double newPower = hyperFactions.getPowerManager().resetPlayerPower(target.uuid());
        logAdminPowerChange(target.uuid(), senderUuid,
            "Admin reset " + target.name() + "'s power to " + String.format("%.1f", newPower) + " (was " + String.format("%.1f", oldPower) + ")");
        ctx.sendMessage(prefix().insert(msg("Reset ", COLOR_GREEN))
            .insert(msg(target.name(), COLOR_CYAN))
            .insert(msg("'s power to ", COLOR_GREEN))
            .insert(msg(String.format("%.1f", newPower), COLOR_WHITE))
            .insert(msg(" (was " + String.format("%.1f", oldPower) + ")", COLOR_GRAY)));
    }

    // /f admin power setmax <player> <amount>
    private void handlePowerSetMax(CommandContext ctx, UUID senderUuid, String[] args) {
        if (args.length < 3) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin power setmax <player> <amount>", COLOR_YELLOW)));
            return;
        }
        ResolvedPlayer target = resolvePlayer(args[1]);
        if (target == null) {
            ctx.sendMessage(prefix().insert(msg("Player not found: " + args[1], COLOR_RED)));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
            if (amount <= 0) {
                ctx.sendMessage(prefix().insert(msg("Max power must be positive.", COLOR_RED)));
                return;
            }
        } catch (NumberFormatException e) {
            ctx.sendMessage(prefix().insert(msg("Invalid number: " + args[2], COLOR_RED)));
            return;
        }

        PlayerPower oldPower = hyperFactions.getPowerManager().getPlayerPower(target.uuid());
        double oldMax = oldPower.getEffectiveMaxPower();
        double newCurrentPower = hyperFactions.getPowerManager().setPlayerMaxPower(target.uuid(), amount);
        logAdminPowerChange(target.uuid(), senderUuid,
            "Admin set " + target.name() + "'s max power to " + String.format("%.1f", amount) + " (was " + String.format("%.1f", oldMax) + ")");
        ctx.sendMessage(prefix().insert(msg("Set ", COLOR_GREEN))
            .insert(msg(target.name(), COLOR_CYAN))
            .insert(msg("'s max power to ", COLOR_GREEN))
            .insert(msg(String.format("%.1f", amount), COLOR_WHITE))
            .insert(msg(" (was " + String.format("%.1f", oldMax) + ", power now " + String.format("%.1f", newCurrentPower) + ")", COLOR_GRAY)));
    }

    // /f admin power resetmax <player>
    private void handlePowerResetMax(CommandContext ctx, UUID senderUuid, String[] args) {
        if (args.length < 2) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin power resetmax <player>", COLOR_YELLOW)));
            return;
        }
        ResolvedPlayer target = resolvePlayer(args[1]);
        if (target == null) {
            ctx.sendMessage(prefix().insert(msg("Player not found: " + args[1], COLOR_RED)));
            return;
        }

        PlayerPower oldPower = hyperFactions.getPowerManager().getPlayerPower(target.uuid());
        double oldMax = oldPower.getEffectiveMaxPower();
        hyperFactions.getPowerManager().resetPlayerMaxPower(target.uuid());
        double globalMax = ConfigManager.get().getMaxPlayerPower();
        logAdminPowerChange(target.uuid(), senderUuid,
            "Admin reset " + target.name() + "'s max power to global default (" + String.format("%.1f", globalMax) + ")");
        ctx.sendMessage(prefix().insert(msg("Reset ", COLOR_GREEN))
            .insert(msg(target.name(), COLOR_CYAN))
            .insert(msg("'s max power to global default ", COLOR_GREEN))
            .insert(msg(String.format("%.1f", globalMax), COLOR_WHITE))
            .insert(msg(" (was " + String.format("%.1f", oldMax) + ")", COLOR_GRAY)));
    }

    // /f admin power noloss <player>
    private void handlePowerNoLoss(CommandContext ctx, UUID senderUuid, String[] args) {
        if (args.length < 2) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin power noloss <player>", COLOR_YELLOW)));
            return;
        }
        ResolvedPlayer target = resolvePlayer(args[1]);
        if (target == null) {
            ctx.sendMessage(prefix().insert(msg("Player not found: " + args[1], COLOR_RED)));
            return;
        }

        PlayerPower current = hyperFactions.getPowerManager().getPlayerPower(target.uuid());
        boolean newState = !current.powerLossDisabled();
        hyperFactions.getPowerManager().setPlayerPowerLossDisabled(target.uuid(), newState);
        logAdminPowerChange(target.uuid(), senderUuid,
            "Admin " + (newState ? "disabled" : "enabled") + " power loss for " + target.name());
        ctx.sendMessage(prefix().insert(msg("Power loss ", COLOR_GREEN))
            .insert(msg(newState ? "disabled" : "enabled", newState ? COLOR_RED : COLOR_GREEN))
            .insert(msg(" for ", COLOR_GREEN))
            .insert(msg(target.name(), COLOR_CYAN)));
    }

    // /f admin power nodecay <player>
    private void handlePowerNoDecay(CommandContext ctx, UUID senderUuid, String[] args) {
        if (args.length < 2) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin power nodecay <player>", COLOR_YELLOW)));
            return;
        }
        ResolvedPlayer target = resolvePlayer(args[1]);
        if (target == null) {
            ctx.sendMessage(prefix().insert(msg("Player not found: " + args[1], COLOR_RED)));
            return;
        }

        PlayerPower current = hyperFactions.getPowerManager().getPlayerPower(target.uuid());
        boolean newState = !current.claimDecayExempt();
        hyperFactions.getPowerManager().setPlayerClaimDecayExempt(target.uuid(), newState);
        logAdminPowerChange(target.uuid(), senderUuid,
            "Admin " + (newState ? "enabled" : "disabled") + " claim decay exemption for " + target.name());
        ctx.sendMessage(prefix().insert(msg("Claim decay exemption ", COLOR_GREEN))
            .insert(msg(newState ? "enabled" : "disabled", newState ? COLOR_GREEN : COLOR_RED))
            .insert(msg(" for ", COLOR_GREEN))
            .insert(msg(target.name(), COLOR_CYAN)));
    }

    // /f admin power faction <name> set|add|remove|reset <amount>
    private void handlePowerFaction(CommandContext ctx, UUID senderUuid, String[] args) {
        if (args.length < 3) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin power faction <name> set|add|remove|reset [amount]", COLOR_YELLOW)));
            return;
        }
        String factionName = args[1];
        Faction faction = hyperFactions.getFactionManager().getFactionByName(factionName);
        if (faction == null) {
            ctx.sendMessage(prefix().insert(msg("Faction not found: " + factionName, COLOR_RED)));
            return;
        }

        String action = args[2].toLowerCase();
        Set<UUID> members = faction.members().keySet();

        switch (action) {
            case "set" -> {
                if (args.length < 4) {
                    ctx.sendMessage(prefix().insert(msg("Usage: /f admin power faction <name> set <amount>", COLOR_YELLOW)));
                    return;
                }
                double amount = parseDouble(ctx, args[3]);
                if (Double.isNaN(amount)) return;
                for (UUID memberUuid : members) {
                    hyperFactions.getPowerManager().setPlayerPower(memberUuid, amount);
                }
                hyperFactions.getFactionManager().updateFaction(faction.withLog(FactionLog.create(
                    FactionLog.LogType.ADMIN_POWER,
                    "Admin set all " + members.size() + " members' power to " + String.format("%.1f", amount),
                    senderUuid)));
                ctx.sendMessage(prefix().insert(msg("Set power to ", COLOR_GREEN))
                    .insert(msg(String.format("%.1f", amount), COLOR_WHITE))
                    .insert(msg(" for " + members.size() + " members of ", COLOR_GREEN))
                    .insert(msg(faction.name(), COLOR_CYAN)));
            }
            case "add" -> {
                if (args.length < 4) {
                    ctx.sendMessage(prefix().insert(msg("Usage: /f admin power faction <name> add <amount>", COLOR_YELLOW)));
                    return;
                }
                double amount = parseDouble(ctx, args[3]);
                if (Double.isNaN(amount)) return;
                for (UUID memberUuid : members) {
                    hyperFactions.getPowerManager().adjustPlayerPower(memberUuid, amount);
                }
                hyperFactions.getFactionManager().updateFaction(faction.withLog(FactionLog.create(
                    FactionLog.LogType.ADMIN_POWER,
                    "Admin added " + String.format("%.1f", amount) + " power to all " + members.size() + " members",
                    senderUuid)));
                ctx.sendMessage(prefix().insert(msg("Added ", COLOR_GREEN))
                    .insert(msg(String.format("%.1f", amount), COLOR_WHITE))
                    .insert(msg(" power to " + members.size() + " members of ", COLOR_GREEN))
                    .insert(msg(faction.name(), COLOR_CYAN)));
            }
            case "remove" -> {
                if (args.length < 4) {
                    ctx.sendMessage(prefix().insert(msg("Usage: /f admin power faction <name> remove <amount>", COLOR_YELLOW)));
                    return;
                }
                double amount = parseDouble(ctx, args[3]);
                if (Double.isNaN(amount)) return;
                for (UUID memberUuid : members) {
                    hyperFactions.getPowerManager().adjustPlayerPower(memberUuid, -amount);
                }
                hyperFactions.getFactionManager().updateFaction(faction.withLog(FactionLog.create(
                    FactionLog.LogType.ADMIN_POWER,
                    "Admin removed " + String.format("%.1f", amount) + " power from all " + members.size() + " members",
                    senderUuid)));
                ctx.sendMessage(prefix().insert(msg("Removed ", COLOR_GREEN))
                    .insert(msg(String.format("%.1f", amount), COLOR_WHITE))
                    .insert(msg(" power from " + members.size() + " members of ", COLOR_GREEN))
                    .insert(msg(faction.name(), COLOR_CYAN)));
            }
            case "reset" -> {
                for (UUID memberUuid : members) {
                    hyperFactions.getPowerManager().resetPlayerPower(memberUuid);
                }
                hyperFactions.getFactionManager().updateFaction(faction.withLog(FactionLog.create(
                    FactionLog.LogType.ADMIN_POWER,
                    "Admin reset power for all " + members.size() + " members",
                    senderUuid)));
                ctx.sendMessage(prefix().insert(msg("Reset power for ", COLOR_GREEN))
                    .insert(msg(String.valueOf(members.size()), COLOR_WHITE))
                    .insert(msg(" members of ", COLOR_GREEN))
                    .insert(msg(faction.name(), COLOR_CYAN)));
            }
            default -> ctx.sendMessage(prefix().insert(msg("Unknown faction power action. Use: set, add, remove, reset", COLOR_RED)));
        }
    }

    // /f admin power info <player>
    private void handlePowerInfo(CommandContext ctx, String[] args) {
        if (args.length < 2) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f admin power info <player>", COLOR_YELLOW)));
            return;
        }
        ResolvedPlayer target = resolvePlayer(args[1]);
        if (target == null) {
            ctx.sendMessage(prefix().insert(msg("Player not found: " + args[1], COLOR_RED)));
            return;
        }

        PlayerPower power = hyperFactions.getPowerManager().getPlayerPower(target.uuid());
        ctx.sendMessage(prefix().insert(msg("Power Info: ", COLOR_CYAN)).insert(msg(target.name(), COLOR_WHITE)));
        ctx.sendMessage(msg("  Power: ", COLOR_GRAY)
            .insert(msg(String.format("%.1f / %.1f (%d%%)", power.power(), power.getEffectiveMaxPower(), power.getPowerPercent()), COLOR_WHITE)));
        ctx.sendMessage(msg("  Base Max: ", COLOR_GRAY)
            .insert(msg(String.format("%.1f", power.maxPower()), COLOR_WHITE)));
        if (power.maxPowerOverride() != null) {
            ctx.sendMessage(msg("  Max Override: ", COLOR_GRAY)
                .insert(msg(String.format("%.1f", power.maxPowerOverride()), COLOR_YELLOW)));
        } else {
            ctx.sendMessage(msg("  Max Override: ", COLOR_GRAY)
                .insert(msg("None (using global)", COLOR_GRAY)));
        }
        ctx.sendMessage(msg("  Power Loss: ", COLOR_GRAY)
            .insert(msg(power.powerLossDisabled() ? "DISABLED (immune)" : "Enabled", power.powerLossDisabled() ? COLOR_RED : COLOR_GREEN)));
        ctx.sendMessage(msg("  Claim Decay: ", COLOR_GRAY)
            .insert(msg(power.claimDecayExempt() ? "EXEMPT (always online)" : "Normal", power.claimDecayExempt() ? COLOR_YELLOW : COLOR_GREEN)));

        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(target.uuid());
        if (faction != null) {
            ctx.sendMessage(msg("  Faction: ", COLOR_GRAY).insert(msg(faction.name(), COLOR_CYAN)));
        } else {
            ctx.sendMessage(msg("  Faction: ", COLOR_GRAY).insert(msg("None", COLOR_GRAY)));
        }
    }

    /**
     * Parses a double from string, sending error message if invalid.
     * Returns NaN on failure.
     */
    private double parseDouble(CommandContext ctx, String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            ctx.sendMessage(prefix().insert(msg("Invalid number: " + value, COLOR_RED)));
            return Double.NaN;
        }
    }
}
