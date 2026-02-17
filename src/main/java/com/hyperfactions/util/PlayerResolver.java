package com.hyperfactions.util;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Centralized player name resolution for HyperFactions.
 * <p>
 * Resolution chain: online players -> faction member records -> PlayerDB API.
 * Use {@link #resolve(HyperFactions, String)} for single-result exact lookup (commands),
 * and {@link #search(HyperFactions, String, UUID)} for multi-result partial matching (GUIs).
 */
public final class PlayerResolver {

    /**
     * Where the player was found.
     */
    public enum Source {
        ONLINE,
        FACTION_MEMBER,
        PLAYER_DB
    }

    /**
     * A resolved player with UUID, properly-cased username, and resolution source.
     */
    public record ResolvedPlayer(
            @NotNull UUID uuid,
            @NotNull String username,
            @NotNull Source source,
            @Nullable String factionName
    ) {}

    private PlayerResolver() {}

    /**
     * Resolves a single player by exact username match.
     * Checks online players, then faction members, then PlayerDB API.
     *
     * @param hyperFactions the HyperFactions instance
     * @param name the player username (case-insensitive)
     * @return resolved player, or null if not found anywhere
     */
    @Nullable
    public static ResolvedPlayer resolve(@NotNull HyperFactions hyperFactions,
                                          @NotNull String name) {
        // 1. Check online players
        for (PlayerRef ref : hyperFactions.getOnlinePlayerRefs()) {
            String username = ref.getUsername();
            if (username != null && username.equalsIgnoreCase(name)) {
                String factionName = getFactionName(hyperFactions, ref.getUuid());
                return new ResolvedPlayer(ref.getUuid(), username, Source.ONLINE, factionName);
            }
        }

        // 2. Check faction member records (offline players who have been in a faction)
        for (Faction faction : hyperFactions.getFactionManager().getAllFactions()) {
            for (FactionMember member : faction.members().values()) {
                if (member.username().equalsIgnoreCase(name)) {
                    return new ResolvedPlayer(member.uuid(), member.username(),
                            Source.FACTION_MEMBER, faction.name());
                }
            }
        }

        // 3. Fall back to PlayerDB API (any Hytale player)
        var info = PlayerDBService.lookup(name).join();
        if (info != null) {
            String factionName = getFactionName(hyperFactions, info.uuid());
            return new ResolvedPlayer(info.uuid(), info.username(), Source.PLAYER_DB, factionName);
        }

        return null;
    }

    /**
     * Searches for players matching a partial query. Returns multiple results.
     * Searches online players (partial), then faction members (partial),
     * then tries PlayerDB (exact) if no results found.
     * <p>
     * Results are deduplicated by UUID and sorted (exact matches first, then alphabetical).
     *
     * @param hyperFactions the HyperFactions instance
     * @param query the search query (case-insensitive, partial match for online/faction)
     * @param excludeUuid UUID to exclude from results (e.g., self), or null
     * @return list of matching players (may be empty)
     */
    @NotNull
    public static List<ResolvedPlayer> search(@NotNull HyperFactions hyperFactions,
                                               @NotNull String query,
                                               @Nullable UUID excludeUuid) {
        List<ResolvedPlayer> results = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        String queryLower = query.toLowerCase();

        // 1. Search online players (partial match)
        for (PlayerRef ref : hyperFactions.getOnlinePlayerRefs()) {
            UUID uuid = ref.getUuid();
            if (uuid.equals(excludeUuid)) continue;
            if (seen.contains(uuid)) continue;

            String username = ref.getUsername();
            if (username != null && username.toLowerCase().contains(queryLower)) {
                seen.add(uuid);
                String factionName = getFactionName(hyperFactions, uuid);
                results.add(new ResolvedPlayer(uuid, username, Source.ONLINE, factionName));
            }
        }

        // 2. Search faction members (partial match, includes offline players)
        for (Faction faction : hyperFactions.getFactionManager().getAllFactions()) {
            for (FactionMember member : faction.members().values()) {
                UUID uuid = member.uuid();
                if (uuid.equals(excludeUuid)) continue;
                if (seen.contains(uuid)) continue;

                if (member.username().toLowerCase().contains(queryLower)) {
                    seen.add(uuid);
                    results.add(new ResolvedPlayer(uuid, member.username(),
                            Source.FACTION_MEMBER, faction.name()));
                }
            }
        }

        // 3. If no player results and query is long enough, try PlayerDB exact lookup
        if (results.isEmpty() && query.length() >= 3) {
            var info = PlayerDBService.lookup(query).join();
            if (info != null && !info.uuid().equals(excludeUuid)) {
                String factionName = getFactionName(hyperFactions, info.uuid());
                results.add(new ResolvedPlayer(info.uuid(), info.username(),
                        Source.PLAYER_DB, factionName));
            }
        }

        // Sort: exact matches first, then alphabetical
        results.sort((a, b) -> {
            boolean aExact = a.username().equalsIgnoreCase(query);
            boolean bExact = b.username().equalsIgnoreCase(query);
            if (aExact != bExact) return aExact ? -1 : 1;
            return a.username().compareToIgnoreCase(b.username());
        });

        return results;
    }

    /**
     * Gets the faction name for a player, or null if not in a faction.
     */
    @Nullable
    private static String getFactionName(@NotNull HyperFactions hyperFactions, @NotNull UUID playerUuid) {
        Faction faction = hyperFactions.getFactionManager().getPlayerFaction(playerUuid);
        return faction != null ? faction.name() : null;
    }
}
