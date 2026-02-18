# Phase 3: Input Fields

> Add PlaceholderText, MultilineTextField, and CompactTextField for search

## Overview

HyperFactions uses `$C.@TextField` in several places but without `PlaceholderText` hints. Phase 3 adds placeholder text to all input fields, replaces description fields with `$C.@MultilineTextField`, and introduces `CompactTextField` for search.

---

## Changes

### PlaceholderText on existing TextFields

Add `PlaceholderText` to all `$C.@TextField` and `TextField` elements:

| Page | Field | PlaceholderText |
|------|-------|-----------------|
| `create_faction.ui` | Name input | `"Enter faction name..."` |
| `create_faction.ui` | Tag input | `"2-4 chars"` |
| `tag_modal.ui` | Tag edit | `"2-4 characters"` |
| `rename_modal.ui` | Name edit | `"New faction name..."` |
| `description_modal.ui` | Description | `"Describe your faction..."` |
| `faction_browser.ui` | Search | `"Search factions..."` |
| `faction_chat.ui` | Message input | `"Type a message..."` |
| `set_relation_modal.ui` | Search | `"Search factions..."` |
| `treasury_deposit_modal.ui` | Amount input | `"Enter amount..."` |
| `treasury_transfer_search.ui` | Search | `"Search players..."` |
| `admin_factions.ui` | Search | `"Search factions..."` |
| `admin_players.ui` | Search | `"Search players..."` |
| `admin_zones.ui` | Search | `"Search zones..."` |
| `zone_rename_modal.ui` | Zone name | `"Zone name..."` |
| `create_zone_wizard.ui` | Zone name | `"Enter zone name..."` |
| `newplayer/create_faction.ui` | Name input | `"Enter faction name..."` |
| `newplayer/create_faction.ui` | Tag input | `"2-4 chars"` |
| `newplayer/browse.ui` | Search | `"Search factions..."` |

### Syntax

```
$C.@TextField #NameInput {
  PlaceholderText: "Enter faction name...";
  Anchor: (Height: 28);
  Style: (FontSize: 11, TextColor: #FFFFFF);
}
```

### MultilineTextField for descriptions

Replace single-line `$C.@TextField` with `$C.@MultilineTextField` for faction description editing:

| Page | Field | Change |
|------|-------|--------|
| `description_modal.ui` | Description input | `$C.@TextField` → `$C.@MultilineTextField` |
| `create_faction.ui` | Description field | `$C.@TextField` → `$C.@MultilineTextField` |
| `newplayer/create_faction.ui` | Description field | `$C.@TextField` → `$C.@MultilineTextField` |

```
// OLD
$C.@TextField #DescInput {
  Anchor: (Height: 28);
  Style: (FontSize: 11);
}

// NEW
$C.@MultilineTextField #DescInput {
  PlaceholderText: "Describe your faction...";
  Anchor: (Height: 80);
  Style: (FontSize: 11);
}
```

### CompactTextField for search fields

For pages with search functionality, consider using `CompactTextField` which animates expand/collapse:

```
CompactTextField #SearchField {
  CollapsedWidth: 34;
  ExpandedWidth: 200;
  PlaceholderText: "Search...";
}
```

**Alternative**: Use `$C.@HeaderSearch` in container title bars for a more integrated look.

**Candidate pages**:
- `faction_browser.ui` — Faction search
- `set_relation_modal.ui` — Faction search for relation target
- `admin_factions.ui` — Admin faction search
- `admin_players.ui` — Admin player search
- `admin_zones.ui` — Admin zone search

**Note**: `CompactTextField` and `$C.@HeaderSearch` are newer elements discovered in the UI Gallery. Test behavior with existing Java event handlers (`ValueChanged` events) before committing to this pattern. If they fire events differently, stick with styled `$C.@TextField` + `PlaceholderText`.

---

## Java Code Changes

No Java changes needed for `PlaceholderText` — it's a .ui-only property.

For `$C.@MultilineTextField`, verify that:
- `cmd.set("#DescInput.Text", value)` still works
- `EventData.append("@DescInput", "#DescInput.Value")` reads correctly
- The element ID selector doesn't change (template wrapper issue)

---

## Verification

1. Build and deploy
2. Check each input field shows placeholder text when empty
3. Check placeholder disappears when typing
4. Check `$C.@MultilineTextField` expands to accommodate multiple lines
5. Verify all existing functionality (submit, search debounce) still works
6. If `CompactTextField` is used, verify expand/collapse animation and event binding
