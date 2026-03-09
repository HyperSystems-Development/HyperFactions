package com.hyperfactions.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Constants for zone flag names.
 * These flags control various behaviors within zones.
 *
 * <p>Flags are verified against actual Hytale server events:
 * - Combat flags: Use Damage event with different source types
 * - Building flags: Use BreakBlockEvent, PlaceBlockEvent, UseBlockEvent
 * - Item flags: Use DropItemEvent, InteractivelyPickupItemEvent
 *
 * <p>MIXIN-DEPENDENT FLAGS
 * Some flags require OrbisGuard-Mixins to function properly. When OrbisGuard-Mixins
 * is not installed, these flags will have no effect. Use requiresMixin(String)
 * to check if a flag requires mixin support, and getMixinType(String) to
 * determine which specific mixin is required.
 *
 * <p>Mixin Types:
 * - MIXIN_PICKUP: Required for F-key item pickup blocking
 * - MIXIN_DEATH: Required for keep inventory on death
 * - MIXIN_DURABILITY: Required for invincible items (no durability loss)
 * - MIXIN_SEATING: Required for enhanced seat/mount blocking
 *
 * <p>Removed Minecraft-specific flags:
 * - hunger_loss: No hunger system in Hytale
 * - container_access + interact_allowed: Consolidated to block_interact
 */
public final class ZoneFlags {

  private ZoneFlags() {} // Prevent instantiation

  // ==========================================================================
  // MIXIN TYPE CONSTANTS
  // ==========================================================================

  /** Mixin type for F-key item pickup interception. */
  public static final String MIXIN_PICKUP = "pickup";

  /** Mixin type for death event interception (keep inventory). */
  public static final String MIXIN_DEATH = "death";

  /** Mixin type for durability/damage interception (invincible items). */
  public static final String MIXIN_DURABILITY = "durability";

  /** Mixin type for seating/mounting interception. */
  public static final String MIXIN_SEATING = "seating";

  /** Mixin type for NPC spawn interception. */
  public static final String MIXIN_SPAWN = "spawn";

  /** Mixin type for explosion interception. */
  public static final String MIXIN_EXPLOSION = "explosion";

  /** Mixin type for fire spread interception. */
  public static final String MIXIN_FIRE_SPREAD = "fire_spread";

  /** Mixin type for builder tools (paste) interception. */
  public static final String MIXIN_BUILDER_TOOLS = "builder_tools";

  /** Mixin type for teleporter interception. */
  public static final String MIXIN_TELEPORTER = "teleporter";

  /** Mixin type for portal interception. */
  public static final String MIXIN_PORTAL = "portal";

  /** Mixin type for hammer cycling interception. */
  public static final String MIXIN_HAMMER = "hammer";

  /** Mixin type for block placement interception. */
  public static final String MIXIN_PLACE = "block_place";

  /** Mixin type for use hook interception (capture crate, NPC tame). */
  public static final String MIXIN_USE = "use";

  // ==========================================================================
  // COMBAT FLAGS (6)
  // ==========================================================================

  /** Whether PvP is enabled (parent of FRIENDLY_FIRE). Uses Damage event with EntitySource (player). */
  public static final String PVP_ENABLED = "pvp_enabled";

  /** Whether friendly fire is allowed (child of PVP_ENABLED, parent of FACTION/ALLY). Uses Damage + faction check. */
  public static final String FRIENDLY_FIRE = "friendly_fire";

  /** Whether same-faction players can damage each other (child of FRIENDLY_FIRE). */
  public static final String FRIENDLY_FIRE_FACTION = "friendly_fire_faction";

  /** Whether allied faction players can damage each other (child of FRIENDLY_FIRE). */
  public static final String FRIENDLY_FIRE_ALLY = "friendly_fire_ally";

  /** Whether projectile damage is allowed. Uses Damage event with ProjectileSource. */
  public static final String PROJECTILE_DAMAGE = "projectile_damage";

  /** Whether mobs can damage players. Uses Damage event with EntitySource (non-player). */
  public static final String MOB_DAMAGE = "mob_damage";

  /** Whether players can damage mobs/NPCs. Uses entity damage mixin hook (player attacker, non-player target). */
  public static final String PVE_DAMAGE = "pve_damage";

  // ==========================================================================
  // BUILDING FLAGS (5)
  // ==========================================================================

  /** Whether players can place/break blocks. Uses BreakBlockEvent, PlaceBlockEvent. */
  public static final String BUILD_ALLOWED = "build_allowed";

  /**
   * Whether players can place blocks via mixin hook.
   * REQUIRES: Mixin support (block_place interceptor)
   * Child of BUILD_ALLOWED.
   */
  public static final String BLOCK_PLACE = "block_place";

  /**
   * Whether players can use the hammer (block cycling).
   * REQUIRES: Mixin support (hammer interceptor)
   * Child of BUILD_ALLOWED.
   */
  public static final String HAMMER_USE = "hammer_use";

  /**
   * Whether players can use builder tools (paste, copy).
   * REQUIRES: Mixin support (builder_tools interceptor)
   * Child of BUILD_ALLOWED.
   */
  public static final String BUILDER_TOOLS_USE = "builder_tools_use";

  /** Whether players can interact with blocks (general fallback). Uses UseBlockEvent. */
  public static final String BLOCK_INTERACT = "block_interact";

  // ==========================================================================
  // INTERACTION FLAGS (5) - Specific block interaction types
  // ==========================================================================

  /** Whether players can use doors and gates. Uses DoorInteraction. */
  public static final String DOOR_USE = "door_use";

  /** Whether players can use storage containers (chests, backpacks). Uses OpenContainerInteraction. */
  public static final String CONTAINER_USE = "container_use";

  /** Whether players can use crafting benches/tables. Uses OpenBenchPageInteraction. */
  public static final String BENCH_USE = "bench_use";

  /** Whether players can use processing blocks (furnaces, smelters). Uses OpenProcessingBenchInteraction. */
  public static final String PROCESSING_USE = "processing_use";

  /** Whether players can sit on seats/chairs/mounts. Uses MountInteraction. */
  public static final String SEAT_USE = "seat_use";

  /**
   * Whether players can mount rideable entities.
   * REQUIRES: Mixin support (seating interceptor — shared with SEAT_USE)
   */
  public static final String MOUNT_USE = "mount_use";

  /** Whether players can toggle lights/lanterns/campfires. Uses UseBlockEvent with light state. */
  public static final String LIGHT_USE = "light_use";

