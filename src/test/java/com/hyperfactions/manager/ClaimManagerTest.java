package com.hyperfactions.manager;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.ChunkKey;
import com.hyperfactions.data.Faction;
import com.hyperfactions.manager.ClaimManager.ClaimResult;
import com.hyperfactions.testutil.MockStorage;
import com.hyperfactions.testutil.TestFactionFactory;
import com.hyperfactions.testutil.TestPlayerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ClaimManager.
 */
@DisplayName("ClaimManager")
class ClaimManagerTest {

    private MockStorage.MockFactionStorage factionStorage;
    private MockStorage.MockPlayerStorage playerStorage;
    private FactionManager factionManager;
    private PowerManager powerManager;
    private ClaimManager claimManager;

    @BeforeEach
    void setUp() {
        ConfigManager.initTestDefaults();
        factionStorage = MockStorage.factionStorage();
        playerStorage = MockStorage.playerStorage();
        factionManager = new FactionManager(factionStorage);
        powerManager = new PowerManager(playerStorage, factionManager);
        claimManager = new ClaimManager(factionManager, powerManager);
    }

    private void setupFactionWithPower(Faction faction, UUID memberUuid, double power) {
        factionStorage.addFaction(faction);
        factionManager.loadAll().join();
        playerStorage.addPlayerPower(TestPlayerFactory.createPower(memberUuid, power, 20.0));
        powerManager.loadPlayer(memberUuid).join();
        claimManager.buildIndex();
    }

    @Nested
    @DisplayName("buildIndex()")
    class BuildIndexTests {

        @Test
        @DisplayName("populates index from factions")
        void buildIndex_populatesFromFactions() {
            UUID leader = UUID.randomUUID();
            Faction faction = TestFactionFactory.builder()
                    .addLeader(leader, "Leader")
                    .addClaim("world", 0, 0, leader)
                    .addClaim("world", 1, 0, leader)
                    .build();

            setupFactionWithPower(faction, leader, 20.0);

            assertEquals(2, claimManager.getTotalClaimCount());
            assertEquals(faction.id(), claimManager.getClaimOwner("world", 0, 0));
            assertEquals(faction.id(), claimManager.getClaimOwner("world", 1, 0));
        }

        @Test
        @DisplayName("clears old index on rebuild")
        void buildIndex_clearsOldIndex() {
            UUID leader = UUID.randomUUID();
            Faction faction = TestFactionFactory.builder()
                    .addLeader(leader, "Leader")
                    .addClaim("world", 0, 0, leader)
                    .build();

            setupFactionWithPower(faction, leader, 20.0);
            assertEquals(1, claimManager.getTotalClaimCount());

            // Recreate with empty storage and rebuild
            factionStorage.clear();
            factionManager = new FactionManager(factionStorage);
            claimManager = new ClaimManager(factionManager, powerManager);
            factionManager.loadAll().join();
            claimManager.buildIndex();

            assertEquals(0, claimManager.getTotalClaimCount());
        }
    }

    @Nested
    @DisplayName("Queries")
    class QueryTests {

        @Test
        @DisplayName("getClaimOwner returns owner")
        void getClaimOwner_returnsOwner() {
            UUID leader = UUID.randomUUID();
            Faction faction = TestFactionFactory.builder()
                    .addLeader(leader, "Leader")
                    .addClaim("world", 5, 10, leader)
                    .build();
            setupFactionWithPower(faction, leader, 20.0);

            UUID owner = claimManager.getClaimOwner("world", 5, 10);

            assertEquals(faction.id(), owner);
        }

        @Test
        @DisplayName("getClaimOwner returns null for unclaimed")
        void getClaimOwner_nullForUnclaimed() {
            assertNull(claimManager.getClaimOwner("world", 100, 100));
        }

        @Test
        @DisplayName("hasAdjacentClaim checks all directions")
        void hasAdjacentClaim_checksAllDirections() {
            UUID leader = UUID.randomUUID();
            Faction faction = TestFactionFactory.builder()
                    .addLeader(leader, "Leader")
                    .addClaim("world", 5, 5, leader)
                    .build();
            setupFactionWithPower(faction, leader, 20.0);

            // Adjacent chunks (N, S, E, W)
            assertTrue(claimManager.hasAdjacentClaim("world", 5, 4, faction.id())); // North
            assertTrue(claimManager.hasAdjacentClaim("world", 5, 6, faction.id())); // South
            assertTrue(claimManager.hasAdjacentClaim("world", 6, 5, faction.id())); // East
            assertTrue(claimManager.hasAdjacentClaim("world", 4, 5, faction.id())); // West

            // Diagonal - not adjacent
            assertFalse(claimManager.hasAdjacentClaim("world", 6, 6, faction.id()));

            // Distant - not adjacent
            assertFalse(claimManager.hasAdjacentClaim("world", 10, 10, faction.id()));
        }

