package fr.laboulangerie.multiplerespawnlocation.menu;

import fr.laboulangerie.multiplerespawnlocation.MultipleRespawnLocation;
import fr.laboulangerie.multiplerespawnlocation.hooks.MineletHook;
import fr.laboulangerie.multiplerespawnlocation.hooks.MultiSpawnHook;
import fr.laboulangerie.multiplerespawnlocation.models.BedData;
import fr.laboulangerie.multiplerespawnlocation.models.BedsDataType;
import fr.laboulangerie.multiplerespawnlocation.models.PlayerBedsData;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static fr.laboulangerie.multiplerespawnlocation.utils.PlayerUtils.getPlayerRespawnLoc;

public class RespawnMenuBuilder {

    public static final String KEY_UUID = "uuid";
    public static final String KEY_LOCATION = "location";
    public static final String KEY_WORLD = "world";
    public static final String KEY_COOLDOWN = "cooldown";
    public static final String KEY_HAMLET = "hamlet";
    public static final String KEY_MULTISPAWN = "multispawn";
    public static final String KEY_SPAWN = "spawn";

    public static class BuildResult {
        private final Inventory inventory;
        private final boolean hasCooldown;

        public BuildResult(Inventory inventory, boolean hasCooldown) {
            this.inventory = inventory;
            this.hasCooldown = hasCooldown;
        }

        public Inventory getInventory() {
            return inventory;
        }

        public boolean hasCooldown() {
            return hasCooldown;
        }
    }

    public static BuildResult build(Player player) {
        MultipleRespawnLocation plugin = MultipleRespawnLocation.getInstance();

        Map<String, BedData> beds = getPlayerBeds(player, plugin);
        Location hamletSpawn = MineletHook.getPlayerHamletSpawn(player);
        String worldName = getPlayerRespawnLoc(player).getWorld().getName();
        List<MultiSpawnHook.SpawnInfo> multiSpawns = MultiSpawnHook.getWorldSpawns(worldName);

        if (beds.isEmpty() && hamletSpawn == null && multiSpawns.isEmpty()) {
            return null;
        }

        boolean[] hasCooldown = {false};
        int bedIndex = 1;

        List<MenuItem> bedItems = new ArrayList<>();
        for (Map.Entry<String, BedData> entry : beds.entrySet()) {
            MenuItem bedItem = createBedItem(entry.getKey(), entry.getValue(), bedIndex++, hasCooldown, plugin);
            bedItems.add(bedItem);
        }

        MenuItem spawnItem = null;
        List<MenuItem> multiSpawnItems = new ArrayList<>();

        if (!multiSpawns.isEmpty()) {
            for (MultiSpawnHook.SpawnInfo spawn : multiSpawns) {
                multiSpawnItems.add(createMultiSpawnItem(spawn, plugin));
            }
        } else {
            spawnItem = createSpawnItem(plugin);
        }

        MenuItem hamletItem = hamletSpawn != null ? createHamletItem(player, hamletSpawn, plugin) : null;

        MenuLayout layout = MenuLayout.calculate(bedItems, spawnItem, hamletItem, multiSpawnItems);
        Inventory inventory = layout.createInventory(player, plugin.getMessages("menu-title"));

        return new BuildResult(inventory, hasCooldown[0]);
    }

    private static Map<String, BedData> getPlayerBeds(Player player, MultipleRespawnLocation plugin) {
        Map<String, BedData> result = new LinkedHashMap<>();

        PersistentDataContainer playerData = player.getPersistentDataContainer();
        NamespacedKey bedsKey = new NamespacedKey(plugin, "beds");

        if (!playerData.has(bedsKey, new BedsDataType())) {
            return result;
        }

        PlayerBedsData playerBedsData = playerData.get(bedsKey, new BedsDataType());
        if (playerBedsData == null || playerBedsData.getPlayerBedData() == null) {
            return result;
        }

        Map<String, BedData> beds = playerBedsData.getPlayerBedData();

        if (plugin.getConfig().getBoolean("link-worlds")) {
            result.putAll(beds);
        } else {
            String currentWorld = getPlayerRespawnLoc(player).getWorld().getName();
            beds.forEach((uuid, bed) -> {
                if (bed.getBedWorld().equalsIgnoreCase(currentWorld)) {
                    result.put(uuid, bed);
                }
            });
        }

        return result;
    }

