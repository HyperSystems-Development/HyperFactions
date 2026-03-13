---
id: admin_managing_factions
---
# Managing Factions

Admins can inspect and modify any faction on the server through the dashboard or commands.

## Browsing Factions

`/f admin factions`
Opens the admin faction browser. View all factions with member counts, power levels, and territory.

`/f admin info <faction>`
Opens the admin info panel for a specific faction with full details and management options.

## Modifying Faction Settings

With `hyperfactions.admin.modify` permission, you can:

- **Rename** a faction to resolve conflicts
- **Set color** to fix display issues
- **Toggle open/close** to override join policy
- **Edit description** for moderation purposes

>[!TIP] Use `/f admin who <player>` to look up which faction a specific player belongs to and view their details.

## Viewing Members and Relations

The admin info panel shows:

| Section | Details |
|---------|---------|
| **Members** | Full roster with roles and last seen |
| **Relations** | All ally, enemy, and neutral standings |
| **Territory** | Claimed chunks and power balance |
| **Economy** | Treasury balance and transaction log |

>[!NOTE] Admin inspection commands do not notify the faction being viewed. Only modifications trigger alerts.
