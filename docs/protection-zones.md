# Zone Protection

> **Version**: 0.10.3 | **Source**: [`data/ZoneFlags.java`](../src/main/java/com/hyperfactions/data/ZoneFlags.java), [`protection/ProtectionChecker.java`](../src/main/java/com/hyperfactions/protection/ProtectionChecker.java)

How admin-created SafeZones and WarZones protect areas. For faction claim protection, see [protection-claims.md](protection-claims.md). For cross-cutting concerns (wilderness, explosions, fire), see [protection-global.md](protection-global.md).

## Overview

Zones are admin-controlled protected areas with 41 configurable flags. They **always override** faction claim permissions when both apply.

- **SafeZone**: PvP disabled, building disabled by default. Used for spawns, shops, arenas.
- **WarZone**: PvP enabled, building controlled by flags. Used for contested areas.

```
Zone check → Claim check → Wilderness
(zone flags)   (faction perms)  (no protection)
```

Source: `ProtectionChecker.canInteractChunk()` lines 173–209

---

## Zone Flags (41 Flags)

Source: [`ZoneFlags.java`](../src/main/java/com/hyperfactions/data/ZoneFlags.java) — `ALL_FLAGS` array (line 274), `getSafeZoneDefault()` (line 388), `getWarZoneDefault()` (line 452)

### Combat Flags (6)

| Flag | Description | SafeZone Default | WarZone Default |
|------|-------------|------------------|-----------------|
| `pvp_enabled` | Players can damage players (parent) | false | true |
| ↳ `friendly_fire` | Same-faction/ally damage (parent) | false | false |
| &emsp;↳ `friendly_fire_faction` | Same-faction members can damage each other | true\* | true\* |
| &emsp;↳ `friendly_fire_ally` | Allied faction members can damage each other | true\* | true\* |
| `projectile_damage` | Projectiles deal damage | false | true |
| `mob_damage` | Mobs can damage players | false | true |

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

### Interaction Flags (6)

| Flag | Description | SafeZone Default | WarZone Default | Mixin Required |
|------|-------------|------------------|-----------------|----------------|
| `block_interact` | General block interaction (parent) | true | true | No (ECS) |
| ↳ `door_use` | Use doors and gates | true | true | No |
| ↳ `container_use` | Use chests and storage | false | false | No |
| ↳ `bench_use` | Use crafting tables | false | false | No |
| ↳ `processing_use` | Use furnaces, smelters | false | false | No |
| ↳ `seat_use` | Sit on seats/mounts | true | true | No |

> **Parent-child**: `block_interact` is the parent of all 5 interaction sub-flags. Disabling the parent disables all children.

### NPC & Crate Flags (5)

| Flag | Description | SafeZone Default | WarZone Default | Mixin Required |
|------|-------------|------------------|-----------------|----------------|
| `npc_use` | NPC interaction (parent) | false | true | Yes (use hook) |
| ↳ `npc_tame` | Tame NPCs with F-key | false | true | Yes (use hook) |
| ↳ `npc_interact` | NPC dialogue, shops, quests | true | true | Yes (use hook) |
| `crate_pickup` | Pick up animals with capture crate | false | true | Yes (use hook) |
| `crate_place` | Release animals from capture crate | false | true | Yes (use hook) |

> **Parent-child**: `npc_use` is the parent of `npc_tame` and `npc_interact`. Disabling the parent visually disables children in the admin UI. NPC role classification uses a fail-open blocklist — only known tameable creature roles trigger `npc_tame`; all other NPC interactions use `npc_interact`.

### Transport Flags (2)

| Flag | Description | SafeZone Default | WarZone Default | Mixin Required |
|------|-------------|------------------|-----------------|----------------|
| `teleporter_use` | Teleporter block use | false | true | HyperProtect only |
| `portal_use` | Portal block use | false | true | HyperProtect only |

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

### Integration Flags (1)

| Flag | Description | SafeZone Default | WarZone Default |
|------|-------------|------------------|-----------------|
| `gravestone_access` | Non-owners can loot/break other players' gravestones | false | true |

---

## Mixin-Dependent Zone Flags

These zone flags require a mixin system to be installed. Without a mixin, the flag exists in zone data but has **no enforcement**.

Source: `ZoneFlags.MIXIN_DEPENDENT_FLAGS` (line 338)

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
| `npc_use` | **Not enforced** | **Not enforced** | Enforced |
| `npc_tame` | **Not enforced** | **Not enforced** | Enforced |
| `npc_interact` | **Not enforced** | **Not enforced** | Enforced |

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
| Prevent power loss | `power_loss` | Uses FactionsConfig settings instead |

See [protection-claims.md § Known Limitations](protection-claims.md#known-limitations--gaps) for details.

---

## Zone Check Behavior

Source: `ProtectionChecker.canInteractChunk()` lines 173–209

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

Source: `ProtectionChecker.canInteractChunk()` lines 177–188

| InteractionType | Zone Flag Checked |
|-----------------|-------------------|
| BUILD | `build_allowed` |
| INTERACT, USE | `block_interact` |
| DOOR | `door_use` |
| CONTAINER | `container_use` |
| BENCH | `bench_use` |
| PROCESSING | `processing_use` |
| SEAT | `seat_use` |
| TELEPORTER | `teleporter_use` |
| PORTAL | `portal_use` |
| DAMAGE | `pvp_enabled` |

### PvP Check in Zones

Source: `ProtectionChecker.canDamagePlayerChunk()` lines 369–408

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
