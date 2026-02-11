package fr.laboulangerie.multiplerespawnlocation.commands;

import fr.laboulangerie.multiplerespawnlocation.MultipleRespawnLocation;
import fr.laboulangerie.multiplerespawnlocation.models.BedData;
import fr.laboulangerie.multiplerespawnlocation.models.BedsDataType;
import fr.laboulangerie.multiplerespawnlocation.models.PlayerBedsData;

import static fr.laboulangerie.multiplerespawnlocation.listeners.RemoveMenuHandler.openRemoveMenu;
import static fr.laboulangerie.multiplerespawnlocation.utils.BedsUtils.checkIfIsBed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class MrlCommand extends BukkitCommand {
    private final MultipleRespawnLocation plugin;

    public MrlCommand(MultipleRespawnLocation plugin, String name) {
        super(name);
        this.plugin = plugin;
        this.description = "MultipleRespawnLocation main command";
        this.usageMessage = "/mrl <listbed|rename|delete|share>";
        this.setAliases(Arrays.asList("multiplerespawnlocation"));
    }

    @Override
    public boolean execute(CommandSender sender, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

        switch (subCommand) {
            case "listbed":
                return executeListBed(player);
            case "rename":
                return executeRename(player, subArgs);
            case "delete":
                return executeDelete(player);
            case "share":
                return executeShare(player, subArgs);
            default:
                sendHelp(player);
                return true;
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== MultipleRespawnLocation Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/mrl listbed" + ChatColor.WHITE + " - List your registered beds");
        player.sendMessage(ChatColor.YELLOW + "/mrl rename <name>" + ChatColor.WHITE + " - Rename the bed you're looking at");
        if (plugin.getConfig().getBoolean("remove-beds-gui")) {
            player.sendMessage(ChatColor.YELLOW + "/mrl delete" + ChatColor.WHITE + " - Open the bed removal menu");
        }
        if (plugin.getConfig().getBoolean("bed-sharing")) {
            player.sendMessage(ChatColor.YELLOW + "/mrl share <player>" + ChatColor.WHITE + " - Share the bed you're looking at");
        }
    }

    private boolean executeListBed(Player player) {
        PersistentDataContainer playerData = player.getPersistentDataContainer();

        if (!playerData.has(new NamespacedKey(plugin, "beds"), new BedsDataType())) {
            player.sendMessage(ChatColor.RED + plugin.getMessages("no-beds-message"));
            return true;
        }

        PlayerBedsData playerBedsData = playerData.get(new NamespacedKey(plugin, "beds"), new BedsDataType());
        if (playerBedsData == null || playerBedsData.getPlayerBedData() == null || playerBedsData.getPlayerBedData().isEmpty()) {
            player.sendMessage(ChatColor.RED + plugin.getMessages("no-beds-message"));
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "=== Vos lits enregistrés ===");
        int index = 1;
        for (Map.Entry<String, BedData> entry : playerBedsData.getPlayerBedData().entrySet()) {
            BedData bed = entry.getValue();
            String name = bed.getBedName() != null ? bed.getBedName() : plugin.getMessages("default-bed-name").replace("{1}", String.valueOf(index));
            String[] coords = bed.getBedCoords().split(":");
            String location = "X:" + coords[0].split("\\.")[0] + " Y:" + coords[1].split("\\.")[0] + " Z:" + coords[2].split("\\.")[0];
            player.sendMessage(ChatColor.YELLOW + "- " + name + ChatColor.GRAY + " (" + bed.getBedWorld() + ": " + location + ")");
            index++;
        }
        return true;
    }

    private boolean executeRename(Player player, String[] args) {
        String name = String.join(" ", args);

        Block bed = checkIfIsBed(player.getTargetBlockExact(4));
        if (bed != null) {
            BlockState blockState = bed.getState();
            String bedUUID = null;
            if (blockState instanceof TileState tileState) {
                PersistentDataContainer container = tileState.getPersistentDataContainer();

                if (container.has(new NamespacedKey(plugin, "uuid"), PersistentDataType.STRING)) {
                    bedUUID = container.get(new NamespacedKey(plugin, "uuid"), PersistentDataType.STRING);
                }

                tileState.update();
            }

            if (bedUUID == null) {
                player.sendMessage(ChatColor.RED + plugin.getMessages("bed-not-registered-message"));
                return false;
            }

            PlayerBedsData playerBedsData = null;
            PersistentDataContainer playerData = player.getPersistentDataContainer();

            if (playerData.has(new NamespacedKey(plugin, "beds"), new BedsDataType())) {
                playerBedsData = playerData.get(new NamespacedKey(plugin, "beds"), new BedsDataType());
                if (playerBedsData != null && playerBedsData.getPlayerBedData() != null
                        && playerBedsData.hasBed(bedUUID)) {
                    BedData bedData = playerBedsData.getPlayerBedData().get(bedUUID);
                    bedData.setBedName(name);
                    playerData.set(new NamespacedKey(plugin, "beds"), new BedsDataType(), playerBedsData);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getMessages("bed-name-registered-successfully-message")));
                } else {
                    player.sendMessage(ChatColor.RED + plugin.getMessages("bed-not-registered-message"));
                    return false;
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + plugin.getMessages("bed-not-found-message"));
            return false;
        }

        return true;
    }

    private boolean executeDelete(Player player) {
        if (!plugin.getConfig().getBoolean("remove-beds-gui")) {
            player.sendMessage(ChatColor.RED + "This feature is disabled.");
            return true;
        }
        openRemoveMenu(player);
        return true;
    }

    private boolean executeShare(Player player, String[] args) {
        if (!plugin.getConfig().getBoolean("bed-sharing")) {
            player.sendMessage(ChatColor.RED + "This feature is disabled.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /mrl share <player>");
            return false;
        }

        Player receiverPlayer = Bukkit.getPlayer(args[0]);
        if (receiverPlayer == null) {
            player.sendMessage(ChatColor.RED + plugin.getMessages("player-not-found"));
            return false;
        }
        if (receiverPlayer == player) {
            player.sendMessage(ChatColor.RED + "You cannot share a bed with yourself.");
            return false;
        }

        Block bed = checkIfIsBed(player.getTargetBlockExact(4));
        if (bed != null) {
            BlockState blockState = bed.getState();
            String bedUUID = null;
            if (blockState instanceof TileState tileState) {
                PersistentDataContainer container = tileState.getPersistentDataContainer();
                if (container.has(new NamespacedKey(plugin, "uuid"), PersistentDataType.STRING)) {
                    bedUUID = container.get(new NamespacedKey(plugin, "uuid"), PersistentDataType.STRING);
                }
            }

            if (bedUUID == null) {
                player.sendMessage(ChatColor.RED + plugin.getMessages("bed-not-registered-message"));
                return false;
            }

            PlayerBedsData playerBedsData = null;
            PersistentDataContainer playerData = player.getPersistentDataContainer();

            if (playerData.has(new NamespacedKey(plugin, "beds"), new BedsDataType())) {
                playerBedsData = playerData.get(new NamespacedKey(plugin, "beds"), new BedsDataType());
                if (playerBedsData != null && playerBedsData.getPlayerBedData() != null
                        && playerBedsData.hasBed(bedUUID)) {
                    PersistentDataContainer receiverData = receiverPlayer.getPersistentDataContainer();
                    PlayerBedsData receiverBedsData = receiverData.has(new NamespacedKey(plugin, "beds"),
                            new BedsDataType())
                                    ? receiverData.get(new NamespacedKey(plugin, "beds"), new BedsDataType())
                                    : new PlayerBedsData();

                    playerBedsData.shareBed(receiverBedsData, bedUUID);
                    receiverData.set(new NamespacedKey(plugin, "beds"), new BedsDataType(), receiverBedsData);
                    playerData.set(new NamespacedKey(plugin, "beds"), new BedsDataType(), playerBedsData);

                    receiverPlayer.sendMessage(plugin.getMessages("bed-registered-successfully-message"));
                } else {
                    player.sendMessage(ChatColor.RED + plugin.getMessages("bed-not-registered-message"));
                    return false;
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + plugin.getMessages("bed-not-found-message"));
            return false;
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        return getCompletions(sender, args);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return getCompletions(sender, args);
    }

    private List<String> getCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        // Construire la liste des sous-commandes disponibles
        List<String> subCommands = new ArrayList<>();
        subCommands.add("listbed");
        subCommands.add("rename");
        if (plugin.getConfig().getBoolean("remove-beds-gui")) {
            subCommands.add("delete");
        }
        if (plugin.getConfig().getBoolean("bed-sharing")) {
            subCommands.add("share");
        }

        if (args.length == 0) {
            // Pas d'argument, retourner toutes les sous-commandes
            return subCommands;
        } else if (args.length == 1) {
            // Premier argument en cours de saisie
            String input = args[0].toLowerCase();
            for (String cmd : subCommands) {
                if (cmd.toLowerCase().startsWith(input)) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("share")) {
            // Deuxième argument pour share = nom de joueur
            String input = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (sender instanceof Player && player.equals(sender)) {
                    continue; // Ne pas suggérer soi-même
                }
                if (player.getName().toLowerCase().startsWith(input)) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }
}
