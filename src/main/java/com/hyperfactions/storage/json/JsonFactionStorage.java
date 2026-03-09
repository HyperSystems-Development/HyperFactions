package com.hyperfactions.storage.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hyperfactions.data.*;
import com.hyperfactions.storage.FactionStorage;
import com.hyperfactions.storage.StorageHealth;
import com.hyperfactions.storage.StorageUtils;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

/**
 * JSON file-based implementation of FactionStorage.
 * Stores each faction in its own file: data/factions/{uuid}.json
 */
public class JsonFactionStorage implements FactionStorage {

  private final Path dataDir;

  private final Path factionsDir;

  private final Gson gson;

  /** Creates a new JsonFactionStorage. */
  public JsonFactionStorage(@NotNull Path dataDir) {
    this.dataDir = dataDir;
    this.factionsDir = dataDir.resolve("factions");
    this.gson = new GsonBuilder()
      .setPrettyPrinting()
      .disableHtmlEscaping()
      .create();
  }

  /** Initializes this component. */
  @Override
  public CompletableFuture<Void> init() {
    return CompletableFuture.runAsync(() -> {
      try {
        Files.createDirectories(factionsDir);
        // Clean up orphaned temp and backup files from previous crashes
        StorageUtils.cleanupOrphanedFiles(factionsDir);
        Logger.info("[Storage] Faction storage initialized at %s", factionsDir);
      } catch (IOException e) {
        ErrorHandler.report("Failed to create factions directory", e);
      }
    });
  }

  /** Cleans up resources. */
  @Override
  public CompletableFuture<Void> shutdown() {
    return CompletableFuture.completedFuture(null);
  }

  /** Loads faction. */
  @Override
  public CompletableFuture<Optional<Faction>> loadFaction(@NotNull UUID factionId) {
    return CompletableFuture.supplyAsync(() -> {
      Path file = factionsDir.resolve(factionId + ".json");
      if (!Files.exists(file)) {
        // Check if there's a backup we can recover from
        if (StorageUtils.hasBackup(file)) {
          Logger.warn("Faction file %s missing but backup exists, attempting recovery", factionId);
          if (StorageUtils.recoverFromBackup(file)) {
            Logger.info("[Storage] Successfully recovered faction %s from backup", factionId);
          } else {
            return Optional.empty();
          }
        } else {
          return Optional.empty();
        }
      }

      try {
        String json = Files.readString(file);
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        return Optional.of(deserializeFaction(obj));
      } catch (Exception e) {
        ErrorHandler.report(String.format("Failed to load faction %s, attempting backup recovery", factionId), e);
        // Attempt backup recovery on parse failure
        if (StorageUtils.recoverFromBackup(file)) {
          try {
            String json = Files.readString(file);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            Logger.info("[Storage] Successfully loaded faction %s from recovered backup", factionId);
            return Optional.of(deserializeFaction(obj));
          } catch (Exception e2) {
            ErrorHandler.report(String.format("Backup recovery failed for faction %s", factionId), e2);
          }
        }
        return Optional.empty();
      }
    });
  }

  /** Saves faction. */
  @Override
  public CompletableFuture<Void> saveFaction(@NotNull Faction faction) {
    return CompletableFuture.runAsync(() -> {
      Path file = factionsDir.resolve(faction.id() + ".json");
      String filePath = file.toString();

      try {
        JsonObject obj = serializeFaction(faction);
        String content = gson.toJson(obj);

        // Use atomic write for bulletproof data protection
        StorageUtils.WriteResult result = StorageUtils.writeAtomic(file, content);

        if (result instanceof StorageUtils.WriteResult.Success success) {
          StorageHealth.get().recordSuccess(filePath);
          Logger.debug("Saved faction %s (checksum: %s)", faction.name(), success.checksum().substring(0, 8));
        } else if (result instanceof StorageUtils.WriteResult.Failure failure) {
          StorageHealth.get().recordFailure(filePath, failure.error());
          ErrorHandler.report(String.format("Failed to save faction %s: %s", faction.name(), failure.error()), failure.cause());
        }
      } catch (Exception e) {
        StorageHealth.get().recordFailure(filePath, e.getMessage());
        ErrorHandler.report(String.format("Failed to save faction %s", faction.name()), e);
      }
    });
  }

