# Page Migration Guide

> Per-page checklist for applying all redesign phases

## How to Use

For each page, apply the applicable phases in order (1 → 7). Check off each item as completed. Not all phases apply to every page.

---

## High-Impact Pages (redesign first)

### FactionDashboardPage

**Files**: `faction/faction_dashboard.ui`, `faction/dashboard_stat_card.ui`, `faction/dashboard_action_btn.ui`, `faction/dashboard_action_btn_disabled.ui`, `faction/history_entry.ui`, `FactionDashboardPage.java`

| Phase | Change | Status |
|-------|--------|--------|
| 1 | Button styles → `$C.@SecondaryTextButtonStyle` | |
| 1 | `Value.ref()` in Java → native style refs | |
| 2 | Stat card Groups → `$C.@SimpleContainer` | |
| 2 | Activity feed wrapper → `$C.@Panel` | |
| 2 | Quick actions wrapper → `$C.@Panel` | |
| 2 | Dividers → `$C.@ContentSeparator` | |
| 4 | Activity feed scrollbar → `$C.@TranslucentScrollbarStyle` | |
| 5 | Tooltips on stat cards (dynamic: power, members, territory values) | |
| 5 | Tooltips on quick action buttons | |
| 7 | `#888888` → `$C.@ColorGrayCaption`, `#FFFFFF` → `$C.@ColorDefault` | |

### CreateFactionPage

**Files**: `newplayer/create_faction.ui`, `CreateFactionPage.java`

| Phase | Change | Status |
|-------|--------|--------|
| 1 | "Create Faction" button: `$C.@DefaultTextButtonStyle` (primary CTA) | |
| 1 | Cancel button: `$C.@CancelTextButtonStyle` | |
| 2 | Form sections → `$C.@Panel` with `$C.@Subtitle` headers | |
| 3 | Name input: `PlaceholderText: "Enter faction name..."` | |
| 3 | Tag input: `PlaceholderText: "2-4 chars"` | |
| 3 | Description: `$C.@MultilineTextField` + `PlaceholderText` | |
| 5 | Tooltips on form fields (requirements, limits) | |
| 7 | Color standardization | |

### FactionSettingsPage

**Files**: `faction/faction_settings.ui`, `faction/settings_danger_zone.ui`, `FactionSettingsPage.java`

| Phase | Change | Status |
|-------|--------|--------|
| 1 | Edit buttons → `$C.@SecondaryTextButtonStyle` (or `$C.@SmallSecondaryTextButton`) | |
| 1 | Disband → `$C.@CancelTextButtonStyle` | |
| 1 | Java `Value.ref()` → native refs | |
| 2 | Settings sections → `$C.@Panel` | |
| 2 | Danger zone → `$C.@Panel` | |
| 2 | Section headers → `$C.@Subtitle` + `$C.@ContentSeparator` | |
| 5 | Tooltips on all settings (name, tag, description, toggles) | |
| 5 | Tooltip on disband: `"Permanently delete this faction"` | |
| 5 | Tooltip on server-locked options: `"Controlled by server admin"` | |
| 7 | Color standardization | |

### HelpMainPage

**Files**: `help/help_main.ui`, `HelpMainPage.java`

| Phase | Change | Status |
|-------|--------|--------|
| 1 | Category buttons → `$C.@SecondaryTextButtonStyle` | |
| 1 | Remove 130+ lines of custom `@CatStyle*` definitions | |
| 2 | Topic cards → `$C.@Panel` | |
| 4 | Content scrollbar → `$C.@TranslucentScrollbarStyle` | |
| 5 | Tooltips on category buttons | |
| 6 | Full redesign: UI Gallery-inspired layout, accent bars, gradient titles | |
| 7 | Color standardization (keep category accent colors) | |

### FactionBrowserPage

**Files**: `faction/faction_browser.ui`, `faction/faction_browse_entry.ui`, `FactionBrowserPage.java`

| Phase | Change | Status |
|-------|--------|--------|
| 1 | Sort/filter buttons → `$C.@SecondaryTextButtonStyle` | |
| 2 | Search area → `$C.@Panel` | |
| 2 | Results wrapper → `$C.@Container` | |
| 3 | Search field: `PlaceholderText: "Search factions..."` | |
| 3 | Consider `CompactTextField` or `$C.@HeaderSearch` | |
| 4 | Results scrollbar → `$C.@TranslucentScrollbarStyle` | |
| 5 | Tooltips on sort options | |
| 7 | Color standardization | |

### MainMenuPage

**Files**: `shared/main_menu.ui`, `shared/main_menu_faction.ui`, `shared/main_menu_no_faction.ui`, `shared/main_menu_admin.ui`, `shared/main_menu_browse.ui`, `shared/main_menu_territory.ui`, `shared/menu_section.ui`