        @Test
        @DisplayName("isClaimed returns correct values")
        void isClaimed_returnsCorrectly() {
            UUID leader = UUID.randomUUID();
            Faction faction = TestFactionFactory.builder()
                    .addLeader(leader, "Leader")
                    .addClaim("world", 5, 5, leader)
                    .build();
            setupFactionWithPower(faction, leader, 20.0);

            assertTrue(claimManager.isClaimed("world", 5, 5));
            assertFalse(claimManager.isClaimed("world", 10, 10));
        }

        @Test
        @DisplayName("getFactionClaims returns all faction claims")
        void getFactionClaims_returnsAll() {
            UUID leader = UUID.randomUUID();
            Faction faction = TestFactionFactory.builder()
                    .addLeader(leader, "Leader")
                    .addClaim("world", 0, 0, leader)
                    .addClaim("world", 1, 0, leader)
                    .addClaim("world", 2, 0, leader)
                    .build();
            setupFactionWithPower(faction, leader, 20.0);

            Set<ChunkKey> claims = claimManager.getFactionClaims(faction.id());

            assertEquals(3, claims.size());
        }
    }

    @Nested
    @DisplayName("claim()")
    class ClaimTests {

        @Test
        @DisplayName("succeeds when valid")
        void claim_success_whenValid() {
            UUID leader = UUID.randomUUID();
            Faction faction = TestFactionFactory.builder()
                    .addLeader(leader, "Leader")
                    .build();
            setupFactionWithPower(faction, leader, 20.0);

            ClaimResult result = claimManager.claim(leader, "world", 5, 5);

            assertEquals(ClaimResult.SUCCESS, result);
            assertEquals(faction.id(), claimManager.getClaimOwner("world", 5, 5));
        }

        @Test
        @DisplayName("fails when not in faction")
        void claim_fails_notInFaction() {
            UUID randomPlayer = UUID.randomUUID();

            ClaimResult result = claimManager.claim(randomPlayer, "world", 5, 5);

            assertEquals(ClaimResult.NOT_IN_FACTION, result);
        }

        @Test
        @DisplayName("fails when not officer")
        void claim_fails_notOfficer() {
            UUID leader = UUID.randomUUID();
            UUID member = UUID.randomUUID();
            Faction faction = TestFactionFactory.builder()
                    .addLeader(leader, "Leader")
                    .addRegularMember(member, "Member")
                    .build();
            setupFactionWithPower(faction, leader, 20.0);

            ClaimResult result = claimManager.claim(member, "world", 5, 5);

            assertEquals(ClaimResult.NOT_OFFICER, result);
        }

        @Test
        @DisplayName("fails when already claimed by self")
        void claim_fails_alreadyClaimedSelf() {
            UUID leader = UUID.randomUUID();
            Faction faction = TestFactionFactory.builder()
                    .addLeader(leader, "Leader")
                    .addClaim("world", 5, 5, leader)
                    .build();
            setupFactionWithPower(faction, leader, 20.0);

            ClaimResult result = claimManager.claim(leader, "world", 5, 5);

            assertEquals(ClaimResult.ALREADY_CLAIMED_SELF, result);
        }

        @Test
        @DisplayName("fails when already claimed by other")
        void claim_fails_alreadyClaimedOther() {
            UUID leader1 = UUID.randomUUID();
            UUID leader2 = UUID.randomUUID();

            Faction faction1 = TestFactionFactory.builder()
                    .addLeader(leader1, "Leader1")
                    .addClaim("world", 5, 5, leader1)
                    .build();
            Faction faction2 = TestFactionFactory.builder()
                    .addLeader(leader2, "Leader2")
                    .build();

            factionStorage.addFaction(faction1);
            factionStorage.addFaction(faction2);
            factionManager.loadAll().join();
            playerStorage.addPlayerPower(TestPlayerFactory.createPower(leader1, 20.0, 20.0));
            playerStorage.addPlayerPower(TestPlayerFactory.createPower(leader2, 20.0, 20.0));
            powerManager.loadPlayer(leader1).join();
            powerManager.loadPlayer(leader2).join();
            claimManager.buildIndex();

            ClaimResult result = claimManager.claim(leader2, "world", 5, 5);

            assertEquals(ClaimResult.ALREADY_CLAIMED_OTHER, result);
        }

