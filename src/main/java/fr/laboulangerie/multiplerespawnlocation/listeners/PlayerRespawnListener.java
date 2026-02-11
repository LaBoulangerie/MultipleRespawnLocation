package fr.laboulangerie.multiplerespawnlocation.listeners;

import fr.laboulangerie.multiplerespawnlocation.MultipleRespawnLocation;
import fr.laboulangerie.multiplerespawnlocation.models.BedData;
import fr.laboulangerie.multiplerespawnlocation.models.BedsDataType;
import fr.laboulangerie.multiplerespawnlocation.models.PlayerBedsData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerRespawnEvent.RespawnReason;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;

import static fr.laboulangerie.multiplerespawnlocation.listeners.RespawnMenuHandler.openRespawnMenu;
import static fr.laboulangerie.multiplerespawnlocation.utils.BedsUtils.checksIfBedExists;
import static fr.laboulangerie.multiplerespawnlocation.utils.PlayerUtils.locationToString;

public class PlayerRespawnListener implements Listener {
    static MultipleRespawnLocation plugin;

    public PlayerRespawnListener(MultipleRespawnLocation plugin) {
        PlayerRespawnListener.plugin = plugin;
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {

        World world = e.getRespawnLocation().getWorld();
        if (world.getEnvironment() == Environment.NETHER || world.getEnvironment() == Environment.THE_END)
            return;
        if (e.getRespawnReason() == RespawnReason.PLUGIN)
            return;
        String worldName = world.getName();
        List<String> denylist = plugin.getConfig().getStringList("denylist");
        List<String> allowlist = plugin.getConfig().getStringList("allowlist");
        boolean passLists = (!denylist.contains(worldName)) && (allowlist.contains(worldName) || allowlist.isEmpty());

        if (passLists) {
            Player p = e.getPlayer();
            PersistentDataContainer playerData = p.getPersistentDataContainer();
            PlayerBedsData playerBedsData;
            HashMap<String, BedData> beds;
            if (playerData.has(new NamespacedKey(plugin, "beds"), new BedsDataType())) {
                playerBedsData = playerData.get(new NamespacedKey(plugin, "beds"), new BedsDataType());
                if (playerBedsData != null && playerBedsData.getPlayerBedData() != null) {
                    beds = playerBedsData.getPlayerBedData();
                    beds.forEach((uuid, bed) -> { // loops all beds to check if they still exist
                        String loc[] = bed.getBedCoords().split(":");
                        String bedWorld = bed.getBedWorld();
                        Location bedLoc = new Location(Bukkit.getWorld(bedWorld), Double.parseDouble(loc[0]),
                                Double.parseDouble(loc[1]), Double.parseDouble(loc[2]));
                        checksIfBedExists(bedLoc, p, uuid);
                    });

                }
            }
            Location loc = e.getRespawnLocation();
            if (plugin.getConfig().getBoolean("spawn-on-sky")) {
                playerData.set(new NamespacedKey(plugin, "spawnLoc"), PersistentDataType.STRING, locationToString(loc));
                loc.setY(loc.getY() + 300);
            }
            openRespawnMenu(p);
        }
    }
}
