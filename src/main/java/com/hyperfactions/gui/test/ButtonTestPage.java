package com.hyperfactions.gui.test;

import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.shared.data.PlaceholderData;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;

/**
 * Permanent element & style test page for research and debugging.
 * Open via: /f admin testgui
 */
public class ButtonTestPage extends InteractiveCustomUIPage<PlaceholderData> {

  private final PlayerRef playerRef;

  /** Creates a new ButtonTestPage. */
  public ButtonTestPage(PlayerRef playerRef) {
    super(playerRef, CustomPageLifetime.CanDismiss, PlaceholderData.CODEC);
    this.playerRef = playerRef;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {
    cmd.append(UIPaths.BUTTON_TEST);

    // ColorPicker: set initial value from Java (NOT in .ui — crashes)
    cmd.set("#TestColorPicker.Value", "#55FFFF");

    // Disabled checkbox test — target inner #CheckBox element
    cmd.set("#TestCheckBoxDisabled #CheckBox.Disabled", true);

    // NumberField: template wraps inner element, try child selector
    // cmd.set("#TestNumberField.Value", "42");  // CRASHES — "couldn't set value"

    // Value.ref test — apply SecondaryTextButtonStyle from Java
    cmd.appendInline("#JavaTestArea",
        "TextButton #TestSecRef { Text: \"Value.ref test\"; Anchor: (Height: 36, Width: 200, Bottom: 6); }");
    cmd.set("#TestSecRef.Style", Value.ref("Common.ui", "SecondaryTextButtonStyle"));

    // DropdownBox: populate entries from Java
    List<DropdownEntryInfo> entries = List.of(
        new DropdownEntryInfo(LocalizableString.fromString("Option A"), "a"),
        new DropdownEntryInfo(LocalizableString.fromString("Option B"), "b"),
        new DropdownEntryInfo(LocalizableString.fromString("Option C"), "c")
    );
    cmd.set("#TestDropdown.Entries", entries);
    cmd.set("#TestDropdown.Value", "a");

    // ColorPicker ValueChanged event
    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        "#TestColorPicker",
        EventData.of("Button", "ColorChanged")
            .append("@Color", "#TestColorPicker.Value"),
        false
    );

    Logger.info("[ElementTest] Build complete for %s", playerRef.getUsername());
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                PlaceholderData data) {
    super.handleDataEvent(ref, store, data);
    if (data.button != null) {
      switch (data.button) {
        case "ColorChanged" -> Logger.info("[ElementTest] Color changed");
        default -> Logger.info("[ElementTest] Button: %s", data.button);
      }
    }
  }
}