  // ==========================================================================
  // ENTITY INTERACTION FLAGS (5) - NPC and capture crate interactions
  // ==========================================================================

  /**
   * Whether players can pick up animals with capture crates.
   * REQUIRES: Mixin support (use hook via CaptureCrateGate)
   */
  public static final String CRATE_PICKUP = "crate_pickup";

  /**
   * Whether players can release animals from capture crates.
   * REQUIRES: Mixin support (use hook via CaptureCrateGate)
   */
  public static final String CRATE_PLACE = "crate_place";

  /**
   * Whether players can tame NPCs with F-key.
   * REQUIRES: Mixin support (use hook via SimpleInstantInteractionGate)
   */
  public static final String NPC_TAME = "npc_tame";

  /**
   * Whether players can interact with NPCs (shops, dialogue).
   * Uses PlayerInteractEvent (event-listener based, no mixin required).
   */
  public static final String NPC_INTERACT = "npc_interact";

  /** Parent flag for all NPC interactions (taming, dialogue, trading). */
  public static final String NPC_USE = "npc_use";

  // ==========================================================================
  // ITEM FLAGS (4)
  // ==========================================================================

  /** Whether players can drop items. Uses DropItemEvent. */
  public static final String ITEM_DROP = "item_drop";

  /** Whether players can pick up items automatically (walking over them). Uses InteractivelyPickupItemEvent. */
  public static final String ITEM_PICKUP = "item_pickup";

  /**
   * Whether players can manually pick up items (F-key).
   * REQUIRES: OrbisGuard-Mixins (pickup mixin)
   * Uses OrbisGuard-Mixins pickup hook.
   */
  public static final String ITEM_PICKUP_MANUAL = "item_pickup_manual";

  /**
   * Whether items are invincible (no durability loss).
   * REQUIRES: OrbisGuard-Mixins (durability mixin)
   * Uses OrbisGuard-Mixins durability hook.
   */
  public static final String INVINCIBLE_ITEMS = "invincible_items";

  /**
   * Whether mounted players can enter this zone.
   * When false, players are dismounted upon entering the zone.
   * Enforced by territory tracking on chunk transitions.
   */
  public static final String MOUNT_ENTRY = "mount_entry";

  // ==========================================================================
  // TRANSPORT FLAGS (2)
  // ==========================================================================

  /**
   * Whether players can use teleporter blocks.
   * REQUIRES: Mixin support (teleporter interceptor)
   */
  public static final String TELEPORTER_USE = "teleporter_use";

  /**
   * Whether players can use portal blocks.
   * REQUIRES: Mixin support (portal interceptor)
   */
  public static final String PORTAL_USE = "portal_use";

  // ==========================================================================
  // DAMAGE FLAGS (4)
  // ==========================================================================

  /** Whether players take fall damage. Uses Damage event with DamageCause.FALL. */
  public static final String FALL_DAMAGE = "fall_damage";

  /** Whether players take environmental damage (drowning, suffocation). Uses Damage with EnvironmentSource. */
  public static final String ENVIRONMENTAL_DAMAGE = "environmental_damage";

  /**
   * Whether explosions can damage blocks.
   * REQUIRES: Mixin support (explosion interceptor)
   */
  public static final String EXPLOSION_DAMAGE = "explosion_damage";

  /**
   * Whether fire can spread.
   * REQUIRES: Mixin support (fire_spread interceptor)
   */
  public static final String FIRE_SPREAD = "fire_spread";

  // ==========================================================================
  // DEATH FLAGS (2)
  // ==========================================================================

  /**
   * Whether players keep their inventory on death.
   * REQUIRES: OrbisGuard-Mixins (death mixin)
   * Uses OrbisGuard-Mixins death hook to prevent item drops.
   */
  public static final String KEEP_INVENTORY = "keep_inventory";

  /** Whether players lose faction power on death in this zone. */
  public static final String POWER_LOSS = "power_loss";

  // ==========================================================================
  // MOB SPAWNING FLAGS (4) - Uses SpawnSuppressionController
  // ==========================================================================

  /**
   * Master toggle for mob spawning. When false, blocks ALL mob spawning.
   * When true, spawning is controlled by the specific group flags below.
   * Uses Hytale's SpawnSuppressionController for chunk-based suppression.
   */
  public static final String MOB_SPAWNING = "mob_spawning";

  /**
   * Whether hostile mobs can spawn. Only applies when MOB_SPAWNING is true.
   * Uses NPCGroup "hostile" to determine which mobs are hostile.
   */
  public static final String HOSTILE_MOB_SPAWNING = "hostile_mob_spawning";

  /**
   * Whether passive mobs can spawn. Only applies when MOB_SPAWNING is true.
   * Uses NPCGroup "passive" to determine which mobs are passive.
   */
  public static final String PASSIVE_MOB_SPAWNING = "passive_mob_spawning";

  /**
   * Whether neutral mobs can spawn. Only applies when MOB_SPAWNING is true.
   * Uses NPCGroup "neutral" to determine which mobs are neutral (conditionally aggressive).
   */
  public static final String NEUTRAL_MOB_SPAWNING = "neutral_mob_spawning";

  /**
   * Whether NPC spawning is allowed (via mixin hook).
   * REQUIRES: OrbisGuard-Mixins (spawn hook via WorldSpawnJobSystemsMixin)
   * This is separate from the native hostile/passive/neutral flags.
   * Uses OrbisGuard-Mixins spawn hook to intercept world spawn jobs.
   */
  public static final String NPC_SPAWNING = "npc_spawning";

  // ==========================================================================
  // MOB CLEARING FLAGS (4) - Active periodic mob removal
  // ==========================================================================

  /**
   * Master toggle for mob clearing. When true, enables periodic removal of mobs.
   * When false, no mob clearing occurs regardless of child flags.
   * Parent of HOSTILE_MOB_CLEAR, PASSIVE_MOB_CLEAR, NEUTRAL_MOB_CLEAR.
   */
  public static final String MOB_CLEAR = "mob_clear";

  /**
   * Whether hostile mobs are periodically removed. Only applies when MOB_CLEAR is true.
   * Uses NPCGroup "hostile" to determine which mobs are hostile.
   * Conflicts with HOSTILE_MOB_SPAWNING — if spawning is true, clearing is forced false.
   */
  public static final String HOSTILE_MOB_CLEAR = "hostile_mob_clear";

