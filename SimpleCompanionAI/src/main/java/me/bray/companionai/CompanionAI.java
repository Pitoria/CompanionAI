package me.bray.companionai;

import me.bray.companionai.commands.CompanionCommand;
import me.bray.companionai.listeners.*;
import me.bray.companionai.managers.DataManager;
import me.bray.companionai.listeners.CompanionFollowWatchdog;
import me.bray.companionai.listeners.CompanionRegenTask;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Owner;
import java.util.UUID;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
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

        dataManager = new DataManager(this);

        runMigrations();

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

        getLogger().info("SimpleCompanionAI enabled!");
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

        getLogger().info("SimpleCompanionAI disabled!");
        getLogger().info("Goodbye :)");
    }

    private void runMigrations() {
        int version = dataManager.getData().getInt("data-version", 0);

        if (version < 2) {
            migrateToV2_SetCitizensOwners();
            dataManager.getData().set("data-version", 2);
            dataManager.save();

            getLogger().info("Migrated data.yml to version 2.");
        }
    }

    private void migrateToV2_SetCitizensOwners() {
        if (!dataManager.getData().contains("players")) {
            return;
        }

        int fixed = 0;

        for (String uuidString : dataManager.getData()
                .getConfigurationSection("players")
                .getKeys(false)) {

            try {
                UUID uuid = UUID.fromString(uuidString);
                int npcId = dataManager.getData().getInt("players." + uuid + ".npc-id");

                NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);

                if (npc == null) {
                    continue;
                }

                npc.getOrAddTrait(Owner.class).setOwner(uuid);
                fixed++;

            } catch (Exception exception) {
                getLogger().warning("Could not migrate companion owner: " + uuidString);
            }
        }

        getLogger().info("Updated Citizens owners for " + fixed + " companions.");
    }

}