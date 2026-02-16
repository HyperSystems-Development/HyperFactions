package com.hyperfactions.manager;

import com.hyperfactions.Permissions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.*;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Manages faction relations (ally, enemy, neutral).
 */
public class RelationManager {

    private final FactionManager factionManager;

    // Pending ally requests: target faction -> (requesting faction -> requester player UUID)
    private final Map<UUID, Map<UUID, UUID>> pendingAllyRequests = new ConcurrentHashMap<>();

    // GUI update callbacks
    @Nullable
    private BiConsumer<UUID, UUID> onRelationChanged;
    @Nullable
    private BiConsumer<UUID, UUID> onRelationChangedForMapFilter;
    @Nullable
    private BiConsumer<UUID, UUID> onAllyRequestReceived;

    // Announcement callbacks
    @Nullable
    private BiConsumer<String, String> onWarDeclared;
    @Nullable
    private BiConsumer<String, String> onAllianceFormed;
    @Nullable
    private BiConsumer<String, String> onAllianceBroken;

    public RelationManager(@NotNull FactionManager factionManager) {
        this.factionManager = factionManager;
    }

    /**
     * Sets a callback for when a relation changes between two factions.
     * Params: factionId, targetFactionId
     */
    public void setOnRelationChanged(@Nullable BiConsumer<UUID, UUID> callback) {
        this.onRelationChanged = callback;
    }

    /**
     * Sets a callback for when a relation changes, specifically for map player filter updates.
     * Separate from the GUI callback to keep concerns independent.
     * Params: factionId, targetFactionId
     */
    public void setOnRelationChangedForMapFilter(@Nullable BiConsumer<UUID, UUID> callback) {
        this.onRelationChangedForMapFilter = callback;
    }

    /**
     * Sets a callback for when an ally request is received.
     * Params: targetFactionId, fromFactionId
     */
    public void setOnAllyRequestReceived(@Nullable BiConsumer<UUID, UUID> callback) {
        this.onAllyRequestReceived = callback;
    }

    /**
     * Sets a callback for when war is declared.
     * Params: declaringFactionName, targetFactionName
     */
    public void setOnWarDeclared(@Nullable BiConsumer<String, String> callback) {
        this.onWarDeclared = callback;
    }

    /**
     * Sets a callback for when an alliance is formed.
     * Params: faction1Name, faction2Name
     */
    public void setOnAllianceFormed(@Nullable BiConsumer<String, String> callback) {
        this.onAllianceFormed = callback;
    }

    /**
     * Sets a callback for when an alliance is broken.
     * Params: faction1Name, faction2Name
     */
    public void setOnAllianceBroken(@Nullable BiConsumer<String, String> callback) {
        this.onAllianceBroken = callback;
    }

    /**
     * Result of a relation operation.
     */
    public enum RelationResult {
        SUCCESS,
        NO_PERMISSION,
        NOT_IN_FACTION,
        NOT_OFFICER,
        FACTION_NOT_FOUND,
        CANNOT_RELATE_SELF,
        ALREADY_ALLY,
        ALREADY_ENEMY,
        ALREADY_NEUTRAL,
        REQUEST_SENT,
        REQUEST_ACCEPTED,
        NO_PENDING_REQUEST,
        ALLY_LIMIT_REACHED,
        ENEMY_LIMIT_REACHED
    }

    // === Queries ===

    /**
     * Gets the relation between two factions.
     *
     * @param factionId1 first faction ID
     * @param factionId2 second faction ID
     * @return the relation type
     */
    @NotNull
    public RelationType getRelation(@NotNull UUID factionId1, @NotNull UUID factionId2) {
        if (factionId1.equals(factionId2)) {
            return RelationType.ALLY; // Same faction is always friendly
        }

        Faction faction = factionManager.getFaction(factionId1);
        if (faction == null) {
            return RelationType.NEUTRAL;
        }

        return faction.getRelationType(factionId2);
    }

    /**
     * Gets the relation between two players based on their factions.
     *
     * @param player1 first player UUID
     * @param player2 second player UUID
     * @return the relation type, or null if either has no faction
     */
    @Nullable
    public RelationType getPlayerRelation(@NotNull UUID player1, @NotNull UUID player2) {
        UUID faction1 = factionManager.getPlayerFactionId(player1);
        UUID faction2 = factionManager.getPlayerFactionId(player2);

        if (faction1 == null || faction2 == null) {
            return null;
        }

        if (faction1.equals(faction2)) {
            return RelationType.ALLY; // Same faction
        }

        return getRelation(faction1, faction2);
    }

