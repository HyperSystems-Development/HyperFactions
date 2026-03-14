package com.hyperfactions.testutil;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.PlayerData;
import com.hyperfactions.data.PlayerPower;
import com.hyperfactions.data.Zone;
import com.hyperfactions.storage.FactionStorage;
import com.hyperfactions.storage.PlayerStorage;
import com.hyperfactions.storage.ZoneStorage;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * In-memory storage implementations for testing.
 */
public final class MockStorage {

    private MockStorage() {}

    /**
     * In-memory faction storage for testing.
     */
    public static class MockFactionStorage implements FactionStorage {
        private final Map<UUID, Faction> factions = new ConcurrentHashMap<>();

        @Override
        public CompletableFuture<Void> init() {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> shutdown() {
            factions.clear();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Optional<Faction>> loadFaction(@NotNull UUID factionId) {
            return CompletableFuture.completedFuture(Optional.ofNullable(factions.get(factionId)));
        }

        @Override
        public CompletableFuture<Void> saveFaction(@NotNull Faction faction) {
            factions.put(faction.id(), faction);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> deleteFaction(@NotNull UUID factionId) {
            factions.remove(factionId);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Collection<Faction>> loadAllFactions() {
            return CompletableFuture.completedFuture(new ArrayList<>(factions.values()));
        }

        /**
         * Adds a faction directly to storage (for test setup).
         *
         * @param faction the faction to add
         */
        public void addFaction(@NotNull Faction faction) {
            factions.put(faction.id(), faction);
        }

        /**
         * Gets the current faction count.
         *
         * @return number of stored factions
         */
        public int size() {
            return factions.size();
        }

        /**
         * Clears all stored factions.
         */
        public void clear() {
            factions.clear();
        }

        /**
         * Gets all stored factions.
         *
         * @return collection of factions
         */
        public Collection<Faction> getAll() {
            return new ArrayList<>(factions.values());
        }
    }

    /**
     * In-memory player storage for testing.
     */
    public static class MockPlayerStorage implements PlayerStorage {
        private final Map<UUID, PlayerPower> players = new ConcurrentHashMap<>();
        private final Map<UUID, PlayerData> playerData = new ConcurrentHashMap<>();

        @Override
        public CompletableFuture<Void> init() {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> shutdown() {
            players.clear();
            playerData.clear();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Optional<PlayerPower>> loadPlayerPower(@NotNull UUID uuid) {
            return CompletableFuture.completedFuture(Optional.ofNullable(players.get(uuid)));
        }

        @Override
        public CompletableFuture<Void> savePlayerPower(@NotNull PlayerPower power) {
            players.put(power.uuid(), power);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> deletePlayerPower(@NotNull UUID uuid) {
            players.remove(uuid);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Collection<PlayerPower>> loadAllPlayerPower() {
            return CompletableFuture.completedFuture(new ArrayList<>(players.values()));
        }

        @Override
        public CompletableFuture<Set<UUID>> getAllPlayerUuids() {
            Set<UUID> uuids = new HashSet<>(players.keySet());
            uuids.addAll(playerData.keySet());
            return CompletableFuture.completedFuture(uuids);
        }

        @Override
        public CompletableFuture<Optional<PlayerData>> loadPlayerData(@NotNull UUID uuid) {
            return CompletableFuture.completedFuture(Optional.ofNullable(playerData.get(uuid)));
        }

        @Override
        public CompletableFuture<Void> savePlayerData(@NotNull PlayerData data) {
            playerData.put(data.getUuid(), data);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> updatePlayerData(@NotNull UUID uuid, @NotNull Consumer<PlayerData> updater) {
            PlayerData data = playerData.get(uuid);
            if (data == null) {
                data = new PlayerData(uuid);
                playerData.put(uuid, data);
            }
            updater.accept(data);
            return CompletableFuture.completedFuture(null);
        }

        /**
         * Adds player power directly to storage (for test setup).
         *
         * @param power the player power to add
         */
        public void addPlayerPower(@NotNull PlayerPower power) {
            players.put(power.uuid(), power);
        }

        /**
         * Gets the current player count.
         *
         * @return number of stored players
         */
        public int size() {
            return players.size();
        }

        /**
         * Clears all stored players.
         */
        public void clear() {
            players.clear();
            playerData.clear();
        }

        /**
         * Gets player power by UUID.
         *
         * @param uuid the player UUID
         * @return the player power, or null if not found
         */
        public PlayerPower get(@NotNull UUID uuid) {
            return players.get(uuid);
        }
    }

    /**
     * In-memory zone storage for testing.
     */
    public static class MockZoneStorage implements ZoneStorage {
        private final Map<UUID, Zone> zones = new ConcurrentHashMap<>();

        @Override
        public CompletableFuture<Void> init() {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> shutdown() {
            zones.clear();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Collection<Zone>> loadAllZones() {
            return CompletableFuture.completedFuture(new ArrayList<>(zones.values()));
        }

        @Override
        public CompletableFuture<Void> saveAllZones(@NotNull Collection<Zone> zonesToSave) {
            zones.clear();
            for (Zone zone : zonesToSave) {
                zones.put(zone.id(), zone);
            }
            return CompletableFuture.completedFuture(null);
        }

        /**
         * Adds a zone directly to storage (for test setup).
         *
         * @param zone the zone to add
         */
        public void addZone(@NotNull Zone zone) {
            zones.put(zone.id(), zone);
        }

        /**
         * Gets the current zone count.
         *
         * @return number of stored zones
         */
        public int size() {
            return zones.size();
        }

        /**
         * Clears all stored zones.
         */
        public void clear() {
            zones.clear();
        }
    }

    /**
     * Creates a new mock faction storage.
     *
     * @return a new MockFactionStorage
     */
    public static MockFactionStorage factionStorage() {
        return new MockFactionStorage();
    }

    /**
     * Creates a new mock player storage.
     *
     * @return a new MockPlayerStorage
     */
    public static MockPlayerStorage playerStorage() {
        return new MockPlayerStorage();
    }

    /**
     * Creates a new mock zone storage.
     *
     * @return a new MockZoneStorage
     */
    public static MockZoneStorage zoneStorage() {
        return new MockZoneStorage();
    }
}