| Phase | Change | Status |
|-------|--------|--------|
| 1 | All menu buttons → `$C.@SecondaryTextButtonStyle` | |
| 2 | Menu sections → `$C.@Panel` or `$C.@SimpleContainer` | |
| 2 | Section dividers → `$C.@ContentSeparator` | |
| 5 | Tooltips on menu items | |
| 7 | Color standardization | |

---

## Medium-Impact Pages

### FactionMembersPage

**Files**: `faction/faction_members.ui`, `faction/member_entry.ui`, `faction/player_info.ui`

| Phase | Change | Status |
|-------|--------|--------|
| 1 | Action buttons → native styles | |
| 2 | Member list container improvements | |
| 4 | Member list scrollbar → `$C.@TranslucentScrollbarStyle` | |
| 5 | Tooltips on role badges, action buttons | |
| 7 | Color standardization | |

### FactionRelationsPage

**Files**: `faction/faction_relations.ui`, `faction/faction_relation_entry.ui`, `faction/relation_entry.ui`, `faction/relation_btn_*.ui`

| Phase | Change | Status |
|-------|--------|--------|
| 1 | Relation action buttons → native styles | |
| 4 | Relations list scrollbar → `$C.@TranslucentScrollbarStyle` | |
| 5 | Tooltips on relation types, action buttons | |
| 7 | Color standardization | |

### FactionInvitesPage

**Files**: `faction/faction_invites.ui`, `faction/faction_invite_entry.ui`, `faction/invite_entry.ui`, `faction/request_entry.ui`

| Phase | Change | Status |
|-------|--------|--------|
| 1 | Accept → `$C.@SecondaryTextButtonStyle`, Reject → `$C.@CancelTextButtonStyle` | |
| 4 | Invites list scrollbar → `$C.@TranslucentScrollbarStyle` | |
| 5 | Tooltips on accept/reject buttons | |
| 7 | Color standardization | |

### SetRelationModalPage

**Files**: `faction/set_relation_modal.ui`, `faction/set_relation_card.ui`, `faction/relation_set_btn.ui`

| Phase | Change | Status |
|-------|--------|--------|
| 1 | Buttons → native styles | |
| 2 | Search results → `$C.@Panel` | |
| 3 | Search: `PlaceholderText: "Search factions..."` | |
| 5 | Tooltips on relation type buttons | |
| 7 | Color standardization | |

### TreasuryPage

**Files**: `faction/faction_treasury.ui`, `faction/treasury_deposit_modal.ui`, `faction/treasury_settings.ui`, `faction/treasury_transfer_search.ui`, `faction/treasury_transfer_confirm.ui`

| Phase | Change | Status |
|-------|--------|--------|
| 1 | Action buttons → native styles | |
| 2 | Balance display → `$C.@SimpleContainer` | |
| 3 | Amount input: `PlaceholderText: "Enter amount..."` | |
| 3 | Transfer search: `PlaceholderText: "Search players..."` | |
| 5 | Tooltips on treasury actions | |
| 7 | Color standardization | |

### FactionChatPage

**Files**: `faction/faction_chat.ui`, `FactionChatPage.java`

| Phase | Change | Status |
|-------|--------|--------|
| 1 | Send button → native style | |
| 3 | Message input: `PlaceholderText: "Type a message..."` | |
| 4 | Chat history scrollbar → `$C.@TranslucentScrollbarStyle` | |
| 7 | Color standardization | |

### Confirmation Modals (5 pages)

**Files**: `shared/disband_confirm.ui`, `shared/leave_confirm.ui`, `shared/leader_leave_confirm.ui`, `faction/transfer_confirm.ui`, `admin/unclaim_all_confirm.ui`

| Phase | Change | Status |
|-------|--------|--------|
| 1 | Confirm → `$C.@CancelTextButtonStyle`, Cancel → `$C.@TertiaryTextButtonStyle` | |
| 2 | Content → `$C.@Panel` or `$C.@SimpleContainer` | |
| 2 | Divider → `$C.@ContentSeparator` between warning and buttons | |
| 5 | Tooltip on dangerous buttons: `"This action cannot be undone"` | |
| 7 | Color standardization | |

---

## Admin Pages

### Admin Main & Navigation

**Files**: `admin/admin_main.ui`, `admin/admin_dashboard.ui`

| Phase | Change | Status |
|-------|--------|--------|
| 1 | Nav buttons → native styles | |
| 2 | Dashboard stat cards → `$C.@SimpleContainer` | |
| 5 | Tooltips on nav items | |
| 7 | Color standardization | |

### Admin Factions

**Files**: `admin/admin_factions.ui`, `admin/admin_faction_entry.ui`, `admin/admin_faction_info.ui`, `admin/admin_faction_members.ui`, `admin/admin_faction_members_entry.ui`, `admin/admin_faction_relations.ui`, `admin/admin_faction_relations_entry.ui`, `admin/admin_faction_settings.ui`

