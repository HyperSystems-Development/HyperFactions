package com.hyperfactions.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Expanded player data wrapper that combines power fields with membership
 * history, kill/death stats, and cached username.
 *
 * <p>
 * Stored in {@code data/players/{uuid}.json}. Backwards-compatible with
 * old files that only contain power fields — new fields default to
 * null/empty/zero.
 */
public class PlayerData {

  private UUID uuid;

  private String username;

  private double power;

  private double maxPower;

  private long lastDeath;

  private long lastRegen;

  private int kills;

  private int deaths;

  private long firstJoined;

  private long lastOnline;

  private List<MembershipRecord> membershipHistory = new ArrayList<>();

  private Double maxPowerOverride;

  private boolean powerLossDisabled;

  private boolean claimDecayExempt;

  private boolean adminBypassEnabled;

  // === Player Preferences (i18n + notifications) ===
  private String languagePreference;

  private boolean territoryAlertsEnabled = true;

  private boolean deathAnnouncementsEnabled = true;

  private boolean powerNotificationsEnabled = true;

  /** Creates a new PlayerData. */
  public PlayerData() {}

  /** Creates a new PlayerData. */
  public PlayerData(@NotNull UUID uuid) {
    this.uuid = uuid;
  }

  // === Power conversion ===

  /**
   * Creates a PlayerPower record from this data's power fields.
   */
  @NotNull
  public PlayerPower toPower() {
    return new PlayerPower(uuid, power, maxPower, lastDeath, lastRegen, maxPowerOverride, powerLossDisabled, claimDecayExempt);
  }

  /**
   * Updates power fields from a PlayerPower record.
   */
  public void updatePower(@NotNull PlayerPower p) {
    this.power = p.power();
    this.maxPower = p.maxPower();
    this.lastDeath = p.lastDeath();
    this.lastRegen = p.lastRegen();
    this.maxPowerOverride = p.maxPowerOverride();
    this.powerLossDisabled = p.powerLossDisabled();
    this.claimDecayExempt = p.claimDecayExempt();
  }

  /**
   * Creates a PlayerData from a PlayerPower (for migration from old format).
   */
  @NotNull
  public static PlayerData fromPower(@NotNull PlayerPower p) {
    PlayerData data = new PlayerData(p.uuid());
    data.updatePower(p);
    return data;
  }

  // === Membership history ===

  /**
   * Adds a membership record, capping at maxHistory (oldest-first eviction).
   * Only evicts closed records — the active record is never evicted.
   */
  public void addRecord(@NotNull MembershipRecord rec, int maxHistory) {
    membershipHistory.add(rec);
    // Evict oldest closed records if over limit
    while (membershipHistory.size() > maxHistory) {
      // Find first closed record to remove
      boolean removed = false;
      for (int i = 0; i < membershipHistory.size(); i++) {
        if (!membershipHistory.get(i).isActive()) {
          membershipHistory.remove(i);
          removed = true;
          break;
        }
      }
      if (!removed) { // All records are active (shouldn't happen)
        break;
      }
    }
  }

  /**
   * Closes the currently active membership record with the given reason.
   */
  public void closeActiveRecord(@NotNull MembershipRecord.LeaveReason reason) {
    for (int i = 0; i < membershipHistory.size(); i++) {
      if (membershipHistory.get(i).isActive()) {
        membershipHistory.set(i, membershipHistory.get(i).withClosed(reason));
        return;
      }
    }
  }

  /**
   * Gets the currently active membership record, or null if none.
   */
  @Nullable
  public MembershipRecord getActiveRecord() {
    for (MembershipRecord rec : membershipHistory) {
      if (rec.isActive()) {
        return rec;
      }
    }
    return null;
  }

  /**
   * Updates the highest role on the active record if the new role is higher.
   */
  public void updateHighestRole(@NotNull FactionRole role) {
    for (int i = 0; i < membershipHistory.size(); i++) {
      MembershipRecord rec = membershipHistory.get(i);
      if (rec.isActive()) {
        membershipHistory.set(i, rec.withHighestRole(role));
        return;
      }
    }
  }

  /**
   * Clears all membership history records.
   */
  public void clearHistory() {
    membershipHistory.clear();
  }

  // === Kill/death stats ===

  /** Increment Kills. */
  public void incrementKills() {
    kills++;
  }

  public void incrementDeaths() {
    deaths++;
  }

  // === Getters/Setters ===

  /** Returns the uuid. */
  @NotNull public UUID getUuid() {
    return uuid;
  }

  /** Sets the uuid. */
  public void setUuid(@NotNull UUID uuid) {
    this.uuid = uuid;
  }

