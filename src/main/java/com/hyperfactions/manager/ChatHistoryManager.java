package com.hyperfactions.manager;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.ChatMessage;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionChatHistory;
import com.hyperfactions.data.FactionRelation;
import com.hyperfactions.storage.ChatHistoryStorage;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Manages in-memory chat history cache with debounced persistence.
 * <p>
 * Histories are loaded on demand and evicted when no online members remain.
 * Saves are debounced (5s delay) to batch rapid messages into single writes.
 */
public class ChatHistoryManager {

    private static final long DEBOUNCE_DELAY_MS = 5_000;

    private final ChatHistoryStorage storage;
    private final ConcurrentHashMap<UUID, FactionChatHistory> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ScheduledFuture<?>> pendingSaves = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;

    public ChatHistoryManager(@NotNull ChatHistoryStorage storage) {
        this.storage = storage;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ChatHistory-Scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Records a new chat message for a faction.
     * Appends to the in-memory cache and schedules a debounced save.
     *
     * @param factionId the faction the message belongs to
     * @param msg       the chat message
     */
    public void recordMessage(@NotNull UUID factionId, @NotNull ChatMessage msg) {
        if (!ConfigManager.get().isChatHistoryEnabled()) {
            return;
        }

        int maxMessages = ConfigManager.get().getChatHistoryMaxMessages();

        cache.compute(factionId, (id, existing) -> {
            FactionChatHistory history = existing != null ? existing : FactionChatHistory.empty(id);
            return history.withMessage(msg, maxMessages);
        });

        scheduleDebouncedSave(factionId);
    }

    /**
     * Gets the chat history for a faction.
     * Returns from cache if available, otherwise loads from disk.
     *
     * @param factionId the faction ID
     * @return the chat history (never null, may be empty)
     */
    @NotNull
    public CompletableFuture<FactionChatHistory> getHistory(@NotNull UUID factionId) {
        FactionChatHistory cached = cache.get(factionId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return storage.loadHistory(factionId).thenApply(history -> {
            cache.putIfAbsent(factionId, history);
            return cache.get(factionId);
        });
    }

    /**
     * Gets merged ally chat history — all allied factions' messages in chronological order.
     *
     * @param factionId      the faction requesting ally history
     * @param factionManager for looking up allied factions
     * @return merged list of messages from all allied factions, newest first
     */
    @NotNull
    public CompletableFuture<List<ChatMessage>> getAlliedHistory(
            @NotNull UUID factionId,
            @NotNull FactionManager factionManager) {

        Faction faction = factionManager.getFaction(factionId);
        if (faction == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        // Collect all allied faction IDs + sender's own faction
        List<UUID> factionIds = new ArrayList<>();
        factionIds.add(factionId);

        for (var entry : faction.relations().entrySet()) {
            FactionRelation rel = entry.getValue();
            if (rel != null && rel.isAlly()) {
                factionIds.add(entry.getKey());
            }
        }

        // Load all histories in parallel
        List<CompletableFuture<FactionChatHistory>> futures = factionIds.stream()
                .map(this::getHistory)
                .toList();

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(v -> {
                    List<ChatMessage> merged = new ArrayList<>();
                    for (var future : futures) {
                        FactionChatHistory history = future.join();
                        // Only include ALLY channel messages for the merged view
                        for (ChatMessage msg : history.messages()) {
                            if (msg.channel() == ChatMessage.Channel.ALLY) {
                                merged.add(msg);
                            }
                        }
                    }
                    // Sort newest first
                    merged.sort(Comparator.comparingLong(ChatMessage::timestamp).reversed());
                    return merged;
                });
    }

    /**
     * Pre-warms the cache for a faction.
     * Call when a faction member connects.
     *
     * @param factionId the faction ID
     */
    public void preWarmCache(@NotNull UUID factionId) {
        if (!ConfigManager.get().isChatHistoryEnabled()) {
            return;
        }

        if (!cache.containsKey(factionId)) {
            storage.loadHistory(factionId).thenAccept(history -> {
                cache.putIfAbsent(factionId, history);
                Logger.debug("Pre-warmed chat history for faction %s (%d messages)", factionId, history.size());
            });
        }
    }

    /**
     * Evicts a faction's history from cache.
     * Call when no online members remain for that faction.
     *
     * @param factionId the faction ID
     */
    public void evictCache(@NotNull UUID factionId) {
        // Flush any pending save first
        flushPendingSave(factionId);
        cache.remove(factionId);
    }

    /**
     * Prunes expired messages from all cached and on-disk histories.
     */
    public void pruneExpired() {
        if (!ConfigManager.get().isChatHistoryEnabled()) {
            return;
        }

        int retentionDays = ConfigManager.get().getChatHistoryRetentionDays();
        long cutoff = System.currentTimeMillis() - (retentionDays * 24L * 60 * 60 * 1000);

        // Prune cached histories
        for (var entry : cache.entrySet()) {
            FactionChatHistory pruned = entry.getValue().pruneOlderThan(cutoff);
            if (pruned != entry.getValue()) {
                cache.put(entry.getKey(), pruned);
                scheduleDebouncedSave(entry.getKey());
            }
        }

        // Prune on-disk histories not in cache
        storage.listAllFactionIds().thenAccept(ids -> {
            for (UUID id : ids) {
                if (!cache.containsKey(id)) {
                    storage.loadHistory(id).thenAccept(history -> {
                        FactionChatHistory pruned = history.pruneOlderThan(cutoff);
                        if (pruned != history) {
                            if (pruned.isEmpty()) {
                                storage.deleteHistory(id);
                            } else {
                                storage.saveHistory(pruned);
                            }
                        }
                    });
                }
            }
        });

        Logger.debug("Chat history retention cleanup completed (cutoff: %d days)", retentionDays);
    }

    /**
     * Deletes all chat history for a faction.
     * Call when a faction is disbanded.
     *
     * @param factionId the faction ID
     */
    public void deleteHistory(@NotNull UUID factionId) {
        cancelPendingSave(factionId);
        cache.remove(factionId);
        storage.deleteHistory(factionId);
        Logger.debug("Deleted chat history for faction %s", factionId);
    }

    /**
     * Flushes all pending saves and shuts down the scheduler.
     * Call during plugin shutdown.
     */
    public void shutdown() {
        // Flush all pending saves immediately
        for (UUID factionId : pendingSaves.keySet()) {
            flushPendingSave(factionId);
        }

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        Logger.info("[Shutdown] Chat history manager shut down");
    }

    // === Private helpers ===

    private void scheduleDebouncedSave(@NotNull UUID factionId) {
        cancelPendingSave(factionId);

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            pendingSaves.remove(factionId);
            FactionChatHistory history = cache.get(factionId);
            if (history != null) {
                storage.saveHistory(history);
            }
        }, DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);

        pendingSaves.put(factionId, future);
    }

    private void cancelPendingSave(@NotNull UUID factionId) {
        ScheduledFuture<?> existing = pendingSaves.remove(factionId);
        if (existing != null) {
            existing.cancel(false);
        }
    }

    private void flushPendingSave(@NotNull UUID factionId) {
        ScheduledFuture<?> existing = pendingSaves.remove(factionId);
        if (existing != null) {
            existing.cancel(false);
        }
        FactionChatHistory history = cache.get(factionId);
        if (history != null) {
            try {
                storage.saveHistory(history).join();
            } catch (Exception e) {
                Logger.severe("Failed to flush chat history for %s: %s", factionId, e.getMessage());
            }
        }
    }
}
