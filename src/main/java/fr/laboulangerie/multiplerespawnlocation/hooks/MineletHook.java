package fr.laboulangerie.multiplerespawnlocation.hooks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class MineletHook {

    private static boolean enabled = false;

    public static void init() {
        Bukkit.getLogger().info("[MRL-Debug] init() called");

        // Lister tous les plugins pour trouver le bon nom
        Bukkit.getLogger().info("[MRL-Debug] Plugins actifs:");
        for (var plugin : Bukkit.getPluginManager().getPlugins()) {
            if (plugin.getName().toLowerCase().contains("mine") || plugin.getName().toLowerCase().contains("let")) {
                Bukkit.getLogger().info("[MRL-Debug]   - " + plugin.getName() + " (enabled=" + plugin.isEnabled() + ")");
            }
        }

        boolean pluginFound = Bukkit.getPluginManager().isPluginEnabled("Minelet");
        Bukkit.getLogger().info("[MRL-Debug] isPluginEnabled('Minelet'): " + pluginFound);

        if (!pluginFound) {
            // Essayer avec d'autres noms possibles
            pluginFound = Bukkit.getPluginManager().isPluginEnabled("MineLet");
            Bukkit.getLogger().info("[MRL-Debug] isPluginEnabled('MineLet'): " + pluginFound);
        }

        if (pluginFound) {
            try {
                Bukkit.getLogger().info("[MRL-Debug] Trying Class.forName('fr.minelet.api.MineletAPI')...");
                Class.forName("fr.minelet.api.MineletAPI");
                Bukkit.getLogger().info("[MRL-Debug] Class found! Checking isAvailable()...");

                boolean available = fr.minelet.api.MineletAPI.isAvailable();
                Bukkit.getLogger().info("[MRL-Debug] MineletAPI.isAvailable(): " + available);

                if (available) {
                    enabled = true;
                    Bukkit.getLogger().info("[MultipleRespawnLocation] Minelet detected!");
                } else {
                    Bukkit.getLogger().warning("[MRL-Debug] MineletAPI not available");
                }
            } catch (ClassNotFoundException e) {
                Bukkit.getLogger().warning("[MRL-Debug] ClassNotFoundException: " + e.getMessage());
                enabled = false;
            } catch (NoClassDefFoundError e) {
                Bukkit.getLogger().warning("[MRL-Debug] NoClassDefFoundError: " + e.getMessage());
                enabled = false;
            } catch (Exception e) {
                Bukkit.getLogger().warning("[MRL-Debug] Exception: " + e.getClass().getName() + " - " + e.getMessage());
                enabled = false;
            }
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static String getPlayerHamletName(Player player) {
        if (!enabled) return null;
        try {
            var hamletOpt = fr.minelet.api.MineletAPI.getHamlets().getPlayerHamlet(player.getUniqueId());
            if (hamletOpt.isPresent()) {
                return hamletOpt.get().getName();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Location getPlayerHamletSpawn(Player player) {
        Bukkit.getLogger().info("[MRL-Debug] getPlayerHamletSpawn called for " + player.getName() + ", enabled=" + enabled);
        if (!enabled) return null;
        try {
            var hamletOpt = fr.minelet.api.MineletAPI.getHamlets().getPlayerHamlet(player.getUniqueId());
            Bukkit.getLogger().info("[MRL-Debug] getPlayerHamlet result: " + (hamletOpt.isPresent() ? "found hamlet" : "no hamlet"));

            if (hamletOpt.isPresent()) {
                var hamlet = hamletOpt.get();
                Bukkit.getLogger().info("[MRL-Debug] Hamlet name: " + hamlet.getName());
                var spawnOpt = hamlet.getSpawn();
                Bukkit.getLogger().info("[MRL-Debug] Hamlet spawn: " + (spawnOpt.isPresent() ? spawnOpt.get() : "no spawn defined"));
                return spawnOpt.orElse(null);
            }
            return null;
        } catch (NoClassDefFoundError e) {
            Bukkit.getLogger().warning("[MRL-Debug] NoClassDefFoundError: " + e.getMessage());
            enabled = false;
            return null;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[MRL-Debug] Exception: " + e.getClass().getName() + " - " + e.getMessage());
            return null;
        }
    }
}
