package com.enhancedjobs.api.events;

import com.enhancedjobs.api.Job;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a player leaves a job. Cancellable. */
public class JobLeaveEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final Player player;
    private final Job job;

    public JobLeaveEvent(Player player, Job job) {
        this.player = player;
        this.job = job;
    }

    public Player getPlayer() { return player; }
    public Job getJob()       { return job; }

    @Override public boolean isCancelled()        { return cancelled; }
    @Override public void setCancelled(boolean c) { this.cancelled = c; }
    @Override public HandlerList getHandlers()    { return HANDLERS; }
    public static HandlerList getHandlerList()    { return HANDLERS; }
}
