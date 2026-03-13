package com.enhancedjobs.api;

import org.bukkit.entity.Player;

/**
 * Represents a reward granted to a player upon reaching an even level.
 * Implement this in your Job subclass to define what players receive at levels 2, 4, 6, 8, 10.
 */
public interface LevelReward {

    /**
     * A short display name shown in the GUI (e.g. "Double XP Boost").
     */
    String getName();

    /**
     * A description of the reward shown in the GUI.
     */
    String getDescription();

    /**
     * Called once when the player first reaches this reward level.
     * Use this to give items, grant permissions, etc.
     *
     * @param player The player who reached the level
     * @param level  The level that was reached
     */
    void grant(Player player, int level);

    /**
     * Called when the reward needs to be revoked (e.g. player leaves the job).
     * May be a no-op for one-time rewards like item grants.
     */
    default void revoke(Player player, int level) {}
}
