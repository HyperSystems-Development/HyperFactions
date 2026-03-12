package com.hyperfactions.integration.protection;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.protection.ProtectionChecker;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReferenceArray;
import org.jetbrains.annotations.NotNull;

/**
 * Integration with HyperProtect-Mixin using the bridge-slot architecture.
 *
 * <p>Registers hook wrapper objects at numeric slot indices in the bridge
 * {@link AtomicReferenceArray}. Each wrapper exposes methods matching the
 * exact signatures that HyperProtect interceptors resolve via MethodHandle.
 *
 * <p>Verdict protocol:
 * <ul>
 *   <li>0 = ALLOW</li>
 *   <li>1 = DENY_WITH_MESSAGE (interceptor calls fetchDenyReason)</li>
 *   <li>2 = DENY_SILENT</li>
 *   <li>3 = DENY_MOD_HANDLES (consumer mod sends messages)</li>
 * </ul>
 */
public final class HyperProtectIntegration {

  // Verdict constants
  private static final int ALLOW             = 0;

  private static final int DENY_WITH_MESSAGE = 1;

  private static final int DENY_SILENT       = 2;

  /**
   * Reads the interaction class name set by HyperProtect-Mixin before invoking the hook.
   * Returns a short simple name (e.g. "UseCaptureCrateInteraction") for logging.
   * Returns "unknown" if the mixin doesn't set this (older mixin version).
   */
  private static String getInteractionName() {
    try {
      Object val = System.getProperties().get("hyperprotect.context.interaction");
      if (val instanceof String s && !s.isEmpty()) {
        int dot = s.lastIndexOf('.');
        return dot >= 0 ? s.substring(dot + 1) : s;
      }
    } catch (Exception ignored) {}
    return "unknown";
  }

  /**
   * Reads and CONSUMES the block ID set by the mixin for block interactions.
   * Returns null if not set (non-block interaction or old mixin version).
   */
  private static String consumeBlockId() {
    try {
      Object val = System.getProperties().remove("hyperprotect.context.block_id");
      if (val instanceof String s && !s.isEmpty()) {
        return s;
      }
    } catch (Exception ignored) {}
    return null;
  }

  /**
   * Reads and CONSUMES the block state set by the mixin for block interactions.
   * The state indicates the block's interaction type (e.g. "Door", "container",
   * "processingBench"). Returns null if not set or block has no state.
   */
  private static String consumeBlockState() {
    try {
      Object val = System.getProperties().remove("hyperprotect.context.block_state");
      if (val instanceof String s && !s.isEmpty()) {
        return s;
      }
    } catch (Exception ignored) {}
    return null;
  }

  /**
   * Reads and CONSUMES the NPC role name set by the mixin.
   * Clears the system property after reading to prevent stale context
   * leaking to subsequent non-NPC interactions handled by other gates.
   */
  private static String consumeNpcRole() {
    try {
      Object val = System.getProperties().remove("hyperprotect.context.npc_role");
      if (val instanceof String s && !s.isEmpty()) {
        return s;
      }
    } catch (Exception ignored) {}
    return null;
  }

  /**
   * Determines if an NPC role represents a tameable creature (animals, pets).
   * Tameable roles use NPC_TAME (restricted). Everything else — service NPCs,
   * 3rd-party mod NPCs (KyuubiSoft Citizens, etc.), and unknown roles — defaults
   * to NPC_INTERACT (permissive, fail-open) to avoid blocking legitimate interactions.
   *
   * <p>Hytale's tameable creatures use their species name as the role (e.g. "Chicken",
   * "Wolf", "Cow"), while service/dialogue NPCs use descriptive roles like "Merchant",
   * "Guard", or mod-prefixed roles like "KS_NPC_Interactable_Role".
   */
  private static boolean isTameableCreatureRole(String roleName) {
    String lower = roleName.toLowerCase();
    // Known Hytale tameable creature species
    return lower.equals("chicken")
        || lower.equals("wolf")
        || lower.equals("cow")
        || lower.equals("pig")
        || lower.equals("sheep")
        || lower.equals("cat")
        || lower.equals("horse")
        || lower.equals("donkey")
        || lower.equals("rabbit")
        || lower.equals("parrot")
        || lower.equals("fox")
        || lower.equals("goat")
        || lower.equals("camel")
        || lower.contains("_tame")
        || lower.contains("tamed_")
        || lower.contains("tameable")
        || lower.contains("pet_");
  }

  /**
   * Determines if an NPC role represents a rideable/mountable creature.
   * These use the MOUNT interaction type instead of NPC_TAME or NPC_INTERACT.
   */
  private static boolean isMountableCreatureRole(String roleName) {
    String lower = roleName.toLowerCase();
    return lower.startsWith("tamed_horse")
        || lower.startsWith("tamed_donkey")
        || lower.startsWith("tamed_camel")
        || lower.contains("mount")
        || lower.contains("rideable");
  }

  /**
   * Checks if a block ID indicates a light/lantern/campfire/torch block.
   * These blocks use the LIGHT_USE zone flag instead of generic BLOCK_INTERACT.
   */
  private static boolean isLightBlock(String blockId) {
    if (blockId == null) {
      return false;
    }
    String lower = blockId.toLowerCase();
    return lower.contains("lantern") || lower.contains("campfire")
        || lower.contains("torch") || lower.contains("candle")
        || lower.contains("lamp");
  }

  // Bridge slot indices (must match ProtectionBridge constants in HyperProtect-Mixin)
  private static final int SLOT_BLOCK_BREAK      = 0;

  private static final int SLOT_EXPLOSION        = 1;

  private static final int SLOT_FIRE_SPREAD      = 2;

  private static final int SLOT_BUILDER_TOOLS    = 3;

  private static final int SLOT_ITEM_PICKUP      = 4;

  private static final int SLOT_DEATH_DROP       = 5;

  private static final int SLOT_DURABILITY       = 6;

  private static final int SLOT_CONTAINER_ACCESS = 7;

  private static final int SLOT_MOB_SPAWN        = 8;

  private static final int SLOT_TELEPORTER       = 9;

  private static final int SLOT_PORTAL           = 10;

  private static final int SLOT_COMMAND          = 11;

  private static final int SLOT_INTERACTION_LOG  = 12;

  private static final int SLOT_FORMAT_HANDLE    = 15;

  private static final int SLOT_ENTITY_DAMAGE    = 16;

  private static final int SLOT_CONTAINER_OPEN   = 17;

  private static final int SLOT_BLOCK_PLACE      = 18;

  private static final int SLOT_HAMMER           = 19;

  private static final int SLOT_USE              = 20;

  private static final int SLOT_SEAT             = 21;

  private static final int SLOT_RESPAWN          = 22;

  // v1.2.0 slots
  private static final int SLOT_CRAFTING_RESOURCE = 23;

  private static final int SLOT_MAP_MARKER_FILTER = 24;

  private static final int SLOT_FLUID_SPREAD      = 25;

