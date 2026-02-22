# HyperFactions Protection System

> **Version**: 0.8.2

Architecture documentation for the HyperFactions protection system.

## Overview

HyperFactions uses a multi-layered protection system that controls block interactions, PvP combat, and damage types based on:

- **Zones** - SafeZone and WarZone with configurable flags
- **Faction Claims** - Territory permissions for members, allies, and outsiders
- **Relations** - Same faction, ally, enemy, neutral
- **Combat State** - Spawn protection, combat tagging
- **OrbisGuard-Mixins** - 11 mixin hooks for extended protection coverage
- **Spawn Suppression** - Mob spawning control in claims and zones

```mermaid
flowchart TD
    A[Interaction Event] --> B{Admin Bypass?}
    B -->|Yes| C[ALLOWED]
    B -->|No| D{Bypass Permission?}
    D -->|Yes| C
    D -->|No| E{In Zone?}
    E -->|SafeZone| F[Check Zone Flags]
    E -->|WarZone| F
    E -->|No| G{In Claim?}
    G -->|No| C
    G -->|Yes| H{Same Faction?}
    H -->|Yes: Member| I[Check Member Flags]
    H -->|Yes: Officer| J[Check Officer Flags]
    H -->|No| K{Relation?}
    K -->|Ally| L[Check Ally Flags]
    K -->|Enemy/Neutral| M[Check Outsider Flags]
    F --> N{Flag Allows?}
    I --> N
    J --> N
    L --> N
    M --> N
    N -->|Yes| C
    N -->|No| O[DENIED]

    style C fill:#22c55e,color:#fff
    style O fill:#ef4444,color:#fff
```

## Architecture

```
Hytale ECS Events                  OrbisGuard-Mixins Hooks
     │                                    │
     ▼                                    ▼
ECS Protection Systems             OrbisMixinsIntegration (11 hooks)
├── BlockPlaceProtectionSystem     ├── PickupHook
├── BlockBreakProtectionSystem     ├── HammerHook (zone + claim BUILD)
├── BlockUseProtectionSystem       ├── HarvestHook (zone + claim INTERACT)
├── ItemDropProtectionSystem       ├── PlaceHook (bucket/fluid)
├── ItemPickupProtectionSystem     ├── UseHook (campfire, lantern)
├── HarvestPickupProtectionSystem  ├── SeatHook
├── PlayerDeathSystem              ├── ExplosionHook (claim + safezone)
├── PlayerRespawnSystem            ├── CommandHook
└── DamageProtectionSystem         ├── DeathHook (keep inventory)
     │                             ├── DurabilityHook
     ▼                             └── SpawnHook
ProtectionChecker (central logic)
├── canInteract() ─► Result        Interaction Codec Replacements
├── canDamagePlayer() ─► PvPResult ├── HarvestCrop (scythe protection)
└── isDamageAllowed() ─► Zone flag ├── PlaceFluid (bucket protection)
                                   └── RefillContainer (scoop protection)
     │
     ├─► ZoneManager (zone flag lookup)
     ├─► ClaimManager (territory ownership)
     ├─► FactionManager (faction membership)
     ├─► RelationManager (faction relations)
     └─► CombatTagManager (spawn protection)
```

## Key Classes

| Class | Path | Purpose |
|-------|------|---------|
| ProtectionChecker | [`protection/ProtectionChecker.java`](../src/main/java/com/hyperfactions/protection/ProtectionChecker.java) | Central protection logic |
| ProtectionListener | [`protection/ProtectionListener.java`](../src/main/java/com/hyperfactions/protection/ProtectionListener.java) | High-level event callbacks |
| SpawnProtection | [`protection/SpawnProtection.java`](../src/main/java/com/hyperfactions/protection/SpawnProtection.java) | Spawn protection data record |
| ZoneFlags | [`data/ZoneFlags.java`](../src/main/java/com/hyperfactions/data/ZoneFlags.java) | Zone flag constants and defaults |

### ECS Systems (protection/ecs/)

