package com.hyperfactions.lifecycle;

import com.hyperfactions.api.events.FactionDisbandEvent;
import com.hyperfactions.api.events.FactionMemberEvent;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.data.MembershipRecord;
import com.hyperfactions.data.PlayerData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.storage.PlayerStorage;
import com.hyperfactions.util.Logger;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Handles membership history recording for faction member and disband events.
 * Also provides migration logic for upgrading existing members to the history system.
 */
public class MembershipHistoryHandler {

  private final PlayerStorage playerStorage;

  private final FactionManager factionManager;

  /** Creates a new MembershipHistoryHandler. */
  public MembershipHistoryHandler(@NotNull PlayerStorage playerStorage, @NotNull FactionManager factionManager) {
    this.playerStorage = playerStorage;
    this.factionManager = factionManager;
  }

  /**
   * Handles membership history recording for member events.
   */
  public void handleMembershipHistory(@NotNull FactionMemberEvent event) {
    Faction faction = event.faction();
    UUID playerUuid = event.playerUuid();
    int maxHistory = ConfigManager.get().getMaxMembershipHistory();

    playerStorage.loadPlayerData(playerUuid).thenAccept(opt -> {
      PlayerData data = opt.orElseGet(() -> new PlayerData(playerUuid));
      switch (event.type()) {
        case JOIN -> {
          FactionMember member = faction.getMember(playerUuid);
          FactionRole role = member != null ? member.role() : FactionRole.MEMBER;
          MembershipRecord rec = MembershipRecord.createActive(
              faction.id(), faction.name(), faction.tag(), role);
          data.addRecord(rec, maxHistory);
          Logger.debug("Membership history: %s joined %s", playerUuid, faction.name());
        }
        case LEAVE -> {
          data.closeActiveRecord(MembershipRecord.LeaveReason.LEFT);
          Logger.debug("Membership history: %s left %s", playerUuid, faction.name());
        }
        case KICK -> {
          data.closeActiveRecord(MembershipRecord.LeaveReason.KICKED);
          Logger.debug("Membership history: %s kicked from %s", playerUuid, faction.name());
        }
        case PROMOTE -> {
          FactionMember member = faction.getMember(playerUuid);
          if (member != null) {
            data.updateHighestRole(member.role());
            Logger.debug("Membership history: %s promoted in %s to %s",
                playerUuid, faction.name(), ConfigManager.get().getRoleDisplayName(member.role()));
          }
        }
        case DEMOTE -> {
          // Demote doesn't affect highestRole (it tracks the highest achieved)
        }
        default -> throw new IllegalStateException("Unexpected value");
      }
      playerStorage.savePlayerData(data);
    }).exceptionally(e -> {
      Logger.severe("Failed to record membership history for %s", e, playerUuid);
      return null;
    });
  }

  /**
   * Handles membership history recording for faction disband events.
   * Closes active records for ALL members with DISBANDED reason.
   */
  public void handleDisbandHistory(@NotNull FactionDisbandEvent event) {
    Faction faction = event.faction();

    for (UUID memberUuid : faction.members().keySet()) {
      playerStorage.loadPlayerData(memberUuid).thenAccept(opt -> {
        PlayerData data = opt.orElseGet(() -> new PlayerData(memberUuid));
        data.closeActiveRecord(MembershipRecord.LeaveReason.DISBANDED);
        playerStorage.savePlayerData(data);
        Logger.debug("Membership history: %s's faction %s disbanded", memberUuid, faction.name());
      }).exceptionally(e -> {
        Logger.severe("Failed to record disband history for %s", e, memberUuid);
        return null;
      });
    }
  }

  /**
   * Migrates existing faction members on upgrade -- creates active membership
   * records for players who don't have one yet. Only runs once per player
   * (subsequent startups skip players who already have an active record).
   */
  public void migrateMembershipHistory() {
    int maxHistory = ConfigManager.get().getMaxMembershipHistory();
    int migrated = 0;

    for (Faction faction : factionManager.getAllFactions()) {
      for (FactionMember member : faction.getMembersSorted()) {
        try {
          PlayerData data = playerStorage.loadPlayerData(member.uuid()).join()
            .orElseGet(() -> new PlayerData(member.uuid()));

          // Skip if they already have an active record
          if (data.getActiveRecord() != null) {
            continue;
          }

          // Create active record using their actual join date and current role
          MembershipRecord record = new MembershipRecord(
            faction.id(), faction.name(), faction.tag(),
            member.role(), member.joinedAt(), 0,
            MembershipRecord.LeaveReason.ACTIVE
          );
          data.addRecord(record, maxHistory);

          // Cache username and initialize firstJoined if not set
          if (data.getUsername() == null) {
            data.setUsername(member.username());
          }
          if (data.getFirstJoined() == 0) {
            // Best proxy: their faction join date
            data.setFirstJoined(member.joinedAt());
          }
          if (data.getLastOnline() == 0 && member.lastOnline() > 0) {
            data.setLastOnline(member.lastOnline());
          }

          playerStorage.savePlayerData(data).join();
          migrated++;
        } catch (Exception e) {
          Logger.severe("Failed to migrate membership history for %s", e, member.username());
        }
      }
    }

    if (migrated > 0) {
      Logger.info("[Migration] Migrated membership history for %d existing faction members", migrated);
    }
  }
}
