package com.hyperfactions.protection;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.RelationType;
import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneType;
import com.hyperfactions.integration.permissions.HyperPermsIntegration;
import com.hyperfactions.manager.*;
import com.hyperfactions.protection.ProtectionChecker.InteractionType;
import com.hyperfactions.protection.ProtectionChecker.ProtectionResult;
import com.hyperfactions.protection.ProtectionChecker.PvPResult;
import com.hyperfactions.testutil.MockStorage;
import com.hyperfactions.testutil.TestFactionFactory;
import com.hyperfactions.testutil.TestPlayerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProtectionChecker.
 */
@DisplayName("ProtectionChecker")
class ProtectionCheckerTest {

    private MockStorage.MockFactionStorage factionStorage;
    private MockStorage.MockPlayerStorage playerStorage;
    private MockStorage.MockZoneStorage zoneStorage;
    private FactionManager factionManager;
    private PowerManager powerManager;
    private ClaimManager claimManager;
    private ZoneManager zoneManager;
    private RelationManager relationManager;
    private CombatTagManager combatTagManager;
    private ProtectionChecker protectionChecker;

    @BeforeEach
    void setUp() {
        // Enable test mode so permission checks return false (no bypass)
        HyperPermsIntegration.setTestMode(true);
        com.hyperfactions.config.ConfigManager.initTestDefaults();

        factionStorage = MockStorage.factionStorage();
        playerStorage = MockStorage.playerStorage();
        zoneStorage = MockStorage.zoneStorage();

        factionManager = new FactionManager(factionStorage);
        powerManager = new PowerManager(playerStorage, factionManager);
        claimManager = new ClaimManager(factionManager, powerManager);
        zoneManager = new ZoneManager(zoneStorage, claimManager);
        relationManager = new RelationManager(factionManager);
        combatTagManager = new CombatTagManager();

        protectionChecker = new ProtectionChecker(
                factionManager,
                claimManager,
                zoneManager,
                relationManager,
                combatTagManager
        );
    }

    @AfterEach
    void tearDown() {
        // Restore normal mode after tests
        HyperPermsIntegration.setTestMode(false);
    }

    private void setupFaction(Faction faction, UUID memberUuid) {
        factionStorage.addFaction(faction);
        factionManager.loadAll().join();
        playerStorage.addPlayerPower(TestPlayerFactory.createPower(memberUuid, 20.0, 20.0));
        powerManager.loadPlayer(memberUuid).join();
        claimManager.buildIndex();
    }

    private void setupZone(Zone zone) {
        zoneStorage.addZone(zone);
        zoneManager.loadAll().join();
    }

    @Nested
    @DisplayName("Wilderness Interactions")
    class WildernessTests {

        @Test
        @DisplayName("allows build in wilderness")
        void canInteract_allowedInWilderness() {
            UUID player = UUID.randomUUID();

            ProtectionResult result = protectionChecker.canInteractChunk(
                    player, "world", 100, 100, InteractionType.BUILD);

            assertEquals(ProtectionResult.ALLOWED_WILDERNESS, result);
            assertTrue(protectionChecker.isAllowed(result));
        }

        @Test
        @DisplayName("allows container access in wilderness")
        void canInteract_containerInWilderness() {
            UUID player = UUID.randomUUID();

            ProtectionResult result = protectionChecker.canInteractChunk(
                    player, "world", 100, 100, InteractionType.CONTAINER);

            assertEquals(ProtectionResult.ALLOWED_WILDERNESS, result);
        }
    }

    @Nested
    @DisplayName("Own Claim Interactions")
    class OwnClaimTests {

        @Test
        @DisplayName("allows build in own claim")
        void canInteract_allowedInOwnClaim() {
            UUID leader = UUID.randomUUID();
            Faction faction = TestFactionFactory.builder()
                    .addLeader(leader, "Leader")
                    .addClaim("world", 5, 5, leader)
                    .build();
            setupFaction(faction, leader);

            ProtectionResult result = protectionChecker.canInteractChunk(
                    leader, "world", 5, 5, InteractionType.BUILD);

            assertEquals(ProtectionResult.ALLOWED_OWN_CLAIM, result);
        }

