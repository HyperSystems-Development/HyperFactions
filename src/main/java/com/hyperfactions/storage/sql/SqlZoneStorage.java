package com.hyperfactions.storage.sql;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hyperfactions.data.ChunkKey;
import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneType;
import com.hyperfactions.storage.ZoneStorage;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;

/**
 * SQL implementation of {@link ZoneStorage}.
 */
public class SqlZoneStorage implements ZoneStorage {

  private final Jdbi jdbi;
  private final String zonesTable;
  private final String chunksTable;
  private final Gson gson;

  public SqlZoneStorage(@NotNull Jdbi jdbi, @NotNull String prefix) {
    this.jdbi = jdbi;
    this.zonesTable = SqlHelper.table(prefix, "zones");
    this.chunksTable = SqlHelper.table(prefix, "zone_chunks");
    this.gson = new GsonBuilder().disableHtmlEscaping().create();
  }

  @Override
  public CompletableFuture<Void> init() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> shutdown() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  @NotNull
  public CompletableFuture<Collection<Zone>> loadAllZones() {
    return CompletableFuture.supplyAsync(() -> {
      try {
        // Load all zone rows
        Map<UUID, Zone> zonesById = new LinkedHashMap<>();
        jdbi.withHandle(handle -> {
          handle.createQuery("SELECT * FROM " + zonesTable)
              .map((rs, ctx) -> {
                UUID id = UUID.fromString(rs.getString("id"));
                String flagsJson = rs.getString("flags");
                String settingsJson = rs.getString("settings");
                Map<String, Boolean> flags = flagsJson != null
                    ? gson.fromJson(flagsJson, new TypeToken<Map<String, Boolean>>(){}.getType())
                    : null;
                Map<String, String> settings = settingsJson != null
                    ? gson.fromJson(settingsJson, new TypeToken<Map<String, String>>(){}.getType())
                    : null;

                Zone zone = new Zone(
                    id,
                    rs.getString("name"),
                    ZoneType.valueOf(rs.getString("type")),
                    rs.getString("world"),
                    new HashSet<>(), // chunks loaded separately
                    rs.getLong("created_at"),
                    UUID.fromString(rs.getString("created_by")),
                    flags,
                    settings,
                    rs.getObject("notify_on_entry") != null ? rs.getBoolean("notify_on_entry") : null,
                    rs.getString("notify_title_upper"),
                    rs.getString("notify_title_lower")
                );
                zonesById.put(id, zone);
                return zone;
              })
              .list();
          return null;
        });

        // Load all chunks and attach to zones
        if (!zonesById.isEmpty()) {
          jdbi.withHandle(handle -> {
            handle.createQuery("SELECT * FROM " + chunksTable)
                .map((rs, ctx) -> {
                  UUID zoneId = UUID.fromString(rs.getString("zone_id"));
                  Zone zone = zonesById.get(zoneId);
                  if (zone != null) {
                    zone.chunks().add(new ChunkKey(
                        rs.getString("world"),
                        rs.getInt("chunk_x"),
                        rs.getInt("chunk_z")
                    ));
                  }
                  return null;
                })
                .list();
            return null;
          });
        }

        Logger.info("[Storage] Loaded %d zones from SQL", zonesById.size());
        return zonesById.values();
      } catch (Exception e) {
        ErrorHandler.report("Failed to load zones from SQL", e);
        return List.of();
      }
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Void> saveAllZones(@NotNull Collection<Zone> zones) {
    return CompletableFuture.runAsync(() -> {
      try {
        jdbi.useTransaction(handle -> {
          // Truncate and reinsert (zones are always saved as a complete set)
          handle.execute("DELETE FROM " + chunksTable);
          handle.execute("DELETE FROM " + zonesTable);

          if (zones.isEmpty()) {
            return;
          }

          // Insert zones
          var zoneBatch = handle.prepareBatch(
              "INSERT INTO " + zonesTable
                  + " (id, name, type, world, created_at, created_by, flags, settings,"
                  + " notify_on_entry, notify_title_upper, notify_title_lower)"
                  + " VALUES (:id, :name, :type, :world, :createdAt, :createdBy, :flags, :settings,"
                  + " :notifyOnEntry, :notifyTitleUpper, :notifyTitleLower)");
          for (Zone zone : zones) {
            zoneBatch
                .bind("id", zone.id())
                .bind("name", zone.name())
                .bind("type", zone.type().name())
                .bind("world", zone.world())
                .bind("createdAt", zone.createdAt())
                .bind("createdBy", zone.createdBy())
                .bind("flags", zone.flags() != null ? gson.toJson(zone.flags()) : null)
                .bind("settings", zone.settings() != null ? gson.toJson(zone.settings()) : null)
                .bind("notifyOnEntry", zone.notifyOnEntry())
                .bind("notifyTitleUpper", zone.notifyTitleUpper())
                .bind("notifyTitleLower", zone.notifyTitleLower())
                .add();
          }
          zoneBatch.execute();

          // Insert chunks
          var chunkBatch = handle.prepareBatch(
              "INSERT INTO " + chunksTable
                  + " (world, chunk_x, chunk_z, zone_id)"
                  + " VALUES (:world, :chunkX, :chunkZ, :zoneId)");
          for (Zone zone : zones) {
            for (ChunkKey chunk : zone.chunks()) {
              chunkBatch
                  .bind("world", chunk.world())
                  .bind("chunkX", chunk.chunkX())
                  .bind("chunkZ", chunk.chunkZ())
                  .bind("zoneId", zone.id())
                  .add();
            }
          }
          if (zones.stream().anyMatch(z -> !z.chunks().isEmpty())) {
            chunkBatch.execute();
          }
        });

        Logger.debug("Saved %d zones to SQL", zones.size());
      } catch (Exception e) {
        ErrorHandler.report("Failed to save zones to SQL", e);
      }
    });
  }
}
