package me.bray.companionai.listeners;

import me.bray.companionai.CompanionAI;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class CompanionQuitListener implements Listener {

    private final CompanionAI plugin;

    public CompanionQuitListener(CompanionAI plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String path = "players." + player.getUniqueId() + ".npc-id";

        // 1. Buscamos de forma real en tu data.yml usando tu DataManager
        if (!plugin.getDataManager().getData().contains(path)) {
            return;
        }

        int npcId = plugin.getDataManager().getData().getInt(path);
        NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);

        // 2. Si el NPC está activo en el mundo, lo despawneamos de forma segura
        if (npc != null && npc.isSpawned()) {
            npc.despawn();
        }
    }
}