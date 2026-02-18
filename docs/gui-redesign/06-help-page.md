# Phase 6: Help Page Redesign

> Redesign the help center with a UI Gallery-inspired layout using native panels and gradient titles

## Overview

The current help page (`help/help_main.ui`, `HelpMainPage.java`) uses a sidebar + content layout with 7 color-coded categories, each having its own custom `TextButtonStyle` (130+ lines of inline style definitions). Phase 6 replaces this with a cleaner, native Hytale-styled layout inspired by the official UI Gallery page.

---

## Current State

- **7 categories**: Getting Started, Territory, Relations, Economy, Settings, Commands, Admin
- **Custom styles**: Each category has a unique `@CatStyle*` TextButtonStyle with custom colors (130 lines)
- **Layout**: Sidebar (category list) + content area (topic cards)
- **Content area**: Raw Groups with hardcoded background colors for topic cards

---

## Redesign

### Layout structure

Keep sidebar + content area pattern but use native templates:

```
$C.@PageOverlay {
  Anchor: (Width: 750, Height: 650);

  // Page wrapper
  $C.@DecoratedContainer {
    Anchor: (Width: 750, Height: 650);

    // Sidebar (left, ~180px)
    Group #Sidebar {
      Anchor: (Width: 180);
      LayoutMode: Top;

      $C.@Title { @Text = "Help Center"; }
      $C.@ContentSeparator {}

      // Category buttons — styled natively, NOT custom styles
      Group #CategoryList {
        LayoutMode: Top;
        Padding: (Full: 8);
        // Categories appended from Java
      }
    }

    // Content area (right, fill remaining)
    Group #Content {
      LayoutMode: TopScrolling;
      ScrollbarStyle: $C.@TranslucentScrollbarStyle;
      Padding: (Full: 16);
      // Topic cards appended from Java
    }
  }
}
```

### Category sidebar buttons

Replace 7 custom `@CatStyle*` definitions with native button styles + colored accent indicators:

```
// Each category entry
Group {
  LayoutMode: Left;
  Padding: (Left: 4, Top: 2, Bottom: 2);

  // Color accent bar (category color)
  Group #ColorBar {
    Anchor: (Width: 3, Height: 24);
    Background: (Color: #00FFFF);  // Category-specific color, set from Java
  }

  // Category button
  TextButton #CatBtn {
    Text: "Territory";
    Style: $C.@SecondaryTextButtonStyle;
    Anchor: (Height: 28);
    Padding: (Left: 8);
    TooltipText: "Territory claims, maps, and chunk management";
    TextTooltipStyle: $C.@DefaultTextTooltipStyle;
  }
}
```

Active state: Set `Disabled: true` on the active category button (visually distinct from clickable).

### Topic cards

Replace raw Group backgrounds with `$C.@Panel`:

```
// Each topic card
$C.@Panel {
  Padding: (Full: 12);

  $C.@Subtitle { @Text = "Claiming Territory"; }
  Label #TopicDesc {
    Text: "Learn how to claim and manage chunks for your faction.";
    Style: (FontSize: 10, TextColor: $C.@ColorGrayCaption);
  }
  $C.@ContentSeparator {}
  // Topic content (commands, tips, etc.)
}
```

### Gradient title (optional enhancement)

The UI Gallery uses `MaskTexturePath: $C.@TextHighlightGradientMask` on titles for a gradient text effect:

```
$C.@Title {
  @Text = "Territory";
  MaskTexturePath: $C.@TextHighlightGradientMask;
}
```

This is optional — only use if the gradient effect fits the help page aesthetic.

---

## Category Colors (kept as accent indicators)

| Category | Color | Hex |
|----------|-------|-----|
| Getting Started | Cyan | `#00FFFF` |
| Territory | Green | `#44CC44` |
| Relations | Blue | `#4a9eff` |
| Economy | Gold | `#FFD700` |
| Settings | Orange | `#FFAA00` |
| Commands | Purple | `#AA88FF` |
| Admin | Red | `#FF5555` |

These colors are used ONLY for the thin accent bar beside each category button, not for the buttons themselves.

---

## styles.ui Impact

Remove all 7 `@CatStyle*` TextButtonStyle definitions (~130 lines) from:
- `help/help_main.ui` (if defined inline) OR
- `shared/styles.ui` (if defined there)

---

## Java Changes

`HelpMainPage.java` needs updates to:

1. Use native button styles instead of custom `@CatStyle*` references
2. Set category accent colors via `cmd.set("#ColorBar.Background.Color", categoryColor)`
3. Use `$C.@Panel` template for topic cards instead of raw Group backgrounds
4. Add tooltips to category buttons

---

## Verification

1. Build and deploy
2. `/f help` — Page renders with native-styled sidebar and content
3. All 7 categories clickable and load correct content
4. Topic cards render in `$C.@Panel` containers
5. Category accent colors visible
6. Scrolling works in content area with translucent scrollbar
7. No remnants of custom `@CatStyle*` styles
