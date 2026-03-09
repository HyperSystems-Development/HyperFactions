# HyperFactions GUI System

> **Version**: 0.11.0 | **~76 pages** across **3 registries**

Architecture documentation for the HyperFactions GUI system using Hytale's CustomUI.

## Overview

HyperFactions uses Hytale's `InteractiveCustomUIPage` system with:

- **GuiManager** - Central coordinator (page registration + delegation to openers)
- **3 Page Openers** - FactionPageOpener, AdminPageOpener, NewPlayerPageOpener
- **3 Page Registries** - Type-safe navigation between pages
- **UIPaths** - Centralized UI template path constants
- **NavBarUtil + NavEntry** - Shared navigation bar logic
- **Data Models** - Records for page state
- **Shared Components** - Reusable modals and UI elements
- **Help System** - Integrated help pages
- **Real-Time Updates** - ActivePageTracker for live data refresh

## Navigation Flows

```mermaid
stateDiagram-v2
    [*] --> HasFaction: /f (has faction)
    [*] --> NoFaction: /f (no faction)
    [*] --> Admin: /f admin

    state HasFaction {
        FactionMain --> Members
        FactionMain --> Relations
        FactionMain --> Territory
        FactionMain --> Settings
        FactionMain --> Economy
        FactionMain --> Help
    }

    state NoFaction {
        NewPlayerMain --> CreateFaction
        NewPlayerMain --> BrowseFactions
        NewPlayerMain --> ViewInvites
        NewPlayerMain --> NewPlayerHelp
    }

    state Admin {
        AdminMain --> FactionsList
        AdminMain --> ZoneManagement
        AdminMain --> ConfigEditor
        AdminMain --> BackupManager
        AdminMain --> DebugTools
    }
```

## Architecture

```
GuiManager (registration + delegation)
     │
     ├─► FactionPageOpener (35 methods)
     │        ├─► FactionMainPage (dashboard)
     │        ├─► FactionMembersPage
     │        ├─► FactionRelationsPage
     │        ├─► FactionSettingsPage
     │        ├─► TreasuryPage
     │        └─► ... (15+ pages)
     │
     ├─► NewPlayerPageOpener (8 methods)
     │        ├─► NewPlayerBrowsePage
     │        ├─► CreateFactionPage
     │        ├─► InvitesPage
     │        ├─► HelpPage
     │        └─► ... (5+ pages)
     │
     ├─► AdminPageOpener (38 methods)
     │        ├─► AdminMainPage
     │        ├─► AdminZoneMapPage
     │        ├─► AdminFactionsPage
     │        └─► ... (12+ pages)
     │
     └─► Shared Components
              ├─► UIPaths (centralized template paths)
              ├─► NavBarUtil + NavEntry (shared nav logic)
              ├─► InputModal
              ├─► ColorPickerModal
              └─► ConfirmationModal
```

## Key Classes

| Class | Path | Purpose |
|-------|------|---------|
| GuiManager | [`gui/GuiManager.java`](../src/main/java/com/hyperfactions/gui/GuiManager.java) | Central coordinator (registration + delegation) |
| FactionPageOpener | [`gui/FactionPageOpener.java`](../src/main/java/com/hyperfactions/gui/FactionPageOpener.java) | Faction page opening (35 methods) |
| AdminPageOpener | [`gui/AdminPageOpener.java`](../src/main/java/com/hyperfactions/gui/AdminPageOpener.java) | Admin page opening (38 methods) |
| NewPlayerPageOpener | [`gui/NewPlayerPageOpener.java`](../src/main/java/com/hyperfactions/gui/NewPlayerPageOpener.java) | New player page opening (8 methods) |
| UIPaths | [`gui/UIPaths.java`](../src/main/java/com/hyperfactions/gui/UIPaths.java) | Centralized UI template path constants |
| GuiType | [`gui/GuiType.java`](../src/main/java/com/hyperfactions/gui/GuiType.java) | Page type enumeration |
| FactionPageRegistry | [`gui/faction/FactionPageRegistry.java`](../src/main/java/com/hyperfactions/gui/faction/FactionPageRegistry.java) | Faction page navigation |
| NewPlayerPageRegistry | [`gui/newplayer/NewPlayerPageRegistry.java`](../src/main/java/com/hyperfactions/gui/newplayer/NewPlayerPageRegistry.java) | New player page navigation |
| AdminPageRegistry | [`gui/admin/AdminPageRegistry.java`](../src/main/java/com/hyperfactions/gui/admin/AdminPageRegistry.java) | Admin page navigation |
| NavBarUtil | [`gui/shared/NavBarUtil.java`](../src/main/java/com/hyperfactions/gui/shared/NavBarUtil.java) | Shared nav bar button builder |
| NavEntry | [`gui/shared/NavEntry.java`](../src/main/java/com/hyperfactions/gui/shared/NavEntry.java) | Navigation entry interface |