  private static final int SLOT_PREFAB_SPAWN      = 26;

  private static final int SLOT_PROJECTILE_LAUNCH = 27;

  private static final int SLOT_MOUNT             = 28;

  private static final int SLOT_BARTER_TRADE      = 29;

  private HyperProtectIntegration() {}

  /**
   * Detects the HyperProtect-Mixin version from the JAR filename in earlyplugins/.
   * Falls back to "unknown" if not found.
   */
  private static String detectVersion() {
    try {
      Path epDir = Path.of(System.getProperty("user.dir")).resolve("earlyplugins");
      if (Files.isDirectory(epDir)) {
        try (var stream = Files.list(epDir)) {
          var match = stream
              .map(p -> p.getFileName().toString())
              .filter(n -> n.startsWith("HyperProtect-Mixin-") && n.endsWith(".jar"))
              .findFirst();
          if (match.isPresent()) {
            // Extract version from "HyperProtect-Mixin-X.Y.Z.jar"
            String name = match.get();
            return name.substring("HyperProtect-Mixin-".length(), name.length() - ".jar".length());
          }
        }
      }
    } catch (Exception e) {
      Logger.debugMixin("Failed to detect HP version from JAR: %s", e.getMessage());
    }
    return "unknown";
  }

  /**
   * Safely sets a bridge slot, handling old HP versions with fewer slots.
   * Silently skips if the bridge is too small for the requested slot.
   */
  private static void safeSetSlot(AtomicReferenceArray<Object> bridge, int slot, Object hook) {
    if (bridge.length() > slot) {
      bridge.set(slot, hook);
    }
    // Silently skip if bridge too small (old HP version)
  }

  /**
   * Safely clears a bridge slot, handling old HP versions with fewer slots.
   */
  private static void safeClearSlot(AtomicReferenceArray<Object> bridge, int slot) {
    if (bridge.length() > slot) {
      bridge.set(slot, null);
    }
  }

  /**
   * Registers all applicable hooks with the HyperProtect bridge.
   */
  @SuppressWarnings("unchecked")
  public static void registerAllHooks(@NotNull HyperFactions hf) {
    Object bridgeObj = System.getProperties().get("hyperprotect.bridge");
    if (bridgeObj == null) {
      // Bridge not initialized (Hyxin doesn't instantiate early plugin Main class).
      // Create it lazily — interceptors read from it via System.getProperties().
      bridgeObj = new AtomicReferenceArray<Object>(30);
      Object existing = System.getProperties().putIfAbsent("hyperprotect.bridge", bridgeObj);
      if (existing != null) {
        bridgeObj = existing;
      }
      System.setProperty("hyperprotect.bridge.active", "true");
      System.setProperty("hyperprotect.bridge.version", detectVersion());
    }
    if (!(bridgeObj instanceof AtomicReferenceArray<?> rawBridge)) {
      Logger.warn("HyperProtect bridge has unexpected type — cannot register hooks");
      return;
    }

    AtomicReferenceArray<Object> bridge = (AtomicReferenceArray<Object>) rawBridge;
    ProtectionChecker checker = hf.getProtectionChecker();

    // Register ALL hooks unconditionally. Hyxin does not call the early plugin's setup(),
    // so hyperprotect.intercept.* system properties are NOT set at this point — they only
    // get set when mixin target classes load later. We populate all bridge slots now so
    // the hooks are ready when mixin interceptors eventually fire.
    bridge.set(SLOT_BLOCK_BREAK, new BlockBreakHook(checker));
    bridge.set(SLOT_EXPLOSION, new ExplosionHook(checker));
    bridge.set(SLOT_FIRE_SPREAD, new FireSpreadHook(checker));
    bridge.set(SLOT_BUILDER_TOOLS, new BuilderToolsHook(checker));
    bridge.set(SLOT_ITEM_PICKUP, new ItemPickupHook(checker));
    bridge.set(SLOT_DEATH_DROP, new DeathDropHook(checker));
    bridge.set(SLOT_DURABILITY, new DurabilityHook(checker));
    bridge.set(SLOT_CONTAINER_ACCESS, new ContainerAccessHook(checker));
    bridge.set(SLOT_MOB_SPAWN, new MobSpawnHook(checker));
    bridge.set(SLOT_TELEPORTER, new TeleporterHook(checker));
    bridge.set(SLOT_PORTAL, new PortalHook(checker));
    bridge.set(SLOT_COMMAND, new CommandHook(hf));
    bridge.set(SLOT_INTERACTION_LOG, new InteractionLogHook());
    bridge.set(SLOT_ENTITY_DAMAGE, new EntityDamageHook(checker));
    bridge.set(SLOT_CONTAINER_OPEN, new ContainerOpenHook(checker));
    bridge.set(SLOT_BLOCK_PLACE, new BlockPlaceHook(checker));
    bridge.set(SLOT_HAMMER, new HammerHook(checker));
    bridge.set(SLOT_USE, new UseHook(checker));
    bridge.set(SLOT_SEAT, new SeatHook(checker));
    bridge.set(SLOT_RESPAWN, new RespawnHook(hf));

    // v1.2.0 hooks (use safeSetSlot for backwards compat with old HP bridges)
    safeSetSlot(bridge, SLOT_CRAFTING_RESOURCE, new CraftingResourceHook(checker));
    safeSetSlot(bridge, SLOT_MAP_MARKER_FILTER, new MapMarkerFilterHook(hf));
    safeSetSlot(bridge, SLOT_FLUID_SPREAD, new FluidSpreadHook(checker));
    safeSetSlot(bridge, SLOT_PREFAB_SPAWN, new PrefabSpawnHook(checker));
    safeSetSlot(bridge, SLOT_PROJECTILE_LAUNCH, new ProjectileLaunchHook(checker));
    safeSetSlot(bridge, SLOT_MOUNT, new MountHook(checker));
    safeSetSlot(bridge, SLOT_BARTER_TRADE, new BarterTradeHook(checker));

    // Register message formatter at slot 15 — converts raw String → Message for deny messages.
    // All mixin interceptors call getBridge(15) to format deny reason strings before sending.
    try {
      MethodHandle fmtHandle = MethodHandles.publicLookup().findStatic(
          HyperProtectIntegration.class, "formatDenyMessage",
          MethodType.methodType(Message.class, String.class));
      bridge.set(SLOT_FORMAT_HANDLE, fmtHandle);
    } catch (Exception e) {
      ErrorHandler.report("Failed to register format handle at slot 15", e);
    }

    Logger.debug("Registered 27 HyperProtect hook(s) + format handle at bridge slots (unconditional)");
  }

