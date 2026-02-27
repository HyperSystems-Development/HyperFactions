package com.hyperfactions.gui.faction.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.faction.data.LeaderLeaveConfirmData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

/**
 * Confirmation modal for when a leader wants to leave their faction.
 * Shows succession information - who will become the new leader.
 * If no successor is available, warns about faction disbanding.
 */
public class LeaderLeaveConfirmPage extends InteractiveCustomUIPage<LeaderLeaveConfirmData> {

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final GuiManager guiManager;

  private final Faction faction;

  private final FactionMember successor;

  /** Creates a new LeaderLeaveConfirmPage. */
  public LeaderLeaveConfirmPage(PlayerRef playerRef,
                 FactionManager factionManager,
                 GuiManager guiManager,
                 Faction faction) {
    super(playerRef, CustomPageLifetime.CanDismiss, LeaderLeaveConfirmData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.guiManager = guiManager;
    this.faction = faction;
    // Find the successor (highest officer, or senior member)
    this.successor = faction.findSuccessor();
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    // Load the leader leave confirmation template
    cmd.append(UIPaths.LEADER_LEAVE_CONFIRM);

    // Set faction name
    cmd.set("#FactionName.Text", faction.name());

    // Show succession information
    if (successor != null) {
      cmd.set("#SuccessionTitle.Text", "Leadership will transfer to:");
      cmd.set("#SuccessorName.Text", successor.username());
      cmd.set("#SuccessorRole.Text", successor.role().getDisplayName());
      cmd.set("#WarningText.Text", "");

      // Show Leave button
      cmd.set("#LeaveBtn.Visible", true);
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#LeaveBtn",
          EventData.of("Button", "Leave"),
          false
      );

      // Hide Disband button
      cmd.set("#DisbandBtn.Visible", false);
    } else {
      // No successor - faction will disband
      cmd.set("#SuccessionTitle.Text", "WARNING: No other members!");
      cmd.set("#SuccessorName.Text", "");
      cmd.set("#SuccessorRole.Text", "");
      cmd.set("#WarningText.Text", "Leaving will disband the faction permanently.");

      // Hide Leave button, show Disband button
      cmd.set("#LeaveBtn.Visible", false);
      cmd.set("#DisbandBtn.Visible", true);
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#DisbandBtn",
          EventData.of("Button", "Disband"),
          false
      );
    }

    // Cancel button - return to dashboard
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#CancelBtn",
        EventData.of("Button", "Cancel"),
        false
    );
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                LeaderLeaveConfirmData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null || data.button == null) {
      return;
    }

    UUID uuid = playerRef.getUuid();
    FactionMember member = faction.getMember(uuid);

    // Verify still in faction and still leader
    if (member == null) {
      player.sendMessage(MessageUtil.errorText("You are not in this faction."));
      guiManager.openFactionMain(player, ref, store, playerRef);
      return;
    }

    if (member.role() != FactionRole.LEADER) {
      player.sendMessage(MessageUtil.errorText("You are no longer the leader."));
      Faction fresh = factionManager.getFaction(faction.id());
      if (fresh != null) {
        guiManager.openFactionDashboard(player, ref, store, playerRef, fresh);
      } else {
        guiManager.openFactionMain(player, ref, store, playerRef);
      }
      return;
    }

    switch (data.button) {
      case "Cancel" -> {
        // Return to dashboard
        Faction fresh = factionManager.getFaction(faction.id());
        if (fresh != null) {
          guiManager.openFactionDashboard(player, ref, store, playerRef, fresh);
        } else {
          guiManager.openFactionMain(player, ref, store, playerRef);
        }
      }

      case "Leave" -> {
        // Transfer leadership to successor and leave
        if (successor == null) {
          player.sendMessage(MessageUtil.errorText("No successor available. Use disband instead."));
          return;
        }

        String factionName = faction.name();

        // Transfer leadership first (newLeader=successor, actor=current leader)
        FactionManager.FactionResult transferResult = factionManager.transferLeadership(
            faction.id(), successor.uuid(), uuid);

        if (transferResult != FactionManager.FactionResult.SUCCESS) {
          player.sendMessage(Message.raw("Failed to transfer leadership: " + transferResult).color("#FF5555"));
          return;
        }

        // Now leave the faction (as officer/member)
        FactionManager.FactionResult leaveResult = factionManager.removeMember(
            faction.id(), uuid, uuid, false);

        if (leaveResult == FactionManager.FactionResult.SUCCESS) {
          player.sendMessage(
              Message.raw("Leadership transferred to ").color("#55FF55")
                  .insert(Message.raw(successor.username()).color("#00FFFF"))
                  .insert(Message.raw(". You have left ").color("#55FF55"))
                  .insert(Message.raw(factionName).color("#00FFFF"))
                  .insert(Message.raw(".").color("#55FF55"))
          );
          guiManager.openFactionMain(player, ref, store, playerRef);
        } else {
          player.sendMessage(Message.raw("Failed to leave faction: " + leaveResult).color("#FF5555"));
          Faction fresh = factionManager.getFaction(faction.id());
          if (fresh != null) {
            guiManager.openFactionDashboard(player, ref, store, playerRef, fresh);
          }
        }
      }

      case "Disband" -> {
        // Open the disband confirmation page
        guiManager.openDisbandConfirm(player, ref, store, playerRef, faction);
      }
      default -> throw new IllegalStateException("Unexpected value");
    }
  }
}