| Class | Path | Event Handled |
|-------|------|---------------|
| BlockPlaceProtectionSystem | [`ecs/BlockPlaceProtectionSystem.java`](../src/main/java/com/hyperfactions/protection/ecs/BlockPlaceProtectionSystem.java) | PlaceBlockEvent |
| BlockBreakProtectionSystem | [`ecs/BlockBreakProtectionSystem.java`](../src/main/java/com/hyperfactions/protection/ecs/BlockBreakProtectionSystem.java) | BreakBlockEvent |
| BlockUseProtectionSystem | [`ecs/BlockUseProtectionSystem.java`](../src/main/java/com/hyperfactions/protection/ecs/BlockUseProtectionSystem.java) | UseBlockEvent |
| ItemDropProtectionSystem | [`ecs/ItemDropProtectionSystem.java`](../src/main/java/com/hyperfactions/protection/ecs/ItemDropProtectionSystem.java) | DropItemEvent |
| ItemPickupProtectionSystem | [`ecs/ItemPickupProtectionSystem.java`](../src/main/java/com/hyperfactions/protection/ecs/ItemPickupProtectionSystem.java) | InteractivelyPickupItemEvent |
| HarvestPickupProtectionSystem | [`ecs/HarvestPickupProtectionSystem.java`](../src/main/java/com/hyperfactions/protection/ecs/HarvestPickupProtectionSystem.java) | InteractivelyPickupItemEvent (manual F-key) |
| PlayerDeathSystem | [`ecs/PlayerDeathSystem.java`](../src/main/java/com/hyperfactions/protection/ecs/PlayerDeathSystem.java) | DeathComponent (power loss by death cause, kill rewards) |
| PlayerRespawnSystem | [`ecs/PlayerRespawnSystem.java`](../src/main/java/com/hyperfactions/protection/ecs/PlayerRespawnSystem.java) | DeathComponent removal (spawn protection) |
| DamageProtectionSystem | [`ecs/DamageProtectionSystem.java`](../src/main/java/com/hyperfactions/protection/ecs/DamageProtectionSystem.java) | Damage event |
| PvPProtectionSystem | [`ecs/PvPProtectionSystem.java`](../src/main/java/com/hyperfactions/protection/ecs/PvPProtectionSystem.java) | Alias for DamageProtectionSystem |

### Interaction Codec Replacements (protection/interactions/)

Codec replacements extend vanilla interaction classes to add protection checks. They override `interactWithBlock()` to check zone flags and faction permissions before calling `super`. Registered in `HyperFactionsPlugin.registerInteractionCodecs()`.

| Class | Path | Protects Against |
|-------|------|-----------------|
| HyperFactionsHarvestCropInteraction | [`interactions/HyperFactionsHarvestCropInteraction.java`](../src/main/java/com/hyperfactions/protection/interactions/HyperFactionsHarvestCropInteraction.java) | Scythe/tool crop harvesting |
| HyperFactionsPlaceFluidInteraction | [`interactions/HyperFactionsPlaceFluidInteraction.java`](../src/main/java/com/hyperfactions/protection/interactions/HyperFactionsPlaceFluidInteraction.java) | Bucket fluid placement |
| HyperFactionsRefillContainerInteraction | [`interactions/HyperFactionsRefillContainerInteraction.java`](../src/main/java/com/hyperfactions/protection/interactions/HyperFactionsRefillContainerInteraction.java) | Scooping fluid with empty bucket |

### Damage Handlers (protection/damage/)