## GuiManager

[`gui/GuiManager.java`](../src/main/java/com/hyperfactions/gui/GuiManager.java)

Central coordinator that handles page registration and delegates page opening to focused opener classes:

```java
public class GuiManager {

    private final Supplier<FactionManager> factionManager;
    // ... other manager suppliers

    private final FactionPageOpener factionPageOpener;
    private final AdminPageOpener adminPageOpener;
    private final NewPlayerPageOpener newPlayerPageOpener;

    public GuiManager(...) {
        // Register pages with all three registries
        registerPages();
        registerNewPlayerPages();
        registerAdminPages();

        // Initialize page opener delegates
        this.factionPageOpener = new FactionPageOpener(this);
        this.adminPageOpener = new AdminPageOpener(this);
        this.newPlayerPageOpener = new NewPlayerPageOpener(this);
    }

    // All openXxx() methods delegate to the appropriate opener
    public void openFactionMain(Player player, Ref<EntityStore> ref,
                                Store<EntityStore> store, PlayerRef playerRef) {
        factionPageOpener.openFactionMain(player, ref, store, playerRef);
    }

    public void openAdminMain(Player player, Ref<EntityStore> ref,
                              Store<EntityStore> store, PlayerRef playerRef) {
        adminPageOpener.openAdminMain(player, ref, store, playerRef);
    }
}
```

## Page Flows

### Faction Member Flow

For players who belong to a faction:

```
FactionMainPage (dashboard)
     │
     ├─► FactionMembersPage
     │        └─► PlayerInfoPage
     │             └─► TransferConfirmPage / LeaveConfirmPage
     │
     ├─► FactionRelationsPage
     │        └─► SetRelationModalPage
     │
     ├─► FactionSettingsPage
     │        ├─► RenameModalPage
     │        ├─► DescriptionModalPage
     │        ├─► TagModalPage
     │        └─► ColorPickerPage
     │
     ├─► ChunkMapPage
     │
     ├─► FactionBrowserPage
     │        └─► FactionInfoPage (other faction)
     │
     ├─► LogsViewerPage
     │
     └─► FactionHelpPage
```

### New Player Flow

For players without a faction:

```
MainMenuPage
     │
     ├─► CreateFactionStep1Page
     │        └─► CreateFactionStep2Page
     │
     ├─► InvitesPage
     │        └─► Accept invite → joins faction
     │
     ├─► NewPlayerBrowsePage
     │        └─► Request to join
     │
     ├─► NewPlayerMapPage
     │
     └─► HelpPage
```

### Admin Flow

For players with admin permission:

```
AdminMainPage
     │
     ├─► AdminDashboardPage (stats)
     │
     ├─► AdminFactionsPage
     │        ├─► AdminFactionInfoPage
     │        ├─► AdminFactionMembersPage
     │        ├─► AdminFactionRelationsPage
     │        ├─► AdminFactionSettingsPage
     │        └─► AdminDisbandConfirmPage
     │
     ├─► AdminZoneMapPage
     │        ├─► CreateZoneWizardPage
     │        └─► AdminZoneSettingsPage
     │                 └─► AdminZoneIntegrationFlagsPage
     │
     ├─► AdminConfigPage
     │
     ├─► AdminBackupsPage
     │
     ├─► AdminUpdatesPage
     │
     └─► AdminHelpPage
```

## Page Registry Pattern

Each flow uses a registry for type-safe navigation:

### FactionPageRegistry

[`gui/faction/FactionPageRegistry.java`](../src/main/java/com/hyperfactions/gui/faction/FactionPageRegistry.java)

