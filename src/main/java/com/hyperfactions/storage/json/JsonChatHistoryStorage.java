package com.hyperfactions.storage.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hyperfactions.data.ChatMessage;
import com.hyperfactions.data.FactionChatHistory;
import com.hyperfactions.storage.ChatHistoryStorage;
import com.hyperfactions.storage.StorageHealth;
import com.hyperfactions.storage.StorageUtils;
import com.hyperfactions.util.Logger;
import com.hyperfactions.util.UuidUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * JSON file-based implementation of ChatHistoryStorage.
 * Stores each faction's chat history in: data/chat/{uuid}.json
 */
public class JsonChatHistoryStorage implements ChatHistoryStorage {

    private final Path chatDir;
    private final Gson gson;

    public JsonChatHistoryStorage(@NotNull Path dataDir) {
        this.chatDir = dataDir.resolve("chat");
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    }

    @Override
    public CompletableFuture<Void> init() {
        return CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(chatDir);
                StorageUtils.cleanupOrphanedFiles(chatDir);
                Logger.info("[Storage] Chat history storage initialized at %s", chatDir);
            } catch (IOException e) {
                Logger.severe("Failed to create chat history directory", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<FactionChatHistory> loadHistory(@NotNull UUID factionId) {
        return CompletableFuture.supplyAsync(() -> {
            Path file = chatDir.resolve(factionId + ".json");
            if (!Files.exists(file)) {
                if (StorageUtils.hasBackup(file)) {
                    Logger.warn("Chat history file %s missing but backup exists, attempting recovery", factionId);
                    if (!StorageUtils.recoverFromBackup(file)) {
                        return FactionChatHistory.empty(factionId);
                    }
                } else {
                    return FactionChatHistory.empty(factionId);
                }
            }

            try {
                String json = Files.readString(file);
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                return deserializeHistory(factionId, obj);
            } catch (Exception e) {
                Logger.severe("Failed to load chat history for %s, attempting backup recovery", e, factionId);
                if (StorageUtils.recoverFromBackup(file)) {
                    try {
                        String json = Files.readString(file);
                        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                        return deserializeHistory(factionId, obj);
                    } catch (Exception e2) {
                        Logger.severe("Backup recovery failed for chat history %s", e2, factionId);
                    }
                }
                return FactionChatHistory.empty(factionId);
            }
        });
    }

    @Override
    public CompletableFuture<Void> saveHistory(@NotNull FactionChatHistory history) {
        return CompletableFuture.runAsync(() -> {
            Path file = chatDir.resolve(history.factionId() + ".json");
            String filePath = file.toString();

            try {
                JsonObject obj = serializeHistory(history);
                String content = gson.toJson(obj);

                StorageUtils.WriteResult result = StorageUtils.writeAtomic(file, content);

                if (result instanceof StorageUtils.WriteResult.Success success) {
                    StorageHealth.get().recordSuccess(filePath);
                    Logger.debug("Saved chat history for %s (%d messages)", history.factionId(), history.size());
                } else if (result instanceof StorageUtils.WriteResult.Failure failure) {
                    StorageHealth.get().recordFailure(filePath, failure.error());
                    Logger.severe("Failed to save chat history for %s: %s", history.factionId(), failure.error());
                }
            } catch (Exception e) {
                StorageHealth.get().recordFailure(filePath, e.getMessage());
                Logger.severe("Failed to save chat history for %s", e, history.factionId());
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteHistory(@NotNull UUID factionId) {
        return CompletableFuture.runAsync(() -> {
            Path file = chatDir.resolve(factionId + ".json");
            StorageUtils.deleteWithBackup(file);
            Logger.debug("Deleted chat history for %s", factionId);
        });
    }

    @Override
    public CompletableFuture<List<UUID>> listAllFactionIds() {
        return CompletableFuture.supplyAsync(() -> {
            List<UUID> ids = new ArrayList<>();
            if (!Files.exists(chatDir)) {
                return ids;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(chatDir, "*.json")) {
                for (Path file : stream) {
                    String fileName = file.getFileName().toString();
                    String uuidStr = fileName.substring(0, fileName.length() - 5); // strip .json
                    UUID uuid = UuidUtil.parseOrNull(uuidStr);
                    if (uuid != null) {
                        ids.add(uuid);
                    } else {
                        Logger.warn("Skipping non-UUID chat history file: %s", fileName);
                    }
                }
            } catch (IOException e) {
                Logger.severe("Failed to list chat history files", e);
            }

            return ids;
        });
    }

    // === Serialization ===

    private JsonObject serializeHistory(@NotNull FactionChatHistory history) {
        JsonObject obj = new JsonObject();
        obj.addProperty("factionId", history.factionId().toString());

        JsonArray messages = new JsonArray();
        for (ChatMessage msg : history.messages()) {
            messages.add(serializeMessage(msg));
        }
        obj.add("messages", messages);

        return obj;
    }

    private JsonObject serializeMessage(@NotNull ChatMessage msg) {
        JsonObject obj = new JsonObject();
        obj.addProperty("senderId", msg.senderId().toString());
        obj.addProperty("senderName", msg.senderName());
        obj.addProperty("senderFactionTag", msg.senderFactionTag());
        obj.addProperty("channel", msg.channel().name());
        obj.addProperty("message", msg.message());
        obj.addProperty("timestamp", msg.timestamp());
        return obj;
    }

    // === Deserialization ===

    private FactionChatHistory deserializeHistory(@NotNull UUID factionId, @NotNull JsonObject obj) {
        List<ChatMessage> messages = new ArrayList<>();

        if (obj.has("messages")) {
            for (JsonElement el : obj.getAsJsonArray("messages")) {
                messages.add(deserializeMessage(el.getAsJsonObject()));
            }
        }

        return new FactionChatHistory(factionId, messages);
    }

    private ChatMessage deserializeMessage(@NotNull JsonObject obj) {
        return new ChatMessage(
            UUID.fromString(obj.get("senderId").getAsString()),
            obj.get("senderName").getAsString(),
            obj.get("senderFactionTag").getAsString(),
            ChatMessage.Channel.valueOf(obj.get("channel").getAsString()),
            obj.get("message").getAsString(),
            obj.get("timestamp").getAsLong()
        );
    }
}