        @Test
        @DisplayName("allows container in own claim")
        void canInteract_containerInOwnClaim() {
            UUID leader = UUID.randomUUID();
            Faction faction = TestFactionFactory.builder()
                    .addLeader(leader, "Leader")
                    .addClaim("world", 5, 5, leader)
                    .build();
            setupFaction(faction, leader);

            ProtectionResult result = protectionChecker.canInteractChunk(
                    leader, "world", 5, 5, InteractionType.CONTAINER);

            assertEquals(ProtectionResult.ALLOWED_OWN_CLAIM, result);
        }
    }

    @Nested
    @DisplayName("Ally Claim Interactions")
    class AllyClaimTests {

        @Test
        @DisplayName("allows build in ally claim")
        void canInteract_allowedInAllyClaim() {
            UUID leader1 = UUID.randomUUID();
            UUID leader2 = UUID.randomUUID();

            // Create two allied factions
            Faction faction1 = TestFactionFactory.builder()
                    .addLeader(leader1, "Leader1")
                    .build();
            // Enable ally build permissions for faction2
            com.hyperfactions.data.FactionPermissions allyBuildPerms =
                    com.hyperfactions.data.FactionPermissions.defaults()
                            .set("allyBreak", true)
                            .set("allyPlace", true);
            Faction faction2 = TestFactionFactory.builder()
                    .addLeader(leader2, "Leader2")
                    .addClaim("world", 5, 5, leader2)
                    .addAlly(faction1.id())
                    .build()
                    .withPermissions(allyBuildPerms);

            // Update faction1 to be allied with faction2
            faction1 = TestFactionFactory.builder()
                    .id(faction1.id())
                    .addLeader(leader1, "Leader1")
                    .addAlly(faction2.id())
                    .build();

            factionStorage.addFaction(faction1);
            factionStorage.addFaction(faction2);
            factionManager.loadAll().join();
            playerStorage.addPlayerPower(TestPlayerFactory.createPower(leader1, 20.0));
            playerStorage.addPlayerPower(TestPlayerFactory.createPower(leader2, 20.0));
            powerManager.loadPlayer(leader1).join();
            powerManager.loadPlayer(leader2).join();
            claimManager.buildIndex();

            // leader1 tries to build in faction2's claim
            ProtectionResult result = protectionChecker.canInteractChunk(
                    leader1, "world", 5, 5, InteractionType.BUILD);

            assertEquals(ProtectionResult.ALLOWED_ALLY_CLAIM, result);
        }
    }

    @Nested
    @DisplayName("Enemy Claim Interactions")
    class EnemyClaimTests {

        @Test
        @DisplayName("denies build in enemy claim")
        void canInteract_deniedInEnemyClaim() {
            UUID leader1 = UUID.randomUUID();
            UUID leader2 = UUID.randomUUID();

            // Create two enemy factions
            Faction faction1 = TestFactionFactory.builder()
                    .addLeader(leader1, "Leader1")
                    .build();
            Faction faction2 = TestFactionFactory.builder()
                    .addLeader(leader2, "Leader2")
                    .addClaim("world", 5, 5, leader2)
                    .addEnemy(faction1.id())
                    .build();

            faction1 = TestFactionFactory.builder()
                    .id(faction1.id())
                    .addLeader(leader1, "Leader1")
                    .addEnemy(faction2.id())
                    .build();

            factionStorage.addFaction(faction1);
            factionStorage.addFaction(faction2);
            factionManager.loadAll().join();
            playerStorage.addPlayerPower(TestPlayerFactory.createPower(leader1, 20.0));
            playerStorage.addPlayerPower(TestPlayerFactory.createPower(leader2, 20.0));
            powerManager.loadPlayer(leader1).join();
            powerManager.loadPlayer(leader2).join();
            claimManager.buildIndex();

            ProtectionResult result = protectionChecker.canInteractChunk(
                    leader1, "world", 5, 5, InteractionType.BUILD);

            assertEquals(ProtectionResult.DENIED_ENEMY_CLAIM, result);
            assertFalse(protectionChecker.isAllowed(result));
        }
    }

