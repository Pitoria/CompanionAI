package me.bray.companionai;

import me.bray.companionai.commands.CompanionCommand;
import me.bray.companionai.listeners.*;
import me.bray.companionai.managers.DataManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import me.bray.companionai.listeners.CompanionFollowWatchdog;
import me.bray.companionai.listeners.CompanionRegenTask;
import java.io.File;

public class CompanionAI extends JavaPlugin {

    private DataManager dataManager;

    private FileConfiguration messages;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        saveResource("messages.yml", false);

        File messagesFile = new File(getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        CompanionCommand companionCommand = new CompanionCommand(this);




        getCommand("companion").setExecutor(companionCommand);
        getCommand("companion").setTabCompleter(companionCommand);

        getServer().getPluginManager().registerEvents(
                new CompanionClickListener(this),
                this
        );

        getServer().getPluginManager().registerEvents(
                new CompanionProtectListener(this),
                this
        );

        getServer().getPluginManager().registerEvents(
                new CompanionAssistListener(this),
                this
        );

        getServer().getPluginManager().registerEvents(
                new CompanionHealthListener(this),
                this
        );

        getServer().getPluginManager().registerEvents(
                new CompanionDeathListener(this),
                this
        );

        getServer().getPluginManager().registerEvents(
                new CompanionWorldListener(this),
                this
        );

        getServer().getPluginManager().registerEvents(
                new CompanionQuitListener(this),
                this
        );

        getServer().getPluginManager().registerEvents(
                new CompanionJoinListener(this)
                , this
        );

        getServer().getPluginManager().registerEvents(
                new CombatListener(this),
                this
        );

        new CompanionFollowWatchdog(this);

        new CompanionRegenTask(this);

        dataManager = new DataManager(this);

        getLogger().info("CompanionAI enabled!");
        getLogger().info("Created by Braiton");
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public void reloadMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    @Override
    public void onDisable() {

        getLogger().info("CompanionAI disabled!");
        getLogger().info("Goodbye :)");
    }
}