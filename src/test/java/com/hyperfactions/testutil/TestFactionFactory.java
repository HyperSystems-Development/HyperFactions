package com.hyperfactions.testutil;

import com.hyperfactions.data.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Factory for creating test faction instances.
 */
public final class TestFactionFactory {

    private TestFactionFactory() {}

    /**
     * Builder for creating test factions with configurable properties.
     */
    public static class FactionBuilder {
        private UUID id = UUID.randomUUID();
        private String name = "TestFaction";
        private String description = null;
        private String tag = null;
        private String color = "b";
        private long createdAt = System.currentTimeMillis();
        private Faction.FactionHome home = null;
        private final Map<UUID, FactionMember> members = new HashMap<>();
        private final Set<FactionClaim> claims = new HashSet<>();
        private final Map<UUID, FactionRelation> relations = new HashMap<>();
        private final List<FactionLog> logs = new ArrayList<>();
        private boolean open = false;

        public FactionBuilder() {}

        public FactionBuilder id(@NotNull UUID id) {
            this.id = id;
            return this;
        }

        public FactionBuilder name(@NotNull String name) {
            this.name = name;
            return this;
        }

        public FactionBuilder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        public FactionBuilder tag(@Nullable String tag) {
            this.tag = tag;
            return this;
        }

        public FactionBuilder color(@NotNull String color) {
            this.color = color;
            return this;
        }

        public FactionBuilder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public FactionBuilder home(@NotNull String world, double x, double y, double z, @NotNull UUID setBy) {
            this.home = new Faction.FactionHome(world, x, y, z, 0, 0, System.currentTimeMillis(), setBy);
            return this;
        }

        public FactionBuilder home(@Nullable Faction.FactionHome home) {
            this.home = home;
            return this;
        }

        public FactionBuilder addMember(@NotNull FactionMember member) {
            this.members.put(member.uuid(), member);
            return this;
        }

        public FactionBuilder addLeader(@NotNull UUID uuid, @NotNull String username) {
            return addMember(TestPlayerFactory.createLeader(uuid, username));
        }

        public FactionBuilder addOfficer(@NotNull UUID uuid, @NotNull String username) {
            return addMember(TestPlayerFactory.createOfficer(uuid, username));
        }

        public FactionBuilder addRegularMember(@NotNull UUID uuid, @NotNull String username) {
            return addMember(TestPlayerFactory.createRegularMember(uuid, username));
        }

        public FactionBuilder addClaim(@NotNull String world, int chunkX, int chunkZ, @NotNull UUID claimedBy) {
            this.claims.add(FactionClaim.create(world, chunkX, chunkZ, claimedBy));
            return this;
        }

        public FactionBuilder addClaims(@NotNull String world, int startX, int startZ, int count, @NotNull UUID claimedBy) {
            for (int i = 0; i < count; i++) {
                this.claims.add(FactionClaim.create(world, startX + i, startZ, claimedBy));
            }
            return this;
        }

        public FactionBuilder addRelation(@NotNull UUID targetFactionId, @NotNull RelationType type) {
            this.relations.put(targetFactionId, FactionRelation.create(targetFactionId, type));
            return this;
        }

        public FactionBuilder addAlly(@NotNull UUID targetFactionId) {
            return addRelation(targetFactionId, RelationType.ALLY);
        }

        public FactionBuilder addEnemy(@NotNull UUID targetFactionId) {
            return addRelation(targetFactionId, RelationType.ENEMY);
        }

        public FactionBuilder open(boolean open) {
            this.open = open;
            return this;
        }

        public Faction build() {
            return new Faction(id, name, description, tag, color, createdAt, home, members, claims, relations, logs, open, null, null);
        }
    }

    /**
     * Creates a new faction builder.
     *
     * @return a new builder
     */
    public static FactionBuilder builder() {
        return new FactionBuilder();
    }

    /**
     * Creates a simple faction with one leader.
     *
     * @param name       the faction name
     * @param leaderUuid the leader's UUID
     * @param leaderName the leader's username
     * @return a new faction
     */
    public static Faction createSimple(@NotNull String name, @NotNull UUID leaderUuid, @NotNull String leaderName) {
        return builder()
                .name(name)
                .addLeader(leaderUuid, leaderName)
                .build();
    }

    /**
     * Creates a faction with multiple members.
     *
     * @param name        the faction name
     * @param leaderUuid  the leader's UUID
     * @param leaderName  the leader's username
     * @param memberCount number of additional regular members to add
     * @return a new faction
     */
    public static Faction createWithMembers(@NotNull String name, @NotNull UUID leaderUuid,
                                            @NotNull String leaderName, int memberCount) {
        FactionBuilder builder = builder()
                .name(name)
                .addLeader(leaderUuid, leaderName);

        for (int i = 0; i < memberCount; i++) {
            builder.addRegularMember(UUID.randomUUID(), "Member" + i);
        }

        return builder.build();
    }

    /**
     * Creates a faction with claims.
     *
     * @param name       the faction name
     * @param leaderUuid the leader's UUID
     * @param leaderName the leader's username
     * @param claimCount number of claims to add
     * @return a new faction with claims
     */
    public static Faction createWithClaims(@NotNull String name, @NotNull UUID leaderUuid,
                                           @NotNull String leaderName, int claimCount) {
        return builder()
                .name(name)
                .addLeader(leaderUuid, leaderName)
                .addClaims("world", 0, 0, claimCount, leaderUuid)
                .build();
    }

    /**
     * Creates two allied factions.
     *
     * @return an array of two allied factions [faction1, faction2]
     */
    public static Faction[] createAlliedPair() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID leader1 = UUID.randomUUID();
        UUID leader2 = UUID.randomUUID();

        Faction faction1 = builder()
                .id(id1)
                .name("Faction1")
                .addLeader(leader1, "Leader1")
                .addAlly(id2)
                .build();

        Faction faction2 = builder()
                .id(id2)
                .name("Faction2")
                .addLeader(leader2, "Leader2")
                .addAlly(id1)
                .build();

        return new Faction[]{faction1, faction2};
    }

    /**
     * Creates two enemy factions.
     *
     * @return an array of two enemy factions [faction1, faction2]
     */
    public static Faction[] createEnemyPair() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID leader1 = UUID.randomUUID();
        UUID leader2 = UUID.randomUUID();

        Faction faction1 = builder()
                .id(id1)
                .name("Faction1")
                .addLeader(leader1, "Leader1")
                .addEnemy(id2)
                .build();

        Faction faction2 = builder()
                .id(id2)
                .name("Faction2")
                .addLeader(leader2, "Leader2")
                .addEnemy(id1)
                .build();

        return new Faction[]{faction1, faction2};
    }
}
