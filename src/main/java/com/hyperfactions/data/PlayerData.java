package com.hyperfactions.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Expanded player data wrapper that combines power fields with membership
 * history, kill/death stats, and cached username.
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

    public PlayerData() {}

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
            if (!removed) break; // All records are active (shouldn't happen)
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
            if (rec.isActive()) return rec;
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

    public void incrementKills() { kills++; }
    public void incrementDeaths() { deaths++; }

    // === Getters/Setters ===

    @NotNull public UUID getUuid() { return uuid; }
    public void setUuid(@NotNull UUID uuid) { this.uuid = uuid; }

    @Nullable public String getUsername() { return username; }
    public void setUsername(@Nullable String username) { this.username = username; }

    public double getPower() { return power; }
    public void setPower(double power) { this.power = power; }

    public double getMaxPower() { return maxPower; }
    public void setMaxPower(double maxPower) { this.maxPower = maxPower; }

    public long getLastDeath() { return lastDeath; }
    public void setLastDeath(long lastDeath) { this.lastDeath = lastDeath; }

    public long getLastRegen() { return lastRegen; }
    public void setLastRegen(long lastRegen) { this.lastRegen = lastRegen; }

    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; }

    public int getDeaths() { return deaths; }
    public void setDeaths(int deaths) { this.deaths = deaths; }

    public long getFirstJoined() { return firstJoined; }
    public void setFirstJoined(long firstJoined) { this.firstJoined = firstJoined; }

    public long getLastOnline() { return lastOnline; }
    public void setLastOnline(long lastOnline) { this.lastOnline = lastOnline; }

    @NotNull
    public List<MembershipRecord> getMembershipHistory() { return membershipHistory; }
    public void setMembershipHistory(@NotNull List<MembershipRecord> membershipHistory) {
        this.membershipHistory = membershipHistory;
    }

    @Nullable public Double getMaxPowerOverride() { return maxPowerOverride; }
    public void setMaxPowerOverride(@Nullable Double maxPowerOverride) { this.maxPowerOverride = maxPowerOverride; }

    public boolean isPowerLossDisabled() { return powerLossDisabled; }
    public void setPowerLossDisabled(boolean powerLossDisabled) { this.powerLossDisabled = powerLossDisabled; }

    public boolean isClaimDecayExempt() { return claimDecayExempt; }
    public void setClaimDecayExempt(boolean claimDecayExempt) { this.claimDecayExempt = claimDecayExempt; }
}
