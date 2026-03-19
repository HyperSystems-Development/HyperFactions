package com.hyperfactions.manager;

import com.hyperfactions.data.CombatTag;
import com.hyperfactions.protection.SpawnProtection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CombatTagManager.
 */
@DisplayName("CombatTagManager")
class CombatTagManagerTest {

    private CombatTagManager manager;

    @BeforeEach
    void setUp() {
        com.hyperfactions.config.ConfigManager.initTestDefaults();
        manager = new CombatTagManager();
    }

    @Nested
    @DisplayName("Combat Tag Queries")
    class CombatTagQueryTests {

        @Test
        @DisplayName("isTagged returns false when not tagged")
        void isTagged_falseWhenNotTagged() {
            UUID uuid = UUID.randomUUID();
            assertFalse(manager.isTagged(uuid));
        }

        @Test
        @DisplayName("isTagged returns true when actively tagged")
        void isTagged_trueWhenActive() {
            UUID uuid = UUID.randomUUID();
            manager.tagPlayer(uuid, 60);

            assertTrue(manager.isTagged(uuid));
        }

        @Test
        @DisplayName("isTagged returns false when tag expired")
        void isTagged_falseWhenExpired() {
            UUID uuid = UUID.randomUUID();
            manager.tagPlayer(uuid, 0); // Zero duration = immediately expired

            assertFalse(manager.isTagged(uuid));
        }

        @Test
        @DisplayName("getTag returns null when not tagged")
        void getTag_nullWhenNotTagged() {
            UUID uuid = UUID.randomUUID();
            assertNull(manager.getTag(uuid));
        }

        @Test
        @DisplayName("getTag returns tag when tagged")
        void getTag_returnsTag() {
            UUID uuid = UUID.randomUUID();
            manager.tagPlayer(uuid, 30);

            CombatTag tag = manager.getTag(uuid);
            assertNotNull(tag);
            assertEquals(uuid, tag.playerUuid());
        }

        @Test
        @DisplayName("getRemainingSeconds returns correct value")
        void getRemainingSeconds_calculatesCorrectly() {
            UUID uuid = UUID.randomUUID();
            manager.tagPlayer(uuid, 60);

            int remaining = manager.getRemainingSeconds(uuid);
            assertTrue(remaining >= 59 && remaining <= 60);
        }

        @Test
        @DisplayName("getRemainingSeconds returns zero when not tagged")
        void getRemainingSeconds_zeroWhenNotTagged() {
            UUID uuid = UUID.randomUUID();
            assertEquals(0, manager.getRemainingSeconds(uuid));
        }
    }

    @Nested
    @DisplayName("Tag Operations")
    class TagOperationTests {

        @Test
        @DisplayName("tagPlayer creates new tag")
        void tagPlayer_createsNewTag() {
            UUID uuid = UUID.randomUUID();

            CombatTag tag = manager.tagPlayer(uuid, 30);

            assertNotNull(tag);
            assertEquals(uuid, tag.playerUuid());
            assertEquals(30, tag.durationSeconds());
            assertTrue(manager.isTagged(uuid));
        }

        @Test
        @DisplayName("tagPlayer refreshes existing tag")
        void tagPlayer_refreshesExistingTag() throws InterruptedException {
            UUID uuid = UUID.randomUUID();
            manager.tagPlayer(uuid, 30);
            long originalTaggedAt = manager.getTag(uuid).taggedAt();

            Thread.sleep(10);
            CombatTag refreshed = manager.tagPlayer(uuid, 45);

            assertTrue(refreshed.taggedAt() > originalTaggedAt);
            assertEquals(45, refreshed.durationSeconds());
        }

        @Test
        @DisplayName("tagCombat tags both players")
        void tagCombat_tagsBothPlayers() {
            UUID attacker = UUID.randomUUID();
            UUID defender = UUID.randomUUID();

            manager.tagCombat(attacker, defender);

            assertTrue(manager.isTagged(attacker));
            assertTrue(manager.isTagged(defender));
        }

        @Test
        @DisplayName("clearTag removes tag")
        void clearTag_removesTag() {
            UUID uuid = UUID.randomUUID();
            manager.tagPlayer(uuid, 60);

            manager.clearTag(uuid);

            assertFalse(manager.isTagged(uuid));
        }

        @Test
        @DisplayName("getTaggedPlayers returns active tags only")
        void getTaggedPlayers_returnsActiveOnly() {
            UUID active1 = UUID.randomUUID();
            UUID active2 = UUID.randomUUID();
            UUID expired = UUID.randomUUID();

            manager.tagPlayer(active1, 60);
            manager.tagPlayer(active2, 60);
            manager.tagPlayer(expired, 0); // Immediately expired

            Set<UUID> tagged = manager.getTaggedPlayers();

            assertTrue(tagged.contains(active1));
            assertTrue(tagged.contains(active2));
            assertFalse(tagged.contains(expired));
        }
    }

    @Nested
    @DisplayName("Disconnect Handling")
    class DisconnectHandlingTests {

        @Test
        @DisplayName("handleDisconnect returns true if tagged")
        void handleDisconnect_returnsTrueIfTagged() {
            UUID uuid = UUID.randomUUID();
            manager.tagPlayer(uuid, 60);

            boolean wasTagged = manager.handleDisconnect(uuid);

            assertTrue(wasTagged);
        }

