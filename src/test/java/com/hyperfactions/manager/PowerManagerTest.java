package com.hyperfactions.manager;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.PlayerPower;
import com.hyperfactions.testutil.MockStorage;
import com.hyperfactions.testutil.TestFactionFactory;
import com.hyperfactions.testutil.TestPlayerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PowerManager.
 */
@DisplayName("PowerManager")
class PowerManagerTest {

    private MockStorage.MockPlayerStorage playerStorage;
    private MockStorage.MockFactionStorage factionStorage;
    private FactionManager factionManager;
    private PowerManager powerManager;

    @BeforeEach
    void setUp() {
        com.hyperfactions.config.ConfigManager.initTestDefaults();
        playerStorage = MockStorage.playerStorage();
        factionStorage = MockStorage.factionStorage();
        factionManager = new FactionManager(factionStorage);
        powerManager = new PowerManager(playerStorage, factionManager);
    }

    @Nested
    @DisplayName("getPlayerPower()")
    class GetPlayerPowerTests {

        @Test
        @DisplayName("returns existing power from cache")
        void getPlayerPower_returnsCached() {
            UUID uuid = UUID.randomUUID();
            PlayerPower power = TestPlayerFactory.createPower(uuid, 15.0, 20.0);
            playerStorage.addPlayerPower(power);

            // Load into cache
            powerManager.loadPlayer(uuid).join();

            PlayerPower result = powerManager.getPlayerPower(uuid);
            assertNotNull(result);
            assertEquals(15.0, result.power());
        }

        @Test
        @DisplayName("creates default power if not in cache or storage")
        void getPlayerPower_createsDefaultIfMissing() {
            UUID uuid = UUID.randomUUID();

            PlayerPower result = powerManager.getPlayerPower(uuid);

            assertNotNull(result);
            assertEquals(uuid, result.uuid());
            // Should have default starting power (typically 10.0)
            assertTrue(result.power() > 0);
        }
    }

    @Nested
    @DisplayName("loadPlayer()")
    class LoadPlayerTests {

        @Test
        @DisplayName("loads from storage if exists")
        void loadPlayer_loadsFromStorage() {
            UUID uuid = UUID.randomUUID();
            PlayerPower stored = TestPlayerFactory.createPower(uuid, 18.5, 25.0);
            playerStorage.addPlayerPower(stored);

            CompletableFuture<PlayerPower> future = powerManager.loadPlayer(uuid);
            PlayerPower result = future.join();

            assertNotNull(result);
            assertEquals(18.5, result.power());
            assertEquals(25.0, result.maxPower());
        }

        @Test
        @DisplayName("returns default if not in storage")
        void loadPlayer_defaultIfMissing() {
            UUID uuid = UUID.randomUUID();

            CompletableFuture<PlayerPower> future = powerManager.loadPlayer(uuid);
            PlayerPower result = future.join();

            assertNotNull(result);
            assertEquals(uuid, result.uuid());
        }
    }

    @Nested
    @DisplayName("applyDeathPenalty()")
    class ApplyDeathPenaltyTests {

        @Test
        @DisplayName("reduces power by config penalty amount")
        void applyDeathPenalty_reducesPower() {
            UUID uuid = UUID.randomUUID();
            // Default death penalty is 1.0
            PlayerPower power = TestPlayerFactory.createPower(uuid, 10.0, 20.0);
            playerStorage.addPlayerPower(power);
            powerManager.loadPlayer(uuid).join();

            double newPower = powerManager.applyDeathPenalty(uuid);
            PlayerPower result = powerManager.getPlayerPower(uuid);

            // With default 1.0 penalty: 10.0 - 1.0 = 9.0
            assertEquals(9.0, newPower);
            assertEquals(9.0, result.power());
        }

