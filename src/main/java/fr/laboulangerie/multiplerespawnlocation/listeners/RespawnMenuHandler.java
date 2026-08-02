package fr.laboulangerie.multiplerespawnlocation.listeners;

import fr.laboulangerie.multiplerespawnlocation.MultipleRespawnLocation;
import fr.laboulangerie.multiplerespawnlocation.hooks.MineletHook;
import fr.laboulangerie.multiplerespawnlocation.hooks.MultiSpawnHook;
import fr.laboulangerie.multiplerespawnlocation.menu.RespawnMenuBuilder;
import fr.laboulangerie.multiplerespawnlocation.models.BedsDataType;
import fr.laboulangerie.multiplerespawnlocation.models.PlayerBedsData;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

import static fr.laboulangerie.multiplerespawnlocation.utils.BedsUtils.checksIfBedExists;
import static fr.laboulangerie.multiplerespawnlocation.utils.PlayerUtils.*;
import static fr.laboulangerie.multiplerespawnlocation.utils.RunCommandUtils.runCommandOnSpawn;

@SuppressWarnings("deprecation")
public class RespawnMenuHandler implements Listener {

    static MultipleRespawnLocation plugin;

    public RespawnMenuHandler(MultipleRespawnLocation plugin) {
        RespawnMenuHandler.plugin = plugin;
    }

    public static void updateItens(Inventory gui, Player p) {
        if (gui.getViewers().isEmpty()) {
            return;
        }

        NamespacedKey cooldownKey = new NamespacedKey(plugin, "cooldown");
        NamespacedKey uuidKey = new NamespacedKey(plugin, "uuid");
        boolean hasActiveCooldown = false;

        int baseLoreSize = 2;
        if (plugin.getConfig().getBoolean("disable-bed-world-desc")) baseLoreSize--;
        if (plugin.getConfig().getBoolean("disable-bed-coords-desc")) baseLoreSize--;

        for (ItemStack item : gui.getContents()) {
            if (item == null || !item.hasItemMeta()) continue;

            ItemMeta meta = item.getItemMeta();
            PersistentDataContainer data = meta.getPersistentDataContainer();

            if (!data.has(cooldownKey, PersistentDataType.LONG) || !data.has(uuidKey, PersistentDataType.STRING)) {
                continue;
            }

            long cooldown = data.get(cooldownKey, PersistentDataType.LONG);
            List<String> lore = meta.getLore() != null ? meta.getLore() : new ArrayList<>();

            if (cooldown > System.currentTimeMillis()) {
                hasActiveCooldown = true;
                long seconds = (cooldown - System.currentTimeMillis()) / 1000;
                String cooldownText = ChatColor.GOLD + "" + ChatColor.BOLD +
                    plugin.getMessages("cooldown-text").replace("{1}", String.valueOf(seconds));

                if (lore.size() > baseLoreSize) {
                    lore.set(baseLoreSize, cooldownText);
                } else {
                    lore.add(cooldownText);
                }
            } else if (lore.size() > baseLoreSize) {
                lore.remove(baseLoreSize);
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        if (hasActiveCooldown) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> updateItens(gui, p), 10L);
        }
    }

    public static void openRespawnMenu(Player p) {
        RespawnMenuBuilder.BuildResult result = RespawnMenuBuilder.build(p);

        if (result == null) {
            teleportToDefaultSpawn(p);
            return;
        }

        setPropPlayer(p);
        Inventory gui = result.getInventory();

        if (result.hasCooldown()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> updateItens(gui, p), 10L);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> p.openInventory(gui), 0L);
    }

    private static void teleportToDefaultSpawn(Player p) {
        PersistentDataContainer playerData = p.getPersistentDataContainer();
        NamespacedKey spawnLocKey = new NamespacedKey(plugin, "spawnLoc");

        if (playerData.has(spawnLocKey)) {
            Location location = getPlayerRespawnLoc(p);
            playerData.remove(spawnLocKey);
            undoPropPlayer(p);
            Bukkit.getScheduler().runTaskLater(plugin, () -> p.teleport(location), 1L);
        }
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equalsIgnoreCase(plugin.getMessages("menu-title"))) {
            return;
        }

        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        Player p = (Player) e.getWhoClicked();
        PersistentDataContainer data = clicked.getItemMeta().getPersistentDataContainer();

        if (data.has(key(RespawnMenuBuilder.KEY_UUID), PersistentDataType.STRING)) {
            handleBedClick(p, data);
        } else if (data.has(key(RespawnMenuBuilder.KEY_HAMLET), PersistentDataType.STRING)) {
            handleHamletClick(p);
        } else if (data.has(key(RespawnMenuBuilder.KEY_MULTISPAWN), PersistentDataType.STRING)) {
            handleMultiSpawnClick(p, data);
        } else if (data.has(key(RespawnMenuBuilder.KEY_SPAWN), PersistentDataType.STRING)) {
            handleSpawnClick(p);
        }
    }

    private NamespacedKey key(String name) {
        return new NamespacedKey(plugin, name);
    }

    private void handleBedClick(Player p, PersistentDataContainer data) {
        String[] coords = data.get(key(RespawnMenuBuilder.KEY_LOCATION), PersistentDataType.STRING).split(":");
        String worldName = data.get(key(RespawnMenuBuilder.KEY_WORLD), PersistentDataType.STRING);
        String uuid = data.get(key(RespawnMenuBuilder.KEY_UUID), PersistentDataType.STRING);

        Location location = new Location(Bukkit.getWorld(worldName),
            Double.parseDouble(coords[0]),
            Double.parseDouble(coords[1]),
            Double.parseDouble(coords[2]));

        PersistentDataContainer playerData = p.getPersistentDataContainer();
        PlayerBedsData playerBedsData = playerData.has(key("beds"), new BedsDataType())
            ? playerData.get(key("beds"), new BedsDataType())
            : null;

        if (checksIfBedExists(location, p, uuid)) {
            teleportPlayer(p, data, playerData, playerBedsData, uuid);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> p.closeInventory(), 0L);
        }
    }

    private void handleHamletClick(Player p) {
        Location spawn = MineletHook.getPlayerHamletSpawn(p);
        if (spawn != null) {
            teleportAndCleanup(p, spawn);
        }
    }

    private void handleMultiSpawnClick(Player p, PersistentDataContainer data) {
        String spawnName = data.get(key(RespawnMenuBuilder.KEY_MULTISPAWN), PersistentDataType.STRING);
        String worldName = getPlayerRespawnLoc(p).getWorld().getName();

        MultiSpawnHook.getWorldSpawns(worldName).stream()
            .filter(s -> s.getName().equals(spawnName))
            .findFirst()
            .ifPresent(spawnInfo -> teleportAndCleanup(p, spawnInfo.getLocation()));
    }

    private void handleSpawnClick(Player p) {
        teleportAndCleanup(p, getPlayerRespawnLoc(p));
    }

    private void teleportAndCleanup(Player p, Location location) {
        undoPropPlayer(p);
        p.getPersistentDataContainer().remove(key("spawnLoc"));
        p.teleport(location);
        runCommandOnSpawn(p);
    }

    @EventHandler
    public void onMenuClose(InventoryCloseEvent e) {
        if (!e.getView().getTitle().equalsIgnoreCase(plugin.getMessages("menu-title"))) {
            return;
        }

        Player p = (Player) e.getPlayer();
        if (!p.getCanPickupItems()) {
            openRespawnMenu(p);
        }
    }

}
