package fr.laboulangerie.multiplerespawnlocation.listeners;

import fr.laboulangerie.multiplerespawnlocation.MultipleRespawnLocation;
import fr.laboulangerie.multiplerespawnlocation.hooks.MineletHook;
import fr.laboulangerie.multiplerespawnlocation.hooks.MultiSpawnHook;
import fr.laboulangerie.multiplerespawnlocation.models.BedData;
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
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static fr.laboulangerie.multiplerespawnlocation.utils.BedsUtils.checksIfBedExists;
import static fr.laboulangerie.multiplerespawnlocation.utils.PlayerUtils.*;
import static fr.laboulangerie.multiplerespawnlocation.utils.RunCommandUtils.runCommandOnSpawn;;

@SuppressWarnings("deprecation")
public class RespawnMenuHandler implements Listener {

    static MultipleRespawnLocation plugin;

    public RespawnMenuHandler(MultipleRespawnLocation plugin) {
        RespawnMenuHandler.plugin = plugin;
    }

    public static void updateItens(Inventory gui, Player p) {

        if (gui.getViewers().toString().length() > 2) {

            ItemStack itens[] = gui.getContents();
            boolean hasActiveCooldown = false;
            for (ItemStack item : itens) {

                if (item != null && item.hasItemMeta()) {

                    ItemMeta item_meta = item.getItemMeta();
                    PersistentDataContainer data = item_meta.getPersistentDataContainer();

                    if (data.has(new NamespacedKey(plugin, "cooldown"), PersistentDataType.LONG)
                            && data.has(new NamespacedKey(plugin, "uuid"), PersistentDataType.STRING)) {

                        long cooldown = data.get(new NamespacedKey(plugin, "cooldown"), PersistentDataType.LONG);
                        List<String> lore = item_meta.getLore();

                        int optionsCount = 2;
                        if (plugin.getConfig().getBoolean("disable-bed-world-desc")) {
                            optionsCount--;
                        }
                        if (plugin.getConfig().getBoolean("disable-bed-coords-desc")) {
                            optionsCount--;
                        }
                        if (cooldown > System.currentTimeMillis()) {
                            hasActiveCooldown = true;
                            long sec = (cooldown - System.currentTimeMillis()) / 1000;
                            String seconds = Long.toString(sec);
                            if (lore == null) {
                                lore = new ArrayList<>();
                            }
                            if (lore.size() > optionsCount) {
                                lore.set(
                                        optionsCount,
                                        ChatColor.GOLD + "" + ChatColor.BOLD
                                                + plugin.getMessages("cooldown-text").replace("{1}", seconds));
                            } else {
                                lore.add(
                                        ChatColor.GOLD + "" + ChatColor.BOLD
                                                + plugin.getMessages("cooldown-text").replace("{1}", seconds));
                            }
                        } else {
                            if (lore.size() > optionsCount) {
                                lore.remove(optionsCount);
                            }
                        }

                        item_meta.setLore(lore);
                        item.setItemMeta(item_meta);
                    }
                }
            }

            if (hasActiveCooldown) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    updateItens(gui, p);
                }, 10L);
            }

        }

    }

    public static void openRespawnMenu(Player p) {

        // gets how much beds player has to use on for loop and for the if check
        PersistentDataContainer playerData = p.getPersistentDataContainer();
        PlayerBedsData playerBedsData = null;

        int playerBedsCount = getPlayerBedsCount(p);

        if (playerData.has(new NamespacedKey(plugin, "beds"), new BedsDataType())) {
            playerBedsData = playerData.get(new NamespacedKey(plugin, "beds"), new BedsDataType());
        }

        // Vérifier les options de spawn disponibles
        Location hamletSpawn = MineletHook.getPlayerHamletSpawn(p);
        String worldName = getPlayerRespawnLoc(p).getWorld().getName();
        boolean hasMultiSpawns = MultiSpawnHook.hasWorldSpawns(worldName);

        // Ouvrir le menu si le joueur a des lits, un hamlet, ou des spawns MultiSpawn
        if (playerBedsCount > 0 || hamletSpawn != null || hasMultiSpawns) {

            // sets stuff to player be invul and invis on spawn
            setPropPlayer(p);

            // create inventory
            int bedCount = playerBedsCount + 1;
            if (hamletSpawn != null) {
                bedCount++;
            }
            int inventorySize = 9 * ((int) Math.ceil(bedCount / (Double) 9.0));
            if (hasMultiSpawns) {
                inventorySize = Math.max(inventorySize, 18); // Au moins 2 lignes pour les spawns MultiSpawn (slots 9-17)
            }
            if (hamletSpawn != null) {
                inventorySize = Math.max(inventorySize, 27); // Au moins 3 lignes pour le hamlet au slot 18
            }
            Inventory gui = Bukkit.createInventory(p, inventorySize,
                    ChatColor.translateAlternateColorCodes('&', plugin.getMessages("menu-title")));

            HashMap<String, BedData> beds = playerBedsData != null ? playerBedsData.getPlayerBedData() : new HashMap<>();
            if (!plugin.getConfig().getBoolean("link-worlds")) {
                World world = getPlayerRespawnLoc(p).getWorld();
                HashMap<String, BedData> bedsT = (HashMap<String, BedData>) beds.clone();
                beds.forEach((uuid, bed) -> {
                    // clear lists so beds are only from the world that player is in
                    if (!bed.getBedWorld().equalsIgnoreCase(world.getName())) {
                        bedsT.remove(uuid);
                    }
                });
                beds = bedsT;
            }
            AtomicBoolean hasCooldown = new AtomicBoolean(false);
            AtomicInteger cont = new AtomicInteger(1);
            beds.forEach((uuid, bed) -> {
                ItemStack item = new ItemStack(bed.getBedMaterial(), 1);
                ItemMeta item_meta = item.getItemMeta();
                String bedName = plugin.getMessages("default-bed-name").replace("{1}", cont.toString());
                item_meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', bedName));
                if (bed.getBedName() != null) {
                    item_meta.setDisplayName(ChatColor.RESET + bed.getBedName());
                }
                PersistentDataContainer data = item_meta.getPersistentDataContainer();

                List<String> lore = new ArrayList<>();
                if (!plugin.getConfig().getBoolean("disable-bed-world-desc")) {
                    lore.add(ChatColor.DARK_PURPLE + bed.getBedWorld().toUpperCase());
                }
                if (!plugin.getConfig().getBoolean("disable-bed-coords-desc")) {
                    String[] location = bed.getBedCoords().split(":");
                    String locText = "X: " + location[0].substring(0, location[0].length() - 2) +
                            " Y: " + location[1].substring(0, location[1].length() - 2) +
                            " Z: " + location[2].substring(0, location[2].length() - 2);
                    lore.add(ChatColor.GRAY + locText);
                }
                // checks if has any cooldowns
                if (bed.getBedCooldown() > 0L) {

                    long cooldown = bed.getBedCooldown();
                    if (cooldown > System.currentTimeMillis()) { // if cooldown isnt expired
                        hasCooldown.set(true);
                        data.set(new NamespacedKey(plugin, "cooldown"), PersistentDataType.LONG, cooldown);
                    } else {
                        bed.setBedCooldown(0L);
                    }

                }

                data.set(new NamespacedKey(plugin, "uuid"), PersistentDataType.STRING, uuid);
                data.set(new NamespacedKey(plugin, "location"), PersistentDataType.STRING, bed.getBedCoords());
                data.set(new NamespacedKey(plugin, "world"), PersistentDataType.STRING, bed.getBedWorld());

                item_meta.setLore(lore);
                item.setItemMeta(item_meta);
                gui.setItem(cont.get() - 1, item);
                cont.getAndIncrement();
            });

            if (hasCooldown.get()) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    updateItens(gui, p);
                }, 10L);
            }

            // Ajouter option Hamlet si joueur a un spawn de hamlet
            if (hamletSpawn != null) {
                ItemStack hamletItem = new ItemStack(Material.CAMPFIRE, 1);
                ItemMeta hamletMeta = hamletItem.getItemMeta();
                hamletMeta.setDisplayName(ChatColor.BLUE + "Hamlet");

                List<String> hamletLore = new ArrayList<>();
                String hamletName = MineletHook.getPlayerHamletName(p);
                if (hamletName != null) {
                    hamletLore.add(ChatColor.WHITE + hamletName);
                }
                hamletLore.add(ChatColor.GRAY + "X: " + hamletSpawn.getBlockX() +
                              " Y: " + hamletSpawn.getBlockY() +
                              " Z: " + hamletSpawn.getBlockZ());
                hamletMeta.setLore(hamletLore);

                PersistentDataContainer hamletData = hamletMeta.getPersistentDataContainer();
                hamletData.set(new NamespacedKey(plugin, "hamlet"), PersistentDataType.STRING, "true");

                hamletItem.setItemMeta(hamletMeta);
                gui.setItem(18, hamletItem);
            }

            // Ajouter les spawns MultiSpawn sur la ligne 2 (slots 9-17)
            List<MultiSpawnHook.SpawnInfo> multiSpawns = MultiSpawnHook.getWorldSpawns(worldName);

            int multiSpawnSlot = 9;
            for (MultiSpawnHook.SpawnInfo spawnInfo : multiSpawns) {
                if (multiSpawnSlot > 17) break; // Max 9 spawns sur la ligne 2

                Material iconMaterial = Material.ENDER_PEARL; // Fallback
                String iconName = spawnInfo.getIcon();
                if (iconName != null) {
                    try {
                        iconMaterial = Material.valueOf(iconName.toUpperCase());
                    } catch (IllegalArgumentException ignored) {}
                }
                ItemStack spawnItem = new ItemStack(iconMaterial, 1);
                ItemMeta spawnMeta = spawnItem.getItemMeta();
                spawnMeta.setDisplayName(ChatColor.AQUA + spawnInfo.getName());

                List<String> spawnLore = new ArrayList<>();
                Location spawnLoc = spawnInfo.getLocation();
                spawnLore.add(ChatColor.GRAY + "X: " + spawnLoc.getBlockX() +
                             " Y: " + spawnLoc.getBlockY() +
                             " Z: " + spawnLoc.getBlockZ());
                spawnMeta.setLore(spawnLore);

                PersistentDataContainer spawnData = spawnMeta.getPersistentDataContainer();
                spawnData.set(new NamespacedKey(plugin, "multispawn"), PersistentDataType.STRING, spawnInfo.getName());

                spawnItem.setItemMeta(spawnMeta);
                gui.setItem(multiSpawnSlot, spawnItem);
                multiSpawnSlot++;
            }

            // Afficher le bloc SPAWN seulement si MultiSpawn n'a pas de spawns
            if (!hasMultiSpawns) {
                ItemStack item = new ItemStack(Material.GRASS_BLOCK, 1);
                ItemMeta item_meta = item.getItemMeta();
                item_meta.setDisplayName(ChatColor.YELLOW + "SPAWN");
                item.setItemMeta(item_meta);
                gui.setItem(8, item); // Dernier slot de la 1ère ligne
            }

            // I dont know why but if openInventory is not on a scheduler is does not open
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                p.openInventory(gui);
            }, 0L);

        } else {

            if (playerData.has(new NamespacedKey(plugin, "spawnLoc"))) {
                Location location = getPlayerRespawnLoc(p);
                playerData.remove(new NamespacedKey(plugin, "spawnLoc"));
                undoPropPlayer(p);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    p.teleport(location);
                }, 1L);
            }

        }

    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {

        if (e.getView().getTitle().equalsIgnoreCase(plugin.getMessages("menu-title"))) {
            e.setCancelled(true);
            Player p = (Player) e.getWhoClicked();
            if (e.getCurrentItem() != null) {
                PersistentDataContainer playerData = p.getPersistentDataContainer();
                int playerBedsCount = 0;
                PlayerBedsData playerBedsData = null;
                if (playerData.has(new NamespacedKey(plugin, "beds"), new BedsDataType())) {
                    playerBedsData = playerData.get(new NamespacedKey(plugin, "beds"), new BedsDataType());
                    if (playerBedsData != null && playerBedsData.getPlayerBedData() != null) {
                        playerBedsCount = playerBedsData.getPlayerBedData().size();
                    }
                }
                double bedCount = playerBedsCount + 1;
                int index = e.getSlot();
                if (e.getCurrentItem().getType().toString().toLowerCase().contains("bed")) {

                    ItemMeta item_meta = e.getCurrentItem().getItemMeta();
                    PersistentDataContainer data = item_meta.getPersistentDataContainer();

                    String bedCoord[] = data.get(new NamespacedKey(plugin, "location"), PersistentDataType.STRING)
                            .split(":");
                    String world = data.get(new NamespacedKey(plugin, "world"), PersistentDataType.STRING);
                    Location location = new Location(Bukkit.getWorld(world), Double.parseDouble(bedCoord[0]),
                            Double.parseDouble(bedCoord[1]), Double.parseDouble(bedCoord[2]));
                    String uuid = data.get(new NamespacedKey(plugin, "uuid"), PersistentDataType.STRING);

                    if (checksIfBedExists(location, p, uuid)) {

                        teleportPlayer(p, data, playerData, playerBedsData, uuid);

                    } else {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            p.closeInventory();
                        }, 0L);
                    }

                } else if (e.getCurrentItem().getType() == Material.CAMPFIRE) {
                    // Clic sur le spawn du hamlet
                    ItemMeta meta = e.getCurrentItem().getItemMeta();
                    PersistentDataContainer data = meta.getPersistentDataContainer();
                    if (data.has(new NamespacedKey(plugin, "hamlet"), PersistentDataType.STRING)) {
                        Location spawn = MineletHook.getPlayerHamletSpawn(p);
                        if (spawn != null) {
                            undoPropPlayer(p);
                            playerData.remove(new NamespacedKey(plugin, "spawnLoc"));
                            p.teleport(spawn);
                            runCommandOnSpawn(p);
                        }
                    }
                } else if (e.getCurrentItem().hasItemMeta()) {
                    // Clic sur un spawn MultiSpawn (identifié par le tag "multispawn")
                    ItemMeta meta = e.getCurrentItem().getItemMeta();
                    PersistentDataContainer data = meta.getPersistentDataContainer();
                    if (data.has(new NamespacedKey(plugin, "multispawn"), PersistentDataType.STRING)) {
                        String spawnName = data.get(new NamespacedKey(plugin, "multispawn"), PersistentDataType.STRING);
                        String worldName = getPlayerRespawnLoc(p).getWorld().getName();
                        List<MultiSpawnHook.SpawnInfo> spawns = MultiSpawnHook.getWorldSpawns(worldName);
                        for (MultiSpawnHook.SpawnInfo spawnInfo : spawns) {
                            if (spawnInfo.getName().equals(spawnName)) {
                                undoPropPlayer(p);
                                playerData.remove(new NamespacedKey(plugin, "spawnLoc"));
                                p.teleport(spawnInfo.getLocation());
                                runCommandOnSpawn(p);
                                break;
                            }
                        }
                    }
                } else if (index == 8 && e.getCurrentItem().getType() == Material.GRASS_BLOCK) {
                    // Dernier slot de la 1ère ligne (SPAWN) - seulement si pas de MultiSpawn
                    undoPropPlayer(p);
                    Location location = getPlayerRespawnLoc(p);
                    playerData.remove(new NamespacedKey(plugin, "spawnLoc"));
                    p.teleport(location);
                    runCommandOnSpawn(p);
                }
            }

        }

    }

    @EventHandler
    public void onMenuClose(InventoryCloseEvent e) {

        if (e.getView().getTitle().equalsIgnoreCase(plugin.getMessages("menu-title"))) {

            Player p = (Player) e.getPlayer();
            if (!p.getCanPickupItems()) {
                openRespawnMenu(p);
            }

        }

    }

}
