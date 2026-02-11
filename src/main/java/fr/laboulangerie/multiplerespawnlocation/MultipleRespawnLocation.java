package fr.laboulangerie.multiplerespawnlocation;

import fr.laboulangerie.multiplerespawnlocation.commands.MrlCommand;
import fr.laboulangerie.multiplerespawnlocation.hooks.MineletHook;
import fr.laboulangerie.multiplerespawnlocation.hooks.MultiSpawnHook;
import fr.laboulangerie.multiplerespawnlocation.listeners.*;

import org.bukkit.command.CommandMap;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;

public final class MultipleRespawnLocation extends JavaPlugin {

    private Configuration messages;

    private static MultipleRespawnLocation instance;

    @Override
    public void onEnable() {
        instance = this;

        getConfig().options().copyDefaults(true);
        saveConfig();
        createLanguageConfig();

        // Initialize hooks after all plugins are loaded
        getServer().getScheduler().runTaskLater(this, () -> {
            MineletHook.init();
            MultiSpawnHook.init();
        }, 1L);

        getServer().getPluginManager().registerEvents(new PlayerRespawnListener(this), this);
        getServer().getPluginManager().registerEvents(new RespawnMenuHandler(this), this);
        getServer().getPluginManager().registerEvents(new RemoveMenuHandler(this), this);
        getServer().getPluginManager().registerEvents(new PlayerGetsOnBedListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new TabCompleteListener(this), this);

        try {
            CommandMap commandMap = fr.laboulangerie.multiplerespawnlocation.utils.CommandMapUtil.getCommandMap();
            commandMap.register(this.getName(), new MrlCommand(this, "mrl"));
            this.getLogger().info("Commands added successfully");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            this.getLogger().warning("Could not access commandMap. Commands will not work");
            this.getLogger().warning(e.getMessage());
        }
    }

    public static MultipleRespawnLocation getInstance() {
        return instance;
    }

    // get message of selected language
    public String getMessages(String path) {
        return this.messages.getString(path);
    }

    private void createLanguageConfig() {
        String lang = this.getConfig().getString("lang");
        InputStream input;
        try { // tries getting selected languages
            input = getClass().getClassLoader().getResourceAsStream("languages/{key}.yml".replace("{key}", lang));
            this.messages = YamlConfiguration.loadConfiguration(new InputStreamReader(input));
        } catch (Exception e) { // else sets enUS as default
            input = getClass().getClassLoader().getResourceAsStream("languages/enUS.yml");
            this.messages = YamlConfiguration.loadConfiguration(new InputStreamReader(input));
        }
    }
}
