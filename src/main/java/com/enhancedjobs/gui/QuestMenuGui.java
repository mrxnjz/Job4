package com.enhancedjobs.gui;

import com.enhancedjobs.EnhancedJobsPlugin;
import com.enhancedjobs.api.Job;
import com.enhancedjobs.api.Quest;
import com.enhancedjobs.api.QuestObjective;
import com.enhancedjobs.data.ActiveQuestData;
import com.enhancedjobs.data.PlayerData;
import com.enhancedjobs.util.MessageUtil;
import com.enhancedjobs.util.XpUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The /jq (job quests) GUI — shows all jobs the player is enrolled in,
 * their active quests, and options to acquire/abandon quests.
 */
public class QuestMenuGui {

    private static final String TITLE_MAIN  = "&8📜 &6Quest Menu";
    private static final String TITLE_JOB   = "&8📜 &6%s Quests";
    private final EnhancedJobsPlugin plugin;

    public QuestMenuGui(EnhancedJobsPlugin plugin) {
        this.plugin = plugin;
    }

    // ─── Main Quest Selection GUI ──────────────────────────────────────────────

    /**
     * Shows all jobs the player is enrolled in; click to manage quests for that job.
     */
    public Inventory buildMainQuestGui(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        data.checkAndReset();

        Collection<Job> allJobs = plugin.getJobManager().getAllJobs();
        List<Job> myJobs = allJobs.stream()
                .filter(j -> data.hasJob(j.getId()))
                .toList();

        int rows = Math.max(3, Math.min(6, (int) Math.ceil((myJobs.size() + 2) / 7.0) + 2));
        Inventory inv = GuiUtil.createInventory(TITLE_MAIN, rows);
        GuiUtil.fillBorder(inv);

        if (myJobs.isEmpty()) {
            inv.setItem(22, GuiUtil.makeItem(Material.BARRIER,
                    "&cNo Jobs Enrolled",
                    "&7Use &e/job &7to join a job first!"));
            return inv;
        }

        inv.setItem(4, GuiUtil.makeItem(Material.BOOK,
                "&e&lYour Quests",
                "&7Select a job to manage its quests.",
                "",
                "&8Free quest: &a✔ &7Daily",
                "&8Additional quests cost &egold &8(doubles each purchase)",
                "&8Resets every 24 hours."));

        int[] innerSlots = GuiUtil.innerSlots(rows);
        for (int i = 0; i < myJobs.size() && i < innerSlots.length; i++) {
            Job job = myJobs.get(i);
            inv.setItem(innerSlots[i], buildJobQuestSummaryItem(job, player, data));
        }

        return inv;
    }

    private ItemStack buildJobQuestSummaryItem(Job job, Player player, PlayerData data) {
        boolean hasActive = data.hasActiveQuest(job.getId());
        int acquiredToday = data.getQuestsAcquiredToday(job.getId());
        int freeDaily = plugin.getConfig().getInt("quests.free-daily-quests", 1);
        boolean freeAvailable = acquiredToday < freeDaily;
        double nextCost = plugin.getQuestManager().getNextQuestCost(player, job.getId());
        var state = data.getJobState(job.getId());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(MessageUtil.color("&7Level: &eLv." + state.getLevel()
                + " &8(" + state.getXpInCurrentLevel() + "/" + state.getXpRequiredForCurrentLevel() + " XP)"));
        lore.add("");

        if (hasActive) {
            ActiveQuestData aqd = data.getActiveQuest(job.getId());
            Quest quest = plugin.getQuestManager().findQuest(job, aqd.getQuestId());
            lore.add(MessageUtil.color("&6Active Quest:"));
            if (quest != null) {
                lore.add(MessageUtil.color("  &e" + quest.getName()));
                for (QuestObjective obj : quest.getObjectives()) {
                    int prog = aqd.getProgress(obj.getId());
                    String tick = prog >= obj.getRequired() ? "&a✔ " : "&7▸ ";
                    lore.add(MessageUtil.color("  " + tick + "&7" + obj.getProgressLabel(prog)));
                }
            }
            lore.add("");
            lore.add(MessageUtil.color("&aLeft-click &7» View quest details"));
            lore.add(MessageUtil.color("&cRight-click &7» Abandon quest"));
        } else {
            lore.add(MessageUtil.color("&7No active quest."));
            lore.add("");
            if (freeAvailable) {
                lore.add(MessageUtil.color("&a✔ Free quest available!"));
                lore.add(MessageUtil.color("&aLeft-click &7» Accept free quest"));
            } else {
                lore.add(MessageUtil.color("&7Next quest cost: &e"
                        + MessageUtil.formatCurrency(nextCost) + " gold"));
                lore.add(MessageUtil.color("&aLeft-click &7» Purchase quest"));
            }
        }

        lore.add("");
        lore.add(MessageUtil.color("&8Acquired today: &7" + acquiredToday
                + " &8| Reset: &7" + data.getFormattedTimeUntilReset()));

        String statusColor = hasActive ? "&6" : (freeAvailable ? "&a" : "&7");
        Material icon = job.getIcon() != null ? job.getIcon() : Material.PAPER;
        return GuiUtil.makeItem(icon, statusColor + job.getName(), lore);
    }

