package com.hyperfactions.gui.faction.page;

import com.hyperfactions.data.*;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.faction.NavBarHelper;
import com.hyperfactions.gui.faction.data.FactionPageData;
import com.hyperfactions.gui.newplayer.NewPlayerNavBarHelper;
import com.hyperfactions.manager.*;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.util.UuidUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import java.util.UUID;

/**
 * Faction Dashboard page - displays faction stats and quick actions.
 * Uses the unified FactionPageData for event handling.
 */
public class FactionMainPage extends InteractiveCustomUIPage<FactionPageData> {

  private static final String PAGE_ID = "dashboard";

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final ClaimManager claimManager;

  private final PowerManager powerManager;

  private final TeleportManager teleportManager;

  private final InviteManager inviteManager;

  private final GuiManager guiManager;

  /** Creates a new FactionMainPage. */
  public FactionMainPage(PlayerRef playerRef,
             FactionManager factionManager,
             ClaimManager claimManager,
             PowerManager powerManager,
             TeleportManager teleportManager,
             InviteManager inviteManager,
             GuiManager guiManager) {
    super(playerRef, CustomPageLifetime.CanDismiss, FactionPageData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.claimManager = claimManager;
    this.powerManager = powerManager;
    this.teleportManager = teleportManager;
    this.inviteManager = inviteManager;
    this.guiManager = guiManager;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    UUID uuid = playerRef.getUuid();
    Faction faction = factionManager.getPlayerFaction(uuid);
    boolean hasFaction = faction != null;

    // Load the main template
    cmd.append(UIPaths.FACTION_MAIN);

    // Setup navigation bar - use new player nav when no faction
    if (hasFaction) {
      NavBarHelper.setupBar(playerRef, faction, PAGE_ID, cmd, events);
    } else {
      NewPlayerNavBarHelper.setupBar(playerRef, PAGE_ID, cmd, events);
    }

    // Check for pending invites
    buildInviteNotification(cmd, events, uuid);

    if (!hasFaction) {
      // Player has no faction - show "no faction" state
      buildNoFactionView(cmd, events);
    } else {
      // Player has a faction - show dashboard
      buildFactionDashboard(cmd, events, faction, uuid);
    }
  }

  private void buildInviteNotification(UICommandBuilder cmd, UIEventBuilder events, UUID uuid) {
    List<PendingInvite> invites = inviteManager.getPlayerInvites(uuid);
    PendingInvite invite = invites.isEmpty() ? null : invites.get(0);

    if (invite != null) {
      Faction invitingFaction = factionManager.getFaction(invite.factionId());
      if (invitingFaction != null) {
        cmd.append("#InviteNotification", UIPaths.INVITE_NOTIFICATION);
        cmd.set("#InviteNotification #InviteFactionName.Text", invitingFaction.name());

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#InviteNotification #AcceptInviteBtn",
            EventData.of("Button", "AcceptInvite")
                .append("FactionId", invite.factionId().toString()),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#InviteNotification #DeclineInviteBtn",
            EventData.of("Button", "DeclineInvite")
                .append("FactionId", invite.factionId().toString()),
            false
        );
      }
    }
  }

