package com.enhancedjobs.data;

import com.enhancedjobs.util.XpUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * Stores a player's state within a single job: level, XP, active perks.
 */
public class PlayerJobState {

    private final String jobId;
    private int totalXp;
    private boolean perksActive;
    /** IDs of perks the player has enabled for this job. */
    private final Set<String> enabledPerkIds;

    public PlayerJobState(String jobId) {
        this.jobId = jobId;
        this.totalXp = 0;
        this.perksActive = true;
        this.enabledPerkIds = new HashSet<>();
    }

    public PlayerJobState(String jobId, int totalXp, boolean perksActive, Set<String> enabledPerkIds) {
        this.jobId = jobId;
        this.totalXp = totalXp;
        this.perksActive = perksActive;
        this.enabledPerkIds = new HashSet<>(enabledPerkIds);
    }

    public String getJobId()    { return jobId; }
    public int getTotalXp()     { return totalXp; }
    public boolean isPerksActive() { return perksActive; }
    public Set<String> getEnabledPerkIds() { return enabledPerkIds; }

    public int getLevel() {
        return XpUtil.getLevelFromTotalXp(totalXp);
    }

    public int getXpInCurrentLevel() {
        return XpUtil.getXpInCurrentLevel(totalXp);
    }

    public int getXpRequiredForCurrentLevel() {
        return XpUtil.getXpRequired(getLevel());
    }

    /**
     * Add XP and return the new level (may have increased).
     */
    public int addXp(int amount) {
        int oldLevel = getLevel();
        totalXp += amount;
        return getLevel();
    }

    public void setTotalXp(int xp) { this.totalXp = Math.max(0, xp); }
    public void setPerksActive(boolean active) { this.perksActive = active; }

    public void enablePerk(String perkId)  { enabledPerkIds.add(perkId); }
    public void disablePerk(String perkId) { enabledPerkIds.remove(perkId); }
    public boolean isPerkEnabled(String perkId) { return enabledPerkIds.contains(perkId); }

    public boolean isMaxLevel() {
        return getLevel() >= XpUtil.MAX_LEVEL;
    }

    /** Percentage progress through current level (0.0–1.0). */
    public double getLevelProgress() {
        int req = getXpRequiredForCurrentLevel();
        if (req <= 0) return 1.0;
        return (double) getXpInCurrentLevel() / req;
    }
}