  /**
   * Unregisters all hooks by nulling bridge slots.
   */
  @SuppressWarnings("unchecked")
  public static void unregisterAllHooks() {
    Object bridgeObj = System.getProperties().get("hyperprotect.bridge");
    if (!(bridgeObj instanceof AtomicReferenceArray<?> rawBridge)) {
      return;
    }

    AtomicReferenceArray<Object> bridge = (AtomicReferenceArray<Object>) rawBridge;
    int[] slots = {
      SLOT_BLOCK_BREAK, SLOT_EXPLOSION, SLOT_FIRE_SPREAD, SLOT_BUILDER_TOOLS,
      SLOT_ITEM_PICKUP, SLOT_DEATH_DROP, SLOT_DURABILITY, SLOT_CONTAINER_ACCESS,
      SLOT_MOB_SPAWN, SLOT_TELEPORTER, SLOT_PORTAL, SLOT_COMMAND, SLOT_INTERACTION_LOG,
      SLOT_FORMAT_HANDLE, SLOT_ENTITY_DAMAGE, SLOT_CONTAINER_OPEN, SLOT_BLOCK_PLACE,
      SLOT_HAMMER, SLOT_USE, SLOT_SEAT, SLOT_RESPAWN
    };
    for (int slot : slots) {
      bridge.set(slot, null);
    }
    // v1.2.0 slots (safe clear for old HP bridges)
    int[] newSlots = {
      SLOT_CRAFTING_RESOURCE, SLOT_MAP_MARKER_FILTER, SLOT_FLUID_SPREAD,
      SLOT_PREFAB_SPAWN, SLOT_PROJECTILE_LAUNCH, SLOT_MOUNT, SLOT_BARTER_TRADE
    };
    for (int slot : newSlots) {
      safeClearSlot(bridge, slot);
    }
    Logger.debugMixin("Unregistered all HyperProtect hooks");
  }

  /**
   * Registers hooks ONLY for the 5 unique HP mixins (used in BOTH mode).
   *
   * <p>When OrbisGuard handles the 17 overlapping features, HP still provides:
   * <ul>
   *   <li>SimpleBlockInteractionGate: use, hammer, seat, container_open, teleporter, portal, crop_harvest, block_break routing</li>
   *   <li>SimpleInstantInteractionGate: interaction_log (ChangeState etc.)</li>
   *   <li>BlockPlaceInterceptor: block_place</li>
   *   <li>EntityDamageInterceptor: entity_damage</li>
   *   <li>RespawnInterceptor: respawn</li>
   * </ul>
   */
  @SuppressWarnings("unchecked")
  public static void registerUniqueHooks(@NotNull HyperFactions hf) {
    Object bridgeObj = System.getProperties().get("hyperprotect.bridge");
    if (bridgeObj == null) {
      bridgeObj = new AtomicReferenceArray<Object>(30);
      Object existing = System.getProperties().putIfAbsent("hyperprotect.bridge", bridgeObj);
      if (existing != null) {
        bridgeObj = existing;
      }
      System.setProperty("hyperprotect.bridge.active", "true");
      System.setProperty("hyperprotect.bridge.version", detectVersion());
    }
    if (!(bridgeObj instanceof AtomicReferenceArray<?> rawBridge)) {
      Logger.warn("HyperProtect bridge has unexpected type — cannot register unique hooks");
      return;
    }

    AtomicReferenceArray<Object> bridge = (AtomicReferenceArray<Object>) rawBridge;
    ProtectionChecker checker = hf.getProtectionChecker();

    // SimpleBlockInteractionGate routes BreakBlockInteraction through slot 0
    bridge.set(SLOT_BLOCK_BREAK, new BlockBreakHook(checker));

    // Unique HP slots — features OG does NOT cover
    bridge.set(SLOT_TELEPORTER, new TeleporterHook(checker));
    bridge.set(SLOT_PORTAL, new PortalHook(checker));
    bridge.set(SLOT_INTERACTION_LOG, new InteractionLogHook());
    bridge.set(SLOT_ENTITY_DAMAGE, new EntityDamageHook(checker));
    bridge.set(SLOT_CONTAINER_OPEN, new ContainerOpenHook(checker));
    bridge.set(SLOT_BLOCK_PLACE, new BlockPlaceHook(checker));
    bridge.set(SLOT_HAMMER, new HammerHook(checker));
    bridge.set(SLOT_USE, new UseHook(checker));
    bridge.set(SLOT_SEAT, new SeatHook(checker));
    bridge.set(SLOT_RESPAWN, new RespawnHook(hf));

    // v1.2.0 unique hooks — OG has no equivalents, always register in BOTH mode
    safeSetSlot(bridge, SLOT_CRAFTING_RESOURCE, new CraftingResourceHook(checker));
    safeSetSlot(bridge, SLOT_MAP_MARKER_FILTER, new MapMarkerFilterHook(hf));
    safeSetSlot(bridge, SLOT_FLUID_SPREAD, new FluidSpreadHook(checker));
    safeSetSlot(bridge, SLOT_PREFAB_SPAWN, new PrefabSpawnHook(checker));
    safeSetSlot(bridge, SLOT_PROJECTILE_LAUNCH, new ProjectileLaunchHook(checker));
    safeSetSlot(bridge, SLOT_MOUNT, new MountHook(checker));
    safeSetSlot(bridge, SLOT_BARTER_TRADE, new BarterTradeHook(checker));

    // Format handle needed for deny messages from the unique mixins
    try {
      MethodHandle fmtHandle = MethodHandles.publicLookup().findStatic(
          HyperProtectIntegration.class, "formatDenyMessage",
          MethodType.methodType(Message.class, String.class));
      bridge.set(SLOT_FORMAT_HANDLE, fmtHandle);
    } catch (Exception e) {
      ErrorHandler.report("Failed to register format handle at slot 15", e);
    }

    Logger.debug("Registered HP unique hooks (slots 0,9-12,15-29) for BOTH mode");
  }

  /**
   * Unregisters only the unique HP hooks (used in BOTH mode shutdown).
   */
  @SuppressWarnings("unchecked")
  public static void unregisterUniqueHooks() {
    Object bridgeObj = System.getProperties().get("hyperprotect.bridge");
    if (!(bridgeObj instanceof AtomicReferenceArray<?> rawBridge)) {
      return;
    }

    AtomicReferenceArray<Object> bridge = (AtomicReferenceArray<Object>) rawBridge;
    int[] uniqueSlots = {
      SLOT_BLOCK_BREAK, SLOT_TELEPORTER, SLOT_PORTAL, SLOT_INTERACTION_LOG,
      SLOT_FORMAT_HANDLE, SLOT_ENTITY_DAMAGE, SLOT_CONTAINER_OPEN, SLOT_BLOCK_PLACE,
      SLOT_HAMMER, SLOT_USE, SLOT_SEAT, SLOT_RESPAWN
    };
    for (int slot : uniqueSlots) {
      bridge.set(slot, null);
    }
    // v1.2.0 slots
    int[] newSlots = {
      SLOT_CRAFTING_RESOURCE, SLOT_MAP_MARKER_FILTER, SLOT_FLUID_SPREAD,
      SLOT_PREFAB_SPAWN, SLOT_PROJECTILE_LAUNCH, SLOT_MOUNT, SLOT_BARTER_TRADE
    };
    for (int slot : newSlots) {
      safeClearSlot(bridge, slot);
    }
    Logger.debugMixin("Unregistered HP unique hooks (BOTH mode)");
  }

