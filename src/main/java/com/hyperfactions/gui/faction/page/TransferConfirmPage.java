package com.hyperfactions.gui.faction.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.faction.data.TransferConfirmData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.MessageKeys;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

/**
 * Confirmation modal for transferring faction leadership.
 * Shows a warning and requires explicit confirmation.
 */
public class TransferConfirmPage extends InteractiveCustomUIPage<TransferConfirmData> {

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final GuiManager guiManager;

  private final Faction faction;

  private final UUID targetUuid;

  private final String targetName;

  /** Creates a new TransferConfirmPage. */
  public TransferConfirmPage(PlayerRef playerRef,
               FactionManager factionManager,
               GuiManager guiManager,
               Faction faction,
               UUID targetUuid,
               String targetName) {
    super(playerRef, CustomPageLifetime.CanDismiss, TransferConfirmData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.guiManager = guiManager;
    this.faction = faction;
    this.targetUuid = targetUuid;
    this.targetName = targetName;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    // Load the transfer confirmation template
    cmd.append(UIPaths.TRANSFER_CONFIRM);

    // Set dynamic values
    cmd.set("#TargetName.Text", targetName);

    // Cancel button - return to members page
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#CancelBtn",
        EventData.of("Button", "Cancel"),
        false
    );

    // Confirm button - actually transfer leadership
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
                TransferConfirmData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null || data.button == null) {
      return;
    }

    UUID uuid = playerRef.getUuid();

    // Re-fetch faction to ensure fresh state
    Faction currentFaction = factionManager.getFaction(faction.id());
    if (currentFaction == null) {
      player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.ConfirmGui.FACTION_GONE));
      guiManager.openFactionMain(player, ref, store, playerRef);
      return;
    }

    FactionMember member = currentFaction.getMember(uuid);

    // Verify leader permission
    if (member == null || member.role() != FactionRole.LEADER) {
      player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.ConfirmGui.NOT_LEADER_TRANSFER));
      guiManager.openFactionMembers(player, ref, store, playerRef, currentFaction);
      return;
    }

    switch (data.button) {
      case "Cancel" -> {
        // Return to members page
        guiManager.openFactionMembers(player, ref, store, playerRef, currentFaction);
      }

      case "Confirm" -> {
        // Actually transfer leadership
        FactionManager.FactionResult result = factionManager.transferLeadership(
            faction.id(), targetUuid, uuid);

        if (result == FactionManager.FactionResult.SUCCESS) {
          player.sendMessage(MessageUtil.successText(playerRef, MessageKeys.ConfirmGui.LEADERSHIP_TRANSFERRED, targetName));
          // Refresh to show updated roles
          Faction refreshedFaction = factionManager.getFaction(faction.id());
          if (refreshedFaction != null) {
            guiManager.openFactionMembers(player, ref, store, playerRef, refreshedFaction);
          } else {
            guiManager.openFactionMain(player, ref, store, playerRef);
          }
        } else {
          player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.ConfirmGui.TRANSFER_FAILED, result));
          guiManager.openFactionMembers(player, ref, store, playerRef, currentFaction);
        }
      }
      default -> throw new IllegalStateException("Unexpected value");
    }
  }
}