| Class | Path | Purpose |
|-------|------|---------|
| DamageProtectionHandler | [`damage/DamageProtectionHandler.java`](../src/main/java/com/hyperfactions/protection/damage/DamageProtectionHandler.java) | Coordinates damage checks |
| FallDamageProtection | [`damage/FallDamageProtection.java`](../src/main/java/com/hyperfactions/protection/damage/FallDamageProtection.java) | Fall damage zone check |
| EnvironmentalDamageProtection | [`damage/EnvironmentalDamageProtection.java`](../src/main/java/com/hyperfactions/protection/damage/EnvironmentalDamageProtection.java) | Drowning, suffocation |
| ProjectileDamageProtection | [`damage/ProjectileDamageProtection.java`](../src/main/java/com/hyperfactions/protection/damage/ProjectileDamageProtection.java) | Arrow, thrown item damage |
| MobDamageProtection | [`damage/MobDamageProtection.java`](../src/main/java/com/hyperfactions/protection/damage/MobDamageProtection.java) | Mob → player damage |
| PvPDamageProtection | [`damage/PvPDamageProtection.java`](../src/main/java/com/hyperfactions/protection/damage/PvPDamageProtection.java) | Player vs player damage |

### Zone Protection (protection/zone/)

| Class | Path | Purpose |
|-------|------|---------|
| ZoneDamageProtection | [`zone/ZoneDamageProtection.java`](../src/main/java/com/hyperfactions/protection/zone/ZoneDamageProtection.java) | Zone flag damage checks |
| ZoneInteractionProtection | [`zone/ZoneInteractionProtection.java`](../src/main/java/com/hyperfactions/protection/zone/ZoneInteractionProtection.java) | Zone interaction checks |

### Debug (protection/debug/)

| Class | Path | Purpose |
|-------|------|---------|
| ProtectionTrace | [`debug/ProtectionTrace.java`](../src/main/java/com/hyperfactions/protection/debug/ProtectionTrace.java) | Interaction debug logging |
| PvPTrace | [`debug/PvPTrace.java`](../src/main/java/com/hyperfactions/protection/debug/PvPTrace.java) | PvP debug logging |

## Protection Results

### ProtectionResult

Result of interaction checks (build, interact, container, use):

```java
public enum ProtectionResult {
    ALLOWED,
    ALLOWED_BYPASS,        // Admin bypass or bypass permission
    ALLOWED_WILDERNESS,    // Unclaimed territory
    ALLOWED_OWN_CLAIM,     // Player's faction territory
    ALLOWED_ALLY_CLAIM,    // Allied faction territory
    ALLOWED_WARZONE,       // WarZone with permission granted
    DENIED_SAFEZONE,       // SafeZone blocked
    DENIED_ENEMY_CLAIM,    // Enemy territory
    DENIED_NEUTRAL_CLAIM,  // Neutral faction territory
    DENIED_NO_PERMISSION   // Faction permission denied
}
```

### PvPResult

Result of PvP damage checks:

```java
public enum PvPResult {
    ALLOWED,
    ALLOWED_WARZONE,           // WarZone PvP enabled
    DENIED_SAFEZONE,           // SafeZone PvP disabled
    DENIED_SAME_FACTION,       // Same faction, no friendly fire
    DENIED_ALLY,               // Allied faction, ally damage disabled
    DENIED_ATTACKER_SAFEZONE,  // Attacker in SafeZone
    DENIED_DEFENDER_SAFEZONE,  // Defender in SafeZone
    DENIED_SPAWN_PROTECTED,    // Defender has spawn protection
    DENIED_TERRITORY_NO_PVP    // Territory PvP disabled
}
```

### InteractionType

Types of interactions for protection checks:

```java
public enum InteractionType {
    BUILD,      // Place/break blocks
    INTERACT,   // Doors, buttons, levers
    CONTAINER,  // Chests, storage
    DAMAGE,     // Entity damage (non-player)
    USE         // Item usage
}
```

## Zone Flags

[`data/ZoneFlags.java`](../src/main/java/com/hyperfactions/data/ZoneFlags.java)

### Combat Flags

| Flag | Description | SafeZone Default | WarZone Default |
|------|-------------|------------------|-----------------|
| `pvp_enabled` | Players can damage players | false | true |
| `friendly_fire` | Same-faction damage | false | false |
| `projectile_damage` | Projectiles deal damage | false | true |
| `mob_damage` | Mobs can damage players | false | true |

### Building Flags