```java
public class FactionPageRegistry {

    public enum Entry {
        MAIN,
        MEMBERS,
        RELATIONS,
        SETTINGS,
        MAP,
        BROWSER,
        LOGS,
        HELP,
        // ... modals
        PLAYER_INFO,
        RENAME_MODAL,
        COLOR_PICKER,
        DISBAND_CONFIRM,
        LEAVE_CONFIRM,
        TRANSFER_CONFIRM
    }

    public static void openPage(
            Entry entry,
            Player player,
            Ref<EntityStore> ref,
            Store<EntityStore> store,
            PlayerRef playerRef,
            Object... args) {

        InteractiveCustomUIPage page = createPage(entry, player, ref, store, playerRef, args);
        PageManager pageManager = player.getPageManager();
        pageManager.openPage(page);
    }

    private static InteractiveCustomUIPage createPage(Entry entry, ...) {
        return switch (entry) {
            case MAIN -> new FactionMainPage(playerRef, ref, store);
            case MEMBERS -> new FactionMembersPage(playerRef, ref, store);
            case RELATIONS -> new FactionRelationsPage(playerRef, ref, store);
            // ...
        };
    }
}
```

## Page Implementation

### Base Pattern

Each page extends `InteractiveCustomUIPage`:

```java
public class FactionMainPage extends InteractiveCustomUIPage {

    private final PlayerRef playerRef;
    private final Ref<EntityStore> ref;
    private final Store<EntityStore> store;

    public FactionMainPage(PlayerRef playerRef, Ref<EntityStore> ref, Store<EntityStore> store) {
        super("hyperfactions:faction_main"); // UI definition ID
        this.playerRef = playerRef;
        this.ref = ref;
        this.store = store;
    }

    @Override
    public void init(Data data) {
        // Populate initial page data
        FactionMainData pageData = buildData();
        data.set(pageData);
    }

    @Override
    public void handleEvent(String event, Data data) {
        // Handle button clicks
        switch (event) {
            case "members_clicked" -> navigateToMembers();
            case "relations_clicked" -> navigateToRelations();
            case "claim_clicked" -> performClaim();
            // ...
        }
    }

    private void navigateToMembers() {
        FactionPageRegistry.openPage(Entry.MEMBERS, player, ref, store, playerRef);
    }
}
```

### Data Records

Pages use records for their data models:

```java
// gui/faction/data/FactionMainData.java
public record FactionMainData(
    String factionName,
    String factionTag,
    String factionColor,
    int memberCount,
    int maxMembers,
    int claimCount,
    int maxClaims,
    double factionPower,
    double maxPower,
    boolean isLeader,
    boolean isOfficer,
    List<MemberEntry> onlineMembers
) {
    public record MemberEntry(
        String username,
        String role,
        boolean online
    ) {}
}
```

## Shared Components

### InputModal

[`gui/shared/component/InputModal.java`](../src/main/java/com/hyperfactions/gui/shared/component/InputModal.java)

Generic text input modal:

```java
public class InputModal {

    public static void show(
            Player player,
            String title,
            String placeholder,
            String currentValue,
            Consumer<String> onSubmit,
            Runnable onCancel) {

        // Open modal with callback handlers
    }
}
```

### ColorPickerModal

[`gui/shared/component/ColorPickerModal.java`](../src/main/java/com/hyperfactions/gui/shared/component/ColorPickerModal.java)

Color selection grid:

```java
public class ColorPickerModal {

    // 16 Minecraft color codes (0-9, a-f)
    private static final String[] COLORS = {
        "0", "1", "2", "3", "4", "5", "6", "7",
        "8", "9", "a", "b", "c", "d", "e", "f"
    };

    public static void show(
            Player player,
            String currentColor,
            Consumer<String> onSelect) {
        // Open color grid modal
    }
}
```

### ConfirmationModal

[`gui/shared/component/ConfirmationModal.java`](../src/main/java/com/hyperfactions/gui/shared/component/ConfirmationModal.java)

Yes/No confirmation dialog:

```java
public class ConfirmationModal {

    public static void show(
            Player player,
            String title,
            String message,
            String confirmText,
            String cancelText,
            Runnable onConfirm,
            Runnable onCancel) {
        // Open confirmation dialog
    }
}
```

