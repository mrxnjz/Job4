package com.enhancedjobs.data;

import java.util.*;

/**
 * All job-system data for a single player.
 * This is the root object that gets saved/loaded per player.
 */
public class PlayerData {

    private final UUID playerId;
    /** Map of jobId → job state for every job the player has joined. */
    private final Map<String, PlayerJobState> jobStates;
    /** Currently active quests, keyed by jobId. */
    private final Map<String, ActiveQuestData> activeQuests;
    /** Timestamp (ms) of the last daily quest reset for this player. */
    private long lastQuestResetMs;
    /**
     * Number of quests acquired today per job (free + purchased).
     * Resets every 24h. Key = jobId.
     */
    private final Map<String, Integer> questsAcquiredToday;

    public PlayerData(UUID playerId) {
        this.playerId = playerId;
        this.jobStates = new HashMap<>();
        this.activeQuests = new HashMap<>();
        this.lastQuestResetMs = System.currentTimeMillis();
        this.questsAcquiredToday = new HashMap<>();
    }

    // ─── Jobs ──────────────────────────────────────────────────────────────────

    public boolean hasJob(String jobId) {
        return jobStates.containsKey(jobId);
    }

    public void joinJob(String jobId) {
        jobStates.putIfAbsent(jobId, new PlayerJobState(jobId));
    }

    public void leaveJob(String jobId) {
        jobStates.remove(jobId);
        activeQuests.remove(jobId);
        questsAcquiredToday.remove(jobId);
    }

    public PlayerJobState getJobState(String jobId) {
        return jobStates.get(jobId);
    }

    public Map<String, PlayerJobState> getAllJobStates() {
        return Collections.unmodifiableMap(jobStates);
    }

    public Set<String> getJobIds() {
        return Collections.unmodifiableSet(jobStates.keySet());
    }

    // ─── Quests ────────────────────────────────────────────────────────────────

    public boolean hasActiveQuest(String jobId) {
        return activeQuests.containsKey(jobId);
    }

    public ActiveQuestData getActiveQuest(String jobId) {
        return activeQuests.get(jobId);
    }

    public void setActiveQuest(String jobId, ActiveQuestData quest) {
        activeQuests.put(jobId, quest);
    }

    public void removeActiveQuest(String jobId) {
        activeQuests.remove(jobId);
    }

    public Map<String, ActiveQuestData> getAllActiveQuests() {
        return Collections.unmodifiableMap(activeQuests);
    }

    // ─── Daily Limits ──────────────────────────────────────────────────────────

    public int getQuestsAcquiredToday(String jobId) {
        return questsAcquiredToday.getOrDefault(jobId, 0);
    }

    public void incrementQuestsAcquired(String jobId) {
        questsAcquiredToday.merge(jobId, 1, Integer::sum);
    }

    public long getLastQuestResetMs() { return lastQuestResetMs; }

    /**
     * Check if 24 hours have passed since the last reset, and reset if so.
     * @return true if a reset was performed
     */
    public boolean checkAndReset() {
        long now = System.currentTimeMillis();
        if (now - lastQuestResetMs >= 24 * 60 * 60 * 1000L) {
            questsAcquiredToday.clear();
            lastQuestResetMs = now;
            return true;
        }
        return false;
    }

    /** Milliseconds until the next daily reset. */
    public long getMsUntilReset() {
        long elapsed = System.currentTimeMillis() - lastQuestResetMs;
        long remaining = 24 * 60 * 60 * 1000L - elapsed;
        return Math.max(0, remaining);
    }

    public String getFormattedTimeUntilReset() {
        long ms = getMsUntilReset();
        long hours   = ms / 3_600_000;
        long minutes = (ms % 3_600_000) / 60_000;
        long seconds = (ms % 60_000) / 1_000;
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }

    public UUID getPlayerId() { return playerId; }

    // ─── Raw access for DataManager ────────────────────────────────────────────

    public Map<String, PlayerJobState> getRawJobStates()       { return jobStates; }
    public Map<String, ActiveQuestData> getRawActiveQuests()   { return activeQuests; }
    public Map<String, Integer> getRawQuestsAcquiredToday()    { return questsAcquiredToday; }
    public void setLastQuestResetMs(long ms)                   { this.lastQuestResetMs = ms; }
}