    private static MenuItem createBedItem(String uuid, BedData bed, int index, boolean[] hasCooldown,
                                           MultipleRespawnLocation plugin) {
        ItemStack item = new ItemStack(bed.getBedMaterial(), 1);
        ItemMeta meta = item.getItemMeta();

        String displayName = bed.getBedName() != null
            ? ChatColor.RESET + bed.getBedName()
            : ChatColor.translateAlternateColorCodes('&',
                plugin.getMessages("default-bed-name").replace("{1}", String.valueOf(index)));
        meta.setDisplayName(displayName);

        List<String> lore = new ArrayList<>();
        if (!plugin.getConfig().getBoolean("disable-bed-world-desc")) {
            lore.add(ChatColor.DARK_PURPLE + bed.getBedWorld().toUpperCase());
        }
        if (!plugin.getConfig().getBoolean("disable-bed-coords-desc")) {
            lore.add(ChatColor.GRAY + formatCoords(bed.getBedCoords()));
        }
        meta.setLore(lore);

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(new NamespacedKey(plugin, KEY_UUID), PersistentDataType.STRING, uuid);
        data.set(new NamespacedKey(plugin, KEY_LOCATION), PersistentDataType.STRING, bed.getBedCoords());
        data.set(new NamespacedKey(plugin, KEY_WORLD), PersistentDataType.STRING, bed.getBedWorld());

        long cooldown = bed.getBedCooldown();
        if (cooldown > System.currentTimeMillis()) {
            hasCooldown[0] = true;
            data.set(new NamespacedKey(plugin, KEY_COOLDOWN), PersistentDataType.LONG, cooldown);
        }

        item.setItemMeta(meta);
        return new MenuItem(item, MenuItem.Type.BED, uuid);
    }

    private static String formatCoords(String coords) {
        String[] parts = coords.split(":");
        return "X: " + parts[0].substring(0, parts[0].length() - 2) +
               " Y: " + parts[1].substring(0, parts[1].length() - 2) +
               " Z: " + parts[2].substring(0, parts[2].length() - 2);
    }

    private static String formatLocation(Location loc) {
        return "X: " + loc.getBlockX() + " Y: " + loc.getBlockY() + " Z: " + loc.getBlockZ();
    }

    private static MenuItem createSpawnItem(MultipleRespawnLocation plugin) {
        ItemStack item = new ItemStack(Material.GRASS_BLOCK, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "SPAWN");

        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, KEY_SPAWN), PersistentDataType.STRING, "true");

        item.setItemMeta(meta);
        return new MenuItem(item, MenuItem.Type.SPAWN);
    }

    private static MenuItem createHamletItem(Player player, Location spawn, MultipleRespawnLocation plugin) {
        ItemStack item = new ItemStack(Material.CAMPFIRE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.BLUE + "Hamlet");

        List<String> lore = new ArrayList<>();
        String hamletName = MineletHook.getPlayerHamletName(player);
        if (hamletName != null) {
            lore.add(ChatColor.WHITE + hamletName);
        }
        lore.add(ChatColor.GRAY + formatLocation(spawn));
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, KEY_HAMLET), PersistentDataType.STRING, "true");

        item.setItemMeta(meta);
        return new MenuItem(item, MenuItem.Type.HAMLET);
    }

    private static MenuItem createMultiSpawnItem(MultiSpawnHook.SpawnInfo spawnInfo, MultipleRespawnLocation plugin) {
        Material iconMaterial = parseMaterial(spawnInfo.getIcon(), Material.ENDER_PEARL);

        ItemStack item = new ItemStack(iconMaterial, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + spawnInfo.getName());
        meta.setLore(List.of(ChatColor.GRAY + formatLocation(spawnInfo.getLocation())));

        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, KEY_MULTISPAWN), PersistentDataType.STRING, spawnInfo.getName());

        item.setItemMeta(meta);
        return new MenuItem(item, MenuItem.Type.MULTISPAWN, spawnInfo.getName());
    }

    private static Material parseMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
