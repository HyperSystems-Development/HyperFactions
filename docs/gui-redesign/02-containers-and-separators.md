# Phase 2: Containers & Separators

> Replace raw `Group { Background }` containers with native Hytale container templates and separators

## Overview

HyperFactions uses raw `Group { Background: (Color: #1a2a3a); Padding: ... }` for content sections throughout the UI. Phase 2 replaces these with native Hytale container templates that provide consistent styling, borders, and spacing.

---

## Native Container Templates

### Available containers (from UI Gallery)

| Template | Description | Use Case |
|----------|-------------|----------|
| `$C.@Panel` | Bordered panel with optional title | Form sections, settings groups |
| `$C.@PanelTitle` | Title bar for a Panel | Section headers inside panels |
| `$C.@Container` | Container with `#Title` and `#Content` slots | Major content areas |
| `$C.@DecoratedContainer` | Ornate bordered container | Page-level wrappers (already used) |
| `$C.@SimpleContainer` | Minimal styled container | Stat cards, compact info boxes |

### Native separators

| Template | Description | Replaces |
|----------|-------------|----------|
| `$C.@ContentSeparator` | Horizontal divider line | `Group { Anchor: (Height: 1); Background: (Color: #334455); }` |
| `$C.@PanelSeparatorFancy` | Decorated panel divider | Thicker manual dividers |

### Section headers

| Template | Description | Replaces |
|----------|-------------|----------|
| `$C.@Title` | Large title text | Manual `Label { Style: (FontSize: 14, RenderBold: true) }` |
| `$C.@Subtitle` | Section subtitle text | `Label { Style: (FontSize: 11, TextColor: #888888, RenderBold: true) }` |

---

## Migration Patterns

### Stat cards (Dashboard)

```
// OLD
Group {
  Background: (Color: #1a2a3a);
  Padding: (Left: 12, Right: 12, Top: 8, Bottom: 8);
  Label #PowerLabel { Text: "Power"; Style: (FontSize: 9, TextColor: #888888); }
  Label #PowerValue { Text: "0/0"; Style: (FontSize: 14, TextColor: #FFFFFF); }
}

// NEW
$C.@SimpleContainer {
  Padding: (Full: 12);
  Label #PowerLabel { Text: "Power"; Style: (FontSize: 9, TextColor: $C.@ColorGrayCaption); }
  Label #PowerValue { Text: "0/0"; Style: (FontSize: 14, TextColor: $C.@ColorDefault); }
}
```

### Form sections (Settings, Create)

```
// OLD
Label { Text: "General Settings"; Style: (FontSize: 11, TextColor: #888888, RenderBold: true); }
Group { Anchor: (Height: 1, Bottom: 8); Background: (Color: #334455); }
Group {
  Background: (Color: #1a2a3a);
  Padding: (Left: 12, Right: 12, Top: 10, Bottom: 10);
  // ... form fields
}

// NEW
$C.@Subtitle { @Text = "General Settings"; }
$C.@ContentSeparator {}
$C.@Panel {
  Padding: (Full: 12);
  // ... form fields
}
```

### Content areas (major sections)

```
// OLD
Group {
  Background: (Color: #0d1520);
  Padding: (Left: 16, Right: 16, Top: 12, Bottom: 12);
  Label { Text: "Members"; Style: (FontSize: 12, RenderBold: true); }
  // ... content
}

// NEW
$C.@Container {
  Label #Title { Text: "Members"; }
  Group #Content {
    // ... content
  }
}
```

### Danger zones

```
// OLD
Group {
  Background: (Color: #2a1a1a);
  Padding: (Left: 12, Right: 12, Top: 10, Bottom: 10);
  Label { Text: "Danger Zone"; Style: (FontSize: 11, TextColor: #FF5555, RenderBold: true); }
  TextButton #DisbandBtn { Text: "Disband Faction"; Style: $S.@RedButtonStyle; }
}

// NEW
$C.@Panel {
  Padding: (Full: 12);
  $C.@Subtitle { @Text = "Danger Zone"; }
  $C.@ContentSeparator {}
  TextButton #DisbandBtn {
    Text: "Disband Faction";
    Style: $C.@CancelTextButtonStyle;
    Anchor: (Height: 28);
  }
}
```