    /**
     * Checks if two factions are allies.
     *
     * @param factionId1 first faction ID
     * @param factionId2 second faction ID
     * @return true if allies
     */
    public boolean areAllies(@NotNull UUID factionId1, @NotNull UUID factionId2) {
        return getRelation(factionId1, factionId2) == RelationType.ALLY;
    }

    /**
     * Checks if two factions are enemies.
     *
     * @param factionId1 first faction ID
     * @param factionId2 second faction ID
     * @return true if enemies
     */
    public boolean areEnemies(@NotNull UUID factionId1, @NotNull UUID factionId2) {
        return getRelation(factionId1, factionId2) == RelationType.ENEMY;
    }

    /**
     * Checks if two players are in allied factions.
     *
     * @param player1 first player UUID
     * @param player2 second player UUID
     * @return true if allies or same faction
     */
    public boolean arePlayersAllied(@NotNull UUID player1, @NotNull UUID player2) {
        if (factionManager.areInSameFaction(player1, player2)) {
            return true;
        }

        RelationType relation = getPlayerRelation(player1, player2);
        return relation == RelationType.ALLY;
    }

    /**
     * Gets all faction IDs that are allies with the given faction.
     *
     * @param factionId the faction ID
     * @return list of ally faction IDs
     */
    @NotNull
    public List<UUID> getAllies(@NotNull UUID factionId) {
        Faction faction = factionManager.getFaction(factionId);
        if (faction == null) {
            return Collections.emptyList();
        }

        return faction.relations().values().stream()
                .filter(r -> r.type() == RelationType.ALLY)
                .map(FactionRelation::targetFactionId)
                .toList();
    }

    /**
     * Gets all faction IDs that are enemies of the given faction.
     *
     * @param factionId the faction ID
     * @return list of enemy faction IDs
     */
    @NotNull
    public List<UUID> getEnemies(@NotNull UUID factionId) {
        Faction faction = factionManager.getFaction(factionId);
        if (faction == null) {
            return Collections.emptyList();
        }

        return faction.relations().values().stream()
                .filter(r -> r.type() == RelationType.ENEMY)
                .map(FactionRelation::targetFactionId)
                .toList();
    }

    /**
     * Checks if there's a pending ally request.
     *
     * @param fromFactionId the requesting faction
     * @param toFactionId   the target faction
     * @return true if pending
     */
    public boolean hasPendingRequest(@NotNull UUID fromFactionId, @NotNull UUID toFactionId) {
        Map<UUID, UUID> pending = pendingAllyRequests.get(toFactionId);
        return pending != null && pending.containsKey(fromFactionId);
    }

    /**
     * Gets all pending ally requests for a faction.
     *
     * @param factionId the faction ID
     * @return set of requesting faction IDs
     */
    @NotNull
    public Set<UUID> getPendingRequests(@NotNull UUID factionId) {
        Map<UUID, UUID> pending = pendingAllyRequests.get(factionId);
        return pending != null ? Collections.unmodifiableSet(pending.keySet()) : Collections.emptySet();
    }

    /**
     * Gets all outbound ally requests from a faction.
     * These are requests this faction has sent to other factions.
     *
     * @param factionId the faction ID
     * @return set of target faction IDs that have pending requests from this faction
     */
    @NotNull
    public Set<UUID> getOutboundRequests(@NotNull UUID factionId) {
        Set<UUID> outbound = new HashSet<>();
        for (Map.Entry<UUID, Map<UUID, UUID>> entry : pendingAllyRequests.entrySet()) {
            // entry.getKey() = target faction receiving requests
            // entry.getValue() = map of (requesterFactionId -> actorUuid)
            if (entry.getValue().containsKey(factionId)) {
                outbound.add(entry.getKey());
            }
        }
        return outbound;
    }

    // === Operations ===