  private void buildNoFactionView(UICommandBuilder cmd, UIEventBuilder events) {
    cmd.set("#FactionName.Text", "No Faction");

    // Show create/browse buttons
    cmd.append("#ActionArea", UIPaths.NO_FACTION_ACTIONS);

    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#ActionArea #CreateFactionBtn",
        EventData.of("Button", "CreateFaction"),
        false
    );

    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#ActionArea #BrowseFactionsBtn",
        EventData.of("Button", "BrowseFactions"),
        false
    );
  }

  private void buildFactionDashboard(UICommandBuilder cmd, UIEventBuilder events,
                   Faction faction, UUID playerUuid) {
    FactionMember member = faction.getMember(playerUuid);
    FactionRole role = member != null ? member.role() : FactionRole.MEMBER;
    boolean isLeader = role == FactionRole.LEADER;
    boolean isOfficer = role.getLevel() >= FactionRole.OFFICER.getLevel();

    // Set faction name
    cmd.set("#FactionName.Text", faction.name());

    // Power stats
    PowerManager.FactionPowerStats powerStats = powerManager.getFactionPowerStats(faction.id());
    cmd.set("#PowerValue.Text", String.format("%.0f / %.0f", powerStats.currentPower(), powerStats.maxPower()));

    // Claim stats
    int claimCount = faction.claims().size();
    int claimCapacity = powerStats.maxClaims();
    cmd.set("#ClaimsValue.Text", String.format("%d / %d", claimCount, claimCapacity));

    // Member count
    cmd.set("#MembersValue.Text", String.valueOf(faction.members().size()));

    // Relation counts
    long allyCount = faction.relations().values().stream()
        .filter(r -> r.type() == RelationType.ALLY).count();
    long enemyCount = faction.relations().values().stream()
        .filter(r -> r.type() == RelationType.ENEMY).count();
    cmd.set("#AlliesValue.Text", String.valueOf(allyCount));
    cmd.set("#EnemiesValue.Text", String.valueOf(enemyCount));

    // Role display
    cmd.set("#RoleValue.Text", role.name());

    // Quick action buttons
    cmd.append("#ActionArea", UIPaths.FACTION_ACTIONS);

    // Home button
    if (faction.home() != null) {
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#ActionArea #HomeBtn",
          EventData.of("Button", "Home"),
          false
      );
    }

    // Leave button (for non-leaders)
    if (!isLeader) {
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#ActionArea #LeaveBtn",
          EventData.of("Button", "Leave"),
          false
      );
    }

    // Disband button (for leader only)
    if (isLeader) {
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#ActionArea #DisbandBtn",
          EventData.of("Button", "Disband"),
          false
      );
    }
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                FactionPageData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null || data.button == null) {
      sendUpdate();
      return;
    }

    UUID uuid = playerRef.getUuid();
    Faction faction = factionManager.getPlayerFaction(uuid);

    // Handle navigation - use new player nav when no faction
    if (faction != null) {
      if (NavBarHelper.handleNavEvent(data, player, ref, store, playerRef, faction, guiManager)) {
        return;
      }
    } else {
      if (NewPlayerNavBarHelper.handleNavEvent(data, player, ref, store, playerRef, guiManager)) {
        return;
      }
    }

    // Handle page-specific actions
    switch (data.button) {
      case "CreateFaction" -> {
        guiManager.openCreateFaction(player, ref, store, playerRef);
      }

      case "BrowseFactions" -> {
        guiManager.openFactionBrowser(player, ref, store, playerRef);
      }

      case "AcceptInvite" -> handleAcceptInvite(player, ref, store, playerRef, data, uuid);

      case "DeclineInvite" -> handleDeclineInvite(player, ref, store, playerRef, data, uuid);

      case "Home" -> handleHomeTeleport(player, ref, store, faction, uuid);

      case "Leave" -> handleLeave(player, ref, store, playerRef, faction, uuid);

      case "Disband" -> handleDisband(player, ref, store, faction, uuid);

      default -> sendUpdate();
    }
  }

  private void handleAcceptInvite(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                  PlayerRef playerRef, FactionPageData data, UUID uuid) {
    if (data.factionId == null) {
      return;
    }

    UUID factionId = UuidUtil.parseOrNull(data.factionId);
    if (factionId == null) {
      player.sendMessage(MessageUtil.errorText("Invalid faction ID."));
      return;
    }

    PendingInvite pendingInvite = inviteManager.getInvite(factionId, uuid);
    if (pendingInvite != null) {
      FactionManager.FactionResult result = factionManager.addMember(
          factionId, uuid, playerRef.getUsername()
      );
      if (result == FactionManager.FactionResult.SUCCESS) {
        inviteManager.removeInvite(factionId, uuid);
        player.sendMessage(MessageUtil.text("You joined the faction!", "#44CC44"));
        // Refresh the page
        guiManager.openFactionMain(player, ref, store, playerRef);
      } else {
        player.sendMessage(Message.raw("Failed to join faction: " + result).color("#FF5555"));
      }
    }
  }

  private void handleDeclineInvite(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                  PlayerRef playerRef, FactionPageData data, UUID uuid) {
    if (data.factionId == null) {
      return;
    }

    UUID factionId = UuidUtil.parseOrNull(data.factionId);
    if (factionId == null) {
      player.sendMessage(MessageUtil.errorText("Invalid faction ID."));
      return;
    }

    inviteManager.removeInvite(factionId, uuid);
    player.sendMessage(MessageUtil.text("Invite declined.", MessageUtil.COLOR_GOLD));
    // Refresh the page
    guiManager.openFactionMain(player, ref, store, playerRef);
  }

  private void handleHomeTeleport(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                  Faction faction, UUID uuid) {
    if (faction == null || faction.home() == null) {
      sendUpdate();
      return;
    }

    Faction.FactionHome home = faction.home();

    // Close the GUI first
    guiManager.closePage(player, ref, store);

    // Check cooldown
    if (teleportManager.isOnCooldown(uuid)) {
      int remaining = teleportManager.getCooldownRemaining(uuid);
      player.sendMessage(Message.raw("Teleport on cooldown! " + remaining + "s remaining.").color("#FF5555"));
      return;
    }

    // Get current world
    World currentWorld = player.getWorld();
    if (currentWorld == null) {
      player.sendMessage(MessageUtil.errorText("Cannot teleport - world not found."));
      return;
    }

    // Get target world
    World targetWorld;
    if (currentWorld.getName().equals(home.world())) {
      targetWorld = currentWorld;
    } else {
      targetWorld = Universe.get().getWorld(home.world());
      if (targetWorld == null) {
        player.sendMessage(MessageUtil.errorText("Cannot teleport - target world not found."));
        return;
      }
    }

    // Execute teleport on the target world's thread using createForPlayer for proper player teleportation
    targetWorld.execute(() -> {
      Vector3d position = new Vector3d(home.x(), home.y(), home.z());
      Vector3f rotation = new Vector3f(home.pitch(), home.yaw(), 0);
      Teleport teleport = Teleport.createForPlayer(targetWorld, position, rotation);
      store.addComponent(ref, Teleport.getComponentType(), teleport);
    });

    player.sendMessage(MessageUtil.text("Teleported to faction home!", "#44CC44"));
  }

  private void handleLeave(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
              PlayerRef playerRef, Faction faction, UUID uuid) {
    if (faction == null) {
      return;
    }

    FactionManager.FactionResult result = factionManager.removeMember(faction.id(), uuid, uuid, false);
    if (result == FactionManager.FactionResult.SUCCESS) {
      player.sendMessage(MessageUtil.text("You left the faction.", MessageUtil.COLOR_GOLD));
      guiManager.openFactionMain(player, ref, store, playerRef);
    } else {
      player.sendMessage(Message.raw("Failed to leave: " + result).color("#FF5555"));
    }
  }

  private void handleDisband(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
               Faction faction, UUID uuid) {
    if (faction == null) {
      return;
    }

    FactionMember member = faction.getMember(uuid);
    if (member != null && member.role() == FactionRole.LEADER) {
      guiManager.openDisbandConfirm(player, ref, store, playerRef, faction);
    }
  }
}