  private static boolean isFeatureAvailable(String featureName) {
    return "true".equalsIgnoreCase(System.getProperty("hyperprotect.intercept." + featureName));
  }

  /**
   * Formats a raw deny reason string into a styled Message.
   * Called by mixin interceptors via MethodHandle at bridge slot 15.
   */
  public static Message formatDenyMessage(String raw) {
    return Message.raw(raw).color("#FF5555");
  }

  // ========================================================================
  // Hook Wrapper Classes
  // Method names MUST match what HyperProtect interceptors resolve via MethodHandle
  // ========================================================================

  // --- ThreadLocal for caching deny reasons between evaluate() and fetch() calls ---
  private static final ThreadLocal<String> cachedReason = new ThreadLocal<>();

  /** Slot 0: block_break — evaluate(UUID, String, int, int, int). */
  public static final class BlockBreakHook {
    private final ProtectionChecker checker;

    BlockBreakHook(ProtectionChecker checker) {
      this.checker = checker;
    }

    /** Evaluate. */
    public int evaluate(UUID playerUuid, String worldName, int x, int y, int z) {
      try {
        String interaction = getInteractionName();
        String reason = checker.checkBuild(playerUuid, worldName, x, y, z);
        int verdict = reason == null ? ALLOW : DENY_WITH_MESSAGE;
        Logger.debugInteraction("[Mixin:BlockBreak] player=%s, world=%s, pos=(%d,%d,%d), interaction=%s, verdict=%s",
          playerUuid, worldName, x, y, z, interaction, verdict == ALLOW ? "ALLOW" : "DENY");
        if (reason != null) {
          cachedReason.set(reason);
        }
        return verdict;
      } catch (Exception e) {
        ErrorHandler.report("Block break mixin hook error (fail-closed)", e);
        cachedReason.set("Protection error — action blocked for safety.");
        return DENY_WITH_MESSAGE;
      }
    }

    /** Fetch Deny Reason. */
    public String fetchDenyReason(UUID playerUuid, String worldName, int x, int y, int z) {
      String reason = cachedReason.get();
      cachedReason.remove();
      return reason;
    }
  }

  /** Slot 1: explosion — evaluateExplosion(World, int, int, int). */
  public static final class ExplosionHook {
    private final ProtectionChecker checker;

    ExplosionHook(ProtectionChecker checker) {
      this.checker = checker;
    }

    /** Evaluate Explosion. */
    public int evaluateExplosion(Object world, int x, int y, int z) {
      try {
        String worldName = "";
        if (world != null) {
          worldName = world.getClass().getMethod("getName").invoke(world).toString();
        }
        boolean blocked = checker.shouldBlockExplosion(worldName, x, y, z);
        Logger.debugInteraction("[Mixin:Explosion] world=%s, pos=(%d,%d,%d), blocked=%b", worldName, x, y, z, blocked);
        return blocked ? DENY_SILENT : ALLOW;
      } catch (Exception e) {
        ErrorHandler.report("Explosion mixin hook error (fail-closed)", e);
        return DENY_SILENT;
      }
    }
  }

  /** Slot 2: fire_spread — evaluateFlame(String, int, int, int). */
  public static final class FireSpreadHook {
    private final ProtectionChecker checker;

    FireSpreadHook(ProtectionChecker checker) {
      this.checker = checker;
    }

    /** Evaluate Flame. */
    public int evaluateFlame(String worldName, int x, int y, int z) {
      boolean blocked = checker.shouldBlockFireSpread(worldName, x, y, z);
      Logger.debugInteraction("[Mixin:FireSpread] world=%s, pos=(%d,%d,%d), blocked=%b", worldName, x, y, z, blocked);
      return blocked ? DENY_SILENT : ALLOW;
    }
  }

  /** Slot 3: builder_tools — evaluatePaste(UUID, String, int, int, int). */
  public static final class BuilderToolsHook {
    private final ProtectionChecker checker;

    BuilderToolsHook(ProtectionChecker checker) {
      this.checker = checker;
    }

    /** Evaluate Paste. */
    public int evaluatePaste(UUID playerUuid, String worldName, int x, int y, int z) {
      try {
        String interaction = getInteractionName();
        String reason = checker.checkBuilderTool(playerUuid, worldName, x, y, z);
        int verdict = reason == null ? ALLOW : DENY_WITH_MESSAGE;
        Logger.debugInteraction("[Mixin:BuilderTools] player=%s, world=%s, pos=(%d,%d,%d), interaction=%s, verdict=%s",
          playerUuid, worldName, x, y, z, interaction, verdict == ALLOW ? "ALLOW" : "DENY");
        if (reason != null) {
          cachedReason.set(reason);
        }
        return verdict;
      } catch (Exception e) {
        ErrorHandler.report("Builder tools mixin hook error (fail-closed)", e);
        cachedReason.set("Protection error — action blocked for safety.");
        return DENY_WITH_MESSAGE;
      }
    }

    /** Fetch Paste Deny Reason. */
    public String fetchPasteDenyReason(UUID playerUuid, String worldName, int x, int y, int z) {
      String reason = cachedReason.get();
      cachedReason.remove();
      return reason;
    }
  }

  /** Slot 4: item_pickup — evaluate(UUID, String, double, double, double). */
  public static final class ItemPickupHook {
    private final ProtectionChecker checker;

    ItemPickupHook(ProtectionChecker checker) {
      this.checker = checker;
    }

    public int evaluate(UUID playerUuid, String worldName, double x, double y, double z) {
      boolean allowed = checker.canPickupItem(playerUuid, worldName, x, y, z);
      Logger.debugInteraction("[Mixin:ItemPickup] player=%s, world=%s, pos=(%.1f,%.1f,%.1f), allowed=%b",
        playerUuid, worldName, x, y, z, allowed);
      return allowed ? ALLOW : DENY_SILENT;
    }
  }

  /** Slot 5: death_drop — evaluateDeathLoot(UUID, String, int, int, int). */
  public static final class DeathDropHook {
    private final ProtectionChecker checker;

    DeathDropHook(ProtectionChecker checker) {
      this.checker = checker;
    }

    public int evaluateDeathLoot(UUID playerUuid, String worldName, int x, int y, int z) {
      boolean keepInventory = checker.shouldKeepInventory(playerUuid, worldName, x, y, z);
      Logger.debugInteraction("[Mixin:DeathDrop] player=%s, world=%s, pos=(%d,%d,%d), keepInventory=%b",
        playerUuid, worldName, x, y, z, keepInventory);
      return keepInventory ? DENY_SILENT : ALLOW;
    }
  }