## Navigation Pattern

### Forward Navigation

```java
// From FactionMainPage
private void onMembersClicked() {
    FactionPageRegistry.openPage(
        Entry.MEMBERS,
        player, ref, store, playerRef
    );
}
```

### Navigation with Arguments

```java
// Open player info for specific player
private void onMemberClicked(UUID targetUuid) {
    FactionPageRegistry.openPage(
        Entry.PLAYER_INFO,
        player, ref, store, playerRef,
        targetUuid // Additional argument
    );
}
```

### Back Navigation

```java
// Close current page (returns to previous)
private void onBackClicked() {
    player.getPageManager().closePage();
}
```

### Modal Flow

```java
// Show rename modal, then return to settings
private void onRenameClicked() {
    InputModal.show(
        player,
        "Rename Faction",
        "New name",
        currentName,
        newName -> {
            // Process rename
            factionManager.renameFaction(factionId, newName);
            // Refresh settings page
            FactionPageRegistry.openPage(Entry.SETTINGS, ...);
        },
        () -> {
            // Cancelled - stay on current page
        }
    );
}
```

## Page Directory Structure

```
gui/
├── GuiManager.java               # Central coordinator (registration + delegation)
├── FactionPageOpener.java        # Faction page opening methods (35 methods)
├── AdminPageOpener.java          # Admin page opening methods (38 methods)
├── NewPlayerPageOpener.java      # New player page opening methods (8 methods)
├── UIPaths.java                  # Centralized UI template path constants
├── GuiType.java                  # Page type enum
├── ActivePageTracker.java        # Live data refresh tracking
├── RefreshablePage.java          # Refreshable page interface
├── GuiUpdateService.java         # GUI update coordination
│
├── faction/                      # Faction member pages
│   ├── FactionPageRegistry.java  # Navigation registry
│   ├── NavBarHelper.java         # Faction navigation bar
│   ├── ChunkMapAsset.java        # Chunk map asset
│   ├── page/                     # Page implementations
│   │   ├── FactionMainPage.java
│   │   ├── FactionMembersPage.java
│   │   ├── FactionRelationsPage.java
│   │   ├── FactionSettingsPage.java
│   │   ├── FactionBrowserPage.java
│   │   ├── FactionDashboardPage.java
│   │   ├── FactionHelpPage.java
│   │   ├── FactionInvitesPage.java
│   │   ├── FactionModulesPage.java
│   │   ├── FactionChatPage.java
│   │   ├── FactionLeaderboardPage.java
│   │   ├── LogsViewerPage.java
│   │   ├── PlayerInfoPage.java
│   │   ├── TreasuryPage.java
│   │   ├── TreasuryDepositModalPage.java
│   │   ├── TreasurySettingsPage.java
│   │   ├── TreasuryTransferSearchPage.java
│   │   ├── TreasuryTransferConfirmPage.java
│   │   ├── SetRelationModalPage.java
│   │   ├── DisbandConfirmPage.java
│   │   ├── LeaveConfirmPage.java
│   │   ├── LeaderLeaveConfirmPage.java
│   │   └── TransferConfirmPage.java
│   └── data/                     # Data records
│       ├── FactionMainData.java
│       ├── FactionMembersData.java
│       ├── FactionRelationsData.java
│       └── ...
│
├── admin/                       # Admin pages (registry + pages + data)
│   ├── AdminPageRegistry.java
│   ├── AdminNavBarHelper.java
│   ├── page/                    # Admin page implementations
│   │   ├── AdminMainPage.java
│   │   ├── AdminDashboardPage.java
│   │   ├── AdminFactionsPage.java
│   │   ├── AdminFactionInfoPage.java
│   │   ├── AdminFactionMembersPage.java
│   │   ├── AdminFactionRelationsPage.java
│   │   ├── AdminFactionSettingsPage.java
│   │   ├── AdminPlayersPage.java
│   │   ├── AdminPlayerInfoPage.java
│   │   ├── AdminZoneMapPage.java
│   │   ├── AdminZonePage.java
│   │   ├── AdminZoneSettingsPage.java
│   │   ├── AdminZoneIntegrationFlagsPage.java
│   │   ├── CreateZoneWizardPage.java
│   │   ├── ZoneRenameModalPage.java
│   │   ├── ZoneChangeTypeModalPage.java
│   │   ├── AdminConfigPage.java
│   │   ├── AdminBackupsPage.java
│   │   ├── AdminUpdatesPage.java
│   │   ├── AdminActivityLogPage.java
│   │   ├── AdminActionsPage.java
│   │   ├── AdminEconomyPage.java
│   │   ├── AdminEconomyAdjustPage.java
│   │   ├── AdminVersionPage.java
│   │   ├── AdminZonePropertiesPage.java
│   │   ├── AdminHelpPage.java
│   │   ├── AdminDisbandConfirmPage.java
│   │   └── AdminUnclaimAllConfirmPage.java
│   └── data/                    # Admin data records
│       ├── AdminMainData.java
│       ├── AdminDashboardData.java
│       └── ...
│
├── newplayer/                   # New player flow (registry + pages + data)
│   ├── NewPlayerPageRegistry.java  # New player navigation
│   ├── NewPlayerNavBarHelper.java  # New player navigation bar
│   ├── page/                    # New player page implementations
│   │   ├── CreateFactionPage.java
│   │   ├── InvitesPage.java
│   │   ├── HelpPage.java
│   │   ├── NewPlayerMapPage.java
│   │   └── NewPlayerBrowsePage.java
│   └── data/                    # New player data models
│       └── NewPlayerPageData.java
│
├── shared/                      # Shared components
│   ├── NavEntry.java            # Navigation entry interface
│   ├── NavBarUtil.java          # Shared nav bar button builder
│   ├── component/
│   │   ├── InputModal.java
│   │   └── ConfirmationModal.java
│   ├── page/
│   │   ├── MainMenuPage.java
│   │   ├── FactionInfoPage.java
│   │   ├── PlaceholderPage.java
│   │   ├── RenameModalPage.java
│   │   ├── DescriptionModalPage.java
│   │   └── TagModalPage.java
│   └── data/
│       ├── NavAwareData.java
│       ├── MainMenuData.java
│       └── ...
│
├── help/                        # Help system
│   ├── HelpCategory.java
│   ├── HelpTopic.java
│   ├── HelpRegistry.java
│   ├── data/
│   │   └── HelpPageData.java
│   └── page/
│       └── HelpMainPage.java
│
└── test/                        # Test pages
    └── ButtonTestPage.java
```

