# GUI Redesign Progress

> Tracks completed work and remaining items for the Native Hytale Style redesign.
>
> Branch: `gui/hytaleNative` | Base: `main` (v0.8.1)

---

## Phase Status

| Phase | Feature | Status | Remaining |
|-------|---------|--------|-----------|
| 1 | Styles & Buttons | **COMPLETE** | — |
| 2 | Containers & Separators | **PARTIAL** | Container/Panel/SimpleContainer templates not adopted |
| 3 | Input Fields | **PARTIAL** | MultilineTextField, CompactTextField not adopted |
| 4 | Scrollbars | **COMPLETE** | — |
| 5 | Tooltips | **PARTIAL** | Only nav, dashboard, settings pages covered (~8 files) |
| 6 | Help Page | **COMPLETE** | — |
| 7 | Color Standardization | **NOT STARTED** | 259 hardcoded hex values across 75 files |

---

## Phase 1: Styles & Buttons — COMPLETE

### What was done

- **Removed** all custom color-variant button styles from `styles.ui`:
  `@ButtonStyle`, `@CyanButtonStyle`, `@GreenButtonStyle`, `@GoldButtonStyle`, `@RedButtonStyle`, `@FlatRedButtonStyle`, `@FlatDarkRedButtonStyle`, `@DisabledButtonStyle`, `@SmallTealButtonStyle`, `@SmallPurpleButtonStyle`

- **Retained** 2 custom styles with no native equivalent:
  - `@InvisibleButtonStyle` — transparent click overlays (chunk map cells, list row targets)
  - `@PlayerMarkerButtonStyle` — white bold centered text for map player markers

- **Migrated** all .ui files to native styles:
  - `$C.@SecondaryTextButtonStyle` — 191 occurrences across 77 files (standard actions)
  - `$C.@CancelTextButtonStyle` — 45 occurrences across 31 files (destructive actions)
  - `$C.@DefaultTextButtonStyle` — 8 occurrences across 3 files (active tab states)

- **Updated** 5 Java files (12 `Value.ref()` calls):
  - `AdminZonePage.java` — 3 tab style refs
  - `FactionDashboardPage.java` — 3 action button refs (Home, Claim, Leave)
  - `FactionRelationsPage.java` — 2 tab style refs
  - `FactionInvitesPage.java` — 2 tab style refs
  - `FactionChatPage.java` — 2 tab style refs

### Style mapping applied

| Old Custom | New Native | Usage |
|------------|-----------|-------|
| `$S.@ButtonStyle` | `$C.@SecondaryTextButtonStyle` | Default actions |
| `$S.@CyanButtonStyle` | `$C.@SecondaryTextButtonStyle` | Brand-accent buttons |
| `$S.@GreenButtonStyle` | `$C.@SecondaryTextButtonStyle` | Success/confirm |
| `$S.@GoldButtonStyle` | `$C.@SecondaryTextButtonStyle` | Premium/leader |
| `$S.@RedButtonStyle` | `$C.@CancelTextButtonStyle` | Destructive |
| `$S.@FlatRedButtonStyle` | `$C.@CancelTextButtonStyle` | Danger zone |
| `$S.@DisabledButtonStyle` | `$C.@DefaultTextButtonStyle` (active tab) / `$C.@SecondaryTextButtonStyle` + `Disabled: true` | Locked state |
| `$S.@SmallTealButtonStyle` | `$C.@SecondaryTextButtonStyle` | Admin tabs |
| `$S.@SmallPurpleButtonStyle` | `$C.@SecondaryTextButtonStyle` | Admin tabs |

### Remaining `$S.@` references (intentional)

13 files still reference `$S.@InvisibleButtonStyle` — these are transparent click targets for chunk map cells and list row overlays. This is correct and must not be changed.

---

## Phase 2: Containers & Separators — PARTIAL

### What was done

- **`$C.@ContentSeparator`** — 29 occurrences across 5 files:
  - `faction_settings.ui` (10), `admin_faction_settings.ui` (9), `faction_dashboard.ui` (1), `newplayer/create_faction.ui` (8), `treasury_transfer_confirm.ui` (1)

- **`$C.@Subtitle`** — 29 occurrences across 4 files:
  - `faction_settings.ui` (10), `admin_faction_settings.ui` (9), `faction_dashboard.ui` (2), `newplayer/create_faction.ui` (8)

### What remains

- **`$C.@SimpleContainer`** — 0 uses. Stat cards and info boxes still use raw `Group { Background }`.
- **`$C.@Container`** — 0 uses. Major content sections not wrapped.
- **`$C.@Panel`** — 0 uses. Form sections not wrapped.
- Some `Background: (Color: #1a2a3a)` patterns remain in map files and style definitions (acceptable).

---

## Phase 3: Input Fields — PARTIAL

### What was done

- **`PlaceholderText`** — 29 occurrences across 21 files. All text inputs have placeholder hints:
  - Search fields, create form inputs, modal text fields, admin search/filter fields

### What remains

- **`$C.@MultilineTextField`** — 0 uses. Description fields still use standard TextField.
- **`CompactTextField`** — 0 uses. Search fields use standard TextField with PlaceholderText.

---

## Phase 4: Scrollbars — COMPLETE

### What was done

- **`$C.@TranslucentScrollbarStyle`** — 12 occurrences across 12 files (player-facing content)
- **`$C.@DefaultScrollbarStyle`** — 8 occurrences across 8 files (admin dense data)

Scrollbar style selection follows the design plan: translucent for player pages, visible for admin data tables.

---

