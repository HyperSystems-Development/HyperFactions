package com.hyperfactions.gui.shared.page;

import com.hyperfactions.api.events.EventBus;
import com.hyperfactions.api.events.FactionRenameEvent;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.shared.data.RenameModalData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.CommonKeys;
import com.hyperfactions.util.GuiKeys;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.worldmap.WorldMapService;
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
import org.jetbrains.annotations.Nullable;

/**
 * Modal for renaming a faction.
 * Validates name uniqueness before saving.
 */
public class RenameModalPage extends InteractiveCustomUIPage<RenameModalData> {

  private static final int MIN_NAME_LENGTH = 3;

  private static final int MAX_NAME_LENGTH = 32;

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final GuiManager guiManager;

  private final Faction faction;

  @Nullable
  private final WorldMapService worldMapService;

  private final boolean adminMode;

  /** Creates a new RenameModalPage. */
  public RenameModalPage(PlayerRef playerRef,
             FactionManager factionManager,
             GuiManager guiManager,
             Faction faction,
             @Nullable WorldMapService worldMapService) {
    this(playerRef, factionManager, guiManager, faction, worldMapService, false);
  }

  /** Creates a new RenameModalPage. */
  public RenameModalPage(PlayerRef playerRef,
             FactionManager factionManager,
             GuiManager guiManager,
             Faction faction,
             @Nullable WorldMapService worldMapService,
             boolean adminMode) {
    super(playerRef, CustomPageLifetime.CanDismiss, RenameModalData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.guiManager = guiManager;
    this.faction = faction;
    this.worldMapService = worldMapService;
    this.adminMode = adminMode;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    // Load the modal template
    cmd.append(UIPaths.RENAME_MODAL);

    // Static labels
    cmd.set("#PageTitle.Text", HFMessages.get(playerRef, GuiKeys.RenameGui.TITLE));
    cmd.set("#CurrentLabel.Text", HFMessages.get(playerRef, GuiKeys.RenameGui.CURRENT_LABEL));
    cmd.set("#NewNameLabel.Text", HFMessages.get(playerRef, GuiKeys.RenameGui.NEW_NAME_LABEL));
    cmd.set("#CancelBtn.Text", HFMessages.get(playerRef, CommonKeys.Common.CANCEL));
    cmd.set("#SaveBtn.Text", HFMessages.get(playerRef, CommonKeys.Common.SAVE));

    // Show current name
    cmd.set("#CurrentName.Text", faction.name());

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
                RenameModalData data) {
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
      player.sendMessage(MessageUtil.error(playerRef, GuiKeys.RenameGui.NO_PERMISSION));
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

      case "Save" -> {
        String newName = data.name;

        // Validation
        if (newName == null || newName.trim().isEmpty()) {
          player.sendMessage(MessageUtil.error(playerRef, GuiKeys.RenameGui.ENTER_NAME));
          sendUpdate();
          return;
        }

        newName = newName.trim();

        if (newName.length() < MIN_NAME_LENGTH) {
          player.sendMessage(MessageUtil.error(playerRef, GuiKeys.RenameGui.TOO_SHORT, MIN_NAME_LENGTH));
          sendUpdate();
          return;
        }

        if (newName.length() > MAX_NAME_LENGTH) {
          player.sendMessage(MessageUtil.error(playerRef, GuiKeys.RenameGui.TOO_LONG, MAX_NAME_LENGTH));
          sendUpdate();
          return;
        }

        // Check if name is the same
        if (newName.equalsIgnoreCase(faction.name())) {
          player.sendMessage(MessageUtil.info(playerRef, GuiKeys.RenameGui.SAME_NAME, "#FFD700"));
          sendUpdate();
          return;
        }

        // Check uniqueness
        Faction existing = factionManager.getFactionByName(newName);
        if (existing != null) {
          player.sendMessage(MessageUtil.error(playerRef, GuiKeys.RenameGui.NAME_TAKEN));
          sendUpdate();
          return;
        }

        // Update the faction
        String oldName = faction.name();
        Faction updatedFaction = faction.withName(newName);
        factionManager.updateFaction(updatedFaction);
        EventBus.publish(new FactionRenameEvent(faction.id(), FactionRenameEvent.Field.NAME, oldName, newName, uuid));

        // Refresh world maps to show new faction name (respects configured refresh mode)
        if (worldMapService != null) {
          worldMapService.triggerFactionWideRefresh(faction.id());
        }

        String msg = HFMessages.get(playerRef, GuiKeys.RenameGui.SUCCESS, oldName, newName);
        if (adminMode) {
          msg = HFMessages.get(playerRef, CommonKeys.Common.ADMIN_PREFIX) + " " + msg;
        }
        player.sendMessage(Message.raw(msg).color("#55FF55"));

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