    /**
     * Sends an ally request or accepts if one is pending from target.
     *
     * @param actorUuid    the actor's UUID
     * @param targetFactionId the target faction ID
     * @return the result
     */
    public RelationResult requestAlly(@NotNull UUID actorUuid, @NotNull UUID targetFactionId) {
        // Check permission first
        if (!PermissionManager.get().hasPermission(actorUuid, Permissions.ALLY)) {
            return RelationResult.NO_PERMISSION;
        }

        Faction actorFaction = factionManager.getPlayerFaction(actorUuid);
        if (actorFaction == null) {
            return RelationResult.NOT_IN_FACTION;
        }

        var member = actorFaction.getMember(actorUuid);
        if (member == null || !member.isOfficerOrHigher()) {
            return RelationResult.NOT_OFFICER;
        }

        if (actorFaction.id().equals(targetFactionId)) {
            return RelationResult.CANNOT_RELATE_SELF;
        }

        Faction targetFaction = factionManager.getFaction(targetFactionId);
        if (targetFaction == null) {
            return RelationResult.FACTION_NOT_FOUND;
        }

        // Check if already allies
        if (actorFaction.isAlly(targetFactionId)) {
            return RelationResult.ALREADY_ALLY;
        }

        // Check ally cap
        int maxAllies = ConfigManager.get().getMaxAllies();
        if (maxAllies >= 0) {
            long allyCount = actorFaction.relations().values().stream()
                .filter(r -> r.type() == RelationType.ALLY)
                .count();
            if (allyCount >= maxAllies) {
                return RelationResult.ALLY_LIMIT_REACHED;
            }
        }

        // Check if target has a pending request from us
        if (hasPendingRequest(targetFactionId, actorFaction.id())) {
            // Accept the existing request
            return acceptAlly(actorUuid, targetFactionId);
        }

        // Send new request (store requester's player UUID for logging)
        pendingAllyRequests.computeIfAbsent(targetFactionId, k -> new ConcurrentHashMap<>())
            .put(actorFaction.id(), actorUuid);

        Logger.info("Faction '%s' sent ally request to '%s'", actorFaction.name(), targetFaction.name());

        if (onAllyRequestReceived != null) {
            try { onAllyRequestReceived.accept(targetFactionId, actorFaction.id()); } catch (Exception e) { Logger.warn("Error in ally request callback: %s", e.getMessage()); }
        }

        return RelationResult.REQUEST_SENT;
    }

    /**
     * Accepts a pending ally request.
     *
     * @param actorUuid       the actor's UUID
     * @param fromFactionId   the faction that sent the request
     * @return the result
     */
    public RelationResult acceptAlly(@NotNull UUID actorUuid, @NotNull UUID fromFactionId) {
        Faction actorFaction = factionManager.getPlayerFaction(actorUuid);
        if (actorFaction == null) {
            return RelationResult.NOT_IN_FACTION;
        }

        var member = actorFaction.getMember(actorUuid);
        if (member == null || !member.isOfficerOrHigher()) {
            return RelationResult.NOT_OFFICER;
        }

        Map<UUID, UUID> pending = pendingAllyRequests.get(actorFaction.id());
        if (pending == null || !pending.containsKey(fromFactionId)) {
            return RelationResult.NO_PENDING_REQUEST;
        }

        // Get the original requester's UUID for logging
        UUID requesterUuid = pending.get(fromFactionId);

        Faction fromFaction = factionManager.getFaction(fromFactionId);
        if (fromFaction == null) {
            pending.remove(fromFactionId);
            return RelationResult.FACTION_NOT_FOUND;
        }

        // Set mutual ally relation (both sides get proper actor attribution)
        setRelation(actorFaction.id(), fromFactionId, RelationType.ALLY, actorUuid);
        setRelation(fromFactionId, actorFaction.id(), RelationType.ALLY, requesterUuid);

        // Remove pending request
        pending.remove(fromFactionId);

        Logger.debugRelation("Alliance accepted: faction1=%s, faction2=%s, accepter=%s, requester=%s",
            actorFaction.name(), fromFaction.name(), actorUuid, requesterUuid);
        Logger.info("Factions '%s' and '%s' are now allies", actorFaction.name(), fromFaction.name());

        if (onAllianceFormed != null) {
            try { onAllianceFormed.accept(actorFaction.name(), fromFaction.name()); } catch (Exception e) { Logger.warn("Error in alliance formed callback: %s", e.getMessage()); }
        }

        return RelationResult.REQUEST_ACCEPTED;
    }

    /**
     * Cancels an outbound ally request.
     *
     * @param actorUuid       the actor's UUID
     * @param targetFactionId the faction the request was sent to
     * @return the result
     */
    public RelationResult cancelRequest(@NotNull UUID actorUuid, @NotNull UUID targetFactionId) {
        Faction actorFaction = factionManager.getPlayerFaction(actorUuid);
        if (actorFaction == null) {
            return RelationResult.NOT_IN_FACTION;
        }

        var member = actorFaction.getMember(actorUuid);
        if (member == null || !member.isOfficerOrHigher()) {
            return RelationResult.NOT_OFFICER;
        }

        // Check if there's a pending request from our faction to the target
        Map<UUID, UUID> pending = pendingAllyRequests.get(targetFactionId);
        if (pending == null || !pending.containsKey(actorFaction.id())) {
            return RelationResult.NO_PENDING_REQUEST;
        }

        // Remove the request
        pending.remove(actorFaction.id());

        Faction targetFaction = factionManager.getFaction(targetFactionId);
        String targetName = targetFaction != null ? targetFaction.name() : "Unknown";
        Logger.info("Faction '%s' cancelled ally request to '%s'", actorFaction.name(), targetName);
        return RelationResult.SUCCESS;
    }