  /** Returns the username. */
  @Nullable public String getUsername() {
    return username;
  }

  /** Sets the username. */
  public void setUsername(@Nullable String username) {
    this.username = username;
  }

  public double getPower() {
    return power;
  }

  public void setPower(double power) {
    this.power = power;
  }

  /** Returns the max power. */
  public double getMaxPower() {
    return maxPower;
  }

  /** Sets the max power. */
  public void setMaxPower(double maxPower) {
    this.maxPower = maxPower;
  }

  /** Returns the last death. */
  public long getLastDeath() {
    return lastDeath;
  }

  /** Sets the last death. */
  public void setLastDeath(long lastDeath) {
    this.lastDeath = lastDeath;
  }

  /** Returns the last regen. */
  public long getLastRegen() {
    return lastRegen;
  }

  public void setLastRegen(long lastRegen) {
    this.lastRegen = lastRegen;
  }

  public int getKills() {
    return kills;
  }

  /** Sets the kills. */
  public void setKills(int kills) {
    this.kills = kills;
  }

  /** Returns the deaths. */
  public int getDeaths() {
    return deaths;
  }

  /** Sets the deaths. */
  public void setDeaths(int deaths) {
    this.deaths = deaths;
  }

  /** Returns the first joined. */
  public long getFirstJoined() {
    return firstJoined;
  }

  /** Sets the first joined. */
  public void setFirstJoined(long firstJoined) {
    this.firstJoined = firstJoined;
  }

  public long getLastOnline() {
    return lastOnline;
  }

  /** Sets the last online. */
  public void setLastOnline(long lastOnline) {
    this.lastOnline = lastOnline;
  }

  /** Returns the membership history. */
  @NotNull
  public List<MembershipRecord> getMembershipHistory() {
    return membershipHistory;
  }

  /** Sets the membership history. */
  public void setMembershipHistory(@NotNull List<MembershipRecord> membershipHistory) {
    this.membershipHistory = membershipHistory;
  }

  /** Returns the max power override. */
  @Nullable public Double getMaxPowerOverride() {
    return maxPowerOverride;
  }

  /** Sets the max power override. */
  public void setMaxPowerOverride(@Nullable Double maxPowerOverride) {
    this.maxPowerOverride = maxPowerOverride;
  }

  /** Checks if power loss disabled. */
  public boolean isPowerLossDisabled() {
    return powerLossDisabled;
  }

  /** Sets the power loss disabled. */
  public void setPowerLossDisabled(boolean powerLossDisabled) {
    this.powerLossDisabled = powerLossDisabled;
  }

  /** Checks if claim decay exempt. */
  public boolean isClaimDecayExempt() {
    return claimDecayExempt;
  }

  /** Sets the claim decay exempt. */
  public void setClaimDecayExempt(boolean claimDecayExempt) {
    this.claimDecayExempt = claimDecayExempt;
  }

  public boolean isAdminBypassEnabled() {
    return adminBypassEnabled;
  }

  /** Sets the admin bypass enabled. */
  public void setAdminBypassEnabled(boolean adminBypassEnabled) {
    this.adminBypassEnabled = adminBypassEnabled;
  }

  // === Player Preferences ===

  /** Returns the player's preferred language, or null for auto-detect. */
  @Nullable public String getLanguagePreference() {
    return languagePreference;
  }

  /** Sets the player's preferred language (null = auto-detect from client/server). */
  public void setLanguagePreference(@Nullable String languagePreference) {
    this.languagePreference = languagePreference;
  }

  /** Whether territory entry/exit alerts are enabled for this player. */
  public boolean isTerritoryAlertsEnabled() {
    return territoryAlertsEnabled;
  }

  /** Sets territory entry/exit alerts enabled. */
  public void setTerritoryAlertsEnabled(boolean territoryAlertsEnabled) {
    this.territoryAlertsEnabled = territoryAlertsEnabled;
  }

  /** Whether faction death location broadcasts are enabled for this player. */
  public boolean isDeathAnnouncementsEnabled() {
    return deathAnnouncementsEnabled;
  }

  /** Sets faction death announcement broadcasts enabled. */
  public void setDeathAnnouncementsEnabled(boolean deathAnnouncementsEnabled) {
    this.deathAnnouncementsEnabled = deathAnnouncementsEnabled;
  }

  /** Whether power change notifications are enabled for this player. */
  public boolean isPowerNotificationsEnabled() {
    return powerNotificationsEnabled;
  }

  /** Sets power change notifications enabled. */
  public void setPowerNotificationsEnabled(boolean powerNotificationsEnabled) {
    this.powerNotificationsEnabled = powerNotificationsEnabled;
  }
}
