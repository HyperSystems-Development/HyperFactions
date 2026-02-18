# Phase 1: Styles & Button Migration

> Replace custom `$S.@*Style` references with native Hytale button templates/styles

## Overview

HyperFactions currently defines 27 custom `TextButtonStyle` definitions and 24 custom `LabelStyle` definitions in `shared/styles.ui`. These produce a "modded" look that doesn't match Hytale's native UI. Phase 1 replaces all standard button styles with native equivalents from `Common.ui`.

---

## Migration Mapping

### Button styles

| Old Custom Style | New Native | Usage Context |
|------------------|-----------|---------------|
| `$S.@ButtonStyle` | `$C.@SecondaryTextButtonStyle` | Default action buttons |
| `$S.@CyanButtonStyle` | `$C.@SecondaryTextButtonStyle` | Brand-accent buttons (context via labels, not button color) |
| `$S.@GreenButtonStyle` | `$C.@SecondaryTextButtonStyle` | Success/confirm actions (context via tooltip) |
| `$S.@GoldButtonStyle` | `$C.@SecondaryTextButtonStyle` | Premium/leader actions (context via gold label) |
| `$S.@RedButtonStyle` | `$C.@CancelTextButtonStyle` | Destructive actions |
| `$S.@FlatRedButtonStyle` | `$C.@CancelTextButtonStyle` | Danger zone actions |
| `$S.@FlatDarkRedButtonStyle` | `$C.@CancelTextButtonStyle` | Danger zone (darker variant) |
| `$S.@DisabledButtonStyle` | Native `Disabled: true;` on any button | Locked/unavailable actions |
| `$C.@DefaultTextButtonStyle` (primary blue) | `$C.@SecondaryTextButtonStyle` | Most buttons; primary reserved for CTAs |

### Styles to KEEP (no native equivalent)

| Style | Reason |
|-------|--------|
| `@InvisibleButtonStyle` | Intentionally transparent — used as click overlay on rendered chunk map |
| `@PlayerMarkerButtonStyle` | White bold centered text for player name markers on map overlay |

### Label styles

Custom label styles (`@SectionHeaderStyle`, `@SubheaderStyle`, etc.) can be replaced with:

| Old | New |
|-----|-----|
| Section headers | `$C.@Subtitle { @Text = "..."; }` template |
| Body text | `Style: (FontSize: 11, TextColor: $C.@ColorDefault)` |
| Caption/muted text | `Style: (FontSize: 9, TextColor: $C.@ColorGrayCaption)` |

---

## Template vs Style Approach

There are two ways to use native button styles:

### Option A: Style reference on native element (RECOMMENDED)

```
TextButton #MyBtn {
  Text: "Click Me";
  Style: $C.@SecondaryTextButtonStyle;
  Anchor: (Height: 28);
}
```

**Pros**: No selector changes needed. `#MyBtn.Text`, `#MyBtn.Disabled` all work directly.
**Cons**: Slightly more verbose.

### Option B: Template wrapper

```
$C.@SecondaryTextButton #MyBtn {
  @Text = "Click Me";
  Anchor: (Height: 28);
}
```

**Pros**: Concise.
**Cons**: Template wraps the TextButton, so `cmd.set("#MyBtn.Text", ...)` may need to become `cmd.set("#MyBtn #TextButton.Text", ...)`. Event selectors may also change.

### Recommendation

Use **Option A** (`Style: $C.@SecondaryTextButtonStyle`) for migration of existing buttons — avoids any Java selector changes. Reserve **Option B** (template syntax) for new buttons where there's no existing Java code to update.

---

## Java Code Changes

### Value.ref() updates

Many page classes apply styles dynamically via `Value.ref()`:

```java
// OLD
cmd.set("#Btn.Style", Value.ref("HyperFactions/shared/styles.ui", "CyanButtonStyle"));
cmd.set("#Btn.Style", Value.ref("HyperFactions/shared/styles.ui", "ButtonStyle"));
cmd.set("#Btn.Style", Value.ref("HyperFactions/shared/styles.ui", "RedButtonStyle"));
cmd.set("#Btn.Style", Value.ref("HyperFactions/shared/styles.ui", "DisabledButtonStyle"));

// NEW
cmd.set("#Btn.Style", Value.ref("../../Common.ui", "SecondaryTextButtonStyle"));
cmd.set("#Btn.Style", Value.ref("../../Common.ui", "CancelTextButtonStyle"));
cmd.set("#Btn.Style", Value.ref("../../Common.ui", "DefaultTextButtonStyle"));  // primary CTA only
// For disabled state, set the property instead:
cmd.set("#Btn.Disabled", true);
```

