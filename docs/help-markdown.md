# Help Markdown Style Guide

Reference for content authors writing HyperFactions help topics.

Help files are located at `src/main/resources/Server/Languages/{locale}/help/{category}/{topic}.md` and compiled into `.lang` files and `help-manifest.json` at build time by `HelpLangGenerator`.

## Frontmatter

Every topic file starts with YAML frontmatter:

```markdown
---
id: welcome_started
commands: gui, menu, create
---
```

- `id` — Unique topic identifier (optional, defaults to `{category}_{filename}`)
- `commands` — Comma-separated list of command names that deep-link to this topic

## Syntax Reference

### Basic Entry Types

| Syntax | Type | Default Color | Style |
|---|---|---|---|
| Plain text | TEXT | #CCCCCC | normal |
| `## Heading` | HEADING | #00AAAA | bold |
| `` `command` `` | COMMAND | #FFFF55 | bold |
| Blank line | SPACER | — | — |

### Text Formatting

| Syntax | Type | Style |
|---|---|---|
| `**bold text**` | BOLD | #CCCCCC, bold |
| `*italic text*` | ITALIC | #CCCCCC, italic |

Bold and italic are **whole-line only**. You cannot mix bold/italic within a line (`some **bold** here` does NOT work — the entire line must be wrapped).

### Lists

| Syntax | Rendering |
|---|---|
| `- item text` | Bullet list item (indented, with bullet prefix) |
| `1. item text` | Numbered list item (indented, number preserved in text) |

List items are indented 12px from normal text. Bullet items get a `•` prefix automatically. Numbered items keep the `1.` prefix as written.

### Separators

```markdown
---
```

Three or more dashes on a line (outside frontmatter) render as a visible horizontal rule — a thin line at `#2a3a4a`.

### Inline Colors

#### Hex Colors

```markdown
[#FF5555] This text appears in red
[#55AAFF] This text appears in blue
```

Any `[#RRGGBB]` prefix sets the text color. Uses the TEXT template.

#### Named Shortcuts

| Syntax | Color | Use Case |
|---|---|---|
| `!warning text` | #FF5555 (red) | Warnings, errors |
| `!success text` | #55FF55 (green) | Success messages |
| `!note text` | #55AAFF (blue) | Informational notes |
| `!muted text` | #888888 (gray) | De-emphasized text |

Named shortcuts are syntactic sugar for `[#hex]` colors. Uses the TEXT template.

### Callout Boxes

Callouts render as boxed text with a colored left accent bar and tinted background.

#### Simple Callout (Tip)

```markdown
> This renders as a green tip callout
```

`>` (blockquote) is shorthand for `>[!TIP]`.

#### Typed Callouts

| Syntax | Color | Use Case |
|---|---|---|
| `>[!TIP] text` | #55FF55 (green) | Tips and advice |
| `>[!WARNING] text` | #FF5555 (red) | Dangers, cautions |
| `>[!INFO] text` | #55AAFF (blue) | Supplementary info |
| `>[!NOTE] text` | #FFAA55 (orange) | Important notes |
| `>[!SUCCESS] text` | #55FF55 (green) | Confirmation messages |

The type tag (`[!WARNING]`, etc.) controls the accent bar and text color.

### Tables

Tables use standard markdown pipe syntax:

```markdown
| Level | Members | Daily Upkeep |
|-------|---------|--------------|
| 1     | 1-5     | 0            |
| 2     | 6-10    | 5            |
| 3     | 11-20   | 15           |
```

- The first row is the **header** (bold, teal `#00AAAA`) — it must be followed by a separator row (`|---|---|---|`)
- The separator row is consumed by the parser and not rendered
- Subsequent `|` rows are **data rows** (normal text, `#CCCCCC`)
- Columns are laid out horizontally using `LayoutMode: Left`
- Each cell is individually localized (e.g., `line.5.col.0`, `line.5.col.1`)

Tables are ideal for reference data like upkeep scales, permission lists, or config examples.

## Example Topic

```markdown
---
id: power_claiming
commands: claim, unclaim, autoclaim
---
# Claiming Territory

## How Claims Work

Each chunk you claim costs 1 power. Your faction can claim
as many chunks as it has power.

`/f claim`
`/f unclaim`

- Stand in the chunk you want to claim
- Your faction must have enough power
- You cannot claim next to enemy territory

## Auto-Claim Mode

**Auto-claim claims every chunk you walk into.**

`/f autoclaim`

> Toggle auto-claim off when you're done!

>[!WARNING] Don't wander into enemy territory with auto-claim on!

---

## Power Costs

| Chunks | Power Cost |
|--------|------------|
| 1-10   | 1 per chunk |
| 11-25  | 2 per chunk |
| 26+    | 3 per chunk |

## Losing Claims

!warning Territory can be overclaimed if your power drops below your claim count.

*Keep your power above your claim count to stay safe.*
```

## Formatting Limitations

1. **Whole-line only** — Bold, italic, commands, callouts, and colors apply to entire lines. No inline mixing (e.g., `some **bold** here` won't work).
2. **No underline** — Hytale Labels have no underline property.
3. **No nested formatting** — Cannot combine bold + color on the same line through markdown syntax. Colors override the template default; bold/italic are separate templates.
4. **Single-level lists** — No nested/indented sub-lists.

## Line Length

The help content area is approximately 450px wide. Text that exceeds this width wraps naturally. For readability:
- Keep text lines under ~70 characters
- Long commands may wrap — test visually
- Callout boxes have slightly less width (padding + accent bar)

## Testing

Use `/f admin test md` in-game to open the markdown rendering test page, which shows every supported entry type rendered with the real templates.
