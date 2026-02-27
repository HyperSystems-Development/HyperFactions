package com.hyperfactions.config;

import com.hyperfactions.config.modules.WorldsConfig.WorldSettings;
import com.hyperfactions.config.modules.WorldsConfig;
import com.hyperfactions.util.Logger;
import java.util.*;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves per-world settings from {@link WorldsConfig} with wildcard pattern matching.
 *
 * <p>
 * Matching priority:
 * <ol>
 *   <li>Exact name match (e.g., "events")</li>
 *   <li>Wildcard patterns ordered by specificity (fewer wildcards = higher priority)</li>
 *   <li>Default policy (allow or deny)</li>
 * </ol>
 *
 * <p>
 * Wildcard syntax: {@code %} matches any sequence of characters.
 * Example: {@code arena_%} matches {@code arena_1}, {@code arena_pvp}, etc.
 */
public class WorldSettingsResolver {

  /** Cached compiled patterns for wildcard world keys. */
  private final List<WildcardEntry> wildcardPatterns = new ArrayList<>();

  /** Exact-match world settings. */
  private final Map<String, WorldSettings> exactMatches = new HashMap<>();

  /** The default policy when no match is found. */
  private boolean defaultAllow = true;

  /** Claim blacklist (always blocked, regardless of per-world settings). */
  private Set<String> claimBlacklist = new HashSet<>();

  /** Record for a wildcard pattern with its priority. */
  private record WildcardEntry(String key, Pattern pattern, int wildcardCount, WorldSettings settings) {}

  /**
   * Rebuilds the resolver from the current WorldsConfig.
   * Called on load/reload.
   *
   * @param config the worlds config
   */
  public void rebuild(@NotNull WorldsConfig config) {
    exactMatches.clear();
    wildcardPatterns.clear();
    defaultAllow = "allow".equals(config.getDefaultPolicy());
    claimBlacklist = new HashSet<>(config.getClaimBlacklist());

    for (Map.Entry<String, WorldSettings> entry : config.getWorlds().entrySet()) {
      String key = entry.getKey();
      if (key.contains("%")) {
        // Wildcard pattern
        String regex = Pattern.quote(key).replace("%", "\\E.*\\Q");
        // Clean up empty quote groups
        regex = regex.replace("\\Q\\E", "");
        Pattern pattern = Pattern.compile("^" + regex + "$");
        int wildcardCount = (int) key.chars().filter(c -> c == '%').count();
        wildcardPatterns.add(new WildcardEntry(key, pattern, wildcardCount, entry.getValue()));
      } else {
        // Exact match
        exactMatches.put(key, entry.getValue());
      }
    }

    // Sort wildcards: fewer wildcards = higher priority (more specific)
    wildcardPatterns.sort(Comparator.comparingInt(WildcardEntry::wildcardCount));

    Logger.debug("[Worlds] Resolver rebuilt: %d exact, %d wildcard, defaultAllow=%s, blacklist=%d",
        exactMatches.size(), wildcardPatterns.size(), defaultAllow, claimBlacklist.size());
  }

  /**
   * Resolves the settings for a world by name.
   * Returns null if no match found (use defaults).
   *
   * @param worldName the world name
   * @return the resolved settings, or null if no specific settings exist
   */
  @Nullable
  public WorldSettings resolve(@NotNull String worldName) {
    // 1. Exact match
    WorldSettings exact = exactMatches.get(worldName);
    if (exact != null) {
      return exact;
    }

    // 2. Wildcard match (already sorted by specificity)
    for (WildcardEntry entry : wildcardPatterns) {
      if (entry.pattern().matcher(worldName).matches()) {
        return entry.settings();
      }
    }

    // 3. No match
    return null;
  }

  /**
   * Checks if claiming is allowed in the given world.
   * Resolution order: per-world setting → claim blacklist → default policy.
   *
   * @param worldName the world name
   * @return true if claiming is allowed
   */
  public boolean isClaimingAllowed(@NotNull String worldName) {
    // Claim blacklist always takes precedence
    if (claimBlacklist.contains(worldName)) {
      return false;
    }

    WorldSettings settings = resolve(worldName);
    if (settings != null && settings.claiming() != null) {
      return settings.claiming();
    }

    return defaultAllow;
  }

  /**
   * Checks if power loss on death is enabled in the given world.
   * Resolution order: per-world setting → default (true = enabled).
   *
   * @param worldName the world name
   * @return true if power loss is enabled
   */
  public boolean isPowerLossEnabled(@NotNull String worldName) {
    WorldSettings settings = resolve(worldName);
    if (settings != null && settings.powerLoss() != null) {
      return settings.powerLoss();
    }

    // Default: power loss is always enabled unless explicitly disabled
    return true;
  }

  /**
   * Checks if faction-on-faction friendly fire is allowed in the given world.
   * Resolution order: per-world setting → global config fallback (handled by caller).
   *
   * @param worldName the world name
   * @return the per-world override, or null to use global config
   */
  @Nullable
  public Boolean isFriendlyFireFactionAllowed(@NotNull String worldName) {
    WorldSettings settings = resolve(worldName);
    if (settings != null && settings.friendlyFireFaction() != null) {
      return settings.friendlyFireFaction();
    }
    return null; // Caller uses global config
  }

  /**
   * Checks if ally-on-ally friendly fire is allowed in the given world.
   * Resolution order: per-world setting → global config fallback (handled by caller).
   *
   * @param worldName the world name
   * @return the per-world override, or null to use global config
   */
  @Nullable
  public Boolean isFriendlyFireAllyAllowed(@NotNull String worldName) {
    WorldSettings settings = resolve(worldName);
    if (settings != null && settings.friendlyFireAlly() != null) {
      return settings.friendlyFireAlly();
    }
    return null; // Caller uses global config
  }

  /**
   * Checks if the default policy is "allow".
   *
   * @return true if default policy is allow
   */
  public boolean isDefaultAllow() {
    return defaultAllow;
  }

  /**
   * Gets the claim blacklist.
   *
   * @return unmodifiable set of blacklisted world names
   */
  @NotNull
  public Set<String> getClaimBlacklist() {
    return Collections.unmodifiableSet(claimBlacklist);
  }
}
