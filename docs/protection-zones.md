# Zone Protection

> **Version**: 0.12.0 | **Source**: [`data/ZoneFlags.java`](../src/main/java/com/hyperfactions/data/ZoneFlags.java), [`protection/ProtectionChecker.java`](../src/main/java/com/hyperfactions/protection/ProtectionChecker.java)

How admin-created SafeZones and WarZones protect areas. For faction claim protection, see [protection-claims.md](protection-claims.md). For cross-cutting concerns (wilderness, explosions, fire), see [protection-global.md](protection-global.md).

## Overview

Zones are admin-controlled protected areas with 52 configurable flags. They **always override** faction claim permissions when both apply.

- **SafeZone**: PvP disabled, building disabled by default. Used for spawns, shops, arenas.
- **WarZone**: PvP enabled, building controlled by flags. Used for contested areas.

```
Zone check → Claim check → Wilderness
(zone flags)   (faction perms)  (no protection)
```

Source: `ProtectionChecker.canInteractChunk()`

---

## Zone Flags (52 Flags)

Source: [`ZoneFlags.java`](../src/main/java/com/hyperfactions/data/ZoneFlags.java) — `ALL_FLAGS` array, `getSafeZoneDefault()`, `getWarZoneDefault()`

### Combat Flags (7)

| Flag | Description | SafeZone Default | WarZone Default |
|------|-------------|------------------|-----------------|
| `pvp_enabled` | Players can damage players (parent) | false | true |
| ↳ `friendly_fire` | Same-faction/ally damage (parent) | false | false |
| &emsp;↳ `friendly_fire_faction` | Same-faction members can damage each other | true\* | true\* |
| &emsp;↳ `friendly_fire_ally` | Allied faction members can damage each other | true\* | true\* |
| `projectile_damage` | Projectiles deal damage | false | true |
| `mob_damage` | Mobs can damage players | false | true |
| `pve_damage` | Players can damage mobs/NPCs | false | true |

> **3-level hierarchy**: `pvp_enabled` → `friendly_fire` → `friendly_fire_faction` / `friendly_fire_ally`. Disabling a parent disables all children.
>
> \*Default `true` so these take effect immediately when parent flags are enabled. When `pvp_enabled` or `friendly_fire` is off, child values are irrelevant.

### Damage Flags (4)

| Flag | Description | SafeZone Default | WarZone Default | Mixin Required |
|------|-------------|------------------|-----------------|----------------|
| `fall_damage` | Fall damage applies | false | true | No (zone-only) |
| `environmental_damage` | Drowning, suffocation | false | true | No (zone-only) |
| `explosion_damage` | Explosion block damage | false | true | Yes |
| `fire_spread` | Fire spread | false | true | Yes |

### Death Flags (2)

| Flag | Description | SafeZone Default | WarZone Default | Mixin Required |
|------|-------------|------------------|-----------------|----------------|
| `keep_inventory` | Keep inventory on death | true | false | Yes |
| `power_loss` | Apply power loss on death | false | false | No |

### Building Flags (4)

| Flag | Description | SafeZone Default | WarZone Default | Mixin Required |
|------|-------------|------------------|-----------------|----------------|
| `build_allowed` | Place/break blocks (parent) | false | false | No (ECS) |
| ↳ `block_place` | Block placement | false | true | Yes |
| ↳ `hammer_use` | Hammer block cycling | false | true | Yes |
| ↳ `builder_tools_use` | Builder tool paste | false | true | HyperProtect only |

### Interaction Flags (13)

| Flag | Description | SafeZone Default | WarZone Default | Mixin Required |
|------|-------------|------------------|-----------------|----------------|
| `block_interact` | General block interaction (parent) | true | true | No (ECS) |
| ↳ `door_use` | Use doors and gates | true | true | No |
| ↳ `container_use` | Use chests and storage | false | false | No |
| ↳ `bench_use` | Use crafting tables | false | false | No |
| ↳ `processing_use` | Use furnaces, smelters | false | false | No |
| ↳ `seat_use` | Sit on seats/mounts | true | true | No |
| `mount_use` | Use mounts | false | false | Yes |
| `light_use` | Use light sources | false | true | Yes (use hook, but not in MIXIN_DEPENDENT_FLAGS) |
| `npc_use` | NPC interaction (parent) | true | true | No (parent flag, not in MIXIN_DEPENDENT_FLAGS) |
| ↳ `npc_tame` | Tame NPCs with F-key | false | true | Yes (use hook) |
| ↳ `npc_interact` | NPC dialogue, shops, quests | true | true | No (event-listener based) |
| `crate_pickup` | Pick up animals with capture crate | false | true | Yes (use hook) |
| `crate_place` | Release animals from capture crate | false | true | Yes (use hook) |

