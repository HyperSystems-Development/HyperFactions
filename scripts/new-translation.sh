#!/bin/bash
# ============================================================
# new-translation.sh — Scaffold a new HyperFactions locale
# Usage: ./scripts/new-translation.sh <locale-code>
# Example: ./scripts/new-translation.sh fr-FR
# ============================================================
set -euo pipefail

if [ $# -lt 1 ]; then
    echo "Usage: $0 <locale-code>"
    echo "Example: $0 fr-FR"
    exit 1
fi

LOCALE="$1"

# Resolve project root (parent of scripts/)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

LANG_SRC="$PROJECT_ROOT/src/main/resources/Server/Languages/en-US"
LANG_DST="$PROJECT_ROOT/src/main/resources/Server/Languages/$LOCALE"

HELP_SRC="$PROJECT_ROOT/src/main/help/en-US"
HELP_DST="$PROJECT_ROOT/src/main/help/$LOCALE"

# --- Validate inputs ---
if [[ ! "$LOCALE" =~ ^[a-z]{2}-[A-Z]{2}$ ]]; then
    echo "Warning: '$LOCALE' does not match standard locale format (e.g., fr-FR)."
    echo "Continuing anyway..."
fi

if [ ! -d "$LANG_SRC" ]; then
    echo "Error: Source language directory not found: $LANG_SRC"
    exit 1
fi

# --- Copy .lang files ---
LANG_COUNT=0
if [ -d "$LANG_DST" ]; then
    echo "Language directory already exists: $LANG_DST"
    echo "Skipping .lang file copy (delete the directory first to re-scaffold)."
else
    mkdir -p "$LANG_DST"
    for file in "$LANG_SRC"/*.lang; do
        if [ -f "$file" ]; then
            cp "$file" "$LANG_DST/"
            LANG_COUNT=$((LANG_COUNT + 1))
        fi
    done
    echo "Copied $LANG_COUNT .lang file(s) to $LANG_DST"
fi

# --- Copy help markdown ---
HELP_COUNT=0
if [ -d "$HELP_SRC" ]; then
    if [ -d "$HELP_DST" ]; then
        echo "Help directory already exists: $HELP_DST"
        echo "Skipping help file copy (delete the directory first to re-scaffold)."
    else
        cp -r "$HELP_SRC" "$HELP_DST"
        HELP_COUNT=$(find "$HELP_DST" -name '*.md' -type f | wc -l)
        echo "Copied $HELP_COUNT help file(s) to $HELP_DST"
    fi
else
    echo "No help directory found at $HELP_SRC — skipping help files."
fi

# --- Summary ---
echo ""
echo "=== Scaffold Summary ==="
echo "Locale:      $LOCALE"
echo "Lang files:  $LANG_COUNT copied to src/main/resources/Server/Languages/$LOCALE/"
echo "Help files:  $HELP_COUNT copied to src/main/help/$LOCALE/"
echo ""
echo "Next steps:"
echo "  1. Add a header comment to each .lang file indicating the language and status"
echo "  2. Translate the values (keep keys and {0} placeholders unchanged)"
echo "  3. Translate the help markdown files"
echo "  4. Test in-game with /f settings to switch language"
