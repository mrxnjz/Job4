package com.enhancedjobs.manager;

import com.enhancedjobs.EnhancedJobsPlugin;
import com.enhancedjobs.api.Job;
import com.enhancedjobs.api.JobPerk;
import com.enhancedjobs.api.LevelReward;
import com.enhancedjobs.api.events.JobJoinEvent;
import com.enhancedjobs.api.events.JobLeaveEvent;
import com.enhancedjobs.api.events.JobLevelUpEvent;
import com.enhancedjobs.data.PlayerData;
import com.enhancedjobs.data.PlayerJobState;
import com.enhancedjobs.util.MessageUtil;
import com.enhancedjobs.util.XpUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Level;

/**
 * Central registry and controller for all Job operations.
 * Jobs are registered here and can be retrieved by any other system.
 */
public class JobManager {

    private final EnhancedJobsPlugin plugin;
    private final Map<String, Job> registeredJobs = new LinkedHashMap<>();

    public JobManager(EnhancedJobsPlugin plugin) {
        this.plugin = plugin;
    }

    // ─── Registration ──────────────────────────────────────────────────────────

    /**
     * Register a job. Call this from your plugin's onEnable() to add custom jobs.
     */
    public void registerJob(Job job) {
        if (registeredJobs.containsKey(job.getId())) {
            plugin.getLogger().warning("Job '" + job.getId() + "' is already registered — skipping.");
            return;
        }
        registeredJobs.put(job.getId(), job);
        plugin.getLogger().info("Registered job: " + job.getName() + " [" + job.getId() + "]");
    }

    public void unregisterJob(String jobId) {
        registeredJobs.remove(jobId);
    }

    public Optional<Job> getJob(String jobId) {
        return Optional.ofNullable(registeredJobs.get(jobId));
    }

    public Collection<Job> getAllJobs() {
        return Collections.unmodifiableCollection(registeredJobs.values());
    }

    public boolean isRegistered(String jobId) {
        return registeredJobs.containsKey(jobId);
    }

    // ─── Join / Leave ──────────────────────────────────────────────────────────

    /**
     * Attempt to have a player join a job.
     * @return true if successful
     */
    public boolean joinJob(Player player, String jobId) {
        Job job = registeredJobs.get(jobId);
        if (job == null) {
            MessageUtil.sendError(player, "Unknown job: " + jobId);
            return false;
        }

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        if (data.hasJob(jobId)) {
            MessageUtil.sendError(player, "You are already working as a &e" + job.getName() + "&c!");
            return false;
        }

        JobJoinEvent event = new JobJoinEvent(player, job);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        data.joinJob(jobId);
        job.onPlayerJoin(player);

        // Activate any perks the player might have (new job = level 1, no perks yet)
        applyActivePerks(player, job, data.getJobState(jobId));

        playSound(player, Sound.ENTITY_PLAYER_LEVELUP);
        MessageUtil.sendSuccess(player, "You are now a &e" + job.getName() + "&a! Use &e/jq&a to view quests.");
        plugin.getDataManager().save(player.getUniqueId());
        return true;
    }

    /**
     * Have a player leave a job.
     * @return true if successful
     */
    public boolean leaveJob(Player player, String jobId) {
        Job job = registeredJobs.get(jobId);
        if (job == null) return false;

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (!data.hasJob(jobId)) {
            MessageUtil.sendError(player, "You are not working as a " + job.getName() + ".");
            return false;
        }

        JobLeaveEvent event = new JobLeaveEvent(player, job);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        // Deactivate perks
        PlayerJobState state = data.getJobState(jobId);
        deactivateAllPerks(player, job, state);

        // Revoke level rewards
        revokeRewards(player, job, state.getLevel());

        data.leaveJob(jobId);
        job.onPlayerLeave(player);

        MessageUtil.send(player, "You have left the &e" + job.getName() + "&r job.");
        plugin.getDataManager().save(player.getUniqueId());
        return true;
    }

