package com.hyperfactions.gui.shared.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.shared.data.DescriptionModalData;
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
 * Modal for editing faction description.
 * Maximum 256 characters.
 */
public class DescriptionModalPage extends InteractiveCustomUIPage<DescriptionModalData> {

  private static final int MAX_DESCRIPTION_LENGTH = 256;

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final GuiManager guiManager;

  private final Faction faction;

  private final boolean adminMode;

  /** Creates a new DescriptionModalPage. */
  public DescriptionModalPage(PlayerRef playerRef,
                FactionManager factionManager,
                GuiManager guiManager,
                Faction faction) {
    this(playerRef, factionManager, guiManager, faction, false);
  }

  /** Creates a new DescriptionModalPage. */
  public DescriptionModalPage(PlayerRef playerRef,
                FactionManager factionManager,
                GuiManager guiManager,
                Faction faction,
                boolean adminMode) {
    super(playerRef, CustomPageLifetime.CanDismiss, DescriptionModalData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.guiManager = guiManager;
    this.faction = faction;
    this.adminMode = adminMode;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    // Load the modal template
    cmd.append(UIPaths.DESCRIPTION_MODAL);

    // Show current description
    String currentDesc = faction.description();
    if (currentDesc == null || currentDesc.isEmpty()) {
      cmd.set("#CurrentDesc.Text", "(None)");
    } else {
      // Truncate display if too long
      String display = currentDesc.length() > 100
          ? currentDesc.substring(0, 97) + "..."
          : currentDesc;
      cmd.set("#CurrentDesc.Text", display);
    }

    // Cancel button
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#CancelBtn",
        EventData.of("Button", "Cancel"),
        false
    );

    // Clear button
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#ClearBtn",
        EventData.of("Button", "Clear"),
        false
    );

    // Save button - captures text input value
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#SaveBtn",
        EventData.of("Button", "Save").append("@Description", "#DescInput.Value"),
        false
    );
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                DescriptionModalData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null || data.button == null) {
      return;
    }

    UUID uuid = playerRef.getUuid();
    FactionMember member = faction.getMember(uuid);

    // Verify officer permission (skip in admin mode)
    if (!adminMode && (member == null || member.role().getLevel() < FactionRole.OFFICER.getLevel())) {
      player.sendMessage(MessageUtil.errorText("You don't have permission to edit the description."));
      guiManager.openFactionSettings(player, ref, store, playerRef,
          factionManager.getFaction(faction.id()));
      return;
    }

    switch (data.button) {
      case "Cancel" -> {
        if (adminMode) {
          guiManager.openAdminFactionSettings(player, ref, store, playerRef, faction.id());
        } else {
          guiManager.openFactionSettings(player, ref, store, playerRef,
              factionManager.getFaction(faction.id()));
        }
      }

      case "Clear" -> {
        // Clear the description
        Faction updatedFaction = faction.withDescription(null);
        factionManager.updateFaction(updatedFaction);

        String prefix = adminMode ? "[Admin] " : "";
        player.sendMessage(Message.raw(prefix + "Faction description cleared.").color("#AAAAAA"));

        if (adminMode) {
          guiManager.openAdminFactionSettings(player, ref, store, playerRef, faction.id());
        } else {
          guiManager.openFactionSettings(player, ref, store, playerRef,
              factionManager.getFaction(faction.id()));
        }
      }

      case "Save" -> {
        String newDesc = data.description;
        String prefix = adminMode ? "[Admin] " : "";

        // Empty is allowed (clears description)
        if (newDesc == null || newDesc.trim().isEmpty()) {
          Faction updatedFaction = faction.withDescription(null);
          factionManager.updateFaction(updatedFaction);
          player.sendMessage(Message.raw(prefix + "Faction description cleared.").color("#AAAAAA"));
        } else {
          newDesc = newDesc.trim();

          if (newDesc.length() > MAX_DESCRIPTION_LENGTH) {
            newDesc = newDesc.substring(0, MAX_DESCRIPTION_LENGTH);
          }

          Faction updatedFaction = faction.withDescription(newDesc);
          factionManager.updateFaction(updatedFaction);

          player.sendMessage(Message.raw(prefix + "Faction description updated!").color("#55FF55"));
        }

        if (adminMode) {
          guiManager.openAdminFactionSettings(player, ref, store, playerRef, faction.id());
        } else {
          guiManager.openFactionSettings(player, ref, store, playerRef,
              factionManager.getFaction(faction.id()));
        }
      }
      default -> throw new IllegalStateException("Unexpected value");
    }
  }
}
