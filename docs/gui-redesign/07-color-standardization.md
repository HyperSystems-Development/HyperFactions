# Phase 7: Color Standardization

> Replace hardcoded hex colors with `$C.@Color*` constants where native equivalents exist

## Overview

HyperFactions uses hundreds of hardcoded hex color values throughout .ui templates. Phase 7 replaces colors that have native Hytale equivalents with `$C.@Color*` constants for consistency with the platform's visual language. Faction-specific colors (cyan, gold, red, green, etc.) are kept as hardcoded values since they have no native equivalent.

---

## Native Color Constants

Discovered from the UI Gallery source files:

| Constant | Approximate Hex | Use Case |
|----------|----------------|----------|
| `$C.@ColorDefault` | `#FFFFFF` / `#CCCCCC` | Primary text |
| `$C.@ColorDefaultLabel` | `#AAAAAA` / `#CCCCCC` | Standard label text |
| `$C.@ColorGrayCaption` | `#888888` / `#666666` | Caption, muted text, secondary info |
| `$C.@ColorBlueAccent` | `#4a9eff` / `#58a6ff` | Links, clickable text, player names |

**Note**: The exact hex values of these constants are defined by the Hytale client and may differ slightly from the approximations above. Using the constants ensures consistency with the rest of Hytale's UI regardless of future client updates.

---

## Migration Rules

### Colors TO REPLACE

| Hardcoded Hex | Replace With | Occurrences (approx) |
|---------------|-------------|---------------------|
| `#FFFFFF` (text) | `$C.@ColorDefault` | ~50 |
| `#CCCCCC` (text) | `$C.@ColorDefault` or `$C.@ColorDefaultLabel` | ~40 |
| `#AAAAAA` (labels) | `$C.@ColorDefaultLabel` | ~60 |
| `#888888` (captions) | `$C.@ColorGrayCaption` | ~190 |
| `#666666` (muted) | `$C.@ColorGrayCaption` | ~110 |
| `#555555` (dim) | `$C.@ColorGrayCaption` | ~50 |
| `#4a9eff` (links) | `$C.@ColorBlueAccent` | ~20 |
| `#58a6ff` (links) | `$C.@ColorBlueAccent` | ~5 |

### Colors to KEEP (no native equivalent)

These are HyperFactions brand/semantic colors — do NOT replace:

| Hex | Meaning | Where Used |
|-----|---------|-----------|
| `#00FFFF` | Faction cyan (brand) | Faction names, headers, accents |
| `#00AAAA` | Teal (secondary brand) | Secondary faction accents |
| `#FFD700` | Gold (leader) | Leader names, premium indicators |
| `#87CEEB` | Sky blue (officer) | Officer role indicators |
| `#44CC44` / `#55FF55` | Green (success/ally) | Ally badges, success states, online indicators |
| `#FF5555` | Red (danger/enemy) | Enemy badges, danger actions, error states |
| `#FFAA00` | Orange (warning) | Warning messages, pending states |
| `#00AAFF` | Blue (ally relation) | Ally relation badges |
| `#1a2a3a` | Dark panel background | Container backgrounds (may change in Phase 2) |
| `#0d1520` | Darker background | Page-level backgrounds |
| `#334455` / `#2a3a4a` | Divider lines | Manual separators (replaced in Phase 2 by `$C.@ContentSeparator`) |

### Background colors

Background hex values (`#1a2a3a`, `#0d1520`, etc.) are NOT replaced with `$C.@Color*` constants in this phase. Those are addressed in Phase 2 (containers) where raw Groups are replaced with `$C.@Panel`, `$C.@SimpleContainer`, etc. that bring their own native backgrounds.

---

## Syntax

### In .ui files

```
// OLD
Label { Style: (FontSize: 11, TextColor: #888888); }
Label { Style: (FontSize: 14, TextColor: #FFFFFF, RenderBold: true); }
Label { Style: (FontSize: 10, TextColor: #AAAAAA); }

// NEW
Label { Style: (FontSize: 11, TextColor: $C.@ColorGrayCaption); }
Label { Style: (FontSize: 14, TextColor: $C.@ColorDefault, RenderBold: true); }
Label { Style: (FontSize: 10, TextColor: $C.@ColorDefaultLabel); }
```

### In Java (dynamic color setting)

```java
// OLD
cmd.set("#Label.Style.TextColor", "#888888");

// NEW — verify syntax
cmd.set("#Label.Style.TextColor", "$C.@ColorGrayCaption");  // May need Value.ref()
// OR
cmd.set("#Label.Style.TextColor", Value.ref("../../Common.ui", "ColorGrayCaption"));
```

**Important**: Test whether `$C.@ColorGrayCaption` syntax works in Java `cmd.set()` calls. If not, use `Value.ref()` pattern. If neither works, keep hardcoded hex in Java and only replace in .ui files.

---

## Approach

### Recommended order

1. Start with `.ui` files (static colors are safe to replace)
2. Test a small batch first (e.g., `faction_dashboard.ui` + `faction_members.ui`)
3. If visual result is good, proceed with remaining files
4. Handle Java dynamic colors last (need syntax verification)

### Search patterns

```bash
# Find all hardcoded gray text colors in .ui files
grep -rn 'TextColor: #888888\|TextColor: #666666\|TextColor: #555555' HyperFactions/src/main/resources/

# Find all hardcoded white text
grep -rn 'TextColor: #FFFFFF\|TextColor: #CCCCCC' HyperFactions/src/main/resources/

# Find all hardcoded blue accents
grep -rn 'TextColor: #4a9eff\|TextColor: #58a6ff' HyperFactions/src/main/resources/

# Find all Java color references
grep -rn 'TextColor.*#' HyperFactions/src/main/java/
```

---

## Risks

- **Color mismatch**: `$C.@ColorDefault` may not be exactly `#FFFFFF`. If the native constant looks noticeably different, document the difference and decide case-by-case.
- **Java syntax**: `$C.@Color*` references may not work in Java `cmd.set()` calls. Have fallback plan (keep hex in Java).
- **Template variable resolution**: `$C.@Color*` requires `$C = "../../Common.ui"` import in each .ui file. Files that don't currently import `$C` need it added.

---

## Verification

1. Build and deploy
2. Compare screenshots before/after for each page
3. Text should look consistent with Hytale's native UI (settings, inventory, etc.)
4. Faction-specific colors (cyan, gold, red, green) should be unchanged
5. No "invisible text" from wrong color constant
6. Dynamic Java-set colors still render correctly
