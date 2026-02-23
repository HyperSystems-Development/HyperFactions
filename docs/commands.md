# HyperFactions Command System

> **Version**: 0.9.0 | **42 subcommands** across **10 categories**

Architecture documentation for the HyperFactions command system.

## Overview

HyperFactions uses a subcommand-based dispatcher pattern built on Hytale's `AbstractPlayerCommand` system. The main `/faction` command routes to category-specific subcommands.

## Architecture

```
FactionCommand (dispatcher): /f, /hf, /faction, /hyperfactions
     │
     ├─► FactionSubCommand (base class)
     │        │
     │        ├─► command/faction/     (7 subcommands: create, disband, rename, desc, color, open, close)
     │        ├─► command/member/      (7 subcommands: invite, accept, leave, kick, promote, demote, transfer)
     │        ├─► command/territory/   (4 subcommands: claim, unclaim, overclaim, stuck)
     │        ├─► command/teleport/    (3 subcommands: home, sethome, delhome)
     │        ├─► command/relation/    (4 subcommands: ally, enemy, neutral, relations)
     │        ├─► command/info/        (6 subcommands: info, list, map, members, who, power)
     │        ├─► command/social/      (3 subcommands: request, invites, chat)
     │        ├─► command/ui/          (2 subcommands: gui, settings)
     │        ├─► HelpSubCommand       (1 subcommand: help)
     │        └─► command/admin/       (17+ admin subcommands with nested routing)
     │
     └─► FactionCommandContext (execution state, --text flag)
```

## Key Classes

| Class | Path | Purpose |
|-------|------|---------|
| FactionCommand | [`command/FactionCommand.java`](../src/main/java/com/hyperfactions/command/FactionCommand.java) | Main dispatcher, registers all subcommands |
| FactionSubCommand | [`command/FactionSubCommand.java`](../src/main/java/com/hyperfactions/command/FactionSubCommand.java) | Base class with shared utilities and `requireFaction()` helper |
| FactionCommandContext | [`command/FactionCommandContext.java`](../src/main/java/com/hyperfactions/command/FactionCommandContext.java) | Execution context, `--text` flag parsing |
| CommandUtil | [`command/util/CommandUtil.java`](../src/main/java/com/hyperfactions/command/util/CommandUtil.java) | Shared utilities (delegates to `util/MessageUtil` for message composition) |

## Main Dispatcher

[`FactionCommand.java`](../src/main/java/com/hyperfactions/command/FactionCommand.java)

The main command class:

```java
public class FactionCommand extends AbstractPlayerCommand {

    public FactionCommand(HyperFactions hyperFactions, HyperFactionsPlugin plugin) {
        super("faction", "Faction management commands");
        addAliases("f", "hf", "hyperfactions");
        setAllowsExtraArguments(true);

        // Register all subcommands by category

        // Faction management
        addSubCommand(new CreateSubCommand(hyperFactions, plugin));
        addSubCommand(new DisbandSubCommand(hyperFactions, plugin));
        // ...

        // Member management
        addSubCommand(new InviteSubCommand(hyperFactions, plugin));
        // ...
    }

    @Override
    protected void execute(...) {
        // No subcommand provided - open GUI dashboard
        hyperFactions.getGuiManager().openFactionMain(player, ...);
    }
}
```

**Key Points:**
- Aliases: `/f`, `/hf`, `/hyperfactions`
- No subcommand → opens GUI (if has `hyperfactions.use` permission)
- Disables Hytale's auto-generated permissions via `canGeneratePermission() = false`

## Subcommand Base Class

[`FactionSubCommand.java`](../src/main/java/com/hyperfactions/command/FactionSubCommand.java)

All subcommands extend this base class which provides:

```java
public abstract class FactionSubCommand extends AbstractPlayerCommand {

    protected final HyperFactions hyperFactions;
    protected final HyperFactionsPlugin plugin;

    // Utility methods
    protected Message prefix() { ... }           // "[HyperFactions]" prefix
    protected Message msg(String, String) { ... } // Colored message
    protected boolean hasPermission(PlayerRef, String) { ... }
    protected PlayerRef findOnlinePlayer(String) { ... }
    protected void broadcastToFaction(UUID, Message) { ... }
    protected FactionCommandContext parseContext(String[] args) { ... }
    protected Faction requireFaction(PlayerRef, CommandContext) { ... } // Returns player's faction or sends error

    // Color constants
    protected static final String COLOR_CYAN = "#00FFFF";
    protected static final String COLOR_GREEN = "#00FF00";
    protected static final String COLOR_RED = "#FF0000";
    protected static final String COLOR_YELLOW = "#FFFF00";
    protected static final String COLOR_GRAY = "#888888";
}
```

## Command Context

[`FactionCommandContext.java`](../src/main/java/com/hyperfactions/command/FactionCommandContext.java)

Handles execution context including the `--text` flag for text-mode output:

```java
public record FactionCommandContext(
    boolean textMode,      // --text flag present
    String[] args          // Remaining args after flag extraction
) {
    public static FactionCommandContext parse(String[] rawArgs) {
        // Extract --text flag and return remaining args
    }
}
```

**Text Mode:**
- `--text` flag disables GUI and uses chat output instead
- Useful for console/automation or players preferring text

## Subcommand Categories

### Package Structure

```
command/
├── FactionCommand.java         # Main dispatcher
├── FactionSubCommand.java      # Base class
├── FactionCommandContext.java  # Execution context
├── util/CommandUtil.java       # Shared utilities
│
├── faction/                    # Faction management
│   ├── CreateSubCommand.java
│   ├── DisbandSubCommand.java
│   ├── RenameSubCommand.java
│   ├── DescSubCommand.java
│   ├── ColorSubCommand.java
│   ├── OpenSubCommand.java
│   └── CloseSubCommand.java
│
├── member/                     # Membership
│   ├── InviteSubCommand.java
│   ├── AcceptSubCommand.java
│   ├── LeaveSubCommand.java
│   ├── KickSubCommand.java
│   ├── PromoteSubCommand.java
│   ├── DemoteSubCommand.java
│   └── TransferSubCommand.java
│
├── territory/                  # Territory claims
│   ├── ClaimSubCommand.java
│   ├── UnclaimSubCommand.java
│   ├── OverclaimSubCommand.java
│   └── StuckSubCommand.java
│
├── teleport/                   # Teleportation
│   ├── HomeSubCommand.java
│   └── SetHomeSubCommand.java
│
├── relation/                   # Diplomacy
│   ├── AllySubCommand.java
│   ├── EnemySubCommand.java
│   ├── NeutralSubCommand.java
│   └── RelationsSubCommand.java
│
├── info/                       # Information
│   ├── InfoSubCommand.java
│   ├── ListSubCommand.java
│   ├── MapSubCommand.java
│   ├── MembersSubCommand.java
│   ├── WhoSubCommand.java
│   ├── PowerSubCommand.java
│   └── HelpSubCommand.java
│
├── social/                     # Social features
│   ├── RequestSubCommand.java
│   ├── InvitesSubCommand.java
│   └── ChatSubCommand.java
│
├── ui/                         # UI commands
│   ├── GuiSubCommand.java
│   └── SettingsSubCommand.java
│
└── admin/                      # Admin commands
    ├── AdminSubCommand.java    # Router delegating to handler/ classes
    └── handler/               # Admin command handlers
        ├── AdminZoneHandler.java
        ├── AdminBackupHandler.java
        ├── AdminDebugHandler.java
        ├── AdminImportHandler.java
        ├── AdminUpdateHandler.java
        ├── AdminIntegrationHandler.java
        ├── AdminPowerHandler.java
        └── AdminMapDecayHandler.java
```

### Category Summary