  /**
   * Whether passive mobs are periodically removed. Only applies when MOB_CLEAR is true.
   * Uses NPCGroup "passive" to determine which mobs are passive.
   * Conflicts with PASSIVE_MOB_SPAWNING — if spawning is true, clearing is forced false.
   */
  public static final String PASSIVE_MOB_CLEAR = "passive_mob_clear";

  /**
   * Whether neutral mobs are periodically removed. Only applies when MOB_CLEAR is true.
   * Uses NPCGroup "neutral" to determine which mobs are neutral.
   * Conflicts with NEUTRAL_MOB_SPAWNING — if spawning is true, clearing is forced false.
   */
  public static final String NEUTRAL_MOB_CLEAR = "neutral_mob_clear";

  // ==========================================================================
  // INTEGRATION FLAGS (1)
  // ==========================================================================

  /** Whether non-owners can access (collect/break) other players' gravestones in this zone. Owners can always access their own. */
  public static final String GRAVESTONE_ACCESS = "gravestone_access";

  /** Whether world map visibility override is enabled for this zone. When enabled, MAP_VISIBILITY setting controls the level. */
  public static final String SHOW_ON_MAP = "show_on_map";

  /** Whether players can set/teleport to homes (HyperEssentials integration). */
  public static final String ESSENTIALS_HOMES = "essentials_homes";

  /** Whether players can teleport to warps (HyperEssentials integration). */
  public static final String ESSENTIALS_WARPS = "essentials_warps";

  /** Whether players can claim kits (HyperEssentials integration). */
  public static final String ESSENTIALS_KITS = "essentials_kits";

  /** Whether players can use /back to return to previous locations (HyperEssentials integration). */
  public static final String ESSENTIALS_BACK = "essentials_back";

  /**
   * All available flag names for validation.
   */
  public static final String[] ALL_FLAGS = {
    // Combat (7)
    PVP_ENABLED,
    FRIENDLY_FIRE,
    FRIENDLY_FIRE_FACTION,
    FRIENDLY_FIRE_ALLY,
    PROJECTILE_DAMAGE,
    MOB_DAMAGE,
    PVE_DAMAGE,
    // Damage (4)
    FALL_DAMAGE,
    ENVIRONMENTAL_DAMAGE,
    EXPLOSION_DAMAGE,
    FIRE_SPREAD,
    // Death (2)
    KEEP_INVENTORY,
    POWER_LOSS,
    // Building (4)
    BUILD_ALLOWED,
    BLOCK_PLACE,
    HAMMER_USE,
    BUILDER_TOOLS_USE,
    // Interaction (13)
    BLOCK_INTERACT,
    DOOR_USE,
    CONTAINER_USE,
    BENCH_USE,
    PROCESSING_USE,
    SEAT_USE,
    MOUNT_USE,
    LIGHT_USE,
    NPC_USE,
    NPC_TAME,
    NPC_INTERACT,
    CRATE_PICKUP,
    CRATE_PLACE,
    // Transport (3)
    TELEPORTER_USE,
    PORTAL_USE,
    MOUNT_ENTRY,
    // Items (4)
    ITEM_DROP,
    ITEM_PICKUP,
    ITEM_PICKUP_MANUAL,
    INVINCIBLE_ITEMS,
    // Mob Spawning (5)
    MOB_SPAWNING,
    HOSTILE_MOB_SPAWNING,
    PASSIVE_MOB_SPAWNING,
    NEUTRAL_MOB_SPAWNING,
    NPC_SPAWNING,
    // Mob Clearing (4)
    MOB_CLEAR,
    HOSTILE_MOB_CLEAR,
    PASSIVE_MOB_CLEAR,
    NEUTRAL_MOB_CLEAR,
    // Integration (6)
    GRAVESTONE_ACCESS,
    SHOW_ON_MAP,
    ESSENTIALS_HOMES,
    ESSENTIALS_WARPS,
    ESSENTIALS_KITS,
    ESSENTIALS_BACK
  };

  /**
   * Flag categories for UI organization.
   * Note: BLOCK_INTERACT is the parent of INTERACTION_FLAGS, MOB_SPAWNING is the parent of its children.
   */
  public static final String[] COMBAT_FLAGS = { PVP_ENABLED, FRIENDLY_FIRE, FRIENDLY_FIRE_FACTION, FRIENDLY_FIRE_ALLY, PROJECTILE_DAMAGE, MOB_DAMAGE, PVE_DAMAGE };

  public static final String[] DAMAGE_FLAGS = { FALL_DAMAGE, ENVIRONMENTAL_DAMAGE, EXPLOSION_DAMAGE, FIRE_SPREAD };

  public static final String[] DEATH_FLAGS = { KEEP_INVENTORY, POWER_LOSS };

  public static final String[] BUILDING_FLAGS = { BUILD_ALLOWED, BLOCK_PLACE, HAMMER_USE, BUILDER_TOOLS_USE };

  public static final String[] INTERACTION_FLAGS = { BLOCK_INTERACT, DOOR_USE, CONTAINER_USE, BENCH_USE, PROCESSING_USE, SEAT_USE, MOUNT_USE, LIGHT_USE, NPC_USE, NPC_TAME, NPC_INTERACT, CRATE_PICKUP, CRATE_PLACE };

  public static final String[] TRANSPORT_FLAGS = { TELEPORTER_USE, PORTAL_USE, MOUNT_ENTRY };

  public static final String[] ITEM_FLAGS = { ITEM_DROP, ITEM_PICKUP, ITEM_PICKUP_MANUAL, INVINCIBLE_ITEMS };

  public static final String[] SPAWNING_FLAGS = { MOB_SPAWNING, HOSTILE_MOB_SPAWNING, PASSIVE_MOB_SPAWNING, NEUTRAL_MOB_SPAWNING, NPC_SPAWNING };

  public static final String[] MOB_CLEAR_FLAGS = { MOB_CLEAR, HOSTILE_MOB_CLEAR, PASSIVE_MOB_CLEAR, NEUTRAL_MOB_CLEAR };

  public static final String[] INTEGRATION_FLAGS = { GRAVESTONE_ACCESS, SHOW_ON_MAP, ESSENTIALS_HOMES, ESSENTIALS_WARPS, ESSENTIALS_KITS, ESSENTIALS_BACK };