> **Parent-child**: `block_interact` is the parent of 7 interaction sub-flags (`door_use`, `container_use`, `bench_use`, `processing_use`, `seat_use`, `mount_use`, `light_use`). `npc_use` is the parent of `npc_tame` and `npc_interact`. Disabling a parent disables all its children.

### Transport Flags (3)

| Flag | Description | SafeZone Default | WarZone Default | Mixin Required |
|------|-------------|------------------|-----------------|----------------|
| `teleporter_use` | Teleporter block use | true | true | HyperProtect only |
| `portal_use` | Portal block use | true | true | HyperProtect only |
| `mount_entry` | Mounted players can enter zone | false | false | No (territory tracking) |

### Item Flags (4)

| Flag | Description | SafeZone Default | WarZone Default | Mixin Required |
|------|-------------|------------------|-----------------|----------------|
| `item_drop` | Players can drop items | false | true | No |
| `item_pickup` | Players can pick up items (auto) | true | true | No |
| `item_pickup_manual` | F-key pickup allowed | false | true | Yes |
| `invincible_items` | Prevent durability loss | true | false | Yes |

### Spawning Flags (5)

| Flag | Description | SafeZone Default | WarZone Default | Mixin Required |
|------|-------------|------------------|-----------------|----------------|
| `mob_spawning` | Mob spawning in zone (parent) | false | true | No |
| ↳ `hostile_mob_spawning` | Hostile mobs can spawn | false | true | No |
| ↳ `passive_mob_spawning` | Passive mobs can spawn | false | true | No |
| ↳ `neutral_mob_spawning` | Neutral mobs can spawn | false | true | No |
| `npc_spawning` | NPC spawning via mixin | false | true | Yes |

> **Parent-child**: `mob_spawning` is the parent of the 3 group sub-flags (`hostile`, `passive`, `neutral`). `npc_spawning` is separate and requires a mixin hook.

### Mob Clearing Flags (4)

| Flag | Description | SafeZone Default | WarZone Default |
|------|-------------|------------------|-----------------|
| `mob_clear` | Clear all mobs in zone (parent) | true | false |
| ↳ `hostile_mob_clear` | Clear hostile mobs | true | false |
| ↳ `passive_mob_clear` | Clear passive mobs | false | false |
| ↳ `neutral_mob_clear` | Clear neutral mobs | false | false |

> **Parent-child**: `mob_clear` is the parent of the 3 group sub-flags. When enabled, existing mobs of the specified type are periodically removed from the zone.

### Integration Flags (6)

| Flag | Description | SafeZone Default | WarZone Default |
|------|-------------|------------------|-----------------|
| `gravestone_access` | Non-owners can loot/break other players' gravestones | false | true |
| `show_on_map` | Override map visibility for players in zone | false | false |
| `essentials_homes` | HyperEssentials /home works in zone | true | false |
| `essentials_warps` | HyperEssentials /warp works in zone | true | true |
| `essentials_kits` | HyperEssentials /kit works in zone | true | true |
| `essentials_back` | HyperEssentials /back works in zone | true | true |

---

## Mixin-Dependent Zone Flags

These zone flags require a mixin system to be installed. Without a mixin, the flag exists in zone data but has **no enforcement**.

Source: `ZoneFlags.MIXIN_DEPENDENT_FLAGS`

15 flags require a mixin system for enforcement:

| Flag | Without Mixin | With OrbisGuard-Mixins | With HyperProtect-Mixin |
|------|--------------|----------------------|------------------------|
| `block_place` | **Not enforced** | Enforced | Enforced |
| `hammer_use` | **Not enforced** | Enforced | Enforced |
| `builder_tools_use` | **Not enforced** | **Not enforced** | Enforced |
| `explosion_damage` | **Not enforced** | Enforced | Enforced |
| `fire_spread` | **Not enforced** | **Not enforced** | Enforced |
| `teleporter_use` | **Not enforced** | **Not enforced** | Enforced |
| `portal_use` | **Not enforced** | **Not enforced** | Enforced |
| `item_pickup_manual` | **Not enforced** | Enforced | Enforced |
| `invincible_items` | **Not enforced** | Enforced | Enforced |
| `keep_inventory` | **Not enforced** | Enforced | Enforced |
| `npc_spawning` | **Not enforced** | Enforced | Enforced |
| `crate_pickup` | **Not enforced** | **Not enforced** | Enforced |
| `crate_place` | **Not enforced** | **Not enforced** | Enforced |
| `npc_tame` | **Not enforced** | **Not enforced** | Enforced |
| `mount_use` | **Not enforced** | **Not enforced** | Enforced |

