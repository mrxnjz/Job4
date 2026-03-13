package com.enhancedjobs.api.events;

import com.enhancedjobs.api.Job;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a player levels up in a job. */
public class JobLevelUpEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final Player player;
    private final Job job;
    private final int oldLevel;
    private final int newLevel;

    public JobLevelUpEvent(Player player, Job job, int oldLevel, int newLevel) {
        this.player = player;
        this.job = job;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public Player getPlayer()  { return player; }
    public Job getJob()        { return job; }
    public int getOldLevel()   { return oldLevel; }
    public int getNewLevel()   { return newLevel; }

    @Override public boolean isCancelled()        { return cancelled; }
    @Override public void setCancelled(boolean c) { this.cancelled = c; }
    @Override public HandlerList getHandlers()    { return HANDLERS; }
    public static HandlerList getHandlerList()    { return HANDLERS; }
}