  /**
   * Flags that require OrbisGuard-Mixins to function.
   * When mixins are not installed, these flags will have no effect even if enabled.
   */
  public static final String[] MIXIN_DEPENDENT_FLAGS = {
    ITEM_PICKUP_MANUAL,  // Requires pickup mixin
    INVINCIBLE_ITEMS,    // Requires durability mixin
    KEEP_INVENTORY,      // Requires death mixin
    NPC_SPAWNING,        // Requires spawn mixin
    EXPLOSION_DAMAGE,    // Requires explosion mixin
    FIRE_SPREAD,         // Requires fire_spread mixin
    BUILDER_TOOLS_USE,   // Requires builder_tools mixin
    TELEPORTER_USE,      // Requires teleporter mixin
    PORTAL_USE,          // Requires portal mixin
    HAMMER_USE,          // Requires hammer mixin
    BLOCK_PLACE,         // Requires block_place mixin
    CRATE_PICKUP,        // Requires use mixin (CaptureCrateGate)
    CRATE_PLACE,         // Requires use mixin (CaptureCrateGate)
    NPC_TAME,            // Requires use mixin (SimpleInstantInteractionGate)
    MOUNT_USE            // Requires seating mixin (shared with SEAT_USE)
  };

  /**
   * Set of mixin-dependent flags for fast lookup.
   */
  private static final Set<String> MIXIN_FLAG_SET = Set.of(MIXIN_DEPENDENT_FLAGS);

