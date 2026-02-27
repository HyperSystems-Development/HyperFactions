package com.hyperfactions.gui.faction;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
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
 * Central registry for all HyperFactions GUI pages.
 * Follows the AdminUI pattern for page registration and navigation.
 */
public final class FactionPageRegistry {

  private static final FactionPageRegistry INSTANCE = new FactionPageRegistry();

  private final Map<String, Entry> entries = new ConcurrentHashMap<>();

  private final List<Entry> orderedEntries = new ArrayList<>();

  private FactionPageRegistry() {
  }

  /** Returns the instance. */
  public static FactionPageRegistry getInstance() {
    return INSTANCE;
  }

  /**
   * Represents a registered GUI page entry.
   *
   * @param id           Unique page identifier (e.g., "dashboard", "members")
   * @param displayName  UI display name (e.g., "Dashboard", "Members")
   * @param permission   Required permission node (null for no permission required)
   * @param guiSupplier  Function to create the page instance
   * @param showsInNavBar Whether this page appears in the navigation bar
   * @param requiresFaction Whether this page requires the player to be in a faction
   * @param minimumRole  Minimum faction role required to access this page (null for no role requirement)
   * @param order        Display order in navigation (lower = first)
   */
  public record Entry(
      @NotNull String id,
      @NotNull String displayName,
      @Nullable String permission,
      @NotNull PageSupplier guiSupplier,
      boolean showsInNavBar,
      boolean requiresFaction,
      @Nullable FactionRole minimumRole,
      int order
  ) implements NavEntry, Comparable<Entry> {

    /**
     * Convenience constructor for entries without role requirements.
     */
    public Entry(
        @NotNull String id,
        @NotNull String displayName,
        @Nullable String permission,
        @NotNull PageSupplier guiSupplier,
        boolean showsInNavBar,
        boolean requiresFaction,
        int order
    ) {
      this(id, displayName, permission, guiSupplier, showsInNavBar, requiresFaction, null, order);
    }

    /** Compare To. */
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
     * @param player    The player entity
     * @param ref       Entity reference
     * @param store     Entity store
     * @param playerRef Player reference component
     * @param faction   The player's faction (may be null)
     * @param guiManager The GUI manager
     * @return The created page, or null if page cannot be created
     */
    @Nullable InteractiveCustomUIPage<?> create(
        Player player,
        Ref<EntityStore> ref,
        Store<EntityStore> store,
        PlayerRef playerRef,
        @Nullable Faction faction,
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
   * Gets entries accessible to a player (permission and role check).
   *
   * @param playerRef The player to check
   * @param faction   The player's faction (may be null)
   * @return List of accessible entries
   */
  @NotNull
  public List<Entry> getAccessibleEntries(@NotNull PlayerRef playerRef, @Nullable Faction faction) {
    boolean hasFaction = faction != null;
    return orderedEntries.stream()
        .filter(entry -> {
          // Check faction requirement
          if (entry.requiresFaction() && !hasFaction) {
            return false;
          }

          // Check permission
          if (entry.permission() != null) {
            if (!PermissionManager.get().hasPermission(playerRef.getUuid(), entry.permission())) {
              return false;
            }
          }

          // Check role requirement
          if (entry.minimumRole() != null && faction != null) {
            FactionMember member = faction.getMember(playerRef.getUuid());
            if (member == null || !member.role().isAtLeast(entry.minimumRole())) {
              return false;
            }
          }
          return true;
        })
        .toList();
  }

  /**
   * Gets nav bar entries accessible to a player.
   *
   * @param playerRef The player to check
   * @param faction   The player's faction (may be null)
   * @return List of accessible nav bar entries
   */
  @NotNull
  public List<Entry> getAccessibleNavBarEntries(@NotNull PlayerRef playerRef, @Nullable Faction faction) {
    return getAccessibleEntries(playerRef, faction).stream()
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
