package me.bray.companionai.listeners;

import me.bray.companionai.CompanionAI;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.FollowTrait;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class CompanionWorldListener implements Listener {

    private final CompanionAI plugin;

    public CompanionWorldListener(CompanionAI plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        NPC npc = getPlayerCompanion(player);

        if (npc == null || !npc.isSpawned()) {
            return;
        }

        // CORREGIDO: Cambiado a data.yml
        String state = plugin.getDataManager().getData().getString(
                "players." + player.getUniqueId() + ".state",
                "STAY"
        );

        if (!state.equalsIgnoreCase("FOLLOW")) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            FollowTrait followTrait = npc.getOrAddTrait(FollowTrait.class);
            followTrait.follow(null);
            followTrait.follow(player);
            followTrait.setFollowingMargin(3.0);
            followTrait.setProtect(true);
        }, 1L);
    }

    private NPC getPlayerCompanion(Player player) {
        String path = "players." + player.getUniqueId() + ".npc-id";

        // CORREGIDO: Cambiado a data.yml
        if (!plugin.getDataManager().getData().contains(path)) {
            return null;
        }

        int npcId = plugin.getDataManager().getData().getInt(path);
        return CitizensAPI.getNPCRegistry().getById(npcId);
    }
}