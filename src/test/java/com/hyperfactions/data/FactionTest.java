package com.hyperfactions.data;

import com.hyperfactions.testutil.TestFactionFactory;
import com.hyperfactions.testutil.TestPlayerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Faction record.
 */
@DisplayName("Faction")
class FactionTest {

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("creates faction with leader as member")
        void create_hasLeader() {
            UUID leaderUuid = UUID.randomUUID();
            Faction faction = Faction.create("TestFaction", leaderUuid, "TestLeader");

            assertNotNull(faction.id());
            assertEquals("TestFaction", faction.name());
            assertEquals(1, faction.getMemberCount());
            assertTrue(faction.hasMember(leaderUuid));
        }

        @Test
        @DisplayName("leader has LEADER role")
        void create_leaderHasLeaderRole() {
            UUID leaderUuid = UUID.randomUUID();
            Faction faction = Faction.create("TestFaction", leaderUuid, "TestLeader");

            FactionMember leader = faction.getMember(leaderUuid);
            assertNotNull(leader);
            assertTrue(leader.isLeader());
        }
    }

    @Nested
    @DisplayName("getLeader()")
    class GetLeaderTests {

        @Test
        @DisplayName("returns the leader member")
        void getLeader_returnsLeader() {
            UUID leaderUuid = UUID.randomUUID();
            Faction faction = Faction.create("TestFaction", leaderUuid, "TestLeader");

            FactionMember leader = faction.getLeader();
            assertNotNull(leader);
            assertEquals(leaderUuid, leader.uuid());
            assertEquals("TestLeader", leader.username());
        }

        @Test
        @DisplayName("returns null when no leader exists")
        void getLeader_nullWhenNoLeader() {
            // Create faction with only a regular member (edge case)
            UUID memberUuid = UUID.randomUUID();
            Faction faction = TestFactionFactory.builder()
                    .addRegularMember(memberUuid, "Member")
                    .build();

            assertNull(faction.getLeader());
        }
    }

    @Nested
    @DisplayName("getLeaderId()")
    class GetLeaderIdTests {

        @Test
        @DisplayName("returns leader UUID")
        void getLeaderId_returnsUuid() {
            UUID leaderUuid = UUID.randomUUID();
            Faction faction = Faction.create("TestFaction", leaderUuid, "TestLeader");

            assertEquals(leaderUuid, faction.getLeaderId());
        }
    }

    @Nested
    @DisplayName("findSuccessor()")
    class FindSuccessorTests {

        @Test
        @DisplayName("prioritizes officers over members")
        void findSuccessor_prioritizesRole() {
            UUID leaderUuid = UUID.randomUUID();
            UUID officerUuid = UUID.randomUUID();
            UUID memberUuid = UUID.randomUUID();

            Faction faction = TestFactionFactory.builder()
                    .addLeader(leaderUuid, "Leader")
                    .addOfficer(officerUuid, "Officer")
                    .addRegularMember(memberUuid, "Member")
                    .build();

            FactionMember successor = faction.findSuccessor();
            assertNotNull(successor);
            assertEquals(officerUuid, successor.uuid());
        }

        @Test
        @DisplayName("selects oldest member when same role")
        void findSuccessor_thenByTenure() {
            UUID leaderUuid = UUID.randomUUID();
            UUID member1Uuid = UUID.randomUUID();
            UUID member2Uuid = UUID.randomUUID();

            // member1 joined earlier
            long now = System.currentTimeMillis();
            FactionMember member1 = TestPlayerFactory.createMember(member1Uuid, "Member1", FactionRole.MEMBER, now - 10000);
            FactionMember member2 = TestPlayerFactory.createMember(member2Uuid, "Member2", FactionRole.MEMBER, now);

            Faction faction = TestFactionFactory.builder()
                    .addLeader(leaderUuid, "Leader")
                    .addMember(member1)
                    .addMember(member2)
                    .build();

            FactionMember successor = faction.findSuccessor();
            assertNotNull(successor);
            assertEquals(member1Uuid, successor.uuid());
        }

        @Test
        @DisplayName("returns null when only leader exists")
        void findSuccessor_nullWhenOnlyLeader() {
            UUID leaderUuid = UUID.randomUUID();
            Faction faction = Faction.create("TestFaction", leaderUuid, "Leader");

            assertNull(faction.findSuccessor());
        }
    }

    @Nested
    @DisplayName("withMember() / withoutMember()")
    class MemberOperationsTests {

        @Test
        @DisplayName("withMember adds new member")
        void withMember_addsNewMember() {
            UUID leaderUuid = UUID.randomUUID();
            UUID newMemberUuid = UUID.randomUUID();

            Faction faction = Faction.create("TestFaction", leaderUuid, "Leader");
            FactionMember newMember = FactionMember.create(newMemberUuid, "NewMember");

            Faction updated = faction.withMember(newMember);

            assertEquals(1, faction.getMemberCount()); // Original unchanged
            assertEquals(2, updated.getMemberCount());
            assertTrue(updated.hasMember(newMemberUuid));
        }

        @Test
        @DisplayName("withoutMember removes member")
        void withoutMember_removesMember() {
            UUID leaderUuid = UUID.randomUUID();
            UUID memberUuid = UUID.randomUUID();

            Faction faction = TestFactionFactory.builder()
                    .addLeader(leaderUuid, "Leader")
                    .addRegularMember(memberUuid, "Member")
                    .build();

            Faction updated = faction.withoutMember(memberUuid);

            assertEquals(2, faction.getMemberCount()); // Original unchanged
            assertEquals(1, updated.getMemberCount());
            assertFalse(updated.hasMember(memberUuid));
        }
    }

    @Nested
    @DisplayName("Claim Operations")
    class ClaimOperationsTests {

        @Test
        @DisplayName("withClaim adds claim")
        void withClaim_addsClaim() {
            UUID leaderUuid = UUID.randomUUID();
            Faction faction = Faction.create("TestFaction", leaderUuid, "Leader");

            FactionClaim claim = FactionClaim.create("world", 5, 10, leaderUuid);
            Faction updated = faction.withClaim(claim);

            assertEquals(0, faction.getClaimCount()); // Original unchanged
            assertEquals(1, updated.getClaimCount());
            assertTrue(updated.hasClaimAt("world", 5, 10));
        }

        @Test
        @DisplayName("withoutClaimAt removes claim")
        void withoutClaimAt_removesClaim() {
            UUID leaderUuid = UUID.randomUUID();
            Faction faction = TestFactionFactory.builder()
                    .addLeader(leaderUuid, "Leader")
                    .addClaim("world", 5, 10, leaderUuid)
                    .addClaim("world", 6, 10, leaderUuid)
                    .build();

            Faction updated = faction.withoutClaimAt("world", 5, 10);

            assertEquals(2, faction.getClaimCount()); // Original unchanged
            assertEquals(1, updated.getClaimCount());
            assertFalse(updated.hasClaimAt("world", 5, 10));
            assertTrue(updated.hasClaimAt("world", 6, 10));
        }

        @Test
        @DisplayName("hasClaimAt returns false for unclaimed chunk")
        void hasClaimAt_falseForUnclaimed() {
            UUID leaderUuid = UUID.randomUUID();
            Faction faction = TestFactionFactory.builder()
                    .addLeader(leaderUuid, "Leader")
                    .addClaim("world", 5, 10, leaderUuid)
                    .build();

            assertFalse(faction.hasClaimAt("world", 6, 10));
            assertFalse(faction.hasClaimAt("nether", 5, 10));
        }
    }

    @Nested
    @DisplayName("Relation Operations")
    class RelationOperationsTests {

        @Test
        @DisplayName("getRelationType defaults to NEUTRAL")
        void getRelationType_defaultsNeutral() {
            UUID leaderUuid = UUID.randomUUID();
            UUID otherFactionId = UUID.randomUUID();

            Faction faction = Faction.create("TestFaction", leaderUuid, "Leader");

            assertEquals(RelationType.NEUTRAL, faction.getRelationType(otherFactionId));
        }

        @Test
        @DisplayName("isAlly returns true for ally relation")
        void isAlly_trueForAlly() {
            UUID otherFactionId = UUID.randomUUID();
            Faction faction = TestFactionFactory.builder()
                    .addLeader(UUID.randomUUID(), "Leader")
                    .addAlly(otherFactionId)
                    .build();

            assertTrue(faction.isAlly(otherFactionId));
            assertFalse(faction.isEnemy(otherFactionId));
        }

        @Test
        @DisplayName("isEnemy returns true for enemy relation")
        void isEnemy_trueForEnemy() {
            UUID otherFactionId = UUID.randomUUID();
            Faction faction = TestFactionFactory.builder()
                    .addLeader(UUID.randomUUID(), "Leader")
                    .addEnemy(otherFactionId)
                    .build();

            assertTrue(faction.isEnemy(otherFactionId));
            assertFalse(faction.isAlly(otherFactionId));
        }

        @Test
        @DisplayName("withRelation removes neutral relations")
        void withRelation_removesNeutral() {
            UUID otherFactionId = UUID.randomUUID();
            Faction faction = TestFactionFactory.builder()
                    .addLeader(UUID.randomUUID(), "Leader")
                    .addAlly(otherFactionId)
                    .build();

            FactionRelation neutralRelation = FactionRelation.create(otherFactionId, RelationType.NEUTRAL);
            Faction updated = faction.withRelation(neutralRelation);

            // Setting to neutral should remove the relation from the map
            assertNull(updated.getRelation(otherFactionId));
            assertEquals(RelationType.NEUTRAL, updated.getRelationType(otherFactionId));
        }
    }

    @Nested
    @DisplayName("Log Operations")
    class LogOperationsTests {

        @Test
        @DisplayName("withLog adds log at beginning")
        void withLog_addsAtBeginning() {
            UUID leaderUuid = UUID.randomUUID();
            Faction faction = Faction.create("TestFaction", leaderUuid, "Leader");

            FactionLog log1 = FactionLog.create(FactionLog.LogType.CLAIM, "First log", leaderUuid);
            FactionLog log2 = FactionLog.create(FactionLog.LogType.CLAIM, "Second log", leaderUuid);

            Faction updated1 = faction.withLog(log1);
            Faction updated2 = updated1.withLog(log2);

            // Creation log from create() + log1 + log2
            assertTrue(updated2.logs().size() >= 2);
            // Most recent log should be first
            assertEquals("Second log", updated2.logs().get(0).message());
        }

        @Test
        @DisplayName("withLog trims to MAX_LOGS")
        void withLog_trimsToMax() {
            UUID leaderUuid = UUID.randomUUID();
            Faction faction = Faction.create("TestFaction", leaderUuid, "Leader");

            // Add more than MAX_LOGS entries
            for (int i = 0; i < Faction.MAX_LOGS + 10; i++) {
                FactionLog log = FactionLog.create(FactionLog.LogType.CLAIM, "Log " + i, leaderUuid);
                faction = faction.withLog(log);
            }

            assertEquals(Faction.MAX_LOGS, faction.logs().size());
        }
    }

    @Nested
    @DisplayName("Property Updates")
    class PropertyUpdateTests {

        @Test
        @DisplayName("withName updates name")
        void withName_updatesName() {
            Faction faction = Faction.create("OldName", UUID.randomUUID(), "Leader");
            Faction updated = faction.withName("NewName");

            assertEquals("OldName", faction.name());
            assertEquals("NewName", updated.name());
        }

        @Test
        @DisplayName("withColor updates color")
        void withColor_updatesColor() {
            Faction faction = Faction.create("Test", UUID.randomUUID(), "Leader");
            Faction updated = faction.withColor("a");

            assertEquals("#55FF55", updated.color());
        }

        @Test
        @DisplayName("getColoredName returns faction name")
        void getColoredName_returnsName() {
            Faction faction = TestFactionFactory.builder()
                    .name("TestFaction")
                    .color("#55FF55")
                    .addLeader(UUID.randomUUID(), "Leader")
                    .build();

            assertEquals("TestFaction", faction.getColoredName());
        }

        @Test
        @DisplayName("withOpen updates open status")
        void withOpen_updatesStatus() {
            Faction faction = Faction.create("Test", UUID.randomUUID(), "Leader");

            assertFalse(faction.open());

            Faction opened = faction.withOpen(true);
            assertTrue(opened.open());
        }
    }

    @Nested
    @DisplayName("Compact Constructor")
    class CompactConstructorTests {

        @Test
        @DisplayName("makes collections immutable")
        void compactConstructor_immutableCollections() {
            Faction faction = Faction.create("Test", UUID.randomUUID(), "Leader");

            assertThrows(UnsupportedOperationException.class, () ->
                    faction.members().put(UUID.randomUUID(), null));
            assertThrows(UnsupportedOperationException.class, () ->
                    faction.claims().add(null));
            assertThrows(UnsupportedOperationException.class, () ->
                    faction.relations().put(UUID.randomUUID(), null));
        }

        @Test
        @DisplayName("defaults null color to white")
        void compactConstructor_defaultsColor() {
            // Direct construction with null color
            Faction faction = new Faction(
                    UUID.randomUUID(), "Test", null, null, null,
                    System.currentTimeMillis(), null,
                    java.util.Map.of(), java.util.Set.of(), java.util.Map.of(),
                    java.util.List.of(), false, null, null
            );

            assertEquals("#FFFFFF", faction.color());
        }
    }
}
