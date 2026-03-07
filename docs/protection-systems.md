# Protection Systems & Architecture

> **Version**: 0.10.0 | Developer-focused reference for the HyperFactions protection implementation.

For admin/config documentation, see [protection-claims.md](protection-claims.md), [protection-zones.md](protection-zones.md), and [protection-global.md](protection-global.md).

## Architecture

```
Hytale ECS Events                  ProtectionMixinBridge (auto-detect)
     │                                    │
     ▼                                    ├─► HyperProtectIntegration (27 hooks + format handle, recommended)
ECS Protection Systems                    │   ├── BlockBreak, BlockPlace, Explosion
├── BlockPlaceProtectionSystem            │   ├── FireSpread, BuilderTools
├── BlockBreakProtectionSystem            │   ├── ItemPickup, DeathDrop, Durability
├── BlockUseProtectionSystem              │   ├── ContainerAccess, ContainerOpen
├── ItemDropProtectionSystem              │   ├── MobSpawn, Command
├── ItemPickupProtectionSystem            │   ├── Teleporter, Portal (unique to HP)
├── HarvestPickupProtectionSystem         │   ├── EntityDamage, Respawn (unique to HP)
├── PlayerDeathSystem                     │   ├── Hammer, Use, Seat
│                                         │   └── Mount, BarterTrade, FluidSpread, PrefabSpawn,
│                                         │       ProjectileLaunch, CraftingResource, MapMarkerFilter
├── PlayerRespawnSystem                   │
└── DamageProtectionSystem                └─► OrbisMixinsIntegration (11 hooks)
     │                                         ├── Pickup, Hammer, Harvest
     ▼                                         ├── Place, Use, Seat
ProtectionChecker (central logic)              ├── Explosion, Command
├── canInteract() ─► ProtectionResult          └── Death, Durability, Spawn
├── canDamagePlayer() ─► PvPResult
├── checkBuild/Place/Hammer() ─► String   Interaction Codec Replacements
├── checkTeleporter/Portal() ─► String    ├── HarvestCrop (only when no mixin active)
├── shouldBlockExplosion() (3-way config) ├── PlaceFluid (bucket protection)
├── shouldBlockFireSpread() (configurable)
├── shouldKeepInventory/PreventDurability
├── canPickupItem() (outsider config)    └── RefillContainer (scoop protection)
├── checkEntityDamage() ─► String
└── getRespawnOverride() ─► double[]
     │
     ├─► ZoneManager (zone flag lookup)
     ├─► ClaimManager (territory ownership)
     ├─► FactionManager (faction membership)
     ├─► RelationManager (faction relations)
     └─► CombatTagManager (spawn protection)
```

---

## Result Enums

### ProtectionResult (11 values)

Source: `ProtectionChecker.java` lines 66–78

```java
ALLOWED               // Outsider permission granted
ALLOWED_BYPASS        // Admin bypass or bypass permission
ALLOWED_WILDERNESS    // Unclaimed territory
ALLOWED_OWN_CLAIM     // Player's faction territory
ALLOWED_ALLY_CLAIM    // Allied faction territory
ALLOWED_WARZONE       // WarZone with permission granted
DENIED_SAFEZONE       // SafeZone blocked
DENIED_WARZONE        // WarZone blocked
DENIED_ENEMY_CLAIM    // Enemy territory
DENIED_NEUTRAL_CLAIM  // Neutral faction territory
DENIED_NO_PERMISSION  // Faction permission denied (member/officer/ally)
```

### PvPResult (9 values)

Source: `ProtectionChecker.java` lines 83–93

```java
ALLOWED                    // Combat allowed
ALLOWED_WARZONE            // WarZone PvP enabled
DENIED_SAFEZONE            // SafeZone PvP disabled
DENIED_SAME_FACTION        // Same faction, no friendly fire
DENIED_ALLY                // Allied faction, ally damage disabled
DENIED_ATTACKER_SAFEZONE   // Attacker in SafeZone
DENIED_DEFENDER_SAFEZONE   // Defender in SafeZone
DENIED_SPAWN_PROTECTED     // Defender has spawn protection
DENIED_TERRITORY_NO_PVP    // Territory pvpEnabled=false
```

