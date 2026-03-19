# Protection Systems & Architecture

> **Version**: 0.11.0 | Developer-focused reference for the HyperFactions protection implementation.

For admin/config documentation, see [protection-claims.md](protection-claims.md), [protection-zones.md](protection-zones.md), and [protection-global.md](protection-global.md).

## Architecture

```
Hytale ECS Events                  ProtectionMixinBridge (auto-detect)
     │                                    │
     ▼                                    ├─► HyperProtectIntegration (27 hooks + format handle, recommended)
ECS Protection Systems (11)                │   ├── BlockBreak, BlockPlace, Explosion
├── BlockPlaceProtectionSystem            │   ├── FireSpread, BuilderTools
├── BlockBreakProtectionSystem            │   ├── ItemPickup, DeathDrop, Durability
├── BlockUseProtectionSystem              │   ├── ContainerAccess, ContainerOpen
├── ItemDropProtectionSystem              │   ├── MobSpawn, Command
├── ItemPickupProtectionSystem            │   ├── Teleporter, Portal (unique to HP)
├── HarvestPickupProtectionSystem         │   ├── EntityDamage, Respawn (unique to HP)
├── DamageProtectionSystem                │   ├── Hammer, Use, Seat
├── PvPProtectionSystem                   │   └── Mount, BarterTrade, FluidSpread, PrefabSpawn,
├── PlayerDeathSystem                     │       ProjectileLaunch, CraftingResource, MapMarkerFilter
├── PlayerRespawnSystem                   │
└── TeleportCancelOnDamageSystem          └─► OrbisMixinsIntegration (11 hooks)
     │                                         ├── Pickup, Hammer, Harvest
     ▼                                         ├── Place, Use, Seat
ProtectionChecker (central logic)              ├── Explosion, Command
├── canInteract() ─► ProtectionResult          └── Death, Durability, Spawn
├── canDamagePlayer() ─► PvPResult
├── checkBuild/Place/Hammer() ─► String   Interaction Codec Replacements
├── checkTeleporter/Portal() ─► String    ├── HarvestCrop (only when no mixin active)
├── checkSeat/Mount/Use() ─► String       ├── PlaceFluid (bucket protection)
├── checkBench/Container() ─► String      └── RefillContainer (scoop protection)
├── checkEntityDamage() ─► String
├── checkPveInTerritory() (PvE in claims)
├── checkProjectileLaunch() ─► String
├── checkTrade() ─► String (barter NPC)
├── shouldBlockExplosion() (3-way config)
├── shouldBlockFireSpread() (configurable)
├── shouldBlockFluidSpread()
├── shouldBlockSpawn() (zone + claim)
├── shouldKeepInventory/PreventDurability
├── canPickupItem() (outsider config)
├── checkCommandBlock() ─► CommandCheckResult
├── shouldHideMapMarker() ─► boolean
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

### ProtectionResult (12 values)

Source: `ProtectionChecker.java`

```java
ALLOWED               // Outsider permission granted
ALLOWED_BYPASS        // Admin bypass or bypass permission
ALLOWED_WILDERNESS    // Unclaimed territory
ALLOWED_SAFEZONE      // SafeZone with flag allowed, no claim below
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

Source: `ProtectionChecker.java`

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

### InteractionType (20 values)

Source: `ProtectionChecker.java`

```java
BUILD         // Place/break blocks
INTERACT      // General block interaction (fallback)
CONTAINER     // Open chests, etc.
DOOR          // Use doors/gates
BENCH         // Crafting tables
PROCESSING    // Furnaces/smelters
SEAT          // Seats/mounts
LIGHT         // Lights/lanterns/campfires
DAMAGE        // Damage entities (not players)
USE           // Use items (fallback)
TELEPORTER    // Use teleporter blocks
PORTAL        // Use portal blocks
CRATE_PICKUP  // Capture crate entity pickup
CRATE_PLACE   // Capture crate entity release
NPC_TAME      // F-key NPC taming
NPC_INTERACT  // NPC shops/dialogue interaction
MOUNT         // Mount/ride entities
PVE_DAMAGE    // Damage non-player entities (mobs)
ITEM_DROP     // Drop items
ITEM_PICKUP   // Pick up items
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
world.registerSystem(new HarvestPickupProtectionSystem(this, protectionListener));
world.registerSystem(new DamageProtectionSystem(this, protectionListener));
world.registerSystem(new PvPProtectionSystem(this, protectionListener));
world.registerSystem(new PlayerDeathSystem(this));
world.registerSystem(new PlayerRespawnSystem(this));
world.registerSystem(new TeleportCancelOnDamageSystem(this));
```

### Damage System Group

Damage systems use `DamageModule.get().getFilterDamageGroup()` to run BEFORE damage is applied. Without this, `event.setCancelled(true)` has no effect.

### System Reference