    // ─── Job-Specific Quest GUI ────────────────────────────────────────────────

    /**
     * Shows the full detail view for a player's active quest in a specific job,
     * plus options to abandon or get a new quest if none is active.
     */
    public Inventory buildJobQuestGui(Player player, Job job) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        data.checkAndReset();

        String title = String.format(TITLE_JOB, job.getName());
        Inventory inv = GuiUtil.createInventory(title, 4);
        GuiUtil.fillBorder(inv);

        // Back button
        inv.setItem(27, GuiUtil.makeItem(Material.ARROW, "&cBack &8» &7Quest Menu"));

        boolean hasActive = data.hasActiveQuest(job.getId());
        int acquiredToday = data.getQuestsAcquiredToday(job.getId());
        int freeDaily = plugin.getConfig().getInt("quests.free-daily-quests", 1);
        double nextCost = plugin.getQuestManager().getNextQuestCost(player, job.getId());

        // Progress info
        var state = data.getJobState(job.getId());
        if (state != null) {
            int level   = state.getLevel();
            int xpIn    = state.getXpInCurrentLevel();
            int xpReq   = state.getXpRequiredForCurrentLevel();
            String bar  = GuiUtil.progressBar(state.getLevelProgress(), 14);
            inv.setItem(4, GuiUtil.makeItem(Material.EXPERIENCE_BOTTLE,
                    "&eLv." + level + " " + job.getName(),
                    "&7XP: &e" + xpIn + " &8/ &e" + (xpReq > 0 ? xpReq : "MAX"),
                    "      " + bar));
        }

        if (hasActive) {
            // Active quest detail
            ActiveQuestData aqd = data.getActiveQuest(job.getId());
            Quest quest = plugin.getQuestManager().findQuest(job, aqd.getQuestId());
            if (quest != null) {
                inv.setItem(13, buildActiveQuestItem(quest, aqd));
            }
            // Abandon button
            inv.setItem(31, GuiUtil.makeItem(Material.BARRIER,
                    "&c✗ Abandon Quest",
                    "&7Click to abandon your current quest.",
                    "&cNo XP will be awarded!"));
        } else {
            // Get quest button
            String costStr = acquiredToday < freeDaily
                    ? "&aFREE"
                    : "&e" + MessageUtil.formatCurrency(nextCost) + " gold";

            List<String> getLore = new ArrayList<>();
            getLore.add("");
            getLore.add(MessageUtil.color("&7A random quest will be assigned."));
            getLore.add(MessageUtil.color("&7Cost: " + costStr));
            getLore.add("");
            getLore.add(MessageUtil.color("&8Acquired today: &7" + acquiredToday));
            getLore.add(MessageUtil.color("&8Resets in: &7" + data.getFormattedTimeUntilReset()));
            getLore.add("");
            getLore.add(MessageUtil.color("&aClick to accept!"));

            inv.setItem(13, GuiUtil.makeItem(Material.WRITABLE_BOOK,
                    "&aGet New Quest", getLore));

            // No active quests filler
            inv.setItem(22, GuiUtil.makeItem(Material.GRAY_DYE,
                    "&7No Active Quest",
                    "&7Accept a quest to get started!"));
        }

        return inv;
    }

    private ItemStack buildActiveQuestItem(Quest quest, ActiveQuestData aqd) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(MessageUtil.color("&7" + quest.getDescription()));
        lore.add("");
        lore.add(MessageUtil.color("&6Objectives:"));
        for (QuestObjective obj : quest.getObjectives()) {
            int prog = aqd.getProgress(obj.getId());
            boolean done = prog >= obj.getRequired();
            String tick = done ? "&a✔ " : "&7▸ ";
            String barColor = done ? "&a" : "&e";
            lore.add(MessageUtil.color("  " + tick + barColor + obj.getProgressLabel(prog)));
        }
        lore.add("");
        lore.add(MessageUtil.color("&7Reward: &e" + quest.getXpReward() + " XP"));
        for (String extra : quest.getExtraLore()) {
            lore.add(MessageUtil.color("&8" + extra));
        }

        Material icon = quest.getIcon() != null ? quest.getIcon() : Material.MAP;
        return GuiUtil.makeItem(icon, "&6" + quest.getName(), lore);
    }
}