        @Test
        @DisplayName("clamps at zero when power is low")
        void applyDeathPenalty_clampsAtZero() {
            UUID uuid = UUID.randomUUID();
            // Start with 0.5 power, penalty of 1.0 should clamp to 0
            PlayerPower power = TestPlayerFactory.createPower(uuid, 0.5, 20.0);
            playerStorage.addPlayerPower(power);
            powerManager.loadPlayer(uuid).join();

            double newPower = powerManager.applyDeathPenalty(uuid);

            assertEquals(0.0, newPower);
        }

        @Test
        @DisplayName("saves to storage after penalty")
        void applyDeathPenalty_savesToStorage() {
            UUID uuid = UUID.randomUUID();
            PlayerPower power = TestPlayerFactory.createPower(uuid, 10.0, 20.0);
            playerStorage.addPlayerPower(power);
            powerManager.loadPlayer(uuid).join();

            powerManager.applyDeathPenalty(uuid);

            // The manager saves immediately after applyDeathPenalty
            PlayerPower stored = playerStorage.get(uuid);
            assertNotNull(stored);
            // With default 1.0 penalty: 10.0 - 1.0 = 9.0
            assertEquals(9.0, stored.power());
        }
    }

    @Nested
    @DisplayName("regeneratePower()")
    class RegeneratePowerTests {

        @Test
        @DisplayName("increases power by regen amount")
        void regeneratePower_increasesPower() {
            UUID uuid = UUID.randomUUID();
            PlayerPower power = TestPlayerFactory.createPower(uuid, 10.0, 20.0);
            playerStorage.addPlayerPower(power);
            powerManager.loadPlayer(uuid).join();

            powerManager.regeneratePower(uuid, 2.5);
            PlayerPower result = powerManager.getPlayerPower(uuid);

            assertEquals(12.5, result.power());
        }

        @Test
        @DisplayName("respects max power limit")
        void regeneratePower_respectsMaxPower() {
            UUID uuid = UUID.randomUUID();
            PlayerPower power = TestPlayerFactory.createPower(uuid, 18.0, 20.0);
            playerStorage.addPlayerPower(power);
            powerManager.loadPlayer(uuid).join();

            powerManager.regeneratePower(uuid, 5.0);
            PlayerPower result = powerManager.getPlayerPower(uuid);

            assertEquals(20.0, result.power());
        }
    }

    @Nested
    @DisplayName("Faction Power Aggregation")
    class FactionPowerAggregationTests {

        @Test
        @DisplayName("getFactionPower aggregates member power")
        void getFactionPower_aggregatesMemberPower() {
            UUID leader = UUID.randomUUID();
            UUID member1 = UUID.randomUUID();
            UUID member2 = UUID.randomUUID();

            Faction faction = TestFactionFactory.builder()
                    .addLeader(leader, "Leader")
                    .addRegularMember(member1, "Member1")
                    .addRegularMember(member2, "Member2")
                    .build();
            factionStorage.addFaction(faction);
            factionManager.loadAll().join();

            // Set up power for each player
            playerStorage.addPlayerPower(TestPlayerFactory.createPower(leader, 15.0, 20.0));
            playerStorage.addPlayerPower(TestPlayerFactory.createPower(member1, 10.0, 20.0));
            playerStorage.addPlayerPower(TestPlayerFactory.createPower(member2, 5.0, 20.0));
            powerManager.loadPlayer(leader).join();
            powerManager.loadPlayer(member1).join();
            powerManager.loadPlayer(member2).join();

            double totalPower = powerManager.getFactionPower(faction.id());

            assertEquals(30.0, totalPower); // 15 + 10 + 5
        }

        @Test
        @DisplayName("getFactionMaxPower aggregates member max power")
        void getFactionMaxPower_aggregatesMaxPower() {
            UUID leader = UUID.randomUUID();
            UUID member = UUID.randomUUID();

            Faction faction = TestFactionFactory.builder()
                    .addLeader(leader, "Leader")
                    .addRegularMember(member, "Member")
                    .build();
            factionStorage.addFaction(faction);
            factionManager.loadAll().join();

            playerStorage.addPlayerPower(TestPlayerFactory.createPower(leader, 10.0, 25.0));
            playerStorage.addPlayerPower(TestPlayerFactory.createPower(member, 10.0, 20.0));
            powerManager.loadPlayer(leader).join();
            powerManager.loadPlayer(member).join();

            double totalMax = powerManager.getFactionMaxPower(faction.id());

            assertEquals(45.0, totalMax); // 25 + 20
        }

