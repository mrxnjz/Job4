package com.enhancedjobs.listener;

import com.enhancedjobs.EnhancedJobsPlugin;
import com.enhancedjobs.api.Job;
import com.enhancedjobs.api.JobPerk;
import com.enhancedjobs.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Handles all click events for EnhancedJobSystem GUIs.
 * Uses only legacy Bukkit APIs — no Adventure/Paper imports required.
 */
public class GuiListener implements Listener {

    private static final String TITLE_JOB_MENU   = "Job Menu";
    private static final String TITLE_PERK_SUFFIX = "Perks";
    private static final String TITLE_QUEST_MENU  = "Quest Menu";
    private static final String TITLE_JOB_QUEST   = "Quests";

    private final EnhancedJobsPlugin plugin;

    public GuiListener(EnhancedJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String rawTitle = stripColor(getInventoryTitle(event.getInventory()));

        boolean isPerkMenu  = rawTitle.contains(TITLE_PERK_SUFFIX) && !rawTitle.contains(TITLE_JOB_MENU);
        boolean isJobMenu   = rawTitle.contains(TITLE_JOB_MENU) && !isPerkMenu;
        boolean isQuestMain = rawTitle.equals(TITLE_QUEST_MENU);
        boolean isJobQuest  = rawTitle.contains(TITLE_JOB_QUEST) && !isQuestMain;

        if (isJobMenu) {
            event.setCancelled(true);
            handleJobMenuClick(player, event.getSlot(), event.isLeftClick(), event.isRightClick());
        } else if (isPerkMenu) {
            event.setCancelled(true);
            handlePerkMenuClick(player, event.getSlot(), rawTitle);
        } else if (isQuestMain) {
            event.setCancelled(true);
            handleQuestMainClick(player, event.getSlot(), event.isLeftClick(), event.isRightClick());
        } else if (isJobQuest) {
            event.setCancelled(true);
            handleJobQuestClick(player, event.getSlot(), event.isLeftClick(), rawTitle);
        }
    }

    // ─── Job Menu ──────────────────────────────────────────────────────────────

    private void handleJobMenuClick(Player player, int slot, boolean left, boolean right) {
        if (slot < 0) return;
        playClick(player);

        ItemStack item = player.getOpenInventory().getTopInventory().getItem(slot);
        if (item == null || isUnclickable(item)) return;

        String itemName = getItemName(item);
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        for (Job job : plugin.getJobManager().getAllJobs()) {
            if (itemName.contains(stripColor(job.getName()))) {
                if (right && data.hasJob(job.getId())) {
                    player.closeInventory();
                    plugin.getJobManager().leaveJob(player, job.getId());
                } else if (left && data.hasJob(job.getId())) {
                    player.openInventory(plugin.getJobMenuGui().buildPerkGui(player, job));
                } else if (left) {
                    player.closeInventory();
                    plugin.getJobManager().joinJob(player, job.getId());
                }
                return;
            }
        }
    }

    // ─── Perk Menu ─────────────────────────────────────────────────────────────

    private void handlePerkMenuClick(Player player, int slot, String rawTitle) {
        if (slot < 0) return;
        playClick(player);

        ItemStack item = player.getOpenInventory().getTopInventory().getItem(slot);
        if (item == null || item.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        if (item.getType() == Material.ARROW) {
            player.openInventory(plugin.getJobMenuGui().buildJobListGui(player));
            return;
        }

        Job job = getJobFromTitle(rawTitle, TITLE_PERK_SUFFIX);
        if (job == null) return;

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (!data.hasJob(job.getId())) return;

        if (slot == 4) {
            plugin.getJobManager().toggleAllPerks(player, job.getId());
            player.openInventory(plugin.getJobMenuGui().buildPerkGui(player, job));
            return;
        }

        String itemName = getItemName(item);
        for (JobPerk perk : job.getPerks()) {
            if (itemName.contains(stripColor(perk.getName()))) {
                plugin.getJobManager().togglePerk(player, job.getId(), perk.getId());
                player.openInventory(plugin.getJobMenuGui().buildPerkGui(player, job));
                return;
            }
        }
    }

    // ─── Quest Main Menu ───────────────────────────────────────────────────────

    private void handleQuestMainClick(Player player, int slot, boolean left, boolean right) {
        if (slot < 0) return;
        playClick(player);

        ItemStack item = player.getOpenInventory().getTopInventory().getItem(slot);
        if (item == null || isUnclickable(item)) return;
        if (item.getType() == Material.BARRIER) return;

        String itemName = getItemName(item);
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        for (Job job : plugin.getJobManager().getAllJobs()) {
            if (!data.hasJob(job.getId())) continue;
            if (itemName.contains(stripColor(job.getName()))) {
                if (right && data.hasActiveQuest(job.getId())) {
                    plugin.getQuestManager().abandonQuest(player, job.getId());
                    player.openInventory(plugin.getQuestMenuGui().buildMainQuestGui(player));
                } else if (left) {
                    player.openInventory(plugin.getQuestMenuGui().buildJobQuestGui(player, job));
                }
                return;
            }
        }
    }

    // ─── Job Quest GUI ─────────────────────────────────────────────────────────

    private void handleJobQuestClick(Player player, int slot, boolean left, String rawTitle) {
        if (slot < 0) return;
        playClick(player);

        ItemStack item = player.getOpenInventory().getTopInventory().getItem(slot);
        if (item == null || item.getType() == Material.GRAY_STAINED_GLASS_PANE) return;
        if (item.getType() == Material.EXPERIENCE_BOTTLE) return;

        if (item.getType() == Material.ARROW) {
            player.openInventory(plugin.getQuestMenuGui().buildMainQuestGui(player));
            return;
        }

        Job job = getJobFromTitle(rawTitle, TITLE_JOB_QUEST);
        if (job == null) return;

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        if (item.getType() == Material.BARRIER && slot == 31) {
            plugin.getQuestManager().abandonQuest(player, job.getId());
            player.openInventory(plugin.getQuestMenuGui().buildJobQuestGui(player, job));
            return;
        }

        if (item.getType() == Material.WRITABLE_BOOK && slot == 13 && !data.hasActiveQuest(job.getId())) {
            player.closeInventory();
            plugin.getQuestManager().acquireQuest(player, job.getId());
        }
    }

    // ─── Utilities ─────────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private String getInventoryTitle(Inventory inv) {
        return inv.getTitle();
    }

    @SuppressWarnings("deprecation")
    private String getItemName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return "";
        return stripColor(meta.getDisplayName());
    }

    private boolean isUnclickable(ItemStack item) {
        return item.getType() == Material.GRAY_STAINED_GLASS_PANE
                || item.getType() == Material.BOOK;
    }

    private Job getJobFromTitle(String rawTitle, String suffix) {
        String cleaned = rawTitle.replace(suffix, "").replaceAll("[^a-zA-Z0-9 ]", "").trim();
        for (Job job : plugin.getJobManager().getAllJobs()) {
            String jobName = stripColor(job.getName()).replaceAll("[^a-zA-Z0-9 ]", "").trim();
            if (cleaned.equalsIgnoreCase(jobName) || cleaned.contains(jobName)) return job;
        }
        return null;
    }

    private void playClick(Player player) {
        String soundName = plugin.getConfig().getString("gui.click-sound", "UI_BUTTON_CLICK");
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), 0.5f, 1f);
        } catch (Exception ignored) {}
    }

    private static String stripColor(String s) {
        String stripped = ChatColor.stripColor(s);
        return stripped != null ? stripped.trim() : s.trim();
    }
}
