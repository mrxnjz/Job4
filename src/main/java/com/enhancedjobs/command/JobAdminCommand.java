package com.enhancedjobs.command;

import com.enhancedjobs.EnhancedJobsPlugin;
import com.enhancedjobs.api.Job;
import com.enhancedjobs.data.PlayerData;
import com.enhancedjobs.data.PlayerJobState;
import com.enhancedjobs.util.MessageUtil;
import com.enhancedjobs.util.XpUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * /jobadmin — administrative commands.
 *
 * Subcommands:
 *   reload                            — reloads config
 *   reset <player> [job]              — resets player job data
 *   setlevel <player> <job> <level>   — sets a player's level in a job
 *   addxp <player> <job> <amount>     — adds XP to a player's job
 *   info <player>                     — shows all job data for a player
 *   givequest <player> <job>          — gives a player a new quest (free)
 */
public class JobAdminCommand implements CommandExecutor, TabCompleter {

    private final EnhancedJobsPlugin plugin;

    public JobAdminCommand(EnhancedJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("enhancedjobs.admin")) {
            MessageUtil.sendError(sender, "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload"     -> handleReload(sender);
            case "reset"      -> handleReset(sender, args);
            case "setlevel"   -> handleSetLevel(sender, args);
            case "addxp"      -> handleAddXp(sender, args);
            case "info"       -> handleInfo(sender, args);
            case "givequest"  -> handleGiveQuest(sender, args);
            default           -> sendUsage(sender);
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        MessageUtil.sendSuccess(sender, "EnhancedJobSystem config reloaded.");
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) { MessageUtil.sendError(sender, "Usage: /jobadmin reset <player> [jobId]"); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { MessageUtil.sendError(sender, "Player not found (must be online)."); return; }

        if (args.length >= 3) {
            plugin.getJobManager().resetPlayerJob(target, args[2]);
            MessageUtil.sendSuccess(sender, "Reset " + target.getName() + "'s job: " + args[2]);
        } else {
            // Reset all jobs
            PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
            for (String jobId : new ArrayList<>(data.getJobIds())) {
                plugin.getJobManager().resetPlayerJob(target, jobId);
            }
            MessageUtil.sendSuccess(sender, "Reset all jobs for " + target.getName() + ".");
        }
    }

    private void handleSetLevel(CommandSender sender, String[] args) {
        if (args.length < 4) { MessageUtil.sendError(sender, "Usage: /jobadmin setlevel <player> <jobId> <level>"); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { MessageUtil.sendError(sender, "Player not found (must be online)."); return; }

        int level;
        try { level = Integer.parseInt(args[3]); } catch (NumberFormatException e) {
            MessageUtil.sendError(sender, "Invalid level number."); return;
        }

        level = Math.max(1, Math.min(XpUtil.MAX_LEVEL, level));
        String jobId = args[2];

        if (!plugin.getJobManager().isRegistered(jobId)) {
            MessageUtil.sendError(sender, "Unknown job: " + jobId); return;
        }

        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        if (!data.hasJob(jobId)) {
            data.joinJob(jobId);
        }

        int totalXp = XpUtil.getTotalXpToReachLevel(level);
        data.getJobState(jobId).setTotalXp(totalXp);
        plugin.getDataManager().save(target.getUniqueId());
        MessageUtil.sendSuccess(sender, "Set " + target.getName() + "'s " + jobId + " level to " + level + ".");
    }

    private void handleAddXp(CommandSender sender, String[] args) {
        if (args.length < 4) { MessageUtil.sendError(sender, "Usage: /jobadmin addxp <player> <jobId> <amount>"); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { MessageUtil.sendError(sender, "Player not found (must be online)."); return; }

        int amount;
        try { amount = Integer.parseInt(args[3]); } catch (NumberFormatException e) {
            MessageUtil.sendError(sender, "Invalid XP amount."); return;
        }

        String jobId = args[2];
        plugin.getJobManager().addXp(target, jobId, amount);
        MessageUtil.sendSuccess(sender, "Added " + amount + " XP to " + target.getName() + "'s " + jobId + " job.");
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) { MessageUtil.sendError(sender, "Usage: /jobadmin info <player>"); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { MessageUtil.sendError(sender, "Player not found (must be online)."); return; }

        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        MessageUtil.sendRaw(sender, MessageUtil.color("&6=== Job Info: " + target.getName() + " ==="));
        MessageUtil.sendRaw(sender, MessageUtil.color("&7Reset in: &e" + data.getFormattedTimeUntilReset()));

        if (data.getJobIds().isEmpty()) {
            MessageUtil.sendRaw(sender, MessageUtil.color("&7No jobs enrolled."));
            return;
        }

        for (String jobId : data.getJobIds()) {
            PlayerJobState state = data.getJobState(jobId);
            int acquiredToday = data.getQuestsAcquiredToday(jobId);
            boolean hasActive = data.hasActiveQuest(jobId);
            MessageUtil.sendRaw(sender, MessageUtil.color(
                    "&e" + jobId + "&7: Lv." + state.getLevel()
                    + " &8(" + state.getTotalXp() + " total XP)"
                    + " | Today's quests: &e" + acquiredToday
                    + " &7| Active quest: " + (hasActive ? "&aYes" : "&cNo")));
        }
    }

    private void handleGiveQuest(CommandSender sender, String[] args) {
        if (args.length < 3) { MessageUtil.sendError(sender, "Usage: /jobadmin givequest <player> <jobId>"); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { MessageUtil.sendError(sender, "Player not found (must be online)."); return; }

        // Override economy for this call by temporarily incrementing free quests
        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        // Give directly by resetting today's count for this job to 0 so the acquire is free
        data.getRawQuestsAcquiredToday().put(args[2], 0);
        plugin.getQuestManager().acquireQuest(target, args[2]);
        MessageUtil.sendSuccess(sender, "Gave quest to " + target.getName() + " for job " + args[2] + ".");
    }

    private void sendUsage(CommandSender sender) {
        MessageUtil.sendRaw(sender, MessageUtil.color("&6=== JobAdmin Commands ==="));
        MessageUtil.sendRaw(sender, MessageUtil.color("&e/jobadmin reload &7— Reload config"));
        MessageUtil.sendRaw(sender, MessageUtil.color("&e/jobadmin reset <player> [jobId] &7— Reset job data"));
        MessageUtil.sendRaw(sender, MessageUtil.color("&e/jobadmin setlevel <player> <jobId> <level> &7— Set level"));
        MessageUtil.sendRaw(sender, MessageUtil.color("&e/jobadmin addxp <player> <jobId> <amount> &7— Add XP"));
        MessageUtil.sendRaw(sender, MessageUtil.color("&e/jobadmin info <player> &7— View job data"));
        MessageUtil.sendRaw(sender, MessageUtil.color("&e/jobadmin givequest <player> <jobId> &7— Give a free quest"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("enhancedjobs.admin")) return List.of();

        if (args.length == 1) {
            return Arrays.asList("reload", "reset", "setlevel", "addxp", "info", "givequest");
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("setlevel")
                || args[0].equalsIgnoreCase("addxp")
                || args[0].equalsIgnoreCase("reset")
                || args[0].equalsIgnoreCase("givequest"))) {
            return plugin.getJobManager().getAllJobs().stream().map(j -> j.getId()).toList();
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("setlevel")) {
            return List.of("1","2","3","4","5","6","7","8","9","10");
        }
        return List.of();
    }
}
