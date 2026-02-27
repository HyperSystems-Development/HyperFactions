# Claim Protection

> **Version**: 0.10.0 | **Source**: [`protection/ProtectionChecker.java`](../src/main/java/com/hyperfactions/protection/ProtectionChecker.java), [`data/FactionPermissions.java`](../src/main/java/com/hyperfactions/data/FactionPermissions.java)

How faction territory claims are protected. For zone protection, see [protection-zones.md](protection-zones.md). For cross-cutting concerns (wilderness, explosions, fire), see [protection-global.md](protection-global.md).

## Overview

Faction claims are chunk-based territory owned by factions via `/f claim`. Protection priority:

```
Zone > Claim > Wilderness
```

- **Zone**: Admin SafeZone/WarZone with 40 flags — checked first, overrides claims
- **Claim**: Faction territory with 53 permission flags — checked only when NOT in a zone
- **Wilderness**: Unclaimed land — no protection, all interactions allowed

Source: `ProtectionChecker.canInteractChunk()` lines 142–287

---

## Faction Permissions (53 Flags)

Source: [`FactionPermissions.java`](../src/main/java/com/hyperfactions/data/FactionPermissions.java) `ALL_FLAGS` constant

### Per-Level Interaction Flags (4 levels x 11 = 44 flags)

Each level has the same 11 flag suffixes. Flag name = `{level}{Suffix}` (e.g., `memberBreak`, `allyDoorUse`).

| Suffix | Controls | Parent |
|--------|----------|--------|
| `Break` | Block breaking | — |
| `Place` | Block placement | — |
| `Interact` | General block interaction (parent) | — |
| `DoorUse` | Doors and gates | `{level}Interact` |
| `ContainerUse` | Chests and storage | `{level}Interact` |
| `BenchUse` | Crafting tables | `{level}Interact` |
| `ProcessingUse` | Furnaces, smelters | `{level}Interact` |
| `SeatUse` | Seats and mounts | `{level}Interact` |
| `TransportUse` | Teleporters and portals | `{level}Interact` |
| `CrateUse` | Capture crate pickup and placement (mixin) | — |
| `NpcTame` | F-key NPC taming (mixin) | — |

**Levels**: `outsider`, `ally`, `member`, `officer`

### Default Values

| Flag Suffix | Outsider | Ally | Member | Officer |
|-------------|----------|------|--------|---------|
| `Break` | false | false | **true** | **true** |
| `Place` | false | false | **true** | **true** |
| `Interact` | false | **true** | **true** | **true** |
| `DoorUse` | false | **true** | **true** | **true** |
| `ContainerUse` | false | false | **true** | **true** |
| `BenchUse` | false | false | **true** | **true** |
| `ProcessingUse` | false | false | **true** | **true** |
| `SeatUse` | false | **true** | **true** | **true** |
| `TransportUse` | false | **true** | **true** | **true** |
| `CrateUse` | false | false | **true** | **true** |
| `NpcTame` | false | false | **true** | **true** |

**Summary**: Members/officers get full access. Allies can interact, use doors/seats/transport but cannot break/place/access containers/benches/furnaces/crates/taming. Outsiders are denied everything.

### Mob Spawning Flags (4 flags)

| Flag | Default | Parent |
|------|---------|--------|
| `mobSpawning` | true | — |
| `hostileMobSpawning` | true | `mobSpawning` |
| `passiveMobSpawning` | true | `mobSpawning` |
| `neutralMobSpawning` | true | `mobSpawning` |

### Global Flags (2 flags)

| Flag | Default | Notes |
|------|---------|-------|
| `pvpEnabled` | true | Per-faction territory PvP toggle |
| `officersCanEdit` | false | Whether officers can modify faction permissions |

### Treasury Flags (3 flags)

| Flag | Default | Notes |
|------|---------|-------|
| `treasuryDeposit` | true | Members can deposit into treasury |
| `treasuryWithdraw` | false | Officers can withdraw from treasury |
| `treasuryTransfer` | false | Officers can transfer to other factions |

