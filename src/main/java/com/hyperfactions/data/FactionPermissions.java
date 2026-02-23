package com.hyperfactions.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Represents configurable territory permissions for a faction.
 * Controls what outsiders, allies, members, and officers can do in faction territory,
 * plus mob spawning and global settings.
 * <p>
 * Backed by an immutable Map of flag names to boolean values.
 * Supports parent-child relationships where child flags are implicitly false
 * when their parent is false.
 * <p>
 * Parent-child relationships:
 * <ul>
 *   <li>{level}Interact → {level}DoorUse, {level}ContainerUse, {level}BenchUse, {level}ProcessingUse, {level}SeatUse, {level}TransportUse</li>
 *   <li>mobSpawning → hostileMobSpawning, passiveMobSpawning, neutralMobSpawning</li>
 * </ul>
 */
public final class FactionPermissions {

    // =========================================================================
    // LEVEL CONSTANTS
    // =========================================================================

    public static final String LEVEL_OUTSIDER = "outsider";
    public static final String LEVEL_ALLY = "ally";
    public static final String LEVEL_MEMBER = "member";
    public static final String LEVEL_OFFICER = "officer";

    public static final String[] ALL_LEVELS = { LEVEL_OUTSIDER, LEVEL_ALLY, LEVEL_MEMBER, LEVEL_OFFICER };

    // =========================================================================
    // PER-LEVEL FLAG SUFFIXES
    // =========================================================================

    public static final String SUFFIX_BREAK = "Break";
    public static final String SUFFIX_PLACE = "Place";
    public static final String SUFFIX_INTERACT = "Interact";
    public static final String SUFFIX_DOOR_USE = "DoorUse";
    public static final String SUFFIX_CONTAINER_USE = "ContainerUse";
    public static final String SUFFIX_BENCH_USE = "BenchUse";
    public static final String SUFFIX_PROCESSING_USE = "ProcessingUse";
    public static final String SUFFIX_SEAT_USE = "SeatUse";
    public static final String SUFFIX_TRANSPORT_USE = "TransportUse";

    // =========================================================================
    // OUTSIDER FLAGS
    // =========================================================================

    public static final String OUTSIDER_BREAK = "outsiderBreak";
    public static final String OUTSIDER_PLACE = "outsiderPlace";
    public static final String OUTSIDER_INTERACT = "outsiderInteract";
    public static final String OUTSIDER_DOOR_USE = "outsiderDoorUse";
    public static final String OUTSIDER_CONTAINER_USE = "outsiderContainerUse";
    public static final String OUTSIDER_BENCH_USE = "outsiderBenchUse";
    public static final String OUTSIDER_PROCESSING_USE = "outsiderProcessingUse";
    public static final String OUTSIDER_SEAT_USE = "outsiderSeatUse";
    public static final String OUTSIDER_TRANSPORT_USE = "outsiderTransportUse";

    // =========================================================================
    // ALLY FLAGS
    // =========================================================================

    public static final String ALLY_BREAK = "allyBreak";
    public static final String ALLY_PLACE = "allyPlace";
    public static final String ALLY_INTERACT = "allyInteract";
    public static final String ALLY_DOOR_USE = "allyDoorUse";
    public static final String ALLY_CONTAINER_USE = "allyContainerUse";
    public static final String ALLY_BENCH_USE = "allyBenchUse";
    public static final String ALLY_PROCESSING_USE = "allyProcessingUse";
    public static final String ALLY_SEAT_USE = "allySeatUse";
    public static final String ALLY_TRANSPORT_USE = "allyTransportUse";

    // =========================================================================
    // MEMBER FLAGS
    // =========================================================================

    public static final String MEMBER_BREAK = "memberBreak";
    public static final String MEMBER_PLACE = "memberPlace";
    public static final String MEMBER_INTERACT = "memberInteract";
    public static final String MEMBER_DOOR_USE = "memberDoorUse";
    public static final String MEMBER_CONTAINER_USE = "memberContainerUse";
    public static final String MEMBER_BENCH_USE = "memberBenchUse";
    public static final String MEMBER_PROCESSING_USE = "memberProcessingUse";
    public static final String MEMBER_SEAT_USE = "memberSeatUse";
    public static final String MEMBER_TRANSPORT_USE = "memberTransportUse";

    // =========================================================================
    // OFFICER FLAGS
    // =========================================================================