    /**
     * Sets another faction as enemy.
     *
     * @param actorUuid       the actor's UUID
     * @param targetFactionId the target faction ID
     * @return the result
     */
    public RelationResult setEnemy(@NotNull UUID actorUuid, @NotNull UUID targetFactionId) {
        // Check permission first
        if (!PermissionManager.get().hasPermission(actorUuid, Permissions.ENEMY)) {
            return RelationResult.NO_PERMISSION;
        }

        Faction actorFaction = factionManager.getPlayerFaction(actorUuid);
        if (actorFaction == null) {
            return RelationResult.NOT_IN_FACTION;
        }

        var member = actorFaction.getMember(actorUuid);
        if (member == null || !member.isOfficerOrHigher()) {
            return RelationResult.NOT_OFFICER;
        }

        if (actorFaction.id().equals(targetFactionId)) {
            return RelationResult.CANNOT_RELATE_SELF;
        }

        Faction targetFaction = factionManager.getFaction(targetFactionId);
        if (targetFaction == null) {
            return RelationResult.FACTION_NOT_FOUND;
        }

        if (actorFaction.isEnemy(targetFactionId)) {
            return RelationResult.ALREADY_ENEMY;
        }

        // Check enemy cap
        int maxEnemies = ConfigManager.get().getMaxEnemies();
        if (maxEnemies >= 0) {
            long enemyCount = actorFaction.relations().values().stream()
                .filter(r -> r.type() == RelationType.ENEMY)
                .count();
            if (enemyCount >= maxEnemies) {
                return RelationResult.ENEMY_LIMIT_REACHED;
            }
        }

        // Remove any pending ally requests
        Map<UUID, UUID> pending = pendingAllyRequests.get(actorFaction.id());
        if (pending != null) {
            pending.remove(targetFactionId);
        }
        pending = pendingAllyRequests.get(targetFactionId);
        if (pending != null) {
            pending.remove(actorFaction.id());
        }

        // Check if breaking an alliance before overwriting
        boolean wasAlly = actorFaction.isAlly(targetFactionId);

        setRelation(actorFaction.id(), targetFactionId, RelationType.ENEMY, actorUuid);

        Logger.debugRelation("Enemy declared: faction=%s, target=%s, actor=%s",
            actorFaction.name(), targetFaction.name(), actorUuid);
        Logger.info("Faction '%s' declared '%s' as enemy", actorFaction.name(), targetFaction.name());

        if (wasAlly && onAllianceBroken != null) {
            try { onAllianceBroken.accept(actorFaction.name(), targetFaction.name()); } catch (Exception e) { Logger.warn("Error in alliance broken callback: %s", e.getMessage()); }
        }

        if (onWarDeclared != null) {
            try { onWarDeclared.accept(actorFaction.name(), targetFaction.name()); } catch (Exception e) { Logger.warn("Error in war declared callback: %s", e.getMessage()); }
        }

        return RelationResult.SUCCESS;
    }

    /**
     * Sets another faction as neutral.
     *
     * @param actorUuid       the actor's UUID
     * @param targetFactionId the target faction ID
     * @return the result
     */
    public RelationResult setNeutral(@NotNull UUID actorUuid, @NotNull UUID targetFactionId) {
        // Check permission first
        if (!PermissionManager.get().hasPermission(actorUuid, Permissions.NEUTRAL)) {
            return RelationResult.NO_PERMISSION;
        }

        Faction actorFaction = factionManager.getPlayerFaction(actorUuid);
        if (actorFaction == null) {
            return RelationResult.NOT_IN_FACTION;
        }

        var member = actorFaction.getMember(actorUuid);
        if (member == null || !member.isOfficerOrHigher()) {
            return RelationResult.NOT_OFFICER;
        }

        if (actorFaction.id().equals(targetFactionId)) {
            return RelationResult.CANNOT_RELATE_SELF;
        }

        Faction targetFaction = factionManager.getFaction(targetFactionId);
        if (targetFaction == null) {
            return RelationResult.FACTION_NOT_FOUND;
        }

        RelationType currentRelation = actorFaction.getRelationType(targetFactionId);
        if (currentRelation == RelationType.NEUTRAL) {
            return RelationResult.ALREADY_NEUTRAL;
        }

        // If breaking alliance, update both sides
        boolean wasAlly = currentRelation == RelationType.ALLY;
        if (wasAlly) {
            setRelation(targetFactionId, actorFaction.id(), RelationType.NEUTRAL, null);
        }

        setRelation(actorFaction.id(), targetFactionId, RelationType.NEUTRAL, actorUuid);

        Logger.info("Faction '%s' set '%s' as neutral", actorFaction.name(), targetFaction.name());

        if (wasAlly && onAllianceBroken != null) {
            try { onAllianceBroken.accept(actorFaction.name(), targetFaction.name()); } catch (Exception e) { Logger.warn("Error in alliance broken callback: %s", e.getMessage()); }
        }

        return RelationResult.SUCCESS;
    }

