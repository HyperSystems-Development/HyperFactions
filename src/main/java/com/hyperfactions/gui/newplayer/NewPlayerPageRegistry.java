package com.hyperfactions.gui.newplayer;

import com.hyperfactions.data.Faction;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.integration.PermissionManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for New Player GUI pages.
 * Manages pages shown to players who are NOT in a faction.
 *
 * Nav bar order: BROWSE | CREATE | INVITES | MAP | HELP
 */
public final class NewPlayerPageRegistry {

    private static final NewPlayerPageRegistry INSTANCE = new NewPlayerPageRegistry();

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final List<Entry> orderedEntries = new ArrayList<>();

    private NewPlayerPageRegistry() {
    }

    public static NewPlayerPageRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Represents a registered GUI page entry for new players.
     *
     * @param id           Unique page identifier (e.g., "browse", "create", "invites")
     * @param displayName  UI display name (e.g., "Browse", "Create", "Invites")
     * @param permission   Required permission node (null for no permission required)
     * @param guiSupplier  Function to create the page instance
     * @param showsInNavBar Whether this page appears in the navigation bar
     * @param order        Display order in navigation (lower = first)
     */
    public record Entry(
            @NotNull String id,
            @NotNull String displayName,
            @Nullable String permission,
            @NotNull PageSupplier guiSupplier,
            boolean showsInNavBar,
            int order
    ) implements Comparable<Entry> {

        @Override
        public int compareTo(@NotNull Entry other) {
            return Integer.compare(this.order, other.order);
        }
    }

    /**
     * Functional interface for creating page instances.
     */
    @FunctionalInterface
    public interface PageSupplier {
        /**
         * Creates a new page instance.
         *
         * @param player     The player entity
         * @param ref        Entity reference
         * @param store      Entity store
         * @param playerRef  Player reference component
         * @param guiManager The GUI manager
         * @return The created page, or null if page cannot be created
         */
        @Nullable InteractiveCustomUIPage<?> create(
                Player player,
                Ref<EntityStore> ref,
                Store<EntityStore> store,
                PlayerRef playerRef,
                GuiManager guiManager
        );
    }

    /**
     * Registers a page entry.
     *
     * @param entry The entry to register
     */
    public void registerEntry(@NotNull Entry entry) {
        entries.put(entry.id(), entry);
        orderedEntries.add(entry);
        orderedEntries.sort(Entry::compareTo);
    }

    /**
     * Gets an entry by ID.
     *
     * @param id The page ID
     * @return The entry, or null if not found
     */
    @Nullable
    public Entry getEntry(@NotNull String id) {
        return entries.get(id);
    }

    /**
     * Gets all registered entries in display order.
     *
     * @return Unmodifiable list of entries
     */
    @NotNull
    public List<Entry> getEntries() {
        return Collections.unmodifiableList(orderedEntries);
    }

    /**
     * Gets entries that should appear in the navigation bar.
     *
     * @return List of nav bar entries in display order
     */
    @NotNull
    public List<Entry> getNavBarEntries() {
        return orderedEntries.stream()
                .filter(Entry::showsInNavBar)
                .toList();
    }

    /**
     * Gets entries accessible to a player (permission check).
     *
     * @param playerRef The player to check
     * @return List of accessible entries
     */
    @NotNull
    public List<Entry> getAccessibleEntries(@NotNull PlayerRef playerRef) {
        return orderedEntries.stream()
                .filter(entry -> {
                    // Check permission
                    if (entry.permission() != null) {
                        return PermissionManager.get().hasPermission(playerRef.getUuid(), entry.permission());
                    }
                    return true;
                })
                .toList();
    }

    /**
     * Gets nav bar entries accessible to a player.
     *
     * @param playerRef The player to check
     * @return List of accessible nav bar entries
     */
    @NotNull
    public List<Entry> getAccessibleNavBarEntries(@NotNull PlayerRef playerRef) {
        return getAccessibleEntries(playerRef).stream()
                .filter(Entry::showsInNavBar)
                .toList();
    }

    /**
     * Clears all registered entries.
     * Used for testing or reloading.
     */
    public void clear() {
        entries.clear();
        orderedEntries.clear();
    }
}