        @Test
        @DisplayName("getFactionClaimCapacity calculates from power")
        void getFactionClaimCapacity_calculatesFromPower() {
            UUID leader = UUID.randomUUID();

            Faction faction = TestFactionFactory.builder()
                    .addLeader(leader, "Leader")
                    .build();
            factionStorage.addFaction(faction);
            factionManager.loadAll().join();

            playerStorage.addPlayerPower(TestPlayerFactory.createPower(leader, 20.0, 20.0));
            powerManager.loadPlayer(leader).join();

            int capacity = powerManager.getFactionClaimCapacity(faction.id());

            // With default powerPerClaim of 2.0, 20 power = 10 claims
            assertEquals(10, capacity);
        }
    }

    @Nested
    @DisplayName("isFactionRaidable()")
    class IsFactionRaidableTests {

        @Test
        @DisplayName("returns true when overclaimed")
        void isFactionRaidable_trueWhenOverclaimed() {
            UUID leader = UUID.randomUUID();

            // Create faction with many claims but low power
            Faction faction = TestFactionFactory.builder()
                    .addLeader(leader, "Leader")
                    .addClaims("world", 0, 0, 15, leader) // 15 claims
                    .build();
            factionStorage.addFaction(faction);
            factionManager.loadAll().join();

            // Low power - only 10.0 (supports 5 claims with default 2.0 per claim)
            playerStorage.addPlayerPower(TestPlayerFactory.createPower(leader, 10.0, 20.0));
            powerManager.loadPlayer(leader).join();

            boolean raidable = powerManager.isFactionRaidable(faction.id());

            assertTrue(raidable);
        }

        @Test
        @DisplayName("returns false when under capacity")
        void isFactionRaidable_falseWhenUnder() {
            UUID leader = UUID.randomUUID();

            // Create faction with few claims and high power
            Faction faction = TestFactionFactory.builder()
                    .addLeader(leader, "Leader")
                    .addClaims("world", 0, 0, 2, leader) // 2 claims
                    .build();
            factionStorage.addFaction(faction);
            factionManager.loadAll().join();

            // High power - 20.0 (supports 10 claims)
            playerStorage.addPlayerPower(TestPlayerFactory.createPower(leader, 20.0, 20.0));
            powerManager.loadPlayer(leader).join();

            boolean raidable = powerManager.isFactionRaidable(faction.id());

            assertFalse(raidable);
        }
    }

    @Nested
    @DisplayName("Online Tracking")
    class OnlineTrackingTests {

        @Test
        @DisplayName("playerOnline loads player data")
        void playerOnline_loadsPlayer() {
            UUID uuid = UUID.randomUUID();
            PlayerPower stored = TestPlayerFactory.createPower(uuid, 15.0, 20.0);
            playerStorage.addPlayerPower(stored);

            powerManager.playerOnline(uuid);

            // Player should be loaded into cache
            PlayerPower cached = powerManager.getPlayerPower(uuid);
            assertEquals(15.0, cached.power());
        }

        @Test
        @DisplayName("playerOffline saves player data")
        void playerOffline_savesPlayer() {
            UUID uuid = UUID.randomUUID();
            playerStorage.addPlayerPower(TestPlayerFactory.createPower(uuid, 10.0, 20.0));
            powerManager.loadPlayer(uuid).join();

            // Modify power
            powerManager.regeneratePower(uuid, 5.0);

            // Go offline - should save
            powerManager.playerOffline(uuid);

            PlayerPower stored = playerStorage.get(uuid);
            assertNotNull(stored);
            assertEquals(15.0, stored.power());
        }
    }
}