        @Test
        @DisplayName("handleDisconnect returns false if not tagged")
        void handleDisconnect_returnsFalseIfNotTagged() {
            UUID uuid = UUID.randomUUID();

            boolean wasTagged = manager.handleDisconnect(uuid);

            assertFalse(wasTagged);
        }

        @Test
        @DisplayName("handleDisconnect triggers callback")
        void handleDisconnect_triggersCallback() {
            AtomicReference<UUID> callbackUuid = new AtomicReference<>();
            manager.setOnCombatLogout(callbackUuid::set);

            UUID uuid = UUID.randomUUID();
            manager.tagPlayer(uuid, 60);
            manager.handleDisconnect(uuid);

            assertEquals(uuid, callbackUuid.get());
        }

        @Test
        @DisplayName("handleDisconnect removes tag")
        void handleDisconnect_removesTag() {
            UUID uuid = UUID.randomUUID();
            manager.tagPlayer(uuid, 60);

            manager.handleDisconnect(uuid);

            assertFalse(manager.isTagged(uuid));
        }
    }

    @Nested
    @DisplayName("Tick Decay")
    class TickDecayTests {

        @Test
        @DisplayName("tickDecay cleans expired tags")
        void tickDecay_cleansExpired() {
            UUID expired = UUID.randomUUID();
            manager.tagPlayer(expired, 0); // Immediately expired

            manager.tickDecay();

            assertEquals(0, manager.getTagCount());
        }

        @Test
        @DisplayName("tickDecay triggers expiry callback")
        void tickDecay_triggersExpiryCallback() {
            AtomicBoolean callbackTriggered = new AtomicBoolean(false);
            manager.setOnTagExpired(uuid -> callbackTriggered.set(true));

            UUID expired = UUID.randomUUID();
            manager.tagPlayer(expired, 0);

            manager.tickDecay();

            assertTrue(callbackTriggered.get());
        }

        @Test
        @DisplayName("tickDecay preserves active tags")
        void tickDecay_preservesActive() {
            UUID active = UUID.randomUUID();
            manager.tagPlayer(active, 60);

            manager.tickDecay();

            assertTrue(manager.isTagged(active));
        }
    }

    @Nested
    @DisplayName("Spawn Protection")
    class SpawnProtectionTests {

        @Test
        @DisplayName("hasSpawnProtection returns false when not protected")
        void hasSpawnProtection_falseWhenNotProtected() {
            UUID uuid = UUID.randomUUID();
            assertFalse(manager.hasSpawnProtection(uuid));
        }

        @Test
        @DisplayName("hasSpawnProtection returns true when protected")
        void hasSpawnProtection_trueWhenProtected() {
            UUID uuid = UUID.randomUUID();
            manager.applySpawnProtection(uuid, 60, "world", 0, 0);

            assertTrue(manager.hasSpawnProtection(uuid));
        }

        @Test
        @DisplayName("applySpawnProtection creates protection")
        void applySpawnProtection_createsProtection() {
            UUID uuid = UUID.randomUUID();

            SpawnProtection protection = manager.applySpawnProtection(uuid, 30, "world", 5, 10);

            assertNotNull(protection);
            assertEquals(uuid, protection.playerUuid());
            assertEquals("world", protection.world());
            assertEquals(5, protection.chunkX());
            assertEquals(10, protection.chunkZ());
        }

        @Test
        @DisplayName("clearSpawnProtection removes protection")
        void clearSpawnProtection_removesProtection() {
            UUID uuid = UUID.randomUUID();
            manager.applySpawnProtection(uuid, 60, "world", 0, 0);

            manager.clearSpawnProtection(uuid);

            assertFalse(manager.hasSpawnProtection(uuid));
        }

        @Test
        @DisplayName("checkSpawnProtectionMove breaks on move")
        void checkSpawnProtectionMove_breaksOnMove() {
            UUID uuid = UUID.randomUUID();
            manager.applySpawnProtection(uuid, 60, "world", 5, 5);

            // Move to different chunk
            boolean broken = manager.checkSpawnProtectionMove(uuid, "world", 6, 5);

            assertTrue(broken);
            assertFalse(manager.hasSpawnProtection(uuid));
        }

        @Test
        @DisplayName("checkSpawnProtectionMove keeps protection in same chunk")
        void checkSpawnProtectionMove_keepsInSameChunk() {
            UUID uuid = UUID.randomUUID();
            manager.applySpawnProtection(uuid, 60, "world", 5, 5);

            boolean broken = manager.checkSpawnProtectionMove(uuid, "world", 5, 5);

            assertFalse(broken);
            assertTrue(manager.hasSpawnProtection(uuid));
        }

        @Test
        @DisplayName("getSpawnProtectionRemainingSeconds returns correct value")
        void getSpawnProtectionRemainingSeconds_calculatesCorrectly() {
            UUID uuid = UUID.randomUUID();
            manager.applySpawnProtection(uuid, 30, "world", 0, 0);

            int remaining = manager.getSpawnProtectionRemainingSeconds(uuid);

            assertTrue(remaining >= 29 && remaining <= 30);
        }
    }
}
