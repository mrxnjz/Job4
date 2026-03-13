package com.enhancedjobs.api;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;

/**
 * Represents a single trackable objective within a Quest.
 * Subclass this to create custom objective types (e.g., mine blocks, kill mobs).
 *
 * Objective progress is stored per-player by the QuestManager.
 * The objective itself is stateless — progress data lives in ActiveQuestData.
 */
public abstract class QuestObjective {

    private final String id;
    private final String description;
    private final int required;

    protected QuestObjective(String id, String description, int required) {
        this.id = id;
        this.description = description;
        this.required = required;
    }

    public String getId()          { return id; }
    public String getDescription() { return description; }
    public int getRequired()       { return required; }

    /**
     * A short display label used in progress messages (e.g. "50/100 blocks mined").
     */
    public String getProgressLabel(int current) {
        return current + "/" + required + " " + description;
    }

    /**
     * Serialize this objective's definition to a Map for YAML storage.
     * Must be paired with a static deserialize factory method in subclasses.
     */
    public abstract Map<String, Object> serialize();

    /**
     * Deserialize from a ConfigurationSection. Implement in subclasses.
     */
    public static QuestObjective deserialize(ConfigurationSection section) {
        throw new UnsupportedOperationException("Subclasses must implement deserialize()");
    }

    /**
     * The type identifier used during deserialization.
     * Must be unique across all registered objective types.
     */
    public abstract String getType();

    @Override
    public String toString() {
        return "QuestObjective{id='" + id + "', type=" + getType() + ", required=" + required + "}";
    }
}
