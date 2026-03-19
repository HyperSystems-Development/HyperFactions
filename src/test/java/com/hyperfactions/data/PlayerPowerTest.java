package com.hyperfactions.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PlayerPower record.
 */
@DisplayName("PlayerPower")
class PlayerPowerTest {

    private static final UUID TEST_UUID = UUID.randomUUID();

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("creates with specified starting power and max power")
        void create_setsInitialValues() {
            PlayerPower power = PlayerPower.create(TEST_UUID, 10.0, 20.0);

            assertEquals(TEST_UUID, power.uuid());
            assertEquals(10.0, power.power());
            assertEquals(20.0, power.maxPower());
            assertEquals(0, power.lastDeath());
            assertTrue(power.lastRegen() > 0);
        }
    }

    @Nested
    @DisplayName("withPower()")
    class WithPowerTests {

        @Test
        @DisplayName("clamps power to zero when negative")
        void withPower_clampsToZero() {
            PlayerPower power = PlayerPower.create(TEST_UUID, 10.0, 20.0);
            PlayerPower updated = power.withPower(-5.0);

            assertEquals(0.0, updated.power());
        }

        @Test
        @DisplayName("clamps power to max when exceeds maximum")
        void withPower_clampsToMax() {
            PlayerPower power = PlayerPower.create(TEST_UUID, 10.0, 20.0);
            PlayerPower updated = power.withPower(30.0);

            assertEquals(20.0, updated.power());
        }

        @Test
        @DisplayName("sets exact value when within range")
        void withPower_setsExactValue() {
            PlayerPower power = PlayerPower.create(TEST_UUID, 10.0, 20.0);
            PlayerPower updated = power.withPower(15.5);

            assertEquals(15.5, updated.power());
        }

        @Test
        @DisplayName("preserves other fields when updating power")
        void withPower_preservesOtherFields() {
            PlayerPower power = new PlayerPower(TEST_UUID, 10.0, 20.0, 12345L, 67890L, null, false, false);
            PlayerPower updated = power.withPower(15.0);

            assertEquals(TEST_UUID, updated.uuid());
            assertEquals(20.0, updated.maxPower());
            assertEquals(12345L, updated.lastDeath());
            assertEquals(67890L, updated.lastRegen());
        }
    }

    @Nested
    @DisplayName("withDeathPenalty()")
    class WithDeathPenaltyTests {

        @Test
        @DisplayName("reduces power by penalty amount")
        void withDeathPenalty_reducesPower() {
            PlayerPower power = PlayerPower.create(TEST_UUID, 10.0, 20.0);
            PlayerPower updated = power.withDeathPenalty(3.0);

            assertEquals(7.0, updated.power());
        }

        @Test
        @DisplayName("clamps at zero when penalty exceeds current power")
        void withDeathPenalty_clampsAtZero() {
            PlayerPower power = PlayerPower.create(TEST_UUID, 2.0, 20.0);
            PlayerPower updated = power.withDeathPenalty(5.0);

            assertEquals(0.0, updated.power());
        }

        @Test
        @DisplayName("updates lastDeath timestamp")
        void withDeathPenalty_updatesLastDeath() {
            PlayerPower power = PlayerPower.create(TEST_UUID, 10.0, 20.0);
            long before = System.currentTimeMillis();
            PlayerPower updated = power.withDeathPenalty(1.0);
            long after = System.currentTimeMillis();

            assertTrue(updated.lastDeath() >= before);
            assertTrue(updated.lastDeath() <= after);
        }
    }

    @Nested
    @DisplayName("withRegen()")
    class WithRegenTests {

        @Test
        @DisplayName("increases power by regen amount")
        void withRegen_increasesPower() {
            PlayerPower power = PlayerPower.create(TEST_UUID, 10.0, 20.0);
            PlayerPower updated = power.withRegen(2.5);

            assertEquals(12.5, updated.power());
        }

        @Test
        @DisplayName("clamps at max when regen would exceed maximum")
        void withRegen_clampsAtMax() {
            PlayerPower power = PlayerPower.create(TEST_UUID, 18.0, 20.0);
            PlayerPower updated = power.withRegen(5.0);

            assertEquals(20.0, updated.power());
        }

        @Test
        @DisplayName("updates lastRegen timestamp")
        void withRegen_updatesLastRegen() {
            PlayerPower power = new PlayerPower(TEST_UUID, 10.0, 20.0, 0, 0, null, false, false);
            long before = System.currentTimeMillis();
            PlayerPower updated = power.withRegen(1.0);
            long after = System.currentTimeMillis();

            assertTrue(updated.lastRegen() >= before);
            assertTrue(updated.lastRegen() <= after);
        }
    }

    @Nested
    @DisplayName("withMaxPower()")
    class WithMaxPowerTests {

        @Test
        @DisplayName("updates max power value")
        void withMaxPower_updatesMax() {
            PlayerPower power = PlayerPower.create(TEST_UUID, 10.0, 20.0);
            PlayerPower updated = power.withMaxPower(30.0);

            assertEquals(30.0, updated.maxPower());
        }

        @Test
        @DisplayName("adjusts current power if it exceeds new max")
        void withMaxPower_adjustsPowerIfExceeds() {
            PlayerPower power = PlayerPower.create(TEST_UUID, 15.0, 20.0);
            PlayerPower updated = power.withMaxPower(10.0);

            assertEquals(10.0, updated.maxPower());
            assertEquals(10.0, updated.power());
        }

        @Test
        @DisplayName("preserves current power if within new max")
        void withMaxPower_preservesPowerIfWithin() {
            PlayerPower power = PlayerPower.create(TEST_UUID, 8.0, 20.0);
            PlayerPower updated = power.withMaxPower(15.0);

            assertEquals(15.0, updated.maxPower());
            assertEquals(8.0, updated.power());
        }
    }

    @Nested
    @DisplayName("getPowerInt() / getMaxPowerInt()")
    class IntegerConversionTests {

        @Test
        @DisplayName("floors power correctly")
        void getPowerInt_floorsCorrectly() {
            PlayerPower power = new PlayerPower(TEST_UUID, 15.7, 20.0, 0, 0, null, false, false);
            assertEquals(15, power.getPowerInt());

            PlayerPower power2 = new PlayerPower(TEST_UUID, 15.2, 20.0, 0, 0, null, false, false);
            assertEquals(15, power2.getPowerInt());
        }

        @Test
        @DisplayName("floors max power correctly")
        void getMaxPowerInt_floorsCorrectly() {
            PlayerPower power = new PlayerPower(TEST_UUID, 10.0, 25.9, 0, 0, null, false, false);
            assertEquals(25, power.getMaxPowerInt());
        }
    }

    @Nested
    @DisplayName("isAtMax()")
    class IsAtMaxTests {

        @Test
        @DisplayName("returns true when power equals max")
        void isAtMax_trueWhenAtMax() {
            PlayerPower power = new PlayerPower(TEST_UUID, 20.0, 20.0, 0, 0, null, false, false);
            assertTrue(power.isAtMax());
        }

        @Test
        @DisplayName("returns true when power exceeds max (edge case)")
        void isAtMax_trueWhenAboveMax() {
            // Direct construction can bypass clamping for edge case testing
            PlayerPower power = new PlayerPower(TEST_UUID, 25.0, 20.0, 0, 0, null, false, false);
            assertTrue(power.isAtMax());
        }

        @Test
        @DisplayName("returns false when power is below max")
        void isAtMax_falseWhenBelow() {
            PlayerPower power = new PlayerPower(TEST_UUID, 19.9, 20.0, 0, 0, null, false, false);
            assertFalse(power.isAtMax());
        }
    }

    @Nested
    @DisplayName("getPowerPercent()")
    class PowerPercentTests {

        @Test
        @DisplayName("calculates percentage correctly")
        void getPowerPercent_calculatesCorrectly() {
            PlayerPower power = new PlayerPower(TEST_UUID, 10.0, 20.0, 0, 0, null, false, false);
            assertEquals(50, power.getPowerPercent());

            PlayerPower power2 = new PlayerPower(TEST_UUID, 15.0, 20.0, 0, 0, null, false, false);
            assertEquals(75, power2.getPowerPercent());

            PlayerPower power3 = new PlayerPower(TEST_UUID, 20.0, 20.0, 0, 0, null, false, false);
            assertEquals(100, power3.getPowerPercent());
        }

        @Test
        @DisplayName("returns zero when max power is zero")
        void getPowerPercent_handlesZeroMax() {
            PlayerPower power = new PlayerPower(TEST_UUID, 0.0, 0.0, 0, 0, null, false, false);
            assertEquals(0, power.getPowerPercent());
        }

        @Test
        @DisplayName("returns zero when max power is negative")
        void getPowerPercent_handlesNegativeMax() {
            PlayerPower power = new PlayerPower(TEST_UUID, 5.0, -10.0, 0, 0, null, false, false);
            assertEquals(0, power.getPowerPercent());
        }

        @Test
        @DisplayName("rounds to nearest integer")
        void getPowerPercent_roundsCorrectly() {
            // 7.5 / 20 = 0.375 = 37.5% -> rounds to 38
            PlayerPower power = new PlayerPower(TEST_UUID, 7.5, 20.0, 0, 0, null, false, false);
            assertEquals(38, power.getPowerPercent());
        }
    }
}
