# Changelog

All notable changes to HyperFactions will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

**Sentry Error Tracking Integration**
- Sentry SDK (v8.33.0) bundled for automatic error reporting to Sentry dashboard
- Non-blocking async event delivery — Sentry never impacts server performance
- All Sentry operations wrapped in try/catch — failures never crash the server
- Sentry config nested under `config/debug.json` → `"sentry"` section (no separate file)
- Auto-migration: existing `config/sentry.json` values are read into `debug.json` on first load, old file deleted
- DSN pre-configured with default — works out of the box
- Source context upload via Sentry Gradle plugin (stack traces show source code in Sentry)
- HyperFactions frames highlighted in stack traces via `addInAppInclude`
- New admin command: `/f admin sentry` — view status, enable/disable error reporting at runtime
- New admin command: `/f admin sentrytest` — sends a test error with stack trace to verify integration
- Sentry cleanly flushes pending events on server shutdown (2s timeout)
- Auth token stored in `.sentry-auth-token` file (gitignored) with env var fallback

**Global Error Handling via ErrorHandler**
- New `ErrorHandler` utility class — centralized error handling that logs to console AND reports to Sentry
- 6 static methods covering all error patterns: `report()`, `report(@Nullable)`, `wrapTask()`, `guard()`, `runSafely()` (2 overloads)
- ~190 `Logger.severe()` calls in catch blocks across ~50 files now route through ErrorHandler to Sentry
- Scheduled/timer tasks wrapped with `wrapTask()` — exceptions no longer silently kill scheduler threads
- CompletableFuture chains guarded with `guard()` — async errors no longer swallowed
- Shutdown sequence steps isolated with `runSafely()` — one failure doesn't skip remaining cleanup
- `WriteResult.Failure` storage errors (with `@Nullable Exception cause`) now report to Sentry
- Pre-init error buffering: errors during config/data loading (before Sentry initializes) are buffered and flushed once Sentry is ready, tagged with `pre_init: true`

**World Map Player & Marker Hiding**
- Configurable world map visibility: hide enemy/neutral players and shared markers independently via `worldMap` config section
- New `show_on_map` zone flag + `map_visibility` setting — first selection-type zone setting with three levels: "Faction Only", "Faction + Allies", "All Players"
- Zone settings system: `Map<String, String> settings` on Zone record for enum/selection values alongside existing boolean flags
- SharedMarkerFilter integration via HyperProtect-Mixin — filters user-placed shared markers by creator faction
- Admin GUI: Integration Flags page now shows World Map and HyperEssentials sections with map visibility cycling button

**NPC_USE Parent Flag & NPC Role Classification**
- New `NPC_USE` parent flag with `NPC_TAME` and `NPC_INTERACT` children — enables granular NPC interaction control in zone settings
- NPC role classification via `isTameableCreatureRole()` blocklist (fail-open) classifies NPC roles as tameable vs interactive based on role name patterns
- `ALLOWED_SAFEZONE` result type — `ProtectionChecker` now distinguishes "allowed because SafeZone defaults" from "allowed because wilderness"

