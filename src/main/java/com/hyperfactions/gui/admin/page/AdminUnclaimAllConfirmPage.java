package com.hyperfactions.gui.admin.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.data.AdminUnclaimAllConfirmData;
import com.hyperfactions.manager.ClaimManager;
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
 * Admin confirmation modal for unclaiming all territory from a faction.
 */
public class AdminUnclaimAllConfirmPage extends InteractiveCustomUIPage<AdminUnclaimAllConfirmData> {

  private final PlayerRef playerRef;

  private final ClaimManager claimManager;

  private final GuiManager guiManager;

  private final UUID factionId;

  private final String factionName;

  private final int claimCount;

  /** Creates a new AdminUnclaimAllConfirmPage. */
  public AdminUnclaimAllConfirmPage(PlayerRef playerRef,
                   ClaimManager claimManager,
                   GuiManager guiManager,
                   UUID factionId,
                   String factionName,
                   int claimCount) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminUnclaimAllConfirmData.CODEC);
    this.playerRef = playerRef;
    this.claimManager = claimManager;
    this.guiManager = guiManager;
    this.factionId = factionId;
    this.factionName = factionName;
    this.claimCount = claimCount;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    cmd.append(UIPaths.UNCLAIM_ALL_CONFIRM);

    // Set faction info
    cmd.set("#FactionName.Text", factionName);
    cmd.set("#ClaimCount.Text", claimCount + " chunks");

    // Cancel button
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#CancelBtn",
        EventData.of("Button", "Cancel"),
        false
    );

    // Confirm button
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
                AdminUnclaimAllConfirmData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null || data.button == null) {
      return;
    }

    switch (data.button) {
      case "Cancel" -> {
        guiManager.openAdminFactions(player, ref, store, playerRef);
      }

      case "Confirm" -> {
        // Unclaim all territory (claimCount was stored from when modal opened)
        claimManager.unclaimAll(factionId);

        if (claimCount > 0) {
          player.sendMessage(
              Message.raw("[Admin] Removed ").color("#FF5555")
                  .insert(Message.raw(String.valueOf(claimCount)).color("#FFFFFF"))
                  .insert(Message.raw(" claims from ").color("#FF5555"))
                  .insert(Message.raw(factionName).color("#00FFFF"))
                  .insert(Message.raw(".").color("#FF5555"))
          );
        } else {
          player.sendMessage(
              Message.raw("[Admin] ").color("#FFAA00")
                  .insert(Message.raw(factionName).color("#00FFFF"))
                  .insert(Message.raw(" had no claims to remove.").color("#FFAA00"))
          );
        }

        guiManager.openAdminFactions(player, ref, store, playerRef);
      }
      default -> throw new IllegalStateException("Unexpected value");
    }
  }
}