| Phase | Change | Status |
|-------|--------|--------|
| 1 | All buttons → native styles | |
| 2 | Table wrappers → `$C.@Container` | |
| 3 | Search: `PlaceholderText: "Search factions..."` | |
| 5 | Tooltips on admin action buttons | |
| 7 | Color standardization | |

### Admin Players

**Files**: `admin/admin_players.ui`, `admin/admin_player_entry.ui`, `admin/admin_player_info.ui`

| Phase | Change | Status |
|-------|--------|--------|
| 1 | Buttons → native styles | |
| 2 | Table wrapper → `$C.@Container` | |
| 3 | Search: `PlaceholderText: "Search players..."` | |
| 5 | Tooltips on admin actions | |
| 7 | Color standardization | |

### Admin Zones

**Files**: `admin/admin_zones.ui`, `admin/admin_zone_entry.ui`, `admin/admin_zone_map.ui`, `admin/admin_zone_settings.ui`, `admin/admin_zone_integration_flags.ui`, `admin/create_zone_wizard.ui`, `admin/zone_change_type_modal.ui`, `admin/zone_rename_modal.ui`

| Phase | Change | Status |
|-------|--------|--------|
| 1 | Buttons → native styles | |
| 2 | Zone list → `$C.@Container` | |
| 3 | Zone name input: `PlaceholderText` in wizard and rename modal | |
| 5 | Tooltips on zone flag checkboxes (use `ZoneFlags.getDescription()`) | |
| 7 | Color standardization | |

**Note**: Zone map (`admin_zone_map.ui`) is highly custom — do NOT change its layout or transparent button overlays. Only update non-map buttons (zoom, mode) and add tooltips.

### Admin Economy

**Files**: `admin/admin_economy.ui`, `admin/admin_economy_entry.ui`, `admin/admin_economy_adjust.ui`

| Phase | Change | Status |
|-------|--------|--------|
| 1 | Buttons → native styles | |
| 3 | Amount input: `PlaceholderText` | |
| 5 | Tooltips on adjust actions | |
| 7 | Color standardization | |

### Admin Config, Backups, Updates, Help

**Files**: `admin/admin_config.ui`, `admin/admin_backups.ui`, `admin/admin_updates.ui`, `admin/admin_help.ui`

| Phase | Change | Status |
|-------|--------|--------|
| 1 | Buttons → native styles | |
| 2 | Config sections → `$C.@Panel` with `$C.@PanelTitle` | |
| 2 | Add `$C.@DefaultSpinner` for backup progress (optional) | |
| 5 | Tooltips on config options (explain each setting) | |
| 7 | Color standardization | |

---

## New Player Pages

**Files**: `newplayer/browse.ui`, `newplayer/create_faction.ui`, `newplayer/faction_card.ui`, `newplayer/help.ui`, `newplayer/invite_card.ui`, `newplayer/invites.ui`, `newplayer/invites_empty.ui`, `newplayer/map_readonly.ui`, `newplayer/newplayer_faction_entry.ui`, `newplayer/request_card.ui`

| Phase | Change | Status |
|-------|--------|--------|
| 1 | All buttons → native styles (Create = primary CTA) | |
| 2 | Content containers → native templates | |
| 3 | Search/input fields: `PlaceholderText` | |
| 4 | Browse scrollbar → `$C.@TranslucentScrollbarStyle` | |
| 5 | Tooltips on action buttons | |
| 7 | Color standardization | |

---

## Shared Components

**Files**: `shared/styles.ui`, `shared/description_modal.ui`, `shared/error_page.ui`, `shared/faction_info.ui`, `shared/invite_notification.ui`, `shared/no_faction_actions.ui`, `shared/placeholder_page.ui`, `shared/rename_modal.ui`, `shared/tag_modal.ui`, `nav/nav_button.ui`, `nav/nav_button_active.ui`

| Phase | Change | Status |
|-------|--------|--------|
| 1 | styles.ui: remove all custom styles except Invisible/PlayerMarker | |
| 1 | Modal buttons → native styles | |
| 2 | Modal content → `$C.@Panel` | |
| 3 | Modal inputs: `PlaceholderText` | |
| 5 | Nav tooltips on each tab | |
| 7 | Color standardization | |

---

## Territory Map Pages (MINIMAL changes)

**Files**: `faction/chunk_map.ui`, `faction/chunk_map_terrain.ui`, `faction/chunk_btn.ui`, `faction/chunk_btn_player.ui`

| Phase | Change | Status |
|-------|--------|--------|
| 1 | Keep `$S.@InvisibleButtonStyle` and `$S.@PlayerMarkerButtonStyle` | |
| 1 | Update non-map buttons (zoom, mode) → native styles | |
| 5 | Tooltips on zoom buttons, mode toggle, legend items | |

**DO NOT** change the transparent button grid, player markers, or territory rendering — these are highly custom and intentionally designed.
