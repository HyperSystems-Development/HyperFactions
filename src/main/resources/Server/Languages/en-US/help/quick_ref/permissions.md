---
id: quickref_permissions
---
# Permissions

Key permission nodes for HyperFactions. All nodes fall under the **hyperfactions** root namespace.

## Core Permissions

| Permission | Description |
|-----------|-------------|
| hyperfactions.use | Access to basic faction commands |
| hyperfactions.faction.create | Create a new faction |
| hyperfactions.faction.disband | Disband your faction |

## Membership

| Permission | Description |
|-----------|-------------|
| hyperfactions.member.invite | Invite players |
| hyperfactions.member.kick | Kick members |
| hyperfactions.member.promote | Promote members |

## Territory

| Permission | Description |
|-----------|-------------|
| hyperfactions.territory.claim | Claim chunks |
| hyperfactions.territory.unclaim | Release chunks |
| hyperfactions.territory.overclaim | Overclaim weakened land |

## Teleport

| Permission | Description |
|-----------|-------------|
| hyperfactions.teleport.home | Use faction home |
| hyperfactions.teleport.sethome | Set faction home |
| hyperfactions.teleport.stuck | Use stuck teleport |

## Diplomacy and Chat

| Permission | Description |
|-----------|-------------|
| hyperfactions.relation.ally | Manage alliances |
| hyperfactions.relation.enemy | Declare enemies |
| hyperfactions.chat.faction | Use faction chat |
| hyperfactions.chat.ally | Use ally chat |

## Information and Economy

| Permission | Description |
|-----------|-------------|
| hyperfactions.info.show | View faction info |
| hyperfactions.info.list | Browse factions |
| hyperfactions.economy.deposit | Deposit to treasury |
| hyperfactions.economy.withdraw | Withdraw from treasury |

## Bypass Permissions

| Permission | Description |
|-----------|-------------|
| hyperfactions.bypass.* | Bypass all restrictions |
| hyperfactions.bypass.combat | Bypass combat tag |
| hyperfactions.bypass.power | Bypass power limits |
| hyperfactions.bypass.territory | Bypass land protection |

>[!INFO] Server admins can grant hyperfactions.* to give access to all permissions at once.

>[!NOTE] Some permissions are restricted by faction role regardless of permission nodes. For example, only Officers can claim even with the permission.
