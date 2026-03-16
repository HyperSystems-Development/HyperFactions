package com.hyperfactions.gui.shared.page;

import com.hyperfactions.api.events.EventBus;
import com.hyperfactions.api.events.FactionRenameEvent;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.shared.data.TagModalData;
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
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

/**
 * Modal for editing faction tag.
 * Max 5 chars, alphanumeric only, must be unique.
 */
public class TagModalPage extends InteractiveCustomUIPage<TagModalData> {

  private static final int MIN_TAG_LENGTH = 1;

  private static final int MAX_TAG_LENGTH = 5;

  private static final Pattern TAG_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final GuiManager guiManager;

  private final Faction faction;

  @Nullable
  private final WorldMapService worldMapService;

  private final boolean adminMode;

  /** Creates a new TagModalPage. */
  public TagModalPage(PlayerRef playerRef,
            FactionManager factionManager,
            GuiManager guiManager,
            Faction faction,
            @Nullable WorldMapService worldMapService) {
    this(playerRef, factionManager, guiManager, faction, worldMapService, false);
  }

  /** Creates a new TagModalPage. */
  public TagModalPage(PlayerRef playerRef,
            FactionManager factionManager,
            GuiManager guiManager,
            Faction faction,
            @Nullable WorldMapService worldMapService,
            boolean adminMode) {
    super(playerRef, CustomPageLifetime.CanDismiss, TagModalData.CODEC);
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
    cmd.append(UIPaths.TAG_MODAL);

    // Static labels
    cmd.set("#PageTitle.Text", HFMessages.get(playerRef, GuiKeys.TagGui.TITLE));
    cmd.set("#CurrentLabel.Text", HFMessages.get(playerRef, GuiKeys.TagGui.CURRENT_LABEL));
    cmd.set("#TagInstructions.Text", HFMessages.get(playerRef, GuiKeys.TagGui.INSTRUCTIONS));
    cmd.set("#TagHelpText.Text", HFMessages.get(playerRef, GuiKeys.TagGui.HELP_TEXT));
    cmd.set("#CancelBtn.Text", HFMessages.get(playerRef, CommonKeys.Common.CANCEL));
    cmd.set("#SaveBtn.Text", HFMessages.get(playerRef, CommonKeys.Common.SAVE));

    // Show current tag
    String currentTag = faction.tag();
    if (currentTag == null || currentTag.isEmpty()) {
      cmd.set("#CurrentTag.Text", HFMessages.get(playerRef, GuiKeys.TagGui.DISPLAY_NONE));
    } else {
      cmd.set("#CurrentTag.Text", "[" + currentTag.toUpperCase() + "]");
    }

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
        EventData.of("Button", "Save").append("@Tag", "#TagInput.Value"),
        false
    );
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                TagModalData data) {
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
      player.sendMessage(MessageUtil.error(playerRef, GuiKeys.TagGui.NO_PERMISSION));
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
        String newTag = data.tag;

        // Empty clears tag
        if (newTag == null || newTag.trim().isEmpty()) {
          Faction updatedFaction = faction.withTag(null);
          factionManager.updateFaction(updatedFaction);
          EventBus.publish(new FactionRenameEvent(faction.id(), FactionRenameEvent.Field.TAG, faction.tag(), null, uuid));

          // Refresh world maps to remove faction tag (respects configured refresh mode)
          if (worldMapService != null) {
            worldMapService.triggerFactionWideRefresh(faction.id());
          }

          String clearMsg = HFMessages.get(playerRef, GuiKeys.TagGui.CLEARED);
          if (adminMode) {
            clearMsg = HFMessages.get(playerRef, CommonKeys.Common.ADMIN_PREFIX) + " " + clearMsg;
          }
          player.sendMessage(Message.raw(clearMsg).color("#AAAAAA"));
          if (adminMode) {
            guiManager.openAdminFactionSettings(player, ref, store, playerRef, faction.id());
          } else {
            guiManager.openFactionSettings(player, ref, store, playerRef,
                factionManager.getFaction(faction.id()));
          }
          return;
        }

        newTag = newTag.trim().toUpperCase();

        // Validate length
        if (newTag.length() < MIN_TAG_LENGTH) {
          player.sendMessage(MessageUtil.error(playerRef, GuiKeys.TagGui.TOO_SHORT, MIN_TAG_LENGTH));
          sendUpdate();
          return;
        }

        if (newTag.length() > MAX_TAG_LENGTH) {
          player.sendMessage(MessageUtil.error(playerRef, GuiKeys.TagGui.TOO_LONG, MAX_TAG_LENGTH));
          sendUpdate();
          return;
        }

        // Validate format (alphanumeric only)
        if (!TAG_PATTERN.matcher(newTag).matches()) {
          player.sendMessage(MessageUtil.error(playerRef, GuiKeys.TagGui.INVALID_FORMAT));
          sendUpdate();
          return;
        }

        // Check if same as current
        if (newTag.equalsIgnoreCase(faction.tag())) {
          player.sendMessage(MessageUtil.info(playerRef, GuiKeys.TagGui.SAME_TAG, "#FFD700"));
          sendUpdate();
          return;
        }

        // Check uniqueness
        Faction existing = factionManager.getFactionByTag(newTag);
        if (existing != null && !existing.id().equals(faction.id())) {
          player.sendMessage(MessageUtil.error(playerRef, GuiKeys.TagGui.TAG_TAKEN));
          sendUpdate();
          return;
        }

        // Update the faction
        Faction updatedFaction = faction.withTag(newTag);
        factionManager.updateFaction(updatedFaction);
        EventBus.publish(new FactionRenameEvent(faction.id(), FactionRenameEvent.Field.TAG, faction.tag(), newTag, uuid));

        // Refresh world maps to show new faction tag (respects configured refresh mode)
        if (worldMapService != null) {
          worldMapService.triggerFactionWideRefresh(faction.id());
        }

        String successMsg = HFMessages.get(playerRef, GuiKeys.TagGui.SUCCESS, newTag);
        if (adminMode) {
          successMsg = HFMessages.get(playerRef, CommonKeys.Common.ADMIN_PREFIX) + " " + successMsg;
        }
        player.sendMessage(Message.raw(successMsg).color("#55FF55"));

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
