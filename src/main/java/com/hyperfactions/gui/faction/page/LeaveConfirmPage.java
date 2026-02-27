package com.hyperfactions.gui.faction.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.faction.data.LeaveConfirmData;
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
 * Confirmation modal for leaving a faction.
 * Shows a warning and requires explicit confirmation.
 */
public class LeaveConfirmPage extends InteractiveCustomUIPage<LeaveConfirmData> {

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final GuiManager guiManager;

  private final Faction faction;

  /** Creates a new LeaveConfirmPage. */
  public LeaveConfirmPage(PlayerRef playerRef,
              FactionManager factionManager,
              GuiManager guiManager,
              Faction faction) {
    super(playerRef, CustomPageLifetime.CanDismiss, LeaveConfirmData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.guiManager = guiManager;
    this.faction = faction;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    // Load the leave confirmation template
    cmd.append(UIPaths.LEAVE_CONFIRM);

    // Set faction name in the modal
    cmd.set("#FactionName.Text", faction.name());

    // Cancel button - return to dashboard
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#CancelBtn",
        EventData.of("Button", "Cancel"),
        false
    );

    // Confirm button - actually leave
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#ConfirmBtn",
        EventData.of("Button", "Confirm"),
        false
    );
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                LeaveConfirmData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null || data.button == null) {
      return;
    }

    UUID uuid = playerRef.getUuid();
    FactionMember member = faction.getMember(uuid);

    // Verify still in faction
    if (member == null) {
      player.sendMessage(MessageUtil.errorText("You are not in this faction."));
      guiManager.openFactionMain(player, ref, store, playerRef);
      return;
    }

    // Leaders cannot leave via this modal (they must disband or transfer leadership)
    if (member.role() == FactionRole.LEADER) {
      player.sendMessage(MessageUtil.errorText("Leaders cannot leave. Transfer leadership or disband the faction."));
      guiManager.openFactionDashboard(player, ref, store, playerRef,
          factionManager.getFaction(faction.id()));
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

      case "Confirm" -> {
        // Actually leave the faction
        String factionName = faction.name();
        FactionManager.FactionResult result = factionManager.removeMember(
            faction.id(), uuid, uuid, false);

        if (result == FactionManager.FactionResult.SUCCESS) {
          player.sendMessage(
              Message.raw("You have left ").color("#FFAA00")
                  .insert(Message.raw(factionName).color("#00FFFF"))
                  .insert(Message.raw(".").color("#FFAA00"))
          );
          guiManager.openFactionMain(player, ref, store, playerRef);
        } else {
          player.sendMessage(Message.raw("Failed to leave faction: " + result).color("#FF5555"));
          guiManager.openFactionMain(player, ref, store, playerRef);
        }
      }
      default -> throw new IllegalStateException("Unexpected value");
    }
  }
}
