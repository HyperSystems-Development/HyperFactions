package com.hyperfactions.platform;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.chat.PublicChatListener;
import com.hyperfactions.listener.PlayerListener;
import com.hyperfactions.protection.NpcInteractionProtectionHandler;
import com.hyperfactions.protection.ProtectionListener;
import com.hyperfactions.protection.damage.DamageProtectionHandler;
import com.hyperfactions.protection.ecs.BlockBreakProtectionSystem;
import com.hyperfactions.protection.ecs.BlockPlaceProtectionSystem;
import com.hyperfactions.protection.ecs.BlockUseProtectionSystem;
import com.hyperfactions.protection.ecs.HarvestPickupProtectionSystem;
import com.hyperfactions.protection.ecs.ItemDropProtectionSystem;
import com.hyperfactions.protection.ecs.ItemPickupProtectionSystem;
import com.hyperfactions.protection.ecs.PlayerDeathSystem;
import com.hyperfactions.protection.ecs.PlayerRespawnSystem;
import com.hyperfactions.protection.ecs.PvPProtectionSystem;
import com.hyperfactions.protection.ecs.TeleportCancelOnDamageSystem;
import com.hyperfactions.territory.TerritoryTickingSystem;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.system.ISystem;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.world.events.AddWorldEvent;
import com.hypixel.hytale.server.core.universe.world.events.RemoveWorldEvent;


/**
 * Handles registration of all event listeners and ECS systems for HyperFactions.
 * Extracted from HyperFactionsPlugin to reduce class complexity.
 */
public class EventRegistration {

  /**
   * Result of registering all event listeners.
   * Contains the created listener instances that HyperFactionsPlugin needs to store.
   */
  public record RegistrationResult(
      PlayerListener playerListener,
      ProtectionListener protectionListener,
      PublicChatListener publicChatListener
  ) {}

  private final HyperFactionsPlugin plugin;

  private final HyperFactions hyperFactions;

  private TerritoryTickingSystem territoryTickingSystem;

  /** Creates a new EventRegistration. */
  public EventRegistration(HyperFactionsPlugin plugin, HyperFactions hyperFactions) {
    this.plugin = plugin;
    this.hyperFactions = hyperFactions;
  }

  /**
   * Registers all event listeners and returns the created listener instances.
   *
   * @param worldSetup          the world setup handler for world add/remove events
   * @param connectionHandler   the connection handler for player connect/disconnect/chat events
   * @return the created listener instances
   */
  public RegistrationResult registerAll(WorldSetup worldSetup, PlayerConnectionHandler connectionHandler) {
    // World add event - register world map provider for each world
    plugin.getEventRegistry().registerGlobal(AddWorldEvent.class, worldSetup::onWorldAdd);

    // World remove event - cleanup
    plugin.getEventRegistry().registerGlobal(RemoveWorldEvent.class, worldSetup::onWorldRemove);

    // Player connect event
    plugin.getEventRegistry().register(PlayerConnectEvent.class, connectionHandler::onPlayerConnect);

    // Player disconnect event
    plugin.getEventRegistry().register(PlayerDisconnectEvent.class, connectionHandler::onPlayerDisconnect);

    // Player chat event (for faction/ally chat channels)
    // Use async global handler since PlayerChatEvent is an async event
    plugin.getEventRegistry().registerAsyncGlobal(
      EventPriority.NORMAL,
      PlayerChatEvent.class,
      connectionHandler::onPlayerChatAsync
    );

    // Player ready event — apply map player filter (must be PlayerReadyEvent, not PlayerConnectEvent,
    // because the world map tracker is not initialized until the player is fully ready)
    plugin.getEventRegistry().registerGlobal(PlayerReadyEvent.class, event -> {
      Player player = event.getPlayer();
      if (player != null) {
        hyperFactions.getMapPlayerFilterService().applyFilter(player);
      }
    });

    // Public chat formatting (faction tags with relation colors)
    // Register at configured priority (default LATE) to run after LuckPerms
    PublicChatListener publicChatListener = new PublicChatListener(hyperFactions);
    EventPriority chatPriority = publicChatListener.getEventPriority();
    plugin.getEventRegistry().registerAsyncGlobal(
      chatPriority,
      PlayerChatEvent.class,
      publicChatListener::onPlayerChatAsync
    );
    Logger.debug("Registered public chat formatter at %s priority", chatPriority);

    // Create listeners
    PlayerListener playerListener = new PlayerListener(hyperFactions);
    ProtectionListener protectionListener = new ProtectionListener(hyperFactions);

    // Create and set damage protection handler (coordinates all damage protection systems)
    DamageProtectionHandler damageHandler = new DamageProtectionHandler(
      hyperFactions.getZoneDamageProtection(),
      hyperFactions.getProtectionChecker(),
      hyperFactions.getCombatTagManager(),
      protectionListener::getDenialMessage
    );
    hyperFactions.setDamageProtectionHandler(damageHandler);

    // NPC interaction protection (F-key tame, contextual NPC use)
    NpcInteractionProtectionHandler npcProtection = new NpcInteractionProtectionHandler(hyperFactions);
    plugin.getEventRegistry().registerGlobal(PlayerInteractEvent.class, npcProtection::onPlayerInteract);
    Logger.debug("Registered NPC interaction protection handler");

    // Register ECS event systems for block protection
    registerBlockProtectionSystems(protectionListener);

    // Register harvest pickup protection (InteractivelyPickupItemEvent - for block harvest drops)
    // Note: F-key entity pickup (PickupItemInteraction) cannot be intercepted - server API limitation
    registerHarvestPickupProtection(protectionListener);

    // Register update notification listener (if update checking is enabled)
    if (hyperFactions.getUpdateNotificationListener() != null) {
      hyperFactions.getUpdateNotificationListener().register(plugin.getEventRegistry());
      Logger.debug("Registered update notification listener");
    }

    return new RegistrationResult(playerListener, protectionListener, publicChatListener);
  }

