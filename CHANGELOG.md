# Changelog

All notable changes to HyperFactions will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

*No changes yet*

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

**Closes:** [#30](https://github.com/HyperSystemsDev/HyperFactions/issues/30), [#31](https://github.com/HyperSystemsDev/HyperFactions/issues/31), [#32](https://github.com/HyperSystemsDev/HyperFactions/issues/32), [#34](https://github.com/HyperSystemsDev/HyperFactions/issues/34), [#36](https://github.com/HyperSystemsDev/HyperFactions/issues/36)

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

**Faction Treasury & Economy System** ([#36](https://github.com/HyperSystemsDev/HyperFactions/issues/36))
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

**Player Info & Membership History** ([#32](https://github.com/HyperSystemsDev/HyperFactions/issues/32))
- **Player info page** (`/f who [player]`): Full GUI page with faction membership, power stats, combat stats (kills/deaths/KDR), and membership history
- **Membership history tracking**: Records all faction joins, leaves, kicks, and disbands with timestamps, highest role achieved, and leave reason
- **Player data expansion**: New `PlayerData` model with `firstJoined`, `lastOnline`, kills, deaths, username caching, and membership history — backwards-compatible with existing player JSON files
- "View Profile" button on members page to navigate to player info
- `/f admin clearhistory <player>` command to clear a player's membership history
- **Upgrade migration**: Existing faction members automatically get an active membership record on first startup, using their actual faction join date and current role
- First joined / last online tracking displayed on the player info page header
- `maxMembershipHistory` config option (default: 10) to cap history records per player

**Admin Power Management** ([#34](https://github.com/HyperSystemsDev/HyperFactions/issues/34))
- **Admin power command** (`/f admin power`): Full admin control over player power — set, adjust, reset, per-player max override, power loss bypass, claim decay exemption
- **Admin Player Info page**: GUI page accessible from admin member list with power controls, bypass toggles, and kick/promote/demote actions
- **Faction bulk power operations**: Reset All / +/-1/5 All buttons on AdminFactionInfoPage
- `ADMIN_POWER` log type for faction activity logs
- `hyperfactions.admin.power` permission node

**Terrain Map Mode** ([#30](https://github.com/HyperSystemsDev/HyperFactions/issues/30))
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

**Faction-Aware Map & Compass Visibility** ([#31](https://github.com/HyperSystemsDev/HyperFactions/issues/31))
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

**GravestonePlugin Integration**
> **Note**: The gravestone integration requires a new release of [GravestonePlugin](https://github.com/zurkubusiness/gravestones) that includes the v2 API with AccessChecker and events. This is pending review in [zurkubusiness/gravestones#2](https://github.com/zurkubusiness/gravestones/pull/2). Until that PR is merged and released, the integration will detect the plugin but the AccessChecker will not be registered.

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
- [#7](https://github.com/HyperSystemsDev/HyperFactions/issues/7) — Announcement system for major faction events
- [#16](https://github.com/HyperSystemsDev/HyperFactions/issues/16) — Add data import from ElbaphFactions
- [#18](https://github.com/HyperSystemsDev/HyperFactions/issues/18) — Integrate with Better Scoreboard for faction placeholders
- [#22](https://github.com/HyperSystemsDev/HyperFactions/issues/22) — Hide GUI buttons when player lacks permission
- [#23](https://github.com/HyperSystemsDev/HyperFactions/issues/23) — Add Placeholders for scoreboards, holograms, and menus

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
