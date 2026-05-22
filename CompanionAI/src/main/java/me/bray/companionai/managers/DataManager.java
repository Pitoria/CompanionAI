package me.bray.companionai.managers;

import me.bray.companionai.CompanionAI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class DataManager {

    private final CompanionAI plugin;
    private final File file;
    private FileConfiguration data;

    public DataManager(CompanionAI plugin) {

        this.plugin = plugin;

        file = new File(plugin.getDataFolder(), "data.yml");

        if (!file.exists()) {
            plugin.saveResource("data.yml", false);
        }

        data = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getData() {
        return data;
    }

    public void save() {

        try {
            data.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reload() {
        data = YamlConfiguration.loadConfiguration(file);
    }
}