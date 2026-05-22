package me.bray.companionai.listeners;

import me.bray.companionai.CompanionAI;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.FollowTrait;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class CompanionJoinListener implements Listener {

    private final CompanionAI plugin;

    public CompanionJoinListener(CompanionAI plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String path = "players." + player.getUniqueId() + ".npc-id";

        // 1. Verificamos en data.yml si el jugador tiene un companion registrado
        if (!plugin.getDataManager().getData().contains(path)) {
            return;
        }

        int npcId = plugin.getDataManager().getData().getInt(path);
        NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);

        // 2. Si el NPC existe pero no está spawneado en el mundo, lo traemos de vuelta
        if (npc != null && !npc.isSpawned()) {

            // Le damos un delay mínimo de 5 ticks (un cuarto de segundo)
            // para asegurar que el jugador terminó de cargar el mundo por completo
            Bukkit.getScheduler().runTaskLater(plugin, () -> {

                // Spawnear al NPC al lado del jugador
                npc.spawn(player.getLocation());

                // Forzar que lo vuelva a seguir si su estado guardado era "FOLLOW"
                String state = plugin.getDataManager().getData().getString(
                        "players." + player.getUniqueId() + ".state",
                        "STAY"
                );

                if (state.equalsIgnoreCase("FOLLOW")) {
                    FollowTrait followTrait = npc.getOrAddTrait(FollowTrait.class);
                    followTrait.follow(player);
                    followTrait.setFollowingMargin(3.0);
                    followTrait.setProtect(true);
                }

            }, 5L);
        }
    }
}