  /** Deletes faction. */
  @Override
  public CompletableFuture<Void> deleteFaction(@NotNull UUID factionId) {
    return CompletableFuture.runAsync(() -> {
      Path file = factionsDir.resolve(factionId + ".json");
      // Delete both the main file and its backup
      StorageUtils.deleteWithBackup(file);
    });
  }

  /** Loads all factions. */
  @Override
  public CompletableFuture<Collection<Faction>> loadAllFactions() {
    return CompletableFuture.supplyAsync(() -> {
      List<Faction> factions = new ArrayList<>();
      List<String> failedFiles = new ArrayList<>();
      int totalFiles = 0;

      if (!Files.exists(factionsDir)) {
        Logger.info("[Storage] Factions directory does not exist yet, no factions to load");
        return factions;
      }

      try (DirectoryStream<Path> stream = Files.newDirectoryStream(factionsDir, "*.json")) {
        for (Path file : stream) {
          totalFiles++;
          try {
            String json = Files.readString(file);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            factions.add(deserializeFaction(obj));
          } catch (Exception e) {
            failedFiles.add(file.getFileName().toString());
            ErrorHandler.report(String.format("Failed to load faction file %s", file.getFileName()), e);
            // Log full stack trace for debugging
            Logger.debug("Stack trace for %s: %s", file.getFileName(), e.toString());
          }
        }
      } catch (IOException e) {
        ErrorHandler.report("CRITICAL: Failed to read factions directory - data may be lost!", e);
        throw new RuntimeException("Failed to read factions directory", e);
      }

      // Report loading results
      if (!failedFiles.isEmpty()) {
        String msg = String.format("WARNING: %d of %d faction files failed to load: %s — these factions will NOT be available until the files are fixed!",
          failedFiles.size(), totalFiles, String.join(", ", failedFiles));
        ErrorHandler.report(msg, (Exception) null);
      }

      if (totalFiles > 0 && factions.isEmpty()) {
        ErrorHandler.report(String.format("CRITICAL: Found %d faction files but loaded 0 factions - possible data corruption!", totalFiles), (Exception) null);
      }

      Logger.info("[Storage] Loaded %d/%d factions successfully", factions.size(), totalFiles);
      return factions;
    });
  }

  // === Serialization ===

  private JsonObject serializeFaction(Faction faction) {
    JsonObject obj = new JsonObject();
    obj.addProperty("id", faction.id().toString());
    obj.addProperty("name", faction.name());
    if (faction.description() != null) {
      obj.addProperty("description", faction.description());
    }
    if (faction.tag() != null) {
      obj.addProperty("tag", faction.tag());
    }
    obj.addProperty("color", faction.color());
    obj.addProperty("createdAt", faction.createdAt());
    obj.addProperty("open", faction.open());

    // Home
    if (faction.home() != null) {
      obj.add("home", serializeHome(faction.home()));
    }

    // Members
    JsonArray members = new JsonArray();
    for (FactionMember member : faction.members().values()) {
      members.add(serializeMember(member));
    }
    obj.add("members", members);

    // Claims
    JsonArray claims = new JsonArray();
    for (FactionClaim claim : faction.claims()) {
      claims.add(serializeClaim(claim));
    }
    obj.add("claims", claims);

    // Relations
    JsonArray relations = new JsonArray();
    for (FactionRelation rel : faction.relations().values()) {
      relations.add(serializeRelation(rel));
    }
    obj.add("relations", relations);

    // Logs (only save last 50 to keep file size reasonable)
    JsonArray logs = new JsonArray();
    int logCount = Math.min(faction.logs().size(), 50);
    for (int i = 0; i < logCount; i++) {
      logs.add(serializeLog(faction.logs().get(i)));
    }
    obj.add("logs", logs);

    // Permissions
    if (faction.permissions() != null) {
      obj.add("permissions", serializePermissions(faction.permissions()));
    }

    // Hardcore power
    if (faction.hardcorePower() != null) {
      obj.addProperty("hardcorePower", faction.hardcorePower());
    }

    return obj;
  }

  private JsonObject serializePermissions(FactionPermissions perms) {
    JsonObject obj = new JsonObject();
    for (Map.Entry<String, Boolean> entry : perms.toMap().entrySet()) {
      obj.addProperty(entry.getKey(), entry.getValue());
    }
    return obj;
  }

  private JsonObject serializeHome(Faction.FactionHome home) {
    JsonObject obj = new JsonObject();
    obj.addProperty("world", home.world());
    obj.addProperty("x", home.x());
    obj.addProperty("y", home.y());
    obj.addProperty("z", home.z());
    obj.addProperty("yaw", home.yaw());
    obj.addProperty("pitch", home.pitch());
    obj.addProperty("setAt", home.setAt());
    obj.addProperty("setBy", home.setBy().toString());
    return obj;
  }