    public static final String OFFICER_BREAK = "officerBreak";
    public static final String OFFICER_PLACE = "officerPlace";
    public static final String OFFICER_INTERACT = "officerInteract";
    public static final String OFFICER_DOOR_USE = "officerDoorUse";
    public static final String OFFICER_CONTAINER_USE = "officerContainerUse";
    public static final String OFFICER_BENCH_USE = "officerBenchUse";
    public static final String OFFICER_PROCESSING_USE = "officerProcessingUse";
    public static final String OFFICER_SEAT_USE = "officerSeatUse";
    public static final String OFFICER_TRANSPORT_USE = "officerTransportUse";

    // =========================================================================
    // MOB SPAWNING FLAGS
    // =========================================================================

    public static final String MOB_SPAWNING = "mobSpawning";
    public static final String HOSTILE_MOB_SPAWNING = "hostileMobSpawning";
    public static final String PASSIVE_MOB_SPAWNING = "passiveMobSpawning";
    public static final String NEUTRAL_MOB_SPAWNING = "neutralMobSpawning";

    // =========================================================================
    // GLOBAL SETTINGS
    // =========================================================================

    public static final String PVP_ENABLED = "pvpEnabled";
    public static final String OFFICERS_CAN_EDIT = "officersCanEdit";

    // =========================================================================
    // TREASURY FLAGS
    // =========================================================================

    /** Whether members can deposit into the faction treasury */
    public static final String TREASURY_DEPOSIT = "treasuryDeposit";
    /** Whether members can withdraw from the faction treasury */
    public static final String TREASURY_WITHDRAW = "treasuryWithdraw";
    /** Whether members can transfer money to other factions */
    public static final String TREASURY_TRANSFER = "treasuryTransfer";

    // =========================================================================
    // ALL FLAGS
    // =========================================================================

    /** All 45 flag names for iteration, validation, and serialization. */
    public static final List<String> ALL_FLAGS = List.of(
        // Outsider (9)
        OUTSIDER_BREAK, OUTSIDER_PLACE, OUTSIDER_INTERACT,
        OUTSIDER_DOOR_USE, OUTSIDER_CONTAINER_USE, OUTSIDER_BENCH_USE,
        OUTSIDER_PROCESSING_USE, OUTSIDER_SEAT_USE, OUTSIDER_TRANSPORT_USE,
        // Ally (9)
        ALLY_BREAK, ALLY_PLACE, ALLY_INTERACT,
        ALLY_DOOR_USE, ALLY_CONTAINER_USE, ALLY_BENCH_USE,
        ALLY_PROCESSING_USE, ALLY_SEAT_USE, ALLY_TRANSPORT_USE,
        // Member (9)
        MEMBER_BREAK, MEMBER_PLACE, MEMBER_INTERACT,
        MEMBER_DOOR_USE, MEMBER_CONTAINER_USE, MEMBER_BENCH_USE,
        MEMBER_PROCESSING_USE, MEMBER_SEAT_USE, MEMBER_TRANSPORT_USE,
        // Officer (9)
        OFFICER_BREAK, OFFICER_PLACE, OFFICER_INTERACT,
        OFFICER_DOOR_USE, OFFICER_CONTAINER_USE, OFFICER_BENCH_USE,
        OFFICER_PROCESSING_USE, OFFICER_SEAT_USE, OFFICER_TRANSPORT_USE,
        // Mob Spawning (4)
        MOB_SPAWNING, HOSTILE_MOB_SPAWNING, PASSIVE_MOB_SPAWNING, NEUTRAL_MOB_SPAWNING,
        // Global (2)
        PVP_ENABLED, OFFICERS_CAN_EDIT,
        // Treasury (3)
        TREASURY_DEPOSIT, TREASURY_WITHDRAW, TREASURY_TRANSFER
    );

    /** Set for fast validation. */
    private static final Set<String> ALL_FLAGS_SET = Set.copyOf(ALL_FLAGS);

    /** Per-level flag suffixes (for building flag names from level + suffix). */
    private static final String[] LEVEL_SUFFIXES = {
        SUFFIX_BREAK, SUFFIX_PLACE, SUFFIX_INTERACT,
        SUFFIX_DOOR_USE, SUFFIX_CONTAINER_USE, SUFFIX_BENCH_USE,
        SUFFIX_PROCESSING_USE, SUFFIX_SEAT_USE, SUFFIX_TRANSPORT_USE
    };

    /** Interaction child suffixes (children of Interact). */
    private static final String[] INTERACT_CHILD_SUFFIXES = {
        SUFFIX_DOOR_USE, SUFFIX_CONTAINER_USE, SUFFIX_BENCH_USE,
        SUFFIX_PROCESSING_USE, SUFFIX_SEAT_USE, SUFFIX_TRANSPORT_USE
    };

