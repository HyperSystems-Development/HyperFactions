# Global & Cross-Cutting Protection

> **Version**: 0.10.0 | **Source**: [`protection/ProtectionChecker.java`](../src/main/java/com/hyperfactions/protection/ProtectionChecker.java)

Cross-cutting protection concerns that span zones, claims, and wilderness. For zone-specific flags, see [protection-zones.md](protection-zones.md). For claim permissions, see [protection-claims.md](protection-claims.md).

---

## Wilderness Behavior

Source: `ProtectionChecker.canInteractChunk()`

**Wilderness** = any chunk that is NOT in a zone AND NOT claimed by a faction.

When `claimManager.getClaimOwner()` returns `null`:
- Result is `ALLOWED_WILDERNESS`
- **No protection at all** — any player can build, break, interact, etc.
- No faction permissions are checked
- No damage restrictions apply

There is no configuration to protect wilderness areas. If wilderness protection is needed, use admin zones.

---

## Explosion Protection Matrix

Source: `ProtectionChecker.shouldBlockExplosion()`

| Location | Behavior | Source of Truth |
|----------|----------|-----------------|
| **Zone** | Zone flag `explosion_damage` (per zone) | Zone data |
| **Claim** | 3 granular flags: `factionlessExplosionsAllowed`, `enemyExplosionsAllowed`, `neutralExplosionsAllowed` | `config/factions.json` (`claims` section) |
| **Wilderness** | Always allowed | Hardcoded |

**Claim behavior**: NOT a faction permission. Factions cannot control this independently — these are server-wide settings. Three granular flags in `config/factions.json` under `claims` control which relationship types allow explosions: `factionlessExplosionsAllowed`, `enemyExplosionsAllowed`, `neutralExplosionsAllowed`. Default: all three are `false` (explosions blocked in claims). Note: explosion hooks lack player context, so the 3-way check is combined — all 3 flags must be `false` to fully block explosions in claims.

**Mixin required**: Without a mixin system, explosion blocking has no enforcement in either zones or claims.

---

## Fire Spread Protection Matrix

Source: `ProtectionChecker.shouldBlockFireSpread()`

| Location | Behavior | Configurable? |
|----------|----------|---------------|
| **Zone** | Zone flag `fire_spread` (per zone) | Yes (zone flag) |
| **Claim** | Configurable via `fireSpreadAllowed` | Yes (`config/factions.json`, `claims` section) |
| **Wilderness** | Always allowed | N/A |

**Claim behavior**: Controlled by `fireSpreadAllowed` in `config/factions.json` under the `claims` section. Default: `true` for new installs, `false` for migrated configurations.

**Mixin required**: Without HyperProtect-Mixin, fire spread blocking has no enforcement (OrbisGuard-Mixins does not provide a fire spread hook).

---

## Keep Inventory / Durability

### Keep Inventory on Death

Source: `ProtectionChecker.shouldKeepInventory()`

| Location | Behavior |
|----------|----------|
| **Zone** | Zone flag `keep_inventory` (SafeZone default: true, WarZone default: false) |
| **Claim** | Returns `false` — items always drop |
| **Wilderness** | Returns `false` — items always drop |

No faction permission or config controls keep-inventory in claims.

### Durability Prevention

Source: `ProtectionChecker.shouldPreventDurability()`

| Location | Behavior |
|----------|----------|
| **Zone** | Zone flag `invincible_items` (SafeZone default: true, WarZone default: false) |
| **Claim** | Returns `false` — items always degrade |
| **Wilderness** | Returns `false` — items always degrade |

No faction permission or config controls durability in claims.

**Mixin required**: Both features require a mixin system. Without one, no enforcement occurs anywhere.

---

## Spawn Protection

Source: `ProtectionChecker.canDamagePlayerChunk()`, [`SpawnProtection.java`](../src/main/java/com/hyperfactions/protection/SpawnProtection.java)

Temporary immunity after respawning. Applies everywhere (zones, claims, wilderness).

### Configuration

| Key | Type | Default | Purpose |
|-----|------|---------|---------|
| `combat.spawnProtection.enabled` | bool | true | Enable/disable spawn protection |
| `combat.spawnProtection.durationSeconds` | int | 5 | Protection duration after respawn |
| `combat.spawnProtection.breakOnAttack` | bool | true | Remove protection if player attacks |
| `combat.spawnProtection.breakOnMove` | bool | true | Remove protection if player moves |

### Protection Removal Triggers

1. Duration expires
2. Player leaves spawn chunk (if `breakOnMove = true`)
3. Player attacks another player (if `breakOnAttack = true`)

### Check Priority

Spawn protection is checked **first** in PvP checks, before zone or claim checks:

```
canDamagePlayerChunk():
  1. Defender has spawn protection? → DENIED_SPAWN_PROTECTED
  2. Break attacker's spawn protection if they attack
  3. Zone checks...
  4. Claim checks...
```