  private JsonObject serializeMember(FactionMember member) {
    JsonObject obj = new JsonObject();
    obj.addProperty("uuid", member.uuid().toString());
    obj.addProperty("username", member.username());
    obj.addProperty("role", member.role().name());
    obj.addProperty("joinedAt", member.joinedAt());
    obj.addProperty("lastOnline", member.lastOnline());
    return obj;
  }

  private JsonObject serializeClaim(FactionClaim claim) {
    JsonObject obj = new JsonObject();
    obj.addProperty("world", claim.world());
    obj.addProperty("chunkX", claim.chunkX());
    obj.addProperty("chunkZ", claim.chunkZ());
    obj.addProperty("claimedAt", claim.claimedAt());
    obj.addProperty("claimedBy", claim.claimedBy().toString());
    return obj;
  }

  private JsonObject serializeRelation(FactionRelation rel) {
    JsonObject obj = new JsonObject();
    obj.addProperty("targetFactionId", rel.targetFactionId().toString());
    obj.addProperty("type", rel.type().name());
    obj.addProperty("since", rel.since());
    return obj;
  }

  private JsonObject serializeLog(FactionLog log) {
    JsonObject obj = new JsonObject();
    obj.addProperty("type", log.type().name());
    obj.addProperty("message", log.message());
    obj.addProperty("timestamp", log.timestamp());
    if (log.actorUuid() != null) {
      obj.addProperty("actorUuid", log.actorUuid().toString());
    }
    return obj;
  }

  // === Deserialization ===

  private Faction deserializeFaction(JsonObject obj) {
    UUID id = UUID.fromString(obj.get("id").getAsString());
    String name = obj.get("name").getAsString();
    String description = obj.has("description") ? obj.get("description").getAsString() : null;
    String tag = obj.has("tag") ? obj.get("tag").getAsString() : null;
    String rawColor = obj.has("color") ? obj.get("color").getAsString() : "#FFFFFF";
    // Auto-migrate legacy single-char color codes to hex
    String color;
    if (rawColor.startsWith("#")) {
      color = rawColor;
    } else if (rawColor.length() == 1) {
      color = com.hyperfactions.util.LegacyColorParser.codeToHex(rawColor.charAt(0));
    } else {
      color = "#FFFFFF";
    }
    long createdAt = obj.get("createdAt").getAsLong();
    boolean open = obj.has("open") && obj.get("open").getAsBoolean();

    // Home
    Faction.FactionHome home = null;
    if (obj.has("home") && obj.get("home").isJsonObject()) {
      home = deserializeHome(obj.getAsJsonObject("home"));
    }

    // Members
    Map<UUID, FactionMember> members = new HashMap<>();
    if (obj.has("members")) {
      for (JsonElement el : obj.getAsJsonArray("members")) {
        FactionMember member = deserializeMember(el.getAsJsonObject());
        members.put(member.uuid(), member);
      }
    }

    // Claims
    Set<FactionClaim> claims = new HashSet<>();
    if (obj.has("claims")) {
      for (JsonElement el : obj.getAsJsonArray("claims")) {
        claims.add(deserializeClaim(el.getAsJsonObject()));
      }
    }

    // Relations
    Map<UUID, FactionRelation> relations = new HashMap<>();
    if (obj.has("relations")) {
      for (JsonElement el : obj.getAsJsonArray("relations")) {
        FactionRelation rel = deserializeRelation(el.getAsJsonObject());
        relations.put(rel.targetFactionId(), rel);
      }
    }

    // Logs
    List<FactionLog> logs = new ArrayList<>();
    if (obj.has("logs")) {
      for (JsonElement el : obj.getAsJsonArray("logs")) {
        logs.add(deserializeLog(el.getAsJsonObject()));
      }
    }

    // Permissions
    FactionPermissions permissions = null;
    if (obj.has("permissions") && obj.get("permissions").isJsonObject()) {
      permissions = deserializePermissions(obj.getAsJsonObject("permissions"));
    }

    // Hardcore power
    Double hardcorePower = obj.has("hardcorePower") ? obj.get("hardcorePower").getAsDouble() : null;

    return new Faction(id, name, description, tag, color, createdAt, home, members, claims, relations, logs, open, permissions, hardcorePower);
  }

