package com.hyperfactions.gui.admin.page;

import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneType;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.data.ZoneChangeTypeModalData;
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
 * Modal for changing a zone's type (SafeZone <-> WarZone).
 * Offers options to keep existing flag overrides or reset to new type defaults.
 */
public class ZoneChangeTypeModalPage extends InteractiveCustomUIPage<ZoneChangeTypeModalData> {

  private final PlayerRef playerRef;

  private final ZoneManager zoneManager;

  private final GuiManager guiManager;

  private final UUID zoneId;

  private final boolean returnToSettings;

  private final String currentTab;

  private final int currentPage;

  /** Creates a new ZoneChangeTypeModalPage. */
  public ZoneChangeTypeModalPage(PlayerRef playerRef,
                 ZoneManager zoneManager,
                 GuiManager guiManager,
                 UUID zoneId,
                 String currentTab,
                 int currentPage) {
    this(playerRef, zoneManager, guiManager, zoneId, false, currentTab, currentPage);
  }

  /** Creates a new ZoneChangeTypeModalPage. */
  public ZoneChangeTypeModalPage(PlayerRef playerRef,
                 ZoneManager zoneManager,
                 GuiManager guiManager,
                 UUID zoneId,
                 boolean returnToSettings,
                 String currentTab,
                 int currentPage) {
    super(playerRef, CustomPageLifetime.CanDismiss, ZoneChangeTypeModalData.CODEC);
    this.playerRef = playerRef;
    this.zoneManager = zoneManager;
    this.guiManager = guiManager;
    this.zoneId = zoneId;
    this.returnToSettings = returnToSettings;
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
    cmd.append(UIPaths.ZONE_CHANGE_TYPE_MODAL);

    // Localize labels
    cmd.set("#PageTitle.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZTYPE_TITLE));
    cmd.set("#ZoneLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZTYPE_ZONE_LABEL));
    cmd.set("#CurrentLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZTYPE_CURRENT));
    cmd.set("#WillBecomeLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZTYPE_WILL_BECOME));
    cmd.set("#NewLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZTYPE_NEW));
    cmd.set("#WarningLine1.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZTYPE_WARNING1));
    cmd.set("#WarningLine2.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZTYPE_WARNING2));
    cmd.set("#KeepFlagsDesc.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZTYPE_KEEP_DESC));
    cmd.set("#KeepFlagsBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZTYPE_KEEP_FLAGS));
    cmd.set("#ResetFlagsDesc.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZTYPE_RESET_DESC));
    cmd.set("#ResetFlagsBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZTYPE_RESET_FLAGS));
    cmd.set("#CancelBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_CANCEL));

    // Zone name
    cmd.set("#ZoneName.Text", zone.name());

    // Current and new types - use visibility toggle instead of setting style dynamically
    ZoneType currentType = zone.type();
    ZoneType newType = currentType == ZoneType.SAFE ? ZoneType.WAR : ZoneType.SAFE;

    // Toggle visibility of current type labels
    cmd.set("#CurrentTypeSafe.Visible", currentType == ZoneType.SAFE);
    cmd.set("#CurrentTypeWar.Visible", currentType == ZoneType.WAR);

    // Toggle visibility of new type labels
    cmd.set("#NewTypeSafe.Visible", newType == ZoneType.SAFE);
    cmd.set("#NewTypeWar.Visible", newType == ZoneType.WAR);

    // Cancel button
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#CancelBtn",
        EventData.of("Button", "Cancel"),
        false
    );

    // Keep flags button
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#KeepFlagsBtn",
        EventData.of("Button", "KeepFlags"),
        false
    );

    // Reset flags button
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#ResetFlagsBtn",
        EventData.of("Button", "ResetFlags"),
        false
    );
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                ZoneChangeTypeModalData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null || data.button == null) {
      return;
    }

    Zone zone = zoneManager.getZoneById(zoneId);
    if (zone == null) {
      player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.ZTYPE_ZONE_GONE));
      navigateBack(player, ref, store, playerRef);
      return;
    }

    switch (data.button) {
      case "Cancel" -> {
        navigateBack(player, ref, store, playerRef);
      }

      case "KeepFlags" -> {
        handleTypeChange(player, ref, store, playerRef, zone, false);
      }

      case "ResetFlags" -> {
        handleTypeChange(player, ref, store, playerRef, zone, true);
      }
      default -> throw new IllegalStateException("Unexpected value");
    }
  }

  private void handleTypeChange(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                 PlayerRef playerRef, Zone zone, boolean resetFlags) {
    ZoneType oldType = zone.type();
    ZoneManager.ZoneResult result = zoneManager.changeZoneType(zoneId, resetFlags);

    if (result == ZoneManager.ZoneResult.SUCCESS) {
      Zone updated = zoneManager.getZoneById(zoneId);
      ZoneType newType = updated != null ? updated.type() : (oldType == ZoneType.SAFE ? ZoneType.WAR : ZoneType.SAFE);

      String oldColor = oldType == ZoneType.SAFE ? "#55FF55" : "#FF5555";
      String newColor = newType == ZoneType.SAFE ? "#55FF55" : "#FF5555";

      player.sendMessage(MessageUtil.text(playerRef, MessageKeys.AdminGui.ZTYPE_CHANGED, "#AAAAAA",
          zone.name(), oldType.getDisplayName(), newType.getDisplayName(),
          HFMessages.get(playerRef, resetFlags ? MessageKeys.AdminGui.ZTYPE_FLAGS_RESET : MessageKeys.AdminGui.ZTYPE_FLAGS_KEPT)));
    } else {
      player.sendMessage(MessageUtil.adminError(playerRef, MessageKeys.AdminGui.ZTYPE_FAILED, result));
    }

    navigateBack(player, ref, store, playerRef);
  }

  private void navigateBack(Player player, Ref<EntityStore> ref,
               Store<EntityStore> store, PlayerRef playerRef) {
    if (returnToSettings) {
      guiManager.openAdminZoneProperties(player, ref, store, playerRef,
          zoneId, currentTab, currentPage);
    } else {
      guiManager.openAdminZone(player, ref, store, playerRef, currentTab, currentPage);
    }
  }
}