### InteractionType (11 values)

Source: `ProtectionChecker.java` lines 98–110

```java
BUILD        // Place/break blocks
INTERACT     // General block interaction (fallback)
CONTAINER    // Open chests, etc.
DOOR         // Use doors/gates
BENCH        // Crafting tables
PROCESSING   // Furnaces/smelters
SEAT         // Seats/mounts
DAMAGE       // Damage entities (not players)
USE          // Use items (fallback)
TELEPORTER   // Use teleporter blocks
PORTAL       // Use portal blocks
```

---

## ECS Protection Systems

Source: `protection/ecs/`

### Registration

Systems are registered in `HyperFactions.init()` on each world:

```java
world.registerSystem(new BlockPlaceProtectionSystem(this, protectionListener));
world.registerSystem(new BlockBreakProtectionSystem(this, protectionListener));
world.registerSystem(new BlockUseProtectionSystem(this, protectionListener));
world.registerSystem(new ItemDropProtectionSystem(this, protectionListener));
world.registerSystem(new ItemPickupProtectionSystem(this, protectionListener));
world.registerSystem(new DamageProtectionSystem(this, protectionListener));
```

### Damage System Group

Damage systems use `DamageModule.get().getFilterDamageGroup()` to run BEFORE damage is applied. Without this, `event.setCancelled(true)` has no effect.

### System Reference

| System | Event | Delegates To | Checks Claims? |
|--------|-------|-------------|----------------|
| BlockPlaceProtectionSystem | PlaceBlockEvent | `canInteract(BUILD)` | Yes |
| BlockBreakProtectionSystem | BreakBlockEvent | `canInteract(BUILD)` | Yes |
| BlockUseProtectionSystem | UseBlockEvent | `canInteract(DOOR/CONTAINER/BENCH/PROCESSING/SEAT/INTERACT)` | Yes |
| ItemDropProtectionSystem | DropItemEvent | `ZoneInteractionProtection.isItemDropAllowed()` + `outsiderDropAllowed` config | **Zone + claim config** |
| ItemPickupProtectionSystem | InteractivelyPickupItemEvent | `canInteract(INTERACT)` | Yes |
| HarvestPickupProtectionSystem | InteractivelyPickupItemEvent (F-key) | `canPickupItem()` | Yes |
| DamageProtectionSystem | Damage event | `DamageProtectionHandler` | PvP only |
| PlayerDeathSystem | DeathComponent | Power loss, kill rewards | Uses config |
| PlayerRespawnSystem | DeathComponent removal | Spawn protection | N/A |

### Block Type Detection (BlockUseProtectionSystem)

Source: `ZoneInteractionProtection.detectBlockTypeFromState()` and `BlockUseProtectionSystem`

The system detects block type from state ID to determine InteractionType:

| Block Pattern | Detected As | InteractionType |
|---|---|---|
| Contains "door" or "gate" | DOOR | `DOOR` |
| Contains "processing" or "processingbench" | PROCESSING | `PROCESSING` |
| Contains "container" or "storage" | CONTAINER | `CONTAINER` |
| Contains "bench" or "crafting" | BENCH | `BENCH` |
| Contains "seat" or "sittable" | SEAT | `SEAT` |
| Everything else | OTHER | `INTERACT` |

---

## Mixin Bridge

Source: [`ProtectionMixinBridge.java`](../src/main/java/com/hyperfactions/integration/protection/ProtectionMixinBridge.java)

### Detection Priority

```
1. System property check (hyperprotect.bridge.active)
2. JAR file presence check (earlyplugins/)
3. OrbisMixinsIntegration.isMixinsAvailable()
```

### Modes

| Mode | Condition | Behavior |
|------|-----------|----------|
| HYPERPROTECT | HP detected, no OG | All 27 HP hooks active |
| ORBISGUARD | OG detected, no HP | All 11 OG hooks active |
| BOTH | HP + OG detected | OG handles 11 features, HP handles unique features |
| NONE | Neither detected | No mixin protection (graceful degradation) |

