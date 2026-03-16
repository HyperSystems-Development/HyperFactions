package com.hyperfactions.manager;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.RelationType;
import com.hyperfactions.manager.RelationManager.RelationResult;
import com.hyperfactions.testutil.MockStorage;
import com.hyperfactions.testutil.TestFactionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RelationManager.
 */
@DisplayName("RelationManager")
class RelationManagerTest {

    private MockStorage.MockFactionStorage factionStorage;
    private FactionManager factionManager;
    private RelationManager relationManager;

    @BeforeEach
    void setUp() {
        com.hyperfactions.config.ConfigManager.initTestDefaults();
        factionStorage = MockStorage.factionStorage();
        factionManager = new FactionManager(factionStorage);
        relationManager = new RelationManager(factionManager);
    }

    private void setupFactions(Faction... factions) {
        for (Faction faction : factions) {
            factionStorage.addFaction(faction);
        }
        factionManager.loadAll().join();
    }

    @Nested
    @DisplayName("getRelation()")
    class GetRelationTests {

        @Test
        @DisplayName("same faction is always ALLY")
        void getRelation_sameFactionIsAlly() {
            UUID leader = UUID.randomUUID();
            Faction faction = TestFactionFactory.createSimple("Test", leader, "Leader");
            setupFactions(faction);

            RelationType relation = relationManager.getRelation(faction.id(), faction.id());

            assertEquals(RelationType.ALLY, relation);
        }

        @Test
        @DisplayName("returns stored relation")
        void getRelation_returnsStoredRelation() {
            UUID leader1 = UUID.randomUUID();
            UUID leader2 = UUID.randomUUID();

            Faction[] factions = TestFactionFactory.createEnemyPair();
            setupFactions(factions);

            RelationType relation = relationManager.getRelation(factions[0].id(), factions[1].id());

            assertEquals(RelationType.ENEMY, relation);
        }

        @Test
        @DisplayName("defaults to NEUTRAL")
        void getRelation_defaultsNeutral() {
            UUID leader1 = UUID.randomUUID();
            UUID leader2 = UUID.randomUUID();

            Faction faction1 = TestFactionFactory.createSimple("Faction1", leader1, "Leader1");
            Faction faction2 = TestFactionFactory.createSimple("Faction2", leader2, "Leader2");
            setupFactions(faction1, faction2);

            RelationType relation = relationManager.getRelation(faction1.id(), faction2.id());

            assertEquals(RelationType.NEUTRAL, relation);
        }
    }

    @Nested
    @DisplayName("getPlayerRelation()")
    class GetPlayerRelationTests {

        @Test
        @DisplayName("returns relation based on factions")
        void getPlayerRelation_basedOnFactions() {
            Faction[] factions = TestFactionFactory.createAlliedPair();
            setupFactions(factions);

            UUID player1 = factions[0].getLeaderId();
            UUID player2 = factions[1].getLeaderId();

            RelationType relation = relationManager.getPlayerRelation(player1, player2);

            assertEquals(RelationType.ALLY, relation);
        }

        @Test
        @DisplayName("returns null if player has no faction")
        void getPlayerRelation_nullIfNoFaction() {
            UUID player1 = UUID.randomUUID();
            UUID player2 = UUID.randomUUID();

            RelationType relation = relationManager.getPlayerRelation(player1, player2);

            assertNull(relation);
        }

        @Test
        @DisplayName("same faction players are allies")
        void getPlayerRelation_sameFactionIsAlly() {
            UUID leader = UUID.randomUUID();
            UUID member = UUID.randomUUID();

            Faction faction = TestFactionFactory.builder()
                    .addLeader(leader, "Leader")
                    .addRegularMember(member, "Member")
                    .build();
            setupFactions(faction);

            RelationType relation = relationManager.getPlayerRelation(leader, member);

            assertEquals(RelationType.ALLY, relation);
        }
    }

    @Nested
    @DisplayName("areAllies() / areEnemies()")
    class RelationCheckTests {

        @Test
        @DisplayName("areAllies returns true for allies")
        void areAllies_trueForAllies() {
            Faction[] factions = TestFactionFactory.createAlliedPair();
            setupFactions(factions);

            assertTrue(relationManager.areAllies(factions[0].id(), factions[1].id()));
        }

        @Test
        @DisplayName("areAllies returns false for enemies")
        void areAllies_falseForEnemies() {
            Faction[] factions = TestFactionFactory.createEnemyPair();
            setupFactions(factions);

            assertFalse(relationManager.areAllies(factions[0].id(), factions[1].id()));
        }

        @Test
        @DisplayName("areEnemies returns true for enemies")
        void areEnemies_trueForEnemies() {
            Faction[] factions = TestFactionFactory.createEnemyPair();
            setupFactions(factions);

            assertTrue(relationManager.areEnemies(factions[0].id(), factions[1].id()));
        }
    }