| Flag | Description | SafeZone Default | WarZone Default |
|------|-------------|------------------|-----------------|
| `build_allowed` | Place/break blocks | false | false |
| `block_interact` | General block interaction | true | true |

### Interaction Flags

| Flag | Description | SafeZone Default | WarZone Default |
|------|-------------|------------------|-----------------|
| `door_use` | Use doors and gates | true | true |
| `container_use` | Use chests and storage | false | false |
| `bench_use` | Use crafting tables | false | false |
| `processing_use` | Use furnaces, smelters | false | false |
| `seat_use` | Sit on seats/mounts | true | true |

### Item Flags

| Flag | Description | SafeZone Default | WarZone Default |
|------|-------------|------------------|-----------------|
| `item_drop` | Players can drop items | false | true |
| `item_pickup` | Players can pick up items | true | true |

### Damage Flags

| Flag | Description | SafeZone Default | WarZone Default |
|------|-------------|------------------|-----------------|
| `fall_damage` | Fall damage applies | false | true |
| `environmental_damage` | Drowning, suffocation | false | true |

## Protection Check Flow

### Interaction Protection

```
canInteract(playerUuid, world, x, z, type)
     │
     ├─1─► Admin Bypass Check
     │     └─► If admin with bypass toggle ON → ALLOWED_BYPASS
     │
     ├─2─► Standard Bypass Permission
     │     └─► hyperfactions.bypass.{build|interact|container|use}
     │         └─► If granted → ALLOWED_BYPASS
     │
     ├─3─► Zone Check
     │     └─► Get zone at (world, chunkX, chunkZ)
     │         ├─► SafeZone + flag disabled → DENIED_SAFEZONE
     │         ├─► WarZone + flag enabled → ALLOWED_WARZONE
     │         └─► Continue to claim check
     │
     ├─4─► Claim Check
     │     └─► Get claim owner
     │         └─► No owner → ALLOWED_WILDERNESS
     │
     ├─5─► Same Faction Check
     │     └─► Player in owning faction
     │         └─► Check memberBreak/memberPlace/memberInteract
     │             └─► Allowed → ALLOWED_OWN_CLAIM
     │
     ├─6─► Ally Check
     │     └─► Player allied with owner
     │         └─► Check allyBreak/allyPlace/allyInteract
     │             └─► Allowed → ALLOWED_ALLY_CLAIM
     │
     ├─7─► Outsider Check
     │     └─► Check outsiderBreak/outsiderPlace/outsiderInteract
     │         └─► Allowed → ALLOWED
     │
     └─8─► Default Deny
           ├─► Enemy → DENIED_ENEMY_CLAIM
           └─► Neutral → DENIED_NEUTRAL_CLAIM
```

### PvP Protection

```
canDamagePlayer(attackerUuid, defenderUuid, world, x, z)
     │
     ├─1─► Spawn Protection Check
     │     └─► Defender has spawn protection → DENIED_SPAWN_PROTECTED
     │
     ├─2─► Zone PvP Check
     │     └─► Get zone at location
     │         ├─► PvP disabled → DENIED_SAFEZONE
     │         └─► Check friendly fire for same faction/ally
     │
     ├─3─► Territory PvP Check
     │     └─► Get claim owner's faction permissions
     │         └─► pvpEnabled = false → DENIED_TERRITORY_NO_PVP
     │
     ├─4─► Same Faction Check
     │     └─► Both in same faction + factionDamage disabled
     │         └─► DENIED_SAME_FACTION
     │
     ├─5─► Ally Check
     │     └─► Factions are allies + allyDamage disabled
     │         └─► DENIED_ALLY
     │
     └─6─► Default Allow
           └─► ALLOWED or ALLOWED_WARZONE
```

### Damage Protection