### Dual-Mixin Mode

When BOTH systems are detected:
- **OrbisGuard handles**: block_break, item_pickup, death_drop, durability, mob_spawn, explosion, command, hammer, use, seat, block_place
- **HyperProtect handles unique**: container_open, entity_damage, teleporter, portal, respawn, interaction_log

---

## HyperProtect-Mixin Integration (30 Slots, 27 Used)

Source: [`HyperProtectIntegration.java`](../src/main/java/com/hyperfactions/integration/protection/HyperProtectIntegration.java)

Verdict protocol: 0=ALLOW, 1=DENY_WITH_MESSAGE, 2=DENY_SILENT, 3=DENY_MOD_HANDLES

| Slot | Hook | ProtectionChecker Method | Checks Claims |
|------|------|--------------------------|---------------|
| 0 | BlockBreak | `checkBuild()` | Yes |
| 1 | Explosion | `shouldBlockExplosion()` | Yes (3-way config) |
| 2 | FireSpread | `shouldBlockFireSpread()` | Yes (configurable) |
| 3 | BuilderTools | `checkBuilderTool()` | Yes |
| 4 | ItemPickup | `canPickupItem()` | Yes |
| 5 | DeathDrop | `shouldKeepInventory()` | No (zone-only) |
| 6 | Durability | `shouldPreventDurability()` | No (zone-only) |
| 7 | ContainerAccess | `checkBench()` | Yes |
| 8 | MobSpawn | `shouldBlockSpawn()` | Yes |
| 9 | Teleporter | `checkTeleporter()` | Yes |
| 10 | Portal | `checkPortal()` | Yes |
| 11 | Command | `checkCommandBlock()` | No (combat tag only) |
| 12 | InteractionLog | `isLogFiltered()` | N/A |
| 15 | FormatHandle | `formatDenyMessage()` (MethodHandle) | N/A — message formatting, not a protection hook |
| 16 | EntityDamage | `checkEntityDamage()` | Yes |
| 17 | ContainerOpen | `checkContainer()` | Yes |
| 18 | BlockPlace | `checkPlace()` | Yes |
| 19 | Hammer | `checkHammer()` | Yes |
| 20 | Use | `checkUse(type)` | Yes (routes CRATE_PICKUP, CRATE_PLACE, NPC_USE, NPC_TAME, NPC_INTERACT, INTERACT) |
| 21 | Seat | `checkSeat()` | Yes |
| 22 | Respawn | `getRespawnOverride()` | Yes |
| 23 | Mount | `checkMount()` | Yes |
| 24 | BarterTrade | `checkBarterTrade()` | Yes |
| 25 | FluidSpread | `shouldBlockFluidSpread()` | Yes |
| 26 | PrefabSpawn | `shouldBlockPrefabSpawn()` | Yes |
| 27 | ProjectileLaunch | `shouldBlockProjectileLaunch()` | Yes |
| 28 | CraftingResource | `checkCraftingResource()` | Yes |
| 29 | MapMarkerFilter | `filterMapMarker()` | N/A — visibility filter |

---

## OrbisGuard-Mixins Integration (11 Hooks)

Source: [`OrbisMixinsIntegration.java`](../src/main/java/com/hyperfactions/integration/protection/OrbisMixinsIntegration.java)

Uses `System.getProperties()` registry with key-based hook lookup.

| Hook | ProtectionChecker Method | Checks Claims |
|------|--------------------------|---------------|
| Pickup | `canPickupItem()` | Yes |
| Hammer | `checkHammer()` | Yes |
| Harvest | `canPickupItem("manual")` | Yes |
| Place | `canPlaceAt()` | Yes |
| Use | `canInteractAt()` | Yes |
| Seat | `canSeatAt()` | Yes |
| Explosion | `shouldBlockExplosion()` | Yes (config) |
| Command | `checkCommandBlock()` | No (combat tag) |
| Death | `shouldKeepInventory()` | No (zone-only) |
| Durability | `shouldPreventDurability()` | No (zone-only) |
| Spawn | `shouldBlockSpawn()` | Yes |

