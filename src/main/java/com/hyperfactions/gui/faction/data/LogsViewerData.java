package com.hyperfactions.gui.faction.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Data for the Logs Viewer page.
 */
public class LogsViewerData {

  /** The button/action that triggered the event. */
  public String button;

  /** Current page number. */
  public String page;

  /** Filter by log type. */
  public String filterType;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<LogsViewerData> CODEC = BuilderCodec
      .builder(LogsViewerData.class, LogsViewerData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .addField(
          new KeyedCodec<>("Page", Codec.STRING),
          (data, value) -> data.page = value,
          data -> data.page
      )
      .addField(
          new KeyedCodec<>("FilterType", Codec.STRING),
          (data, value) -> data.filterType = value,
          data -> data.filterType
      )
      .build();

  /** Creates a new LogsViewerData. */
  public LogsViewerData() {
  }
}
