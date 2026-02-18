# Phase 4: Scrollbars

> Replace `$C.@DefaultScrollbarStyle` with `$C.@TranslucentScrollbarStyle` on content areas

## Overview

HyperFactions uses `$C.@DefaultScrollbarStyle` on all scrollable containers. Phase 4 replaces most with `$C.@TranslucentScrollbarStyle` for a cleaner look, while keeping the visible scrollbar on admin pages where dense content benefits from it.

---

## Scrollbar Styles Available

| Style | Appearance | Use Case |
|-------|-----------|----------|
| `$C.@DefaultScrollbarStyle` | Always-visible solid scrollbar | Admin data tables, dense lists |
| `$C.@DefaultExtraSpacingScrollbarStyle` | Solid scrollbar with extra padding | Wide content areas |
| `$C.@TranslucentScrollbarStyle` | Appears on hover, semi-transparent | Player-facing content, cleaner look |

---

## Migration Plan

### Player-facing pages → `$C.@TranslucentScrollbarStyle`

| File | Element |
|------|---------|
| `faction_dashboard.ui` | Activity feed scroll area |
| `faction_members.ui` | Member list |
| `faction_relations.ui` | Relations list |
| `faction_invites.ui` | Invites list |
| `faction_browser.ui` | Browse results |
| `faction_chat.ui` | Chat message history |
| `faction_settings.ui` | Settings scroll area |
| `faction_actions.ui` | Actions list |
| `faction_modules.ui` | Modules list |
| `faction_treasury.ui` | Treasury transaction history |
| `set_relation_modal.ui` | Search results |
| `help/help_main.ui` | Help content area |
| `newplayer/browse.ui` | Browse results |
| `newplayer/invites.ui` | Invites list |

### Admin pages → Keep `$C.@DefaultScrollbarStyle`

| File | Reason |
|------|--------|
| `admin_factions.ui` | Dense faction table, needs visible scroll position |
| `admin_players.ui` | Dense player table |
| `admin_zones.ui` | Zone list with coordinates |
| `admin_config.ui` | Long config list |
| `admin_economy.ui` | Transaction log |
| `admin_faction_members.ui` | Member management table |
| `admin_zone_settings.ui` | 31 flag checkboxes |
| `admin_backups.ui` | Backup list |

---

## Syntax

```
// OLD
Group #MemberList {
  LayoutMode: TopScrolling;
  ScrollbarStyle: $C.@DefaultScrollbarStyle;
  // ...
}

// NEW
Group #MemberList {
  LayoutMode: TopScrolling;
  ScrollbarStyle: $C.@TranslucentScrollbarStyle;
  // ...
}
```

No Java changes needed — this is a .ui-only property swap.

---

## Verification

1. Build and deploy
2. Player pages: scrollbar should be invisible until hover, then fade in as translucent
3. Admin pages: scrollbar should remain always-visible
4. Verify scroll functionality is unchanged (scroll speed, position, content rendering)
5. Verify long lists (50+ members, 100+ factions) scroll smoothly
