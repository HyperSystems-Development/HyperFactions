package com.hyperfactions.gui.admin;

import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.shared.NavEntry;
import com.hyperfactions.integration.PermissionManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Central registry for all HyperFactions Admin GUI pages.
 * Follows the FactionPageRegistry pattern for admin-specific navigation.
 */
public final class AdminPageRegistry {

  private static final AdminPageRegistry INSTANCE = new AdminPageRegistry();

  private final Map<String, Entry> entries = new ConcurrentHashMap<>();

  private final List<Entry> orderedEntries = new ArrayList<>();

  private AdminPageRegistry() {
  }

  /** Returns the instance. */
  public static AdminPageRegistry getInstance() {
    return INSTANCE;
  }

  /**
   * Represents a registered admin GUI page entry.
   *
   * @param id           Unique page identifier (e.g., "dashboard", "zones")
   * @param displayName  UI display name (e.g., "Dashboard", "Zones")
   * @param permission   Required permission node (null for no permission required beyond admin)
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
  ) implements NavEntry, Comparable<Entry> {

    /** Compare To. */
    @Override
    public int compareTo(@NotNull Entry other) {
      return Integer.compare(this.order, other.order);
    }
  }

  /**
   * Functional interface for creating admin page instances.
   */
  @FunctionalInterface
  public interface PageSupplier {
    /**
     * Creates a new page instance.
     *
     * @param player    The player entity
     * @param ref       Entity reference
     * @param store     Entity store
     * @param playerRef Player reference component
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