**Light Use Zone Flag**
- New `LIGHT_USE` zone flag under `BLOCK_INTERACT` parent — controls toggling lanterns, campfires, torches, candles, and lamps
- Detection via both block state ID and block ID fallback (lanterns/campfires have null stateId)
- SafeZone default: false (protected), WarZone default: true (allowed)
- Admin GUI: new flag slot (#Flag24) in Interaction category, all subsequent flags renumbered

**Configurable Wilderness Notifications**
- Wilderness notification text and visibility now configurable per exit context (leaving a zone vs leaving a faction claim)
- Separate `upper` (subtitle) and `lower` (main banner) text for each context
- Each context independently toggleable with `enabled` flag — disable wilderness notifications for zones, claims, or both
- Config in `config/announcements.json` → `territoryNotifications.wilderness.onLeaveZone` and `onLeaveClaim`
- Defaults match previous behavior: lower="Wilderness", upper="" (no subtitle), enabled=true

**Mount Entry Zone Enforcement**
- New runtime enforcement for `MOUNT_ENTRY` flag — mounted players are blocked from entering zones with mount entry disabled
- Safe teleport system: pushes player 2 blocks back from zone boundary with safe ground detection (heightmap scan)
- Zone-aware safe position: if push-back lands in another protected zone, tries cardinal directions (N/E/S/W) before falling back to previous position
- Mounted teleport blocking: faction teleports (/f home, /f stuck, etc.) are denied if destination zone blocks mount entry
- Admin bypass: players with admin bypass enabled skip all mount entry checks
- Player disconnect cleanup for tracking data

**KyuubiSoft Core Integration**
- Reflection-based `CitizenDialogInterceptor` registration for citizen zone protection, with fail-open design and auto-detection
- Admin commands: `/f admin integration kyuubisoft` (aliases: `kyuubi`, `ks`, `citizens`)

**HyperProtect-Mixin v1.2.0 Hook Wrappers**
- 7 new HP-Mixin 1.2.0 hook wrappers: MountHook, BarterTradeHook, FluidSpreadHook, PrefabSpawnHook, ProjectileLaunchHook, CraftingResourceHook, MapMarkerFilterHook
- Block type context for protection decisions — reads `hyperprotect.context.block_id` and `hyperprotect.context.block_state` from HP-Mixin bridge
- NPC role context — reads `hyperprotect.context.npc_role` from HP-Mixin bridge for targeted NPC protection

### Improved

- **Specific protection denial messages**: All protection denial messages now describe the exact action being blocked (e.g., "You can't open containers in enemy territory." instead of generic "You don't have permission to do that.") with territory context (Ally/Enemy/Claimed territory, SafeZone/WarZone). Applied across all protection systems: block break/place/use, harvest/pickup, fluid place/refill, NPC interact, crop harvest, item drop, item pickup, mount
- **Item drop/pickup denial messages**: Item drop and item pickup protection now send debounced, territory-aware denial messages instead of hardcoded text or silent denial
- **Centralized denial message delivery**: New `ProtectionMessageDebounce.sendDenial()` consolidates the repeated `Message.raw(...).color("#FF5555")` + debounce pattern into a single call across all 12 protection systems
- **Eliminated double protection checks**: `HarvestPickupProtectionSystem` and `BlockUseProtectionSystem` (crop path) no longer call `canInteract()` twice per event — once for the boolean and again for the message. Now uses single-call `checkItemPickup()` pattern
- **Consistent denial message phrasing**: All zone denial messages standardized to "You can't ..." (matching territory denial phrasing) instead of mixed "You cannot ..."/"You can't ..."

### Fixed

- **Mount use flag not blocking in SafeZones**: `UseNPCInteraction` with tamed horses/donkeys/camels was classified as `NPC_INTERACT` instead of `MOUNT` — players could mount in SafeZones. Added `isMountableCreatureRole()` check that runs before tameable check, routing rideable creatures to `MOUNT` interaction type
- **Light use flag not blocking**: Lanterns, campfires, and torches have null `stateId` — the zone flag system only checked stateId, falling through to generic `BLOCK_INTERACT`. Both mixin and ECS paths now fall back to `blockId` pattern matching (contains "lantern", "campfire", "torch", etc.) when stateId is null
- **Admin bypass not working for mount flags**: Mount entry enforcement and mount use protection did not check `isAdminBypassEnabled()` — admins were blocked like regular players
- **Chunk map GUI performance**: Reduced terrain map generation time by using bulk pixel writes (row-at-a-time instead of per-pixel `setRGB`) and faster PNG compression. Added timing debug logs for future diagnostics
- **Zero power on faction creation**: New players who joined during server startup could get 0 power due to a race condition where `loadAll()` wiped in-flight defaults. Now merges loaded data instead of clearing the cache, and persists defaults immediately on player join
- **Gravestone enemy loot in own territory**: `enemiesCanLootInOwnTerritory` config option was declared but never checked — enemies could always loot gravestones in the territory owner's claim regardless of the setting. Now properly checks faction relation between the accessor and gravestone owner when in own territory
- **Backup file walk race condition**: `addDirectoryToZip()` crashed with `NoSuchFileException` when a `.bak` file vanished (deleted by concurrent `writeAtomic()`) between directory listing and attribute read during `Files.walkFileTree()` ([HYPERFACTIONS-3](https://hypersystems.sentry.io/issues/HYPERFACTIONS-3))
- **Backup cleanup race condition**: Pre-backup `cleanupOrphanedFiles()` could delete in-flight temp files from concurrent `writeAtomic()` calls, causing `NoSuchFileException` during player data saves ([HYPERFACTIONS-4](https://hypersystems.sentry.io/issues/HYPERFACTIONS-4))
- **Orphan cleanup safety**: `cleanupOrphanedFiles()` now skips `.tmp` files younger than 5 seconds to prevent racing with active writes

### Changed

- Zone flag category consolidation — Entity Interaction category merged into Interaction category (13 flags total: block_interact + 6 children, NPC_USE + 2 children, mount_use, crate_pickup, crate_place)
- Zone flags reindexed in admin UI — all 42 core flags (0-41) with updated category grouping
- Fixed "Combat (6)" comment to "Combat (7)" in `ZoneFlags.ALL_FLAGS`
- Fixed missing `BOTH` case in `AdminIntegrationHandler.handleIntegrations()` MixinProvider switch

## [0.10.2] - 2026-02-28

**Server Version:** `2026.02.19-1a311a592`

### Added

**PvE Damage Permission Flag**
- New per-role faction permission flag: `PveDamage` (4 flags total across outsider/ally/member/officer)
- Faction owners can control who can kill mobs in their territory per relationship level
- Defaults: member/officer = allowed, ally = allowed, outsider = denied
- Configurable in `config/faction-permissions.json` with server lock support
- Territory protection check: players without PvE permission cannot damage mobs in claimed territory
- Faction settings, admin faction settings, and create faction GUIs all include PveDamage toggles
- CrateUse and NpcTame toggles added to Create Faction GUI for parity with Faction Settings (9 → 12 permission rows)

**Protection Message Debounce**
- New centralized debounce system prevents chat spam from repeated protection denial messages
- Per-player, per-action cooldown (2 seconds) — different action types have independent cooldowns
- Integrated across all 9 protection systems: block break/place/use, harvest/pickup, fluid place/refill, NPC interact, PvP damage, crop harvest
- Automatic cleanup of stale entries every 30 seconds

## [0.10.1] - 2026-02-28

**Server Version:** `2026.02.19-1a311a592`

### Added

**PvE Damage Zone Flag**
- New `pve_damage` zone flag: controls whether players can damage mobs/NPCs within a zone
- Added to Combat category in admin zone settings GUI
- Safe zone preset defaults to PvE damage disabled; PvP zone preset defaults to enabled
- Renamed `mob_damage` display name to "Take Mob Damage" for clarity; new flag displays as "Give Mob Damage"

### Fixed

- Mob-on-mob damage (both attacker and target UUIDs null) no longer triggers protection checks in mixin hook

## [0.10.0] - 2026-02-27

**Closes:** [#26](https://github.com/HyperSystems-Development/HyperFactions/issues/26), [#59](https://github.com/HyperSystems-Development/HyperFactions/issues/59), [#61](https://github.com/HyperSystems-Development/HyperFactions/issues/61), [#62](https://github.com/HyperSystems-Development/HyperFactions/issues/62), [#64](https://github.com/HyperSystems-Development/HyperFactions/issues/64), [#65](https://github.com/HyperSystems-Development/HyperFactions/issues/65), [#66](https://github.com/HyperSystems-Development/HyperFactions/issues/66), [#67](https://github.com/HyperSystems-Development/HyperFactions/issues/67), [#68](https://github.com/HyperSystems-Development/HyperFactions/issues/68), [#69](https://github.com/HyperSystems-Development/HyperFactions/issues/69), [#70](https://github.com/HyperSystems-Development/HyperFactions/issues/70), [#71](https://github.com/HyperSystems-Development/HyperFactions/issues/71), [#72](https://github.com/HyperSystems-Development/HyperFactions/issues/72)

**Server Version:** `2026.02.19-1a311a592`

### Added

#### Gameplay

**Per-World Settings** ([#59](https://github.com/HyperSystems-Development/HyperFactions/issues/59))
- New `config/worlds.json` module config for per-world behavior overrides
- Four settings per world: `claiming`, `powerLoss`, `friendlyFireFaction`, `friendlyFireAlly`
- Wildcard pattern support using `%` (e.g., `arena_%` matches `arena_1`, `arena_pvp`)
- Priority resolution: exact name match → wildcard patterns (fewer wildcards = higher priority) → default policy
- `claimBlacklist` for unconditionally blocking claims in specific worlds
- Legacy `worldWhitelist`/`worldBlacklist` in CoreConfig still works as fallback when worlds module is disabled
- Admin commands: `/f admin world list|info|set|reset`

**Faction Leaderboard** ([#67](https://github.com/HyperSystems-Development/HyperFactions/issues/67))
- New `/f leaderboard` command (alias `/f top`) with sortable rankings
- Sort modes: POWER, TERRITORY, BALANCE, MEMBERS, K/D (default)
- 10 entries per page with pagination, numbered rankings
- Integrated into faction nav bar between map and relations
- Aggregated faction K/D ratio from member stats with configurable background cache refresh (`gui.leaderboardKdRefreshSeconds`, default 300s)

**Faction Power Hardcore Mode** ([#69](https://github.com/HyperSystems-Development/HyperFactions/issues/69))
- New `power.hardcoreMode` config option (default: false)
- Shared faction power pool — deaths/kills affect the faction total directly
- No per-death cap or floor — faction power can reach 0
- Power regen applies to the faction pool when enabled

**Persistent Admin Bypass** ([#71](https://github.com/HyperSystems-Development/HyperFactions/issues/71))
- Admin bypass state now persists across server restarts
- Stored in `PlayerData`, restored on connect if player has `hyperfactions.admin.use`
- Automatically cleared if player no longer has the permission

**Configurable Role Display Names**
- New `roles` section in `config/factions.json` for custom role names and abbreviations
- Server owners can rename roles (e.g., "Boss" / "Underboss" / "Soldier") and set short forms (e.g., "BO" / "UB" / "SO")
- New placeholders: `factions_role_display` (custom name), `factions_role_short` (abbreviation)
- All GUIs, commands, and log messages use configured display names
- Existing `factions_role` placeholder unchanged (returns internal name)

**Instance World Protection**
- Default `instance-%` wildcard rule in `config/worlds.json` blocks claiming in temporary instance worlds (power loss defers to global config)
- `/f sethome` now blocked in worlds where claiming is disallowed
- Automatic cleanup of stale claims and homes in disallowed worlds on startup and config reload

#### Protection

**Capture Crate & NPC Tame Protection**
- 2 new per-role faction permission flags: `CrateUse` (covers both crate pickup and placement), `NpcTame` (8 new flags total across outsider/ally/member/officer)
- 3 new zone flags: `crate_pickup`, `crate_place`, `npc_tame` — separate zone-level control for crate pickup, crate placement, and NPC taming
- Faction owners can independently control capture crate use and NPC taming per relationship level
- Defaults: member/officer = allowed, ally/outsider = denied
- Configurable in `config/faction-permissions.json` with server lock support
- NPC interaction protection handler uses `NPC_TAME` interaction type for event-based NPC interaction blocking
- Requires HyperProtect-Mixin v1.0.0+ for mixin-based protection (CaptureCrateGate, NPC entries in SimpleInstantInteractionGate)

**Zone Notification Settings**
- Per-zone notification toggle: suppress entry/leave title notifications for specific zones
- Customizable upper title text (default: zone status like "PvP Disabled")
- Customizable lower title text (default: zone name)
- Admin commands: `/f admin zone notify <zone> <true|false>`, `/f admin zone title <zone> upper|lower <text|clear>`
- Zone info (`/f admin zone info`) now displays notification settings

#### GUI & Admin

**Activity Log** ([#68](https://github.com/HyperSystems-Development/HyperFactions/issues/68))
- Wired existing `LogsViewerPage` into faction nav bar
- `/f logs` command for viewing faction activity
- New `AdminActivityLogPage` aggregating logs from all factions
- Type filter, player filter, time filter (1h/24h/7d)
- `/f admin log` command and admin nav bar integration

**Admin K/D Management**
- Per-player K/D reset button on admin player info page (two-column redesign)
- New "Actions" page in admin nav with global K/D reset (two-step confirmation)
- Admin commands: `/f admin info [faction]` and `/f admin who [player]` to open admin GUIs directly

**Admin Info Page Redesign**
- Admin player info now uses two-column layout: stats/info/history on left, controls on right
- Admin player info now mirrors `/f who` data: first joined, last online, faction card with View Faction button, and scrollable membership history
- Admin faction info now uses two-column layout: leadership on left, power/economy/disband controls on right
- Disband button added to admin faction info page (danger zone section)

**Admin Version Page**
- New `/f admin version` command — opens GUI for players, prints integration status for console
- Admin Version page in nav bar (after Help) showing HyperFactions version, server version, Java version
- Integration status display: HyperPerms, LuckPerms, VaultUnlocked, HytaleNative, HyperProtect, OrbisGuard Mixins, OrbisGuard API, Mixin Hooks, Gravestones, PlaceholderAPI, WiFlow PAPI, Treasury
- Treasury status shows economy provider name via VaultUnlocked (e.g. "Active (Ecotale)")
- Color-coded status: green (active), yellow (installed but limited), gray (not found)

**Admin Zone Properties Page**
- New consolidated zone properties page combining name editing, type changing, and notification settings
- Zone list entry replaces separate "Rename" and "Type" buttons with single "Settings" button
- Type change modal navigates back to properties page (instead of zone list) when accessed from properties
- Flags page back button returns to properties when opened from properties context

#### Config & Storage

**Config Restructure — Split config.json into Module Configs**
- New `config/factions.json` for all faction gameplay settings (faction, power, claims, combat, relations, invites, stuck)
- New `config/server.json` for server behavior settings (teleport, autoSave, messages, gui, permissions, updates, configVersion)
- 9 new claim protection server overrides in `config/factions.json` under `claims`:
  - `outsiderPickupAllowed` — control outsider item pickup in claims (previously hardcoded deny)
  - `outsiderDropAllowed` — control outsider item drop in claims
  - `factionlessExplosionsAllowed`, `enemyExplosionsAllowed`, `neutralExplosionsAllowed` — granular explosion control replacing `allowExplosionsInClaims`
  - `fireSpreadAllowed` — control fire spread in claims (previously hardcoded block)
  - `factionlessDamageAllowed`, `enemyDamageAllowed`, `neutralDamageAllowed` — outsider entity damage control
- V5→V6 config migration: automatically splits `config.json`, transforms `allowExplosionsInClaims` into 3 granular flags, preserves all existing values
- Legacy fallback: plugin loads from old `config.json` if migration fails or hasn't run
- Treasury permission flags (`treasuryDeposit`, `treasuryWithdraw`, `treasuryTransfer`) added to `faction-permissions.json` for server-level defaults and locks
- `territoryNotifications.enabled` moved to `config/announcements.json`
- Allow Explosions in Claims ([#65](https://github.com/HyperSystems-Development/HyperFactions/issues/65)) — original `territory.allowExplosionsInClaims` toggle, superseded by the 3 granular explosion flags above

**Data Directory Restructure**
- All data files now live under `data/` subdirectory (factions, players, chat, economy, zones, invites, join requests)
- `DataV0ToV1Migration` automatically moves existing data files into `data/` on first startup
- `data/.version` marker tracks data layout version independently from config version
- Fresh installs create `data/` directory directly (no migration needed)

**Atomic Writes & Backup Expansion**
- All storage types now use `StorageUtils.writeAtomic()` for crash-safe writes (economy, invites, join requests upgraded from direct writes)
- `.bak` files cleaned up after successful atomic writes — no more indefinite accumulation
- Backup system now includes chat history, economy, invites, and join requests in ZIP archives

#### Placeholders

**Expanded Placeholders** ([#72](https://github.com/HyperSystems-Development/HyperFactions/issues/72))
- Colored variants: `name_colored`, `tag_colored`, `name_colored_legacy`, `tag_colored_legacy`
- Legacy color code: `color_legacy` (nearest `&X` code from hex color)
- Relational placeholders (PAPI only): `rel_factions_relation`, `rel_factions_relation_color`

**Treasury Placeholders** ([#66](https://github.com/HyperSystems-Development/HyperFactions/issues/66))
- `treasury_balance` — formatted via `EconomyManager.formatCurrency()`
- `treasury_balance_raw` — raw BigDecimal with scale 2
- `treasury_autopay` — true/false
- `treasury_limit` — max treasury limit

**Relational Placeholders** ([#26](https://github.com/HyperSystems-Development/HyperFactions/issues/26))
- `relation_color` — now returns hex color (`#RRGGBB`) via `LegacyColorParser.codeToHex()` (was returning `&X` legacy format)
- `relation_color_legacy` — new, returns `&X` legacy color code for the relation
- `relation_colored` — new, returns `§XName` (color code + display name) for direct chat use

### Changed

**Treasury BigDecimal Refactor**
- All treasury/economy values converted from `double` to `BigDecimal` for currency precision
- ~21 files updated: data models, manager, storage, config, VaultUnlocked provider, commands, GUI pages
- Uses `BigDecimal.ZERO`, `RoundingMode.HALF_UP` with scale 2, string constructor for initialization
- JSON backward compatible: `getAsBigDecimal()` handles both old double and new string formats

**Protection Hardening**
- ECS protection systems hardened to fail-closed — all 7 ECS protection systems now cancel events when world name cannot be determined or an exception occurs (previously silently allowed the action)
- Anti-pillar exploit fix — denied block placements now teleport the player to their current position with velocity reset, preventing ghost-block pillar climbing on client-predicted blocks
- Optimized protection evaluation — BlockBreak and BlockPlace systems now evaluate `canInteract()` once and reuse the result for both the check and the denial message (was called twice)
- NPC interaction handler registered — `NpcInteractionProtectionHandler` now registered in `EventRegistration` for F-key tame and contextual NPC use events

**Claim Protection Config**
- `allowExplosionsInClaims` replaced by 3 granular explosion flags (factionless/enemy/neutral)
- Explosion check in claims now uses combined 3-way logic (all must be false to block — mixin hooks lack player context)
- Fire spread in claims now configurable via `fireSpreadAllowed` (was hardcoded block)
- Outsider item pickup in claims now configurable via `outsiderPickupAllowed` (was hardcoded deny)
- Outsider item drops in claims now checked against `outsiderDropAllowed` config (was zone-only)
- Outsider entity damage in claims now checked against 3-way damage config by relation (factionless/enemy/neutral)

**Code Cleanup & Refactoring**
- Extracted `CommandUtil.parseRawArgs()` — 36 command files now use shared arg parsing helper instead of duplicated 3-line pattern
- Extracted `UIPaths.java` — 110 centralized UI template path constants replacing 150 hardcoded `"HyperFactions/..."` strings across 70 files
- Extracted `NavBarUtil` + `NavEntry` interface — shared navigation bar button building logic used by all 3 nav bar helpers
- Split `GuiManager` (2834 → 1089 lines) — 81 page opener methods extracted into `FactionPageOpener` (35), `AdminPageOpener` (38), `NewPlayerPageOpener` (8); public API unchanged via delegation
- All bare `@Deprecated` annotations now include `forRemoval = true` with version info
- Codebase-wide style enforcement: 2-space indentation, Google Java Style import ordering, Javadoc on all public APIs, consistent brace placement, operator wrapping, and blank line separators across all 399 source files
- Added Checkstyle 10.26.1 with customized Google Java Style config (`config/checkstyle/checkstyle.xml`) — warnings-only mode, 120-char line limit, star imports allowed, relaxed Javadoc scope
- Extracted `GuiColors.java` — centralized semantic color constants and resolution methods (`forRole()`, `forLeaveReason()`, `forLogType()`, `forPowerLevel()`, `forOnlineStatus()`, `forRank()`, relation colors). Replaced duplicated `getRoleColor()`/`getReasonColor()`/`getLogTypeColor()` methods across 8 GUI page files

**Other Changes**
- Debug logging standardized — all ECS systems use `[ECS:*]` prefix, all OrbisGuard hooks use `[OG:*]` prefix via `Logger.debugInteraction()`
- `CoreConfig` deprecated — replaced by `FactionsConfig` and `ServerConfig`
- `pvpEnabled()` and `officersCanEdit()` accessors in `FactionPermissions` now use `get()` instead of `getRaw()` for consistent parent-child flag resolution
- Admin zone flags page restructured from 2-column to 3-column layout for better visual balance across 36 flags (Combat/Damage/Death | Building/Interaction/Entity Interaction | Transport/Items/Spawning)

### Removed

- 22 unused backward-compat accessor/builder methods in `FactionPermissions` (dead code with zero callers)

### Fixed

- **HyperProtect version display**: `HyperProtectIntegration` hardcoded the version as "1.0.0" when lazily creating the bridge (Hyxin never calls early plugin `setup()`); now detects the actual version from the JAR filename in `earlyplugins/`
- **HyperProtect-Mixin update checker showing v0.0.0**: `initHyperProtectMixinLifecycle()` read the system property before `HyperProtectIntegration.registerAllHooks()` set it; now detects version from JAR filename on disk before branching, and sets the system property early for AdminVersionPage
- **Old GitHub URL not migrated**: Default update checker URL in `ServerConfig` and `CoreConfig` still referenced the old `ZenithDevHQ` organization; updated defaults to `HyperSystems-Development` and added migration that auto-corrects the old URL on load (custom URLs preserved)
- **Config files not saving new keys**: Modules like `FactionPermissionsConfig` that bypass base `getInt`/`getBool` helpers didn't trigger `needsSave` when new flags were added in code updates; `ConfigFile.load()` now recursively compares serialized keys against the on-disk JSON and auto-saves when new keys are detected
- **Admin player info color inconsistencies**: Officer role color was `#87CEEB` (light blue) instead of `#00AAFF` (standard blue); offline player status was `#FF5555` (red) instead of `#888888` (gray). Both now use centralized `GuiColors` constants
- **Kill/death tracking race conditions**: Concurrent deaths could lose increments due to unsynchronized load-modify-save cycles; power regen could overwrite kill/death data. Added per-UUID locking to `JsonPlayerStorage` and atomic `updatePlayerData` method
- **Logs viewer page crash**: `#Title.Text` selector targeted a Group instead of a Label
- **Dashboard "View All" button**: Now navigates to activity logs page (was disabled)
- **Atomic write failures on Windows**: `ATOMIC_MOVE` can fail when antivirus or file indexer holds a handle on the target file; `StorageUtils.writeAtomic()` now retries with backoff (3 attempts, 50/100/150ms) then falls back to non-atomic move (safe — `.bak` backup already exists)
- **Backup orphaned files** ([#61](https://github.com/HyperSystems-Development/HyperFactions/issues/61)): `BackupManager` now skips `.tmp`/`.bak` orphans and cleans up before backup. Incomplete backup ZIPs are deleted on failure
- **Warzone power loss** ([#62](https://github.com/HyperSystems-Development/HyperFactions/issues/62)): Zone check failure now defaults to **no power loss** (fail-safe) instead of silently falling through. Logged at WARN instead of DEBUG
- **Chunk map buttons ignore permissions** ([#64](https://github.com/HyperSystems-Development/HyperFactions/issues/64)): Claim/unclaim/overclaim buttons on chunk map now require the corresponding `hyperfactions.territory.*` permission in addition to officer role; previously only checked `isOfficer`
- **Warzone friendly fire default blocks ally PvP** ([#70](https://github.com/HyperSystems-Development/HyperFactions/issues/70)): Changed `FRIENDLY_FIRE` warzone default from `false` to `true` so PvP-enabled warzones allow full combat by default (including between faction members and allies)

## [0.9.0] - 2026-02-22

**Server Version:** `2026.02.19-1a311a592`

**Closes:** [#10](https://github.com/HyperSystems-Development/HyperFactions/issues/10), [#13](https://github.com/HyperSystems-Development/HyperFactions/issues/13), [#14](https://github.com/HyperSystems-Development/HyperFactions/issues/14), [#46](https://github.com/HyperSystems-Development/HyperFactions/issues/46), [#50](https://github.com/HyperSystems-Development/HyperFactions/issues/50), [#51](https://github.com/HyperSystems-Development/HyperFactions/issues/51), [#52](https://github.com/HyperSystems-Development/HyperFactions/issues/52), [#53](https://github.com/HyperSystems-Development/HyperFactions/issues/53), [#55](https://github.com/HyperSystems-Development/HyperFactions/issues/55), [#56](https://github.com/HyperSystems-Development/HyperFactions/issues/56), [#57](https://github.com/HyperSystems-Development/HyperFactions/issues/57)

### Added

**Granular Friendly Fire Flags**
- **`friendly_fire_faction`**: Controls whether same-faction members can damage each other (child of `friendly_fire`)
- **`friendly_fire_ally`**: Controls whether allied faction members can damage each other (child of `friendly_fire`)
- **3-level combat hierarchy**: `pvp_enabled` → `friendly_fire` → `friendly_fire_faction` / `friendly_fire_ally`. Projectile Damage and Mob Damage remain standalone
- **Recursive parent check**: Admin zone settings GUI greys out grandchildren when any ancestor is OFF (e.g., faction/ally flags disabled when PvP is off)
- **Config fallback preserved**: When zone `friendly_fire` is OFF, `config.factionDamage` and `config.allyDamage` still serve as independent overrides
- **Block Interact as Interaction parent**: Moved Block Interact from the Building box into the Interaction box as the parent flag with indented children (Door Use, Container Use, etc.), matching the Mob Spawning pattern
- **Backward compatible**: Existing zones with `friendly_fire=true` get both children defaulting to `true` (same behavior as before). No data migration needed

**HyperProtect-Mixin Integration**
- **ProtectionMixinBridge**: Facade that auto-detects which mixin system(s) are available (HyperProtect-Mixin, OrbisGuard-Mixins, or both) and provides a unified API. New `BOTH` provider mode registers OG hooks for OG-handled features and HP hooks for HP’s unique features. Falls back to JAR file detection in `earlyplugins/` when system properties aren’t set
- **HyperProtectIntegration**: 20 hook wrappers using HyperProtect’s bridge-slot architecture (`AtomicReferenceArray` at `hyperprotect.bridge`), verdict protocol (ALLOW/DENY_WITH_MESSAGE/DENY_SILENT/DENY_MOD_HANDLES). Registers all hooks unconditionally with lazy bridge initialization (creates bridge if not yet present). Caches `ChatFormatter` MethodHandle at slot 15 for cross-classloader message formatting
- **Auto-download HyperProtect-Mixin**: On startup, if HyperProtect-Mixin is not installed and `autoDownload` is enabled, downloads the latest release to `earlyplugins/` and prompts for server restart. Disabled by default — server logs instructions for manual install or how to enable. Gracefully handles repositories with no published releases (logs info, no crash/error)
- **Auto-update checking for HyperProtect-Mixin**: When HyperProtect-Mixin is installed, checks for newer versions on startup via GitHub Releases API and notifies admins with version comparison (e.g., "v1.0.0 -> v1.1.0"). Configurable via `updates.hyperProtect.autoUpdate` (default: `true`)
- **`/f admin update mixin`**: Manual command to check for and download HyperProtect-Mixin updates (or initial install). Works regardless of auto-download config — creates an on-the-fly UpdateChecker if needed
- **`/f admin update toggle-mixin-download`**: Runtime command to toggle HP-Mixin auto-download on/off, persisted to config
- **Config: `updates.hyperProtect` section**: New nested config section with `autoDownload` (bool), `autoUpdate` (bool), and `url` (GitHub Releases API endpoint) settings for managing HyperProtect-Mixin lifecycle
- **7 new zone flags**: `explosion_damage`, `fire_spread`, `block_place`, `hammer_use`, `builder_tools_use`, `teleporter_use`, `portal_use` — all mixin-dependent, with SafeZone (false) and WarZone (true) defaults
- **New “Transport” zone flag category**: Groups `teleporter_use` and `portal_use` flags
- **Unified "Transport" faction permission**: Merged `teleporterUse` and `portalUse` into a single `transportUse` permission per role level (outsider/ally/member/officer). Both teleporter and portal interactions now check the same flag
- **~15 new ProtectionChecker methods**: `shouldBlockExplosion`, `shouldBlockFireSpread`, `checkBuilderTool`, `checkTeleporter`, `checkPortal`, `shouldKeepInventory`, `shouldPreventDurability`, `canAccessContainer`, and more
- **Admin zone settings GUI**: Updated for 31 total flags (was 24), new flag categories, mixin provider indicator
- **Configurable power loss by death cause**: New `powerLossOnMobDeath` and `powerLossOnEnvironmentalDeath` config options in the `power` section (both default `true` for backward compatibility). Set either to `false` to skip the death power penalty for that cause type while still incrementing kill/death counters and announcing gravestone locations. PvP deaths always incur power loss. Zone flag `power_loss=false` takes priority over these settings
- **Death cause tracking**: `DamageProtectionHandler` now records the type of each damage event (PVP, MOB, ENVIRONMENTAL) that passes through protection checks into `CombatTagManager`, which `PlayerDeathSystem` reads at death time to apply the correct power loss policy
- **JitPack publishing**: Added `maven-publish` plugin and `jitpack.yml` — other developers can now depend on HyperFactions via `com.github.HyperSystems-Development:HyperFactions:<version>` from JitPack
- **Standalone build support**: Build resolves Hytale server version independently when built outside the monorepo (JitPack, CI)
- **Contributor docs**: Updated CONTRIBUTING.md and README.md with soft dependency download instructions and CurseForge links
- **Developer docs**: Updated README and API reference with JitPack dependency instructions, marked WiFlow/Gravestone libs as optional

**New Player Experience GUI**
- **Create faction page redesign**: Two-column form combining name, tag, color picker, description, recruitment setting, and full territory permissions with live preview into a single screen. Permission toggles support parent-child hierarchy with server-locked flags greyed out
- **New player map terrain mode**: New player territory map now respects `terrain-map-enabled` config, showing terrain imagery with semi-transparent claim overlays matching the faction map style. All claims shown as "Other" since new players have no faction
- **New player map OG rendering**: OrbisGuard-protected regions shown on new player territory map in both flat and terrain modes with dynamic legend entry

**Admin Tools**
- **Admin zone map terrain mode**: `/f admin zone map` now respects `terrain-map-enabled` config, showing terrain imagery behind semi-transparent zone overlays (75% opacity for current zone, 50% for other zones, 63% for faction claims). Uses `ChunkMapAsset` async pipeline matching the faction territory map pattern
- **Admin zone map OG rendering**: OrbisGuard-protected regions shown on admin zone map in both flat and terrain modes with dynamic legend entry

**OrbisGuard Compatibility**
- **Hook chaining**: All 11 OrbisGuard-Mixins hooks now chain with existing OG hooks instead of overwriting them. When OG registers hooks before HF, the originals are captured and called first — if OG denies the action (region protection), HF respects it; if OG allows, HF applies faction-based checks on top. Hooks are restored to originals on unregister
- **OrbisGuard API integration**: New `OrbisGuardIntegration` class provides soft-dependency access to OG's region container via reflection and cached MethodHandles. Supports `getRegionsAt()` for point queries, `canCreateClaim()` for chunk-level AABB checks, and `getLoadedManagers()` for region enumeration
- **Claim blocking in OG regions**: Claims are denied in any chunk that overlaps an OrbisGuard-protected region. Uses `canCreateClaim` API when available (checks corners, center, and full overlap), falls back to 5-point sampling (4 corners + center). Global (`__global__`) regions are excluded
- **World map OG region rendering**: OrbisGuard regions are rendered on the world map (M key) as colored overlays using OG's 8-color palette with priority-based stacking, border detection, and opacity blending. Region candidates are pre-filtered per chunk via AABB overlap to minimize per-pixel work
- **Territory Map GUI OG rendering**: Chunks overlapping OrbisGuard regions are shown as dark orange (`#FF8C00`) "Protected" cells in the `/f map` chunk grid. Click events are suppressed on protected chunks. Works in both flat and terrain rendering modes
- **Dynamic legend entry**: When OrbisGuard is detected, a "Protected" legend entry with orange swatch is dynamically appended to the Territory Map GUI legend
- **`RegionInfo` record**: Exposes OG region spatial data (id, world, type, priority, AABB bounds) with chunk coordinate helpers for map rendering

### Changed

- **Faction settings UI redesign**: Rotated the permission grid — permissions are now rows with role columns (Out/Ally/Mem/Off) instead of role rows with permission columns. Fixes "Processing" truncation, eliminates crowding, and makes adding future permissions trivial (just add rows). Building and Interaction permissions unified into a single "Territory Permissions" section with parent-child indentation
- **Faction permission flags**: Reduced from 49 to 45 flags by merging 8 teleporter/portal flags (4 each) into 4 transport flags. `ALL_FLAGS` updated accordingly
- **Data migration**: `JsonFactionStorage` automatically migrates old `teleporterUse`/`portalUse` flags to `transportUse` on load using OR logic (permissive — allows if either was allowed)
- **Plugin initialization**: Refactored `initializeOrbisIntegrations()` → `initializeProtectionMixins()` using ProtectionMixinBridge for mixin system detection and hook registration
- **Conditional HarvestCrop codec**: `HyperFactionsHarvestCropInteraction` codec only registered when no mixin system is active (mixin handles harvest interception natively)
- **Zone flags**: `ALL_FLAGS` expanded to 31, `MIXIN_DEPENDENT_FLAGS` expanded to 11, new parent-child relationships (`BUILD_ALLOWED` → `BLOCK_PLACE`, `HAMMER_USE`, `BUILDER_TOOLS_USE`)
- **Admin integration handler**: Now uses ProtectionMixinBridge to show correct mixin provider status with version and hook details
- **UpdateChecker**: Parameterized to support multiple artifacts (HyperFactions and HyperProtect-Mixin) with configurable target directories
- **manifest.json**: Added `HyperProtect-Mixin` to SoftDependencies
- **Maven build migration**: Hytale Server API now resolved from `maven.hytale.com` instead of local JAR files. Use `-Phytale_channel=pre-release` to build against the pre-release server
- **Local soft dependencies**: Moved WiFlowPlaceholderAPI and GravestonePlugin references to local `libs/` directory with `fileTree` glob — version bumps only need a symlink update, no `build.gradle` edits
- **GravestoneIntegration**: Refactored from direct API imports to pure reflection — compiles and runs without GravestonePlugin JAR present
- **WiFlowPlaceholderIntegration**: Refactored bridge to pure reflection — compiles without WiFlowPlaceholderAPI JAR. WiFlowExpansion conditionally excluded from compilation when WiFlow is absent
- **HyperPerms dependency**: Conditional resolution — uses project reference in monorepo, JitPack coordinate when building standalone
- **Logger**: Migrated from `java.util.logging.Logger` to Hytale's `HytaleLogger` (Google Flogger) for proper log routing. All log calls updated to fluent `.at(Level).log()` API
- **CallbackWiring**: Removed legacy OrbisGuard-specific wiring methods (`wireExplosionProtection`, `wireHarvestProtection`, `wireHammerProtection`) — all protection hooks now registered through `ProtectionMixinBridge`
- **BackupManager**: Simplified backup directory handling and error reporting
- **StorageUtils**: Streamlined file I/O operations
- **HP-Mixin auto-download**: Default changed to `false` (opt-in). Detects on-disk JAR that failed to initialize and checks for updates against the existing version. When no JAR exists and auto-download is off, logs install instructions instead of silently skipping
- **Faction members list**: Member list container now uses `TopScrolling` layout with scrollbar for overflow handling

### Fixed

- **Permission toggle children not enabling**: Fixed parent-child checkbox state in faction settings and create faction page — toggling a parent permission (e.g., Interact) ON now immediately enables its children (Door, Chest, etc.) without requiring a page close/reopen. Root cause: `Disabled` state was only ever set to `true` but never explicitly reset to `false` on update, and toggling sent an empty update instead of rebuilding the toggle tree
- **Claim blocking in OG regions**: Fixed `isChunkProtected()` using 16-block chunk size (`<< 4`) instead of Hytale's 32-block chunks (`<< 5`), causing protection checks to examine the wrong area and fail to block claims
- **New player map terrain crash**: Fixed crash when opening new player map in terrain mode — `#FactionLegend.Visible` selector targeted an element only present in the flat template

### Migration Guide

**Recommended: Switch to HyperProtect-Mixin**

HyperFactions v0.9.0 introduces [HyperProtect-Mixin](https://www.curseforge.com/hytale/bootstrap/hyperprotect-mixin) as the **recommended** mixin system, replacing OrbisGuard-Mixins. HyperProtect-Mixin provides 20 protection hooks designed specifically for HyperFactions, including explosion damage, fire spread, block placement, builder tools, transport, keep inventory, and durability protection.

**Installation:**
1. Download [Hyxin](https://www.curseforge.com/hytale/bootstrap/hyxin) and [HyperProtect-Mixin](https://www.curseforge.com/hytale/bootstrap/hyperprotect-mixin) from CurseForge
2. Place both JARs in your server's `earlyplugins/` folder (NOT `mods/`)
3. Ensure `--accept-early-plugins` is in your server start script
4. Remove OrbisGuard-Mixins from `earlyplugins/` (optional but recommended)
5. Restart the server

**Alternatively**, enable auto-download in config or use `/f admin update mixin` to install HyperProtect-Mixin from within the game.

**OrbisGuard Compatibility Note:**
We recommend **not** using OrbisGuard alongside HyperFactions, as both mods implement their own protection systems that can conflict with each other. If you need OrbisGuard for non-faction worlds, HyperFactions supports a dual-provider mode — keep both OrbisGuard **and** OrbisGuard-Mixins installed, and HyperFactions will route hooks appropriately. However, for the simplest and most reliable setup, use **only** HyperProtect-Mixin.

## [0.8.1] - 2026-02-17

**Server Version:** `2026.02.17-255364b8e`

### Fixed

- **Map GUI crash**: Fixed `NoSuchMethodError` on `PacketHandler.write(Packet[])` — changed to `ToClientPacket[]` for server compatibility
- **World map generator**: Fixed generator not being set on live worlds added after startup
- **Server version warning**: Manifest now specifies target server version (prevents PluginManager "does not specify a target server version" warning)

### Added

- **Centralized player resolution** (`PlayerResolver`): Unified player name lookup used across all commands and GUIs — resolves online players, then faction member records, then PlayerDB API as fallback
- **Offline player support**: Treasury transfer search, `/f who`, `/f power`, and admin commands now resolve offline players via faction member records and PlayerDB
- **Search debounce**: Treasury transfer search waits 500ms after last keystroke before searching to avoid spamming PlayerDB API
- **PlayerDB integration**: New `PlayerDBService` utility for looking up any Hytale player by username (5-minute TTL cache, min 3-char query for API calls)
- **Debug logging**: Added diagnostic logging to player connection, online player search, world map setup, and world map generation

### Changed

- **Target-aware build**: Compile against release or prerelease server JAR via `-PhytaleTarget` Gradle flag

## [0.8.0] - 2026-02-16

**Closes:** [#30](https://github.com/HyperSystems-Development/HyperFactions/issues/30), [#31](https://github.com/HyperSystems-Development/HyperFactions/issues/31), [#32](https://github.com/HyperSystems-Development/HyperFactions/issues/32), [#34](https://github.com/HyperSystems-Development/HyperFactions/issues/34), [#36](https://github.com/HyperSystems-Development/HyperFactions/issues/36)

### Added

**Help Center Redesign**
- **Guide-first help GUI**: Complete rewrite of the help system from command-dump format to conceptual guides with commands woven in contextually
- **Wide sidebar layout** (750x650): Colored category sidebar with 7 topic categories, scrollable card-based content area replacing the old flat list
- **7 help categories**: Welcome (cyan), Your Faction (green), Power & Land (gold), Diplomacy (blue), Combat & Safety (red), Economy (orange), Quick Reference (gray) — each with unique accent color on sidebar
- **22 guide-first topics**: Organized into conceptual guides that teach game mechanics with commands referenced in context, rather than raw command lists
- **Typed content entries**: New `HelpEntry` record with 5 entry types (TEXT, COMMAND, TIP, HEADING, SPACER) replacing fragile string-based line type detection
- **i18n foundation** (`HelpMessages.java`): All help content uses message key lookups (`HelpMessages.get("help.welcome.title")`) — hardcoded English now, locale file loading in the future
- **Deep-linking support**: `/f <command> help` opens directly to the relevant category (e.g., `/f claim help` opens Power & Land)
- **Card-based topic rendering**: Dark background cards with teal titles, body text, yellow command callouts, green tips, and teal sub-headings
- **All 43 commands verified**: Quick Reference section verified against actual command implementations with correct syntax and permission requirements
- Deleted 8 obsolete UI templates, added 5 new templates (topic card, line text, line tip, line heading, spacer)

**Faction Treasury & Economy System** ([#36](https://github.com/HyperSystems-Development/HyperFactions/issues/36))
- **Treasury page** (`/f treasury`): Full GUI with balance display, deposit/withdraw buttons, transfer system, transaction log, and treasury settings — accessible from the faction nav bar
- **Deposit/Withdraw modals**: Amount input with live preview, fee calculation display, and balance validation via VaultUnlocked player wallets
- **Transfer system**: Search factions by name, confirm transfer with fee preview, complete inter-faction treasury transfers
- **Treasury settings page**: Officers can configure deposit/withdraw limits, toggle auto-pay upkeep, and view fee rates
- **Transaction log**: Scrollable history of all treasury activity (deposits, withdrawals, transfers, admin adjustments) with timestamps and actor names
- **Economy CLI commands**: `/f balance`, `/f deposit <amount>`, `/f withdraw <amount>`, `/f money` for quick treasury access without GUI
- **Admin economy page** (`/f admin economy`): Server economy overview with total balance, faction count, average balance stats, and sortable/searchable faction treasury list with per-row Adjust and Info buttons
- **Admin economy adjust modal**: Set exact balance or add/deduct amounts with live preview, faction info card, and error display
- **Admin economy CLI** (`/f admin economy <balance|set|add|take|total|reset> ...`): Full console-compatible treasury management
- **Economy stats on admin dashboard**: Total economy, wealthiest faction, and average balance cards (conditionally visible when economy enabled)
- **Economy stats on faction dashboard**: Treasury balance, next upkeep, and personal wallet cards (conditionally visible)
- **Treasury info on admin faction info page**: Balance display in stats grid, "Adjust Balance" and "View Treasury" quick action buttons in Economy Management section
- **Treasury balance on faction info page**: Gold-colored treasury card visible to all players when economy is enabled
- **Economy configuration** (`economy.json`): Starting balance, treasury limits (deposit/withdraw min/max/daily), transfer fees, upkeep settings, fee rates
- **VaultUnlocked integration**: Player wallet operations (balance check, deposit, withdraw) via VaultUnlocked economy API
- **Faction permissions for economy**: `DEPOSIT`, `WITHDRAW`, `TRANSFER`, `MANAGE_TREASURY` permission flags per role
- `hyperfactions.admin.economy` permission node for admin economy operations
- `ECONOMY` debug category with `Logger.debugEconomy()` — togglable via `/f admin debug toggle economy` and `debug.json` config
- `UiUtil` shared utility class for CustomUI text sanitization and amount parsing

**Player Info & Membership History** ([#32](https://github.com/HyperSystems-Development/HyperFactions/issues/32))
- **Player info page** (`/f who [player]`): Full GUI page with faction membership, power stats, combat stats (kills/deaths/KDR), and membership history
- **Membership history tracking**: Records all faction joins, leaves, kicks, and disbands with timestamps, highest role achieved, and leave reason
- **Player data expansion**: New `PlayerData` model with `firstJoined`, `lastOnline`, kills, deaths, username caching, and membership history — backwards-compatible with existing player JSON files
- "View Profile" button on members page to navigate to player info
- `/f admin clearhistory <player>` command to clear a player's membership history
- **Upgrade migration**: Existing faction members automatically get an active membership record on first startup, using their actual faction join date and current role
- First joined / last online tracking displayed on the player info page header
- `maxMembershipHistory` config option (default: 10) to cap history records per player

**Admin Power Management** ([#34](https://github.com/HyperSystems-Development/HyperFactions/issues/34))
- **Admin power command** (`/f admin power`): Full admin control over player power — set, adjust, reset, per-player max override, power loss bypass, claim decay exemption
- **Admin Player Info page**: GUI page accessible from admin member list with power controls, bypass toggles, and kick/promote/demote actions
- **Faction bulk power operations**: Reset All / +/-1/5 All buttons on AdminFactionInfoPage
- `ADMIN_POWER` log type for faction activity logs
- `hyperfactions.admin.power` permission node

**Terrain Map Mode** ([#30](https://github.com/HyperSystems-Development/HyperFactions/issues/30))
- **Terrain map mode for territory GUI**: Territory map now renders actual terrain imagery behind claim overlays using `ChunkWorldMap`, making it much easier to orient yourself and identify terrain features when claiming (rivers, biomes, elevation)
- Dynamic terrain image asset delivery via `ChunkMapAsset` — generates a composite PNG from chunk world map data and sends it to the player's client at runtime
- Static placeholder `Map.png` sent immediately on page open, with terrain loading asynchronously in the background
- Semi-transparent claim color overlays on top of terrain: own (green), ally (blue), enemy (red), other (gold), safe zone (teal), war zone (purple), wilderness (transparent)
- Clickable cells for claim/unclaim actions in terrain mode (officers only)
- Centered "+" player marker via dedicated `chunk_map_marker.ui` template
- Compact 2-row legend showing all territory types and player marker
- `terrainMapEnabled` config toggle in GUI section (default: `true`) — set to `false` to revert to the flat colored grid

**Admin GUI Improvements**
- **Admin players page** (`/f admin players`): New admin page for browsing all known server players with search, sort (Name/Faction/Power/Last Online), expandable entries with power stats, faction membership, and quick actions (View Profile, View Faction, Teleport)
- **Admin search on all list pages**: Added search bars to admin factions, admin faction members, and admin zones pages matching the faction browser search pattern
- **Admin zone sort dropdown**: Zones can now be sorted by Name, Type, Chunks, or World (previously name-only)
- **Admin faction members overhaul**: Upgraded from read-only list to full admin tool with search, sort dropdown (Role/Online/Name/Power), pagination (8 per page), and 700x500 container matching the admin factions page layout
- `getAllPlayerUuids()` storage method for admin player list enumeration

**Relations Redesign**
- **Relations page redesign**: Merged three tabs (Allies/Enemies/Pending) into two tabs (Relations/Pending) with collapsible row entries showing faction name, leader, relation badge, member count, power stats, and inline quick actions (View, Set Neutral, Set Enemy, Request Ally, Accept, Decline, Cancel)

**Faction-Aware Map & Compass Visibility** ([#31](https://github.com/HyperSystems-Development/HyperFactions/issues/31))
- **Player visibility filtering on world map and compass**: Only faction members and allies are visible by default — enemies, neutrals, and factionless players are hidden from the map and compass
- Configurable per-relation visibility: `showOwnFaction`, `showAllies`, `showNeutrals`, `showEnemies`, `showFactionlessPlayers`, `showFactionlessToFactionless` in `worldmap.json` under `playerVisibility`
- Master toggle (`playerVisibility.enabled`) to disable filtering entirely (vanilla behavior: all players visible)
- `hyperfactions.bypass.mapfilter` permission — player can see all others on map regardless of faction filter
- `hyperfactions.bypass.mapvisibility` permission — player is always visible to everyone on the map
- Filters update automatically on faction join/leave/kick/disband and relation changes

### Fixed

- **Zone power loss flag ignored on death** — `PlayerDeathSystem` passed already-converted chunk coordinates to `getZoneAt()` (which expects world coordinates), causing a double conversion that looked up the wrong chunk. Zone flags like `power_loss=false` were never applied. Changed to `getZone()` which accepts chunk coordinates directly.
- **Zone flags reset on server restart**: WarZone migration code ran on every startup and unconditionally stripped the `BUILD_ALLOWED` flag, reverting any admin customizations to defaults. Migration now only removes genuinely obsolete keys (`container_access`, `interact_allowed`) and applies to all zone types.
- **Terrain map lag on claim/unclaim**: Each claim/unclaim action opened a new page instance, regenerating the entire 17x17 terrain image from scratch. Now reuses the same page via `rebuild()`, skipping terrain generation and only updating the overlay grid
- **Membership history not recorded for faction creation**: `FactionManager.createFaction()` now publishes a JOIN event for the faction creator
- **Admin kicks not recorded in membership history**: `FactionManager.adminRemoveMember()` now publishes a KICK event
- **Player info back navigation**: Back button on player info page now returns to the source page (member list, browser) instead of closing the GUI entirely
- **Faction info back from player profile**: Viewing a faction from a player's profile page and pressing back now returns to the profile instead of closing the GUI
- **Invite page active tab not disabled**: Selected tab on the invites page is now visually disabled to indicate current selection
- **Set Relation "View" prints to chat**: View button in the set relation modal now opens the faction info page instead of dumping text to chat
- **Set Relation ally request goes to wrong tab**: After requesting an ally, the relations page now opens on the Pending tab instead of Relations
- **Pending cancel goes to wrong tab**: Canceling a pending request now stays on the Pending tab instead of switching to Relations
- **Members page search row position**: Search bar moved above the header/sort row for consistency with other pages

### Changed

- **Faction info page redesigned** (`/f info`): Converted from flat label rows to card-based stat layout — Power/Claims/Members stat cards, Relations (ally/enemy split), Status (raidable + founded), Treasury card, Leadership section
- **Player info page redesigned** (`/f who`): Converted to card-based layout — header with online indicator, faction membership card with View Faction button, Power/Combat/KDR stat cards, power bar, scrollable membership history
- **Admin faction info page redesigned**: Increased container to 700x640, added Economy Management section with Adjust Balance and View Treasury buttons
- **Admin dashboard expanded**: Added conditional economy stats row (Total Economy, Wealthiest Faction, Avg Balance)
- **Faction dashboard expanded**: Added conditional economy row (Treasury Balance, Next Upkeep, Personal Wallet)
- "Economy" tab added to admin nav bar between Players and Zones (conditionally visible when economy enabled)
- Relations page uses 2-tab layout (Relations combining allies+enemies, Pending combining incoming+outgoing) instead of 3 separate tabs
- Admin faction members page upgraded from DecoratedContainer (520x550) to Container (700x500) with sort dropdown replacing sort buttons
- CurseForge description updated with Discord community link and HyBounty integration callout

### Refactored

- **Codebase cleanup** — Purely mechanical refactoring with no behavioral changes:
  - `MessageUtil` and `UuidUtil` utilities replace ~200 hardcoded color/prefix/UUID patterns across ~85 files
  - `requireFaction()` helper in `FactionSubCommand` replaces 26 duplicate null-check blocks
  - GUI packages reorganized: consistent `gui/<subsystem>/page/` layout (admin, faction, newplayer, help)
  - Integration packages reorganized into `permissions/`, `protection/`, `placeholder/` subdirectories
  - `HyperFactions.java` decomposed (1,177→906 lines) with `lifecycle/` helpers (CallbackWiring, PeriodicTaskManager, MembershipHistoryHandler)
  - `HyperFactionsPlugin.java` decomposed (997→440 lines) with `platform/` helpers (EventRegistration, WorldSetup, PlayerConnectionHandler)
  - `AdminSubCommand.java` decomposed (2,462→306 lines) into 8 focused handler classes in `command/admin/handler/`
  - All internal documentation updated to reflect new structure

## [0.7.4] - 2026-02-14

### Added

- **`power_loss` zone flag**: Controls whether players lose faction power on death in a zone (default: `false` for both SafeZone and WarZone — no power loss in admin zones)
- **Config migration v4→v5**: Removes deprecated `warzonePowerLoss` config option (replaced by per-zone `power_loss` flag)
- Zone settings GUI now shows Power Loss toggle in the Death category
- **HytaleNative permission provider**: Automatic compatibility with any permission plugin that registers with Hytale's native PermissionsModule (LuckPerms, PermissionsPlus, etc.)
- **LuckPerms permission discovery**: All 53 HyperFactions permission nodes (44 permissions + 10 wildcards) now register with LuckPerms' PermissionRegistry on boot for web editor autocomplete
- **Lazy initialization for permission providers**: LuckPerms and VaultUnlocked providers now retry initialization on first use if they failed at startup due to load order timing, ensuring prefix/suffix data is available for chat formatting
- **Integration detail commands**: `/f admin integration luckperms`, `vaultunlocked`, and `native` now provide detailed status for each provider
- **Console support for admin commands**: All admin commands that don't require player context now work from the server console (`/f admin reload`, `debug status`, `backup list`, `decay`, `integrations`, `zone list`, `update`, `import`, etc.)
- Console receives help text instead of GUI when running `/f admin` with no arguments
- Player-only commands (`safezone`, `warzone`, `testgui`, GUI-based pages) show a clear error when run from console
- LuckPerms and VaultUnlocked declared as SoftDependencies in manifest.json

### Fixed

- **Fluid protection via interaction codecs**: Liquid placement (water/lava buckets) and fluid pickup (scooping with empty buckets) now blocked in protected zones and faction territory via `PlaceFluid` and `RefillContainer` codec replacements (no mixin dependency)
- **WarZone denial message**: Zone-denied actions in WarZones now show "You cannot do that in a WarZone." instead of the generic "You don't have permission to do that." (added `DENIED_WARZONE` protection result)
- **LuckPerms prefix/suffix not showing in chat**: Permission providers failed to initialize at startup because LuckPerms loads after HyperFactions. Chat format `{prefix}` resolved to empty. Lazy init ensures the LuckPerms provider retries on first chat message when LuckPerms is loaded, so prefixes like `[Admin]` now appear alongside faction tags. (#25, #11, #24)
- **VaultUnlocked provider using wrong class paths**: Corrected `net.milkbowl.vault2.VaultUnlocked` to `net.cfh.vault.VaultUnlocked`, fixed Subject/Context sub-package paths, unwrapped `Optional<>` returns from `permission()`/`chat()`, and fixed `getPrimaryGroup` → `primaryGroup` method name
- **Admin commands crashing on world thread assertion**: `AdminSubCommand` extended `AbstractAsyncCommand` which runs on ForkJoinPool, but ECS calls like `store.getComponent()` assert WorldThread. Restructured to dispatch player commands back to world thread via `runAsync(ctx, runnable, world)`.
- **WiFlow PlaceholderAPI NoClassDefFoundError**: Loading `WiFlowPlaceholderIntegration` class triggered class resolution for WiFlow imports, crashing when WiFlow wasn't installed. Wrapped in try-catch for `NoClassDefFoundError`.
- **PermissionRegistrar silent failure**: Used `findMethod("getPlugin")` on LuckPermsApiProvider which has no such method. Changed to private field access via `Field.setAccessible(true)` to access the `plugin` field.
- **World-specific permissions not working with HyperPerms**: Permission checks ignored world context, so permissions assigned to a specific world (e.g., `hyperfactions.create world=default`) were never matched. HyperPermsProviderAdapter now calls the context-aware `hasPermission(UUID, String, ContextSet)` with auto-resolved player contexts including current world.

## [0.7.3] - 2026-02-13

### Fixed

- **Startup crash when GravestonePlugin is outdated**: `GravestoneIntegration.init()` threw `NoSuchMethodError` when an older Gravestones version was installed without the v2 API (`getInstance()`). Now catches `LinkageError` (covers both missing plugin and missing methods) so HyperFactions starts cleanly regardless of Gravestones version.

## [0.7.2] - 2026-02-11

### Added

**Integration Flags GUI Sub-Page**
- New Admin Zone Integration Flags page for configuring integration-specific zone flags
- Accessible via "Integration Flags" button in Zone Settings footer
- Shows `gravestone_access` flag with clear description: "When ON, non-owners can loot graves. Owners always can."
- Dedicated Reset to Defaults button (only clears integration flags, not all zone flags)
- Scalable layout for future integration flags
- Integration flags disabled with "(no plugin)" indicator when the required integration is not installed or not enabled, matching the existing "(mixin)" pattern for mixin-dependent flags

**Expanded Admin Integrations Command**
- `/f admin integrations` now shows all 7 integrations organized by category:
  - Permissions: HyperPerms, LuckPerms, VaultUnlocked
  - Protection: OrbisGuard API, OrbisGuard-Mixins, Gravestones
  - Placeholders: PlaceholderAPI, WiFlow PAPI
- `/f admin integration <name>` detail views for all integrations:
  - `hyperperms` — Provider chain, active providers, fallback config
  - `orbisguard` — API detection, claim conflict detection status
  - `mixins` — Mixin availability, individual hook load status
  - `papi` — Expansion registration, placeholder count
  - `wiflow` — WiFlow expansion status
  - `gravestones` — Existing detail view (unchanged)

### Fixed

- **GUI crash on promotion**: Players with the faction GUI open would crash when another player promoted/demoted a member, due to stale `ActivePageTracker` entries sending UI updates to dismissed pages. Added `onDismiss()` cleanup to all 8 pages that register with the tracker (members, dashboard, invites, relations, chat map, new player map, new player invites, admin zone map). `FactionChatPage` already handled this correctly.
- **World map empty after teleport**: Batch refresh was calling `WorldMapTracker.clear()` (nuclear wipe of all loaded tiles + ClearWorldMap packet) instead of `clearChunks()` (surgical invalidation of only changed chunks). After teleporting, the slow spiral iterator would get wiped on every batch cycle before it could finish loading. Now uses `clearChunks()` matching Hytale's own BuilderToolsPlugin pattern.
- **Placeholders not resolving for factionless players**: Both PlaceholderAPI and WiFlow expansions returned `null` for faction-specific placeholders when a player had no faction. WiFlow's parser treats `null` as "unknown placeholder" and preserves the raw text (e.g., `{factions_tag}` shown literally). Now returns empty string `""` for text placeholders and sensible defaults (`"0"`, `"false"`) for numeric/boolean ones.
- **Set Relation search interrupting typing**: The Set Relation modal opened a brand new page on every keystroke via `ValueChanged`, destroying the text field focus. Now uses partial `sendUpdate()` like the Faction Browser page to preserve focus during search.

**GravestonePlugin Integration** (now fully functional with GravestonePlugin v2 API)
- Reflection-based soft dependency on Zurku's GravestonePlugin for faction-aware gravestone protection
- New `GravestoneIntegration` class discovers running GravestonePlugin via Hytale's PluginManager
- Faction-aware access control for gravestone break and collection events:
  - Owner always has access to their own gravestone
  - Faction members can access gravestones in own territory (configurable)
  - Allies can access gravestones in allied territory (configurable)
  - Enemies/outsiders blocked from gravestones in claimed territory
  - Per-zone-type settings: SafeZone, WarZone, Wilderness each configurable independently
- Admin bypass and `hyperfactions.gravestone.bypass` permission support
- Death location announcements: faction members notified with coordinates when a member dies
- New `/f admin gravestone` command showing integration status, plugin detection, and config values
- New `config/gravestones.json` module config with 7 settings:
  - `protectInOwnTerritory`, `factionMembersCanAccess`, `alliesCanAccess`
  - `protectInSafeZone`, `protectInWarZone`, `protectInWilderness`
  - `announceDeathLocation`
- Gravestone block detection in `BlockBreakProtectionSystem` and `BlockUseProtectionSystem` (intercepts before normal protection)
- Startup banner shows GravestonePlugin detection status
- Debug logging for all gravestone access checks

### Changed

- Zone flag `gravestone_access` display name changed to "Others Loot Graves" for clarity
- Zone flag description updated to explicitly state owners always have access to their own gravestone
- Placeholder expansions (PAPI + WiFlow) now return empty strings/defaults for factionless players instead of `null`

## [0.7.1] - 2026-02-08

### Added

**Faction & Alliance Chat Overhaul**
- New persistent chat history system with per-faction JSON storage and debounced disk writes
- Chat history GUI page with Faction/Ally tabs, scrollable message list, and send-from-GUI input bar
- Toggle-only chat command: `/f c` cycles modes, `/f c f` for faction, `/f c a` for ally, `/f c off` for public
- Configurable chat formatting with colors and prefixes per channel in `config/chat.json`
- Ally tab merges messages from all allied factions into a chronological timeline with faction tag prefixes
- Automatic retention cleanup of old messages (configurable days and interval)
- Real-time GUI push: new messages appear instantly for all faction members with the chat page open
- Chat history pre-warming on player connect for instant page loads

**Dashboard Chat Mode Button**
- Replaced separate F-Chat and A-Chat buttons with a single unified "Chat Mode" toggle button
- Cycles through Public → Faction → Ally modes with color-coded labels
- Respects `chat.faction` and `chat.ally` permissions

**PlaceholderAPI Improvements**
- Refactored PAPI and WiFlow expansions for cleaner placeholder resolution
- Added `factions_home_pitch` placeholder (WiFlow)
- Both expansions now share consistent null handling and formatting

**CurseForge Description**
- Complete rewrite of the CurseForge listing page with comprehensive feature documentation
- Added emojis, colored accents, and marketing-friendly layout
- Detailed sections for protection system, zone flags, chat, announcements, GUI system, admin tools, and integrations
- Integration links for HyperPerms, LuckPerms, VaultUnlocked, Hyxin, OrbisGuard-Mixins, PlaceholderAPI, WiFlow
- Upcoming integration callouts for Ecotale, RPG Leveling, NPC Dialog, NPC Quests Maker, BetterScoreBoard

### Changed

- Chat command no longer supports one-shot `/f c <message>` send (use toggle mode instead)
- `ChatConfig` now includes a `factionChat` nested section for channel-specific settings
- `GuiManager` bumped Settings to order 7 and Help to order 8 to accommodate Chat at order 6

### Fixed

- Shadow JAR clobbering in multi-project builds: added `jar { archiveClassifier = 'plain' }` to prevent the plain jar task from overwriting the shadow JAR
- WorldMapConfig minor cleanup

### Docs

- New `docs/placeholders.md` with complete placeholder reference for PAPI and WiFlow (34+ placeholders)
- Updated `docs/integrations.md` with WiFlow expansion details
- Updated `docs/readme.md` with chat page entry

## [0.7.0] - 2026-02-07

**Closes issues:**
- [#7](https://github.com/HyperSystems-Development/HyperFactions/issues/7) — Announcement system for major faction events
- [#16](https://github.com/HyperSystems-Development/HyperFactions/issues/16) — Add data import from ElbaphFactions
- [#18](https://github.com/HyperSystems-Development/HyperFactions/issues/18) — Integrate with Better Scoreboard for faction placeholders
- [#22](https://github.com/HyperSystems-Development/HyperFactions/issues/22) — Hide GUI buttons when player lacks permission
- [#23](https://github.com/HyperSystems-Development/HyperFactions/issues/23) — Add Placeholders for scoreboards, holograms, and menus

### Added

**Server-Wide Faction Announcements**
- New `announcements.json` module config with master toggle and per-event toggles
- Broadcasts to all online players when significant faction events occur:
  - Faction created, faction disbanded, leadership transferred
  - Territory overclaimed, war declared, alliance formed, alliance broken
- Admin actions (force disband, admin set relation) do not trigger announcements
- Auto-disbands (leader leaves with no members) do not trigger announcements

**Admin Faction List Quick Actions**
- Added "Members" and "Settings" quick buttons to expanded faction entries in admin faction list
- Navigate directly to a faction's members or settings page without going through View Info first

**Create Faction Page Redesign**
- Merged the two-step create wizard into a single two-column page (950x650)
- Left column: preview card, name/tag inputs, description, recruitment toggle
- Right column: color picker, territory permission toggles (break/place/interact for outsiders/allies/members), PvP toggle, create button
- Players can now configure territory permissions at faction creation time instead of getting only defaults

**Permission-Based GUI Filtering**
- Nav bar entries now respect server permissions (e.g., removing `hyperfactions.info.members` hides Members tab)
- Dashboard quick-action buttons check permissions (Home, Claim, F-Chat, Leave)
- Members page action buttons check permissions (Promote, Demote, Kick, Transfer)
- Relations page management buttons check permissions (Ally, Enemy, Neutral)

**Real-Time GUI Updates**
- GUI pages now refresh automatically when underlying data changes
- New `ActivePageTracker` tracks which page each player has open
- New `GuiUpdateService` bridges manager change events to live GUI refresh
- Invite created/removed refreshes recipient's and faction's invites page
- Join request created/accepted/declined refreshes faction invites page
- Member joined/left/kicked refreshes faction members and dashboard pages
- Member promoted/demoted refreshes faction members page
- Relation changed refreshes both factions' relations and dashboard pages
- Ally request received refreshes target faction's relations page
- Chunk claimed/unclaimed refreshes all map viewers (faction, new player, admin zone)
- Thread-safe: dispatches refresh on correct world thread with stale-check

**PlaceholderAPI Integration**
- Soft dependency on PlaceholderAPI for Hytale
- Faction placeholders available for scoreboards and chat

**Configuration**
- New `allowWithoutPermissionMod` boolean in config.json permissions section (default: false)
  - When true, all non-admin user permissions are allowed when no permission mod is installed
  - Admin permissions always require Hytale OP as a fallback when no permission mod handles them
  - Bypass and limit permissions always require explicit grants regardless of this setting
- Configurable combat tag duration and settings
- Configurable power gain/loss settings

**GUI Map Improvements**
- Player position on all GUI maps now shows a white "+" marker overlaid on the chunk color instead of a solid white cell
- Chunk color is visible behind the marker so you can see what territory type you're standing in
- Legend updated to show "+" symbol for "You are here" entry

**Backup Retention**
- Configurable `backupRetentionDays` in backup config (default: 7)
- Shutdown backups now automatically clean up backups older than the retention period
- Setting to 0 disables automatic cleanup

**Debug Tools**
- New element test page (`/f admin testgui`) for CustomUI research and verification

### Changed

**Admin Faction Settings Layout Unified**
- Admin faction settings page now matches the normal faction settings layout with boxed sections (`#1a2a3a` backgrounds)
- Appearance (color picker) moved from left column to right column
- Combat and Access Control merged into single "Faction Settings" section
- Danger Zone now uses `#2a1a1a` background box with "This action is irreversible" text
- Container height increased from 700 to 780 to eliminate right column scrolling

**GUI Native UI Audit (Batch 1)**
- Migrated all button styles from native `$C.@SecondaryTextButtonStyle` to custom `$S.@ButtonStyle` with controlled FontSize 13 across 90+ .ui files
- Migrated destructive action buttons from `$C.@CancelTextButton` template to `TextButton` with `$S.@RedButtonStyle` across 28 files
- Converted ALL CAPS button and label text to Title Case across all .ui templates and Java files
- Cleaned up `styles.ui` — removed unused styles, consolidated definitions (260→260 lines, focused)
- Settings danger zone now inlined in template (visibility toggle) instead of dynamic append
- Settings container height increased from 620 to 680 for better content visibility

- War zone color on all GUI maps changed from red to purple (`#c084fc`) for visual clarity
- Admin zone map war zone colors updated to purple (bright and transparent variants)
- Pages accessible to non-faction players (Dashboard, Map, Browse) now show the new player nav bar (Browse, Create, Invites, Map, Help) instead of the faction nav bar
- Replaced `permissions.fallbackBehavior` string config ("allow"/"deny") with `permissions.allowWithoutPermissionMod` boolean (default: false)

### Fixed

- Overclaim notification and teleport messages now use the configured prefix from `config.json` instead of hardcoded "HyperFactions"
- Faction member events now properly published for promote/demote actions
- Non-faction players seeing wrong nav bar (faction nav instead of new player nav) on Dashboard, Map, and Browse pages
- Nav event handling for non-faction players now correctly routes through new player page registry
- Unclaim command improvements

## [0.6.2] - 2026-02-04

### Added

**Delete Faction Home**
- New `/f delhome` command to delete faction home location
- New `hyperfactions.teleport.delhome` permission node
- DELETE button in faction settings GUI (General tab, Home Location section)
- Button automatically disabled when no home is set (same for TELEPORT button)

**World Map Configuration**
- New `showFactionTags` option in worldmap.json to hide faction tags on the in-game world map while keeping faction color overlays visible
- New `refreshMode` option to control when world map tiles are regenerated:
  - `proximity` (default) - Only refreshes for players within range of claim changes. Best for most servers.
  - `incremental` - Refreshes specific chunks for all players. Good balance of performance and consistency.
  - `debounced` - Full map refresh after a quiet period (5s default). Use if incremental causes issues.
  - `immediate` - Full map refresh on every claim change. Original behavior, not recommended for busy servers.
  - `manual` - No automatic refresh. Use `/f admin map refresh` to update manually.
- Configurable batch intervals, chunk limits, and proximity radius (default: 32 chunks) per mode
- Auto-fallback option if reflection errors occur

**Configuration**
- Configurable message prefix via `messagePrefix` in config.json (default: "[HyperFactions]")
- Migration backups now use ZIP format for better compression and organization

### Removed

- Removed dead code: `FactionSettingsPage.java`, `FactionSettingsData.java`, `faction_settings.ui` (replaced by tabbed settings system)

## [0.6.1] - 2026-02-04

### Added

**Update System Enhancements**
- Pre-update data backup: Automatically creates a full backup of configs and data before downloading updates
- Old JAR cleanup: Removes old `.jar.backup` files during updates, keeping only the version being upgraded from for rollback
- New `/f admin rollback` command to revert to previous version before server restart
- Rollback safety detection: Blocks automatic rollback after server restart (when migrations may have run)
- Rollback marker system to track update state

### Fixed

**Zone Protection**
- Factions can no longer claim SafeZone or WarZone chunks (security fix)
- Added `ZONE_PROTECTED` result to ClaimManager for proper error messaging

**Permission System**
- Fixed wildcard permission expansion: `hyperfactions.teleport.*` now properly grants `home`, `sethome`, `stuck`
- Fixed root wildcard: `hyperfactions.*` now grants all faction permissions
- `hyperfactions.use` no longer grants all user-level actions (now only grants `/f` command and GUI access)
- Fixed documentation: `fallbackBehavior` default is `"deny"`, not `"allow"`

### Changed

- ClaimManager now receives ZoneManager reference for zone protection checks
- PermissionManager checks category wildcards before falling back to defaults
- Updated help text to include `/f admin rollback` command

## [0.6.0] - 2026-02-03

### Breaking Changes

**New Optional Dependencies for Full Protection**

HyperFactions now integrates with OrbisGuard-Mixins for enhanced protection coverage. While HyperFactions works without these, some zone protections require the mixin system:

- [Hyxin](https://www.curseforge.com/hytale/mods/hyxin) - Mixin loader (enables protection hooks)
- [OrbisGuard-Mixins](https://www.curseforge.com/hytale/mods/orbisguard-mixins) - F-key pickup, keep inventory, invincible items

**Installation for Hyxin + OrbisGuard-Mixins:**
1. Create an `earlyplugins/` folder in your server directory
2. Place Hyxin and OrbisGuard-Mixins JARs in `earlyplugins/` (NOT mods/)
3. Add `--accept-early-plugins` to your server start script:
   - Linux: `DEFAULT_ARGS="--accept-early-plugins --assets ../Assets.zip ..."`
   - Windows: `set DEFAULT_ARGS=--accept-early-plugins --assets ../Assets.zip ...`

**Additional Recommended Dependencies:**
- [HyperPerms](https://www.curseforge.com/hytale/mods/hyperperms) - Permission-based limits for claims, power, and features
- [VaultUnlocked](https://www.curseforge.com/hytale/mods/vaultunlocked) - Chat, economy, and permission compatibility with other mods

**New Zone Flags (some require mixins)**

New flags added that require OrbisGuard-Mixins to function:
- `item_pickup_manual` - F-key pickup blocking (requires mixin)
- `invincible_items` - Prevent durability loss (requires mixin)
- `keep_inventory` - Keep items on death (requires mixin)
- `npc_spawning` - NPC spawn control via mixin hook (requires mixin)

Native flags that work without mixins:
- `item_pickup` - Auto pickup (walking over items)
- `item_drop` - Item dropping from inventory

### Added

**OrbisGuard-Mixins Integration**
- New `OrbisMixinsIntegration` class for registering protection hooks
- Harvest hook for F-key rubble/crop pickup protection
- Pickup hook for auto and manual item pickup protection
- Spawn hook for NPC spawn control in zones
- Detection system with status logging at startup
- Graceful degradation when mixins are not installed

**OrbisGuard API Integration (Work in Progress)**
- New `OrbisGuardIntegration` class for region protection detection
- Foundation for preventing claims in OrbisGuard-protected regions

**Enhanced Item Protection**
- `HarvestPickupProtectionSystem` - ECS system for InteractivelyPickupItemEvent
- `ProtectionChecker.canPickupItem()` method with mode awareness (auto vs manual)
- Admin bypass support for all pickup protections
- Zone flag checks cascade properly (global → zone-specific)

**New Zone Flags**
- `item_pickup_manual` - Control F-key pickup separately from auto pickup
- `invincible_items` - Prevent tool/armor durability loss in zones
- `keep_inventory` - Keep inventory on death in zones
- `npc_spawning` - Control NPC spawning via mixin hook
- Updated SafeZone defaults: auto pickup ON, manual pickup OFF, keep inventory ON, invincible items ON
- Updated WarZone defaults: all pickup ON, keep inventory OFF, invincible items OFF

**Zone Flag Categories**
- New "Death" category with `keep_inventory` flag
- Reorganized "Items" category with 4 flags
- `ZoneFlags.requiresMixin()` method to check if a flag needs mixin support
- `ZoneFlags.getMixinType()` method to identify which mixin a flag requires
- `ZoneFlags.getCategory()` and `ZoneFlags.getFlagsByCategory()` for UI organization

**Debug System**
- New `mixin` debug category for OrbisGuard-Mixins integration logging
- New `spawning` debug category for mob/NPC spawn control logging
- `Logger.debugMixin()` and `Logger.debugSpawning()` methods

**Zone Type Change Feature**
- New TYPE button in zone list entries to change between SafeZone and WarZone
- Modal dialog with options to keep existing flag overrides or reset to new type defaults
- ZoneManager.changeZoneType() method for programmatic zone type changes

**Redesigned Create Zone Wizard**
- Modern card-based two-column layout with better organization
- 5 claiming methods: No Claims, Single Chunk, Radius (Circle), Radius (Square), Use Claim Map
- Radius presets (3, 5, 10, 15, 20) with custom input up to 50 chunks
- Live chunk count preview for radius selections
- Flag customization choice: use defaults or customize after creation
- Action buttons moved to top with cyan divider
- Proper navigation flow: USE_MAP + customize flags goes create → map → flags

### Changed

**WarZone Color Scheme**
- Changed WarZone color from purple/orange to red (#FF5555) across all maps
- In-game world map now shows WarZones in red with light opacity
- GUI faction map and admin zone map use consistent red coloring
- Better visual distinction between SafeZones (teal) and WarZones (red)

**Zone List UI Improvements**
- Removed bounds from collapsed zone entry (still visible when expanded)
- Cleaner inline stats showing only chunk count

**Mob Spawn Suppression System**
- New SpawnSuppressionManager integrates with Hytale's native SpawnSuppressionController
- Chunk-based spawn suppression using the server's built-in suppression system
- 4 new zone flags for granular mob spawning control:
  - `mob_spawning` - Master toggle (false = block all mob spawning)
  - `hostile_mob_spawning` - Control hostile mob spawning
  - `passive_mob_spawning` - Control passive mob spawning
  - `neutral_mob_spawning` - Control neutral mob spawning
- SafeZones now block all mob spawning by default
- WarZones allow all mob spawning by default
- Uses Hytale's NPCGroup system (hostile, passive, neutral) for categorization
- Suppression automatically applied to new worlds on load

**Overclaim Defender Alerts**
- Faction members now receive real-time alerts when their territory is overclaimed
- Alert message includes attacker faction name and chunk coordinates

### Changed

- Updated admin zone settings UI with mob spawning controls
- Reorganized zone flag categories for better UI organization:
  - Combat flags: PvP, friendly fire, projectile damage, mob damage
  - Building flags: Build allowed
  - Spawning flags: Mob spawning (4 flags)
  - Interaction flags: Block interact, door, container, bench, processing, seat use
  - Item flags: Item drop, item pickup
  - Damage flags: Fall damage, environmental damage

## [0.5.2] - 2026-02-03

### Fixed

**Teleport System Overhaul**
- Fixed warmup teleports not executing (countdown messages worked but teleport never happened)
  - Root cause: Teleport component must be added via `targetWorld.execute()` on the destination world's thread
  - Changed from `new Teleport()` to `Teleport.createForPlayer()` for proper player head/body rotation setup
  - Fixed in all 7 teleport locations: TerritoryTickingSystem, HomeSubCommand, FactionSettingsPage, FactionSettingsTabsPage, FactionDashboardPage, FactionMainPage, AdminFactionsPage

**Message Formatting**
- Fixed garbled chat messages showing `Ã?Â§b[HyperFactions]Ã?Â§r` instead of proper colors
  - TeleportManager was using legacy `\u00A7` color codes which `Message.raw()` doesn't parse
  - Changed to proper `Message.raw(text).color(hexColor)` pattern

**Client Crash on /f power**
- Fixed `/f power` and `/f who` commands crashing the client
  - Commands referenced non-existent UI templates (`player_info.ui`)
  - Disabled GUI mode, now falls back to text mode until templates are created

### Added

**Teleport Countdown Messages**
- Warmup teleports now show incremental countdown messages
  - High warmup (30+ seconds): announces at 30, 15, then every second from 10 down
  - Low warmup (under 10 seconds): announces every second
  - Example: "Teleporting in 30 seconds...", "Teleporting in 10 seconds...", etc.

### Changed

- Removed unused `TeleportContext.java` class (replaced by `TeleportManager.TeleportDestination`)
- Refactored `TeleportManager.PendingTeleport` from record to class to support countdown state tracking
- `/f stuck` now uses generic `scheduleTeleport()` method instead of faction-specific teleport logic

## [0.5.1] - 2026-02-02

### Fixed

**Debug Toggle Persistence**
- Fixed debug categories not staying disabled after server restart
- Root cause: `applyToLogger()` was using `enabledByDefault || category` logic which re-enabled categories on load
- Individual category settings now take direct precedence over enabledByDefault
- `enableAll()` and `disableAll()` now properly clear the enabledByDefault flag

**Debug Config Defaults**
- All debug categories now correctly default to `false` on first load
- Fixed `loadModuleSettings()` using field values as defaults instead of explicit `false`

### Added

**World Map Debug Category**
- New `worldmap` debug category separates verbose map generation logs from territory notifications
- Use `/f admin debug toggle worldmap on` to enable map tile generation logging
- Use `/f admin debug toggle territory on` for territory entry/exit notifications only
- Significantly reduces console spam when debugging territory features

### Changed

- Territory debug now only logs chunk entry/exit notifications
- World map debug logs all map generation, tile updates, and claim rendering

## [0.5.0] - 2026-02-02

### Added

**Death Power Loss System**
- Implemented ECS-based `PlayerDeathSystem` to detect player deaths via `DeathComponent`
- Power penalty now correctly applied when players die (was previously orphaned code)
- Uses Hytale's native ECS pattern (`RefChangeSystem<EntityStore, DeathComponent>`)

**Respawn Handling System**
- Implemented ECS-based `PlayerRespawnSystem` to detect respawns via `DeathComponent` removal
- Combat tag automatically cleared on respawn
- Spawn protection applied at respawn location (configurable duration)

**Claim Decay System**
- New automatic claim decay for inactive factions
- If ALL faction members are offline longer than `decayDaysInactive` (default: 30 days), all claims are removed
- Decay runs hourly via scheduled task
- Admin commands for decay management:
  - `/f admin decay` - Show decay system status
  - `/f admin decay run` - Manually trigger decay check
  - `/f admin decay check <faction>` - Check specific faction's decay status

**Debug Toggle Persistence**
- Implemented `/f admin debug toggle <category> [on|off]` command
- Debug category changes now persist to `config/debug.json` across server restarts
- `/f admin debug toggle` shows current status of all 6 categories
- `/f admin debug toggle all` enables/disables all categories at once
- `/f admin debug status` now shows debug logging status alongside data counts

**Zone Rename Modal**
- Admin zone rename UI accessible from AdminZonePage
- New ZoneRenameModalPage and ZoneRenameModalData classes
- Zone name input with validation and immediate save

### Changed

**Config System Restructure**
- Migrated all 31 files from deprecated `HyperFactionsConfig.get()` to `ConfigManager.get()`
- New modular config architecture with `ConfigFile`, `ModuleConfig`, and `ConfigManager`
- Added validation support with auto-correction for invalid config values
- `HyperFactionsConfig` facade retained for backward compatibility (marked deprecated)
- Config modules now support individual save/reload operations

**Command Architecture Refactor**
- Split monolithic FactionCommand.java (3500+ lines) into 40+ individual subcommand files
- New `FactionSubCommand` base class with shared functionality and permission checks
- Commands organized by category:
  - `command/admin/` - Admin subcommands (AdminSubCommand handles all /f admin *)
  - `command/faction/` - Create, Disband, Rename, Desc, Color, Open, Close
  - `command/info/` - Help, Info, List, Map, Members, Power, Who
  - `command/member/` - Accept, Demote, Invite, Kick, Leave, Promote, Transfer
  - `command/relation/` - Ally, Enemy, Neutral, Relations
  - `command/social/` - Chat, Invites, Request
  - `command/teleport/` - Home, SetHome
  - `command/territory/` - Claim, Overclaim, Stuck, Unclaim
  - `command/ui/` - Gui, Settings
  - `command/util/` - CommandUtil shared utilities
- Added `/hyperfactions` as additional command alias

### Fixed

**Power Debug Logging**
- Fixed `PlayerListener.onPlayerDeath()` using wrong logger category (`Logger.debug` → `Logger.debugPower`)

## [0.4.3] - 2026-02-02

### Fixed

**In-Game World Map Not Showing Claims**
- Fixed world map claim overlays not appearing on production servers
- Root cause: `setWorldMapProvider()` only affects future world loads, not the live WorldMapManager
- Now calling `setGenerator()` directly on WorldMapManager to properly register our claim renderer
- Added auto-recovery if another mod overwrites the generator during runtime

## [0.4.2] - 2026-02-02

### Fixed

**Admin Debug Commands**
- Fixed debug commands to be under `/f admin debug` instead of `/f debug`
- Debug subcommands: `power`, `combat`, `claim`, `zone`, `protection`

**Admin Unclaim All GUI**
- Fixed "unclaim all" in admin factions menu not updating faction's claim count
- GUI map now correctly shows 0 claims after unclaiming all territory
- Faction record is now properly updated when bulk unclaiming

### Changed

**Help GUI Overhaul**
- Added command syntax legend at top explaining `<required>` vs `[optional]` notation
- Restructured command reference with descriptions on separate indented lines
- Added new description line template for cleaner visual hierarchy
- Corrected all command syntax to match actual code:
  - `/f admin zone create <name> <safe|war>` (was `<type>`)
  - `/f admin zone radius <name> <radius> [circle|square]` (was `<r> [shape]`)
  - `/f admin zone info [name]` (was missing entirely)
- Removed non-existent `/f admin bypass` command from help
- Removed non-functional `--text` flag references from all help content

## [0.4.1] - 2026-02-02

### Fixed

**Combat Tagging Restored**
- Fixed combat tagging not working after protection system refactor to ECS-based handlers
  - PvP combat now properly tags both attacker and defender
  - PvE combat (mob damage) now properly tags the player being attacked
- Added configurable `logoutPowerLoss` setting for combat logout penalty (default: 1.0)
  - Separate from normal death penalty for finer control
  - Set to 0 to disable combat logout power loss while keeping other penalties

**HyFactions Import Map Display**
- Fixed in-game world map not showing imported claims and zones after import
- Fixed GUI territory map not displaying imported faction claims
- Import now rebuilds claim index and refreshes world maps after completion

## [0.4.0] - 2026-02-01

### Added

**Admin GUI System**
- Complete admin interface accessible via `/f admin` command
- Dashboard page with server statistics overview
- Factions management: browse all factions, view details, edit settings
- Zone management: create, configure, and delete zones with visual map
- Configuration page for runtime settings adjustment
- Backups management page (placeholder for future functionality)
- Help page with command reference
- Updates page for version information
- Navigation bar for consistent page switching
- Admin faction settings now include both general settings and permissions

**Admin Mode for Modals**
- Admins can now edit faction settings (name, tag, description, color, recruitment) without being a member
- All admin actions are prefixed with `[Admin]` in chat messages

**Faction Dashboard Redesign**
- New admin-style info blocks with 6 key statistics:
  - Power (current/max with percentage)
  - Claims (used/max with available count)
  - Members (total with online count)
  - Relations (ally/enemy counts)
  - Status (Open/Invite Only indicator)
  - Invites (sent invites and join requests count)

**Leader Leave Flow**
- New leader leave confirmation page with succession information
- Shows who will become the new leader (highest officer, then most senior member)
- If no successor available, offers faction disband option
- Automatic leadership transfer on leader departure

**Browser Page Improvements**
- Both faction browsers now use expandable IndexCards pattern (matching admin pages)
- Expandable entries with faction details and action buttons
- Improved search and sort functionality

**Zone Import Improvements**
- `ZoneFlags.getDefaultFlags()` helper method for importing zones
- Zones imported from mods without flag systems now get proper defaults
- Import validation report for HyFactions importer

### Changed

**Protection System Reorganization**
- Reorganized protection code into logical subdirectories:
  - `protection/zone/` - Zone-specific protection checks
  - `protection/damage/` - Damage type handlers
  - `protection/ecs/` - ECS event systems
  - `protection/debug/` - Debug utilities
- Moved SpawnProtection and ProtectionListener into protection package

**Logging Improvements**
- Converted verbose zone lookup logs to debug level
- Converted GUI build/event logs to debug level
- Converted world map provider logs to debug level

**Nav Bar Role-Based Filtering**
- Invites button now only visible to officers and leaders
- FactionPageRegistry now supports `minimumRole` for page visibility
- NavBarHelper updated to filter buttons based on viewer's role

### Fixed

**Search Not Working**
- Fixed search functionality in faction browser and new player browser
- Codec key mismatch (`SearchQuery` vs `@SearchQuery`) now resolved
- Search input values now correctly passed to event handlers

**Sort Buttons Breaking Navigation**
- Fixed sort buttons causing nav bar to disappear
- Implemented proper `rebuildList()` pattern instead of full page rebuild

**Leader Cannot Leave**
- Leaders can now properly leave their faction
- Leadership is automatically transferred to the best successor
- Fixed `transferLeadership` parameter order bug (newLeader, actorUuid)

**CustomUI Visible Property**
- Fixed `.Visible` property using string instead of boolean
- Changed `cmd.set("#Element.Visible", "true")` to `cmd.set("#Element.Visible", true)`

**New Player Map Relation Indicators**
- New player map no longer shows ally/enemy indicators
- Players not in a faction no longer see relation-based colors

## [0.3.1] - 2026-02-01

### Fixed

**Storage Race Condition**
- Fixed checksum verification failures when saving factions rapidly
  - Concurrent writes no longer overwrite each other's temp files
  - Each atomic write now uses a unique temp file name

**TextField Input**
- Fixed text input fields not accepting keyboard input in GUI modals
  - Faction name input (create wizard step 1)
  - Description input (create wizard step 2)
  - Rename modal, tag modal, description modal
  - Zone creation wizard name input
  - Relation search input

**Logging Cleanup**
- Removed excessive debug logging that was spamming server console
  - World map generation no longer logs every chunk render
  - Territory notifications converted to debug category
  - GUI build/event logs removed or converted to debug
  - All debug categories remain disabled by default

## [0.3.0] - 2026-02-01

### Fixed

**CRITICAL: Data Loss Prevention**
- Fixed faction data loss on update/reload when deserialization fails
  - FactionManager now validates loaded data before clearing caches
  - If loading returns empty but data existed, keeps in-memory data safe
  - Added `.exceptionally()` handlers to catch and log exceptions without data loss
- Fixed silent exception handling in all storage classes
  - JsonFactionStorage now reports all failed files with SEVERE level logging
  - JsonPlayerStorage now reports all failed files with SEVERE level logging
  - JsonZoneStorage now reports all failed zones with SEVERE level logging
  - Storage methods now throw RuntimeException on critical I/O failures instead of returning empty
- Added comprehensive loading validation
  - Detects when 0 items load from non-empty directories (corruption indicator)
  - Logs CRITICAL warnings when data appears to be missing
  - Reports total files vs successfully loaded files for debugging
- Fixed ZoneManager and PowerManager with same safety protections
  - Both managers now validate loading before clearing caches
  - Exception handlers prevent data loss on unexpected errors

**WarZone/SafeZone Protection**
- Fixed container protection in WarZones - chests, furnaces, and workbenches are now properly blocked
  - Previously only doors were blocked; now all non-door blocks are protected
  - Uses door-only detection: only blocks with "door" state or door/gate in block ID are allowed
  - All other block interactions (containers, processing benches, etc.) are blocked
- Fixed protection denial messages showing raw color codes (e.g., `§c`)
  - Messages now use clean text without legacy formatting codes

**Help System**
- Added backup and admin commands to help GUI
  - `/f admin backup create [name]`, `/f admin backup list`, etc.
  - `/f admin zone` and `/f admin update` now listed

**HyperPerms Integration**
- Fixed faction prefix display in HyperPerms chat formatting
  - Added missing `ReflectiveHyperFactionsProvider` implementation
  - Faction names now appear correctly in chat when using HyperPerms

### Added

**Update System**
- `/f admin update` command to download and install plugin updates
- Release channel config option (`releaseChannel`: "stable" or "prerelease")
- Pre-release support in update checker (uses /releases endpoint when enabled)

**Configuration**
- Config merge behavior: missing keys are added with defaults without overwriting user values
- `configNeedsSave` flag to only write config when new keys are added

### Migration Guide (from v0.1.0)

**Permission Node Changes**

If upgrading from v0.1.0, the permission system has been restructured. Individual permission nodes (e.g., `hyperfactions.create`, `hyperfactions.invite`) are now organized under category wildcards.

**Recommended Setup (HyperPerms commands):**

Grant full faction functionality to default group:
```
/hp group setperm default hyperfactions.use
/hp group setperm default hyperfactions.faction.*
/hp group setperm default hyperfactions.member.*
/hp group setperm default hyperfactions.territory.*
/hp group setperm default hyperfactions.teleport.*
/hp group setperm default hyperfactions.relation.*
/hp group setperm default hyperfactions.chat.*
/hp group setperm default hyperfactions.info.*
```

**Permission Categories:**

| Category | Description |
|----------|-------------|
| `hyperfactions.use` | **Required** - Base permission to use `/f` command |
| `hyperfactions.faction.*` | Create, disband, rename, tag, color, open/close |
| `hyperfactions.member.*` | Invite, join, leave, kick, promote, demote, transfer |
| `hyperfactions.territory.*` | Claim, unclaim, overclaim, map |
| `hyperfactions.teleport.*` | Home, sethome, stuck |
| `hyperfactions.relation.*` | Ally, enemy, neutral, view relations |
| `hyperfactions.chat.*` | Faction chat, ally chat |
| `hyperfactions.info.*` | Info, list, who, power, members, logs, help |

**Note:** `hyperfactions.use` is required as the base permission to access the `/f` command. Category permissions control specific functionality. Admin, bypass, and limit permissions require explicit grants.

## [0.2.0] - 2026-02-01

### Added

**Update System**
- GitHub releases update checker with HTTP caching
- Login notifications for admins when updates are available
- Per-player notification preferences (opt-out support)

**Permission System**
- Unified PermissionManager with chain-of-responsibility pattern
- Support for VaultUnlocked, HyperPerms, and LuckPerms providers
- `hyperfactions.use` now grants all user-level permissions for simpler setup
- Centralized Permissions.java with all permission node definitions
- Fallback behavior: admin perms require OP, user perms allow by default

**PvP Protection**
- PvPProtectionSystem to enforce faction/ally damage rules
- Respects `allyDamage` and `factionDamage` config settings
- Denial messages sent to attacker when PvP is blocked

**Chat Formatting**
- Faction tags in public chat with relation-based coloring
- Colors: green (same faction), pink (ally), red (enemy), gray (neutral)
- Configurable chat format string with placeholders
- ChatContext for thread-safe sender tracking

**GUI Improvements**
- Configurable nav bar title via `gui.title` in config.json
- Wider nav bar title area (120px → 160px) for full "HyperFactions" display

**Build System**
- BuildInfo.java auto-generation with version, Java version, and timestamp
- Centralized version management in build.gradle

**Multi-Chunk Zone System**
- Zones can now span multiple chunks (previously limited to single chunk)
- Zone chunk claiming/unclaiming via GUI and commands
- Zone changes now trigger world map refresh for all players

**Admin Zone GUI**
- Zone list page with tab filtering (All/Safe/War)
- Interactive zone map page for visual chunk claiming/unclaiming
- Create zone wizard with optional initial chunk claim
- Zone entry display with chunk counts and edit/delete actions

**Zone Admin Commands**
- `/f admin zone create <name> <safe|war>` - create empty zone
- `/f admin zone claim <name>` - claim current chunk for zone
- `/f admin zone unclaim` - unclaim current chunk from its zone
- `/f admin zone remove <name>` - delete zone entirely
- `/f admin zone list [safe|war]` - list zones with optional filter
- `/f admin zone radius <name> <radius> [circle]` - claim chunks in radius

**Help System**
- Refactored help GUI with improved layout and organization

**Members Page Overhaul**
- Expandable member entries following AdminUI pattern (click to expand/collapse)
- Sort members by role (Leader → Officer → Member) or last online time
- Action buttons with text labels: PROMOTE, DEMOTE, KICK, MAKE LEADER
- Expanded view shows power (with color coding), joined date, and last death (relative format)
- Transfer leadership now shows confirmation modal before executing

**Faction Permissions System**
- New FactionPermissions data model with 11 boolean flags
- Territory access control: break/place/interact permissions for outsiders, allies, and members
- PvP toggle for faction territory
- Officers can edit permissions toggle (leader-only setting)
- Tabbed settings page: General | Permissions | Members tabs
- Server-side permission locks to enforce server-wide rules

**Admin Improvements**
- Admin disband now shows confirmation modal before executing
- Admin faction list properly refreshes after disbanding a faction

**Commands**
- `/f sync` - Admin command to merge disk data with in-memory faction data (timestamp-based)

### Fixed
- Nav bar selector crash when opening GUI (use element ID instead of type selector)
- Description text wiped when toggling recruitment in create faction wizard
- Ally PvP protection not enforced (PvPProtectionSystem was missing)
- Codec key mismatch for zone name input (`@Name` vs `Name`)
- Reload button showing wrong command (`/f reload` not `/f admin reload`)
- Faction GUI map not updating on claim/unclaim operations
- In-game world map not updating when zones are created/updated/deleted
- GUI pages not refreshing properly (replaced `sendUpdate()` with new page instances)
- Navigation from members page now works (FactionMembersData implements NavAwareData)
- Online member count in settings now shows actual count instead of "?"

### Changed
- Shadow plugin updated from 8.3.5 to 9.3.1 (fixes BuildInfo generation)
- Zone storage format updated to support multiple chunks per zone
- Admin zone page now uses tabbed filtering instead of separate pages

## [0.1.0] - 2026-01-30

### Added

**GUI System (Phase 2.11)**
- Main menu GUI accessible via `/f` command
- Faction dashboard with stats, quick actions, and navigation
- Interactive territory map with mouse-based chunk selection
- Faction settings page (rename, tag, description, color, recruitment)
- Relations management page with ally/enemy requests
- Member management with role changes and kick functionality
- New player flow: browse factions, create faction wizard, view invites
- Reusable modal components (color picker, input fields, confirmations)
- Navigation bar system with back button support
- Logs viewer for faction activity history

**Core Features**
- GitHub release update checker with automatic notifications (Phase 2.9)
- Spawnkill prevention with configurable invulnerability period (Phase 2.10)
- Per-zone flag configuration for WarZones/SafeZones (Phase 3.0)
- Item pickup protection in SafeZones and protected territories
- Teleport warmup damage cancellation system (Phase 2.8)
- World map overlay system with claim visualization
- Banner notifications for territory entry/exit
- Public API expansion with EconomyAPI interface
- Join request system for closed factions
- ChatManager for faction/ally chat channels

**Faction System**
- Faction creation, management, and deletion
- Territory claiming with power mechanics
- Faction roles: LEADER, OFFICER, MEMBER with granular permissions
- Diplomatic relations: ALLY, NEUTRAL, ENEMY
- Combat tagging system to prevent logout during combat
- Territory protection and safe zones
- Power-based claim limits (power regenerates over time)
- 42 commands for faction management
- HyperPerms integration for permission checks

**Testing Infrastructure**
- Unit tests for core data classes (ChunkKey, Faction, CombatTag, PlayerPower)
- Manager tests (ClaimManager, CombatTagManager, PowerManager, RelationManager)
- Protection system tests (ProtectionChecker)
- Test utilities: MockStorage, TestFactionFactory, TestPlayerFactory

**Technical Improvements**
- TeleportContext object for simplified teleport callbacks
- Auto-save system (30-minute intervals)
- Invite cleanup task for expired invitations
- Faction claim reverse index for O(1) lookups
- Zone flags system with 11 configurable flags
- Economy foundation with FactionEconomy and EconomyManager
- Territory ticking system for periodic updates

### Fixed
- Crash bug from improper UI element handling
- Help formatting standardized to match HyperPerms style
- `/f home` command now provides proper user feedback
- Promotion logic error preventing officer promotions
- Overclaim power check using wrong comparison operator
- SafeZone item pickup exploit allowing item theft
- Ally acceptance logging - both sides now get proper actor attribution
- Zone creation validation - cannot create zones on claimed chunks
- GUI navigation stability with nav bar fixes
- Chat system improvements

### Changed
- Refactored all GUIs into organized package structure (admin/, faction/, newplayer/, shared/)
- Territory map redesigned with mouse-based interaction (replaced button navigation)
- Improved HyperPerms integration reliability
- Enhanced faction relations display with visual indicators
- Improved TeleportManager API with context object pattern
- Enhanced ClaimManager with reverse index for performance
