package com.hyperfactions.gui.admin.page;

import com.hyperfactions.data.Zone;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.data.ZoneRenameModalData;
import com.hyperfactions.manager.ZoneManager;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.MessageKeys;
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

    // Localize labels
    cmd.set("#PageTitle.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZREN_TITLE));
    cmd.set("#CurrentLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZREN_CURRENT));
    cmd.set("#NewNameLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZREN_NEW_NAME));
    cmd.set("#CancelBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_CANCEL));
    cmd.set("#SaveBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SAVE));

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
      player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.ZREN_ZONE_GONE));
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
          player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.ZREN_ENTER_NAME));
          sendUpdate();
          return;
        }

        newName = newName.trim();

        if (newName.length() < MIN_NAME_LENGTH) {
          player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.ZREN_TOO_SHORT, MIN_NAME_LENGTH));
          sendUpdate();
          return;
        }

        if (newName.length() > MAX_NAME_LENGTH) {
          player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.ZREN_TOO_LONG, MAX_NAME_LENGTH));
          sendUpdate();
          return;
        }

        // Check if name is the same
        if (newName.equalsIgnoreCase(zone.name())) {
          player.sendMessage(MessageUtil.text(playerRef, MessageKeys.AdminGui.ZREN_SAME_NAME, MessageUtil.COLOR_GOLD));
          sendUpdate();
          return;
        }

        // Use ZoneManager's rename method which handles uniqueness check
        String oldName = zone.name();
        ZoneManager.ZoneResult result = zoneManager.renameZone(zoneId, newName);

        switch (result) {
          case SUCCESS -> {
            player.sendMessage(MessageUtil.text(playerRef, MessageKeys.AdminGui.ZREN_RENAMED, "#AAAAAA", oldName, newName));
            guiManager.openAdminZone(player, ref, store, playerRef, currentTab, currentPage);
          }
          case NAME_TAKEN -> {
            player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.ZREN_NAME_TAKEN));
            sendUpdate();
          }
          case INVALID_NAME -> {
            player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.ZREN_INVALID_NAME));
            sendUpdate();
          }
          case NOT_FOUND -> {
            player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.ZREN_ZONE_GONE));
            guiManager.openAdminZone(player, ref, store, playerRef, currentTab, currentPage);
          }
          default -> {
            player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.ZREN_RENAME_FAILED, result));
            sendUpdate();
          }
        }
      }
    }
  }
}
