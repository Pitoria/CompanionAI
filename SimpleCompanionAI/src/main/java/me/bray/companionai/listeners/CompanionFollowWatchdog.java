package me.bray.companionai.listeners;

import me.bray.companionai.CompanionAI;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.FollowTrait;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CompanionFollowWatchdog {

    private final CompanionAI plugin;

    public CompanionFollowWatchdog(CompanionAI plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 40L, 40L);
    }

    private void tick() {
        // 1. Comprobar y leer de base de datos data.yml
        if (!plugin.getDataManager().getData().contains("players")) {
            return;
        }

        for (String uuidStr : plugin.getDataManager().getData().getConfigurationSection("players").getKeys(false)) {

            // 2. Leer estado real del jugador en data.yml
            String state = plugin.getDataManager().getData().getString("players." + uuidStr + ".state", "STAY");

            if (!state.equalsIgnoreCase("FOLLOW")) {
                continue;
            }

            Player player = Bukkit.getPlayer(UUID.fromString(uuidStr));

            if (player == null || !player.isOnline()) {
                continue;
            }

            // 3. Obtener el ID del NPC desde data.yml
            int npcId = plugin.getDataManager().getData().getInt("players." + uuidStr + ".npc-id");
            NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);

            if (npc == null || !npc.isSpawned()) {
                continue;
            }

            if (!npc.getEntity().getWorld().equals(player.getWorld())) {
                continue;
            }

            double distance = npc.getEntity().getLocation().distance(player.getLocation());

            double verticalDistance = Math.abs(
                    npc.getEntity().getLocation().getY() - player.getLocation().getY()
            );

            // Opciones estáticas se quedan en el config.yml general
            double maxDistance = plugin.getConfig().getDouble("settings.follow.max-distance", 30.0);
            double maxVerticalDistance = plugin.getConfig().getDouble("settings.follow.max-vertical-distance", 8.0);

            // Si se alejó demasiado, teletransportar
            if (distance > maxDistance || verticalDistance > maxVerticalDistance) {
                npc.teleport(
                        player.getLocation(),
                        org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN
                );
                continue;
            }

            // Si está lo suficientemente cerca (menos de 6 bloques), que deje de caminar
            if (distance < 6) {
                continue;
            }

            // REFRESCAR TRAIT
            //FollowTrait followTrait = npc.getOrAddTrait(FollowTrait.class);
            //followTrait.follow(player);
            //followTrait.setFollowingMargin(3.0);
            //followTrait.setProtect(true);
        }
    }
}