### Affected Java files

Any file calling `Value.ref("HyperFactions/shared/styles.ui", ...)` needs updating. Expected files:

- `FactionDashboardPage.java` — Quick action buttons (enable/disable states)
- `FactionSettingsPage.java` — Edit/save button state toggles
- `FactionMembersPage.java` — Role action buttons
- `FactionRelationsPage.java` — Relation action buttons
- `FactionInvitesPage.java` — Accept/reject buttons
- `FactionChatPage.java` — Send button state
- `AdminZoneSettingsPage.java` — Zone flag toggle buttons
- Various admin pages with dynamic button styling

### Search pattern to find all references

```bash
grep -rn 'Value.ref.*styles.ui' HyperFactions/src/main/java/
```

---

## .ui File Changes

### Per-file migration pattern

For each `.ui` file containing `$S.@*Style` references:

1. Change `$S = "path/to/styles.ui"` import to only reference if `@InvisibleButtonStyle` or `@PlayerMarkerButtonStyle` is used
2. Replace all `Style: $S.@ButtonStyle` → `Style: $C.@SecondaryTextButtonStyle`
3. Replace all `Style: $S.@CyanButtonStyle` → `Style: $C.@SecondaryTextButtonStyle`
4. Replace all `Style: $S.@GreenButtonStyle` → `Style: $C.@SecondaryTextButtonStyle`
5. Replace all `Style: $S.@RedButtonStyle` → `Style: $C.@CancelTextButtonStyle`
6. Replace all `Style: $S.@GoldButtonStyle` → `Style: $C.@SecondaryTextButtonStyle`
7. Replace all `Style: $S.@FlatRedButtonStyle` → `Style: $C.@CancelTextButtonStyle`
8. Replace all `Style: $S.@DisabledButtonStyle` → `Style: $C.@SecondaryTextButtonStyle` + `Disabled: true;`
9. Ensure `$C = "../../Common.ui"` (or correct relative path) import exists

### Files using $S.@InvisibleButtonStyle (DO NOT migrate)

These files must keep the `$S` import for transparent map overlays:

- `faction/chunk_btn.ui` — Map cell click target
- `faction/chunk_btn_player.ui` — Player marker on map
- `admin/admin_zone_entry.ui` — Zone list row click
- Various entry templates using invisible row click targets

### styles.ui final state

After Phase 1, `styles.ui` should contain ONLY:

```
$C = "../../Common.ui";

// Transparent click overlays for chunk map & list rows
@InvisibleLabelStyle = LabelStyle( ... );
@InvisibleButtonStyle = TextButtonStyle( ... );

// Player name markers on map overlay
@PlayerMarkerLabelStyle = LabelStyle( ... );
@PlayerMarkerButtonStyle = TextButtonStyle( ... );
```

All other style definitions are removed.

---

## Primary CTA Exception

The "Create Faction" button is the ONE button that keeps `$C.@DefaultTextButtonStyle` (primary blue):

```
TextButton #CreateBtn {
  Text: "Create Faction";
  Style: $C.@DefaultTextButtonStyle;
  Anchor: (Height: 34);
}
```

This is the main call-to-action in the new player flow. All other buttons use secondary or cancel styles.

---

## Verification

After completing Phase 1:

1. `grep -rn '\$S\.@' HyperFactions/src/main/resources/` — Only `@InvisibleButtonStyle` and `@PlayerMarkerButtonStyle` references should remain
2. `grep -rn 'Value.ref.*styles.ui' HyperFactions/src/main/java/` — Should return 0 results (except invisible/marker style refs)
3. Build and deploy
4. In-game: verify every page that has buttons — should look consistent with Hytale's native UI
5. Verify chunk map still works (transparent overlay buttons must remain functional)
