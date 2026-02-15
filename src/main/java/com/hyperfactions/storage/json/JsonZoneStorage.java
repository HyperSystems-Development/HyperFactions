package com.hyperfactions.storage.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hyperfactions.data.ChunkKey;
import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneFlags;
import com.hyperfactions.data.ZoneType;
import com.hyperfactions.storage.StorageHealth;
import com.hyperfactions.storage.StorageUtils;
import com.hyperfactions.storage.ZoneStorage;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * JSON file-based implementation of ZoneStorage.
 * Stores all zones in a single file: data/zones.json
 *
 * Supports both old single-chunk format (for migration) and new multi-chunk format.
 */
public class JsonZoneStorage implements ZoneStorage {

    private final Path dataDir;
    private final Path zonesFile;
    private final Gson gson;

    public JsonZoneStorage(@NotNull Path dataDir) {
        this.dataDir = dataDir;
        this.zonesFile = dataDir.resolve("zones.json");
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    }

    @Override
    public CompletableFuture<Void> init() {
        return CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(dataDir);
                // Clean up orphaned temp files from previous crashes
                StorageUtils.cleanupOrphanedFiles(dataDir);
                Logger.info("Zone storage initialized");
            } catch (IOException e) {
                Logger.severe("Failed to create data directory", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Collection<Zone>> loadAllZones() {
        return CompletableFuture.supplyAsync(() -> {
            List<Zone> zones = new ArrayList<>();
            List<String> failedZones = new ArrayList<>();

            if (!Files.exists(zonesFile)) {
                // Check if there's a backup we can recover from
                if (StorageUtils.hasBackup(zonesFile)) {
                    Logger.warn("Zones file missing but backup exists, attempting recovery");
                    if (StorageUtils.recoverFromBackup(zonesFile)) {
                        Logger.info("Successfully recovered zones file from backup");
                    } else {
                        Logger.info("Zones file does not exist yet, no zones to load");
                        return zones;
                    }
                } else {
                    Logger.info("Zones file does not exist yet, no zones to load");
                    return zones;
                }
            }

            try {
                String json = Files.readString(zonesFile);
                JsonArray array = JsonParser.parseString(json).getAsJsonArray();
                int totalZones = array.size();

                for (JsonElement el : array) {
                    try {
                        zones.add(deserializeZone(el.getAsJsonObject()));
                    } catch (Exception e) {
                        // Try to get zone name for better error reporting
                        String zoneName = "unknown";
                        try {
                            if (el.isJsonObject() && el.getAsJsonObject().has("name")) {
                                zoneName = el.getAsJsonObject().get("name").getAsString();
                            }
                        } catch (Exception ignored) {}
                        failedZones.add(zoneName);
                        Logger.severe("Failed to parse zone '%s': %s", zoneName, e.getMessage());
                    }
                }

                // Report loading results
                if (!failedZones.isEmpty()) {
                    Logger.severe("WARNING: %d of %d zones failed to load: %s",
                        failedZones.size(), totalZones, String.join(", ", failedZones));
                }

                if (totalZones > 0 && zones.isEmpty()) {
                    Logger.severe("CRITICAL: Found %d zones in file but loaded 0 - possible data corruption!", totalZones);
                    // Attempt backup recovery
                    if (StorageUtils.recoverFromBackup(zonesFile)) {
                        Logger.info("Attempting to load zones from recovered backup");
                        return loadAllZones().join(); // Recursive call with recovered file
                    }
                }

            } catch (Exception e) {
                Logger.severe("CRITICAL: Failed to load zones file, attempting backup recovery", e);
                // Attempt backup recovery on parse failure
                if (StorageUtils.recoverFromBackup(zonesFile)) {
                    try {
                        String json = Files.readString(zonesFile);
                        JsonArray array = JsonParser.parseString(json).getAsJsonArray();
                        for (JsonElement el : array) {
                            try {
                                zones.add(deserializeZone(el.getAsJsonObject()));
                            } catch (Exception ignored) {}
                        }
                        Logger.info("Successfully loaded %d zones from recovered backup", zones.size());
                        return zones;
                    } catch (Exception e2) {
                        Logger.severe("Backup recovery failed for zones file", e2);
                    }
                }
                throw new RuntimeException("Failed to load zones file", e);
            }

            Logger.info("Loaded %d zones", zones.size());
            return zones;
        });
    }

    @Override
    public CompletableFuture<Void> saveAllZones(@NotNull Collection<Zone> zones) {
        return CompletableFuture.runAsync(() -> {
            String filePath = zonesFile.toString();

            try {
                JsonArray array = new JsonArray();
                for (Zone zone : zones) {
                    array.add(serializeZone(zone));
                }
                String content = gson.toJson(array);

                // Pre-write validation: ensure we can parse what we're about to write
                try {
                    JsonArray validation = JsonParser.parseString(content).getAsJsonArray();
                    if (validation.size() != zones.size()) {
                        String error = String.format("Pre-write validation failed: expected %d zones, got %d in JSON",
                            zones.size(), validation.size());
                        Logger.severe("[ZoneStorage] %s", error);
                        StorageHealth.get().recordFailure(filePath, error);
                        return;
                    }
                } catch (Exception e) {
                    String error = "Pre-write validation failed: generated JSON is not valid: " + e.getMessage();
                    Logger.severe("[ZoneStorage] %s", error);
                    StorageHealth.get().recordFailure(filePath, error);
                    return;
                }

                // Use atomic write for bulletproof data protection
                StorageUtils.WriteResult result = StorageUtils.writeAtomic(zonesFile, content);

                if (result instanceof StorageUtils.WriteResult.Success success) {
                    StorageHealth.get().recordSuccess(filePath);
                    Logger.debug("Saved %d zones (checksum: %s)", zones.size(), success.checksum().substring(0, 8));
                } else if (result instanceof StorageUtils.WriteResult.Failure failure) {
                    StorageHealth.get().recordFailure(filePath, failure.error());
                    Logger.severe("Failed to save zones: %s", failure.error());
                }
            } catch (Exception e) {
                StorageHealth.get().recordFailure(filePath, e.getMessage());
                Logger.severe("Failed to save zones", e);
            }
        });
    }

    private JsonObject serializeZone(Zone zone) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", zone.id().toString());
        obj.addProperty("name", zone.name());
        obj.addProperty("type", zone.type().name());
        obj.addProperty("world", zone.world());

        // Serialize chunks as array
        JsonArray chunksArray = new JsonArray();
        for (ChunkKey chunk : zone.chunks()) {
            JsonObject chunkObj = new JsonObject();
            chunkObj.addProperty("x", chunk.chunkX());
            chunkObj.addProperty("z", chunk.chunkZ());
            chunksArray.add(chunkObj);
        }
        obj.add("chunks", chunksArray);

        obj.addProperty("createdAt", zone.createdAt());
        obj.addProperty("createdBy", zone.createdBy().toString());

        // Serialize flags if present
        if (zone.flags() != null && !zone.flags().isEmpty()) {
            JsonObject flagsObj = new JsonObject();
            for (Map.Entry<String, Boolean> entry : zone.flags().entrySet()) {
                flagsObj.addProperty(entry.getKey(), entry.getValue());
            }
            obj.add("flags", flagsObj);
        }

        return obj;
    }

    private Zone deserializeZone(JsonObject obj) {
        UUID id = UUID.fromString(obj.get("id").getAsString());
        String name = obj.get("name").getAsString();
        ZoneType type = ZoneType.valueOf(obj.get("type").getAsString());
        String world = obj.get("world").getAsString();
        long createdAt = obj.get("createdAt").getAsLong();
        UUID createdBy = UUID.fromString(obj.get("createdBy").getAsString());

        // Deserialize chunks - support both old and new format
        Set<ChunkKey> chunks = new HashSet<>();

        if (obj.has("chunks") && obj.get("chunks").isJsonArray()) {
            // New multi-chunk format
            JsonArray chunksArray = obj.getAsJsonArray("chunks");
            for (JsonElement el : chunksArray) {
                JsonObject chunkObj = el.getAsJsonObject();
                int x = chunkObj.get("x").getAsInt();
                int z = chunkObj.get("z").getAsInt();
                chunks.add(new ChunkKey(world, x, z));
            }
        } else if (obj.has("chunkX") && obj.has("chunkZ")) {
            // Old single-chunk format (migration support)
            int chunkX = obj.get("chunkX").getAsInt();
            int chunkZ = obj.get("chunkZ").getAsInt();
            chunks.add(new ChunkKey(world, chunkX, chunkZ));
            Logger.info("Migrated zone '%s' from single-chunk to multi-chunk format", name);
        }

        // Deserialize flags if present
        Map<String, Boolean> flags = null;
        if (obj.has("flags") && obj.get("flags").isJsonObject()) {
            flags = new HashMap<>();
            JsonObject flagsObj = obj.getAsJsonObject("flags");
            for (String key : flagsObj.keySet()) {
                flags.put(key, flagsObj.get(key).getAsBoolean());
            }
        }

        // Migration: Remove obsolete flag keys that were replaced by new flags
        if (flags != null) {
            boolean migrated = false;
            // Migrate old container_access and interact_allowed to new block_interact
            if (flags.containsKey("container_access")) {
                flags.remove("container_access");
                migrated = true;
            }
            if (flags.containsKey("interact_allowed")) {
                flags.remove("interact_allowed");
                migrated = true;
            }
            if (migrated) {
                Logger.info("Migrated zone '%s': removed obsolete flag keys", name);
            }
            // If flags map is now empty, set to null
            if (flags.isEmpty()) {
                flags = null;
            }
        }

        return new Zone(id, name, type, world, chunks, createdAt, createdBy, flags);
    }
}
