# HyperFactions Translation Guide

This guide explains how to contribute translations for HyperFactions.

## Quick Start

1. Run the scaffolding script to create a new locale:
   ```bash
   ./scripts/new-translation.sh fr-FR    # Linux/Mac
   scripts\new-translation.bat fr-FR     # Windows
   ```

2. Edit the `.lang` files in `src/main/resources/Server/Languages/<locale>/`
3. Edit the help markdown files in `src/main/resources/Server/Languages/<locale>/help/`
4. Build to verify: `./gradlew :HyperFactions:shadowJar`
5. Submit a pull request

## Supported Locales

| Code   | Language              | Status        |
|--------|-----------------------|---------------|
| en-US  | English (US)          | Complete      |
| es-ES  | Spanish (Spain)       | Complete      |
| de-DE  | German                | Untranslated  |
| fr-FR  | French                | Untranslated  |
| ja-JP  | Japanese              | Untranslated  |
| pt-BR  | Brazilian Portuguese  | Untranslated  |
| ru-RU  | Russian               | Untranslated  |
| tr-TR  | Turkish               | Untranslated  |
| zh-CN  | Simplified Chinese    | Untranslated  |

## File Structure

### .lang Files (Commands, GUI, Admin)

Located at `src/main/resources/Server/Languages/<locale>/`:

| File                       | Content                          | Key Count |
|----------------------------|----------------------------------|-----------|
| `hyperfactions.lang`       | Commands, errors, common strings | ~450      |
| `hyperfactions_gui.lang`   | GUI labels, buttons, nav         | ~440      |
| `hyperfactions_admin.lang` | Admin GUI strings                | ~260      |

### .lang File Format

```properties
# Section comments start with #
key.name = Translated value here
key.with.placeholder = Hello {0}, you have {1} power
```

**Rules:**
- Keys are on the left side of `=` — **never modify keys**
- Values are on the right side — translate these
- `{0}`, `{1}`, etc. are placeholders — keep them in the translation
- Lines starting with `#` are comments — translate for context but not required
- Blank lines are ignored
- Backslash `\` at end of line continues to next line

### Help Markdown Files

Located at `src/main/resources/Server/Languages/<locale>/help/<category>/<topic>.md`.

Each file has YAML frontmatter and markdown content:

```markdown
---
id: welcome_started
commands: gui, menu
---
# Getting Started

Ready to dive in? Here's how:

`/f`
Opens the faction menu.

> Tip: Once in, explore territory and start claiming!
```

**Rules:**
- **YAML frontmatter** (`---` block): Do NOT translate `id` or `commands` — these are identifiers
- **`# Title`**: Translate the heading text
- **Plain text**: Translate normally
- **`` `command` ``** (backtick lines): Do NOT translate command syntax (e.g., `/f create <name>`)
- **`> Tip text`** (blockquotes): Translate the tip content
- **Blank lines**: Keep as-is (they create spacing in the help viewer)

### Markdown → Entry Type Mapping

| Markdown Syntax            | Help Entry Type | Translate? |
|----------------------------|-----------------|------------|
| `# Heading`                | Topic title     | Yes        |
| `## Subheading`            | HEADING entry   | Yes        |
| Plain text line            | TEXT entry      | Yes        |
| Blank line                 | SPACER entry    | Keep as-is |
| `` `command text` ``       | COMMAND entry   | No         |
| `> Tip text`               | TIP entry       | Yes        |

## Translation Tips

### Character Limits

GUI labels have limited space. Keep translations concise:

| Element Type     | Max Length (approx) |
|------------------|---------------------|
| Nav bar buttons  | 12 characters       |
| Button labels    | 20 characters       |
| Section titles   | 30 characters       |
| Descriptions     | 60 characters       |
| Chat messages    | No limit            |
| Help content     | No limit            |

If a translation is too long, it may overflow or be truncated in the UI.

### Gaming Terminology

Use commonly understood gaming terms in your language. Some terms are typically kept in English across all languages:

- **PvP** (Player vs Player)
- **PvE** (Player vs Environment)
- **NPC** (Non-Player Character)
- **K/D** (Kill/Death ratio)
- **UUID**
- **chunk** (a 16x16 block area)

Brand names should not be translated:
- **HyperFactions**
- **HyperPerms**
- **OrbisGuard**
- **HyperProtect**

### Placeholder Values

Placeholders like `{0}`, `{1}` are replaced at runtime with dynamic values. The order matters — `{0}` is always the first argument, `{1}` the second, etc.

Common placeholder meanings (by context):
- `{0}` in faction messages: usually faction name or player name
- `{0}` in error messages: usually the specific value that failed
- `{0}`, `{1}` in range messages: min and max values

### Consistency

Use consistent terminology throughout your translation:
- Pick one word for "faction" and use it everywhere
- Pick one word for "claim/territory" and use it consistently
- Role names should be consistent (Leader, Officer, Member, Recruit)

## Checking Your Translation

### Build and Test

```bash
# Build (generates help .lang from markdown + compiles)
./gradlew :HyperFactions:shadowJar

# Deploy to dev server
./gradlew buildAndDeploy

# In-game: change your client language to test
```

### Check for Missing Keys

```bash
# Compare key counts between locales
./gradlew :HyperFactions:checkTranslations
```

This task reports any keys present in en-US but missing in other locales.

## Contributing

1. Fork the repository
2. Create a branch: `feat/i18n-<locale>` (e.g., `feat/i18n-fr-FR`)
3. Run `./scripts/new-translation.sh <locale>` if starting fresh
4. Translate all `.lang` files and help `.md` files
5. Build and test locally
6. Submit a pull request

### Review Process

- Translations are reviewed by native speakers when possible
- Machine translations are accepted as a starting point but should be refined
- Partial translations are welcome — untranslated keys fall back to English
