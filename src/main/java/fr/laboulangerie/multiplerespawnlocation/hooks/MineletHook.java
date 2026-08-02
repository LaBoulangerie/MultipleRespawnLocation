package fr.laboulangerie.multiplerespawnlocation.hooks;

import fr.minelet.api.MineletAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class MineletHook {

    private static boolean enabled = false;

    public static void init() {
        boolean pluginFound = Bukkit.getPluginManager().isPluginEnabled("Minelet");

        if (pluginFound) {
            try {
                Class.forName("fr.minelet.api.MineletAPI");
                if (MineletAPI.isAvailable()) {
                    enabled = true;
                    Bukkit.getLogger().info("[MultipleRespawnLocation] Minelet detected!");
                }
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
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
            var hamletOpt = MineletAPI.getHamlets().getEffectiveHamlet(player);
            if (hamletOpt.isPresent()) {
                return hamletOpt.get().getName();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Location getPlayerHamletSpawn(Player player) {
        if (!enabled) return null;
        try {
            var hamletOpt = MineletAPI.getHamlets().getEffectiveHamlet(player);
            if (hamletOpt.isPresent()) {
                return hamletOpt.get().getSpawn().orElse(null);
            }
            return null;
        } catch (NoClassDefFoundError e) {
            enabled = false;
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
