---
id: admin_zone_basics
---
# Zone Basics

Zones are admin-controlled territories with custom
rules that override normal faction protection.

## Zone Types

- **SafeZone** -- No PvP, no building, no damage.
  Ideal for spawn areas and trading hubs.
- **WarZone** -- PvP always enabled, no building.
  Ideal for arenas and contested battle areas.

## Creating Zones

`/f admin safezone <name>`
Creates a SafeZone and claims your current chunk.

`/f admin warzone <name>`
Creates a WarZone and claims your current chunk.

After creation, stand in additional chunks and use
`/f admin zone claim <zone>` to expand the zone.

## Managing Zone Chunks

`/f admin zone claim <zone>`
Add the current chunk to the named zone.

`/f admin zone unclaim <zone>`
Remove the current chunk from the named zone.

`/f admin zone radius <zone> <radius>`
Claim a square of chunks around your position.

## Deleting Zones

`/f admin removezone <name>`
Permanently deletes the zone and releases all its
claimed chunks.

>[!WARNING] Deleting a zone releases all its chunks instantly. This cannot be undone without a backup restore.

>[!INFO] Zone rules **always override** faction territory rules. A SafeZone inside enemy land is still safe.
