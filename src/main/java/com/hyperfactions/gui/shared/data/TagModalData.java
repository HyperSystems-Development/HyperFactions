package com.hyperfactions.gui.shared.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Event data for the Tag modal.
 */
public class TagModalData {

  /** The button/action that triggered the event. */
  public String button;

  /** The new faction tag entered by user. */
  public String tag;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<TagModalData> CODEC = BuilderCodec
      .builder(TagModalData.class, TagModalData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .addField(
          new KeyedCodec<>("@Tag", Codec.STRING),
          (data, value) -> data.tag = value,
          data -> data.tag
      )
      .build();

  /** Creates a new TagModalData. */
  public TagModalData() {
  }
}