  private FactionPermissions deserializePermissions(JsonObject obj) {
    Map<String, Boolean> flags = new HashMap<>();

    // Read all boolean flags from JSON
    for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
      if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isBoolean()) {
        flags.put(entry.getKey(), entry.getValue().getAsBoolean());
      }
    }

    // Backward compatibility: if no officer flags, derive from member flags
    if (!flags.containsKey("officerBreak") && flags.containsKey("memberBreak")) {
      flags.put("officerBreak", flags.get("memberBreak"));
      flags.put("officerPlace", flags.getOrDefault("memberPlace", true));
      flags.put("officerInteract", flags.getOrDefault("memberInteract", true));
      boolean interact = flags.getOrDefault("memberInteract", true);
      flags.put("officerDoorUse", interact);
      flags.put("officerContainerUse", interact);
      flags.put("officerBenchUse", interact);
      flags.put("officerProcessingUse", interact);
      flags.put("officerSeatUse", interact);
    }

    // If no sub-type flags, derive from parent interact flag
    for (String level : FactionPermissions.ALL_LEVELS) {
      String interactKey = level + "Interact";
      if (!flags.containsKey(level + "DoorUse") && flags.containsKey(interactKey)) {
        boolean interact = flags.get(interactKey);
        flags.put(level + "DoorUse", interact);
        flags.put(level + "ContainerUse", interact);
        flags.put(level + "BenchUse", interact);
        flags.put(level + "ProcessingUse", interact);
        flags.put(level + "SeatUse", interact);
      }
    }

    // Migrate old teleporterUse/portalUse → transportUse
    for (String level : FactionPermissions.ALL_LEVELS) {
      String oldTeleporter = level + "TeleporterUse";
      String oldPortal = level + "PortalUse";
      String newTransport = level + "TransportUse";
      if (!flags.containsKey(newTransport)) {
        boolean t = flags.getOrDefault(oldTeleporter, false);
        boolean p = flags.getOrDefault(oldPortal, false);
        flags.put(newTransport, t || p);
      }
      flags.remove(oldTeleporter);
      flags.remove(oldPortal);
    }

    // If no mob spawning flags, default to blocking all in claims
    if (!flags.containsKey("mobSpawning")) {
      flags.put("mobSpawning", false);
      flags.put("hostileMobSpawning", false);
      flags.put("passiveMobSpawning", true);
      flags.put("neutralMobSpawning", true);
    }

    // Constructor fills remaining missing flags from defaults
    return new FactionPermissions(flags);
  }

  private Faction.FactionHome deserializeHome(JsonObject obj) {
    return new Faction.FactionHome(
      obj.get("world").getAsString(),
      obj.get("x").getAsDouble(),
      obj.get("y").getAsDouble(),
      obj.get("z").getAsDouble(),
      obj.get("yaw").getAsFloat(),
      obj.get("pitch").getAsFloat(),
      obj.get("setAt").getAsLong(),
      UUID.fromString(obj.get("setBy").getAsString())
    );
  }

  private FactionMember deserializeMember(JsonObject obj) {
    return new FactionMember(
      UUID.fromString(obj.get("uuid").getAsString()),
      obj.get("username").getAsString(),
      FactionRole.valueOf(obj.get("role").getAsString()),
      obj.get("joinedAt").getAsLong(),
      obj.get("lastOnline").getAsLong()
    );
  }

  private FactionClaim deserializeClaim(JsonObject obj) {
    return new FactionClaim(
      obj.get("world").getAsString(),
      obj.get("chunkX").getAsInt(),
      obj.get("chunkZ").getAsInt(),
      obj.get("claimedAt").getAsLong(),
      UUID.fromString(obj.get("claimedBy").getAsString())
    );
  }

  private FactionRelation deserializeRelation(JsonObject obj) {
    return new FactionRelation(
      UUID.fromString(obj.get("targetFactionId").getAsString()),
      RelationType.valueOf(obj.get("type").getAsString()),
      obj.get("since").getAsLong()
    );
  }

  private FactionLog deserializeLog(JsonObject obj) {
    UUID actorUuid = obj.has("actorUuid") ? UUID.fromString(obj.get("actorUuid").getAsString()) : null;
    return new FactionLog(
      FactionLog.LogType.valueOf(obj.get("type").getAsString()),
      obj.get("message").getAsString(),
      obj.get("timestamp").getAsLong(),
      actorUuid
    );
  }
}