Treasury flags are exposed in the **TreasurySettingsPage** GUI (accessible via `/f treasury settings` or `/f settings` treasury tab). They can also be configured via `faction-permissions.json` defaults/locks. See [Managing Permissions](#managing-faction-permissions).

---

## Parent-Child Flag Hierarchy

Source: `FactionPermissions.getParentFlag()` lines 363–378, `get()` lines 292–298

When a flag has a parent, `FactionPermissions.get()` checks the parent first. If the parent is `false`, the child returns `false` **regardless of its stored value**.

```java
// FactionPermissions.get() — line 292
public boolean get(@NotNull String flagName) {
    String parent = getParentFlag(flagName);
    if (parent != null && !getRaw(parent)) {
        return false;  // Parent is false → child is implicitly false
    }
    return getRaw(flagName);  // Otherwise return stored value
}
```

### Relationships

```
{level}Interact (parent)
├── {level}DoorUse
├── {level}ContainerUse
├── {level}BenchUse
├── {level}ProcessingUse
├── {level}SeatUse
└── {level}TransportUse

mobSpawning (parent)
├── hostileMobSpawning
├── passiveMobSpawning
└── neutralMobSpawning
```

### Gotcha Example

A faction sets `memberInteract=false` but `memberDoorUse=true`:

```
get("memberDoorUse")
  → parent = getParentFlag("memberDoorUse") = "memberInteract"
  → getRaw("memberInteract") = false
  → return false  (parent disabled — door access DENIED despite doorUse=true)
```

The GUI handles this correctly by disabling child toggles when the parent is off.

---

## Interaction Check Flow

Source: `ProtectionChecker.canInteractChunk()` lines 142–287

When a player performs any action in claimed territory, this is the exact check order:

### Step 1: Admin Bypass (lines 145–155)

```
Is player admin? (has "hyperfactions.admin.use")
  → YES: Is admin bypass toggle ON? (/f admin bypass)
    → YES: ALLOWED_BYPASS (skip all checks)
    → NO: Fall through to zone/claim checks (admins with bypass OFF get NO special treatment)
  → NO: Continue to Step 2
```

**Key**: Admins do NOT use standard bypass permissions. Only the explicit toggle matters.

### Step 2: Standard Bypass Permissions (lines 157–171)

Non-admin players only. Checked by interaction type:

| InteractionType | Bypass Permission |
|-----------------|-------------------|
| BUILD | `hyperfactions.bypass.build` |
| INTERACT, DOOR, BENCH, PROCESSING, SEAT, TELEPORTER, PORTAL | `hyperfactions.bypass.interact` |
| CONTAINER | `hyperfactions.bypass.container` |
| DAMAGE | `hyperfactions.bypass.damage` |
| USE | `hyperfactions.bypass.use` |

Wildcard `hyperfactions.bypass.*` bypasses all types. If granted → `ALLOWED_BYPASS`.

### Step 3: Zone Check (lines 173–209)

If the chunk is in a zone, zone flags take precedence:
- Zone flag disabled → `DENIED_SAFEZONE` or `DENIED_WARZONE`
- WarZone with flag allowed → `ALLOWED_WARZONE` (returns immediately, skips claim checks)
- SafeZone with flag allowed → falls through to claim check below

### Step 4: Claim Ownership (lines 211–217)

```
claimManager.getClaimOwner(world, chunkX, chunkZ)
  → NULL: ALLOWED_WILDERNESS (anyone can interact)
  → UUID: Continue to faction permission checks
```

### Step 5: Same Faction (lines 231–252)

Player is in the owning faction. Role determines which flag level is checked:

```java
boolean isOfficerOrLeader = factionMember.role().getLevel() >= FactionRole.OFFICER.getLevel();
```

- **Officer/Leader** (role level >= 2): Checked against `officer{Suffix}` flags
- **Member** (role level 1): Checked against `member{Suffix}` flags

If the appropriate flag denies the action → `DENIED_NO_PERMISSION`.
If allowed → `ALLOWED_OWN_CLAIM`.

### Step 6: Ally Check (lines 254–267)

Player has a faction and that faction has `ALLY` relation with claim owner:

- Check `ally{Suffix}` flags
- If allowed → `ALLOWED_ALLY_CLAIM`
- If denied → `DENIED_NO_PERMISSION`

### Step 7: Outsider Check (lines 269–272)

Player is not in the owning faction and not allied:

- Check `outsider{Suffix}` flags
- If allowed → `ALLOWED`

### Step 8: Default Deny (lines 274–286)

If outsider flags deny the action:
- Player is `ENEMY` → `DENIED_ENEMY_CLAIM`
- Otherwise → `DENIED_NEUTRAL_CLAIM` (includes factionless players)

---

## InteractionType to Flag Mapping

Source: `ProtectionChecker.checkPermission()` lines 298–310

When the checker evaluates a faction permission, it maps the action type to specific flag name(s):

| InteractionType | Flag(s) Checked | Resolution |
|-----------------|-----------------|------------|
| `BUILD` | `{level}Break` **OR** `{level}Place` | Either flag grants permission |
| `INTERACT` | `{level}Interact` | Parent flag — gates 6 children |
| `USE` | `{level}Interact` | Same as INTERACT |
| `DOOR` | `{level}DoorUse` | Child of Interact |
| `CONTAINER` | `{level}ContainerUse` | Child of Interact |
| `BENCH` | `{level}BenchUse` | Child of Interact |
| `PROCESSING` | `{level}ProcessingUse` | Child of Interact |
| `SEAT` | `{level}SeatUse` | Child of Interact |
| `TELEPORTER` | `{level}TransportUse` | Child of Interact |
| `PORTAL` | `{level}TransportUse` | Child of Interact |
| `DAMAGE` | Hardcoded: non-outsiders always allowed | Outsiders always denied |

**BUILD note**: `checkPermission()` uses `perms.get(level + "Break") || perms.get(level + "Place")`. This means having EITHER break OR place permission allows the BUILD interaction type. This is because the ECS systems may route both break and place events through the same BUILD type.

**DAMAGE note**: Entity damage (non-player) uses `!"outsider".equals(level)` — members, officers, and allies can always damage entities in claims. Outsiders cannot. There is no configurable flag for this.

---

## Faction Roles

Source: [`FactionRole.java`](../src/main/java/com/hyperfactions/data/FactionRole.java) lines 8–11

| Role | Level | Permission Check Level |
|------|-------|------------------------|
| LEADER | 3 | `officer` flags (same as officer) |
| OFFICER | 2 | `officer` flags |
| MEMBER | 1 | `member` flags |

Leaders and officers both use `officer{Suffix}` flags. There are no separate leader-specific permission flags.

---

## PvP in Claimed Territory

Source: `ProtectionChecker.canDamagePlayerChunk()` lines 354–449

### Check Order

1. **Spawn protection** (line 360): Defender has spawn protection → `DENIED_SPAWN_PROTECTED`
2. **Break attacker spawn protection** (line 365): If config `spawnProtection.breakOnAttack=true` and attacker has spawn protection, it's cleared
3. **Zone PvP** (lines 369–408): If in a zone, zone flags override (including friendly fire hierarchy)
4. **Territory PvP flag** (lines 412–426): Claim owner's `pvpEnabled` faction permission checked. If false → `DENIED_TERRITORY_NO_PVP`
5. **Same faction** (lines 428–433): `ConfigManager.isFactionDamage(world)` — per-world override support. If false → `DENIED_SAME_FACTION`
6. **Ally check** (lines 435–443): `ConfigManager.isAllyDamage(world)` — per-world override support. If false → `DENIED_ALLY`
7. **Default allow** (line 448): `ALLOWED`

### PvP Configuration Layers

| Setting | Type | Scope | Default | Where Set |
|---------|------|-------|---------|-----------|
| `pvpEnabled` | Faction permission | Per-faction, per-territory | true | `/f settings` GUI |
| `factionDamage` | Server config | Global, per-world override | false | `config/factions.json` |
| `allyDamage` | Server config | Global, per-world override | false | `config/factions.json` |

**Note**: `pvpEnabled` is a faction permission flag and can be toggled by faction officers. `factionDamage` and `allyDamage` are server configs in `config/factions.json` that factions cannot control. Per-world overrides for faction/ally damage use `ConfigManager.isFactionDamage(world)` and `ConfigManager.isAllyDamage(world)`.

---

## Item Pickup in Claims

Source: `ProtectionChecker.canPickupItem()` lines 517–583

Pickup checks are **faction-relationship-based**, not permission-flag-based:

1. Admin bypass toggle → allow
2. `hyperfactions.bypass.pickup` or `hyperfactions.bypass.*` → allow
3. Zone flag (`ITEM_PICKUP` for auto, `ITEM_PICKUP_MANUAL` for F-key) → if zone denies, blocked
4. Wilderness (no claim owner) → allow
5. Same faction → always allow
6. Ally faction → always allow
7. **Outsider/enemy/neutral → controlled by `claims.outsiderPickupAllowed`** in `config/factions.json`

The `outsiderPickupAllowed` setting (default: `true`, migrated servers: `false`) controls whether outsiders can pick up items in claimed territory. Previously this was hardcoded to deny.

---

## Mob Spawning in Claims

Source: `ProtectionChecker.shouldBlockSpawn()` (see protection-systems.md for line reference)

1. Zone flags checked first (`ZoneFlags.MOB_SPAWNING` / `ZoneFlags.NPC_SPAWNING`)
2. Claim owner's faction permissions checked: `mobSpawning` parent + sub-type children
3. Parent-child resolution applies: `mobSpawning=false` blocks all spawning types
4. Wilderness → no restriction

---

## Server-Wide Configuration

> **Note**: As of v0.10.0 (Migration V5 -> V6), `config.json` has been split into `config/factions.json` (faction gameplay settings) and `config/server.json` (server behavior settings). The old `config.json` is deleted after migration. `CoreConfig` is deprecated in favor of `FactionsConfig` and `ServerConfig`.

### FactionsConfig (`config/factions.json`)

Source: [`FactionsConfig.java`](../src/main/java/com/hyperfactions/config/FactionsConfig.java)

#### Claim Settings

| Key | Type | Default | Purpose |
|-----|------|---------|---------|
| `claims.maxClaims` | int | 100 | Maximum claims per faction |
| `claims.onlyAdjacent` | bool | false | Claims must be adjacent to existing claims |
| `claims.preventDisconnect` | bool | false | Prevent claim gaps in faction territory |
| `claims.decayEnabled` | bool | true | Auto-remove inactive faction claims |
| `claims.decayDaysInactive` | int | 30 | Days of inactivity before decay |
| `claims.worldWhitelist` | List | [] | Worlds where claiming is allowed (if non-empty, overrides blacklist) |
| `claims.worldBlacklist` | List | [] | Worlds where claiming is forbidden |

#### Claim Protection Settings (9 flags)

These flags control server-wide protection behavior in claimed territory. They are NOT faction permissions — factions cannot change them.

| Key | Type | Default | Migrated Default | Purpose |
|-----|------|---------|------------------|---------|
| `claims.outsiderPickupAllowed` | bool | true | false | Whether outsiders can pick up items in claims |
| `claims.outsiderDropAllowed` | bool | true | — | Whether outsiders can drop items in claims |
| `claims.factionlessExplosionsAllowed` | bool | false | — | Allow explosion damage from factionless sources in claims |
| `claims.enemyExplosionsAllowed` | bool | false | — | Allow explosion damage from enemy faction sources in claims |
| `claims.neutralExplosionsAllowed` | bool | false | — | Allow explosion damage from neutral faction sources in claims |
| `claims.fireSpreadAllowed` | bool | true | false | Allow fire to spread in claimed territory |
| `claims.factionlessDamageAllowed` | bool | true | — | Allow entity damage from factionless players in claims |
| `claims.enemyDamageAllowed` | bool | true | — | Allow entity damage from enemy faction players in claims |
| `claims.neutralDamageAllowed` | bool | true | — | Allow entity damage from neutral faction players in claims |

**Migrated Default** column: when a server is migrated from V5 to V6, `outsiderPickupAllowed` and `fireSpreadAllowed` are set to `false` to preserve the previous hardcoded behavior. Fresh installs use the regular defaults.

**Explosion limitation**: Explosion hooks do not provide a player context, so `shouldBlockExplosion()` performs a combined 3-way check: if ANY of the three explosion flags (`factionlessExplosionsAllowed`, `enemyExplosionsAllowed`, `neutralExplosionsAllowed`) is true, explosions are allowed. All three must be false to block explosions. This replaces the old single `allowExplosionsInClaims` flag.

#### Combat Settings

| Key | Type | Default | Purpose |
|-----|------|---------|---------|
| `combat.factionDamage` | bool | false | Same-faction PvP allowed |
| `combat.allyDamage` | bool | false | Ally faction PvP allowed |
| `combat.tagDurationSeconds` | int | 15 | Combat tag duration |
| `combat.taggedLogoutPenalty` | bool | true | Penalize tagged player logout |
| `combat.logoutPowerLoss` | double | 1.0 | Power lost on tagged logout |
| `combat.neutralAttackPenalty` | double | 0.0 | Power lost attacking neutrals |

#### Spawn Protection

| Key | Type | Default | Purpose |
|-----|------|---------|---------|
| `combat.spawnProtection.enabled` | bool | true | Enable spawn protection |
| `combat.spawnProtection.durationSeconds` | int | 5 | Duration after respawn |
| `combat.spawnProtection.breakOnAttack` | bool | true | Remove if player attacks |
| `combat.spawnProtection.breakOnMove` | bool | true | Remove if player moves |

#### Power Settings

| Key | Type | Default | Purpose |
|-----|------|---------|---------|
| `power.powerLossOnMobDeath` | bool | true | Power lost to mob kills |
| `power.powerLossOnEnvironmentalDeath` | bool | true | Power lost to fall/drown |

### FactionPermissionsConfig (`config/faction-permissions.json`)

Source: [`FactionPermissionsConfig.java`](../src/main/java/com/hyperfactions/config/modules/FactionPermissionsConfig.java), loaded at [`ConfigManager.java`](../src/main/java/com/hyperfactions/config/ConfigManager.java)

**Separate file** from `config/factions.json`. Three sections:

#### `defaults` Section

Server-wide defaults for **new factions**. Also the value used when a flag is locked.

```json
{
  "defaults": {
    "outsider": {
      "break": false, "place": false, "interact": false,
      "doorUse": false, "containerUse": false, "benchUse": false,
      "processingUse": false, "seatUse": false, "transportUse": false
    },
    "ally": {
      "break": false, "place": false, "interact": true,
      "doorUse": true, "containerUse": false, "benchUse": false,
      "processingUse": false, "seatUse": true, "transportUse": true
    },
    "member": {
      "break": true, "place": true, "interact": true,
      "doorUse": true, "containerUse": true, "benchUse": true,
      "processingUse": true, "seatUse": true, "transportUse": true
    },
    "officer": {
      "break": true, "place": true, "interact": true,
      "doorUse": true, "containerUse": true, "benchUse": true,
      "processingUse": true, "seatUse": true, "transportUse": true
    },
    "mobSpawning": {
      "enabled": true, "hostile": true,
      "passive": true, "neutral": true
    },
    "pvpEnabled": true,
    "officersCanEdit": false,
    "treasury": {
      "deposit": true,
      "withdraw": false,
      "transfer": false
    }
  }
}
```

Supports both nested JSON format (above) and legacy flat format (`"outsiderBreak": false`).

#### `treasury` Section

Treasury permission flags are also configured in `faction-permissions.json` under both `defaults` and `locks`. The treasury key controls deposit, withdraw, and transfer permissions:

| Key | Default | Purpose |
|-----|---------|---------|
| `treasury.deposit` | true | Whether members can deposit into faction treasury |
| `treasury.withdraw` | false | Whether officers can withdraw from treasury |
| `treasury.transfer` | false | Whether officers can transfer to other factions |

These flags are exposed in-game via the **TreasurySettingsPage** GUI.

#### `locks` Section

Which flags are **server-locked** (factions cannot change them). Structure mirrors `defaults`. Any flag set to `true` here forces the default value.

```json
{
  "locks": {
    "outsider": { "break": false },
    "pvpEnabled": false
  }
}
```

All locks default to `false` (unlocked).

#### Lock Resolution

Source: `FactionPermissionsConfig.getEffectiveFactionPermissions()` lines 163–173

```java
public FactionPermissions getEffectiveFactionPermissions(FactionPermissions factionPerms) {
    Map<String, Boolean> effective = new HashMap<>(factionPerms.toMap());
    for (Map.Entry<String, Boolean> entry : locks.entrySet()) {
        if (entry.getValue()) {
            // Flag is locked — override with server default
            effective.put(entry.getKey(), defaults.getOrDefault(entry.getKey(), false));
        }
    }
    return new FactionPermissions(effective);
}
```

**When locked**: The faction's stored value is ignored, server default is used.
**When unlocked**: Faction's stored value is used.

---

## Managing Faction Permissions

Source: [`FactionSettingsPage.java`](../src/main/java/com/hyperfactions/gui/faction/page/FactionSettingsPage.java)

### GUI Access

- **Command**: `/f settings`
- **Required role**: Officer or higher (role level >= 2)
- **Officers see**: All 42 flags (36 per-level + 4 mob spawning + pvpEnabled + officersCanEdit as disabled)
- **Leaders see**: All 42 flags including `officersCanEdit` as editable
- **Treasury flags**: 3 treasury flags (`treasuryDeposit`, `treasuryWithdraw`, `treasuryTransfer`) are exposed in the **TreasurySettingsPage** GUI (accessible via `/f treasury settings` or `/f settings` treasury tab)

### No CLI Command

There is no `/f perm` or similar CLI command for editing faction permissions. The settings GUI is the only in-game interface.

### Server Lock Behavior

When a flag is locked via `faction-permissions.json`:
- The toggle appears **disabled** in the GUI
- Attempting to toggle shows: "This setting is locked by the server."
- The effective value is always the server default

---

## Mixin-Dependent Claim Protections

Some protections require a mixin system to be installed. Without one, certain protections have **no enforcement at all**.

Source: [`HyperProtectIntegration.java`](../src/main/java/com/hyperfactions/integration/protection/HyperProtectIntegration.java), [`OrbisMixinsIntegration.java`](../src/main/java/com/hyperfactions/integration/protection/OrbisMixinsIntegration.java)

| Protection | No Mixin | OrbisGuard-Mixins | HyperProtect-Mixin | Checks Claims? |
|---|---|---|---|---|
| Block break | ECS system | Mixin hook | Mixin hook (slot 0) | **Yes** — faction perms |
| Block place | ECS system | Mixin hook | Mixin hook (slot 18) | **Yes** — faction perms |
| Block use (doors, etc.) | ECS system | Mixin hook | Mixin hook (slot 20) | **Yes** — faction perms |
| Container access | ECS system | — | Mixin hook (slot 7) | **Yes** — faction perms |
| Container open | — | — | Mixin hook (slot 17) | **Yes** — faction perms |
| Item pickup (auto) | ECS system | Mixin hook | Mixin hook (slot 4) | **Yes** — faction relationship |
| Item pickup (F-key) | ECS system | Mixin hook | Mixin hook (slot 4) | **Yes** — faction relationship |
| Crop harvest | Codec replacement | Mixin hook | Mixin hook | **Yes** — via canInteract() |
| Hammer use | **None** | Mixin hook | Mixin hook (slot 19) | **Yes** — faction perms |
| Builder tools | **None** | — | Mixin hook (slot 3) | **Yes** — faction perms |
| Explosions | **None** | Mixin hook | Mixin hook (slot 1) | **Yes** — 3-way config |
| Fire spread | **None** | — | Mixin hook (slot 2) | **Yes** — configurable |
| Teleporter use | **None** | — | Mixin hook (slot 9) | **Yes** — faction perms |
| Portal use | **None** | — | Mixin hook (slot 10) | **Yes** — faction perms |
| Entity damage | ECS system | — | Mixin hook (slot 16) | **Yes** — faction perms |
| Seat protection | ECS system | Mixin hook | Mixin hook (slot 21) | **Yes** — faction perms |
| Mob spawn suppression | **None** | Mixin hook | Mixin hook (slot 8) | **Yes** — faction perms |
| Respawn override | — | — | Mixin hook (slot 22) | **Yes** |
| Item drop | Zone flag only | Zone flag only | Zone flag only | **Partial** — zone flag + `outsiderDropAllowed` config |
| Keep inventory | **None** | Mixin hook | Mixin hook (slot 5) | **No** — zone-only |
| Durability loss | **None** | Mixin hook | Mixin hook (slot 6) | **No** — zone-only |
| Command blocking | — | Mixin hook | Mixin hook (slot 11) | **No** — combat tag only |

**"None"** means no enforcement exists for that feature without a mixin. The protection simply doesn't happen.

---

## Known Limitations & Gaps

### Zone-Only Protections (No Claim Equivalent)

These protections work in admin zones but have **no faction permission or config equivalent for claims**. In claimed territory outside a zone, these features are unprotected.

| Feature | Zone Support | Claim Support | Source |
|---------|-------------|---------------|--------|
| Keep inventory on death | `KEEP_INVENTORY` zone flag | **None** — returns `false` | `shouldKeepInventory()` line 940–952 |
| Durability prevention | `INVINCIBLE_ITEMS` zone flag | **None** — returns `false` | `shouldPreventDurability()` line 960–972 |
| Item drop prevention | `ITEM_DROP` zone flag | **Partial** — `outsiderDropAllowed` config | `ItemDropProtectionSystem` |
| Fall damage prevention | `FALL_DAMAGE` zone flag | **None** — zone-only | `FallDamageProtection` |
| Environmental damage prevention | `ENVIRONMENTAL_DAMAGE` zone flag | **None** — zone-only | `EnvironmentalDamageProtection` |
| Projectile damage prevention | `PROJECTILE_DAMAGE` zone flag | **None** — zone-only | `ProjectileDamageProtection` |
| Mob damage prevention | `MOB_DAMAGE` zone flag | **None** — zone-only | `MobDamageProtection` |

### Asymmetries & Known Limitations

| Issue | Details | Source |
|-------|---------|--------|
| **Pickup vs Drop** | Item PICKUP checks faction relationships + `outsiderPickupAllowed` config. Item DROP checks zone flags + `outsiderDropAllowed` config. | `canPickupItem()` vs `ItemDropProtectionSystem` |
| **Explosion source attribution (KNOWN LIMITATION)** | Explosion hooks do not provide a player UUID, so `shouldBlockExplosion()` cannot determine the source faction. Instead it performs a combined 3-way check: if ANY of `factionlessExplosionsAllowed`, `enemyExplosionsAllowed`, or `neutralExplosionsAllowed` is true, explosions are allowed. All three must be false to block. This is a platform limitation, not a bug. | `shouldBlockExplosion()` |
| **DAMAGE type for outsiders** | Outsider entity damage (non-player) is now controlled by 3 config flags: `factionlessDamageAllowed`, `enemyDamageAllowed`, `neutralDamageAllowed` in `config/factions.json`. | `checkPermission()` |

### Config vs Faction Permission Confusion

Some claim protections are per-faction (toggleable by officers), others are server-wide (only admin can change), and some are hardcoded:

| Protection | Control Type | Who Controls |
|------------|-------------|--------------|
| Block interactions | **Faction permission** (45 flags) | Faction officers via GUI |
| PvP in territory | **Faction permission** (`pvpEnabled`) | Faction officers via GUI |
| Mob spawning | **Faction permission** (4 flags) | Faction officers via GUI |
| Same-faction PvP | **Server config** (`combat.factionDamage`) | Server admin via `config/factions.json` |
| Ally PvP | **Server config** (`combat.allyDamage`) | Server admin via `config/factions.json` |
| Explosions | **Server config** (3-way: `claims.factionlessExplosionsAllowed`, `claims.enemyExplosionsAllowed`, `claims.neutralExplosionsAllowed`) | Server admin via `config/factions.json` |
| Fire spread | **Server config** (`claims.fireSpreadAllowed`) | Server admin via `config/factions.json` |
| Outsider entity damage | **Server config** (3-way: `claims.factionlessDamageAllowed`, `claims.enemyDamageAllowed`, `claims.neutralDamageAllowed`) | Server admin via `config/factions.json` |
| Item pickup | **Server config** (`claims.outsiderPickupAllowed`) | Server admin via `config/factions.json` |
| Item drop | **Server config** (`claims.outsiderDropAllowed`) + zone flags | Server admin via `config/factions.json` / zone admins |
| Keep inventory | **Zone-only** (no claim protection) | Zone admins only |
| Durability | **Zone-only** (no claim protection) | Zone admins only |

### Backward-Compatibility Accessor Methods

Source: `FactionPermissions.java` lines 498–515

The `outsiderBreak()`, `memberInteract()`, etc. accessor methods use `getRaw()` — they **bypass parent-child logic**. Code using these accessors instead of `get()` will not respect the parent-child hierarchy. The main `checkPermission()` in ProtectionChecker correctly uses `get()`, but any external code using the named accessors should be verified.

---

## Code Links

| Class | Path |
|-------|------|
| FactionPermissions | [`data/FactionPermissions.java`](../src/main/java/com/hyperfactions/data/FactionPermissions.java) |
| FactionRole | [`data/FactionRole.java`](../src/main/java/com/hyperfactions/data/FactionRole.java) |
| ProtectionChecker | [`protection/ProtectionChecker.java`](../src/main/java/com/hyperfactions/protection/ProtectionChecker.java) |
| FactionPermissionsConfig | [`config/modules/FactionPermissionsConfig.java`](../src/main/java/com/hyperfactions/config/modules/FactionPermissionsConfig.java) |
| CoreConfig (deprecated) | [`config/CoreConfig.java`](../src/main/java/com/hyperfactions/config/CoreConfig.java) |
| FactionsConfig | [`config/FactionsConfig.java`](../src/main/java/com/hyperfactions/config/FactionsConfig.java) |
| ServerConfig | [`config/ServerConfig.java`](../src/main/java/com/hyperfactions/config/ServerConfig.java) |
| ConfigManager | [`config/ConfigManager.java`](../src/main/java/com/hyperfactions/config/ConfigManager.java) |
| FactionSettingsPage | [`gui/faction/page/FactionSettingsPage.java`](../src/main/java/com/hyperfactions/gui/faction/page/FactionSettingsPage.java) |