  /** Slot 6: durability — evaluateWear(UUID, String, int, int, int). */
  public static final class DurabilityHook {
    private final ProtectionChecker checker;

    DurabilityHook(ProtectionChecker checker) {
      this.checker = checker;
    }

    public int evaluateWear(UUID playerUuid, String worldName, int x, int y, int z) {
      boolean prevent = checker.shouldPreventDurability(playerUuid, worldName, x, y, z);
      Logger.debugInteraction("[Mixin:Durability] player=%s, world=%s, pos=(%d,%d,%d), preventWear=%b",
        playerUuid, worldName, x, y, z, prevent);
      return prevent ? DENY_SILENT : ALLOW;
    }
  }

  /** Slot 7: container_access — evaluateCrafting(UUID, String, int, int, int). */
  public static final class ContainerAccessHook {
    private final ProtectionChecker checker;

    ContainerAccessHook(ProtectionChecker checker) {
      this.checker = checker;
    }

    public int evaluateCrafting(UUID playerUuid, String worldName, int x, int y, int z) {
      try {
        String interaction = getInteractionName();
        String reason = checker.checkBench(playerUuid, worldName, x, y, z);
        int verdict = reason == null ? ALLOW : DENY_WITH_MESSAGE;
        Logger.debugInteraction("[Mixin:ContainerAccess] player=%s, world=%s, pos=(%d,%d,%d), interaction=%s, verdict=%s",
          playerUuid, worldName, x, y, z, interaction, verdict == ALLOW ? "ALLOW" : "DENY");
        if (reason != null) {
          cachedReason.set(reason);
        }
        return verdict;
      } catch (Exception e) {
        ErrorHandler.report("Container access mixin hook error (fail-closed)", e);
        cachedReason.set("Protection error — action blocked for safety.");
        return DENY_WITH_MESSAGE;
      }
    }

    /** Fetch Crafting Deny Reason. */
    public String fetchCraftingDenyReason(UUID playerUuid, String worldName, int x, int y, int z) {
      String reason = cachedReason.get();
      cachedReason.remove();
      return reason;
    }
  }

  /** Slot 8: mob_spawn — evaluateCreatureSpawn(String, int, int, int). */
  public static final class MobSpawnHook {
    private final ProtectionChecker checker;

    MobSpawnHook(ProtectionChecker checker) {
      this.checker = checker;
    }

    /** Evaluate Creature Spawn (position-only, called by HP 1.1.x). */
    public int evaluateCreatureSpawn(String worldName, int x, int y, int z) {
      boolean blocked = checker.shouldBlockSpawn(worldName, x, y, z);
      Logger.debugSpawning("[Mixin:MobSpawn] world=%s, pos=(%d,%d,%d), blocked=%b", worldName, x, y, z, blocked);
      return blocked ? DENY_SILENT : ALLOW;
    }

    /** Enhanced: evaluate with mob type (called by HP 1.2.0+). */
    public int evaluateCreatureSpawnTyped(String worldName, String npcType, int x, int y, int z) {
      boolean blocked = checker.shouldBlockSpawn(worldName, npcType, x, y, z);
      Logger.debugSpawning("[Mixin:MobSpawn] world=%s, type=%s, pos=(%d,%d,%d), blocked=%b",
        worldName, npcType, x, y, z, blocked);
      return blocked ? DENY_SILENT : ALLOW;
    }
  }

  /** Slot 9: teleporter — evaluateTeleporter(UUID, String, int, int, int). */
  public static final class TeleporterHook {
    private final ProtectionChecker checker;

    TeleporterHook(ProtectionChecker checker) {
      this.checker = checker;
    }

    /** Evaluate Teleporter. */
    public int evaluateTeleporter(UUID playerUuid, String worldName, int x, int y, int z) {
      try {
        String interaction = getInteractionName();
        String reason = checker.checkTeleporter(playerUuid, worldName, x, y, z);
        int verdict = reason == null ? ALLOW : DENY_WITH_MESSAGE;
        Logger.debugInteraction("[Mixin:Teleporter] player=%s, world=%s, pos=(%d,%d,%d), interaction=%s, verdict=%s",
          playerUuid, worldName, x, y, z, interaction, verdict == ALLOW ? "ALLOW" : "DENY");
        if (reason != null) {
          cachedReason.set(reason);
        }
        return verdict;
      } catch (Exception e) {
        ErrorHandler.report("Teleporter mixin hook error (fail-closed)", e);
        cachedReason.set("Protection error — action blocked for safety.");
        return DENY_WITH_MESSAGE;
      }
    }

    /** Fetch Teleporter Deny Reason. */
    public String fetchTeleporterDenyReason(UUID playerUuid, String worldName, int x, int y, int z) {
      String reason = cachedReason.get();
      cachedReason.remove();
      return reason;
    }
  }

  /** Slot 10: portal — evaluateGateway(UUID, String, int, int, int). */
  public static final class PortalHook {
    private final ProtectionChecker checker;

    PortalHook(ProtectionChecker checker) {
      this.checker = checker;
    }

    /** Evaluate Gateway. */
    public int evaluateGateway(UUID playerUuid, String worldName, int x, int y, int z) {
      try {
        String interaction = getInteractionName();
        String reason = checker.checkPortal(playerUuid, worldName, x, y, z);
        int verdict = reason == null ? ALLOW : DENY_WITH_MESSAGE;
        Logger.debugInteraction("[Mixin:Portal] player=%s, world=%s, pos=(%d,%d,%d), interaction=%s, verdict=%s",
          playerUuid, worldName, x, y, z, interaction, verdict == ALLOW ? "ALLOW" : "DENY");
        if (reason != null) {
          cachedReason.set(reason);
        }
        return verdict;
      } catch (Exception e) {
        ErrorHandler.report("Portal mixin hook error (fail-closed)", e);
        cachedReason.set("Protection error — action blocked for safety.");
        return DENY_WITH_MESSAGE;
      }
    }

    /** Fetch Gateway Deny Reason. */
    public String fetchGatewayDenyReason(UUID playerUuid, String worldName, int x, int y, int z) {
      String reason = cachedReason.get();
      cachedReason.remove();
      return reason;
    }
  }

  /** Slot 11: command — evaluateCommand(Player, String). */
  public static final class CommandHook {
    private final HyperFactions hf;

    CommandHook(HyperFactions hf) {
      this.hf = hf;
    }

    /** Evaluate Command. */
    public int evaluateCommand(Player player, String command) {
      if (player == null || command == null) {
        return ALLOW;
      }
      UUID uuid = player.getUuid();
      OrbisMixinsIntegration.CommandCheckResult result =
          hf.getProtectionChecker().checkCommandBlock(uuid, "", 0, 0, 0, command);
      Logger.debugInteraction("[Mixin:Command] player=%s, command=%s, verdict=%s",
        uuid, command, result.block() ? "DENY" : "ALLOW");
      if (!result.block()) {
        return ALLOW;
      }
      cachedReason.set(result.denialMessage());
      return DENY_WITH_MESSAGE;
    }

