package com.hyperfactions.gui.shared.component;

import com.hyperfactions.gui.UIPaths;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reusable input modal component.
 * Displays a text input prompt with custom title, placeholder, and validation.
 */
public class InputModal {

  private final String title;

  private final String label;

  private final String placeholder;

  private final String currentValue;

  private final int maxLength;

  private final boolean multiline;

  private final String submitEventName;

  private final String cancelEventName;

  private final EventData submitEventData;

  private final EventData cancelEventData;

  /**
   * Builder for InputModal.
   */
  public static class Builder {
    private String title = "Input";

    private String label = "Enter value:";

    private String placeholder = "";

    private String currentValue = "";

    private int maxLength = 50;

    private boolean multiline = false;

    private String submitEventName = "Submit";

    private String cancelEventName = "Cancel";

    private EventData submitEventData;

    private EventData cancelEventData;

    /** Title. */
    public Builder title(@NotNull String title) {
      this.title = title;
      return this;
    }

    /** Label. */
    public Builder label(@NotNull String label) {
      this.label = label;
      return this;
    }

    /** Placeholder. */
    public Builder placeholder(@NotNull String placeholder) {
      this.placeholder = placeholder;
      return this;
    }

    /** Current Value. */
    public Builder currentValue(@Nullable String currentValue) {
      this.currentValue = currentValue != null ? currentValue : "";
      return this;
    }

    /** Max Length. */
    public Builder maxLength(int maxLength) {
      this.maxLength = maxLength;
      return this;
    }

    public Builder multiline(boolean multiline) {
      this.multiline = multiline;
      return this;
    }

    /** Submit Event. */
    public Builder submitEvent(@NotNull String eventName) {
      this.submitEventName = eventName;
      return this;
    }

    /** Submit Event. */
    public Builder submitEvent(@NotNull String eventName, @NotNull EventData data) {
      this.submitEventName = eventName;
      this.submitEventData = data;
      return this;
    }

    /** Checks if cel event. */
    public Builder cancelEvent(@NotNull String eventName) {
      this.cancelEventName = eventName;
      return this;
    }

    /** Checks if cel event. */
    public Builder cancelEvent(@NotNull String eventName, @NotNull EventData data) {
      this.cancelEventName = eventName;
      this.cancelEventData = data;
      return this;
    }

    /** Builds . */
    public InputModal build() {
      if (submitEventData == null) {
        submitEventData = EventData.of("Button", submitEventName);
      }
      if (cancelEventData == null) {
        cancelEventData = EventData.of("Button", cancelEventName);
      }
      return new InputModal(
        title, label, placeholder, currentValue, maxLength, multiline,
        submitEventName, cancelEventName, submitEventData, cancelEventData
      );
    }
  }

  private InputModal(String title, String label, String placeholder, String currentValue,
           int maxLength, boolean multiline, String submitEventName,
           String cancelEventName, EventData submitEventData, EventData cancelEventData) {
    this.title = title;
    this.label = label;
    this.placeholder = placeholder;
    this.currentValue = currentValue;
    this.maxLength = maxLength;
    this.multiline = multiline;
    this.submitEventName = submitEventName;
    this.cancelEventName = cancelEventName;
    this.submitEventData = submitEventData;
    this.cancelEventData = cancelEventData;
  }

  /** Builds er. */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Renders the input modal into the UI.
   *
   * @param cmd        UI command builder
   * @param events     UI event builder
   * @param targetId   Target element ID where modal should be appended (e.g., "#ModalContainer")
   */
  public void render(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events,
           @NotNull String targetId) {
    // Append modal template (single-line or multiline)
    if (multiline) {
      cmd.append(targetId, UIPaths.MODAL_INPUT_MULTILINE);
    } else {
      cmd.append(targetId, UIPaths.MODAL_INPUT);
    }

    // Set title and label
    cmd.set(targetId + " #ModalTitle.Text", title);
    cmd.set(targetId + " #InputLabel.Text", label);

    // Set input properties
    cmd.set(targetId + " #InputField.Placeholder", placeholder);
    if (!currentValue.isEmpty()) {
      cmd.set(targetId + " #InputField.Text", currentValue);
    }
    cmd.set(targetId + " #InputField.MaxLength", String.valueOf(maxLength));

    // Bind submit button (will include input value in event data)
    events.addEventBinding(
      CustomUIEventBindingType.Activating,
      targetId + " #SubmitBtn",
      submitEventData.append("InputValue", targetId + " #InputField.Text"),
      false
    );

    // Bind cancel button
    events.addEventBinding(
      CustomUIEventBindingType.Activating,
      targetId + " #CancelBtn",
      cancelEventData,
      false
    );
  }

  /**
   * Quick helper for faction rename.
   */
  public static InputModal rename(String currentName) {
    return builder()
      .title("Rename Faction")
      .label("Enter new faction name:")
      .placeholder("My Faction")
      .currentValue(currentName)
      .maxLength(32)
      .submitEvent("RenameSubmit")
      .build();
  }

  /**
   * Quick helper for faction description.
   */
  public static InputModal description(String currentDescription) {
    return builder()
      .title("Set Description")
      .label("Enter faction description:")
      .placeholder("A great faction...")
      .currentValue(currentDescription)
      .maxLength(200)
      .multiline(true)
      .submitEvent("DescriptionSubmit")
      .build();
  }

  /**
   * Quick helper for player search/invite.
   */
  public static InputModal playerName() {
    return builder()
      .title("Invite Player")
      .label("Enter player name:")
      .placeholder("PlayerName")
      .maxLength(16)
      .submitEvent("InviteSubmit")
      .build();
  }

  // Getters
  public String getTitle() {
    return title;
  }

  /** Returns the label. */
  public String getLabel() {
    return label;
  }

  /** Returns the placeholder. */
  public String getPlaceholder() {
    return placeholder;
  }

  /** Returns the current value. */
  public String getCurrentValue() {
    return currentValue;
  }

  /** Returns the max length. */
  public int getMaxLength() {
    return maxLength;
  }

  public boolean isMultiline() {
    return multiline;
  }
}
