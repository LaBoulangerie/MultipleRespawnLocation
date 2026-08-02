package fr.laboulangerie.multiplerespawnlocation.menu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MenuLayout {

    private static final int SLOTS_PER_ROW = 9;

    private final int inventorySize;
    private final Map<Integer, MenuItem> items;

    public MenuLayout(int inventorySize, Map<Integer, MenuItem> items) {
        this.inventorySize = inventorySize;
        this.items = items;
    }

    public static MenuLayout calculate(List<MenuItem> beds, MenuItem spawn, MenuItem hamlet,
                                        List<MenuItem> multiSpawns) {
        Map<Integer, MenuItem> items = new LinkedHashMap<>();
        int currentLine = 0;

        // Ligne(s) des lits
        if (beds != null && !beds.isEmpty()) {
            int slot = 0;
            for (MenuItem bed : beds) {
                items.put(slot++, bed);
            }
            currentLine = (slot - 1) / SLOTS_PER_ROW + 1;
        }

        // Ligne spawn ou multispawns
        if (multiSpawns != null && !multiSpawns.isEmpty()) {
            int lineStart = currentLine * SLOTS_PER_ROW;
            int slot = lineStart;
            for (MenuItem ms : multiSpawns) {
                if (slot >= lineStart + SLOTS_PER_ROW) break;
                items.put(slot++, ms);
            }
            currentLine++;
        } else if (spawn != null) {
            items.put(currentLine * SLOTS_PER_ROW, spawn);
            currentLine++;
        }

        // Ligne hamlet
        if (hamlet != null) {
            items.put(currentLine * SLOTS_PER_ROW, hamlet);
        }

        int maxSlot = items.keySet().stream().max(Integer::compare).orElse(0);
        int size = Math.max(SLOTS_PER_ROW, (maxSlot / SLOTS_PER_ROW + 1) * SLOTS_PER_ROW);

        return new MenuLayout(size, items);
    }

    public Inventory createInventory(Player player, String title) {
        Inventory gui = Bukkit.createInventory(player, inventorySize,
            ChatColor.translateAlternateColorCodes('&', title));

        items.forEach((slot, item) -> gui.setItem(slot, item.getItem()));

        return gui;
    }
}
