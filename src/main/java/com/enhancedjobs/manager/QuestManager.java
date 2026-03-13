package com.enhancedjobs.manager;

import com.enhancedjobs.EnhancedJobsPlugin;
import com.enhancedjobs.api.Job;
import com.enhancedjobs.api.Quest;
import com.enhancedjobs.api.QuestObjective;
import com.enhancedjobs.api.events.QuestCompleteEvent;
import com.enhancedjobs.data.ActiveQuestData;
import com.enhancedjobs.data.PlayerData;
import com.enhancedjobs.util.MessageUtil;
import com.enhancedjobs.util.XpUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Level;

/**
 * Manages quest assignment, progress tracking, and completion for all players.
 */
public class QuestManager {

    private final EnhancedJobsPlugin plugin;
    private final Random random = new Random();

    public QuestManager(EnhancedJobsPlugin plugin) {
        this.plugin = plugin;
    }

    // ─── Acquiring Quests ──────────────────────────────────────────────────────

    /**
     * Attempt to give a player a new quest for the given job.
     * Handles both free daily quests and paid quests automatically.
     *
     * @return true if a quest was successfully assigned
     */
    public boolean acquireQuest(Player player, String jobId) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        data.checkAndReset();

        if (!data.hasJob(jobId)) {
            MessageUtil.sendError(player, "You are not enrolled in that job.");
            return false;
        }

        if (data.hasActiveQuest(jobId)) {
            MessageUtil.sendError(player, "You already have an active quest for this job! Complete it first.");
            return false;
        }

        Optional<Job> jobOpt = plugin.getJobManager().getJob(jobId);
        if (jobOpt.isEmpty()) return false;

        Job job = jobOpt.get();
        List<Quest> available = job.getQuests();

        if (available.isEmpty()) {
            MessageUtil.sendError(player, "No quests are currently available for " + job.getName() + ".");
            return false;
        }

        int acquiredToday = data.getQuestsAcquiredToday(jobId);
        int freeQuests = plugin.getConfig().getInt("quests.free-daily-quests", 1);

        // Check if free quest available
        if (acquiredToday < freeQuests) {
            return assignRandomQuest(player, job, data, available);
        }

        // Paid quest — calculate cost
        int paidSoFar = acquiredToday - freeQuests;
        double baseCost = plugin.getConfig().getDouble("economy.base-quest-cost", 30.0);
        double multiplier = plugin.getConfig().getDouble("economy.cost-multiplier", 2.0);
        double cost = XpUtil.getQuestCost(paidSoFar + 1, baseCost, multiplier);

        if (!plugin.getConfig().getBoolean("economy.enabled", true)) {
            return assignRandomQuest(player, job, data, available);
        }

        Economy eco = plugin.getEconomy();
        if (eco == null) {
            MessageUtil.sendError(player, "Economy is not available. Contact an admin.");
            return false;
        }

        if (!eco.has(player, cost)) {
            MessageUtil.sendError(player, "You need &e" + MessageUtil.formatCurrency(cost)
                    + " gold&c to purchase a quest. You have &e"
                    + MessageUtil.formatCurrency(eco.getBalance(player)) + " gold&c.");
            return false;
        }

