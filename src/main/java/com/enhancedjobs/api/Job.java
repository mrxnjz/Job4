package com.enhancedjobs.api;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

/**
 * Represents a job/profession in the Enhanced Job System.
 *
 * To add a new job:
 * 1. Create a class that extends Job
 * 2. Implement all abstract methods
 * 3. Register via EnhancedJobsPlugin.getInstance().getJobManager().registerJob(new YourJob())
 *
 * Jobs can be registered from external plugins via the API.
 * Registering should be done during your plugin's onEnable().
 */
public abstract class Job {

    private final String id;
    private final String name;
    private final String description;

    protected Job(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    /** Unique identifier (lowercase, no spaces). e.g. "miner", "farmer" */
    public String getId()          { return id; }

    /** Display name shown in GUIs. e.g. "⛏ Miner" */
    public String getName()        { return name; }

    /** Short description shown in the job GUI. */
    public String getDescription() { return description; }

    /**
     * The icon displayed in the job selection GUI.
     * Pick a Material that represents this job visually.
     */
    public abstract Material getIcon();

    /**
     * All quests available for this job.
     * The QuestManager randomly selects from this pool when assigning quests.
     * Return an empty list if you haven't defined quests yet.
     */
    public abstract List<Quest> getQuests();

    /**
     * All perks associated with this job, keyed by the level at which they unlock.
     * Example: level 2 → double drop perk.
     * Perks at even levels (2,4,6,8,10) are special reward perks.
     */
    public abstract List<JobPerk> getPerks();

    /**
     * Rewards given when the player reaches even levels (2, 4, 6, 8, 10).
     * Map key is the level (must be 2, 4, 6, 8, or 10).
     */
    public abstract Map<Integer, LevelReward> getLevelRewards();

    /**
     * Additional lore lines shown in the job selection GUI.
     * Override to add flavour text, instructions, etc.
     */
    public List<String> getExtraDescription() { return List.of(); }

    /**
     * The maximum number of active quests a player can hold for this job at once.
     * Default is 1. Override to allow more.
     */
    public int getMaxActiveQuests() { return 1; }

    /**
     * Called when a player joins this job for the first time.
     */
    public void onPlayerJoin(org.bukkit.entity.Player player) {}

    /**
     * Called when a player leaves this job.
     */
    public void onPlayerLeave(org.bukkit.entity.Player player) {}

    /**
     * Called when a player levels up in this job.
     *
     * @param player   The player
     * @param oldLevel Previous level
     * @param newLevel New level reached
     */
    public void onLevelUp(org.bukkit.entity.Player player, int oldLevel, int newLevel) {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Job job)) return false;
        return id.equals(job.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() { return "Job{id='" + id + "', name='" + name + "'}"; }
}