  /**
   * Registers ECS event systems for block protection.
   */
  private void registerBlockProtectionSystems(ProtectionListener protectionListener) {
    try {
      // Block place protection
      plugin.getEntityStoreRegistry().registerSystem(new BlockPlaceProtectionSystem(hyperFactions, protectionListener));

      // Block break protection
      plugin.getEntityStoreRegistry().registerSystem(new BlockBreakProtectionSystem(hyperFactions, protectionListener));

      // Block use/interact protection
      plugin.getEntityStoreRegistry().registerSystem(new BlockUseProtectionSystem(hyperFactions, protectionListener));

      // Item pickup protection
      plugin.getEntityStoreRegistry().registerSystem(new ItemPickupProtectionSystem(hyperFactions, protectionListener));

      // Item drop protection
      plugin.getEntityStoreRegistry().registerSystem(new ItemDropProtectionSystem(hyperFactions, protectionListener));

      // PvP protection
      plugin.getEntityStoreRegistry().registerSystem(new PvPProtectionSystem(hyperFactions, protectionListener));

      // Player death and respawn systems (power loss, spawn protection)
      plugin.getEntityStoreRegistry().registerSystem(new PlayerDeathSystem(hyperFactions));
      plugin.getEntityStoreRegistry().registerSystem(new PlayerRespawnSystem(hyperFactions));

      Logger.debug("Registered block, item, and player ECS protection systems");
    } catch (Exception e) {
      ErrorHandler.report("Failed to register block protection systems", e);
    }
  }

  /**
   * Registers the harvest pickup protection system.
   * This handles block harvest drops (farming, mining, etc.) via InteractivelyPickupItemEvent.
   *
   * <p>NOTE: F-key entity pickup of items already on the ground (PickupItemInteraction)
   * cannot be intercepted as it does not fire any cancellable event. This is a
   * limitation of the Hytale server API.
   */
  private void registerHarvestPickupProtection(ProtectionListener protectionListener) {
    try {
      plugin.getEntityStoreRegistry().registerSystem(new HarvestPickupProtectionSystem(hyperFactions, protectionListener));
      Logger.debug("Registered harvest pickup ECS protection system");
    } catch (Exception e) {
      ErrorHandler.report("Failed to register harvest pickup protection system", e);
    }
  }

  /**
   * Registers ECS event systems for teleport management.
   */
  public void registerTeleportSystems() {
    try {
      // Cancel teleport on damage
      plugin.getEntityStoreRegistry().registerSystem(new TeleportCancelOnDamageSystem(hyperFactions));

      Logger.debug("Registered teleport cancel-on-damage ECS system");
    } catch (Exception e) {
      ErrorHandler.report("Failed to register teleport systems", e);
    }
  }

  /**
   * Registers ECS ticking systems for territory tracking.
   * The TerritoryTickingSystem reliably detects player chunk changes
   * each game tick, triggering territory notifications.
   */
  public void registerTerritorySystems() {
    try {
      // Territory chunk tracking (ticks every game tick for players)
      // Cast to ISystem as required by Hytale's registration
      territoryTickingSystem = new TerritoryTickingSystem(hyperFactions);
      plugin.getEntityStoreRegistry().registerSystem((ISystem) territoryTickingSystem);

      Logger.debug("Registered territory ticking ECS system");
    } catch (Exception e) {
      ErrorHandler.report("Failed to register territory ticking system", e);
    }
  }

  /**
   * Shuts down the territory ticking system.
   */
  public void shutdownTerritory() {
    if (territoryTickingSystem != null) {
      territoryTickingSystem.shutdown();
      territoryTickingSystem = null;
    }
  }
}