    @Nested
    @DisplayName("requestAlly()")
    class RequestAllyTests {

        @Test
        @DisplayName("sends request when no pending")
        void requestAlly_sendsRequest() {
            UUID leader1 = UUID.randomUUID();
            UUID leader2 = UUID.randomUUID();

            Faction faction1 = TestFactionFactory.createSimple("Faction1", leader1, "Leader1");
            Faction faction2 = TestFactionFactory.createSimple("Faction2", leader2, "Leader2");
            setupFactions(faction1, faction2);

            RelationResult result = relationManager.requestAlly(leader1, faction2.id());

            assertEquals(RelationResult.REQUEST_SENT, result);
            assertTrue(relationManager.hasPendingRequest(faction1.id(), faction2.id()));
        }

        @Test
        @DisplayName("accepts if pending request exists")
        void requestAlly_acceptsIfPending() {
            UUID leader1 = UUID.randomUUID();
            UUID leader2 = UUID.randomUUID();

            Faction faction1 = TestFactionFactory.createSimple("Faction1", leader1, "Leader1");
            Faction faction2 = TestFactionFactory.createSimple("Faction2", leader2, "Leader2");
            setupFactions(faction1, faction2);

            // Faction2 sends request to Faction1
            relationManager.requestAlly(leader2, faction1.id());

            // Faction1 responds - should accept
            RelationResult result = relationManager.requestAlly(leader1, faction2.id());

            assertEquals(RelationResult.REQUEST_ACCEPTED, result);
            assertTrue(relationManager.areAllies(faction1.id(), faction2.id()));
        }

        @Test
        @DisplayName("fails when not in faction")
        void requestAlly_failsNotInFaction() {
            UUID random = UUID.randomUUID();
            UUID target = UUID.randomUUID();

            RelationResult result = relationManager.requestAlly(random, target);

            assertEquals(RelationResult.NOT_IN_FACTION, result);
        }

        @Test
        @DisplayName("fails when already ally")
        void requestAlly_failsAlreadyAlly() {
            Faction[] factions = TestFactionFactory.createAlliedPair();
            setupFactions(factions);

            UUID leader1 = factions[0].getLeaderId();

            RelationResult result = relationManager.requestAlly(leader1, factions[1].id());

            assertEquals(RelationResult.ALREADY_ALLY, result);
        }

        @Test
        @DisplayName("fails when trying to ally self")
        void requestAlly_failsSelf() {
            UUID leader = UUID.randomUUID();
            Faction faction = TestFactionFactory.createSimple("Test", leader, "Leader");
            setupFactions(faction);

            RelationResult result = relationManager.requestAlly(leader, faction.id());

            assertEquals(RelationResult.CANNOT_RELATE_SELF, result);
        }
    }

    @Nested
    @DisplayName("acceptAlly()")
    class AcceptAllyTests {

        @Test
        @DisplayName("sets mutual ally relation")
        void acceptAlly_setsMutualRelation() {
            UUID leader1 = UUID.randomUUID();
            UUID leader2 = UUID.randomUUID();

            Faction faction1 = TestFactionFactory.createSimple("Faction1", leader1, "Leader1");
            Faction faction2 = TestFactionFactory.createSimple("Faction2", leader2, "Leader2");
            setupFactions(faction1, faction2);

            // Send request
            relationManager.requestAlly(leader1, faction2.id());

            // Accept
            RelationResult result = relationManager.acceptAlly(leader2, faction1.id());

            assertEquals(RelationResult.REQUEST_ACCEPTED, result);

            // Check both sides
            assertTrue(relationManager.areAllies(faction1.id(), faction2.id()));
            assertTrue(relationManager.areAllies(faction2.id(), faction1.id()));
        }

        @Test
        @DisplayName("fails when no pending request")
        void acceptAlly_failsNoPending() {
            UUID leader1 = UUID.randomUUID();
            UUID leader2 = UUID.randomUUID();

            Faction faction1 = TestFactionFactory.createSimple("Faction1", leader1, "Leader1");
            Faction faction2 = TestFactionFactory.createSimple("Faction2", leader2, "Leader2");
            setupFactions(faction1, faction2);

            RelationResult result = relationManager.acceptAlly(leader2, faction1.id());

            assertEquals(RelationResult.NO_PENDING_REQUEST, result);
        }
    }

    @Nested
    @DisplayName("setEnemy()")
    class SetEnemyTests {

        @Test
        @DisplayName("sets enemy relation")
        void setEnemy_setsRelation() {
            UUID leader1 = UUID.randomUUID();
            UUID leader2 = UUID.randomUUID();

            Faction faction1 = TestFactionFactory.createSimple("Faction1", leader1, "Leader1");
            Faction faction2 = TestFactionFactory.createSimple("Faction2", leader2, "Leader2");
            setupFactions(faction1, faction2);

            RelationResult result = relationManager.setEnemy(leader1, faction2.id());

            assertEquals(RelationResult.SUCCESS, result);
            assertTrue(relationManager.areEnemies(faction1.id(), faction2.id()));
        }