  /**
   * Checks if a flag name is valid.
   *
   * @param flagName the flag name to check
   * @return true if valid
   */
  public static boolean isValidFlag(String flagName) {
    if (flagName == null) {
      return false;
    }
    for (String flag : ALL_FLAGS) {
      if (flag.equals(flagName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gets the default value for a flag in SafeZones.
   * SafeZones are protected areas where combat and building are disabled.
   *
   * <p>SafeZone philosophy:
   * - No combat, no building, no damage
   * - Allow basic traversal (doors, seats)
   * - Auto item pickup allowed, manual F-key pickup blocked
   * - Keep inventory on death (if mixins available)
   * - Invincible items (if mixins available)
   * - No mob spawning
   *
   * @param flagName the flag name
   * @return the default value
   */
  public static boolean getSafeZoneDefault(String flagName) {
    return switch (flagName) {
      // Combat: All disabled in SafeZones
      case PVP_ENABLED -> false;
      case FRIENDLY_FIRE -> false;
      case FRIENDLY_FIRE_FACTION -> true;   // Irrelevant when parent chain off, but default true for when toggled on
      case FRIENDLY_FIRE_ALLY -> true;      // Irrelevant when parent chain off, but default true for when toggled on
      case PROJECTILE_DAMAGE -> false;
      case MOB_DAMAGE -> false;
      case PVE_DAMAGE -> false;
      // Damage: No damage in safe zones
      case FALL_DAMAGE -> false;
      case ENVIRONMENTAL_DAMAGE -> false;
      case EXPLOSION_DAMAGE -> false;       // No explosions (mixin)
      case FIRE_SPREAD -> false;            // No fire spread (mixin)
      // Death: Keep inventory (mixin), no power loss
      case KEEP_INVENTORY -> true;
      case POWER_LOSS -> false;
      // Building: Disabled, but basic interaction allowed
      case BUILD_ALLOWED -> false;
      case BLOCK_PLACE -> false;            // No block placement (mixin)
      case HAMMER_USE -> false;             // No hammer cycling (mixin)
      case BUILDER_TOOLS_USE -> false;      // No builder tools (mixin)
      case BLOCK_INTERACT -> true;
      // Interaction: Doors/seats allowed, but no containers/benches/processing
      case DOOR_USE -> true;
      case CONTAINER_USE -> false;
      case BENCH_USE -> false;
      case PROCESSING_USE -> false;
      case SEAT_USE -> true;
      case MOUNT_USE -> false;              // No mounting in safe zones
      case LIGHT_USE -> false;              // No light toggling in safe zones
      // Entity Interaction: NPC parent off, taming/crates blocked, but NPC shops allowed
      case NPC_USE -> true;                 // NPC parent enabled (children control specifics)
      case CRATE_PICKUP -> false;           // No crate pickup (mixin)
      case CRATE_PLACE -> false;            // No crate placement (mixin)
      case NPC_TAME -> false;               // No NPC taming (mixin)
      case NPC_INTERACT -> true;            // NPC shops/dialogue allowed
      // Transport: Allowed in safe zones (safe havens, not prisons — transit should work)
      case TELEPORTER_USE -> true;          // Teleporters allowed (mixin)
      case PORTAL_USE -> true;              // Portals allowed (mixin)
      case MOUNT_ENTRY -> false;            // Dismount on entry to safe zones
      // Items: Auto pickup allowed, manual F-key blocked, items are invincible
      case ITEM_DROP -> false;
      case ITEM_PICKUP -> true;             // Auto pickup allowed
      case ITEM_PICKUP_MANUAL -> false;     // F-key pickup blocked (mixin)
      case INVINCIBLE_ITEMS -> true;        // No durability loss (mixin)
      // Mob Spawning: Entirely disabled in safe zones
      case MOB_SPAWNING -> false;
      case HOSTILE_MOB_SPAWNING -> false;
      case PASSIVE_MOB_SPAWNING -> false;
      case NEUTRAL_MOB_SPAWNING -> false;
      case NPC_SPAWNING -> false;           // Mixin spawn hook blocked
      // Mob Clearing: Only clear hostile mobs by default in safe zones
      case MOB_CLEAR -> true;
      case HOSTILE_MOB_CLEAR -> true;
      case PASSIVE_MOB_CLEAR -> false;
      case NEUTRAL_MOB_CLEAR -> false;
      // Integration: Protected in safe zones
      case GRAVESTONE_ACCESS -> false;
      case SHOW_ON_MAP -> false;      // Map hiding stays active in safe zones by default
      // Integration: HyperEssentials — all features available in safe zones (hubs/spawns)
      case ESSENTIALS_HOMES -> true;
      case ESSENTIALS_WARPS -> true;
      case ESSENTIALS_KITS -> true;
      case ESSENTIALS_BACK -> true;
      default -> false;
    };
  }

  /**
   * Gets the default value for a flag in WarZones.
   * WarZones are PvP-enabled areas where building is blocked to prevent griefing.
   *
   * <p>WarZone philosophy:
   * - Full PvP combat enabled
   * - No building to prevent griefing
   * - All item interactions allowed
   * - NO keep inventory - deaths have consequences
   * - NO invincible items - equipment can break
   * - Full mob spawning enabled
   *
   * @param flagName the flag name
   * @return the default value
   */
  public static boolean getWarZoneDefault(String flagName) {
    return switch (flagName) {
      // Combat: PvP and mob damage enabled
      case PVP_ENABLED -> true;
      case FRIENDLY_FIRE -> true;               // Full PvP in warzones by default
      case FRIENDLY_FIRE_FACTION -> true;       // Both allowed when parent toggled on
      case FRIENDLY_FIRE_ALLY -> true;          // Both allowed when parent toggled on
      case PROJECTILE_DAMAGE -> true;
      case MOB_DAMAGE -> true;
      case PVE_DAMAGE -> true;
      // Damage: All damage enabled
      case FALL_DAMAGE -> true;
      case ENVIRONMENTAL_DAMAGE -> true;
      case EXPLOSION_DAMAGE -> true;        // Explosions allowed
      case FIRE_SPREAD -> true;             // Fire spread allowed
      // Death: No keep inventory, no power loss in admin zones
      case KEEP_INVENTORY -> false;
      case POWER_LOSS -> false;
      // Building: Disabled to prevent griefing, but basic interaction allowed
      case BUILD_ALLOWED -> false;
      case BLOCK_PLACE -> true;             // Block placement if build allowed
      case HAMMER_USE -> true;              // Hammer cycling if build allowed
      case BUILDER_TOOLS_USE -> true;       // Builder tools if build allowed
      case BLOCK_INTERACT -> true;
      // Interaction: Doors/seats allowed, but no containers/benches/processing
      case DOOR_USE -> true;
      case CONTAINER_USE -> false;
      case BENCH_USE -> false;
      case PROCESSING_USE -> false;
      case SEAT_USE -> true;
      case MOUNT_USE -> false;              // No mounting in war zones
      case LIGHT_USE -> true;               // Light toggling allowed in war zones
      // Entity Interaction: All allowed in war zones
      case NPC_USE -> true;                 // NPC interactions allowed in war zones
      case CRATE_PICKUP -> true;            // Crate pickup allowed
      case CRATE_PLACE -> true;             // Crate placement allowed
      case NPC_TAME -> true;                // NPC taming allowed
      case NPC_INTERACT -> true;            // NPC shops/dialogue allowed
      // Transport: Allowed in war zones
      case TELEPORTER_USE -> true;
      case PORTAL_USE -> true;
      case MOUNT_ENTRY -> false;            // Dismount on entry to war zones
      // Items: All allowed - looting and item interactions permitted
      case ITEM_DROP -> true;
      case ITEM_PICKUP -> true;             // Auto pickup allowed
      case ITEM_PICKUP_MANUAL -> true;      // F-key pickup allowed
      case INVINCIBLE_ITEMS -> false;       // Items can break (no protection)
      // Mob Spawning: All mob spawning enabled in war zones
      case MOB_SPAWNING -> true;
      case HOSTILE_MOB_SPAWNING -> true;
      case PASSIVE_MOB_SPAWNING -> true;
      case NEUTRAL_MOB_SPAWNING -> true;
      case NPC_SPAWNING -> true;            // Mixin spawn hook allowed
      // Mob Clearing: No clearing in war zones (mobs add danger)
      case MOB_CLEAR -> false;
      case HOSTILE_MOB_CLEAR -> false;
      case PASSIVE_MOB_CLEAR -> false;
      case NEUTRAL_MOB_CLEAR -> false;
      // Integration: Free for all in war zones
      case GRAVESTONE_ACCESS -> true;
      case SHOW_ON_MAP -> false;      // Map hiding stays active in war zones by default
      // Integration: HyperEssentials — homes blocked in combat zones, warps/kits/back allowed
      case ESSENTIALS_HOMES -> false;
      case ESSENTIALS_WARPS -> true;
      case ESSENTIALS_KITS -> true;
      case ESSENTIALS_BACK -> true;
      default -> false;
    };
  }

  /**
   * Gets the default value for a flag based on zone type.
   *
   * @param flagName the flag name
   * @param type     the zone type
   * @return the default value
   */
  public static boolean getDefault(String flagName, @NotNull ZoneType type) {
    return type == ZoneType.SAFE ? getSafeZoneDefault(flagName) : getWarZoneDefault(flagName);
  }

  /**
   * Gets a map of all default flags for a zone type.
   * Useful for importing zones from mods that don't have a flag system.
   *
   * @param type the zone type
   * @return map of flag name to default value
   */
  @NotNull
  public static Map<String, Boolean> getDefaultFlags(@NotNull ZoneType type) {
    Map<String, Boolean> defaults = new HashMap<>();
    for (String flag : ALL_FLAGS) {
      defaults.put(flag, getDefault(flag, type));
    }
    return defaults;
  }

  /**
   * Gets a human-readable display name for a flag.
   *
   * @param flagName the flag name
   * @return the display name
   */
  @NotNull
  public static String getDisplayName(String flagName) {
    return switch (flagName) {
      case PVP_ENABLED -> "PvP Enabled";
      case FRIENDLY_FIRE -> "Friendly Fire";
      case FRIENDLY_FIRE_FACTION -> "Faction Damage";
      case FRIENDLY_FIRE_ALLY -> "Ally Damage";
      case PROJECTILE_DAMAGE -> "Projectile Damage";
      case MOB_DAMAGE -> "Take Mob Damage";
      case PVE_DAMAGE -> "Give Mob Damage";
      case FALL_DAMAGE -> "Fall Damage";
      case ENVIRONMENTAL_DAMAGE -> "Env. Damage";
      case EXPLOSION_DAMAGE -> "Explosion Damage";
      case FIRE_SPREAD -> "Fire Spread";
      case KEEP_INVENTORY -> "Keep Inventory";
      case POWER_LOSS -> "Power Loss";
      case BUILD_ALLOWED -> "Building Allowed";
      case BLOCK_PLACE -> "Block Placement";
      case HAMMER_USE -> "Hammer Use";
      case BUILDER_TOOLS_USE -> "Builder Tools";
      case BLOCK_INTERACT -> "Block Interaction";
      case DOOR_USE -> "Door Use";
      case CONTAINER_USE -> "Container Use";
      case BENCH_USE -> "Bench Use";
      case PROCESSING_USE -> "Processing Use";
      case SEAT_USE -> "Seat Use";
      case MOUNT_USE -> "Mount Use";
      case LIGHT_USE -> "Light Use";
      case NPC_USE -> "NPC Interaction";
      case CRATE_PICKUP -> "Crate Pickup";
      case CRATE_PLACE -> "Crate Place";
      case NPC_TAME -> "NPC Tame";
      case NPC_INTERACT -> "NPC Interact";
      case TELEPORTER_USE -> "Teleporter Use";
      case PORTAL_USE -> "Portal Use";
      case MOUNT_ENTRY -> "Mount Entry";
      case ITEM_DROP -> "Item Drop";
      case ITEM_PICKUP -> "Auto Pickup";
      case ITEM_PICKUP_MANUAL -> "F-Key Pickup";
      case INVINCIBLE_ITEMS -> "Invincible Items";
      case MOB_SPAWNING -> "Mob Spawning";
      case HOSTILE_MOB_SPAWNING -> "Hostile Mobs";
      case PASSIVE_MOB_SPAWNING -> "Passive Mobs";
      case NEUTRAL_MOB_SPAWNING -> "Neutral Mobs";
      case NPC_SPAWNING -> "NPC Spawning";
      case MOB_CLEAR -> "Mob Clearing";
      case HOSTILE_MOB_CLEAR -> "Clear Hostile Mobs";
      case PASSIVE_MOB_CLEAR -> "Clear Passive Mobs";
      case NEUTRAL_MOB_CLEAR -> "Clear Neutral Mobs";
      case GRAVESTONE_ACCESS -> "Others Loot Graves";
      case SHOW_ON_MAP -> "Show on Map";
      case ESSENTIALS_HOMES -> "Home Use";
      case ESSENTIALS_WARPS -> "Warp Use";
      case ESSENTIALS_KITS -> "Kit Claiming";
      case ESSENTIALS_BACK -> "Back Teleport";
      default -> flagName;
    };
  }

  /**
   * Gets a short description for a flag.
   *
   * @param flagName the flag name
   * @return the description
   */
  @NotNull
  public static String getDescription(String flagName) {
    return switch (flagName) {
      case PVP_ENABLED -> "Players can damage other players (parent)";
      case FRIENDLY_FIRE -> "Allow friendly damage when PvP is on (parent)";
      case FRIENDLY_FIRE_FACTION -> "Same-faction players can damage each other";
      case FRIENDLY_FIRE_ALLY -> "Allied faction players can damage each other";
      case PROJECTILE_DAMAGE -> "Projectiles deal damage";
      case MOB_DAMAGE -> "Mobs can damage players";
      case PVE_DAMAGE -> "Players can damage mobs";
      case FALL_DAMAGE -> "Fall damage applies";
      case ENVIRONMENTAL_DAMAGE -> "Drowning, suffocation, etc.";
      case EXPLOSION_DAMAGE -> "Explosions can damage blocks (requires mixin)";
      case FIRE_SPREAD -> "Fire can spread to nearby blocks (requires mixin)";
      case KEEP_INVENTORY -> "Keep items on death (requires mixin)";
      case POWER_LOSS -> "Players lose faction power on death";
      case BUILD_ALLOWED -> "Players can place and break blocks (parent)";
      case BLOCK_PLACE -> "Block placement via mixin (requires mixin)";
      case HAMMER_USE -> "Hammer block cycling (requires mixin)";
      case BUILDER_TOOLS_USE -> "Builder tools paste/copy (requires mixin)";
      case BLOCK_INTERACT -> "General block interaction (parent)";
      case DOOR_USE -> "Players can use doors and gates";
      case CONTAINER_USE -> "Players can use chests and storage";
      case BENCH_USE -> "Players can use crafting tables";
      case PROCESSING_USE -> "Players can use furnaces and smelters";
      case SEAT_USE -> "Players can sit on seats and chairs";
      case MOUNT_USE -> "Players can mount rideable entities (requires mixin)";
      case LIGHT_USE -> "Players can toggle lights, lanterns, and campfires";
      case NPC_USE -> "Players can interact with NPCs (parent)";
      case CRATE_PICKUP -> "Pick up animals with capture crate (requires mixin)";
      case CRATE_PLACE -> "Release animals from capture crate (requires mixin)";
      case NPC_TAME -> "Tame NPCs with F-key (requires mixin)";
      case NPC_INTERACT -> "Players can interact with NPCs (shops, dialogue)";
      case TELEPORTER_USE -> "Players can use teleporter blocks (requires mixin)";
      case PORTAL_USE -> "Players can use portal blocks (requires mixin)";
      case MOUNT_ENTRY -> "Mounted players can enter (dismounts if false)";
      case ITEM_DROP -> "Players can drop items";
      case ITEM_PICKUP -> "Auto-collect items when walking over them";
      case ITEM_PICKUP_MANUAL -> "Pick up items with F-key (requires mixin)";
      case INVINCIBLE_ITEMS -> "Items don't lose durability (requires mixin)";
      case MOB_SPAWNING -> "Master toggle for mob spawning (parent)";
      case HOSTILE_MOB_SPAWNING -> "Aggressive mobs can spawn";
      case PASSIVE_MOB_SPAWNING -> "Non-aggressive mobs can spawn";
      case NEUTRAL_MOB_SPAWNING -> "Conditionally aggressive mobs can spawn";
      case NPC_SPAWNING -> "NPC spawning via mixin (requires mixin)";
      case MOB_CLEAR -> "Actively remove mobs from this zone (parent)";
      case HOSTILE_MOB_CLEAR -> "Periodically remove hostile mobs";
      case PASSIVE_MOB_CLEAR -> "Periodically remove passive mobs";
      case NEUTRAL_MOB_CLEAR -> "Periodically remove neutral mobs";
      case GRAVESTONE_ACCESS -> "Non-owners can loot/break other players' gravestones (owners always can)";
      case SHOW_ON_MAP -> "Override map visibility for players in this zone";
      case ESSENTIALS_HOMES -> "Players can set and teleport to homes (HyperEssentials)";
      case ESSENTIALS_WARPS -> "Players can teleport to warps (HyperEssentials)";
      case ESSENTIALS_KITS -> "Players can claim kits (HyperEssentials)";
      case ESSENTIALS_BACK -> "Players can use /back to return to previous locations (HyperEssentials)";
      default -> "Unknown flag";
    };
  }

  /**
   * Gets the parent flag for a flag, if it has one.
   * Child flags only take effect when their parent is enabled.
   *
   * @param flagName the flag name
   * @return the parent flag name, or null if no parent
   */
  @Nullable
  public static String getParentFlag(String flagName) {
    return switch (flagName) {
      // Combat hierarchy: PVP_ENABLED → FRIENDLY_FIRE → FACTION/ALLY
      case FRIENDLY_FIRE -> PVP_ENABLED;
      case FRIENDLY_FIRE_FACTION, FRIENDLY_FIRE_ALLY -> FRIENDLY_FIRE;
      // Building children have BUILD_ALLOWED as parent
      case BLOCK_PLACE, HAMMER_USE, BUILDER_TOOLS_USE -> BUILD_ALLOWED;
      // Interaction flags have BLOCK_INTERACT as parent
      case DOOR_USE, CONTAINER_USE, BENCH_USE, PROCESSING_USE, SEAT_USE, MOUNT_USE, LIGHT_USE -> BLOCK_INTERACT;
      // NPC flags have NPC_USE as parent
      case NPC_TAME, NPC_INTERACT -> NPC_USE;
      // Mob group flags have MOB_SPAWNING as parent
      case HOSTILE_MOB_SPAWNING, PASSIVE_MOB_SPAWNING, NEUTRAL_MOB_SPAWNING, NPC_SPAWNING -> MOB_SPAWNING;
      // Mob clearing children have MOB_CLEAR as parent
      case HOSTILE_MOB_CLEAR, PASSIVE_MOB_CLEAR, NEUTRAL_MOB_CLEAR -> MOB_CLEAR;
      default -> null;
    };
  }

  /**
   * Checks if a flag is a parent flag (has children).
   *
   * @param flagName the flag name
   * @return true if this is a parent flag
   */
  public static boolean isParentFlag(String flagName) {
    return PVP_ENABLED.equals(flagName) || FRIENDLY_FIRE.equals(flagName)
       || BUILD_ALLOWED.equals(flagName) || BLOCK_INTERACT.equals(flagName)
       || NPC_USE.equals(flagName) || MOB_SPAWNING.equals(flagName)
       || MOB_CLEAR.equals(flagName);
  }

  /**
   * Gets the child flags for a parent flag.
   *
   * @param parentFlagName the parent flag name
   * @return array of child flag names, or empty array if not a parent
   */
  @NotNull
  public static String[] getChildFlags(String parentFlagName) {
    return switch (parentFlagName) {
      case PVP_ENABLED -> new String[] { FRIENDLY_FIRE };
      case FRIENDLY_FIRE -> new String[] { FRIENDLY_FIRE_FACTION, FRIENDLY_FIRE_ALLY };
      case BUILD_ALLOWED -> new String[] { BLOCK_PLACE, HAMMER_USE, BUILDER_TOOLS_USE };
      case BLOCK_INTERACT -> new String[] { DOOR_USE, CONTAINER_USE, BENCH_USE, PROCESSING_USE, SEAT_USE, MOUNT_USE, LIGHT_USE };
      case NPC_USE -> new String[] { NPC_TAME, NPC_INTERACT };
      case MOB_SPAWNING -> new String[] { HOSTILE_MOB_SPAWNING, PASSIVE_MOB_SPAWNING, NEUTRAL_MOB_SPAWNING, NPC_SPAWNING };
      case MOB_CLEAR -> new String[] { HOSTILE_MOB_CLEAR, PASSIVE_MOB_CLEAR, NEUTRAL_MOB_CLEAR };
      default -> new String[0];
    };
  }

  /**
   * Gets the conflicting spawn flag for a mob clear flag.
   * When a spawning sub-flag is true, the corresponding clear sub-flag
   * must resolve to false (it's contradictory to spawn AND clear the same type).
   *
   * <p>Only child-level conflicts. Parent MOB_CLEAR does NOT conflict with MOB_SPAWNING.
   *
   * @param clearFlagName the clear flag name
   * @return the conflicting spawn flag name, or null if no conflict
   */
  @Nullable
  public static String getConflictingSpawnFlag(String clearFlagName) {
    return switch (clearFlagName) {
      case HOSTILE_MOB_CLEAR -> HOSTILE_MOB_SPAWNING;
      case PASSIVE_MOB_CLEAR -> PASSIVE_MOB_SPAWNING;
      case NEUTRAL_MOB_CLEAR -> NEUTRAL_MOB_SPAWNING;
      default -> null;
    };
  }

  // ==========================================================================
  // MIXIN DEPENDENCY METHODS
  // ==========================================================================

  /**
   * Checks if a flag requires OrbisGuard-Mixins to function.
   *
   * @param flagName the flag name
   * @return true if this flag requires mixin support
   */
  public static boolean requiresMixin(String flagName) {
    return MIXIN_FLAG_SET.contains(flagName);
  }

  /**
   * Gets the specific mixin type required for a flag.
   *
   * @param flagName the flag name
   * @return the mixin type constant, or null if no mixin required
   */
  @Nullable
  public static String getMixinType(String flagName) {
    return switch (flagName) {
      case ITEM_PICKUP_MANUAL -> MIXIN_PICKUP;
      case INVINCIBLE_ITEMS -> MIXIN_DURABILITY;
      case KEEP_INVENTORY -> MIXIN_DEATH;
      case NPC_SPAWNING -> MIXIN_SPAWN;
      case EXPLOSION_DAMAGE -> MIXIN_EXPLOSION;
      case FIRE_SPREAD -> MIXIN_FIRE_SPREAD;
      case BUILDER_TOOLS_USE -> MIXIN_BUILDER_TOOLS;
      case TELEPORTER_USE -> MIXIN_TELEPORTER;
      case PORTAL_USE -> MIXIN_PORTAL;
      case HAMMER_USE -> MIXIN_HAMMER;
      case BLOCK_PLACE -> MIXIN_PLACE;
      case CRATE_PICKUP, CRATE_PLACE, NPC_TAME -> MIXIN_USE;
      case MOUNT_USE -> MIXIN_SEATING;
      default -> null;
    };
  }

  /**
   * Gets a human-readable name for a mixin type.
   *
   * @param mixinType the mixin type constant
   * @return the display name
   */
  @NotNull
  public static String getMixinDisplayName(String mixinType) {
    return switch (mixinType) {
      case MIXIN_PICKUP -> "Item Pickup Mixin";
      case MIXIN_DEATH -> "Death Event Mixin";
      case MIXIN_DURABILITY -> "Durability Mixin";
      case MIXIN_SEATING -> "Seating Mixin";
      case MIXIN_SPAWN -> "NPC Spawn Mixin";
      case MIXIN_EXPLOSION -> "Explosion Mixin";
      case MIXIN_FIRE_SPREAD -> "Fire Spread Mixin";
      case MIXIN_BUILDER_TOOLS -> "Builder Tools Mixin";
      case MIXIN_TELEPORTER -> "Teleporter Mixin";
      case MIXIN_PORTAL -> "Portal Mixin";
      case MIXIN_HAMMER -> "Hammer Mixin";
      case MIXIN_PLACE -> "Block Place Mixin";
      case MIXIN_USE -> "Use Hook Mixin";
      default -> "Unknown Mixin";
    };
  }

  /**
   * Gets the category name for UI organization.
   *
   * @param flagName the flag name
   * @return the category name
   */
  @NotNull
  public static String getCategory(String flagName) {
    for (String f : COMBAT_FLAGS) {
      if (f.equals(flagName)) {
        return "Combat";
      }
    }
    for (String f : BUILDING_FLAGS) {
      if (f.equals(flagName)) {
        return "Building";
      }
    }
    for (String f : DAMAGE_FLAGS) {
      if (f.equals(flagName)) {
        return "Damage";
      }
    }
    for (String f : DEATH_FLAGS) {
      if (f.equals(flagName)) {
        return "Death";
      }
    }
    for (String f : SPAWNING_FLAGS) {
      if (f.equals(flagName)) {
        return "Spawning";
      }
    }
    for (String f : MOB_CLEAR_FLAGS) {
      if (f.equals(flagName)) {
        return "Mob Clearing";
      }
    }
    for (String f : INTERACTION_FLAGS) {
      if (f.equals(flagName)) {
        return "Interaction";
      }
    }
    for (String f : TRANSPORT_FLAGS) {
      if (f.equals(flagName)) {
        return "Transport";
      }
    }
    for (String f : ITEM_FLAGS) {
      if (f.equals(flagName)) {
        return "Items";
      }
    }
    for (String f : INTEGRATION_FLAGS) {
      if (f.equals(flagName)) {
        return "Integration";
      }
    }
    return "Other";
  }

  /**
   * Gets all flags organized by category for UI display.
   *
   * @return map of category name to flag arrays
   */
  @NotNull
  public static Map<String, String[]> getFlagsByCategory() {
    Map<String, String[]> categories = new HashMap<>();
    categories.put("Combat", COMBAT_FLAGS);
    categories.put("Building", BUILDING_FLAGS);
    categories.put("Interaction", INTERACTION_FLAGS);
    categories.put("Items", ITEM_FLAGS);
    categories.put("Damage", DAMAGE_FLAGS);
    categories.put("Death", DEATH_FLAGS);
    categories.put("Transport", TRANSPORT_FLAGS);
    categories.put("Spawning", SPAWNING_FLAGS);
    categories.put("Mob Clearing", MOB_CLEAR_FLAGS);
    categories.put("Integration", INTEGRATION_FLAGS);
    return categories;
  }

  /**
   * Gets the ordered list of category names for UI display.
   *
   * @return array of category names in display order
   */
  @NotNull
  public static String[] getCategoryOrder() {
    return new String[] {
      "Combat",
      "Damage",
      "Death",
      "Building",
      "Interaction",
      "Transport",
      "Items",
      "Spawning",
      "Mob Clearing",
      "Integration"
    };
  }

  // ==========================================================================
  // STRING-VALUED SETTINGS (enum/selection flags)
  // ==========================================================================

  /** Map visibility level — controls which players are visible on the map when SHOW_ON_MAP is enabled. */
  public static final String MAP_VISIBILITY = "map_visibility";

  /** Map visibility options. */
  public static final String MAP_VISIBILITY_FACTION = "faction";
  public static final String MAP_VISIBILITY_ALLY = "ally";
  public static final String MAP_VISIBILITY_ALL = "all";

  /** All valid values for MAP_VISIBILITY. */
  public static final String[] MAP_VISIBILITY_OPTIONS = {
    MAP_VISIBILITY_FACTION, MAP_VISIBILITY_ALLY, MAP_VISIBILITY_ALL
  };

  /** All available zone settings. */
  public static final String[] ALL_SETTINGS = { MAP_VISIBILITY };

  /**
   * Gets the default value for a string setting based on zone type.
   *
   * @param settingName the setting name
   * @param type        the zone type
   * @return the default value
   */
  @NotNull
  public static String getSettingDefault(@NotNull String settingName, @NotNull ZoneType type) {
    return switch (settingName) {
      case MAP_VISIBILITY -> MAP_VISIBILITY_FACTION; // Default: show only own faction
      default -> "";
    };
  }

  /**
   * Checks if a setting value is valid for the given setting.
   *
   * @param settingName the setting name
   * @param value       the value to check
   * @return true if valid
   */
  public static boolean isValidSettingValue(@NotNull String settingName, @NotNull String value) {
    return switch (settingName) {
      case MAP_VISIBILITY -> {
        for (String opt : MAP_VISIBILITY_OPTIONS) {
          if (opt.equals(value)) yield true;
        }
        yield false;
      }
      default -> false;
    };
  }

  /**
   * Gets a display name for a setting value.
   *
   * @param settingName the setting name
   * @param value       the value
   * @return display text
   */
  @NotNull
  public static String getSettingValueDisplay(@NotNull String settingName, @NotNull String value) {
    if (MAP_VISIBILITY.equals(settingName)) {
      return switch (value) {
        case MAP_VISIBILITY_FACTION -> "Faction Only";
        case MAP_VISIBILITY_ALLY -> "Faction + Allies";
        case MAP_VISIBILITY_ALL -> "All Players";
        default -> value;
      };
    }
    return value;
  }

  /**
   * Gets the available options for a setting.
   *
   * @param settingName the setting name
   * @return array of valid values
   */
  @NotNull
  public static String[] getSettingOptions(@NotNull String settingName) {
    return switch (settingName) {
      case MAP_VISIBILITY -> MAP_VISIBILITY_OPTIONS;
      default -> new String[0];
    };
  }

  /**
   * Gets the display name for a setting.
   *
   * @param settingName the setting name
   * @return the display name
   */
  @NotNull
  public static String getSettingDisplayName(@NotNull String settingName) {
    return switch (settingName) {
      case MAP_VISIBILITY -> "Map Visibility";
      default -> settingName;
    };
  }

  /**
   * Gets the description for a setting.
   *
   * @param settingName the setting name
   * @return the description
   */
  @NotNull
  public static String getSettingDescription(@NotNull String settingName) {
    return switch (settingName) {
      case MAP_VISIBILITY -> "Which players are visible on the world map in this zone";
      default -> "Unknown setting";
    };
  }
}