    /** Fetch Command Deny Reason. */
    public String fetchCommandDenyReason(Player player, String command) {
      String reason = cachedReason.get();
      cachedReason.remove();
      return reason != null ? reason : "You cannot use that command here.";
    }
  }

  /** Slot 12: interaction_log — isLogFiltered(). */
  public static final class InteractionLogHook {
    /** Checks if log filtered. */
    public boolean isLogFiltered() {
      return false; // Don't suppress any logs
    }
  }

  /** Slot 16: entity_damage — evaluateEntityDamage(UUID, UUID, String, int, int, int). */
  public static final class EntityDamageHook {
    private final ProtectionChecker checker;

    EntityDamageHook(ProtectionChecker checker) {
      this.checker = checker;
    }

    /** Evaluate Entity Damage. */
    public int evaluateEntityDamage(UUID attackerUuid, UUID targetUuid, String worldName,
                    int x, int y, int z) {
      try {
        // Both null means mob-on-mob — not our concern
        if (attackerUuid == null && targetUuid == null) {
          return ALLOW;
        }
        String reason = checker.checkEntityDamage(attackerUuid, targetUuid, worldName, x, y, z);
        int verdict = reason == null ? ALLOW : DENY_WITH_MESSAGE;
        Logger.debugInteraction("[Mixin:EntityDamage] attacker=%s, target=%s, world=%s, pos=(%d,%d,%d), verdict=%s",
          attackerUuid, targetUuid, worldName, x, y, z, verdict == ALLOW ? "ALLOW" : "DENY");
        if (reason != null) {
          cachedReason.set(reason);
        }
        return verdict;
      } catch (Exception e) {
        ErrorHandler.report("Entity damage mixin hook error (fail-closed)", e);
        cachedReason.set("Protection error — action blocked for safety.");
        return DENY_WITH_MESSAGE;
      }
    }

    /** Fetch Entity Damage Deny Reason. */
    public String fetchEntityDamageDenyReason(UUID attackerUuid, UUID targetUuid, String worldName,
                          int x, int y, int z) {
      String reason = cachedReason.get();
      cachedReason.remove();
      return reason;
    }
  }

  /** Slot 17: container_open — evaluateContainerOpen(UUID, String, int, int, int). */
  public static final class ContainerOpenHook {
    private final ProtectionChecker checker;

    ContainerOpenHook(ProtectionChecker checker) {
      this.checker = checker;
    }

    /** Evaluate Container Open. */
    public int evaluateContainerOpen(UUID playerUuid, String worldName, int x, int y, int z) {
      try {
        String interaction = getInteractionName();
        String reason = checker.checkContainer(playerUuid, worldName, x, y, z);
        int verdict = reason == null ? ALLOW : DENY_WITH_MESSAGE;
        Logger.debugInteraction("[Mixin:ContainerOpen] player=%s, world=%s, pos=(%d,%d,%d), interaction=%s, verdict=%s",
          playerUuid, worldName, x, y, z, interaction, verdict == ALLOW ? "ALLOW" : "DENY");
        if (reason != null) {
          cachedReason.set(reason);
        }
        return verdict;
      } catch (Exception e) {
        ErrorHandler.report("Container open mixin hook error (fail-closed)", e);
        cachedReason.set("Protection error — action blocked for safety.");
        return DENY_WITH_MESSAGE;
      }
    }

    public String fetchContainerOpenDenyReason(UUID playerUuid, String worldName, int x, int y, int z) {
      String reason = cachedReason.get();
      cachedReason.remove();
      return reason;
    }
  }

  /** Slot 18: block_place — evaluateBlockPlace(UUID, String, int, int, int). */
  public static final class BlockPlaceHook {
    private final ProtectionChecker checker;

    BlockPlaceHook(ProtectionChecker checker) {
      this.checker = checker;
    }

    /** Evaluate Block Place. */
    public int evaluateBlockPlace(UUID playerUuid, String worldName, int x, int y, int z) {
      try {
        String interaction = getInteractionName();
        String reason = checker.checkPlace(playerUuid, worldName, x, y, z);
        int verdict = reason == null ? ALLOW : DENY_WITH_MESSAGE;
        Logger.debugInteraction("[Mixin:BlockPlace] player=%s, world=%s, pos=(%d,%d,%d), interaction=%s, verdict=%s",
          playerUuid, worldName, x, y, z, interaction, verdict == ALLOW ? "ALLOW" : "DENY");
        if (reason != null) {
          cachedReason.set(reason);
        }
        return verdict;
      } catch (Exception e) {
        ErrorHandler.report("Block place mixin hook error (fail-closed)", e);
        cachedReason.set("Protection error — action blocked for safety.");
        return DENY_WITH_MESSAGE;
      }
    }

    /** Fetch Block Place Deny Reason. */
    public String fetchBlockPlaceDenyReason(UUID playerUuid, String worldName, int x, int y, int z) {
      String reason = cachedReason.get();
      cachedReason.remove();
      return reason;
    }
  }

  /** Slot 19: hammer — evaluateHammer(UUID, String, int, int, int). */
  public static final class HammerHook {
    private final ProtectionChecker checker;

    HammerHook(ProtectionChecker checker) {
      this.checker = checker;
    }

    /** Evaluate Hammer. */
    public int evaluateHammer(UUID playerUuid, String worldName, int x, int y, int z) {
      try {
        String interaction = getInteractionName();
        String reason = checker.checkHammer(playerUuid, worldName, x, y, z);
        int verdict = reason == null ? ALLOW : DENY_WITH_MESSAGE;
        Logger.debugInteraction("[Mixin:Hammer] player=%s, world=%s, pos=(%d,%d,%d), interaction=%s, verdict=%s",
          playerUuid, worldName, x, y, z, interaction, verdict == ALLOW ? "ALLOW" : "DENY");
        if (reason != null) {
          cachedReason.set(reason);
        }
        return verdict;
      } catch (Exception e) {
        ErrorHandler.report("Hammer mixin hook error (fail-closed)", e);
        cachedReason.set("Protection error — action blocked for safety.");
        return DENY_WITH_MESSAGE;
      }
    }

    /** Fetch Hammer Deny Reason. */
    public String fetchHammerDenyReason(UUID playerUuid, String worldName, int x, int y, int z) {
      String reason = cachedReason.get();
      cachedReason.remove();
      return reason;
    }
  }

  /** Slot 20: use — evaluateUse(UUID, String, int, int, int). */
  public static final class UseHook {
    private final ProtectionChecker checker;

    UseHook(ProtectionChecker checker) {
      this.checker = checker;
    }