**Not mixin-dependent** (despite being in entity/NPC categories): `npc_use` (parent flag, checked before mixin hooks), `npc_interact` (uses event-listener, not mixin), `light_use` (uses mixin use hook but not listed in `MIXIN_DEPENDENT_FLAGS`).

`build_allowed` (the parent) and `block_interact` are enforced by ECS systems and do not require mixins.

---

## Zone-Exclusive Features (No Claim Equivalent)

These protections only work inside admin zones. They have **no faction permission equivalent** for claims:

| Feature | Zone Flag | In Claims |
|---------|-----------|-----------|
| Keep inventory on death | `keep_inventory` | Always false (items drop) |
| Prevent durability loss | `invincible_items` | Always false (items degrade) |
| Prevent item drops | `item_drop` | No check — drops always allowed |
| Prevent fall damage | `fall_damage` | No check — fall damage applies |
| Prevent environmental damage | `environmental_damage` | No check — damage applies |
| Prevent projectile damage | `projectile_damage` | No check — damage applies |
| Prevent mob damage | `mob_damage` | No check — damage applies |
| Prevent PvE damage | `pve_damage` | Checked via `PVE_DAMAGE` InteractionType + `{level}PveDamage` faction flags |
| Prevent power loss | `power_loss` | Uses FactionsConfig settings instead |

See [protection-claims.md § Known Limitations](protection-claims.md#known-limitations--gaps) for details.

---

## Zone Check Behavior

Source: `ProtectionChecker.canInteractChunk()`

### Interaction Check

```
Zone at location?
  → NO: Fall through to claim check
  → YES: Get zone flag for interaction type
    → Flag DISABLED:
      - SafeZone → DENIED_SAFEZONE
      - WarZone → DENIED_WARZONE
    → Flag ENABLED:
      - WarZone → ALLOWED_WARZONE (returns immediately)
      - SafeZone → falls through to claim check (see note)
```

**SafeZone fall-through**: When a SafeZone flag allows an interaction, the check continues to the claim check below. This means a SafeZone can grant permission that a claim would deny, but only if the zone flag is explicitly enabled.

### Zone Flag → InteractionType Mapping

Source: `ProtectionChecker.canInteractChunk()`

| InteractionType | Zone Flag Checked |
|-----------------|-------------------|
| BUILD | `build_allowed` |
| INTERACT, USE | `block_interact` |
| DOOR | `door_use` |
| CONTAINER | `container_use` |
| BENCH | `bench_use` |
| PROCESSING | `processing_use` |
| SEAT | `seat_use` |
| LIGHT | `light_use` |
| TELEPORTER | `teleporter_use` |
| PORTAL | `portal_use` |
| DAMAGE | `pvp_enabled` |
| PVE_DAMAGE | `pve_damage` |
| CRATE_PICKUP | `crate_pickup` |
| CRATE_PLACE | `crate_place` |
| NPC_TAME | `npc_tame` |
| NPC_INTERACT | `npc_interact` |
| MOUNT | `mount_use` |
| ITEM_DROP, ITEM_PICKUP | `block_interact` (zone checks handled by ECS systems) |

### PvP Check in Zones

Source: `ProtectionChecker.canDamagePlayerChunk()`

Zones have a 3-level friendly fire hierarchy:

```
pvp_enabled = false → DENIED_SAFEZONE (no PvP at all)
pvp_enabled = true →
  Same faction?
    → friendly_fire = true → friendly_fire_faction check
    → friendly_fire = false → config.isFactionDamage() fallback
  Ally?
    → friendly_fire = true → friendly_fire_ally check
    → friendly_fire = false → config.isAllyDamage() fallback
```

When zone friendly_fire parent is OFF, the system falls back to server config values (`factionDamage`, `allyDamage`) rather than blocking outright.

---

## Code Links

| Class | Path |
|-------|------|
| ZoneFlags | [`data/ZoneFlags.java`](../src/main/java/com/hyperfactions/data/ZoneFlags.java) |
| Zone | [`data/Zone.java`](../src/main/java/com/hyperfactions/data/Zone.java) |
| ZoneManager | [`manager/ZoneManager.java`](../src/main/java/com/hyperfactions/manager/ZoneManager.java) |
| ZoneInteractionProtection | [`protection/zone/ZoneInteractionProtection.java`](../src/main/java/com/hyperfactions/protection/zone/ZoneInteractionProtection.java) |
| ZoneDamageProtection | [`protection/zone/ZoneDamageProtection.java`](../src/main/java/com/hyperfactions/protection/zone/ZoneDamageProtection.java) |