| Category | Commands | Permission Prefix |
|----------|----------|-------------------|
| faction | create, disband, rename, desc, color, open, close | `hyperfactions.faction.*` |
| member | invite, accept, leave, kick, promote, demote, transfer | `hyperfactions.member.*` |
| territory | claim, unclaim, overclaim, stuck | `hyperfactions.territory.*` |
| teleport | home, sethome, delhome | `hyperfactions.teleport.*` |
| relation | ally, enemy, neutral, relations | `hyperfactions.relation.*` |
| info | info, list, map, members, who, power, help | `hyperfactions.info.*` |
| social | request, invites, chat | `hyperfactions.member.*`, `hyperfactions.chat.*` |
| ui | gui, settings | `hyperfactions.use` |
| admin | zone, backup, reload, debug, bypass | `hyperfactions.admin.*` |

### Notable Command Behaviors

**`/f stuck`** — Teleports the player to a random safe unclaimed chunk. Walks outward in a random direction from the player's position, increasing the search radius on each failed attempt. Configurable via `stuckMinRadius`, `stuckRadiusIncrease`, and `stuckMaxAttempts` in config.json. Uses the faction teleport warmup/cooldown system.

**`/f info [faction] --text`** — Text mode shows ally/enemy counts and bidirectional relation status. Displays "They consider you" and "You consider them" lines using `RelationManager.getEffectiveRelation()` for accurate bidirectional awareness. Color-coded: green for ally, red for enemy, gray for neutral.

**`/f claim` / `/f unclaim`** — 500ms per-player debounce prevents double-execution from rapid command dispatch.

## Subcommand Implementation Pattern

Example: [`command/territory/ClaimSubCommand.java`](../src/main/java/com/hyperfactions/command/territory/ClaimSubCommand.java)

```java
public class ClaimSubCommand extends FactionSubCommand {

    public ClaimSubCommand(HyperFactions hyperFactions, HyperFactionsPlugin plugin) {
        super("claim", "Claim the current chunk for your faction", hyperFactions, plugin);
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store,
                          Ref<EntityStore> ref, PlayerRef player, World world) {

        // 1. Parse context for --text flag
        FactionCommandContext fctx = parseContext(ctx.getRemainingArguments());

        // 2. Permission check (optional - manager also checks)
        if (!hasPermission(player, Permissions.CLAIM)) {
            ctx.sendMessage(prefix().insert(msg("No permission.", COLOR_RED)));
            return;
        }

        // 3. Get player position
        int chunkX = ChunkUtil.toChunkCoord(player.getPosition().getX());
        int chunkZ = ChunkUtil.toChunkCoord(player.getPosition().getZ());

        // 4. Call manager (does permission check + business logic)
        ClaimResult result = hyperFactions.getClaimManager()
            .claim(player.getUuid(), world.getName(), chunkX, chunkZ);

        // 5. Handle result
        switch (result) {
            case SUCCESS -> {
                ctx.sendMessage(prefix().insert(msg("Chunk claimed!", COLOR_GREEN)));
            }
            case NO_PERMISSION -> {
                ctx.sendMessage(prefix().insert(msg("No permission.", COLOR_RED)));
            }
            case INSUFFICIENT_POWER -> {
                ctx.sendMessage(prefix().insert(msg("Not enough power.", COLOR_RED)));
            }
            // ... other cases
        }
    }
}
```

## Permission Checking

Permissions are checked at **two levels**:

1. **Command Level** (optional) - Early rejection with specific error message
2. **Manager Level** (required) - Ensures GUI operations are also protected

```java
// Command checks permission for specific error message
if (!hasPermission(player, Permissions.CLAIM)) {
    ctx.sendMessage(msg("You need hyperfactions.territory.claim", COLOR_RED));
    return;
}

// Manager also checks (protects GUI path)
ClaimResult result = claimManager.claim(uuid, world, x, z);
if (result == ClaimResult.NO_PERMISSION) {
    // Shouldn't reach here if command checked, but GUI might call directly
}
```

## Admin Commands

[`command/admin/AdminSubCommand.java`](../src/main/java/com/hyperfactions/command/admin/AdminSubCommand.java)

