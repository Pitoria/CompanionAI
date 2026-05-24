package me.bray.companionai.listeners;

import me.bray.companionai.CompanionAI;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class CompanionDeathListener implements Listener {

    private final CompanionAI plugin;

    public CompanionDeathListener(CompanionAI plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getEntity());

        if (npc == null || !isCompanion(npc)) {
            return;
        }

        event.getDrops().clear();
        event.setDroppedExp(0);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            npc.despawn();
        }, 5L);
    }

    private boolean isCompanion(NPC npc) {
        // CORREGIDO: Cambiado a data.yml
        if (!plugin.getDataManager().getData().contains("players")) {
            return false;
        }

        for (String uuid : plugin.getDataManager().getData().getConfigurationSection("players").getKeys(false)) {
            int npcId = plugin.getDataManager().getData().getInt("players." + uuid + ".npc-id");
            if (npc.getId() == npcId) {
                return true;
            }
        }
        return false;
    }
}