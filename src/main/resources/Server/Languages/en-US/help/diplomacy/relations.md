---
id: diplomacy_relations
commands: relations
---
# Faction Relations

Every pair of factions has a diplomatic relation that determines how they interact. There are three states: **Ally**, **Enemy**, and **Neutral**.

---

## Relation Comparison

| Effect | Ally | Neutral | Enemy |
|--------|------|---------|-------|
| **PvP in territory** | Disabled | Standard rules | Enabled |
| **Territory protection** | Mutual protection | Standard protection | Can overclaim if weakened |
| **Friendly fire** | Disabled | N/A | Enabled everywhere |
| **Map color** | [#5555FF] Blue | [#AAAAAA] Gray | [#FF5555] Red |
| **How to set** | Mutual agreement | Default state | One-way declaration |
| **Chat access** | Ally chat channel | None | None |

---

## Viewing Relations

`/f relations`

Shows all your current alliances, enemies, and any pending alliance requests.

## How Relations Work

- **Neutral** is the default state between all factions. Standard server rules apply.
- **Alliance** requires both factions to agree. Either side can break it unilaterally.
- **Enemy** is declared one-way. No agreement needed -- the other faction is immediately marked as your enemy.

>[!INFO] Relations are managed by Officers and Leaders. Members can view relations but cannot change them.

>[!TIP] Use `/f relations` regularly to keep track of the diplomatic landscape. Knowing who your enemies are helps you prepare for territorial conflicts.
