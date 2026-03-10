---
id: admin_disbanding
---
# Force Disbanding

Admins can forcefully disband any faction, regardless
of the leader's wishes.

## Command

`/f admin disband <faction>`
Force-disband the named faction. A confirmation
prompt will appear before the action is executed.

**Permission**: `hyperfactions.admin.disband`

>[!WARNING] Disbanding a faction is **irreversible**. All claims are released, all members are removed, and the faction ceases to exist. Create a backup first.

## Consequences

When a faction is disbanded:

| Effect | Description |
|--------|-------------|
| **Claims** | All territory is released immediately |
| **Members** | All players are removed from the roster |
| **Relations** | All alliances and enemies are cleared |
| **Treasury** | Handled per economy config settings |
| **Home** | Faction home is deleted |
| **Chat** | Faction chat history is removed |

## Best Practices

1. Always run `/f admin backup create` before disbanding
2. Notify faction members when possible
3. Document the reason for server records
4. Check `/f admin info <faction>` to review before acting

>[!TIP] If the issue is with a specific member, consider using `/f admin modify` to transfer leadership rather than disbanding the entire faction.