    // =========================================================================
    // INSTANCE DATA
    // =========================================================================

    private final Map<String, Boolean> flags;

    /**
     * Creates FactionPermissions from a map of flag values.
     * Missing flags are filled from defaults.
     *
     * @param flags the flag values
     */
    public FactionPermissions(@NotNull Map<String, Boolean> flags) {
        Map<String, Boolean> complete = new HashMap<>(getDefaultMap());
        complete.putAll(flags);
        this.flags = Map.copyOf(complete);
    }

    // =========================================================================
    // STATIC FACTORIES
    // =========================================================================

    /**
     * Creates FactionPermissions with all default values.
     *
     * @return default permissions
     */
    @NotNull
    public static FactionPermissions defaults() {
        return new FactionPermissions(getDefaultMap());
    }

    /**
     * Gets the default values for all flags.
     */
    @NotNull
    private static Map<String, Boolean> getDefaultMap() {
        Map<String, Boolean> defaults = new HashMap<>();

        // Outsider: deny all except door/seat use
        defaults.put(OUTSIDER_BREAK, false);
        defaults.put(OUTSIDER_PLACE, false);
        defaults.put(OUTSIDER_INTERACT, false);
        defaults.put(OUTSIDER_DOOR_USE, false);
        defaults.put(OUTSIDER_CONTAINER_USE, false);
        defaults.put(OUTSIDER_BENCH_USE, false);
        defaults.put(OUTSIDER_PROCESSING_USE, false);
        defaults.put(OUTSIDER_SEAT_USE, false);
        defaults.put(OUTSIDER_TRANSPORT_USE, false);

        // Ally: interact + doors/seats/teleporters/portals
        defaults.put(ALLY_BREAK, false);
        defaults.put(ALLY_PLACE, false);
        defaults.put(ALLY_INTERACT, true);
        defaults.put(ALLY_DOOR_USE, true);
        defaults.put(ALLY_CONTAINER_USE, false);
        defaults.put(ALLY_BENCH_USE, false);
        defaults.put(ALLY_PROCESSING_USE, false);
        defaults.put(ALLY_SEAT_USE, true);
        defaults.put(ALLY_TRANSPORT_USE, true);

        // Member: full access
        defaults.put(MEMBER_BREAK, true);
        defaults.put(MEMBER_PLACE, true);
        defaults.put(MEMBER_INTERACT, true);
        defaults.put(MEMBER_DOOR_USE, true);
        defaults.put(MEMBER_CONTAINER_USE, true);
        defaults.put(MEMBER_BENCH_USE, true);
        defaults.put(MEMBER_PROCESSING_USE, true);
        defaults.put(MEMBER_SEAT_USE, true);
        defaults.put(MEMBER_TRANSPORT_USE, true);

        // Officer: full access
        defaults.put(OFFICER_BREAK, true);
        defaults.put(OFFICER_PLACE, true);
        defaults.put(OFFICER_INTERACT, true);
        defaults.put(OFFICER_DOOR_USE, true);
        defaults.put(OFFICER_CONTAINER_USE, true);
        defaults.put(OFFICER_BENCH_USE, true);
        defaults.put(OFFICER_PROCESSING_USE, true);
        defaults.put(OFFICER_SEAT_USE, true);
        defaults.put(OFFICER_TRANSPORT_USE, true);

        // Mob spawning: allowed by default (same as no protection)
        defaults.put(MOB_SPAWNING, true);
        defaults.put(HOSTILE_MOB_SPAWNING, true);
        defaults.put(PASSIVE_MOB_SPAWNING, true);
        defaults.put(NEUTRAL_MOB_SPAWNING, true);

        // Global
        defaults.put(PVP_ENABLED, true);
        defaults.put(OFFICERS_CAN_EDIT, false);

        // Treasury: deposit open to all, withdraw/transfer officer+ only
        defaults.put(TREASURY_DEPOSIT, true);
        defaults.put(TREASURY_WITHDRAW, false);
        defaults.put(TREASURY_TRANSFER, false);

        return defaults;
    }

    // =========================================================================
    // FLAG ACCESS
    // =========================================================================

    /**
     * Gets a flag value with parent-child logic applied.
     * If the flag has a parent and the parent is false, returns false regardless
     * of the child's stored value.
     *
     * @param flagName the flag name
     * @return the effective value
     */
    public boolean get(@NotNull String flagName) {
        String parent = getParentFlag(flagName);
        if (parent != null && !getRaw(parent)) {
            return false;
        }
        return getRaw(flagName);
    }