    /** Evaluate Use. */
    public int evaluateUse(UUID playerUuid, String worldName, int x, int y, int z) {
      try {
        String interaction = getInteractionName();

        // Consume block context (clears system properties to prevent stale context)
        String blockId = consumeBlockId();
        String blockState = consumeBlockState();

        // Determine specific interaction type from mixin context
        ProtectionChecker.InteractionType interactionType;
        String npcRole = null;
        if (interaction.contains("entity-capture")) {
          interactionType = ProtectionChecker.InteractionType.CRATE_PICKUP;
        } else if (interaction.equals("UseCaptureCrateInteraction")) {
          interactionType = ProtectionChecker.InteractionType.CRATE_PLACE;
        } else if (interaction.equals("UseNPCInteraction") || interaction.equals("ContextualUseNPCInteraction")) {
          // Consume NPC role (clears system property to prevent stale context)
          npcRole = consumeNpcRole();
          if (npcRole != null && isMountableCreatureRole(npcRole)) {
            // Tamed rideable creatures (horses, donkeys, camels) → MOUNT (uses mount_use flag)
            interactionType = ProtectionChecker.InteractionType.MOUNT;
          } else if (npcRole != null && isTameableCreatureRole(npcRole)) {
            // Known tameable creatures (animals, pets) → NPC_TAME (restricted)
            interactionType = ProtectionChecker.InteractionType.NPC_TAME;
          } else {
            // Service NPCs, 3rd-party mod NPCs, and unknown roles → NPC_INTERACT (permissive)
            interactionType = ProtectionChecker.InteractionType.NPC_INTERACT;
          }
        } else if (isLightBlock(blockId)) {
          interactionType = ProtectionChecker.InteractionType.LIGHT;
        } else {
          interactionType = ProtectionChecker.InteractionType.INTERACT;
        }

        String reason = checker.checkUse(playerUuid, worldName, x, y, z, interactionType);
        int verdict = reason == null ? ALLOW : DENY_WITH_MESSAGE;
        Logger.debugInteraction("[Mixin:Use] player=%s, world=%s, pos=(%d,%d,%d), interaction=%s, type=%s, blockId=%s, blockState=%s, npcRole=%s, verdict=%s",
          playerUuid, worldName, x, y, z, interaction, interactionType,
          blockId != null ? blockId : "n/a",
          blockState != null ? blockState : "n/a",
          npcRole != null ? npcRole : "n/a", verdict == ALLOW ? "ALLOW" : "DENY");
        if (reason != null) {
          cachedReason.set(reason);
        }
        return verdict;
      } catch (Exception e) {
        ErrorHandler.report("Use mixin hook error (fail-closed)", e);
        cachedReason.set("Protection error — action blocked for safety.");
        return DENY_WITH_MESSAGE;
      }
    }

    /** Fetch Use Deny Reason. */
    public String fetchUseDenyReason(UUID playerUuid, String worldName, int x, int y, int z) {
      String reason = cachedReason.get();
      cachedReason.remove();
      return reason;
    }
  }

  /** Slot 21: seat — evaluateSeat(UUID, String, int, int, int). */
  public static final class SeatHook {
    private final ProtectionChecker checker;

    SeatHook(ProtectionChecker checker) {
      this.checker = checker;
    }

    /** Evaluate Seat. */
    public int evaluateSeat(UUID playerUuid, String worldName, int x, int y, int z) {
      try {
        String interaction = getInteractionName();
        String reason = checker.checkSeat(playerUuid, worldName, x, y, z);
        int verdict = reason == null ? ALLOW : DENY_WITH_MESSAGE;
        Logger.debugInteraction("[Mixin:Seat] player=%s, world=%s, pos=(%d,%d,%d), interaction=%s, verdict=%s",
          playerUuid, worldName, x, y, z, interaction, verdict == ALLOW ? "ALLOW" : "DENY");
        if (reason != null) {
          cachedReason.set(reason);
        }
        return verdict;
      } catch (Exception e) {
        ErrorHandler.report("Seat mixin hook error (fail-closed)", e);
        cachedReason.set("Protection error — action blocked for safety.");
        return DENY_WITH_MESSAGE;
      }
    }

    public String fetchSeatDenyReason(UUID playerUuid, String worldName, int x, int y, int z) {
      String reason = cachedReason.get();
      cachedReason.remove();
      return reason;
    }
  }

  /** Slot 22: respawn — evaluateRespawn(UUID, String, int, int, int) -> double[] or null. */
  public static final class RespawnHook {
    private final HyperFactions hf;

    RespawnHook(HyperFactions hf) {
      this.hf = hf;
    }

    /** Evaluate Respawn. */
    public double[] evaluateRespawn(UUID playerUuid, String worldName, int x, int y, int z) {
      double[] override = hf.getProtectionChecker().getRespawnOverride(playerUuid, worldName, x, y, z);
      Logger.debugInteraction("[Mixin:Respawn] player=%s, world=%s, pos=(%d,%d,%d), hasOverride=%b",
        playerUuid, worldName, x, y, z, override != null);
      return override;
    }
  }

  // ========================================================================
  // v1.2.0 Hook Wrapper Classes (slots 23-29)
  // ========================================================================

  /** Slot 23: crafting_resource — evaluateChestAccess(UUID, String, int,int,int, int,int,int). */
  public static final class CraftingResourceHook {
    private final ProtectionChecker checker;

    CraftingResourceHook(ProtectionChecker checker) {
      this.checker = checker;
    }

    /**
     * Checks if a player can access crafting bench resources at this position.
     * Returns true = allow, false = deny.
     */
    public boolean evaluateChestAccess(UUID playerUuid, String worldName,
        int chestX, int chestY, int chestZ,
        int benchX, int benchY, int benchZ) {
      try {
        // Check if the player can access the bench area
        String reason = checker.checkBench(playerUuid, worldName, benchX, benchY, benchZ);
        boolean allowed = reason == null;
        Logger.debugInteraction("[Mixin:CraftingResource] player=%s, world=%s, bench=(%d,%d,%d), allowed=%b",
          playerUuid, worldName, benchX, benchY, benchZ, allowed);
        return allowed;
      } catch (Exception e) {
        ErrorHandler.report("Crafting resource mixin hook error (fail-open)", e);
        return true; // Fail-open: allow crafting
      }
    }
  }

  /**
   * Slot 24: map_marker_filter — player icon and shared marker filtering.
   *
   * <p>Two methods on the same hook object:
   * <ul>
   *   <li>{@code filterPlayerMarker(UUID, UUID, String, int, int, int)} — called by MapMarkerFilter mixin
   *       for other-player icons on the world map</li>
   *   <li>{@code filterSharedMarker(UUID, UUID, String, float, float)} — called by SharedMarkerFilter mixin
   *       for user-placed shared markers on the world map</li>
   * </ul>
   */
  public static final class MapMarkerFilterHook {
    private final HyperFactions hf;

    MapMarkerFilterHook(HyperFactions hf) {
      this.hf = hf;
    }

