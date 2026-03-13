package com.enhancedjobs.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class MessageUtil {

    public static final String PREFIX = ChatColor.GOLD + "[" + ChatColor.YELLOW + "Jobs" + ChatColor.GOLD + "] " + ChatColor.RESET;

    private MessageUtil() {}

    public static String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public static void send(CommandSender sender, String msg) {
        sender.sendMessage(PREFIX + color(msg));
    }

    public static void sendRaw(CommandSender sender, String msg) {
        sender.sendMessage(color(msg));
    }

    public static void sendError(CommandSender sender, String msg) {
        sender.sendMessage(PREFIX + ChatColor.RED + color(msg));
    }

    public static void sendSuccess(CommandSender sender, String msg) {
        sender.sendMessage(PREFIX + ChatColor.GREEN + color(msg));
    }

    public static void sendInfo(CommandSender sender, String msg) {
        sender.sendMessage(PREFIX + ChatColor.AQUA + color(msg));
    }

    /**
     * Sends an action bar message.
     * Uses the legacy deprecated method which is still present in Spigot 1.20.6
     * and requires no extra dependencies.
     */
    @SuppressWarnings("deprecation")
    public static void actionBar(Player player, String msg) {
        player.sendActionBar(color(msg));
    }

    public static List<String> colorList(List<String> lines) {
        return lines.stream().map(MessageUtil::color).toList();
    }

    public static String formatCurrency(double amount) {
        if (amount == Math.floor(amount)) return String.valueOf((long) amount);
        return String.format("%.1f", amount);
    }
}
