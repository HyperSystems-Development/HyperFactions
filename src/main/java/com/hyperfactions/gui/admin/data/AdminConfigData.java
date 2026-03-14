package com.hyperfactions.gui.admin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Event data for the Admin Config editor page.
 */
public class AdminConfigData implements AdminNavAwareData {

  /** The button/action that triggered the event. */
  public String button;

  /** Admin nav bar target (for navigation). */
  public String adminNavBar;

  /** The tab being switched to. */
  public String tab;

  /** The setting key being changed. */
  public String settingKey;

  /** The setting value (for text input). */
  public String settingValue;

  /** Dynamic text input from text fields. */
  public String textInput;

  /** Dynamic numeric input from number text fields. */
  public String numInput;

  /** Dynamic enum/dropdown selection value. */
  public String enumValue;

  /** Dynamic string input from text fields. */
  public String strInput;

  /** Dynamic color value from ColorPickerDropdownBox. */
  public String colorValue;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<AdminConfigData> CODEC = BuilderCodec
      .builder(AdminConfigData.class, AdminConfigData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .addField(
          new KeyedCodec<>("AdminNavBar", Codec.STRING),
          (data, value) -> data.adminNavBar = value,
          data -> data.adminNavBar
      )
      .addField(
          new KeyedCodec<>("Tab", Codec.STRING),
          (data, value) -> data.tab = value,
          data -> data.tab
      )
      .addField(
          new KeyedCodec<>("SettingKey", Codec.STRING),
          (data, value) -> data.settingKey = value,
          data -> data.settingKey
      )
      .addField(
          new KeyedCodec<>("SettingValue", Codec.STRING),
          (data, value) -> data.settingValue = value,
          data -> data.settingValue
      )
      .addField(
          new KeyedCodec<>("@textInput", Codec.STRING),
          (data, value) -> data.textInput = value,
          data -> data.textInput
      )
      .addField(
          new KeyedCodec<>("@numInput", Codec.STRING),
          (data, value) -> data.numInput = value,
          data -> data.numInput
      )
      .addField(
          new KeyedCodec<>("@enumValue", Codec.STRING),
          (data, value) -> data.enumValue = value,
          data -> data.enumValue
      )
      .addField(
          new KeyedCodec<>("@strInput", Codec.STRING),
          (data, value) -> data.strInput = value,
          data -> data.strInput
      )
      .addField(
          new KeyedCodec<>("@colorValue", Codec.STRING),
          (data, value) -> data.colorValue = value,
          data -> data.colorValue
      )
      .build();

  /** Creates a new AdminConfigData. */
  public AdminConfigData() {
  }

  /** Returns the admin nav bar. */
  @Override
  @Nullable
  public String getAdminNavBar() {
    return adminNavBar;
  }
}
