package com.hyperfactions.gui.admin.page;

import com.hyperfactions.data.Zone;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.data.ZoneRenameModalData;
import com.hyperfactions.manager.ZoneManager;
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
 * Modal for renaming a zone (safe zone or war zone).
 * Validates name uniqueness before saving.
 */
public class ZoneRenameModalPage extends InteractiveCustomUIPage<ZoneRenameModalData> {

  private static final int MIN_NAME_LENGTH = 1;

  private static final int MAX_NAME_LENGTH = 32;

  private final PlayerRef playerRef;

  private final ZoneManager zoneManager;

  private final GuiManager guiManager;

  private final UUID zoneId;

  private final String currentTab;

  private final int currentPage;

  /** Creates a new ZoneRenameModalPage. */
  public ZoneRenameModalPage(PlayerRef playerRef,
               ZoneManager zoneManager,
               GuiManager guiManager,
               UUID zoneId,
               String currentTab,
               int currentPage) {
    super(playerRef, CustomPageLifetime.CanDismiss, ZoneRenameModalData.CODEC);
    this.playerRef = playerRef;
    this.zoneManager = zoneManager;
    this.guiManager = guiManager;
    this.zoneId = zoneId;
    this.currentTab = currentTab;
    this.currentPage = currentPage;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    Zone zone = zoneManager.getZoneById(zoneId);
    if (zone == null) {
      return;
    }

    // Load the modal template
    cmd.append(UIPaths.ZONE_RENAME_MODAL);

    // Show current name
    cmd.set("#CurrentName.Text", zone.name());

    // Cancel button
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#CancelBtn",
        EventData.of("Button", "Cancel"),
        false
    );

    // Save button - captures text input value
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#SaveBtn",
        EventData.of("Button", "Save").append("@Name", "#NameInput.Value"),
        false
    );
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                ZoneRenameModalData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null || data.button == null) {
      return;
    }

    Zone zone = zoneManager.getZoneById(zoneId);
    if (zone == null) {
      player.sendMessage(MessageUtil.errorText("Zone no longer exists."));
      guiManager.openAdminZone(player, ref, store, playerRef, currentTab, currentPage);
      return;
    }

    switch (data.button) {
      case "Cancel" -> {
        guiManager.openAdminZone(player, ref, store, playerRef, currentTab, currentPage);
      }

      case "Save" -> {
        String newName = data.name;

        // Validation
        if (newName == null || newName.trim().isEmpty()) {
          player.sendMessage(MessageUtil.errorText("Please enter a zone name."));
          sendUpdate();
          return;
        }

        newName = newName.trim();

        if (newName.length() < MIN_NAME_LENGTH) {
          player.sendMessage(MessageUtil.errorText("Zone name must be at least " + MIN_NAME_LENGTH + " character."));
          sendUpdate();
          return;
        }

        if (newName.length() > MAX_NAME_LENGTH) {
          player.sendMessage(MessageUtil.errorText("Zone name cannot exceed " + MAX_NAME_LENGTH + " characters."));
          sendUpdate();
          return;
        }

        // Check if name is the same
        if (newName.equalsIgnoreCase(zone.name())) {
          player.sendMessage(MessageUtil.text("That's already this zone's name.", MessageUtil.COLOR_GOLD));
          sendUpdate();
          return;
        }

        // Use ZoneManager's rename method which handles uniqueness check
        String oldName = zone.name();
        ZoneManager.ZoneResult result = zoneManager.renameZone(zoneId, newName);

        switch (result) {
          case SUCCESS -> {
            player.sendMessage(
                Message.raw("[Admin] Zone renamed from ").color("#AAAAAA")
                    .insert(Message.raw(oldName).color("#888888"))
                    .insert(Message.raw(" to ").color("#AAAAAA"))
                    .insert(Message.raw(newName).color("#00FFFF"))
                    .insert(Message.raw("!").color("#AAAAAA"))
            );
            guiManager.openAdminZone(player, ref, store, playerRef, currentTab, currentPage);
          }
          case NAME_TAKEN -> {
            player.sendMessage(MessageUtil.errorText("A zone with that name already exists."));
            sendUpdate();
          }
          case INVALID_NAME -> {
            player.sendMessage(MessageUtil.errorText("Invalid zone name."));
            sendUpdate();
          }
          case NOT_FOUND -> {
            player.sendMessage(MessageUtil.errorText("Zone no longer exists."));
            guiManager.openAdminZone(player, ref, store, playerRef, currentTab, currentPage);
          }
          default -> {
            player.sendMessage(MessageUtil.errorText("Failed to rename zone: " + result));
            sendUpdate();
          }
        }
      }
    }
  }
}