| System | Event | Delegates To | Checks Claims? |
|--------|-------|-------------|----------------|
| BlockPlaceProtectionSystem | PlaceBlockEvent | `canInteract(BUILD)` | Yes |
| BlockBreakProtectionSystem | BreakBlockEvent | `canInteract(BUILD)` | Yes |
| BlockUseProtectionSystem | UseBlockEvent.Pre | `canInteract(DOOR/CONTAINER/BENCH/PROCESSING/SEAT/INTERACT)` | Yes |
| ItemDropProtectionSystem | DropItemEvent.PlayerRequest | `ZoneInteractionProtection.isItemDropAllowed()` + `outsiderDropAllowed` config | **Zone + claim config** |
| ItemPickupProtectionSystem | InteractivelyPickupItemEvent | `canInteract(INTERACT)` | Yes |
| HarvestPickupProtectionSystem | InteractivelyPickupItemEvent | `canPickupItem()` | Yes |
| DamageProtectionSystem | Damage | `DamageProtectionHandler` | PvP only |
| PvPProtectionSystem | Damage (extends DamageProtectionSystem) | PvP-specific damage handling | Yes |
| PlayerDeathSystem | DeathComponent (RefChangeSystem) | Power loss, kill rewards | Uses config |
| PlayerRespawnSystem | DeathComponent removal (RefChangeSystem) | Spawn protection | N/A |
| TeleportCancelOnDamageSystem | Damage | Cancel pending teleports on damage | N/A |

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

## HyperProtect-Mixin Integration (30 Slots, 28 Used)

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
| 20 | Use | `checkUse(type)` | Yes (routes CRATE_PICKUP, CRATE_PLACE, NPC_TAME, MOUNT, LIGHT, INTERACT) |
| 21 | Seat | `checkSeat()` | Yes |
| 22 | Respawn | `getRespawnOverride()` | Yes |
| 23 | CraftingResource | `CraftingResourceHook.evaluateCraftingResource()` | Yes |
| 24 | MapMarkerFilter | `MapMarkerFilterHook.filterPlayerMarker()` | N/A — visibility filter |
| 25 | FluidSpread | `shouldBlockFluidSpread()` | Yes (zone-only, claims always allow) |
| 26 | PrefabSpawn | `shouldBlockSpawn()` (via `PrefabSpawnHook`) | Yes |
| 27 | ProjectileLaunch | `checkProjectileLaunch()` (via `ProjectileLaunchHook`) | Yes |
| 28 | Mount | `checkMount()` (via `MountHook`) | Yes |
| 29 | BarterTrade | `checkTrade()` (via `BarterTradeHook`) | Yes |

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

| Class | Path | Purpose |
|-------|------|---------|
| ProtectionChecker | `protection/ProtectionChecker.java` | Central protection logic (~1,871 lines) |
| ProtectionListener | `protection/ProtectionListener.java` | High-level event callbacks |
| ProtectionMessageDebounce | `protection/ProtectionMessageDebounce.java` | Debounces repeated denial messages |
| NpcInteractionProtectionHandler | `protection/NpcInteractionProtectionHandler.java` | NPC interaction protection |
| MobCleanupManager | `protection/MobCleanupManager.java` | Periodic mob removal in zones |
| SpawnProtection | `protection/SpawnProtection.java` | Spawn protection data record |
| ProtectionMixinBridge | `integration/protection/ProtectionMixinBridge.java` | Mixin auto-detection and routing |
| HyperProtectIntegration | `integration/protection/HyperProtectIntegration.java` | HyperProtect-Mixin hooks |
| OrbisMixinsIntegration | `integration/protection/OrbisMixinsIntegration.java` | OrbisGuard-Mixins hooks |
| OrbisGuardIntegration | `integration/protection/OrbisGuardIntegration.java` | OrbisGuard region conflict detection |
| GravestoneIntegration | `integration/protection/GravestoneIntegration.java` | Gravestone plugin integration |
| KyuubiSoftIntegration | `integration/protection/KyuubiSoftIntegration.java` | KyuubiSoft integration |
| FactionPermissions | `data/FactionPermissions.java` | 57-flag permission model |
| ZoneFlags | `data/ZoneFlags.java` | 52 zone flag constants and defaults |
| DamageProtectionHandler | `protection/damage/DamageProtectionHandler.java` | Damage check coordinator |
| PvPDamageProtection | `protection/damage/PvPDamageProtection.java` | PvP damage checks |
| FallDamageProtection | `protection/damage/FallDamageProtection.java` | Fall damage (zone-only) |
| EnvironmentalDamageProtection | `protection/damage/EnvironmentalDamageProtection.java` | Environmental damage (zone-only) |
| ProjectileDamageProtection | `protection/damage/ProjectileDamageProtection.java` | Projectile damage (zone-only) |
| MobDamageProtection | `protection/damage/MobDamageProtection.java` | Mob damage (zone-only) |
| ZoneInteractionProtection | `protection/zone/ZoneInteractionProtection.java` | Zone interaction checks + block type detection |
| ZoneDamageProtection | `protection/zone/ZoneDamageProtection.java` | Zone damage flag checks |
| FactionsConfig | `config/modules/FactionsConfig.java` | Faction gameplay settings (`config/factions.json`) |
| ServerConfig | `config/modules/ServerConfig.java` | Server behavior settings (`config/server.json`) |
| CoreConfig *(deprecated)* | `config/CoreConfig.java` | Replaced by FactionsConfig + ServerConfig (Migration V5 -> V6) |
