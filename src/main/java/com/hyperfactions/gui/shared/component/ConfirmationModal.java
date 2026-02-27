package com.hyperfactions.gui.shared.component;

import com.hyperfactions.gui.UIPaths;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reusable confirmation modal component.
 * Displays a yes/no prompt with custom title, message, and event data.
 */
public class ConfirmationModal {

  private final String title;

  private final String message;

  private final String confirmEventName;

  private final String cancelEventName;

  private final EventData confirmEventData;

  private final EventData cancelEventData;

  /**
   * Builder for ConfirmationModal.
   */
  public static class Builder {
    private String title = "Confirm Action";

    private String message = "Are you sure?";

    private String confirmEventName = "Confirm";

    private String cancelEventName = "Cancel";

    private EventData confirmEventData;

    private EventData cancelEventData;

    /** Title. */
    public Builder title(@NotNull String title) {
      this.title = title;
      return this;
    }

    /** Message. */
    public Builder message(@NotNull String message) {
      this.message = message;
      return this;
    }

    /** Confirm Event. */
    public Builder confirmEvent(@NotNull String eventName) {
      this.confirmEventName = eventName;
      return this;
    }

    /** Confirm Event. */
    public Builder confirmEvent(@NotNull String eventName, @NotNull EventData data) {
      this.confirmEventName = eventName;
      this.confirmEventData = data;
      return this;
    }

    /** Checks if cel event. */
    public Builder cancelEvent(@NotNull String eventName) {
      this.cancelEventName = eventName;
      return this;
    }

    public Builder cancelEvent(@NotNull String eventName, @NotNull EventData data) {
      this.cancelEventName = eventName;
      this.cancelEventData = data;
      return this;
    }

    /** Builds . */
    public ConfirmationModal build() {
      if (confirmEventData == null) {
        confirmEventData = EventData.of("Button", confirmEventName);
      }
      if (cancelEventData == null) {
        cancelEventData = EventData.of("Button", cancelEventName);
      }
      return new ConfirmationModal(
        title, message, confirmEventName, cancelEventName,
        confirmEventData, cancelEventData
      );
    }
  }

  private ConfirmationModal(String title, String message,
               String confirmEventName, String cancelEventName,
               EventData confirmEventData, EventData cancelEventData) {
    this.title = title;
    this.message = message;
    this.confirmEventName = confirmEventName;
    this.cancelEventName = cancelEventName;
    this.confirmEventData = confirmEventData;
    this.cancelEventData = cancelEventData;
  }

  /** Builds er. */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Renders the confirmation modal into the UI.
   *
   * @param cmd        UI command builder
   * @param events     UI event builder
   * @param targetId   Target element ID where modal should be appended (e.g., "#ModalContainer")
   */
  public void render(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events,
           @NotNull String targetId) {
    // Append modal template
    cmd.append(targetId, UIPaths.MODAL_CONFIRMATION);

    // Set title and message
    cmd.set(targetId + " #ModalTitle.Text", title);
    cmd.set(targetId + " #ModalMessage.Text", message);

    // Bind confirm button
    events.addEventBinding(
      CustomUIEventBindingType.Activating,
      targetId + " #ConfirmBtn",
      confirmEventData,
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
   * Quick helper for dangerous actions (disband, kick, war, etc.).
   * Uses red styling for confirm button.
   */
  public static ConfirmationModal dangerous(String title, String message, String confirmEvent) {
    return builder()
      .title(title)
      .message(message)
      .confirmEvent(confirmEvent)
      .build();
  }

  /**
   * Quick helper for safe confirmations (leave, neutral, etc.).
   * Uses default styling.
   */
  public static ConfirmationModal safe(String title, String message, String confirmEvent) {
    return builder()
      .title(title)
      .message(message)
      .confirmEvent(confirmEvent)
      .build();
  }

  // Getters
  /** Returns the title. */
  public String getTitle() {
    return title;
  }

  /** Returns the message. */
  public String getMessage() {
    return message;
  }

  /** Returns the confirm event name. */
  public String getConfirmEventName() {
    return confirmEventName;
  }

  /** Returns the cancel event name. */
  public String getCancelEventName() {
    return cancelEventName;
  }
}
