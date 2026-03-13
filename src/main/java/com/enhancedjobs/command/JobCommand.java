package com.enhancedjobs.command;

import com.enhancedjobs.EnhancedJobsPlugin;
import com.enhancedjobs.util.MessageUtil;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /job — opens the Job Management GUI.
 * Aliases: /jobs
 */
public class JobCommand implements CommandExecutor, TabCompleter {

    private final EnhancedJobsPlugin plugin;

    public JobCommand(EnhancedJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("enhancedjobs.use")) {
            MessageUtil.sendError(player, "You don't have permission to use the job system.");
            return true;
        }

        openJobGui(player);
        return true;
    }

    private void openJobGui(Player player) {
        String soundName = plugin.getConfig().getString("gui.open-sound", "BLOCK_CHEST_OPEN");
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), 0.7f, 1f);
        } catch (Exception ignored) {}
        player.openInventory(plugin.getJobMenuGui().buildJobListGui(player));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return List.of();
    }
}