    /**
     * Internal method to set a relation.
     */
    private void setRelation(@NotNull UUID factionId, @NotNull UUID targetId,
                             @NotNull RelationType type, @Nullable UUID actorUuid) {
        Faction faction = factionManager.getFaction(factionId);
        if (faction == null) return;

        Faction target = factionManager.getFaction(targetId);
        String targetName = target != null ? target.name() : "Unknown";

        FactionRelation relation = FactionRelation.create(targetId, type);
        FactionLog.LogType logType = switch (type) {
            case OWN -> FactionLog.LogType.RELATION_ALLY; // OWN shouldn't be used in setRelation, but handle for completeness
            case ALLY -> FactionLog.LogType.RELATION_ALLY;
            case ENEMY -> FactionLog.LogType.RELATION_ENEMY;
            case NEUTRAL -> FactionLog.LogType.RELATION_NEUTRAL;
        };

        Faction updated = faction.withRelation(relation)
            .withLog(FactionLog.create(logType, "Set " + targetName + " as " + type.getDisplayName(), actorUuid));

        factionManager.updateFaction(updated);

        if (onRelationChanged != null) {
            try { onRelationChanged.accept(factionId, targetId); } catch (Exception e) { Logger.warn("Error in relation changed callback: %s", e.getMessage()); }
        }
        if (onRelationChangedForMapFilter != null) {
            try { onRelationChangedForMapFilter.accept(factionId, targetId); } catch (Exception e) { Logger.warn("Error in map filter relation callback: %s", e.getMessage()); }
        }
    }

    // === Admin Methods ===

    /**
     * Admin: Force sets a mutual relation between two factions.
     * This bypasses the request/accept flow for allies.
     *
     * @param factionAId the first faction ID
     * @param factionBId the second faction ID
     * @param type       the relation type (ALLY, ENEMY, or NEUTRAL)
     * @return SUCCESS if the relation was set, FACTION_NOT_FOUND if either faction doesn't exist
     */
    @NotNull
    public RelationResult adminSetRelation(@NotNull UUID factionAId, @NotNull UUID factionBId,
                                           @NotNull RelationType type) {
        Faction factionA = factionManager.getFaction(factionAId);
        Faction factionB = factionManager.getFaction(factionBId);

        if (factionA == null || factionB == null) {
            return RelationResult.FACTION_NOT_FOUND;
        }

        // Clear any pending requests between these factions
        Map<UUID, UUID> requestsToA = pendingAllyRequests.get(factionAId);
        if (requestsToA != null) {
            requestsToA.remove(factionBId);
        }
        Map<UUID, UUID> requestsToB = pendingAllyRequests.get(factionBId);
        if (requestsToB != null) {
            requestsToB.remove(factionAId);
        }

        // Set mutual relations (null actorUuid for admin action)
        setRelation(factionAId, factionBId, type, null);
        setRelation(factionBId, factionAId, type, null);

        Logger.info("[Admin] Set mutual relation %s between '%s' and '%s'",
                type.name(), factionA.name(), factionB.name());

        return RelationResult.SUCCESS;
    }

    /**
     * Clears all relations for a faction (used when disbanding).
     *
     * @param factionId the faction ID
     */
    public void clearAllRelations(@NotNull UUID factionId) {
        // Remove pending requests
        pendingAllyRequests.remove(factionId);
        for (Map<UUID, UUID> requests : pendingAllyRequests.values()) {
            requests.remove(factionId);
        }

        // Remove relations from other factions pointing to this one
        for (Faction faction : factionManager.getAllFactions()) {
            if (faction.getRelation(factionId) != null) {
                FactionRelation neutral = FactionRelation.create(factionId, RelationType.NEUTRAL);
                Faction updated = faction.withRelation(neutral);
                factionManager.updateFaction(updated);
            }
        }
    }
}
