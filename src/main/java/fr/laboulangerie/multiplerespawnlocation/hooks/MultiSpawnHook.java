package fr.laboulangerie.multiplerespawnlocation.hooks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MultiSpawnHook {

    private static boolean enabled = false;
    private static fr.laboulangerie.multispawn.MultiSpawn multiSpawnPlugin = null;

    public static void init() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("MultiSpawn");

        if (plugin != null && plugin.isEnabled()) {
            try {
                Class.forName("fr.laboulangerie.multispawn.MultiSpawn");
                multiSpawnPlugin = (fr.laboulangerie.multispawn.MultiSpawn) plugin;
                enabled = true;
                Bukkit.getLogger().info("[MultipleRespawnLocation] MultiSpawn detected!");
            } catch (ClassNotFoundException e) {
                enabled = false;
            } catch (NoClassDefFoundError e) {
                enabled = false;
            }
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean hasWorldSpawns(String worldName) {
        if (!enabled || multiSpawnPlugin == null) return false;
        try {
            var spawnManager = multiSpawnPlugin.getSpawnManager();
            Collection<?> spawns = spawnManager.getSpawns(worldName);
            return spawns != null && !spawns.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public static List<SpawnInfo> getWorldSpawns(String worldName) {
        List<SpawnInfo> result = new ArrayList<>();
        if (!enabled || multiSpawnPlugin == null) return result;

        try {
            var spawnManager = multiSpawnPlugin.getSpawnManager();
            Collection<fr.laboulangerie.multispawn.models.Spawn> spawns = spawnManager.getSpawns(worldName);
            if (spawns != null) {
                for (var spawn : spawns) {
                    result.add(new SpawnInfo(spawn.getName(), spawn.toLocation(), spawn.getIcon()));
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[MultipleRespawnLocation] Error getting MultiSpawn spawns: " + e.getMessage());
        }

        return result;
    }

    public static class SpawnInfo {
        private final String name;
        private final Location location;
        private final String icon;

        public SpawnInfo(String name, Location location, String icon) {
            this.name = name;
            this.location = location;
            this.icon = icon;
        }

        public String getName() {
            return name;
        }

        public Location getLocation() {
            return location;
        }

        public String getIcon() {
            return icon;
        }
    }
}
