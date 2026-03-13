package com.enhancedjobs.gui;

import com.enhancedjobs.EnhancedJobsPlugin;
import com.enhancedjobs.api.Job;
import com.enhancedjobs.api.JobPerk;
import com.enhancedjobs.data.PlayerData;
import com.enhancedjobs.data.PlayerJobState;
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
 * The main /job GUI — lists all jobs, shows player progress, allows joining/leaving
 * and navigating into job-specific perk management.
 */
public class JobMenuGui {

    private static final String TITLE = "&8⚒ &6Job Menu";
    private final EnhancedJobsPlugin plugin;

    public JobMenuGui(EnhancedJobsPlugin plugin) {
        this.plugin = plugin;
    }

    // ─── Job List GUI ──────────────────────────────────────────────────────────

    public Inventory buildJobListGui(Player player) {
        Collection<Job> jobs = plugin.getJobManager().getAllJobs();
        int rows = Math.max(3, Math.min(6, (int) Math.ceil((jobs.size() + 2) / 7.0) + 2));
        Inventory inv = GuiUtil.createInventory(TITLE, rows);
        GuiUtil.fillBorder(inv);

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        int slot = 10;
        int col = 0;
        for (Job job : jobs) {
            if (col == 7) { slot += 2; col = 0; }
            inv.setItem(slot, buildJobItem(job, data));
            slot++;
            col++;
        }

        // Info item
        inv.setItem(4, GuiUtil.makeItem(Material.BOOK, "&e&lJob System",
                "&7Join a job to earn XP through quests.",
                "&7Complete quests to level up (max: &eLv.10&7).",
                "&7Even levels grant special rewards!",
                "",
                "&8Left-click a job to join / view details",
                "&8Right-click to leave a job"));

        return inv;
    }

    private ItemStack buildJobItem(Job job, PlayerData data) {
        boolean enrolled = data.hasJob(job.getId());
        PlayerJobState state = enrolled ? data.getJobState(job.getId()) : null;

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(MessageUtil.color("&7" + job.getDescription()));

        for (String extra : job.getExtraDescription()) {
            lore.add(MessageUtil.color("&8" + extra));
        }

        lore.add("");
        if (enrolled && state != null) {
            int level = state.getLevel();
            int xpIn  = state.getXpInCurrentLevel();
            int xpReq = state.getXpRequiredForCurrentLevel();
            String bar = GuiUtil.progressBar(state.getLevelProgress(), 15);
            lore.add(MessageUtil.color("&7Status: &aEnrolled"));
            lore.add(MessageUtil.color("&7Level: &e" + level + " &8/ &e" + XpUtil.MAX_LEVEL));
            lore.add(MessageUtil.color("&7XP:    &e" + xpIn + " &8/ &e" + (xpReq > 0 ? xpReq : "MAX")));
            lore.add(MessageUtil.color("        " + bar));
            lore.add("");
            lore.add(MessageUtil.color("&aLeft-click &7» Manage perks"));
            lore.add(MessageUtil.color("&cRight-click &7» Leave job"));
        } else {
            lore.add(MessageUtil.color("&7Status: &cNot enrolled"));
            lore.add("");
            lore.add(MessageUtil.color("&aLeft-click &7» Join this job"));
        }

        Material icon = job.getIcon() != null ? job.getIcon() : Material.PAPER;
        String nameColor = enrolled ? "&a" : "&7";
        String namePrefix = enrolled ? "✦ " : "  ";
        return GuiUtil.makeItem(icon, nameColor + namePrefix + job.getName(), lore);
    }

    // ─── Perk Management GUI ───────────────────────────────────────────────────

    public Inventory buildPerkGui(Player player, Job job) {
        Inventory inv = GuiUtil.createInventory("&8⚙ &6" + job.getName() + " &8— Perks", 6);
        GuiUtil.fillBorder(inv);

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        PlayerJobState state = data.getJobState(job.getId());

        if (state == null) return inv;

        int playerLevel = state.getLevel();

        // Global toggle at top-centre
        boolean allActive = state.isPerksActive();
        inv.setItem(4, GuiUtil.makeItem(
                allActive ? Material.LIME_DYE : Material.GRAY_DYE,
                allActive ? "&aAll Perks: &2ON" : "&7All Perks: &8OFF",
                "&7Click to toggle all perks on/off",
                "",
                "&8This affects all " + job.getName() + " perks at once"));

        // XP info bar at slot 49 (bottom-centre)
        int xpIn  = state.getXpInCurrentLevel();
        int xpReq = state.getXpRequiredForCurrentLevel();
        String bar = GuiUtil.progressBar(state.getLevelProgress(), 14);
        inv.setItem(49, GuiUtil.makeItem(Material.EXPERIENCE_BOTTLE,
                "&eLv." + playerLevel + " " + job.getName(),
                "&7XP: &e" + xpIn + " &8/ &e" + (xpReq > 0 ? xpReq : "MAX"),
                "      " + bar,
                "",
                "&8Max Level: " + XpUtil.MAX_LEVEL));

        // Back button
        inv.setItem(45, GuiUtil.makeItem(Material.ARROW, "&cBack &8» &7Job Menu"));

        // Perks (slots 10–43 inner grid)
        List<JobPerk> perks = job.getPerks();
        int[] innerSlots = GuiUtil.innerSlots(6);
        for (int i = 0; i < perks.size() && i < innerSlots.length; i++) {
            JobPerk perk = perks.get(i);
            inv.setItem(innerSlots[i], buildPerkItem(perk, state, playerLevel));
        }

        return inv;
    }

    private ItemStack buildPerkItem(JobPerk perk, PlayerJobState state, int playerLevel) {
        boolean unlocked = perk.getUnlockLevel() <= playerLevel;
        boolean enabled  = state.isPerkEnabled(perk.getId()) && state.isPerksActive();

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(MessageUtil.color("&7" + perk.getDescription()));
        lore.add("");
        lore.add(MessageUtil.color("&7Unlocks at: &eLv." + perk.getUnlockLevel()));
        lore.add("");

        Material mat;
        String status;
        if (!unlocked) {
            mat = Material.BARRIER;
            status = "&8Locked &7(Reach Lv." + perk.getUnlockLevel() + ")";
            lore.add(MessageUtil.color(status));
        } else if (enabled) {
            mat = Material.LIME_STAINED_GLASS_PANE;
            status = "&aEnabled";
            lore.add(MessageUtil.color(status));
            lore.add(MessageUtil.color("&7Click to &cdisable"));
        } else {
            mat = Material.RED_STAINED_GLASS_PANE;
            status = "&cDisabled";
            lore.add(MessageUtil.color(status));
            lore.add(MessageUtil.color("&7Click to &aenable"));
        }

        return GuiUtil.makeItem(mat, (unlocked ? "&e" : "&8") + perk.getName(), lore);
    }
}