        @Test
        @DisplayName("fails when max claims reached")
        void claim_fails_maxClaimsReached() {
            UUID leader = UUID.randomUUID();
            // With power 4.0 and powerPerClaim 2.0, max claims = 2
            Faction faction = TestFactionFactory.builder()
                    .addLeader(leader, "Leader")
                    .addClaim("world", 0, 0, leader)
                    .addClaim("world", 1, 0, leader)
                    .build();

            factionStorage.addFaction(faction);
            factionManager.loadAll().join();
            playerStorage.addPlayerPower(TestPlayerFactory.createPower(leader, 4.0, 20.0));
            powerManager.loadPlayer(leader).join();
            claimManager.buildIndex();

            ClaimResult result = claimManager.claim(leader, "world", 2, 0);

            assertEquals(ClaimResult.MAX_CLAIMS_REACHED, result);
        }
    }

    @Nested
    @DisplayName("unclaim()")
    class UnclaimTests {

        @Test
        @DisplayName("succeeds when valid")
        void unclaim_success_whenValid() {
            UUID leader = UUID.randomUUID();
            Faction faction = TestFactionFactory.builder()
                    .addLeader(leader, "Leader")
                    .addClaim("world", 5, 5, leader)
                    .build();
            setupFactionWithPower(faction, leader, 20.0);

            ClaimResult result = claimManager.unclaim(leader, "world", 5, 5);

            assertEquals(ClaimResult.SUCCESS, result);
            assertNull(claimManager.getClaimOwner("world", 5, 5));
        }

        @Test
        @DisplayName("fails when chunk not claimed")
        void unclaim_fails_notClaimed() {
            UUID leader = UUID.randomUUID();
            Faction faction = TestFactionFactory.builder()
                    .addLeader(leader, "Leader")
                    .build();
            setupFactionWithPower(faction, leader, 20.0);

            ClaimResult result = claimManager.unclaim(leader, "world", 5, 5);

            assertEquals(ClaimResult.CHUNK_NOT_CLAIMED, result);
        }

        @Test
        @DisplayName("fails when not your claim")
        void unclaim_fails_notYourClaim() {
            UUID leader1 = UUID.randomUUID();
            UUID leader2 = UUID.randomUUID();

            Faction faction1 = TestFactionFactory.builder()
                    .addLeader(leader1, "Leader1")
                    .addClaim("world", 5, 5, leader1)
                    .build();
            Faction faction2 = TestFactionFactory.builder()
                    .addLeader(leader2, "Leader2")
                    .build();

            factionStorage.addFaction(faction1);
            factionStorage.addFaction(faction2);
            factionManager.loadAll().join();
            playerStorage.addPlayerPower(TestPlayerFactory.createPower(leader1, 20.0, 20.0));
            playerStorage.addPlayerPower(TestPlayerFactory.createPower(leader2, 20.0, 20.0));
            powerManager.loadPlayer(leader1).join();
            powerManager.loadPlayer(leader2).join();
            claimManager.buildIndex();

            ClaimResult result = claimManager.unclaim(leader2, "world", 5, 5);

            assertEquals(ClaimResult.NOT_YOUR_CLAIM, result);
        }

        @Test
        @DisplayName("fails when home in chunk")
        void unclaim_fails_homeInChunk() {
            UUID leader = UUID.randomUUID();
            Faction faction = TestFactionFactory.builder()
                    .addLeader(leader, "Leader")
                    .addClaim("world", 5, 5, leader)
                    .home("world", 160.0, 64.0, 160.0, leader) // Block 160 = chunk 5 (160 >> 5)
                    .build();
            setupFactionWithPower(faction, leader, 20.0);

            ClaimResult result = claimManager.unclaim(leader, "world", 5, 5);

            assertEquals(ClaimResult.CANNOT_UNCLAIM_HOME, result);
        }
    }

    @Nested
    @DisplayName("overclaim()")
    class OverclaimTests {