---

## Combat Tagging

Source: `CombatTagManager` (referenced in `ProtectionChecker`)

### Configuration

| Key | Type | Default | Purpose |
|-----|------|---------|---------|
| `combat.tagDurationSeconds` | int | 15 | How long combat tag lasts |
| `combat.taggedLogoutPenalty` | bool | true | Penalize logout while tagged |
| `combat.logoutPowerLoss` | double | 1.0 | Power lost on tagged logout |
| `combat.neutralAttackPenalty` | double | 0.0 | Power lost when attacking neutral players |

### Command Blocking (Combat Tag Only)

Source: `ProtectionChecker.checkCommandBlock()`

Commands can be blocked for combat-tagged players. The check includes:
1. **Admin bypass** — if admin bypass toggle is ON, commands are never blocked
2. **Bypass permission** — `hyperfactions.bypass.command` or `hyperfactions.bypass.*` allows all commands
3. **Combat tag check** — tagged players are blocked from teleport commands (`/f home`, `/home`, `/spawn`, `/tp`, `/tpa`)

---

## Death & Power Loss

Source: [`PlayerDeathSystem.java`](../src/main/java/com/hyperfactions/protection/ecs/PlayerDeathSystem.java)

### Death Cause Tracking Pipeline

```
DamageProtectionHandler → records damage type → CombatTagManager
PlayerDeathSystem → reads last damage type → applies power loss
```

| Death Cause | Power Loss? | Config Key |
|-------------|-------------|------------|
| PvP | Always | — |
| Mob | Configurable | `power.powerLossOnMobDeath` (default: true) |
| Environmental (fall, drown) | Configurable | `power.powerLossOnEnvironmentalDeath` (default: true) |
| Unknown | Always | — |

### Zone Override

Zone flag `power_loss=false` takes priority over config settings. If a player dies in a zone with `power_loss=false`, no power loss occurs regardless of death cause or config.

### Death Effects (Always Fire)

Even when power loss is skipped:
- Kill/death counters still update
- Gravestone announcements still fire
- Only the power penalty is omitted

---

## Damage Protection in Claims vs Zones

Source: [`DamageProtectionHandler.java`](../src/main/java/com/hyperfactions/protection/damage/DamageProtectionHandler.java)

### Coverage Comparison

| Damage Type | Zone Protection | Claim Protection | Handler |
|-------------|----------------|------------------|---------|
| Fall damage | `fall_damage` flag | **None** | `FallDamageProtection` |
| Environmental (drown, suffocate) | `environmental_damage` flag | **None** | `EnvironmentalDamageProtection` |
| Projectile | `projectile_damage` flag | **None** (unless PvP source) | `ProjectileDamageProtection` |
| Mob → Player | `mob_damage` flag | **None** | `MobDamageProtection` |
| Player → Player (PvP) | Zone PvP + friendly fire flags | Faction `pvpEnabled` + config | `PvPDamageProtection` |

**Only PvP damage** has claim-level protection. All other damage types are **zone-only**.

---

## Bypass Permissions

Source: `ProtectionChecker.canInteractChunk()`

### Standard Bypass Permissions (Non-Admin)

| Permission | Bypasses |
|------------|----------|
| `hyperfactions.bypass.build` | Block place/break protection |
| `hyperfactions.bypass.interact` | Door, bench, processing, seat, light, mount, teleporter, portal, crate, NPC tame, NPC interact, item drop, item pickup protection |
| `hyperfactions.bypass.container` | Chest/storage access protection |
| `hyperfactions.bypass.damage` | Entity damage protection (DAMAGE and PVE_DAMAGE) |
| `hyperfactions.bypass.use` | Item use protection (USE type) |
| `hyperfactions.bypass.pickup` | Item pickup protection in `canPickupItem()` (auto + F-key) |
| `hyperfactions.bypass.command` | Combat tag command blocking |
| `hyperfactions.bypass.*` | All of the above |

### Admin Bypass (Separate Mechanism)

Source: `ProtectionChecker.canInteractChunk()`

- Requires `hyperfactions.admin.use` permission
- Must explicitly toggle ON via `/f admin bypass`
- Admins with bypass OFF get **no special treatment** — standard bypass permissions do NOT apply to admins
- This is intentional: prevents accidental bypass when admin is playing normally

### Bypass Check Order

```
Is admin? (has hyperfactions.admin.use)
  → YES: Check admin bypass toggle ONLY
    → Toggle ON → ALLOWED_BYPASS
    → Toggle OFF → normal protection checks (NO standard bypass)
  → NO: Check standard bypass permissions
    → Has matching bypass perm → ALLOWED_BYPASS
    → No matching perm → normal protection checks
```

**Key**: Admin and non-admin bypass are **mutually exclusive paths**. An admin player will never have standard bypass permissions checked.

---

## Multi-World Settings