### Section dividers

```
// OLD
Group { Anchor: (Height: 1, Bottom: 8); Background: (Color: #334455); }
Group { Anchor: (Height: 1); Background: (Color: #2a3a4a); }

// NEW
$C.@ContentSeparator {}
```

---

## Page-by-Page Scope

### High-impact pages

| Page | Container Changes |
|------|-------------------|
| `faction_dashboard.ui` | 4-6 stat card Groups → `$C.@SimpleContainer`, activity feed Group → `$C.@Panel`, quick actions Group → `$C.@Panel` |
| `create_faction.ui` / `newplayer/create_faction.ui` | Form section Groups → `$C.@Panel` with `$C.@Subtitle` headers, ~3 sections |
| `faction_settings.ui` | Settings section Groups → `$C.@Panel`, danger zone → `$C.@Panel` with cancel buttons |
| `faction_members.ui` | Member list container → `$C.@Container`, section dividers → `$C.@ContentSeparator` |
| `faction_relations.ui` | Relations list → `$C.@Container`, empty state → `$C.@SimpleContainer` |
| `faction_browser.ui` | Search area → `$C.@Panel`, results list → `$C.@Container` |

### Admin pages

| Page | Container Changes |
|------|-------------------|
| `admin_dashboard.ui` | Stats area → `$C.@SimpleContainer` cards |
| `admin_factions.ui` | Table wrapper → `$C.@Container` |
| `admin_players.ui` | Table wrapper → `$C.@Container` |
| `admin_zones.ui` | Zone list → `$C.@Container` |
| `admin_config.ui` | Config sections → `$C.@Panel` with `$C.@PanelTitle` |
| `admin_backups.ui` | Backup list → `$C.@Panel` |
| `create_zone_wizard.ui` | Wizard steps → `$C.@Panel` per step |

### Confirmation modals

All confirmation modals (`disband_confirm.ui`, `leave_confirm.ui`, `leader_leave_confirm.ui`, `transfer_confirm.ui`, admin confirms):

- Wrap content in `$C.@Panel` or `$C.@SimpleContainer`
- Add `$C.@ContentSeparator` between warning text and action buttons

### Entry templates (list rows)

Entry templates (`member_entry.ui`, `faction_browse_entry.ui`, `relation_entry.ui`, etc.) are appended dynamically — they typically use raw Groups for row backgrounds. These should generally stay as raw Groups since they're styled per-row from Java (different colors for different states).

---

## What NOT to Change

- **Chunk map** (`chunk_map.ui`, `chunk_map_terrain.ui`) — Highly custom rendering, raw Groups are intentional
- **Entry row backgrounds** that are styled dynamically from Java (colors change per faction, role, etc.)
- **Inline layout Groups** used purely for horizontal/vertical arrangement (no visual styling)
- **Page-level `$C.@DecoratedContainer`** wrappers — Already native, no change needed
- **`$C.@PageOverlay`** wrappers — Already native

---

## Verification

After completing Phase 2:

1. `grep -rn 'Background: (Color: #1a2a3a)' HyperFactions/src/main/resources/` — Should be significantly reduced (only dynamic/entry rows remaining)
2. `grep -rn 'Height: 1.*Background' HyperFactions/src/main/resources/` — Manual dividers should be replaced with `$C.@ContentSeparator`
3. Build and deploy
4. In-game: verify containers have consistent native Hytale styling (borders, spacing)
5. Verify entry list rows still render correctly (dynamic backgrounds preserved)
6. Verify chunk map is unaffected
