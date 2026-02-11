package fr.laboulangerie.multiplerespawnlocation.listeners;

import fr.laboulangerie.multiplerespawnlocation.MultipleRespawnLocation;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import static fr.laboulangerie.multiplerespawnlocation.utils.PlayerUtils.stringToLocation;
import static fr.laboulangerie.multiplerespawnlocation.utils.PlayerUtils.undoPropPlayer;

public class PlayerJoinListener implements Listener {
    MultipleRespawnLocation plugin;

    public PlayerJoinListener(MultipleRespawnLocation plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        PersistentDataContainer playerData = p.getPersistentDataContainer();
        NamespacedKey spawnLocName = new NamespacedKey(plugin, "spawnLoc");
        if (plugin.getConfig().getBoolean("spawn-on-sky") && playerData.has(spawnLocName, PersistentDataType.STRING)) {
            Location location = stringToLocation(playerData.get(spawnLocName, PersistentDataType.STRING));
            playerData.remove(spawnLocName);
            p.teleport(location);
        }
        undoPropPlayer(p);
    }
}