    /**
     * Gets the raw stored value for a flag without parent checks.
     *
     * @param flagName the flag name
     * @return the stored value, or false if unknown
     */
    public boolean getRaw(@NotNull String flagName) {
        return flags.getOrDefault(flagName, false);
    }

    /**
     * Creates a new FactionPermissions with one flag toggled.
     *
     * @param flagName the flag to toggle
     * @return new FactionPermissions with toggled value, or same if unknown flag
     */
    @NotNull
    public FactionPermissions toggle(@NotNull String flagName) {
        if (!ALL_FLAGS_SET.contains(flagName)) {
            return this;
        }
        Map<String, Boolean> newFlags = new HashMap<>(flags);
        newFlags.put(flagName, !getRaw(flagName));
        return new FactionPermissions(newFlags);
    }

    /**
     * Creates a new FactionPermissions with a flag set to a specific value.
     *
     * @param flagName the flag name
     * @param value    the new value
     * @return new FactionPermissions
     */
    @NotNull
    public FactionPermissions set(@NotNull String flagName, boolean value) {
        if (!ALL_FLAGS_SET.contains(flagName)) {
            return this;
        }
        Map<String, Boolean> newFlags = new HashMap<>(flags);
        newFlags.put(flagName, value);
        return new FactionPermissions(newFlags);
    }

    /**
     * Returns the underlying flag map for serialization.
     *
     * @return unmodifiable map of all flag values
     */
    @NotNull
    public Map<String, Boolean> toMap() {
        return flags;
    }

    // =========================================================================
    // PARENT-CHILD RELATIONSHIPS
    // =========================================================================

    /**
     * Gets the parent flag for a given flag, if it has one.
     *
     * @param flagName the flag name
     * @return the parent flag name, or null if no parent
     */
    @Nullable
    public static String getParentFlag(@NotNull String flagName) {
        // Interaction children: {level}DoorUse etc. → {level}Interact
        for (String level : ALL_LEVELS) {
            for (String childSuffix : INTERACT_CHILD_SUFFIXES) {
                if (flagName.equals(level + childSuffix)) {
                    return level + SUFFIX_INTERACT;
                }
            }
        }
        // Mob spawning children → mobSpawning
        return switch (flagName) {
            case HOSTILE_MOB_SPAWNING, PASSIVE_MOB_SPAWNING, NEUTRAL_MOB_SPAWNING -> MOB_SPAWNING;
            default -> null;
        };
    }

    /**
     * Checks if a flag is a parent flag (has children).
     *
     * @param flagName the flag name
     * @return true if parent
     */
    public static boolean isParentFlag(@NotNull String flagName) {
        if (MOB_SPAWNING.equals(flagName)) return true;
        for (String level : ALL_LEVELS) {
            if (flagName.equals(level + SUFFIX_INTERACT)) return true;
        }
        return false;
    }

    /**
     * Gets the child flags for a parent flag.
     *
     * @param parentFlagName the parent flag
     * @return list of child flag names, or empty if not a parent
     */
    @NotNull
    public static List<String> getChildFlags(@NotNull String parentFlagName) {
        if (MOB_SPAWNING.equals(parentFlagName)) {
            return List.of(HOSTILE_MOB_SPAWNING, PASSIVE_MOB_SPAWNING, NEUTRAL_MOB_SPAWNING);
        }
        for (String level : ALL_LEVELS) {
            if (parentFlagName.equals(level + SUFFIX_INTERACT)) {
                List<String> children = new ArrayList<>();
                for (String suffix : INTERACT_CHILD_SUFFIXES) {
                    children.add(level + suffix);
                }
                return children;
            }
        }
        return List.of();
    }

    /**
     * Gets the flags for a specific level.
     *
     * @param level the level (outsider, ally, member, officer)
     * @return list of flag names for that level
     */
    @NotNull
    public static List<String> getFlagsForLevel(@NotNull String level) {
        List<String> levelFlags = new ArrayList<>();
        for (String suffix : LEVEL_SUFFIXES) {
            levelFlags.add(level + suffix);
        }
        return levelFlags;
    }

    /**
     * Checks if a flag name is valid.
     *
     * @param flagName the flag name
     * @return true if valid
     */
    public static boolean isValidFlag(@NotNull String flagName) {
        return ALL_FLAGS_SET.contains(flagName);
    }