`AdminSubCommand` acts as a router that delegates to specialized handler classes in `command/admin/handler/`:

- `AdminZoneHandler` - Zone create/delete/claim/unclaim/radius/list, zoneflag, safezone, warzone
- `AdminBackupHandler` - Backup create/list/restore/delete
- `AdminDebugHandler` - Debug toggle, trace, diagnostics
- `AdminImportHandler` - Data import from other faction plugins
- `AdminUpdateHandler` - Update check and notification
- `AdminIntegrationHandler` - Integration status reporting
- `AdminPowerHandler` - Power set/reset/modify
- `AdminMapDecayHandler` - Map decay management

Admin commands use nested subcommand structure:

```
/f admin
├── zone          # Zone management
│   ├── create
│   ├── delete
│   ├── claim
│   ├── unclaim
│   ├── radius
│   └── list
├── zoneflag      # Zone flag management
├── safezone      # Quick SafeZone creation
├── warzone       # Quick WarZone creation
├── bypass        # Toggle admin bypass
├── backup        # Backup management
│   ├── create
│   ├── list
│   ├── restore
│   └── delete
├── reload        # Reload config
├── update        # Check for updates
│   ├── mixin     # Check/download HyperProtect-Mixin
│   └── toggle-mixin-download  # Toggle HP-Mixin auto-download
└── debug         # Debug commands
```

## Message Formatting

Commands use the `Message` API with `Message.join()`:

```java
// Correct: use Message.join() for composition
Message msg = Message.join(
    prefix(),
    Message.text("Player "),
    Message.text(playerName).color(Color.hex(COLOR_CYAN)),
    Message.text(" joined your faction!")
);

// Wrong: don't use .then() (legacy API)
// Wrong: don't use legacy color codes (§c, &c)
```

## Tab Completion

Hytale handles tab completion automatically based on command arguments. For custom completion, override in subcommand:

```java
@Override
protected List<String> tabComplete(CommandContext ctx, PlayerRef player) {
    String[] args = ctx.getRemainingArguments();
    if (args.length == 1) {
        // Complete first argument with faction names
        return hyperFactions.getFactionManager().getAllFactions()
            .stream()
            .map(Faction::name)
            .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
            .toList();
    }
    return List.of();
}
```

## Adding a New Command

1. Create subcommand class in appropriate category package
2. Extend `FactionSubCommand`
3. Implement `execute()` method
4. Register in `FactionCommand` constructor
5. Add permission constant to `Permissions.java` if new
6. Update help system if needed

Example skeleton:

```java
package com.hyperfactions.command.faction;

public class NewSubCommand extends FactionSubCommand {

    public NewSubCommand(HyperFactions hyperFactions, HyperFactionsPlugin plugin) {
        super("newcmd", "Description of the command", hyperFactions, plugin);
        // Optional: add aliases
        addAliases("nc");
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store,
                          Ref<EntityStore> ref, PlayerRef player, World world) {
        // Implementation
    }
}
```

Register in `FactionCommand`:
```java
addSubCommand(new NewSubCommand(hyperFactions, plugin));
```

## Code Links

| Class | Path |
|-------|------|
| FactionCommand | [`command/FactionCommand.java`](../src/main/java/com/hyperfactions/command/FactionCommand.java) |
| FactionSubCommand | [`command/FactionSubCommand.java`](../src/main/java/com/hyperfactions/command/FactionSubCommand.java) |
| FactionCommandContext | [`command/FactionCommandContext.java`](../src/main/java/com/hyperfactions/command/FactionCommandContext.java) |
| CommandUtil | [`command/util/CommandUtil.java`](../src/main/java/com/hyperfactions/command/util/CommandUtil.java) |
| AdminSubCommand | [`command/admin/AdminSubCommand.java`](../src/main/java/com/hyperfactions/command/admin/AdminSubCommand.java) |
| Permissions | [`Permissions.java`](../src/main/java/com/hyperfactions/Permissions.java) |
