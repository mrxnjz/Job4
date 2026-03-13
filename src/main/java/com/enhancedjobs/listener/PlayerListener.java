package com.enhancedjobs.listener;

import com.enhancedjobs.EnhancedJobsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final EnhancedJobsPlugin plugin;

    public PlayerListener(EnhancedJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getJobManager().onPlayerLogin(event.getPlayer());
        }, 20L); // slight delay to ensure player is fully loaded
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getJobManager().onPlayerLogout(event.getPlayer());
        plugin.getDataManager().unload(event.getPlayer().getUniqueId());
    }
}