        @Test
        @DisplayName("succeeds when target is raidable")
        void overclaim_success_whenRaidable() {
            UUID attacker = UUID.randomUUID();
            UUID defender = UUID.randomUUID();

            // Defender has 5 claims but only power for 2 (raidable)
            Faction defenderFaction = TestFactionFactory.builder()
                    .addLeader(defender, "Defender")
                    .addClaims("world", 0, 0, 5, defender)
                    .build();

            // Attacker has enough power
            Faction attackerFaction = TestFactionFactory.builder()
                    .addLeader(attacker, "Attacker")
                    .build();

            factionStorage.addFaction(defenderFaction);
            factionStorage.addFaction(attackerFaction);
            factionManager.loadAll().join();
            playerStorage.addPlayerPower(TestPlayerFactory.createPower(defender, 4.0, 20.0)); // Only 2 claims worth
            playerStorage.addPlayerPower(TestPlayerFactory.createPower(attacker, 20.0, 20.0));
            powerManager.loadPlayer(defender).join();
            powerManager.loadPlayer(attacker).join();
            claimManager.buildIndex();

            ClaimResult result = claimManager.overclaim(attacker, "world", 0, 0);

            assertEquals(ClaimResult.SUCCESS, result);
            assertEquals(attackerFaction.id(), claimManager.getClaimOwner("world", 0, 0));
        }

        @Test
        @DisplayName("fails when target has power")
        void overclaim_fails_hasStrength() {
            UUID attacker = UUID.randomUUID();
            UUID defender = UUID.randomUUID();

            // Defender has 2 claims and power for 10 (not raidable)
            Faction defenderFaction = TestFactionFactory.builder()
                    .addLeader(defender, "Defender")
                    .addClaims("world", 0, 0, 2, defender)
                    .build();

            Faction attackerFaction = TestFactionFactory.builder()
                    .addLeader(attacker, "Attacker")
                    .build();

            factionStorage.addFaction(defenderFaction);
            factionStorage.addFaction(attackerFaction);
            factionManager.loadAll().join();
            playerStorage.addPlayerPower(TestPlayerFactory.createPower(defender, 20.0, 20.0));
            playerStorage.addPlayerPower(TestPlayerFactory.createPower(attacker, 20.0, 20.0));
            powerManager.loadPlayer(defender).join();
            powerManager.loadPlayer(attacker).join();
            claimManager.buildIndex();

            ClaimResult result = claimManager.overclaim(attacker, "world", 0, 0);

            assertEquals(ClaimResult.TARGET_HAS_POWER, result);
        }

        @Test
        @DisplayName("fails when ally claim")
        void overclaim_fails_allyClaim() {
            UUID attacker = UUID.randomUUID();
            UUID defender = UUID.randomUUID();

            Faction defenderFaction = TestFactionFactory.builder()
                    .addLeader(defender, "Defender")
                    .addClaim("world", 0, 0, defender)
                    .build();

            // Make attacker allied with defender
            Faction attackerFaction = TestFactionFactory.builder()
                    .addLeader(attacker, "Attacker")
                    .addAlly(defenderFaction.id())
                    .build();

            factionStorage.addFaction(defenderFaction);
            factionStorage.addFaction(attackerFaction);
            factionManager.loadAll().join();
            playerStorage.addPlayerPower(TestPlayerFactory.createPower(defender, 0.0, 20.0)); // No power
            playerStorage.addPlayerPower(TestPlayerFactory.createPower(attacker, 20.0, 20.0));
            powerManager.loadPlayer(defender).join();
            powerManager.loadPlayer(attacker).join();
            claimManager.buildIndex();

            ClaimResult result = claimManager.overclaim(attacker, "world", 0, 0);

            assertEquals(ClaimResult.ALREADY_CLAIMED_ALLY, result);
        }
    }

    @Nested
    @DisplayName("unclaimAll()")
    class UnclaimAllTests {

        @Test
        @DisplayName("removes all claims for faction")
        void unclaimAll_removesAllClaims() {
            UUID leader = UUID.randomUUID();
            Faction faction = TestFactionFactory.builder()
                    .addLeader(leader, "Leader")
                    .addClaims("world", 0, 0, 10, leader)
                    .build();
            setupFactionWithPower(faction, leader, 20.0);

            assertEquals(10, claimManager.getTotalClaimCount());

            claimManager.unclaimAll(faction.id());

            assertEquals(0, claimManager.getTotalClaimCount());
            assertTrue(claimManager.getFactionClaims(faction.id()).isEmpty());
        }
    }
}