    /**
     * Filters player markers on the world map based on faction relationships.
     * Returns 0 = SHOW, >0 = HIDE.
     */
    public int filterPlayerMarker(UUID viewer, UUID target, String worldName,
        int x, int y, int z) {
      try {
        ProtectionChecker checker = hf.getProtectionChecker();
        boolean hidden = checker.shouldHideMapMarker(viewer, target, worldName, x, z);
        // Logger.debugInteraction("[Mixin:MapMarker] viewer=%s, target=%s, world=%s, pos=(%d,%d,%d), hidden=%b",
        //   viewer, target, worldName, x, y, z, hidden);
        return hidden ? 1 : 0;
      } catch (Exception e) {
        ErrorHandler.report("Map marker filter mixin hook error (fail-open)", e);
        return 0; // Fail-open: show marker
      }
    }

    /**
     * Filters shared (user-placed) markers on the world map based on faction relationships.
     * Called by SharedMarkerFilter mixin for each UserMapMarker.
     * Returns 0 = SHOW, >0 = HIDE.
     */
    public int filterSharedMarker(UUID viewer, UUID creator, String worldName,
        float x, float z) {
      try {
        ProtectionChecker checker = hf.getProtectionChecker();
        boolean hidden = checker.shouldHideSharedMarker(viewer, creator, worldName, x, z);
        // Logger.debugInteraction("[Mixin:SharedMarker] viewer=%s, creator=%s, world=%s, pos=(%.0f,%.0f), hidden=%b",
        //   viewer, creator, worldName, x, z, hidden);
        return hidden ? 1 : 0;
      } catch (Exception e) {
        ErrorHandler.report("Shared marker filter mixin hook error (fail-open)", e);
        return 0; // Fail-open: show marker
      }
    }
  }

  /** Slot 25: fluid_spread — evaluateFluidSpread(String, int, int, int). */
  public static final class FluidSpreadHook {
    private final ProtectionChecker checker;

    FluidSpreadHook(ProtectionChecker checker) {
      this.checker = checker;
    }

    /** Evaluate Fluid Spread (water/lava). */
    public int evaluateFluidSpread(String worldName, int x, int y, int z) {
      boolean blocked = checker.shouldBlockFluidSpread(worldName, x, y, z);
      Logger.debugInteraction("[Mixin:FluidSpread] world=%s, pos=(%d,%d,%d), blocked=%b",
        worldName, x, y, z, blocked);
      return blocked ? DENY_SILENT : ALLOW;
    }
  }

  /** Slot 26: prefab_spawn — evaluatePrefabSpawn(String, int, int, int). */
  public static final class PrefabSpawnHook {
    private final ProtectionChecker checker;

    PrefabSpawnHook(ProtectionChecker checker) {
      this.checker = checker;
    }

    /** Evaluate Prefab/NPC Entity Loading. */
    public int evaluatePrefabSpawn(String worldName, int x, int y, int z) {
      boolean blocked = checker.shouldBlockSpawn(worldName, x, y, z);
      Logger.debugSpawning("[Mixin:PrefabSpawn] world=%s, pos=(%d,%d,%d), blocked=%b",
        worldName, x, y, z, blocked);
      return blocked ? DENY_SILENT : ALLOW;
    }
  }

  /** Slot 27: projectile_launch — evaluateProjectileLaunch(UUID, String, int, int, int). */
  public static final class ProjectileLaunchHook {
    private final ProtectionChecker checker;

    ProjectileLaunchHook(ProtectionChecker checker) {
      this.checker = checker;
    }

    /** Evaluate Projectile Launch. */
    public int evaluateProjectileLaunch(UUID playerUuid, String worldName, int x, int y, int z) {
      try {
        String reason = checker.checkProjectileLaunch(playerUuid, worldName, x, y, z);
        int verdict = reason == null ? ALLOW : DENY_WITH_MESSAGE;
        Logger.debugInteraction("[Mixin:ProjectileLaunch] player=%s, world=%s, pos=(%d,%d,%d), verdict=%s",
          playerUuid, worldName, x, y, z, verdict == ALLOW ? "ALLOW" : "DENY");
        if (reason != null) {
          cachedReason.set(reason);
        }
        return verdict;
      } catch (Exception e) {
        ErrorHandler.report("Projectile launch mixin hook error (fail-open)", e);
        return ALLOW;
      }
    }

    /** Fetch Projectile Launch Deny Reason. */
    public String fetchProjectileLaunchDenyReason(UUID playerUuid, String worldName, int x, int y, int z) {
      String reason = cachedReason.get();
      cachedReason.remove();
      return reason;
    }
  }

  /** Slot 28: mount — evaluateMount(UUID, String, int, int, int). */
  public static final class MountHook {
    private final ProtectionChecker checker;

    MountHook(ProtectionChecker checker) {
      this.checker = checker;
    }

    /** Evaluate Mount. */
    public int evaluateMount(UUID playerUuid, String worldName, int x, int y, int z) {
      try {
        String reason = checker.checkMount(playerUuid, worldName, x, y, z);
        int verdict = reason == null ? ALLOW : DENY_WITH_MESSAGE;
        Logger.debugInteraction("[Mixin:Mount] player=%s, world=%s, pos=(%d,%d,%d), verdict=%s",
          playerUuid, worldName, x, y, z, verdict == ALLOW ? "ALLOW" : "DENY");
        if (reason != null) {
          cachedReason.set(reason);
        }
        return verdict;
      } catch (Exception e) {
        ErrorHandler.report("Mount mixin hook error (fail-closed)", e);
        cachedReason.set("Protection error — action blocked for safety.");
        return DENY_WITH_MESSAGE;
      }
    }

    /** Fetch Mount Deny Reason. */
    public String fetchMountDenyReason(UUID playerUuid, String worldName, int x, int y, int z) {
      String reason = cachedReason.get();
      cachedReason.remove();
      return reason;
    }
  }

  /** Slot 29: barter_trade — evaluateTrade(UUID, String, int, int, int). */
  public static final class BarterTradeHook {
    private final ProtectionChecker checker;

    BarterTradeHook(ProtectionChecker checker) {
      this.checker = checker;
    }

    /** Evaluate Trade. */
    public int evaluateTrade(UUID playerUuid, String worldName, int x, int y, int z) {
      try {
        String reason = checker.checkTrade(playerUuid, worldName, x, y, z);
        int verdict = reason == null ? ALLOW : DENY_WITH_MESSAGE;
        Logger.debugInteraction("[Mixin:BarterTrade] player=%s, world=%s, pos=(%d,%d,%d), verdict=%s",
          playerUuid, worldName, x, y, z, verdict == ALLOW ? "ALLOW" : "DENY");
        if (reason != null) {
          cachedReason.set(reason);
        }
        return verdict;
      } catch (Exception e) {
        ErrorHandler.report("Barter trade mixin hook error (fail-open)", e);
        return ALLOW;
      }
    }

    /** Fetch Trade Deny Reason. */
    public String fetchTradeDenyReason(UUID playerUuid, String worldName, int x, int y, int z) {
      String reason = cachedReason.get();
      cachedReason.remove();
      return reason;
    }
  }
}