        eco.withdrawPlayer(player, cost);
        MessageUtil.send(player, "&7Paid &e" + MessageUtil.formatCurrency(cost) + " gold&7 for a quest.");
        return assignRandomQuest(player, job, data, available);
    }

    private boolean assignRandomQuest(Player player, Job job, PlayerData data, List<Quest> available) {
        // Exclude quest types already done today if possible
        Quest selected = available.get(random.nextInt(available.size()));

        ActiveQuestData questData = new ActiveQuestData(selected.getId(), job.getId());
        data.setActiveQuest(job.getId(), questData);
        data.incrementQuestsAcquired(job.getId());

        selected.onAccept(player);
        plugin.getDataManager().save(player.getUniqueId());

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        MessageUtil.sendSuccess(player, "Quest accepted: &e" + selected.getName());
        MessageUtil.send(player, "&7" + selected.getDescription());
        for (QuestObjective obj : selected.getObjectives()) {
            MessageUtil.sendRaw(player, MessageUtil.color("  &8▸ &7" + obj.getProgressLabel(0)));
        }
        String reset = data.getFormattedTimeUntilReset();
        MessageUtil.send(player, "&8Quest limit resets in: &7" + reset);

        return true;
    }

    // ─── Progress Tracking ─────────────────────────────────────────────────────

    /**
     * Add progress to a specific objective of a player's active quest.
     * Automatically completes the quest if all objectives are met.
     *
     * @param player      The player
     * @param jobId       The job the quest belongs to
     * @param objectiveId The objective being progressed
     * @param amount      How much progress to add
     */
    public void addProgress(Player player, String jobId, String objectiveId, int amount) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (!data.hasActiveQuest(jobId)) return;

        ActiveQuestData aqd = data.getActiveQuest(jobId);
        Optional<Job> jobOpt = plugin.getJobManager().getJob(jobId);
        if (jobOpt.isEmpty()) return;

        Job job = jobOpt.get();
        Quest quest = findQuest(job, aqd.getQuestId());
        if (quest == null) return;

        // Find the objective
        QuestObjective objective = quest.getObjectives().stream()
                .filter(o -> o.getId().equals(objectiveId))
                .findFirst().orElse(null);
        if (objective == null) return;

        // Don't over-progress
        int currentProgress = aqd.getProgress(objectiveId);
        if (currentProgress >= objective.getRequired()) return;

        int newProgress = Math.min(currentProgress + amount, objective.getRequired());
        aqd.setProgress(objectiveId, newProgress);

        // Notify action bar
        if (plugin.getConfig().getBoolean("notifications.xp-action-bar", true)) {
            MessageUtil.actionBar(player, "&7[" + job.getName() + "] &e"
                    + objective.getProgressLabel(newProgress));
        }

        // Check if all objectives are complete
        boolean allComplete = quest.getObjectives().stream()
                .allMatch(o -> aqd.isObjectiveComplete(o.getId(), o.getRequired()));

        if (allComplete) {
            completeQuest(player, job, quest, data);
        } else {
            plugin.getDataManager().save(player.getUniqueId());
        }
    }

    // ─── Completion ────────────────────────────────────────────────────────────

    private void completeQuest(Player player, Job job, Quest quest, PlayerData data) {
        data.removeActiveQuest(job.getId());

        int xpReward = quest.getXpReward();
        plugin.getJobManager().addXp(player, job.getId(), xpReward);

        QuestCompleteEvent event = new QuestCompleteEvent(player, job, quest, xpReward);
        Bukkit.getPluginManager().callEvent(event);

        quest.onComplete(player);

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        MessageUtil.sendSuccess(player, "&#FFD700✦ Quest Complete: &e" + quest.getName()
                + " &7(+" + xpReward + " XP)");
        plugin.getDataManager().save(player.getUniqueId());
    }

    // ─── Abandon ───────────────────────────────────────────────────────────────

    /**
     * Abandon the active quest for a job. No XP is awarded.
     */
    public boolean abandonQuest(Player player, String jobId) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (!data.hasActiveQuest(jobId)) {
            MessageUtil.sendError(player, "You don't have an active quest for that job.");
            return false;
        }

        ActiveQuestData aqd = data.getActiveQuest(jobId);
        Optional<Job> jobOpt = plugin.getJobManager().getJob(jobId);
        jobOpt.ifPresent(job -> {
            Quest quest = findQuest(job, aqd.getQuestId());
            if (quest != null) quest.onAbandon(player);
        });

        data.removeActiveQuest(jobId);
        plugin.getDataManager().save(player.getUniqueId());
        MessageUtil.send(player, "&7Quest abandoned.");
        return true;
    }

    // ─── Info helpers ──────────────────────────────────────────────────────────

    /**
     * Get the cost of the next quest for a player, or 0 if it's free.
     */
    public double getNextQuestCost(Player player, String jobId) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        data.checkAndReset();
        int acquiredToday = data.getQuestsAcquiredToday(jobId);
        int freeQuests = plugin.getConfig().getInt("quests.free-daily-quests", 1);
        if (acquiredToday < freeQuests) return 0;
        int paidSoFar = acquiredToday - freeQuests;
        double baseCost = plugin.getConfig().getDouble("economy.base-quest-cost", 30.0);
        double multiplier = plugin.getConfig().getDouble("economy.cost-multiplier", 2.0);
        return XpUtil.getQuestCost(paidSoFar + 1, baseCost, multiplier);
    }

    // ─── Utility ───────────────────────────────────────────────────────────────

    public Quest findQuest(Job job, String questId) {
        return job.getQuests().stream()
                .filter(q -> q.getId().equals(questId))
                .findFirst().orElse(null);
    }
}
