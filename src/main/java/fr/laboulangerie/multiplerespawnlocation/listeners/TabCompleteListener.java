package fr.laboulangerie.multiplerespawnlocation.listeners;

import fr.laboulangerie.multiplerespawnlocation.MultipleRespawnLocation;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.ArrayList;
import java.util.List;

public class TabCompleteListener implements Listener {
    private final MultipleRespawnLocation plugin;

    public TabCompleteListener(MultipleRespawnLocation plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        String buffer = event.getBuffer();

        // Vérifier si c'est notre commande
        if (!buffer.toLowerCase().startsWith("/mrl ") &&
            !buffer.toLowerCase().startsWith("/multiplerespawnlocation ")) {
            return;
        }

        // Extraire les arguments après la commande
        String[] parts = buffer.split(" ", -1);
        List<String> completions = new ArrayList<>();

        // Construire liste des sous-commandes disponibles
        List<String> subCommands = new ArrayList<>();
        subCommands.add("listbed");
        subCommands.add("rename");
        if (plugin.getConfig().getBoolean("remove-beds-gui")) {
            subCommands.add("delete");
        }
        if (plugin.getConfig().getBoolean("bed-sharing")) {
            subCommands.add("share");
        }

        if (parts.length == 2) {
            // Premier argument = sous-commande
            String input = parts[1].toLowerCase();
            for (String cmd : subCommands) {
                if (cmd.startsWith(input)) {
                    completions.add(cmd);
                }
            }
        } else if (parts.length == 3 && parts[1].equalsIgnoreCase("share")) {
            // Deuxième argument pour "share" = nom de joueur
            String input = parts[2].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (event.getSender() instanceof Player && player.equals(event.getSender())) {
                    continue;
                }
                if (player.getName().toLowerCase().startsWith(input)) {
                    completions.add(player.getName());
                }
            }
        }

        event.setCompletions(completions);
    }
}
