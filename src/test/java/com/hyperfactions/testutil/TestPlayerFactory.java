package com.hyperfactions.testutil;

import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.data.PlayerPower;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Factory for creating test player instances.
 */
public final class TestPlayerFactory {

    private TestPlayerFactory() {}

    /**
     * Creates a PlayerPower with specified values.
     *
     * @param uuid     the player UUID
     * @param power    current power
     * @param maxPower maximum power
     * @return a new PlayerPower
     */
    public static PlayerPower createPower(@NotNull UUID uuid, double power, double maxPower) {
        return new PlayerPower(uuid, power, maxPower, 0, System.currentTimeMillis(), null, false, false);
    }

    /**
     * Creates a PlayerPower with default max power of 20.
     *
     * @param uuid  the player UUID
     * @param power current power
     * @return a new PlayerPower
     */
    public static PlayerPower createPower(@NotNull UUID uuid, double power) {
        return createPower(uuid, power, 20.0);
    }

    /**
     * Creates a PlayerPower with full power.
     *
     * @param uuid the player UUID
     * @return a new PlayerPower at max
     */
    public static PlayerPower createFullPower(@NotNull UUID uuid) {
        return createPower(uuid, 20.0, 20.0);
    }

    /**
     * Creates a PlayerPower with zero power.
     *
     * @param uuid the player UUID
     * @return a new PlayerPower at zero
     */
    public static PlayerPower createZeroPower(@NotNull UUID uuid) {
        return createPower(uuid, 0.0, 20.0);
    }

    /**
     * Creates a FactionMember with specified role.
     *
     * @param uuid     the player UUID
     * @param username the player username
     * @param role     the faction role
     * @return a new FactionMember
     */
    public static FactionMember createMember(@NotNull UUID uuid, @NotNull String username, @NotNull FactionRole role) {
        long now = System.currentTimeMillis();
        return new FactionMember(uuid, username, role, now, now);
    }

    /**
     * Creates a FactionMember with specified role and join time.
     *
     * @param uuid     the player UUID
     * @param username the player username
     * @param role     the faction role
     * @param joinedAt when the player joined (epoch millis)
     * @return a new FactionMember
     */
    public static FactionMember createMember(@NotNull UUID uuid, @NotNull String username,
                                              @NotNull FactionRole role, long joinedAt) {
        return new FactionMember(uuid, username, role, joinedAt, System.currentTimeMillis());
    }

    /**
     * Creates a leader FactionMember.
     *
     * @param uuid     the player UUID
     * @param username the player username
     * @return a new leader FactionMember
     */
    public static FactionMember createLeader(@NotNull UUID uuid, @NotNull String username) {
        return createMember(uuid, username, FactionRole.LEADER);
    }

    /**
     * Creates an officer FactionMember.
     *
     * @param uuid     the player UUID
     * @param username the player username
     * @return a new officer FactionMember
     */
    public static FactionMember createOfficer(@NotNull UUID uuid, @NotNull String username) {
        return createMember(uuid, username, FactionRole.OFFICER);
    }

    /**
     * Creates a regular FactionMember.
     *
     * @param uuid     the player UUID
     * @param username the player username
     * @return a new FactionMember
     */
    public static FactionMember createRegularMember(@NotNull UUID uuid, @NotNull String username) {
        return createMember(uuid, username, FactionRole.MEMBER);
    }

    /**
     * Creates a random UUID for testing.
     *
     * @return a random UUID
     */
    public static UUID randomUuid() {
        return UUID.randomUUID();
    }

    /**
     * Creates a deterministic UUID from a seed for reproducible tests.
     *
     * @param seed the seed value
     * @return a UUID derived from the seed
     */
    public static UUID seededUuid(long seed) {
        return new UUID(seed, seed);
    }
}