```
DamageProtectionHandler.handleDamage(event, defender, world, x, z)
     │
     ├─1─► Fall Damage
     │     └─► Check DamageCause.FALL + zone fallDamage flag
     │
     ├─2─► Environmental Damage
     │     └─► Drowning, suffocation + zone environmentalDamage flag
     │
     ├─3─► Source Type Check
     │     └─► Not EntitySource → Allow (unknown source)
     │
     ├─4─► Projectile Damage
     │     └─► Check zone projectileDamage flag
     │
     ├─5─► Mob Damage
     │     └─► Non-player entity → Check zone mobDamage flag
     │
     └─6─► PvP Damage
           └─► Player attacker → Use PvPDamageProtection
```

## Death Cause Type Tracking

`DamageProtectionHandler` records the type of each damage event that goes through protection checks into `CombatTagManager`. When a player dies, `PlayerDeathSystem` reads the last recorded damage type to determine whether power loss applies.

```
DamageProtectionHandler                CombatTagManager              PlayerDeathSystem
     │                                      │                              │
     ├─ Fall damage allowed ──────► recordDamageType(ENVIRONMENTAL)        │
     ├─ Environmental allowed ────► recordDamageType(ENVIRONMENTAL)        │
     ├─ Mob damage allowed ───────► recordDamageType(MOB)                  │
     ├─ PvP damage allowed ───────► recordDamageType(PVP)                  │
     │                                      │                              │
     │                                      │    Player dies ──────────────┤
     │                                      │◄── getLastDamageType() ──────┤
     │                                      │                              │
     │                                      │    Check config:             │
     │                                      │    MOB + !powerLossOnMobDeath → skip
     │                                      │    ENV + !powerLossOnEnvDeath → skip
     │                                      │    PVP/UNKNOWN → always apply │
```

**Configuration** (in `config.json` → `power` section):

| Key | Default | Description |
|-----|---------|-------------|
| `powerLossOnMobDeath` | `true` | Apply power loss when killed by mobs |
| `powerLossOnEnvironmentalDeath` | `true` | Apply power loss for fall, drowning, suffocation |

When power loss is skipped, kill/death counters and gravestone announcements still fire — only the power penalty is omitted. Zone flag `power_loss=false` takes priority over these settings.

## ECS Integration

### System Registration

Protection systems are registered in `HyperFactions.init()`:

```java
// Register ECS protection systems
World world = // from universe
world.registerSystem(new BlockPlaceProtectionSystem(this, protectionListener));
world.registerSystem(new BlockBreakProtectionSystem(this, protectionListener));
world.registerSystem(new BlockUseProtectionSystem(this, protectionListener));
world.registerSystem(new ItemDropProtectionSystem(this, protectionListener));
world.registerSystem(new ItemPickupProtectionSystem(this, protectionListener));
world.registerSystem(new DamageProtectionSystem(this, protectionListener));
```

### System Group

Damage systems use `DamageModule.get().getFilterDamageGroup()` to run BEFORE damage is applied:

```java
@Override
public SystemGroup<EntityStore> getGroup() {
    return DamageModule.get().getFilterDamageGroup();
}
```

Without this, `event.setCancelled(true)` has no effect because `ApplyDamage` runs first.

### Event Handling Pattern

All ECS systems follow this pattern:

```java
@Override
public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                   Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                   EventType event) {
    try {
        if (event.isCancelled()) return;

        // Get player from entity
        PlayerRef player = chunk.getComponent(entityIndex, PlayerRef.getComponentType());
        if (player == null) return;

        // Get world name
        String worldName = store.getExternalData().getWorld().getName();

        // Delegate to ProtectionListener/ProtectionChecker
        boolean blocked = protectionListener.onBlockPlace(/* params */);

        if (blocked) {
            event.setCancelled(true);
            player.sendMessage(/* denial message */);
        }
    } catch (Exception e) {
        Logger.severe("Error processing protection event", e);
    }
}
```

## Bypass Permissions

Bypass permissions allow players to ignore protection rules:

| Permission | Bypasses |
|------------|----------|
| `hyperfactions.bypass.build` | Block place/break in protected areas |
| `hyperfactions.bypass.interact` | Door/button protection |
| `hyperfactions.bypass.container` | Chest access protection |
| `hyperfactions.bypass.damage` | Entity damage protection |
| `hyperfactions.bypass.use` | Item use protection |
| `hyperfactions.bypass.pickup` | Item pickup protection (F-key) |
| `hyperfactions.bypass.*` | All interaction protections |

**Admin Bypass:**

Admins with `hyperfactions.admin.use` have a separate bypass toggle:
- `/f admin bypass` toggles admin bypass mode
- Standard bypass permissions do NOT apply to admins
- This ensures admins only bypass when explicitly toggled

## Spawn Protection

[`protection/SpawnProtection.java`](../src/main/java/com/hyperfactions/protection/SpawnProtection.java)

Temporary protection after respawning:

```java
public record SpawnProtection(
    UUID playerUuid,
    long protectedAt,
    int durationSeconds,
    String world,
    int chunkX,
    int chunkZ
) {
    public boolean isExpired() { ... }
    public int getRemainingSeconds() { ... }
    public boolean hasLeftSpawnChunk(String world, int chunkX, int chunkZ) { ... }
}
```

**Configuration:**
- `combat.spawnProtection.enabled` - Enable spawn protection
- `combat.spawnProtection.durationSeconds` - Protection duration
- `combat.spawnProtection.breakOnAttack` - Remove protection if player attacks

**Spawn protection is removed when:**
1. Duration expires
2. Player leaves spawn chunk
3. Player attacks another player (if configured)

## Faction Permissions

Territory permissions are checked for member/ally/outsider access:

| Permission | Description |
|------------|-------------|
| `memberBreak` | Members can break blocks |
| `memberPlace` | Members can place blocks |
| `memberInteract` | Members can interact |
| `allyBreak` | Allies can break blocks |
| `allyPlace` | Allies can place blocks |
| `allyInteract` | Allies can interact |
| `outsiderBreak` | Outsiders can break blocks |
| `outsiderPlace` | Outsiders can place blocks |
| `outsiderInteract` | Outsiders can interact |
| `pvpEnabled` | PvP allowed in territory |

These are configured per-faction and can be locked by server config.

## Debug Logging

Enable protection debug logging in `config/debug.json`:

```json
{
  "categories": {
    "protection": true
  }
}
```

This enables detailed logging of protection checks:
```
[Protection] Zone 'Spawn' (SAFE) flag 'build_allowed' = false for player abc at world/1/2
[Protection] Zone blocked: DENIED_SAFEZONE
```

## Code Links

| Class | Path |
|-------|------|
| ProtectionChecker | [`protection/ProtectionChecker.java`](../src/main/java/com/hyperfactions/protection/ProtectionChecker.java) |
| ProtectionListener | [`protection/ProtectionListener.java`](../src/main/java/com/hyperfactions/protection/ProtectionListener.java) |
| SpawnProtection | [`protection/SpawnProtection.java`](../src/main/java/com/hyperfactions/protection/SpawnProtection.java) |
| ZoneFlags | [`data/ZoneFlags.java`](../src/main/java/com/hyperfactions/data/ZoneFlags.java) |
| DamageProtectionHandler | [`protection/damage/DamageProtectionHandler.java`](../src/main/java/com/hyperfactions/protection/damage/DamageProtectionHandler.java) |
| ZoneDamageProtection | [`protection/zone/ZoneDamageProtection.java`](../src/main/java/com/hyperfactions/protection/zone/ZoneDamageProtection.java) |
| BlockPlaceProtectionSystem | [`protection/ecs/BlockPlaceProtectionSystem.java`](../src/main/java/com/hyperfactions/protection/ecs/BlockPlaceProtectionSystem.java) |
| DamageProtectionSystem | [`protection/ecs/DamageProtectionSystem.java`](../src/main/java/com/hyperfactions/protection/ecs/DamageProtectionSystem.java) |
| PvPDamageProtection | [`protection/damage/PvPDamageProtection.java`](../src/main/java/com/hyperfactions/protection/damage/PvPDamageProtection.java) |