    /**
     * Gets a human-readable display name for a flag.
     *
     * @param flagName the flag name
     * @return the display name
     */
    @NotNull
    public static String getDisplayName(@NotNull String flagName) {
        // Determine level prefix
        String level = null;
        String suffix = null;
        for (String l : ALL_LEVELS) {
            if (flagName.startsWith(l)) {
                level = l;
                suffix = flagName.substring(l.length());
                break;
            }
        }

        if (level != null && suffix != null) {
            String levelDisplay = level.substring(0, 1).toUpperCase() + level.substring(1);
            String suffixDisplay = switch (suffix) {
                case "Break" -> "Break";
                case "Place" -> "Place";
                case "Interact" -> "Interact";
                case "DoorUse" -> "Door Use";
                case "ContainerUse" -> "Container Use";
                case "BenchUse" -> "Bench Use";
                case "ProcessingUse" -> "Processing Use";
                case "SeatUse" -> "Seat Use";
                case "TransportUse" -> "Transport";
                default -> suffix;
            };
            return levelDisplay + " " + suffixDisplay;
        }

        return switch (flagName) {
            case MOB_SPAWNING -> "Mob Spawning";
            case HOSTILE_MOB_SPAWNING -> "Hostile Mobs";
            case PASSIVE_MOB_SPAWNING -> "Passive Mobs";
            case NEUTRAL_MOB_SPAWNING -> "Neutral Mobs";
            case PVP_ENABLED -> "PvP Enabled";
            case OFFICERS_CAN_EDIT -> "Officers Can Edit";
            case TREASURY_DEPOSIT -> "Treasury Deposit";
            case TREASURY_WITHDRAW -> "Allow Officers to Withdraw";
            case TREASURY_TRANSFER -> "Allow Officers to Transfer";
            default -> flagName;
        };
    }

    // =========================================================================
    // BACKWARD-COMPATIBLE ACCESSOR METHODS
    // =========================================================================

    // These keep existing code working without mass refactoring.

    public boolean outsiderBreak() { return getRaw(OUTSIDER_BREAK); }
    public boolean outsiderPlace() { return getRaw(OUTSIDER_PLACE); }
    public boolean outsiderInteract() { return getRaw(OUTSIDER_INTERACT); }

    public boolean allyBreak() { return getRaw(ALLY_BREAK); }
    public boolean allyPlace() { return getRaw(ALLY_PLACE); }
    public boolean allyInteract() { return getRaw(ALLY_INTERACT); }

    public boolean memberBreak() { return getRaw(MEMBER_BREAK); }
    public boolean memberPlace() { return getRaw(MEMBER_PLACE); }
    public boolean memberInteract() { return getRaw(MEMBER_INTERACT); }

    public boolean officerBreak() { return getRaw(OFFICER_BREAK); }
    public boolean officerPlace() { return getRaw(OFFICER_PLACE); }
    public boolean officerInteract() { return getRaw(OFFICER_INTERACT); }

    public boolean pvpEnabled() { return getRaw(PVP_ENABLED); }
    public boolean officersCanEdit() { return getRaw(OFFICERS_CAN_EDIT); }

    // =========================================================================
    // BACKWARD-COMPATIBLE BUILDER METHODS
    // =========================================================================

    public FactionPermissions withOutsiderBreak(boolean value) { return set(OUTSIDER_BREAK, value); }
    public FactionPermissions withOutsiderPlace(boolean value) { return set(OUTSIDER_PLACE, value); }
    public FactionPermissions withOutsiderInteract(boolean value) { return set(OUTSIDER_INTERACT, value); }

    public FactionPermissions withAllyBreak(boolean value) { return set(ALLY_BREAK, value); }
    public FactionPermissions withAllyPlace(boolean value) { return set(ALLY_PLACE, value); }
    public FactionPermissions withAllyInteract(boolean value) { return set(ALLY_INTERACT, value); }

    public FactionPermissions withMemberBreak(boolean value) { return set(MEMBER_BREAK, value); }
    public FactionPermissions withMemberPlace(boolean value) { return set(MEMBER_PLACE, value); }
    public FactionPermissions withMemberInteract(boolean value) { return set(MEMBER_INTERACT, value); }

    public FactionPermissions withPvpEnabled(boolean value) { return set(PVP_ENABLED, value); }
    public FactionPermissions withOfficersCanEdit(boolean value) { return set(OFFICERS_CAN_EDIT, value); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FactionPermissions other)) return false;
        return flags.equals(other.flags);
    }

    @Override
    public int hashCode() {
        return flags.hashCode();
    }

    @Override
    public String toString() {
        return "FactionPermissions" + flags;
    }
}
