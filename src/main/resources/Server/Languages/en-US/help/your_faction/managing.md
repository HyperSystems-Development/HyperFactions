---
id: faction_managing
commands: invite, kick, promote, demote, transfer
---
# Managing Members

Officers and Leaders share responsibility for managing the faction roster. Here are the key commands and who can use them.

---

## Commands

| Command | What It Does | Required Role |
|---------|-------------|---------------|
| `/f invite <player>` | Sends a join invitation (expires in 5 min) | Officer+ |
| `/f kick <player>` | Removes a member from the faction | Officer+ (see note) |
| `/f promote <player>` | Promotes a Member to Officer | Leader only |
| `/f demote <player>` | Demotes an Officer to Member | Leader only |
| `/f transfer <player>` | Transfers faction ownership | Leader only |

>[!NOTE] Officers can only kick **Members**. To remove another Officer, the Leader must either demote them first or kick them directly.

---

## Invitations

- Invitations expire after **5 minutes** if not accepted
- The invited player sees it in their Invites tab when they open `/f`
- There is no limit to how many invitations you can send at once
- Your faction can hold up to **50 members** total

## Promotions and Demotions

- Only the **Leader** can promote or demote
- `/f promote <player>` raises a Member to Officer
- `/f demote <player>` lowers an Officer back to Member

## Transferring Leadership

>[!WARNING] Transferring leadership is **irreversible**. You will be demoted to Officer and the target player becomes the new Leader. Make sure you trust them completely.

`/f transfer <player>`

The target must be a current member of your faction.
