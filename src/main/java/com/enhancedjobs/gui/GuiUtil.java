package com.enhancedjobs.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class GuiUtil {

    private GuiUtil() {}

    /**
     * Create an inventory with a legacy-colored title (&-codes translated to §-codes).
     * Uses the deprecated String-title constructor which still works in Spigot 1.20.6.
     */
    @SuppressWarnings("deprecation")
    public static Inventory createInventory(String title, int rows) {
        String colored = ChatColor.translateAlternateColorCodes('&', title);
        return Bukkit.createInventory(null, rows * 9, colored);
    }

    /** Build an ItemStack with a legacy-colored display name and optional lore. */
    @SuppressWarnings("deprecation")
    public static ItemStack makeItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

        if (lore.length > 0) {
            List<String> coloredLore = Arrays.stream(lore)
                    .map(l -> ChatColor.translateAlternateColorCodes('&', l))
                    .collect(Collectors.toList());
            meta.setLore(coloredLore);
        }
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack makeItem(Material mat, String name, List<String> lore) {
        return makeItem(mat, name, lore.toArray(new String[0]));
    }

    public static ItemStack fillerPane() {
        return makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
    }

    public static void fillBorder(Inventory inv) {
        int size = inv.getSize();
        int cols = 9;
        int rows = size / cols;
        ItemStack pane = fillerPane();
        for (int i = 0; i < cols; i++) inv.setItem(i, pane);
        for (int i = size - cols; i < size; i++) inv.setItem(i, pane);
        for (int r = 1; r < rows - 1; r++) {
            inv.setItem(r * cols, pane);
            inv.setItem(r * cols + cols - 1, pane);
        }
    }

    public static void fillRow(Inventory inv, int row) {
        ItemStack pane = fillerPane();
        int start = row * 9;
        for (int i = start; i < start + 9; i++) inv.setItem(i, pane);
    }

    /** Build a coloured progress bar string using Unicode block characters. */
    public static String progressBar(double progress, int length) {
        int filled = (int) Math.round(progress * length);
        filled = Math.max(0, Math.min(filled, length));
        String filledColor = progress >= 1.0 ? "§a" : progress >= 0.5 ? "§e" : "§c";
        return filledColor + "█".repeat(filled) + "§8" + "░".repeat(length - filled);
    }

    /**
     * Returns inner slot indices (excluding the 1-slot border) for a given total row count.
     */
    public static int[] innerSlots(int rows) {
        int innerRows = rows - 2;
        int[] slots = new int[innerRows * 7];
        int idx = 0;
        for (int r = 1; r <= innerRows; r++) {
            for (int c = 1; c <= 7; c++) {
                slots[idx++] = r * 9 + c;
            }
        }
        return slots;
    }
}