## Permission Checks in GUI

Pages check permissions before sensitive operations:

```java
public class FactionSettingsPage extends InteractiveCustomUIPage {

    @Override
    public void handleEvent(String event, Data data) {
        if (event.equals("disband_clicked")) {
            // Check if player is leader
            if (!isLeader(playerRef.getUuid())) {
                showError("Only the faction leader can disband.");
                return;
            }

            // Check permission
            if (!hasPermission(playerRef.getUuid(), Permissions.DISBAND)) {
                showError("You don't have permission to disband.");
                return;
            }

            // Show confirmation
            FactionPageRegistry.openPage(Entry.DISBAND_CONFIRM, ...);
        }
    }
}
```

## New Pages in v0.10.0

### Admin Pages

#### AdminActionsPage
Global admin quick actions: K/D reset (per-player and server-wide), bulk operations. Accessible from admin nav bar.

#### AdminActivityLogPage
Server-wide faction activity browser. Aggregates logs across all factions with filters for log type, player name, and time range (1h/24h/7d/all). Paginated with expandable entries showing actor, target, and details.

#### AdminEconomyPage
Server economy overview with sortable faction balance list. Shows total server economy, average balance, and per-faction treasury details. Links to AdminEconomyAdjustPage for individual adjustments.

#### AdminEconomyAdjustPage
Per-faction treasury adjustment modal. Supports set, add, and remove operations with admin audit logging. Opened from AdminEconomyPage.

#### AdminVersionPage
Displays mod version, server version, build info, and integration status for all 12 supported mods (HyperPerms, LuckPerms, VaultUnlocked, Ecotale, PAPI, WiFlow, OrbisGuard, HyperProtect-Mixin, OG-Mixins, Gravestones, HyBounty, MultipleHUD). Green/red status indicators.