## Phase 5: Tooltips — PARTIAL

### What was done

- **`TooltipText`** + **`$C.@DefaultTextTooltipStyle`** — 29 occurrences across 8 files:
  - `faction_settings.ui` (10) — setting field descriptions
  - `faction_dashboard.ui` (9) — stat card explanations
  - `nav_button.ui` — navigation tab labels
  - `main_menu_*.ui` (5 files) — menu item descriptions

### What remains

- Member list tooltips (role badges, action buttons)
- Relations page tooltips (relation type badges)
- Invites page tooltips (accept/reject)
- Territory/map tooltips (zoom, mode, legend)
- Confirmation modal tooltips (danger warnings)
- Admin page tooltips (zone flags, config options)
- All other interactive elements per the [05-tooltips.md](05-tooltips.md) design doc

---

## Phase 6: Help Page — COMPLETE

### What was done

- Sidebar layout with 7 color-coded categories using native Hytale button backgrounds (`$C.@DefaultSquareButton*Background`)
- Category-specific `LabelStyle` variants for disabled state (semantic colors)
- Content area with topic cards and scrollable content
- `$C.@TranslucentScrollbarStyle` on content area

---

## Phase 7: Color Standardization — NOT STARTED

### Current state

- **0** uses of `$C.@ColorDefault`, `$C.@ColorGrayCaption`, `$C.@ColorDefaultLabel`, `$C.@ColorBlueAccent`
- **259** hardcoded hex color values across 75 files
- Breakdown: ~228 `#888888`, ~31 `#FFFFFF` as TextColor, plus `#666666`, `#555555`, `#AAAAAA`, `#CCCCCC`, etc.

### Faction-specific colors to keep (no native equivalent)

`#00FFFF` (cyan), `#00AAAA` (teal), `#FFD700` (gold), `#87CEEB` (sky blue), `#44CC44` (green), `#FF5555` (red), `#FFAA00` (orange), `#00AAFF` (ally blue), `#4a9eff` (player blue)

---

## Files Modified

### .ui templates (97 files)

**admin/** (22): `admin_backups.ui`, `admin_config.ui`, `admin_dashboard.ui`, `admin_economy.ui`, `admin_economy_adjust.ui`, `admin_economy_entry.ui`, `admin_faction_entry.ui`, `admin_faction_info.ui`, `admin_faction_members.ui`, `admin_faction_members_entry.ui`, `admin_faction_relations.ui`, `admin_faction_relations_entry.ui`, `admin_faction_settings.ui`, `admin_factions.ui`, `admin_help.ui`, `admin_main.ui`, `admin_player_entry.ui`, `admin_player_info.ui`, `admin_players.ui`, `admin_updates.ui`, `admin_zone_entry.ui`, `admin_zone_map.ui`, `admin_zones.ui`, `create_zone_wizard.ui`, `unclaim_all_confirm.ui`, `zone_change_type_modal.ui`, `zone_rename_modal.ui`

**faction/** (35): `chunk_map.ui`, `chunk_map_terrain.ui`, `dashboard_action_btn.ui`, `dashboard_action_btn_disabled.ui`, `dashboard_stat_card.ui`, `faction_actions.ui`, `faction_browse_entry.ui`, `faction_browser.ui`, `faction_card.ui`, `faction_chat.ui`, `faction_dashboard.ui`, `faction_invite_entry.ui`, `faction_invites.ui`, `faction_members.ui`, `faction_modules.ui`, `faction_relation_entry.ui`, `faction_relations.ui`, `faction_settings.ui`, `faction_treasury.ui`, `history_entry.ui`, `invite_entry.ui`, `member_entry.ui`, `player_info.ui`, `relation_btn_accept.ui`, `relation_btn_ally.ui`, `relation_btn_cancel.ui`, `relation_btn_decline.ui`, `relation_btn_enemy.ui`, `relation_btn_neutral.ui`, `relation_entry.ui`, `relation_set_btn.ui`, `request_entry.ui`, `set_relation_card.ui`, `set_relation_modal.ui`, `settings_danger_zone.ui`, `transfer_confirm.ui`, `transfer_search_entry.ui`, `treasury_deposit_modal.ui`, `treasury_settings.ui`, `treasury_transfer_confirm.ui`, `treasury_transfer_search.ui`

**shared/** (15): `description_modal.ui`, `disband_confirm.ui`, `error_page.ui`, `faction_info.ui`, `invite_notification.ui`, `leader_leave_confirm.ui`, `leave_confirm.ui`, `main_menu.ui`, `main_menu_admin.ui`, `main_menu_browse.ui`, `main_menu_faction.ui`, `main_menu_no_faction.ui`, `main_menu_territory.ui`, `menu_section.ui`, `no_faction_actions.ui`, `placeholder_page.ui`, `rename_modal.ui`, `styles.ui`, `tag_modal.ui`

**nav/** (2): `nav_button.ui`, `nav_button_active.ui`

**newplayer/** (9): `browse.ui`, `create_faction.ui`, `faction_card.ui`, `help.ui`, `invite_card.ui`, `invites.ui`, `invites_empty.ui`, `map_readonly.ui`, `newplayer_faction_entry.ui`, `request_card.ui`

**help/** (1): `help_main.ui`

**test/** (1): `button_test.ui`

### Java files (5)

- `AdminZonePage.java` — tab style migrations
- `FactionDashboardPage.java` — action button style migrations
- `FactionRelationsPage.java` — tab style migrations
- `FactionInvitesPage.java` — tab style migrations
- `FactionChatPage.java` — tab style migrations
