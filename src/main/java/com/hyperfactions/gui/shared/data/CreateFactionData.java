package com.hyperfactions.gui.shared.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Data for the Create Faction page.
 */
public class CreateFactionData {

  /** The button/action that triggered the event. */
  public String button;

  /** Faction name input. */
  public String name;

  /** Faction description input. */
  public String description;

  /** Selected color hex code. */
  public String color;

  /** Whether faction is open (true) or invite-only (false). */
  public String isOpen;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<CreateFactionData> CODEC = BuilderCodec
      .builder(CreateFactionData.class, CreateFactionData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .addField(
          new KeyedCodec<>("Name", Codec.STRING),
          (data, value) -> data.name = value,
          data -> data.name
      )
      .addField(
          new KeyedCodec<>("Description", Codec.STRING),
          (data, value) -> data.description = value,
          data -> data.description
      )
      .addField(
          new KeyedCodec<>("Color", Codec.STRING),
          (data, value) -> data.color = value,
          data -> data.color
      )
      .addField(
          new KeyedCodec<>("IsOpen", Codec.STRING),
          (data, value) -> data.isOpen = value,
          data -> data.isOpen
      )
      .build();

  /** Creates a new CreateFactionData. */
  public CreateFactionData() {
  }
}