    @Nested
    @DisplayName("Neutral Claim Interactions")
    class NeutralClaimTests {

        @Test
        @DisplayName("denies build in neutral claim")
        void canInteract_deniedInNeutralClaim() {
            UUID leader1 = UUID.randomUUID();
            UUID leader2 = UUID.randomUUID();

            // Create two factions with no relation (neutral)
            Faction faction1 = TestFactionFactory.builder()
                    .addLeader(leader1, "Leader1")
                    .build();
            Faction faction2 = TestFactionFactory.builder()
                    .addLeader(leader2, "Leader2")
                    .addClaim("world", 5, 5, leader2)
                    .build();

            factionStorage.addFaction(faction1);
            factionStorage.addFaction(faction2);
            factionManager.loadAll().join();
            playerStorage.addPlayerPower(TestPlayerFactory.createPower(leader1, 20.0));
            playerStorage.addPlayerPower(TestPlayerFactory.createPower(leader2, 20.0));
            powerManager.loadPlayer(leader1).join();
            powerManager.loadPlayer(leader2).join();
            claimManager.buildIndex();

            ProtectionResult result = protectionChecker.canInteractChunk(
                    leader1, "world", 5, 5, InteractionType.BUILD);

            assertEquals(ProtectionResult.DENIED_NEUTRAL_CLAIM, result);
        }
    }

    @Nested
    @DisplayName("SafeZone Interactions")
    class SafeZoneTests {

        @Test
        @DisplayName("denies build in SafeZone")
        void canInteract_deniedInSafeZone() {
            UUID admin = UUID.randomUUID();
            Zone safeZone = Zone.create("Spawn", ZoneType.SAFE, "world", 0, 0, admin);
            setupZone(safeZone);

            UUID player = UUID.randomUUID();
            ProtectionResult result = protectionChecker.canInteractChunk(
                    player, "world", 0, 0, InteractionType.BUILD);

            assertEquals(ProtectionResult.DENIED_SAFEZONE, result);
        }
    }

    @Nested
    @DisplayName("WarZone Interactions")
    class WarZoneTests {

        @Test
        @DisplayName("allows build in WarZone with build flag")
        void canInteract_allowedInWarZoneWithFlag() {
            UUID admin = UUID.randomUUID();
            Zone warZone = Zone.create("Warzone", ZoneType.WAR, "world", 0, 0, admin)
                    .withFlag("build_allowed", true);
            setupZone(warZone);

            UUID player = UUID.randomUUID();
            ProtectionResult result = protectionChecker.canInteractChunk(
                    player, "world", 0, 0, InteractionType.BUILD);

            assertEquals(ProtectionResult.ALLOWED_WARZONE, result);
        }
    }

    @Nested
    @DisplayName("PvP Protection")
    class PvPTests {

        @Test
        @DisplayName("denies PvP in SafeZone")
        void canDamagePlayer_deniedInSafeZone() {
            UUID admin = UUID.randomUUID();
            Zone safeZone = Zone.create("Spawn", ZoneType.SAFE, "world", 0, 0, admin);
            setupZone(safeZone);

            UUID attacker = UUID.randomUUID();
            UUID defender = UUID.randomUUID();

            PvPResult result = protectionChecker.canDamagePlayerChunk(
                    attacker, defender, "world", 0, 0);

            assertEquals(PvPResult.DENIED_SAFEZONE, result);
            assertFalse(protectionChecker.isAllowed(result));
        }

        @Test
        @DisplayName("denies PvP against same faction")
        void canDamagePlayer_deniedSameFaction() {
            UUID leader = UUID.randomUUID();
            UUID member = UUID.randomUUID();

            Faction faction = TestFactionFactory.builder()
                    .addLeader(leader, "Leader")
                    .addRegularMember(member, "Member")
                    .build();

            factionStorage.addFaction(faction);
            factionManager.loadAll().join();

            PvPResult result = protectionChecker.canDamagePlayerChunk(
                    leader, member, "world", 100, 100);

            assertEquals(PvPResult.DENIED_SAME_FACTION, result);
        }