---

## Interaction Codec Replacements

Source: `protection/interactions/`

Override vanilla interaction classes to add protection checks. Registered in `HyperFactionsPlugin.registerInteractionCodecs()`.

| Codec | Replaces | Protection Method | Notes |
|-------|----------|-------------------|-------|
| HyperFactionsHarvestCropInteraction | HarvestCropInteraction | Zone flag + `canInteract(INTERACT)` | **Only when no mixin active** |
| HyperFactionsPlaceFluidInteraction | PlaceFluidInteraction | `canInteract(BUILD)` | Always registered |
| HyperFactionsRefillContainerInteraction | RefillContainerInteraction | `canInteract(BUILD)` | Always registered |

All three check both zones AND claims.

---

## Damage Handler Pipeline

Source: `protection/damage/`

`DamageProtectionHandler` coordinates all damage checks in order:

```
1. Fall damage → FallDamageProtection (zone flag only)
2. Environmental → EnvironmentalDamageProtection (zone flag only)
3. Source type check → Not EntitySource? Allow (unknown source)
4. Projectile → ProjectileDamageProtection (zone flag only)
5. Mob → MobDamageProtection (zone flag only)
6. Player → PvPDamageProtection (zone + claim checks)
```

Each handler records the damage type to `CombatTagManager` for later use by `PlayerDeathSystem`.

---

## Debug Tools

Source: `protection/debug/`

### Enabling

Set in `config/debug.json`:

```json
{
  "categories": {
    "protection": true
  }
}
```

### Trace Classes

| Class | Purpose | Output Format |
|-------|---------|---------------|
| ProtectionTrace | Interaction protection checks | `[Protection] Zone 'Spawn' (SAFE) flag 'build_allowed' = false for player abc at world/1/2` |
| PvPTrace | PvP damage attempts | `[PvP] attacker=... defender=... result=DENIED_SAFEZONE` |

---

## Key Class Reference

| Class | Path | Lines | Purpose |
|-------|------|-------|---------|
| ProtectionChecker | `protection/ProtectionChecker.java` | ~1,320 | Central protection logic |
| ProtectionListener | `protection/ProtectionListener.java` | ~140 | High-level event callbacks |
| ProtectionMixinBridge | `integration/protection/ProtectionMixinBridge.java` | ~297 | Mixin auto-detection and routing |
| HyperProtectIntegration | `integration/protection/HyperProtectIntegration.java` | ~570 | HyperProtect-Mixin hooks |
| OrbisMixinsIntegration | `integration/protection/OrbisMixinsIntegration.java` | ~1,332 | OrbisGuard-Mixins hooks |
| OrbisGuardIntegration | `integration/protection/OrbisGuardIntegration.java` | ~425 | OrbisGuard region conflict detection |
| GravestoneIntegration | `integration/protection/GravestoneIntegration.java` | ~347 | Gravestone plugin integration |
| SpawnProtection | `protection/SpawnProtection.java` | ~73 | Spawn protection data record |
| FactionPermissions | `data/FactionPermissions.java` | ~552 | 45-flag permission model |
| ZoneFlags | `data/ZoneFlags.java` | — | Zone flag constants and defaults |
| DamageProtectionHandler | `protection/damage/DamageProtectionHandler.java` | ~132 | Damage check coordinator |
| ZoneInteractionProtection | `protection/zone/ZoneInteractionProtection.java` | — | Zone interaction checks + block type detection |
| ZoneDamageProtection | `protection/zone/ZoneDamageProtection.java` | — | Zone damage flag checks |
| FactionsConfig | `config/FactionsConfig.java` | — | Faction gameplay settings (`config/factions.json`) |
| ServerConfig | `config/ServerConfig.java` | — | Server behavior settings (`config/server.json`) |
| CoreConfig *(deprecated)* | `config/CoreConfig.java` | — | Replaced by FactionsConfig + ServerConfig (Migration V5 -> V6) |