#### AdminZonePropertiesPage
Consolidated zone property editor. Edit zone name, type (SafeZone/WarZone), and notification settings (entry/leave title suppression, custom text) in a single page. Opened from AdminZonePage.

### Faction Pages

#### FactionLeaderboardPage
Sortable faction leaderboard with 5 sort modes: K/D (default), Power, Territory, Balance, Members. 10 entries per page with pagination. Top 3 get gold/silver/bronze rank colors. Own faction row highlighted. Accessible from nav bar and `/f leaderboard`.

#### TreasuryPage
Faction treasury management hub. Shows balance, recent transactions, and autopay status. Links to deposit, withdrawal, transfer, and settings sub-pages. Requires economy integration (Ecotale/VaultUnlocked).

#### TreasuryDepositModalPage
Deposit modal with amount input and confirmation. Validates against player balance. Shows current treasury and player balance.

#### TreasurySettingsPage
Treasury autopay and access settings. Configure auto-deposit percentage, withdrawal permissions per role, and transfer limits.

#### TreasuryTransferSearchPage
Inter-faction transfer search. Browse and search target factions for treasury transfers. Shows faction names with balance preview.

#### TreasuryTransferConfirmPage
Transfer confirmation modal. Shows source faction, target faction, amount, and fee (if configured). Requires officer+ permission.

## Adding New Pages

1. **Create data record** in appropriate `data/` package:
   ```java
   public record NewFeatureData(
       String title,
       List<ItemEntry> items
   ) {}
   ```

2. **Create page class** in appropriate `page/` package:
   ```java
   public class NewFeaturePage extends InteractiveCustomUIPage {
       // Implementation
   }
   ```

3. **Add to registry** enum and switch:
   ```java
   // In FactionPageRegistry
   public enum Entry {
       // ...
       NEW_FEATURE
   }

   private static InteractiveCustomUIPage createPage(Entry entry, ...) {
       return switch (entry) {
           // ...
           case NEW_FEATURE -> new NewFeaturePage(playerRef, ref, store);
       };
   }
   ```

4. **Add navigation** from existing pages:
   ```java
   private void onNewFeatureClicked() {
       FactionPageRegistry.openPage(Entry.NEW_FEATURE, ...);
   }
   ```

## Code Links

| Class | Path |
|-------|------|
| GuiColors | [`gui/GuiColors.java`](../src/main/java/com/hyperfactions/gui/GuiColors.java) |
| GuiManager | [`gui/GuiManager.java`](../src/main/java/com/hyperfactions/gui/GuiManager.java) |
| FactionPageOpener | [`gui/FactionPageOpener.java`](../src/main/java/com/hyperfactions/gui/FactionPageOpener.java) |
| AdminPageOpener | [`gui/AdminPageOpener.java`](../src/main/java/com/hyperfactions/gui/AdminPageOpener.java) |
| NewPlayerPageOpener | [`gui/NewPlayerPageOpener.java`](../src/main/java/com/hyperfactions/gui/NewPlayerPageOpener.java) |
| UIPaths | [`gui/UIPaths.java`](../src/main/java/com/hyperfactions/gui/UIPaths.java) |
| FactionPageRegistry | [`gui/faction/FactionPageRegistry.java`](../src/main/java/com/hyperfactions/gui/faction/FactionPageRegistry.java) |
| NewPlayerPageRegistry | [`gui/newplayer/NewPlayerPageRegistry.java`](../src/main/java/com/hyperfactions/gui/newplayer/NewPlayerPageRegistry.java) |
| AdminPageRegistry | [`gui/admin/AdminPageRegistry.java`](../src/main/java/com/hyperfactions/gui/admin/AdminPageRegistry.java) |
| NavBarUtil | [`gui/shared/NavBarUtil.java`](../src/main/java/com/hyperfactions/gui/shared/NavBarUtil.java) |
| NavEntry | [`gui/shared/NavEntry.java`](../src/main/java/com/hyperfactions/gui/shared/NavEntry.java) |
| NavBarHelper | [`gui/faction/NavBarHelper.java`](../src/main/java/com/hyperfactions/gui/faction/NavBarHelper.java) |
| InputModal | [`gui/shared/component/InputModal.java`](../src/main/java/com/hyperfactions/gui/shared/component/InputModal.java) |
