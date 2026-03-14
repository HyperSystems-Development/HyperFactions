package com.hyperfactions.gui.admin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Event data for the Admin Backups page.
 */
public class AdminBackupsData implements AdminNavAwareData {

  /** The button/action that triggered the event. */
  public String button;

  /** Admin nav bar target (for navigation). */
  public String adminNavBar;

  /** Backup name for expand/restore/delete targets. */
  public String backupName;

  /** Dynamic text field value for manual backup name. */
  public String backupInputName;

  /** Page number for pagination (as String, parsed to int). */
  public int page;

  /** Codec for serialization/deserialization. */
  public static final BuilderCodec<AdminBackupsData> CODEC = BuilderCodec
      .builder(AdminBackupsData.class, AdminBackupsData::new)
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
          new KeyedCodec<>("BackupName", Codec.STRING),
          (data, value) -> data.backupName = value,
          data -> data.backupName
      )
      .addField(
          new KeyedCodec<>("@BackupInputName", Codec.STRING),
          (data, value) -> data.backupInputName = value,
          data -> data.backupInputName
      )
      .addField(
          new KeyedCodec<>("Page", Codec.STRING),
          (data, value) -> {
            try {
              data.page = Integer.parseInt(value);
            } catch (NumberFormatException e) {
              data.page = 0;
            }
          },
          data -> String.valueOf(data.page)
      )
      .build();

  /** Creates a new AdminBackupsData. */
  public AdminBackupsData() {
  }

  /** Returns the admin nav bar. */
  @Override
  @Nullable
  public String getAdminNavBar() {
    return adminNavBar;
  }
}