        @Test
        @DisplayName("removes ally request when setting enemy")
        void setEnemy_removesAllyRequest() {
            UUID leader1 = UUID.randomUUID();
            UUID leader2 = UUID.randomUUID();

            Faction faction1 = TestFactionFactory.createSimple("Faction1", leader1, "Leader1");
            Faction faction2 = TestFactionFactory.createSimple("Faction2", leader2, "Leader2");
            setupFactions(faction1, faction2);

            // Send ally request
            relationManager.requestAlly(leader1, faction2.id());
            assertTrue(relationManager.hasPendingRequest(faction1.id(), faction2.id()));

            // Set enemy
            relationManager.setEnemy(leader2, faction1.id());

            // Request should be removed
            assertFalse(relationManager.hasPendingRequest(faction1.id(), faction2.id()));
        }

        @Test
        @DisplayName("fails when already enemy")
        void setEnemy_failsAlreadyEnemy() {
            Faction[] factions = TestFactionFactory.createEnemyPair();
            setupFactions(factions);

            RelationResult result = relationManager.setEnemy(factions[0].getLeaderId(), factions[1].id());

            assertEquals(RelationResult.ALREADY_ENEMY, result);
        }
    }

    @Nested
    @DisplayName("setNeutral()")
    class SetNeutralTests {

        @Test
        @DisplayName("breaks alliance on both sides")
        void setNeutral_breaksAlliance() {
            Faction[] factions = TestFactionFactory.createAlliedPair();
            setupFactions(factions);

            UUID leader1 = factions[0].getLeaderId();

            RelationResult result = relationManager.setNeutral(leader1, factions[1].id());

            assertEquals(RelationResult.SUCCESS, result);

            // Both sides should be neutral now
            assertEquals(RelationType.NEUTRAL, relationManager.getRelation(factions[0].id(), factions[1].id()));
            assertEquals(RelationType.NEUTRAL, relationManager.getRelation(factions[1].id(), factions[0].id()));
        }

        @Test
        @DisplayName("fails when already neutral")
        void setNeutral_failsAlreadyNeutral() {
            UUID leader1 = UUID.randomUUID();
            UUID leader2 = UUID.randomUUID();

            Faction faction1 = TestFactionFactory.createSimple("Faction1", leader1, "Leader1");
            Faction faction2 = TestFactionFactory.createSimple("Faction2", leader2, "Leader2");
            setupFactions(faction1, faction2);

            RelationResult result = relationManager.setNeutral(leader1, faction2.id());

            assertEquals(RelationResult.ALREADY_NEUTRAL, result);
        }
    }

    @Nested
    @DisplayName("Ally/Enemy Lists")
    class ListTests {

        @Test
        @DisplayName("getAllies returns ally list")
        void getAllies_returnsList() {
            UUID leader1 = UUID.randomUUID();
            UUID leader2 = UUID.randomUUID();
            UUID leader3 = UUID.randomUUID();

            Faction faction1 = TestFactionFactory.builder()
                    .addLeader(leader1, "Leader1")
                    .build();
            Faction faction2 = TestFactionFactory.builder()
                    .addLeader(leader2, "Leader2")
                    .addAlly(faction1.id())
                    .build();
            Faction faction3 = TestFactionFactory.builder()
                    .addLeader(leader3, "Leader3")
                    .addAlly(faction1.id())
                    .build();

            // Update faction1 to have allies
            faction1 = TestFactionFactory.builder()
                    .id(faction1.id())
                    .addLeader(leader1, "Leader1")
                    .addAlly(faction2.id())
                    .addAlly(faction3.id())
                    .build();

            setupFactions(faction1, faction2, faction3);

            List<UUID> allies = relationManager.getAllies(faction1.id());

            assertEquals(2, allies.size());
            assertTrue(allies.contains(faction2.id()));
            assertTrue(allies.contains(faction3.id()));
        }

        @Test
        @DisplayName("getEnemies returns enemy list")
        void getEnemies_returnsList() {
            UUID leader1 = UUID.randomUUID();
            UUID leader2 = UUID.randomUUID();

            Faction faction1 = TestFactionFactory.builder()
                    .addLeader(leader1, "Leader1")
                    .build();
            Faction faction2 = TestFactionFactory.createSimple("Faction2", leader2, "Leader2");

            // Update faction1 to have enemy
            faction1 = TestFactionFactory.builder()
                    .id(faction1.id())
                    .addLeader(leader1, "Leader1")
                    .addEnemy(faction2.id())
                    .build();

            setupFactions(faction1, faction2);

            List<UUID> enemies = relationManager.getEnemies(faction1.id());

            assertEquals(1, enemies.size());
            assertTrue(enemies.contains(faction2.id()));
        }
    }
}
