package com.enhancedjobs.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks a player's current progress on an active quest.
 * Objective progress is stored as a map of objectiveId → currentProgress.
 */
public class ActiveQuestData {

    private final String questId;
    private final String jobId;
    private final long acceptedAtMs;
    /** Maps objectiveId → current progress count. */
    private final Map<String, Integer> objectiveProgress;

    public ActiveQuestData(String questId, String jobId) {
        this.questId = questId;
        this.jobId = jobId;
        this.acceptedAtMs = System.currentTimeMillis();
        this.objectiveProgress = new HashMap<>();
    }

    public ActiveQuestData(String questId, String jobId, long acceptedAtMs,
                           Map<String, Integer> objectiveProgress) {
        this.questId = questId;
        this.jobId = jobId;
        this.acceptedAtMs = acceptedAtMs;
        this.objectiveProgress = new HashMap<>(objectiveProgress);
    }

    public String getQuestId()  { return questId; }
    public String getJobId()    { return jobId; }
    public long getAcceptedAt() { return acceptedAtMs; }

    public int getProgress(String objectiveId) {
        return objectiveProgress.getOrDefault(objectiveId, 0);
    }

    public void setProgress(String objectiveId, int progress) {
        objectiveProgress.put(objectiveId, progress);
    }

    public void addProgress(String objectiveId, int amount) {
        objectiveProgress.merge(objectiveId, amount, Integer::sum);
    }

    public Map<String, Integer> getAllProgress() {
        return Map.copyOf(objectiveProgress);
    }

    public boolean isObjectiveComplete(String objectiveId, int required) {
        return getProgress(objectiveId) >= required;
    }
}
