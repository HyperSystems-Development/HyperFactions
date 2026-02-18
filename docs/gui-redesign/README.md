# HyperFactions GUI Redesign — Native Hytale Style

> **Status**: Design phase | **Target**: v0.9.0 | **Pages**: 61 | **Templates**: 126 .ui files

## Goal

Redesign the HyperFactions GUI to look and feel like a native Hytale interface — consistent with Hytale's own settings, inventory, and UI Gallery pages — while preserving all existing functionality.

The 2026.02.17 server update revealed a rich set of official Hytale UI templates (`$C.@SecondaryTextButton`, `$C.@Container`, `$C.@Panel`, `$C.@Title`, tooltips, tab navigation, etc.) that match Hytale's built-in interface style. HyperFactions currently uses a custom style system (`styles.ui`) with hand-crafted `TextButtonStyle` definitions that feels "modded" rather than native.

---

## Design Principles

1. **Use native templates over custom styles** — Replace custom `TextButtonStyle` definitions with `$C.@SecondaryTextButton`, `$C.@CancelTextButton`, etc.
2. **Secondary buttons as default** — `$C.@SecondaryTextButton` for most actions (not primary blue)
3. **Icon buttons where appropriate** — `$C.@SecondaryButton` (square, no text) for compact actions like Edit, Delete
4. **Native containers** — `$C.@Panel`, `$C.@Container`, `$C.@SimpleContainer` instead of raw `Group { Background: ... }`
5. **Tooltips everywhere** — `TooltipText` + `$C.@DefaultTextTooltipStyle` on buttons, stats, and interactive elements
6. **Native text colors** — `$C.@ColorDefault`, `$C.@ColorDefaultLabel`, `$C.@ColorGrayCaption` instead of hardcoded hex
7. **Native inputs** — `$C.@TextField` with `PlaceholderText`, `$C.@MultilineTextField`, `CompactTextField` for search
8. **Translucent scrollbars** — `$C.@TranslucentScrollbarStyle` for cleaner scroll areas
9. **Native separators** — `$C.@ContentSeparator` instead of `Group { Height: 1; Background: ... }`
10. **Consistent spacing** — Use `Padding: (Full: N)` and `Padding: (Horizontal: N)` shorthands

---

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **Default button style** | `$C.@SecondaryTextButtonStyle` | Matches Hytale's own UI; primary reserved for main CTAs |
| **Destructive actions** | `$C.@CancelTextButtonStyle` | Native red button for delete/disband/leave |
| **Primary CTA exception** | `$C.@DefaultTextButtonStyle` on "Create Faction" only | One true call-to-action per flow |
| **Navigation** | Keep text-based MenuItem bar, restyle with native colors | Lower effort, text labels clearer for complex plugin; TabNavigation deferred |
| **Icon buttons** | Mix of icon + small text buttons | `$C.@SecondaryButton` for edit/delete with PNGs, `$C.@SmallSecondaryTextButton` for text fallback |
| **Loading states** | `$C.@DefaultSpinner` for async pages | Show spinner while data loads (browser, member lists, admin tables) |
| **Scrollbars** | `$C.@TranslucentScrollbarStyle` for content, `$C.@DefaultScrollbarStyle` for admin | Clean look for players, visible scrollbar for dense admin data |

---

## Implementation Phases

| Phase | Document | Scope | Files Affected |
|-------|----------|-------|----------------|
| 1 | [Styles & Buttons](01-styles-and-buttons.md) | Replace custom styles with native templates | ~126 .ui + ~20 Java |
| 2 | [Containers & Separators](02-containers-and-separators.md) | Native containers and dividers | ~60 .ui |
| 3 | [Input Fields](03-input-fields.md) | PlaceholderText, MultilineTextField, CompactTextField | ~10 .ui |
| 4 | [Scrollbars](04-scrollbars.md) | TranslucentScrollbarStyle migration | ~20 .ui |
| 5 | [Tooltips](05-tooltips.md) | TooltipText on interactive elements | ~40 Java + .ui |
| 6 | [Help Page Redesign](06-help-page.md) | UI Gallery-inspired layout | 2 .ui + 1 Java |
| 7 | [Color Standardization](07-color-standardization.md) | Replace hardcoded hex with $C.@Color* | All files |
| — | [Page Migration Guide](page-migration-guide.md) | Per-page change checklist | Reference |

---

## Verification Protocol

After each phase:

1. Build: `./gradlew :HyperFactions:shadowJar`
2. Deploy to dev server
3. Test each modified page in-game:
   - Visual: buttons, containers, separators render correctly
   - Functional: all buttons still fire events
   - Tooltips: hover shows text (Phase 5+)
   - Scrollbars: translucent style appears on hover (Phase 4+)
   - No crashes from missing styles or invalid templates

### Key test scenarios

- `/f` — Main menu renders with new button styles
- `/f create` — Create page with placeholders, multiline description, native buttons
- `/f dashboard` — Stats with containers, tooltips, activity feed scroll
- `/f settings` — All edit/toggle actions work
- `/f help` — Redesigned help center with panels
- `/f admin` — Admin pages functional
- All confirmation modals (disband, leave, transfer) work

---

## Files Inventory

### Custom styles (styles.ui)

**Keep** (no native equivalent):
- `@InvisibleButtonStyle` — Transparent click overlays for chunk map cells
- `@PlayerMarkerButtonStyle` — White bold text for map player markers

**Remove** (replace with native):
- `@ButtonStyle` → `$C.@SecondaryTextButtonStyle`
- `@CyanButtonStyle` → `$C.@SecondaryTextButtonStyle`
- `@GreenButtonStyle` → `$C.@SecondaryTextButtonStyle`
- `@RedButtonStyle` → `$C.@CancelTextButtonStyle`
- `@GoldButtonStyle` → `$C.@SecondaryTextButtonStyle`
- `@DisabledButtonStyle` → Native `Disabled: true` on any button
- `@FlatRedButtonStyle` → `$C.@CancelTextButtonStyle`

### Template file counts

| Directory | Files | Description |
|-----------|-------|-------------|
| `faction/` | ~45 | Player-facing faction pages |
| `admin/` | ~30 | Admin management pages |
| `shared/` | ~15 | Modals, menus, reusable components |
| `nav/` | ~5 | Navigation bar templates |
| `newplayer/` | ~12 | Pre-faction pages (browse, create, invites) |
| `help/` | ~3 | Help system pages |
| `test/` | ~2 | Development test pages |
