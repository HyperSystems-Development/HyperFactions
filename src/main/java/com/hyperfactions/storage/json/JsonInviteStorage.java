package com.hyperfactions.storage.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hyperfactions.data.PendingInvite;
import com.hyperfactions.storage.InviteStorage;
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
 * Stores pending faction invites in {@code data/invites.json}.
 */
public class JsonInviteStorage implements InviteStorage {

  private final Path dataFile;
  private final Gson gson;

  public JsonInviteStorage(@NotNull Path dataDir) {
    this.dataFile = dataDir.resolve("invites.json");
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
  public CompletableFuture<List<PendingInvite>> loadAll() {
    return CompletableFuture.supplyAsync(() -> {
      List<PendingInvite> invites = new ArrayList<>();

      if (!Files.exists(dataFile)) {
        Logger.info("[Storage] No invites file found, starting fresh");
        return invites;
      }

      try {
        String json = Files.readString(dataFile);
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();

        int expired = 0;
        for (JsonElement el : array) {
          PendingInvite invite = deserialize(el.getAsJsonObject());
          if (invite.isExpired()) {
            expired++;
            continue;
          }
          invites.add(invite);
        }

        Logger.info("[Storage] Loaded %d invites (%d expired and skipped)", invites.size(), expired);
      } catch (Exception e) {
        ErrorHandler.report("Failed to load invites", e);
      }

      return invites;
    });
  }

  @Override
  public CompletableFuture<Void> saveAll(Collection<PendingInvite> invites) {
    return CompletableFuture.runAsync(() -> {
      try {
        Files.createDirectories(dataFile.getParent());
      } catch (IOException e) {
        ErrorHandler.report("Failed to create invites directory", e);
        return;
      }

      JsonArray array = new JsonArray();
      for (PendingInvite invite : invites) {
        if (!invite.isExpired()) {
          array.add(serialize(invite));
        }
      }

      StorageUtils.WriteResult result = StorageUtils.writeAtomic(dataFile, gson.toJson(array));
      if (result instanceof StorageUtils.WriteResult.Failure failure) {
        ErrorHandler.report(String.format("Failed to save invites: %s", failure.error()), failure.cause());
      }
    });
  }

  private JsonObject serialize(PendingInvite invite) {
    JsonObject obj = new JsonObject();
    obj.addProperty("factionId", invite.factionId().toString());
    obj.addProperty("playerUuid", invite.playerUuid().toString());
    obj.addProperty("invitedBy", invite.invitedBy().toString());
    obj.addProperty("createdAt", invite.createdAt());
    obj.addProperty("expiresAt", invite.expiresAt());
    return obj;
  }

  private PendingInvite deserialize(JsonObject obj) {
    return new PendingInvite(
        UUID.fromString(obj.get("factionId").getAsString()),
        UUID.fromString(obj.get("playerUuid").getAsString()),
        UUID.fromString(obj.get("invitedBy").getAsString()),
        obj.get("createdAt").getAsLong(),
        obj.get("expiresAt").getAsLong()
    );
  }
}
