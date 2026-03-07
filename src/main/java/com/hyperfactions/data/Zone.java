package com.hyperfactions.data;

import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a special zone (SafeZone or WarZone) that can span multiple chunks.
 *
 * @param id               unique identifier for the zone
 * @param name             display name of the zone
 * @param type             the zone type
 * @param world            the world name
 * @param chunks           the set of chunks belonging to this zone
 * @param createdAt        when the zone was created (epoch millis)
 * @param createdBy        UUID of the admin who created it
 * @param flags            custom flags for this zone (null = use defaults)
 * @param settings         string-valued zone settings for enum/selection options (null = use defaults)
 * @param notifyOnEntry    whether to show entry notification (null/true = show, false = suppress)
 * @param notifyTitleUpper custom upper title text (null = default status text)
 * @param notifyTitleLower custom lower title text (null = default zone name)
 */
public record Zone(
  @NotNull UUID id,
  @NotNull String name,
  @NotNull ZoneType type,
  @NotNull String world,
  @NotNull Set<ChunkKey> chunks,
  long createdAt,
  @NotNull UUID createdBy,
  @Nullable Map<String, Boolean> flags,
  @Nullable Map<String, String> settings,
  @Nullable Boolean notifyOnEntry,
  @Nullable String notifyTitleUpper,
  @Nullable String notifyTitleLower
) {
  /**
   * Compact constructor ensures chunks is never null and immutable.
   */
  public Zone {
    chunks = chunks != null ? Set.copyOf(chunks) : Set.of();
  }

  /**
   * Creates a new zone with no chunks (empty zone).
   *
   * @param name      the zone name
   * @param type      the zone type
   * @param world     the world name
   * @param createdBy UUID of the creator
   * @return a new Zone with no chunks
   */
  public static Zone create(@NotNull String name, @NotNull ZoneType type,
               @NotNull String world, @NotNull UUID createdBy) {
    return new Zone(UUID.randomUUID(), name, type, world, Set.of(),
           System.currentTimeMillis(), createdBy, null, null, null, null, null);
  }

  /**
   * Creates a new zone with a single initial chunk.
   * This provides backwards compatibility with the old single-chunk creation pattern.
   *
   * @param name      the zone name
   * @param type      the zone type
   * @param world     the world name
   * @param chunkX    the chunk X
   * @param chunkZ    the chunk Z
   * @param createdBy UUID of the creator
   * @return a new Zone with one chunk
   */
  public static Zone create(@NotNull String name, @NotNull ZoneType type,
               @NotNull String world, int chunkX, int chunkZ,
               @NotNull UUID createdBy) {
    ChunkKey chunk = new ChunkKey(world, chunkX, chunkZ);
    return new Zone(UUID.randomUUID(), name, type, world, Set.of(chunk),
           System.currentTimeMillis(), createdBy, null, null, null, null, null);
  }

  /**
   * Checks if this zone contains a specific chunk.
   *
   * @param chunk the chunk key to check
   * @return true if the zone contains this chunk
   */
  public boolean containsChunk(@NotNull ChunkKey chunk) {
    return chunks.contains(chunk);
  }

  /**
   * Checks if this zone contains a chunk at the given coordinates.
   *
   * @param chunkX the chunk X coordinate
   * @param chunkZ the chunk Z coordinate
   * @return true if the zone contains this chunk
   */
  public boolean containsChunk(int chunkX, int chunkZ) {
    return chunks.contains(new ChunkKey(world, chunkX, chunkZ));
  }

  /**
   * Checks if this zone is at the given location (backwards compatibility).
   *
   * @param worldName the world name
   * @param x         the chunk X
   * @param z         the chunk Z
   * @return true if the zone contains this chunk
   */
  public boolean isAt(@NotNull String worldName, int x, int z) {
    if (!world.equals(worldName)) {
      return false;
    }
    return containsChunk(x, z);
  }

  /**
   * Creates a copy with an additional chunk.
   *
   * @param chunk the chunk to add
   * @return a new Zone with the chunk added
   */
  public Zone withChunk(@NotNull ChunkKey chunk) {
    if (chunks.contains(chunk)) {
      return this;
    }
    Set<ChunkKey> newChunks = new HashSet<>(chunks);
    newChunks.add(chunk);
    return new Zone(id, name, type, world, newChunks, createdAt, createdBy, flags,
           settings, notifyOnEntry, notifyTitleUpper, notifyTitleLower);
  }

  /**
   * Creates a copy with an additional chunk at the given coordinates.
   *
   * @param chunkX the chunk X coordinate
   * @param chunkZ the chunk Z coordinate
   * @return a new Zone with the chunk added
   */
  public Zone withChunk(int chunkX, int chunkZ) {
    return withChunk(new ChunkKey(world, chunkX, chunkZ));
  }

  /**
   * Creates a copy with a chunk removed.
   *
   * @param chunk the chunk to remove
   * @return a new Zone with the chunk removed
   */
  public Zone withoutChunk(@NotNull ChunkKey chunk) {
    if (!chunks.contains(chunk)) {
      return this;
    }
    Set<ChunkKey> newChunks = new HashSet<>(chunks);
    newChunks.remove(chunk);
    return new Zone(id, name, type, world, newChunks, createdAt, createdBy, flags,
           settings, notifyOnEntry, notifyTitleUpper, notifyTitleLower);
  }

  /**
   * Creates a copy with a chunk at the given coordinates removed.
   *
   * @param chunkX the chunk X coordinate
   * @param chunkZ the chunk Z coordinate
   * @return a new Zone with the chunk removed
   */
  public Zone withoutChunk(int chunkX, int chunkZ) {
    return withoutChunk(new ChunkKey(world, chunkX, chunkZ));
  }

  /**
   * Gets the number of chunks in this zone.
   *
   * @return the chunk count
   */
  public int getChunkCount() {
    return chunks.size();
  }

  /**
   * Checks if this zone has no chunks.
   *
   * @return true if the zone is empty
   */
  public boolean isEmpty() {
    return chunks.isEmpty();
  }

  /**
   * Checks if this is a safe zone.
   *
   * @return true if safe zone
   */
  public boolean isSafeZone() {
    return type == ZoneType.SAFE;
  }

  /**
   * Checks if this is a war zone.
   *
   * @return true if war zone
   */
  public boolean isWarZone() {
    return type == ZoneType.WAR;
  }

  /**
   * Creates a copy with a new name.
   *
   * @param newName the new name
   * @return a new Zone with updated name
   */
  public Zone withName(@NotNull String newName) {
    return new Zone(id, newName, type, world, chunks, createdAt, createdBy, flags,
           settings, notifyOnEntry, notifyTitleUpper, notifyTitleLower);
  }

  /**
   * Creates a copy with a flag set.
   *
   * @param flagName the flag name
   * @param value    the flag value
   * @return a new Zone with updated flag
   */
  public Zone withFlag(@NotNull String flagName, boolean value) {
    Map<String, Boolean> newFlags = flags != null ? new HashMap<>(flags) : new HashMap<>();
    newFlags.put(flagName, value);
    return new Zone(id, name, type, world, chunks, createdAt, createdBy, newFlags,
           settings, notifyOnEntry, notifyTitleUpper, notifyTitleLower);
  }

  /**
   * Creates a copy with a flag removed (reverts to default).
   *
   * @param flagName the flag name to remove
   * @return a new Zone with flag removed
   */
  public Zone withoutFlag(@NotNull String flagName) {
    if (flags == null || !flags.containsKey(flagName)) {
      return this;
    }
    Map<String, Boolean> newFlags = new HashMap<>(flags);
    newFlags.remove(flagName);
    return new Zone(id, name, type, world, chunks, createdAt, createdBy,
           newFlags.isEmpty() ? null : newFlags, settings,
           notifyOnEntry, notifyTitleUpper, notifyTitleLower);
  }

  /**
   * Creates a copy with updated flags.
   *
   * @param newFlags the new flags map (null = use defaults)
   * @return a new Zone with updated flags
   */
  public Zone withFlags(@Nullable Map<String, Boolean> newFlags) {
    return new Zone(id, name, type, world, chunks, createdAt, createdBy, newFlags,
           settings, notifyOnEntry, notifyTitleUpper, notifyTitleLower);
  }

  /**
   * Creates a copy with a different notifyOnEntry setting.
   *
   * @param notifyOnEntry whether to show entry notification (null = default/true, false = suppress)
   * @return a new Zone with updated setting
   */
  public Zone withNotifyOnEntry(@Nullable Boolean notifyOnEntry) {
    return new Zone(id, name, type, world, chunks, createdAt, createdBy, flags,
           settings, notifyOnEntry, notifyTitleUpper, notifyTitleLower);
  }

  /**
   * Creates a copy with a different upper title text.
   *
   * @param title the custom upper title (null = use default)
   * @return a new Zone with updated title
   */
  public Zone withNotifyTitleUpper(@Nullable String title) {
    return new Zone(id, name, type, world, chunks, createdAt, createdBy, flags,
           settings, notifyOnEntry, title, notifyTitleLower);
  }

  /**
   * Creates a copy with a different lower title text.
   *
   * @param title the custom lower title (null = use default)
   * @return a new Zone with updated title
   */
  public Zone withNotifyTitleLower(@Nullable String title) {
    return new Zone(id, name, type, world, chunks, createdAt, createdBy, flags,
           settings, notifyOnEntry, notifyTitleUpper, title);
  }

  /**
   * Gets the value of a specific flag, or null if using default.
   *
   * @param flagName the flag name
   * @return the flag value, or null if not set (use default)
   */
  @Nullable
  public Boolean getFlag(@NotNull String flagName) {
    return flags != null ? flags.get(flagName) : null;
  }

  /**
   * Checks if a specific flag has been explicitly set (overriding default).
   *
   * @param flagName the flag name
   * @return true if flag has been explicitly set
   */
  public boolean hasFlagSet(@NotNull String flagName) {
    return flags != null && flags.containsKey(flagName);
  }

  /**
   * Gets all explicitly set flags (immutable copy).
   *
   * @return map of flag name to value, never null
   */
  @NotNull
  public Map<String, Boolean> getFlags() {
    return flags != null ? Collections.unmodifiableMap(flags) : Collections.emptyMap();
  }

  /**
   * Gets the effective value for a flag, considering zone type defaults.
   *
   * @param flagName the flag name
   * @return the effective flag value
   */
  public boolean getEffectiveFlag(@NotNull String flagName) {
    // Check if explicitly set
    if (flags != null && flags.containsKey(flagName)) {
      return flags.get(flagName);
    }

    // Return default based on zone type
    return isSafeZone() ? ZoneFlags.getSafeZoneDefault(flagName) : ZoneFlags.getWarZoneDefault(flagName);
  }

  // === String-valued settings (enum/selection flags) ===

  /**
   * Gets the value of a string setting, or null if not set.
   *
   * @param settingName the setting name
   * @return the setting value, or null if not set
   */
  @Nullable
  public String getSetting(@NotNull String settingName) {
    return settings != null ? settings.get(settingName) : null;
  }

  /**
   * Gets the effective value for a string setting, considering zone type defaults.
   *
   * @param settingName the setting name
   * @return the effective setting value
   */
  @NotNull
  public String getEffectiveSetting(@NotNull String settingName) {
    if (settings != null && settings.containsKey(settingName)) {
      return settings.get(settingName);
    }
    return ZoneFlags.getSettingDefault(settingName, type);
  }

  /**
   * Checks if a setting has been explicitly set.
   *
   * @param settingName the setting name
   * @return true if setting has been explicitly set
   */
  public boolean hasSettingSet(@NotNull String settingName) {
    return settings != null && settings.containsKey(settingName);
  }

  /**
   * Creates a copy with a setting set.
   *
   * @param settingName the setting name
   * @param value       the setting value
   * @return a new Zone with updated setting
   */
  public Zone withSetting(@NotNull String settingName, @NotNull String value) {
    Map<String, String> newSettings = settings != null ? new HashMap<>(settings) : new HashMap<>();
    newSettings.put(settingName, value);
    return new Zone(id, name, type, world, chunks, createdAt, createdBy, flags,
           newSettings, notifyOnEntry, notifyTitleUpper, notifyTitleLower);
  }

  /**
   * Creates a copy with a setting removed (reverts to default).
   *
   * @param settingName the setting name to remove
   * @return a new Zone with setting removed
   */
  public Zone withoutSetting(@NotNull String settingName) {
    if (settings == null || !settings.containsKey(settingName)) {
      return this;
    }
    Map<String, String> newSettings = new HashMap<>(settings);
    newSettings.remove(settingName);
    return new Zone(id, name, type, world, chunks, createdAt, createdBy, flags,
           newSettings.isEmpty() ? null : newSettings,
           notifyOnEntry, notifyTitleUpper, notifyTitleLower);
  }

  /**
   * Gets all explicitly set settings (immutable copy).
   *
   * @return map of setting name to value, never null
   */
  @NotNull
  public Map<String, String> getSettings() {
    return settings != null ? Collections.unmodifiableMap(settings) : Collections.emptyMap();
  }
}