Source: `ConfigManager`

### Claiming Restrictions

| Key | Type | Default | Behavior |
|-----|------|---------|----------|
| `claims.worldWhitelist` | List | [] | If non-empty, ONLY these worlds allow claiming |
| `claims.worldBlacklist` | List | [] | These worlds forbid claiming (ignored if whitelist non-empty) |

### Per-World Overrides

Some combat settings support per-world overrides via `ConfigManager`:

| Setting | Method | Fallback |
|---------|--------|----------|
| Faction damage | `ConfigManager.isFactionDamage(world)` | `FactionsConfig.factionDamage` (`config/factions.json`) |
| Ally damage | `ConfigManager.isAllyDamage(world)` | `FactionsConfig.allyDamage` (`config/factions.json`) |
| Power loss enabled | `ConfigManager.isPowerLossEnabledInWorld(world)` | `true` |

Per-world overrides require the `WorldsConfig` module (`config/worlds.json`). If not enabled, the global config value is used.

### Outsider PvP Damage Settings

Three damage flags in `config/factions.json` under the `claims` section control whether outsiders can deal **PvP damage** to other players inside claimed territory. These are checked in `canDamagePlayerChunk()` after faction/ally checks, only when the attacker is not the claim owner:

| Key | Type | Default | Behavior |
|-----|------|---------|----------|
| `claims.factionlessDamageAllowed` | bool | **true** | Allow factionless players to PvP in claims |
| `claims.enemyDamageAllowed` | bool | **true** | Allow enemy faction members to PvP in claims |
| `claims.neutralDamageAllowed` | bool | **true** | Allow neutral faction members to PvP in claims |

**Note**: These control **PvP** damage (player-vs-player), not entity damage. Entity damage by outsiders uses the `DAMAGE` InteractionType which is hardcoded to deny outsiders, or the `PVE_DAMAGE` type which uses per-level `{level}PveDamage` faction permission flags.

### Fluid Spread Protection

Source: `ProtectionChecker.shouldBlockFluidSpread()`

| Location | Behavior | Configurable? |
|----------|----------|---------------|
| **Zone** | Reuses `fire_spread` zone flag — if fire spread is blocked, fluid spread is also blocked | Yes (zone flag) |
| **Claim** | Always allowed — fluid is intentionally placed by players | No |
| **Wilderness** | Always allowed | N/A |

**Mixin required**: HyperProtect-Mixin slot 25 (FluidSpread hook).

---

## OrbisGuard Region Integration

Source: [`OrbisGuardIntegration.java`](../src/main/java/com/hyperfactions/integration/protection/OrbisGuardIntegration.java)

When OrbisGuard is installed, its regions prevent faction claim creation:

- Before a claim is created, chunk center is checked for OrbisGuard protective regions
- If a region exists → claim denied with message
- Reflection-based detection — fail-open (errors allow claims)

This prevents conflicts between OrbisGuard region protection and faction claim protection.

---

## Gravestone Integration

Source: [`GravestoneIntegration.java`](../src/main/java/com/hyperfactions/integration/protection/GravestoneIntegration.java)

When GravestonePlugin is installed:

- Death announcements include faction information
- Gravestone locations are persisted
- Kill/death counts are tracked per player and per faction

---

## Code Links

| Class | Path |
|-------|------|
| ProtectionChecker | [`protection/ProtectionChecker.java`](../src/main/java/com/hyperfactions/protection/ProtectionChecker.java) |
| SpawnProtection | [`protection/SpawnProtection.java`](../src/main/java/com/hyperfactions/protection/SpawnProtection.java) |
| DamageProtectionHandler | [`protection/damage/DamageProtectionHandler.java`](../src/main/java/com/hyperfactions/protection/damage/DamageProtectionHandler.java) |
| PlayerDeathSystem | [`protection/ecs/PlayerDeathSystem.java`](../src/main/java/com/hyperfactions/protection/ecs/PlayerDeathSystem.java) |
| OrbisGuardIntegration | [`integration/protection/OrbisGuardIntegration.java`](../src/main/java/com/hyperfactions/integration/protection/OrbisGuardIntegration.java) |
| GravestoneIntegration | [`integration/protection/GravestoneIntegration.java`](../src/main/java/com/hyperfactions/integration/protection/GravestoneIntegration.java) |
| CoreConfig *(deprecated)* | [`config/CoreConfig.java`](../src/main/java/com/hyperfactions/config/CoreConfig.java) |
| FactionsConfig | [`config/modules/FactionsConfig.java`](../src/main/java/com/hyperfactions/config/modules/FactionsConfig.java) |
| ServerConfig | [`config/modules/ServerConfig.java`](../src/main/java/com/hyperfactions/config/modules/ServerConfig.java) |
| ConfigManager | [`config/ConfigManager.java`](../src/main/java/com/hyperfactions/config/ConfigManager.java) |