    // ─── XP & Leveling ─────────────────────────────────────────────────────────

    /**
     * Award XP to a player in a job and handle level-up logic.
     */
    public void addXp(Player player, String jobId, int amount) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (!data.hasJob(jobId)) return;

        PlayerJobState state = data.getJobState(jobId);
        if (state.isMaxLevel()) return;

        Job job = registeredJobs.get(jobId);
        if (job == null) return;

        int oldLevel = state.getLevel();
        int newLevel = state.addXp(amount);

        // Notify via action bar
        if (plugin.getConfig().getBoolean("notifications.xp-action-bar", true)) {
            int xpInLevel = state.getXpInCurrentLevel();
            int xpRequired = state.getXpRequiredForCurrentLevel();
            String bar = XpUtil.buildProgressBar(xpInLevel, xpRequired, 20, "█", "░", "§a", "§7");
            MessageUtil.actionBar(player, "&7[" + job.getName() + "] &e+" + amount + " XP &r" + bar
                    + " &e" + xpInLevel + "&7/&e" + xpRequired);
        }

        // Handle level-up(s) — could level up multiple times from a big XP grant
        if (newLevel > oldLevel) {
            handleLevelUp(player, job, state, oldLevel, newLevel);
        }
    }

    private void handleLevelUp(Player player, Job job, PlayerJobState state, int oldLevel, int newLevel) {
        newLevel = Math.min(newLevel, XpUtil.MAX_LEVEL);

        JobLevelUpEvent event = new JobLevelUpEvent(player, job, oldLevel, newLevel);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            // Roll back XP to the start of oldLevel
            state.setTotalXp(XpUtil.getTotalXpToReachLevel(oldLevel));
            return;
        }

        playSound(player, getSoundFromConfig("notifications.levelup-sound", Sound.UI_TOAST_CHALLENGE_COMPLETE));

        boolean broadcast = plugin.getConfig().getBoolean("notifications.level-up-broadcast", false);
        String msg = "&6✦ &eLevel Up! &7You are now &eLv." + newLevel + " " + job.getName() + " &7✦";
        MessageUtil.send(player, msg);
        if (broadcast) {
            Bukkit.broadcastMessage(MessageUtil.color(MessageUtil.PREFIX + "&e" + player.getName()
                    + " &7reached &eLv." + newLevel + " " + job.getName() + "!"));
        }

        // Grant rewards at even levels
        for (int lvl = oldLevel + 1; lvl <= newLevel; lvl++) {
            if (XpUtil.isRewardLevel(lvl)) {
                LevelReward reward = job.getLevelRewards().get(lvl);
                if (reward != null) {
                    try {
                        reward.grant(player, lvl);
                        MessageUtil.send(player, "&6★ Reward unlocked at Lv." + lvl
                                + ": &e" + reward.getName());
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Error granting reward at level " + lvl, e);
                    }
                }
            }
        }

        // Activate newly unlocked perks
        applyActivePerks(player, job, state);

        job.onLevelUp(player, oldLevel, newLevel);
    }

    // ─── Perks ─────────────────────────────────────────────────────────────────

    /**
     * Apply all perks the player has unlocked and enabled for this job.
     */
    public void applyActivePerks(Player player, Job job, PlayerJobState state) {
        if (!state.isPerksActive()) return;
        int playerLevel = state.getLevel();
        for (JobPerk perk : job.getPerks()) {
            if (perk.getUnlockLevel() <= playerLevel && state.isPerkEnabled(perk.getId())) {
                try {
                    perk.onActivate(player);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error activating perk " + perk.getId(), e);
                }
            }
        }
    }

    /**
     * Deactivate all perks for the given job.
     */
    public void deactivateAllPerks(Player player, Job job, PlayerJobState state) {
        for (JobPerk perk : job.getPerks()) {
            try {
                perk.onDeactivate(player);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error deactivating perk " + perk.getId(), e);
            }
        }
    }

    /**
     * Toggle a specific perk on or off for a player.
     */
    public void togglePerk(Player player, String jobId, String perkId) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        PlayerJobState state = data.getJobState(jobId);
        if (state == null) return;

        Job job = registeredJobs.get(jobId);
        if (job == null) return;

        JobPerk perk = job.getPerks().stream()
                .filter(p -> p.getId().equals(perkId))
                .findFirst().orElse(null);
        if (perk == null) return;

        if (perk.getUnlockLevel() > state.getLevel()) {
            MessageUtil.sendError(player, "You need to reach level " + perk.getUnlockLevel() + " to use this perk.");
            return;
        }

        if (state.isPerkEnabled(perkId)) {
            state.disablePerk(perkId);
            perk.onDeactivate(player);
            MessageUtil.send(player, "&7Perk &e" + perk.getName() + "&7 disabled.");
        } else {
            state.enablePerk(perkId);
            if (state.isPerksActive()) perk.onActivate(player);
            MessageUtil.send(player, "&aEnabled perk &e" + perk.getName() + "&a.");
        }
        plugin.getDataManager().save(player.getUniqueId());
    }

    /**
     * Toggle all perks for a job on/off at once.
     */
    public void toggleAllPerks(Player player, String jobId) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        PlayerJobState state = data.getJobState(jobId);
        if (state == null) return;

        Job job = registeredJobs.get(jobId);
        if (job == null) return;

        boolean newState = !state.isPerksActive();
        state.setPerksActive(newState);

        if (newState) {
            applyActivePerks(player, job, state);
            MessageUtil.sendSuccess(player, "All " + job.getName() + " perks &aenabled&a.");
        } else {
            deactivateAllPerks(player, job, state);
            MessageUtil.send(player, "All " + job.getName() + " perks &7disabled&r.");
        }
        plugin.getDataManager().save(player.getUniqueId());
    }

    // ─── Logout / Login ────────────────────────────────────────────────────────

    /**
     * Called on player login to restore perks.
     */
    public void onPlayerLogin(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        data.checkAndReset();
        for (String jobId : data.getJobIds()) {
            Job job = registeredJobs.get(jobId);
            if (job == null) continue;
            PlayerJobState state = data.getJobState(jobId);
            applyActivePerks(player, job, state);
        }
    }

    /**
     * Called on player logout to clean up perks.
     */
    public void onPlayerLogout(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        for (String jobId : data.getJobIds()) {
            Job job = registeredJobs.get(jobId);
            if (job == null) continue;
            PlayerJobState state = data.getJobState(jobId);
            deactivateAllPerks(player, job, state);
        }
    }

    // ─── Rewards revocation ────────────────────────────────────────────────────

    private void revokeRewards(Player player, Job job, int currentLevel) {
        for (int lvl = 2; lvl <= currentLevel; lvl += 2) {
            LevelReward reward = job.getLevelRewards().get(lvl);
            if (reward != null) {
                try {
                    reward.revoke(player, lvl);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error revoking reward at level " + lvl, e);
                }
            }
        }
    }

    // ─── Admin helpers ─────────────────────────────────────────────────────────

    /**
     * Reset a player's data for a specific job.
     */
    public void resetPlayerJob(Player target, String jobId) {
        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        if (!data.hasJob(jobId)) return;

        Job job = registeredJobs.get(jobId);
        if (job != null) {
            PlayerJobState state = data.getJobState(jobId);
            deactivateAllPerks(target, job, state);
            revokeRewards(target, job, state.getLevel());
        }

        data.leaveJob(jobId);
        plugin.getDataManager().save(target.getUniqueId());
    }

    // ─── Utility ───────────────────────────────────────────────────────────────

    private void playSound(Player player, Sound sound) {
        player.playSound(player.getLocation(), sound, 1f, 1f);
    }

    private Sound getSoundFromConfig(String path, Sound fallback) {
        String name = plugin.getConfig().getString(path, "");
        try {
            return Sound.valueOf(name);
        } catch (Exception e) {
            return fallback;
        }
    }
}
