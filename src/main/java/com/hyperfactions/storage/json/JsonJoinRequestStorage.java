package com.hyperfactions.storage.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hyperfactions.data.JoinRequest;
import com.hyperfactions.storage.JoinRequestStorage;
import com.hyperfactions.storage.StorageUtils;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

/**
 * Stores pending join requests in {@code data/join_requests.json}.
 */
public class JsonJoinRequestStorage implements JoinRequestStorage {

  private final Path dataFile;
  private final Gson gson;

  public JsonJoinRequestStorage(@NotNull Path dataDir) {
    this.dataFile = dataDir.resolve("join_requests.json");
    this.gson = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();
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
  public CompletableFuture<List<JoinRequest>> loadAll() {
    return CompletableFuture.supplyAsync(() -> {
      List<JoinRequest> requests = new ArrayList<>();

      if (!Files.exists(dataFile)) {
        Logger.info("[Storage] No join requests file found, starting fresh");
        return requests;
      }

      try {
        String json = Files.readString(dataFile);
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();

        int expired = 0;
        for (JsonElement el : array) {
          JoinRequest request = deserialize(el.getAsJsonObject());
          if (request.isExpired()) {
            expired++;
            continue;
          }
          requests.add(request);
        }

        Logger.info("[Storage] Loaded %d join requests (%d expired and skipped)", requests.size(), expired);
      } catch (Exception e) {
        ErrorHandler.report("Failed to load join requests", e);
      }

      return requests;
    });
  }

  @Override
  public CompletableFuture<Void> saveAll(Collection<JoinRequest> requests) {
    return CompletableFuture.runAsync(() -> {
      try {
        Files.createDirectories(dataFile.getParent());
      } catch (IOException e) {
        ErrorHandler.report("Failed to create join requests directory", e);
        return;
      }

      JsonArray array = new JsonArray();
      for (JoinRequest request : requests) {
        if (!request.isExpired()) {
          array.add(serialize(request));
        }
      }

      StorageUtils.WriteResult result = StorageUtils.writeAtomic(dataFile, gson.toJson(array));
      if (result instanceof StorageUtils.WriteResult.Failure failure) {
        ErrorHandler.report(String.format("Failed to save join requests: %s", failure.error()), failure.cause());
      }
    });
  }

  private JsonObject serialize(JoinRequest request) {
    JsonObject obj = new JsonObject();
    obj.addProperty("factionId", request.factionId().toString());
    obj.addProperty("playerUuid", request.playerUuid().toString());
    obj.addProperty("playerName", request.playerName());
    if (request.message() != null) {
      obj.addProperty("message", request.message());
    }
    obj.addProperty("createdAt", request.createdAt());
    obj.addProperty("expiresAt", request.expiresAt());
    return obj;
  }

  private JoinRequest deserialize(JsonObject obj) {
    return new JoinRequest(
        UUID.fromString(obj.get("factionId").getAsString()),
        UUID.fromString(obj.get("playerUuid").getAsString()),
        obj.get("playerName").getAsString(),
        obj.has("message") ? obj.get("message").getAsString() : null,
        obj.get("createdAt").getAsLong(),
        obj.get("expiresAt").getAsLong()
    );
  }
}