        @Test
        @DisplayName("denies PvP against ally")
        void canDamagePlayer_deniedAlly() {
            UUID leader1 = UUID.randomUUID();
            UUID leader2 = UUID.randomUUID();

            Faction[] factions = TestFactionFactory.createAlliedPair();
            factionStorage.addFaction(factions[0]);
            factionStorage.addFaction(factions[1]);
            factionManager.loadAll().join();

            UUID player1 = factions[0].getLeaderId();
            UUID player2 = factions[1].getLeaderId();

            PvPResult result = protectionChecker.canDamagePlayerChunk(
                    player1, player2, "world", 100, 100);

            assertEquals(PvPResult.DENIED_ALLY, result);
        }

        @Test
        @DisplayName("denies PvP when defender has spawn protection")
        void canDamagePlayer_deniedSpawnProtected() {
            UUID attacker = UUID.randomUUID();
            UUID defender = UUID.randomUUID();

            // Give defender spawn protection
            combatTagManager.applySpawnProtection(defender, 60, "world", 100, 100);

            PvPResult result = protectionChecker.canDamagePlayerChunk(
                    attacker, defender, "world", 100, 100);

            assertEquals(PvPResult.DENIED_SPAWN_PROTECTED, result);
        }

        @Test
        @DisplayName("allows PvP in WarZone")
        void canDamagePlayer_allowedInWarZone() {
            UUID admin = UUID.randomUUID();
            Zone warZone = Zone.create("Arena", ZoneType.WAR, "world", 0, 0, admin);
            setupZone(warZone);

            UUID attacker = UUID.randomUUID();
            UUID defender = UUID.randomUUID();

            PvPResult result = protectionChecker.canDamagePlayerChunk(
                    attacker, defender, "world", 0, 0);

            assertEquals(PvPResult.ALLOWED_WARZONE, result);
            assertTrue(protectionChecker.isAllowed(result));
        }

        @Test
        @DisplayName("allows PvP against enemies in wilderness")
        void canDamagePlayer_allowedAgainstEnemy() {
            UUID leader1 = UUID.randomUUID();
            UUID leader2 = UUID.randomUUID();

            Faction[] factions = TestFactionFactory.createEnemyPair();
            factionStorage.addFaction(factions[0]);
            factionStorage.addFaction(factions[1]);
            factionManager.loadAll().join();

            UUID player1 = factions[0].getLeaderId();
            UUID player2 = factions[1].getLeaderId();

            PvPResult result = protectionChecker.canDamagePlayerChunk(
                    player1, player2, "world", 100, 100);

            assertEquals(PvPResult.ALLOWED, result);
        }
    }

    @Nested
    @DisplayName("Convenience Methods")
    class ConvenienceMethodTests {

        @Test
        @DisplayName("canBuild returns boolean correctly")
        void canBuild_returnsBoolean() {
            UUID player = UUID.randomUUID();

            assertTrue(protectionChecker.canBuild(player, "world", 100.0, 100.0));
        }

        @Test
        @DisplayName("canAccessContainer returns boolean correctly")
        void canAccessContainer_returnsBoolean() {
            UUID player = UUID.randomUUID();

            assertTrue(protectionChecker.canAccessContainer(player, "world", 100.0, 100.0));
        }
    }

    @Nested
    @DisplayName("Denial Messages")
    class DenialMessageTests {

        @Test
        @Disabled("Requires Hytale server I18nModule on test classpath")
        @DisplayName("getDenialMessage returns appropriate message")
        void getDenialMessage_returnsMessage() {
            String safeZoneMsg = protectionChecker.getDenialMessage(ProtectionResult.DENIED_SAFEZONE);
            assertTrue(safeZoneMsg.contains("SafeZone"));

            String enemyMsg = protectionChecker.getDenialMessage(ProtectionResult.DENIED_ENEMY_CLAIM);
            assertTrue(enemyMsg.contains("enemy"));

            String pvpMsg = protectionChecker.getDenialMessage(PvPResult.DENIED_SAME_FACTION);
            assertTrue(pvpMsg.contains("faction"));
        }
    }
}
