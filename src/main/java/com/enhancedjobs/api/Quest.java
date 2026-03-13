package com.enhancedjobs.api;

import org.bukkit.Material;

import java.util.List;

/**
 * Represents a quest that belongs to a Job.
 * Extend this class and register instances via your Job's getQuests() method.
 *
 * Each quest grants XP_PER_QUEST (200) XP on completion.
 * A quest consists of one or more QuestObjectives that the player must complete.
 */
public abstract class Quest {

    private final String id;
    private final String name;
    private final String description;
    private final String jobId;

    protected Quest(String id, String name, String description, String jobId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.jobId = jobId;
    }

    public String getId()          { return id; }
    public String getName()        { return name; }
    public String getDescription() { return description; }
    public String getJobId()       { return jobId; }

    /**
     * The list of objectives the player must complete for this quest.
     * Objectives are evaluated in order; all must be completed.
     */
    public abstract List<QuestObjective> getObjectives();

    /**
     * The icon shown in the quest GUI.
     */
    public abstract Material getIcon();

    /**
     * Optional additional lore lines shown in the quest GUI beneath the description.
     */
    public List<String> getExtraLore() { return List.of(); }

    /**
     * XP awarded on completion. Default is 200 (one quest worth).
     */
    public int getXpReward() { return 200; }

    /**
     * Called when the player accepts this quest. Override for custom logic.
     */
    public void onAccept(org.bukkit.entity.Player player) {}

    /**
     * Called when the player completes this quest. Override for custom logic.
     */
    public void onComplete(org.bukkit.entity.Player player) {}

    /**
     * Called when the player abandons this quest. Override for custom logic.
     */
    public void onAbandon(org.bukkit.entity.Player player) {}

    @Override
    public String toString() {
        return "Quest{id='" + id + "', job='" + jobId + "'}";
    }
}
