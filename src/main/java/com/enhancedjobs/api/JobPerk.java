package com.enhancedjobs.api;

import org.bukkit.entity.Player;

/**
 * A passive perk tied to a job that can be toggled on or off independently.
 * Perks are unlocked as players level up and can be individually enabled/disabled
 * via the /job GUI, allowing players to mix-and-match perks across multiple jobs.
 */
public abstract class JobPerk {

    private final String id;
    private final String name;
    private final String description;
    private final int unlockLevel;

    protected JobPerk(String id, String name, String description, int unlockLevel) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.unlockLevel = unlockLevel;
    }

    public String getId()          { return id; }
    public String getName()        { return name; }
    public String getDescription() { return description; }
    public int getUnlockLevel()    { return unlockLevel; }

    /**
     * Called every time the perk is activated (player turns it on or logs in with it active).
     */
    public abstract void onActivate(Player player);

    /**
     * Called every time the perk is deactivated (player turns it off or logs out).
     */
    public abstract void onDeactivate(Player player);

    /**
     * Whether this perk is passive (always on when enabled) or requires event handling.
     * Override to return false if you handle effects via Bukkit events.
     */
    public boolean isPassive() { return true; }

    @Override
    public String toString() {
        return "JobPerk{id='" + id + "', level=" + unlockLevel + "}";
    }
}
