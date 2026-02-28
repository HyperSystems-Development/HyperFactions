package com.hyperfactions.integration.protection;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.protection.ProtectionChecker;
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
   * Registers all applicable hooks with the HyperProtect bridge.
   */
  @SuppressWarnings("unchecked")
  public static void registerAllHooks(@NotNull HyperFactions hf) {
    Object bridgeObj = System.getProperties().get("hyperprotect.bridge");
    if (bridgeObj == null) {
      // Bridge not initialized (Hyxin doesn't instantiate early plugin Main class).
      // Create it lazily — interceptors read from it via System.getProperties().
      bridgeObj = new AtomicReferenceArray<Object>(24);
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

    // Register message formatter at slot 15 — converts raw String → Message for deny messages.
    // All mixin interceptors call getBridge(15) to format deny reason strings before sending.
    try {
      MethodHandle fmtHandle = MethodHandles.publicLookup().findStatic(
          HyperProtectIntegration.class, "formatDenyMessage",
          MethodType.methodType(Message.class, String.class));
      bridge.set(SLOT_FORMAT_HANDLE, fmtHandle);
    } catch (Exception e) {
      Logger.severe("Failed to register format handle at slot 15: %s", e.getMessage());
    }

    Logger.debug("Registered 20 HyperProtect hook(s) + format handle at bridge slots (unconditional)");
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
      bridgeObj = new AtomicReferenceArray<Object>(24);
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

    // Format handle needed for deny messages from the unique mixins
    try {
      MethodHandle fmtHandle = MethodHandles.publicLookup().findStatic(
          HyperProtectIntegration.class, "formatDenyMessage",
          MethodType.methodType(Message.class, String.class));
      bridge.set(SLOT_FORMAT_HANDLE, fmtHandle);
    } catch (Exception e) {
      Logger.severe("Failed to register format handle at slot 15: %s", e.getMessage());
    }

    Logger.debug("Registered HP unique hooks (slots 0,9-12,15-22) for BOTH mode");
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
        Logger.severe("Block break mixin hook error (fail-closed)", e);
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
        Logger.severe("Explosion mixin hook error (fail-closed)", e);
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
        Logger.severe("Builder tools mixin hook error (fail-closed)", e);
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
        Logger.severe("Container access mixin hook error (fail-closed)", e);
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

    /** Evaluate Creature Spawn. */
    public int evaluateCreatureSpawn(String worldName, int x, int y, int z) {
      boolean blocked = checker.shouldBlockSpawn(worldName, x, y, z);
      Logger.debugSpawning("[Mixin:MobSpawn] world=%s, pos=(%d,%d,%d), blocked=%b", worldName, x, y, z, blocked);
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
        Logger.severe("Teleporter mixin hook error (fail-closed)", e);
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
        Logger.severe("Portal mixin hook error (fail-closed)", e);
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
        Logger.severe("Entity damage mixin hook error (fail-closed)", e);
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
        Logger.severe("Container open mixin hook error (fail-closed)", e);
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
        Logger.severe("Block place mixin hook error (fail-closed)", e);
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
        Logger.severe("Hammer mixin hook error (fail-closed)", e);
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

        // Determine specific interaction type from mixin context
        ProtectionChecker.InteractionType interactionType;
        if (interaction.contains("entity-capture")) {
          interactionType = ProtectionChecker.InteractionType.CRATE_PICKUP;
        } else if (interaction.equals("UseCaptureCrateInteraction")) {
          interactionType = ProtectionChecker.InteractionType.CRATE_PLACE;
        } else if (interaction.equals("UseNPCInteraction") || interaction.equals("ContextualUseNPCInteraction")) {
          interactionType = ProtectionChecker.InteractionType.NPC_TAME;
        } else {
          interactionType = ProtectionChecker.InteractionType.INTERACT;
        }

        String reason = checker.checkUse(playerUuid, worldName, x, y, z, interactionType);
        int verdict = reason == null ? ALLOW : DENY_WITH_MESSAGE;
        Logger.debugInteraction("[Mixin:Use] player=%s, world=%s, pos=(%d,%d,%d), interaction=%s, type=%s, verdict=%s",
          playerUuid, worldName, x, y, z, interaction, interactionType, verdict == ALLOW ? "ALLOW" : "DENY");
        if (reason != null) {
          cachedReason.set(reason);
        }
        return verdict;
      } catch (Exception e) {
        Logger.severe("Use mixin hook error (fail-closed)", e);
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
        Logger.severe("Seat mixin hook error (fail-closed)", e);
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
}
