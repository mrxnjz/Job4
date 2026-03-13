package com.enhancedjobs.api.events;

import com.enhancedjobs.api.Job;
import com.enhancedjobs.api.Quest;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a player completes a quest. */
public class QuestCompleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Job job;
    private final Quest quest;
    private final int xpAwarded;

    public QuestCompleteEvent(Player player, Job job, Quest quest, int xpAwarded) {
        this.player = player;
        this.job = job;
        this.quest = quest;
        this.xpAwarded = xpAwarded;
    }

    public Player getPlayer()  { return player; }
    public Job getJob()        { return job; }
    public Quest getQuest()    { return quest; }
    public int getXpAwarded()  { return xpAwarded; }

    @Override public HandlerList getHandlers()  { return HANDLERS; }
    public static HandlerList getHandlerList()  { return HANDLERS; }
}
